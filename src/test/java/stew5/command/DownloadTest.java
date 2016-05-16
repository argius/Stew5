package stew5.command;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import static stew5.command.Download.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;
import stew5.ui.console.ConsoleOutputProcessor;

public class DownloadTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Download cmd = new Download();

    @Before
    public void initEnv() {
        Environment env = new Environment();
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws SQLException {
        String tmpFolderPath = tmpFolder.getRoot().getAbsolutePath();
        try (Connection conn = connection()) {
            cmd.execute(conn, p("download " + tmpFolderPath + "/a select id, id||'.txt' from table1"));
            final String filePathA = tmpFolderPath + "/a/1.txt";
            assertTrue("file does not exists: " + filePathA, Paths.get(filePathA).toFile().exists());
            cmd.execute(conn, p("download " + tmpFolderPath + "/b select name, id||'.txt' from table1"));
            final String filePathB = tmpFolderPath + "/b/1.txt";
            assertTrue("file does not exists: " + filePathB, Paths.get(filePathB).toFile().exists());
        }
    }

    @Test
    public void testExecuteUsageException1() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, p(""));
        }
    }

    @Test
    public void testExecuteUsageException2() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, p("download"));
        }
    }

    @Test
    public void testExecuteUsageException3() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, p("download 1"));
        }
    }

    @Ignore
    @Test
    public void testExecuteIOException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<IOException> allOf(instanceOf(IOException.class),
                                                            hasProperty("message", containsString("("))));
            cmd.execute(conn, p("download : select id from table1"));
        }
    }

    @Test
    public void testExecuteSQLException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<SQLException> allOf(instanceOf(SQLException.class),
                                                             hasProperty("message", containsString("Syntax error"))));
            cmd.execute(conn, p("download 1 select id from table1 where"));
        }
    }

    @Test
    public void testGetSizeString() {
        assertEquals("0byte", getSizeString(0));
        assertEquals("1byte", getSizeString(1));
        assertEquals("2bytes", getSizeString(2));
        assertEquals("511bytes", getSizeString(511));
        assertEquals("0.5KB", getSizeString(512));
        assertEquals("0.502KB", getSizeString(514));
        assertEquals("0.515KB", getSizeString(527));
        assertEquals("0.999KB", getSizeString(1023));
        assertEquals("1KB", getSizeString(1024));
        assertEquals("1.001KB", getSizeString(1025));
        assertEquals("2KB", getSizeString(2048));
        assertEquals("10.001KB", getSizeString(10241));
        assertEquals("511.999KB", getSizeString(524287));
        assertEquals("0.5MB", getSizeString(524288));
        assertEquals("0.501MB", getSizeString(525289));
        assertEquals("0.501MB", getSizeString(525335));
        assertEquals("0.999MB", getSizeString(1047946));
        assertEquals("0.999MB", getSizeString(1047947));
        assertEquals("1MB", getSizeString(1048571));
        assertEquals("4.768MB", getSizeString(5000000));
        assertEquals("0.999GB", getSizeString(1073204953));
        assertEquals("1GB", getSizeString(1073204964));
    }

}
