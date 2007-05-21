<%@ page import="org.labkey.api.view.ViewURLHelper"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController"%>
<%@ page import="org.labkey.flow.analysis.chart.FlowLogarithmicAxis" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.GraphWindowPage" %>
<% ViewURLHelper urlGraph = getPageFlow().cloneViewURLHelper();
    urlGraph.setAction(ScriptController.Action.graphImage.toString());
%>
<script src="<%=resourceURL("util.js")%>"></script>
<script src="<%=resourceURL("graphWindow.js")%>"></script>
<script type="text/javascript">
    var LOG_LIN_SWITCH = <%=FlowLogarithmicAxis.LOG_LIN_SWITCH%>;
    var rcChart = <%=jscriptRect(plotInfo.getChartArea())%>;
    var rcData = <%=jscriptRect(plotInfo.getDataArea())%>;
    var rangeX = <%=jscriptRange(plotInfo.getRangeX())%>;
    var rangeY = <%=jscriptRange(plotInfo.getRangeY())%>;
    var handleSrc = '<%=resourceURL("handle.gif")%>';
    var handles = [];

</script>
<html>
<body style="margin:0" onload="updateImage()" leftMargin="0" topMargin="0"
      onmousedown="mouseDown(this, event)"
      onmousemove="mouseMove(this, event)"
      onmouseup="mouseUp(this, event)"
      scroll="NO">
<img GALLERYIMG="no" style="position:absolute;z-index:0" id="graph">
<!--<img GALLERYIMG="no" style="position:absolute;z-index:1" id="selection">-->
</body>
</html>
