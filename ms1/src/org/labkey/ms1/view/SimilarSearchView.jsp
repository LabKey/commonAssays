<%
/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
<%@ page import="org.labkey.ms1.MS1Controller" %>
<%@ page import="org.labkey.ms1.model.SimilarSearchModel" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<SimilarSearchModel> me = (JspView<SimilarSearchModel>) HttpView.currentView();
    SimilarSearchModel model = me.getModelBean();
%>
<script type="text/javascript">
    function onTimeUnitsChange(units)
    {
        var txtTimeSource = document.getElementById("txtTimeSource");
        var lblTime = document.getElementById("lblTime");
        if (null == lblTime || null == txtTimeSource)
            return;

        if (units == "<%=MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans.name()%>")
        {
            lblTime.innerHTML = "<%=model.getTimeUnitsLabel(MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans)%>";
            txtTimeSource.value = "<%=h(model.getFeatureScan())%>";
        }
        else
        {
            lblTime.innerHTML = "<%=model.getTimeUnitsLabel(MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt)%>";
            txtTimeSource.value = "<%=model.formatTimeSource(model.getFeatureTime(), MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt)%>";
        }
    }
</script>
<labkey:form action="<%=model.getResultsUri()%>" method="get">
    <% if (model.getFeatureId() != null) { %>
    <input type="hidden" name="<%=MS1Controller.SimilarSearchForm.ParamNames.featureId.name()%>"
           value="<%=h(model.getFeatureId())%>"/>
    <% } %>
    <table>
        <tr>
            <td>
                <table>
                    <tr>
                        <td>m/z</td>
                        <td>=</td>
                        <td>
                            <input type="text" name="<%=MS1Controller.SimilarSearchForm.ParamNames.mzSource.name()%>"
                                value="<%=model.formatMzSource()%>"/>
                        </td>
                        <td>&#177;<input name="<%=MS1Controller.SimilarSearchForm.ParamNames.mzOffset.name()%>"
                                         value="<%=h(model.getMzOffset())%>" size="7"/>
                            <select name="<%=MS1Controller.SimilarSearchForm.ParamNames.mzUnits.name()%>">
                                <option value="<%=MS1Controller.SimilarSearchForm.MzOffsetUnits.ppm.name()%>"
                                        <%=selected(model.getMzUnits() == MS1Controller.SimilarSearchForm.MzOffsetUnits.ppm)%>
                                        >ppm</option>
                                <option value="<%=MS1Controller.SimilarSearchForm.MzOffsetUnits.mz.name()%>"
                                        <%=selected(model.getMzUnits() == MS1Controller.SimilarSearchForm.MzOffsetUnits.mz)%>
                                        >m/z</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td style="text-align:center"><b>and</b></td>
                        <td colspan="3">&nbsp;</td>
                    </tr>
                    <tr>
                        <td>
                            <span id="lblTime"><%=model.getTimeUnitsLabel()%></span>
                        </td>
                        <td>=</td>
                        <td>
                            <input id="txtTimeSource" type="text" name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeSource.name()%>"
                                value="<%=model.formatTimeSource()%>"/>
                        </td>
                        <td>&#177;<input id="txtTimeOffset" 
                                         name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeOffset.name()%>"
                                         value="<%=h(model.getTimeOffset())%>" size="7"/>
                            <select name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeUnits.name()%>" onchange="onTimeUnitsChange(this.value);">
                                <option value="<%=MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt.name()%>"
                                        <%=selected(model.getTimeUnits() == MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt)%>
                                        >Seconds</option>
                                <option value="<%=MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans.name()%>"
                                        <%=selected(model.getTimeUnits() == MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans)%>
                                        >Scans</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>Search Subfolders</td>
                        <td>:</td>
                        <td><input id="cbxSubfolders" name="<%=MS1Controller.SimilarSearchForm.ParamNames.subfolders.name()%>"
                                   type="checkbox" style="vertical-align:middle"
                                    <%=checked(model.searchSubfolders())%>/>
                        </td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <td colspan="4" style="text-align:right">
                            <%= button("Search").submit(true) %>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>


</labkey:form>
