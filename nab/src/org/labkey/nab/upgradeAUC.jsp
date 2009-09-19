<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page import="org.labkey.api.pipeline.PipelineUrls" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<form action="upgradeNabAUC.view" method="post">
    <table width="75%">
        <tr class="labkey-wp-header">
            <th colspan=2>NAb AUC Conversion</th>
        </tr>
        <tr><td>&nbsp;</td></tr>
        <tr>
            <td><i>Calculation of NAb area under the curve (AUC) values for all curve fit methods will be performed for
                all existing NAb runs.
                Additionally, IC calculations will be performed for all curve fit methods. The upgrade task may be long
                running but will be
                invoked as a pipeline job, you will be able to monitor progress and view log information from
                the <a href="<%=PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(ContainerManager.getRoot())%>">pipeline status </a>page.</i></td>
    </tr>
    <tr><td>&nbsp;</td></tr>
    <tr><td><input type="submit" value="Upgrade"> </td></tr>
</table>
</form>
