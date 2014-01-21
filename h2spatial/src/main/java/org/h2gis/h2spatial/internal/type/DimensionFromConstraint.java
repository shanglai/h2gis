package org.h2gis.h2spatial.internal.type;

import org.h2gis.h2spatial.internal.function.spatial.properties.ST_CoordDim;
import org.h2gis.h2spatial.internal.function.spatial.properties._ColumnSRID;
import org.h2gis.h2spatialapi.DeterministicScalarFunction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check column constraint for Z constraint. Has M is not supported yet by JTS Topology Suite WKTReader.
 * @author Nicolas Fortin
 */
public class DimensionFromConstraint extends DeterministicScalarFunction {
    private static final Pattern Z_CONSTRAINT_PATTERN = Pattern.compile(
            "ST_COORDDIM\\s*\\(\\s*((([\"`][^\"`]+[\"`])|(\\w+)))\\s*\\)\\s*(((!|<|>)?=)|<>|>|<)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * Check column constraint for Z constraint. Has M is not supported yet by JTS Topology Suite WKTReader.
     */
    public DimensionFromConstraint() {
        addProperty(PROP_REMARKS, "Check column constraint for Z constraint.");
        addProperty(PROP_NAME, "_DimensionFromConstraint");
    }

    @Override
    public String getJavaStaticMethod() {
        return "dimensionFromConnection";
    }

    public static int dimensionFromConstraint(String constraint, String columnName) {
        Matcher matcher = Z_CONSTRAINT_PATTERN.matcher(constraint);
        if(matcher.find()) {
            String extractedColumnName = matcher.group(1).replace("\"","").replace("`", "");
            if(extractedColumnName.equalsIgnoreCase(columnName)) {
                int constraint_value = Integer.valueOf(matcher.group(8));
                String sign = matcher.group(5);
                if("<>".equals(sign) || "!=".equals(sign)) {
                    constraint_value = constraint_value == 3 ? 2 : 3;
                }
                if("<".equals(sign)) {
                    constraint_value = 2;
                }
                if(">".equals(sign)) {
                    constraint_value = constraint_value == 2 ? 3 : 2;
                }
                return constraint_value;
            }
        }
        return 2;
    }

    public static int dimensionFromConnection(Connection connection, String catalogName, String schemaName, String tableName, String columnName,String constraint) {
        try {
            Statement st = connection.createStatement();
            // Merge column constraint and table constraint
            constraint+= _ColumnSRID.fetchConstraint(connection, catalogName, schemaName, tableName);
            return dimensionFromConstraint(constraint, columnName);
        } catch (SQLException ex) {
            return 2;
        }
    }
}
