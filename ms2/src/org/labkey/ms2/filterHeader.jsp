<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.peptideview.MS2RunViewType" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.FilterHeaderBean bean = ((JspView<MS2Controller.FilterHeaderBean>)HttpView.currentView()).getModelBean();
%>
<form method="post" action="<%=h(bean.applyViewURL)%>">
    <table class="dataRegion" border="0">
        <tr>
            <td valign=bottom><%=bean.applyView%></td>
            <td valign=bottom><input type="image" value="Go" src="<%=PageFlowUtil.buttonSrc("Go")%>"></td>
            <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
            <td valign=bottom><a href="<%=h(bean.saveViewURL)%>"><img border=0 src="<%=PageFlowUtil.buttonSrc("Save View")%>"></a></td>
            <td valign=bottom><a href="<%=h(bean.manageViewsURL)%>"><img border=0 src="<%=PageFlowUtil.buttonSrc("Manage Views")%>"></a></td><%
            if (bean.currentViewType.supportsPeptideColumnPicker())
            { %>
            <td valign=bottom><a href="<%=h(bean.pickPeptideColumnsURL)%>" id="pickPeptideColumns"><img border=0 src="<%=PageFlowUtil.buttonSrc("Pick Peptide Columns")%>"></a></td><%
            }
            if (bean.currentViewType.supportsProteinColumnPicker())
            { %>
            <td valign=bottom><a href="<%=h(bean.pickProteinColumnsURL)%>" id="pickProteinColumns"><img border=0 src="<%=PageFlowUtil.buttonSrc("Pick Protein Columns")%>"></a></td><%
            } %>
        </tr>
    </table>
</form>
<form method="post" id="extraFilter" action="<%=h(bean.extraFilterURL)%>">
    <table cellspacing="0" cellpadding="0" border="0">
        <tr>
            <td style="height: 100%; padding-right: 1em">
                <fieldset>
                    <legend>Grouping</legend>
                    <table class="dataRegion">
                        <tr>
                            <td style="vertical-align: middle;" nowrap>
                                <select id="viewTypeGrouping" name="grouping" onchange="document.getElementById('viewTypeExpanded').disabled = !viewTypeInfo[document.getElementById('viewTypeGrouping').selectedIndex];"><%

                                for(MS2RunViewType viewType : bean.viewTypes)
                                { %>
                                    <option value="<%=h(viewType.getURLName())%>" <%=viewType.equals(bean.currentViewType) ? "selected" : ""%>><%=viewType.getName()%></option><%
                                } %>
                                </select><%=PageFlowUtil.helpPopup("Grouping", "<table><tr><td style=\"vertical-align: top;\"><b>None</b></td><td>Shows peptides without nesting them.</td></tr><tr><td style=\"vertical-align: top;\"><b>Protein</b></td><td>Shows peptides, grouped by their search engine assigned protein.</td></tr><tr><td style=\"vertical-align: top;\"><b>Protein Prophet</b></td><td>Shows peptides, grouped by their ProteinProphet protein groups.</td></tr><tr><td style=\"vertical-align: top;\"><b>Query - Peptides</b></td><td>Shows peptides. If you choose columns from ProteinProphet or the search engine assigned protein, the peptides will be grouped under those columns. Use the Customize View link to change the column list.</td></tr><tr><td style=\"vertical-align: top;\"><b>Query - Protein Groups</b></td><td>Shows proteins, grouped under the ProteinProphet assigned groups. Use the Customize View link to change the column list.</td></tr></table>", true)%>
                            </td>
                            <td style="vertical-align: middle;" nowrap><input id="viewTypeExpanded" style="margin-right: 0px;" type="checkbox" name="expanded" value="1"<%=bean.expanded ? " checked" : ""%><%=!bean.currentViewType.supportsExpansion() ? " disabled" : ""%>>Expanded<%=PageFlowUtil.helpPopup("Expanded", "If selected, the groups will all be expanded. If not, the groups will be collapsed but can be expanded individually")%></td>
                            <td style="vertical-align: middle;" nowrap><input type="image" id="viewTypeSubmitButton" src="<%=PageFlowUtil.buttonSrc("Go")%>" />
                        </tr>
                    </table>
                </fieldset>
            </td><%

            if (bean.currentViewType.supportsExtraFilters())
            { %>
                <td style="height: 100%; padding-right: 1em">
                    <fieldset>
                        <legend><%=h(bean.run.getChargeFilterColumnName())%> charge filter</legend>
                        <table class="dataRegion" border="0">
                            <tr>
                                <td nowrap style="vertical-align: middle;">1+&nbsp;<input id="Charge1" type="text" name=charge1 value="<%=bean.charge1%>" size="2"><%=PageFlowUtil.helpPopup("1+ " + bean.run.getChargeFilterColumnName() + " charge filter", "The minimum " + bean.run.getChargeFilterColumnName() + " value for peptides in the 1+ charge state.")%></td>
                                <td nowrap style="vertical-align: middle;">2+&nbsp;<input id="Charge2" type="text" name=charge2 value="<%=bean.charge2%>" size="2"><%=PageFlowUtil.helpPopup("2+ " + bean.run.getChargeFilterColumnName() + " charge filter", "The minimum " + bean.run.getChargeFilterColumnName() + " value for peptides in the 2+ charge state.")%></td>
                                <td nowrap style="vertical-align: middle;">3+&nbsp;<input id="Charge3" type="text" name=charge3 value="<%=bean.charge3%>" size="2"><%=PageFlowUtil.helpPopup("3+ " + bean.run.getChargeFilterColumnName() + " charge filter", "The minimum " + bean.run.getChargeFilterColumnName() + " value for peptides in the 3+ charge state.")%></td>
                                <td nowrap style="vertical-align: middle;"><input type="image" id="AddChargeScoreFilterButton" value="Add Filter" src="<%=PageFlowUtil.buttonSrc("Go")%>" name=""></td>
                            </tr>
                        </table>
                    </fieldset>
                </td>
                <td style="height: 100%">
                    <fieldset>
                        <legend>Tryptic&nbsp;ends</legend>
                        <table>
                            <tr>
                                <td nowrap style="vertical-align: middle;">
                                    <input type="radio" name="tryptic"<%=(0 == bean.tryptic ? " checked" : "")%> value="0">0 <%=PageFlowUtil.helpPopup("0 tryptic ends", "All peptides will be shown, regardless of tryptic ends")%>
                                    <input type="radio" name="tryptic"<%=(1 == bean.tryptic ? " checked" : "")%> value="1">1 <%=PageFlowUtil.helpPopup("1 tryptic end", "At least one end of the peptide must be tryptic")%>
                                    <input type="radio" name="tryptic"<%=(2 == bean.tryptic ? " checked" : "")%> value="2">2 <%=PageFlowUtil.helpPopup("2 tryptic ends", "Both ends of the peptide must be tryptic")%>
                                </td>
                                <td nowrap style="vertical-align: middle;"><input type="image" id="AddTrypticEndsFilterButton" value="Add Filter" src="<%=PageFlowUtil.buttonSrc("Go")%>" name=""></td>
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
</form>
<script type="text/javascript">
    var viewTypeInfo = new Object();
    var count = 0;<%

    for(MS2RunViewType viewType : bean.viewTypes)
    { %>
        viewTypeInfo[count++] = <%=viewType.supportsExpansion()%>;<%
    } %>
</script>
