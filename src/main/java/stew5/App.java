package stew5;

import java.io.*;
import java.util.*;
import stew5.ui.console.ConsoleLauncher;
import stew5.ui.swing.WindowLauncher;

/**
 * Main class.
 */
public final class App {

    public static final String rootPackageName = App.class.getPackage().getName();

    private static final Logger log = Logger.getLogger(App.class);
    private static final String PropKey = "net.argius.stew.properties";
    private static final String PropFileName = "stew.properties";
    private static final String DefaultDir = ".stew";

    private static final File dir = initializeDirectory();
    private static Properties props = initializeProperties();

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

    private static Properties initializeProperties() {
        Properties group3 = System.getProperties();
        Properties group2 = new Properties(group3);
        try {
            group2.putAll(getFileProperties());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Properties group1 = new Properties(group2);
        if (log.isDebugEnabled()) {
            int i = 3;
            for (Properties p : new Properties[]{group3, group2}) {
                List<String> list = new ArrayList<String>(p.size());
                for (Object key : p.keySet()) {
                    list.add((String)key);
                }
                Collections.sort(list);
                Writer buffer = new StringWriter();
                PrintWriter out = new PrintWriter(buffer);
                out.println();
                out.println("--- property group " + (i--) + " ---");
                for (Iterator<String> it = list.iterator(); it.hasNext();) {
                    String key = it.next();
                    out.println(key + '=' + p.getProperty(key));
                }
                log.setEnteredMethodName("initializeProperties");
                log.debug(buffer);
                log.setEnteredMethodName("");
            }
        }
        return group1;
    }

    private static Properties getFileProperties() throws IOException {
        Properties props = new Properties();
        // system property
        String path = System.getProperty(PropFileName);
        if (path != null) {
            File file = new File(path);
            if (file.isDirectory()) {
                file = new File(file, PropFileName);
            }
            if (file.exists()) {
                InputStream is = new FileInputStream(file);
                try {
                    props.load(is);
                    return props;
                } finally {
                    is.close();
                }
            }
        }
        // classpath
        String resourcePath = "/" + PropFileName;
        InputStream res = App.class.getResourceAsStream(resourcePath);
        if (res != null) {
            try {
                props.load(res);
                return props;
            } finally {
                res.close();
            }
        }
        // system directory
        File currentdirfile = new File(dir, PropFileName);
        if (currentdirfile.exists()) {
            InputStream is = new FileInputStream(currentdirfile);
            try {
                props.load(is);
                return props;
            } finally {
                is.close();
            }
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
    public static String getProperty(String key) {
        return props.getProperty(key, "");
    }

    /**
     * Returns this app's system property.
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Returns this app's system property as int.
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getPropertyAsInt(String key, int defaultValue) {
        if (props.getProperty(key) != null) {
            try {
                return Integer.parseInt(props.getProperty(key, ""));
            } catch (NumberFormatException ex) {
                log.warn(ex);
            }
        }
        return defaultValue;
    }

    /**
     * Returns this app's system property as boolean.
     * @param key
     * @return
     */
    public static boolean getPropertyAsBoolean(String key) {
        return Boolean.valueOf(props.getProperty(key, ""));
    }

    /**
     * Returns whether the property has specified key or not.
     * @param key
     * @return
     */
    public static boolean hasProperty(String key) {
        return props.containsKey(key);
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
        List<String> a = new ArrayList<String>();
        for (String arg : args) {
            if (arg.matches("(?i)\\s*--GUI\\s*")) {
                ++guiCount;
            } else if (arg.matches("(?i)\\s*--CUI\\s*")) {
                ++cuiCount;
            } else {
                a.add(arg);
            }
        }
        if (guiCount == 0 && cuiCount == 0) {
            for (String k : new String[]{"stew.bootstrap", "stew.boot",
                                         "net.argius.stew.bootstrap", "net.argius.stew.boot",}) {
                final String v = props.getProperty(k, "");
                if (v.equalsIgnoreCase("GUI")) {
                    ++guiCount;
                }
                if (v.equalsIgnoreCase("CUI")) {
                    ++cuiCount;
                }
            }
        }
        if (guiCount > 0 && cuiCount > 0) {
            throw new IllegalArgumentException("bad option: both --gui and --cui were specified.");
        }
        log.debug("cui=%d, gui=%d, new-args=%s", cuiCount, guiCount, a);
        if (guiCount > 0) {
            WindowLauncher.main();
        } else {
            ConsoleLauncher.main(a.toArray(new String[a.size()]));
        }
    }

}
