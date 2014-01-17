/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.di;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.gwt.client.util.StringUtils;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.di.pipeline.TransformRun;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Dax
 * Date: 9/13/13
 * Time: 12:27 PM
 * To change this template use File | Settings | File Templates.
 */
abstract public class TransformBaseTable extends VirtualTable
{
    protected SQLFragment _sql;
    private final UserSchema _schema;
    // map base table column name to alias name
    private HashMap<String, String> _nameMap;

    protected HashMap<String, String> buildNameMap()
    {
        HashMap<String, String> colMap = new HashMap<>();
        colMap.put("TransformId", "Name");
        colMap.put("TransformVersion", "Version");
        colMap.put("RecordCount", "RecordsProcessed");
        colMap.put("ExecutionTime", "ExecutionTime");
        return colMap;
    }

    protected HashMap<String, String> getNameMap()
    {
        return _nameMap;
    }


    public SQLFragment getFromSQL()
    {
        return _sql;
    }

    public TransformBaseTable(UserSchema schema, String name)
    {
        super(DataIntegrationQuerySchema.getSchema(), name);
        _nameMap = buildNameMap();
        _schema = schema;
    }

    protected String getBaseSql()
    {
        SqlDialect dialect = getSqlDialect();
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT t.TransformId AS ");
        sql.append(_nameMap.get("TransformId"));
        sql.append(", t.TransformVersion AS ");
        sql.append(_nameMap.get("TransformVersion"));
        sql.append(", t.StartTime AS ");
        sql.append(_nameMap.get("StartTime"));
        sql.append(", t.Status AS ");
        sql.append(_nameMap.get("Status"));
        sql.append(", t.RecordCount AS ");
        sql.append(_nameMap.get("RecordCount"));
        sql.append(", (CAST("); // get the number of seconds as n.nnn
        sql.append(dialect.getDateDiff(Calendar.MILLISECOND, "t.EndTime", "t.StartTime"));
        sql.append(" AS FLOAT)/1000)");
        sql.append(" AS ");
        sql.append(_nameMap.get("ExecutionTime"));
        sql.append(", t.JobId, t.TransformRunId");
        sql.append(" FROM ");
        sql.append(DataIntegrationQuerySchema.getTransformRunTableName());
        sql.append(" t\n");
        return sql.toString();
    }

    protected String getWhereClause()
    {
        return getWhereClause(null);
    }

    // filter out NO_WORK as well as
    // scope to the current container
    protected String getWhereClause(String tableAlias)
    {
        StringBuilder sqlWhere = new StringBuilder();
        sqlWhere.append("WHERE ");
        if (!StringUtils.isEmpty(tableAlias))
        {
            sqlWhere.append(tableAlias);
            sqlWhere.append(".");
        }
        sqlWhere.append("Status <> '");
        sqlWhere.append(TransformRun.TransformRunStatus.NO_WORK.getDisplayName());
        sqlWhere.append("'");
        sqlWhere.append(" AND ");
        sqlWhere.append(" Container = '");
        sqlWhere.append(_schema.getContainer().getId());
        sqlWhere.append("'");
        return sqlWhere.toString();
    }

