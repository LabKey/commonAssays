<%
/*
 * Copyright (c) 2004-2008 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.util.Formats" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="java.text.Format" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();
    Format intFormat = Formats.commaf0;
    Format percentFormat = Formats.percent;
%>
<table class="labkey-data-region" width=1150px>
    <col width=15%><col width=85%>
    <tr><td>&nbsp;</td><td>&nbsp;</td></tr>
    <tr><td>Sequence Mass:</td><td><%=h(intFormat.format(bean.protein.getMass()))%></td></tr><%

    if (bean.showPeptides)
    { %>
    <tr><td>AA Coverage:</td><td><%=h(percentFormat.format(bean.protein.getAAPercent()))%> (<%=intFormat.format(bean.protein.getAACoverage())%> / <%=intFormat.format(bean.protein.getSequence().length())%>)</td></tr>
    <tr><td>Mass Coverage:</td><td><%=h(percentFormat.format(bean.protein.getMassPercent()))%> (<%=intFormat.format(bean.protein.getMassCoverage())%> / <%=intFormat.format(bean.protein.getMass())%>)</td></tr><%
    } %>
    <tr><td>&nbsp;</td><td>&nbsp;</td></tr>
    <tr><td colspan=2><big><tt><%=bean.protein.getFormattedSequence()%></tt></big></td></tr>
</table>

<script language="javascript 1.1" type="text/javascript">
    function grabFocus()
    {
        self.focus();
    }
    window.onload = grabFocus;
</script>