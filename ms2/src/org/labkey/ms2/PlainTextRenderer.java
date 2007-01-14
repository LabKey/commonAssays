package org.labkey.ms2;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * User: adam
 * Date: May 6, 2006
 * Time: 4:35 pm
 */
public class PlainTextRenderer
{
    private static Logger _log = Logger.getLogger(PlainTextRenderer.class);

    HttpServletResponse _response;
    PrintWriter _out;

    public PlainTextRenderer(HttpServletResponse response, String filenamePrefix, String extension) throws IOException
    {
        // Flush any extraneous output (e.g., <CR><LF> from JSPs)
        response.reset();
        response.setContentType("text/plain");
        response.setHeader("Content-disposition", "attachment; filename=\"" + filenamePrefix + "_" + Math.round(Math.random() * 100000) + "." + extension + "\"");

        // Get the outputstream of the servlet (always get the outputstream AFTER you've set the content-disposition and content-type)
        _out = response.getWriter();
        _response = response;
    }


    public void close() throws IOException
    {
        // Flush the writer
        _out.flush();
        // Finally, close the writer
        _out.close();
    }
}
