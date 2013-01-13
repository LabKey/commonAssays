/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

package org.labkey.flow.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.flow.controllers.FlowController;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryService;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.CompareType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.security.User;

import java.util.Map;

public enum FlowTableType
{
    Runs("The flow 'Runs' table shows experiment runs in the three steps of analysis: read Keywords, calculate Compensation, and perform Analysis.", false),
    CompensationMatrices("The 'CompensationMatrices' table shows compensation matrices and their spill values.", false),
    FCSFiles("The 'FCSFiles' table shows FCS files and their keywords.", false),
    FCSAnalyses("The 'FCSAnalyses' table shows statistics and graphs of FCS files.", false),
    CompensationControls("The 'CompensationControls' table shows statistics and graphs of FCS files that were used to calculate a compensation matrix.", false),
    AnalysisScripts("An analysis script contains the rules for calculating the compensation matrix for a run, as well as gates to apply, statistics to calculate, and graphs to draw.", true),
    Analyses("When a flow runs are analyzed, the results are grouped in an analysis.", true),
    Statistics("The 'Statistics' table shows the names of all statistics.", true),
    Keywords("The 'Keywords' table show the names of all keywords.", true),
    Graphs("The 'Graphs' table show the names of all graphs.", true),
    ;
    final String _description;
    final boolean _hidden;
    FlowTableType(String description, boolean hidden)
    {
        _description = description;
        _hidden = hidden;
    }

    public boolean isHidden()
    {
        return _hidden;
    }

    public String getDescription()
    {
        return _description;
    }

    public ActionURL urlFor(User user, Container container, QueryAction action)
    {
        ActionURL url = QueryService.get().urlFor(user, container, action, FlowSchema.SCHEMANAME, toString());
        if (action == QueryAction.executeQuery)
            url.setAction(FlowController.QueryAction.class);
        return url;
    }

    public ActionURL urlFor(User user, Container container, SimpleFilter filter)
    {
        return urlFor(user, container, filter, null);
    }

    public ActionURL urlFor(User user, Container container, @Nullable SimpleFilter filter, @Nullable Sort sort)
    {
        ActionURL ret = urlFor(user, container, QueryAction.executeQuery);
        if (filter != null)
        {
            String strQuery = filter.toQueryString(FlowQueryView.DATAREGIONNAME_DEFAULT);
            for (Map.Entry<String, String> entry : PageFlowUtil.fromQueryString(strQuery))
            {
                ret.addParameter(entry.getKey(), entry.getValue());
            }
        }
        if (sort != null)
        {
            ret.addParameter(FlowQueryView.DATAREGIONNAME_DEFAULT + ".sort", sort.getSortParamValue());
        }
        return ret;
    }

    public ActionURL urlFor(User user, Container container, String colName, Object value)
    {
        SimpleFilter filter = new SimpleFilter(colName, value);
        return urlFor(user, container, filter);
    }

    public ActionURL urlFor(User user, Container container, String colName, CompareType compare, Object value)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(colName, value, compare);
        return urlFor(user, container, filter);
    }
}
