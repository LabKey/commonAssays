<%
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
%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.module.ModuleLoader" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<Object> me = (JspView<Object>) HttpView.currentView();
    ViewContext ctx = me.getViewContext();
    boolean targetedMSModuleActive = ctx.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule("TargetedMS"));
    String renderId = "tabbed-search-form-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>
<div id=<%=q(renderId)%>></div>

<style type="text/css">

    .non-ext-search-tab-panel table {
        border-collapse: separate;
        border-spacing: 6px;
    }

</style>

<script type="text/javascript">

    Ext4.onReady(function(){

        var tabPanel = Ext4.create('Ext.tab.Panel', {
            anchor: '100%',
            renderTo: <%=q(renderId)%>,
            defaults: { padding: 10 },
            items: [{
                // protein search webpart from the ... module
                title: 'Protein Search',
                cls: 'non-ext-search-tab-panel',
                height: <%=(targetedMSModuleActive ? 100 : 170)%>,
                border : false,
                items : [
                    Ext4.create('Ext.Component', {
                        border : false,
                        listeners : {
                            scope: this,
                            render : function(cmp) {
                                var wp = new LABKEY.WebPart({
                                    partName: <%=q(targetedMSModuleActive ? "Targeted MS Protein Search" : "Protein Search")%>,
                                    frame: 'none',
                                    renderTo: cmp.getId()
                                });
                                wp.render();
                            }
                        }
                    })
                ]
            },{
                // peptide search webpart from the ... module
                title: 'Peptide Search',
                cls: 'non-ext-search-tab-panel',
                height: 100,
                border : false,
                items : [
                    Ext4.create('Ext.Component', {
                        border : false,
                        listeners : {
                            scope: this,
                            render : function(cmp) {
                                var wp = new LABKEY.WebPart({
                                    partName: 'Peptide Search',
                                    frame: 'none',
                                    renderTo: cmp.getId()
                                });
                                wp.render();
                            }
                        }
                    })
                ]
            }]
        });

<% if (targetedMSModuleActive) { %>
        tabPanel.add({
            // modification search webpart from the targetedms module
            title: 'Modification Search',
            height: 250,
            border : false,
            items : [
                Ext4.create('Ext.Component', {
                    border : false,
                    listeners : {
                        scope: this,
                        render : function(cmp) {
                            var wp = new LABKEY.WebPart({
                                partName: 'Targeted MS Modification Search',
                                frame: 'none',
                                renderTo: cmp.getId()
                            });
                            wp.render();
                        }
                    }
                })
            ]
        });
<% } %>

    });

</script>