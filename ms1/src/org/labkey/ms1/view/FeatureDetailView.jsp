<%
/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms1.model.Feature" %>
<%@ page import="org.labkey.ms1.model.FeatureDetailsModel" %>
<%@ page import="org.labkey.ms1.model.Peptide" %>
<%@ page import="org.labkey.ms1.MS1Controller" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
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
        if (!filterbox)
            return;

        if ("none" == filterbox.style.display)
        {
            _oldMzLow = <%=model.getMzWindowLow()%>;
            _oldMzHigh = <%=model.getMzWindowHigh()%>;
            filterbox.style.display="";
            _slider.recalculate();

            var scc = document.getElementById("spectrumChartContainer");
            if (scc)
                scc.className = "labkey-frame";
            var bcc = document.getElementById("bubbleChartContainer");
            if (bcc)
                bcc.className = "labkey-frame";

            document.getElementById("sliderMzWindow").focus();
        }
        else
            hideMzFilter();
    }

    function hideMzFilter()
    {
        var filterbox = document.getElementById("mzFilterUI");
        if (filterbox)
            filterbox.style.display = "none";

        var scc = document.getElementById("spectrumChartContainer");
        if (scc)
            scc.className = "";
        var bcc = document.getElementById("bubbleChartContainer");
        if (bcc)
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
        if (null != frm)
            frm.submit();
    }

    function resetMzWindowFilter()
    {
        var txt = document.getElementById("txtMzWindowLow");
        if (null != txt)
            txt.value = "-1.0";
        txt = document.getElementById("txtMzWindowHigh");
        if (null != txt)
            txt.value = "5.0";

        submitMzWindowFilter();
    }

    function showScanFilter(elem)
    {
        var filterbox = document.getElementById("scanFilterUI");
        if (!filterbox)
            return;

        if ("none" == filterbox.style.display)
        {
            _oldScanLow = <%=model.getScanWindowLow()%>;
            _oldScanHigh = <%=model.getScanWindowHigh()%>;

            setElemDisplay("scanFilterUI", "");
            setElemDisplay("scanFilterCol-1", "");
            setElemDisplay("scanFilterCol-2", "");
            setElemDisplay("scanFilterCol-3", "");

            _sliderScan.recalculate();

            setElemClassName("elutionChartContainer", "labkey-frame");
            setElemClassName("bubbleChartContainer", "labkey-frame");
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
        if (null != frm)
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
        if (elem)
            elem.style.display = val;
    }

    function setElemClassName(elemid, val)
    {
        var elem = document.getElementById(elemid);
        if (elem)
            elem.className = val;
    }


</script>
<link type="text/css" rel="StyleSheet" href="<%=h(contextPath)%>/slider/css/rangeslider.css" />
<script type="text/javascript" src="<%=h(contextPath)%>/slider/range.js"></script>
<script type="text/javascript" src="<%=h(contextPath)%>/slider/slidertimer.js"></script>
<script type="text/javascript" src="<%=h(contextPath)%>/slider/rangeslider.js"></script>

<!-- Main View Layout Table -->
<table>
    <tr>
        <!-- Previous/Next Feature buttons -->
        <td align="left">
            <%
                String prevFeatureCaption = "<< Previous Feature";
                String nextFeatureCaption = "Next Feature >>";

                if (model.getPrevFeatureId() < 0)
                    out.write(PageFlowUtil.generateDisabledButton(prevFeatureCaption));
                else
                    out.print(button(prevFeatureCaption).href(model.getPrevFeatureUrl()));

                out.write("&nbsp;");

                if (model.getNextFeatureId() < 0)
                    out.write(PageFlowUtil.generateDisabledButton(nextFeatureCaption));
                else
                    out.print(button(nextFeatureCaption).href(model.getNextFeatureUrl()));
            %>
        </td>

        <td id="scanFilterCol-1" style="display:none">&nbsp;</td>
        
        <td></td>
    </tr>
    <tr>
        <td valign="top">
            <!-- feature data -->
            <table>
                <tr>
                    <td class="labkey-alternate-row">Scan</td>
                    <td><%=feature.getScan()%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Time</td>
                    <td><%=text(model.formatNumber(feature.getTime()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">m/z</td>
                    <td><%=text(model.formatNumber(feature.getMz()))%>
                        &nbsp;<%=textLink("find similar", model.getFindSimilarUrl())%>
                    </td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Accurate</td>
                    <td><%=h(feature.getAccurateMz())%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Mass</td>
                    <td><%=text(model.formatNumber(feature.getMass()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Intensity</td>
                    <td><%=text(model.formatNumber(feature.getIntensity()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Charge</td>
                    <td>+<%=h(feature.getCharge())%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Charge States</td>
                    <td><%=h(feature.getChargeStates())%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">KL</td>
                    <td><%=text(model.formatNumber(feature.getKl()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Background</td>
                    <td><%=text(model.formatNumber(feature.getBackground()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Median</td>
                    <td><%=text(model.formatNumber(feature.getMedian()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Peaks</td>
                    <td><%=h(feature.getPeaks())%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">First Scan</td>
                    <td><%=h(feature.getScanFirst())%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Last Scan</td>
                    <td><%=h(feature.getScanLast())%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Total Intensity</td>
                    <td><%=text(model.formatNumber(feature.getTotalIntensity()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">MS2 Scan</td>
                    <td>
                        <%
                            if (feature.getMs2Scan() != null)
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
                    <td class="labkey-alternate-row">MS2 Charge</td>
                    <td>+<%=h(feature.getMs2Charge())%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">MS2 Probability</td>
                    <td><%=text(model.formatNumber(feature.getMs2ConnectivityProbability()))%></td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Matching Peptide</td>
                    <td>
                        <%
                            {
                                Peptide[] peptides = feature.getMatchingPeptides();
                                Peptide pep = null;
                                for(int idx = 0; idx < peptides.length; ++idx)
                                {
                                    if (idx > 0)
                                        out.print(", ");

                                    pep = peptides[idx];

                                    out.print("<a href=\"");
                                    out.print(model.getPepUrl(pep.getRun(), pep.getRowId(), idx+1, pep.getScan()));
                                    out.print("\" target=\"peptide\">");
                                    out.print(pep.getPeptide());
                                    out.print("</a>");

                                    out.print("&nbsp;" + textLink("features with same", model.getPepSearchUrl(pep.getTrimmedPeptide())));
                                }
                            }
                        %>
                    </td>
                </tr>
                <tr>
                    <td class="labkey-alternate-row">Experiment Run</td>
                    <td>
                        <a href="<%=h(model.getRunDetailsUrl())%>">
                        <%=feature.getExpRun() == null ? "&nbsp;" : h(feature.getExpRun().getName())%>
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
                    out.print(PageFlowUtil.generateDisabledButton(prevScanCaption));
                else
                    out.print(button(prevScanCaption).href(prevScanUrl));

                out.print("&nbsp;");

                if (null == nextScanUrl)
                    out.print(PageFlowUtil.generateDisabledButton(nextScanCaption));
                else
                    out.print(button(nextScanCaption).href(nextScanUrl));

            %>
            <!-- m/z and intensity peaks mass chart -->
            <br/>
            <a href="<%=h(model.getPeaksUrl(true))%>">
            <img width="425" height="300" src="<%=h(model.getChartUrl("spectrum"))%>" alt="Spectrum chart" title="Click to see tabular data"/>
            </a>
            <br/>Intensities of peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);" title="Click to adjust">
            similar m/z as the feature (<%=h(model.getMzWindow())%> <b>[adjust]</b>)</a>,
            for a particular scan.
        </td>
    </tr>

    <tr id="mzFilterUI" style="display:none">

        <td>&nbsp;</td>
        <td id="scanFilterCol-3" style="display:none">&nbsp;</td>

        <td class="labkey-ms1-filter">

            <!-- m/z filter UI -->

            <table>
                <tr>
                    <td colspan="3" style="text-align:center;font-weight:bold">Show Peaks within:</td>
                </tr>
                <tr>
                    <td width="50%" style="font-size:x-small;text-align:right">-50</td>
                    <td>
                        <div class="slider" id="sliderMzWindow" tabindex="1" width="350px"/>
                    </td>
                    <td width="50%" style="font-size:x-small;text-align:left">50</td>
                </tr>
                <tr>
                    <td colspan="3" style="text-align:center">
                        <labkey:form id="frmMzWindowFilter" action="<%=h(buildURL(MS1Controller.ShowFeatureDetailsAction.class))%>" method="GET">
                            <input type="hidden" name="srcUrl" value="<%=h(model.getSrcUrl())%>"/>
                            <input type="hidden" name="featureId" value="<%=feature.getFeatureId()%>"/>
                            <input type="hidden" name="scan" value="<%=model.getScan()%>"/>
                            <input type="hidden" name="scanWindowLow" value="<%=model.getScanWindowLow()%>"/>
                            <input type="hidden" name="scanWindowHigh" value="<%=model.getScanWindowHigh()%>"/>
                            <%=text(model.getQueryFiltersAsInputs())%>
                            <input type="submit" value="Filter" style="display:none;"/>

                            <input type="text" id="txtMzWindowLow" name="mzWindowLow" size="4" onchange="_slider.setValueLow(this.value);" tabindex="2"/>
                            and <input type="text" id="txtMzWindowHigh" name="mzWindowHigh" size="4" onchange="_slider.setValueHigh(this.value);" tabindex="3"/>
                            <br/>of the Feature's m/z value.
                        </labkey:form>
                    </td>
                </tr>
                <tr>
                    <td colspan="3" style="text-align:right">
                        <%= button("Cancel").submit(true).onClick("cancelMzFilter();").attributes("title=\"Cancel Changes\" tabindex=\"4\"")%>
                        <%= button("Reset").submit(true).onClick("resetMzWindowFilter();").attributes("title=\"Reset to Defaults and Refresh\" tabindex=\"5\"")%>
                        <%= button("Filter").submit(true).onClick("submitMzWindowFilter();").attributes("title=\"Set Filter and Refresh\" tabindex=\"6\"")%>
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

            <a href="<%=h(model.getPeaksUrl(-0.02, 0.02, false))%>">
            <img width="425" height="300" src="<%=h(model.getChartUrl("elution"))%>" alt="Elution chart" title="Click to see tabular data"/>
            </a>

            <br/>Intensity of the peaks with the closest m/z value to the feature, across
            <a href="javascript:{}" onclick="showScanFilter();">
                all scans within the feature's range (<%=h(model.getScanWindow())%> scans <b>[adjust]</b>)
            </a>.
        </td>

        <td id="scanFilterUI" class="labkey-ms1-filter" style="display: none;">

            <!-- Scan Filter UI -->

            <table>
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
                        <labkey:form id="frmScanWindowFilter" action="<%=h(buildURL(MS1Controller.ShowFeatureDetailsAction.class))%>" method="GET">
                            <input type="hidden" name="srcUrl" value="<%=h(model.getSrcUrl())%>"/>
                            <input type="hidden" name="featureId" value="<%=feature.getFeatureId()%>"/>
                            <input type="hidden" name="scan" value="<%=model.getScan()%>"/>
                            <input type="hidden" name="mzWindowLow" value="<%=model.getMzWindowLow()%>"/>
                            <input type="hidden" name="mzWindowHigh" value="<%=model.getMzWindowHigh()%>"/>
                            <%=text(model.getQueryFiltersAsInputs())%>
                            <input type="submit" value="Filter" style="display:none;"/>

                            <input type="text" id="txtScanWindowLow" name="scanWindowLow" size="3" onchange="_sliderScan.setValueLow(this.value)" tabindex="102"/>
                            and <input type="text" id="txtScanWindowHigh" name="scanWindowHigh" size="3" onchange="_sliderScan.setValueHigh(this.value)" tabindex="103"/>
                            <br/>
                            of the feature's scan range.
                        </labkey:form>
                    </td>
                </tr>
                <tr>
                    <td nowrap style="text-align:right">
                        <%= button("Cancel").submit(true).onClick("cancelScanFilter();").attributes("title=\"Cancel Changes\" tabindex=\"104\"")%>
                        <%= button("Reset").submit(true).onClick("resetScanWindowFilter();").attributes("title=\"Reset to Defaults and Refresh\" tabindex=\"105\"")%>
                        <%= button("Filter").submit(true).onClick("submitScanWindowFilter();").attributes("title=\"Set Filter and Refresh\" tabindex=\"106\"")%>
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

            <a href="<%=h(model.getPeaksUrl(false))%>">
            <img width="425" height="300" src="<%=h(model.getChartUrl("bubble"))%>"
                 alt="Intesities Bubble chart" title="Click to see tabular data"/>
            </a>

            <br/>Peaks with a
            <a href="javascript:{}" onclick="showMzFilter(this);" title="Click to adjust">
            similar m/z as the feature (<%=h(model.getMzWindow())%> <b>[adjust]</b>)</a>,
            across
            <a href="javascript:{}" onclick="showScanFilter();">
                all scans within the feature's range (<%=h(model.getScanWindow())%> scans <b>[adjust]</b>)
            </a>. The size and color of the bubbles represent the peak's relative intensity.
        </td>
    </tr>
</table>
