<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.labkey.ms2.MS2Controller"%>
<%@ page import="java.text.DecimalFormat" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.PeptideProphetDetailsBean bean = ((JspView<MS2Controller.PeptideProphetDetailsBean>)HttpView.currentView()).getModelBean();
    DecimalFormat df4 = new DecimalFormat("0.0000");
    DecimalFormat df2 = new DecimalFormat("0.00");
    ViewContext me = HttpView.currentContext();
    ActionURL sensitivityURL = me.cloneActionURL().setAction(bean.action);
    float[] minProb = bean.summary.getMinProb();
    float[] sensitivity = bean.summary.getSensitivity();
    float[] error = bean.summary.getError();
%>
<table>
<tr><td colspan="2"><span class="navPageHeader"><%=h(bean.title)%></span></td></tr>
<tr><td colspan="2">&nbsp;</td></tr>
<tr>
    <td><img src="<%=h(sensitivityURL)%>" alt="Sensitivity Plot"></td>
    <td><table class="dataRegion">
        <tr><td><b>Minimum probability</b></td><td><b>Sensitivity</b></td><td><b>Error rate</b></td></tr>
<%
    for (int i = 0; i < sensitivity.length; i++)
        out.print("<tr><td>" + df2.format(minProb[i]) + "</td><td>" + df4.format(sensitivity[i]) + "</td><td>" + df4.format(error[i]) + "</td></tr>");
%>
    </table></td>
</tr>
</table>
