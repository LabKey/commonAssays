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
<%@ page import="org.labkey.api.security.permissions.UpdatePermission"%>
<%@ page import="org.labkey.api.settings.AppProps"%>
<%@ page import="org.labkey.api.util.Formats"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.util.URLHelper"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.ms2.MS2Controller"%>
<%@ page import="org.labkey.ms2.MS2Fraction" %>
<%@ page import="org.labkey.ms2.MS2GZFileRenderer" %>
<%@ page import="org.labkey.ms2.MS2Manager" %>
<%@ page import="org.labkey.ms2.MS2Peptide" %>
<%@ page import="org.labkey.ms2.PeptideQuantitation" %>
<%@ page import="org.labkey.ms2.ShowPeptideContext" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ShowPeptideContext> me = (JspView<ShowPeptideContext>) HttpView.currentView();
    ShowPeptideContext ctx = me.getModelBean();
    MS2Peptide p = ctx.peptide;
    MS2Fraction fraction = MS2Manager.getFraction(p.getFraction());
%>
<!--OUTER-->
<table><tr>

<!--LEFT-->
<td valign=top width="610">
    <table>
    <tr><td height=60>
        <strong><%=p.getPeptide()%></strong>
        <%=PageFlowUtil.generateButton("Blast", AppProps.getInstance().getBLASTServerBaseURL() + p.getTrimmedPeptide(), "", "target=\"cmt\"")%><br>
        <%=p.getScan()%>&nbsp;&nbsp;<%=p.getCharge()%>+&nbsp;&nbsp;<%=h(p.getRawScore())%>&nbsp;&nbsp;<%=p.getDiffScore() == null ? "" : Formats.f3.format(p.getDiffScore())%>&nbsp;&nbsp;<%=p.getZScore() == null ? "" : Formats.f3.format(p.getZScore())%>&nbsp;&nbsp;<%=Formats.percent.format(p.getIonPercent())%>&nbsp;&nbsp;<%=Formats.f4.format(p.getMass())%>&nbsp;&nbsp;<%=Formats.signf4.format(p.getDeltaMass())%>&nbsp;&nbsp;<%=Formats.f4.format(p.getPeptideProphet())%>&nbsp;&nbsp;<%=p.getProteinHits()%><br>
        <%=p.getProtein()%><br>
<%
        if (fraction.wasloadedFromGzFile())
        {
            out.println("    " + MS2GZFileRenderer.getFileNameInGZFile(fraction.getFileName(), p.getScan(), p.getCharge(), "dta") + "<br>");
        }

        if (null == ctx.previousUrl)
            out.print(PageFlowUtil.generateDisabledButton("<< Prev"));
        else
            out.print("    " + generateButton("<< Prev", ctx.previousUrl));
%>&nbsp;
<%
    if (null == ctx.nextUrl)
        out.print(PageFlowUtil.generateDisabledButton("Next >>"));
    else
        out.print("    " + generateButton("Next >>", ctx.nextUrl));
%>&nbsp;
<%
    if (fraction.wasloadedFromGzFile() && null != ctx.showGzUrl)
    {
        String[] gzFileExtensions = ctx.run.getGZFileExtensions();

        for (String gzFileExtension : gzFileExtensions)
        {
            ctx.showGzUrl.replaceParameter("extension", gzFileExtension);
            out.println("    " + generateButton("Show " + gzFileExtension.toUpperCase(), ctx.showGzUrl) + "&nbsp;");
        }
    }
    out.print(ctx.modificationHref);
%>
        <% if(null != ctx.pepSearchHref && ctx.pepSearchHref.length() > 0) { %>
        <%=PageFlowUtil.generateButton("Find Features", ctx.pepSearchHref, "", "target=\"pepSearch\"")%>
        <% } %>

    </td></tr>

    <tr><td valign=top height="100%">
<%
    ActionURL graphUrl = ctx.url.clone();

    graphUrl.setAction(MS2Controller.ShowGraphAction.class);
    graphUrl.deleteParameter("rowIndex");
    graphUrl.addParameter("width", "600");
    graphUrl.addParameter("height", "400");

    if (Double.MIN_VALUE != ctx.form.getxStartDouble()) graphUrl.replaceParameter("xStart", ctx.form.getStringXStart());
    if (Double.MAX_VALUE != ctx.form.getxEnd()) graphUrl.replaceParameter("xEnd", ctx.form.getStringXEnd());

    graphUrl.replaceParameter("tolerance", String.valueOf(ctx.form.getTolerance()));
