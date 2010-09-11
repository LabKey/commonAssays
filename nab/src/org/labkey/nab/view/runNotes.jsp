<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.NabAssayRun" %>
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
    NabAssayRun assay = bean.getAssay();
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
    <td class="labkey-form-label">
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
        <% include(duplicateDataFileView, out); %>
    </td>
</tr>
<%
    }
%>
