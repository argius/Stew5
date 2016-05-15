package stew5.io;

import java.io.*;

/**
 * A factory of Importer.
 */
final class ImporterFactory {

    private ImporterFactory() {
        // empty
    }

    /**
     * Returns an Importer.
     * @param path
     * @return
     * @throws IOException
     */
    static Importer createImporter(Path path) throws IOException {
        final String ext = path.getExtension();
        if (ext.equalsIgnoreCase("xml")) {
            return new XmlImporter(openFile(path));
        } else if (ext.equalsIgnoreCase("csv")) {
            return new SmartImporter(openFile(path), ",");
        } else {
            return new SmartImporter(openFile(path), "\t");
        }
    }

    private static InputStream openFile(File file) throws IOException {
        return new FileInputStream(file);
    }

}
