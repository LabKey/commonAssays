<%@ page import="java.util.List" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="org.labkey.flow.controllers.editscript.NewProtocolForm"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    NewProtocolForm form = (NewProtocolForm) __form;
%>
<%=PageFlowUtil.getStrutsError(request, null)%>

<form method="post">
    <p>What do you want to call this analysis script?<br>
        <input type="text" name="ff_name" value="<%=h(form.ff_name)%>">
    </p>

    <labkey:button text="Create Analysis Script" />
</form>