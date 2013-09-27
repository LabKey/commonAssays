<%
/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
<%@ page import="org.labkey.api.study.PlateTemplate"%>
<%@ page import="org.labkey.api.study.WellGroup"%>
<%@ page import="org.labkey.api.study.WellGroupTemplate"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.nab.NabController" %>
<%@ page import="org.labkey.nab.RunSettings" %>
<%@ page import="org.labkey.api.assay.dilution.SampleInfo" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.study.assay.PlateUrls" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabController.UploadAssayForm> me = (JspView<NabController.UploadAssayForm>) HttpView.currentView();
    NabController.UploadAssayForm form = me.getModelBean();
    String dataTDStyle = "padding-left:20px";
    PlateTemplate activeTemplate = me.getModelBean().getActivePlateTemplate(me.getViewContext().getContainer(), me.getViewContext().getUser());
    PlateTemplate[] templates = me.getModelBean().getPlateTemplates(me.getViewContext().getContainer(), me.getViewContext().getUser());
    int specimenCount = activeTemplate.getWellGroupCount(WellGroup.Type.SPECIMEN);
    List<? extends WellGroupTemplate> wellGroupTemplates = activeTemplate.getWellGroups();
    List<WellGroupTemplate> specimenWellGroups = new ArrayList<>();
    for (WellGroupTemplate groupTemplate : wellGroupTemplates)
    {
        if (groupTemplate.getType() == WellGroup.Type.SPECIMEN)
            specimenWellGroups.add(groupTemplate);
    }

    ActionURL choosePlateURL = urlProvider(PlateUrls.class).getPlateTemplateListURL(me.getViewContext().getContainer());

    String errorParameter = request.getParameter("error");
    if (errorParameter != null)
    {
        out.write("<span class=\"labkey-error\">");
        out.write(errorParameter);
        out.write("</span>");
    }

    String messageId = "editRunProperies";
    Boolean showWarning = null != session && !Boolean.FALSE.equals(session.getAttribute(messageId));
    if (showWarning)
    {
%>
<div id="nab-msg-target" style="margin-bottom: 10px;"></div>
<%  } %>
<form method="post" onSubmit="return copyImpliedValues();" action="upload.view" enctype="multipart/form-data">

