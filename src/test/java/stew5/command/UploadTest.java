package stew5.command;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.io.*;
import java.sql.*;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;
import stew5.ui.console.ConsoleOutputProcessor;

public class UploadTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Upload cmd = new Upload();

    @Before
    public void initEnv() {
        Environment env = new Environment();
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws Exception {
        try (Connection conn = TestUtils.connection()) {
            File f = new File(tmpFolder.getRoot(), "1.txt");
            try (FileWriter out = new FileWriter(f)) {
                out.write("uploadx");
            }
            cmd.execute(conn, p("upload " + f + " update table1 set name = ? where id = 1"));
            assertEquals("uploadx", select(conn, "select name from table1 where id = 1"));
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

    @Ignore
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
