package stew5.command;

import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;

public final class FindTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Find();
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
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("create index table1index on table1 (id)");
                conn.commit();
            }
            TestUtils.setConnectionToEnv(conn, env);
            executeCommand(cmd, conn, "TABLE1");
            assertThat(op.getOutputString(), Matchers.containsString("[TABLE1, TABLE, PUBLIC, TEST]"));
            // TODO fix it
            // executeCommand(cmd, conn, "*TABLE* INDEX"));
            // assertThat(op.getOutputString(), Matchers.containsString("[TABLE1, TABLE, PUBLIC, TEST]"));
        }
    }

    @Test
    public void testIsReadOnly() {
        assertTrue(cmd.isReadOnly());
    }

    @Test
    public void testUsageException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            executeCommand(cmd, conn, "");
        }
    }

}
