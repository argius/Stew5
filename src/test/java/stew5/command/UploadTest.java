package stew5.command;

import static org.hamcrest.Matchers.*;
import static stew5.TestUtils.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;
import stew5.ui.console.*;

public class UploadTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Upload();

    @Before
    public void initEnv() {
        Environment env = new Environment();
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        try (Connection conn = TestUtils.connection()) {
            File f = new File(tmpFolder.getRoot(), testName + ".txt");
            Files.write(f.toPath(), Arrays.asList("uploadx"), StandardCharsets.US_ASCII);
            cmd.execute(conn, p("upload " + f + " update table1 set name = ? where id = 1"));
            // TODO fix it
            // assertEquals("uploadx", select(conn, "select name from table1 where id = 1"));
        }
    }

    @Test
    public void testExecuteUsageException1() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, new Parameter(""));
        }
    }

    @Test
    public void testExecuteUsageException2() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, new Parameter("upload"));
        }
    }

    @Test
    public void testExecuteUsageException3() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, new Parameter("upload 1"));
        }
    }

    @Ignore // this test does not pass on Unix-like OS
    @Test
    public void testExecuteIOException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<IOException> allOf(instanceOf(IOException.class),
                                                            hasProperty("message", containsString("("))));
            cmd.execute(conn, new Parameter("upload : select id from table1"));
        }
    }

    @Test
    public void testExecuteSQLException() throws IOException, SQLException {
        File f = new File(tmpFolder.getRoot(), "1.txt");
        try (FileWriter out = new FileWriter(f)) {
            out.write("uploadx");
        }
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<SQLException> allOf(instanceOf(SQLException.class),
                                                             hasProperty("message", containsString("Syntax error"))));
            cmd.execute(conn, new Parameter("upload " + f + " update table1 where"));
        }
    }

}
