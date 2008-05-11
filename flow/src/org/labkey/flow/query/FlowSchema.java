package org.labkey.flow.query;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.*;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.view.FlowQueryView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.sql.Types;
import java.util.*;

public class FlowSchema extends UserSchema
{
    static public final String SCHEMANAME = "flow";
    private FlowExperiment _experiment;
    private FlowRun _run;
    private FlowProtocol _protocol = null;

    public FlowSchema(User user, Container container)
    {
        this(user, container, FlowProtocol.getForContainer(container));
    }

    public FlowSchema(ViewContext context) throws ServletException
    {
        this(context.getUser(), context.getContainer());
        setExperiment(FlowExperiment.fromURL(context.getActionURL(), context.getRequest()));
        setRun(FlowRun.fromURL(context.getActionURL()));
    }

    // FlowSchema.createView()
    private FlowSchema(ViewContext context, FlowSchema from) throws ServletException
    {
        this(context.getUser(), context.getContainer(), from._protocol);

        if (from._experiment != null)
        {
            _experiment = from._experiment;
            assert _experiment.getExperimentId() == getIntParam(context.getRequest(), FlowParam.experimentId);
        }

        if (from._run != null)
        {
            _run = from._run;
            assert _run.getRunId() == getIntParam(context.getRequest(), FlowParam.runId);
        }

        if (null == _experiment)
            setExperiment(FlowExperiment.fromURL(context.getActionURL(), context.getRequest()));
        if (null == _run)
            setRun(FlowRun.fromURL(context.getActionURL()));
    }

    private FlowSchema(User user, Container container, FlowProtocol protocol)
    {
        super(SCHEMANAME, user, container, ExperimentService.get().getSchema());
        _protocol = protocol;
    }

    public FlowSchema detach()
    {
        if (_experiment == null && _run == null)
            return this;
        return new FlowSchema(_user, _container, _protocol);
    }

    public TableInfo getTable(String name, String alias)
    {
        try
        {
            FlowTableType type = FlowTableType.valueOf(name);
            switch (type)
            {
                case FCSFiles:
                    return createFCSFileTable(alias);
                case FCSAnalyses:
                    return createFCSAnalysisTable(alias, FlowDataType.FCSAnalysis);
                case CompensationControls:
                    return createCompensationControlTable(alias);
                case Runs:
                    return createRunTable(alias, null);
                case CompensationMatrices:
                    return createCompensationMatrixTable(alias);
                case AnalysisScripts:
                    return createAnalysisScriptTable(alias, false);
                case Analyses:
                    return createAnalysesTable(alias);
                case Statistics:
                    return createStatisticsTable(alias);
                case Keywords:
                    return createKeywordsTable(alias);
            }
            return null;
        }
        catch (IllegalArgumentException iae)
        {
            // ignore
        }
        return super.getTable(name, alias);
    }

    public Set<String> getVisibleTableNames()
    {
        Set<String> ret = new HashSet<String>();
        for (FlowTableType tt : FlowTableType.values())
        {
            if (!tt.isHidden())
                ret.add(tt.toString());
        }
        return ret;
    }

    public QueryDefinition getQueryDef(String name)
    {
        return QueryService.get().getQueryDef(getContainer(), getSchemaName(), name);
    }

    public QueryDefinition getQueryDef(FlowTableType qt)
    {
        return getQueryDef(qt.name());
    }

    public Set<String> getTableNames()
    {
        Set<String> ret = new LinkedHashSet<String>();
        for (FlowTableType type : FlowTableType.values())
        {
            ret.add(type.toString());
        }
        return ret;
    }

    public Container getContainer()
    {
        return _container;
    }

    public User getUser()
    {
        return _user;
    }

    public void setExperiment(FlowExperiment experiment)
    {
        _experiment = experiment;
    }

    public void setRun(FlowRun run)
    {
        _run = run;
    }

    public FlowExperiment getExperiment()
    {
        return _experiment;
    }

    public FlowRun getRun()
    {
        return _run;
    }

    public ActionURL urlFor(QueryAction action, FlowTableType type)
    {
        return urlFor(action, getQueryDefForTable(type.name()));
    }

