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
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.Filter;
import org.labkey.api.data.NestableDataRegion;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.ShowRows;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2ExportType;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.PeptideManager;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.ProteinGroupTableInfo;
import org.springframework.validation.BindException;

import java.util.Arrays;
import java.util.List;

/**
 * User: jeckels
 * Date: Apr 11, 2007
 */
public class QueryProteinGroupMS2RunView extends AbstractMS2RunView
{
    private static final String DATA_REGION_NAME = "ProteinGroups";

    public QueryProteinGroupMS2RunView(ViewContext viewContext, MS2Run[] runs)
    {
        super(viewContext, runs);
    }

    protected QuerySettings createQuerySettings(UserSchema schema) throws RedirectException
    {
        QuerySettings settings = schema.getSettings(_url.getPropertyValues(), DATA_REGION_NAME);
        settings.setAllowChooseView(true);
        settings.setQueryName(MS2Schema.HiddenTableType.ProteinGroupsForRun.toString());

        return settings;
    }

    @Override
    public ProteinGroupQueryView createGridView(boolean expanded, boolean forExport)
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);

        QuerySettings settings = createQuerySettings(schema);

        // Show all of the member proteins with no offsets for this nested grid
        settings.setOffset(0);
        settings.setShowRows(ShowRows.ALL);

        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(schema, settings, expanded, forExport);

        peptideView.setTitle("Protein Groups");
        return peptideView;
    }

    @Override
    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        NestableQueryView queryView = createGridView(false, false);
        FieldKey desiredFK = FieldKey.fromParts("Proteins", "Protein", "SeqId");

        Pair<ColumnInfo, SQLFragment> pair = generateSubSelect(queryView, queryUrl, null, desiredFK);
        ColumnInfo desiredCol = pair.first;
        SQLFragment sql = pair.second;

        SQLFragment result = new SQLFragment("SELECT " + desiredCol.getAlias() + " FROM (");
        result.append(sql);
        result.append(") x");
        return result;
    }

    public class ProteinGroupQueryView extends AbstractMS2QueryView
    {
        public ProteinGroupQueryView(UserSchema schema, QuerySettings settings, boolean expanded, boolean forExport)
        {
            super(schema, settings, expanded, forExport, new QueryNestingOption(FieldKey.fromParts("RowId"), FieldKey.fromParts("RowId"), getAJAXNestedGridURL())
            {
                @Override
                public boolean isOuter(FieldKey fieldKey)
                {
                    return fieldKey.size() == 1;
                }
            });
        }

        @Override
        protected DataRegion createDataRegion()
        {
            DataRegion rgn = super.createDataRegion();
            // Need to use a custom action to handle selection, since we need to scope to the current run, etc
            rgn.setSelectAllURL(getViewContext().cloneActionURL().setAction(MS2Controller.SelectAllAction.class));
            rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns

            return rgn;
        }

        @Override
        protected Sort getBaseSort()
        {
            return new Sort("RowId");
        }

        @Override
        public ProteinGroupTableInfo createTable()
        {
            ProteinGroupTableInfo result = ((MS2Schema)getSchema()).createProteinGroupsForRunTable(getContainerFilter(), false);
            result.setRunFilter(Arrays.asList(_runs));
            return result;
        }
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
        ProteinGroupQueryView peptideView = new ProteinGroupQueryView(schema, settings, true, false);
        NestableDataRegion rgn = (NestableDataRegion)peptideView.createDataRegion();

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion, (BindException)null);

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(PeptideManager.getPeptideFilter(_url, PeptideManager.EXTRA_FILTER, getUser(), getSingleRun()));

        try
        {
            int groupId = Integer.parseInt(proteinGroupingId);
            filter.addCondition(peptideView.getSelectedNestingOption().getRowIdFieldKey(), groupId);
            result.getRenderContext().setBaseFilter(filter);
        }
        catch (NumberFormatException e)
        {
            throw new NotFoundException("Invalid proteinGroupingId parameter: " + proteinGroupingId);
        }

        return result;
    }

    @Override
    protected List<MS2ExportType> getExportTypes()
    {
        return Arrays.asList(MS2ExportType.Excel, MS2ExportType.TSV);
    }
}
