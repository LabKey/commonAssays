/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.CachedRowSetImpl;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.RReportDescriptor;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.query.FlowSchema;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Sep 1, 2009
 * Time: 3:30:40 PM
 */
public class ControlsQCReport extends FlowReport
{
    public String getType()
    {
        return "Flow.QCControlReport";
    }


    public String getTypeDescription()
    {
        return "Flow Controls Statistics over Time";
    }


    RReport _inner = null;
    String _query = null;

    public static class Filter
    {
        public String property;
        public String type;
        public String value;
        public String op="eq";

        private String _get(PropertyValues pvs, String key)
        {
            PropertyValue pv = pvs.getPropertyValue(key);
            return null == pv ? null : pv.getValue() == null ? null : String.valueOf(pv.getValue());
        }
        
        Filter(PropertyValues pvs, int i)
        {
            property = _get(pvs,"filter[" + i + "].property");
            type = _get(pvs,"filter[" + i + "].type");
            value = _get(pvs,"filter[" + i + "].value");
            op = _get(pvs, "filter[" + i + "].op");
        }

        public Filter(ReportDescriptor d, int i)
        {
            property = d.getProperty("filter[" + i + "].property");
            type = d.getProperty("filter[" + i + "].type");
            value = d.getProperty("filter[" + i + "].value");
            op = d.getProperty("filter[" + i + "].op");
        }

        boolean isValid()
        {
            return !StringUtils.isEmpty(property) &&
                    !StringUtils.isEmpty(value) &&
                    ("keyword".equals(type) || "sample".equals(type));
        }
    }


    RReport getInnerReport() throws IOException
    {
        if (null == _inner)
        {
            _inner = new RReport()
            {
                @Override
                public Results generateResults(ViewContext context) throws Exception
                {
                    ResultSet rs = ControlsQCReport.this.generateResultSet(context);
                    return rs == null ? null : new Results(rs,null);
                }

                @Override
                protected String getScriptProlog(ViewContext context, File inputFile)
                {
                    String labkeyProlog = super.getScriptProlog(context, inputFile);

                    StringBuffer reportProlog = new StringBuffer(labkeyProlog);
                    reportProlog.append("report.parameters <- list(");
                    ReportDescriptor d = ControlsQCReport.this.getDescriptor();
                    Map<String,Object> props = d.getProperties();
                    String comma = "";
                    for (Map.Entry<String,Object> e : props.entrySet())
                    {
                        String key = e.getKey();
                        if (RReportDescriptor.Prop.script.name().equals(key))
                            continue;
                        String value = null == e.getValue() ? null : String.valueOf(e.getValue());
                        reportProlog.append(comma);
                        reportProlog.append(toR(e.getKey())).append("=").append(toR(value));
                        comma=",";
                    }
                    reportProlog.append(")\n");
                    return reportProlog.toString();
                }
            };

            String script = getScriptResource("qc.R");
            _inner.setScriptSource(script);
        }
        return _inner;
    }



    private void convertDateColumn(CachedRowSetImpl rs, String fromCol, String toCol) throws SQLException
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
                    d = (Date)o;
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
                            d = new Date(DateUtil.parseDateTime(s.replace('-',' ')));
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


    private CachedRowSetImpl filterDateRange(CachedRowSetImpl rs, String dateColumn, Date start, Date end) throws SQLException
    {
        int col = rs.findColumn(dateColumn);
        if (null == start && null == end)
            return rs;
        int size = rs.getSize();
        ArrayList<Map<String,Object>> rows = new ArrayList<Map<String,Object>>(size);
        rs.beforeFirst();
        while (rs.next())
        {
            Date d = rs.getTimestamp(col);
            if (null == d || null != start && start.compareTo(d) > 0 || null != end && end.compareTo(d) <= 0)
                continue;
            rows.add(rs.getRowMap());
        }
        CachedRowSetImpl ret;
        if (rs.getSize() == rows.size())
            ret = rs;
        else
        {
            ret = new CachedRowSetImpl(rs.getMetaData(), rows, true);
            rs.close();
        }
        ret.beforeFirst();
        return ret;
    }


