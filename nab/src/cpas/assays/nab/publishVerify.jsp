<%@ page import="org.fhcrc.cpas.view.HttpView"%>
<%@ page import="org.fhcrc.cpas.view.JspView"%>
<%@ page import="Nab.NabController" %>
<%@ page import="org.fhcrc.cpas.study.WellGroup" %>
<%@ page import="org.fhcrc.cpas.study.GenericAssayService" %>
<%@ page import="org.fhcrc.cpas.util.PageFlowUtil" %>
<%@ page extends="org.fhcrc.cpas.jsp.JspBase" %>
<%
    JspView<NabController.PublishVerifyBean> me = (JspView<NabController.PublishVerifyBean>) HttpView.currentView();
    NabController.PublishVerifyBean bean = me.getModel();
    String errors = PageFlowUtil.getStrutsError(request, "main");
%>
<span class="cpas-error"><%=errors%></span>
Publishing results to <b></b><%= h(bean.getTargetContainer().getPath()) %></b>.<br><br>
<form action="handlePublish.post" method="POST">
    <input type="hidden" name="plateIds" value="false">
    <input type="hidden" name="targetContainerId" value="<%= bean.getTargetContainer().getId() %>">
    <table class="normal">
        <tr>
            <th>Include</th>
            <th>Specimen Id</th>
            <th>Visit Id</th>
            <th>Participant Id</th>
<%
    for (String property : bean.getSampleProperties())
    {
%>
            <th><%= h(property) %></th>
<%
    }
%>
        </tr>
<%
    for (WellGroup group : bean.getSampleInfoMap().keySet())
    {
        GenericAssayService.SampleInfo sampleInfo = bean.getSampleInfoMap().get(group);
%>
        <tr>
            <td>
                <input type="checkbox" name="includedSampleIds" value="<%= h(sampleInfo.getSampleId())%>" CHECKED>
                <input type="hidden" name="sampleIds" value="<%= h(sampleInfo.getSampleId())%>">
                <input type="hidden" name="id" value="<%= group.getRowId() %>">
            </td>
            <td><%= h(sampleInfo.getSampleId())%></td>
            <td>
                <input type="text" name="sequenceNums"
                  onKeyDown="return ctrlKeyCheck(event);"
                  onBlur="hideCompletionDiv();"
                  autocomplete="off"
                  value="<%= h(bean.format(sampleInfo.getSequenceNum())) %>"
                  onKeyUp="return handleChange(this, event, '<%= bean.getVisitIdCompletionBase() %>');">
                </td>
            <td>
                <input type="text" name="participantIds"
                  onKeyDown="return ctrlKeyCheck(event);"
                  onBlur="hideCompletionDiv();"
                  autocomplete="off"
                  value="<%= h(bean.format(sampleInfo.getParticipantId())) %>"
                  onKeyUp="return handleChange(this, event, '<%= bean.getParticipantCompletionBase() %>');">

                </td>
        <%
            for (String property : bean.getSampleProperties())
            {
                Object value = group.getProperty(property);
        %>
            <td><%= h(bean.format(value)) %></td>
        <%
            }
        %>
        </tr>
<%
    }
%>
    </table>
    <%= buttonLink("Cancel", "begin.view") %> <%= buttonImg("Publish")%>
</form>
