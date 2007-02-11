<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Map"%>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%=pageHeader(ScriptController.Action.editAnalysis)%>
<% ScriptController.AnalysisForm bean = (ScriptController.AnalysisForm) form; %>
<%Map<String,String> params = form.getParameters();
  List<String> subsets = form.analysisScript.getSubsets();
%>
<script src="<%=request.getContextPath()%>/Flow/editScript.js"></script>
<script>
    function addStat()
    {
        var subset = getValue(document.getElementById("stat_subset"));
        var stat = getValue(document.getElementById("stat_stat"));
        var parameter = getValue(document.getElementById("stat_parameter"));

        var statistic;

        if (stat == "Median" || stat == "Mean" || stat == "Std_Dev" || stat == "Percentile")
        {
            if (!parameter)
            {
                alert("You must specify a parameter for the statistic '" + stat + "'");
                return;
            }
            if (subset)
            {
                statistic = subset + ":";
            }
            if (stat == "Percentile")
            {
                var percentile = getValue(document.getElementById("stat_parameter2"));
                var num = new Number(percentile);
                if (!(num >= 0 && num <= 100))
                {
                    alert(percentile + " needs to be a number between 0 and 100");
                    document.getElementById('stat_parameter2').focus();
                    return;
                }
                parameter += ":" + percentile;
            }

            statistic += stat + "(" + parameter + ")";
        }
        else
        {
            if (subset)
            {
                statistic = subset + ":";
            }
            else
            {
                if (stat != "Count")
                {
                    alert("You cannot calculate the " + stat + " statistic on the ungated population.");
                    return;
                }
                statistic = "";
            }
            statistic += stat;
        }
        appendLine(document.getElementsByName("statistics")[0], statistic);
    }
    function addGraph()
    {
        var subset = getValue(document.getElementById("graph_subset"));
        var x = getValue(document.getElementById("graph_x"));
        var y = getValue(document.getElementById("graph_y"));
        var graph = "";
        if (subset)
        {
            graph = subset;
        }
        graph += "(" + x + ":" + y + ")";
        appendLine(document.getElementsByName("graphs")[0], graph);
    }
</script>

<form method="post" action="<%=formAction(ScriptController.Action.editAnalysis)%>">
    <p class="normal">Which statistics do you want to calculate? Enter one statistic per line.<br>
        <textarea name="statistics" rows="10" cols="60" wrap="off"><%=h(bean.statistics)%></textarea><br>
        <table>
            <tr><th>Subset</th><th>Statistic</th><th>Parameter</th><th>Percentile</th></tr>
            <tr><td>
                <select id="stat_subset">
                    <option value="">Ungated</option>
                    <% for (String subset : subsets)
                    { %>
                    <option value="<%=h(subset)%>"><%=h(subset)%></option>
                    <% } %>
                </select></td>
                <td>
                    <select id="stat_stat">
                        <option value="Count">Count</option>
                        <option value="Frequency">Frequency</option>
                        <option value="Freq_Of_Parent">Freq_Of_Parent</option>
                        <option value="Freq_Of_Grandparent">Freq_Of_Grandparent</option>
                        <option value="Median">Median</option>
                        <option value="Mean">Mean</option>
                        <option value="Std_Dev">Standard Deviation</option>
                        <option value="Percentile">Percentile</option>
                    </select>
                </td>
                <td>
                    <select id="stat_parameter">
                        <% for(Map.Entry<String,String> param : params.entrySet()) { %>
                            <option value="<%=h(param.getKey())%>"><%=h(param.getValue())%></option>
                        <% } %>
                    </select>
                </td>
                <td>
                    <input id="stat_parameter2">
                </td>
                <td><input type="button" onclick="addStat()" value="Add Statistic"></td>
            </tr>
        </table>

    </p>
    <p class="normal">Which graphs do you want to calculate? Enter one graph per line.<br>
        <textarea name="graphs" rows="10" cols="60" wrap="off"><%=h(bean.graphs)%></textarea><br>
        <table>
            <tr><th>Subset</th><th>X Axis</th><th>Y Axis</th></tr>
            <tr><td>
                <select id="graph_subset">
                    <option value="">Ungated</option>
                    <% for (String subset : subsets)
                    { %>
                    <option value="<%=h(subset)%>"><%=h(subset)%></option>
                    <% } %>
                </select></td>
                <td>
                    <select id="graph_x">
                        <% for(Map.Entry<String,String> param : form.getParameters().entrySet()) {%>
                        <option value="<%=h(param.getKey())%>"><%=h(param.getValue())%></option>
                        <% } %>
                    </select>
                </td>
                <td>
                    <select id="graph_y">
                        <% for(Map.Entry<String,String> param : params.entrySet()) { %>
                            <option value="<%=h(param.getKey())%>"><%=h(param.getValue())%></option>
                        <% } %>
                    </select>
                </td>
                <td><input type="button" onclick="addGraph()" value="Add Graph"></td>
            </tr>
        </table>

    </p>
    <input type="submit" value="Submit">

</form>