<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.OldMS2Controller" %>
<%@ page import="org.labkey.ms2.protein.tools.ProteinDictionaryHelpers.*" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.GoChartBean bean = ((JspView<MS2Controller.GoChartBean>) HttpView.currentView()).getModelBean();
%>
Run: <%=h(bean.run.toString())%><br>
<%=h(bean.peptideFilterInfo)%><br>
<%=h(bean.proteinFilterInfo)%><br>
<%=h(bean.proteinGroupFilterInfo)%><br>
<form name="chartForm" action="peptideCharts.view">
<%=bean.imageMap%>
<table align="left">
<tr>
    <td>
        <table>
            <tr>
                <td valign="middle">Chart type:</td>
                <td valign="middle">
                    <select name="chartType" id="chartType">
                        <option value="<%=GoTypes.CELL_LOCATION%>"<%=GoTypes.CELL_LOCATION == bean.goChartType ? " selected" : ""%>><%=GoTypes.CELL_LOCATION%></option>
                        <option value="<%=GoTypes.FUNCTION%>"<%=GoTypes.FUNCTION == bean.goChartType ? " selected" : ""%>><%=GoTypes.FUNCTION%></option>
                        <option value="<%=GoTypes.PROCESS%>"<%=GoTypes.PROCESS == bean.goChartType ? " selected" : ""%>><%=GoTypes.PROCESS%></option>
                    </select>
                </td>
                <td valign="middle"><input type="image" src="<%=PageFlowUtil.buttonSrc("Submit")%>"></td>
            </tr>
        </table>
    </td>
</tr>
<tr><td>
<img src="<%=h(bean.chartUrl)%>" width="800" height="800" alt="GO Chart" usemap="#pie1">
<input type="hidden" name="run" value="<%=bean.run.getRun()%>">
<input type="hidden" name="queryString" value="<%=h(bean.queryString)%>">
<input type="hidden" name="grouping" value="<%=h(bean.grouping)%>">
</td></tr>
</table>
</form>
