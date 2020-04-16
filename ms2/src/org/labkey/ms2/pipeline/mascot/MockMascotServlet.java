package org.labkey.ms2.pipeline.mascot;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Mocks a minimal set of Mascot APIs to allow for rudimentary testing without a Mascot server.
 */
@MultipartConfig
public class MockMascotServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        // Respond to GET mockservlet/cgi/client.pl?version
        if (req.getPathInfo().equals("/cgi/client.pl") && req.getQueryString().equals("version"))
        {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setHeader("Server", "LabKey MockMascotServer 1.0");
            resp.getOutputStream().print("Hello");
            resp.flushBuffer();
        }
        else if (req.getPathInfo().equals("/cgi/login.pl"))
        {
            resp.getOutputStream().print("sessionID=1234");
            resp.flushBuffer();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
    {
        if (req.getPathInfo().equals("/cgi/submit.pl"))
        {
            assert req.getQueryString().equals("1+--taskID+5678+--sessionID+1234");
            assert req.getPart("FILE").getSize() == 8403;
            resp.setStatus(HttpServletResponse.SC_OK);
            ServletOutputStream os = resp.getOutputStream();
            os.print("Peptide #1: GWKEPA");
            os.print("Peptide #2: AQPPVTA");
            os.print("Finished uploading search details");
            resp.flushBuffer();
        }
    }
}
