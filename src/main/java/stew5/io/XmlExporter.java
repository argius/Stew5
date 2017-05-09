package stew5.io;

import java.io.*;
import stew5.io.StringBasedSerializer.Element;

/**
 * The Exporter for XML.
 */
public final class XmlExporter extends Exporter {

    private static final String ENCODING = "utf-8";
    private static final String TAG_TABLE = "table";
    private static final String TAG_TABLE_START = "<" + TAG_TABLE + " writer=\"" + XmlExporter.class.getName() + "\">";
    private static final String TAG_TABLE_END = "</" + TAG_TABLE + ">";
    private static final String TAG_HEADERROW = "headerrow";
    private static final String TAG_HEADERROW_END = "</" + TAG_HEADERROW + ">";
    private static final String TAG_HEADERROW_START = "<" + TAG_HEADERROW + ">";
    private static final String TAG_HEADER = "header";
    private static final String TAG_HEADER_START = "<" + TAG_HEADER;
    private static final String TAG_HEADER_END = "</" + TAG_HEADER + ">";
    private static final String TAG_ROW = "row";
    private static final String TAG_ROW_START = "<" + TAG_ROW + ">";
    private static final String TAG_ROW_END = "</" + TAG_ROW + ">";

    private PrintWriter out;

    /**
     * An constructor.
     * @param outputStream
     */
    public XmlExporter(OutputStream outputStream) {
        super(outputStream);
        try {
            this.out = new PrintWriter(new OutputStreamWriter(outputStream, ENCODING));
            out.println("<?xml version=\"1.0\" encoding=\"" + ENCODING + "\"?>");
            out.println("<!DOCTYPE " + TAG_TABLE + " SYSTEM \"stew-table.dtd\">");
            out.println(TAG_TABLE_START);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected void writeHeader(Object[] header) throws IOException {
        ensureOpen();
        out.println(TAG_HEADERROW_START);
        final String fmt = TAG_HEADER_START + " index=\"%d\">%s" + TAG_HEADER_END + "%n";
        for (int i = 0; i < header.length; i++) {
            out.printf(fmt, i, convertCData(String.valueOf(header[i])));
        }
        out.println(TAG_HEADERROW_END);
        out.flush();
    }

    @Override
    public void addRow(Object[] values) throws IOException {
        ensureOpen();
        out.print(TAG_ROW_START);
        for (int i = 0; i < values.length; i++) {
            Object o = values[i];
            Element element = StringBasedSerializer.serialize(o);
            String type = element.getType();
            if (element.isNull()) {
                out.print("<" + type + "/>");
            } else {
                out.print("<" + type);
                if (type.equals(Element.OBJECT)) {
                    out.print(" class=\"");
                    out.print(o.getClass().getName());
                    out.print("\"");
                } else if (type.equals(Element.TIME)) {
                    out.print(" display=\"");
                    out.print(o);
                    out.print("\"");
                }
                out.print(">");
                out.print(convertCData(element.getValue()));
                out.print("</" + type + ">");
            }
        }
        out.println(TAG_ROW_END);
        out.flush();
    }

    private static String convertCData(String string) {
        String s = string;
        if (s.indexOf('<') >= 0 || s.indexOf('>') >= 0) {
            if (s.contains("]]>")) {
                s = s.replaceAll("\\]\\]>", "]]&gt;");
            }
            return "<![CDATA[" + s + "]]>";
        }
        return s;
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        try {
            if (out != null) {
                out.print(TAG_TABLE_END);
                out.close();
            }
        } finally {
            out = null;
            super.close();
        }
    }

}
