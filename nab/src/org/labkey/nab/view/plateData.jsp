<%
/*
 * Copyright (c) 2010 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.NabAssayRun" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.nab.Luc5Assay" %>
<%@ page import="org.labkey.api.study.Plate" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    NabAssayRun assay = bean.getAssay();
    ViewContext context = me.getViewContext();
%>
<table>
<%
    for (Plate plate : assay.getPlates())
    {
%>
<tr>
    <td valign=top>
        <table>
            <tr>
                <td>&nbsp;</td>
                <%
                    for (int c = 1; c <= plate.getColumns(); c++)
                    {
                %>
                <th><%=c %></th>
                <%
                    }
                %>
            </tr>
            <%
                for (int row = 0; row < plate.getRows(); row++)
                {
            %>
            <tr>
                <th><%=(char) ('A' + row)%></th>

                <%
                    for (int col = 0; col < plate.getColumns(); col++)
                    {
                %>
                <td align=right>
                    <%=Luc5Assay.intString(plate.getWell(row, col).getValue())%></td>
                <%
                    }
                %>
            </tr>
            <%
                }
            %>
        </table>
    </td>
</tr>
<%
    }
%>
</table>