<%
/*
 * Copyright (c) 2011-2018 LabKey Corporation
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
<%@ page import="org.labkey.api.util.Tuple3" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.flow.controllers.ReportsController.BeginAction" %>
<%@ page import="org.labkey.flow.controllers.ReportsController.DeleteAction" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController.EditICSMetadataAction" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.data.ICSMetadata" %>
<%@ page import="org.labkey.flow.reports.FilterFlowReport" %>
<%@ page import="org.labkey.flow.reports.PositivityFlowReport" %>
<%@ page import="org.labkey.flow.reports.StatPickerView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext3");
        dependencies.add("Flow/flowReport.js");
    }
%>
<%
    Container c = getContainer();

    Tuple3<PositivityFlowReport, ActionURL, ActionURL> bean = (Tuple3<PositivityFlowReport, ActionURL, ActionURL>) HttpView.currentModel();
    PositivityFlowReport report = bean.first;
    ActionURL returnURL = bean.second;
    ActionURL cancelURL = bean.third;
    ReportDescriptor d = report.getDescriptor();
    String reportId = d.getReportId() == null ? null : d.getReportId().toString();

    ActionURL retURL = returnURL == null ? urlFor(BeginAction.class) : returnURL;
    ActionURL canURL = cancelURL == null ? retURL : cancelURL;

    FlowProtocol protocol = FlowProtocol.getForContainer(c);
    ICSMetadata metadata = protocol == null ? null : protocol.getICSMetadata();

    ActionURL currentURL = getActionURL();
    ActionURL editICSMetadataURL = null;
    if (protocol != null)
    {
        editICSMetadataURL = protocol.urlFor(EditICSMetadataAction.class);
        editICSMetadataURL.addReturnURL(currentURL);
    }
%>

<% if (metadata == null || !metadata.isComplete()) { %>
  <p class='labkey-message'>
  The positivity report requires metadata describing the sample and background information
  of the flow experiment before it can be run.
  <br>
  <%=link("Edit Metadata").href(editICSMetadataURL)%>
  </p>
<% } %>

<div id="labkey-error" class="labkey-error">
<labkey:errors/>
</div>
<style type="text/css">
    .x-form-item {
        margin:2px;
    }

    .labkey-text-link {
        cursor: pointer;
    }
</style>
<div id="form"></div>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">

var form;
var report =
{
    reportId:<%=q(reportId)%>,
    name:<%=q(d.getReportName())%>,
    description:<%=q(d.getReportDescription())%>,
    subset:<%=q(d.getProperty("subset"))%>,
    filter :
    [<%
    String comma = "";
    for (int i=0 ; true ; i++)
    {
        FilterFlowReport.Filter f = new FilterFlowReport.Filter(d,i);
        if (f.type == null)
            break;
        %><%=unsafe(comma)%>{
             property:<%=q(f.property)%>,
             value:<%=q(f.value)%>,
             type:<%=q(f.type)%>,
             op:<%=(null==f.op?q("eq"):q(f.op))%>}<%
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
    window.location = <%=q(canURL)%>;
}

function Form_onDelete()
{
   <%
   ActionURL url = null;
   if (d.getReportId() != null)
   {
       url = urlFor(DeleteAction.class).addParameter("reportId", report.getReportId().toString());
       if (returnURL != null)
           url.addReturnURL(returnURL);
   }
   else if (returnURL != null)
   {
       url = returnURL;
   }
   else
   {
       url = urlFor(BeginAction.class);
   }
   %>
   window.location = <%=q(url)%>;
}

// help solve link conflicts with strict CSP
function linkHelper(text, onClick) {
    const id = Ext.id();
    const html = '<a id="' + id +'" href="#" class="labkey-text-link" >' + text + '</a>';
    const callback = () => {
        LABKEY.Utils.attachEventHandler(id, "click", onClick);
    };
    return {"html": html, "callback": callback }
}

LABKEY.Utils.onReady(function() {

    var i;
    var keyword = [];
    var sample = [];
    var statistic = [];
    var fieldKey = [];
    var analysisFolder = null;
    var startDate = null;
    var endDate = null;
    var hasSampleFilters = false;

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
        {
            if (f.property && f.value)
                hasSampleFilters = true;
            sample.push(f);
        }
        else if (f.type == 'statistic')
            statistic.push(f);
        else if (f.type == 'fieldkey')
        {
            if (f.property == 'Run/RunGroups/Name' && f.op == 'eq')
                analysisFolder = f.value;
            else
                fieldKey.push(f);
        }
    }
    for (i=1;i<=1;i++)
    {
        if (keyword.length<i) keyword.push({property:null, value:null});
        if (sample.length<i) sample.push({property:null, value:null});
        if (statistic.length<i) statistic.push({property:null, value:null});
        if (fieldKey.length<i) fieldKey.push({property:null, value:null});
    }

    var spacer = {xtype:'spacer', height:15};

    var items = [
        {fieldLabel:'Name', name:'reportName', value:report.name, allowBlank:false},
        {fieldLabel:'Description', name:'reportDescription', value:report.description, allowBlank:true},
        {fieldLabel:'Subset', name:'subset', xtype:'subsetField', value:report.subset, allowBlank:false}
    ];

    var filterItems = [];

    //
    // keyword filters
    //

    var filterIdx = 0;
    for (i = 0; i < keyword.length; i++)
    {
        var filterItem = createKeywordFilter(filterIdx++, keyword[i]);
        filterItems.push(filterItem);
    }

    const keywordLink = linkHelper('Add Keyword Filter', addKeywordFilter);

    filterItems.push({
        xtype: 'displayfield',
        id: 'add-keyword-button',
        fieldLabel: '',
        hideLabel: false,
        html: keywordLink.html
    });

    filterItems.push(spacer);


    //
    // sample filters
    //

    let sampleLink;
    if (hasSampleFilters || SampleType.properties.length > 0)
    {
        for (i = 0; i < sample.length; i++)
        {
            var filterItem = createSampleFilter(filterIdx++, sample[i]);
            filterItems.push(filterItem);
        }

        sampleLink = linkHelper('Add Sample Filter', addSampleFilter);

        filterItems.push({
            xtype: 'displayfield',
            id: 'add-sample-button',
            fieldLabel: '',
            hideLabel: false,
            html: sampleLink.html
        });

        filterItems.push(spacer);
    }


    //
    // statistic filters
    //

    for (i = 0; i < statistic.length; i++)
    {
        var filterItem = createStatisticFilter(filterIdx++, statistic[i]);
        filterItems.push(filterItem);
    }

    const statisticLink = linkHelper('Add Statistic Filter', addStatisticFilter);

    filterItems.push({
        xtype: 'displayfield',
        id: 'add-statistic-button',
        fieldLabel: '',
        hideLabel: false,
        html: statisticLink.html
    });

    filterItems.push(spacer);


    //
    // generic FieldKey filters
    //

    for (i = 0; i < fieldKey.length; i++)
    {
        var filterItem = createFieldKeyFilter(filterIdx++, fieldKey[i]);
        filterItems.push(filterItem);
    }

    const filterLink = linkHelper('Add Field Filter', addFieldKeyFilter);

    filterItems.push({
        xtype: 'displayfield',
        id: 'add-fieldkey-button',
        fieldLabel: '',
        hideLabel: false,
        html: filterLink.html
    });

    filterItems.push(spacer);


    //
    // Analysis Folder filter
    //

    filterIdx++;
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].type', value:'fieldkey'});
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].property', value:'Run/RunGroups/Name'});
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].op', value:'eq'});
    filterItems.push({
        xtype: 'combo',
        fieldLabel: 'Analysis folder',
        name: 'filter[' + filterIdx + '].value',
        value: analysisFolder,
        displayField: 'Name',
        valueField: 'Name',
        allowBlank: true,
        triggerAction: 'all',
        mode: 'local',
        store: {
            xtype: 'labkey-store',
            schemaName: 'flow',
            queryName: 'Analyses',
            columns: ['Name'],
            containerPath: LABKEY.container.path,
            updatable: false,
            autoLoad: true
        }
    });

    filterItems.push(spacer);

    //
    // Add date filters
    //

    filterIdx++;
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].type', value:'keyword'});
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].property', value:'EXPORT TIME'});
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].op', value:'gte'});
    filterItems.push({xtype:'datefield', fieldLabel:'On or after', name:'filter[' + filterIdx + '].value', value:startDate});

    filterItems.push(spacer);

    filterIdx++;
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].type', value:'keyword'});
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].property', value:'EXPORT TIME'});
    filterItems.push({xtype:'hidden', name:'filter[' + filterIdx + '].op', value:'lt'});
    filterItems.push({xtype:'datefield', fieldLabel:'Before', name:'filter[' + filterIdx + '].value', value:endDate});

    items.push({
        xtype: 'box',
        html: '<div>Filters:</div>'
    },{
        xtype:'fieldset',
        id:'filtersFieldSet',
        padding: 10,
        defaults: {
            labelStyle: 'font-weight: normal;'
        },
        items: filterItems
    });

    form = new LABKEY.ext.FormPanel({
        id:'reportForm',
        url:window.location,
        defaults:{
            msgTarget:'side',
            width:700,
            labelStyle: 'font-weight: normal;'
        },
        border:false,
        bodyStyle: 'background:transparent;',
        padding:10,
        margins:"40px",
        defaultType: 'textfield',
        filterCount: filterIdx,
        items:items,
        buttons:[
            {text:'Delete', handler:Form_onDelete, style:'margin-right: 10px;'},
            {text:'Cancel', handler:Form_onCancel},
            {text:'Save', handler:Form_onSave}
        ],
        buttonAlign:'left'
    });
    form.render('form');

    keywordLink.callback();
    sampleLink?.callback();
    statisticLink.callback();
    filterLink.callback();
});


</script>

<%
    JspView<Object> statPicker = new StatPickerView();
    statPicker.include(statPicker, out);
%>
