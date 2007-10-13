package org.labkey.nab.query;

import org.labkey.api.data.DataColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;

import java.io.Writer;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Jul 23, 2007
 */
public class NabDataLinkDisplayColumn extends SimpleDisplayColumn
{
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Object runId = ctx.getRow().get(ExpRunTable.Column.RowId.toString());
        if (runId != null)
        {
            ViewURLHelper url = new ViewURLHelper("NabAssay", "details", ctx.getContainer()).addParameter("rowId", "" + runId);
            out.write("[<a href=\"" + url.getLocalURIString() + "\" title=\"View run details\">details</a>]");
        }
    }
}
