package org.labkey.ms2.compare;

import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.view.ViewContext;

import java.sql.ResultSet;

/**
 * User: jeckels
 * Date: Jan 22, 2008
 */
public class SpectraCountRReport extends RReport
{
    public static final String TYPE = "MS2.SpectraCount.rReport";

    public ResultSet generateResultSet(ViewContext context) throws Exception
    {
        return super.generateResultSet(context);
    }

    public ReportDescriptor createDescriptor()
    {
        ReportDescriptor result = super.createDescriptor();
        result.setReportType(TYPE);
        return result;
    }
}
