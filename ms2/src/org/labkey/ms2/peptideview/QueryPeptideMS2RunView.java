/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.api.view.*;
import org.labkey.api.query.*;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.*;

import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2RunType;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.PeptidesTableInfo;
import org.springframework.validation.BindException;

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

    protected QuerySettings createQuerySettings(MS2Schema schema) throws RedirectException
    {
        QuerySettings settings = schema.getSettings(_url.getPropertyValues(), MS2Manager.getDataRegionNamePeptides());
        settings.setAllowChooseQuery(false);
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
            List<FieldKey> fieldKeys = new ArrayList<FieldKey>();
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
            throw new RedirectException(url.toString());
        }

        return settings;
    }

    public PeptideQueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws RedirectException
    {
        MS2Schema schema = new MS2Schema(getUser(), getContainer());
        schema.setRuns(_runs);

        QuerySettings settings = createQuerySettings(schema);

        PeptideQueryView peptideView = new PeptideQueryView(schema, settings, expanded, allowNesting);

        peptideView.setTitle("Peptides");
        return peptideView;
    }

    public AbstractMS2QueryView createGridView(SimpleFilter baseFilter) throws RedirectException, SQLException
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

    private class PeptideQueryView extends AbstractMS2QueryView
    {
        public PeptideQueryView(MS2Schema schema, QuerySettings settings, boolean expanded, boolean allowNesting)
        {
            super(schema, settings, expanded, allowNesting);
        }

        public List<DisplayColumn> getDisplayColumns()
        {
            if (_overrideColumns != null)
            {
                List<DisplayColumn> result = new ArrayList<DisplayColumn>();
                for (ColumnInfo colInfo : QueryService.get().getColumns(getTable(), _overrideColumns).values())
                {
                    result.add(colInfo.getRenderer());
                }
                assert result.size() == _overrideColumns.size() : "Got the wrong number of columns back, " + result.size() + " instead of " + _overrideColumns.size();
                return result;
            }

            return super.getDisplayColumns();
        }

        protected DataRegion createDataRegion()
        { 
            List<DisplayColumn> originalColumns = getDisplayColumns();
            ProteinProphetQueryNestingOption proteinProphetNesting = new ProteinProphetQueryNestingOption(_allowNesting);
            StandardProteinQueryNestingOption standardProteinNesting = new StandardProteinQueryNestingOption(_allowNesting);

            if (_allowNesting)
            {
                if (proteinProphetNesting.isNested(originalColumns))
                {
                    _selectedNestingOption = proteinProphetNesting;
                }
                else if (standardProteinNesting.isNested(originalColumns))
                {
                    _selectedNestingOption = standardProteinNesting;
                }
            }

            DataRegion rgn;
            if (_selectedNestingOption != null && (_allowNesting || !_expanded))
            {
                rgn = _selectedNestingOption.createDataRegion(originalColumns, _url, getDataRegionName(), _expanded);
            }
            else
            {
                rgn = new DataRegion();
                rgn.setDisplayColumns(originalColumns);
            }
            rgn.setSettings(getSettings());

            rgn.setShowRecordSelectors(true);
            rgn.setFixedWidthColumns(true);

            setPeptideUrls(rgn, null);

            rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
            rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it

            return rgn;
        }

        public DataView createDataView()
        {
            DataRegion rgn = createDataRegion();
            GridView result = new GridView(rgn, new NestedRenderContext(_selectedNestingOption, getViewContext()));
            setupDataView(result);

            Sort customViewSort = result.getRenderContext().getBaseSort();
            Sort sort = ProteinManager.getPeptideBaseSort();
            if (customViewSort != null)
            {
                sort.insertSort(customViewSort);
            }
            result.getRenderContext().setBaseSort(sort);
            Filter customViewFilter = result.getRenderContext().getBaseFilter();
            SimpleFilter filter = new SimpleFilter(customViewFilter);
            if (_selectedRows != null)
            {
                String columnName = _selectedNestingOption == null ? "RowId" : _selectedNestingOption.getRowIdColumnName();
                filter.addClause(new SimpleFilter.InClause(columnName, _selectedRows));
            }
            filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, getUser(), _runs));
            result.getRenderContext().setBaseFilter(filter);
            return result;
        }

        public PeptidesTableInfo createTable()
        {
            return createPeptideTable((MS2Schema)getSchema());
        }
    }

    private PeptidesTableInfo createPeptideTable(MS2Schema schema)
    {
        if (_peptidesTable == null)
        {
            Set<MS2RunType> runTypes = new HashSet<MS2RunType>(_runs.length);
            for (MS2Run run : _runs)
            {
                runTypes.add(run.getRunType());
            }
            _peptidesTable =  new PeptidesTableInfo(schema, _url.clone(), true, ContainerFilter.CURRENT, runTypes.toArray(new MS2RunType[runTypes.size()]));
            // Manually apply the metadata
            _peptidesTable.overlayMetadata(_peptidesTable.getPublicName(), schema, new ArrayList<QueryException>());
        }
        return _peptidesTable;
    }

    public void addSQLSummaries(SimpleFilter peptideFilter, List<Pair<String, String>> sqlSummaries)
    {
    }

    public GridView createPeptideViewForGrouping(MS2Controller.DetailsForm form)
    {
        throw new UnsupportedOperationException();
    }

    public String[] getPeptideStringsForGrouping(MS2Controller.DetailsForm form) throws SQLException
    {
        throw new UnsupportedOperationException();
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
        if (!(region instanceof QueryPeptideDataRegion))
        {
            throw new NotFoundException("No nesting possible");
        }
        QueryPeptideDataRegion rgn = (QueryPeptideDataRegion) region;

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
        filter.addCondition(view._selectedNestingOption.getRowIdColumnName(), groupId.intValue());
        result.getRenderContext().setBaseFilter(filter);

        return result;
    }

    protected List<MS2Controller.MS2ExportType> getExportTypes()
    {
        return Arrays.asList(MS2Controller.MS2ExportType.Excel, MS2Controller.MS2ExportType.TSV, MS2Controller.MS2ExportType.AMT, MS2Controller.MS2ExportType.MS2Ions);
    }
}
