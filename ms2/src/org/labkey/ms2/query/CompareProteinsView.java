package org.labkey.ms2.query;

import org.labkey.api.data.TableInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

import javax.servlet.ServletException;

/**
 * User: jeckels
 * Date: Apr 12, 2007
 */
public class CompareProteinsView extends AbstractRunCompareView
{
    public CompareProteinsView(ViewContext context, int runListIndex, boolean forExport, String peptideViewName) throws ServletException
    {
        super(context, runListIndex, forExport, MS2Schema.COMPARE_PROTEIN_PROPHET_TABLE_NAME, peptideViewName);
    }

    protected String getGroupingColumnName()
    {
        return "SeqId";
    }

    protected String getGroupHeader()
    {
        return "Protein Information";
    }

    public String getComparisonName()
    {
        return "Proteins";
    }

    protected TableInfo createTable()
    {
        return getSchema().createProteinProphetCompareTable(null, getViewContext().getRequest(), null);
    }
}
