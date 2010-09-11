<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    Map<String, Object> runProperties = bean.getRunDisplayProperties();
    int columnCount = 2;
%>
<table width="100%">
    <%
        Iterator<Map.Entry<String, Object>> propertyIt = runProperties.entrySet().iterator();
        Pair<String, Object>[] entries = new Pair[runProperties.size()];
        for (int i = 0; i < entries.length; i++)
        {
            Map.Entry<String, Object> entry = propertyIt.next();
            entries[i] = new Pair<String, Object>(entry.getKey(), entry.getValue());
        }

        int longestColumn = (int) Math.ceil(entries.length/2.0);
        for (int row = 0; row < longestColumn; row++)
        {
    %>
        <tr>
        <%
            for (int col = 0; col < columnCount; col++)
            {
                int index = col*longestColumn + row;
                if (index < entries.length)
                {
                    Pair<String, Object> property = index < entries.length ? entries[index] : null;
            %>
                <th style="text-align:left"><%= property != null ? h(property.getKey()) : "&nbsp;"  %></th>
                <td><%= property != null ? h(property.getValue()) : "&nbsp;"  %></td>
            <%
                }
            }
        %>
        </tr>
    <%
        }
    %>
</table>
