package stew5;

import java.io.*;
import java.util.*;
import stew5.ui.console.ConsoleLauncher;
import stew5.ui.swing.WindowLauncher;

/**
 * Main class.
 */
public final class App {

    // These fields need to evaluate orderly on static-initializer
    private static final Logger log = Logger.getLogger(App.class);

    public static final String rootPackageName = App.class.getPackage().getName();
    private static final File dir = initializeDirectory();
    public static final LayeredProperties props = initializeProperties();

    private static final String PropKey = "net.argius.stew.properties";
    private static final String PropFileName = "stew.properties";
    private static final String DefaultDir = ".stew";

    private App() { // empty
    }

    private static File initializeDirectory() {
        File directory;
        String path = System.getProperty(PropKey, System.getProperty(PropFileName, ""));
        if (path.length() == 0) {
            directory = new File(DefaultDir);
        } else {
            File file = new File(path);
            if (file.isDirectory()) {
                directory = file;
            } else {
                directory = file.getParentFile();
                if (directory == null) {
                    directory = new File(DefaultDir);
                }
            }
        }
        try {
            if (!directory.isDirectory()) {
                if (!directory.mkdirs() || !directory.isDirectory()) {
                    throw new IOException("can't make directory: " + directory);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return directory;
    }

    private static LayeredProperties initializeProperties() {
        Map<String, String> layer1 = System.getenv();
        Properties layer2 = getFileProperties();
        Properties layer3 = System.getProperties();
        LayeredProperties newProps = new LayeredProperties(layer1, layer2, layer3);
        if (log.isDebugEnabled()) {
            log.debug(newProps.dump());
        }
        return newProps;
    }

    private static Properties getFileProperties() {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(new File(dir, PropFileName))) {
            props.load(is);
        } catch (IOException e) {
            log.warn(e, "getFileProperties");
        }
        return props;
    }

    /**
     * Returns system directory.
     * @return
     */
    public static File getSystemDirectory() {
        return dir;
    }

    public static File getSystemFile(String name) {
        return new File(dir, name);
    }

    /**
     * Returns this app's system property.
     * @param key
     * @return
     */
    @Deprecated
    public static String getProperty(String key) {
        return props.get(key.replace("net.argius.stew.", ""), "");
    }

    /**
     * Returns this app's system property.
     * @param key
     * @param defaultValue
     * @return
     */
    @Deprecated
    public static String getProperty(String key, String defaultValue) {
        return props.get(key.replace("net.argius.stew.", defaultValue));
    }

    /**
     * Returns this app's system property as int.
     * @param key
     * @param defaultValue
     * @return
     */
    @Deprecated
    public static int getPropertyAsInt(String key, int defaultValue) {
        return props.getAsInt(key.replace("net.argius.stew.", ""), defaultValue);
    }

    /**
     * Returns this app's system property as boolean.
     * @param key
     * @return
     */
    @Deprecated
    public static boolean getPropertyAsBoolean(String key) {
        return props.getAsBoolean(key.replace("net.argius.stew.", ""));
    }

    /**
     * Returns whether the property has specified key or not.
     * @param key
     * @return
     */
    @Deprecated
    public static boolean hasProperty(String key) {
        return props.hasKey(key.replace("net.argius.stew.", ""));
    }

    /**
     * Returns app version.
     * @return
     */
    public static String getVersion() {
        return ResourceManager.Default.read("version", "(UNKNOWN)");
    }

    /** main **/
    public static void main(String... args) {
        int guiCount = 0;
        int cuiCount = 0;
        OptionSet opts;
        try {
            opts = OptionSet.parseArguments(args);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        if (opts.isShowVersion()) {
            System.out.println("Stew " + App.getVersion());
            return;
        }
        if (guiCount == 0 && cuiCount == 0) {
            for (String k : new String[]{"bootstrap", "boot",}) {
                final String v = props.get(k, "");
                if (v.equalsIgnoreCase("GUI")) {
                    ++guiCount;
                }
                if (v.equalsIgnoreCase("CUI")) {
                    ++cuiCount;
                }
            }
        }
        if (opts.isCui()) {
            ConsoleLauncher.main(args);
        } else if (opts.isGui()) {
            WindowLauncher.main(args);
        } else if (cuiCount > 0) {
            ConsoleLauncher.main(args);
        } else if (guiCount > 0) {
            WindowLauncher.main(args);
        }
    }

}
