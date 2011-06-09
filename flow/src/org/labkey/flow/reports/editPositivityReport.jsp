<%
/*
 * Copyright (c) 2011 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page import="org.labkey.flow.reports.FilterFlowReport" %>
<%@ page import="org.labkey.flow.reports.PositivityFlowReport" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    PositivityFlowReport report = (PositivityFlowReport) HttpView.currentModel();
    ReportDescriptor d = report.getDescriptor();
    String reportId = d.getReportId() == null ? null : d.getReportId().toString();
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
    subset:<%=PageFlowUtil.jsString(d.getProperty("subset"))%>,
    filter :
    [<%
    String comma = "";
    for (int i=0 ; i<10 ; i++)
    {
        FilterFlowReport.Filter f = new FilterFlowReport.Filter(d,i);
        %><%=comma%>{
             property:<%=q(f.property)%>,
             value:<%=q(f.value)%>,
             type:<%=q(f.type)%>,
             op:<%=null==f.op?q("eq"):q(f.op)%>}<%
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
            window.location="execute.view?reportId=" + encodeURIComponent(report.reportId);
        },
        failure:function(form,action)
        {
            Ext.getBody().unmask();
        }
    });
}


function Form_onCancel()
{
   window.location = "begin.view";
}


function Form_onDelete()
{
   if (report.reportId)
       window.location = "delete.view?reportId=" + encodeURIComponent(report.reportId);
   else
       Form_onCancel();
}

Ext.onReady(function() {

    var i;
    var keyword = [];
    var sample = [];
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
    }
    for (i=1;i<=2;i++)
    {
        if (keyword.length<i) keyword.push({property:null, value:null});
        if (sample.length<i) sample.push({property:null, value:null});
    }

    var spacer = {xtype:'spacer', minHeight:5, html:'<hr width=20 size=1 color=gray>', style:{'margin-left':'75px'}};

    form = new Ext.form.FormPanel({
        url:window.location,
        defaults:{msgTarget:'side', width:700},
        border:false,
        defaultType: 'textfield',
        items:[
            {fieldLabel:'Name', name:'reportName', value:report.name, allowBlank:false},
            {fieldLabel:'Description', name:'reportDescription', value:report.description, allowBlank:true},
            {fieldLabel:'Subset', name:'subset', xtype:'subsetField', value:report.subset, allowBlank:false},

            spacer,

            {xtype:'hidden', name:'filter[0].type', value:'keyword'},
            {fieldLabel:'Keyword', name:'filter[0].property', xtype:'combo', store:FlowPropertySet.keywords, value:keyword[0].property},
            {fieldLabel:'Value', name:'filter[0].value', value:keyword[0].value},

            spacer,

            {xtype:'hidden', name:'filter[1].type', value:'keyword'},
            {fieldLabel:'Keyword', name:'filter[1].property', xtype:'combo', store:FlowPropertySet.keywords, value:keyword[1].property},
            {fieldLabel:'Value', name:'filter[1].value', value:keyword[1].value},

            spacer,

            {xtype:'hidden', name:'filter[2].type', value:'sample'},
            {fieldLabel:'Sample Property', name:'filter[2].property', xtype:'combo', store:SampleSet.properties, value:sample[0].property},
            {fieldLabel:'Value', name:'filter[2].value', value:sample[0].value},

            spacer,

            {xtype:'hidden', name:'filter[3].type', value:'sample'},
            {fieldLabel:'Sample Property', name:'filter[3].property', xtype:'combo', store:SampleSet.properties, value:sample[1].property},
            {fieldLabel:'Value', name:'filter[3].value', value:sample[1].value},

            spacer,

            {xtype:'hidden', name:'filter[4].type', value:'keyword'},
            {xtype:'hidden', name:'filter[4].property', value:'EXPORT TIME'},
            {xtype:'hidden', name:'filter[4].op', value:'gte'},
            {xtype:'datefield', fieldLabel:'On or After', name:'filter[4].value', value:startDate},

            {xtype:'hidden', name:'filter[5].type', value:'keyword'},
            {xtype:'hidden', name:'filter[5].property', value:'EXPORT TIME'},
            {xtype:'hidden', name:'filter[5].op', value:'lt'},
            {xtype:'datefield', fieldLabel:'Before', name:'filter[5].value', value:endDate}
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
    JspView<Object> statPicker = new JspView<Object>(FlowReport.class, "statPicker.jsp", null, null);
    statPicker.include(statPicker, out);
%>

