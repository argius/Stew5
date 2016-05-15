package stew5;

import java.sql.*;

public final class TestUtils {

    public static Parameter p(String paramString) {
        return new Parameter(paramString);
    }

    public static Connection connection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "sa");
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("create table table1 (id bigint primary key, name text)");
            stmt.executeUpdate("insert into table1 values (1, 'name')");
        }
        conn.commit();
        return conn;
    }

    public static String select(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "";
    }

}
