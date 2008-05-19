<%
/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
<%@ page import="org.labkey.ms2.MS2Controller.ManageViewsBean" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    HttpView<MS2Controller.ManageViewsBean> me = (HttpView<MS2Controller.ManageViewsBean>) HttpView.currentView();
    MS2Controller.ManageViewsBean bean = me.getModelBean();
%>
<table class="dataRegion">
    <tr>
        <td>Select one or more views and press Delete</td>
    </tr>
    <tr>
        <td>
            <form method="post" action="">
                <table class="dataRegion" border="0">
                    <tr>
                        <td><input type=hidden value="<%=h(bean.returnURL)%>"><%=bean.selectHTML%></td>
                    </tr>
                    <tr>
                        <td align=center><%=buttonImg("Delete")%> <%=PageFlowUtil.buttonLink("Done", bean.returnURL)%></td>
                    </tr>
                </table>
            </form>
        </td>
    </tr>
</table>