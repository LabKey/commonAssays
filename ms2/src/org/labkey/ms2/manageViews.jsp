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
<%@ page import="java.util.TreeSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    HttpView<MS2Controller.ManageViewsBean> me = (HttpView<MS2Controller.ManageViewsBean>) HttpView.currentView();
    MS2Controller.ManageViewsBean bean = me.getModelBean();
%>
<table class="labkey-data-region">
    <tr>
        <td>
            <form method="post" name="manageViewsForm" action="">
                <p>
                    <input type=hidden value="<%=h(bean.getReturnURL())%>">
                    <% for (MS2Controller.DefaultViewType defaultViewType : MS2Controller.DefaultViewType.values())
                    { %>
                        <input onchange="updateForm();" type="radio" <% if (bean.getDefaultViewType() == defaultViewType) { %>checked<% } %> name="defaultViewType" value="<%= PageFlowUtil.filter(defaultViewType.toString()) %>" id="defaultViewType<%= PageFlowUtil.filter(defaultViewType.toString()) %>"/> <%= PageFlowUtil.filter(defaultViewType.getDescription()) %><br/>
                    <% } %>
                </p>
                <table class="labkey-data-region">
                    <tr>
                        <td><strong>Default</strong></td><td><strong>Delete</strong></td><td><strong>View Name</strong></td>
                    </tr>
                    <%
                    // Use TreeSet to sort by name
                    TreeSet<String> names = new TreeSet<String>(bean.getViews().keySet());
                    for (String name : names)
                    { %>
                        <tr>
                            <td>
                                <input <% if (name.equals(bean.getViewName())) { %>checked <% } %> type="radio" name="defaultViewName" value="<%= PageFlowUtil.filter(name) %>" />
                            </td>
                            <td>
                                <input type="checkbox" name="viewsToDelete" value="<%= PageFlowUtil.filter(name) %>" />
                            </td>
                            <td>
                                <%= PageFlowUtil.filter(name) %>
                            </td>
                        </tr>
                    <% } %>
                    <tr>
                        <td colspan="3"><%=buttonImg("OK")%> <%=PageFlowUtil.buttonLink("Cancel", bean.getReturnURL())%></td>
                    </tr>
                </table>
            </form>
        </td>
    </tr>
</table>

<script type="text/javascript">
    function updateForm()
    {
        var radioElements = document.getElementsByName("defaultViewName");
        for (var i = 0; i < radioElements.length; i++)
        {
            radioElements[i].disabled = !document.getElementById('defaultViewType<%= MS2Controller.DefaultViewType.Manual %>').checked;
        }
    }

    updateForm();
</script>
