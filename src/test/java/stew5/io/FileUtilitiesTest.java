package stew5.io;

import static org.junit.Assert.*;
import static stew5.io.FileUtilities.*;
import java.io.*;
import org.junit.*;
import org.junit.rules.*;
import stew5.*;

public final class FileUtilitiesTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testResolveFileFile() {
        assertEquals(new File("/a"), resolve(new File("/"), new File("a")));
        // assertEquals(new File("/tmp/a"), resolve(new File("/usr"), new File("/tmp/a").getAbsoluteFile()));
    }

    @Test
    public void testResolveFileString() {
        assertEquals(new File("/a"), resolve(new File("/"), "a"));
    }

    @Test
    public void testResolveStringString() {
        assertEquals(new File("/a"), resolve("/", "a"));
    }

    @Test
    public void testGetExtensionFile() {
        assertEquals("txt", getExtension(new File("test.txt")));
        assertEquals("Txt", getExtension(new File("test.Txt")));
    }

    @Test
    public void testGetExtensionString() {
        assertEquals("txt", getExtension("test.txt"));
        assertEquals("Txt", getExtension("test.Txt"));
        assertEquals("", getExtension("test"));
    }

    @Test
    public void testMakeDirectory() throws IOException {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        File dir = tmpFolder.newFolder(testName);
        File subdir = new File(dir, "aa");
        makeDirectory(subdir);
        assertTrue(subdir.exists() && subdir.isDirectory());
    }

    @Test
    public void testMakeDirectoryThrowsIOException() throws IOException {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        File file = tmpFolder.newFile(testName);
        thrown.expect(IOException.class);
        makeDirectory(file);
    }

}
