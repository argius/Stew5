package stew5.command;

import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;

public class TimeTest {

    private static final String CMD = "time";

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Time();
    Environment env = new Environment();
    StringBuilderOutputProcessor op = new StringBuilderOutputProcessor();

    @Before
    public void initEnv() {
        env.setOutputProcessor(op);
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws SQLException {
        try (Connection conn = connection()) {
            cmd.execute(conn, p(CMD + " select id, substr(name, 0, 2) from table1"));
            assertThat(op.getOutputString(), Matchers.matchesPattern(".+0\\.\\d{3}.+"));
            cmd.execute(conn, p(CMD + " insert into table1 values (2, 'Bob')"));
            assertThat(op.getOutputString(), Matchers.matchesPattern(".+0\\.\\d{3}.+"));
            cmd.execute(conn, p(CMD + " 2 select id, substr(name, 0, 2) from table1"));
            assertThat(op.getOutputString(),
                       Matchers.matchesPattern("(?s).+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+"));
            cmd.execute(conn, p(CMD + " 3 update table1 set name = 'test'"));
            assertThat(op.getOutputString(),
                       Matchers.matchesPattern("(?s).+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+"));
            conn.rollback();
        }
    }

    @Test
    public void testUsageException1() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, p(CMD));
        }
    }

    @Test
    public void testUsageException2() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, p(CMD + " 1"));
        }
    }

    @Test
    public void testCommandException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.any(SQLException.class));
            thrown.expectMessage("Syntax error");
            cmd.execute(conn, p(CMD + " select from"));
        }
    }

}
