<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.nab.NabController"%>
<%@ page import="org.labkey.nab.SampleInfo"%>
<%@ page import="org.labkey.nab.RunSettings"%>
<%@ page import="org.labkey.api.study.PlateTemplate" %>
<%@ page import="org.labkey.api.study.WellGroup" %>
<%@ page import="org.labkey.api.view.ViewURLHelper" %>
<%@ page import="org.labkey.api.study.PlateService" %>
<%@ page import="org.labkey.api.study.WellGroupTemplate" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.UploadAssayForm> me = (JspView<NabController.UploadAssayForm>) HttpView.currentView();
    org.labkey.nab.NabController.UploadAssayForm form = me.getModel();
    String headerTDStyle = "text-align:left;background-color:#EEEEEE;border-top:solid 1px";
    String dataTDStyle = "padding-left:20px";
    PlateTemplate activeTemplate = me.getModel().getActivePlateTemplate(me.getViewContext().getContainer(), me.getViewContext().getUser());
    PlateTemplate[] templates = me.getModel().getPlateTemplates(me.getViewContext().getContainer(), me.getViewContext().getUser());
    if (activeTemplate == null)
        activeTemplate = templates[0];
    int specimenCount = activeTemplate.getWellGroupCount(WellGroup.Type.SPECIMEN);
    List<? extends WellGroupTemplate> wellGroupTemplates = activeTemplate.getWellGroups();
    List<WellGroupTemplate> specimenWellGroups = new ArrayList<WellGroupTemplate>();
    for (WellGroupTemplate groupTemplate : wellGroupTemplates)
    {
        if (groupTemplate.getType() == WellGroup.Type.SPECIMEN)
            specimenWellGroups.add(groupTemplate);
    }

    ViewURLHelper choosePlateURL = new ViewURLHelper("Plate", "plateTemplateList", me.getViewContext().getContainer());

    String errorParameter = request.getParameter("error");
    if (errorParameter != null)
    {
        out.write("<span class=\"labkey-error\">");
        out.write(errorParameter);
        out.write("</span>");
    }
%>
<script type="text/javascript">
    function changeVisibility(elementPrefix, visible)
    {
        for (var sampId = 1; sampId < <%= specimenCount %>; sampId++)
        {
            var elem = document.getElementById(elementPrefix + sampId);
            elem.style.display = (visible ? "block" : "none");
        }
        copyImpliedValue(elementPrefix);
    }

    function copyImpliedValues()
    {
        copyImpliedValue("initialDilutionText");
        copyImpliedValue("methodName");
        copyImpliedValue("factor");
        return true;
    }

    function copyImpliedValue(prefix)
    {
        var checkbox = document.getElementById(prefix + "Check");
        if (!checkbox.checked)
            return;
        var copiedValue = document.getElementById(prefix + "0").value;
        for (var i = 1; i < <%= specimenCount %>; i++)
            document.getElementById(prefix + i).value = copiedValue;
    }

    function toggleFileProperties(disabled)
    {
        document.getElementById("experimentDate").disabled = disabled;
        document.getElementById("fileId").disabled = disabled;
    }
</script>
<form method="post" onSubmit="return copyImpliedValues()" action="upload.view" enctype="multipart/form-data" class="normal">

