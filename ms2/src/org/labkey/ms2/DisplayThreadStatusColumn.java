/*
 * Copyright (c) 2007-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
