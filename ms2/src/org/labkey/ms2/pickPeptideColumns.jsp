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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.PickColumnsBean bean = ((JspView<MS2Controller.PickColumnsBean>) HttpView.currentView()).getModelBean();
    Container c = getViewContext().getContainer();
%>
<form method="post">
<table class="labkey-data-region">
    <tr>
        <td><strong>Common:</strong></td>
        <td><%=h(bean.commonColumns)%></td>
        <td nowrap><input type="image" src="<%=PageFlowUtil.buttonSrc("Pick")%>" onclick="return setCurrent('<%=h(bean.commonColumns)%>')"> <input type="image" src="<%=PageFlowUtil.buttonSrc("Add")%>" onclick="return appendToCurrent('<%=h(bean.commonColumns)%>')"></td>
    </tr>
    <tr>
        <td nowrap><strong>Protein Prophet:</strong></td>
        <td><%=h(bean.proteinProphetColumns)%></td>
        <td nowrap align=right><input type="image" src="<%=PageFlowUtil.buttonSrc("Add")%>" onclick="return appendToCurrent('<%=h(bean.proteinProphetColumns)%>')"></td>
    </tr>
    <tr>
        <td><strong>Quantitation:</strong></td>
        <td><%=h(bean.quantitationColumns)%></td>
        <td nowrap align=right><input type="image" src="<%=PageFlowUtil.buttonSrc("Add")%>" onclick="return appendToCurrent('<%=h(bean.quantitationColumns)%>')"></td>
    </tr>
    <tr>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td>&nbsp;</td>
        <td><i>If you are viewing a run that does not have Protein Prophet or quantitation data loaded, those columns will be blank.</i></td>
    </tr>
    <tr>
        <td>&nbsp;</td>
    </tr>
      <tr>
          <td>&nbsp;</td>
      </tr>
    <tr>
        <td><strong>Default:</strong></td>
        <td><%=h(bean.defaultColumns)%></td>
        <td nowrap ><input type="image" src="<%=PageFlowUtil.buttonSrc("Pick")%>" onclick="return setCurrent('<%=h(bean.defaultColumns)%>');"> <input type="image" src="<%=PageFlowUtil.buttonSrc("Add")%>" onclick="return appendToCurrent('<%=h(bean.defaultColumns)%>')"></td>
    </tr>
    <tr>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td><strong>Current:</strong></td>
        <td><textarea style="width:100%;" name="columns" id="columns" rows="3" cols="100"><%=h(bean.currentColumns)%></textArea></td>
    </tr>
    <tr><td colspan=2 align=center><input type="image" src="<%=PageFlowUtil.buttonSrc("Pick Columns")%>" onclick="this.form.action='<%=h(MS2Controller.getPickPeptideColumnsPostURL(c, bean.returnURL, false))%>';"> <input type="image" src="<%=PageFlowUtil.buttonSrc("Save As Default")%>" onclick="this.form.action='<%=h(MS2Controller.getPickPeptideColumnsPostURL(c, bean.returnURL, true))%>';"></td></tr>
</table></form>

<script type="text/javascript">
function setCurrent(newString)
{
    document.getElementById("columns").value = newString;
    return false;
}
function appendToCurrent(newString)
{
    if (document.getElementById("columns") != "")
    {
        document.getElementById("columns").value += ", ";
    }
    document.getElementById("columns").value += newString;
    return false;
}
</script>