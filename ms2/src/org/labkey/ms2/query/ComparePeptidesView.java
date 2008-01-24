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
    public ComparePeptidesView(ViewContext context, int runListIndex, boolean forExport, String peptideViewName) throws ServletException
    {
        super(context, runListIndex, forExport, MS2Schema.COMPARE_PEPTIDES_TABLE_NAME, peptideViewName);
    }

    protected String getGroupingColumnName()
    {
        return "PeptideSequence";
    }

    protected String getGroupHeader()
    {
        return "Peptide Information";
    }

    public String getComparisonName()
    {
        return "Peptides";
    }

    protected TableInfo createTable()
    {
        return getSchema().createPeptidesCompareTable(_forExport, getViewContext().getRequest(), _peptideViewName);
    }
}
