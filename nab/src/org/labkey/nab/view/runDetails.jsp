<%
/*
 * Copyright (c) 2007-2010 LabKey Corporation
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
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.labkey.api.exp.Lsid"%>
<%@ page import="org.labkey.api.exp.OntologyManager" %>
<%@ page import="org.labkey.api.exp.PropertyDescriptor" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.security.permissions.DeletePermission" %>
<%@ page import="org.labkey.api.security.permissions.InsertPermission" %>
<%@ page import="org.labkey.api.study.DilutionCurve" %>
<%@ page import="org.labkey.api.study.WellData" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.*" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.util.*" %>
<%@ page import="org.labkey.api.study.Plate" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<labkey:errors/>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    NabAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();

    boolean writer = context.getContainer().hasPermission(context.getUser(), InsertPermission.class);
%>
<table>
<%
    if (bean.needsNotesView())
    {
%>
    <tr class="labkey-wp-header">
        <th align="left">Notes</th>
    </tr>
    <% me.include(bean.getRunNotesView(), out); %>
<%
    }
%>
    <tr class="labkey-wp-header">
        <th>Run Summary<%= assay.getRunName() != null ? ": " + h(assay.getRunName()) : "" %></th>
    </tr>
    <tr>
        <td><% me.include(bean.getRunPropertiesView(), out); %></td>
    </tr>
    <tr>
        <td>
            <table>
                <tr>
                    <td valign="top"><% me.include(bean.getGraphView(), out); %></td>
                    <td valign="top"><% include(bean.getControlsView(), out); %><br>
                        <% me.include(bean.getCutoffsView(), out); %>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td><% me.include(bean.getSamplePropertiesView(), out); %></td>
    </tr>
</table>
<table>
    <tr class="labkey-wp-header">
        <th>Sample Information</th>
    </tr>
    <tr>
        <td><% me.include(bean.getSampleDilutionsView(), out); %></td>
    </tr>
</table>
<table>
    <tr class="labkey-wp-header">
        <th>Plate Data</th>
    </tr>
    <tr>
        <td><% me.include(bean.getPlateDataView(), out); %></td>
    </tr>
    <%
    if (!bean.isPrintView() && writer)
    {
%>
    <tr class="labkey-wp-header">
        <th>Discussions</th>
    </tr>
    <tr>
        <td><% me.include(bean.getDiscussionView(HttpView.getRootContext()), out); %></td>
    </tr>
<%
    }
%>
</table>