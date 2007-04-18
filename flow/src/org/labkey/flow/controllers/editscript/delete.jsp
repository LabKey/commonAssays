<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%  FlowScript script = getForm().analysisScript;
    int runCount = script.getRunCount();
%>
<% if (runCount > 0) { %>
<p>This analysis script cannot be deleted because it has been used by <%=runCount%> runs.  You must delete the runs that use this script before it can be deleted.<br>
<labkey:button text="Go Back" href="<%=script.urlShow()%>" />
</p>
<% } else { %>
<p>Are you sure that you want to delete the analysis script '<%=h(script.getName())%>'?<br>
    <form action="<%=script.urlFor(ScriptController.Action.delete)%>" method="POST">
        <labkey:button text="OK" /> <labkey:button text="Cancel" href="<%=script.urlShow()%>" />
    </form>
</p>
<% } %>