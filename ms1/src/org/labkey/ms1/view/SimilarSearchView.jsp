<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms1.model.SimilarSearchModel" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms1.MS1Controller" %>
<%
    JspView<SimilarSearchModel> me = (JspView<SimilarSearchModel>) HttpView.currentView();
    SimilarSearchModel model = me.getModelBean();
%>
<script type="text/javascript">
    function onTimeUnitsChange(units)
    {
        var txtTimeSource = document.getElementById("txtTimeSource");
        var lblTime = document.getElementById("lblTime");
        if(null == lblTime || null == txtTimeSource)
            return;

        if(units == "<%=MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans.name()%>")
        {
            lblTime.innerHTML = "<%=model.getTimeUnitsLabel(MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans)%>";
            txtTimeSource.value = "<%=PageFlowUtil.filter(model.getFeatureScan())%>";
        }
        else
        {
            lblTime.innerHTML = "<%=model.getTimeUnitsLabel(MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt)%>";
            txtTimeSource.value = "<%=model.formatTimeSource(model.getFeatureTime(), MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt)%>";
        }
    }
</script>
<form action="<%=model.getResultsUri()%>" method="get">
    <% if(model.getFeatureId() != null) { %>
    <input type="hidden" name="<%=MS1Controller.SimilarSearchForm.ParamNames.featureId.name()%>"
           value="<%=PageFlowUtil.filter(model.getFeatureId())%>"/>
    <% } %>
    <table border="0" width="100%">
        <tr>
            <td>
                <table border="0" cellspacing="0" cellpadding="3">
                    <tr>
                        <td>m/z</td>
                        <td>=</td>
                        <td>
                            <input type="text" name="<%=MS1Controller.SimilarSearchForm.ParamNames.mzSource.name()%>"
                                value="<%=model.formatMzSource()%>"/>
                        </td>
                        <td>&#177;<input name="<%=MS1Controller.SimilarSearchForm.ParamNames.mzOffset.name()%>"
                                         value="<%=PageFlowUtil.filter(model.getMzOffset())%>" size="7"/>
                            <select name="<%=MS1Controller.SimilarSearchForm.ParamNames.mzUnits.name()%>">
                                <option value="<%=MS1Controller.SimilarSearchForm.MzOffsetUnits.ppm.name()%>"
                                        <%=model.getMzUnits() == MS1Controller.SimilarSearchForm.MzOffsetUnits.ppm ? "selected=\"1\"" : ""%>
                                        >ppm</option>
                                <option value="<%=MS1Controller.SimilarSearchForm.MzOffsetUnits.mz.name()%>"
                                        <%=model.getMzUnits() == MS1Controller.SimilarSearchForm.MzOffsetUnits.mz ? "selected=\"1\"" : ""%>
                                        >m/z</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td style="text-align:center"><b>and</b></td>
                        <td colspan="3">&nbsp;</td>
                    </tr>
                    <tr>
                        <td>
                            <span id="lblTime"><%=model.getTimeUnitsLabel()%></span>
                        </td>
                        <td>=</td>
                        <td>
                            <input id="txtTimeSource" type="text" name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeSource.name()%>"
                                value="<%=model.formatTimeSource()%>"/>
                        </td>
                        <td>&#177;<input id="txtTimeOffset" 
                                         name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeOffset.name()%>"
                                         value="<%=PageFlowUtil.filter(model.getTimeOffset())%>" size="7"/>
                            <select name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeUnits.name()%>" onchange="onTimeUnitsChange(this.value);">
                                <option value="<%=MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt.name()%>"
                                        <%=model.getTimeUnits() == MS1Controller.SimilarSearchForm.TimeOffsetUnits.rt ? "selected=\"1\"" : ""%>
                                        >Seconds</option>
                                <option value="<%=MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans.name()%>"
                                        <%=model.getTimeUnits() == MS1Controller.SimilarSearchForm.TimeOffsetUnits.scans ? "selected=\"1\"" : ""%>
                                        >Scans</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>Search Subfolders</td>
                        <td>:</td>
                        <td><input id="cbxSubfolders" name="<%=MS1Controller.SimilarSearchForm.ParamNames.subfolders.name()%>"
                                   type="checkbox" style="vertical-align:middle"
                                    <%=model.searchSubfolders() ? "checked=\"1\"" : "" %>/>
                        </td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <td colspan="4" style="text-align:right">
                            <input name="submit" type="image" src="<%=PageFlowUtil.buttonSrc("Search")%>"/>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>


</form>