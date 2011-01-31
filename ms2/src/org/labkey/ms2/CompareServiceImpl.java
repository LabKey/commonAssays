/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.query.ComparisonCrosstabView;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.client.CompareService;
import org.labkey.ms2.query.CompareProteinsView;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.NormalizedProteinProphetCrosstabView;
import org.labkey.ms2.query.ProteinProphetCrosstabView;
import org.labkey.ms2.query.PeptideCrosstabView;

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

    public GWTComparisonResult getProteinProphetComparison(String originalURL)
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);
            int runList = Integer.parseInt(url.getParameter("runList"));
            ViewContext queryContext = new ViewContext(_context);
            queryContext.setActionURL(url);

            CompareProteinsView view = new CompareProteinsView(queryContext, runList, false);
            return view.createCompareResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw UnexpectedException.wrap(e);
        }
    }

    // This method supports venn diagrams for both proteins and peptides
    public GWTComparisonResult getQueryCrosstabComparison(String originalURL, String comparisonGroup)
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);

            MS2Controller.PeptideFilteringComparisonForm form = new MS2Controller.PeptideFilteringComparisonForm();
            int listId;
            try
            {
                listId = Integer.parseInt(url.getParameter("runList"));
            }
            catch (NumberFormatException e)
            {
                throw new NotFoundException("Invalid run list id: " + url.getParameter("runList"));
            }
            form.setRunList(listId);
            form.setPeptideProphetProbability(getProbability(url, MS2Controller.PeptideFilteringFormElements.peptideProphetProbability));
            form.setPeptideFilterType(url.getParameter(MS2Controller.PeptideFilteringFormElements.peptideFilterType));

            ViewContext queryContext = new ViewContext(_context);
            queryContext.setActionURL(url);

            List<MS2Run> runs = RunListCache.getCachedRuns(form.getRunList(), false, queryContext);

            MS2Schema schema = new MS2Schema(getUser(), getContainer());
            schema.setRuns(runs);
            ComparisonCrosstabView view;
            if (comparisonGroup.equalsIgnoreCase("ProteinProphetCrosstab"))
            {
                form.setProteinProphetProbability(getProbability(url, MS2Controller.PeptideFilteringFormElements.proteinProphetProbability));
                form.setProteinGroupFilterType(url.getParameter(MS2Controller.PeptideFilteringFormElements.proteinGroupFilterType));
                form.setNormalizeProteinGroups(Boolean.parseBoolean(url.getParameter(MS2Controller.NORMALIZE_PROTEIN_GROUPS_NAME)));
                view = form.isNormalizeProteinGroups() ? new NormalizedProteinProphetCrosstabView(schema, form, url) :
                    new ProteinProphetCrosstabView(schema, form, url);
            }
            else
            {
               view = new PeptideCrosstabView(schema, form, url);
            }

            return view.createComparisonResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw UnexpectedException.wrap(e);
        }
    }


    private Float getProbability(ActionURL url, MS2Controller.PeptideFilteringFormElements type)
    {
        String probString = url.getParameter(type);
        if (probString != null && !"".equals(probString))
        {
            try
            {
                return Float.parseFloat(probString);
            }
            catch (NumberFormatException e) {} // Just ignore if illegal value
        }
        return null;
    }

    public GWTComparisonResult getPeptideComparison(String originalURL)
    {
        throw new UnsupportedOperationException();
    }
}