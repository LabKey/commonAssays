<%
/*
 * Copyright (c) 2015-2018 LabKey Corporation
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
<%@ page import="org.labkey.api.util.Link" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page import="org.labkey.luminex.LuminexController.GuideSetsDeleteBean.GuideSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="static org.labkey.api.util.DOM.Attribute.tabindex" %>
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
    List<GuideSet> guideSets = bean.getGuideSets();

    ActionURL successUrl = bean.getSuccessActionURL(new ActionURL(LuminexController.ManageGuideSetAction.class, getContainer()));
    ActionURL cancelUrl = bean.getCancelActionURL(successUrl);
%>

<% if (bean.getGuideSets() == null || bean.getGuideSets().isEmpty()) { %>

    <p>There are no selected guide sets to delete.</p>
    <%= unsafe(button("OK").href(successUrl).toString())%>

<% } else { %>
    <%--NOTE: here is where we need to display all the information about what is being deleted--%>

    <p>Are you sure you want to delete the following guide set<%=h(guideSets.size()!=1 ? "s" : "")%>?</p>

    <ul>
        <% for (GuideSet gs : guideSets) { %>
            <li>
            <%
                new Link.LinkBuilder("Guide Set " + gs.getGuideSetId() + ": " + gs.getComment())
                    .href("#")
                    .attributes(Map.of(tabindex.name(), "-1"))
                    .onClick("createGuideSetWindow(" + bean.getProtocol().getRowId() + ", " + gs.getGuideSetId() + ", false)")
                    .appendTo(out);
            %>
            </li>
            <br>
            Type: <% if (gs.isValueBased()) {%>Value-based<%} else {%>Run-based<%}%>
            <br><br>
            Current Guide Set: <%=h(gs.getCurrent())%>
            <br>
            <%
                if (!gs.getMemberRuns().isEmpty()) { %>
                <br>
                Member Runs:
                <ul>
                    <% for (String run : gs.getMemberRuns()) { %>
                        <li><%=h(run)%></li>
                    <% } %>
                </ul>
            <%
                }

                if (!gs.getUserRuns().isEmpty())
                {
            %>
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
            <%
                }
            %>
                </ul>
            <% } %>
            <br>
        <% } %>
    </ul>

    <%--NOTE: this is all required boilerplate--%>
    <labkey:form action="<%=getViewContext().cloneActionURL().deleteParameters()%>" method="post">
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
        <% } if (successUrl != null) { %>
            <input type="hidden" name="<%=ActionURL.Param.successUrl%>" value="<%= h(successUrl) %>"/>
        <% } if (bean.getProtocol() != null) { %>
            <input type="hidden" name="rowId" value="<%=bean.getProtocol().getRowId()%>"/>
        <% } %>
        <input type="hidden" name="forceDelete" value="true"/>
        <%= button("Confirm Delete").submit(true) %>
        <%= unsafe(button("Cancel").href(cancelUrl).toString())%>
    </labkey:form>
<% } %>