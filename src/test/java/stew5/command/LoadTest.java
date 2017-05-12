package stew5.command;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;
import stew5.io.*;

public class LoadTest {

    static final String CMD = "load";
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Load();
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
        File f1 = tmpFolder.newFile(testName + ".sql");
        File f2 = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.setConnectionToEnv(conn, env); // for using Command.invoke
            // SQL select file
            Files.write(f1.toPath(), Arrays.asList("select id || '+' || name from table1"), UTF_8);
            cmd.execute(conn, p(CMD + " " + f1.getAbsolutePath()));
            assertThat(op.getOutputString(), Matchers.startsWith("[1+argius]"));
            // data file
            Files.write(f2.toPath(), Arrays.asList("2,Bob", "3,Chris"), UTF_8);
            cmd.execute(conn, p(CMD + " " + f2.getAbsolutePath() + " table1"));
            op.clearBuffer();
            Command.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Chris]"));
            // SQL update file
            Files.write(f1.toPath(), Arrays.asList("update table1 set name='Davis' where id=3"), UTF_8);
            cmd.execute(conn, p(CMD + " " + f1.getAbsolutePath()));
            op.clearBuffer();
            Command.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Davis]"));
            conn.rollback();
        }
    }

    @Test
    public void testInsertRecords() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        Load cmdLoad = (Load)cmd;
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.setConnectionToEnv(conn, env); // for using Command.invoke
            Files.write(f.toPath(), Arrays.asList("2,Bob", "3,Chris"), UTF_8);
            PreparedStatement stmt = conn.prepareStatement("insert into table1 values (?, ?)");
            cmdLoad.insertRecords(stmt, Importer.getImporter(f));
            op.clearBuffer();
            Command.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Chris]"));
            // in case of error
            Files.write(f.toPath(), Arrays.asList("1"), UTF_8);
            cmdLoad.insertRecords(stmt, Importer.getImporter(f));
            assertThat(op.getOutputString(), Matchers.matchesPattern(".+0.+"));
            conn.rollback();
        }
    }

    @Test
    public void testUsageException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            cmd.execute(conn, p(CMD));
        }
    }

}
