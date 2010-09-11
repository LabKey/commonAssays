<%@ page import="org.labkey.nab.Luc5Assay" %>
<%@ page import="org.labkey.api.study.Plate" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.NabAssayRun" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    NabAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();

    // Lay out the table vertically, rather than horizontally, if we have more than one plate
    // to keep the table from getting too wide.
    if (assay.getPlates().size() > 1)
    {
%>
<table width="100%" class="labkey-data-region labkey-show-borders">
    <tr>
        <th>Plate</th>
        <th>Range</th>
        <th>Virus Control</th>
        <th>Cell Control</th>
    </tr>
<%
        int plateNum = 1;
        for (Plate plate : assay.getPlates())
        {
%>
    <tr>
        <td style="font-weight:bold"><%= plateNum++ %></td>
        <td align=left><%=Luc5Assay.intString(assay.getControlRange(plate))%></td>
        <td align="left"><%=Luc5Assay.intString(assay.getVirusControlMean(plate))%> &plusmn;
            <%=Luc5Assay.percentString(assay.getVirusControlPlusMinus(plate))%></td>
        <td align=left><%=Luc5Assay.intString(assay.getCellControlMean(plate))%> &plusmn;
            <%=Luc5Assay.percentString(assay.getCellControlPlusMinus(plate))%></td>
    </tr>
<%
        }
%>
</table>
<%
    }
    else
    {
        Plate plate = assay.getPlates().get(0);
%>
<table>
    <tr>
        <th style="text-align:left">Range</th>
        <td align=left><%=Luc5Assay.intString(assay.getControlRange(plate))%></td>
    </tr>
    <tr>
        <th style="text-align:left">Virus Control</th>
        <td align="left"><%=Luc5Assay.intString(assay.getVirusControlMean(plate))%> &plusmn;
            <%=Luc5Assay.percentString(assay.getVirusControlPlusMinus(plate))%></td>
    </tr>
    <tr>
        <th style="text-align:left">Cell Control</th>
        <td align=left><%=Luc5Assay.intString(assay.getCellControlMean(plate))%> &plusmn;
            <%=Luc5Assay.percentString(assay.getCellControlPlusMinus(plate))%></td>
    </tr>
</table>
<%
    }
%>