<table>
    <tr class="labkey-wp-header">
        <th>Plate Data</th>
    </tr>
    <tr>
        <td>
            <table style="<%= dataTDStyle %>">
                <tr>
                    <td align=top>
                        Plate template:<br><select name="plateTemplate" onChange="document.location='create.view?plateTemplate=' + escape(this.options[this.selectedIndex].value);">
                        <%
                            for (PlateTemplate current : templates)
                            {
                        %>
                            <option value="<%= h(current.getName()) %>"<%=selected(activeTemplate.getName().equals(current.getName()))%>>
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
    <tr class="labkey-wp-header">
        <th>Sample Properties</th>
    </tr>
    <tr>
        <td style="<%= dataTDStyle %>">
            <table>
                <tr>
                    <td colspan="2">
                        <table>
                            <tr>
                                <td class="labkey-header" style="text-align:center;vertical-align:bottom">
                                    Short Sample Id</td>
                                <td class="labkey-header" style="text-align:center;vertical-align:bottom">
                                    Sample Description</td>
                                <td class="labkey-header" style="text-align:center;vertical-align:bottom">
                                    Initial Dilution/<br>
                                    Concentration</td>
                                <td class="labkey-header" style="text-align:center;vertical-align:bottom">
                                    Method</td>
                                <td class="labkey-header" style="text-align:center;vertical-align:bottom">
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
                                <td>
                                    <input id="initialDilutionText<%= sampId %>"
                                           size="8"
                                           name="sampleInfos[<%= sampId %>].initialDilutionText"
                                           value="<%= h(sampleInfo.getInitialDilutionText()) %>"
                                           style="display:<%= sampId > 0 && form.getRunSettings().isSameInitialValue() ? "none" : "block" %>">
                                </td>
                                <td>
                                    <select id="methodName<%= sampId %>"
                                            name="sampleInfos[<%= sampId %>].methodName"
                                            style="display:<%= sampId > 0 && form.getRunSettings().isSameMethod() ? "none" : "block" %>">
                                        <option value="<%= SampleInfo.Method.Dilution.name() %>"
                                                <%=selected(SampleInfo.Method.Dilution == sampleInfo.getMethod())%>>Dilution</option>
                                        <option value="<%= SampleInfo.Method.Concentration.name() %>"
                                                <%=selected(SampleInfo.Method.Concentration == sampleInfo.getMethod())%>>Concentration</option>
                                    </select>
                                </td>
                                <td>
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
                               <%=checked(form.getRunSettings().isSameInitialValue())%>>
                        Use same initial concentration/dilution for all samples<br>
                        <input type="checkbox"
                               id="methodNameCheck"
                               name="runSettings.sameMethod"
                               onClick="changeVisibility('methodName', !this.checked);"
                               <%=checked(form.getRunSettings().isSameMethod())%>>
                               Use same method (concentration or dilution) for all samples<br>
                        <input type="checkbox"
                               id="factorCheck"
                               name="runSettings.sameFactor"
                               onClick="changeVisibility('factor', !this.checked);"
                               <%=checked(form.getRunSettings().isSameFactor())%>>
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
    <tr class="labkey-wp-header">
        <th>Experiment/Virus Properties</th>
    </tr>
    <tr>
        <td style="<%= dataTDStyle %>">
            <%
                String labelStyle = "text-align:right;vertical-align:middle";
            %>
            <table>
                <tr>
                    <td style="<%= labelStyle %>">Virus Name</td>
                    <td><input type="text" name="metadata.virusName" size="35"
                               value="<%= h(form.getMetadata().getVirusName()) %>"></td>
                    <td style="<%= labelStyle %>">Virus ID</td>
                    <td><input type="text" name="metadata.virusId" size="35"
                               value="<%= h(form.getMetadata().getVirusId()) %>"></td>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Host Cell</td>
                    <td><input type="text" name="metadata.hostCell" size="35"
                               value="<%= h(form.getMetadata().getHostCell()) %>"></td>
                    <td style="<%= labelStyle %>">Study Name</td>
                    <td><input type="text" name="metadata.studyName" size="35"
                               value="<%= h(form.getMetadata().getStudyName()) %>"></td>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Experiment Performer</td>
                    <td><input type="text" name="metadata.experimentPerformer" size="35"
                               value="<%= h(form.getMetadata().getExperimentPerformer()) %>"></td>
                    <td style="<%= labelStyle %>">Experiment ID</td>
                    <td><input type="text" name="metadata.experimentId" size="35"
                               value="<%= h(form.getMetadata().getExperimentId()) %>"></td>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Incubation Time</td>
                    <td><input type="text" name="metadata.incubationTime" size="35"
                               value="<%= h(form.getMetadata().getIncubationTime()) %>"></td>
                    <td colspan="2">&nbsp;</td>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Plate Number</td>
                    <td><input type="text" id="plateNumber" name="metadata.plateNumber" size="35"
                               value="<%= h(form.getMetadata().getPlateNumber()) %>">
                    </td>
                    <td colspan="2" align="center"><input type="checkbox" name="runSettings.inferFromFile"
                               onClick="toggleFileProperties(this.checked)"
                               <%=checked(form.getRunSettings().isInferFromFile())%>>
                        Infer properties from file name</td>
                </tr>
                <tr>
                    <td style="<%= labelStyle %>">Experiment Date</td>
                    <td><input type="text" id="experimentDate" name="metadata.experimentDateString"
                               size="35"
                               value="<%=h(form.getRunSettings().isInferFromFile() ? "" : form.getMetadata().getExperimentDateString())%>"<%=disabled(form.getRunSettings().isInferFromFile())%>>
                    </td>
                    <td style="<%= labelStyle %>">File ID</td>
                    <td><input type="text" id="fileId" name="metadata.fileId" size="35"
                               value="<%=h(form.getRunSettings().isInferFromFile() ? "" : form.getMetadata().getFileId())%>"<%=disabled(form.getRunSettings().isInferFromFile())%>>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
    <%= generateSubmitButton("Calculate") %> <%= generateButton("Reset Form", buildURL(NabController.BeginAction.class,"reset=true")) %>

</form>
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
<%
    if (showWarning)
    {
%>
<script type="text/javascript">

    Ext.onReady(function() {

        // Prototype for Messaging Component to show general page/site information
        LABKEY.MessageComponent = Ext.extend(Ext.BoxComponent, {

            initComponent : function() {

                Ext.applyIf(this, {
                    closable : true
                });

                LABKEY.MessageComponent.superclass.initComponent.call(this);
            },

            onRender : function(ct, position) {
                if (!this.template) {
                    if (!LABKEY.MessageComponent.msgTemplate) {
                        var tpl  = '<div class="labkey-warning-messages" style="{2}">';

                        // closeable icon
                        if (this.closable) {
                            tpl += '<img src="' + LABKEY.contextPath + '/_images/partdelete.gif" alt="x" style="float: right;cursor:pointer;">';
                        }

                        tpl     += '{1}</div>';

                        LABKEY.MessageComponent.msgTemplate = new Ext.Template(tpl);
                        LABKEY.MessageComponent.msgTemplate.compile();
                    }
                    this.template = LABKEY.MessageComponent.msgTemplate;
                }

                var targs = this.getTemplateArgs();

                if(position){
                    btn = this.template.insertBefore(position, targs, true);
                }else{
                    btn = this.template.append(ct, targs, true);
                }

                if (this.closable)
                    this.container.child('img').on('click', this.close, this);
            },

            getTemplateArgs : function() {
                return [this.type, this.messages, this.msgStyle]
            },

            close : function() {
                this.container.setDisplayed(false);
                Ext.Ajax.request({
                    method : 'GET',
                    url    : LABKEY.ActionURL.buildURL('user', 'setShowWarningMessages.api'),
                    params : {
                        action       : 'editRunProperies',
                        showMessages : false
                    }
                });
            }
        });

        var msgComponent = new LABKEY.MessageComponent({
            renderTo : 'nab-msg-target',
            messages : '<b>Note:</b> This NAb workflow was deprecated in version 11.3. Consider using the updated NAb assay tool. ' +
                       '<a href="https://www.labkey.org/wiki/home/Documentation/page.view?name=nabAssayTutorial" target="_blank">See&nbsp;Tutorial</a>',
            msgStyle : 'width: 764px; text-align: center;'
        });
    });
</script>
<% } %>