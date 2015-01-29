/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2ExportType;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.ProteinGroupTableInfo;
import org.labkey.api.util.Pair;
import org.springframework.validation.BindException;

import java.util.List;
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
        settings.setAllowChooseView(true);
        settings.setQueryName(MS2Schema.HiddenTableType.ProteinGroupsForRun.toString());

        return settings;
    }

    public ProteinGroupQueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting)
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
            super(schema, settings, expanded, allowNesting, new QueryNestingOption(FieldKey.fromParts("RowId"), FieldKey.fromParts("RowId"), getAJAXNestedGridURL())
            {
                public boolean isOuter(FieldKey fieldKey)
                {
                    return fieldKey.getParts().size() == 1;
                }
            });
        }

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
        NestableDataRegion rgn = (NestableDataRegion)peptideView.createDataRegion();

        DataRegion nestedRegion = rgn.getNestedRegion();
        GridView result = new GridView(nestedRegion, (BindException)null);

        Filter customViewFilter = result.getRenderContext().getBaseFilter();
        SimpleFilter filter = new SimpleFilter(customViewFilter);
        filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER, getUser(), getSingleRun()));

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

    protected List<MS2ExportType> getExportTypes()
    {
        return Arrays.asList(MS2ExportType.Excel, MS2ExportType.TSV);
    }
}
