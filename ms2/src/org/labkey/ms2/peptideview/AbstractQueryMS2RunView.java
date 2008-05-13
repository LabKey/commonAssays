/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.DisplayElement;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.*;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

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

    public SQLFragment getProteins(ActionURL queryUrl, MS2Run run, MS2Controller.ChartForm form)
    {
        throw new UnsupportedOperationException();
    }

    public HashMap<String, SimpleFilter> getFilter(ActionURL queryUrl, MS2Run run)
    {
        throw new UnsupportedOperationException();
    }

    public ModelAndView exportToExcel(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        createGridView(form.getExpanded(), "", "", false).exportToExcel(response, selectedRows);
        return null;
    }

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
            _buttonBarPosition = DataRegion.ButtonBarPosition.BOTTOM;
            setShowExportButtons(false);
        }
        
        public void setOverrideColumns(List<FieldKey> fieldKeys)
        {
            _overrideColumns = fieldKeys;
        }

        protected void populateButtonBar(DataView view, ButtonBar bar)
        {
            super.populateButtonBar(view, bar);
            ButtonBar bb = createButtonBar("exportAllPeptides", "exportSelectedPeptides", "peptides", view.getDataRegion());
            for (DisplayElement element : bb.getList())
            {
                bar.add(element);
            }
        }

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
