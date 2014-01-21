/**
 * h2spatial is a library that brings spatial support to the H2 Java database.
 *
 * h2spatial is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)
 *
 * h2patial is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * h2spatial is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * h2spatial. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.h2gis.h2spatial;

import org.h2gis.h2spatial.ut.SpatialH2UT;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Nicolas Fortin
 */
public class SpatialFunctionTest {
    private static Connection connection;
    private static final String DB_NAME = "SpatialFunctionTest";

    @BeforeClass
    public static void tearUp() throws Exception {
        // Keep a connection alive to not close the DataBase on each unit test
        connection = SpatialH2UT.createSpatialDataBase(DB_NAME);
        // Set up test data
        URL sqlURL = SpatialFunctionTest.class.getResource("ogc_conformance_test3.sql");
        URL sqlURL2 = SpatialFunctionTest.class.getResource("spatial_index_test_data.sql");
        Statement st = connection.createStatement();
        st.execute("RUNSCRIPT FROM '"+sqlURL+"'");
        st.execute("RUNSCRIPT FROM '"+sqlURL2+"'");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        connection.close();
    }

    @Test
    public void test_ST_EnvelopeIntersects() throws Exception  {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT ST_EnvelopesIntersect(road_segments.centerline, divided_routes.centerlines) " +
                "FROM road_segments, divided_routes WHERE road_segments.fid = 102 AND divided_routes.name = 'Route 75'");
        assertTrue(rs.next());
        assertTrue(rs.getBoolean(1));
        rs.close();
    }

    @Test
    public void test_ST_UnionAggregate() throws Exception  {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT ST_Area(ST_Union(ST_Accum(footprint))) FROM buildings GROUP BY SUBSTRING(address,4)");
        assertTrue(rs.next());
        assertEquals(16,rs.getDouble(1),1e-8);
        rs.close();
    }

    @Test
    public void test_ST_UnionSimple() throws Exception  {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT ST_Area(ST_Union('POLYGON((0 0,10 0,10 10,0 10,0 0))'))");
        assertTrue(rs.next());
        assertEquals(100,rs.getDouble(1),0);
        rs.close();
        rs = st.executeQuery("SELECT ST_Area(ST_Union('MULTIPOLYGON(((0 0,5 0,5 5,0 5,0 0)),((5 5,10 5,10 10,5 10,5 5)))'))");
        assertTrue(rs.next());
        assertEquals(50,rs.getDouble(1),0);
        rs.close();
    }

    @Test
    public void test_ST_UnionAggregateAlone() throws Exception  {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT ST_Union('MULTIPOLYGON (((1 4, 1 8, 5 5, 1 4)), ((3 8, 2 5, 5 5, 3 8)))')");
        assertTrue(rs.next());
        assertEquals("POLYGON ((1 4, 1 8, 2.6 6.8, 3 8, 5 5, 1 4))",rs.getString(1));
        rs.close();
    }

    @Test
    public void test_ST_AccumArea() throws Exception  {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT ST_Area(ST_Accum(footprint)) FROM buildings GROUP BY SUBSTRING(address,4)");
        assertTrue(rs.next());
        assertEquals(16,rs.getDouble(1),1e-8);
        rs.close();
    }

    @Test
    public void test_ST_Accum() throws Exception  {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT ST_Accum(footprint) FROM buildings GROUP BY SUBSTRING(address,4)");
        assertTrue(rs.next());
        assertEquals("GEOMETRYCOLLECTION (POLYGON ((50 31, 54 31, 54 29, 50 29, 50 31)), POLYGON ((66 34, 62 34, 62 32, 66 32, 66 34)))",rs.getString(1));
        rs.close();
    }

    @Test
    public void testFunctionRemarks() throws SQLException {
        CreateSpatialExtension.registerFunction(connection.createStatement(), new DummyFunction(), "");
        ResultSet procedures = connection.getMetaData().getProcedures(null, null, "DUMMYFUNCTION");
        assertTrue(procedures.next());
        assertEquals(DummyFunction.REMARKS, procedures.getString("REMARKS"));
        procedures.close();
        CreateSpatialExtension.unRegisterFunction(connection.createStatement(), new DummyFunction());
    }

    @Test
    public void testSetSRID() throws SQLException {
        Statement st = connection.createStatement();
        st.execute("drop table if exists testSrid");
        st.execute("create table testSrid(the_geom geometry)");
        st.execute("insert into testSrid values (ST_GeomFromText('POINT( 15 25 )',27572))");
        ResultSet rs = st.executeQuery("SELECT ST_SRID(ST_SETSRID(the_geom,5321)) trans,ST_SRID(the_geom) original  FROM testSrid");
        assertTrue(rs.next());
        assertEquals(27572, rs.getInt("original"));
        assertEquals(5321, rs.getInt("trans"));
    }


    @Test
    public void test_ST_CoordDim() throws Exception {
        Statement st = connection.createStatement();
        st.execute("DROP TABLE IF EXISTS input_table;" +
                "CREATE TABLE input_table(geom Geometry);" +
                "INSERT INTO input_table VALUES ('POINT(1 2)'),('LINESTRING(0 0, 1 1 2)')," +
                "('LINESTRING (1 1 1, 2 1 2, 2 2 3, 1 2 4, 1 1 5)'),('MULTIPOLYGON (((0 0, 1 1, 0 1, 0 0)))');");
        ResultSet rs = st.executeQuery(
                "SELECT ST_CoordDim(geom) FROM input_table;");
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
        st.execute("DROP TABLE input_table;");
    }
}
