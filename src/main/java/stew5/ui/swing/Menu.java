package stew5.ui.swing;

import static stew5.ui.swing.Menu.Item.*;
import static stew5.ui.swing.Utilities.*;
import java.awt.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;
import java.util.regex.*;
import javax.swing.*;
import stew5.*;

/**
 * The menu bar.
 */
final class Menu extends JMenuBar implements PropertyChangeListener {

    private static final ResourceManager res = ResourceManager.getInstance(Menu.class);
    private static final boolean autoMnemonic = checkAutoMnemonicIsAvailable();

    /**
     * Menu Items.
     */
    enum Item {
        newWindow,
        closeWindow,
        quit,
        cut,
        copy,
        paste,
        selectAll,
        find,
        toggleFocus,
        clearMessage,
        showStatusBar,
        showColumnNumber,
        showInfoTree,
        showAlwaysOnTop,
        refresh,
        widenColumnWidth,
        narrowColumnWidth,
        adjustColumnWidth,
        autoAdjustMode,
        autoAdjustModeNone,
        autoAdjustModeHeader,
        autoAdjustModeValue,
        autoAdjustModeHeaderAndValue,
        executeCommand,
        breakCommand,
        lastHistory,
        nextHistory,
        sendRollback,
        sendCommit,
        connect,
        disconnect,
        postProcessMode,
        postProcessModeNone,
        postProcessModeFocus,
        postProcessModeShake,
        postProcessModeBlink,
        inputEcryptionKey,
        editConnectors,
        sortResult,
        importFile,
        exportFile,
        showHelp,
        showAbout,
        unknown;
        static Item of(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ex) {
                return unknown;
            }
        }
    }

    private List<JMenuItem> lockingTargets;
    private List<JMenuItem> unlockingTargets;
    private EnumMap<Item, JMenuItem> itemToCompMap;

    Menu(final AnyActionListener anyActionListener) {
        this.lockingTargets = new ArrayList<>();
        this.unlockingTargets = new ArrayList<>();
        this.itemToCompMap = new EnumMap<>(Item.class);
        Map<String, JMenuItem> itemMap = new HashMap<>();
        AnyAction aa = new AnyAction(anyActionListener);
        for (final String groupId : res.get("groups").split(",", -1)) {
            JMenu m = add(createJMenu(res, groupId));
            for (final JMenuItem o : createJMenuItems(res, itemMap, "group." + groupId)) {
                if (o == null) {
                    m.add(new JSeparator());
                    continue;
                }
                m.add(o);
                final String itemId = o.getActionCommand();
                Item itemEnum = Item.of(itemId);
                o.addActionListener(aa);
                itemToCompMap.put(itemEnum, o);
                switch (itemEnum) {
                    case closeWindow:
                    case quit:
                    case cut:
                    case copy:
                    case paste:
                    case selectAll:
                    case find:
                    case clearMessage:
                    case refresh:
                    case widenColumnWidth:
                    case narrowColumnWidth:
                    case adjustColumnWidth:
                    case autoAdjustMode:
                    case executeCommand:
                    case lastHistory:
                    case nextHistory:
                    case connect:
                    case disconnect:
                    case postProcessMode:
                    case sortResult:
                    case exportFile:
                        lockingTargets.add(o);
                        break;
                    case breakCommand:
                        unlockingTargets.add(o);
                        break;
                    default:
                }
            }
        }
        for (JMenuItem parent : Arrays.asList(itemToCompMap.get(autoAdjustMode), itemToCompMap.get(postProcessMode))) {
            if (parent != null) {
                for (MenuElement menuGroup : parent.getSubElements()) {
                    for (MenuElement child : menuGroup.getSubElements()) {
                        JMenuItem o = (JMenuItem)child;
                        o.addActionListener(aa);
                        itemToCompMap.put(Item.of(o.getActionCommand()), o);
                    }
                }
            }
        }
        refreshAllAccelerators(itemMap);
        customize(itemMap, anyActionListener);
        setEnabledStates(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        final String propertyName = e.getPropertyName();
        final Object source = e.getSource();
        final Menu.Item key;
        final boolean selected;
        if (source instanceof JLabel && propertyName.equals("ancestor")) {
            key = showStatusBar;
            selected = ((JLabel)source).isVisible();
        } else if (source instanceof ResultSetTable && propertyName.equals("showNumber")) {
            key = showColumnNumber;
            selected = (Boolean)e.getNewValue();
        } else if (source instanceof DatabaseInfoTree) {
            key = showInfoTree;
            selected = ((Component)source).isEnabled();
        } else if (source instanceof JFrame && propertyName.equals("alwaysOnTop")) {
            key = showAlwaysOnTop;
            selected = (Boolean)e.getNewValue();
        } else if (source instanceof ResultSetTable && propertyName.equals("autoAdjustMode")) {
            final String itemName = e.getNewValue().toString();
            if (itemName.matches("[A-Z_]+")) { // ignore old version
                return;
            } else {
                key = Item.of(itemName);
                selected = true;
            }
        } else if (source instanceof WindowOutputProcessor && propertyName.equals("postProcessMode")) {
            final String itemName = e.getNewValue().toString();
            if (itemName.matches("[A-Z_]+")) { // ignore old version
                return;
            } else {
                key = Item.of(itemName);
                selected = true;
            }
        } else {
            return;
        }
        if (itemToCompMap.containsKey(key)) {
            itemToCompMap.get(key).setSelected(selected);
        }
    }

    /**
     * Sets the state that command was started or not.
     * @param commandStarted
     */
    void setEnabledStates(boolean commandStarted) {
        final boolean lockingTargetsState = !commandStarted;
        for (JMenuItem item : lockingTargets) {
            item.setEnabled(lockingTargetsState);
        }
        final boolean unlockingTargetsState = commandStarted;
        for (JMenuItem item : unlockingTargets) {
            item.setEnabled(unlockingTargetsState);
        }
    }

    // Menu factory utilities

    private static JMenu createJMenu(ResourceManager rm, String groupId) {
        final String key = (rm.containsKey("group." + groupId) ? "group" : "item") + '.' + groupId;
        final char mn = rm.getChar(key + ".mnemonic");
        final String groupString = rm.get(key) + (autoMnemonic ? "(" + mn + ")" : "");
        JMenu group = new JMenu(groupString);
        group.setMnemonic(mn);
        return group;
    }

    private static void refreshAllAccelerators(Map<String, JMenuItem> itemMap) {
        // This method is called everytime menu and popup-menu is created.
        File keyBindConf = App.getSystemFile("keybind.conf");
        if (!keyBindConf.exists()) {
            return;
        }
        Map<String, KeyStroke> keyMap = new HashMap<>();
        try (Scanner r = new Scanner(keyBindConf)) {
            final Pattern p = Pattern.compile("\\s*([^=\\s]+)\\s*=(.*)");
            while (r.hasNextLine()) {
                final String line = r.nextLine();
                if (line.trim().length() == 0 || line.matches("\\s*#.*")) {
                    continue;
                }
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    keyMap.put(m.group(1), Utilities.getKeyStroke(m.group(2)));
                }
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        for (final Entry<String, KeyStroke> entry : keyMap.entrySet()) {
            final String k = entry.getKey();
            if (itemMap.containsKey(k)) {
                itemMap.get(k).setAccelerator(entry.getValue());
            }
        }
    }

    static List<JMenuItem> createJMenuItems(ResourceManager rm, String groupKey) {
        HashMap<String, JMenuItem> itemMap = new HashMap<>();
        List<JMenuItem> a = createJMenuItems(rm, itemMap, groupKey);
        refreshAllAccelerators(itemMap);
        return a;
    }

    static List<JMenuItem> createJMenuItems(ResourceManager rm, Map<String, JMenuItem> itemMap, String groupKey) {
        List<JMenuItem> a = new ArrayList<>();
        for (final String itemId : rm.get(groupKey + ".items").split(",", -1)) {
            if (itemId.length() == 0) {
                a.add(null);
            } else {
                JMenuItem o = createJMenuItem(rm, itemId);
                a.add(o);
                itemMap.put(itemId, o);
            }
        }
        return a;
    }

    static JMenuItem createJMenuItem(ResourceManager rm, String itemId) {
        final String itemKey = "item." + itemId;
        final char mn = rm.getChar(itemKey + ".mnemonic");
        final String shortcutKey = itemKey + ".shortcut";
        final JMenuItem o;
        if (rm.isTrue(itemKey + ".checkbox")) {
            o = new JCheckBoxMenuItem();
        } else if (rm.isTrue(itemKey + ".subgroup")) {
            o = createJMenu(rm, itemId);
            ButtonGroup buttonGroup = new ButtonGroup();
            boolean selected = false;
            for (final String id : rm.get(itemKey + ".items").split(",", -1)) {
                final JMenuItem sub = createJMenuItem(rm, itemId + id);
                o.add(sub);
                buttonGroup.add(sub);
                if (!selected) {
                    sub.setSelected(true);
                    selected = true;
                }
            }
        } else {
            o = new JMenuItem();
        }
        if (rm.containsKey(shortcutKey)) {
            KeyStroke ks = Utilities.getKeyStroke(rm.get(shortcutKey));
            if (ks != null) {
                o.setAccelerator(ks);
            }
        }
        o.setText(rm.get(itemKey) + (autoMnemonic ? "(" + mn + ")" : ""));
        o.setMnemonic(mn);
        o.setActionCommand(itemId);
        o.setIcon(getImageIcon(String.format("menu-%s.png", itemId)));
        o.setDisabledIcon(getImageIcon(String.format("menu-disabled-%s.png", itemId)));
        return o;
    }

    private static boolean checkAutoMnemonicIsAvailable() {
        return !App.props.getAsBoolean("ui.suppressGenerateMnemonic") && res.getInt("auto-mnemonic") == 1;
    }

    private void customize(Map<String, JMenuItem> itemMap, final AnyActionListener anyActionListener) {
        try {
            final String javaVersionString = System.getProperty("java.runtime.version", "0");
            final int javaMajorVersion = Integer.parseInt(javaVersionString.replaceFirst("^(\\d+).+?$", "$1"));
            if (System.getProperty("os.name", "").regionMatches(true, 0, "Mac OS X", 0, 8)) {
                AppleMenu.customize(this, javaMajorVersion, itemMap, anyActionListener);
            }
        } catch (Throwable th) {
            WindowOutputProcessor.showErrorDialog(this.getParent(), th);
        }
    }

}
