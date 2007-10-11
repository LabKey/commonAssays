package org.labkey.ms1;

import org.labkey.api.view.WebPartView;
import org.labkey.api.util.PageFlowUtil;

import javax.servlet.ServletException;
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Represents a view that describes the software used to create an MS1 data file
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 10, 2007
 * Time: 2:37:58 PM
 */
public class SoftwareView extends WebPartView
{
    public SoftwareView(int fileId)
    {
        super("Software Information", null);
        _fileId = fileId;
    }

    @Override
    public void renderView(Object model, PrintWriter out) throws IOException, ServletException
    {
        try
        {
            Software[] swares = MS1Manager.get().getSoftware(_fileId);
            if(null != swares)
            {
                out.print("The following software configuration was used to produce these results:");
                for(Software sware : swares)
                    formatSoftware(sware, out);
            }
            else
            {
                out.print("Information about the software used to produce these results is not available.");
            }
        }
        catch(SQLException e)
        {
            throw new ServletException(MS1Manager.get().getAllErrors(e));
        }
    }

    protected void formatSoftware(Software sware, PrintWriter out) throws SQLException
    {
        out.print("<p><b>");
        out.print(PageFlowUtil.filter(sware.getName()));
        out.print("</b>");

        String version = sware.getVersion();
        if(null != version && version.length() > 0)
            out.print(" version " + PageFlowUtil.filter(version));

        String author = sware.getAuthor();
        if(null != author && author.length() > 0)
            out.print(" (" + PageFlowUtil.filter(author) + ")");

        SoftwareParam[] params = sware.getParameters();
        if(null != params && params.length > 0)
        {
            out.print("<ul>");
            for(SoftwareParam param : sware.getParameters())
                formatParam(param, out);
            out.print("</ul>");
        }
    }

    protected void formatParam(SoftwareParam param, PrintWriter out)
    {
        out.print("<li>");
        out.print(PageFlowUtil.filter(param.getName()));
        out.print(" = ");
        out.print(PageFlowUtil.filter(param.getValue()));
        out.print("</li>");
    }

    protected int _fileId;
}
