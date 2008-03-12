package org.labkey.ms2.compare;

import org.labkey.api.data.DataRegion;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.RunListCache;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.SpectraCountConfiguration;

import java.sql.ResultSet;
import java.util.List;

/**
 * User: jeckels
 * Date: Jan 22, 2008
 */
public class SpectraCountRReport extends RReport
{
    public static final String TYPE = "MS2.SpectraCount.rReport";

    public enum Prop implements ReportDescriptor.ReportProperty
    {
        spectraConfig
    }


    public ResultSet generateResultSet(ViewContext context) throws Exception
    {
        return getQueryView(context).getResultset();
    }

    public HttpView renderDataView(ViewContext context) throws Exception
    {
        SpectraCountQueryView view = getQueryView(context);
        QuerySettings settings = view.getSettings();

        // need to set the view name (if any) from the report, else you will just re-render
        // this report.
        settings.setViewName(getDescriptor().getProperty(ReportDescriptor.Prop.viewName));
        settings.setAllowChooseView(false);

        view.setButtonBarPosition(DataRegion.ButtonBarPosition.NONE);
        view.setShowChangeViewPicker(false);

        return view;
    }

    private SpectraCountQueryView getQueryView(ViewContext context) throws Exception
    {
        String spectraConfig = context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.spectraConfig);
        QuerySettings settings = new QuerySettings(context.getActionURL(), "SpectraCount");
        settings.setViewName(getDescriptor().getProperty(ReportDescriptor.Prop.viewName));
        settings.setAllowChooseQuery(false);
        final SpectraCountConfiguration config = SpectraCountConfiguration.findByTableName(spectraConfig);
        if (config == null)
        {
            throw new IllegalArgumentException("Could not find spectra count config: " + spectraConfig);
        }

        MS2Schema schema = new MS2Schema(context.getUser(), context.getContainer());
        MS2Controller.SpectraCountForm form = new MS2Controller.SpectraCountForm();
        form.setRunList(new Integer(getRunList(context)));
        form.setPeptideFilterType(context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType));
        try
        {
            if (context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideProphetProbability) != null)
            {
                form.setPeptideProphetProbability(new Float(context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideProphetProbability)));
            }
        }
        catch (NumberFormatException e) {}

        settings.setQueryName(config.getTableName());

        List<MS2Run> runs = RunListCache.getCachedRuns(form.getRunList().intValue(), false, context);

        schema.setRuns(runs);
        return new SpectraCountQueryView(schema, settings, config, form);
    }

    private String getRunList(ViewContext context)
    {
        return context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.runList);
    }

    public String getType()
    {
        return TYPE;
    }

    public ActionURL getRunReportURL(ViewContext context)
    {
        ActionURL url = super.getRunReportURL(context);
        return addReportParameters(url, context);
    }

    public ActionURL getDownloadDataURL(ViewContext context)
    {
        ActionURL url = super.getDownloadDataURL(context);
        return addReportParameters(url, context);
    }

    private ActionURL addReportParameters(ActionURL url, ViewContext context)
    {
        url.addParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME, context.getActionURL().getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME));
        url.addParameter(MS2Controller.PeptideFilteringFormElements.spectraConfig, context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.spectraConfig));
        url.addParameter(MS2Controller.PeptideFilteringFormElements.peptideProphetProbability, context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideProphetProbability));
        url.addParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType, context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType));
        url.addParameter(MS2Controller.PeptideFilteringFormElements.runList, getRunList(context));

        return url;
    }
}
