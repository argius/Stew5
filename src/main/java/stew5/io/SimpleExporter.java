package stew5.io;

import java.io.*;
import stew5.*;
import stew5.io.CsvFormatter.FormatType;

/**
 * A simple implementation of Exporter.
 */
public final class SimpleExporter extends Exporter {

    private static final String PROP_FORMAT = App.rootPackageName + ".SimpleExporter.format";

    private final String separator;

    private PrintWriter out;
    private CsvFormatter formatter;

    /**
     * A constructor.
     * @param os
     * @param separator
     */
    public SimpleExporter(OutputStream os, String separator) {
        this(os, separator, getDefaultFormatter());
    }

    /**
     * A constructor.
     * @param os
     * @param separator
     * @param formatter
     */
    public SimpleExporter(OutputStream os, String separator, CsvFormatter formatter) {
        super(os);
        this.out = new PrintWriter(os);
        this.separator = separator;
        this.formatter = formatter;
    }

    /**
     * Returns the formatter.
     * @return
     */
    public CsvFormatter getFormatter() {
        return formatter;
    }

    /**
     * Sets a formatter.
     * @param formatter
     */
    public void setFormatter(CsvFormatter formatter) {
        this.formatter = formatter;
    }

    private static CsvFormatter getDefaultFormatter() {
        try {
            switch (FormatType.of(App.props.get(PROP_FORMAT).toUpperCase())) {
                case STRING:
                    return CsvFormatter.STRING;
                case ESCAPE:
                    return CsvFormatter.ESCAPE;
                case AUTO:
                    return CsvFormatter.AUTO;
                case RAW:
                default:
                    return CsvFormatter.RAW;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addRow(Object[] values) throws IOException {
        ensureOpen();
        for (int i = 0; i < values.length; i++) {
            Object o = values[i];
            if (i > 0) {
                out.print(separator);
            }
            String value;
            if (o instanceof String) {
                value = (String)o;
            } else if (values[i] != null) {
                value = o.toString();
            } else {
                value = "";
            }
            out.print(formatter.format(value));
        }
        out.println();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        try {
            if (out != null) {
                out.flush();
                out.close();
            }
        } finally {
            out = null;
            super.close();
        }
    }

}
