package org.labkey.ms2.peptideview;

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.DisplayElement;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.List;
import java.util.ArrayList;
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

    public void exportToTSV(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        createGridView(form.getExpanded(), "", "", false).exportToTSV(response, selectedRows);
    }

    public void exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        exportToTSV(form, response, selectedRows);
    }

    public abstract AbstractMS2QueryView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames, boolean allowNesting) throws ServletException, SQLException;

    public abstract class AbstractMS2QueryView extends QueryView
    {
        protected QueryNestingOption _selectedNestingOption;

        protected final boolean _expanded;
        protected final boolean _allowNesting;
        protected SQLFragment _extraFragment;
        protected List<Integer> _selectedRows;

        public AbstractMS2QueryView(ViewContext context, UserSchema schema, QuerySettings settings, boolean expanded, boolean forExport)
        {
            super(context, schema, settings);
            _expanded = expanded;
            _allowNesting = forExport;
            _buttonBarPosition = DataRegion.ButtonBarPosition.BOTTOM;
        }
        
        protected void populateButtonBar(DataView view, ButtonBar bar)
        {
            super.populateButtonBar(view, bar);
            ButtonBar bb = createButtonBar("exportAllPeptides", "exportSelectedPeptides", "peptides");
            for (DisplayElement element : bb.getList())
            {
                bar.add(element);
            }
        }

        private void createRowIdFragment(List<String> selectedRows)
        {
            if (selectedRows != null)
            {
                _extraFragment = new SQLFragment();
                _selectedRows = new ArrayList<Integer>();
                _extraFragment.append("RowId IN (");
                String separator = "";
                for (String selectedRow : selectedRows)
                {
                    _extraFragment.append(separator);
                    separator = ", ";
                    Integer row = new Integer(selectedRow);
                    _extraFragment.append(row);
                    _selectedRows.add(row);
                }
                _extraFragment.append(")");
            }
            _extraFragment = null;
        }

        public void exportToTSV(HttpServletResponse response, List<String> selectedRows) throws Exception
        {
            createRowIdFragment(selectedRows);
            TSVGridWriter tsvWriter = getTsvWriter();
            tsvWriter.setColumnHeaderType(TSVGridWriter.ColumnHeaderType.caption);
            tsvWriter.write(response);
        }
    }
}
