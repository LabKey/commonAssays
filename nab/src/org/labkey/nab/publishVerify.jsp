<%@ page import="org.labkey.nab.NabController"%>
<%@ page import="org.labkey.api.study.WellGroup" %>
<%@ page import="org.labkey.api.study.GenericAssayService" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<script type="text/javascript">LABKEY.requiresScript('completion.js');</script>
<%
    JspView<NabController.PublishVerifyBean> me = (JspView<NabController.PublishVerifyBean>) HttpView.currentView();
    NabController.PublishVerifyBean bean = me.getModel();
    String errors = PageFlowUtil.getStrutsError(request, "main");
%>
<span class="labkey-error"><%=errors%></span>
Publishing results to <b><%= h(bean.getTargetContainer().getPath()) %></b>.  All data must be associated with a participant/visit within the target study.<br><br>
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
    for (org.labkey.api.study.WellGroup group : bean.getSampleInfoMap().keySet())
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