    public ActionURL urlFor(QueryAction action)
    {
        ActionURL ret = super.urlFor(action);
        addParams(ret);
        return ret;
    }

    public ActionURL urlFor(QueryAction action, QueryDefinition queryDef)
    {
        ActionURL ret = super.urlFor(action, queryDef);
        addParams(ret);
        return ret;
    }

    public void addParams(ActionURL url)
    {
        if (_run != null)
        {
            _run.addParams(url);
        }
        if (_experiment != null)
        {
            _experiment.addParams(url);
        }
    }

    public QueryView createView(ViewContext context, QuerySettings settings) throws ServletException
    {
        return new FlowQueryView(context, new FlowSchema(context, this), (FlowQuerySettings) settings);
    }

/*    private SQLFragment sqlObjectTypeId(SQLFragment sqlDataId)
    {
        SQLFragment ret = new SQLFragment("(SELECT flow.Object.TypeId FROM flow.Object WHERE flow.Object.DataId = (");
        ret.append(sqlDataId);
        ret.append("))");
        return sqlDataId;
    } */

    public ExpRunTable createRunTable(String alias, FlowDataType type)
    {
        ExpRunTable ret = ExperimentService.get().createRunTable(alias);

        if (_container != null)
        {
            ret.setContainer(_container);
        }
        if (_experiment != null)
        {
            ret.setExperiment(_experiment.getExpObject());
        }
        ret.addColumn(ExpRunTable.Column.RowId);
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(RunController.Action.showRun, _container), Collections.singletonMap(FlowParam.runId.toString(), ExpRunTable.Column.RowId.toString())));
        if (type == null || type == FlowDataType.FCSFile || type == FlowDataType.FCSAnalysis)
        {
            ret.addColumn(ExpRunTable.Column.Flag);
        }
        ret.addColumn(ExpRunTable.Column.Name);
        ret.addColumn(ExpRunTable.Column.FilePathRoot).setIsHidden(true);
        ret.addColumn(ExpRunTable.Column.LSID).setIsHidden(true);
        ret.addColumn(ExpRunTable.Column.ProtocolStep);
        if (type != FlowDataType.FCSFile)
        {
            ColumnInfo colAnalysisScript;
            colAnalysisScript = ret.addDataInputColumn("AnalysisScript", InputRole.AnalysisScript.getPropertyDescriptor(getContainer()));
            colAnalysisScript.setFk(new LookupForeignKey(PageFlowUtil.urlFor(AnalysisScriptController.Action.begin, getContainer()),
                    FlowParam.scriptId.toString(), "RowId", "Name"){
                public TableInfo getLookupTableInfo()
                {
                    return detach().createAnalysisScriptTable("Lookup", true);
                }
            });
        }
        if (type != FlowDataType.CompensationMatrix && type != FlowDataType.FCSFile)
        {
            ColumnInfo colCompensationMatrix;
            colCompensationMatrix= ret.addDataInputColumn("CompensationMatrix", InputRole.CompensationMatrix.getPropertyDescriptor(getContainer()));
            colCompensationMatrix.setFk(new LookupForeignKey(null, (String) null, "RowId", "Name") {
                public TableInfo getLookupTableInfo()
                {
                    return detach().createCompensationMatrixTable("Lookup");
                }
            });
        }
        ret.addDataCountColumn("WellCount", InputRole.FCSFile.getPropertyDescriptor(getContainer()));
        ret.addColumn(ExpRunTable.Column.Created);
        ret.addColumn(ExpRunTable.Column.CreatedBy);
        addDataCountColumn(ret, "FCSFileCount", ObjectType.fcsKeywords);
        addDataCountColumn(ret, "CompensationControlCount", ObjectType.compensationControl);
        addDataCountColumn(ret, "FCSAnalysisCount", ObjectType.fcsAnalysis);
        ret.setEditHelper(new RunEditHelper(this));
        return ret;
    }

