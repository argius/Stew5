package stew5.io;

import static org.junit.Assert.*;
import java.io.*;
import org.junit.*;

public final class CsvExporterTest {

    @Test
    public void testCsvExporterOutputStream() {
        // do nothing
    }

    @Test
    public void testCsvExporterOutputStreamChar() {
        // do nothing
    }

    @Test
    public void testAddRow() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (CsvExporter exporter = new CsvExporter(bos, ',')) {
            exporter.addHeader(a("name", "number"));
            exporter.addRow(a("abc", "123"));
        }
        assertEquals("name,number%nabc,123%n", bos.toString().replaceAll("\r?\n", "%n"));
    }

    @Test
    public void testClose() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CsvExporter exporter = new CsvExporter(bos);
        exporter.close();
    }

    @SuppressWarnings("unchecked")
    static <T> T[] a(T... a) {
        return a;
    }

}
