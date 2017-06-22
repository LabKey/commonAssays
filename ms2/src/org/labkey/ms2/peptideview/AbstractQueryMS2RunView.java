/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.DisplayElement;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.RunListException;
import org.labkey.ms2.SpectrumIterator;
import org.labkey.ms2.SpectrumRenderer;
import org.labkey.ms2.protein.ProteinManager;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Apr 27, 2007
 */
public abstract class AbstractQueryMS2RunView extends AbstractMS2RunView<NestableQueryView>
{
    public AbstractQueryMS2RunView(ViewContext viewContext, String columnPropertyName, MS2Run... runs)
    {
        super(viewContext, columnPropertyName, runs);
    }

    public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws IOException
    {
        createGridView(form.getExpanded(), "", "", false).exportToTSV(form, response, selectedRows, headers);
        return null;
    }

    public ModelAndView exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException
    {
        AbstractMS2QueryView ms2QueryView = createGridView(form.getExpanded(), "", "", false);

        List<FieldKey> keys = new ArrayList<>();
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

    public Map<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run) throws ServletException
    {
        NestableQueryView queryView = createGridView(false, null, null, true);
        RenderContext context = queryView.createDataView().getRenderContext();
        TableInfo tinfo = queryView.createTable();

        Sort sort = new Sort();
        return Collections.singletonMap("Filter", context.buildFilter(tinfo, queryUrl, queryView.getDataRegionName(), Table.ALL_ROWS, Table.NO_OFFSET, sort));
    }

    public ModelAndView exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws IOException
    {
        createGridView(form.getExpanded(), "", "", false).exportToExcel(response, selectedRows);
        return null;
    }

    @Override
    public void exportSpectra(MS2Controller.ExportForm form, ActionURL currentURL, SpectrumRenderer spectrumRenderer, List<String> exportRows) throws IOException, RunListException
    {
        List<MS2Run> runs = form.validateRuns();

        // Choose a different iterator based on whether this is a nested view that may include protein group criteria
        NestableQueryView queryView = createGridView(form);
        SQLFragment sql = generateSubSelect(queryView, currentURL, exportRows, FieldKey.fromParts("RowId")).second;
        try (SpectrumIterator iter = new QueryResultSetSpectrumIterator(runs, sql))
        {
            spectrumRenderer.render(iter);
            spectrumRenderer.close();
        }
    }

    /** Generate the SELECT SQL to get a particular FieldKey, respecting the filters and other config on the URL */
    protected Pair<ColumnInfo, SQLFragment> generateSubSelect(NestableQueryView queryView, ActionURL currentURL, @Nullable List<String> selectedIds, FieldKey desiredFK)
    {
        RenderContext context = queryView.createDataView().getRenderContext();
        TableInfo tinfo = queryView.createTable();

        Sort sort = new Sort();
        SimpleFilter filter = context.buildFilter(tinfo, currentURL, queryView.getDataRegionName(), Table.ALL_ROWS, Table.NO_OFFSET, sort);
        addSelectionFilter(selectedIds, queryView, filter);

        ColumnInfo desiredCol = QueryService.get().getColumns(tinfo, Collections.singletonList(desiredFK)).get(desiredFK);
        if (desiredCol == null)
        {
            throw new IllegalArgumentException("Couldn't find column " + desiredFK + " in table " + tinfo);
        }

        List<ColumnInfo> columns = new ArrayList<>();
        columns.add(desiredCol);

        QueryService.get().ensureRequiredColumns(tinfo, columns, filter, sort, new HashSet<>());

        SQLFragment sql = QueryService.get().getSelectSQL(tinfo, columns, filter, sort, Table.ALL_ROWS, Table.NO_OFFSET, false);
        return new Pair<>(desiredCol, sql);
    }

    /** Add a filter for any selection the user might have made. The type of selection depends on the type of view (peptides/protein groups/search engine protein) */
    private void addSelectionFilter(@Nullable List<String> exportRows, NestableQueryView queryView, SimpleFilter filter)
    {
        if (exportRows != null)
        {
            List<Integer> rowIds = parseIds(exportRows);
            FieldKey selectionFK;
            QueryNestingOption nestingOption = queryView.getSelectedNestingOption();
            if (nestingOption != null)
            {
                // We're nested, so the selection key is going to be at the protein or protein group level
                selectionFK = nestingOption.getAggregateRowIdFieldKey();
            }
            else
            {
                // No nesting, so the selection key will just be the peptide's RowId
                selectionFK = FieldKey.fromParts("RowId");
            }
            filter.addClause(new SimpleFilter.InClause(selectionFK, rowIds));
        }
    }

    /**
     * Convert from Strings to Integers
     * @throws NotFoundException if there's an unparseable value
     */
    private List<Integer> parseIds(List<String> exportRows)
    {
        List<Integer> rowIds = new ArrayList<>(exportRows.size());
        for (String exportRow : exportRows)
        {
            try
            {
               rowIds.add(Integer.parseInt(exportRow));
            }
            catch (NumberFormatException e)
            {
                throw new NotFoundException("Invalid selection: " + exportRow);
            }
        }
        return rowIds;
    }

    public abstract AbstractMS2QueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting);

    public abstract class AbstractMS2QueryView extends NestableQueryView
    {
        protected List<Integer> _selectedRows;

        public AbstractMS2QueryView(UserSchema schema, QuerySettings settings, boolean expanded, boolean allowNesting, QueryNestingOption... queryNestingOptions)
        {
            super(schema, settings, expanded, allowNesting, queryNestingOptions);

            setViewItemFilter((type, label) -> SingleMS2RunRReport.TYPE.equals(type));
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

        public ModelAndView exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws IOException
        {
            createRowIdFragment(selectedRows);
            getSettings().setMaxRows(Table.ALL_ROWS);
            TSVGridWriter tsvWriter = getTsvWriter();
            tsvWriter.setColumnHeaderType(ColumnHeaderType.Caption);
            tsvWriter.setFileHeader(headers);
            tsvWriter.write(response);
            return null;
        }

        public ModelAndView exportToExcel(HttpServletResponse response, List<String> selectedRows) throws IOException
        {
            createRowIdFragment(selectedRows);
            getSettings().setMaxRows(ExcelWriter.MAX_ROWS_EXCEL_97);
            exportToExcel(response);
            return null;
        }

        protected void createRowIdFragment(List<String> selectedRows)
        {
            if (selectedRows != null)
            {
                _selectedRows = new ArrayList<>();
                for (String selectedRow : selectedRows)
                {
                    Integer row = new Integer(selectedRow);
                    _selectedRows.add(row);
                }
            }
        }

        public DataView createDataView()
        {
            DataView result = super.createDataView();
            SimpleFilter filter = new SimpleFilter(result.getRenderContext().getBaseFilter());

            if (_selectedRows != null)
            {
                // Don't used _selectedNestingOption one because we want to export as if we're a simple flat view
                QueryNestingOption nesting = determineNestingOption();
                FieldKey column = nesting == null ? FieldKey.fromParts("RowId") : nesting.getRowIdFieldKey();
                filter.addClause(new SimpleFilter.InClause(column, _selectedRows));
            }

            filter.addAllClauses(ProteinManager.getPeptideFilter(_url, ProteinManager.EXTRA_FILTER | ProteinManager.PROTEIN_FILTER, getUser(), _runs));
            result.getRenderContext().setBaseFilter(filter);
            return result;
        }
    }
}
