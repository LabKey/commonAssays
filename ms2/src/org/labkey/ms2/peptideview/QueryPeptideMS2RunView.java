/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
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
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.PeptidesTableInfo;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * User: jeckels
 * Date: Mar 6, 2006
 */
public class QueryPeptideMS2RunView extends AbstractQueryMS2RunView
{
    private PeptidesTableInfo _peptidesTable;

    public QueryPeptideMS2RunView(ViewContext viewContext, MS2Run... runs)
    {
        super(viewContext, "Peptides", runs);
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
    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form) throws ServletException
    {
        NestableQueryView queryView = createGridView(false, null, null, true);
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

        if (queryView.getSelectedNestingOption() != null)
        {
            SQLFragment result = new SQLFragment("SELECT SeqId FROM " + MS2Manager.getTableInfoProteinGroupMemberships() + " WHERE ProteinGroupId IN (");
            result.append(sql);
            result.append(") x");
            return result;
        }
        else
        {
            SQLFragment result = new SQLFragment("SELECT " + desiredCol.getAlias() + " FROM (");
            result.append(sql);
            result.append(") x");
            return result;
        }
    }

    public PeptideQueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting)
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());
        schema.setRuns(_runs);

        QuerySettings settings = createQuerySettings(schema);

        PeptideQueryView peptideView = new PeptideQueryView(schema, settings, expanded, allowNesting);

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
        private List<DisplayColumn> _additionalDisplayColumns = new ArrayList<>();

        public PeptideQueryView(MS2Schema schema, QuerySettings settings, boolean expanded, boolean allowNesting)
        {
            super(schema, settings, expanded, allowNesting,
                    new QueryNestingOption(FieldKey.fromParts("ProteinProphetData", "ProteinGroupId"), FieldKey.fromParts("ProteinProphetData", "ProteinGroupId", "RowId"), getAJAXNestedGridURL()),
                    new QueryNestingOption(FieldKey.fromParts("SeqId"), FieldKey.fromParts("SeqId", "SeqId"), getAJAXNestedGridURL()));
            setShowDetailsColumn(false);
        }

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
            return ProteinManager.getPeptideBaseSort();
        }

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
            boolean highestScoreFlag = false;
            if(_url.getParameter("highestScore") != null)
            {
                highestScoreFlag = true;
            }
            _peptidesTable =  new PeptidesTableInfo(schema, _url.clone(), true, ContainerFilter.CURRENT, runTypes.toArray(new MS2RunType[runTypes.size()]), highestScoreFlag);
            // Manually apply the metadata
            _peptidesTable.overlayMetadata(_peptidesTable.getPublicName(), schema, new ArrayList<QueryException>());
        }
        return _peptidesTable;
    }

    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
    }

    public GridView getPeptideViewForProteinGrouping(String proteinGroupingId, String columns) throws SQLException
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
        PeptideQueryView view = new PeptideQueryView(schema, settings, true, true);
        DataRegion region = view.createDataRegion();
        if (!(region instanceof NestableDataRegion))
        {
            throw new NotFoundException("No nesting possible");
        }
        NestableDataRegion rgn = (NestableDataRegion) region;

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion, (BindException)null);

        Integer groupId = null;

        try
        {
            groupId = Integer.parseInt(proteinGroupingId);
        }
        catch (NumberFormatException e)
        {
        }

        if (null == groupId)
        {
            throw new NotFoundException("Invalid proteinGroupingId parameter");
        }

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, getUser(), getSingleRun()));
        filter.addCondition(view.getSelectedNestingOption().getRowIdFieldKey(), groupId.intValue());
        result.getRenderContext().setBaseFilter(filter);

        return result;
    }

    protected List<MS2ExportType> getExportTypes()
    {
        return Arrays.asList(MS2ExportType.Excel, MS2ExportType.TSV, MS2ExportType.AMT, MS2ExportType.MS2Ions, MS2ExportType.Bibliospec);
    }
}
