package stew5.io;

import java.io.*;

/**
 * The Exporter for HTML.
 */
public final class HtmlExporter extends Exporter {

    private PrintWriter out;

    /**
     * A constructor.
     * @param os
     * @param title
     */
    public HtmlExporter(OutputStream os, String title) {
        super(os);
        this.out = new PrintWriter(os);
        out.println("<html>");
        out.println("<head>");
        out.printf("<title>%s</title>%n", title);
        out.println("</head>");
        out.println("<body>");
        out.printf("<h1>%s</h1>%n", title);
        out.println("<table>");
        out.flush();
    }

    @Override
    protected void writeHeader(Object[] header) throws IOException {
        ensureOpen();
        out.println("<tr>");
        for (Object o : header) {
            out.printf("<th>%s</th>%n", o);
        }
        out.println("</tr>");
        out.flush();
    }

    @Override
    public void addRow(Object[] values) throws IOException {
        ensureOpen();
        out.println("<tr>");
        for (Object o : values) {
            out.printf("<td>%s</td>%n", o);
        }
        out.println("</tr>");
        out.flush();
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        try {
            if (out != null) {
                out.println("</table>");
                out.println("</body>");
                out.println("</html>");
                out.flush();
                out.close();
            }
        } finally {
            out = null;
            super.close();
        }
    }

}
