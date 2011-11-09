<%
/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
<table class="labkey-data-region"><tr>
  <td><form method="post" action=""><table class="labkey-data-region">
    <tr>
        <td><strong>Common:</strong></td>
        <td><%=h(bean.commonColumns)%></td>
        <td nowrap>
            <%=PageFlowUtil.generateSubmitButton("Pick", "setCurrent('" + h(bean.commonColumns) + "');return false;")%>
            <%=PageFlowUtil.generateSubmitButton("Add", "appendToCurrent('" + h(bean.commonColumns) + "');return false;")%></td>
    </tr>
    <tr>
        <td nowrap><strong>Protein Prophet:</strong></td>
        <td><%=h(bean.proteinProphetColumns)%></td>
        <td nowrap align=right>
            <%=PageFlowUtil.generateSubmitButton("Add", "appendToCurrent('" + h(bean.proteinProphetColumns) + "');return false;")%></td>
    </tr>
    <tr>
        <td><strong>Quantitation:</strong></td>
        <td><%=h(bean.quantitationColumns)%></td>
        <td nowrap align=right>
            <%=PageFlowUtil.generateSubmitButton("Add", "appendToCurrent('" + h(bean.quantitationColumns) + "');return false;")%></td>
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
        <td nowrap >
            <%=PageFlowUtil.generateSubmitButton("Pick", "setCurrent('" + h(bean.defaultColumns) + "');return false;")%>
            <%=PageFlowUtil.generateSubmitButton("Add", "appendToCurrent('" +h(bean.defaultColumns) + "');return false;")%></td>
    </tr>
      <tr>
          <td>&nbsp;</td>
      </tr>
    <tr>
        <td><strong>Current:</strong></td>
        <td><textarea style="width: 100%;" name="columns" id="columns" rows="3" cols="100"><%=h(bean.currentColumns)%></textArea></td>
    </tr>
    <tr><td colspan=2 align=center>
        <%=PageFlowUtil.generateSubmitButton("Pick Columns", "this.form.action='" + MS2Controller.getPickProteinColumnsPostURL(c, bean.returnURL, false) + "';")%>
        <%=PageFlowUtil.generateSubmitButton("Save As Default", "this.form.action='" + MS2Controller.getPickProteinColumnsPostURL(c, bean.returnURL, true) + "';")%></td></tr>
  </table></form></td>
</tr></table>

<script type="text/javascript">
function setCurrent(newString)
{
    document.getElementById("columns").value = newString;
}
function appendToCurrent(newString)
{
    if (document.getElementById("columns") != "")
    {
        document.getElementById("columns").value += ", ";
    }
    document.getElementById("columns").value += newString;
}
</script>