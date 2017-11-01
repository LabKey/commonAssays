<%
/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.security.permissions.ReadPermission" %>
<%@ page import="org.labkey.api.study.Study" %>
<%@ page import="org.labkey.api.study.assay.AssayPublishService" %>
<%@ page import="org.labkey.flow.FlowModule" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="org.labkey.flow.controllers.executescript.SelectedSamples" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.flow.data.FlowWell" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    Container container = getContainer();
    PipelineService pipeService = PipelineService.get();
    PipeRoot pipeRoot = pipeService.findPipelineRoot(container);
%>

<input type="hidden" name="selectFCSFilesOption" id="selectFCSFilesOption" value="<%=h(form.getSelectFCSFilesOption())%>">
<input type="hidden" name="existingKeywordRunId" id="existingKeywordRunId" value="<%=h(form.getExistingKeywordRunId())%>">
<% if (form.getKeywordDir() != null) for (String keywordDir : form.getKeywordDir()) { %>
<input type="hidden" name="keywordDir" value="<%=h(keywordDir)%>">
<% } %>
<input type="hidden" name="resolving" value="<%=form.isResolving()%>">
<input type="hidden" name="selectAnalysisEngine" id="selectAnalysisEngine" value="<%=h(form.getSelectAnalysisEngine())%>">

<%--
<% for (int i = 0; form.getEngineOptionFilterKeyword() != null && i < form.getEngineOptionFilterKeyword().length; i++) { %>
<input type="hidden" name="engineOptionFilterKeyword" value="<%=h(form.getEngineOptionFilterKeyword()[i])%>">
<input type="hidden" name="engineOptionFilterOp" value="<%=h(form.getEngineOptionFilterOp()[i])%>">
<input type="hidden" name="engineOptionFilterValue" value="<%=h(form.getEngineOptionFilterValue()[i])%>">
<% } %>
--%>

<input type="hidden" name="importGroupNames" value="<%=h(form.getImportGroupNames())%>"/>
<input type="hidden" name="rEngineNormalization" value="<%=h(form.isrEngineNormalization())%>"/>
<input type="hidden" name="rEngineNormalizationReference" value="<%=h(form.getrEngineNormalizationReference())%>"/>
<input type="hidden" name="rEngineNormalizationSubsets" value="<%=h(form.getrEngineNormalizationSubsets())%>"/>
<input type="hidden" name="rEngineNormalizationParameters" value="<%=h(form.getrEngineNormalizationParameters())%>"/>

<p>
<hr/>
<%
    FlowExperiment[] analyses = FlowExperiment.getAnalyses(container);
    FlowExperiment firstNonDisabledAnalysis = null;
    Map<Integer, String> disabledAnalyses = new HashMap<>();
    if (pipeRoot != null)
    {
        List<File> keywordDirs = new ArrayList<>();
        if (form.getKeywordDir() != null && form.getKeywordDir().length > 0)
        {
            for (String dir : form.getKeywordDir())
            {
                File keywordDir = pipeRoot.resolvePath(dir);
                if (keywordDir != null)
                    keywordDirs.add(keywordDir);
            }
        }
        else if (form.getExistingKeywordRunId() > 0)
        {
            FlowRun keywordsRun = FlowRun.fromRunId(form.getExistingKeywordRunId());
            keywordDirs.add(new File(keywordsRun.getPath()));
        }
        else if (form.getSelectedSamples().getRows() != null && !form.getSelectedSamples().getRows().isEmpty())
        {
            // Ugh. Need to include the file root for the resolved files.  For now, just take the run file path of the first run found.
            for (SelectedSamples.ResolvedSample resolved : form.getSelectedSamples().getRows().values())
            {
                if (resolved.isSelected() && resolved.hasMatchedFile())
                {
                    FlowWell fcsFile = FlowWell.fromWellId(resolved.getMatchedFile());
                    if (fcsFile != null)
                    {
                        FlowRun run = fcsFile.getRun();
                        File file = run.getExperimentRun().getFilePathRoot();
                        if (file != null)
                        {
                            keywordDirs.add(file);
                            break;
                        }
                    }
                }
            }
        }

        if (keywordDirs.size() > 0)
        {
            for (FlowExperiment analysis : analyses)
            {
                for (File keywordDir : keywordDirs)
                {
                    String relativeKeywordDir = pipeRoot.relativePath(keywordDir);
                    if (analysis.hasRun(keywordDir, null))
                        disabledAnalyses.put(analysis.getExperimentId(), "The '" + analysis.getName() + "' analysis folder already contains the FCS files from '" + relativeKeywordDir + "'.");
                    else if (firstNonDisabledAnalysis == null)
                        firstNonDisabledAnalysis = analysis;
                }
            }
        }
    }

    String newAnalysisName = form.getNewAnalysisName();
    if (StringUtils.isEmpty(newAnalysisName))
    {
        Set<String> namesInUse = new HashSet<>();
        for (FlowExperiment analysis : analyses)
            namesInUse.add(analysis.getName().toLowerCase());

        String baseName = FlowExperiment.DEFAULT_ANALYSIS_NAME;
        newAnalysisName = baseName;
        int nameIndex = 0;
        while (namesInUse.contains(newAnalysisName.toLowerCase()))
        {
            nameIndex++;
            newAnalysisName = baseName+nameIndex;
        }
    }
