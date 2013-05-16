<%
/*
 * Copyright (c) 2010-2013 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.assay.dilution.DilutionAssayRun" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.security.permissions.DeletePermission" %>
<%@ page import="org.labkey.api.security.permissions.InsertPermission" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.nab.NabUploadWizardAction" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    DilutionAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();

    ActionURL reRunURL = new ActionURL(NabUploadWizardAction.class, context.getContainer());
    reRunURL.addParameter("rowId", assay.getProtocol().getRowId());
    reRunURL.addParameter("reRunId", bean.getRunId());

    if (bean.needsCurveNote())
    {
        boolean deleteAndInsertPerms = context.getContainer().hasPermission(context.getUser(), DeletePermission.class) &&
        context.getContainer().hasPermission(context.getUser(), InsertPermission.class);
%>
<tr>
    <td>
        This run is shown with a <strong><%= assay.getRenderedCurveFitType().getLabel() %></strong> curve fit,
       but is saved with a <strong><%= assay.getSavedCurveFitType() != null ? assay.getSavedCurveFitType().getLabel() : "unknown" %></strong> curve fit.
        To replace<br>the saved data with the displayed data,
        <%
        if (deleteAndInsertPerms)
        {
        %>
            you must <a href="<%= reRunURL.getLocalURIString() %>">delete and re-import</a> the run.
        <%
        }
        else
        {
        %>
            a user with appropriate permissions must delete and re-import the run.
        <%
        }
        %>
        <p>
    </td>
</tr>
<%
    }
    if (bean.needsNewRunNote())
    {
%>
<tr>
    <td class="labkey-form-label">
        This run has been automatically saved.
    <%
            if (context.getContainer().hasPermission(context.getUser(), DeletePermission.class))
            {
                ActionURL deleteUrl = new ActionURL(NabAssayController.DeleteRunAction.class, context.getContainer());
                deleteUrl.addParameter("rowId", bean.getRunId());
    %>
    <%=generateButton("Delete Run", deleteUrl, "return confirm('Permanently delete this run?')")%>
    <%=generateButton("Delete and Re-Import", reRunURL)%>
    <%
            }
    %>
        <p>
    </td>
</tr>
<%
    }
    if (bean.needsDupFileNote())
    {
        QueryView duplicateDataFileView = bean.getDuplicateDataFileView(me.getViewContext());
%>
<tr>
    <td class="labkey-form-label">
        <span class="labkey-error"><b>WARNING</b>: The following runs use a data file by the same name.</span><br><br>
        <% include(duplicateDataFileView, out); %><p>

    </td>
</tr>
<%
    }
%>
