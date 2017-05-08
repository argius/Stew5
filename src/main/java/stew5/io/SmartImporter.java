package stew5.io;

import java.io.*;
import java.util.*;

/**
 * An implementation of Importer  smart () 
 */
public final class SmartImporter extends Importer {

    private final String separator;
    private final int separatorLength;

    private Reader reader;
    private StringBuilder buffer;
    private char[] chars;

    /**
     * An constructor.
     * @param is
     * @param separator
     */
    public SmartImporter(InputStream is, String separator) {
        super(is);
        this.separator = separator;
        this.separatorLength = separator.length();
        this.reader = new InputStreamReader(is);
        this.buffer = new StringBuilder();
        this.chars = new char[0x4000];
    }

    /**
     * An constructor.
     * @param reader
     * @param separator
     */
    public SmartImporter(Reader reader, String separator) {
        this(new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IllegalStateException("cannot use");
            }

        }, separator);
        this.reader = reader;
    }

    @Override
    public Object[] nextRow() throws IOException {
        ensureOpen();
        List<String> row = new ArrayList<>();
        while (true) {
            if (!fillBuffer(1)) {
                break;
            }
            int start;
            int offset;
            int quoteEnd;
            boolean isQuote = false;
            char initial = buffer.charAt(0);
            if (initial == '"') {
                // quote
                if (fillBuffer(2)) {
                    int fromIndex = 1;
                    while (true) {
                        int i = indexOf(initial, fromIndex, true);
                        if (i < 0) {
                            start = 0;
                            quoteEnd = buffer.length();
                            break;
                        }
                        if (fillBuffer(i + 2) && buffer.charAt(i + 1) == initial) {
                            fromIndex = i + 2;
                        } else {
                            start = 1;
                            quoteEnd = i;
                            isQuote = true;
                            break;
                        }
                    }
                } else {
                    start = 0;
                    quoteEnd = buffer.length();
                }
                offset = quoteEnd + 1;
            } else {
                // not quote
                start = 0;
                quoteEnd = -1;
                offset = 0;
            }
            int index;
            int drawLength = 0;
            boolean isRowEnd = false;
            while (true) {
                int length = buffer.length();
                int indexLS = indexOf('\r', offset, false);
                boolean hasCR = indexLS >= 0;
                if (!hasCR) {
                    indexLS = indexOf('\n', offset, false);
                }
                int indexCS = indexOf(separator, offset, false);
                if (indexLS < 0 && indexCS < 0) {
                    // not found
                    if (read() <= 0) {
                        index = length;
                        drawLength = length;
                        isRowEnd = true;
                        break;
                    }
                    offset = length;
                    continue;
                } else if (indexLS >= 0 && (indexLS < indexCS || indexCS < 0)) {
                    // end of line
                    int lssize = 1;
                    if (hasCR && length > indexLS) {
                        if (fillBuffer(indexLS + 2) && buffer.charAt(indexLS + 1) == '\n') {
                            lssize += 1;
                        }
                    }
                    index = indexLS;
                    drawLength = indexLS + lssize;
                    isRowEnd = true;
                    break;
                } else if (indexCS >= 0) {
                    // separator
                    index = indexCS;
                    drawLength = indexCS + separatorLength;
                    break;
                } else {
                    assert false;
                }
            }
            final int end = (isQuote ? quoteEnd : index);
            final CharSequence column = buffer.subSequence(0, drawLength);
            buffer.delete(0, drawLength);
            addColumn(row, column.subSequence(start, end));
            if (isRowEnd) {
                break;
            }
        }
        return row.toArray();
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        reader = null;
        buffer = null;
        chars = null;
        super.close();
    }

    int indexOf(char c, int fromIndex, boolean autoFill) throws IOException {
        return indexOf(String.valueOf(c), fromIndex, autoFill);
    }

    int indexOf(String s, int fromIndex, boolean autoFill) throws IOException {
        while (true) {
            int index = buffer.indexOf(s, fromIndex);
            if (index >= 0) {
                return index;
            }
            if (!autoFill || read() <= 0) {
                return -1;
            }
        }
    }

    private int read() throws IOException {
        final int length = reader.read(chars);
        if (length > 0) {
            buffer.append(chars, 0, length);
        }
        return length;
    }

    private boolean fillBuffer(int size) throws IOException {
        while (buffer.length() < size) {
            final int rest = size - buffer.length();
            final int length = reader.read(chars, 0, rest);
            if (length <= 0) {
                break;
            }
            buffer.append(chars, 0, length);
        }
        return buffer.length() >= size;
    }

    private static void addColumn(List<String> row, CharSequence cs) {
        StringBuilder buffer = new StringBuilder(cs.length());
        for (String s : cs.toString().split("\"\"")) {
            buffer.append('"');
            buffer.append(s);
        }
        row.add(buffer.substring(1));
    }

}
