package org.labkey.ms2.query;

import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.ComparisonCrosstabView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.CrosstabTableInfo;
import org.labkey.api.data.Sort;
import org.labkey.api.data.AggregateColumnInfo;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.api.gwt.client.model.GWTComparisonMember;
import org.labkey.api.gwt.client.model.GWTComparisonGroup;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * User: jeckels
 * Date: Feb 1, 2008
 */
public class ProteinProphetCrosstabView extends ComparisonCrosstabView
{
    private MS2Schema _schema = null;
    private final MS2Controller.PeptideFilteringComparisonForm _form;

    public ProteinProphetCrosstabView(MS2Schema schema, MS2Controller.PeptideFilteringComparisonForm form)
    {
        super(schema);
        _schema = schema;
        _form = form;

        QuerySettings settings = new QuerySettings(getViewContext().getActionURL(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setSchemaName(schema.getSchemaName());
        settings.setQueryName(MS2Schema.PROTEIN_PROPHET_CROSSTAB_TABLE_NAME);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(true);
        setSettings(settings);

        setShowCustomizeViewLinkInButtonBar(true);
        setShowRecordSelectors(false);
    }

    protected TableInfo createTable()
    {
        return _schema.createProteinProphetCrosstabTable(_form, getViewContext());
    }

    protected DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setViewContext(getViewContext());
        String sortString = CrosstabTableInfo.getDefaultSortString() + ",SeqId/BestName";
        view.getRenderContext().setBaseSort(new Sort(sortString));
        return view;
    }

    protected FieldKey getComparisonColumn()
    {
        return FieldKey.fromParts(AggregateColumnInfo.NAME_PREFIX + "MIN_ProteinGroupId", "Group");
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
                        group.setRowId(experiment.getRowId());
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
