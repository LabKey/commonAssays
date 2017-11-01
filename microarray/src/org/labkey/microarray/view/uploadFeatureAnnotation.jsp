<%
/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page import="org.labkey.api.security.permissions.InsertPermission" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.microarray.controllers.FeatureAnnotationSetController" %>
<%@ page import="org.springframework.validation.Errors" %>
<%@ page import="org.springframework.validation.ObjectError" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>
<%
    JspView<FeatureAnnotationSetController.FeatureAnnotationSetForm> me = (JspView<FeatureAnnotationSetController.FeatureAnnotationSetForm>) HttpView.currentView();
    String vendor = me.getModelBean().getVendor();
    String name = me.getModelBean().getName();
    String description = me.getModelBean().getDescription();
    String comment = me.getModelBean().getComment();
    Errors errors = me.getErrors();
    String returnUrl = me.getModelBean().getReturnActionURL(new ActionURL(FeatureAnnotationSetController.ManageAction.class, getContainer())).toString();

    Map<String, Integer> containers = new LinkedHashMap<>();
    Container project = getContainer().getProject();
    if (project != null && project.hasPermission(getUser(), InsertPermission.class))
        containers.put("Project (" + h(project.getName() + ")"), project.getRowId());
    containers.put("Current Folder (" + h(getContainer().getName()) + ")", getContainer().getRowId());
    Container shared = ContainerManager.getSharedContainer();
    if (shared != null && shared.hasPermission(getUser(), InsertPermission.class))
        containers.put("Shared Folder", shared.getRowId());
%>

<%
    if(errors.hasErrors())
    {
%>
        <div id="errors">
            <ul>
                <%
                    for (ObjectError error : errors.getAllErrors())
                    {
                %>
                <li>
                    <p class="labkey-error"><%=h(getViewContext().getMessage(error))%></p>
                </li>
                <%
                    }
                %>
            </ul>
        </div>
<%
    }
%>

<div id="featureAnnotationSetForm"></div>

<script type="text/javascript">
    function renderPanel(){
        var onCancel = function(){
            window.location = <%=PageFlowUtil.jsString(returnUrl)%>;
        };

        var onUpload = function(){
            var form = panel.getForm();
            if(form.isValid()){
                form.standardSubmit = true;
                form.submit();
            }
        };

        var panel = Ext4.create('Ext.form.Panel', {
            renderTo: 'featureAnnotationSetForm',
            border: false,
            bodyStyle: 'background-color: transparent;',
            bodyPadding: 10,
            width: 600,
            defaults: {
                labelWidth: 125
            },
            items: [
            { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
            {
                xtype: 'textfield',
                width : 580,
                labelWidth: 125,
                name: 'name',
                value: "<%=text(name) == null ? "" : text(name)%>",
                fieldLabel: 'Name',
                allowBlank: false
            }, {
                xtype: 'textfield',
                width : 580,
                labelWidth: 125,
                name: 'vendor',
                value: "<%=text(vendor) == null ? "" : text(vendor)%>",
                fieldLabel: 'Vendor',
                allowBlank: false
            }, {
                xtype: 'textarea',
                width: 580,
                labelWidth: 125,
                name: 'description',
                value: "<%=text(description) == null ? "" : text(description)%>",
                fieldLabel: 'Description',
                allowBlank: true
            }, {
                xtype: 'textfield',
                width: 580,
                labelWidth: 125,
                name: 'comment',
                value: "<%=text(comment) == null ? "" : text(comment)%>",
                fieldLabel: 'Comment',
                allowBlank: true
            }, {
                xtype: 'combobox',
                name: 'targetContainer',
                fieldLabel: 'Folder',
                labelWidth: 125,
                width: 580,
                allowBlank: false,
                value: <%=project != null ? project.getRowId() : getContainer().getRowId()%>,
                store: [
                    <% for (Map.Entry<String, Integer> entry : containers.entrySet()) { %>
                        [ <%= entry.getValue() %>, <%= q(entry.getKey()) %> ],
                    <% } %>
                ]
            }, {
                xtype: 'filefield',
                name: 'annotationFile',
                fieldLabel: 'Annotation File',
                labelWidth: 125,
                width: 580,
                allowBlank: false
            }],
            dockedItems: [{
                xtype: 'toolbar',
                style: 'background-color: transparent;',
                dock: 'bottom',
                ui: 'footer',
                items: [
                    '->',
                    {text: 'upload', handler: onUpload, scope: this},
                    {text: 'cancel', handler: onCancel, scope: this}
                ]
            }]
        });
    }

    Ext4.onReady(function(){
        renderPanel();
    });

</script>