%>

<b>Analysis Folder</b>
<br>
<%=h(FlowModule.getLongProductName())%> organizes results into different "analysis folders".  The same set of FCS files may only
be analyzed once in a given analysis folder.  If you want to analyze the same FCS file in two different ways,
those results must be put into different analysis folders.
<br>
<div style="padding-left: 2em; padding-bottom: 1em;">
    <br>
    <% if (analyses.length == 0 || analyses.length == disabledAnalyses.size()) { %>
    What do you want to call the new analysis folder?  You will be able to use this name for multiple uploaded analyses.<br>
    <input type="text" name="newAnalysisName" value="<%=h(newAnalysisName)%>">
    <input type="hidden" name="createAnalysis" value="true">
    <br>
    <br>
    <% } else { %>
    <table>
        <tr>
            <td valign="top">
                <input type="radio" id="chooseExistingAnalysis" name="createAnalysis" value="false" checked>
            </td>
            <td>
                Choose an analysis folder to put the results into:<br>
                <select name="existingAnalysisId" onfocus="document.forms.importAnalysis.chooseExistingAnalysis.checked = true;">
                    <%
                        FlowExperiment recentAnalysis = FlowExperiment.getMostRecentAnalysis(container);
                        int selectedId = 0;
                        if (firstNonDisabledAnalysis != null)
                            selectedId = firstNonDisabledAnalysis.getExperimentId();
                        if (recentAnalysis != null && !disabledAnalyses.containsKey(recentAnalysis.getExperimentId()))
                            selectedId = recentAnalysis.getExperimentId();

                        for (FlowExperiment analysis : analyses)
                        {
                            String disabledReason = disabledAnalyses.get(analysis.getExperimentId());
                    %>
                    <option value="<%=h(analysis.getExperimentId())%>"
                            <%=selected(disabledReason == null && analysis.getExperimentId() == selectedId)%>
                            <%=text(disabledReason != null ? "disabled=\"disabled\" title=\"" + h(disabledReason) + "\"":"")%>>
                        <%=h(analysis.getName())%>
                    </option>
                    <%
                        }
                    %>
                </select>
                <br><br>
            </td>
        </tr>
        <tr>
            <td valign="top">
                <input type="radio" id="chooseNewAnalysis" name="createAnalysis" value="true">
            </td>
            <td>
                Create a new analysis folder:<br>
                <input type="text" name="newAnalysisName" value="<%=h(newAnalysisName)%>" onfocus="document.forms.importAnalysis.chooseNewAnalysis.checked = true;">
                <br><br>
            </td>
        </tr>
    </table>
<% } %>
</div>

<%
// Let user select a Target Study only if we are also importing a keywords directory.
if (form.getKeywordDir() != null && form.getKeywordDir().length > 0)
{
    // Get set of valid copy to study targets
    Set<Study> validStudies = AssayPublishService.get().getValidPublishTargets(getUser(), ReadPermission.class);
    Map<String, String> targetStudies = new LinkedHashMap<>();
    targetStudies.put("", "[None]");
    for (Study study : validStudies)
    {
        Container c = study.getContainer();
        targetStudies.put(c.getId(), c.getPath() + " (" + study.getLabel() + ")");
    }

    if (validStudies.size() > 0)
    {
        // Pre-select the most recent target study
        if (form.getTargetStudy() == null)
            form.setTargetStudy(FlowRun.findMostRecentTargetStudy(container));

        %>
        <b>Target Study</b>
        <br>
        <em>Optionally</em>, select a target study for the imported flow run.  If the flow metadata includes PTID and Date/Visit columns,
        specimen information from the study will be included in the FCSAnalyses table.
        <br>
        <div style="padding-left: 2em; padding-bottom: 1em;">
            <br>
            Choose a target study folder:<br>
            <select id="targetStudy" name="targetStudy">
                <labkey:options value="<%=text(form.getTargetStudy())%>" map="<%=targetStudies%>"/>
            </select>
            <br><br>
        </div>
        <%
    }
}
%>

