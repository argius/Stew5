package stew5.ui.console;

import java.io.IOException;
import jline.console.ConsoleReader;
import stew5.*;
import stew5.ui.*;

/**
 * The Launcher implementation of console mode.
 */
public final class ConsoleLauncher implements Launcher {

    private static Logger log = Logger.getLogger(ConsoleLauncher.class);
    private static final boolean END = false;

    @Override
    public void launch(Environment env) {
        log.info("start");
        ConsoleReader cr;
        try {
            @SuppressWarnings("resource")
            ConsoleReader cr0 = new ConsoleReader();
            cr = cr0;
        } catch (IOException e) {
            log.error(e, "(new ConsoleReader)");
            System.out.println(e.getMessage());
            return;
        }
        cr.setBellEnabled(false);
        cr.setHistoryEnabled(true);
        Prompt prompt = new Prompt(env);
        while (true) {
            cr.setPrompt(prompt.toString());
            String line;
            try {
                line = cr.readLine();
            } catch (IOException e) {
                log.warn(e);
                continue;
            }
            if (line == null) {
                break;
            }
            log.debug("input : %s", line);
            if (String.valueOf(line).trim().equals("--edit")) {
                ConnectorMapEditor.invoke();
                env.updateConnectorMap();
            } else if (Command.invoke(env, line) == END) {
                break;
            }
        }
        log.info("end");
    }

    public static void main(OptionSet opts) {
        if (opts.isShowVersion()) {
            System.out.println("Stew " + App.getVersion());
            return;
        }
        Environment env = new Environment();
        try {
            final boolean quiet = opts.isQuiet();
            ConsoleOutputProcessor op = new ConsoleOutputProcessor();
            op.setQuiet(quiet);
            env.setOutputProcessor(op);
            if (!quiet) {
                final String about = ResourceManager.Default.get(".about", App.getVersion());
                env.getOutputProcessor().output(about);
            }
            String connectorName = opts.getConnecterName();
            if (!connectorName.isEmpty()) {
                Command.invoke(env, "connect " + connectorName);
            }
            String commandString = opts.getCommandString();
            if (!commandString.isEmpty()) {
                Command.invoke(env, commandString);
                Command.invoke(env, "disconnect");
            } else {
                Launcher o = new ConsoleLauncher();
                o.launch(env);
            }
        } finally {
            env.release();
        }
    }

    /** main **/
    public static void main(String... args) {
        try {
            main(OptionSet.parseArguments(args));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}
