<%
    /*
     * Copyright (c) 2015-2016 LabKey Corporation
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
<%@ page import="org.labkey.api.data.DataRegion" %>
<%@ page import="org.labkey.api.data.DataRegionSelection" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext3"); // TODO: fix rendering issue to be able to remove this dependency
        dependencies.add("Ext4");
        dependencies.add("luminex/GuideSetWindow.js");
    }
%>
<%
    JspView<LuminexController.GuideSetsDeleteBean> me = (JspView<LuminexController.GuideSetsDeleteBean>) HttpView.currentView();
    LuminexController.GuideSetsDeleteBean bean = me.getModelBean();
    List<LuminexController.GuideSetsDeleteBean.GuideSet> guideSets = bean.getGuideSets();
%>

<% if (bean.getGuideSets() == null || bean.getGuideSets().isEmpty()) { %>

    <p>There are no selected guide sets to delete.</p>
    <%= text(bean.getReturnUrl() == null || bean.getReturnUrl().isEmpty() ? button("OK").href(buildURL(LuminexController.ManageGuideSetAction.class)).toString() : button("OK").href(bean.getReturnUrl()).toString())%>

<% } else { %>
    <%--NOTE: here is where we need to display all the information about what is being deleted--%>

    <p>Are you sure you want to delete the following guide set<%=h(guideSets.size()!=1 ? "s" : "")%>?</p>

    <ul>
        <% for (LuminexController.GuideSetsDeleteBean.GuideSet gs : guideSets) { %>
            <li><a href="#" tabindex="-1" onclick="createGuideSetWindow('<%=h(bean.getProtocol().getRowId())%>','<%=h(gs.getGuideSetId())%>', false)">Guide Set <%= h(gs.getGuideSetId()) %>: <%= h(gs.getComment()) %></a></li>
            <br>
            Type: <% if(gs.isValueBased()) out.print("Value-based"); else out.print("Run-based"); %>
            <br><br>
            Current Guide Set: <%=gs.getCurrent()%>
            <br>
            <% if (gs.getMemberRuns().size() > 0 ) { %>
                <br>
                Member Runs:
                <ul>
                    <% for (String run : gs.getMemberRuns()) { %>
                        <li><%=h(run)%></li>
                    <% } %>
                </ul>
            <% } %>
            <% if (gs.getUserRuns().size() > 0 ) { %>
                <br>
                User Runs:
                <ul>
                    <%
                        int len = Math.min(gs.getUserRuns().size(), 20);
                        for (int i = 0; i < len; i++)
                        {
                    %>
                            <li><%=h(gs.getUserRuns().get(i))%></li>
                    <%
                        }

                        if (len < gs.getUserRuns().size())
                        {
                    %>
                            <li>... [Showing first <%=len%> runs, <%=gs.getUserRuns().size()%> total]</li>
                    <%  } %>
                </ul>
            <% } %>
            <br>
        <% } %>
    </ul>

    <%--NOTE: this is all required boilerplate--%>
    <labkey:form action="<%= h(getViewContext().cloneActionURL().deleteParameters()) %>" method="post">
        <%
            if (getViewContext().getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME) != null)
            {
                for (String selectedValue : getViewContext().getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME))
                { %>
                    <input type="hidden" name="<%= h(DataRegion.SELECT_CHECKBOX_NAME) %>" value="<%= h(selectedValue) %>" /><%
                }
            }
        %>
        <% if (bean.getSingleObjectRowId() != null) { %>
            <input type="hidden" name="singleObjectRowId" value="<%= bean.getSingleObjectRowId() %>"/>
        <% }
            if (bean.getDataRegionSelectionKey() != null) { %>
        <input type="hidden" name="<%= h(DataRegionSelection.DATA_REGION_SELECTION_KEY) %>" value="<%= h(bean.getDataRegionSelectionKey()) %>" />
        <% } if (bean.getReturnUrl() != null) { %>
            <input type="hidden" name="returnURL" value="<%= h(bean.getReturnUrl()) %>"/>
        <% } if (bean.getProtocol() != null) { %>
            <input type="hidden" name="rowId" value="<%= h(bean.getProtocol().getRowId()) %>"/>
        <% } %>
        <input type="hidden" name="forceDelete" value="true"/>
        <%= button("Confirm Delete").submit(true) %>
        <%= text(bean.getReturnUrl() == null || bean.getReturnUrl().isEmpty() ? button("Cancel").href(buildURL(LuminexController.ManageGuideSetAction.class)).toString() : button("Cancel").href(bean.getReturnUrl()).toString())%>
    </labkey:form>
<% } %>