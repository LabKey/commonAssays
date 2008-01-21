<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.ms2.*"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.ms2.ShowPeptideContext"%>
<%@ page import="org.labkey.api.util.URLHelper"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="org.labkey.api.security.ACL"%>
<%@ page import="org.labkey.api.util.Formats"%>
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
        <b><%=p.getPeptide()%></b>
        <a target="cmt" href="http://www.ncbi.nlm.nih.gov/blast/Blast.cgi?CMD=Web&amp;LAYOUT=TwoWindows&amp;AUTO_FORMAT=Semiauto&amp;ALIGNMENTS=50&amp;ALIGNMENT_VIEW=Pairwise&amp;CDD_SEARCH=on&amp;CLIENT=web&amp;COMPOSITION_BASED_STATISTICS=on&amp;DATABASE=nr&amp;DESCRIPTIONS=100&amp;ENTREZ_QUERY=(none)&amp;EXPECT=1000&amp;FILTER=L&amp;FORMAT_OBJECT=Alignment&amp;FORMAT_TYPE=HTML&amp;I_THRESH=0.005&amp;MATRIX_NAME=BLOSUM62&amp;NCBI_GI=on&amp;PAGE=Proteins&amp;PROGRAM=blastp&amp;SERVICE=plain&amp;SET_DEFAULTS.x=41&amp;SET_DEFAULTS.y=5&amp;SHOW_OVERVIEW=on&amp;END_OF_HTTPGET=Yes&amp;SHOW_LINKOUT=yes&amp;QUERY=<%=p.getTrimmedPeptide()%>"><%=PageFlowUtil.buttonImg("Blast")%></a><br>

        <%=p.getScan()%>&nbsp;&nbsp;<%=p.getCharge()%>+&nbsp;&nbsp;<%=p.getRawScore()%>&nbsp;&nbsp;<%=Formats.f3.format(p.getDiffScore())%>&nbsp;&nbsp;<%=Formats.f3.format(p.getZScore())%>&nbsp;&nbsp;<%=Formats.percent.format(p.getIonPercent())%>&nbsp;&nbsp;<%=Formats.f4.format(p.getMass())%>&nbsp;&nbsp;<%=Formats.signf4.format(p.getDeltaMass())%>&nbsp;&nbsp;<%=Formats.f4.format(p.getPeptideProphet())%>&nbsp;&nbsp;<%=p.getProteinHits()%><br>
        <%=p.getProtein()%><br>
<%
        if (fraction.wasloadedFromGzFile())
        {
            out.println("    " + MS2GZFileRenderer.getFileNameInGZFile(fraction.getFileName(), p.getScan(), p.getCharge(), "dta") + "<br>");
        }

        if (null == ctx.previousUrl)
            out.print(PageFlowUtil.buttonImg("<< Prev", "disabled"));
        else
            out.print("    <a href=\"" + ctx.previousUrl.getEncodedLocalURIString() + "\">" + PageFlowUtil.buttonImg("<< Prev") + "</a>");
%>&nbsp;
<%
    if (null == ctx.nextUrl)
        out.print(PageFlowUtil.buttonImg("Next >>", "disabled"));
    else
        out.print("    <a href=\"" + ctx.nextUrl.getEncodedLocalURIString() + "\">" + PageFlowUtil.buttonImg("Next >>") + "</a>");
%>&nbsp;
<%
    if (fraction.wasloadedFromGzFile() && null != ctx.showGzUrl)
    {
        String[] gzFileExtensions = ctx.run.getGZFileExtensions();

        for (String gzFileExtension : gzFileExtensions)
        {
            ctx.showGzUrl.replaceParameter("extension", gzFileExtension);
            out.println("    <a href=\"" + ctx.showGzUrl.getEncodedLocalURIString() + "\">" + PageFlowUtil.buttonImg("Show " + gzFileExtension.toUpperCase()) + "</a>&nbsp;");
        }
    }
    out.print(ctx.modificationHref);
%>
        <% if(null != ctx.pepSearchHref && ctx.pepSearchHref.length() > 0) { %>
        <a href="<%=ctx.pepSearchHref%>" target="pepSearch"><%=PageFlowUtil.buttonImg("Find Features")%></a>
        <% } %>

    </td></tr>

    <tr><td valign=top height="100%">