%>
    <img src="<%=graphUrl.getEncodedLocalURIString()%>" alt="Spectrum Graph">
    <form method=get action="updateShowPeptide.post">
        X&nbsp;Start&nbsp;<input name="xStart" id="xStart" value="<%=ctx.actualXStart%>" size=8>
        X&nbsp;End&nbsp;<input name="xEnd" id="xEnd" value="<%=ctx.actualXEnd%>" size=8>
        <input name="queryString" type="hidden" value="<%=h(ctx.url.getRawQuery())%>">
        <%=generateSubmitButton("Scale Graph")%>
    </form>
    </td></tr>
    </table>
</td>
<!--LEFT-->

<!--RIGHT (FRAGMENT)-->
<td valign=top>
<table width="230px">
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
<!--RIGHT (FRAGMENT)-->

</tr></table>

<hr/>
<%
if (p.getQuantitation() != null)
{
    PeptideQuantitation quant = p.getQuantitation();
    ActionURL elutionGraphUrl = ctx.url.clone();
    String errorMessage = ctx.url.getParameter("elutionProfileError");

    elutionGraphUrl.setAction(MS2Controller.ShowLightElutionGraphAction.class);
    elutionGraphUrl.deleteParameter("rowIndex");

    elutionGraphUrl.addParameter("tolerance", String.valueOf(ctx.form.getTolerance()));
    int currentCharge;
    if (ctx.form.getQuantitationCharge() > 0)
    {
        currentCharge = ctx.form.getQuantitationCharge();
    }
    else
    {
        currentCharge = p.getCharge();
    }
    DecimalFormat format = new DecimalFormat();
    ActionURL editUrl = ctx.url.clone();
    editUrl.setAction(MS2Controller.EditElutionGraphAction.class);
    ActionURL toggleUrl = ctx.url.clone();
    toggleUrl.setAction(MS2Controller.ToggleValidQuantitationAction.class);

    if (errorMessage != null)
    { %>
        <span class="labkey-error"><%= errorMessage %></span>
<%  }
%>
<table>
    <tr>
        <td colspan="2" align="center"><strong><a name="quantitation">Quantitation (performed on <%= p.getCharge() %>+)</a></strong></td>
    </tr>
    <tr>
        <td colspan="2" align="center">Currently showing elution profile for <%= currentCharge %>+. Show:
            <% for (int i = 1; i <= 6; i++)
            {
                if (currentCharge != i)
                {
                    URLHelper chargeUrl = ctx.url.clone().replaceParameter("quantitationCharge", Integer.toString(i)); %>
                    <a href="<%= chargeUrl %>#quantitation"><%= i %>+</a><%
                }
            } %>
            <% if (ctx.container.hasPermission(ctx.user, UpdatePermission.class)) {
                if (quant.findScanFile() != null && !"q3".equals(ctx.run.getQuantAnalysisType())) { %><%=textLink("edit elution profile", editUrl)%><% } %>
                <%=textLink((quant.includeInProteinCalc() ? "invalidate" : "revalidate") + " quantitation results", toggleUrl)%><%
            }%>
        </td>
    </tr>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Light</strong></td></tr>
                <tr>
                    <td><span style="font-size: smaller; ">Scans:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getLightFirstScan()%> - <%= quant.getLightLastScan()%></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Mass:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getLightMass() %></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Area:</span></td>
                    <td><span style="font-size: smaller; "><%= format.format(quant.getLightArea()) %></span></td>
                </tr>
            </table>
        </td>
        <td><img src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Light Elution Graph"/></td>
    </tr>
    <%
        elutionGraphUrl.setAction(MS2Controller.ShowHeavyElutionGraphAction.class);
    %>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Heavy</strong></td></tr>
                <tr>
                    <td><span style="font-size: smaller; ">Scans:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getHeavyFirstScan()%> - <%= quant.getHeavyLastScan()%></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Mass:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getHeavyMass() %></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Area:</span></td>
                    <td><span style="font-size: smaller; "><%= format.format(quant.getHeavyArea()) %></span></td>
                </tr>
            </table>
        </td>
        <td><img src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Heavy Elution Graph"/></td>
    </tr>
    <%
        elutionGraphUrl.setAction(MS2Controller.ShowCombinedElutionGraphAction.class);
    %>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Combined</strong></td></tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Heavy to light ratio:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getHeavy2LightRatio()%></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Light to heavy ratio:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getRatio()%></span></td>
                </tr>
            </table>
        </td>
        <td><img src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Combined Elution Graph"/></td>
    </tr>
</table>
<%
    }
    else
    {
%>
No quantitation data available.
<%
    }
%>