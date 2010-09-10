/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.ProteinGroupTableInfo;
import org.labkey.api.util.Pair;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 11, 2007
 */
public class QueryProteinGroupMS2RunView extends AbstractQueryMS2RunView
{
    private static final String DATA_REGION_NAME = "ProteinGroups";

    public QueryProteinGroupMS2RunView(ViewContext viewContext, MS2Run[] runs)
    {
        super(viewContext, "Peptides", runs);
    }

    protected QuerySettings createQuerySettings(UserSchema schema) throws RedirectException
    {
        QuerySettings settings = schema.getSettings(_url.getPropertyValues(), DATA_REGION_NAME);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(true);
        settings.setQueryName(MS2Schema.HiddenTableType.ProteinGroupsForRun.toString());

        return settings;
    }

    public ProteinGroupQueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);

        QuerySettings settings = createQuerySettings(schema);

        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(schema, settings, expanded, allowNesting);

        peptideView.setTitle("Protein Groups");
        return peptideView;
    }

    public class ProteinGroupQueryView extends AbstractMS2QueryView
    {
        public ProteinGroupQueryView(UserSchema schema, QuerySettings settings, boolean expanded, boolean allowNesting)
        {
            super(schema, settings, expanded, allowNesting);
        }

        protected DataRegion createDataRegion()
        {
            List<DisplayColumn> originalColumns = getDisplayColumns();
            ProteinGroupQueryNestingOption proteinGroupNesting = new ProteinGroupQueryNestingOption(_allowNesting);

            if (proteinGroupNesting.isNested(originalColumns))
            {
                _selectedNestingOption = proteinGroupNesting;
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

            return rgn;
        }

        public DataView createDataView()
        {
            DataRegion rgn = createDataRegion();
            GridView result = new GridView(rgn, new NestedRenderContext(_selectedNestingOption, getViewContext()));
            setupDataView(result);

            Sort customViewSort = result.getRenderContext().getBaseSort();
            Sort sort = new Sort("RowId");     // Always sort peptide lists by RowId
            if (customViewSort != null)
            {
                sort.insertSort(customViewSort);
            }
            result.getRenderContext().setBaseSort(sort);
            Filter customViewFilter = result.getRenderContext().getBaseFilter();
            SimpleFilter filter = new SimpleFilter(customViewFilter);
            filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, _runs));
            if (_selectedRows != null)
            {
                String columnName = _selectedNestingOption == null ? "RowId" : _selectedNestingOption.getRowIdColumnName();
                filter.addClause(new SimpleFilter.InClause(columnName, _selectedRows));
            }
            result.getRenderContext().setBaseFilter(filter);
            return result;
        }

        public ProteinGroupTableInfo createTable()
        {
            ProteinGroupTableInfo result = ((MS2Schema)getSchema()).createProteinGroupsForRunTable(false);
            result.setRunFilter(Arrays.asList(_runs));
            return result;
        }
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
        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(schema, settings, true, true);
        QueryPeptideDataRegion rgn = (QueryPeptideDataRegion)peptideView.createDataRegion();

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion, (BindException)null);

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, getSingleRun()));

        Integer groupId = null;

        try
        {
            groupId = Integer.parseInt(proteinGroupingId);
        }
        catch (NumberFormatException e)
        {
        }

        if (null == groupId)
            HttpView.throwNotFound("Invalid proteinGroupingId parameter");

        filter.addCondition(peptideView._selectedNestingOption.getRowIdColumnName(), groupId.intValue());
        result.getRenderContext().setBaseFilter(filter);

        return result;
    }

    protected List<String> getExportFormats()
    {
        List<String> result = new ArrayList<String>();
        result.add("Excel");
        result.add("TSV");
        return result;
    }
}
