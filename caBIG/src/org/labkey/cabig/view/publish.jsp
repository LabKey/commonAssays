<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.labkey.cabig.caBIGManager"%>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.util.HelpTopic" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext ctx = HttpView.currentContext();
    boolean isPublished = caBIGManager.get().isPublished(ctx.getContainer());
    ViewURLHelper url = new ViewURLHelper("cabig", isPublished ? "unpublish" : "publish", ctx.getContainer());

    if (isPublished)
    {
%>
This folder is published to the caBIG interface.  If your caBIG webapp is running, all experiment data in this folder is visible publicly
via the caBIG API.<br><br>
<%=PageFlowUtil.buttonLink("Unpublish", url)%>
<%
    }
    else
    {
%>
This folder is not published to the caBIG interface.  Click the button below to publish this folder to caBIG.  If you do this, and your caBIG webapp
is running, all experiment data in the folder will be visible publicly via the caBIG API.<br><br>
<%=PageFlowUtil.buttonLink("Publish", url)%>
<%  }  %>

<br><br>For more information about publishing to caBIG, <a href="<%=h(new HelpTopic("cabig", HelpTopic.Area.CPAS).getHelpTopicLink())%>" target="cabig">click here</a>.

<%
    if (isPublished)
    {
%>
<br><br>If you've configured the caBIG webapp on the same server as your LabKey Server (the standard configuration), then <a href="<%=h(ctx.getViewURLHelper().getBaseServerURI())%>/publish/happy.jsp">click here</a> to test the stupid thing.
<%
    }
%>