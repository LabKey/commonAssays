<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.PickViewBean bean = ((JspView<MS2Controller.PickViewBean>)HttpView.currentView()).getModelBean();
%>
<p><%=h(bean.viewInstructions)%></p>
<p>To define a view: display a run, filter the run, click "Save View", and pick a name.  The filter you've set up will be saved with the view and the name will then appear in the list below.</p>
<form method="get" action="<%=h(bean.nextUrl)%>">
    <input type="hidden" name="runList" value="<%=bean.runList%>">
    <%=bean.select%><br/>
    <br/><br/>
    <% out.flush(); bean.extraOptionsView.render(request, response); %><br/>
    <input type="image" src="<%=PageFlowUtil.buttonSrc("Go")%>" name="submit">
</form>
