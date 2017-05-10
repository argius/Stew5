package stew5;

import java.util.*;
import org.apache.commons.cli.*;

/**
 * A set of patterns and options.
 */
public final class OptionSet {

    private static final ResourceManager res = ResourceManager.getInstance(OptionSet.class);

    private boolean cui;
    private boolean gui;
    private String connecterName;
    private String commandString;
    private boolean showVersion;
    private boolean help;

    private OptionSet() { // empty
    }

    public static OptionSet parseArguments(String[] args) throws Exception {
        return newParser().parse(args);
    }

    public static Parser newParser() {
        return new Parser();
    }

    public boolean isCui() {
        return cui;
    }

    public boolean isGui() {
        return gui;
    }

    public String getCommandString() {
        return commandString;
    }

    public String getConnecterName() {
        return connecterName;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public boolean isHelp() {
        return help;
    }

    /**
     * The parser for OptionSet.
     */
    public static final class Parser {

        private static final Logger log = Logger.getLogger(Parser.class);

        private static final String OPTION_CUI = "cui";
        private static final String OPTION_GUI = "gui";
        private static final String OPTION_CONNECT = "connect";
        private static final String OPTION_VERSION = "version";
        private static final String OPTION_HELP = "help";

        private final Options options;

        public Parser() {
            this.options = new Options();
            option(OPTION_CUI);
            option(OPTION_GUI);
            option(OPTION_CONNECT, "c", true);
            option(OPTION_VERSION, "v");
            option(OPTION_HELP);
        }

        public Options getOptions() {
            return options;
        }

        public OptionSet parse(String... args) throws Exception {
            OptionSet o = new OptionSet();
            CommandLineParser parser = new DefaultParser();
            CommandLine cl = parser.parse(options, args);
            o.cui = bool(cl, OPTION_CUI);
            o.gui = bool(cl, OPTION_GUI);
            o.connecterName = getConnectorName(cl);
            o.showVersion = bool(cl, OPTION_VERSION);
            o.help = bool(cl, OPTION_HELP);
            log.debug("non-option args=" + cl.getArgList());
            StringBuilder sb = new StringBuilder();
            for (final String arg : cl.getArgs())
                sb.append(' ').append(arg);
            if (sb.length() > 0) {
                sb.delete(0, 1);
            }
            o.commandString = sb.toString();
            return o;
        }

        private static String getConnectorName(CommandLine cl) {
            List<String> connectorNames = stringValues(cl, OPTION_CONNECT);
            String connectorName;
            switch (connectorNames.size()) {
                case 0:
                    connectorName = "";
                    break;
                case 1:
                    connectorName = connectorNames.get(0);
                    break;
                default:
                    throw new IllegalArgumentException("duplicate options: connect");
            }
            return connectorName;
        }

        Option option(String optionKey) {
            return option(optionKey, null, false);
        }

        Option option(String optionKey, String shortKey) {
            return option(optionKey, shortKey, false);
        }

        Option option(String optionKey, String shortKey, boolean requiresArgument) {
            String desc = res.get("opt." + optionKey);
            Option opt = new Option(shortKey, optionKey, requiresArgument, desc);
            options.addOption(opt);
            return opt;
        }

        static boolean bool(CommandLine cl, String optionKey) {
            final boolean hasOption = cl.hasOption(optionKey);
            log.debug(String.format("option: hasOption=%s, key=%s", (hasOption ? "T" : "F"), optionKey));
            return hasOption;
        }

        static List<String> stringValues(CommandLine cl, String optionKey) {
            String[] values = cl.getOptionValues(optionKey);
            String[] a = (values == null) ? new String[0] : values;
            log.debug(String.format("option: hasOption=%s, key=%s, values=%s",
                                    (cl.hasOption(optionKey) ? "T" : "F"),
                                    optionKey,
                                    Arrays.toString(a)));
            return Arrays.asList(a);
        }

    }

}
