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
<%@ page import="org.labkey.api.util.Formats"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.util.Pair"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.ms2.MS2Fraction"%>
<%@ page import="org.labkey.ms2.MS2GZFileRenderer"%>
<%@ page import="org.labkey.ms2.MS2Manager"%>
<%@ page import="org.labkey.ms2.MS2Peptide" %>
<%@ page import="org.labkey.ms2.MS2RunType" %>
<%@ page import="org.labkey.ms2.MassType" %>
<%@ page import="org.labkey.ms2.ShowPeptideContext" %>
<%@ page import="org.labkey.ms2.reader.LibraQuantResult" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("MS2/lorikeet");
    }
%>
<%
    JspView<ShowPeptideContext> me = (JspView<ShowPeptideContext>) HttpView.currentView();
    ShowPeptideContext ctx = me.getModelBean();
    MS2Peptide p = ctx.peptide;
    LibraQuantResult libra = p.getLibraQuantResult();
    MS2Fraction fraction = MS2Manager.getFraction(p.getFraction());
    org.labkey.ms2.MS2Run run = ctx.run;
%>
<style type="text/css">
    /*Hide the redundant peptide sequence info*/
    div#seqinfo { display: none; }

    .lk-show-peptide-table td {
        padding: 4px 0;
    }
</style>

<!--OUTER-->
<table class="lk-show-peptide-table">

<!--FIRST ROW-->
<tr><td colspan="2" valign=top width="850px">
    <table width="100%">
    <tr><td>
<%
        if (fraction.wasloadedFromGzFile())
        {
            out.println(" " + MS2GZFileRenderer.getFileNameInGZFile(fraction.getFileName(), p.getScan(), p.getCharge(), "dta") + "<br>");
        }
%>
<%
    if (fraction.wasloadedFromGzFile() && null != ctx.showGzUrl)
    {
        String[] gzFileExtensions = ctx.run.getGZFileExtensions();

        for (String gzFileExtension : gzFileExtensions)
        {
            ctx.showGzUrl.replaceParameter("extension", gzFileExtension);
            out.println("    " + textLink("Show " + gzFileExtension.toUpperCase(), ctx.showGzUrl));
        }
    }
    out.print(text(ctx.modificationHref));
%>

    </td></tr>
    <tr>
        <td>
            <table class="lk-fields-table">
                <tr>
                    <td class="labkey-form-label" width="85px">Scan</td><td width="95px"><%=p.getScan()%></td>
                    <td class="labkey-form-label" width="110px">Delta Mass</td><td width="95px"><%= h(Formats.signf4.format(p.getDeltaMass())) %></td>
                    <td class="labkey-form-label" width="85px">Protein</td><td><%= h(p.getProtein()) %></td>
                </tr>
                <tr>
                    <td class="labkey-form-label">Mass</td><td><%= h(Formats.f4.format(p.getMass())) %></td>
                    <% if (run.getRunType().getScoreColumnList().size() >= 2) { %>
                        <td class="labkey-form-label"><%= h(run.getRunType().getScoreColumnList().get(1)) %></td><td><%= h(p.getDiffScore() == null ? "" : Formats.f3.format(p.getDiffScore())) %></td>
                    <% } %>
                    <td class="labkey-form-label">Fraction</td><td><%= h(fraction.getFileName()) %></td>
                </tr>
                <tr>
                    <% if (run.getRunType().getScoreColumnList().size() >= 1) { %>
                        <td class="labkey-form-label"><%= h(run.getRunType().getScoreColumnList().get(0)) %></td><td><%= h(p.getRawScore() == null ? "" : Formats.f3.format(p.getRawScore())) %></td>
                    <% } %>
                    <td class="labkey-form-label">PeptideProphet</td><td><%= h((p.getPeptideProphet() == null) ? "" : Formats.f2.format(p.getPeptideProphet())) %></td>
                    <td class="labkey-form-label" rowspan="2">Run</td><td rowspan="2"><%= h(run.getDescription()) %></td>
                </tr>
                <tr>
                    <td class="labkey-form-label">Protein Hits</td><td><%= p.getProteinHits() %></td>
                    <td class="labkey-form-label">Ion Percent</td><td><%= h(Formats.percent.format(p.getIonPercent())) %></td>
                </tr>
                <tr>
                    <td class="labkey-form-label">Charge</td><td><%=p.getCharge()%>+</td>
                    <% if (run.getRunType().getScoreColumnList().size() >= 3) { %>
                        <td class="labkey-form-label"><%= h(run.getRunType().getScoreColumnList().get(2)) %></td><td><%= h(p.getZScore() == null ? "" : Formats.f3.format(p.getZScore())) %></td>
                    <% } %>
                </tr>
                <% if (MS2RunType.Mascot.equals(run.getRunType())) { %>
                    <tr>
                        <td class="labkey-form-label">Query Number</td><td><%= p.getQueryNumber() == null ? "" : p.getQueryNumber() %></td>
                        <td class="labkey-form-label">Hit Rank</td><td><%= p.getHitRank() %></td>
                    </tr>
                    <tr>
                        <td class="labkey-form-label">Is Decoy</td><td><%= p.isDecoy() %></td>
                    </tr>
                <% } %>
            </table>

