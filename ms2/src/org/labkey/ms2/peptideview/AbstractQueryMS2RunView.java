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

import org.labkey.api.query.QueryService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.DisplayElement;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.*;
import org.labkey.api.reports.ReportService;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * User: jeckels
 * Date: Apr 27, 2007
 */
public abstract class AbstractQueryMS2RunView extends AbstractMS2RunView<AbstractQueryMS2RunView.AbstractMS2QueryView>
{
    public AbstractQueryMS2RunView(ViewContext viewContext, String columnPropertyName, MS2Run... runs)
    {
        super(viewContext, columnPropertyName, runs);
    }

    public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
    {
        createGridView(form.getExpanded(), "", "", false).exportToTSV(form, response, selectedRows, headers);
        return null;
    }

    public ModelAndView exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        AbstractMS2QueryView ms2QueryView = createGridView(form.getExpanded(), "", "", false);

        List<FieldKey> keys = new ArrayList<FieldKey>();
        keys.add(FieldKey.fromParts("Fraction", "Run", "Run"));
        keys.add(FieldKey.fromParts("Fraction", "Fraction"));
        keys.add(FieldKey.fromParts("Mass"));
        keys.add(FieldKey.fromParts("Scan"));
        keys.add(FieldKey.fromParts("RetentionTime"));
        keys.add(FieldKey.fromParts("H"));
        keys.add(FieldKey.fromParts("PeptideProphet"));
        keys.add(FieldKey.fromParts("Peptide"));
        ms2QueryView.setOverrideColumns(keys);

        return ms2QueryView.exportToTSV(form, response, selectedRows, getAMTFileHeader());
    }

    @Override
    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form) throws ServletException
    {
        AbstractMS2QueryView queryView = createGridView(false, null, null, true);
        RenderContext context = queryView.createDataView().getRenderContext();
        TableInfo tinfo = queryView.createTable();

        Sort sort = new Sort();
        SimpleFilter filter = context.buildFilter(tinfo, queryUrl, queryView.getDataRegionName(), Table.ALL_ROWS, 0, sort);

        FieldKey desiredFK;
        boolean proteinProphetNesting = queryView._selectedNestingOption instanceof ProteinProphetQueryNestingOption;
        if (proteinProphetNesting)
        {
            desiredFK = FieldKey.fromString(queryView._selectedNestingOption.getRowIdColumnName());
        }
        else
        {
            desiredFK = FieldKey.fromString("SeqId");
        }

        ColumnInfo desiredCol = QueryService.get().getColumns(tinfo, Collections.singletonList(desiredFK)).get(desiredFK);
        assert desiredCol != null : "Couldn't find column " + desiredFK;

        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        columns.add(desiredCol);

        QueryService.get().ensureRequiredColumns(tinfo, columns, filter, sort, new HashSet<String>());

        SQLFragment sql = QueryService.get().getSelectSQL(tinfo, columns, filter, sort, Table.ALL_ROWS, 0, false);

        if (proteinProphetNesting)
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

    public Map<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run) throws ServletException
    {
        AbstractMS2QueryView queryView = createGridView(false, null, null, true);
        RenderContext context = queryView.createDataView().getRenderContext();
        TableInfo tinfo = queryView.createTable();

        Sort sort = new Sort();
        return Collections.singletonMap("Filter", context.buildFilter(tinfo, queryUrl, queryView.getDataRegionName(), Table.ALL_ROWS, 0, sort));
    }

    public ModelAndView exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        createGridView(form.getExpanded(), "", "", false).exportToExcel(response, selectedRows);
        return null;
    }

    public abstract AbstractMS2QueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException;

    public abstract class AbstractMS2QueryView extends QueryView
    {
        protected QueryNestingOption _selectedNestingOption;

        protected final boolean _expanded;
        protected final boolean _allowNesting;
        protected List<Integer> _selectedRows;
        protected List<FieldKey> _overrideColumns;

        public AbstractMS2QueryView(UserSchema schema, QuerySettings settings, boolean expanded, boolean allowNesting)
        {
            super(schema, settings);
            _expanded = expanded;
            _allowNesting = allowNesting;
            _buttonBarPosition = DataRegion.ButtonBarPosition.BOTH;
            setShowExportButtons(false);

            setViewItemFilter(new ReportService.ItemFilter()
            {
                public boolean accept(String type, String label)
                {
                    return SingleMS2RunRReport.TYPE.equals(type);
                }
            });

        }
        
        public void setOverrideColumns(List<FieldKey> fieldKeys)
        {
            _overrideColumns = fieldKeys;
        }

        protected void populateButtonBar(DataView view, ButtonBar bar)
        {
            super.populateButtonBar(view, bar);
            ButtonBar bb = createButtonBar(MS2Controller.ExportAllPeptidesAction.class, MS2Controller.ExportSelectedPeptidesAction.class, "peptides", view.getDataRegion());
            for (DisplayElement element : bb.getList())
            {
                bar.add(element);
            }
        }

        public abstract TableInfo createTable();

        private void createRowIdFragment(List<String> selectedRows)
        {
            if (selectedRows != null)
            {
                _selectedRows = new ArrayList<Integer>();
                for (String selectedRow : selectedRows)
                {
                    Integer row = new Integer(selectedRow);
                    _selectedRows.add(row);
                }
            }
        }

        public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
        {
            createRowIdFragment(selectedRows);
            TSVGridWriter tsvWriter = getTsvWriter();
            if (form.isExportAsWebPage())
                tsvWriter.setExportAsWebPage(true);
            tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
            tsvWriter.setFileHeader(headers);
            tsvWriter.write(response);
            return null;
        }

        public ModelAndView exportToExcel(HttpServletResponse response, List<String> selectedRows) throws Exception
        {
            createRowIdFragment(selectedRows);
            exportToExcel(response);
            return null;
        }
    }
}
