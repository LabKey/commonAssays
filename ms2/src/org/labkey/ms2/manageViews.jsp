<%
/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="java.util.TreeSet" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    HttpView<MS2Controller.ManageViewsBean> me = (HttpView<MS2Controller.ManageViewsBean>) HttpView.currentView();
    MS2Controller.ManageViewsBean bean = me.getModelBean();
%>
<labkey:form method="post" name="manageViewsForm" action="">
    <p>
        <input type=hidden value="<%=h(bean.getReturnURL())%>">
        <% for (MS2Controller.DefaultViewType defaultViewType : MS2Controller.DefaultViewType.values())
        { %>
            <input onchange="updateForm();" type="radio" <% if (bean.getDefaultViewType() == defaultViewType) { %>checked<% } %> name="defaultViewType" value="<%=h(defaultViewType.toString()) %>" id="defaultViewType<%=h(defaultViewType.toString()) %>"/> <%=h(defaultViewType.getDescription()) %><br/>
        <% } %>
    </p>
    <table class="labkey-data-region-legacy labkey-show-borders">
        <tr>
            <td class="labkey-column-header">Use as Default</td>
            <td class="labkey-column-header">Delete</td>
            <td class="labkey-column-header">View Name</td>
        </tr>
        <%
        // Use TreeSet to sort by name
        TreeSet<String> names = new TreeSet<>(bean.getViews().keySet());
        for (String name : names)
        { %>
            <tr class="labkey-row">
                <td>
                    <input <% if (name.equals(bean.getViewName())) { %>checked <% } %> type="radio" name="defaultViewName" value="<%=h(name) %>" />
                </td>
                <td>
                    <input type="checkbox" name="viewsToDelete" value="<%=h(name) %>" />
                </td>
                <td>
                    <%=h(name) %>
                </td>
            </tr>
        <%
        }

        if (names.isEmpty())
        { %>
            <tr class="labkey-row">
                <td colspan="3"><em>No data to show</em></td>
            </tr>
        <%
        }
        %>
    </table><br/>
    <%= button("OK").submit(true) %> <%= button("Cancel").href(bean.getReturnURL()) %>
</labkey:form>

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