<%
            // display the Libra quantitation normalization values, if applicable
            if (libra != null)
            {
%>
            <br/>
            <table class="lk-fields-table">
                <tr>
                    <td class="labkey-form-label">iTRAQ/TMT Channel</td>
                    <td class="labkey-form-label">Target Mass</td>
                    <td class="labkey-form-label">Normalized</td>
                    <td class="labkey-form-label">Absolute Intensity</td>
                </tr>
                <% if (libra.getNormalized1() != null) { %>
                    <tr>
                        <td align="right">1</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass1())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized1())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity1())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized2() != null) { %>
                    <tr>
                        <td align="right">2</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass2())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized2())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity2())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized3() != null) { %>
                    <tr>
                        <td align="right">3</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass3())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized3())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity3())) %></td>
                    </tr>
                    <% } %>
                <% if (libra.getNormalized4() != null) { %>
                    <tr>
                        <td align="right">4</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass4())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized4())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity4())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized5() != null) { %>
                    <tr>
                        <td align="right">5</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass5())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized5())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity5())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized6() != null) { %>
                    <tr>
                        <td align="right">6</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass6())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized6())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity6())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized7() != null) { %>
                    <tr>
                        <td align="right">7</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass7())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized7())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity7())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized8() != null) { %>
                    <tr>
                        <td align="right">8</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass8())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized8())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity8())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized9() != null) { %>
                    <tr>
                        <td align="right">9</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass9())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized9())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity9())) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized10() != null) { %>
                    <tr>
                        <td align="right">10</td>
                        <td align="right"><%= h(Formats.f3.format(libra.getTargetMass10())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getNormalized10())) %></td>
                        <td align="right"><%= h(Formats.f3.format(libra.getAbsoluteIntensity10())) %></td>
                    </tr>
                <% } %>
            </table>
<%
            }
%>                
        </td>
    </tr>
</table></td></tr>
</table>

<%
float[] mzs = p.getSpectrumMZ();
float[] intensities = p.getSpectrumIntensity();
if (mzs != null && intensities != null && mzs.length == intensities.length && mzs.length > 0)
{
%>
<!-- PLACE HOLDER DIV FOR THE SPECTRUM -->
<div id="lorikeet"></div>
<script type="text/javascript">

    $(function() {
        /* render the spectrum with the given options */
        $("#lorikeet").specview({
            sequence: <%= PageFlowUtil.jsString(p.getTrimmedPeptide()) %>,
            precursorMz: 1,
            staticMods: staticMods,
            variableMods: varMods,
            width: 600,
            // Pretend to be one charge state higher so that the viewer shows the right number of y/b charge ions
            charge: <%= p.getCharge() + 1 %>,
            peaks: peaks,
            extraPeakSeries: extraPeakSeries
        });
    });

var staticMods = [];
<%
int staticModIndex = 0;
for (org.labkey.ms2.MS2Modification mod : run.getModifications(org.labkey.ms2.MassType.Average))
{
    if (!mod.getVariable())
    { %>
        <%= h(staticModIndex == 0 ? "" : ",") %>staticMods[<%= staticModIndex++%>] = { modMass: <%= h(Formats.f4.format(mod.getMassDiff())) %>, aminoAcid: <%= PageFlowUtil.jsString(mod.getAminoAcid())%>}<%
    }
}
%>

var varMods = [];
<%
String trimmedWithMods = MS2Peptide.trimPeptide(p.getPeptide());
int aaIndex = 0;
int varModIndex = 0;
for (int i = 0; i < trimmedWithMods.length(); i++)
{
    if (Character.isLetter(trimmedWithMods.charAt(i)))
    {
        aaIndex++;
    }
    else
    {
        Double varModWeight=run.getVarModifications(MassType.Average).get(trimmedWithMods.substring(i - 1, i + 1));
        if (varModWeight != null)
        { %>
            varMods[<%= varModIndex++ %>] = {index: <%= aaIndex %>, modMass: <%= h(Formats.f4.format(varModWeight)) %>, aminoAcid: '<%= trimmedWithMods.charAt(i - 1)%>'}; <%
        }
    }
}
%>

// peaks in the scan: [m/z, intensity] pairs.
var peaks = [
    <%
    boolean firstPeak = true;
    java.util.Map<String, java.util.List<Pair<Float, Float>>> customHits = new java.util.HashMap<>();
    for (int i = 0; i < mzs.length; i++)
    {
        String libraMatch = libra != null ? libra.getMatch(mzs[i], 0.2) : null;
        if (libraMatch != null)
        {
            java.util.List<Pair<Float, Float>> peaks = customHits.get(libraMatch);
            if (peaks == null)
            {
                peaks = new ArrayList<>();
                customHits.put(libraMatch, peaks);
            }
            peaks.add(new Pair<>(mzs[i], intensities[i]));
        }
        else
        { %>
            <%= h(firstPeak ? "" : ",") %> [<%= mzs[i]%>, <%= intensities[i]%>]<%
            firstPeak = false;
        }
    } %>
];
var extraPeakSeries = [];
<%
int seriesIndex = 0;
for (Map.Entry<String,java.util.List<Pair<Float,Float>>> customHit : customHits.entrySet())
{ %>
    extraPeakSeries[<%= seriesIndex++ %>] = { data: [ <%
    boolean firstCustomPeak = true;
    for (Pair<Float,Float> peak : customHit.getValue())
    { %>
        <%= h(firstCustomPeak ? "" : ", ") %><% firstCustomPeak = false; %>[<%= h(Formats.f4.format(peak.getKey())) %>, <%= h(Formats.f4.format(peak.getValue())) %>] <%
    } %>
], color: "green"}; <%
} %>
</script>
<% }
else { %> Spectra not available.<% }%>