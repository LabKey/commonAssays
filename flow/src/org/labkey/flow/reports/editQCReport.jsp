<%
/*
 * Copyright (c) 2009-2013 LabKey Corporation
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
%>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.flow.controllers.ReportsController" %>
<%@ page import="org.labkey.flow.reports.FilterFlowReport" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!

    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromFilePath("clientapi/ext3"));
        return resources;
    }
%>
<%
    Container c = getContainer();

    Pair<FilterFlowReport, ActionURL> bean = (Pair<FilterFlowReport, ActionURL>) HttpView.currentModel();
    FilterFlowReport report = bean.first;
    ActionURL returnURL = bean.second;
    ReportDescriptor d = report.getDescriptor();
    String reportId = d.getReportId() == null ? null : d.getReportId().toString();

    String retURL = returnURL == null ? buildURL(ReportsController.BeginAction.class) : returnURL.getLocalURIString();
%>
<style>
    .x-form-item { margin:2px;} 
</style>
<div id="form"></div>
<script type="text/javascript">

var form;
var report =
{
    reportId:<%=PageFlowUtil.jsString(reportId)%>,
    name:<%=PageFlowUtil.jsString(d.getReportName())%>,
    description:<%=PageFlowUtil.jsString(d.getReportDescription())%>,
    statistic:<%=PageFlowUtil.jsString(d.getProperty("statistic"))%>,
    filter :
    [<%
    String comma = "";
    for (int i=0 ; i<10 ; i++)
    {
        FilterFlowReport.Filter f = new FilterFlowReport.Filter(d,i);
        %><%=text(comma)%>{
            property:<%=q(f.property)%>,
            value:<%=q(f.value)%>,
            type:<%=q(f.type)%>,
            op:<%=text(null==f.op?q("eq"):q(f.op))%>}<%
        comma =",";
    }
    %>]
};


function Form_onSave()
{
    Ext.getBody().mask("Saving...");
    form.getForm().submit({
        success:function(form, action)
        {
            report.reportId = action.result.reportId;
            window.location = <%=q(retURL)%>;
        },
        failure:function(form,action)
        {
            Ext.getBody().unmask();
        }
    });
}


function Form_onCancel()
{
    window.location = <%=q(retURL)%>;
}


function Form_onDelete()
{
   <%
   ActionURL url = null;
   if (d.getReportId() != null)
   {
       url = new ActionURL(ReportsController.DeleteAction.class, c).addParameter("reportId", report.getReportId().toString());
       if (returnURL != null)
           url.addReturnURL(returnURL);
   }
   else if (returnURL != null)
   {
       url = returnURL;
   }
   else
   {
       url = new ActionURL(ReportsController.BeginAction.class, c);
   }
   %>
   window.location = <%=PageFlowUtil.jsString(url.getLocalURIString())%>
}

Ext.onReady(function() {

    var i;
    var keyword = [];
    var sample = [];
    var statistic = [];
    var startDate = null;
    var endDate = null;
    
    for (i=0; i<report.filter.length;i++)
    {
        var f = report.filter[i];
        if (f.type == 'keyword')
        {
            if (f.property == 'EXPORT TIME' && f.op == 'gte')
                startDate = f.value;
            else if (f.property == 'EXPORT TIME' && f.op == 'lt')
                endDate = f.value;
            else
                keyword.push(f);
        }
        else if (f.type == 'sample')
            sample.push(f);
        else if (f.type == 'statistic')
            statistic.push(f);
    }
    for (i=1;i<=2;i++)
    {
        if (keyword.length<i) keyword.push({property:null, value:null});
        if (sample.length<i) sample.push({property:null, value:null});
        if (statistic.length<i) statistic.push({property:null, value:null});
    }

    var spacer = {xtype:'spacer', height:15};

    form = new LABKEY.ext.FormPanel({
        url:window.location,
        defaults:{msgTarget:'side', width:700},
        border:false,
        defaultType: 'textfield',
        items:[
            {fieldLabel:'Name', name:'reportName', value:report.name, allowBlank:false},
            {fieldLabel:'Description', name:'reportDescription', value:report.description, allowBlank:true},

            spacer,
            {fieldLabel:'Statistic', name:'statistic', xtype:'statisticField', value:report.statistic, allowBlank:false},

            spacer,
            spacer,

            {xtype:'compositefield', fieldLabel: 'Keyword', items: [
                {xtype:'hidden', name:'filter[0].type', value:'keyword'},
                {xtype:'combo', name:'filter[0].property', store:FlowPropertySet.keywords, value:keyword[0].property},
                {xtype:'textfield', name:'filter[0].value', value:keyword[0].value}
            ]},

            spacer,

            {xtype:'compositefield', fieldLabel: 'Keyword', items: [
                {xtype:'hidden', name:'filter[1].type', value:'keyword'},
                {xtype:'combo', name:'filter[1].property', store:FlowPropertySet.keywords, value:keyword[1].property},
                {xtype:'textfield', name:'filter[1].value', value:keyword[1].value}
            ]},

            spacer,

            {xtype:'compositefield', fieldLabel: 'Sample Property', items: [
                {xtype:'hidden', name:'filter[2].type', value:'sample'},
                {xtype:'combo', name:'filter[2].property', store:SampleSet.properties, value:sample[0].property},
                {xtype:'textfield', name:'filter[2].value', value:sample[0].value}
            ]},

            spacer,

            {xtype:'compositefield', fieldLabel: 'Sample Property', items: [
                {xtype:'hidden', name:'filter[3].type', value:'sample'},
                {xtype:'combo', name:'filter[3].property', store:SampleSet.properties, value:sample[1].property},
                {xtype:'textfield', name:'filter[3].value', value:sample[1].value}
            ]},

            {xtype:'hidden', name:'filter[4].type', value:'statistic'},
            {xtype:'statisticField', fieldLabel: 'Statistic', name:'filter[4].property', value:statistic[0].property},
            {xtype:'compositefield', items: [
                {xtype:'opCombo', name:'filter[4].op', value:statistic[0].op},
                {xtype:'textfield', name:'filter[4].value', value:statistic[0].value}
            ]},

            spacer,

            {xtype:'hidden', name:'filter[5].type', value:'statistic'},
            {xtype:'statisticField', fieldLabel: 'Statistic', name:'filter[5].property', value:statistic[1].property},
            {xtype:'compositefield', items: [
                {xtype:'opCombo', name:'filter[5].op', value:statistic[1].op},
                {xtype:'textfield', name:'filter[5].value', value:statistic[1].value}
            ]},

            spacer,
            spacer,

            {xtype:'hidden', name:'filter[6].type', value:'keyword'},
            {xtype:'hidden', name:'filter[6].property', value:'EXPORT TIME'},
            {xtype:'hidden', name:'filter[6].op', value:'gte'},
            {xtype:'datefield', fieldLabel:'On or After', name:'filter[6].value', value:startDate},

            {xtype:'hidden', name:'filter[7].type', value:'keyword'},
            {xtype:'hidden', name:'filter[7].property', value:'EXPORT TIME'},
            {xtype:'hidden', name:'filter[7].op', value:'lt'},
            {xtype:'datefield', fieldLabel:'Before', name:'filter[7].value', value:endDate}

        ],
        buttons:[
            {text:'Save', handler:Form_onSave},
            {text:'Cancel', handler:Form_onCancel},
            {text:'Delete', handler:Form_onDelete}
        ],
        buttonAlign:'left'
    });
    form.render('form');
});
</script>

<%
    JspView<Object> statPicker = new JspView<>(FlowReport.class, "statPicker.jsp", null, null);
    statPicker.include(statPicker, out);
%>
