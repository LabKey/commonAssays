package org.labkey.ms2;

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms2.protein.AnnotationLoader;
import org.labkey.ms2.protein.AnnotationUploadManager;

import java.io.Writer;
import java.io.IOException;
import java.util.Map;
import java.sql.SQLException;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class DisplayThreadStatusColumn extends SimpleDisplayColumn
{
    public DisplayThreadStatusColumn()
    {
        super("");
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Map rowMap = ctx.getRow();
        int curId = (Integer) rowMap.get("insertId");
        if (curId <= 0) return;
        AnnotationLoader.Status curStatus;
        try
        {
            curStatus = AnnotationUploadManager.getInstance().annotThreadStatus(curId);
        }
        catch (SQLException e)
        {
            throw (IOException)new IOException().initCause(e);
        }
        String curStatusString = curStatus.toString();
        String button1 = "";
        String button2 = "";
        if (!ctx.getViewContext().getUser().isAdministrator())
        {
            out.write(curStatusString);
            return;
        }
        switch (curStatus)
        {
            case RUNNING:
                button1 =
                        "<a href=\"annotThreadControl.view?button=kill&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Kill") + "'></a>";
                button2 =
                        "<a href=\"annotThreadControl.view?button=pause&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Pause") + "'></a>";
                break;
            case PAUSED:
                button1 =
                        "<a href=\"annotThreadControl.view?button=kill&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Kill") + "'></a>";
                button2 =
                        "<a href=\"annotThreadControl.view?button=continue&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Continue") + "'></a>";
                break;
            case INCOMPLETE:
                button1 =
                        "<a href=\"annotThreadControl.view?button=recover&id=" + curId + "\"><img border='0' align='middle' src='" + PageFlowUtil.buttonSrc("Recover") + "'></a>";
                button2 = "";
                break;
            case UNKNOWN:
            case COMPLETE:
                button1 = "";
                button2 = "";
                break;
        }
        out.write(curStatusString + "&nbsp;" + button1 + "&nbsp;" + button2);
    }
}
