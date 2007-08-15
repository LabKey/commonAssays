package org.labkey.flow.controllers.executescript;

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.*;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.api.query.QueryPicker;
import org.labkey.api.query.QueryAction;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Collections;
import java.io.PrintWriter;

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

    protected void renderCustomizeLinks(PrintWriter out) throws Exception
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
        rgn.setShowRecordSelectors(showRecordSelectors());
        rgn.setName(getDataRegionName());
        rgn.setDisplayColumnList(displayColumns);
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

    
    protected ViewURLHelper urlFor(QueryAction action)
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
