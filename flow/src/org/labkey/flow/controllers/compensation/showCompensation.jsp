<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.announcements.DiscussionService" %>
<%@ page import="org.labkey.api.jsp.JspLoader" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.flow.FlowPreference" %>
<%@ page import="org.labkey.flow.analysis.model.CompensationMatrix" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="org.labkey.flow.controllers.FlowParam" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix"%>
<%@ page import="org.labkey.flow.data.FlowDataType"%>
<%@ page import="org.labkey.flow.data.FlowRun"%>
<%@ page import="org.labkey.flow.data.FlowWell" %>
<%@ page import="org.labkey.flow.view.GraphDataRegion" %>
<%@ page import="org.labkey.flow.view.SetCommentView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    String compImg(FlowWell well, String param, String graphSize)
    {
        if (well == null)
            return "N/A";

        GraphSpec spec = new GraphSpec(null, param);
        if (well.getGraphBytes(spec) == null)
            return "N/A";
        ActionURL urlGraph = well.urlFor(WellController.ShowGraphAction.class);
        urlGraph.addParameter(FlowParam.graph.toString(), spec.toString());
        return "<span style=\"display:inline-block; vertical-align:top; height:" + graphSize + "; width:" + graphSize + "\">\n" +
               "<img style=\"width:" + graphSize + ";height:" + graphSize + ";\" class='labkey-flow-graph' src=\"" + h(urlGraph) + "\" onerror=\"flowImgError(this);\">\n" +
               "</span><wbr>";
    }
%>
<script type="text/javascript" src="<%=getContextPath()%>/Flow/util.js"></script>
<%
    final FlowCompensationMatrix flowComp = FlowCompensationMatrix.fromURL(getActionURL(), request, getContainer(), getUser());
    if (null == flowComp)
    {
        %><span class="labkey-error">compensation matrix definition not found</span><%
        return;
    }
    final CompensationMatrix comp = flowComp.getCompensationMatrix();
    if (null == comp)
    {
        %><span class="labkey-error">compensation matrix has no channels</span><%
        return;
    }

    final String[] channelNames = comp.getChannelNames();
    final int channelCount = channelNames.length;
    DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(3);
    format.setMinimumFractionDigits(3);

    boolean canEdit = getViewContext().hasPermission(UpdatePermission.class);
%>

<table>
    <tr><td>Compensation Matrix:</td><td><%=h(comp.getName())%></td></tr>
    <% if (canEdit || flowComp.getExpObject().getComment() != null) { %>
        <tr><td>Compensation Comment:</td>
            <td><%include(new SetCommentView(flowComp), out);%></td>
        </tr>
    <% } %>
</table>
<br/>
<table class="labkey-data-region-legacy labkey-show-borders">
    <colgroup><% for (int i = 0; i < channelCount + 1; i ++)
        { %>
        <col>
        <% } %></colgroup>
    <tr><td class="labkey-column-header">&nbsp;</td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td class="labkey-column-header"><%=h(channelNames[iChannelValue])%></td>
        <% } %>
    </tr>
    <% for (int iChannel = 0; iChannel < channelCount; iChannel ++)
    {
        _HtmlString className = getShadeRowClass(0==(iChannel%2));
    %>
    <tr class="labkey-row">
        <td class="labkey-column-header <%=className%>" style="text-align:right;"><%=h(channelNames[iChannel])%></td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        {
        %><td class="<%=className%>" style="text-align:right;"><%=text(format.format(comp.getRow(iChannel)[iChannelValue]))%></td><%
        }%>
    </tr>
    <%}%>
</table>



    <% final FlowRun run = flowComp.getRun();
    if (run == null)
    {
        return;
    }
    final List<FlowWell> appWells = (List<FlowWell>) run.getDatas(FlowDataType.CompensationControl);
    final Map<String, FlowWell> wellMap = new HashMap();
    for (FlowWell well : appWells)
    {
        wellMap.put(well.getName(), well);
    }
    abstract class Callback
    {
        String title;

        public Callback(String title)
        {
            this.title = title;
        }

        abstract String render(int iChannel, int iChannelValue);
    }

    final String graphSize = FlowPreference.graphSize.getValue(request);
    Callback[] callbacks = new Callback[]
            {
                    new Callback("Uncompensated Graphs")
                    {
                        String render(int iChannel, int iChannelValue)
                        {
                            return compImg(wellMap.get(channelNames[iChannel] + "+"), channelNames[iChannelValue], graphSize);
                        }
                    },
                    new Callback("Compensated Graphs")
                    {
                        String render(int iChannel, int iChannelValue)
                        {
                            return compImg(wellMap.get(channelNames[iChannel] + "+"), "<" + channelNames[iChannelValue] + ">", graphSize);
                        }
                    }
            };
%>
<br/>
<% include(new JspView(JspLoader.createPage(GraphDataRegion.class, "setGraphSize.jsp")), out);%>
<% for (Callback callback : callbacks)
{ %>
<h4><%=h(callback.title)%></h4>
<table class="labkey-data-region-legacy labkey-show-borders">
    <tr><td class="labkey-column-header">&nbsp;</td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td class="labkey-column-header"><%=h(channelNames[iChannelValue])%></td>
        <% } %>
    </tr>
    <% for (int iChannel = 0; iChannel < channelCount; iChannel ++)
    {
        FlowWell well = wellMap.get(channelNames[iChannel] + "+");
    %>
    <tr class="<%=h(iChannel % 2 == 0 ? "labkey-alternate-row" : "labkey-row")%>">
        <%if (well == null) { %>
        <td>N/A</td>
        <% } else { %>
        <td><a href="<%=h(well.urlShow())%>"><%=h(channelNames[iChannel])%></a></td>
        <% } %>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td><%=text(callback.render(iChannel, iChannelValue))%></td>
        <%}%>
    </tr>
    <%}%>
</table>
<% } %>
<labkey:link href="<%=flowComp.urlDownload()%>" text="Download" rel="nofollow"/><br>
<%
    DiscussionService service = DiscussionService.get();
    DiscussionService.DiscussionView discussion = service.getDiscussionArea(
            getViewContext(),
            flowComp.getLSID(),
            flowComp.urlShow(),
            "Discussion of " + flowComp.getLabel(),
            false, true);
    include(discussion, out);
%>