    ResultSet generateResultSet(ViewContext context) throws Exception
    {
        ReportDescriptor d = getDescriptor();
        ArrayList<Filter> filters = new ArrayList<Filter>();
        for (int i=0 ; i<20 ; i++)
        {
            Filter f = new Filter(d,i);
            if (f.isValid())
                filters.add(f);
        }

        String statistic = d.getProperty("statistic");
        String wellURL = new ActionURL(WellController.ShowWellAction.class,context.getContainer()).addParameter("wellId","").getLocalURIString();
        String runURL = new ActionURL(RunController.ShowRunAction.class,context.getContainer()).addParameter("runId","").getLocalURIString();
        Date startDate = null;
        Date endDate = null;

        // UNDONE SQL ENCODING
        StringBuffer query = new StringBuffer();
        query.append("SELECT\n");
        query.append("  A.Run.Name AS run,\n");
        query.append("  " + toSQL(runURL) + " || CONVERT(A.Run, SQL_VARCHAR) AS \"run.href\",\n");
        query.append("  A.RowId AS well,\n");
        query.append("  " + toSQL(wellURL) + " || CONVERT(A.RowID, SQL_VARCHAR) AS \"well.href\",\n");
        query.append("  A.FCSFile.Keyword.\"EXPORT TIME\" AS Xdatetime,\n");
        query.append("  NULL AS datetime,\n");
        query.append("  A.Statistic(" + toSQL(statistic) + ") AS value,\n");
        query.append("  " + toSQL(statistic) + " AS statistic\n");
        query.append("FROM FCSAnalyses A");
        String and = "\nWHERE ";
        for (Filter f : filters)
        {
            if ("keyword".equals(f.type))
            {
                if ("EXPORT TIME".equals(f.property))
                {
                    if ("gte".equals(f.op) && !StringUtils.isEmpty(f.value))
                        try {startDate=new Date(DateUtil.parseDateTime(f.value));}catch(ConversionException x){}
                    if ("lt".equals(f.op) && !StringUtils.isEmpty(f.value))
                        try {endDate=new Date(DateUtil.parseDateTime(f.value));}catch(ConversionException x){}
                    continue;
                }
                query.append(and);
                query.append("A.FCSFile.Keyword.\"" + f.property + "\" = " + toSQL(f.value));
                and = " AND ";
            }
            else if ("sample".equals(f.type))
            {
                query.append(and);
                query.append("A.FCSFile.Sample.Property.\"" + f.property + "\" = " + toSQL(f.value));
                and = " AND\n";
            }
        }
        _query = query.toString();
        QuerySchema flow = new FlowSchema(context);
        ResultSet rs = QueryService.get().select(flow, _query);
        convertDateColumn((CachedRowSetImpl)rs, "Xdatetime", "datetime");
        rs = filterDateRange((CachedRowSetImpl)rs, "datetime", startDate, endDate);
        return rs;
    }
    

    boolean validateParameters()
    {
        return true;
    }


    public HttpView renderReport(ViewContext context) throws Exception
    {
        RReport r = getInnerReport();
        HttpView plot = r.renderReport(context);
        return new VBox(
                plot,
                new HtmlView(PageFlowUtil.filter(_query, true))
                );
    }


    public HttpView getConfigureForm()
    {
        return new JspView<ControlsQCReport>(ControlsQCReport.class, "editQCReport.jsp", this);
    }


    public boolean updateProperties(PropertyValues pvs, BindException errors, boolean override)
    {
        super.updateBaseProperties(pvs, errors, override);
        updateFromPropertyValues(pvs,"statistic");
        if (!override)
            updateFilterProperties(pvs);
        return true;
    }

    
    public void updateFilterProperties(PropertyValues pvs)
    {
        ReportDescriptor d = getDescriptor();

        // delete all previous
        for (String key : getDescriptor().getProperties().keySet())
        {
            if (key.startsWith("filter["))
                d.setProperty(key,null);
        }

        int count=0;
        for (int i=0 ; i<20 ; i++)
        {
            Filter f = new Filter(pvs, i);
            if (f.isValid())
            {
                d.setProperty("filter[" + count + "].property", f.property);
                d.setProperty("filter[" + count + "].type", f.type);
                d.setProperty("filter[" + count + "].value", f.value);
                d.setProperty("filter[" + count + "].op", null==f.op?"eq":f.op);
                count++;
            }
        }
    }


    String toSQL(String s)
    {
        return null==s ? "''" : "'" + StringUtils.replace(s,"'","\'\'") + "'";
    }


    String toR(String s)
    {
        String r = PageFlowUtil.jsString(s);
        return "\"" + StringUtils.strip(r,"'") + "\"";
    }
}
