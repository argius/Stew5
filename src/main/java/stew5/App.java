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

    private static final String PropFileName = "stew.properties";

    private App() { // empty
    }

    private static File initializeDirectory() {
        LayeredProperties tmpProps = new LayeredProperties(System.getenv(), System.getProperties());
        String s = tmpProps.get("home", "");
        if (s.isEmpty()) {
            s = System.getenv("HOME");
            if (s == null || s.isEmpty()) {
                s = System.getProperty("user.home");
            }
        }
        log.debug("HOMEDIR=[%s]", s);
        File homeDir = new File(s);
        try {
            File canonicalFile = homeDir.getCanonicalFile().getAbsoluteFile();
            log.info("HOMEDIR(canonical path)=[%s]", canonicalFile);
            return new File(canonicalFile, ".stew");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
    @Deprecated // TODO Remove this, scheduled for 5.0.0-beta3
    public static String getProperty(String key) {
        return props.get(key.replace("net.argius.stew.", ""), "");
    }

    /**
     * Returns this app's system property.
     * @param key
     * @param defaultValue
     * @return
     */
    @Deprecated // TODO Remove this, scheduled for 5.0.0-beta3
    public static String getProperty(String key, String defaultValue) {
        return props.get(key.replace("net.argius.stew.", defaultValue));
    }

    /**
     * Returns this app's system property as int.
     * @param key
     * @param defaultValue
     * @return
     */
    @Deprecated // TODO Remove this, scheduled for 5.0.0-beta3
    public static int getPropertyAsInt(String key, int defaultValue) {
        return props.getAsInt(key.replace("net.argius.stew.", ""), defaultValue);
    }

    /**
     * Returns this app's system property as boolean.
     * @param key
     * @return
     */
    @Deprecated // TODO Remove this, scheduled for 5.0.0-beta3
    public static boolean getPropertyAsBoolean(String key) {
        return props.getAsBoolean(key.replace("net.argius.stew.", ""));
    }

    /**
     * Returns whether the property has specified key or not.
     * @param key
     * @return
     */
    @Deprecated // TODO Remove this, scheduled for 5.0.0-beta3
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

    static void showUsage() {
        ResourceManager res = ResourceManager.Default;
        System.out.println(res.get("i.usagePrefix") + res.get("i.usage.syntax"));
        System.out.println(res.get("usage.message"));
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
        if (opts.isHelp()) {
            OptionSet.showHelp();
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
        } else {
            showUsage();
        }
    }

}
