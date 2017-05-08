package stew5.ui.console;

import java.sql.*;
import java.util.*;
import stew5.*;
import stew5.text.*;
import stew5.ui.*;

/**
 * This is the implementation of OutputProcessor for console.
 */
public final class ConsoleOutputProcessor implements OutputProcessor {

    private static final int WIDTH_LIMIT = 30;

    @Override
    public void output(Object o) {
        if (o instanceof ResultSetReference) {
            outputResult((ResultSetReference)o);
        } else if (o instanceof ResultSet) {
            outputResult(new ResultSetReference((ResultSet)o, ""));
        } else if (o instanceof Prompt) {
            System.err.print(o);
        } else {
            System.out.println(o);
        }
    }

    private static void outputResult(ResultSetReference ref) {
        try {
            // result
            ResultSet rs = ref.getResultSet();
            ColumnOrder order = ref.getOrder();
            ResultSetMetaData rsmeta = rs.getMetaData();
            final boolean needsOrderChange = order.size() > 0;
            System.err.println();
            // column info
            final int columnCount = (needsOrderChange) ? order.size() : rsmeta.getColumnCount();
            int maxWidth = 1;
            StringBuilder borderFormat = new StringBuilder();
            for (int i = 0; i < columnCount; i++) {
                final int index = (needsOrderChange) ? order.getOrder(i) : i + 1;
                int size = rsmeta.getColumnDisplaySize(index);
                if (size > WIDTH_LIMIT) {
                    size = WIDTH_LIMIT;
                } else if (size < 1) {
                    size = 1;
                }
                maxWidth = Math.max(maxWidth, size);
                final int widthExpression;
                switch (rsmeta.getColumnType(index)) {
                    case Types.TINYINT:
                    case Types.SMALLINT:
                    case Types.INTEGER:
                    case Types.BIGINT:
                    case Types.REAL:
                    case Types.DOUBLE:
                    case Types.FLOAT:
                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        widthExpression = size;
                        break;
                    default:
                        widthExpression = -size;
                }
                final String format = "%" + widthExpression + "s";
                borderFormat.append(" " + format);
                if (i != 0) {
                    System.out.print(' ');
                }
                final String name = (needsOrderChange) ? order.getName(i) : rsmeta.getColumnName(index);
                System.out.print(PrintFormat.format(format, name));
            }
            System.out.println();
            // border
            String format = borderFormat.substring(1);
            char[] borderChars = new char[maxWidth];
            Arrays.fill(borderChars, '-');
            Object[] borders = new String[columnCount];
            Arrays.fill(borders, String.valueOf(borderChars));
            System.out.println(PrintFormat.format(format, borders));
            // beginning of loop
            Object[] a = new Object[columnCount];
            final int limit = App.props.getAsInt("rowcount.limit", Integer.MAX_VALUE);
            int count = 0;
            while (rs.next()) {
                if (count >= limit) {
                    System.err.println(ResourceManager.Default.get("w.exceeded-limit", limit));
                    break;
                }
                ++count;
                for (int i = 0; i < columnCount; i++) {
                    final int index = (needsOrderChange) ? order.getOrder(i) : i + 1;
                    a[i] = rs.getString(index);
                }
                System.out.println(PrintFormat.format(format, a));
            }
            System.out.println();
            // end of loop
            ref.setRecordCount(count);
        } catch (SQLException ex) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void close() {
        // do nothing
    }

}
