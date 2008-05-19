<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.study.ParticipantVisit"%>
<%@ page import="org.labkey.api.study.TimepointType" %>
<%@ page import="org.labkey.api.study.assay.AssayPublishService" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.nab.NabController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<script type="text/javascript">LABKEY.requiresScript('completion.js');</script>
<%
    JspView<NabController.PublishVerifyBean> me = (JspView<NabController.PublishVerifyBean>) HttpView.currentView();
    NabController.PublishVerifyBean bean = me.getModelBean();
    String errors = PageFlowUtil.getStrutsError(request, "main");
%>
Publishing results to <b><%= h(bean.getTargetContainer().getPath()) %></b>.  All data must be associated with a participant/visit within the target study.<br><br>
<form action="handlePublish.post" method="POST">
    <input type="hidden" name="plateIds" value="false">
    <input type="hidden" name="targetContainerId" value="<%= bean.getTargetContainer().getId() %>">
    <table class="normal">
        <tr>
            <th>Include</th>
            <th>Specimen Id</th>
<%
    if (AssayPublishService.get().getTimepointType(bean.getTargetContainer()) == TimepointType.VISIT)
    {
%>
            <th>Visit Id</th>
<%            
    }
    else
    {
%>
            <th>Date</th>
<%
    }
%>
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
        ParticipantVisit sampleInfo = bean.getSampleInfoMap().get(group);

%>
        <tr>
            <td>
                <input type="checkbox" name="includedSampleIds" value="<%= h(sampleInfo.getSpecimenID())%>" CHECKED>
                <input type="hidden" name="sampleIds" value="<%= h(sampleInfo.getSpecimenID())%>">
                <input type="hidden" name="id" value="<%= group.getRowId() %>">
            </td>
            <td><%= h(sampleInfo.getSpecimenID())%></td>
            <td>
<%
            if (AssayPublishService.get().getTimepointType(bean.getTargetContainer()) == TimepointType.VISIT)
            {
                String sequenceNumString = null;
                if (sampleInfo instanceof NabController.PublishSampleInfo)
                    sequenceNumString = ((NabController.PublishSampleInfo) sampleInfo).getVisitIDString();
%>
                <input type="text" name="sequenceNums"
                  onKeyDown="return ctrlKeyCheck(event);"
                  onBlur="hideCompletionDiv();"
                  autocomplete="off"
                  value="<%= h(sequenceNumString != null ? sequenceNumString : bean.format(sampleInfo.getVisitID())) %>"
                  onKeyUp="return handleChange(this, event, '<%= bean.getVisitIdCompletionBase() %>');">
<%
            }
            else
            {
                String dateString = null;
                if (sampleInfo instanceof NabController.PublishSampleInfo)
                    dateString = ((NabController.PublishSampleInfo) sampleInfo).getDateString();
%>
            <input type="text" name="dates"
              value="<%= h(dateString != null ? dateString : bean.format(sampleInfo.getDate())) %>" >

<%
            }
%>
            </td>
            <td>
                <input type="text" name="participantIds"
                  onKeyDown="return ctrlKeyCheck(event);"
                  onBlur="hideCompletionDiv();"
                  autocomplete="off"
                  value="<%= h(bean.format(sampleInfo.getParticipantID())) %>"
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
    <%= buttonLink("Cancel", "begin.view") %> <%= buttonImg("Copy to Study")%>
</form>
