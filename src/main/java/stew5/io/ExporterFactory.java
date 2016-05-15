package stew5.io;

import java.io.*;

/**
 * A factory to create an Exporter.
 */
final class ExporterFactory {

    private ExporterFactory() {
        // empty
    }

    /**
     * Returns an Exporter.
     * @param path
     * @return
     * @throws IOException
     */
    static Exporter createExporter(Path path) throws IOException {
        final String ext = path.getExtension();
        if (ext.equalsIgnoreCase("xml")) {
            return new XmlExporter(openFile(path));
        } else if (ext.equalsIgnoreCase("htm") || ext.equalsIgnoreCase("html")) {
            return new HtmlExporter(openFile(path), "");
        } else if (ext.equalsIgnoreCase("csv")) {
            return new SimpleExporter(openFile(path), ",");
        } else {
            return new SimpleExporter(openFile(path), "\t");
        }
    }

    private static OutputStream openFile(File file) throws IOException {
        return new FileOutputStream(file);
    }

}
