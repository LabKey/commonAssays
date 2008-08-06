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
<%@ page import="org.labkey.flow.analysis.model.CompensationMatrix" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.flow.data.*" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.controllers.FlowParam" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.labkey.api.jsp.JspLoader"%>
<%@ page import="org.labkey.flow.view.GraphView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%!
    String compImg(FlowWell well, String param) throws Exception
    {
        if (well == null)
            return "N/A";

        GraphSpec spec = new GraphSpec(null, param);
        if (well.getGraphBytes(spec) == null)
            return "N/A";
        ActionURL urlGraph = well.urlFor(WellController.Action.showGraph);
        urlGraph.addParameter(FlowParam.graph.toString(), spec.toString());
        return "<img class=\"flow-graph\" src=\"" + h(urlGraph) + "\">";
    }
%>
<%
    final FlowCompensationMatrix flowComp = FlowCompensationMatrix.fromURL(getViewContext().getActionURL(), request);
    if (null == flowComp)
    {
        %><font class="labkey-error">compensation matrix definition not found</font><%
        return;
    }
    final CompensationMatrix comp = flowComp.getCompensationMatrix();
    final String[] channelNames = comp.getChannelNames();
    final int channelCount = channelNames.length;
    DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(3);
    format.setMinimumFractionDigits(3);
%>

    <p>Compensation Matrix: <%=h(comp.getName())%></p>
<table class="labkey-data-region labkey-show-borders">
    <colgroup><% for (int i = 0; i < channelCount + 1; i ++)
        { %>
        <col>
        <% } %></colgroup>
    <tr><td class="labkey-data-region-title">&nbsp;</td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <th class="labkey-col-header"><%=h(channelNames[iChannelValue])%></th>
        <% } %>
    </tr>
    <% for (int iChannel = 0; iChannel < channelCount; iChannel ++)
    {
        String className = 0==(iChannel%2)?"labkey-alternate-row":"labkey-row";
    %>
    <tr>
        <th class="labkey-row-header <%=className%>" style="text-align:right;"><%=h(channelNames[iChannel])%></th>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        {
        %><td class="<%=className%>" style="text-align:right;"><%=format.format(comp.getRow(iChannel)[iChannelValue])%></td><%
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

        abstract String render(int iChannel, int iChannelValue) throws Exception;
    }
    Callback[] callbacks = new Callback[]
            {
                    new Callback("Uncompensated Graphs")
                    {
                        String render(int iChannel, int iChannelValue) throws Exception
                        {
                            return compImg(wellMap.get(channelNames[iChannel] + "+"), channelNames[iChannelValue]);
                        }
                    },
                    new Callback("Compensated Graphs")
                    {
                        String render(int iChannel, int iChannelValue) throws Exception
                        {
                            return compImg(wellMap.get(channelNames[iChannel] + "+"), "<" + channelNames[iChannelValue] + ">");
                        }
                    }
            };
%>
<% include(new JspView(JspLoader.createPage(request, GraphView.class, "setGraphSize.jsp")), out);%>
<% for (Callback callback : callbacks)
{ %>
<p><%=callback.title%></p>
<table>
    <tr><td>&nbsp;</td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td><%=h(channelNames[iChannelValue])%></td>
        <% } %>
    </tr>
    <% for (int iChannel = 0; iChannel < channelCount; iChannel ++)
    {
        FlowWell well = wellMap.get(channelNames[iChannel] + "+");
    %>
    <tr>
        <%if (well == null) { %>
        <td>N/A</td>
        <% } else { %>
        <td><a href="<%=h(well.urlShow())%>"><%=channelNames[iChannel]%></a></td>
        <% } %>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td><%=callback.render(iChannel, iChannelValue)%></td>
        <%}%>
    </tr>
    <%}%>
</table>
<% } %>