package org.labkey.ms2.peptideview;

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.DisplayElement;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.*;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.OldMS2Controller;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 27, 2007
 */
public abstract class AbstractQueryMS2RunView extends AbstractMS2RunView
{
    public AbstractQueryMS2RunView(ViewContext viewContext, String columnPropertyName, MS2Run... runs)
    {
        super(viewContext, columnPropertyName, runs);
    }

    public void exportToTSV(OldMS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
    {
        createGridView(form.getExpanded(), "", "", false).exportToTSV(form, response, selectedRows, headers);
    }

    public void exportToAMT(OldMS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        AbstractQueryMS2RunView.AbstractMS2QueryView ms2QueryView = createGridView(form.getExpanded(), "", "", false);

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

        ms2QueryView.exportToTSV(form, response, selectedRows, getAMTFileHeader());
    }

    public SQLFragment getProteins(ViewURLHelper queryUrl, MS2Run run, OldMS2Controller.ChartForm form)
    {
        throw new UnsupportedOperationException();
    }

    public HashMap<String, SimpleFilter> getFilter(ViewURLHelper queryUrl, MS2Run run)
    {
        throw new UnsupportedOperationException();
    }

    public void exportToExcel(OldMS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        createGridView(form.getExpanded(), "", "", false).exportToExcel(response, selectedRows);
    }

    public abstract AbstractMS2QueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException, SQLException;

    public abstract class AbstractMS2QueryView extends QueryView
    {
        protected QueryNestingOption _selectedNestingOption;

        protected final boolean _expanded;
        protected final boolean _allowNesting;
        protected List<Integer> _selectedRows;
        protected List<FieldKey> _overrideColumns;

        public AbstractMS2QueryView(ViewContext context, UserSchema schema, QuerySettings settings, boolean expanded, boolean allowNesting)
        {
            super(context, schema, settings);
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

        public void exportToTSV(OldMS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows, List<String> headers) throws Exception
        {
            createRowIdFragment(selectedRows);
            TSVGridWriter tsvWriter = getTsvWriter();
            if (form.isExportAsWebPage())
                tsvWriter.setExportAsWebPage(true);
            tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
            tsvWriter.setFileHeader(headers);
            tsvWriter.write(response);
        }

        public void exportToExcel(HttpServletResponse response, List<String> selectedRows) throws Exception
        {
            createRowIdFragment(selectedRows);
            exportToExcel(response);
        }
    }
}
