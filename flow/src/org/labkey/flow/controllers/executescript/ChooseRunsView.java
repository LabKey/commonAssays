/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.controllers.executescript;

import org.labkey.api.data.*;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryPicker;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.flow.view.FlowQueryView;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

public class ChooseRunsView extends FlowQueryView
{
    ChooseRunsToAnalyzeForm _form;

    public ChooseRunsView(ChooseRunsToAnalyzeForm form) throws Exception
    {
        super(form);
        _form = form;
    }

    protected List<QueryPicker> getChangeViewPickers()
    {
        return Collections.EMPTY_LIST;
    }

    protected List<QueryPicker> getQueryPickers()
    {
        return Collections.EMPTY_LIST;
    }

    public void renderCustomizeLinks(PrintWriter out) throws Exception
    {
        return;
    }


    protected boolean canDelete()
    {
        return false;
    }

    protected DataRegion createDataRegion()
    {
        List<DisplayColumn> displayColumns = getDisplayColumns();
        DataRegion rgn = new ChooseRunsRegion(_form);
        rgn.setMaxRows(getMaxRows());
        rgn.setOffset(getOffset());
        rgn.setSelectionKey(getSelectionKey());
        rgn.setShowRecordSelectors(showRecordSelectors());
        rgn.setShowRows(getShowRows());
        rgn.setName(getDataRegionName());
        rgn.setDisplayColumns(displayColumns);
        return rgn;
    }



    protected DataView createDataView()
    {
        DataView ret = super.createDataView();
        DataRegion rgn = ret.getDataRegion();
        RenderContext ctx = ret.getRenderContext();
        Filter filter = ctx.getBaseFilter();
        if (!(filter instanceof SimpleFilter))
        {
            filter = new SimpleFilter(filter);
        }
        ctx.setBaseFilter(_form.getBaseFilter(getTable(), filter));

        return ret;
    }


    protected void populateButtonBar(DataView view, ButtonBar bb)
    {
        view.getDataRegion().setShowRecordSelectors(true);
        ActionButton btnRunAnalysis = new ActionButton("analyzeSelectedRuns.post", "Analyze selected runs");
        bb.add(btnRunAnalysis);
    }


    protected boolean verboseErrors()
    {
        return false;
    }

    
    protected ActionURL urlFor(QueryAction action)
    {
        switch (action)
        {
            case exportRowsExcel:
            case exportRowsTsv:
                return null;
        }
        return super.urlFor(action);
    }
}
