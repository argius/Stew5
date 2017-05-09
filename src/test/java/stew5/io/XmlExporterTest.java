package stew5.io;

import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import org.junit.*;

public final class XmlExporterTest {

    @Test
    public void testAddRow() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XmlExporter exporter = new XmlExporter(bos)) {
            exporter.addHeader(a("name", "number"));
            exporter.addRow(a("test1", "567"));
            exporter.addRow(a("test2", "890 "));
            exporter.addRow(a("nulltest", null));
            exporter.addRow(a("objecttest", new StringBuilder("test")));
            exporter.addRow(a("timetest", new Date(5000000 * 1000L)));
        }
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>%n"
                     + "<!DOCTYPE table SYSTEM \"stew-table.dtd\">%n"
                     + "<table writer=\"stew5.io.XmlExporter\">%n"
                     + "<headerrow>%n<header index=\"0\">name</header>%n<header index=\"1\">number</header>%n</headerrow>%n"
                     + "<row><string>test1</string><string>567</string></row>%n"
                     + "<row><string>test2</string><string>890 </string></row>%n"
                     + "<row><string>nulltest</string><null/></row>%n"
                     + "<row><string>objecttest</string><object class=\"java.lang.StringBuilder\">"
                     + "ACED0005737200176A6176612E6C616E672E537472696E674275696C6465723CD5FB145A4C6ACB03000078707704000"
                     + "00004757200025B43B02666B0E25D84AC02000078700000001400740065007300740000000000000000000000000000"
                     + "00000000000000000000000000000000000078</object></row>%n"
                     + "<row><string>timetest</string><time display=\"Sat Feb 28 05:53:20 JST 1970\">5000000000</time></row>%n"
                     + "</table>",
                     bos.toString().replaceAll("\r?\n", "%n"));
    }

    @SafeVarargs
    static <T> T[] a(T... a) {
        return a;
    }

}
