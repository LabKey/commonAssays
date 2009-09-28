<%@ page import="org.labkey.flow.reports.ControlsQCReport" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.flow.reports.ControlsQCReport" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.reports.report.ReportDescriptor" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ControlsQCReport report = (ControlsQCReport) HttpView.currentModel();
    ReportDescriptor d = report.getDescriptor();
    String reportId = d.getReportId() == null ? null : d.getReportId().toString();
%>
<div id="form"></div>
<script type="text/javascript">

var form;
var report =
{
    reportId:<%=PageFlowUtil.jsString(reportId)%>,
    name:<%=PageFlowUtil.jsString(d.getReportName())%>,
    statistic:<%=PageFlowUtil.jsString(d.getProperty("statistic"))%>,
    filter :
    [<%
    String comma = "";
    for (int i=0 ; i<10 ; i++)
    {
        ControlsQCReport.Filter f = new ControlsQCReport.Filter(d,i);
        %><%=comma%>{property:<%=PageFlowUtil.jsString(f.property)%>, value:<%=PageFlowUtil.jsString(f.value)%>, type:<%=PageFlowUtil.jsString(f.type)%>}<%
        comma =",";
    }
    %>]
};


function Form_onSave()
{
    form.getForm().submit({
        success:function(form, action)
        {
            report.reportId = action.result.reportId;
            window.location="execute.view?reportId=" + encodeURIComponent(report.reportId);
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
    for (i=0; i<report.filter.length;i++)
    {
        if (report.filter[i].type == 'keyword')
            keyword.push(report.filter[i]);
        if (report.filter[i].type == 'sample')
            sample.push(report.filter[i]);
    }
    for (i=1;i<=2;i++)
    {
        if (keyword.length<i) keyword.push({property:null, value:null});
        if (sample.length<i) sample.push({property:null, value:null});
    }

    form = new Ext.form.FormPanel({
        url:window.location,
        defaults:{msgTarget:'side'},
        border:false,
        defaultType: 'textfield',
        items:[
            {fieldLabel:'Name', name:'reportName', value:report.name, allowBlank:false},
            {fieldLabel:'Statistic', name:'statistic', value:report.statistic, allowBlank:false},

            {fieldLabel:'Keyword', name:'filter[0].property', value:keyword[0].property},
            {xtype:'hidden', name:'filter[0].type', value:'keyword'},
            {fieldLabel:'Value', name:'filter[0].value', value:keyword[0].value},

            {fieldLabel:'Keyword', name:'filter[1].property', value:keyword[1].property},
            {xtype:'hidden', name:'filter[1].type', value:'keyword'},
            {fieldLabel:'Value', name:'filter[1].value', value:keyword[1].value},

            {fieldLabel:'Sample Property', name:'filter[2].property', value:sample[0].property},
            {xtype:'hidden', name:'filter[2].type', value:'sample'},
            {fieldLabel:'Value', name:'filter[2].value', value:sample[0].value},

            {fieldLabel:'Sample Property', name:'filter[3].property', value:sample[1].property},
            {xtype:'hidden', name:'filter[3].type', value:'sample'},
            {fieldLabel:'Value', name:'filter[3].value', value:sample[1].value},
        ],
        buttons:[
            {text:'Save', handler:Form_onSave},
            {text:'Cancel', handler:Form_onCancel},
            {text:'Delete', handler:Form_onDelete}
        ]
    });
    form.render('form');
});
</script>