<table>
    <tr>
        <th style="<%= headerTDStyle %>">Plate Data</th>
    </tr>
    <tr>
        <td>
            <table style="<%= dataTDStyle %>">
                <tr>
                    <td align=top>
                        Plate template:<br><select name="plateTemplate" onChange="document.location='begin.view?plateTemplate=' + escape(this.options[this.selectedIndex].value)">
                        <%
                            for (PlateTemplate current : templates)
                            {
                        %>
                            <option value="<%= h(current.getName()) %>" <%= activeTemplate.getName().equals(current.getName()) ? "SELECTED" : ""%>>
                            <%= h(current.getName()) %></option>
                        <%
                            }
                        %>
                        </select> <%= textLink("edit templates", choosePlateURL)%>
                    </td>
                </tr>
                <tr>
                    <td align=top>
                        Data file:<br>
                        <input type="file" size="40" name="dataFile" value="<%= h(form.getFileName()) %>">
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <th style="<%= headerTDStyle %>">Sample Properties</th>
    </tr>
    <tr>
        <td style="<%= dataTDStyle %>">
            <table>
                <tr>
                    <td colspan="2">
                        <table>
                            <tr>
                                <td class="ms-vh" style="text-align:center;vertical-align:bottom">
                                    Short Sample Id</td>
                                <td class="ms-vh" style="text-align:center;vertical-align:bottom">
                                    Sample Description</td>
                                <td class="ms-vh" style="text-align:center;vertical-align:bottom">
                                    Initial Dilution/<br>
                                    Concentration</td>
                                <td class="ms-vh" style="text-align:center;vertical-align:bottom">
                                    Method</td>
                                <td class="ms-vh" style="text-align:center;vertical-align:bottom">
                                    Factor</td>
                            </tr>
                            <%
                                for (int sampId = 0; sampId < specimenCount; sampId++)
                                {
                                    SampleInfo sampleInfo = form.getSampleInfos()[sampId];
                                    String sampleId = sampleInfo.getSampleId();
                                    if (sampleId == null)
                                        sampleId = specimenWellGroups.get(sampId).getName();
                            %>
                            <tr>
                                <td>
                                    <input size="12" name="sampleInfos[<%= sampId %>].sampleId"
                                           value="<%= h(sampleId) %>">
                                </td>
                                <td>
                                    <input size="40" name="sampleInfos[<%= sampId %>].sampleDescription"
                                           value="<%= h(sampleInfo.getSampleDescription()) %>">
                                </td>
                                <td class="ms-vb">
                                    <input id="initialDilutionText<%= sampId %>"
                                           size="8"
                                           name="sampleInfos[<%= sampId %>].initialDilutionText"
                                           value="<%= h(sampleInfo.getInitialDilutionText()) %>"
                                           style="display:<%= sampId > 0 && form.getRunSettings().isSameInitialValue() ? "none" : "block" %>">
                                </td>
                                <td class="ms-vb">
                                    <select id="methodName<%= sampId %>"
                                            name="sampleInfos[<%= sampId %>].methodName"
                                            style="display:<%= sampId > 0 && form.getRunSettings().isSameMethod() ? "none" : "block" %>">
                                        <option value="<%= SampleInfo.Method.Dilution.name() %>"
                                                <%= SampleInfo.Method.Dilution == sampleInfo.getMethod() ? "SELECTED" : "" %>>Dilution</option>
                                        <option value="<%= SampleInfo.Method.Concentration.name() %>"
                                                <%= SampleInfo.Method.Concentration == sampleInfo.getMethod() ? "SELECTED" : "" %>>Concentration</option>
                                    </select>
                                </td>
                                <td class="ms-vb">
                                    <input size="8" id="factor<%= sampId %>"
                                           name="sampleInfos[<%= sampId %>].factor"
                                           value="<%= sampleInfo.getFactor() %>"
                                           style="display:<%= sampId > 0 && form.getRunSettings().isSameFactor() ? "none" : "block" %>">
                                </td>
                            </tr>
                            <%
                                }
                            %>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td>
                        <input type="checkbox"
                               id="initialDilutionTextCheck"
                               name="runSettings.sameInitialValue"
                               onClick="changeVisibility('initialDilutionText', !this.checked);"
                               <%= form.getRunSettings().isSameInitialValue() ? "CHECKED" : ""%>>
                        Use same initial concentration/dilution for all samples<br>
                        <input type="checkbox"
                               id="methodNameCheck"
                               name="runSettings.sameMethod"
                               onClick="changeVisibility('methodName', !this.checked);"
                               <%= form.getRunSettings().isSameMethod() ? "CHECKED" : ""%>>
                               Use same method (concentration or dilution) for all samples<br>
                        <input type="checkbox"
                               id="factorCheck"
                               name="runSettings.sameFactor"
                               onClick="changeVisibility('factor', !this.checked);"
                               <%= form.getRunSettings().isSameFactor() ? "CHECKED" : ""%>>
                               Use same concentration/dilution factor for all samples<br>
                    </td>
                    <td>Desired cutoff percentages:<br>
                    <%
                    for (int i = 0; i < RunSettings.MAX_CUTOFF_OPTIONS; i++)
                    {
                    %>
                        <input type="text" id="cutoff<%= i %>" name="runSettings.cutoffs[<%= i %>].text"
                               size="10" value="<%= h(form.getRunSettings().getCutoffs()[i].getText()) %>">
                    <%
                        }
                    %>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <th style="<%= headerTDStyle %>">Experiment/Virus Properties</th>
    </tr>
    <tr>
        <td style="<%= dataTDStyle %>">
            <%
                String labelStyle = "text-align:right;vertical-align:middle";
            %>
            <table>
                <tr>
                    <td class="ms-vb" style="<%= labelStyle %>">Virus Name</td>
                    <td><input type="text" name="metadata.virusName" size="35"
                               value="<%= h(form.getMetadata().getVirusName()) %>"></td>
                    <td class="ms-vb" style="<%= labelStyle %>">Virus ID</td>
                    <td><input type="text" name="metadata.virusId" size="35"
                               value="<%= h(form.getMetadata().getVirusId()) %>"></td>
                </tr>
                <tr>
                    <td class="ms-vb" style="<%= labelStyle %>">Host Cell</td>
                    <td><input type="text" name="metadata.hostCell" size="35"
                               value="<%= h(form.getMetadata().getHostCell()) %>"></td>
                    <td class="ms-vb" style="<%= labelStyle %>">Study Name</td>
                    <td><input type="text" name="metadata.studyName" size="35"
                               value="<%= h(form.getMetadata().getStudyName()) %>"></td>
                </tr>
                <tr>
                    <td class="ms-vb" style="<%= labelStyle %>">Experiment Performer</td>
                    <td><input type="text" name="metadata.experimentPerformer" size="35"
                               value="<%= h(form.getMetadata().getExperimentPerformer()) %>"></td>
                    <td class="ms-vb" style="<%= labelStyle %>">Experiment ID</td>
                    <td><input type="text" name="metadata.experimentId" size="35"
                               value="<%= h(form.getMetadata().getExperimentId()) %>"></td>
                </tr>
                <tr>
                    <td class="ms-vb" style="<%= labelStyle %>">Incubation Time</td>
                    <td><input type="text" name="metadata.incubationTime" size="35"
                               value="<%= h(form.getMetadata().getIncubationTime()) %>"></td>
                    <td colspan="2">&nbsp;</td>
                </tr>
                <tr>
                    <td class="ms-vb" style="<%= labelStyle %>">Plate Number</td>
                    <td><input type="text" id="plateNumber" name="metadata.plateNumber" size="35"
                               value="<%= h(form.getMetadata().getPlateNumber()) %>">
                    </td>
                    <td colspan="2" align="center"><input type="checkbox" name="runSettings.inferFromFile"
                               onClick="toggleFileProperties(this.checked)"
                               <%= form.getRunSettings().isInferFromFile() ? "CHECKED" : "" %>>
                        Infer properties from file name</td>
                </tr>
                <tr>
                    <td class="ms-vb" style="<%= labelStyle %>">Experiment Date</td>
                    <td><input type="text" id="experimentDate" name="metadata.experimentDateString"
                               size="35"
                               value="<%= form.getRunSettings().isInferFromFile() ? "" : h(form.getMetadata().getExperimentDateString()) %>"  <%= form.getRunSettings().isInferFromFile() ? "DISABLED" : "" %>>
                    </td>
                    <td class="ms-vb" style="<%= labelStyle %>">File ID</td>
                    <td><input type="text" id="fileId" name="metadata.fileId" size="35"
                               value="<%= form.getRunSettings().isInferFromFile() ? "" : h(form.getMetadata().getFileId()) %>"  <%= form.getRunSettings().isInferFromFile() ? "DISABLED" : "" %>>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <th style="<%= headerTDStyle %>">Hill-Slope Curve Properties</th>
    </tr>
    <tr>
        <td style="<%= dataTDStyle %>">
            <table>
                <tr>
                    <td class="ms-vh" style="vertical-align:middle" align="right">Slope</td>
                    <td>
                        <input id="slopeTextBox" name="runSettings.slopeText" type="text" size="3" value="<%= form.getRunSettings().getSlopeText() %>"
                            <%= form.getRunSettings().isAutoSlope() ? "DISABLED" : "" %>>
                        <input type="checkbox" name="runSettings.autoSlope" value="true"
                               onclick="document.getElementById('slopeTextBox').disabled = this.checked;"
                               <%= form.getRunSettings().isAutoSlope() ? "CHECKED" : "" %>>
                        Auto-calculate
                    </td>
                    <td>
                        <input type="checkbox" name="runSettings.endpointsOptional"
                               value="true" <%= form.getRunSettings().isEndpointsOptional() ? "CHECKED" : "" %>>
                        Ignore first and/or last concentration if fit measure improves
                    </td>

                </tr>
            </table>
        </td>
    </tr>
</table>
    <%= buttonImg("Calculate") %> <%= buttonLink("Reset Form", "begin.view?reset=true") %>

</form>