package org.labkey.ms2.peptideview;

import org.labkey.api.data.*;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.api.security.User;
import org.labkey.api.view.*;
import org.labkey.api.query.*;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;

import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.MS2Schema;
import org.apache.commons.lang.StringUtils;

/**
 * User: jeckels
 * Date: Mar 6, 2006
 */
public class QueryPeptideView extends AbstractPeptideView
{
    private ViewContext _viewContext;
    private static final String DATA_REGION_NAME = "MS2Peptides";

    public QueryPeptideView(Container container, User user, ViewURLHelper url, MS2Run[] runs, ViewContext viewContext)
    {
        super(container, user, "Peptides", url, runs);
        _viewContext = viewContext;
    }

    protected QuerySettings createQuerySettings(String tableName, UserSchema schema, int maxRows) throws RedirectException
    {
        QuerySettings settings = new QuerySettings(_url, DATA_REGION_NAME);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(true);
        settings.setQueryName("FlatPeptides");
        settings.setDataRegionName(MS2Manager.getDataRegionNamePeptides());
        settings.setMaxRows(maxRows);
        String columnNames = _url.getParameter("columns");
        if (columnNames != null)
        {
            QueryDefinition def = settings.getQueryDef(schema);
            CustomView view = def.getCustomView(getUser(), "columns");
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
            view.save(getUser());
            settings.setViewName("columns");
            ViewURLHelper url = _url.clone();
            url.deleteParameter("columns");
            url.addParameter(DATA_REGION_NAME + ".viewName", "columns");
            throw new RedirectException(url.toString());
        }

        return settings;
    }