    protected void addBaseColumns()
    {
        // name
        ColumnInfo transformId = new ColumnInfo(_nameMap.get("TransformId"), this);
        transformId.setJdbcType(JdbcType.VARCHAR);
        addColumn(transformId);

        // version
        ColumnInfo transformVersion = new ColumnInfo(_nameMap.get("TransformVersion"), this);
        transformVersion.setJdbcType(JdbcType.INTEGER);
        addColumn(transformVersion);

        //last run
        ColumnInfo startTime = new ColumnInfo(_nameMap.get("StartTime"), this);
        startTime.setJdbcType(JdbcType.TIMESTAMP);
        startTime.setFormat("MM/dd/yy HH:mm");
        startTime.setSortDirection(Sort.SortDirection.DESC);
        addColumn(startTime);

        // last status
        ColumnInfo status = new ColumnInfo(_nameMap.get("Status"), this);
        status.setJdbcType(JdbcType.VARCHAR);
        addColumn(status);

        // records processed
        ColumnInfo recordCount = new ColumnInfo(_nameMap.get("RecordCount"), this);
        recordCount.setJdbcType(JdbcType.INTEGER);
        addColumn(recordCount);

        // execution time
        ColumnInfo execTime = new ColumnInfo(_nameMap.get("ExecutionTime"), this);
        execTime.setJdbcType(JdbcType.DOUBLE);
        addColumn(execTime);

        // job id lookup to log file path
        ColumnInfo jobId = new ColumnInfo("JobId", this);
        jobId.setJdbcType(JdbcType.INTEGER);
        jobId.setFk(new LookupForeignKey("rowId", "FilePath")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return PipelineService.get().getJobsTable(_schema.getUser(), _schema.getContainer());
            }
        });
        jobId.setHidden(true);
        addColumn(jobId);

        status.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new StatusColumn(colInfo);
            }
        });

        ColumnInfo transformRunId = new ColumnInfo("TransformRunId", this);
        transformRunId.setJdbcType(JdbcType.INTEGER);
        transformRunId.setHidden(true);
        addColumn(transformRunId);
    }

    @Override
    public String getSelectName()
    {
        return null;
    }

    //
    // renders a popup that shows the log file for the pipeline job that is
    // run for the transform
    //
    public static class StatusColumn extends DataColumn
    {
        private final ColumnInfo _statusColumn;
        FieldKey _jobFieldKey;
        FieldKey _filePathFieldKey;
        private final String ShowLog = "_showLog";
        private boolean _includeShowLogScript;

        public StatusColumn(ColumnInfo status)
        {
            super(status);
            _statusColumn = status;
            _jobFieldKey = FieldKey.fromString(_statusColumn.getFieldKey().getParent(), "JobId");
            _filePathFieldKey = FieldKey.fromString(_statusColumn.getFieldKey().getParent(), "JobId/FilePath");
            _includeShowLogScript = true;
        }

        @Override
        public boolean isSortable()
        {
            return true;
        }

        private String hq(String s)
        {
            return PageFlowUtil.filter(PageFlowUtil.jsString(s));
        }

        //
        // ideally we would have another event/hook to render column contents just once for
        // a specific region.  We can't render this in the data region since the column could
        // be used outside a specific view designed for it
        //
        private String getShowLogScript(String dataRegionName)
        {
            StringBuilder script = new StringBuilder();
            script.append("<script type=\"text/javascript\">");
            script.append("function ");
            script.append(dataRegionName + ShowLog);
            script.append("(url, title) {");
            script.append("var X = Ext4 || Ext;");
            script.append("X.Ajax.request({");
            script.append("url:url, method: 'GET', success: function(response) {");
            script.append("var win = new X.Window({");
            script.append("title: title, border: false, html: response.responseText.replace(/\\r\\n/g, \"<br>\"),");
            script.append("closeAction: 'close', autoScroll : true, buttons : [{");
            script.append("text: 'Close', handler : function() { win.close(); } }]");
            script.append("});");
            script.append("win.show();");
            script.append("}");
            script.append("});");
            script.append("}");
            script.append("</script>\n");
            return script.toString();
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(_jobFieldKey);
            keys.add(_filePathFieldKey);
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            String dataRegionName = ctx.getCurrentRegion() != null ? ctx.getCurrentRegion().getName() : null;

            if (null != dataRegionName && _includeShowLogScript)
            {
                out.write(getShowLogScript(dataRegionName));
                _includeShowLogScript = false;
            }

            Integer jobId = ctx.get(_jobFieldKey, Integer.class);
            String filePath = ctx.get(_filePathFieldKey, String.class);
            String statusValue = getFormattedValue(ctx);

            if (null != jobId && null != filePath && null != dataRegionName)
            {
                File logFile = new File(filePath);
                if (logFile.exists())
                {
                    String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
                    ActionURL jobAction = new ActionURL("pipeline-status", "showFile", ctx.getContainer());
                    jobAction.addParameter("rowId", jobId);
                    jobAction.addParameter("filename", filename);

                    StringBuilder text = new StringBuilder();
                    text.append("<a href=\"#viewLog\" onclick=\"");
                    text.append(dataRegionName + ShowLog);
                    text.append("(");
                    text.append(hq(jobAction.toString()));
                    text.append(",");
                    text.append(hq(filename));
                    text.append(")\">");
                    text.append(statusValue);
                    text.append("</a>");
                    out.write(text.toString());
                    return;
                }
            }

            // if none of the conditions are met above then just write out the status
            // value without the link
            out.write(statusValue);
        }
    }
}
