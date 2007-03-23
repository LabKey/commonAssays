<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.labkey.cabig.caBIGManager"%>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = HttpView.currentContext();
    boolean isPublished = caBIGManager.get().isPublished(context.getContainer());

    if (isPublished)
    {
%>
This is where the caBIG config stuff goes.
<%
    }
    else
    {
%>
This is where the caBIG config stuff goes.
<%  }  %>