package org.labkey.ms2.query;

import org.labkey.api.data.*;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.OldMS2Controller;

import javax.servlet.ServletException;

/**
 * User: jeckels
 * Date: Apr 12, 2007
 */
public class CompareProteinsView extends AbstractRunCompareView
{
    public CompareProteinsView(ViewContext context, OldMS2Controller controller, int runListIndex, boolean forExport) throws ServletException
    {
        super(context, controller, runListIndex, forExport, MS2Schema.COMPARE_PROTEIN_PROPHET_TABLE_NAME);
    }

    protected String getGroupingColumnName()
    {
        return "SeqId";
    }

    protected String getGroupHeader()
    {
        return "Protein Information";
    }

    protected String getExcelExportActionName()
    {
        return "exportQueryProteinProphetCompareToExcel.view";
    }

    public String getComparisonName()
    {
        return "Proteins";
    }

    protected String getTSVExportActionName()
    {
        return "exportQueryProteinProphetCompareToTSV.view";
    }

    protected TableInfo createTable()
    {
        return new CompareProteinProphetTableInfo(null, getSchema(), _runs, _forExport);
    }
}
