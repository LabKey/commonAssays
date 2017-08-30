<%
/*
 * Copyright (c) 2004-2017 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.peptideview.MS2RunViewType" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.FilterHeaderBean bean = ((JspView<MS2Controller.FilterHeaderBean>)HttpView.currentView()).getModelBean();
    User user = getUser();
    String runChargeFilterColumnName = bean.run.getChargeFilterColumnName();
    String grouping = getViewContext().cloneActionURL().getParameter("grouping");
    boolean isStandardView;
    if (null == grouping)
        isStandardView = true;  // no parameter = standard
    else
        isStandardView = grouping.equals("query");
%>
<labkey:form method="post" action="<%=h(bean.applyViewURL)%>">
    <table id="ms2RunViewConfig" class="lk-fields-table">
        <tr>
            <td valign=bottom><%=bean.applyView%></td>
            <td valign=bottom><%= button("Go").submit(true) %></td>
            <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
            <td valign=bottom><% if (!user.isGuest()) { %>
                <%= button("Save View").href(bean.saveViewURL) %><% } %></td>
            <td valign=bottom><% if (!user.isGuest()) { %>
                <%= button("Manage Views").href(bean.manageViewsURL) %><% } %></td><%
            if (bean.currentViewType.supportsPeptideColumnPicker())
            { %>
            <td valign=bottom>
                <%= PageFlowUtil.button("Pick Peptide Columns").href(bean.pickPeptideColumnsURL).id("pickPeptideColumns") %><% } %>
            </td>
            <%
            if (bean.currentViewType.supportsProteinColumnPicker())
            { %>
            <td valign=bottom>
                <%= PageFlowUtil.button("Pick Protein Columns").href(bean.pickProteinColumnsURL).id("pickProteinColumns") %></td><%
            } %>
        </tr>
    </table>
</labkey:form>
<labkey:form method="post" id="extraFilter" action="<%=h(bean.extraFilterURL)%>">
    <table>
        <tr>
            <td style="height: 100%; padding-right: 1em" id="ms2RunGrouping">
                <fieldset>
                    <legend>Grouping</legend>
                    <table class="labkey-data-region-legacy">
                        <tr>
                            <td style="vertical-align: middle;" nowrap>
                                <select id="viewTypeGrouping" name="grouping" onchange="document.getElementById('viewTypeExpanded').disabled = !viewTypeInfo[document.getElementById('viewTypeGrouping').selectedIndex];"><%

                                for(MS2RunViewType viewType : bean.viewTypes)
                                { %>
                                    <option value="<%=h(viewType.getURLName())%>"<%=selected(viewType.equals(bean.currentViewType))%>><%=h(viewType.getName())%></option><%
                                } %>
                                </select><%=PageFlowUtil.helpPopup("Grouping", "<table><tr><td style=\"vertical-align: top;\" nowrap><b>Standard</b></td><td>Shows peptides. If you choose columns from ProteinProphet or the search engine assigned protein, the peptides will be grouped under those columns. Use Grid Views->Customize Grid to change the column list.</td></tr><tr><td style=\"vertical-align: top;\" nowrap><b>Protein Groups</b></td><td>Shows proteins, grouped under the ProteinProphet assigned groups. Use Grid Views->Customize Grid to change the column list.</td></tr><tr><td style=\"vertical-align: top;\" nowrap><b>Peptides (Legacy)</b></td><td>Shows peptides without nesting them.</td></tr><tr><td style=\"vertical-align: top;\" nowrap><b>Protein (Legacy)</b></td><td>Shows peptides, grouped by their search engine assigned protein.</td></tr><tr><td style=\"vertical-align: top;\" nowrap><b>ProteinProphet (Legacy)</b></td><td>Shows peptides, grouped by their ProteinProphet protein groups.</td></tr></table>", true, 500)%>
                            </td>
                            <td style="vertical-align: middle;" nowrap><input id="viewTypeExpanded" type="checkbox" name="expanded" value="1"<%=checked(bean.expanded)%><%=disabled(!bean.currentViewType.supportsExpansion())%>>Expanded<%=PageFlowUtil.helpPopup("Expanded", "If selected, the groups will all be expanded. If not, the groups will be collapsed but can be expanded individually")%></td>
                            <td style="vertical-align: middle;" nowrap>
                                <%= button("Go").id("viewTypeSubmitButton").submit(true) %></td>
                        </tr>
                    </table>
                </fieldset>
            </td><%

            if (bean.currentViewType.supportsExtraFilters())
            { %>
                <td style="height: 100%; padding-right: 1em" id="ms2RunChargeScoreFilter">
                    <% if (bean.run.getChargeFilterColumnName() != null) { %>
                        <fieldset>
                            <legend><%=h(bean.run.getChargeFilterColumnName())%> charge filter</legend>
                            <table class="labkey-data-region-legacy">
                                <tr>
                                    <td nowrap style="vertical-align: middle;">1+&nbsp;<input id="Charge1" type="text" name=charge1 value="<%=bean.charge1%>" size="2"><%=PageFlowUtil.helpPopup("1+ " + bean.run.getChargeFilterColumnName() + " charge filter", "The minimum " + bean.run.getChargeFilterColumnName() + " value for peptides in the 1+ charge state.")%></td>
                                    <td nowrap style="vertical-align: middle;">2+&nbsp;<input id="Charge2" type="text" name=charge2 value="<%=bean.charge2%>" size="2"><%=PageFlowUtil.helpPopup("2+ " + bean.run.getChargeFilterColumnName() + " charge filter", "The minimum " + bean.run.getChargeFilterColumnName() + " value for peptides in the 2+ charge state.")%></td>
                                    <td nowrap style="vertical-align: middle;">3+&nbsp;<input id="Charge3" type="text" name=charge3 value="<%=bean.charge3%>" size="2"><%=PageFlowUtil.helpPopup("3+ " + bean.run.getChargeFilterColumnName() + " charge filter", "The minimum " + bean.run.getChargeFilterColumnName() + " value for peptides in the 3+ charge state.")%></td>
                                    <td nowrap style="vertical-align: middle;"><%= button("Go").id("AddChargeScoreFilterButton").submit(true) %></td>
                                </tr>
                            </table>
                        </fieldset>
                    <% } %>
                </td>
                <td style="height: 100%; padding-right: 1em" id="ms2RunMinimumTrypticEnds">
                    <fieldset>
                        <legend>Minimum&nbsp;tryptic&nbsp;ends</legend>
                        <table>
                            <tr>
                                <td nowrap style="vertical-align: middle;">
                                    <input type="radio" name="tryptic"<%=checked(0 == bean.tryptic)%> value="0">0<%=PageFlowUtil.helpPopup("0 tryptic ends", "All peptides will be shown, regardless of tryptic ends")%>
                                    <input type="radio" name="tryptic"<%=checked(1 == bean.tryptic)%> value="1">1<%=PageFlowUtil.helpPopup("1 tryptic end", "At least one end of the peptide must be tryptic")%>
                                    <input type="radio" name="tryptic"<%=checked(2 == bean.tryptic)%> value="2">2<%=PageFlowUtil.helpPopup("2 tryptic ends", "Both ends of the peptide must be tryptic")%>
                                </td>
                                <td nowrap style="vertical-align: middle;"><%= button("Go").id("AddTrypticEndsFilterButton").submit(true) %></td>
                            </tr>
                        </table>
                    </fieldset>
                </td>
                <td id="ms2RunHighestScoreFilter" style="height: 100%<% if (!isStandardView) { %>; display:none"<% } %>">
                    <fieldset>
                        <legend>Highest score filter</legend>
                        <table>
                            <tr>
                                <td nowrap style="vertical-align: middle;">
                                    <input type="checkbox" id="highestScore" name="highestScore"<%=checked(bean.highestScore)%> value="1">Only show highest score for <%=h(runChargeFilterColumnName)%><%=PageFlowUtil.helpPopup("Only show highest score for " + runChargeFilterColumnName, "Filter out all but highest " + runChargeFilterColumnName + " score for a peptide")%>
                                </td>
                                <td nowrap style="vertical-align: middle;"><%= button("Go").id("AddHighestScoreFilterButton").submit(true) %></td>
                            </tr>
                        </table>
                    </fieldset>
                </td><%
            }
            else
            { %>
                <input id="Charge1" type="hidden" name=charge1 value="<%=bean.charge1%>">
                <input id="Charge2" type="hidden" name=charge2 value="<%=bean.charge2%>">
                <input id="Charge3" type="hidden" name=charge3 value="<%=bean.charge3%>">
                <input type="hidden" name="tryptic" value="<%=bean.tryptic%>"><%
            } %>
        </tr>
    </table>
</labkey:form>
<script type="text/javascript">
    var viewTypeInfo = {};
    var count = 0;<%

    for (MS2RunViewType viewType : bean.viewTypes)
    { %>
        viewTypeInfo[count++] = <%=viewType.supportsExpansion()%>;<%
    } %>
</script>
