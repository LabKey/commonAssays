<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.ms1.model.SimilarSearchModel" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms1.MS1Controller" %>
<%
    JspView<SimilarSearchModel> me = (JspView<SimilarSearchModel>) HttpView.currentView();
    SimilarSearchModel model = me.getModelBean();
%>
<form action="<%=model.getResultsUri()%>" method="get">
    <input type="hidden" name="<%=MS1Controller.SimilarSearchForm.ParamNames.featureId.name()%>"
           value="<%=model.getFeature().getFeatureId()%>"/>
    <table border="0" width="100%">
        <tr class="wpHeader">
            <td colspan="4" class="wpTitle"><b>Find Features Where</b></td>
        </tr>
        <tr>
            <td>
                <table border="0" cellspacing="0" cellpadding="3">
                    <tr>
                        <td class="ms-searchform">m/z</td>
                        <td>=</td>
                        <td><%=model.formatNumber(model.getFeature().getMz())%></td>
                        <td>&#177;<input name="<%=MS1Controller.SimilarSearchForm.ParamNames.mzOffset.name()%>"
                                         value="<%=model.getMzOffset()%>" size="7"/>
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
                        <td class="ms-searchform" style="text-align:center"><b>and</b></td>
                        <td colspan="3">&nbsp;</td>
                    </tr>
                    <tr>
                        <td class="ms-searchform">Retention Time<br/>(Scan)</td>
                        <td>=</td>
                        <td><%=model.formatNumber(model.getFeature().getTime())%><br/>
                            (<%=model.getFeature().getScan()%>)
                        </td>
                        <td>&#177;<input name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeOffset.name()%>"
                                         value="<%=model.getTimeOffset()%>" size="7"/>
                            <select name="<%=MS1Controller.SimilarSearchForm.ParamNames.timeUnits.name()%>">
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
                        <td class="ms-searchform">Search Subfolders</td>
                        <td></td>
                        <td><input id="cbxSubfolders" name="<%=MS1Controller.SimilarSearchForm.ParamNames.subfolders.name()%>"
                                   type="checkbox" style="vertical-align:middle"
                                    <%=model.searchSubfolders() ? "checked=\"1\"" : "" %>/>
                        </td>
                        <td></td>
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