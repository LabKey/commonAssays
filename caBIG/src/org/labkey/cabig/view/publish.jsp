<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.labkey.cabig.caBIGManager"%>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext ctx = HttpView.currentContext();
    boolean isPublished = caBIGManager.get().isPublished(ctx.getContainer());
    ViewURLHelper url = new ViewURLHelper("cabig", isPublished ? "unpublish" : "publish", ctx.getContainer());

    if (isPublished) {
%>
This folder is published to the caBIG interface.  If your caBIG webapp is running, all experiment data in this folder is visible publically
via the caBIG API.<br><br>
<%=PageFlowUtil.buttonLink("Unpublish", url)%>
<%
    }
    else
    {
%>
This folder is not published to the caBIG interface.  Click the button below to publish this folder to caBIG.  If you do this, and your caBIG webapp
is running, all experiment data in this folder will be visible publicly via the caBIG API.<br><br>
<%=PageFlowUtil.buttonLink("Publish", url)%>
<%  }  %>

<br><br>For more information about caBIG, <a href="http://cabig.cancer.gov/index.asp" target="cabig">click here</a>.