<%@ page import="java.util.List" %>
<%@ page import="Flow.EditScript.ScriptController" %>
<%@ page import="Flow.EditScript.NewProtocolForm"%>
<%@ page import="org.fhcrc.cpas.util.PageFlowUtil"%>
<%@ page extends="org.fhcrc.cpas.jsp.FormPage" %>
<%@ taglib uri="http://cpas.fhcrc.org/taglib/cpas" prefix="cpas" %>
<%
    NewProtocolForm form = (NewProtocolForm) __form;
    List<String> templates = form.getTemplateNames("analysis.xml");
%>
<%=PageFlowUtil.getStrutsError(request, null)%>

<form method="post">
    <p>What do you want to call this analysis script?<br>
        <input type="text" name="ff_name" value="<%=h(form.ff_name)%>">
    </p>

    <p>
        There are some pre-built analyses that you might want to use:
    </p>

    <% for (String template : templates)
    { %>
    <input type="radio" name="ff_template" value="<%=h(template)%>"<%=template.equals(form.ff_template) ? " checked" : ""%>> <%=h(template)%><br>
    <% } %>
    <input type="radio" name="ff_template" value="blank.xml"<%="blank.xml".equals(form.ff_template) ? " checked" : ""%>> Just give me a blank analysis script.</br>

    <cpas:button text="Create Analysis Script" />
</form>