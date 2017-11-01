<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.ms2.MS2Controller"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.PeptideProphetDetailsBean bean = ((JspView<MS2Controller.PeptideProphetDetailsBean>)HttpView.currentView()).getModelBean();
    DecimalFormat df4 = new DecimalFormat("0.0000");
    DecimalFormat df2 = new DecimalFormat("0.00");
    ActionURL sensitivityURL = getViewContext().cloneActionURL().setAction(bean.action);
    float[] minProb = bean.summary.getMinProb();
    float[] sensitivity = bean.summary.getSensitivity();
    float[] error = bean.summary.getError();
%>
<table>
<tr>
    <td><img src="<%=h(sensitivityURL)%>" alt="Sensitivity Plot"></td>
    <td>
        <table class="labkey-data-region-legacy labkey-show-borders">
            <tr>
                <td class="labkey-column-header">Minimum probability</td>
                <td class="labkey-column-header">Sensitivity</td>
                <td class="labkey-column-header">Error rate</td>
            </tr>
<%
    for (int i = 0; i < sensitivity.length; i++)
    {
        out.print("<tr class=\"" + (getShadeRowClass(i % 2 == 0)) + "\">");
        out.print("<td align=\"right\">" + df2.format(minProb[i]) + "</td>");
        out.print("<td align=\"right\">" + df4.format(sensitivity[i]) + "</td>");
        out.print("<td align=\"right\">" + df4.format(error[i]) + "</td>");
        out.print("</tr>");
    }
%>
        </table>
    </td>
</tr>
</table>
