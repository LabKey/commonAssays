<%@ page import="com.labkey.flow.model.CompensationMatrix" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="com.labkey.flow.web.GraphSpec" %>
<%@ page import="java.util.List" %>
<%@ page import="org.fhcrc.cpas.flow.data.*" %>
<%@ page import="org.fhcrc.cpas.view.ViewURLHelper" %>
<%@ page import="Flow.Well.WellController" %>
<%@ page import="Flow.FlowParam" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.fhcrc.cpas.jsp.JspLoader"%>
<%@ page import="org.fhcrc.cpas.flow.view.GraphView"%>
<%@ page import="org.fhcrc.cpas.view.JspView"%>
<%@ page extends="org.fhcrc.cpas.jsp.FormPage" %>
<%!
    String compImg(FlowWell well, String param) throws Exception
    {
        if (well == null)
            return "N/A";

        GraphSpec spec = new GraphSpec(null, param);
        if (well.getGraphBytes(spec) == null)
            return "N/A";
        ViewURLHelper urlGraph = well.urlFor(WellController.Action.showGraph);
        urlGraph.addParameter(FlowParam.graph.toString(), spec.toString());
        return "<img class=\"flow-graph\" border=\"0\" src=\"" + h(urlGraph) + "\">";
    }
%>
<%
    final FlowCompensationMatrix flowComp = FlowCompensationMatrix.fromURL(getViewContext().getViewURLHelper(), request);
    final CompensationMatrix comp = flowComp.getCompensationMatrix();
    final String[] channelNames = comp.getChannelNames();
    final int channelCount = channelNames.length;
    DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(3);
%>

    <p>Compensation Matrix: <%=h(comp.getName())%></p>
<table class="normal">
    <tr><td>&nbsp;</td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td><%=h(channelNames[iChannelValue])%></td>
        <% } %>
    </tr>
    <% for (int iChannel = 0; iChannel < channelCount; iChannel ++)
    {
    %>
    <tr>
        <td><%=h(channelNames[iChannel])%></td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td><%=format.format(comp.getRow(iChannel)[iChannelValue])%></td>
        <%}%>
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
<table class="normal">
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
        <td><a href="<%=h(well.urlShow())%>"><%=channelNames[iChannel]%></a></td>
        <% for (int iChannelValue = 0; iChannelValue < channelCount; iChannelValue ++)
        { %>
        <td><%=callback.render(iChannel, iChannelValue)%></td>
        <%}%>
    </tr>
    <%}%>
</table>
<% } %>