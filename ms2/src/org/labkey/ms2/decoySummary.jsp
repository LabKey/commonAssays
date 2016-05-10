<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
/*
 * Copyright (c) 2005-2016 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Manager" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.List" %>
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

    ActionURL newURL = getViewContext().cloneActionURL();
    String ionCutoff = newURL.getParameter("MS2Peptides.Ion~gte");
    boolean isIonCutoff;
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
%>
<% if (null != desiredFdr && Float.compare(bean.getFdr(), desiredFdr) > 0)
{ %>
<span>No score threshold with FDR below desired value. Showing best FDR over desired value.</span><br/><br/>
<%}%>
<% if (null == desiredFdr && Float.compare(bean.getFdrAtDefaultPvalue(), 1f) > 0)
{ %>
<span>No scores were above threshold for standard p-value. FDR is 100%.</span><br/><br/>
<%}%>
<labkey:form method="GET" action="<%=newURL%>" name="decoySummary">
    <% for (Pair<String, String> param : newURL.getParameters())
    { %>
    <input type="hidden" name="<%=h(param.getKey())%>" value="<%=h(param.getValue())%>"/>
    <% } %>

    <table >
        <tr>
            <td class="labkey-form-label">P Value</td>
            <td class="labkey-form-label">Ion Threshold</td>
            <td class="labkey-form-label">In Target</td>
            <td class="labkey-form-label">In Decoy</td>
            <td class="labkey-form-label" style="padding-left:10px;">FDR</td>
            <td class="labkey-form-label">Adjust FDR To</td>
        </tr>
        <tr>
            <td style="text-align:right"><%=h(pVal)%></td>
            <td style="text-align:right"><%=h(bean.getScoreThreshold())%></td>
            <td style="text-align:right"><%=h(targetCount)%></td>
            <td style="text-align:right"><%=h(decoyCount)%></td>
            <td style="text-align:right; padding-left:10px;"><%=h(fdr)%></td>
            <td style="text-align:right">
                <select name="desiredFdr" onchange="document.getElementById('ionCutoff').checked = false; this.form.submit();">
                    <%
                        List<Float> fdrOptions = new ArrayList<>(Arrays.asList(.001f, .002f, .01f, .02f, .025f, .05f, .1f));
                        fdrOptions.add(bean.getFdrAtDefaultPvalue());
                        Collections.sort(fdrOptions);
                        defaultFormat.setMinimumIntegerDigits(1);
                        defaultFormat.setMinimumFractionDigits(1);
                        for(float fdrOption : fdrOptions)
                        { %>
                    <option value="<%= h(Float.compare(fdrOption, bean.getFdrAtDefaultPvalue()) == 0 ? null : fdrOption)%>"
                            <%=selected(Float.compare(fdrOption, (null == desiredFdr ? bean.getFdrAtDefaultPvalue() : desiredFdr)) == 0)%>><%=h(defaultFormat.format(fdrOption))%></option><%
                    } %>
                </select>
            </td>
            <td style="text-align: right; padding-left:5px">
                <label <% if (!isStandardView) { %> style="display:none"<% } %>><input type="checkbox" onclick="this.form.submit();" name="MS2Peptides.Ion~gte" id="ionCutoff"<%=checked(isIonCutoff)%> value=<%=h(bean.getScoreThreshold())%>></input>Only show Ion &gt= this threshold</label>
            </td>
        </tr>
    </table>
</labkey:form>