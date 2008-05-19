<%
/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.Format" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.ms1.*" %>
<%@ page import="org.labkey.ms1.model.Feature" %>
<%@ page import="org.labkey.ms1.model.FeatureDetailsModel" %>
<%@ page import="org.labkey.ms1.model.Peptide" %>
<%
    JspView<FeatureDetailsModel> me = (JspView<FeatureDetailsModel>) HttpView.currentView();
    FeatureDetailsModel model = me.getModelBean();
    Feature feature = model.getFeature();
    me.setTitle("Feature Details");

    String contextPath = request.getContextPath();
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
            _oldMzLow = <%=model.getMzWindowLow()%>;
            _oldMzHigh = <%=model.getMzWindowHigh()%>;
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
            _oldScanLow = <%=model.getScanWindowLow()%>;
            _oldScanHigh = <%=model.getScanWindowHigh()%>;

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
<style type="text/css">
    td.caption
    {
        background-color: #EEEEEE;
    }
</style>
<table cellspacing="0" cellpadding="4px">
    <tr>
        <!-- Previous/Next Feature buttons -->
        <td align="left">
            <%
                String prevFeatureCaption = "<< Previous Feature";
                String nextFeatureCaption = "Next Feature >>";

                if(model.getPrevFeatureId() < 0)
                    out.write(PageFlowUtil.buttonImg(prevFeatureCaption, "disabled"));
                else
                    out.print("<a href=\"" + model.getPrevFeatureUrl() + "\">" + PageFlowUtil.buttonImg(prevFeatureCaption) + "</a>");

                out.write("&nbsp;");

                if(model.getNextFeatureId() < 0)
                    out.write(PageFlowUtil.buttonImg(nextFeatureCaption, "disabled"));
                else
                    out.print("<a href=\"" + model.getNextFeatureUrl() + "\">" + PageFlowUtil.buttonImg(nextFeatureCaption) + "</a>");
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
                    <td class="caption">Scan</td>
                    <td><%=feature.getScan()%></td>
                </tr>
                <tr>
                    <td class="caption">Time</td>
                    <td><%=model.formatNumber(feature.getTime())%></td>
                </tr>
                <tr>
                    <td class="caption">m/z</td>
                    <td><%=model.formatNumber(feature.getMz())%>
                        &nbsp;[<a href="<%=model.getFindSimilarUrl()%>">find&nbsp;similar</a>]
                    </td>
                </tr>
                <tr>
                    <td class="caption">Accurate</td>
                    <td><%=PageFlowUtil.filter(feature.getAccurateMz())%></td>
                </tr>
                <tr>
                    <td class="caption">Mass</td>
                    <td><%=model.formatNumber(feature.getMass())%></td>
                </tr>
                <tr>
                    <td class="caption">Intensity</td>
                    <td><%=model.formatNumber(feature.getIntensity())%></td>
                </tr>
                <tr>
                    <td class="caption">Charge</td>
                    <td>+<%=PageFlowUtil.filter(feature.getCharge())%></td>
                </tr>
                <tr>
                    <td class="caption">Charge States</td>
                    <td><%=PageFlowUtil.filter(feature.getChargeStates())%></td>
                </tr>
                <tr>
                    <td class="caption">KL</td>
                    <td><%=model.formatNumber(feature.getKl())%></td>
                </tr>
                <tr>
                    <td class="caption">Background</td>
                    <td><%=model.formatNumber(feature.getBackground())%></td>
                </tr>
                <tr>
                    <td class="caption">Median</td>
                    <td><%=model.formatNumber(feature.getMedian())%></td>
                </tr>
                <tr>
                    <td class="caption">Peaks</td>
                    <td><%=PageFlowUtil.filter(feature.getPeaks())%></td>
                </tr>
                <tr>
                    <td class="caption">First Scan</td>
                    <td><%=PageFlowUtil.filter(feature.getScanFirst())%></td>
                </tr>
                <tr>
                    <td class="caption">Last Scan</td>
                    <td><%=PageFlowUtil.filter(feature.getScanLast())%></td>
                </tr>
                <tr>
                    <td class="caption">Total Intensity</td>
                    <td><%=model.formatNumber(feature.getTotalIntensity())%></td>
                </tr>
                <tr>
                    <td class="caption">MS2 Scan</td>
                    <td>
                        <%
                            if(feature.getMs2Scan() != null)
                            {
                                out.print("<a href=\"" + model.getPepUrl() + "\" target=\"peptide\">");
                                out.print(feature.getMs2Scan());
                                out.print("</a>");
                            }
                            else
                                out.print("&nbsp;");
                        %>
                    </td>
                </tr>
                <tr>
                    <td class="caption">MS2 Charge</td>
                    <td>+<%=PageFlowUtil.filter(feature.getMs2Charge())%></td>
                </tr>
                <tr>
                    <td class="caption">MS2 Probability</td>
                    <td><%=model.formatNumber(feature.getMs2ConnectivityProbability())%></td>
                </tr>
                <tr>
                    <td class="caption">Matching Peptide</td>
                    <td>
                        <%
                            {
                                Peptide[] peptides = feature.getMatchingPeptides();
                                Peptide pep = null;
                                for(int idx = 0; idx < peptides.length; ++idx)
                                {
                                    if(idx > 0)
                                        out.print(", ");

                                    pep = peptides[idx];

                                    out.print("<a href=\"");
                                    out.print(model.getPepUrl(pep.getRun(), pep.getRowId(), idx+1, pep.getScan()));
                                    out.print("\" target=\"peptide\">");
                                    out.print(pep.getPeptide());
                                    out.print("</a>");

                                    out.print("&nbsp;[<a href=\"" + model.getPepSearchUrl(pep.getTrimmedPeptide()) + "\">");
                                    out.print("features with same</a>]");
                                }
                            }
                        %>
                    </td>
                </tr>
                <tr>
                    <td class="caption">Experiment Run</td>
                    <td>
                        <a href="<%=model.getRunDetailsUrl()%>">
                        <%=feature.getExpRun() == null ? "&nbsp;" : PageFlowUtil.filter(feature.getExpRun().getName())%>
                        </a>
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
                String prevScanUrl = model.getPrevScanUrl();
                String nextScanUrl = model.getNextScanUrl();

                if (null == prevScanUrl)
                    out.print(PageFlowUtil.buttonImg(prevScanCaption, "disabled"));
                else
                    out.print("<a href=\"" + prevScanUrl + "\">" + PageFlowUtil.buttonImg(prevScanCaption) + "</a>");

                out.print("&nbsp;");

                if (null == nextScanUrl)
                    out.print(PageFlowUtil.buttonImg(nextScanCaption, "disabled"));
                else
                    out.print("<a href=\"" + nextScanUrl + "\">" + PageFlowUtil.buttonImg(nextScanCaption) + "</a>");

            %>
            <!-- m/z and intensity peaks mass chart -->
            <br/>
            <a href="<%=model.getPeaksUrl(true)%>">
            <img width="425" height="300" src="<%=model.getChartUrl("spectrum")%>" alt="Spectrum chart" title="Click to see tabular data"/>
            </a>
            <br/>Intensities of peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);" title="Click to adjust">
            similar m/z as the feature (<%=model.getMzWindow()%> <b>[adjust]</b>)</a>,
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
                            <input type="hidden" name="srcUrl" value="<%=model.getSrcUrl()%>"/>
                            <input type="hidden" name="featureId" value="<%=feature.getFeatureId()%>"/>
                            <input type="hidden" name="scan" value="<%=model.getScan()%>"/>
                            <input type="hidden" name="scanWindowLow" value="<%=model.getScanWindowLow()%>"/>
                            <input type="hidden" name="scanWindowHigh" value="<%=model.getScanWindowHigh()%>"/>
                            <%=model.getQueryFiltersAsInputs()%>
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
                _slider.setValueLow(<%=model.getMzWindowLow()%>);
                _slider.setValueHigh(<%=model.getMzWindowHigh()%>);
                _slider.setPrecision(1);
            </script>

        </td>
    </tr>
    <tr>
        <td  id="elutionChartContainer" valign="top" align="center">

            <!-- retention time and intensity peaks elution chart -->
            <% /*Note that this chart does not use the mzWindow* values since it is supposed to show the closest peak values within a fine tolerance*/ %>

            <a href="<%=model.getPeaksUrl(-0.02, 0.02, false)%>">
            <img width="425" height="300" src="<%=model.getChartUrl("elution")%>" alt="Elution chart" title="Click to see tabular data"/>
            </a>

            <br/>Intensity of the peaks with the closest m/z value to the feature, across
            <a href="javascript:{}" onclick="showScanFilter();">
                all scans within the feature's range (<%=model.getScanWindow()%> scans <b>[adjust]</b>)
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
                            <input type="hidden" name="srcUrl" value="<%=model.getSrcUrl()%>"/>
                            <input type="hidden" name="featureId" value="<%=feature.getFeatureId()%>"/>
                            <input type="hidden" name="scan" value="<%=model.getScan()%>"/>
                            <input type="hidden" name="mzWindowLow" value="<%=model.getMzWindowLow()%>"/>
                            <input type="hidden" name="mzWindowHigh" value="<%=model.getMzWindowHigh()%>"/>
                            <%=model.getQueryFiltersAsInputs()%>
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
                _sliderScan.setValueLow(<%=model.getScanWindowLow()%>);
                _sliderScan.setValueHigh(<%=model.getScanWindowHigh()%>);
            </script>

        </td>
        <td valign="top" align="center" id="bubbleChartContainer">

            <!-- retention time and m/z bubble chart -->

            <a href="<%=model.getPeaksUrl(false)%>">
            <img width="425" height="300" src="<%=model.getChartUrl("bubble")%>"
                 alt="Intesities Bubble chart" title="Click to see tabular data"/>
            </a>

            <br/>Peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);" title="Click to adjust">
            similar m/z as the feature (<%=model.getMzWindow()%> <b>[adjust]</b>)</a>,
            across
            <a href="javascript:{}" onclick="showScanFilter();">
                all scans within the feature's range (<%=model.getScanWindow()%> scans <b>[adjust]</b>)
            </a>. The size and color of the bubbles represent the peak's relative intensity.
        </td>
    </tr>
</table>