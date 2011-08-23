<%
/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
<%@ page import="org.labkey.api.settings.AppProps"%>
<%@ page import="org.labkey.api.util.Formats"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.ms2.MS2Controller"%>
<%@ page import="org.labkey.ms2.MS2Fraction" %>
<%@ page import="org.labkey.ms2.MS2GZFileRenderer" %>
<%@ page import="org.labkey.ms2.MS2Manager" %>
<%@ page import="org.labkey.ms2.MS2Peptide" %>
<%@ page import="org.labkey.ms2.ShowPeptideContext" %>
<%@ page import="java.util.Collections" %>
<%@ page import="org.labkey.ms2.MS2Run" %>
<%@ page import="org.labkey.ms2.reader.LibraQuantResult" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ShowPeptideContext> me = (JspView<ShowPeptideContext>) HttpView.currentView();
    ShowPeptideContext ctx = me.getModelBean();
    MS2Peptide p = ctx.peptide;
    LibraQuantResult libra = p.getLibraQuantResult();
    MS2Fraction fraction = MS2Manager.getFraction(p.getFraction());
    org.labkey.ms2.MS2Run run = ctx.run;
%>
<!--OUTER-->
<table cellspacing="8px">

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
    out.print(ctx.modificationHref);
%>

    </td></tr>

    <tr>
        <td>
            <table width="100%">
                <tr>
                    <td class="labkey-form-label" width="85px">Scan</td><td width="95px"><%=p.getScan()%></td>
                    <td class="labkey-form-label" width="110px">Delta Mass</td><td width="95px"><%= Formats.signf4.format(p.getDeltaMass()) %></td>
                    <td class="labkey-form-label" width="85px">Protein</td><td><%= h(p.getProtein()) %></td>
                </tr>
                <tr>
                    <td class="labkey-form-label">Mass</td><td><%= Formats.f4.format(p.getMass()) %></td>
                    <td class="labkey-form-label"><%= h(run.getRunType().getScoreColumnList().get(1)) %></td><td><%= p.getDiffScore() == null ? "" : Formats.f3.format(p.getDiffScore()) %></td>
                    <td class="labkey-form-label">Fraction</td><td><%= h(fraction.getFileName()) %></td>
                </tr>
                <tr>
                    <td class="labkey-form-label"><%= h(run.getRunType().getScoreColumnList().get(0)) %></td><td><%= p.getRawScore() == null ? "" : Formats.f3.format(p.getRawScore()) %></td>
                    <td class="labkey-form-label">PeptideProphet</td><td><%= Formats.f2.format(p.getPeptideProphet()) %></td>
                    <td class="labkey-form-label" rowspan="2">Run</td><td rowspan="2"><%= h(run.getDescription()) %></td>
                </tr>
                <tr>
                    <td class="labkey-form-label">Protein Hits</td><td><%= p.getProteinHits() %></td>
                    <td class="labkey-form-label">Ion Percent</td><td><%= Formats.percent.format(p.getIonPercent()) %></td>
                </tr>
                <tr>
                    <td class="labkey-form-label">Charge</td><td><%=p.getCharge()%>+</td>
                    <td class="labkey-form-label"><%= h(run.getRunType().getScoreColumnList().get(2)) %></td><td><%= p.getZScore() == null ? "" : Formats.f3.format(p.getZScore()) %></td>
                </tr>
            </table>

<%
            // display the Libra quantitation normalization values, if applicable
            if (libra != null)
            {
%>
            <br/>
            <table width="100%">
                <tr><td colspan="6" style="font-size:110%;font-weight:bold;">iTRAQ Quantitation</td></tr>

                <% if (libra.getNormalized1() != null) { %>
                    <tr>
                    <td class="labkey-form-label" width="85px">Normalized 1</td><td width="95px"><%= Formats.f3.format(libra.getNormalized1()) %></td>
                    <td class="labkey-form-label" width="175px">Absolute Raw Intensity 1</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity1()) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized2() != null) { %>
                    <tr>
                    <td class="labkey-form-label">Normalized 2</td><td><%= Formats.f3.format(libra.getNormalized2()) %></td>
                    <td class="labkey-form-label">Absolute Raw Intensity 2</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity2()) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized3() != null) { %>
                    <tr>
                    <td class="labkey-form-label">Normalized 3</td><td><%= Formats.f3.format(libra.getNormalized3()) %></td>
                    <td class="labkey-form-label">Absolute Raw Intensity 3</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity3()) %></td>
                    </tr>
                    <% } %>
                <% if (libra.getNormalized4() != null) { %>
                    <tr>
                    <td class="labkey-form-label">Normalized 4</td><td><%= Formats.f3.format(libra.getNormalized4()) %></td>
                    <td class="labkey-form-label">Absolute Raw Intensity 4</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity4()) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized5() != null) { %>
                    <tr>
                    <td class="labkey-form-label">Normalized 5</td><td><%= Formats.f3.format(libra.getNormalized5()) %></td>
                    <td class="labkey-form-label">Absolute Raw Intensity 5</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity5()) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized6() != null) { %>
                    <tr>
                    <td class="labkey-form-label">Normalized 6</td><td><%= Formats.f3.format(libra.getNormalized6()) %></td>
                    <td class="labkey-form-label">Absolute Raw Intensity 6</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity6()) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized7() != null) { %>
                    <tr>
                    <td class="labkey-form-label">Normalized 7</td><td><%= Formats.f3.format(libra.getNormalized7()) %></td>
                    <td class="labkey-form-label">Absolute Raw Intensity 7</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity7()) %></td>
                    </tr>
                <% } %>
                <% if (libra.getNormalized8() != null) { %>
                    <tr>
                    <td class="labkey-form-label">Normalized 8</td><td><%= Formats.f3.format(libra.getNormalized8()) %></td>
                    <td class="labkey-form-label">Absolute Raw Intensity 8</td><td><%= Formats.f3.format(libra.getAbsoluteIntensity8()) %></td>
                    </tr>
                <% } %>
            </table>
