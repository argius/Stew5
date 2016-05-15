package stew5.command;

import java.io.*;
import java.sql.*;
import stew5.*;

/**
 * The Upload command used to upload a file into specified column.
 */
public final class Upload extends Command {

    private static final Logger log = Logger.getLogger(Upload.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(2)) {
            throw new UsageException(getUsage());
        }
        final File file = resolvePath(p.at(1));
        final String sql = p.after(2);
        if (log.isDebugEnabled()) {
            log.debug("file: " + file.getAbsolutePath());
            log.debug("SQL: " + sql);
        }
        if (file.length() > Integer.MAX_VALUE) {
            throw new CommandException("file too large: " + file);
        }
        final int length = (int)file.length();
        try (InputStream is = new FileInputStream(file)) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setTimeout(stmt);
                stmt.setBinaryStream(1, is, length);
                final int updatedCount = stmt.executeUpdate();
                outputMessage("i.updated", updatedCount);
            }
        } catch (IOException | SQLException ex) {
            throw new CommandException(ex);
        }
    }

}
