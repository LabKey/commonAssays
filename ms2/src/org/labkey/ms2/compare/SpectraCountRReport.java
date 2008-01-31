package org.labkey.ms2.compare;

import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.ms2.query.SpectraCountConfiguration;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.RunListCache;
import org.labkey.ms2.MS2Run;

import java.sql.ResultSet;
import java.util.List;

/**
 * User: jeckels
 * Date: Jan 22, 2008
 */
public class SpectraCountRReport extends RReport
{
    public static final String TYPE = "MS2.SpectraCount.rReport";

    private static final String RUN_LIST_NAME = "runList";
    private static final String SPECTRA_CONFIG_NAME = "spectraConfig";

    public enum Prop implements ReportDescriptor.ReportProperty
    {
        spectraConfig
    }


    public ResultSet generateResultSet(ViewContext context) throws Exception
    {
        String spectraConfig = context.getActionURL().getParameter(SPECTRA_CONFIG_NAME);
        QuerySettings settings = new QuerySettings(context.getActionURL(), "SpectraCount");
        settings.setAllowChooseQuery(false);
        final SpectraCountConfiguration config = SpectraCountConfiguration.findByTableName(spectraConfig);
        if (config == null)
        {
            throw new IllegalArgumentException("Could not find spectra count config: " + spectraConfig);
        }

        String viewName = context.getActionURL().getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME);
        if ("".equals(viewName))
        {
            viewName = null;
        }
        MS2Schema schema = new MS2Schema(context.getUser(), context.getContainer());

        int runList = Integer.parseInt(getRunList(context));

        settings.setQueryName(config.getTableName());

        List<MS2Run> runs = RunListCache.getCachedRuns(runList, false, context);

        schema.setRuns(runs);
        QueryView view = new SpectraCountQueryView(schema, settings, config, viewName, runList);
        return view.getResultset();
    }

    private String getRunList(ViewContext context)
    {
        return context.getActionURL().getParameter(RUN_LIST_NAME);
    }

    public String getType()
    {
        return TYPE;
    }

    public ActionURL getRunReportURL(ViewContext context)
    {
        ActionURL result = super.getRunReportURL(context);
        result.addParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME, context.getActionURL().getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME));
        result.addParameter(SPECTRA_CONFIG_NAME, context.getActionURL().getParameter(SPECTRA_CONFIG_NAME));
        result.addParameter(RUN_LIST_NAME, getRunList(context));
        return result;
    }
}
