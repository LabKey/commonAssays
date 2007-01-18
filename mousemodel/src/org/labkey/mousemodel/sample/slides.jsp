<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.attachments.DownloadUrlHelper" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<table border=0>
<%
    ViewContext context = HttpView.currentContext();
    Map[] slides = (Map[]) context.get("slides");
    ViewURLHelper deleteUrl = (ViewURLHelper) context.get("deleteUrl");

    int index = 0;

    for (Map slide : slides)
    {
        String downloadUrl = new DownloadUrlHelper(context.getContainer(), (String) slide.get("slideEntityId"), (String) slide.get("DocumentName")).getEncodedLocalURIString();
        String sampleUrl = new ViewURLHelper("MouseModel-Sample", "details.view", context.getContainer())
            .addParameter("sampleId", (String)slide.get("sampleId"))
            .addParameter("modelId", String.valueOf(slide.get("modelId")))
            .getEncodedLocalURIString();

        if ((index % 3) == 0)
            out.print("<tr>");
%><td class="ms-vb" align="center">
<a href="<%=downloadUrl%>" target="_blank"><img border=0 width=300 src="<%=downloadUrl%>"></a><br>
<%=PageFlowUtil.filter(slide.get("sampleType"))%> (<%=PageFlowUtil.filter(slide.get("stain"))%>)
<a href="<%=sampleUrl%>"><%=slide.get("sampleId")%></a>
<%
        if (null != deleteUrl)
        {
            deleteUrl.replaceParameter("entityId", (String)slide.get("slideEntityId"));%>
    [<a href="<%=deleteUrl.getEncodedLocalURIString()%>">Delete</a>]
            <%
        }
%></td><%
        index++;

        if ((index %3) == 0)
            out.print("</tr>");
    }

    if ((index %3) != 0)
        out.print("</tr>");

    String insertUrl = new ViewURLHelper("MouseModel-Sample", "showInsertSlides.view", context.getContainer())
        .addParameter("modelId", String.valueOf(context.get("modelId")))
        .addParameter("sampleId", (String)context.get("sampleId"))
        .getEncodedLocalURIString();
%>
</table>
<a href="<%=insertUrl%>">Add Slides</a>
