/*
 * Copyright (c) 2010-2014 LabKey Corporation
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
package org.labkey.ms2.query;

import org.labkey.api.data.CrosstabTableInfo;
import org.labkey.api.data.Sort;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.gwt.client.model.GWTComparisonGroup;
import org.labkey.api.gwt.client.model.GWTComparisonMember;
import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.api.query.ComparisonCrosstabView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Run;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Sep 14, 2010
 */
public abstract class AbstractQueryCrosstabView extends ComparisonCrosstabView
{
    protected final MS2Schema _schema;
    protected final MS2Controller.PeptideFilteringComparisonForm _form;

    public AbstractQueryCrosstabView(MS2Schema schema, MS2Controller.PeptideFilteringComparisonForm form, ViewContext viewContext, MS2Schema.HiddenTableType tableType)
    {
        super(schema);
        _schema = schema;
        _form = form;

        setViewContext(viewContext);

        QuerySettings settings = schema.getSettings(viewContext.getBindPropertyValues(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setQueryName(tableType.toString());
        settings.setAllowChooseView(true);
        setSettings(settings);
        setAllowExportExternalQuery(false);

        setShowRecordSelectors(false);
    }

    protected abstract Sort getBaseSort();

    public DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setViewContext(getViewContext());
        view.getRenderContext().setBaseSort(getBaseSort());
        return view;
    }

    protected GWTComparisonResult createComparisonResult(boolean[][] hits, CrosstabTableInfo table)
    {
        List<MS2Run> runs = _schema.getRuns();
        GWTComparisonMember[] gwtMembers = new GWTComparisonMember[runs.size()];
        Map<Integer, GWTComparisonGroup> groups = new HashMap<>();
        for (int i = 0; i < runs.size(); i++)
        {
            MS2Run run = runs.get(i);
            String lsid = run.getExperimentRunLSID();
            ExpRun expRun = null;
            if (lsid != null)
            {
                expRun = ExperimentService.get().getExpRun(lsid);
            }

            GWTComparisonMember gwtMember = new GWTComparisonMember(run.getDescription(), hits[i]);
            ActionURL runURL = MS2Controller.MS2UrlsImpl.get().getShowRunUrl(getUser(), run);
            gwtMember.setUrl(runURL.toString());
            gwtMembers[i] = gwtMember;
            if (expRun != null)
            {
                for (ExpExperiment experiment : expRun.getExperiments())
                {
                    GWTComparisonGroup group = groups.get(experiment.getRowId());
                    if (group == null)
                    {
                        group = new GWTComparisonGroup();
                        group.setURL(PageFlowUtil.urlProvider(ExperimentUrls.class).getExperimentDetailsURL(experiment.getContainer(), experiment).toString());
                        group.setName(experiment.getName());
                        groups.put(experiment.getRowId(), group);
                    }
                    group.addMember(gwtMember);
                }
            }
        }
        return new GWTComparisonResult(gwtMembers, groups.values().toArray(new GWTComparisonGroup[groups.size()]), hits[0].length, "Runs");
    }
}
