package stew5;

import static java.sql.Types.*;
import java.math.*;
import java.sql.*;

// This code has been generated by script on 2017-06-09.

/**
 * SqlTypes provides conversions and groupings for SQL data type.
 * @see java.sql.Types
 */
public final class SqlTypes {

    private SqlTypes() { // forbidden
    }

    /**
     * Converts SQL type (int) to Class.
     */
    public static Class<?> toClass(int type) {
        switch (type) {
            case BOOLEAN:
                return Boolean.class;
            case BIT:
                return byte[].class;
            case TINYINT:
                return Byte.class;
            case SMALLINT:
                return Short.class;
            case INTEGER:
                return Integer.class;
            case BIGINT:
                return Long.class;
            case FLOAT:
            case REAL:
                return Float.class;
            case DOUBLE:
                return Double.class;
            case NUMERIC:
            case DECIMAL:
                return BigDecimal.class;
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
                return String.class;
            case DATE:
                return Date.class;
            case TIME:
                return Time.class;
            case TIMESTAMP:
                return Timestamp.class;
            case BLOB:
                return Blob.class;
            case CLOB:
                return Clob.class;
            case NCLOB:
                return NClob.class;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
                return byte[].class;
            case ARRAY:
                return Array.class;
            case STRUCT:
                return Struct.class;
            case REF:
                return Ref.class;
            case SQLXML:
                return SQLXML.class;
            case ROWID:
                return RowId.class;
            case NULL:
            case OTHER:
            case JAVA_OBJECT:
            case DISTINCT:
            case DATALINK:
            default:
                return Object.class;
        }
    }

    /**
     * Converts SQL type (int) to String.
     */
    public static String toTypeString(int type) {
        switch (type) {
            case BIT:
                return "BIT";
            case TINYINT:
                return "TINYINT";
            case SMALLINT:
                return "SMALLINT";
            case INTEGER:
                return "INTEGER";
            case BIGINT:
                return "BIGINT";
            case FLOAT:
                return "FLOAT";
            case REAL:
                return "REAL";
            case DOUBLE:
                return "DOUBLE";
            case NUMERIC:
                return "NUMERIC";
            case DECIMAL:
                return "DECIMAL";
            case CHAR:
                return "CHAR";
            case VARCHAR:
                return "VARCHAR";
            case LONGVARCHAR:
                return "LONGVARCHAR";
            case DATE:
                return "DATE";
            case TIME:
                return "TIME";
            case TIMESTAMP:
                return "TIMESTAMP";
            case BINARY:
                return "BINARY";
            case VARBINARY:
                return "VARBINARY";
            case LONGVARBINARY:
                return "LONGVARBINARY";
            case NULL:
                return "NULL";
            case OTHER:
                return "OTHER";
            case JAVA_OBJECT:
                return "JAVA_OBJECT";
            case DISTINCT:
                return "DISTINCT";
            case STRUCT:
                return "STRUCT";
            case ARRAY:
                return "ARRAY";
            case BLOB:
                return "BLOB";
            case CLOB:
                return "CLOB";
            case REF:
                return "REF";
            case DATALINK:
                return "DATALINK";
            case BOOLEAN:
                return "BOOLEAN";
            case ROWID:
                return "ROWID";
            case NCHAR:
                return "NCHAR";
            case NVARCHAR:
                return "NVARCHAR";
            case LONGNVARCHAR:
                return "LONGNVARCHAR";
            case NCLOB:
                return "NCLOB";
            case SQLXML:
                return "SQLXML";
            default:
                return String.valueOf(type);
        }
    }

    /**
     * Checks text alignment is right or not.
     */
    public static boolean isRightAlign(int type) {
        switch (type) {
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
            case FLOAT:
            case REAL:
            case DOUBLE:
            case NUMERIC:
            case DECIMAL:
                return true;
            case BIT:
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case DATE:
            case TIME:
            case TIMESTAMP:
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case NULL:
            case OTHER:
            case JAVA_OBJECT:
            case DISTINCT:
            case STRUCT:
            case ARRAY:
            case BLOB:
            case CLOB:
            case REF:
            case DATALINK:
            case BOOLEAN:
            case ROWID:
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
            case NCLOB:
            case SQLXML:
            default:
                return false;
        }
    }

    /**
     * Returns whether specified data type should read data as binary.
     */
    public static boolean shouldReadDataAsBinary(int type) {
        switch (type) {
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
            case FLOAT:
            case REAL:
            case DOUBLE:
            case NUMERIC:
            case DECIMAL:
                return false; // numeric to string
            case BIT:
            case DATE:
            case TIME:
            case TIMESTAMP:
            case BOOLEAN:
                return false; // object to string
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
                return false; // char to string
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BLOB:
                return true; // binary to stream
            case CLOB:
            case NCLOB:
                return true; // char to binary-stream
            case SQLXML:
                return false; // char(probably) to string
            case NULL:
                return true; // null (0 byte)
            case OTHER:
                return true; // ? to binary-stream (experimental) (e.g. XML)
            case JAVA_OBJECT:
            case DISTINCT:
            case STRUCT:
            case ARRAY:
            case REF:
            case DATALINK:
            case ROWID:
            default:
                throw new IllegalArgumentException("unsupported type: " + type + " (" + toTypeString(type) + ")");
        }
    }

}
