<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.Format" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.ms1.*" %>
<%
    JspView<FeatureDetailsViewContext> me = (JspView<FeatureDetailsViewContext>) HttpView.currentView();
    FeatureDetailsViewContext ctx = me.getModelBean();
    Feature feature = ctx.getFeature();
    me.setTitle("Feature Details");

    int scan = feature.getScan().intValue();
    String scanParam = me.getViewContext().getRequest().getParameter("scan");
    if (null != scanParam && scanParam.length() > 0)
        scan = Integer.parseInt(scanParam);

    double mzWindowLow = -1;
    double mzWindowHigh = 5;

    String paramVal = me.getViewContext().getRequest().getParameter("mzWindowLow");
    if (null != paramVal && paramVal.length() > 0)
        mzWindowLow = -(Math.abs(Double.parseDouble(paramVal))); //to ensure the value is negative

    paramVal = me.getViewContext().getRequest().getParameter("mzWindowHigh");
    if (null != paramVal && paramVal.length() > 0)
        mzWindowHigh = Math.abs(Double.parseDouble(paramVal)); //to ensure the value is positive

    DecimalFormat fmtDouble = new DecimalFormat("#,##0.0000");
    DecimalFormat fmtPercent = new DecimalFormat("0%");

    ViewURLHelper url = me.getViewContext().getViewURLHelper();
    ViewURLHelper urlPeaksView = url.clone();
    urlPeaksView.setAction("showPeaks.view");
    urlPeaksView.deleteParameters();
    urlPeaksView.addParameter("runId", feature.getRunId());
    urlPeaksView.addParameter("featureId", feature.getFeatureId());

    String contextPath = request.getContextPath();
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
<script type="text/javascript">
    function showMzFilter(elem)
    {
        var posLeft = 0;
        var posTop = 0;
        var offsetElem = elem;

        var filterbox = document.getElementById("boxMzWindowFilter");
        if(null == filterbox)
            return;

        while (offsetElem.tagName != "BODY")
        {
            posLeft += offsetElem.offsetLeft;
            posTop += offsetElem.offsetTop;
            offsetElem = offsetElem.offsetParent;
        }

        posTop += elem.offsetHeight;

        filterbox.style.top = posTop;
        filterbox.style.left = posLeft;

        var viewportWidth = YAHOO.util.Dom.getViewportWidth();
        var leftScroll = YAHOO.util.DragDropMgr.getScrollLeft();
        if (viewportWidth + leftScroll < filterbox.offsetWidth + posLeft)
        {
            posLeft = viewportWidth + leftScroll - filterbox.offsetWidth - 10;
            filterbox.style.left = posLeft;
        }

        filterbox.style.display = "block";

        var txt = document.getElementById("txtMzWindowLow");
        if(null != txt)
        {
            txt.focus();
            txt.select();
        }
    }

    function hideMzFilter()
    {
        var filterbox = document.getElementById("boxMzWindowFilter");
        if(null == filterbox)
            return;
        filterbox.style.display = "none";
    }

    function submitMzWindowFilter()
    {
        var frm = document.getElementById("frmMzWindowFilter");
        if(null != frm)
            frm.submit();
    }

    function resetMzWindowFilter()
    {
        var txt = document.getElementById("txtMzWindowLow");
        if(null != txt)
            txt.value = "1.0";
        txt = document.getElementById("txtMzWindowHigh");
        if(null != txt)
            txt.value = "5.0";

        submitMzWindowFilter();
    }
</script>

<div id="boxMzWindowFilter" style="background-color:#F9F9F9;border:1px solid #AAAAAA;position:absolute;display:none;">
    <form id="frmMzWindowFilter" action="showFeatureDetails.view" method="GET">
    <input type="hidden" name="runId" value="<%=feature.getRunId()%>"/>
    <input type="hidden" name="featureId" value="<%=feature.getFeatureId()%>"/>
    <input type="hidden" name="scan" value="<%=scan%>"/>
    <input type="submit" value="Filter" style="display:none;"/>
    <table cellspacing="0" cellpadding="2px" border="0" class="wp">
        <tr class="wpHeader">
            <th class="wpTitle" style="border-right:0px none;">Adjust m/z Window</th>
            <td class="wpTitle" style="border-left:0px none;text-align:right" title="Close">
                <img border="0" onclick="hideMzFilter()" src="<%=contextPath%>/_images/partdelete.gif" alt="close"/>
            </td>
        </tr>
        <tr>
            <td colspan="2" style="text-align:center">
                Show peaks with an m/z value within
            </td>
        </tr>
        <tr>
            <td colspan="2" style="text-align:center">
                <b>-</b><input id="txtMzWindowLow" type="text" name="mzWindowLow" value="<%=Math.abs(mzWindowLow)%>" size="4"/>
                and <b>+</b>
                <input type="text" id="txtMzWindowHigh" name="mzWindowHigh" value="<%=mzWindowHigh%>" size="4"/>
            </td>
        </tr>
        <tr>
            <td colspan="2" style="text-align:center">
                of the feature's m/z value.
            </td>
        </tr>
        <tr>
            <td colspan="2" style="text-align:right;">
                <img src="<%=PageFlowUtil.buttonSrc("Reset")%>" onclick="resetMzWindowFilter();" alt="Reset"/>
                <img src="<%=PageFlowUtil.buttonSrc("Filter")%>" onclick="submitMzWindowFilter();" alt="Filter"/>
            </td>
        </tr>
    </table>
    </form>
</div>

