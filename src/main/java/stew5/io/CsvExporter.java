package stew5.io;

import java.io.*;
import org.apache.commons.csv.*;
import stew5.*;

/**
 * CSV Exporter implemented with commons-csv.
 */
public final class CsvExporter extends Exporter {

    private CSVPrinter csv;

    protected CsvExporter(OutputStream os) {
        this(os, ',');
    }

    protected CsvExporter(OutputStream os, char delimiter) {
        super(os);
        try {
            this.csv = new CSVPrinter(new PrintWriter(os), getFormat(delimiter));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CSVFormat getFormat(char delimiter) {
        String formatName = App.props.get("io.csvformat", "Default");
        return CSVFormat.valueOf(formatName).withDelimiter(delimiter);
    }

    @Override
    public void addRow(Object[] values) throws IOException {
        ensureOpen();
        csv.printRecord(values);
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        try {
            if (csv != null) {
                csv.flush();
                csv.close();
            }
        } finally {
            csv = null;
            super.close();
        }
    }

}
