package org.labkey.flow.controllers.executescript;

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.*;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.api.query.QueryPicker;
import org.labkey.api.query.QueryAction;

import java.util.List;
import java.util.Collections;

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

    protected DataView createDataView()
    {
        DataView ret = super.createDataView();
        DataRegion rgn = ret.getDataRegion();
        rgn.addHiddenFormField("ff_compensationMatrixOption", _form.ff_compensationMatrixOption);
        if (_form.getProtocolStep() != null)
        {
            rgn.addHiddenFormField("actionSequence", Integer.toString(_form.getProtocolStep().getDefaultActionSequence()));
        }
        if (_form.getProtocol() != null)
        {
            rgn.addHiddenFormField("scriptId", Integer.toString(_form.getProtocol().getScriptId()));
        }
        if (_form.getTargetExperiment() != null)
        {
            rgn.addHiddenFormField("ff_targetExperimentId", _form.ff_targetExperimentId);
        }
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
        super.populateButtonBar(view, bb);
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
