/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.cache.StringKeyCache;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.FilterInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MaterializedQueryHelper;
import org.labkey.api.data.NullColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyColumn;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpMaterialRunInput;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpExperimentTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.exp.query.SamplesSchema;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.RowIdForeignKey;
import org.labkey.api.query.SchemaKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.FileLinkDisplayColumn;
import org.labkey.api.study.assay.SpecimenForeignKey;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.FlowAssayProvider;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.reports.FlowReport;
import org.labkey.flow.reports.FlowReportManager;
import org.labkey.flow.view.FlowQueryView;
import org.springframework.validation.BindException;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;


public class FlowSchema extends UserSchema
{
    static public final String SCHEMANAME = "flow";
    static public final String SCHEMA_DESCR = "Contains data about flow cytometry experiment runs";

    // Column name constants
    public static final String FCSFILE_NAME = "FCSFile";
    public static final FieldKey FCSFILE_FIELDKEY = FieldKey.fromParts(FCSFILE_NAME);

    public static final String ORIGINAL_FCSFILE_NAME = "OriginalFCSFile";
    public static final FieldKey ORIGINAL_FCSFILE_FIELDKEY = FieldKey.fromParts(ORIGINAL_FCSFILE_NAME);
    
    public static final FieldKey SPECIMENID_FIELDKEY = FieldKey.fromParts(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
    public static final FieldKey PARTICIPANTID_FIELDKEY = FieldKey.fromParts(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
    public static final FieldKey VISITID_FIELDKEY = FieldKey.fromParts(AbstractAssayProvider.VISITID_PROPERTY_NAME);
    public static final FieldKey DATE_FIELDKEY = FieldKey.fromParts(AbstractAssayProvider.DATE_PROPERTY_NAME);
    public static final FieldKey TARGET_STUDY_FIELDKEY = FieldKey.fromParts(AbstractAssayProvider.TARGET_STUDY_PROPERTY_NAME);

    private FlowExperiment _experiment;
    private FlowRun _run;
//    private FlowScript _script;
    private FlowProtocol _protocol = null;

    public FlowSchema(User user, Container container)
    {
        this(user, container, FlowProtocol.getForContainer(container));
    }

    public FlowSchema(ViewContext context)
    {
        this(context.getUser(), context.getContainer(), context.getActionURL(), context.getRequest(), context.getContainer());
    }

    public FlowSchema(User user, Container container, @Nullable ActionURL url, @Nullable HttpServletRequest request, Container actionContainer)
    {
        this(user, container, FlowProtocol.getForContainer(container));
        setExperiment(FlowExperiment.fromURL(url, request, actionContainer, getUser()));
        setRun(FlowRun.fromURL(url, actionContainer, getUser()));
//        setScript(FlowScript.fromURL(url, request));
    }

    // FlowSchema.createView()
    private FlowSchema(ViewContext context, FlowSchema from)
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
            setExperiment(FlowExperiment.fromURL(context.getActionURL(), context.getRequest(),context.getContainer(), getUser()));
//        if (null == _script)
//            setScript(FlowScript.fromURL(context.getActionURL(), context.getRequest()));
        if (null == _run)
            setRun(FlowRun.fromURL(context.getActionURL(), context.getContainer(), getUser()));
    }

    private FlowSchema(User user, Container container, FlowProtocol protocol)
    {
        super(SCHEMANAME, SCHEMA_DESCR, user, container, ExperimentService.get().getSchema());
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
            AbstractTableInfo table = (AbstractTableInfo)getTable(type);
            table.setDescription(type.getDescription());
            return table;
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
                return createFCSAnalysisTable(type.toString(), FlowDataType.FCSAnalysis, true);
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
            case Graphs:
                return createGraphsTable(type.toString());
        }
        return null;
    }

    public Set<String> getVisibleTableNames()
    {
        Set<String> ret = new HashSet<>();
        for (FlowTableType tt : FlowTableType.values())
        {
            if (!tt.isHidden())
                ret.add(tt.toString());
        }
        return ret;
    }

    public QueryDefinition getQueryDef(FlowTableType qt)
    {
        return getQueryDef(qt.name());
    }

    public Set<String> getTableNames()
    {
        Set<String> ret = new LinkedHashSet<>();
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

    @Override
    public QueryView createView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new FlowQueryView(new FlowSchema(context, this), (FlowQuerySettings) settings, errors);
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

        FlowProtocol protocol = getProtocol();
        if (protocol != null)
        {
            ret.setProtocol(_protocol.getProtocol());
        }

        ret.addColumn(ExpRunTable.Column.RowId);
        DetailsURL detailsURL = new DetailsURL(new ActionURL(RunController.ShowRunAction.class, _container), Collections.singletonMap(FlowParam.runId.toString(), ExpRunTable.Column.RowId.toString()));
        ret.setDetailsURL(detailsURL);
        if (type == null || type == FlowDataType.FCSFile || type == FlowDataType.FCSAnalysis)
        {
            ColumnInfo flag = ret.addColumn(ExpRunTable.Column.Flag);
            if (type != null)
                flag.setDescription(type.getLabel() + " Flag");
        }
        ret.addColumn(ExpRunTable.Column.Name).setURL(detailsURL);
        ret.addColumn(ExpRunTable.Column.Created);
        ret.addColumn(ExpRunTable.Column.CreatedBy);

        ColumnInfo containerCol = ret.addColumn(ExpRunTable.Column.Folder);
        containerCol.setHidden(true);
        ContainerForeignKey.initColumn(containerCol, this, null);

        ColumnInfo colLSID = ret.addColumn(ExpRunTable.Column.LSID);
        colLSID.setHidden(true);

        ret.addColumn(ExpRunTable.Column.FilePathRoot).setHidden(true);

        PropertyColumn osp = new PropertyColumn(FlowProperty.OriginalSourcePath.getPropertyDescriptor(), colLSID, getContainer(), getUser(), true);
        osp.setHidden(true);
        ret.addColumn(osp);

        ret.addColumn(ExpRunTable.Column.ProtocolStep);

        ColumnInfo analysisFolder = ret.addColumn(ExpRunTable.Column.RunGroups);
        analysisFolder.setLabel("Analysis Folder");
        ActionURL url = new ActionURL(RunController.ShowRunsAction.class, getContainer()).addParameter(FlowQueryView.DATAREGIONNAME_DEFAULT + ".sort", "ProtocolStep");
        analysisFolder.setURL(StringExpressionFactory.create(url.getLocalURIString() + "&experimentId=${experimentId}"));

        if (type != FlowDataType.FCSFile)
        {
            ColumnInfo colAnalysisScript;
            colAnalysisScript = ret.addDataInputColumn("AnalysisScript", InputRole.AnalysisScript.toString());
            colAnalysisScript.setFk(new LookupForeignKey(new ActionURL(AnalysisScriptController.BeginAction.class, getContainer()),
                    FlowParam.scriptId.toString(),
                    FlowTableType.AnalysisScripts.toString(),
                    "RowId", "Name"){
                public TableInfo getLookupTableInfo()
                {
                    return detach().createAnalysisScriptTable("Lookup", true);
                }
            });

            ColumnInfo colWorkspace = ret.addDataInputColumn("Workspace", InputRole.Workspace.toString());
            ExpDataTable workspacesTable = ExperimentService.get().createDataTable("Datas", this);
            workspacesTable.setPublicSchemaName(ExpSchema.SCHEMA_NAME);
            workspacesTable.populate();
            colWorkspace.setFk(new QueryForeignKey(workspacesTable, null, "RowId", "Name"));
            colWorkspace.setHidden(true);
        }

        if (type != FlowDataType.CompensationMatrix && type != FlowDataType.FCSFile)
        {
            ColumnInfo colCompensationMatrix;
            colCompensationMatrix= ret.addDataInputColumn("CompensationMatrix", InputRole.CompensationMatrix.toString());
            colCompensationMatrix.setFk(new LookupForeignKey(null, null,
                    FlowTableType.CompensationMatrices.toString(),
                    "RowId", "Name") {
                public TableInfo getLookupTableInfo()
                {
                    return detach().createCompensationMatrixTable("Lookup");
                }
            });
        }

        if (type == null || type == FlowDataType.FCSFile)
        {
            PropertyDescriptor pd = FlowProperty.TargetStudy.getPropertyDescriptor();
            PropertyColumn colTargetStudy = new PropertyColumn(pd, colLSID, getContainer(), getUser(), true);
            colTargetStudy.setLabel(AbstractAssayProvider.TARGET_STUDY_PROPERTY_CAPTION);
            colTargetStudy.setDisplayColumnFactory(_targetStudyDisplayColumnFactory);
            ret.addColumn(colTargetStudy);
        }

        ret.addDataCountColumn("WellCount", InputRole.FCSFile.toString());
        addDataCountColumn(ret, "FCSFileCount", ObjectType.fcsKeywords);
        addDataCountColumn(ret, "CompensationControlCount", ObjectType.compensationControl);
        addDataCountColumn(ret, "FCSAnalysisCount", ObjectType.fcsAnalysis);

        return ret;
    }

