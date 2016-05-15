package stew5.command;

import static java.sql.Types.*;
import java.io.*;
import java.sql.*;
import stew5.*;

/**
 * The Download command used to save selected data to files.
 */
public final class Download extends Command {

    private static final Logger log = Logger.getLogger(Download.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(2)) {
            throw new UsageException(getUsage());
        }
        final String root = p.at(1);
        final String sql = p.after(2);
        if (log.isDebugEnabled()) {
            log.debug("root: " + root);
            log.debug("SQL: " + sql);
        }
        try (Statement stmt = prepareStatement(conn, p.asString()); ResultSet rs = executeQuery(stmt, sql)) {
            download(rs, root);
        } catch (IOException | SQLException ex) {
            throw new CommandException(ex);
        }
    }

    private void download(ResultSet rs, String root) throws IOException, SQLException {
        final int targetColumn = 1;
        ResultSetMetaData meta = rs.getMetaData();
        final int columnCount = meta.getColumnCount();
        assert columnCount >= 1;
        final int columnType = meta.getColumnType(targetColumn);
        final boolean isBinary;
        switch (columnType) {
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
            case FLOAT:
            case REAL:
            case DOUBLE:
            case NUMERIC:
            case DECIMAL:
                // numeric to string
                isBinary = false;
                break;
            case BOOLEAN:
            case BIT:
            case DATE:
            case TIME:
            case TIMESTAMP:
                // object to string
                isBinary = false;
                break;
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
                // char to string
                isBinary = false;
                break;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BLOB:
                // binary to stream
                isBinary = true;
                break;
            case CLOB:
                // char to binary-stream
                isBinary = true;
                break;
            case OTHER:
                // ? to binary-stream (experimental)
                // (e.g.: XML)
                isBinary = true;
                break;
            case DATALINK:
            case JAVA_OBJECT:
            case DISTINCT:
            case STRUCT:
            case ARRAY:
            case REF:
            default:
                throw new CommandException(String.format("unsupported type: %d", columnType));
        }
        byte[] buffer = new byte[(isBinary) ? 0x10000 : 0];
        int count = 0;
        while (rs.next()) {
            ++count;
            StringBuilder fileName = new StringBuilder();
            for (int i = 2; i <= columnCount; i++) {
                fileName.append(rs.getString(i));
            }
            final File path = resolvePath(root);
            final File file = (columnCount == 1) ? path : new File(path, fileName.toString());
            if (file.exists()) {
                throw new IOException(getMessage("e.file-already-exists", file.getAbsolutePath()));
            }
            if (isBinary) {
                try (InputStream is = rs.getBinaryStream(targetColumn)) {
                    mkdirs(file);
                    if (is == null) {
                        if (!file.createNewFile()) {
                            throw new IOException(getMessage("e.failed-create-new-file", file.getAbsolutePath()));
                        }
                    } else {
                        try (OutputStream os = new FileOutputStream(file)) {
                            while (true) {
                                int readLength = is.read(buffer);
                                if (readLength <= 0) {
                                    break;
                                }
                                os.write(buffer, 0, readLength);
                            }
                        }
                    }
                }
            } else {
                mkdirs(file);
                try (PrintWriter out = new PrintWriter(file)) {
                    out.print(rs.getObject(targetColumn));
                }
            }
            outputMessage("i.downloaded", getSizeString(file.length()), file);
        }
        outputMessage("i.selected", count);
    }

    private void mkdirs(File file) throws IOException {
        final File dir = file.getParentFile();
        if (!dir.isDirectory()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("mkdir [%s]", dir.getAbsolutePath()));
            }
            if (dir.mkdirs()) {
                outputMessage("i.did-mkdir", dir);
            } else {
                throw new IOException(getMessage("e.failed-mkdir-filedir", file));
            }
        }
    }

    static String getSizeString(long size) {
        if (size >= 512) {
            final double convertedSize;
            final String unit;
            if (size >= 536870912) {
                convertedSize = size * 1f / 1073741824f;
                unit = "GB";
            } else if (size >= 524288) {
                convertedSize = size * 1f / 1048576f;
                unit = "MB";
            } else {
                convertedSize = size * 1f / 1024f;
                unit = "KB";
            }
            return String.format("%.3f", convertedSize).replaceFirst("\\.?0+$", "") + unit;
        }
        return String.format("%dbyte%s", size, size < 2 ? "" : "s");
    }

}
