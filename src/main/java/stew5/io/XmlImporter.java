package stew5.io;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * The Importer for XML.
 */
public final class XmlImporter extends Importer {

    private static final Pattern PATTERN_TAG = Pattern.compile("<(/?)([A-Za-z0-9]+)([^/>]+)?(/?) *>");
    private static final Pattern PATTERN_TAG_HEADER_ATTRIBUTE = Pattern.compile("(?i)index=\"[^0-9\"]*([0-9]+)[^0-9\"]*\"");
    private static final String SLASH = "/";
    private static final String TAG_TABLE = "table";
    private static final String TAG_HEADERROW = "headerrow";
    private static final String TAG_HEADER = "header";
    private static final String TAG_ROW = "row";
    private static final String TAG_NULL = "null";

    private BufferedReader reader;
    private StringBuilder buffer;
    private char[] chars;
    private boolean hasMoreData;

    /**
     * An constructor.
     * @param is
     * @throws IOException
     */
    public XmlImporter(InputStream is) throws IOException {
        super(is);
        String enc = getEncoding();
        this.reader = new BufferedReader(new InputStreamReader(is, enc));
        this.buffer = new StringBuilder(1024);
        this.chars = new char[1024];
        this.hasMoreData = seekStartTag(TAG_TABLE);
    }

    @Override
    protected Object[] readHeader() throws IOException {
        Map<Integer, Object> map = new HashMap<Integer, Object>();
        boolean isHeaderRow = false;
        while (true) {
            Matcher m = PATTERN_TAG.matcher(buffer);
            if (m.find()) {
                String g1 = m.group(1);
                String name = m.group(2);
                String attribute = String.valueOf(m.group(3));
                String g4 = m.group(4);
                buffer.delete(0, m.end());
                if (name.equalsIgnoreCase(TAG_HEADERROW)) {
                    if (g1.equals(SLASH) || g4.equals(SLASH)) {
                        isHeaderRow = false;
                        break;
                    }
                    isHeaderRow = true;
                    continue;
                } else if (name.equalsIgnoreCase(TAG_TABLE) || name.equalsIgnoreCase(TAG_ROW)) {
                    break;
                } else if (isHeaderRow && name.equalsIgnoreCase(TAG_HEADER)) {
                    Matcher mAttr = PATTERN_TAG_HEADER_ATTRIBUTE.matcher(attribute);
                    final int index;
                    if (mAttr.find()) {
                        index = Integer.parseInt(mAttr.group(1));
                    } else {
                        index = map.size();
                    }
                    while (true) {
                        Matcher mEnd = PATTERN_TAG.matcher(buffer);
                        if (mEnd.find() && mEnd.group(1).equals(SLASH)) {
                            int iEnd = mEnd.start();
                            Object value = buffer.subSequence(0, iEnd);
                            map.put(index, parseCData(value));
                            buffer.delete(0, mEnd.end());
                            break;
                        }
                        if (readChars() <= 0) {
                            break;
                        }
                    }
                }
            } else {
                if (readChars() <= 0) {
                    break;
                }
            }
        }
        if (map.isEmpty()) {
            return new Object[0];
        }
        final int max = Collections.max(map.keySet());
        Object[] headers = new Object[max + 1];
        for (int i = 0; i <= max; i++) {
            if (map.containsKey(i)) {
                headers[i] = map.get(i);
            }
        }
        return headers;
    }

    @Override
    public Object[] nextRow() throws IOException {
        ensureOpen();
        while (hasMoreData) {
            Matcher m = PATTERN_TAG.matcher(buffer);
            if (m.find()) {
                String name = m.group(2);
                String g4 = m.group(4);
                buffer.delete(0, m.end());
                if (name.equalsIgnoreCase(TAG_ROW)) {
                    if (g4.equals(SLASH)) {
                        return new Object[0];
                    }
                    return parseRow();
                } else if (name.equalsIgnoreCase(TAG_TABLE)) {
                    hasMoreData = false;
                }
            } else {
                if (readChars() <= 0) {
                    hasMoreData = false;
                    break;
                }
            }
        }
        return new Object[0];
    }

    @Override
    public void close() throws IOException {
        try {
            if (reader != null) {
                reader.close();
            }
        } finally {
            reader = null;
            buffer = null;
            super.close();
        }
    }

    private String getEncoding() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int c; (c = is.read()) >= 0;) {
            bos.write(c);
            if (c == '>') {
                break;
            }
        }
        return parseEncoding(bos.toString("ISO8859-1"));
    }

    private static String parseEncoding(String declaration) {
        Pattern p = Pattern.compile("<\\?xml.+encoding=\"([^\"]+)\"\\?>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(declaration);
        if (m.find()) {
            return m.group(1);
        }
        return "utf-8";
    }

    private boolean seekStartTag(String tagName) throws IOException {
        while (true) {
            Matcher m = PATTERN_TAG.matcher(buffer);
            if (m.find()) {
                String g1 = m.group(1);
                if (!g1.equals(SLASH)) {
                    if (tagName.equalsIgnoreCase(m.group(2))) {
                        int position;
                        if (m.group(4).equals(SLASH)) {
                            position = m.start();
                        } else {
                            position = m.end();
                        }
                        buffer.delete(0, position);
                        return true;
                    }
                }
            }
            if (readChars() <= 0) {
                break;
            }
        }
        return false;
    }

    private Object[] parseRow() throws IOException {
        List<Object> list = new ArrayList<Object>();
        while (hasMoreData) {
            Matcher m = PATTERN_TAG.matcher(buffer);
            if (m.find()) {
                String g1 = m.group(1);
                String name = m.group(2);
                String g4 = m.group(4);
                buffer.delete(0, m.end());
                if (g1.equals(SLASH)) {
                    if (name.equals(TAG_ROW)) {
                        return list.toArray();
                    } else if (name.equals(TAG_TABLE)) {
                        break;
                    }
                } else if (name.equalsIgnoreCase(TAG_NULL)) {
                    list.add(null);
                } else if (g4.equals(SLASH)) {
                    list.add(deserialize(name, ""));
                } else {
                    while (true) {
                        Matcher mEnd = PATTERN_TAG.matcher(buffer);
                        if (mEnd.find() && mEnd.group(1).equals(SLASH)) {
                            int iEnd = mEnd.start();
                            list.add(deserialize(name, buffer.subSequence(0, iEnd).toString()));
                            buffer.delete(0, mEnd.end());
                            break;
                        }
                        if (readChars() <= 0) {
                            break;
                        }
                    }
                }
            } else {
                if (readChars() <= 0) {
                    break;
                }
            }
        }
        throw new IOException("</row> not found");
    }

    private static Object deserialize(String name, String value) throws IOException {
        return parseCData(StringBasedSerializer.deserialize(name, value));
    }

    private static Object parseCData(Object o) {
        if (o instanceof String) {
            String s = (String)o;
            if (s.matches("^<!\\[CDATA\\[.*\\]\\]>$")) {
                String value = s.substring(9, s.length() - 3);
                if (value.contains("]]&gt;")) {
                    value = value.replaceAll("\\]\\]&gt;", "]]>");
                }
                return value;
            }
        }
        return o;
    }

    private int readChars() throws IOException {
        final int length = reader.read(chars);
        if (length > 0) {
            buffer.append(chars, 0, length);
        }
        return length;
    }

}