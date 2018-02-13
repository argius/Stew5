package stew5.ui.swing;

import static java.awt.event.ActionEvent.*;
import static javax.swing.JOptionPane.*;
import static javax.swing.JSplitPane.*;
import static javax.swing.ScrollPaneConstants.*;
import static stew5.ui.swing.AnyActionKey.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.lang.Thread.*;
import java.lang.reflect.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;
import java.util.Timer;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import net.argius.stew.*;
import stew5.*;
import stew5.ui.*;

/**
 * The Launcher implementation for GUI(Swing).
 */
public final class WindowLauncher implements
                                  Launcher,
                                  FocusListener,
                                  AnyActionListener,
                                  Runnable,
                                  UncaughtExceptionHandler {

    static final ResourceManager res = ResourceManager.getInstance(WindowLauncher.class);
    private static final Logger log = Logger.getLogger(WindowLauncher.class);
    private static final String configFileName = "stew.ui.swing.window.config.xml";

    private static final List<WindowLauncher> instances = Collections.synchronizedList(new ArrayList<WindowLauncher>());

    private final WindowOutputProcessor op;
    private final Menu menu;
    private final JPanel panel1;
    private final JSplitPane split1;
    private final JSplitPane split2;
    private final ResultSetTable resultSetTable;
    private final ConsoleTextArea textArea;
    private final DatabaseInfoTree infoTree;
    private final TextSearchPanel textSearchPanel;
    private final JLabel statusBar;
    private final List<String> historyList;
    private final ExecutorService executorService;

    private Environment env;
    private Map<JComponent, TextSearch> textSearchMap;
    private int historyIndex;
    private JComponent focused;

    WindowLauncher() {
        // [Instances]
        instances.add(this);
        final JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        final DatabaseInfoTree infoTree = new DatabaseInfoTree(this);
        final ResultSetTable resultSetTable = new ResultSetTable(this);
        final JTableHeader resultSetTableHeader = resultSetTable.getTableHeader();
        final ConsoleTextArea textArea = new ConsoleTextArea(this);
        this.op = new WindowOutputProcessor(this, resultSetTable, textArea);
        this.menu = new Menu(this);
        this.panel1 = new JPanel(new BorderLayout());
        this.split1 = split1;
        this.split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.resultSetTable = resultSetTable;
        this.textArea = textArea;
        this.infoTree = infoTree;
        this.textSearchPanel = new TextSearchPanel(op);
        this.statusBar = new JLabel(" ");
        this.historyList = new LinkedList<>();
        this.historyIndex = 0;
        this.executorService = Executors.newScheduledThreadPool(3, DaemonThreadFactory.getInstance());
        // [Components]
        // OutputProcessor as frame
        op.setTitle(res.get(".title"));
        op.setIconImage(Utilities.getImageIcon("stew.png").getImage());
        op.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // splitpane (infotree and sub-splitpane)
        split1.setResizeWeight(0.6f);
        split1.setDividerSize(4);
        // splitpane (table and textarea)
        split2.setOrientation(VERTICAL_SPLIT);
        split2.setDividerSize(6);
        split2.setResizeWeight(0.6f);
        // text area
        textArea.setMargin(new Insets(4, 8, 4, 4));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        // text search
        this.textSearchMap = new LinkedHashMap<>();
        final TextSearchPanel textSearchPanel = this.textSearchPanel;
        final Map<JComponent, TextSearch> textSearchMap = this.textSearchMap;
        textSearchMap.put(infoTree, infoTree);
        textSearchMap.put(resultSetTable, resultSetTable);
        textSearchMap.put(resultSetTableHeader,
                          new ResultSetTable.TableHeaderTextSearch(resultSetTable, resultSetTableHeader));
        textSearchMap.put(textArea, textArea);
        for (Entry<JComponent, TextSearch> entry : textSearchMap.entrySet()) {
            final JComponent c = entry.getKey();
            c.addFocusListener(this);
            textSearchPanel.addTarget(entry.getValue());
        }
        // status bar
        statusBar.setForeground(Color.BLUE);
        // [Layouts]
        /*
         * split2 = ResultSetTable + TextArea
         * +----------------------------+
         * | split2                     |
         * | +------------------------+ |
         * | | scroll(resultSetTable) | |
         * | +------------------------+ |
         * | +------------------------+ |
         * | | scroll(textArea)       | |
         * | +------------------------+ |
         * +----------------------------+
         * when DatabaseInfoTree is visible
         * +-----------------------------------+
         * | panel1                            |
         * | +-------------------------------+ |
         * | | split1                        | |
         * | | +------------+ +------------+ | |
         * | | | scroll     | | split2     | | |
         * | | | (infoTree) | |            | | |
         * | | +------------+ +------------+ | |
         * | +-------------------------------+ |
         * | | textSearchPanel               | |
         * | +-------------------------------+ |
         * +-----------------------------------+
         * | status bar                        |
         * +-----------------------------------+
         * when DatabaseInfoTree is not visible
         * +-----------------------------------+
         * | panel1                            |
         * | +-------------------------------+ |
         * | | split2                        | |
         * | +-------------------------------+ |
         * | | textSearchPanel               | |
         * | +-------------------------------+ |
         * +-----------------------------------+
         * | status bar                        |
         * +-----------------------------------+
         */
        split2.setTopComponent(new JScrollPane(resultSetTable));
        split2.setBottomComponent(new JScrollPane(textArea, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER));
        op.add(panel1, BorderLayout.CENTER);
        op.add(statusBar, BorderLayout.PAGE_END);
        op.setJMenuBar(menu);
        // [Restores Configs]
        op.addPropertyChangeListener(menu);
        infoTree.addPropertyChangeListener(menu);
        resultSetTable.addPropertyChangeListener(menu);
        statusBar.addPropertyChangeListener(menu);
        loadConfiguration();
        op.removePropertyChangeListener(menu);
        infoTree.removePropertyChangeListener(menu);
        resultSetTable.removePropertyChangeListener(menu);
        // XXX cannot restore config of status-bar at following code
        // statusBar.removePropertyChangeListener(menu);
        // [Events]
        ContextMenu.create(infoTree, infoTree);
        ContextMenu.create(resultSetTable);
        ContextMenu.create(resultSetTable.getRowHeader(), resultSetTable, "ResultSetTable");
        ContextMenu.create(resultSetTableHeader, resultSetTable, "ResultSetTableColumnHeader");
        ContextMenu.createForText(textArea);
    }

    @Override
    public void launch(Environment env) {
        this.env = env;
        op.setEnvironment(env);
        op.setVisible(true);
        op.output(new Prompt(env));
        textArea.requestFocus();
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        invoke(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.fatal(e, "%s", t);
        op.showErrorDialog(e);
    }

    @Override
    public void focusGained(FocusEvent e) {
        JComponent c = (JComponent)e.getSource();
        focused = c;
        textSearchPanel.setCurrentTarget(textSearchMap.get(c));
    }

    @Override
    public void focusLost(FocusEvent e) {
        // ignore
    }

    @Override
    public void anyActionPerformed(AnyActionEvent ev) {
        log.atEnter("anyActionPerformed", ev);
        ev.validate();
        try {
            resultSetTable.editingCanceled(new ChangeEvent(ev.getSource()));
            final Object source = ev.getSource();
            if (ev.isAnyOf(newWindow)) {
                invoke();
            } else if (ev.isAnyOf(closeWindow)) {
                requestClose();
            } else if (ev.isAnyOf(quit)) {
                requestExit();
            } else if (ev.isAnyOf(showInfoTree)) {
                setInfoTreePaneVisibility(((JCheckBoxMenuItem)source).isSelected());
                op.validate();
                op.repaint();
            } else if (ev.isAnyOf(cut, copy, paste, selectAll)) {
                if (focused != null) {
                    final String cmd;
                    if (ev.isAnyOf(cut)) {
                        if (focused instanceof JTextComponent) {
                            cmd = "cut-to-clipboard";
                        } else {
                            cmd = "";
                        }
                    } else if (ev.isAnyOf(copy)) {
                        if (focused instanceof JTextComponent) {
                            cmd = "copy-to-clipboard";
                        } else {
                            cmd = ev.getActionCommand();
                        }
                    } else if (ev.isAnyOf(paste)) {
                        if (focused instanceof JTextComponent) {
                            cmd = "paste-from-clipboard";
                        } else if (focused instanceof DatabaseInfoTree) {
                            cmd = "";
                        } else {
                            cmd = ev.getActionCommand();
                        }
                    } else if (ev.isAnyOf(selectAll)) {
                        if (focused instanceof JTextComponent) {
                            cmd = "select-all";
                        } else {
                            cmd = ev.getActionCommand();
                        }
                    } else {
                        cmd = "";
                    }
                    if (cmd.length() == 0) {
                        log.debug("no action: %s, cmd=%s", focused.getClass(), ev.getActionCommand());
                    } else {
                        final Action action = focused.getActionMap().get(cmd);
                        log.debug("convert to plain Action Event: orig=%s", ev);
                        action.actionPerformed(new ActionEvent(focused, ACTION_PERFORMED, cmd));
                    }
                }
            } else if (ev.isAnyOf(find)) {
                textSearchPanel.setCurrentTarget(textSearchMap.get(focused));
                textSearchPanel.setVisible(true);
            } else if (ev.isAnyOf(toggleFocus)) {
                if (textArea.isFocusOwner()) {
                    infoTree.requestFocus();
                } else if (infoTree.isFocusOwner()) {
                    resultSetTable.requestFocus();
                } else {
                    textArea.requestFocus();
                }
            } else if (ev.isAnyOf(clearMessage)) {
                textArea.clear();
                executeCommand("");
            } else if (ev.isAnyOf(showStatusBar)) {
                statusBar.setVisible(((JCheckBoxMenuItem)source).isSelected());
            } else if (ev.isAnyOf(showColumnNumber)) {
                resultSetTable.anyActionPerformed(ev);
            } else if (ev.isAnyOf(showAlwaysOnTop)) {
                op.setAlwaysOnTop(((JCheckBoxMenuItem)source).isSelected());
            } else if (ev.isAnyOf(refresh)) {
                refreshResult();
            } else if (ev.isAnyOf(autoAdjustModeNone,
                                  autoAdjustModeHeader,
                                  autoAdjustModeValue,
                                  autoAdjustModeHeaderAndValue)) {
                resultSetTable.setAutoAdjustMode(ev.getActionCommand());
            } else if (ev.isAnyOf(widenColumnWidth, narrowColumnWidth, adjustColumnWidth)) {
                resultSetTable.anyActionPerformed(ev);
            } else if (ev.isAnyOf(executeCommand, execute)) {
                executeCommand(textArea.getEditableText());
            } else if (ev.isAnyOf(breakCommand)) {
                env.getOutputProcessor().close();
                env.setOutputProcessor(new WindowOutputProcessor.Bypass(op));
                op.output(res.get("i.cancelled"));
                doPostProcess();
            } else if (ev.isAnyOf(lastHistory)) {
                retrieveHistory(-1);
            } else if (ev.isAnyOf(nextHistory)) {
                retrieveHistory(+1);
            } else if (ev.isAnyOf(showAllHistories)) {
                if (historyList.isEmpty()) {
                    op.showInformationMessageDialog(res.get("w.no-histories"), null);
                } else {
                    final String msg = res.get("i.choose-history", historyList.size());
                    final String lastCommand = historyList.get(historyList.size() - 1);
                    Object value = op.showInputDialog(msg, null, historyList.toArray(), lastCommand);
                    if (value != null) {
                        textArea.replace((String)value);
                        textArea.prepareSubmitting();
                    }
                }
            } else if (ev.isAnyOf(sendRollback)) {
                if (confirmCommitable()
                    && showConfirmDialog(op, res.get("i.confirm-rollback"), null, OK_CANCEL_OPTION) == OK_OPTION) {
                    executeCommand("rollback");
                }
            } else if (ev.isAnyOf(sendCommit)) {
                if (confirmCommitable()
                    && showConfirmDialog(op, res.get("i.confirm-commit"), null, OK_CANCEL_OPTION) == OK_OPTION) {
                    executeCommand("commit");
                }
            } else if (ev.isAnyOf(connect)) {
                env.updateConnectorMap();
                if (env.getConnectorMap().isEmpty()) {
                    showMessageDialog(op, res.get("w.no-connector"));
                    return;
                }
                Object[] a = ConnectorEntry.toList(env.getConnectorMap().values()).toArray();
                final String m = res.get("i.choose-connection");
                Object value = showInputDialog(op, m, null, PLAIN_MESSAGE, null, a, a[0]);
                if (value != null) {
                    ConnectorEntry c = (ConnectorEntry)value;
                    executeCommand("connect " + c.getId());
                }
            } else if (ev.isAnyOf(disconnect)) {
                executeCommand("disconnect");
            } else if (ev.isAnyOf(postProcessModeNone,
                                  postProcessModeFocus,
                                  postProcessModeShake,
                                  postProcessModeBlink)) {
                op.setPostProcessMode(ev.getActionCommand());
            } else if (ev.isAnyOf(inputEcryptionKey)) {
                editEncryptionKey();
            } else if (ev.isAnyOf(editConnectors)) {
                editConnectorMap();
            } else if (ev.isAnyOf(sortResult)) {
                resultSetTable.doSort(resultSetTable.getSelectedColumn());
            } else if (ev.isAnyOf(importFile, exportFile, showAbout)) {
                op.anyActionPerformed(ev);
            } else if (ev.isAnyOf(showHelp)) {
                showHelp();
            } else if (ev.isAnyOf(ResultSetTable.ActionKey.findColumnName)) {
                resultSetTable.getTableHeader().requestFocus();
                textSearchPanel.setVisible(true);
            } else if (ev.isAnyOf(ResultSetTable.ActionKey.jumpToColumn)) {
                resultSetTable.anyActionPerformed(ev);
            } else if (ev.isAnyOf(ConsoleTextArea.ActionKey.insertText)) {
                textArea.anyActionPerformed(ev);
            } else if (ev.isAnyOf(showLimitedRecords)) {
                infoTree.anyActionPerformed(ev);
            } else {
                log.warn("not expected: Event=%s", ev);
            }
        } catch (Exception ex) {
            op.showErrorDialog(ex);
        }
        log.atExit("dispatch");
    }

    /**
     * Controls visibility of DatabaseInfoTree pane.
     * @param show
     */
    void setInfoTreePaneVisibility(boolean show) {
        if (show) {
            split1.removeAll();
            split1.setTopComponent(new JScrollPane(infoTree));
            split1.setBottomComponent(split2);
            panel1.removeAll();
            panel1.add(split1, BorderLayout.CENTER);
            panel1.add(textSearchPanel, BorderLayout.PAGE_END);
            infoTree.setEnabled(true);
            if (env != null) {
                try {
                    infoTree.refreshRoot(env);
                } catch (SQLException ex) {
                    log.error(ex);
                    op.showErrorDialog(ex);
                }
            }
        } else {
            infoTree.clear();
            infoTree.setEnabled(false);
            panel1.removeAll();
            panel1.add(split2, BorderLayout.CENTER);
            panel1.add(textSearchPanel, BorderLayout.PAGE_END);
        }
        SwingUtilities.updateComponentTreeUI(op);
    }

    private void loadConfiguration() {
        Configuration cnf = Configuration.load();
        op.setSize(cnf.getSize());
        op.setLocation(cnf.getLocation());
        split2.setDividerLocation(cnf.getDividerLocation());
        statusBar.setVisible(cnf.isShowStatusBar());
        resultSetTable.setShowColumnNumber(cnf.isShowTableColumnNumber());
        split1.setDividerLocation(cnf.getDividerLocation0());
        op.setAlwaysOnTop(cnf.isAlwaysOnTop());
        resultSetTable.setAutoAdjustMode(cnf.getAutoAdjustMode());
        op.setPostProcessMode(cnf.getPostProcessMode());
        setInfoTreePaneVisibility(cnf.isShowInfoTree());
        changeFont("monospaced", Font.PLAIN, 1.0d);
    }

    private void saveConfiguration() {
        Configuration cnf = Configuration.load();
        if ((op.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
            // only not maximized
            cnf.setSize(op.getSize());
            cnf.setLocation(op.getLocation());
            cnf.setDividerLocation(split2.getDividerLocation());
            cnf.setDividerLocation0(split1.getDividerLocation());
        }
        cnf.setShowStatusBar(statusBar.isVisible());
        cnf.setShowTableColumnNumber(resultSetTable.isShowColumnNumber());
        cnf.setShowInfoTree(infoTree.isEnabled());
        cnf.setAlwaysOnTop(op.isAlwaysOnTop());
        cnf.setAutoAdjustMode(resultSetTable.getAutoAdjustMode());
        cnf.setPostProcessMode(op.getPostProcessMode());
        cnf.save();
    }

    /**
     * Configuration (Bean) for saving and loading.
     */
    @SuppressWarnings("all")
    public static final class Configuration {

        private static final Logger log = Logger.getLogger(Configuration.class);

        private Dimension size;
        private Point location;
        private int dividerLocation;
        private int dividerLocation0;
        private boolean showStatusBar;
        private boolean showTableColumnNumber;
        private boolean showInfoTree;
        private boolean alwaysOnTop;
        private String autoAdjustMode;
        private String postProcessMode;

        public Configuration() {
            this.size = new Dimension(640, 480);
            this.location = new Point(200, 200);
            this.dividerLocation = -1;
            this.dividerLocation0 = -1;
            this.showStatusBar = false;
            this.showTableColumnNumber = false;
            this.showInfoTree = false;
            this.alwaysOnTop = false;
            this.autoAdjustMode = AnyActionKey.autoAdjustMode.toString();
            this.postProcessMode = AnyActionKey.postProcessMode.toString();
        }

        void save() {
            try {
                saveTo(App.getSystemFile(configFileName));
            } catch (Exception ex) {
                log.warn(ex, "failed to save window configuration");
            }
        }

        void saveTo(File file) throws Exception {
            try (XMLEncoder encoder = new XMLEncoder(new FileOutputStream(file))) {
                HashMap<String, Object> m = new HashMap<>();
                BeanInfo beaninfo = Introspector.getBeanInfo(Configuration.class);
                PropertyDescriptor[] desc = beaninfo.getPropertyDescriptors();
                for (PropertyDescriptor o : desc) {
                    String k = o.getName();
                    Method getter = o.getReadMethod();
                    try {
                        m.put(k, getter.invoke(this));
                    } catch (Exception ex) {
                        log.warn("%s at saving configuration, key=%s", ex, k);
                    }
                }
                encoder.writeObject(m);
            }
        }

        static Configuration load() {
            try {
                return loadFrom(App.getSystemFile(configFileName));
            } catch (Exception ex) {
                log.warn(ex, "failed to load window configuration");
                return new Configuration();
            }
        }

        static Configuration loadFrom(File file) throws Exception {
            Configuration config = new Configuration();
            try (XMLDecoder decoder = new XMLDecoder(new FileInputStream(file))) {
                HashMap<String, Object> m = (HashMap<String, Object>)decoder.readObject();
                BeanInfo beaninfo = Introspector.getBeanInfo(Configuration.class);
                PropertyDescriptor[] desc = beaninfo.getPropertyDescriptors();
                for (PropertyDescriptor o : desc) {
                    String k = o.getName();
                    if (k.equals("class")) {
                        continue;
                    }
                    Method setter = o.getWriteMethod();
                    try {
                        setter.invoke(config, m.get(k));
                    } catch (Exception ex) {
                        log.warn("%s at loading configuration, key=%s", ex, k);
                    }
                }
            }
            return config;
        }

        public Dimension getSize() {
            return size;
        }

        public void setSize(Dimension size) {
            this.size = size;
        }

        public Point getLocation() {
            return location;
        }

        public void setLocation(Point location) {
            this.location = location;
        }

        public int getDividerLocation() {
            return dividerLocation;
        }

        public void setDividerLocation(int dividerLocation) {
            this.dividerLocation = dividerLocation;
        }

        public int getDividerLocation0() {
            return dividerLocation0;
        }

        public void setDividerLocation0(int dividerLocation0) {
            this.dividerLocation0 = dividerLocation0;
        }

        public boolean isShowStatusBar() {
            return showStatusBar;
        }

        public void setShowStatusBar(boolean showStatusBar) {
            this.showStatusBar = showStatusBar;
        }

        public boolean isShowTableColumnNumber() {
            return showTableColumnNumber;
        }

        public void setShowTableColumnNumber(boolean showTableColumnNumber) {
            this.showTableColumnNumber = showTableColumnNumber;
        }

        public boolean isShowInfoTree() {
            return showInfoTree;
        }

        public void setShowInfoTree(boolean showInfoTree) {
            this.showInfoTree = showInfoTree;
        }

        public boolean isAlwaysOnTop() {
            return alwaysOnTop;
        }

        public void setAlwaysOnTop(boolean alwaysOnTop) {
            this.alwaysOnTop = alwaysOnTop;
        }

        public String getAutoAdjustMode() {
            return autoAdjustMode;
        }

        public void setAutoAdjustMode(String autoAdjustMode) {
            this.autoAdjustMode = autoAdjustMode;
        }

        public String getPostProcessMode() {
            return postProcessMode;
        }

        public void setPostProcessMode(String postProcessMode) {
            this.postProcessMode = postProcessMode;
        }

    }

    private void changeFont(String family, int style, double sizeRate) {
        FontControlLookAndFeel.change(family, style, sizeRate);
        SwingUtilities.updateComponentTreeUI(op);
        Font newfont = textArea.getFont();
        if (newfont != null) {
            statusBar.setFont(newfont.deriveFont(newfont.getSize() * 0.8f));
        }
    }

    void handleError(Throwable th) {
        log.error(th);
        op.showErrorDialog(th);
    }

    static void invoke() {
        invoke(new WindowLauncher());
    }

    static void invoke(WindowLauncher instance) {
        final Environment env = new Environment();
        env.setOutputProcessor(new WindowOutputProcessor.Bypass(instance.op));
        instance.launch(env);
    }

    /**
     * Starts to exit application.
     */
    static void exit() {
        for (WindowLauncher instance : new ArrayList<>(instances)) {
            try {
                instance.close();
            } catch (Exception ex) {
                log.warn(ex, "error occurred when closing all instances");
            }
        }
    }

    /**
     * Closes this window.
     */
    void close() {
        instances.remove(this);
        try {
            env.release();
            saveConfiguration();
            executorService.shutdown();
        } finally {
            op.dispose();
        }
    }

    /**
     * Confirms whether pressed YES or not at dialog.
     * @param message
     * @return true if pressed YES
     */
    private boolean confirmYes(String message) {
        return showConfirmDialog(op, message, "", YES_NO_OPTION) == YES_OPTION;
    }

    private boolean confirmCommitable() {
        if (env.getCurrentConnection() == null) {
            showMessageDialog(op, res.get("w.not-connect"), null, OK_OPTION);
            return false;
        }
        if (env.getCurrentConnector().isReadOnly()) {
            showMessageDialog(op, res.get("w.connector-readonly"), null, OK_OPTION);
            return false;
        }
        return true;
    }

    private void retrieveHistory(int value) {
        if (historyList.isEmpty()) {
            return;
        }
        historyIndex += value;
        if (historyIndex >= historyList.size()) {
            historyIndex = 0;
        } else if (historyIndex < 0) {
            historyIndex = historyList.size() - 1;
        }
        textArea.replace(historyList.get(historyIndex));
        textArea.prepareSubmitting();
    }

    /**
     * Requests to close this window.
     */
    void requestClose() {
        if (instances.size() == 1) {
            requestExit();
        } else if (env.getCurrentConnection() == null || confirmYes(res.get("i.confirm-close"))) {
            close();
        }
    }

    /**
     * Requests to exit this application.
     */
    void requestExit() {
        if (confirmYes(res.get("i.confirm-quit"))) {
            exit();
        }
    }

    private void refreshResult() {
        if (resultSetTable.getModel() instanceof ResultSetTableModel) {
            ResultSetTableModel m = resultSetTable.getResultSetTableModel();
            if (m.isSameConnection(env.getCurrentConnection())) {
                final String s = m.getCommandString();
                if (s != null && s.length() > 0) {
                    executeCommand(s);
                }
            }
        }
    }

    private void editEncryptionKey() {
        JPasswordField password = new JPasswordField(20);
        Object[] a = {res.get("i.input-encryption-key"), password};
        if (showConfirmDialog(op, a, null, OK_CANCEL_OPTION) == OK_OPTION) {
            CipherPassword.setSecretKey(String.valueOf(password.getPassword()));
        }
    }

    private void editConnectorMap() {
        env.updateConnectorMap();
        if (env.getCurrentConnector() != null) {
            showMessageDialog(op, res.get("i.reconnect-after-edited-current-connector"));
        }
        ConnectorMapEditDialog dialog = new ConnectorMapEditDialog(op, env);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(op);
        dialog.setVisible(true);
        env.updateConnectorMap();
    }

    private static void showHelp() {
        final String suffix;
        if (res.containsKey("key.lang")) {
            suffix = res.get("key.lang");
        } else {
            suffix = "en"; // default locale
        }
        final String url = "https://github.com/argius/Stew5/wiki/UserGuide_" + suffix;
        if (showConfirmDialog(null, res.get("i.confirm-jump-to-web", url), null, OK_CANCEL_OPTION) != OK_OPTION) {
            return;
        }
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(url));
                } catch (IOException | URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * Executes a command.
     * @param commandString
     */
    void executeCommand(String commandString) {
        assert commandString != null;
        if (!commandString.equals(textArea.getEditableText())) {
            textArea.replace(commandString);
        }
        op.output("");
        if (commandString.trim().length() == 0) {
            doPostProcess();
        } else {
            final String cmd = commandString;
            final Environment env = this.env;
            final DatabaseInfoTree infoTree = this.infoTree;
            final JLabel statusBar = this.statusBar;
            final OutputProcessor opref = env.getOutputProcessor();
            final AnyAction invoker = new AnyAction(this);
            final class CommandTask implements Runnable {
                @Override
                public void run() {
                    Connection conn = env.getCurrentConnection();
                    long time = System.currentTimeMillis();
                    if (!Commands.invoke(env, cmd)) {
                        exit();
                    }
                    if (infoTree.isEnabled()) {
                        try {
                            if (env.getCurrentConnection() != conn) {
                                infoTree.clear();
                                if (env.getCurrentConnection() != null) {
                                    infoTree.refreshRoot(env);
                                }
                            }
                        } catch (Throwable th) {
                            handleError(th);
                        }
                    }
                    if (env.getOutputProcessor() == opref) {
                        time = System.currentTimeMillis() - time;
                        statusBar.setText(res.get("i.statusbar-message", time / 1000f, cmd));
                        invoker.doLater("doPostProcess");
                    }
                }
            }
            try {
                doPreProcess();
                executorService.execute(new CommandTask());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                historyIndex = historyList.size();
            }
            if (historyList.contains(commandString)) {
                historyList.remove(commandString);
            }
            historyList.add(commandString);
        }
        historyIndex = historyList.size();
    }

    void doPreProcess() {
        ((Menu)op.getJMenuBar()).setEnabledStates(true);
        resultSetTable.setEnabled(false);
        textArea.setEnabled(false);
        op.repaint();
    }

    void doPostProcess() {
        ((Menu)op.getJMenuBar()).setEnabledStates(false);
        resultSetTable.setEnabled(true);
        textArea.setEnabled(true);
        op.output(new Prompt(env));
        op.doPostProcess();
    }

    static void wakeup() {
        for (WindowLauncher instance : new ArrayList<>(instances)) {
            try {
                SwingUtilities.updateComponentTreeUI(instance.op);
            } catch (Exception ex) {
                log.warn(ex);
            }
        }
        log.info("wake up");
    }

    private static final class WakeupTimerTask extends TimerTask {
        WakeupTimerTask() {
        } // empty
        private final AnyAction aa = new AnyAction(this);
        @Override
        public void run() {
            aa.doLater("callWakeup");
        }
        @SuppressWarnings("unused")
        void callWakeup() {
            wakeup();
        }
    }

    /**
     * (entry point)
     * @param args
     */
    public static void main(String... args) {
        final int residentCycle = App.props.getAsInt("ui.window.resident", 0);
        if (residentCycle > 0) {
            final long msec = residentCycle * 60000L;
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new WakeupTimerTask(), msec, msec);
        }
        EventQueue.invokeLater(new WindowLauncher());
    }

}
