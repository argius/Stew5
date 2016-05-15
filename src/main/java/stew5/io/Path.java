package stew5.io;

import java.io.*;
import java.net.*;

/**
 * Path is an extended java.util.File.
 */
public final class Path extends File {

    private static final long serialVersionUID = 6787315355616650978L;

    private static final String PATTERN_EXTENSION = "^.*\\.([^\\.]+)$";

    /**
     * A constructor.
     * @param file
     */
    public Path(File file) {
        super(file.getPath());
    }

    /**
     * A constructor.
     * @param parent
     * @param child
     */
    public Path(File parent, String child) {
        super(parent, child);
    }

    /**
     * A constructor.
     * @param pathname
     */
    public Path(String pathname) {
        super(pathname);
    }

    /**
     * A constructor.
     * @param parent
     * @param child
     */
    public Path(String parent, String child) {
        super(new File(parent), child);
    }

    /**
     * A constructor.
     * @param uri URI
     */
    public Path(URI uri) {
        super(uri);
    }

    /**
     * Resolves the path.
     * @param parent
     * @param child
     * @return
     */
    public static Path resolve(File parent, File child) {
        if (child.isAbsolute()) {
            return new Path(child.getAbsolutePath());
        }
        return new Path(parent, child.getPath());
    }

    /**
     * Resolves the path.
     * @param parent
     * @param child
     * @return
     */
    public static Path resolve(File parent, String child) {
        return resolve(parent, new File(child));
    }

    /**
     * Resolves the path.
     * @param parent
     * @param child
     * @return
     */
    public static Path resolve(String parent, String child) {
        return resolve(new File(parent), child);
    }

    /**
     * Returns the extension of this path.
     * @return
     * @see #getExtension(String)
     */
    public String getExtension() {
        return getExtension(getPath());
    }

    /**
     * Returns the extension of this path.
     * @param file
     * @return
     * @see #getExtension(String)
     */
    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    /**
     * Returns the extension of this path.
     * If the path string contains periods,
     * the extension is a string from after the last period to the end.
     * Otherwise, returns an empty string.
     * @param path
     * @return
     */
    public static String getExtension(String path) {
        if (path.matches(PATTERN_EXTENSION)) {
            return path.replaceFirst(PATTERN_EXTENSION, "$1");
        }
        return "";
    }

    /**
     * Creates directories if not exists.
     * @throws IOException If it failed to create directories
     */
    public void makeDirectory() throws IOException {
        makeDirectory(this);
    }

    /**
     * Creates directories if not exists.
     * @param file
     * @throws IOException If it failed to create directories
     */
    public static void makeDirectory(File file) throws IOException {
        if (!file.isDirectory()) {
            if (!file.mkdirs() || !file.isDirectory()) {
                throw new IOException("can't make directory: " + file);
            }
        }
    }

}
