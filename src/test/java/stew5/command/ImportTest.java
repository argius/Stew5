package stew5.command;

import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.io.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;
import stew5.io.*;

public class ImportTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Import();
    Environment env = new Environment();
    StringBuilderOutputProcessor op = new StringBuilderOutputProcessor();

    @Before
    public void initEnv() {
        env.setOutputProcessor(op);
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.setConnectionToEnv(conn, env); // for using Command.invoke
            // without header
            TestUtils.writeLines(f.toPath(), "2,Bob", "3,Chris");
            executeCommand(cmd, conn, f.getAbsolutePath() + " table1");
            op.clearBuffer();
            Command.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Chris]"));
            // with header
            TestUtils.writeLines(f.toPath(), "id", "4");
            executeCommand(cmd, conn, f.getAbsolutePath() + " table1 HEADER");
            op.clearBuffer();
            Command.invoke(env, "select id || '+' || IFNULL(name, 'null') from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Chris][4+null]"));
            conn.rollback();
        }
    }

    @Test
    public void testInsertRecords() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        Import cmdImport = (Import)cmd;
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.setConnectionToEnv(conn, env); // for using Command.invoke
            TestUtils.writeLines(f.toPath(), "2,Bob", "3,Chris");
            PreparedStatement stmt = conn.prepareStatement("insert into table1 values (?, ?)");
            cmdImport.insertRecords(stmt, Importer.getImporter(f));
            op.clearBuffer();
            Command.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Chris]"));
            // in case of error
            TestUtils.writeLines(f.toPath(), "X,Y,Z");
            stmt.clearBatch();
            stmt.clearParameters();
            cmdImport.insertRecords(stmt, Importer.getImporter(f));
            assertThat(op.getOutputString(), Matchers.matchesPattern(".+0.+"));
            conn.rollback();
        }
    }

    @Test
    public void testUsageException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            executeCommand(cmd, conn, "");
        }
    }

    @Test
    public void testSQLException1() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        Import cmdImport = (Import)cmd;
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.writeLines(f.toPath(), "1");
            PreparedStatement stmt = conn.prepareStatement("insert into table1 values (?, ?)");
            thrown.expect(SQLException.class);
            cmdImport.insertRecords(stmt, Importer.getImporter(f));
        }

    }

    @Test
    public void testSQLException2() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        Import cmdImport = (Import)cmd;
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.writeLines(f.toPath(), "X,Y");
            PreparedStatement stmt = conn.prepareStatement("insert into table1 values (?, ?)");
            thrown.expect(SQLException.class);
            cmdImport.insertRecords(stmt, Importer.getImporter(f));
        }

    }

    @Test
    public void testCommandException() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.any(SQLException.class));
            executeCommand(cmd, conn, f.getAbsolutePath() + " tableX HEADER");
        }

    }

    @Test
    public void testIllegalStateException() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        Import cmdImport = (Import)cmd;
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.writeLines(f.toPath(), "1");
            PreparedStatement stmt = conn.prepareStatement("create table importtest as select * from table1 where id=?");
            thrown.expect(IllegalStateException.class);
            cmdImport.insertRecords(stmt, Importer.getImporter(f));
            conn.rollback();
        }
    }

}
