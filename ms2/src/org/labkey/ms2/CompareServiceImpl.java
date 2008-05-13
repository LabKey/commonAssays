/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.ms2.client.CompareService;
import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.ms2.query.CompareProteinsView;
import org.labkey.ms2.query.ProteinProphetCrosstabView;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public class CompareServiceImpl extends BaseRemoteService implements CompareService
{
    private static Logger _log = Logger.getLogger(CompareServiceImpl.class);

    public CompareServiceImpl(ViewContext context)
    {
        super(context);
    }

    public GWTComparisonResult getProteinProphetComparison(String originalURL) throws Exception
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);
            int runList = Integer.parseInt(url.getParameter("runList"));
            String viewName = url.getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME);
            ViewContext queryContext = new ViewContext(_context);
            queryContext.setActionURL(url);

            CompareProteinsView view = new CompareProteinsView(queryContext, runList, false, viewName);
            return view.createCompareResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw e;
        }
    }

    public GWTComparisonResult getProteinProphetCrosstabComparison(String originalURL) throws Exception
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);

            MS2Controller.PeptideFilteringComparisonForm form = new MS2Controller.PeptideFilteringComparisonForm();
            form.setRunList(Integer.parseInt(url.getParameter("runList")));
            String probString = url.getParameter(MS2Controller.PeptideFilteringFormElements.peptideProphetProbability);
            form.setPeptideProphetProbability(probString == null || "".equals(probString) ? null : Float.parseFloat(probString));
            form.setPeptideFilterType(url.getParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType));

            ViewContext queryContext = new ViewContext(_context);
            queryContext.setActionURL(url);

            List<MS2Run> runs = RunListCache.getCachedRuns(form.getRunList(), false, queryContext);

            MS2Schema schema = new MS2Schema(getUser(), getContainer());
            schema.setRuns(runs);

            ProteinProphetCrosstabView view = new ProteinProphetCrosstabView(schema, form, url);

            return view.createComparisonResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw e;
        }
    }

    public GWTComparisonResult getPeptideComparison(String originalURL) throws Exception
    {
/*        ActionURL url = new ActionURL(originalURL);
        int runList = Integer.parseInt(url.getParameter("runList"));
        String viewName = url.getParameter(MS2Controller..COMPARE_PEPTIDES_PEPTIDES_FILTER + "." + QueryParam.viewName.toString());
        ViewContext queryContext = new ViewContext(_context);
        queryContext.setActionURL(url);

        ComparePeptidesView view = new ComparePeptidesView(queryContext, _controller, runList, false, viewName);
        return view.createComparisonResult();
        */
        throw new UnsupportedOperationException();
    }
}