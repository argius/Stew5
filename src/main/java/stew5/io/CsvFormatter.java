package stew5.io;

/**
 * CSV Formatter.
 */
@Deprecated // TODO Remove this, scheduled for 5.0.0-beta3
public final class CsvFormatter {

    /**
     * Format Type.
     */
    public enum FormatType {

        /**
         * Encloses by <code>=" "</code> to recognize it as String.
         */
        STRING,

        /**
         * Encloses by <code>" "</code> to escape special characters.
         * (CR, LF, comma, etc)
         */
        ESCAPE,

        /**
         * Detects type automatically.
         */
        AUTO,

        /**
         * Raw.
         */
        RAW;

        static FormatType of(String s) {
            try {
                return valueOf(s);
            } catch (IllegalArgumentException ex) {
                return RAW;
            }
        }

    }

    /** @see FormatType */
    public static final CsvFormatter RAW = new CsvFormatter(FormatType.RAW);

    /** @see FormatType */
    public static final CsvFormatter STRING = new CsvFormatter(FormatType.STRING);

    /** @see FormatType */
    public static final CsvFormatter ESCAPE = new CsvFormatter(FormatType.ESCAPE);

    /** @see FormatType */
    public static final CsvFormatter AUTO = new CsvFormatter(FormatType.AUTO);

    private final FormatType type;

    private CsvFormatter(FormatType type) {
        this.type = type;
    }

    /**
     * Formats a string.
     * @param value
     * @return
     */
    public String format(String value) {
        switch (type) {
            case STRING:
                return editAsStringValue(value);
            case ESCAPE:
                return editAsEscapeValue(value);
            case AUTO:
                return editAuto(value);
            case RAW:
            default:
                return value;
        }
    }

    private static String editAuto(String value) {
        // null or empty string -> RAW
        if (value == null || value.length() == 0) {
            return value;
        }
        // including double quotation -> STRING
        if (value.indexOf('"') >= 0) {
            return editAsEscapeValue(value);
        }
        // including CR or LF -> STRING
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            return editAsEscapeValue(value);
        }
        final String trimmed = value.trim();
        if (trimmed.length() > 0) {
            // including separator (comma) -> STRING
            if (trimmed.indexOf(',') >= 0) {
                return editAsEscapeValue(value);
            }
            final char initial = trimmed.charAt(0);
            // starting zero and is at least 32 characters -> parsing Double + STRING
            if (initial == '0' && trimmed.length() >= 2) {
                try {
                    Double.parseDouble(value);
                    return editAsStringValue(value);
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
            // is decimal number and ends with zero -> parsing Double + STRING
            if (trimmed.indexOf('.') >= 0 && trimmed.charAt(trimmed.length() - 1) == '0') {
                try {
                    Double.parseDouble(value);
                    return editAsStringValue(value);
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
            // more than the max of int -> STRING
            if ('0' <= initial && initial <= '9') {
                try {
                    if (Long.parseLong(trimmed) > Integer.MAX_VALUE) {
                        return editAsStringValue(value);
                    }
                } catch (NumberFormatException ex) {
                    return editAsStringValue(value);
                }
            }
        }
        return value;
    }

    private static String editAsStringValue(String value) {
        return String.format("=\"%s\"", escapeQuote(value));
    }

    private static String editAsEscapeValue(String value) {
        return String.format("\"%s\"", escapeQuote(value));
    }

    private static String escapeQuote(String string) {
        return string.replaceAll("\"", "\"\"");
    }

}