<table cellspacing="0" cellpadding="4px">
    <tr>
        <td colspan="2" align="left">
            <%
                String prevFeatureCaption = "<< Previous Feature";
                String nextFeatureCaption = "Next Feature >>";

                //clone the current url and remove anything specific to the feature
                ViewURLHelper urlFeature = url.clone();
                urlFeature.deleteParameter("scan");

                if(ctx.getPrevFeatureId() < 0)
                    out.write(PageFlowUtil.buttonImg(prevFeatureCaption, "disabled"));
                else
                {
                    urlFeature.replaceParameter("featureId", String.valueOf(ctx.getPrevFeatureId()));
                    out.print("<a href=\"" + urlFeature.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(prevFeatureCaption) + "</a>");
                }

                out.write("&nbsp;");

                if(ctx.getNextFeatureId() < 0)
                    out.write(PageFlowUtil.buttonImg(nextFeatureCaption, "disabled"));
                else
                {
                    urlFeature.replaceParameter("featureId", String.valueOf(ctx.getNextFeatureId()));
                    out.print("<a href=\"" + urlFeature.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(nextFeatureCaption) + "</a>");
                }
            %>
        </td>
    </tr>
    <tr>
        <td valign="top">
            <!-- feature data -->
            <table cellspacing="0" cellpadding="2px">
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
        <td valign="top" align="center">
            <%
                String prevScanCaption = "<< Previous Scan";
                String nextScanCaption = "Next Scan >>";
                Integer[] prevNextScans = ctx.getPrevNextScans(scan, feature.getMz() + mzWindowLow, feature.getMz() + mzWindowHigh);

                if (null == prevNextScans || 0 == prevNextScans.length || null == prevNextScans[0])
                    out.print(PageFlowUtil.buttonImg(prevScanCaption, "disabled"));
                else
                {
                    ViewURLHelper urlPrev = url.clone();
                    urlPrev.deleteParameter("scan");
                    urlPrev.addParameter("scan", prevNextScans[0].intValue());
                    out.print("<a href=\"" + urlPrev.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(prevScanCaption) + "</a>");
                }

                out.print("&nbsp;");

                if (null == prevNextScans || 0 == prevNextScans.length || null == prevNextScans[1])
                    out.print(PageFlowUtil.buttonImg(nextScanCaption, "disabled"));
                else
                {
                    ViewURLHelper urlPrev = url.clone();
                    urlPrev.deleteParameter("scan");
                    urlPrev.addParameter("scan", prevNextScans[1].intValue());
                    out.print("<a href=\"" + urlPrev.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(nextScanCaption) + "</a>");
                }

            %>
            <!-- m/z and intensity peaks mass chart -->
            <br/>
            <a href="<%=urlPeaksView.getLocalURIString() + "&query.ScanId/Scan~eq=" + scan + "&query.MZ~gte=" + (feature.getMz()+mzWindowLow) + "&query.MZ~lte=" + (feature.getMz()+mzWindowHigh)%>">
            <img src="showChart.view?type=spectrum&featureId=<%=feature.getFeatureId()%>&runId=<%=feature.getRunId()%>&scan=<%=scan%>&mzLow=<%=feature.getMz() + mzWindowLow%>&mzHigh=<%=feature.getMz() + mzWindowHigh%>" alt="Spectrum chart"/>
            </a>
            <br/><i>Intensities of peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);">
            similar m/z as the feature (-<%=Math.abs(mzWindowLow)%> to +<%=mzWindowHigh%>)</a>,
            for a particular scan.</i>
        </td>
    </tr>
    <tr>
        <td valign="top" align="center">
            <!-- retention time and intensity peaks elution chart -->
            <% /*Note that this chart does not use the mzWindow* values since it is supposed to show the closest peak values within a fine tolerance*/ %>
            <a href="<%=urlPeaksView.getLocalURIString() + "&query.MZ~gte=" + (feature.getMz()-0.02) + "&query.MZ~lte=" + (feature.getMz()+0.02)%>">
            <img src="showChart.view?type=elution&featureId=<%=feature.getFeatureId()%>&runId=<%=feature.getRunId()%>&scanFirst=<%=feature.getScanFirst()%>&scanLast=<%=feature.getScanLast()%>&mzLow=<%=feature.getMz()-0.02%>&mzHigh=<%=feature.getMz()+0.02%>" alt="Elution chart"/>
            </a>
            <br/><i>Intensity of the peaks with the closest m/z value to the feature, across all scans within the feature's range.</i>
        </td>
        <td valign="top" align="center">
            <!-- retention time and m/z bubble chart -->
            <a href="<%=urlPeaksView.getLocalURIString() + "&query.MZ~gte=" + (feature.getMz()+mzWindowLow) + "&query.MZ~lte=" + (feature.getMz()+mzWindowHigh)%>">
            <img src="showChart.view?type=bubble&featureId=<%=feature.getFeatureId()%>&runId=<%=feature.getRunId()%>&scanFirst=<%=feature.getScanFirst()%>&scanLast=<%=feature.getScanLast()%>&mzLow=<%=feature.getMz() + mzWindowLow%>&mzHigh=<%=feature.getMz() + mzWindowHigh%>" alt="Intesities Bubble chart"/>
            </a>

            <br/><i>Peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);">
            similar m/z as the feature (-<%=Math.abs(mzWindowLow)%> to +<%=mzWindowHigh%>)</a>,
            across all scans within the feature's range. The size of each bubble represents the peak's intensity.</i>
        </td>
    </tr>
</table>