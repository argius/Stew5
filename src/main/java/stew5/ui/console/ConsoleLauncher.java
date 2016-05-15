package stew5.ui.console;

import static stew5.text.TextUtilities.*;
import java.util.*;
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
        OutputProcessor out = env.getOutputProcessor();
        Prompt prompt = new Prompt(env);
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        while (true) {
            out.output(prompt);
            if (!scanner.hasNextLine()) {
                break;
            }
            final String line = scanner.nextLine();
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
        List<String> a = new ArrayList<String>(Arrays.asList(args));
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
