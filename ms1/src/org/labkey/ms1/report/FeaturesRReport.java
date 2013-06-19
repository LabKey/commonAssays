/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.ms1.report;

import org.labkey.api.protein.ProteinService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.reports.report.CustomRReport;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.query.*;
import org.labkey.ms1.view.FeaturesView;

import java.util.ArrayList;
import java.util.List;

/*
* User: Dave
* Date: Sep 5, 2008
* Time: 10:55:13 AM
*/
public class FeaturesRReport extends CustomRReport
{
    public static final String TYPE = "MS1.R.Features";
    public static final String[] PARAMS = new String[]{
            MS1Controller.ShowFeaturesForm.ParamNames.runId.name(),
            MS1Controller.SimilarSearchForm.ParamNames.featureId.name(),
            MS1Controller.SimilarSearchForm.ParamNames.mzOffset.name(),
            MS1Controller.SimilarSearchForm.ParamNames.mzSource.name(),
            MS1Controller.SimilarSearchForm.ParamNames.mzUnits.name(),
            MS1Controller.SimilarSearchForm.ParamNames.subfolders.name(),
            MS1Controller.SimilarSearchForm.ParamNames.timeOffset.name(),
            MS1Controller.SimilarSearchForm.ParamNames.timeSource.name(),
            MS1Controller.SimilarSearchForm.ParamNames.timeUnits.name(),
            ProteinService.PeptideSearchForm.ParamNames.pepSeq.name(),
            ProteinService.PeptideSearchForm.ParamNames.exact.name(),
    };

    public FeaturesRReport()
    {
        super(PARAMS, TYPE);
    }

    protected FeaturesView getQueryView(ViewContext context) throws Exception
    {
        boolean restrictContainer = true;
        boolean forSearch = false;
        ActionURL url = context.getActionURL();
        List<FeaturesFilter> filters = new ArrayList<>();

        //add filters based on what params are present on the query string
        //NOTE: this is absolutely ridiculous. This whole R report architecture needs to be integrated
        //into QueryViewAction

        //if runId param is there, it's a normal showFeatures.view
        if(null != url.getParameter(MS1Controller.ShowFeaturesForm.ParamNames.runId.name()))
            filters.add(new RunFilter(Integer.valueOf(url.getParameter(MS1Controller.ShowFeaturesForm.ParamNames.runId.name())).intValue()));

        //if mzSource is there, it's a similar search view
        if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.mzSource.name()))
        {
            forSearch = true;
            MS1Controller.SimilarSearchForm form = new MS1Controller.SimilarSearchForm();
            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.featureId.name()))
                form.setFeatureId(Integer.valueOf(url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.featureId.name())));
            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.mzSource.name()))
                form.setMzSource(Double.valueOf(url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.mzSource.name())));
            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.mzOffset.name()))
                form.setMzOffset(Double.valueOf(url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.mzOffset.name())).doubleValue());
            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.mzUnits.name()))
                form.setMzUnits(MS1Controller.SimilarSearchForm.MzOffsetUnits.valueOf(url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.mzUnits.name())));

            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.timeSource.name()))
                form.setTimeSource(Double.valueOf(url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.timeSource.name())));
            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.timeOffset.name()))
                form.setTimeOffset(Double.valueOf(url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.timeOffset.name())).doubleValue());
            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.timeUnits.name()))
                form.setTimeUnits(MS1Controller.SimilarSearchForm.TimeOffsetUnits.valueOf(url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.timeUnits.name())));

            if(null != url.getParameter(MS1Controller.SimilarSearchForm.ParamNames.subfolders))
            {
                form.setSubfolders(true);
                restrictContainer = false;
            }

            filters.add(new ContainerFeaturesFilter(context.getContainer(), form.isSubfolders(), context.getUser()));
            filters.add(new MzFilter(form.getMzSource().doubleValue(), form.getMzOffset(), form.getMzUnits()));
            if(MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt == form.getTimeUnits())
                filters.add(new RetentionTimeFilter(form.getTimeSource().doubleValue() - form.getTimeOffset(),
                        form.getTimeSource().doubleValue() + form.getTimeOffset()));
            else
                filters.add(new ScanFilter((int)form.getTimeSource().doubleValue() - (int)form.getTimeOffset(),
                        (int)form.getTimeSource().doubleValue() + (int)(form.getTimeOffset())));

        }

        //if pepSeq is there, it's a peptide search view
        if (null != url.getParameter(ProteinService.PeptideSearchForm.ParamNames.pepSeq.name()))
        {
            forSearch = true;
            MS1Controller.PeptideFilterSearchForm form = new MS1Controller.PeptideFilterSearchForm();
            form.setPepSeq(url.getParameter(ProteinService.PeptideSearchForm.ParamNames.pepSeq.name()));
            if(null != url.getParameter(ProteinService.PeptideSearchForm.ParamNames.exact.name()))
                form.setExact(true);
            if(null != url.getParameter(ProteinService.PeptideSearchForm.ParamNames.subfolders.name()))
            {
                form.setSubfolders(true);
                restrictContainer = false;
            }

            filters.add(new ContainerFeaturesFilter(context.getContainer(), form.isSubfolders(), context.getUser()));
            if (null != form.getPepSeq() && form.getPepSeq().length() > 0)
                filters.add(new PeptideFilter(form.getPepSeq(), form.isExact()));
        }

        return new FeaturesView(new MS1Schema(context.getUser(), context.getContainer(), restrictContainer), filters, forSearch);
    }

    protected boolean hasRequiredParams(ViewContext context)
    {
        //just needs to have at least one of the possible params
        for (String name : getForwardParams())
        {
            if (null != context.getActionURL().getParameter(name))
                return true;
        }
        return false;
    }
}