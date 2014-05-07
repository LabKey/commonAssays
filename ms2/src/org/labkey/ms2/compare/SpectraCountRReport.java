/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

package org.labkey.ms2.compare;

import org.labkey.api.data.DataRegion;
import org.labkey.api.data.Results;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.NotFoundException;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.RunListCache;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.SpectraCountConfiguration;

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


    public Results generateResults(ViewContext context, boolean allowAsyncQuery) throws Exception
    {
        return getQueryView(context).getResults();
    }

    public HttpView renderDataView(ViewContext context) throws Exception
    {
        SpectraCountQueryView view = getQueryView(context);
        QuerySettings settings = view.getSettings();
        // need to reset the report id since we want to render the data grid, not the report
        settings.setReportId(null);

        view.setButtonBarPosition(DataRegion.ButtonBarPosition.NONE);

        return view;
    }

    private SpectraCountQueryView getQueryView(ViewContext context) throws Exception
    {
        String spectraConfig = context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.spectraConfig);
        final SpectraCountConfiguration config = SpectraCountConfiguration.findByTableName(spectraConfig);
        if (config == null)
        {
            throw new NotFoundException("Could not find spectra count config: " + spectraConfig);
        }

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

        List<MS2Run> runs = RunListCache.getCachedRuns(form.getRunList().intValue(), false, context);

        MS2Schema schema = new MS2Schema(context.getUser(), context.getContainer());
        schema.setRuns(runs);

        QuerySettings settings = schema.getSettings(context, "SpectraCount", config.getTableName());
        settings.setViewName(getDescriptor().getProperty(ReportDescriptor.Prop.viewName));

        return new SpectraCountQueryView(schema, settings, null, config, form);
    }

    private static String getRunList(ViewContext context)
    {
        return context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.runList);
    }

    public String getType()
    {
        return TYPE;
    }

    public ActionURL getRunReportURL(ViewContext context)
    {
        if (hasValidParameters(context))
        {
            return super.getRunReportURL(context);
        }
        return null;
    }

    private boolean hasValidParameters(ViewContext context)
    {
        if (getRunList(context) != null &&
                context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.spectraConfig) != null)
        {
            return context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType) != null;
        }
        return false;
    }

    public ActionURL getEditReportURL(ViewContext context)
    {
        // no editing from the manage page
        return null;
    }

/*
    public static ActionURL addReportParameters(ActionURL url, ViewContext context)
    {
        url.replaceParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME, context.getActionURL().getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME));
        url.replaceParameter(MS2Controller.PeptideFilteringFormElements.spectraConfig, context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.spectraConfig));
        url.replaceParameter(MS2Controller.PeptideFilteringFormElements.peptideProphetProbability, context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideProphetProbability));
        url.replaceParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType, context.getActionURL().getParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType));
        url.replaceParameter(MS2Controller.PeptideFilteringFormElements.runList, getRunList(context));

        return url;
    }
*/
}