    public WebPartView createGridView(boolean expanded, String requestedPeptideColumnNames, String requestedProteinColumnNames) throws ServletException, SQLException
    {
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), MS2Schema.SCHEMA_NAME);

        QuerySettings settings = createQuerySettings(DATA_REGION_NAME, schema, MAX_PEPTIDE_DISPLAY_ROWS);

        FlatPeptideQueryView peptideView = new FlatPeptideQueryView(_viewContext, schema, settings, expanded);
        FilteredTable table = peptideView.getFilteredTable();
        List<Object> params = new ArrayList<Object>(_runs.length);
        for (MS2Run run : _runs)
        {
            params.add(run.getRun());
        }

        table.addCondition(new SQLFragment(MS2Manager.getTableInfoPeptidesData() + ".Fraction IN " +
            "(SELECT Fraction FROM " + MS2Manager.getTableInfoFractions() + " WHERE Run IN (?" + StringUtils.repeat(", ?", params.size() - 1) + "))", params));

        peptideView.setTitle("Peptides");
        return peptideView;
    }

    private class NestingOption
    {
        private String _prefix;
        private String _rowIdColumnName;
        private DataColumn _groupIdColumn;
        
        public NestingOption(String prefix, String rowIdColumnName)
        {
            _prefix = prefix;
            _rowIdColumnName = rowIdColumnName;
        }

        public DataColumn getGroupIdColumn()
        {
            return _groupIdColumn;
        }

        public void setupGroupIdColumn(List<DisplayColumn> allColumns, List<DisplayColumn> groupingColumns, TableInfo parentTable)
        {
            if (_groupIdColumn != null)
            {
                return;
            }
            Map<FieldKey, ColumnInfo> infos = QueryService.get().getColumns(parentTable, Collections.singleton(FieldKey.fromString(_rowIdColumnName)));
            for (ColumnInfo info : infos.values())
            {
                _groupIdColumn = new DataColumn(info);
                _groupIdColumn.setVisible(false);
                allColumns.add(_groupIdColumn);
                groupingColumns.add(_groupIdColumn);
            }
        }
        
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NestingOption that = (NestingOption) o;

            if (_prefix != null ? !_prefix.equals(that._prefix) : that._prefix != null) return false;
            if (_rowIdColumnName != null ? !_rowIdColumnName.equals(that._rowIdColumnName) : that._rowIdColumnName != null)
                return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = (_prefix != null ? _prefix.hashCode() : 0);
            result = 31 * result + (_rowIdColumnName != null ? _rowIdColumnName.hashCode() : 0);
            return result;
        }

        public boolean isNested(String name)
        {
            return name.toLowerCase().startsWith(_prefix.toLowerCase());
        }
    }

    private class FlatPeptideQueryView extends QueryView
    {
        private NestingOption _selectedNestingOption;
        private static final String PROTEIN_GROUP_PREFIX = "ProteinProphetData/ProteinGroupId/";
        private static final String PROTEIN_GROUP_ROWID = PROTEIN_GROUP_PREFIX + "RowId";

        private static final String PROTEIN_PREFIX = "SeqId/";
        private static final String PROTEIN_ROWID = PROTEIN_PREFIX + "SeqId";
        private final boolean _expanded;

        public FlatPeptideQueryView(ViewContext context, UserSchema schema, QuerySettings settings, boolean expanded)
        {
            super(context, schema, settings);
            _expanded = expanded;
            _buttonBarPosition = DataRegion.ButtonBarPosition.BOTTOM; 
        }

        protected DataRegion createDataRegion()
        {
            List<DisplayColumn> originalColumns = getDisplayColumns();
            List<DisplayColumn> peptideColumns = new ArrayList<DisplayColumn>();
            List<DisplayColumn> groupingColumns = new ArrayList<DisplayColumn>();
            List<DisplayColumn> allColumns = new ArrayList<DisplayColumn>();

            Set<NestingOption> nestingOptions = new HashSet<NestingOption>();
            nestingOptions.add(new NestingOption(PROTEIN_GROUP_PREFIX, PROTEIN_GROUP_ROWID));
            nestingOptions.add(new NestingOption(PROTEIN_PREFIX, PROTEIN_ROWID));

            for (DisplayColumn column : originalColumns)
            {
                boolean nestedColumn = false;
                for (NestingOption option : nestingOptions)
                {
                    if (option.isNested(column.getColumnInfo().getName()))
                    {
                        nestedColumn = true;
                        if (_selectedNestingOption != null && _selectedNestingOption != option)
                        {
                            throw new IllegalArgumentException();
                        }
                        _selectedNestingOption = option;
                        _selectedNestingOption.setupGroupIdColumn(allColumns, groupingColumns, column.getColumnInfo().getParentTable());
                        groupingColumns.add(column);
                    }
                }
                if (!nestedColumn)
                {
                    peptideColumns.add(column);
                }
                allColumns.add(column);
            }

            DataRegion rgn;
            if (_selectedNestingOption != null)
            {
                QueryPeptideDataRegion ppRgn = new QueryPeptideDataRegion(allColumns, _selectedNestingOption.getGroupIdColumn(), _runs);
                ppRgn.setExpanded(_expanded);
                ppRgn.setRecordSelectorValueColumns(_selectedNestingOption.getGroupIdColumn().getColumnInfo().getAlias());
                DataRegion nestedRgn = new DataRegion();
                nestedRgn.setName(getDataRegionName());
                nestedRgn.setButtonBarPosition(DataRegion.ButtonBarPosition.NONE);
                nestedRgn.setDisplayColumnList(peptideColumns);
                ppRgn.setNestedRegion(nestedRgn);
                for (DisplayColumn column : groupingColumns)
                {
                    column.setCaption(column.getColumnInfo().getCaption());
                }
                ppRgn.setDisplayColumnList(groupingColumns);

                rgn = ppRgn;
            }
            else
            {
                rgn = new DataRegion();
                rgn.setDisplayColumnList(peptideColumns);
            }
            rgn.setMaxRows(getMaxRows());
            rgn.setShowRecordSelectors(showRecordSelectors());
            rgn.setName(getDataRegionName());

            rgn.setShowRecordSelectors(true);
            rgn.setFixedWidthColumns(true);

            setPeptideUrls(rgn, null);

            rgn.addHiddenFormField("queryString", _url.getRawQuery());  // Pass query string for exportSelectedToExcel post case... need to display filter & sort to user, and to show the right columns
            rgn.addHiddenFormField(MS2Manager.getDataRegionNamePeptides() + ".sort", _url.getParameter(MS2Manager.getDataRegionNamePeptides() + ".sort"));     // Stick sort on the request as well so DataRegion sees it
            rgn.addHiddenFormField("columns", _url.getParameter("columns"));
            rgn.addHiddenFormField("run", _url.getParameter("run"));

            return rgn;
        }

        private class SortRewriterRenderContext extends RenderContext
        {
            public SortRewriterRenderContext(ViewContext context)
            {
                super(context);
            }


            protected Sort buildSort(TableInfo tinfo, ViewURLHelper url, String name)
            {
                Sort standardSort = super.buildSort(tinfo, url, name);
                if (_selectedNestingOption != null)
                {
                    boolean foundGroupId = false;
                    standardSort.getSortList();
                    Sort sort = new Sort();
                    sort.setMaxClauses(standardSort.getMaxClauses());

                    int totalIndex = 0;
                    int proteinIndex = 0;
                    for (Sort.SortField field : standardSort.getSortList())
                    {
                        boolean proteinGroupColumn = field.getColumnName().toLowerCase().startsWith(_selectedNestingOption._prefix);
                        foundGroupId = foundGroupId || field.getColumnName().equalsIgnoreCase(_selectedNestingOption._rowIdColumnName);
                        sort.insertSortColumn(field.toUrlString(), field.isUrlClause(), proteinGroupColumn ? proteinIndex++ : totalIndex);
                        totalIndex++;
                    }

                    if (!foundGroupId)
                    {
                        sort.insertSortColumn(_selectedNestingOption._rowIdColumnName, false, proteinIndex++);
                    }

                    return sort;
                }
                else
                {
                    return standardSort;
                }
            }


            protected SimpleFilter buildFilter(TableInfo tinfo, ViewURLHelper url, String name)
            {
                SimpleFilter result = super.buildFilter(tinfo, url, name);
                if (_selectedNestingOption != null)
                {
                    result.addCondition(_selectedNestingOption._rowIdColumnName, null, CompareType.NONBLANK);
                }
                return result;
            }
        }

        protected DataView createDataView()
        {
            DataRegion rgn = createDataRegion();
            GridView result = new GridView(rgn, new SortRewriterRenderContext(getViewContext()));
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
            filter.addAllClauses(ProteinManager.getPeptideFilter(_url, getSingleRun(), ProteinManager.EXTRA_FILTER));
            result.getRenderContext().setBaseFilter(filter);
            return result;
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

        public FilteredTable getFilteredTable()
        {
            return (FilteredTable)getTable();
        }
    }

    public AbstractProteinExcelWriter getExcelProteinGridWriter(String requestedProteinColumnNames) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void exportTSVProteinGrid(ProteinTSVGridWriter tw, String requestedPeptideColumns, MS2Run run, String where) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(String requestedProteinColumnNames, String requestedPeptideColumnNames, boolean expanded) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void setUpExcelProteinGrid(AbstractProteinExcelWriter ewProtein, boolean expanded, String requestedPeptideColumnNames, MS2Run run, String where) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void addSQLSummaries(List<Pair<String, String>> sqlSummaries)
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

    protected List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        throw new UnsupportedOperationException();
    }
}
