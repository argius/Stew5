package stew5.io;

import static org.junit.Assert.*;
import static stew5.io.HtmlExporter.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.rules.*;

public final class HtmlExporterTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testHtmlExporter() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (HtmlExporter exporter = new HtmlExporter(bos, "")) {
            exporter.addHeader(new String[]{"name", "number"});
            exporter.addRow(new String[]{"aaa", "123"});
        }
        String html = bos.toString("UTF-8").replaceAll("[\r\n]", "");
        assertTrue("actual: "
                   + html,
                   html.matches("<!DOCTYPE html><html><head>"
                                + "<meta charset=\"utf-8\" /><meta name=\"generator\".+?></head>"
                                + "<body><h1>Exported  Data</h1><table>.+</table></body></html>"));
    }

    @Test
    public void testCreateStyleTag() throws IOException {
        assertEquals("<link rel=\"stylesheet\" href=\"\" />", createStyleTag("", false));
        assertEquals("<style>/* URI=dummy *//* java.nio.file.NoSuchFileException: dummy */</style>",
                     createStyleTag("dummy", true));
        File tmpFile = tmpFolder.newFile();
        Files.write(tmpFile.toPath(), Arrays.asList("body { color: black }"), StandardCharsets.UTF_8);
        assertEquals("<style>body { color: black }</style>",
                     createStyleTag(tmpFile.getAbsolutePath(), true).replaceAll("[\r\n]", ""));
    }

    @Ignore // surpress
    @Test
    public void testCreateStyleTag2() {
        String uri = "https://raw.githubusercontent.com/argius/Stew4/v4.2.0/markdown.css";
        String tag = createStyleTag(uri, true);
        String tag0 = tag.replaceAll("[\r\n]", "");
        assertTrue("actual: " + tag0, tag0.matches("<style>body \\{ .+; \\}</style>"));
        assertEquals(303, tag.length());
    }

}