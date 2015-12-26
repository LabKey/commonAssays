<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
/*
 * Copyright (c) 2004-2013 Fred Hutchinson Cancer Research Center
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
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<MS2Manager.DecoySummaryBean> me = ((JspView<MS2Manager.DecoySummaryBean>)HttpView.currentView());
    MS2Manager.DecoySummaryBean bean = me.getModelBean();

    NumberFormat defaultFormat = NumberFormat.getPercentInstance();
    defaultFormat.setMinimumFractionDigits(2);
    String fdr = defaultFormat.format(bean.getFdr());
    float fdrThreshold = bean.getFdrThreshold();

    ActionURL setDesiredFdrURL = getViewContext().cloneActionURL();
    setDesiredFdrURL.deleteParameter("fdrThreshold");
%>
<% if (Float.compare(bean.getFdr(), fdrThreshold) > 0)
{ %>
<span>No identity threshold with FDR below desired value. Showing best FDR over desired value.</span><br/><br/>
<%}%>
<labkey:form method="GET" action="<%=setDesiredFdrURL%>" name="decoySummary">
    <% for (Pair<String, String> param : setDesiredFdrURL.getParameters())
    { %>
    <input type="hidden" name="<%=h(param.getKey())%>" value="<%=h(param.getValue())%>"/>
    <% } %>
    <table class="labkey-data-region labkey-show-borders" cellpadding="4" cellspacing="4">
        <tr>
            <th>Identity Threshold</th>
            <th>In Target</th>
            <th>In Decoy</th>
            <th>FDR</th>
        </tr>
        <tr>
        <td style="text-align:right"><%=h(bean.getIdentityThreshold())%></td>
        <td style="text-align:right"><%=h(bean.getTargetCount())%></td>
        <td style="text-align:right"><%=h(bean.getDecoyCount())%></td>
        <td style="text-align:right"><%=h(fdr)%></td>
        <td>
            <%= button("Adjust To").submit(true) %>
            <select name="fdrThreshold">
            <%
                List<Float> fdrOptions = Arrays.asList(.001f, .002f, .01f, .02f, .025f, .05f, .1f);
                defaultFormat.setMinimumIntegerDigits(1);
                defaultFormat.setMinimumFractionDigits(1);
                for(float fdrOption : fdrOptions)
                { %>
            <option value="<%=h(fdrOption)%>"<%=selected(Float.compare(fdrOption, fdrThreshold) == 0)%>><%=h(defaultFormat.format(fdrOption))%></option><%
            } %>
            </select>
        </td>
        </tr>
    </table>
</labkey:form>