<%
            }
%>                
        </td>
    </tr>
</table></td></tr>
<!--FIRST ROW-->

<!--SECOND ROW LEFT-->
<tr><td valign=top width="610">
    <table style="border: solid #EEEEEE 2px">
    <tr><td valign=top height="100%">
<%
    ActionURL graphUrl = ctx.url.clone();

    graphUrl.setAction(MS2Controller.ShowGraphAction.class);
    graphUrl.deleteParameter("rowIndex");
    graphUrl.addParameter("width", "600");
    graphUrl.addParameter("height", "350");

    if (Double.MIN_VALUE != ctx.form.getxStartDouble()) graphUrl.replaceParameter("xStart", ctx.form.getStringXStart());
    if (Double.MAX_VALUE != ctx.form.getxEnd()) graphUrl.replaceParameter("xEnd", ctx.form.getStringXEnd());

    graphUrl.replaceParameter("tolerance", String.valueOf(ctx.form.getTolerance()));
%>
    <img src="<%=graphUrl.getEncodedLocalURIString()%>" height="350px" width="600px" alt="Spectrum Graph">
    </td></tr>
    <tr><td><center>
    <form method=get action="updateShowPeptide.post">
        X&nbsp;Start&nbsp;<input name="xStart" id="xStart" value="<%=ctx.actualXStart%>" size=8>
        X&nbsp;End&nbsp;<input name="xEnd" id="xEnd" value="<%=ctx.actualXEnd%>" size=8>
        <input name="queryString" type="hidden" value="<%=h(ctx.url.getRawQuery())%>">
        <%=generateSubmitButton("Scale Graph")%>
    </form>
    </center></td></tr>
    </table>
</td>
<!--SECOND ROW LEFT-->

<!--SECOND ROW RIGHT (FRAGMENT)-->
<td valign=top width="240px">
<table width="230px" style="border: solid #EEEEEE 2px">
    <tr align=center bgcolor="#EEB422">
<%
    // Render fragment table
    for (int j = p.getIonCount() - 1; j >= 0; j--)
    {
        out.print("<th>b<sup>");
        if (0 < j)
            out.print(j + 1);
        out.print("+</sup></th>");
    }

    out.print("<th>#</th><th>AA</th><th>#</th>");

    for (int j = 0; j < p.getIonCount(); j++)
    {
        out.print("<th>y<sup>");
        if (0 < j)
            out.print(j + 1);
        out.print("+</sup></th>");
    }

    out.println("</tr>");

    String[] aa = p.getAAs();
    boolean[][] bMatches = p.getBMatches();
    double[][] b = p.getBFragments();
    boolean[][] yMatches = p.getYMatches();
    double[][] y = p.getYFragments();
    String color;

    for (int i = 0; i < p.getAACount(); i++)
    {
        out.print("    <tr align=right>");

        for (int j = p.getIonCount() - 1; j >= 0; j--)
        {
            color = "\"#FFFFFF\"";

            if (p.getFragmentCount() > i)
            {
                if (bMatches[j][i])
                    out.print("<td bgcolor=\"#FF7F7F\"><b><tt>" + Formats.f4.format(b[j][i]) + "</tt></b></td>");
                else
                    out.print("<td bgcolor=\"#FFFFFF\"><tt>" + Formats.f4.format(b[j][i]) + "</tt></td>");
            }
            else
                out.print("<td bgcolor=" + color + "><tt>&nbsp;</tt></td>");
        }

        out.print("<td bgcolor=\"#EEB422\"><b><tt>" + (i + 1) + "</tt></b></td>");

        if (aa[i].length() > 1)
            color = "\"#FFFFAA\"";
        else
            color = "\"#EEB422\"";

        out.print("<td bgcolor=" + color + " align=center><b><tt>" + aa[i] + "</tt></b></td>");
        out.print("<td bgcolor=\"#EEB422\"><b><tt>" + (p.getAACount() - i) + "</tt></b></td>");

        for (int j = 0; j < p.getIonCount(); j++)
        {
            color = "\"#FFFFFF\"";

            if (i > 0)
            {
                int index = p.getAACount() - i - 1;

                if (yMatches[j][index])
                    out.print("<td bgcolor=\"#AAAAFF\"><b><tt>" + Formats.f4.format(y[j][index]) + "</tt></b></td>");
                else
                    out.print("<td bgcolor=" + color + "><tt>" + Formats.f4.format(y[j][index]) + "</tt></td>");
            }
            else
                out.print("<td bgcolor=" + color + "><tt>&nbsp;</tt></td>");
        }

        out.println("</tr>");
    }
%>

</table>
</td>
<!--SECOND ROW RIGHT (FRAGMENT)-->

</tr></table>
