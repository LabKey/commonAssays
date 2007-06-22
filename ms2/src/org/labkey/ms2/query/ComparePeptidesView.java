package org.labkey.ms2.query;

import org.labkey.api.data.*;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

import javax.servlet.ServletException;

/**
 * User: jeckels
 * Date: Apr 12, 2007
 */
public class ComparePeptidesView extends AbstractRunCompareView
{
    public ComparePeptidesView(ViewContext context, MS2Controller controller, int runListIndex, boolean forExport) throws ServletException
    {
        super(context, controller, runListIndex, forExport, MS2Schema.COMPARE_PEPTIDES_TABLE_NAME);
    }

    protected String getGroupingColumnName()
    {
        return "PeptideSequence";
    }

    protected String getGroupHeader()
    {
        return "Peptide Information";
    }

    protected String getTSVExportActionName()
    {
        return "exportQueryPeptideCompareToTSV.view";
    }

    protected String getExcelExportActionName()
    {
        return "exportQueryPeptideCompareToExcel.view";
    }

    public String getComparisonName()
    {
        return "Peptides";
    }

    protected TableInfo createTable()
    {
        return new ComparePeptideTableInfo(getSchema(), _runs, _forExport);
    }
}
