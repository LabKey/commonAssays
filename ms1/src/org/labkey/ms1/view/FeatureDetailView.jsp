<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.Format" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.ms1.*" %>
<%@ page import="org.labkey.ms1.model.Feature" %>
<%@ page import="org.labkey.ms1.view.FeatureDetailsViewContext" %>
<%@ page import="org.labkey.ms1.model.Peptide" %>
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
    int scanWindowLow = 0;
    int scanWindowHigh = 0;

    String paramVal = me.getViewContext().getRequest().getParameter("mzWindowLow");
    if (null != paramVal && paramVal.length() > 0)
        mzWindowLow = Double.parseDouble(paramVal);

    paramVal = me.getViewContext().getRequest().getParameter("mzWindowHigh");
    if (null != paramVal && paramVal.length() > 0)
        mzWindowHigh = Double.parseDouble(paramVal);

    paramVal = me.getViewContext().getRequest().getParameter("scanWindowLow");
    if (null != paramVal && paramVal.length() > 0)
        scanWindowLow = Integer.parseInt(paramVal);

    paramVal = me.getViewContext().getRequest().getParameter("scanWindowHigh");
    if (null != paramVal && paramVal.length() > 0)
        scanWindowHigh = Integer.parseInt(paramVal);

    //adjust scan so that it is within the scan window
    scan = Math.max(scan, feature.getScanFirst() + scanWindowLow);
    scan = Math.min(scan, feature.getScanLast() + scanWindowHigh);
    
    DecimalFormat fmtDouble = new DecimalFormat("#,##0.0000");
    DecimalFormat fmtPercent = new DecimalFormat("0%");

    ActionURL url = me.getViewContext().getActionURL();
    ActionURL urlPeaksView = url.clone();
    urlPeaksView.setAction("showPeaks.view");
    urlPeaksView.deleteParameters();
    urlPeaksView.addParameter("runId", feature.getRunId());
    urlPeaksView.addParameter("featureId", feature.getFeatureId());
    urlPeaksView.addParameter("scanFirst", feature.getScanFirst() + scanWindowLow);
    urlPeaksView.addParameter("scanLast", feature.getScanLast() + scanWindowHigh);

    ActionURL urlMs2Scan = url.clone();
    urlMs2Scan.deleteParameters();
    urlMs2Scan.setAction("showMS2Peptide");
    urlMs2Scan.addParameter("featureId", feature.getFeatureId());

    String contextPath = request.getContextPath();
    ActionURL findSimilarUrl = MS1Controller.SimilarSearchForm.getDefaultUrl(me.getViewContext().getContainer());
    findSimilarUrl.addParameter(MS1Controller.SimilarSearchForm.ParamNames.featureId.name(), ctx.getFeature().getFeatureId());
%>
<%!
    public String formatNumber(Object number, Format formatter)
    {
        if(null == number)
            return "&nbsp;";
        if(null == formatter)
            return number.toString();
        return formatter.format(number);
    }

    public String formatWindowExtent(Number number, boolean zeroAsNeg)
    {
        if(0 == number.doubleValue() && zeroAsNeg)
            return "-" + number.toString();
        else if(number.doubleValue() >= 0)
            return "+" + number.toString();
        else
            return number.toString();
    }

    public String formatWindowExtent(Number number)
    {
        return formatWindowExtent(number, false);
    }
%>

