<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.query.QueryPicker" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.query.QueryView" %>
<%@ page import="java.io.PrintWriter" %>
<%
JspView<QueryView> view = (JspView<QueryView>)HttpView.currentView();
QueryView peptidesView = view.getModelBean();
%>

<p>Select a way to compare the runs, and the columns to include in the comparison:</p>
<table>
    <tr>
        <td rowspan="5" valign="top"><input type="radio" name="column" value="ProteinProphet" checked /></td>
        <td colspan="2"><b>ProteinProphet</b></td>
    </tr>
    <tr>
        <td><input type="checkbox" name="proteinGroup" value="1" checked="checked" disabled>Protein Group</td>
        <td><input type="checkbox" name="groupProbability" value="1" checked="checked">Group Probability</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="light2HeavyRatioMean" value="1">Light to Heavy Quantitation</td>
        <td><input type="checkbox" name="heavy2LightRatioMean" value="1">Heavy to Light Quantitation</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="totalPeptides" value="1">Total Peptides</td>
        <td><input type="checkbox" name="uniquePeptides" value="1">Unique Peptides</td>
    </tr>

    <tr><td>&nbsp;</td></tr>

    <tr>
        <td rowspan="3" valign="top"><input type="radio" name="column" value="Protein" /></td>
        <td colspan="2"><b>Search Engine Protein Assignment</b></td>
    </tr>
    <tr>
        <td><input type="checkbox" name="unique" value="1" checked="checked">Unique Peptides</td>
        <td><input type="checkbox" name="total" value="1">Total Peptides</td>
    </tr>
    <!--        <input type="checkbox" name="sumLightArea-Protein" value="1">Total light area (quantitation)<br/>
            <input type="checkbox" name="sumHeavyArea-Protein" value="1">Total heavy area (quantitation)<br/>
            <input type="checkbox" name="avgDecimalRatio-Protein" value="1">Average decimal ratio (quantitation)<br/>
            <input type="checkbox" name="maxDecimalRatio-Protein" value="1">Maximum decimal ratio (quantitation)<br/>
            <input type="checkbox" name="minDecimalRatio-Protein" value="1">Minimum decimal ratio (quantitation)<br/>
            -->

    <tr><td>&nbsp;</td></tr>

    <tr>
        <td rowspan="8" valign="top"><input type="radio" name="column" value="Peptide" /></td>
        <td colspan="2"><b>Peptide</b></td>
    </tr>
    <tr>
        <td><input type="checkbox" name="peptideCount" value="1" checked="checked" disabled>Count</td>
        <td></td>
    </tr>
    <tr>
        <td><input type="checkbox" name="maxPeptideProphet" value="1" checked="checked">Maximum Peptide Prophet Probability</td>
        <td><input type="checkbox" name="avgPeptideProphet" value="1" checked="checked">Average Peptide Prophet Probability</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="minPeptideProphetErrorRate" value="1">Minimum Peptide Prophet Error Rate</td>
        <td><input type="checkbox" name="avgPeptideProphetErrorRate" value="1">Average Peptide Prophet Error Rate</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="sumLightArea-Peptide" value="1">Total light area (quantitation)</td>
        <td><input type="checkbox" name="sumHeavyArea-Peptide" value="1">Total heavy area (quantitation)</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="avgDecimalRatio-Peptide" value="1">Average decimal ratio (quantitation)</td>
        <td><input type="checkbox" name="maxDecimalRatio-Peptide" value="1">Maximum decimal ratio (quantitation)</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="minDecimalRatio-Peptide" value="1">Minimum decimal ratio (quantitation)</td>
        <td></td>
    </tr>
</table>

<br/><hr/><br/>

<table>
    <tr>
        <td rowspan="2" valign="top"><input type="radio" name="column" value="Query" /></td>
        <td colspan="2"><b>ProteinProphet (Query)</b></td>
    </tr>
    <tr>
        <td colspan="2">The query-based comparison does not use the view selected above. Instead, please follow the instructions at the top of the comparison page to customize the results. It is based on ProteinProphet protein groups, so the runs must be associated with ProteinProphet data.</td>
    </tr>
</table>

<!--        <input type="radio" name="column" value="QueryPeptides" /><b>Query Peptides (beta)</b><br/> -->

<br/><hr/><br/>

<table>
    <tr>
        <td rowspan="4" valign="top"><input type="radio" name="column" value="spectraCount" /></td>
        <td colspan="2"><b>Spectra Counts</b></td>
    </tr>
    <tr>
        <td>Group by:
            <input type="checkbox" checked="true" name="spectaGroupByPeptide"> Peptide&nbsp;&nbsp;&nbsp;
            <input type="checkbox" name="spectaGroupByCharge"> Charge&nbsp;&nbsp;&nbsp;
            <input type="checkbox" name="spectaGroupByProtein"> Protein&nbsp;&nbsp;&nbsp;
        </td>
    </tr>
    <tr>
        <td>Use protein assigned by:
            <input type="radio" checked="true" name="spectraProteinAssignment" value="searchEngine"> Search Engine&nbsp;&nbsp;&nbsp;
            <input type="radio" name="spectraProteinAssignment" value="proteinProphet"> ProteinProphet
        </td>
    </tr>
    <tr>
        <td>
            You may use a customized Peptides view to establish criteria for which peptides to include in the spectra counts.
            <%
            QueryPicker picker = peptidesView.getColumnListPicker(request);
            picker.setAutoRefresh(false);
            PrintWriter writer = new PrintWriter(out);
            peptidesView.renderCustomizeViewLink(writer);
            writer.flush();
            %>
            <%= picker.toString()%>
        </td>
    </tr>
</table>
