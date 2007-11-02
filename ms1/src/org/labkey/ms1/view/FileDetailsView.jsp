<%@ page import="org.labkey.api.exp.api.ExpData" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.ms1.model.DataFile" %>
<%
    JspView<DataFile> me = (JspView<DataFile>) HttpView.currentView();
    DataFile dataFile = me.getModelBean();
    ExpData expData = (null == dataFile ? null : dataFile.getExpData());

    ViewURLHelper urlDownload = new ViewURLHelper("Experiment", "showFile", me.getViewContext().getContainer());

%>
<% if(null != dataFile && null != expData) { %>
<table cellpadding="2px" cellspacing="0" border="0">
    <tr>
        <td>Data File:</td>
        <% urlDownload.addParameter("rowId", expData.getRowId()); %>
        <td><%=PageFlowUtil.filter(expData.getDataFileUrl())%>
            [<a href="<%=urlDownload.getLocalURIString()%>">download</a>]
        </td>
    </tr>
    <tr>
        <td>Source MzXML:</td>
        <%
            urlDownload.deleteParameters();
            ExpData[] inputs = expData.getRun().getInputDatas(null, null);
            for(ExpData input : inputs)
            {
                if(input.getDataFileUrl().equalsIgnoreCase(dataFile.getMzXmlUrl()))
                    urlDownload.addParameter("rowId", input.getRowId());
            }
        %>
        <td><%=PageFlowUtil.filter(dataFile.getMzXmlUrl())%>
            [<a href="<%=urlDownload.getLocalURIString()%>">download</a>] <% //TODO: "Open in msInspect" link? %>
        </td>
    </tr>
</table>
<% } %>