    private static final DisplayColumnFactory _targetStudyDisplayColumnFactory = new DisplayColumnFactory()
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new DataColumn(colInfo)
            {
                @Override
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    String targetStudyId = (String)getBoundColumn().getValue(ctx);
                    if (targetStudyId != null && targetStudyId.length() > 0)
                    {
                        Container c = ContainerManager.getForId(targetStudyId);
                        if (c != null)
                        {
                            Study study = StudyService.get().getStudy(c);
                            if (study != null)
                            {
                                out.write("<a href=\"");
                                out.write(PageFlowUtil.filter(PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(c)));
                                out.write("\">");
                                out.write(study.getLabel().replaceAll(" ", "&nbsp;"));
                                out.write("</a>");
                            }
                        }
                    }
                }
            };
        }
    };


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
            super(getDbSchema(), name);
            _expDataAlias = "_expdata_";
            _expData = ExperimentService.get().createDataTable(name, FlowSchema.this);
            _flowObject = FlowManager.get().getTinfoObject();
            _type = type;
            _fps = new FlowPropertySet(_expData);
        }

        ColumnInfo addStatisticColumn(String columnAlias)
        {
            ColumnInfo colStatistic = addObjectIdColumn(columnAlias);
            colStatistic.setFk(new StatisticForeignKey(getContainer(), _fps, _type));
            colStatistic.setIsUnselectable(true);
            addMethod(columnAlias, new StatisticMethod(getContainer(), colStatistic));
            return colStatistic;
        }

        ColumnInfo addKeywordColumn(String columnAlias)
        {
            ColumnInfo colKeyword = addObjectIdColumn(columnAlias);
            colKeyword.setFk(new KeywordForeignKey(FlowSchema.this, _fps));
            colKeyword.setIsUnselectable(true);
            addMethod("Keyword", new KeywordMethod(getContainer(), colKeyword));
            return colKeyword;
        }

        ColumnInfo addGraphColumn(String columnAlias)
        {
            ColumnInfo colGraph = addObjectIdColumn(columnAlias);
            colGraph.setFk(new GraphForeignKey(getContainer(), _fps));
            colGraph.setIsUnselectable(true);
            return colGraph;
        }

        ColumnInfo addObjectIdColumn(String name)
        {
            ColumnInfo underlyingColumn = _flowObject.getColumn("rowid");
            ExprColumn ret = new ExprColumn(this, name, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".objectid"), underlyingColumn.getJdbcType());
            ret.copyAttributesFrom(underlyingColumn);
            addColumn(ret);
            return ret;
        }

        @Override
        public UserSchema getUserSchema()
        {
            return FlowSchema.this;
        }

        @Override
        public void addAllowablePermission(Class<? extends Permission> permission)
        {
            _expData.addAllowablePermission(permission);
        }

        ColumnInfo addExpColumn(ColumnInfo underlyingColumn)
        {
            ExprColumn ret = new ExprColumn(this, underlyingColumn.getAlias(), underlyingColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS), underlyingColumn.getJdbcType());
            ret.copyAttributesFrom(underlyingColumn);
            ret.setHidden(underlyingColumn.isHidden());
            if (underlyingColumn.getFk() instanceof RowIdForeignKey)
                ret.setFk(new RowIdForeignKey(ret));
            addColumn(ret);
            return ret;
        }

        /* TableInfo */
        @NotNull
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
                sqlFlowData.append("_flowObject").append("." + _flowObject.getColumn("scriptid").getName() + ",");
                sqlFlowData.append("_flowObject").append("." + _flowObject.getColumn("uri").getName() + "");
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
            sqlFlowData.append("_flowObject.").append(_flowObject.getColumn("container").getName()).append("='").append(getContainer().getId()).append("'");
            return sqlFlowData;
        }

        @Override
        public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
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

        public void setContainerFilter(@NotNull ContainerFilter filter)
        {
            checkLocked();
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

        public void addCondition(SQLFragment condition, FieldKey... fieldKeys)
        {
            // NOTE: since this is being pushed down we can't use object id here
            _expData.addCondition(condition, fieldKeys);
        }

        public void addRowIdCondition(SQLFragment rowidCondition)
        {
            _expData.addRowIdCondition(rowidCondition);
        }

        public void addLSIDCondition(SQLFragment lsidCondition)
        {
            _expData.addLSIDCondition(lsidCondition);
        }

        public ColumnInfo addColumns(Domain domain, String legacyName)
        {
            ColumnInfo col = _expData.addColumns(domain, legacyName);
            return addExpColumn(col);
        }

        @Override
        public void setDomain(Domain domain)
        {
            _expData.setDomain(domain);
        }

        @Override
        public void setPublicSchemaName(String schemaName)
        {
            _expData.setPublicSchemaName(schemaName);
        }

        @Override
        public ContainerContext getContainerContext()
        {
            return _expData.getContainerContext();
        }

        @Override
        public FieldKey getContainerFieldKey()
        {
            return ((AbstractTableInfo)_expData).getContainerFieldKey();
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
        final SimpleFilter _filter = new SimpleFilter();
        ExpExperiment _experiment = null;
        ExpRun _run = null;
        boolean _runSpecified = false;

        FastFlowDataTable(String name, FlowDataType type)
        {
            super(getDbSchema(), name);
            _expData = ExperimentService.get().createDataTable(name, FlowSchema.this);
            _expData.setDataType(type);
            _flowObject = FlowManager.get().getTinfoObject();
            _type = type;

            _fps = new FlowPropertySet(FlowSchema.this.getContainer());
        }

        @Override
        public String getPublicName()
        {
            // 7471 Let schema explorer resolve lookup table names to "exp.Data" table.
            return ExpSchema.TableType.Data.toString();
        }

        @Override
        public void addAllowablePermission(Class<? extends Permission> permission)
        {
            _expData.addAllowablePermission(permission);
        }

        @Override
        public UserSchema getUserSchema()
        {
            return FlowSchema.this;
        }

        // NOTE: The download column is slightly different from the file column in that it wraps
        // the dataId instead of the flow.object.uri column.  For example, the CompensationMatrix
        // table has no uri value but does support being downloaded.
        ColumnInfo addDownloadColumn()
        {
            // Replace ExpDataTable's DownloadLink column with ours
            ColumnInfo colDownload = new AliasedColumn(this, Column.DownloadLink.name(), this.getColumn(ExpDataTable.Column.RowId));
            // Remove RowIdForeignKey
            colDownload.setFk(null);
            colDownload.setKeyField(false);
            colDownload.setHidden(true);
            colDownload.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new DataColumn(colInfo)
                    {
                        @Override
                        public String renderURL(RenderContext ctx)
                        {
                            Integer dataRowId = (Integer)getBoundColumn().getValue(ctx);
                            if (dataRowId != null)
                            {
                                FlowDataObject fdo = FlowDataObject.fromRowId(dataRowId);
                                if (fdo != null)
                                    return fdo.urlDownload().toString();
                            }
                            return null;
                        }

                        @Override
                        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                        {
                            String url = renderURL(ctx);
                            if (url != null)
                            {
                                out.write(PageFlowUtil.iconLink("fa fa-download", null, url, null, null, null));
                            }
                        }
                    };
                }
            });
            addColumn(colDownload);
            return colDownload;
        }

        ColumnInfo createUriColumnAlias(String columnAlias)
        {
            ColumnInfo uriColumn = _flowObject.getColumn("uri");
            ColumnInfo ret = new AliasedColumn(this, columnAlias, uriColumn);
            return ret;
        }

        ColumnInfo addFileColumn(String columnAlias)
        {
            ColumnInfo ret = createUriColumnAlias(columnAlias);

            DetailsURL detailsURL = new DetailsURL(new ActionURL(FlowController.DownloadAction.class, getContainer()).addParameter("dataId", "${rowId}"));
            //DetailsURL detailsURL = DetailsURL.fromString("/flow/download.view?dataId=${rowId}", getContainer());
            PropertyDescriptor pd = new PropertyDescriptor();
            pd.setURL(detailsURL);
            ret.setDisplayColumnFactory(new FileLinkDisplayColumn.Factory(pd, getContainer(), SchemaKey.fromParts(FlowSchema.SCHEMANAME), FlowTableType.FCSFiles.name(), FieldKey.fromParts("RowId")));
            ret.setURL(detailsURL);
            ret.setHidden(true);
            addColumn(ret);
            return ret;
        }

        ColumnInfo addFilePathColumn(String columnAlias)
        {
            ColumnInfo ret = createUriColumnAlias(columnAlias);
            ret.setHidden(true);
            addColumn(ret);
            return ret;
        }

        ColumnInfo addStatisticColumn(String columnAlias)
        {
            ColumnInfo colStatistic = addObjectIdColumn(columnAlias);
            colStatistic.setFk(new StatisticForeignKey(getContainer(), _fps, _type));
            colStatistic.setIsUnselectable(true);
            addMethod(columnAlias, new StatisticMethod(getContainer(), colStatistic));
            return colStatistic;
        }

        ColumnInfo addKeywordColumn(String columnAlias)
        {
            ColumnInfo colKeyword = addObjectIdColumn(columnAlias);
            colKeyword.setFk(new KeywordForeignKey(FlowSchema.this, _fps));
            colKeyword.setIsUnselectable(true);
            addMethod("Keyword", new KeywordMethod(getContainer(), colKeyword));
            return colKeyword;
        }

        ColumnInfo addGraphColumn(String columnAlias)
        {
            ColumnInfo colGraph = addObjectIdColumn(columnAlias);
            colGraph.setFk(new GraphForeignKey(getContainer(), _fps));
            colGraph.setIsUnselectable(true);
            return colGraph;
        }

        ColumnInfo addObjectIdColumn(String name)
        {
            ColumnInfo underlyingColumn = _flowObject.getColumn("rowid");
            ExprColumn ret = new ExprColumn(this, name, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".objectid"), underlyingColumn.getJdbcType());
            ret.copyAttributesFrom(underlyingColumn);
            ret.setKeyField(false);
            addColumn(ret);
            return ret;
        }

        ColumnInfo addHasURIColumn(String name)
        {
            ColumnInfo underlyingColumn = _flowObject.getColumn("uri");
            ExprColumn ret = new ExprColumn(this, name, new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".uri IS NOT NULL THEN true ELSE false END)"), JdbcType.BOOLEAN);
