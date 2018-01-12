/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
package org.labkey.flow.reports;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.CachedResultSet;
import org.labkey.api.data.CachedResultSets;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.FilterInfo;
import org.labkey.api.data.Results;
import org.labkey.api.data.ResultsImpl;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.AliasManager;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.reports.report.ScriptReportDescriptor;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.api.writer.ContainerUser;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class FilterFlowReport extends FlowReport
{
    public RReport _inner = null;
    public String _query = null;

    abstract String getScriptResource() throws IOException;

    public RReport getInnerReport() throws IOException
    {
        if (null == _inner)
        {
            _inner = new FilterFlowInnerReport(this);

            String script = getScriptResource();
            _inner.getDescriptor().setProperties(getDescriptor().getProperties());
            _inner.setScriptSource(script);
        }

        return _inner;
    }

    protected ICSMetadata getMetadata(Container c)
    {
        FlowProtocol protocol = FlowProtocol.getForContainer(c);
        if (protocol == null)
            throw new NotFoundException("flow protocol not found");

        ICSMetadata metadata = protocol.getICSMetadata();
        if (metadata == null || metadata.isEmpty())
            return null;

        return metadata;
    }

    protected Collection<FieldKey> getMetadataColumns(ICSMetadata metadata)
    {
        Collection<FieldKey> fieldKeys = new ArrayList<>();
        fieldKeys.add(metadata.getParticipantColumn());

        if (metadata.getVisitColumn() != null)
            fieldKeys.add(metadata.getVisitColumn());

        if (metadata.getDateColumn() != null)
            fieldKeys.add(metadata.getDateColumn());

        fieldKeys.addAll(getMatchColumns(metadata));
        return fieldKeys;
    }

    protected Collection<FieldKey> getMatchColumns(ICSMetadata metadata)
    {
        Collection<FieldKey> fieldKeys = new ArrayList<>();
        for (FieldKey fieldKey : metadata.getMatchColumns())
        {
            // Use the 'Run' RowId instead of Run.  The 'Run' display name is already added to the select list.
            if (fieldKey.getName().equals("Run"))
                fieldKey = new FieldKey(fieldKey, "RowId");
            fieldKeys.add(fieldKey);
        }

        return fieldKeys;
    }

    void addScriptProlog(ViewContext context, StringBuffer sb)
    {
        ICSMetadata metadata = getMetadata(context.getContainer());
        if (metadata == null || metadata.isEmpty())
            return;

        if (metadata.hasCompleteStudyMeta())
        {
            if (metadata.getParticipantColumn() != null)
                sb.append("flow.metadata.study.participantColumn <- \"").append(oldLegalName(metadata.getParticipantColumn())).append("\"\n");

            if (metadata.getVisitColumn() != null)
                sb.append("flow.metadata.study.visitColumn <- \"").append(oldLegalName(metadata.getVisitColumn())).append("\"\n");

            if (metadata.getDateColumn() != null)
                sb.append("flow.metadata.study.dateColumn <- \"").append(oldLegalName(metadata.getDateColumn())).append("\"\n");
        }

        if (metadata.hasCompleteBackground())
        {
            String comma = "";
            sb.append("flow.metadata.matchColumns <- c(");
            for (FieldKey fieldKey : getMatchColumns(metadata))
            {
                if (fieldKey != null)
                {
                    String name = oldLegalName(fieldKey);
                    sb.append(comma).append("\"").append(name).append("\"");
                    comma = ", ";
                }
            }
            sb.append(")\n");

            comma = "";
            sb.append("flow.metadata.background <- list(");
            for (FilterInfo filter : metadata.getBackgroundFilter())
            {
                if (filter != null && filter.getField() != null && filter.getOp() != null)
                {
                    sb.append(comma);
                    sb.append("list(");

                    String name = oldLegalName(filter.getField());
                    sb.append("\"filter\"=\"").append(name).append("\"");
                    sb.append(", \"op\"=\"").append(filter.getOp()).append("\"");
                    if (filter.getValue() != null)
                        sb.append(", \"value\"=\"").append(filter.getValue()).append("\"");

                    sb.append(")");
                    comma = ", ";
                }
            }
            sb.append(")\n");
        }
    }

    // UNDONE: Get the name of the column from the Results metadata to match ScriptEngineReport.outputColumnNames().
    // Copied from ScriptEngineReport.
    private String oldLegalName(FieldKey fkey)
    {
        String r = AliasManager.makeLegalName(StringUtils.join(fkey.getParts(), "_"), null, false, false);
        return ColumnInfo.propNameFromName(r).toLowerCase();
    }

    protected void convertDateColumn(CachedResultSet rs, String fromCol, String toCol) throws SQLException
    {
        int from = rs.findColumn(fromCol);
        int to = rs.findColumn(toCol);

        while (rs.next())
        {
            Object o = rs.getObject(from);

            if (o != null)
            {
                Date d = null;

                if (o instanceof Date)
                {
                    d = (Date) o;
                }
                else
                {
                    String s = String.valueOf(o);

                    try
                    {
                        d = new Date(DateUtil.parseDateTime(s));
                    }
                    catch (ConversionException x)
                    {
                        try
                        {
                            d = new Date(DateUtil.parseDateTime(s.replace('-', ' ')));
                        }
                        catch (ConversionException y)
                        {
                        }
                    }
                }

                rs._setObject(to, d);
            }
        }

        rs.beforeFirst();
    }

    protected CachedResultSet filterDateRange(CachedResultSet rs, String dateColumn, Date start, Date end) throws SQLException
    {
        int col = rs.findColumn(dateColumn);
        if (null == start && null == end)
            return rs;
        int size = rs.getSize();
        ArrayList<Map<String, Object>> rows = new ArrayList<>(size);
        rs.beforeFirst();

        while (rs.next())
        {
            Date d = rs.getTimestamp(col);
            if (null == d || null != start && start.compareTo(d) > 0 || null != end && end.compareTo(d) <= 0)
                continue;
            rows.add(rs.getRowMap());
        }

        CachedResultSet ret;

        if (rs.getSize() == rows.size())
            ret = rs;
        else
        {
            // rs is a CachedResultSet, so its meta data is cached. No need to cache it again
            ret = CachedResultSets.create(rs.getMetaData(), false, rows, true);
            rs.close();
        }

        ret.beforeFirst();

        return ret;
    }

    protected List<Filter> getFilters()
    {
        ReportDescriptor d = getDescriptor();
        List<Filter> filters = new ArrayList<>(20);
        for (int i = 0; i < 20; i++)
        {
            Filter f = new Filter(d, i);
            if (f.isValid())
                filters.add(f);
        }

        return filters;
    }

    protected ResultSet generateResultSet(ViewContext context) throws Exception
    {
        String wellURL = new ActionURL(WellController.ShowWellAction.class, context.getContainer()).addParameter("wellId", "").getLocalURIString();
        String runURL = new ActionURL(RunController.ShowRunAction.class, context.getContainer()).addParameter("runId", "").getLocalURIString();
        Date startDate = null;
        Date endDate = null;

        // UNDONE SQL ENCODING
        StringBuilder query = new StringBuilder();
        query.append("SELECT\n");
        query.append("  A.RowId,\n");
        query.append("  A.LSID,\n");
        query.append("  A.Run.Name AS run,\n");
        query.append("  ").append(toSQL(runURL)).append(" || CONVERT(A.Run, SQL_VARCHAR) AS \"run.href\",\n");
        query.append("  A.Name AS well,\n");
        query.append("  ").append(toSQL(wellURL)).append(" || CONVERT(A.RowID, SQL_VARCHAR) AS \"well.href\",\n");
        query.append("  A.FCSFile.Keyword.\"EXPORT TIME\" AS Xdatetime,\n");
        query.append("  NULL AS datetime,\n");
        addSelectList(context, "A", query);
        query.append("FROM FCSAnalyses A");
        String and = "\nWHERE ";

        // UNDONE: use SimpleFilter instead of FilterFlowReport.Filter
        Set<FieldKey> fieldKeys = new HashSet<>();
        SimpleFilter filter = new SimpleFilter();
        List<Filter> filters = getFilters();
        for (Filter f : filters)
        {
            if ("keyword".equals(f.type))
            {
                if ("EXPORT TIME".equals(f.property))
                {
                    if (CompareType.GTE.getPreferredUrlKey().equals(f.op) && !StringUtils.isEmpty(f.value))
                        try
                        {
                            startDate = new Date(DateUtil.parseDateTime(f.value));
                        }
                        catch (ConversionException x)
                        {
                        }
                    if (CompareType.LT.getPreferredUrlKey().equals(f.op) && !StringUtils.isEmpty(f.value))
                        try
                        {
                            endDate = new Date(DateUtil.parseDateTime(f.value));
                        }
                        catch (ConversionException x)
                        {
                        }
                    continue;
                }
                FieldKey key = FieldKey.fromParts("FCSFile", "Keyword", f.property);
                filter.addCondition("A/" + key.toString(), f.value, CompareType.EQUAL);
                fieldKeys.add(key);
            }
            else if ("sample".equals(f.type))
            {
                FieldKey key = FieldKey.fromParts("FCSFile", "Sample", "Property", f.property);
                filter.addCondition("A/" + key.toString(), f.value, CompareType.EQUAL);
                fieldKeys.add(key);
            }
            else if ("statistic".equals(f.type) || "background".equals(f.type))
            {
                String table = f.type.equals("statistic") ? "Statistic" : "Background";
                FieldKey key = FieldKey.fromParts(table, f.property);
                filter.addCondition("A/" + key.toString(), f.value, CompareType.getByURLKey(f.op));
                fieldKeys.add(key);
            }
            else if ("fieldkey".equals(f.type))
            {
                FieldKey key = FieldKey.fromString(f.property);
                filter.addCondition("A/" + key.toString(), f.value, CompareType.getByURLKey(f.op));
                fieldKeys.add(key);
            }
        }

        QuerySchema flow = new FlowSchema(context);
        TableInfo fcsAnalysesTable = flow.getTable(FlowTableType.FCSAnalyses.toString());
        Map<FieldKey, ColumnInfo> columnMap = QueryService.get().getColumns(fcsAnalysesTable, fieldKeys);

        // Add "A" prefix to column map FieldKeys
        Map<FieldKey, ColumnInfo> prefixedColumnMap = new HashMap<>(columnMap.size());
        FieldKey A = new FieldKey(null, "A");
        for (Map.Entry<FieldKey, ColumnInfo> entry : columnMap.entrySet())
        {
            prefixedColumnMap.put(FieldKey.fromParts(A, entry.getKey()), entry.getValue());
        }

        for (SimpleFilter.FilterClause clause : filter.getClauses())
        {
            query.append(and);
            query.append(clause.getLabKeySQLWhereClause(prefixedColumnMap));
            and = " AND\n";
        }

        _query = query.toString();
        ResultSet rs = QueryService.get().select(flow, _query);
        convertDateColumn((CachedResultSet) rs, "Xdatetime", "datetime");
        rs = filterDateRange((CachedResultSet) rs, "datetime", startDate, endDate);
        return rs;
    }

    /** Add any additional columns to the query select list. */
    abstract void addSelectList(ViewContext context, String tableName, StringBuilder query);

    public HttpView renderReport(ViewContext context) throws Exception
    {
        RReport r = getInnerReport();
        HttpView plot = r.renderReport(context);
        return new VBox(
                plot,
                new HtmlView("<textarea style='display:none;' cols=120 rows=30>" + PageFlowUtil.filter(_query) + "</textarea>")
        );
    }

    public void updateFilterProperties(PropertyValues pvs)
    {
        ReportDescriptor d = getDescriptor();

        // delete all previous
        for (String key : getDescriptor().getProperties().keySet())
        {
            if (key.startsWith("filter["))
                d.setProperty(key, null);
        }

        int count = 0;
        for (int i = 0; i < 20; i++)
        {
            Filter f = new Filter(pvs, i);
            if (f.isValid())
            {
                d.setProperty("filter[" + count + "].property", f.property);
                d.setProperty("filter[" + count + "].type", f.type);
                d.setProperty("filter[" + count + "].value", f.value);
                d.setProperty("filter[" + count + "].op", null == f.op ? "eq" : f.op);
                count++;
            }
        }
    }

    public String toSQL(String s)
    {
        return null == s ? "''" : "'" + StringUtils.replace(s, "'", "\'\'") + "'";
    }

    public String toSQL(FieldKey fieldKey)
    {
        String sep = "";
        StringBuilder sb = new StringBuilder();
        for (String part : fieldKey.getParts())
        {
            sb.append(sep).append("\"").append(part).append("\"");
            sep = ".";
        }
        return sb.toString();
    }

    public static class Filter
    {
        public String property;
        public String type;
        public String value;
        public String op = "eq";

        private String _get(PropertyValues pvs, String key)
        {
            PropertyValue pv = pvs.getPropertyValue(key);
            return null == pv ? null : pv.getValue() == null ? null : String.valueOf(pv.getValue());
        }

        Filter(PropertyValues pvs, int i)
        {
            property = _get(pvs, "filter[" + i + "].property");
            type = _get(pvs, "filter[" + i + "].type");
            value = _get(pvs, "filter[" + i + "].value");
            String op = _get(pvs, "filter[" + i + "].op");

            // HACK: The Ext.form.FormPanel uses form submit which takes form values directly from
            // the <input> element value instead of using form.getForm().getFieldValues() to get
            // the actual component value.  I would use .getFieldValues() and submit myself, but
            // the 'compositefield' I'm using in the form layout doesn't work with .getFieldValues().
            // After 11.2, we need to change the edit*Report.jsp pages to use better layout that
            // doesn't require the busted 'compositefield' and use .getFieldValues() instead of basic form submit.
            CompareType compareType = fromDisplayValue(op);
            if (compareType == null)
                compareType = CompareType.getByURLKey(op);
            if (compareType != null)
                this.op = compareType.getPreferredUrlKey();
        }

        public Filter(ReportDescriptor d, int i)
        {
            property = d.getProperty("filter[" + i + "].property");
            type = d.getProperty("filter[" + i + "].type");
            value = d.getProperty("filter[" + i + "].value");
            op = d.getProperty("filter[" + i + "].op");
        }

        public Filter(String property, String type, String value, String op)
        {
            this.property = property;
            this.type = type;
            this.value = value;
            this.op = op;
        }

        boolean isValid()
        {
            return !StringUtils.isEmpty(property) &&
                    !StringUtils.isEmpty(value) &&
                    ("keyword".equals(type) || "sample".equals(type) || "statistic".equals(type) || "fieldkey".equals(type));
        }

        private static CompareType fromDisplayValue(String displayValue)
        {
            if (displayValue == null || displayValue.length() == 0)
                return null;

            for (CompareType ct : CompareType.values())
            {
                if (displayValue.equals(ct.getDisplayValue()))
                    return ct;
            }

            return null;
        }

        SimpleFilter toSimpleFilter()
        {
            CompareType compareType = CompareType.getByURLKey(op);
            if (compareType == null)
                return null;

            String colName;
            if (type.equals("keyword"))
                colName = "FCSFile.Keyword.\"" + property + "\"";
            else if (type.equals("sample"))
                colName = "FCSFile.Sample.\"" + property + "\"";
            else if (type.equals("statistic"))
                colName = "Statistic.\"" + property + "\"";
            else
                throw new RuntimeException();

            return new SimpleFilter(colName, value, compareType);
        }

        public boolean equals(Filter otherFilter)
        {
            return Objects.equals(property, otherFilter.property)
                    && Objects.equals(value, otherFilter.value)
                    && Objects.equals(op, otherFilter.op)
                    && Objects.equals(type, otherFilter.type);
        }
    }

    // Move from annonymous class to static inner class that takes a filterFlowReport to allow de-serialization in Java 7
    private static class FilterFlowInnerReport extends RReport
    {
        private final FilterFlowReport _report;

        // No-args constructor to support de-serialization in Java 7
        @SuppressWarnings({"UnusedDeclaration"})
        private FilterFlowInnerReport()
        {
            _report = null;
        }

        public FilterFlowInnerReport(FilterFlowReport filterFlowReport)
        {
            _report = filterFlowReport;
        }

        @Override
        public Results generateResults(ViewContext context, boolean allowAsyncQuery) throws Exception
        {
            ResultSet rs = _report.generateResultSet(context);
            return rs == null ? null : new ResultsImpl(rs);
        }

        @Override
        protected String getScriptProlog(ScriptEngine engine, ViewContext context, File inputFile, Map<String, Object> inputParameters, boolean isRStudio)
        {
            String labkeyProlog = super.getScriptProlog(engine, context, inputFile, inputParameters, isRStudio);

            StringBuffer reportProlog = new StringBuffer(labkeyProlog);
            reportProlog.append("report.parameters <- list(");
            ReportDescriptor d = _report.getDescriptor();
            Map<String, Object> props = d.getProperties();
            String comma = "";

            for (Map.Entry<String, Object> e : props.entrySet())
            {
                String key = e.getKey();
                if (ScriptReportDescriptor.Prop.script.name().equals(key))
                    continue;
                String value = null == e.getValue() ? null : String.valueOf(e.getValue());
                reportProlog.append(comma);
                reportProlog.append(toR(e.getKey())).append("=").append(toR(value));
                comma = ",";
            }

            reportProlog.append(")\n");
            _report.addScriptProlog(context, reportProlog);
            return reportProlog.toString();
        }

        @Override
        public File getReportDir(@NotNull String executingContainerId)
        {
            // Issue 12625: Create unique directory for the background report job
            boolean isPipeline = _report.saveToDomain();
            return super.getReportDir(executingContainerId, isPipeline);
        }
    }

    protected boolean filterListEqual(List<Filter> otherFilters)
    {
        List<Filter> myFitlers = getFilters();
        if (myFitlers.size() != otherFilters.size())
            return false;
        else
        {
            for (int i = 0; i < myFitlers.size(); i++)
            {
                if (!myFitlers.get(i).equals(otherFilters.get(i)))
                    return false;
            }

            return true;
        }
    }

    protected boolean hasContentModified(ContainerUser context, String descriptorPropName)
    {
        // Content modified if descriptorProp is changed or any of the filter values are changed
        String newPropStr = getDescriptor().getProperty(descriptorPropName);

        if (getReportId() != null)
        {
            FilterFlowReport origReport = (FilterFlowReport) ReportService.get().getReport(context.getContainer(), getReportId().getRowId());
            if (origReport != null)
            {
                String origPropStr = origReport.getDescriptor().getProperty(descriptorPropName);

                return (newPropStr != null && !newPropStr.equals(origPropStr))
                        || !filterListEqual(origReport.getFilters());
            }
        }

        return false;
    }
}
