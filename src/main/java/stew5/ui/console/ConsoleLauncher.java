package stew5.ui.console;

import static stew5.text.TextUtilities.*;
import java.io.IOException;
import java.util.*;
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

    /** main **/
    public static void main(String... args) {
        List<String> a = new ArrayList<>(Arrays.asList(args));
        if (a.contains("-v") || a.contains("--version")) {
            System.out.println("Stew " + App.getVersion());
            return;
        }
        Environment env = new Environment();
        try {
            env.setOutputProcessor(new ConsoleOutputProcessor());
            final String about = ResourceManager.Default.get(".about", App.getVersion());
            env.getOutputProcessor().output(about);
            if (!a.isEmpty() && !a.get(0).startsWith("-")) {
                Command.invoke(env, "connect " + a.remove(0));
            }
            if (!a.isEmpty()) {
                Command.invoke(env, join(" ", a));
                Command.invoke(env, "disconnect");
            } else {
                Launcher o = new ConsoleLauncher();
                o.launch(env);
            }
        } finally {
            env.release();
        }
    }

}
