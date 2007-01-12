package org.labkey.ms2.peptideview;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.view.ViewURLHelper;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetDataRegion extends AbstractProteinDataRegion
{
    public ProteinProphetDataRegion(ViewURLHelper urlHelper)
    {
        super("ProteinGroupId", urlHelper);
        setShadeAlternatingRows(true);
    }

    protected void renderTableRow(RenderContext ctx, Writer out, DisplayColumn[] renderers, int rowIndex) throws SQLException, IOException
    {
        super.renderTableRow(ctx, out, renderers, rowIndex);

        if (_expanded)
        {
            _groupedRS.previous();
            ResultSet nestedRS = _groupedRS.getNextResultSet();

            // Validate that the inner and outer result sets are sorted the same
            while (nestedRS.next())
            {
                if (!ctx.getRow().get("ProteinGroupId").equals(nestedRS.getInt("ProteinGroupId")))
                {
                    throw new IllegalArgumentException("ProteinGroup ids do not match for the outer and inner queries");
                }
            }
            nestedRS.beforeFirst();

            renderNestedGrid(out, ctx, nestedRS, rowIndex);
            nestedRS.close();
        }
        else
        {
            renderPlaceholderGrid(out, ctx, rowIndex);
        }
    }
}
