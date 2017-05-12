package stew5.command;

import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.sql.*;
import java.util.concurrent.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;
import stew5.ui.console.*;

public final class WaitTest {

    private static final String CMD = "wait";
    private static final ResourceManager res = ResourceManager.getInstance(Command.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Wait();

    @Before
    public void initEnv() {
        Environment env = new Environment();
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testAll() throws SQLException {
        try (Connection conn = connection()) {
            cmd.execute(conn, p(CMD + " 0.1"));
        }
    }

    @Ignore
    @Test
    public void testExecute() throws SQLException {
        try (Connection conn = connection()) {
            long t = System.currentTimeMillis();
            cmd.execute(conn, p(CMD + " 3.1"));
            t = System.currentTimeMillis() - t;
            assertThat(t, Matchers.greaterThan(3000L));
            assertThat(t, Matchers.lessThan(3300L));
        }
    }

    @Test
    public void testUsageException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            thrown.expectMessage(res.get("usage." + cmd.getClass().getSimpleName()));
            cmd.execute(conn, p(CMD + " X"));
        }
    }

    @Test
    public void testInterruptedException() throws SQLException {
        ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
        final Thread currThread = Thread.currentThread();
        es.schedule(new Runnable() {
            @Override
            public void run() {
                currThread.interrupt();
            }
        }, 500L, TimeUnit.MILLISECONDS);
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.any(InterruptedException.class));
            cmd.execute(conn, p(CMD + " 3"));
        }
    }

}
