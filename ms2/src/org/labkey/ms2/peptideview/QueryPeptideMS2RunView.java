/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.ms2.peptideview;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.Filter;
import org.labkey.api.data.NestableDataRegion;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.ShowRows;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2ExportType;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;
import org.labkey.ms2.PeptideManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.PeptidesTableInfo;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class QueryPeptideMS2RunView extends AbstractMS2RunView
{
    private PeptidesTableInfo _peptidesTable;

    public QueryPeptideMS2RunView(ViewContext viewContext, MS2Run... runs)
    {
        super(viewContext, runs);
    }

    protected QuerySettings createQuerySettings(MS2Schema schema)
    {
        QuerySettings settings = schema.getSettings(_url.getPropertyValues(), MS2Manager.getDataRegionNamePeptides());
        settings.setAllowChooseView(true);
        settings.setQueryName(createPeptideTable(schema).getPublicName());
        String columnNames = _url.getParameter("columns");
        if (columnNames != null)
        {
            QueryDefinition def = settings.getQueryDef(schema);
            CustomView view = def.getCustomView(getUser(), _viewContext.getRequest(), "columns");
            if (view == null)
            {
                view = def.createCustomView(getUser(), "columns");
            }
            StringTokenizer st = new StringTokenizer(columnNames, ", ");
            List<FieldKey> fieldKeys = new ArrayList<>();
            while (st.hasMoreTokens())
            {
                fieldKeys.add(FieldKey.fromString(st.nextToken()));
            }
            view.setColumns(fieldKeys);
            view.save(getUser(), _viewContext.getRequest());
            settings.setViewName("columns");
            ActionURL url = _url.clone();
            url.deleteParameter("columns");
            url.addParameter(MS2Manager.getDataRegionNamePeptides() + ".viewName", "columns");
            throw new RedirectException(url);
        }

        return settings;
    }

    @Override
    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        NestableQueryView queryView = createGridView(false, false);
        FieldKey desiredFK;
        if (queryView.getSelectedNestingOption() != null)
        {
            desiredFK = queryView.getSelectedNestingOption().getRowIdFieldKey();
        }
        else
        {
            desiredFK = FieldKey.fromParts("SeqId");
        }

        Pair<ColumnInfo, SQLFragment> pair = generateSubSelect(queryView, queryUrl, null, desiredFK);
        ColumnInfo desiredCol = pair.first;
        SQLFragment sql = pair.second;

        SQLFragment result;
        if (queryView.getSelectedNestingOption() != null)
        {
            result = new SQLFragment("SELECT SeqId FROM " + MS2Manager.getTableInfoProteinGroupMemberships() + " WHERE ProteinGroupId IN (");
        }
        else
        {
            result = new SQLFragment("SELECT " + desiredCol.getAlias() + " FROM (");
        }
        result.append(sql);
        result.append(") x");
        return result;
    }

    @Override
    public PeptideQueryView createGridView(boolean expanded, boolean forExport)
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());
        schema.setRuns(_runs);

        QuerySettings settings = createQuerySettings(schema);

        PeptideQueryView peptideView = new PeptideQueryView(schema, settings, expanded, forExport);

        peptideView.setTitle("Peptides and Proteins");
        return peptideView;
    }

    public AbstractMS2QueryView createGridView(SimpleFilter baseFilter)
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());
        schema.setRuns(_runs);

        QuerySettings settings = createQuerySettings(schema);
        settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

        settings.setBaseFilter(baseFilter);
        PeptideQueryView peptideView = new PeptideQueryView(schema, settings, false, false);

        peptideView.setTitle("Peptides");
        return peptideView;
    }

    public class PeptideQueryView extends AbstractMS2QueryView
    {
        private final List<DisplayColumn> _additionalDisplayColumns = new ArrayList<>();

        public PeptideQueryView(MS2Schema schema, QuerySettings settings, boolean expanded, boolean forExport)
        {
            super(schema, settings, expanded, forExport,
                new QueryNestingOption(FieldKey.fromParts("ProteinProphetData", "ProteinGroupId"), FieldKey.fromParts("ProteinProphetData", "ProteinGroupId", "RowId"), getAJAXNestedGridURL()),
                new QueryNestingOption(FieldKey.fromParts("SeqId"), FieldKey.fromParts("SeqId", "SeqId"), getAJAXNestedGridURL()));
            setShowDetailsColumn(false);
        }

        @Override
        public List<DisplayColumn> getDisplayColumns()
        {
            List<DisplayColumn> result = new ArrayList<>();

            if (_overrideColumns != null)
            {
                for (ColumnInfo colInfo : QueryService.get().getColumns(getTable(), _overrideColumns).values())
                {
                    result.add(colInfo.getRenderer());
                }
                assert result.size() == _overrideColumns.size() : "Got the wrong number of columns back, " + result.size() + " instead of " + _overrideColumns.size();
            }
            else result.addAll(super.getDisplayColumns());
            result.addAll(_additionalDisplayColumns);

            return result;
        }

        @Override
        protected DataRegion createDataRegion()
        {
            DataRegion rgn = super.createDataRegion();
            // Need to use a custom action to handle selection, since we need to scope to the current run, etc
            rgn.setSelectAllURL(getViewContext().cloneActionURL().setAction(MS2Controller.SelectAllAction.class));
            setPeptideUrls(rgn, null);

            rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
            rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it

            return rgn;
        }

        @Override
        protected Sort getBaseSort()
        {
            return PeptideManager.getPeptideBaseSort();
        }

        @Override
        public PeptidesTableInfo createTable()
        {
            return createPeptideTable((MS2Schema)getSchema());
        }

        public void addDisplayColumn(DisplayColumn column)
        {
            _additionalDisplayColumns.add(column);
        }
    }

    private PeptidesTableInfo createPeptideTable(MS2Schema schema)
    {
        if (_peptidesTable == null)
        {
            Set<MS2RunType> runTypes = new HashSet<>(_runs.length);
            for (MS2Run run : _runs)
            {
                runTypes.add(run.getRunType());
            }
            boolean highestScoreFlag = _url.getParameter("highestScore") != null;
            _peptidesTable =  new PeptidesTableInfo(schema, _url.clone(), ContainerFilter.current(schema.getContainer()), runTypes.toArray(new MS2RunType[0]), highestScoreFlag);
            // Manually apply the metadata
            _peptidesTable.overlayMetadata(_peptidesTable.getPublicName(), schema, new ArrayList<>());
        }
        return _peptidesTable;
    }

    @Override
    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
    }

    @Override
    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns)
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());
        schema.setRuns(_runs);

        QuerySettings settings;
        try
        {
            settings = createQuerySettings(schema);
        }
        catch (RedirectException e)
        {
            throw new RuntimeException(e);
        }

        // Show all of the peptides with no offsets for this nested grid
        settings.setOffset(0);
        settings.setShowRows(ShowRows.ALL);

        PeptideQueryView view = new PeptideQueryView(schema, settings, true, false);
        DataRegion region = view.createDataRegion();
        if (!(region instanceof NestableDataRegion rgn))
        {
            throw new NotFoundException("No nesting possible");
        }

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion, (BindException)null);

        int groupId;

        try
        {
            groupId = Integer.parseInt(proteinGroupingId);
        }
        catch (NumberFormatException ignored)
        {
            throw new NotFoundException("Invalid proteinGroupingId parameter: " + proteinGroupingId);
        }

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(PeptideManager.getPeptideFilter(_url, PeptideManager.EXTRA_FILTER, getUser(), getSingleRun()));
        filter.addCondition(view.getSelectedNestingOption().getRowIdFieldKey(), groupId);
        result.getRenderContext().setBaseFilter(filter);

        return result;
    }

    @Override
    protected List<MS2ExportType> getExportTypes()
    {
        return Arrays.asList(MS2ExportType.Excel, MS2ExportType.TSV, MS2ExportType.AMT, MS2ExportType.MS2Ions, MS2ExportType.Bibliospec);
    }
}