<!-- Client-side scripts -->
<script type="text/javascript">
    var _oldMzLow = 0;
    var _oldMzHigh = 0;
    var _oldScanLow = 0;
    var _oldScanHigh = 0;

    function showMzFilter(elem)
    {
        var filterbox = document.getElementById("mzFilterUI");
        if(!filterbox)
            return;

        if("none" == filterbox.style.display)
        {
            _oldMzLow = <%=mzWindowLow%>;
            _oldMzHigh = <%=mzWindowHigh%>;
            filterbox.style.display="";
            _slider.recalculate();

            var scc = document.getElementById("spectrumChartContainer");
            if(scc)
                scc.className = "ms-navframe";
            var bcc = document.getElementById("bubbleChartContainer");
            if(bcc)
                bcc.className = "ms-navframe";

            document.getElementById("sliderMzWindow").focus();
        }
        else
            hideMzFilter();
    }

    function hideMzFilter()
    {
        var filterbox = document.getElementById("mzFilterUI");
        if(filterbox)
            filterbox.style.display = "none";

        var scc = document.getElementById("spectrumChartContainer");
        if(scc)
            scc.className = "";
        var bcc = document.getElementById("bubbleChartContainer");
        if(bcc)
            bcc.className = "";
    }

    function cancelMzFilter()
    {
        _slider.setValueLow(_oldMzLow);
        _slider.setValueHigh(_oldMzHigh);
        hideMzFilter();
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
            txt.value = "-1.0";
        txt = document.getElementById("txtMzWindowHigh");
        if(null != txt)
            txt.value = "5.0";

        submitMzWindowFilter();
    }

    function showScanFilter(elem)
    {
        var filterbox = document.getElementById("scanFilterUI");
        if(!filterbox)
            return;

        if("none" == filterbox.style.display)
        {
            _oldScanLow = <%=scanWindowLow%>;
            _oldScanHigh = <%=scanWindowHigh%>;

            setElemDisplay("scanFilterUI", "")
            setElemDisplay("scanFilterCol-1", "");
            setElemDisplay("scanFilterCol-2", "");
            setElemDisplay("scanFilterCol-3", "");

            _sliderScan.recalculate();

            setElemClassName("elutionChartContainer", "ms-navframe");
            setElemClassName("bubbleChartContainer", "ms-navframe");
            document.getElementById("sliderScanWindow").focus();
        }
        else
            hideScanFilter();
    }

    function hideScanFilter()
    {
        setElemDisplay("scanFilterUI", "none");
        setElemDisplay("scanFilterCol-1", "none");
        setElemDisplay("scanFilterCol-2", "none");
        setElemDisplay("scanFilterCol-3", "none");
        setElemClassName("elutionChartContainer", "");
        setElemClassName("bubbleChartContainer", "");
    }

    function cancelScanFilter()
    {
        _sliderScan.setValueLow(_oldScanLow);
        _sliderScan.setValueHigh(_oldScanHigh);
        hideScanFilter();
    }

    function submitScanWindowFilter()
    {
        var frm = document.getElementById("frmScanWindowFilter");
        if(null != frm)
            frm.submit();
    }

    function resetScanWindowFilter()
    {
        _sliderScan.setValueLow(0);
        _sliderScan.setValueHigh(0);
        submitScanWindowFilter();
    }

    function setElemDisplay(elemid, val)
    {
        var elem = document.getElementById(elemid);
        if(elem)
            elem.style.display = val;
    }

    function setElemClassName(elemid, val)
    {
        var elem = document.getElementById(elemid);
        if(elem)
            elem.className = val;
    }


</script>
<link type="text/css" rel="StyleSheet" href="<%=contextPath%>/slider/css/rangeslider.css" />
<script type="text/javascript" src="<%=contextPath%>/slider/range.js"></script>
<script type="text/javascript" src="<%=contextPath%>/slider/slidertimer.js"></script>
<script type="text/javascript" src="<%=contextPath%>/slider/rangeslider.js"></script>

<!-- Main View Layout Table -->

