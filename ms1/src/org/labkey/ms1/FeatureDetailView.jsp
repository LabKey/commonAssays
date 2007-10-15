<%@ page import="org.labkey.ms1.Feature" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.Format" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%
    JspView<Feature> me = (JspView<Feature>) HttpView.currentView();
    Feature feature = me.getModelBean();
    me.setTitle("Feature Details");

    int scan = feature.getScan().intValue();
    String scanParam = me.getViewContext().getRequest().getParameter("scan");
    if (null != scanParam && scanParam.length() > 0)
        scan = Integer.parseInt(scanParam);

    DecimalFormat fmtDouble = new DecimalFormat("#,##0.0000");
    DecimalFormat fmtPercent = new DecimalFormat("0%");

    ViewURLHelper url = me.getViewContext().getViewURLHelper();
%>
<%!
    public String formatNumber(Object number, Format formatter)
    {
        if(null == number)
            return "";
        if(null == formatter)
            return number.toString();
        return formatter.format(number);
    }
%>

<table cellspacing="0" cellpadding="4px">
    <tr>
        <td valign="top">
            <!-- feature data -->
            <table cellspacing="0" cellpadding="0">
                <tr>
                    <td bgcolor="#EEEEEE">Scan</td>
                    <td><%=feature.getScan()%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Time</td>
                    <td><%=formatNumber(feature.getTime(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">m/z</td>
                    <td><%=formatNumber(feature.getMz(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Accurate</td>
                    <td><%=feature.getAccurateMz()%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Mass</td>
                    <td><%=formatNumber(feature.getMass(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Intensity</td>
                    <td><%=formatNumber(feature.getIntensity(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Charge</td>
                    <td><%=feature.getCharge()%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Charge States</td>
                    <td><%=feature.getChargeStates()%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">KL</td>
                    <td><%=formatNumber(feature.getKl(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Background</td>
                    <td><%=formatNumber(feature.getBackground(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Median</td>
                    <td><%=formatNumber(feature.getMedian(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Peaks</td>
                    <td><%=feature.getPeaks()%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">First Scan</td>
                    <td><%=feature.getScanFirst()%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Last Scan</td>
                    <td><%=feature.getScanLast()%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Total Intensity</td>
                    <td><%=formatNumber(feature.getTotalIntensity(), fmtDouble)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">MS2 Scan</td>
                    <td><%=formatNumber(feature.getMs2Scan(), null)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">MS2 Probability</td>
                    <td><%=formatNumber(feature.getMs2ConnectivityProbability(), fmtPercent)%></td>
                </tr>
            </table>
        </td>
        <td align="center">
            <%
                String prevScanCaption = "<< Previous Scan";
                String nextScanCaption = "Next Scan >>";

                if(scan <= feature.getScanFirst())
                    out.print(PageFlowUtil.buttonImg(prevScanCaption,"disabled"));
                else
                {
                    ViewURLHelper urlPrev = url.clone();
                    urlPrev.deleteParameter("scan");
                    urlPrev.addParameter("scan", scan - 1);
                    out.print("<a href=\"" + urlPrev.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(prevScanCaption) + "</a>");
                }

                out.print("&nbsp;");

                if(scan >= feature.getScanLast())
                    out.print(PageFlowUtil.buttonImg(nextScanCaption,"disabled"));
                else
                {
                    ViewURLHelper urlPrev = url.clone();
                    urlPrev.deleteParameter("scan");
                    urlPrev.addParameter("scan", scan + 1);
                    out.print("<a href=\"" + urlPrev.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(nextScanCaption) + "</a>");
                }

            %>
            <!-- m/z and intensity peaks mass chart -->
            <br/>
            <img src="showChart.view?type=spectrum&featureId=<%=feature.getFeatureId()%>&runId=<%=feature.getRunId()%>&scan=<%=scan%>&mzLow=<%=feature.getMz()-1%>&mzHigh=<%=feature.getMz()+5%>" alt="Spectrum chart"/>
        </td>
    </tr>
    <tr>
        <td>
            <!-- retention time and intensity peaks elution chart -->
            Chart goes here
        </td>
        <td>
            <!-- retention time and m/z bubble chart -->
            Chart goes here
        </td>
    </tr>
</table>