//            ret.copyAttributesFrom(underlyingColumn);
            addColumn(ret);
            return ret;
        }

        ColumnInfo addExpColumn(ColumnInfo underlyingColumn)
        {
            ExprColumn ret = new ExprColumn(this, underlyingColumn.getAlias(), underlyingColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS), underlyingColumn.getJdbcType());
            ret.copyAttributesFrom(underlyingColumn);
            ret.setHidden(underlyingColumn.isHidden());
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

            FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
            ICSMetadata metadata = protocol != null ? protocol.getICSMetadata() : null;
            if (metadata == null || !metadata.hasCompleteBackground())
                colBackground.setHidden(true);

            addMethod(columnAlias, new BackgroundMethod(FlowSchema.this, colBackground));
            return colBackground;
        }


        public String getSelectName()
        {
            return null;
        }

        /* TableInfo */
        @NotNull
        public SQLFragment getFromSQL()
        {
            assert _container != null;
            assert _container.getId().equals(getContainer().getId());

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
                where.append(and).append("RunId IN (SELECT RunList.ExperimentRunId FROM exp.RunList WHERE RunList.experimentId = ").append(_experiment.getRowId()).append(")\n");
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
            sqlFlowData.append(getFastFlowObjectFromSql("_", _container, _type.getObjectType().getTypeId()));
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

        public void setContainerFilter(@NotNull ContainerFilter filter)
        {
            checkLocked();
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
            checkLocked();
            ColumnInfo col = _expData.addMaterialInputColumn(alias,schema,inputRole,sampleSet);
            col.setParentTable(this);
            return addColumn(col);
        }

        public ColumnInfo addDataInputColumn(String alias, String role)
        {
            checkLocked();
            ColumnInfo col = _expData.addDataInputColumn(alias, role);
            col.setParentTable(this);
            return addColumn(col);
        }

        public ColumnInfo addInputRunCountColumn(String alias)
        {
            checkLocked();
            ColumnInfo col = _expData.addInputRunCountColumn(alias);
            col.setParentTable(this);
            return addColumn(col);
        }

        /* ExpTable */

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
            _expData.addColumn(col);
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

        public void addCondition(SQLFragment condition, FieldKey... fieldKeys)
        {
            _filter.addWhereClause(condition.getSQL(), condition.getParams().toArray(), fieldKeys);
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

        public ColumnInfo addColumns(Domain domain, String legacyName)
        {
            ColumnInfo col = _expData.addColumns(domain, legacyName);
            col.setHidden(false);
            col.setParentTable(this);
            return addColumn(col);
        }

        public ColumnInfo addReportColumns(final FlowReport report, FlowTableType tableType)
        {
            Domain domain = report.getDomain(tableType);
            if (domain == null)
                return null;

            // Keep expression in sync with FlowReportManager.getReportResultsLsid()
            SQLFragment sql = getSqlDialect().concatenate(
                new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".LSID"),
                new SQLFragment(
                        "'" +
                        "-" + FlowReportManager.FLOW_REPORT_RESULT_OBJECT_LSID_PART +
                        "-" + PageFlowUtil.encode(report.getReportId().toString()) +
                        //"-" + tableType.toString() +
                        "'"));
            // Explicitly create the FieldKey so report names containing '/' aren't parsed by FieldKey.fromString()
            FieldKey fieldKey = new FieldKey(null, report.getDescriptor().getReportName());
            ExprColumn col = new ExprColumn(this, fieldKey, sql, JdbcType.VARCHAR);

            PropertyForeignKey fk = new PropertyForeignKey(domain, FlowSchema.this)
            {
                @Override
                protected ColumnInfo constructColumnInfo(ColumnInfo parent, FieldKey name, PropertyDescriptor pd)
                {
                    ColumnInfo col = super.constructColumnInfo(parent, name, pd);
                    // Include report name so if multiple reports are in the same grid, the 'duplicated' columns are distinct.
                    col.setLabel(report.getDescriptor().getReportName() + " " + col.getLabel());
                    return col;
                }
            };
            fk.setParentIsObjectId(false);
            col.setFk(fk);
            col.setUserEditable(false);
            col.setIsUnselectable(true);
            return addColumn(col);
        }

        @Override
        public void setDomain(Domain domain)
        {
            _expData.setDomain(domain);
        }

        @Override
        public void setPublicSchemaName(String schemaName)
        {
            _expData.setPublicSchemaName(schemaName);
        }

        @Override
        public ContainerContext getContainerContext()
        {
            return _expData.getContainerContext();
        }

        @Override
        public FieldKey getContainerFieldKey()
        {
            return ((AbstractTableInfo)_expData).getContainerFieldKey();
        }

        @Override
        public Map<FieldKey, ColumnInfo> getExtendedColumns(boolean includeHidden)
        {
            LinkedHashMap<FieldKey, ColumnInfo> ret = new LinkedHashMap<>(super.getExtendedColumns(includeHidden));

            // Add Keyword, Statistics, Background, Sample, and Report columns
            addAllColumns(ret, getColumn("Keyword"), includeHidden);
            addAllColumns(ret, getColumn("Statistic"), includeHidden);
            addAllColumns(ret, getColumn("Background"), includeHidden);

            // Include FCSFile and Sample columns
            ColumnInfo fcsFileCol = getColumn(FCSFILE_FIELDKEY);
            if (fcsFileCol != null)
            {
                addAllColumns(ret, fcsFileCol, includeHidden);

                TableInfo fcsFileTable = fcsFileCol.getFkTableInfo();
                if (fcsFileTable != null)
                    addAllColumns(ret, fcsFileTable.getColumn("Sample"), includeHidden);
            }
            else if (getColumn("Sample") != null)
            {
                addAllColumns(ret, getColumn("Sample"), includeHidden);
            }

            // iterating the reports can be expensive. can we hold onto a column reference for the reports? XXX: Only check on FCSAnalysis table for reports
            for (FlowReport report : FlowReportManager.getFlowReports(getContainer(), getUser()))
            {
                FieldKey fieldKey = new FieldKey(null, report.getDescriptor().getReportName());
                addAllColumns(ret, getColumn(fieldKey), includeHidden);
            }

            return Collections.unmodifiableMap(ret);
        }

        private void addAllColumns(Map<FieldKey, ColumnInfo> ret, ColumnInfo lookupColumn, boolean includeHidden)
        {
            if (lookupColumn == null)
                return;

            // Issue 19790: flow: background columns showing up in timechart measure picker even though they should be hidden
            if (includeHidden || !lookupColumn.isHidden())
            {
                TableInfo lookupTable = lookupColumn.getFkTableInfo();
                if (lookupTable != null)
                {
                    for (Map.Entry<FieldKey, ColumnInfo> entry : lookupTable.getExtendedColumns(includeHidden).entrySet())
                    {
                        FieldKey fieldKey = entry.getKey();
                        ColumnInfo col = entry.getValue();
                        // Add the lookup column FieldKey as a parent to the column's FieldKey
                        FieldKey newFieldKey = FieldKey.remap(fieldKey, lookupColumn.getFieldKey(), null);
                        col.setFieldKey(newFieldKey);
                        ret.put(newFieldKey, col);
                    }
                }
            }
        }

        @Override
        protected ColumnInfo resolveColumn(String name)
        {
            ColumnInfo result = super.resolveColumn(name);
            if (result == null)
            {
                // Rename "Specimen" to "SpecimenID" for backwards compatibility <13.1
                if ("Specimen".equalsIgnoreCase(name))
                    return resolveColumn(SPECIMENID_FIELDKEY.getName());
            }
            return result;
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
        ret.addColumn(ExpDataTable.Column.RowId).setHidden(true);
        ret.addColumn(ExpDataTable.Column.LSID).setHidden(true);
        ColumnInfo flag = ret.addColumn(ExpDataTable.Column.Flag);
        if (type != null)
            flag.setDescription(type.getLabel() + " Flag");

        ret.addColumn(ExpDataTable.Column.Created).setHidden(true);
        ret.addColumn(ExpDataTable.Column.CreatedBy).setHidden(true);
        ret.setTitleColumn("Name");

        ColumnInfo sourceProtocolApplication = ret.addColumn(ExpDataTable.Column.SourceProtocolApplication);
        sourceProtocolApplication.setHidden(true);

        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setHidden(true);

        ColumnInfo colRun = ret.addColumn(ExpDataTable.Column.Run);
        colRun.setFk(new LookupForeignKey(new ActionURL(RunController.ShowRunAction.class, getContainer()), FlowParam.runId, "RowId", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                return detach().createRunTable("run", type);
            }
        });
        if (_experiment != null)
        {
            ret.setExperiment(_experiment.getExpObject());
        }
        if (_run != null)
        {
            ret.setRun(_run.getExpObject());
        }

        return ret;
    }



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

        public DeferredFCSAnalysisVisibleColumns(ExpDataTable table, FlowProtocol protocol, ColumnInfo colStatistic, ColumnInfo colGraph, ColumnInfo colBackground)
        {
            _table = table;
            _colStatistic = colStatistic;
            _colBackground = colBackground;
            _colGraph = colGraph;
        }

        public Iterator<FieldKey> iterator()
        {
            Collection<FieldKey> ret = new LinkedHashSet<>();
            ret.addAll(QueryService.get().getDefaultVisibleColumns(_table.getColumns()));
            ret.remove(FieldKey.fromParts("AnalysisScript"));
            ret.remove(FCSFILE_FIELDKEY);
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

            // Background column will be hidden if the ICSMetadata doesn't have background information
            if (_colBackground != null && !_colBackground.isHidden())
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
        return createFCSFileTable(name, true);
    }

    public FlowDataTable createFCSFileTable(String name, boolean specimenRelativeFromFCSFileTable)
    {
        final FlowDataTable ret = createDataTable(name, FlowDataType.FCSFile);
        ret.getColumn(ExpDataTable.Column.Name).setURL(new DetailsURL(new ActionURL(WellController.ShowWellAction.class, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        ret.setDetailsURL(new DetailsURL(new ActionURL(WellController.ShowWellAction.class, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString())));
        final ColumnInfo colKeyword = ret.addKeywordColumn("Keyword");
        ExpSampleSet ss = null;
        if (_protocol != null)
        {
            ss = _protocol.getSampleSet();
        }
        ColumnInfo colMaterialInput = ret.addMaterialInputColumn("Sample", new SamplesSchema(getUser(), getContainer()), ExpMaterialRunInput.DEFAULT_ROLE, ss);
        if (ss == null)
        {
            colMaterialInput.setHidden(true);
        }

        // SpecimenID
        ICSMetadata metadata = getProtocol() == null ? null : getProtocol().getICSMetadata();
        FieldKey specimenIdFieldKey = metadata != null ? removeParent(metadata.getSpecimenIdColumn(), FCSFILE_NAME) : null;
        if (specimenIdFieldKey != null)
        {
            ColumnInfo colSpecimen = new FCSFileCoalescingColumn(ret, SPECIMENID_FIELDKEY, JdbcType.VARCHAR, metadata, true);
            ret.addColumn(colSpecimen);

            ExpProtocol protocol = getProtocol().getProtocol();
            AssayProvider provider = AssayService.get().getProvider(protocol);
            AssayProtocolSchema schema = provider.createProtocolSchema(getUser(), getContainer(), protocol, null);
            FlowAssayProvider.FlowAssayTableMetadata tableMetadata = new FlowAssayProvider.FlowAssayTableMetadata(provider, protocol, true);
            SpecimenForeignKey specimenFK = new SpecimenForeignKey(schema, ret, tableMetadata);
            colSpecimen.setFk(specimenFK);
        }
        else
        {
            ColumnInfo colSpecimen = new NullColumnInfo(ret, SPECIMENID_FIELDKEY, JdbcType.VARCHAR);
            colSpecimen.setHidden(true);
            ret.addColumn(colSpecimen);
        }


        // ParticipantID
        FieldKey participantFieldKey = metadata != null ? removeParent(metadata.getParticipantColumn(), FCSFILE_NAME) : null;
        if (participantFieldKey != null)
        {
            ColumnInfo col = new FCSFileCoalescingColumn(ret, PARTICIPANTID_FIELDKEY, JdbcType.VARCHAR, metadata, true);
            ret.addColumn(col);
            // XXX: PTID ForeignKey ?
        }
        else
        {
            ColumnInfo col = new NullColumnInfo(ret, PARTICIPANTID_FIELDKEY, JdbcType.VARCHAR);
            col.setHidden(true);
            ret.addColumn(col);
        }

        // VisitID
        FieldKey visitIdFieldKey = metadata != null ? removeParent(metadata.getVisitColumn(), FCSFILE_NAME) : null;
        if (visitIdFieldKey != null)
        {
            ColumnInfo col = new FCSFileCoalescingColumn(ret, VISITID_FIELDKEY, JdbcType.DOUBLE, metadata, true);
            ret.addColumn(col);
            // XXX: PTID/Visit ForeignKey ?
        }
        else
        {
            ColumnInfo col = new NullColumnInfo(ret, VISITID_FIELDKEY, JdbcType.VARCHAR);
            col.setHidden(true);
            ret.addColumn(col);
        }

        // Date
        FieldKey dateFieldKey = metadata != null ? removeParent(metadata.getDateColumn(), FCSFILE_NAME) : null;
        if (dateFieldKey != null)
        {
            ColumnInfo col = new FCSFileCoalescingColumn(ret, DATE_FIELDKEY, JdbcType.DOUBLE, metadata, true);
            ret.addColumn(col);
            // XXX: PTID/Date ForeignKey ?
        }
        else
        {
            ColumnInfo col = new NullColumnInfo(ret, DATE_FIELDKEY, JdbcType.DATE);
            col.setHidden(true);
            ret.addColumn(col);
        }

        // TargetStudy
        {
            ColumnInfo colTargetStudy = new FCSFileCoalescingColumn(ret, TARGET_STUDY_FIELDKEY, JdbcType.DOUBLE, metadata, true);
            colTargetStudy.setLabel(AbstractAssayProvider.TARGET_STUDY_PROPERTY_CAPTION);
            colTargetStudy.setDisplayColumnFactory(_targetStudyDisplayColumnFactory);
            colTargetStudy.setHidden(true);
            ret.addColumn(colTargetStudy);
        }

        String bTRUE = _dbSchema.getSqlDialect().getBooleanTRUE();
        String bFALSE = _dbSchema.getSqlDialect().getBooleanFALSE();

        ret.addFileColumn("File");
        // Similar to 'ExpDataTable.Column.DataFileUrl' except uses the flow.object.uri column
        ret.addFilePathColumn("FilePath");
        ret.addDownloadColumn();

        // flow.object.uri -- not to be confused with exp.data.datafileurl
        ExprColumn colHasFile = new ExprColumn(ret, "HasFile", new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".uri IS NOT NULL THEN " + bTRUE +" ELSE " + bFALSE + " END)"), JdbcType.BOOLEAN);
        ret.addColumn(colHasFile);
        colHasFile.setHidden(true);

        // Original input FCSFile (the FCSFile marked as a DataInput of this FCSFile)
        ColumnInfo colFCSFile = new ExprColumn(ret, ORIGINAL_FCSFILE_FIELDKEY, new SQLFragment(ExprColumn.STR_TABLE_ALIAS  + ".fcsid"), JdbcType.INTEGER);
        ret.addColumn(colFCSFile);
        colFCSFile.setHidden(true);
        colFCSFile.setFk(new LookupForeignKey(new ActionURL(WellController.ShowWellAction.class, getContainer()),
                FlowParam.wellId.toString(),
                "RowId", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                return detach().createFCSFileTable("FCSFile");
            }
        });

        // UNDONE: Ideally we should add a column to flow.object to idenfity these wells.
        // Returns true if this is an original FlowFCSFile (not a 'fake' FCSFile created by importing a FlowJo workspace)
        ColumnInfo colOriginal = new ExprColumn(ret, "Original", new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".datafileurl NOT LIKE '%/attributes.flowdata.xml' THEN " + bTRUE + " ELSE " + bFALSE + " END)"), JdbcType.BOOLEAN);
        ret.addColumn(colOriginal);
        colOriginal.setHidden(true);

        PropertyColumn osf = new PropertyColumn(FlowProperty.OriginalSourceFile.getPropertyDescriptor(), ret.getColumn(ExpDataTable.Column.LSID), getContainer(), getUser(), true);
        osf.setHidden(true);
        ret.addColumn(osf);

        ret.setDefaultVisibleColumns(new DeferredFCSFileVisibleColumns(ret, colKeyword));
        return ret;
    }


    public ExpDataTable createFCSAnalysisTable(String alias, FlowDataType type, boolean includeCopiedToStudyColumns)
    {
        FlowDataTable ret = createDataTable(alias, type);

        ColumnInfo colAnalysisScript = new ExprColumn(ret, "AnalysisScript", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".scriptid"), JdbcType.INTEGER);
        ret.addColumn(colAnalysisScript);
        colAnalysisScript.setFk(new LookupForeignKey(new ActionURL(AnalysisScriptController.BeginAction.class, getContainer()),
                FlowParam.scriptId.toString(), "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createAnalysisScriptTable("Lookup", true);
            }
        });

        ColumnInfo colCompensationMatrix = new ExprColumn(ret, "CompensationMatrix", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".compid"), JdbcType.INTEGER);
        ret.addColumn(colCompensationMatrix);
        colCompensationMatrix.setFk(new LookupForeignKey(new ActionURL(CompensationController.ShowCompensationAction.class, getContainer()), FlowParam.compId.toString(),
                "RowId", "Name"){
            public TableInfo getLookupTableInfo()
            {
                return detach().createCompensationMatrixTable("Lookup");
            }
        });

        DetailsURL detailsURL = new DetailsURL(new ActionURL(WellController.ShowWellAction.class, getContainer()), Collections.singletonMap(FlowParam.wellId.toString(), ExpDataTable.Column.RowId.toString()));
        ret.getColumn(ExpDataTable.Column.Name).setURL(detailsURL);
        ret.setDetailsURL(detailsURL);
        if (getExperiment() != null)
        {
            ret.setExperiment(ExperimentService.get().getExpExperiment(getExperiment().getLSID()));
        }

        ColumnInfo colStatistic = ret.addStatisticColumn("Statistic");

        ColumnInfo colBackground = ret.addBackgroundColumn("Background");

        ColumnInfo colGraph = ret.addGraphColumn("Graph");

        ColumnInfo colFCSFile = new ExprColumn(ret, FCSFILE_FIELDKEY, new SQLFragment(ExprColumn.STR_TABLE_ALIAS  + ".fcsid"), JdbcType.INTEGER);
        ret.addColumn(colFCSFile);
        colFCSFile.setFk(new LookupForeignKey(new ActionURL(WellController.ShowWellAction.class, getContainer()),
                FlowParam.wellId.toString(),
                "RowId", "Name") {
                public TableInfo getLookupTableInfo()
                {
                    return detach().createFCSFileTable("FCSFile", false);
                }
            });

        ret.addFileColumn("File");
        // Similar to 'ExpDataTable.Column.DataFileUrl' except uses the flow.object.uri column
        ret.addFilePathColumn("FilePath");
        ret.addDownloadColumn();

        for (FlowReport report : FlowReportManager.getFlowReports(getContainer(), getUser()))
        {
            ret.addReportColumns(report, FlowTableType.FCSAnalyses);
        }

        if (includeCopiedToStudyColumns)
            addCopiedToStudyColumns(ret);

        ret.setDefaultVisibleColumns(new DeferredFCSAnalysisVisibleColumns(ret, _protocol, colStatistic, colGraph, colBackground));
        return ret;
    }

    private Collection<FieldKey> addCopiedToStudyColumns(AbstractTableInfo ret)
    {
        List<FieldKey> copiedToStudyColumns = new ArrayList<>(10);
        FlowProtocol protocol = getProtocol();
        if (protocol == null)
            protocol = FlowProtocol.getForContainer(getContainer());
        if (protocol != null)
        {
            ExpProtocol expProtocol = protocol.getProtocol();
            FlowAssayProvider provider = (FlowAssayProvider)AssayService.get().getProvider(expProtocol);
            if (provider != null)
            {
                Set<String> studyColumnNames = provider.createProtocolSchema(getUser(), getContainer(), expProtocol, null).addCopiedToStudyColumns(ret, false);
                for (String columnName : studyColumnNames)
                    copiedToStudyColumns.add(new FieldKey(null, columnName));
            }
        }
        return copiedToStudyColumns;
    }

    // Rewrite a FieldKey
    // from "FCSFile/Foo" to "FCSFile/OriginalFCSFile/Foo" or
    // from "Foo" to "OriginalFCSFile/Foo"
    public static final FieldKey rewriteAsOriginalFCSFile(FieldKey fieldKey)
    {
        if (fieldKey != null)
        {
            List<String> parts = fieldKey.getParts();
            int insertAt = 0;
            if (parts.get(0).equals(FCSFILE_FIELDKEY.getName()))
                insertAt = 1;
            parts.add(insertAt, ORIGINAL_FCSFILE_FIELDKEY.getName());
            return FieldKey.fromParts(parts);
        }
        
        return null;
    }

    // Remove the root component from a FieldKey if matches the rootPart.
    public static FieldKey removeParent(FieldKey fieldKey, String rootPart)
    {
        if (fieldKey != null)
        {
            return fieldKey.removeParent(rootPart);
        }

        return null;
    }

    
    public ExpDataTable createCompensationControlTable(String alias)
    {
        ExpDataTable ret = createFCSAnalysisTable(alias, FlowDataType.CompensationControl, false);
        List<FieldKey> defColumns = new ArrayList<>(ret.getDefaultVisibleColumns());
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
        DetailsURL detailsURL = new DetailsURL(new ActionURL(CompensationController.ShowCompensationAction.class, getContainer()), Collections.singletonMap(FlowParam.compId.toString(), ExpDataTable.Column.RowId.toString()));
        ret.setDetailsURL(detailsURL);
        ret.getColumn(ExpDataTable.Column.Name).setURL(detailsURL);
        ret.addStatisticColumn("Value");
        // Only add download column -- the file column will always be blank
        ret.addDownloadColumn();
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
        ret.getColumn(ExpDataTable.Column.Run.toString()).setHidden(true);
        DetailsURL detailsURL = new DetailsURL(new ActionURL(AnalysisScriptController.BeginAction.class, getContainer()), Collections.singletonMap(FlowParam.scriptId.toString(), "RowId"));
        ret.setDetailsURL(detailsURL);
        ret.getColumn(ExpDataTable.Column.Name).setURL(detailsURL);

        ret.addFileColumn("File");
        // Similar to 'ExpDataTable.Column.DataFileUrl' except uses the flow.object.uri column
        ret.addFilePathColumn("FilePath");
        ret.addDownloadColumn();

        return ret;
    }

    public FlowDataTable createWorkspaceTable(String alias)
    {
        FlowDataTable ret = createDataTable(alias, FlowDataType.Workspace);
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
            ActionURL detailsURL = FlowTableType.Runs.urlFor(getUser(), getContainer(), "ProtocolStep", FlowProtocolStep.calculateCompensation.getName());
            detailsURL.addParameter(FlowParam.experimentId, "${RowId}");
            colCompensationCount.setURL(StringExpressionFactory.createURL(detailsURL));
            ret.addColumn(colCompensationCount);
        }
        if (analysisProtocol != null)
        {
            ColumnInfo colAnalysisRunCount = ret.createRunCountColumn("AnalysisRunCount", null, analysisProtocol.getProtocol());
            ActionURL detailsURL = FlowTableType.Runs.urlFor(getUser(), getContainer(), "ProtocolStep", FlowProtocolStep.analysis.getName());
            detailsURL.addParameter(FlowParam.experimentId, "${RowId}");
            colAnalysisRunCount.setURL(StringExpressionFactory.createURL(detailsURL));
            ret.addColumn(colAnalysisRunCount);
        }

        DetailsURL detailsUrl = new DetailsURL(new ActionURL(RunController.ShowRunsAction.class, getContainer()).addParameter(FlowQueryView.DATAREGIONNAME_DEFAULT + ".sort", "ProtocolStep"),
                Collections.singletonMap(FlowParam.experimentId.toString(), ExpExperimentTable.Column.RowId.toString()));
        ret.setDetailsURL(detailsUrl);
        ret.getColumn(ExpExperimentTable.Column.Name).setURL(detailsUrl);
        SQLFragment lsidCondition = new SQLFragment("LSID <> ");
        lsidCondition.appendStringLiteral(FlowExperiment.getExperimentRunExperimentLSID(getContainer()));
        ret.addCondition(lsidCondition);
        return ret;
    }


    protected QuerySettings createQuerySettings(String dataRegionName, String queryName, String viewName)
    {
        return new FlowQuerySettings(dataRegionName);
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
        ExprColumn ret = new ExprColumn(runTable, name, sql, JdbcType.INTEGER);
        ret.setHidden(true);
        runTable.addColumn(ret);
        return ret;
    }

    private TableInfo createStatisticsTable(String alias)
    {
        return createAttributeTable(alias, FlowManager.get().getTinfoStatisticAttr());
    }

    private TableInfo createKeywordsTable(String alias)
    {
        return createAttributeTable(alias, FlowManager.get().getTinfoKeywordAttr());
    }

    private TableInfo createGraphsTable(String alias)
    {
        return createAttributeTable(alias, FlowManager.get().getTinfoGraphAttr());
    }

    private TableInfo createAttributeTable(String alias, TableInfo realTable)
    {
        FilteredTable ret = new FilteredTable<>(realTable, this);
        ret.setName(alias);
        ret.addWrapColumn(ret.getRealTable().getColumn("RowId")).setHidden(true);
        ret.addWrapColumn(ret.getRealTable().getColumn("Container")).setHidden(true);
        ret.addWrapColumn(ret.getRealTable().getColumn("Name"));
        ret.addWrapColumn(ret.getRealTable().getColumn("Id")).setHidden(true);

        //ExprColumn preferredNameCol = new ExprColumn(ret.getRealTable(), "AliasOf", sql, JdbcType.BOOLEAN);
        //ret.addColumn(aliasOfCol);
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
        try
        {
            return Integer.valueOf(str);
        }
        catch (NumberFormatException ex)
        {
            return 0;
        }
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

//    private static final Map<String, MaterializedQueryHelper> fastflowCache = new LinkedHashMap<String, MaterializedQueryHelper>(100,0.6F,true)
//    {
//        @Override
//        protected boolean removeEldestEntry(Map.Entry<String, MaterializedQueryHelper> eldest)
//        {
//            return eldest.getValue().lastUsed + CacheManager.MINUTE < HeartBeat.currentTimeMillis();
//        }
//    };
    private static final StringKeyCache<MaterializedQueryHelper> fastflowCache = CacheManager.getStringKeyCache(CacheManager.UNLIMITED,CacheManager.HOUR, "fast flow objects");

    private static final ContainerManager.ContainerListener containerListener = new ContainerManager.ContainerListener()
    {
        @Override
        public void containerDeleted(Container c, User user)
        {
            fastflowCache.removeUsingPrefix(c.getId() + "/");
        }

        @Override public void containerCreated(Container c, User user) { }
        @Override public void containerMoved(Container c, Container oldParent, User user) { }
        @NotNull @Override public Collection<String> canMove(Container c, Container newParent, User user) { return null; }
        @Override public void propertyChange(PropertyChangeEvent evt) { }
    };

    // Returns fully qualified "schema.table" name
    SQLFragment getFastFlowObjectFromSql(String tableAlias, Container c, int typeid)
    {
        String attr = c.getId() + "/flow.object/" + typeid;
        MaterializedQueryHelper helper = fastflowCache.get(attr);
        if (null == helper)
        {
            helper = createFastFlowObjectHelper(c, typeid);
            fastflowCache.put(attr, helper);
        }

        return helper.getFromSql(tableAlias, null);
    }


    /** CONSIDER JOIN ObjectId from exp.Objects */
    MaterializedQueryHelper createFastFlowObjectHelper(Container c, int typeid)
    {
        DbSchema flow = FlowManager.get().getSchema();

        SQLFragment select = new SQLFragment(
                "SELECT \n" +
                "    exp.data.RowId,\n" +
                "    exp.data.LSID,\n" +
                "    exp.data.Name,\n" +
                "    exp.data.CpasType,\n" +
                "    exp.data.SourceApplicationId,\n" +
                "    exp.data.DataFileUrl,\n" +
                "    exp.data.RunId,\n" +
                "    exp.data.Created,\n" +
                "    exp.data.CreatedBy,\n" +
                "    exp.data.Container,\n" +
                "    flow.object.RowId AS objectid,\n" +
                "    flow.object.TypeId,\n" +
                "    flow.object.compid,\n" +
                "    flow.object.fcsid,\n" +
                "    flow.object.scriptid,\n" +
                "    flow.object.uri\n" +
                "FROM exp.data\n" +
                "    INNER JOIN flow.object ON exp.Data.RowId=flow.object.DataId\n");
        select.append(" WHERE flow.Object.container = ").append(c).append(" AND TypeId = ").append(String.valueOf(typeid));
        List<String> indexes = Arrays.asList(
                "CREATE INDEX ix_rowid_${NAME} ON temp.${NAME} (RowId);\n",
                "CREATE INDEX ix_objid_${NAME} ON temp.${NAME} (ObjectId);"
        );

        Supplier<String> uptodate = () -> String.valueOf(FlowManager.get().flowObjectModificationCount.get());
        return MaterializedQueryHelper.create("ffo",flow.getScope(),
                select,
                uptodate,
                indexes,
                30 * CacheManager.MINUTE);
    }


    SQLFragment getBackgroundJunctionFromSql(String tableAlias, Container c)
    {
        String attr =  c.getId() + "/bgjunction";
        MaterializedQueryHelper helper = fastflowCache.get(attr);
        if (null == helper)
        {
            helper = createBackgroundJunctionHelper(c);
            fastflowCache.put(attr, helper);
        }

        if (null == helper)
            return null;
        return helper.getFromSql(tableAlias, null);
    }

    MaterializedQueryHelper createBackgroundJunctionHelper(Container c)
    {
        DbSchema flow = FlowManager.get().getSchema();

        ICSMetadata ics = _protocol.getICSMetadata();
        if (!ics.hasCompleteBackground())
            return null;

        // BACKGROUND
        FlowDataTable bg = (FlowDataTable)detach().createTable(FlowTableType.FCSAnalyses.toString());
        bg.addObjectIdColumn("objectid");
        Set<FieldKey> allColumns = new TreeSet<>(ics.getMatchColumns());
        for (FilterInfo f : ics.getBackgroundFilter())
            allColumns.add(f.getField());
        Map<FieldKey,ColumnInfo> bgMap = QueryService.get().getColumns(bg, allColumns);
        if (bgMap.size() != allColumns.size())
            return null;
        ArrayList<ColumnInfo> bgFields = new ArrayList<>();
        bgFields.add(bg.getColumn("objectid"));
        bgFields.addAll(bgMap.values());
        SimpleFilter filter = new SimpleFilter();
        for (FilterInfo f : ics.getBackgroundFilter())
        {
            Object value = f.getValue();
            if (value instanceof String && f.getField().getParts().get(0).equalsIgnoreCase("Statistic"))
                value = Double.parseDouble((String)value);
            filter.addCondition(bgMap.get(f.getField()), value, f.getOp());
        }
        SQLFragment bgSQL = Table.getSelectSQL(bg, bgFields, null, null);
        if (filter.getClauses().size() > 0)
        {
            Map<FieldKey, ColumnInfo> columnMap = Table.createColumnMap(bg, bgFields);
            SQLFragment filterFrag = filter.getSQLFragment(flow.getSqlDialect(), columnMap);
            SQLFragment t = new SQLFragment("SELECT * FROM (");
            t.append(bgSQL);
            t.append(") _filter_ " );
            t.append(filterFrag);
            bgSQL = t;
        }

        // FOREGROUND
        FlowDataTable fg = (FlowDataTable)detach().createTable(FlowTableType.FCSAnalyses.toString());
        fg.addObjectIdColumn("objectid");
        Set<FieldKey> setMatchColumns = new HashSet<>(ics.getMatchColumns());
        Map<FieldKey,ColumnInfo> fgMap = QueryService.get().getColumns(fg, setMatchColumns);
        if (fgMap.size() != setMatchColumns.size())
            return null;
        ArrayList<ColumnInfo> fgFields = new ArrayList<>();
        fgFields.add(fg.getColumn("objectid"));
        fgFields.addAll(fgMap.values());
        SQLFragment fgSQL = Table.getSelectSQL(fg, fgFields, null, null);

        SQLFragment selectInto = new SQLFragment();
        selectInto.append("SELECT F.objectid as fg, B.objectid as bg ");
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

        SQLFragment select = new SQLFragment(selectInto);
        List<String> indexes = Arrays.asList(
                "CREATE INDEX ix_fg_${NAME} ON temp.${NAME} (fg);\n",
                "CREATE INDEX ix_bg_${NAME} ON temp.${NAME} (bg);\n"
        );

        Supplier<String> uptodate = () -> String.valueOf(FlowManager.get().flowObjectModificationCount.get());
        return MaterializedQueryHelper.create("fbg",flow.getScope(),
                select,
                uptodate,
                indexes,
                30 * CacheManager.MINUTE);
    }
}
