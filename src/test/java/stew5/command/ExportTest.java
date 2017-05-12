package stew5.command;

import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.io.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;
import stew5.ui.console.*;

public final class ExportTest {

    private static final String CMD = "export";
    private static final ResourceManager res = ResourceManager.getInstance(Command.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Export();
    Environment env = new Environment();

    @Before
    public void initEnv() {
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        try (Connection conn = connection()) {
            TestUtils.setConnectionToEnv(conn, env);
            String dir = tmpFolder.newFolder(testName).getAbsolutePath();
            // select
            cmd.execute(conn, p(CMD + " " + dir + "/data1.html select * from table1"));
            cmd.execute(conn, p(CMD + " " + dir + "/data2.html HEADER select * from table1"));
            // find
            cmd.execute(conn, p(CMD + " " + dir + "/tableinfo.html find table1"));
            // report
            cmd.execute(conn, p(CMD + " " + dir + "/report.html report table1"));
        }
    }

    @Test
    public void testIsReadOnly() {
        assertTrue(cmd.isReadOnly());
    }

    @Test
    public void testUsageException1() throws Exception {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            thrown.expectMessage(res.get("usage." + cmd.getClass().getSimpleName()));
            cmd.execute(conn, p(CMD));
        }
    }

    @Test
    public void testUsageException2() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        String path = getTempFilePath(testName + ".txt").getAbsolutePath();
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            thrown.expectMessage(res.get("usage." + cmd.getClass().getSimpleName()));
            cmd.execute(conn, p(CMD + " " + path + " find"));
        }
    }

    @Test
    public void testUsageException3() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        String path = getTempFilePath(testName + ".txt").getAbsolutePath();
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            thrown.expectMessage(res.get("usage." + cmd.getClass().getSimpleName()));
            cmd.execute(conn, p(CMD + " " + path + " report"));
        }
    }

    @Test
    public void testUsageException4() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        String path = getTempFilePath(testName + ".txt").getAbsolutePath();
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            thrown.expectMessage(res.get("usage." + cmd.getClass().getSimpleName()));
            cmd.execute(conn, p(CMD + " " + path + " report -"));
        }
    }

    @Test
    public void testUsageException5() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        String path = getTempFilePath(testName + ".txt").getAbsolutePath();
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            thrown.expectMessage(res.get("usage." + cmd.getClass().getSimpleName()));
            cmd.execute(conn, p(CMD + " " + path + " XXX"));
        }
    }

    @Test
    public void testCommandException() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        File f = tmpFolder.newFile(testName + ".html");
        String path = f.getAbsolutePath();
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectMessage(Matchers.containsString(path));
            cmd.execute(conn, p(CMD + " " + path + " select * from table1"));
        }
    }

    @Test
    public void testCommandExceptionSQLException() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        String path = getTempFilePath(testName + ".txt").getAbsolutePath();
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectMessage(Matchers.containsString("Syntax error"));
            cmd.execute(conn, p(CMD + " " + path + " select * frm table1")); // syntax error: frm
        }
    }

    File getTempFilePath(String name) {
        return new File(tmpFolder.getRoot(), name);
    }

}