<%
    ActionURL graphUrl = ctx.url.clone();

    graphUrl.setAction("showGraph");
    graphUrl.deleteParameter("rowIndex");
    graphUrl.addParameter("width", "600");
    graphUrl.addParameter("height", "400");

    if (Double.MIN_VALUE != ctx.form.getxStartDouble()) graphUrl.addParameter("xStart", ctx.form.getStringXStart());
    if (Double.MAX_VALUE != ctx.form.getxEnd()) graphUrl.addParameter("xEnd", ctx.form.getStringXEnd());

    graphUrl.addParameter("tolerance", String.valueOf(ctx.form.getTolerance()));
%>
    <img src="<%=graphUrl.getEncodedLocalURIString()%>" alt="Spectrum Graph">
    <form method=get action="updateShowPeptide.post">
        X&nbsp;Start&nbsp;<input name="xStart" id="xStart" value="<%=ctx.actualXStart%>" size=8>
        X&nbsp;End&nbsp;<input name="xEnd" id="xEnd" value="<%=ctx.actualXEnd%>" size=8>
        <input name="queryString" type="hidden" value="<%=PageFlowUtil.filter(ctx.url.getRawQuery())%>">
        <input type="image" src="<%=PageFlowUtil.buttonSrc("Scale Graph")%>">
    </form>
    </td></tr>
    </table>
</td>
<!--LEFT-->

<!--RIGHT (FRAGMENT)-->
<td valign=top>
<table border=0 cellspacing=1 cellpadding=1 width=230>
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
    Quantitation quant = p.getQuantitation();
    ActionURL elutionGraphUrl = ctx.url.clone();
    String errorMessage = ctx.url.getParameter("elutionProfileError");

    elutionGraphUrl.setAction("showLightElutionGraph");
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
    editUrl.setAction("editElutionGraph");

    if (errorMessage != null)
    { %>
        <font color="red"><%= errorMessage %></font>
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
            <% if (quant.findScanFile() != null && ctx.container.hasPermission(ctx.user, ACL.PERM_UPDATE) && ! "q3".equals(ctx.run.getQuantAnalysisType())) { %><a href="<%= editUrl %>">Edit elution profile selection</a><% } %>
        </td>
    </tr>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Light</strong></td></tr>
                <tr>
                    <td><font size="-1">Scans:</font></td>
                    <td><font size="-1"><%= quant.getLightFirstScan()%> - <%= quant.getLightLastScan()%></font></td>
                </tr>
                <tr>
                    <td><font size="-1"><%= p.getCharge() %>+ Mass:</font></td>
                    <td><font size="-1"><%= quant.getLightMass() %></font></td>
                </tr>
                <tr>
                    <td><font size="-1"><%= p.getCharge() %>+ Area:</font></td>
                    <td><font size="-1"><%= format.format(quant.getLightArea()) %></font></td>
                </tr>
            </table>
        </td>
        <td><img src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Light Elution Graph"/></td>
    </tr>
    <%
        elutionGraphUrl.setAction("showHeavyElutionGraph");
    %>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Heavy</strong></td></tr>
                <tr>
                    <td><font size="-1">Scans:</font></td>
                    <td><font size="-1"><%= quant.getHeavyFirstScan()%> - <%= quant.getHeavyLastScan()%></font></td>
                </tr>
                <tr>
                    <td><font size="-1"><%= p.getCharge() %>+ Mass:</font></td>
                    <td><font size="-1"><%= quant.getHeavyMass() %></font></td>
                </tr>
                <tr>
                    <td><font size="-1"><%= p.getCharge() %>+ Area:</font></td>
                    <td><font size="-1"><%= format.format(quant.getHeavyArea()) %></font></td>
                </tr>
            </table>
        </td>
        <td><img src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Heavy Elution Graph"/></td>
    </tr>
    <%
        elutionGraphUrl.setAction("showCombinedElutionGraph");
    %>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Combined</strong></td></tr>
                <tr>
                    <td><font size="-1"><%= p.getCharge() %>+ Heavy to light ratio:</font></td>
                    <td><font size="-1"><%= quant.getHeavy2LightRatio()%></font></td>
                </tr>
                <tr>
                    <td><font size="-1"><%= p.getCharge() %>+ Light to heavy ratio:</font></td>
                    <td><font size="-1"><%= quant.getRatio()%></font></td>
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