<table cellspacing="0" cellpadding="4px">
    <tr>
        <!-- Previous/Next Feature buttons -->
        <td align="left">
            <%
                String prevFeatureCaption = "<< Previous Feature";
                String nextFeatureCaption = "Next Feature >>";

                //clone the current url and remove anything specific to the feature
                ActionURL urlFeature = url.clone();
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

        <td id="scanFilterCol-1" style="display:none">&nbsp;</td>
        
        <td></td>
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
                    <td><%=formatNumber(feature.getMz(), fmtDouble)%>
                        &nbsp;[<a href="<%=findSimilarUrl%>">find&nbsp;similar</a>] 
                    </td>
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
                    <td>
                        <%
                            if(feature.getMs2Scan() != null)
                            {
                                out.print("<a href=\"" + urlMs2Scan + "\" target=\"peptide\">");
                                out.print(feature.getMs2Scan());
                                out.print("</a>");
                            }
                            else
                                out.print("&nbsp;");
                        %>
                    </td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">MS2 Probability</td>
                    <td><%=formatNumber(feature.getMs2ConnectivityProbability(), fmtPercent)%></td>
                </tr>
                <tr>
                    <td bgcolor="#EEEEEE">Matching Peptides</td>
                    <td>
                        <%
                            {
                                ActionURL urlShowPep = url.clone();
                                urlShowPep.setAction("showPeptide.view");
                                urlShowPep.setPageFlow("MS2");
                                urlShowPep.deleteParameters();

                                Peptide[] peptides = feature.getMatchingPeptides();
                                for(int idx = 0; idx < peptides.length; ++idx)
                                {
                                    if(idx > 0)
                                        out.print(", ");

                                    urlShowPep.deleteParameters();
                                    urlShowPep.addParameter("run", peptides[idx].getRun());
                                    urlShowPep.addParameter("peptideId", String.valueOf(peptides[idx].getRowId()));
                                    urlShowPep.addParameter("rowIndex", idx+1);

                                    out.print("<a href=\"" + urlShowPep + "&MS2Peptides.Scan~eq=" + peptides[idx].getScan() + "\" target=\"peptide\">");
                                    out.print(peptides[idx].getPeptide());
                                    out.print("</a>");

                                    ActionURL urlPepSearch = new ActionURL(MS1Controller.PepSearchAction.class, me.getViewContext().getContainer());
                                    urlPepSearch.addParameter(MS1Controller.PepSearchForm.ParamNames.pepSeq.name(), peptides[idx].getTrimmedPeptide());
                                    out.print("&nbsp;[<a href=\"" + urlPepSearch.getLocalURIString() + "\">");
                                    out.print("features with same</a>]");
                                }
                            }
                        %>
                    </td>
                </tr>
            </table>
        </td>

        <td id="scanFilterCol-2" style="display:none">&nbsp;</td>

        <td valign="top" align="center" id="spectrumChartContainer">

            <!-- Previous/Next Scan buttons -->

            <%
                String prevScanCaption = "<< Previous Scan";
                String nextScanCaption = "Next Scan >>";
                Integer[] prevNextScans = ctx.getPrevNextScans(scan, feature.getMz() + mzWindowLow, feature.getMz() + mzWindowHigh,
                                                                feature.getScanFirst() + scanWindowLow, feature.getScanLast() + scanWindowHigh);

                if (null == prevNextScans || 0 == prevNextScans.length || null == prevNextScans[0])
                    out.print(PageFlowUtil.buttonImg(prevScanCaption, "disabled"));
                else
                {
                    ActionURL urlPrev = url.clone();
                    urlPrev.deleteParameter("scan");
                    urlPrev.addParameter("scan", prevNextScans[0].intValue());
                    out.print("<a href=\"" + urlPrev.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(prevScanCaption) + "</a>");
                }

                out.print("&nbsp;");

                if (null == prevNextScans || 0 == prevNextScans.length || null == prevNextScans[1])
                    out.print(PageFlowUtil.buttonImg(nextScanCaption, "disabled"));
                else
                {
                    ActionURL urlPrev = url.clone();
                    urlPrev.deleteParameter("scan");
                    urlPrev.addParameter("scan", prevNextScans[1].intValue());
                    out.print("<a href=\"" + urlPrev.getLocalURIString() + "\">" + PageFlowUtil.buttonImg(nextScanCaption) + "</a>");
                }

            %>
            <!-- m/z and intensity peaks mass chart -->
            <br/>
            <a href="<%=urlPeaksView.getLocalURIString() + "&query.ScanId/Scan~eq=" + scan + "&query.MZ~gte=" + (feature.getMz()+mzWindowLow) + "&query.MZ~lte=" + (feature.getMz()+mzWindowHigh)%>">
            <img width="425" height="300" src="showChart.view?type=spectrum&featureId=<%=feature.getFeatureId()%>&runId=<%=feature.getRunId()%>&scan=<%=scan%>&mzLow=<%=feature.getMz() + mzWindowLow%>&mzHigh=<%=feature.getMz() + mzWindowHigh%>" alt="Spectrum chart" title="Click to see tabular data"/>
            </a>
            <br/>Intensities of peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);" title="Click to adjust">
            similar m/z as the feature (<%=formatWindowExtent(mzWindowLow,true)%>/<%=formatWindowExtent(mzWindowHigh)%> <b>[adjust]</b>)</a>,
            for a particular scan.
        </td>
    </tr>

    <tr id="mzFilterUI" style="display:none">

        <td>&nbsp;</td>
        <td id="scanFilterCol-3" style="display:none">&nbsp;</td>

        <td class="ms-navframe" style="text-align:center;border-top:1px solid #AAAAAA;border-bottom:1px solid #AAAAAA">

            <!-- m/z filter UI -->

            <table cellspacing="0" cellpadding="4px" width="100%">
                <tr>
                    <td colspan="3" style="text-align:center;font-weight:bold">Show Peaks within:</td>
                </tr>
                <tr>
                    <td width="50%" style="font-size:x-small;text-align:right">-50</td>
                    <td>
                        <div class="slider" id="sliderMzWindow" tabindex="1" style="width:350px"/>
                    </td>
                    <td width="50%" style="font-size:x-small;text-align:left">50</td>
                </tr>
                <tr>
                    <td colspan="3" style="text-align:center">
                        <form id="frmMzWindowFilter" action="showFeatureDetails.view" method="GET">
                            <input type="hidden" name=".lastFilter" value="true"/>
                            <input type="hidden" name="runId" value="<%=feature.getRunId()%>"/>
                            <input type="hidden" name="featureId" value="<%=feature.getFeatureId()%>"/>
                            <input type="hidden" name="scan" value="<%=scan%>"/>
                            <input type="hidden" name="scanWindowLow" value="<%=scanWindowLow%>"/>
                            <input type="hidden" name="scanWindowHigh" value="<%=scanWindowHigh%>"/>
                            <input type="submit" value="Filter" style="display:none;"/>

                            <input type="text" id="txtMzWindowLow" name="mzWindowLow" size="4" onchange="_slider.setValueLow(this.value);" tabindex="2"/>
                            and <input type="text" id="txtMzWindowHigh" name="mzWindowHigh" size="4" onchange="_slider.setValueHigh(this.value);" tabindex="3"/>
                            <br/>of the Feature's m/z value.
                        </form>
                    </td>
                </tr>
                <tr>
                    <td colspan="3" style="text-align:right">
                        <img src="<%=PageFlowUtil.buttonSrc("Cancel")%>" onclick="cancelMzFilter();" alt="Cancel" title="Cancel Chanages" tabindex="4"/>
                        <img src="<%=PageFlowUtil.buttonSrc("Reset")%>" onclick="resetMzWindowFilter();" alt="Reset" title="Reset to Defaults and Refresh" tabindex="5"/>
                        <img src="<%=PageFlowUtil.buttonSrc("Filter")%>" onclick="submitMzWindowFilter();" alt="Filter" title="Set Filter and Refresh" tabindex="6"/>
                    </td>
                </tr>
            </table>

            <script type="text/javascript">
                var _slider = new Slider(document.getElementById("sliderMzWindow"),
                                   document.getElementById("txtMzWindowLow"),
                                   document.getElementById("txtMzWindowHigh"));
                _slider.setMinimum(-50);
                _slider.setMaximum(50);
                _slider.setValueLow(<%=mzWindowLow%>);
                _slider.setValueHigh(<%=mzWindowHigh%>);
                _slider.setPrecision(1);
            </script>

        </td>
    </tr>
    <tr>
        <td  id="elutionChartContainer" valign="top" align="center">

            <!-- retention time and intensity peaks elution chart -->
            <% /*Note that this chart does not use the mzWindow* values since it is supposed to show the closest peak values within a fine tolerance*/ %>

            <a href="<%=urlPeaksView.getLocalURIString() + "&query.MZ~gte=" + (feature.getMz()-0.02) + "&query.MZ~lte=" + (feature.getMz()+0.02)%>">
            <img width="425" heigh="300" src="showChart.view?type=elution&featureId=<%=feature.getFeatureId()%>&runId=<%=feature.getRunId()%>&scanFirst=<%=feature.getScanFirst() + scanWindowLow%>&scanLast=<%=feature.getScanLast() + scanWindowHigh%>&mzLow=<%=feature.getMz()-0.02%>&mzHigh=<%=feature.getMz()+0.02%>" alt="Elution chart" title="Click to see tabular data"/>
            </a>

            <br/>Intensity of the peaks with the closest m/z value to the feature, across
            <a href="javascript:{}" onclick="showScanFilter();">
                all scans within the feature's range (<%=formatWindowExtent(scanWindowLow, true)%>/<%=formatWindowExtent(scanWindowHigh)%> scans <b>[adjust]</b>)
            </a>.
        </td>

        <td id="scanFilterUI" class="ms-navframe" style="display:none;text-align:center;vertical-align:top">

            <!-- Scan Filter UI -->

            <table cellspacing="0" cellpadding="2px">
                <tr>
                    <td style="text-align:center"><span style="font-weight:bold">Show Peaks in Scans Within:</span></td>
                </tr>
                <tr>
                    <td style="font-size:x-small;text-align:center">+100</td>
                </tr>
                <tr>
                    <td style="text-align:center">
                        <div class="slider" id="sliderScanWindow" tabindex="101" style="height:255px;width:100%"/>
                    </td>
                </tr>
                <tr>
                    <td style="font-size:x-small;text-align:center">-100</td>
                </tr>
                <tr>
                    <td style="text-align:center">
                        <form id="frmScanWindowFilter" action="showFeatureDetails.view" method="GET">
                            <input type="hidden" name=".lastFilter" value="true"/>
                            <input type="hidden" name="runId" value="<%=feature.getRunId()%>"/>
                            <input type="hidden" name="featureId" value="<%=feature.getFeatureId()%>"/>
                            <input type="hidden" name="scan" value="<%=scan%>"/>
                            <input type="hidden" name="mzWindowLow" value="<%=mzWindowLow%>"/>
                            <input type="hidden" name="mzWindowHigh" value="<%=mzWindowHigh%>"/>
                            <input type="submit" value="Filter" style="display:none;"/>

                            <input type="text" id="txtScanWindowLow" name="scanWindowLow" size="3" onchange="_sliderScan.setValueLow(this.value)" tabindex="102"/>
                            and <input type="text" id="txtScanWindowHigh" name="scanWindowHigh" size="3" onchange="_sliderScan.setValueHigh(this.value)" tabindex="103"/>
                            <br/>
                            of the feature's scan range.
                        </form>
                    </td>
                </tr>
                <tr>
                    <td nowrap style="text-align:right">
                        <img src="<%=PageFlowUtil.buttonSrc("Cancel")%>" onclick="cancelScanFilter();" alt="Cancel" title="Cancel Chanages" tabindex="104"/>
                        <img src="<%=PageFlowUtil.buttonSrc("Reset")%>" onclick="resetScanWindowFilter();" alt="Reset" title="Reset to Defaults and Refresh" tabindex="105"/>
                        <img src="<%=PageFlowUtil.buttonSrc("Filter")%>" onclick="submitScanWindowFilter();" alt="Filter" title="Set Filter and Refresh" tabindex="106"/>
                    </td>
                </tr>
            </table>

            <script type="text/javascript">
                var _sliderScan = new Slider(document.getElementById("sliderScanWindow"),
                                   document.getElementById("txtScanWindowLow"),
                                   document.getElementById("txtScanWindowHigh"),
                                    "vertical");
                _sliderScan.setMinimum(-100);
                _sliderScan.setMaximum(100);
                _sliderScan.setValueLow(<%=scanWindowLow%>);
                _sliderScan.setValueHigh(<%=scanWindowHigh%>);
            </script>

        </td>
        <td valign="top" align="center" id="bubbleChartContainer">

            <!-- retention time and m/z bubble chart -->

            <a href="<%=urlPeaksView.getLocalURIString() + "&query.MZ~gte=" + (feature.getMz()+mzWindowLow) + "&query.MZ~lte=" + (feature.getMz()+mzWindowHigh)%>">
            <img width="425" height="300" src="showChart.view?type=bubble&featureId=<%=feature.getFeatureId()%>&runId=<%=feature.getRunId()%>&scanFirst=<%=feature.getScanFirst() + scanWindowLow%>&scanLast=<%=feature.getScanLast() + scanWindowHigh%>&mzLow=<%=feature.getMz() + mzWindowLow%>&mzHigh=<%=feature.getMz() + mzWindowHigh%>&scan=<%=scan%>"
                 alt="Intesities Bubble chart" title="Click to see tabular data"/>
            </a>

            <br/>Peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);" title="Click to adjust">
            similar m/z as the feature (<%=formatWindowExtent(mzWindowLow,true)%>/<%=formatWindowExtent(mzWindowHigh)%> <b>[adjust]</b>)</a>,
            across
            <a href="javascript:{}" onclick="showScanFilter();">
                all scans within the feature's range (<%=formatWindowExtent(scanWindowLow, true)%>/<%=formatWindowExtent(scanWindowHigh)%> scans <b>[adjust]</b>)
            </a>. The size and color of the bubbles represent the peak's relative intensity.
        </td>
    </tr>
</table>