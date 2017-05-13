package stew5.io;

import static org.junit.Assert.*;
import static stew5.TestUtils.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import org.joda.time.*;
import org.junit.*;
import org.junit.rules.*;

public final class XmlImporterTest {

    private static final String TBL = "xmlimporter1"; // table name

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testXmlImporter() throws Exception {
        final String sql = "select * from " + TBL + " order by id";
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            prepareTable(conn);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ResultSet rs = stmt.executeQuery(sql); XmlExporter exp = new XmlExporter(bos)) {
                exp.addHeader(new String[]{"1x", "2", "3y", "4", "5z"});
                while (rs.next()) {
                    List<Object> a = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        a.add(rs.getObject(i + 1));
                    }
                    exp.addRow(a.toArray());
                }
            }
            try (XmlImporter imp = new XmlImporter(new ByteArrayInputStream(bos.toByteArray()))) {
                assertEquals("[1x, 2, 3y, 4, 5z]", Arrays.deepToString(imp.readHeader()));
                String expected1 = "[1, abcde123, " + ts(1200000000000L) + ", LOB1]";
                assertEquals(expected1, Arrays.toString(imp.nextRow()));
                String expected2 = "[2, fgh456, " + ts(1300000000000L) + ", LOB2]";
                assertEquals(expected2, Arrays.toString(imp.nextRow()));
                assertEquals(0, imp.nextRow().length);
            }
        }
    }

    @Test
    public void testIsElementName() {
        XMLEventFactory ef = XMLEventFactory.newInstance();
        assertTrue(XmlImporter.isElementNameEquals(ef.createStartElement(QName.valueOf("test"), null, null), "test"));
        assertTrue(XmlImporter.isElementNameEquals(ef.createEndElement(QName.valueOf("test"), null), "test"));
        assertFalse(XmlImporter.isElementNameEquals(ef.createEndElement(QName.valueOf("test"), null), "TTT"));
    }

    @Test
    public void testXmlImporterIOException() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("XMLStreamException: java.net.MalformedURLException");
        try (XmlImporter imp = new XmlImporter(null)) {
            // skip
        }
    }

    @Test
    public void testReadHeaderThrowsIOException1() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("XMLStreamException: expects char event(4), but 1");
        final String xml = "<table><headerrow><header><header /></header></headerrow></table>";
        try (XmlImporter imp = new XmlImporter(new ByteArrayInputStream(xml.getBytes()))) {
            imp.getHeader();
        }
    }

    @Test
    public void testReadHeaderThrowsIOException2() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("XMLStreamException: expects end event(2), but 4");
        final String xml = "<table><headerrow><header>X<header /></header></headerrow></table>";
        try (XmlImporter imp = new XmlImporter(new ByteArrayInputStream(xml.getBytes()))) {
            imp.getHeader();
        }
    }

    @Test
    public void testNextRowThrowsIOException() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("XMLStreamException: ParseError at [row,col]:[1,2]");
        try (XmlImporter imp = new XmlImporter(new ByteArrayInputStream("< >".getBytes()))) {
            imp.nextRow();
        }
    }

    @Test
    public void testIsElementNameEqualsThrowsIllegalStateException1() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("unexpected event: ");
        XMLEventFactory ef = XMLEventFactory.newInstance();
        XmlImporter.isElementNameEquals(ef.createComment(""), "test");
    }

    @Test
    public void testIsElementNameEqualsThrowsIllegalStateException2() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("unexpected event: ");
        XMLEventFactory ef = XMLEventFactory.newInstance();
        XmlImporter.isElementNameEquals(ef.createComment(""), "test");
    }

    static String ts(long epochMillis) {
        DateTime dt = new DateTime(epochMillis);
        return dt.toString("yyyy-MM-dd'T'HH:mm:ssZZ");
    }

    static void prepareTable(Connection conn) throws SQLException {
        final String sql = "create table "
                           + TBL
                           + " (id bigint primary key, content varchar(256), updated_at timestamp, txt char(10),"
                           + " tags array)";
        int index;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
        try (PreparedStatement stmt = conn.prepareStatement("insert into " + TBL + " values(?, ?, ?, ?, ?)")) {
            index = 0;
            stmt.setLong(++index, 1);
            stmt.setString(++index, "abcde123");
            stmt.setTimestamp(++index, new Timestamp(1200000000000L));
            stmt.setString(++index, "LOB1");
            stmt.setNull(++index, Types.ARRAY);
            stmt.executeUpdate();
            stmt.clearParameters();
            index = 0;
            stmt.setLong(++index, 2);
            stmt.setString(++index, "fgh456");
            stmt.setTimestamp(++index, new Timestamp(1300000000000L));
            stmt.setString(++index, "LOB2");
            stmt.setNull(++index, Types.ARRAY);
            stmt.executeUpdate();
            conn.commit();
        }
    }

}
