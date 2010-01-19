<%
/*
 * Copyright (c) 2008-2010 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%
WellController.UpdateKeywordsForm form = (WellController.UpdateKeywordsForm)HttpView.currentModel();
ViewContext context = HttpView.currentContext();

%>
<span style="color:green;"><%=PageFlowUtil.filter(form.message)%></span><br>
<labkey:errors />
<div id="updateForm" />
<script type="text/javascript">

var keywords = [
<%
    String comma = "";
    for (String kw : form.getKeywords(context))
    {
        out.print(comma);
        out.print("[");
        out.print(PageFlowUtil.jsString(kw));
        out.print("]");
        comma = ",";
    }%>
];
var keywordStore = new Ext.data.SimpleStore({fields:['keyword'], data:keywords}) 
var values = [
<%
    comma = "";
    for (String v : form.getValues(context,form.keyword))
    {
        out.print(comma);
        out.print("[");
        out.print(PageFlowUtil.jsString(v));
        out.print("]");
        comma = ",";
    }%>
];
var valuesStore = new Ext.data.SimpleStore({fields:['value'], data:values})

var updateFormPanel;
var keywordCombo;

function keywordCombo_onSelect()
{
    var kw = keywordCombo.getValue();
    window.location = '?keyword=' + kw;
}

function form_onSubmit()
{
    var form = updateFormPanel.getForm();
    form.getEl().dom.action="bulkUpdateKeywords.post";
    form.getEl().dom.submit();
}

Ext.onReady(function(){

    Ext.QuickTips.init();

    // turn on validation errors beside the field globally
    Ext.form.Field.prototype.msgTarget = 'side';

    updateFormPanel = new Ext.FormPanel({
        
        onSubmit: Ext.emptyFn, submit: form_onSubmit,

        labelWidth: 75, // label settings here cascade unless overridden
        url:'save-form.php',
        frame:true,
        title: 'Update Keywords',
        bodyStyle:'padding:5px 5px 0',
        width: 350,
        defaults: {width: 230},
        defaultType: 'textfield',

        items:
        [
            keywordCombo = new Ext.form.ComboBox({
                fieldLabel: 'Keyword',
                name: 'keyword',
                forceSelection: true,
                value: <%=PageFlowUtil.jsString(form.keyword)%>,
                displayField:'keyword',
                typeAhead: true,
                mode: 'local',
                triggerAction: 'all',
                store: keywordStore
            })
<% if (null != StringUtils.trimToNull(form.keyword)) {for (int i=0; i<4 ; i++) {            
            %>,
            {
                xtype:'fieldset',
                //title:'<%=i%>', collapsible:true, collapsed:<%=i==0?"false":"true"%>,
                width: 330, autoHeight:true, defaults: {width: 210},
                defaultType: 'textfield',
                items:
                [
                    new Ext.form.ComboBox({fieldLabel:'Current Value', name:'from', forceSelection:false,
                        lazyRender:true, displayField:'value', typeAhead:true, mode:'local', triggerAction:'all',
                        store: valuesStore}),
                    new Ext.form.ComboBox({fieldLabel:'New Value', name:'to', forceSelection:false,
                        lazyRender:true, displayField:'value', typeAhead:true, mode:'local', triggerAction:'all',
                        store: valuesStore})
                ]
            } <%}}%>
        ],
        buttons:[{text: 'Update', type:'submit', handler:function() {updateFormPanel.getForm().submit({url:"bulkUpdateKeywords.view"})}}]
    });

    keywordCombo.on('select', keywordCombo_onSelect);
    
    updateFormPanel.render('updateForm');
});

</script>
