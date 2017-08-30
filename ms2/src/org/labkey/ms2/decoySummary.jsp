<%
/*
 * Copyright (c) 2004-2017 Fred Hutchinson Cancer Research Center
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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Manager" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.List" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<MS2Manager.DecoySummaryBean> me = ((JspView<MS2Manager.DecoySummaryBean>)HttpView.currentView());
    MS2Manager.DecoySummaryBean bean = me.getModelBean();

    NumberFormat defaultFormat = NumberFormat.getPercentInstance();
    defaultFormat.setMinimumFractionDigits(2);
    String fdr = defaultFormat.format(bean.getFdr());
    NumberFormat pValFormat = NumberFormat.getInstance();
    pValFormat.setMaximumFractionDigits(3);
    String pVal = pValFormat.format(bean.getpValue());
    NumberFormat nf = NumberFormat.getNumberInstance();
    String targetCount = nf.format(bean.getTargetCount());
    String decoyCount = nf.format(bean.getDecoyCount());
    Float desiredFdr = bean.getDesiredFdr();
    Boolean isIonCutoff;  // we don't ever need to retrieve value from bean on page

    ActionURL newURL = getViewContext().cloneActionURL();
    String ionCutoff = newURL.getParameter("MS2Peptides.Ion~gte");
    if (null != ionCutoff)
    {
        float ionCutoffFloat = Float.parseFloat(ionCutoff);
        if(ionCutoffFloat == bean.getScoreThreshold())  // filter is same as threshold, so check checkbox
            isIonCutoff = true;
        else
            isIonCutoff = false;
    }
    else
    {
        isIonCutoff = false;
    }
    String grouping = newURL.getParameter("grouping");
    boolean isStandardView = false;
    if(null == grouping)
    {
        isStandardView = true;  // no parameter = standard
    }
    else
    {
        if(grouping.equals("query"))  // standard view
            isStandardView = true;
        else  // not standard, so disable checkbox (which would not work anyway)
            isStandardView = false;
    }

    newURL.deleteParameter("MS2Peptides.Ion~gte");
    newURL.deleteParameter("desiredFdr");
    newURL.deleteParameter("isIonCutoff");
%>
<% if (null != desiredFdr && Float.compare(bean.getFdr(), desiredFdr) > 0)
{ %>
<span>No score threshold with FDR below desired value. Showing best FDR over desired value.</span><br/><br/>
<%}%>
<% if (null == desiredFdr && Float.compare(bean.getFdrAtDefaultPvalue(), 1f) > 0)
{ %>
<span>No scores were above threshold for standard p-value. FDR is 100%.</span><br/><br/>
<%}%>
<labkey:form method="GET" action="<%=newURL%>" name="decoySummary" id="decoySummaryForm">
    <% for (Pair<String, String> param : newURL.getParameters())
    { %>
    <input type="hidden" name="<%=h(param.getKey())%>" value="<%=h(param.getValue())%>"/>
    <% } %>

    <table class="lk-fields-table">
        <tr>
            <td class="labkey-form-label">P Value</td>
            <td style="text-align:right;"><%=h(pVal)%></td>
            <td style="padding-left: 1em"></td>
            <td class="labkey-form-label">Ion Threshold</td>
            <td style="text-align:right" id="ionThresholdValue"><%=h(bean.getScoreThreshold())%></td>
        </tr>
        <tr>
            <td class="labkey-form-label">In Target</td>
            <td style="text-align:right" id="inTargetValue"><%=h(targetCount)%></td>
            <td></td>
            <td class="labkey-form-label">In Decoy</td>
            <td style="text-align:right" id="inDecoyValue"><%=h(decoyCount)%></td>
        </tr>
        <tr>
            <td class="labkey-form-label">FDR</td>
            <td style="text-align:right" id="fdrValue"><%=h(fdr)%></td>
            <td></td>
            <td class="labkey-form-label">Adjust FDR To</td>
            <td style="text-align:right">
                <select name="desiredFdr" id="desiredFdr" onchange="setFilterParameter(this.value); this.form.submit();">
                    <%
                        List<Float> fdrOptions = bean.getFdrOptions();
                        defaultFormat.setMinimumIntegerDigits(1);
                        defaultFormat.setMinimumFractionDigits(1);
                        for(Float fdrOption : fdrOptions)
                        { %>
                    <option value="<%= h(fdrOption)%>"
                            <%=selected(Float.compare(fdrOption, (null == desiredFdr ? bean.getFdrAtDefaultPvalue() : desiredFdr)) == 0)%>><%=h(defaultFormat.format(fdrOption))%></option><%
                    } %>
                </select>
            </td>
        </tr>
        <tr>
            <td colspan="4">
                <label <% if (!isStandardView) { %> style="display:none"<% } %>><input type="checkbox" onclick="if(this.checked) {setFilterParameter(document.getElementById('desiredFdr').value)} this.form.submit();"
                       name="isIonCutoff" id="isIonCutoff"<%=checked(isIonCutoff)%> value="true"></input>Only show Ion &gt= this threshold</label>
            </td>
        </tr>
    </table>
</labkey:form>
<script>
    function setFilterParameter(desiredFdr) {
        <%
            JSONObject jsonObj = new JSONObject();

            for (Map.Entry<Float,Float> entry : bean.getFdrOptionToThresholdMap().entrySet()) {
                jsonObj.put(entry.getKey().toString(), entry.getValue());
            }
        %>
        // next value is intentionally not escaped -- comes from server and won't be parsed correctly in escaped form
        var fdrOptionToThresholdMap = JSON.parse( '<%= jsonObj.toString() %>' );
        if(document.getElementById('isIonCutoff').checked) {
            this.setGteParameter(fdrOptionToThresholdMap[desiredFdr]);
        }
        else {
            var gte = LABKEY.ActionURL.getParameter("MS2Peptides.Ion~gte");
            if(typeof gte !== "undefined") {
                this.setGteParameter(gte)
            }
        }
    }

    function setGteParameter(desiredGte) {
        var input = document.createElement("input");
        input.setAttribute("type", "hidden");
        input.setAttribute("name", "MS2Peptides.Ion~gte");
        input.setAttribute("id", "MS2Peptides.Ion~gte");
        input.setAttribute("value", desiredGte);
        document.getElementById('decoySummaryForm').appendChild(input);
    }
</script>