/*    private SQLFragment dataIdCondition(String dataidName, ObjectType ... types)
    {
        SQLFragment ret = new SQLFragment("(SELECT flow.object.TypeId FROM flow.Object WHERE ");
        ret.append(dataidName + " = flow.object.DataId)");

        if (types.length == 1)
        {
            ret.append(" = " + types[0].getTypeId());
        }
        else
        {
            ret.append(" IN ( " + types[0].getTypeId());
            for (int i = 1; i < types.length; i ++)
            {
                ret.append(", " + types[i].getTypeId());
            }
            ret.append(" )");
        }
        return ret;
    } */


    /**
     *  FlowDataTable is ExpDataTable with an ObjectId column
     *
     *  basically rejoins what is effectively a vertically partitioned table (ACKK) 
     */
    class FlowDataTable extends AbstractTableInfo implements ExpDataTable
    {
        final ExpDataTable _expData;
        final TableInfo _flowObject;
        final FlowDataType _type;
        final String _expDataAlias;

        FlowDataTable(String alias, FlowDataType type)
        {
            super(getDbSchema());
            setAlias(alias);
            _expDataAlias = "_expdata_";
            _expData = ExperimentService.get().createDataTable(_expDataAlias);
            _flowObject = DbSchema.get("flow").getTable("object");
            _type = type;
        }

        ColumnInfo addStatisticColumn(String columnAlias)
        {
            ColumnInfo colStatistic = addObjectIdColumn(columnAlias);
            colStatistic.setFk(new StatisticForeignKey(new FlowPropertySet(this)));
            colStatistic.setIsUnselectable(true);
            addMethod(columnAlias, new StatisticMethod(colStatistic));
            return colStatistic;
        }

        ColumnInfo addObjectIdColumn(String name)
        {
            ColumnInfo underlyingColumn = _flowObject.getColumn("rowid");
            ExprColumn ret = new ExprColumn(this, name, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".objectid"), underlyingColumn.getSqlTypeInt());
            ret.copyAttributesFrom(underlyingColumn);
            addColumn(ret);
            return ret;
        }

        ColumnInfo addExpColumn(ColumnInfo underlyingColumn)
        {
            ExprColumn ret = new ExprColumn(this, underlyingColumn.getAlias(), underlyingColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS), underlyingColumn.getSqlTypeInt());
            ret.copyAttributesFrom(underlyingColumn);
            ret.setIsHidden(underlyingColumn.isHidden());
            if (underlyingColumn.getFk() instanceof RowIdForeignKey)
                ret.setFk(new RowIdForeignKey(ret));            
            addColumn(ret);
            return ret;
        }

        /* TableInfo */
        public SQLFragment getFromSQL(String alias)
        {
            SQLFragment sqlFlowData = new SQLFragment();

            sqlFlowData.append("(SELECT " + _expDataAlias + ".*,");
            sqlFlowData.append(_flowObject).append("." + _flowObject.getColumn("rowid").getName() + " AS objectid");
            if (null != _flowObject.getColumn("compid"))
            {
                sqlFlowData.append(",");
                sqlFlowData.append(_flowObject).append("." + _flowObject.getColumn("compid").getName() + ",");
                sqlFlowData.append(_flowObject).append("." + _flowObject.getColumn("fcsid").getName() + ",");
                sqlFlowData.append(_flowObject).append("." + _flowObject.getColumn("scriptid").getName() + "");
            }
            sqlFlowData.append("\nFROM ");
            sqlFlowData.append(_expData);
            sqlFlowData.append(" INNER JOIN " );
            sqlFlowData.append(_flowObject);
            sqlFlowData.append(" ON " +
                    _expDataAlias + "." + _expData.getColumn("rowid").getName() + "=" + _flowObject.toString() + "." + _flowObject.getColumn("dataid").getName() +
                    " AND " +
                    _expDataAlias + ".container=" + _flowObject.toString() + "." + _flowObject.getColumn("container").getName()
                    );
            sqlFlowData.append("\n");
            sqlFlowData.append("WHERE ");
            sqlFlowData.append(_flowObject).append("." + _flowObject.getColumn("typeid").getName() + "=" + _type.getObjectType().getTypeId());
            sqlFlowData.append(" AND ");
            sqlFlowData.append(_flowObject.toString() + "." + _flowObject.getColumn("container").getName() + "='" + getContainer().getId() + "'");
            // UNDONE
            //     final private SimpleFilter _filter;
            //UNDONE
            sqlFlowData.append(") AS " + alias);
            return sqlFlowData;
        }

        @Override
        public boolean hasPermission(User user, int perm)
        {
            return _expData.hasPermission(user, perm);
        }

        /* ExpDataTable */
        public void populate(ExpSchema schema)
        {
            _expData.populate(schema);
            for (ColumnInfo col : _expData.getColumnsList())
            {
                if (null == getColumn(col.getName()))
                    addExpColumn(col);
            }
        }

        public void setExperiment(ExpExperiment experiment)
        {
            _expData.setExperiment(experiment);
        }

        public ExpExperiment getExperiment()
        {
            return _expData.getExperiment();
        }

        public void setRun(ExpRun run)
        {
            _expData.setRun(run);
        }

        public ExpRun getRun()
        {
            return _expData.getRun();
        }

        public void setDataType(DataType type)
        {
            _expData.setDataType(type);
        }

        public DataType getDataType()
        {
            return _expData.getDataType();
        }

        public ColumnInfo addMaterialInputColumn(String alias, SamplesSchema schema, PropertyDescriptor inputRole, ExpSampleSet sampleSet)
        {
            ColumnInfo col = _expData.addMaterialInputColumn(alias,schema,inputRole,sampleSet);
            return addExpColumn(col);
        }

        public ColumnInfo addDataInputColumn(String alias, PropertyDescriptor role)
        {
            ColumnInfo col = _expData.addDataInputColumn(alias,role);
            return addExpColumn(col);
        }

        public ColumnInfo addInputRunCountColumn(String alias)
        {
            ColumnInfo col = _expData.addInputRunCountColumn(alias);
            return addExpColumn(col);
        }

        /* ExpTable */

        public void setContainer(Container container)
        {
            _expData.setContainer(container);
        }

        public Container getContainer()
        {
            return _expData.getContainer();
        }

        public ColumnInfo addColumn(Column column)
        {
            ColumnInfo col = _expData.addColumn(column);
            return addExpColumn(col);
        }

        public ColumnInfo addColumn(String alias, Column column)
        {
            ColumnInfo col = _expData.addColumn(alias, column);
            return addExpColumn(col);
        }

        public ColumnInfo getColumn(Column column)
        {
            for (ColumnInfo info : getColumnsList())
            {
                if (info instanceof ExprColumn && info.getAlias().equals(column.toString()))
                {
                    return info;
                }
            }
            return null;
        }

        public ColumnInfo createColumn(String alias, Column column)
        {
            ColumnInfo col = _expData.createColumn(alias, column);
            return addExpColumn(col);
        }

        public ColumnInfo createPropertyColumn(String alias)
        {
            ColumnInfo col = _expData.createPropertyColumn(alias);
            return addExpColumn(col);
        }

        public void addCondition(SQLFragment condition, String... columnNames)
        {
            // NOTE: since this is being pushed down we can't use object id here
            _expData.addCondition(condition,columnNames);
        }

        public void addRowIdCondition(SQLFragment rowidCondition)
        {
            _expData.addRowIdCondition(rowidCondition);
        }

        public void addLSIDCondition(SQLFragment lsidCondition)
        {
            _expData.addLSIDCondition(lsidCondition);
        }

        public void setEditHelper(TableEditHelper helper)
        {
            _expData.setEditHelper(helper);
        }

        public ColumnInfo addPropertyColumns(String domainDescription, PropertyDescriptor[] pds, QuerySchema schema)
        {
            ColumnInfo col = _expData.addPropertyColumns(domainDescription, pds, schema);
            return addExpColumn(col);
        }
    }


    public FlowDataTable createDataTable(String alias, final FlowDataType type)
    {
        FlowDataTable ret = new FlowDataTable(alias, type);
        ret.setContainer(getContainer());
        ret.setDataType(type);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.RowId).setIsHidden(true);
        ret.addColumn(ExpDataTable.Column.LSID).setIsHidden(true);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setIsHidden(true);

        ColumnInfo colRun = ret.addColumn(ExpDataTable.Column.Run);
        colRun.setFk(new LookupForeignKey(PageFlowUtil.urlFor(RunController.Action.showRun, getContainer()), FlowParam.runId, "RowId", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                return detach().createRunTable("run", type);
            }
        });
        if (_run != null)
        {
            ret.setRun(_run.getExpObject());
        }
        return ret;
    }
    

