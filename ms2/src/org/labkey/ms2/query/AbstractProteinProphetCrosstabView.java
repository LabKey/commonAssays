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
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Run;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Sep 14, 2010
 */
public abstract class AbstractProteinProphetCrosstabView extends ComparisonCrosstabView
{
    protected final MS2Schema _schema;
    protected final MS2Controller.PeptideFilteringComparisonForm _form;

    public AbstractProteinProphetCrosstabView(MS2Schema schema, MS2Controller.PeptideFilteringComparisonForm form, ActionURL url, MS2Schema.HiddenTableType tableType)
    {
        super(schema);
        _schema = schema;
        _form = form;

        getViewContext().setActionURL(url);

        QuerySettings settings = schema.getSettings(url.getPropertyValues(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setQueryName(tableType.toString());
        settings.setAllowChooseQuery(false);
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
        Map<Integer, GWTComparisonGroup> groups = new HashMap<Integer, GWTComparisonGroup>();
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
            ActionURL runURL = MS2Controller.MS2UrlsImpl.get().getShowRunUrl(run);
            gwtMember.setUrl(runURL.toString());
            gwtMembers[i] = gwtMember;
            if (expRun != null)
            {
                ExpExperiment[] experiments = expRun.getExperiments();
                for (ExpExperiment experiment : experiments)
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
