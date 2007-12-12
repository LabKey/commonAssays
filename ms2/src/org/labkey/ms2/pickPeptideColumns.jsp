<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.OldMS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    OldMS2Controller.PickColumnsBean bean = ((JspView<OldMS2Controller.PickColumnsBean>) HttpView.currentView()).getModelBean();
%>
<form method="post" action="<%=h(bean.saveUrl)%>">
<table class="dataRegion">
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
        <td><textarea style="font-family: verdana; font-size: 11; width:100%;" name="columns" id="columns" rows="3" cols="100"><%=h(bean.currentColumns)%></textArea></td>
    </tr>
    <tr><td colspan=2><input type=hidden value="<%=h(bean.queryString)%>" name="queryString"></td></tr>
    <tr><td colspan=2 align=center><input type="image" src="<%=PageFlowUtil.buttonSrc("Pick Columns")%>"> <input type="image" src="<%=PageFlowUtil.buttonSrc("Save As Default")%>" onclick="this.form.action='<%=h(bean.saveDefaultUrl)%>';"></td></tr>
</table></form>

<script type="text/javascript" for=window event=onload>
try {document.getElementById("columns").focus();} catch (x) {}
</script>
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