/*    public ExpDataTable createDataTableOLD(String alias, final FlowDataType type)
    {
        ExpDataTable ret = ExperimentService.get().createDataTable(alias);
        ret.setContainer(getContainer());
        ret.setDataType(type);
        ret.addCondition(dataIdCondition(ExpDataTable.COLUMN_ROWID, type.getObjectType()));
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.LSID).setIsHidden(true);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setIsHidden(true);

        ColumnInfo colRun = ret.addColumn(ExpDataTable.Column.Run);
        colRun.setFk(new LookupForeignKey(PageFlowUtil.urlFor(RunController.Action.showRun, getContainer()), FlowParam.runId, "RowId", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                return detach().createRunTable("run", type);
            }
        });
        if (_run != null)
        {
            ret.setRun(ExperimentService.get().getExpRun(_run.getRunId()));
        }
        return ret;
    }

    protected ColumnInfo addObjectIdColumn(ExpDataTable table, String name)
    {
        SQLFragment sql = new SQLFragment("(SELECT flow.Object.RowId FROM flow.Object WHERE flow.Object.DataId = ");
        sql.append(table.createColumn("RowId", ExpDataTable.Column.RowId).getValueSql(ExprColumn.STR_TABLE_ALIAS));
        sql.append(")");
        ColumnInfo ret = new ExprColumn(table, name, sql, Types.INTEGER);
        table.addColumn(ret);
        return ret;
    } */


    static private class DeferredFCSFileVisibleColumns implements Iterable<FieldKey>
    {
        final private ExpDataTable _table;
        final private ColumnInfo _colKeyword;
        public DeferredFCSFileVisibleColumns(ExpDataTable table, ColumnInfo colKeyword)
        {
            _table = table;
            _colKeyword = colKeyword;
        }

        public Iterator<FieldKey> iterator()
        {
            List<FieldKey> ret = QueryService.get().getDefaultVisibleColumns(_table.getColumnsList());
            TableInfo lookup = _colKeyword.getFk().getLookupTableInfo();
            FieldKey keyKeyword = new FieldKey(null, _colKeyword.getName());
            if (lookup != null)
            {
                for (FieldKey key : lookup.getDefaultVisibleColumns())
                {
                    ret.add(FieldKey.fromParts(keyKeyword, key));
                }
            }
            return ret.iterator();
        }
    }

    static private class DeferredFCSAnalysisVisibleColumns implements Iterable<FieldKey>
    {
        final private ExpDataTable _table;
        final private ColumnInfo _colStatistic;
        final private ColumnInfo _colGraph;

        public DeferredFCSAnalysisVisibleColumns(ExpDataTable table, ColumnInfo colStatistic, ColumnInfo colGraph)
        {
            _table = table;
            _colStatistic = colStatistic;
            _colGraph = colGraph;
        }

        public Iterator<FieldKey> iterator()
        {
            Collection<FieldKey> ret = new LinkedHashSet<FieldKey>();
            ret.addAll(QueryService.get().getDefaultVisibleColumns(_table.getColumnsList()));
            ret.remove(FieldKey.fromParts("AnalysisScript"));
            ret.remove(FieldKey.fromParts("FCSFile"));
            TableInfo lookup = _colStatistic.getFk().getLookupTableInfo();
            if (lookup != null)
            {
                int count = 0;
                FieldKey keyStatistic = new FieldKey(null, _colStatistic.getName());
                for (FieldKey key : lookup.getDefaultVisibleColumns())
                {
                    ret.add(FieldKey.fromParts(keyStatistic, key));
                    if (++count > 3)
                        break;
                }
            }
            lookup = _colGraph.getFk().getLookupTableInfo();
            if (lookup != null)
            {
                int count = 0;
                FieldKey keyGraph = new FieldKey(null, _colGraph.getName());
                for (FieldKey key : lookup.getDefaultVisibleColumns())
                {
                    ret.add(FieldKey.fromParts(keyGraph, key));
                    if (++count > 3)
                        break;
                }
            }
            return ret.iterator();
        }
    }

    public FlowDataTable createFCSFileTable(String alias)
    {
        final FlowDataTable ret = createDataTable(alias, FlowDataType.FCSFile);
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        final ColumnInfo colKeyword = ret.addObjectIdColumn("Keyword");
        FlowPropertySet fps = new FlowPropertySet(ret);
        colKeyword.setFk(new KeywordForeignKey(fps));
        colKeyword.setIsUnselectable(true);
        ret.addMethod("Keyword", new KeywordMethod(colKeyword));
        ExpSampleSet ss = null;
        if (_protocol != null)
        {
            ss = _protocol.getSampleSet();
        }
        ColumnInfo colMaterialInput = ret.addMaterialInputColumn("Sample", new SamplesSchema(getUser(), getContainer()), null, ss);
        if (ss == null)
        {
            colMaterialInput.setIsHidden(true);
        }

        ret.setDefaultVisibleColumns(new DeferredFCSFileVisibleColumns(ret, colKeyword));
        return ret;
    }


    public ExpDataTable createFCSAnalysisTable(String alias, FlowDataType type)
    {
        if (null != DbSchema.get("flow").getTable("object").getColumn("compid"))
            return createFCSAnalysisTableNEW(alias, type);

        FlowDataTable ret = createDataTable(alias, type);
        ColumnInfo colAnalysisScript = ret.addDataInputColumn("AnalysisScript", InputRole.AnalysisScript.getPropertyDescriptor(getContainer()));
        colAnalysisScript.setFk(new LookupForeignKey(PageFlowUtil.urlFor(AnalysisScriptController.Action.begin, getContainer()),
                FlowParam.scriptId.toString(), "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createAnalysisScriptTable("Lookup", true);
            }
        });
        ColumnInfo colCompensationMatrix = ret.addDataInputColumn("CompensationMatrix", InputRole.CompensationMatrix.getPropertyDescriptor(getContainer()));
        colCompensationMatrix.setFk(new LookupForeignKey(PageFlowUtil.urlFor(CompensationController.Action.showCompensation, getContainer()), FlowParam.compId.toString(),
                "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createCompensationMatrixTable("Lookup");
            }
        });

        FlowPropertySet fps = new FlowPropertySet(ret);
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        if (getExperiment() != null)
        {
            ret.setExperiment(ExperimentService.get().getExpExperiment(getExperiment().getLSID()));
        }
        ColumnInfo colStatistic = ret.addStatisticColumn("Statistic");
        ColumnInfo colGraph = ret.addObjectIdColumn("Graph");
        colGraph.setFk(new GraphForeignKey(fps));
        colGraph.setIsUnselectable(true);
        ColumnInfo colFCSFile = ret.addDataInputColumn("FCSFile", InputRole.FCSFile.getPropertyDescriptor(getContainer()));
        colFCSFile.setFk(new LookupForeignKey(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()),
                FlowParam.wellId.toString(),
                "RowId", "Name") {
                public TableInfo getLookupTableInfo()
                {
                    return detach().createFCSFileTable("FCSFile");
                }
            });
        ret.setDefaultVisibleColumns(new DeferredFCSAnalysisVisibleColumns(ret, colStatistic, colGraph));
        return ret;
    }


    public ExpDataTable createFCSAnalysisTableNEW(String alias, FlowDataType type)
    {
        FlowDataTable ret = createDataTable(alias, type);

        ColumnInfo colAnalysisScript = new ExprColumn(ret, "AnalysisScript", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".scriptid"), Types.INTEGER);
        ret.addColumn(colAnalysisScript);
        colAnalysisScript.setFk(new LookupForeignKey(PageFlowUtil.urlFor(AnalysisScriptController.Action.begin, getContainer()),
                FlowParam.scriptId.toString(), "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createAnalysisScriptTable("Lookup", true);
            }
        });

        ColumnInfo colCompensationMatrix = new ExprColumn(ret, "CompensationMatrix", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".compid"), Types.INTEGER);
        ret.addColumn(colCompensationMatrix);
        colCompensationMatrix.setFk(new LookupForeignKey(PageFlowUtil.urlFor(CompensationController.Action.showCompensation, getContainer()), FlowParam.compId.toString(),
                "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createCompensationMatrixTable("Lookup");
            }
        });

        FlowPropertySet fps = new FlowPropertySet(ret);
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        if (getExperiment() != null)
        {
            ret.setExperiment(ExperimentService.get().getExpExperiment(getExperiment().getLSID()));
        }

        ColumnInfo colStatistic = ret.addStatisticColumn("Statistic");

        ColumnInfo colGraph = ret.addObjectIdColumn("Graph");
        colGraph.setFk(new GraphForeignKey(fps));
        colGraph.setIsUnselectable(true);

        ColumnInfo colFCSFile = new ExprColumn(ret, "FCSFile", new SQLFragment(ExprColumn.STR_TABLE_ALIAS  + ".fcsid"), Types.INTEGER);
        ret.addColumn(colFCSFile);
        colFCSFile.setFk(new LookupForeignKey(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()),
                FlowParam.wellId.toString(),
                "RowId", "Name") {
                public TableInfo getLookupTableInfo()
                {
                    return detach().createFCSFileTable("FCSFile");
                }
            });

        ret.setDefaultVisibleColumns(new DeferredFCSAnalysisVisibleColumns(ret, colStatistic, colGraph));
        return ret;
    }


    public ExpDataTable createCompensationControlTable(String alias)
    {
        ExpDataTable ret = createFCSAnalysisTable(alias, FlowDataType.CompensationControl);
        List<FieldKey> defColumns = new ArrayList<FieldKey>(ret.getDefaultVisibleColumns());
        defColumns.add(FieldKey.fromParts("Statistic", new StatisticSpec(FCSAnalyzer.compSubset, StatisticSpec.STAT.Count, null).toString()));
        defColumns.add(FieldKey.fromParts("Statistic", new StatisticSpec(FCSAnalyzer.compSubset, StatisticSpec.STAT.Freq_Of_Parent, null).toString()));
        ret.setDefaultVisibleColumns(defColumns);
        return ret;
    }

    public FlowDataTable createCompensationMatrixTable(String alias)
    {
        FlowDataTable ret = createDataTable(alias, FlowDataType.CompensationMatrix);
        if (getExperiment() != null)
        {
            ret.setExperiment(ExperimentService.get().getExpExperiment(getExperiment().getLSID()));
        }
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(CompensationController.Action.showCompensation, getContainer()), Collections.singletonMap(FlowParam.compId.toString(), ExpDataTable.Column.RowId.toString())));
        ret.addStatisticColumn("Value");
        return ret;
    }

    public ExpDataTable createAnalysisScriptTable(String alias, boolean includePrivate)
    {
        ExpDataTable ret = createDataTable(alias, FlowDataType.Script);
        if (!includePrivate)
        {
            SQLFragment runIdCondition = new SQLFragment("RunId IS NULL");
            ret.addCondition(runIdCondition);
        }
        ret.addInputRunCountColumn("RunCount");
        ret.getColumn(ExpDataTable.Column.Run.toString()).setIsHidden(true);
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(AnalysisScriptController.Action.begin, getContainer()), Collections.singletonMap(FlowParam.scriptId.toString(), "RowId")));
        return ret;
    }

    public ExpExperimentTable createAnalysesTable(String alias)
    {
        ExpExperimentTable ret = ExperimentService.get().createExperimentTable(alias);
        ret.setContainer(getContainer());
        FlowProtocol compensationProtocol = FlowProtocolStep.calculateCompensation.getForContainer(getContainer());
        FlowProtocol analysisProtocol = FlowProtocolStep.analysis.getForContainer(getContainer());
        ret.populate(new ExpSchema(getUser(), getContainer()));
        if (compensationProtocol != null)
        {
            ColumnInfo colCompensationCount = ret.createRunCountColumn("CompensationRunCount", null, compensationProtocol.getProtocol());
            ActionURL detailsURL = FlowTableType.Runs.urlFor(getContainer(), "ProtocolStep", FlowProtocolStep.calculateCompensation.getName());
            colCompensationCount.setURL(detailsURL + "&" + FlowParam.experimentId.toString() + "=${RowId}");
            ret.addColumn(colCompensationCount);
        }
        if (analysisProtocol != null)
        {
            ColumnInfo colAnalysisRunCount = ret.createRunCountColumn("AnalysisRunCount", null, analysisProtocol.getProtocol());
            ActionURL detailsURL = FlowTableType.Runs.urlFor(getContainer(), "ProtocolStep", FlowProtocolStep.analysis.getName());
            colAnalysisRunCount.setURL(detailsURL + "&" + FlowParam.experimentId.toString() + "=${RowId}");
            ret.addColumn(colAnalysisRunCount);
        }
        ret.setDetailsURL(new DetailsURL(FlowTableType.Runs.urlFor(getContainer(), (SimpleFilter) null, new Sort("ProtocolStep")),
                Collections.singletonMap(FlowParam.experimentId.toString(), ExpExperimentTable.Column.RowId.toString())));
        SQLFragment lsidCondition = new SQLFragment("LSID <> ");
        lsidCondition.appendStringLiteral(FlowExperiment.getExperimentRunExperimentLSID(getContainer()));
        ret.addCondition(lsidCondition);
        return ret;
    }

    public FlowQuerySettings getSettings(ActionURL url, String dataRegionName)
    {
        FlowQuerySettings settings = new FlowQuerySettings(url, dataRegionName);
        settings.setSchemaName(getSchemaName());
        return settings;
    }

    public FlowQuerySettings getSettings(Portal.WebPart webPart, ViewContext context)
    {
        FlowQuerySettings settings = new FlowQuerySettings(webPart, context);
        settings.setSchemaName(getSchemaName());
        return settings;
    }

    public QueryDefinition getQueryDefForTable(String name)
    {
        QueryDefinition ret = super.getQueryDefForTable(name);
        try
        {
            FlowTableType type = FlowTableType.valueOf(name);
            ret.setDescription(type.getDescription());
        }
        catch(IllegalArgumentException iae)
        {
            // ignore
        }
        return ret;
    }

    private ColumnInfo addDataCountColumn(ExpRunTable runTable, String name, ObjectType type)
    {
        SQLFragment sql = new SQLFragment("(SELECT COUNT(exp.data.rowid) FROM exp.data WHERE (SELECT flow.object.typeid FROM flow.object WHERE flow.object.dataid = exp.data.rowid) = "
                + type.getTypeId() + " AND exp.data.runid = " + ExprColumn.STR_TABLE_ALIAS + ".RowId)");
        ExprColumn ret = new ExprColumn(runTable, name, sql, Types.INTEGER);
        ret.setIsHidden(true);
        runTable.addColumn(ret);
        return ret;
    }

    private TableInfo createStatisticsTable(String alias)
    {
        FilteredTable ret = new FilteredTable(FlowManager.get().getTinfoAttribute());
        ret.setAlias(alias);
        ret.addWrapColumn(ret.getRealTable().getColumn("Name"));
        ExpDataTable fcsAnalysisTable = createFCSAnalysisTable("fcsAnalysis", FlowDataType.FCSAnalysis);
        FlowPropertySet fps = new FlowPropertySet(fcsAnalysisTable);
        filterTable(ret, fps.getStatistics());
        return ret;
    }

    private TableInfo createKeywordsTable(String alias)
    {
        FilteredTable ret = new FilteredTable(FlowManager.get().getTinfoAttribute());
        ret.setAlias(alias);
        ret.addWrapColumn(ret.getRealTable().getColumn("Name"));
        ExpDataTable fcsFilesTable = createFCSFileTable("fcsFiles");
        FlowPropertySet fps = new FlowPropertySet(fcsFilesTable);
        filterTable(ret, fps.getKeywordProperties());
        return ret;
    }

    private void filterTable(FilteredTable table, Map<? extends Object, Integer> map)
    {
        if (map.isEmpty())
        {
            table.addCondition(new SQLFragment("1 = 0"));
        }
        else
        {
            SQLFragment sqlCondition = new SQLFragment("RowId IN (");
            sqlCondition.append(StringUtils.join(map.values().iterator(), ","));
            sqlCondition.append(")");
            table.addCondition(sqlCondition);
        }
    }



    static public int getIntParam(HttpServletRequest request, FlowParam param)
    {
        String str = request.getParameter(param.toString());
        if (str == null || str.length() == 0)
            return 0;
        return Integer.valueOf(str);
    }
}
