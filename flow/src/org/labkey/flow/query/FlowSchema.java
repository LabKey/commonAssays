/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

package org.labkey.flow.query;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.query.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.util.*;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.collections.Cache;
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
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

public class FlowSchema extends UserSchema
{
    static public final IdentifierString SCHEMANAME = new IdentifierString("flow",false);
    private FlowExperiment _experiment;
    private FlowRun _run;
//    private FlowScript _script;
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
//        setScript(FlowScript.fromURL(context.getActionURL(), context.getRequest()));
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

//        if (from._script != null)
//        {
//            _script = from._script;
//            assert _script.getScriptId() == getIntParam(context.getRequest(), FlowParam.scriptId);
//        }

        if (null == _experiment)
            setExperiment(FlowExperiment.fromURL(context.getActionURL(), context.getRequest()));
//        if (null == _script)
//            setScript(FlowScript.fromURL(context.getActionURL(), context.getRequest()));
        if (null == _run)
            setRun(FlowRun.fromURL(context.getActionURL()));
    }

    private FlowSchema(User user, Container container, FlowProtocol protocol)
    {
        super(SCHEMANAME.toString(), user, container, ExperimentService.get().getSchema());
        _protocol = protocol;
    }

    public FlowSchema detach()
    {
        if (_experiment == null && _run == null /*&& _script == null*/)
            return this;
        return new FlowSchema(_user, _container, _protocol);
    }

    FlowProtocol getProtocol()
    {
        return _protocol;
    }

    public TableInfo createTable(String name)
    {
        try
        {
            FlowTableType type = FlowTableType.valueOf(name);
            return getTable(type);
        }
        catch (IllegalArgumentException iae)
        {
            // ignore
        }
        return null;
    }

    public TableInfo getTable(FlowTableType type)
    {
        switch (type)
        {
            case FCSFiles:
                return createFCSFileTable(type.toString());
            case FCSAnalyses:
                return createFCSAnalysisTable(type.toString(), FlowDataType.FCSAnalysis);
            case CompensationControls:
                return createCompensationControlTable(type.toString());
            case Runs:
                return createRunTable(type.toString(), null);
            case CompensationMatrices:
                return createCompensationMatrixTable(type.toString());
            case AnalysisScripts:
                return createAnalysisScriptTable(type.toString(), false);
            case Analyses:
                return createAnalysesTable(type.toString());
            case Statistics:
                return createStatisticsTable(type.toString());
            case Keywords:
                return createKeywordsTable(type.toString());
        }
        return null;
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

    public void setExperiment(FlowExperiment experiment)
    {
        _experiment = experiment;
    }

    public void setRun(FlowRun run)
    {
        _run = run;
    }

//    public void setScript(FlowScript script)
//    {
//        _script = script;
//    }

    public FlowExperiment getExperiment()
    {
        return _experiment;
    }

    public FlowRun getRun()
    {
        return _run;
    }

//    public FlowScript getScript()
//    {
//        return _script;
//    }

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
//        if (_script != null)
//        {
//            _script.addParams(url);
//        }
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
        ExpRunTable ret = ExperimentService.get().createRunTable(FlowTableType.Runs.toString(), this);

        if (_experiment != null)
        {
            ret.setExperiment(_experiment.getExpObject());
        }
//        if (_script != null)
//        {
//            ret.setInputData(_script.getExpObject());
//        }

        ret.addColumn(ExpRunTable.Column.RowId);
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(RunController.Action.showRun, _container), Collections.singletonMap(FlowParam.runId.toString(), ExpRunTable.Column.RowId.toString())));
        if (type == null || type == FlowDataType.FCSFile || type == FlowDataType.FCSAnalysis)
        {
            ColumnInfo flag = ret.addColumn(ExpRunTable.Column.Flag);
            if (type != null)
                flag.setDescription(type.getLabel() + " Flag");
        }
        ret.addColumn(ExpRunTable.Column.Name);
        ret.addColumn(ExpRunTable.Column.FilePathRoot).setIsHidden(true);
        ret.addColumn(ExpRunTable.Column.LSID).setIsHidden(true);
        ret.addColumn(ExpRunTable.Column.ProtocolStep);

        ColumnInfo analysisFolder = ret.addColumn(ExpRunTable.Column.RunGroups);
        analysisFolder.setCaption("Analysis Folder");
        ActionURL url = PageFlowUtil.urlFor(RunController.Action.showRuns, getContainer()).addParameter(FlowQueryView.DATAREGIONNAME_DEFAULT + ".sort", "ProtocolStep");
        analysisFolder.setURL(StringExpressionFactory.create(url.getLocalURIString() + "&experimentId=${experimentId}"));

        if (type != FlowDataType.FCSFile)
        {
            ColumnInfo colAnalysisScript;
            colAnalysisScript = ret.addDataInputColumn("AnalysisScript", InputRole.AnalysisScript.toString());
            colAnalysisScript.setFk(new LookupForeignKey(PageFlowUtil.urlFor(AnalysisScriptController.Action.begin, getContainer()),
                    FlowParam.scriptId.toString(),
                    FlowTableType.AnalysisScripts.toString(),
                    "RowId", "Name"){
                public TableInfo getLookupTableInfo()
                {
                    return detach().createAnalysisScriptTable("Lookup", true);
                }
            });
        }
        if (type != FlowDataType.CompensationMatrix && type != FlowDataType.FCSFile)
        {
            ColumnInfo colCompensationMatrix;
            colCompensationMatrix= ret.addDataInputColumn("CompensationMatrix", InputRole.CompensationMatrix.toString());
            colCompensationMatrix.setFk(new LookupForeignKey(null, (String) null,
                    FlowTableType.CompensationMatrices.toString(),
                    "RowId", "Name") {
                public TableInfo getLookupTableInfo()
                {
                    return detach().createCompensationMatrixTable("Lookup");
                }
            });
        }
        ret.addDataCountColumn("WellCount", InputRole.FCSFile.toString());
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
    class JoinFlowDataTable extends AbstractTableInfo implements ExpDataTable
    {
        final ExpDataTable _expData;
        final TableInfo _flowObject;
        final FlowDataType _type;
        final String _expDataAlias;
        FlowPropertySet _fps;

        JoinFlowDataTable(String name, FlowDataType type)
        {
            super(getDbSchema());
            setName(name);
            _expDataAlias = "_expdata_";
            _expData = ExperimentService.get().createDataTable(name, FlowSchema.this);
            _flowObject = DbSchema.get("flow").getTable("object");
            _type = type;
            _fps = new FlowPropertySet(_expData);
        }

        ColumnInfo addStatisticColumn(String columnAlias)
        {
            ColumnInfo colStatistic = addObjectIdColumn(columnAlias);
            colStatistic.setFk(new StatisticForeignKey(_fps, _type));
            colStatistic.setIsUnselectable(true);
            addMethod(columnAlias, new StatisticMethod(colStatistic));
            return colStatistic;
        }

        ColumnInfo addKeywordColumn(String columnAlias)
        {
            ColumnInfo colKeyword = addObjectIdColumn(columnAlias);
            colKeyword.setFk(new KeywordForeignKey(_fps));
            colKeyword.setIsUnselectable(true);
            addMethod("Keyword", new KeywordMethod(colKeyword));
            return colKeyword;
        }

        ColumnInfo addGraphColumn(String columnAlias)
        {
            ColumnInfo colGraph = addObjectIdColumn(columnAlias);
            colGraph.setFk(new GraphForeignKey(_fps));
            colGraph.setIsUnselectable(true);
            return colGraph;
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
        public SQLFragment getFromSQL()
        {
            SQLFragment sqlFlowData = new SQLFragment();

            sqlFlowData.append("SELECT " + _expDataAlias + ".*,");
            sqlFlowData.append("_flowObject").append("." + _flowObject.getColumn("rowid").getName() + " AS objectid");
            if (null != _flowObject.getColumn("compid"))
            {
                sqlFlowData.append(",");
                sqlFlowData.append("_flowObject").append("." + _flowObject.getColumn("compid").getName() + ",");
                sqlFlowData.append("_flowObject").append("." + _flowObject.getColumn("fcsid").getName() + ",");
                sqlFlowData.append("_flowObject").append("." + _flowObject.getColumn("scriptid").getName() + "");
            }
            sqlFlowData.append("\nFROM ");
            sqlFlowData.append(_expData, _expDataAlias);
            sqlFlowData.append(" INNER JOIN " );
            sqlFlowData.append(_flowObject, "_flowObject");
            sqlFlowData.append(" ON " +
                    _expDataAlias + "." + _expData.getColumn("rowid").getName() + "=_flowObject." + _flowObject.getColumn("dataid").getName() +
                    " AND " +
                    _expDataAlias + ".container=" + "=_flowObject." + _flowObject.getColumn("container").getName()
                    );
            sqlFlowData.append("\n");
            sqlFlowData.append("WHERE ");
            sqlFlowData.append("_flowObject." + _flowObject.getColumn("typeid").getName() + "=" + _type.getObjectType().getTypeId());
            sqlFlowData.append(" AND ");
            sqlFlowData.append("_flowObject." + _flowObject.getColumn("container").getName() + "='" + getContainer().getId() + "'");
            return sqlFlowData;
        }

        @Override
        public boolean hasPermission(User user, int perm)
        {
            return _expData.hasPermission(user, perm);
        }

        /* ExpDataTable */
        public void populate()
        {
            _expData.populate();
            for (ColumnInfo col : _expData.getColumns())
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

        public ColumnInfo addMaterialInputColumn(String alias, SamplesSchema schema, String inputRole, ExpSampleSet sampleSet)
        {
            ColumnInfo col = _expData.addMaterialInputColumn(alias,schema,inputRole,sampleSet);
            return addExpColumn(col);
        }

        public ColumnInfo addDataInputColumn(String alias, String role)
        {
            ColumnInfo col = _expData.addDataInputColumn(alias, role);
            return addExpColumn(col);
        }

        public ColumnInfo addInputRunCountColumn(String alias)
        {
            ColumnInfo col = _expData.addInputRunCountColumn(alias);
            return addExpColumn(col);
        }

        /* ExpTable */

        public Container getContainer()
        {
            return _expData.getContainer();
        }

        public void setContainerFilter(ContainerFilter filter)
        {
            _expData.setContainerFilter(filter);
        }

        public ContainerFilter getContainerFilter()
        {
            return _expData.getContainerFilter();
        }

        public boolean hasDefaultContainerFilter()
        {
            return _expData.hasDefaultContainerFilter();
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
            for (ColumnInfo info : getColumns())
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


    class FastFlowDataTable extends AbstractTableInfo implements ExpDataTable
    {
        //final ExpDataTable _expData;
        final ExpDataTable _expData;
        final TableInfo _flowObject;
        final FlowDataType _type;
        FlowPropertySet _fps;

        // ExpDataTable support
        TableEditHelper _editHelper = null;
        final SimpleFilter _filter = new SimpleFilter();
        ExpExperiment _experiment = null;
        ExpRun _run = null;
        boolean _runSpecified = false;
        Container _c = null;

        FastFlowDataTable(String name, FlowDataType type)
        {
            super(getDbSchema());
            setName(name);
            _expData = ExperimentService.get().createDataTable(name, FlowSchema.this);
            _expData.setDataType(type);
            _flowObject = DbSchema.get("flow").getTable("object");
            _type = type;

            _fps = new FlowPropertySet(FlowSchema.this.getContainer());
        }

        ColumnInfo addStatisticColumn(String columnAlias)
        {
            ColumnInfo colStatistic = addObjectIdColumn(columnAlias);
            colStatistic.setFk(new StatisticForeignKey(_fps, _type));
            colStatistic.setIsUnselectable(true);
            addMethod(columnAlias, new StatisticMethod(colStatistic));
            return colStatistic;
        }

        ColumnInfo addKeywordColumn(String columnAlias)
        {
            ColumnInfo colKeyword = addObjectIdColumn(columnAlias);
            colKeyword.setFk(new KeywordForeignKey(_fps));
            colKeyword.setIsUnselectable(true);
            addMethod("Keyword", new KeywordMethod(colKeyword));
            return colKeyword;
        }

        ColumnInfo addGraphColumn(String columnAlias)
        {
            ColumnInfo colGraph = addObjectIdColumn(columnAlias);
            colGraph.setFk(new GraphForeignKey(_fps));
            colGraph.setIsUnselectable(true);
            return colGraph;
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


        ColumnInfo addBackgroundColumn(String columnAlias)
        {
            ColumnInfo colBackground = addObjectIdColumn(columnAlias);
            colBackground.setFk(new BackgroundForeignKey(FlowSchema.this, _fps, _type));
            colBackground.setIsUnselectable(true);
            addMethod(columnAlias, new BackgroundMethod(FlowSchema.this, colBackground));
            return colBackground;
        }


        public String getSelectName()
        {
            return null;
        }

        /* TableInfo */
        public SQLFragment getFromSQL()
        {
            assert _container != null;
            assert _container.getId().equals(getContainer().getId());
            String name = getFastFlowObjectTableName(_container, _type.getObjectType().getTypeId());

            SQLFragment where = new SQLFragment();
            SQLFragment filter = _filter.getSQLFragment(getSqlDialect());
            String and = " WHERE ";
            if (filter.getFilterText().length() > 0)
            {
                where.append(" ").append(filter);
                and = " AND ";
            }
//            if (null != _type)
//            {
//                sqlFlowData.append(and).append("TypeId = ").append(_type.getObjectType().getTypeId());
//                and = " AND ";
//            }
            if (null != _experiment)
            {
                where.append(and).append("ExperimentId = ").append(_experiment.getRowId());
                and = " AND ";
            }
            if (_runSpecified)
            {
                if (_run == null)
                {
                    where.append(and).append("RunId IS NULL");
                }
                else
                {
                    where.append(and).append("RunId = ").append(_run.getRowId());
                }
                //and = " AND ";
            }

            SQLFragment sqlFlowData;
            sqlFlowData = new SQLFragment("\n-- <" + this.getClass().getSimpleName() + " name='" + _type.getName() + "'>\n");
            sqlFlowData.append("SELECT * FROM ");
            sqlFlowData.append(name);
            sqlFlowData.append(where);
            sqlFlowData.append("\n-- </" + this.getClass().getSimpleName() + ">\n");
            return sqlFlowData;
        }


        /* ExpDataTable */
        public void populate()
        {
            _expData.populate();
            throw new UnsupportedOperationException();
        }

        public void setExperiment(ExpExperiment experiment)
        {
            _experiment = experiment;
        }

        public ExpExperiment getExperiment()
        {
            return _experiment;
        }

        public void setRun(ExpRun run)
        {
            _runSpecified = true;
            _run = run;
        }

        public ExpRun getRun()
        {
           return _run;
        }

        public void setDataType(DataType type)
        {
            assert type == _type;
        }

        public void setContainerFilter(ContainerFilter filter)
        {
            _expData.setContainerFilter(filter);
        }

        public ContainerFilter getContainerFilter()
        {
            return _expData.getContainerFilter();
        }

        public boolean hasDefaultContainerFilter()
        {
            return _expData.hasDefaultContainerFilter();
        }

        public DataType getDataType()
        {
            return _type;
        }

        public ColumnInfo addMaterialInputColumn(String alias, SamplesSchema schema, String inputRole, ExpSampleSet sampleSet)
        {
            ColumnInfo col = _expData.addMaterialInputColumn(alias,schema,inputRole,sampleSet);
            col.setParentTable(this);
            return addColumn(col);
        }

        public ColumnInfo addDataInputColumn(String alias, String role)
        {
//UNDONE
            assert false;
            ColumnInfo col = _expData.addDataInputColumn(alias, role);
            return addExpColumn(col);
        }

        public ColumnInfo addInputRunCountColumn(String alias)
        {
            ColumnInfo col = _expData.addInputRunCountColumn(alias);
            col.setParentTable(this);
            return addColumn(col);
        }

        /* ExpTable */

        public void setContainer(Container container)
        {
            _container = container;
        }

        public Container getContainer()
        {
            return _container;
        }

        public ColumnInfo addColumn(Column column)
        {
            return addColumn(column.toString(), column);
        }

        public ColumnInfo addColumn(String alias, Column column)
        {
            ColumnInfo col =  createColumn(alias, column);
            addColumn(col);
            return col;
        }

        public ColumnInfo getColumn(Column column)
        {
            for (ColumnInfo info : getColumns())
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
            ColumnInfo col = _expData.createColumn(alias,column);
            col.setParentTable(this);
            return col;
        }
        
        public ColumnInfo createPropertyColumn(String alias)
        {
            ColumnInfo col = _expData.createPropertyColumn(alias);
            throw new UnsupportedOperationException();
        }

        public void addCondition(SQLFragment condition, String... columnNames)
        {
            _filter.addWhereClause(condition.getSQL(), condition.getParams().toArray(), columnNames);
        }

        public void addRowIdCondition(SQLFragment rowidCondition)
        {
            _expData.addRowIdCondition(rowidCondition);
            throw new UnsupportedOperationException();
        }

        public void addLSIDCondition(SQLFragment lsidCondition)
        {
            _expData.addLSIDCondition(lsidCondition);
            throw new UnsupportedOperationException();
        }

        public void setEditHelper(TableEditHelper helper)
        {
            _editHelper = helper;
        }

        public boolean hasPermission(User user, int perm)
        {
            if (_editHelper != null)
                return _editHelper.hasPermission(user, perm);
            return false;
        }

        public ActionURL delete(User user, ActionURL srcURL, QueryUpdateForm form) throws Exception
        {
            if (_editHelper != null)
            {
                return _editHelper.delete(user, srcURL, form);
            }
            throw new UnsupportedOperationException();
        }

        public ColumnInfo addPropertyColumns(String domainDescription, PropertyDescriptor[] pds, QuerySchema schema)
        {
            ColumnInfo col = _expData.addPropertyColumns(domainDescription, pds, schema);
            throw new UnsupportedOperationException();
        }
    }


    public class FlowDataTable extends FastFlowDataTable
    {
        FlowDataTable(String name, FlowDataType type)
        {
            super(name, type);
        }
    }
    

    public FlowDataTable createDataTable(String name, final FlowDataType type)
    {
        FlowDataTable ret = new FlowDataTable(name, type);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.RowId).setIsHidden(true);
        ret.addColumn(ExpDataTable.Column.LSID).setIsHidden(true);
        ColumnInfo flag = ret.addColumn(ExpDataTable.Column.Flag);
        if (type != null)
            flag.setDescription(type.getLabel() + " Flag");
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
        colRun.setFk(new LookupForeignKey(PageFlowUtil.urlFor(RunController.Action.showRun, getContainer()), FlowParam.runId,
                FlowTableType.Runs.toString(), "RowId", "Name")
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
            List<FieldKey> ret = QueryService.get().getDefaultVisibleColumns(_table.getColumns());
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
        final private ColumnInfo _colBackground;
        final private ColumnInfo _colGraph;

        public DeferredFCSAnalysisVisibleColumns(ExpDataTable table, ColumnInfo colStatistic, ColumnInfo colGraph, ColumnInfo colBackground)
        {
            _table = table;
            _colStatistic = colStatistic;
            _colBackground = colBackground;
            _colGraph = colGraph;
        }

        public Iterator<FieldKey> iterator()
        {
            Collection<FieldKey> ret = new LinkedHashSet<FieldKey>();
            ret.addAll(QueryService.get().getDefaultVisibleColumns(_table.getColumns()));
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

            if (_colBackground != null)
            {
                lookup = _colBackground.getFk().getLookupTableInfo();
                if (lookup != null)
                {
                    int count = 0;
                    FieldKey keyStatistic = new FieldKey(null, _colBackground.getName());
                    for (FieldKey key : lookup.getDefaultVisibleColumns())
                    {
                        ret.add(FieldKey.fromParts(keyStatistic, key));
                        if (++count > 3)
                            break;
                    }
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

    public FlowDataTable createFCSFileTable(String name)
    {
        final FlowDataTable ret = createDataTable(name, FlowDataType.FCSFile);
        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        final ColumnInfo colKeyword = ret.addKeywordColumn("Keyword");
        ExpSampleSet ss = null;
        if (_protocol != null)
        {
            ss = _protocol.getSampleSet();
        }
        ColumnInfo colMaterialInput = ret.addMaterialInputColumn("Sample", new SamplesSchema(getUser(), getContainer()), ExpMaterialRunInput.DEFAULT_ROLE, ss);
        if (ss == null)
        {
            colMaterialInput.setIsHidden(true);
        }

        ret.setDefaultVisibleColumns(new DeferredFCSFileVisibleColumns(ret, colKeyword));
        return ret;
    }


    public ExpDataTable createFCSAnalysisTable(String name, FlowDataType type)
    {
        if (null != DbSchema.get("flow").getTable("object").getColumn("compid"))
            return createFCSAnalysisTableNEW(name, type);

        FlowDataTable ret = createDataTable(name, type);
        ColumnInfo colAnalysisScript = ret.addDataInputColumn("AnalysisScript", InputRole.AnalysisScript.toString());
        colAnalysisScript.setFk(new LookupForeignKey(PageFlowUtil.urlFor(AnalysisScriptController.Action.begin, getContainer()),
                FlowParam.scriptId.toString(), FlowTableType.AnalysisScripts.toString(), "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createAnalysisScriptTable("Lookup", true);
            }
        });
        ColumnInfo colCompensationMatrix = ret.addDataInputColumn("CompensationMatrix", InputRole.CompensationMatrix.toString());
        colCompensationMatrix.setFk(new LookupForeignKey(PageFlowUtil.urlFor(CompensationController.Action.showCompensation, getContainer()), FlowParam.compId.toString(),
                FlowTableType.CompensationMatrices.toString(), "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createCompensationMatrixTable("Lookup");
            }
        });

        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        if (getExperiment() != null)
        {
            ret.setExperiment(ExperimentService.get().getExpExperiment(getExperiment().getLSID()));
        }
        ColumnInfo colStatistic = ret.addStatisticColumn("Statistic");
        FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
        ColumnInfo colBackground = null;
        if (protocol != null && protocol.hasICSMetadata())
            colBackground = ret.addBackgroundColumn("Background");
        ColumnInfo colGraph = ret.addGraphColumn("Graph");
        ColumnInfo colFCSFile = ret.addDataInputColumn("FCSFile", InputRole.FCSFile.toString());
        colFCSFile.setFk(new LookupForeignKey(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()),
                FlowParam.wellId.toString(),
                FlowTableType.FCSFiles.toString(), "RowId", "Name") {
                public TableInfo getLookupTableInfo()
                {
                    return detach().createFCSFileTable("FCSFile");
                }
            });
        ret.setDefaultVisibleColumns(new DeferredFCSAnalysisVisibleColumns(ret, colStatistic, colGraph, colBackground));
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

        ret.setDetailsURL(new DetailsURL(PageFlowUtil.urlFor(WellController.Action.showWell, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        if (getExperiment() != null)
        {
            ret.setExperiment(ExperimentService.get().getExpExperiment(getExperiment().getLSID()));
        }

        ColumnInfo colStatistic = ret.addStatisticColumn("Statistic");

        FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
        ColumnInfo colBackground = null;
        if (protocol != null && protocol.hasICSMetadata())
            colBackground = ret.addBackgroundColumn("Background");

        ColumnInfo colGraph = ret.addGraphColumn("Graph");

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

        ret.setDefaultVisibleColumns(new DeferredFCSAnalysisVisibleColumns(ret, colStatistic, colGraph, colBackground));
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

    public FlowDataTable createAnalysisScriptTable(String alias, boolean includePrivate)
    {
        FlowDataTable ret = createDataTable(alias, FlowDataType.Script);
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

    public ExpExperimentTable createAnalysesTable(String name)
    {
        ExpExperimentTable ret = ExperimentService.get().createExperimentTable(name, new ExpSchema(getUser(), getContainer()));
        ret.populate();
        FlowProtocol compensationProtocol = FlowProtocolStep.calculateCompensation.getForContainer(getContainer());
        FlowProtocol analysisProtocol = FlowProtocolStep.analysis.getForContainer(getContainer());
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

        DetailsURL detailsUrl = new DetailsURL(PageFlowUtil.urlFor(RunController.Action.showRuns, getContainer()).addParameter(FlowQueryView.DATAREGIONNAME_DEFAULT + ".sort", "ProtocolStep"),
                Collections.singletonMap(FlowParam.experimentId.toString(), ExpExperimentTable.Column.RowId.toString()));
        ret.setDetailsURL(detailsUrl);
        SQLFragment lsidCondition = new SQLFragment("LSID <> ");
        lsidCondition.appendStringLiteral(FlowExperiment.getExperimentRunExperimentLSID(getContainer()));
        ret.addCondition(lsidCondition);
        return ret;
    }


    protected QuerySettings createQuerySettings(String dataRegionName)
    {
        return new FlowQuerySettings(dataRegionName);    //To change body of overridden methods use File | Settings | File Templates.
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
        ret.setName(alias);
        ret.addWrapColumn(ret.getRealTable().getColumn("Name"));
        ExpDataTable fcsAnalysisTable = createFCSAnalysisTable("fcsAnalysis", FlowDataType.FCSAnalysis);
//        FlowPropertySet fps = new FlowPropertySet(fcsAnalysisTable);
        FlowPropertySet fps = new FlowPropertySet(getContainer());
        filterTable(ret, fps.getStatistics());
        return ret;
    }

    private TableInfo createKeywordsTable(String alias)
    {
        FilteredTable ret = new FilteredTable(FlowManager.get().getTinfoAttribute());
        ret.setName(alias);
        ret.addWrapColumn(ret.getRealTable().getColumn("Name"));
        ExpDataTable fcsFilesTable = createFCSFileTable("fcsFiles");
//        FlowPropertySet fps = new FlowPropertySet(fcsFilesTable);
        FlowPropertySet fps = new FlowPropertySet(getContainer());
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



    // need an an object to track the temptable (don't want to use String, too confusing)
    static class TempTableToken
    {
        TempTableToken(String name)
        {
            this.name = name;
        }
        String name;
    }


    /*
     * Caching needs to be improved
     * need better notification of changes per container
     */

    Map<String,TempTableToken> instanceCache = new HashMap<String,TempTableToken>();
    static Cache staticCache = Cache.getShared();

    String getFastFlowObjectTableName(Container c, int typeid)
    {
        TempTableToken tok = null;
        boolean tx = FlowManager.get().getSchema().getScope().isTransactionActive();
        HttpServletRequest r = HttpView.currentRequest();
        String tkey = "" + FlowManager.get().flowObjectModificationCount.get();
        String attr = FlowSchema.class.getName() + "." + typeid + "." + tkey + ".flow.object$" + c.getId();

        tok = instanceCache.get(attr);
        if (tok != null)
            return tok.name;
        tok = r == null ? null : (TempTableToken)r.getAttribute(attr);
        if (tok != null)
        {
            instanceCache.put(attr, tok);
            return tok.name;
        }
        if (!tx)
            tok = (TempTableToken)staticCache.get(attr);
        if (tok == null)
        {
            String name = createFastFlowObjectTableName(c, typeid);
            tok = new TempTableToken(name);
            TempTableTracker.track(FlowManager.get().getSchema(), name, tok);
            if (!tx)
                staticCache.put(attr, tok, 10 * Cache.SECOND);
        }
        instanceCache.put(attr, tok);
        if (null != r)
            r.setAttribute(attr, tok);
        return tok.name;
    }


    /** CONSIDER JOIN ObjectId from exp.Objects */ 
    String createFastFlowObjectTableName(Container c, int typeid)
    {
        try
        {
            long begin = System.currentTimeMillis();
            DbSchema flow = FlowManager.get().getSchema();
            String shortName = "flowObject" + GUID.makeHash(); 
            String name = flow.getSqlDialect().getGlobalTempTablePrefix() + shortName;
            Table.execute(flow,
                "SELECT \n" +
                "    exp.data.RowId,\n" +
                "    exp.data.LSID,\n" +
                "    exp.data.Name,\n" +
                "    exp.data.CpasType,\n" +
                "    exp.data.SourceApplicationId,\n" +
                "    exp.data.DataFileUrl,\n" +
                "    exp.data.RunId,\n" +
                "    exp.data.Created,\n" +
                "    exp.data.Container,\n" +
                "    flow.object.RowId AS objectid,\n" +
                "    flow.object.TypeId,\n" +
                "    flow.object.compid,\n" +
                "    flow.object.fcsid,\n" +
                "    flow.object.scriptid,\n" +
                "    exp.RunList.ExperimentId\n" +
                "INTO " +  name + "\n" +
                "FROM exp.data\n" +
                "    INNER JOIN flow.object ON exp.Data.RowId=flow.object.DataId\n" +
                "    LEFT OUTER JOIN exp.RunList ON exp.RunList.ExperimentRunid = exp.Data.RunId\n" +
                "WHERE flow.Object.container = ? and TypeId = ?",
               new Object[] {c.getId(), typeid}
            );
            String create =
//                    "CREATE INDEX ix_" + shortName + " ON " + name + " (TypeId,ExperimentId);\n" +
                    "CREATE UNIQUE INDEX ix_" + shortName + "_rowid ON " + name + " (RowId);\n" +
                    "CREATE UNIQUE INDEX ix_" + shortName + "_objectid ON " + name + " (ObjectId);\n";
            Table.execute(flow, create, null);
            long end = System.currentTimeMillis();
            return name;
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }


    String getBackgroundJunctionTableName(Container c)
    {
        TempTableToken tok = null;
        boolean tx = FlowManager.get().getSchema().getScope().isTransactionActive();
        HttpServletRequest r = HttpView.currentRequest();
        String tkey = "" + FlowManager.get().flowObjectModificationCount.get();
        String attr = FlowSchema.class.getName() + "." + tkey + ".flow.bgjunction$" + c.getId();

        tok = instanceCache.get(attr);
        if (tok != null)
            return tok.name;
        tok = r == null ? null : (TempTableToken)r.getAttribute(attr);
        if (tok != null)
        {
            instanceCache.put(attr, tok);
            return tok.name;
        }
        if (!tx)
            tok = (TempTableToken)staticCache.get(attr);
        if (tok == null)
        {
            String name = createBackgroundJunctionTableName(c);
            if (null == name)
                return null;
            tok = new TempTableToken(name);
            TempTableTracker.track(FlowManager.get().getSchema(), name, tok);
            if (!tx)
                staticCache.put(attr, tok, 10 * Cache.SECOND);
        }
        instanceCache.put(attr, tok);
        if (null != r)
            r.setAttribute(attr, tok);
        return tok.name;
    }


    String createBackgroundJunctionTableName(Container c)
    {
        try
        {
            long begin = System.currentTimeMillis();
            DbSchema flow = FlowManager.get().getSchema();
            String shortName = "flowJunction" + GUID.makeHash();
            String name = flow.getSqlDialect().getGlobalTempTablePrefix() + shortName;
            
            ICSMetadata ics = _protocol.getICSMetadata();

            // BACKGROUND            
            FlowDataTable bg = (FlowDataTable)detach().getTable(FlowTableType.FCSAnalyses.toString());
            bg.addObjectIdColumn("objectid");
            Set<FieldKey> allColumns = new TreeSet<FieldKey>(ics.getMatchColumns());
            for (FilterInfo f : ics.getBackgroundFilter())
                allColumns.add(f.getField());
            Map<FieldKey,ColumnInfo> bgMap = QueryService.get().getColumns(bg, allColumns);
            if (bgMap.size() != allColumns.size())
                return null;
            ArrayList<ColumnInfo> bgFields = new ArrayList<ColumnInfo>();
            bgFields.add(bg.getColumn("objectid"));
            bgFields.addAll(bgMap.values());
            SimpleFilter filter = new SimpleFilter();
            for (FilterInfo f : ics.getBackgroundFilter())
                filter.addCondition(bgMap.get(f.getField()), f.getValue(), f.getOp());
            SQLFragment bgSQL = Table.getSelectSQL(bg, bgFields, null, null);
            if (filter.getClauses().size() > 0)
            {
                Map<String, ColumnInfo> columnMap = Table.createColumnMap(bg, bgFields);
                SQLFragment filterFrag = filter.getSQLFragment(flow.getSqlDialect(), columnMap);
                SQLFragment t = new SQLFragment("SELECT * FROM (");
                t.append(bgSQL);
                t.append(") _filter_ " );
                t.append(filterFrag);
                bgSQL = t;
            }

            // FOREGROUND
            FlowDataTable fg = (FlowDataTable)detach().getTable(FlowTableType.FCSAnalyses.toString());
            fg.addObjectIdColumn("objectid");
            Set<FieldKey> setMatchColumns = new HashSet<FieldKey>(ics.getMatchColumns());
            Map<FieldKey,ColumnInfo> fgMap = QueryService.get().getColumns(fg, setMatchColumns);
            if (fgMap.size() != setMatchColumns.size())
                return null;
            ArrayList<ColumnInfo> fgFields = new ArrayList<ColumnInfo>();
            fgFields.add(fg.getColumn("objectid"));
            fgFields.addAll(fgMap.values());
            SQLFragment fgSQL = Table.getSelectSQL(fg, fgFields, null, null);

            SQLFragment selectInto = new SQLFragment();
            selectInto.append("SELECT F.objectid as fg, B.objectid as bg INTO " + name + "\n");
            selectInto.append("FROM (").append(fgSQL).append(") AS F INNER JOIN (").append(bgSQL).append(") AS B");
            selectInto.append(" ON " );
            String and = "";
            for (FieldKey m : setMatchColumns)
            {
                selectInto.append(and);
                if (null == fgMap.get(m) || null == bgMap.get(m))
                    return null;
                selectInto.append("F.").append(fgMap.get(m).getAlias()).append("=B.").append(bgMap.get(m).getAlias());
                and = " AND ";
            }
            Table.execute(flow, selectInto);
            String create =
                    "CREATE INDEX ix_" + shortName + "_fg ON " + name + " (fg);\n" +
                    "CREATE INDEX ix_" + shortName + "_bg ON " + name + " (bg);\n";
            Table.execute(flow, create, null);

            long end = System.currentTimeMillis();
            return name;
        }
        catch (SQLException x)
        {
            throw new RuntimeSQLException(x);
        }
    }
}
