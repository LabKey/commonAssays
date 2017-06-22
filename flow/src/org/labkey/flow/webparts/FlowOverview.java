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

package org.labkey.flow.webparts;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusUrls;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.query.QueryAction;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.Overview;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.query.FlowTableType;

public class FlowOverview extends Overview
{
    boolean _hasPipelineRoot;
    boolean _canSetPipelineRoot;
    boolean _canInsert;
    boolean _canUpdate;
    boolean _canCreateFolder;
    int _fcsFileCount;
    int _fcsRunCount;
    int _fcsRealRunCount;
    int _fcsAnalysisCount;
    int _fcsAnalysisRunCount;
    int _compensationMatrixCount;
    int _compensationRunCount;
    FlowScript[] _scripts;
    FlowScript _scriptCompensation;
    FlowScript _scriptAnalysis;
    FlowProtocol _protocol;
    boolean _requiresCompensation;
    boolean _compensationSeparateStep;

    public FlowOverview(User user, Container container) throws Exception
    {
        super(user, container);
        PipelineService pipeService = PipelineService.get();
        PipeRoot pipeRoot = pipeService.findPipelineRoot(getContainer());
        _hasPipelineRoot = pipeRoot != null;
        _canSetPipelineRoot = hasPermission(AdminOperationsPermission.class);
        _canInsert = hasPermission(InsertPermission.class);
        _canUpdate = hasPermission(UpdatePermission.class);
        _canCreateFolder = getContainer().getParent() != null && !getContainer().getParent().isRoot() &&
                getContainer().getParent().hasPermission(getUser(), AdminPermission.class);

        _fcsFileCount = FlowManager.get().getFCSFileCount(user, getContainer());
        _fcsRunCount = FlowManager.get().getFCSFileOnlyRunsCount(user, getContainer());
        _fcsRealRunCount = FlowManager.get().getFCSRunCount(getContainer());
        _fcsAnalysisCount = FlowManager.get().getObjectCount(getContainer(), ObjectType.fcsAnalysis);
        _fcsAnalysisRunCount = FlowManager.get().getRunCount(getContainer(), ObjectType.fcsAnalysis);
        _compensationMatrixCount = FlowManager.get().getObjectCount(getContainer(), ObjectType.compensationMatrix);
        _compensationRunCount = FlowManager.get().getRunCount(getContainer(), ObjectType.compensationControl);
        _scripts = FlowScript.getAnalysisScripts(getContainer());
        _protocol = FlowProtocol.getForContainer(getContainer());

        for (FlowScript script : _scripts)
        {
            if (script.requiresCompensationMatrix(FlowProtocolStep.analysis))
            {
                _requiresCompensation = true;
            }
            if (script.hasStep(FlowProtocolStep.analysis))
            {
                if (_scriptAnalysis == null || script.hasStep(FlowProtocolStep.calculateCompensation) && !_scriptAnalysis.hasStep(FlowProtocolStep.calculateCompensation))
                {
                    _scriptAnalysis = script;
                }
                if (!script.hasStep(FlowProtocolStep.calculateCompensation))
                {
                    _compensationSeparateStep = true;
                }
            }

            if (script.hasStep(FlowProtocolStep.calculateCompensation))
            {
                if (_scriptCompensation == null)
                {
                    _scriptCompensation = script;
                }
                if (!script.hasStep(FlowProtocolStep.analysis))
                {
                    _compensationSeparateStep = true;
                }
            }

            // this loop is very expensive, break if there is nothing more to learn...
            if (_requiresCompensation && null != _scriptAnalysis && _compensationSeparateStep && null != _scriptCompensation)
                break;
        }

        addStep(getFCSFileStep());
        addStep(getAnalysisScriptStep());
        addStep(getCompensationMatrixStep());
        addStep(getAnalyzeStep());

        if (_canUpdate)
        {
            addStep(getSamplesStep());

            int jobCount = PipelineService.get().getQueuedStatusFiles(getContainer()).size();
            if (jobCount != 0)
            {
                ActionURL runningJobsURL = PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlBegin(getContainer(), true);
                Action action = new Action("Show jobs", runningJobsURL);
                action.setDescriptionHTML("There are " + jobCount + " jobs running in this folder.");
                addAction(action);
            }
        }

        if (_hasPipelineRoot && _canCreateFolder && _protocol != null)
        {
            Action action = new Action("Create new folder", new ActionURL(FlowController.NewFolderAction.class, getContainer()));
            action.setDescriptionHTML("<i>If you want to analyze a new set of experiment runs with a different protocol, you should create a new folder to do this work in. You can copy some of the settings from this folder.</i>");
            addAction(action);
        }
    }

    private Action getPipelineRootAction()
    {
        if (_fcsFileCount != 0 && _hasPipelineRoot) return null;
        if (_hasPipelineRoot && !_canSetPipelineRoot) return null;
        StringBuilder description = new StringBuilder("The pipeline root tells " + FlowModule.getLongProductName() + " where in the file system FCS files are permitted to be loaded from.");
        if (_canSetPipelineRoot)
        {
            ActionURL urlPipelineRoot = PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(getContainer());
            if (_hasPipelineRoot)
            {
                Action ret = new Action("Change pipeline root", urlPipelineRoot);
                ret.setDescriptionHTML(description.toString());
                return ret;
            }
            description.append("<br>The pipeline root must be set for this folder before any FCS files can be loaded.");
            Action ret = new Action("Set pipeline root", urlPipelineRoot);
            ret.setDescriptionHTML(description.toString());
            return ret;
        }
        else
        {
            Action ret = new Action("Contact your administrator to set the pipeline root.", null);
            description.append("<br>The pipeline root has not been set for this folder.");
            ret.setDescriptionHTML(description.toString());
            return ret;
        }
    }

    private Action getBrowseForFCSFilesAction()
    {
        if (!_hasPipelineRoot || !_canInsert) return null;
        ActionURL urlImportFCSFiles = PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(getContainer(), null);
        return new Action(_fcsFileCount == 0 ? "Browse for FCS files to be imported" : "Browse for more FCS files to be imported", urlImportFCSFiles);
    }

    private Action getImportFlowJoAnalysisAction()
    {
        if (!_canInsert) return null;
        ActionURL urlImportAnalysis = new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, getContainer());
        Action ret = new Action("Import FlowJo Workspace Analysis", urlImportAnalysis);
        ret.setExplanatoryHTML("You can also import statistics that have been calculated by FlowJo");
        return ret;
    }

    private Step getFCSFileStep()
    {
        Step ret = new Step("Import FCS Files", _fcsFileCount == 0 ? Step.Status.required : Step.Status.normal);
        if (_fcsFileCount != 0)
        {
            StringBuilder status = new StringBuilder();
            ActionURL urlShowFCSFiles = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery)
                    .addParameter("query.Original~eq", "true");
            status.append("<a href=\"").append(h(urlShowFCSFiles)).append("\">").append(_fcsFileCount).append(" FCS files</a> have been imported.");
            ActionURL urlShowRuns = new ActionURL(RunController.ShowRunsAction.class, getContainer())
                    .addParameter("query.FCSFileCount~neq", 0)
                    .addParameter("query.ProtocolStep~eq", "Keywords");
            if (_fcsRunCount == 1)
            {
                status.append(" These are in <a href=\"").append(h(urlShowRuns)).append("\">1 run</a>.");
            }
            else
            {
                status.append(" These are in <a href=\"").append(h(urlShowRuns)).append("\">").append(_fcsRunCount).append(" runs</a>.");
            }
            ret.setStatusHTML(status.toString());
        }
        else
        {
            ret.setStatusHTML(" No FCS files have been imported yet.");
        }
        ret.addAction(getBrowseForFCSFilesAction());
        ret.addAction(getImportFlowJoAnalysisAction());
        ret.addAction(getPipelineRootAction());
        return ret;
    }

    private Step getAnalysisScriptStep()
    {
        Step.Status status = Step.Status.normal;
        if (_scriptAnalysis != null)
        {
            status = Step.Status.normal;
        }
        else
        {
            if (_fcsRealRunCount == 0)
            {
                status = Step.Status.disabled;
            }
        }
        Step ret = new Step("Create Analysis Script", status);
        ret.setExplanatoryHTML("An analysis script tells " + FlowModule.getLongProductName() + " how to calculate the compensation matrix, what gates to apply, " + "statistics to calculate, and graphs to draw.");
        if (_scripts.length != 0)
        {
            if (_scripts.length == 1)
            {
                FlowScript script = _scripts[0];
                StringBuilder statusHTML = new StringBuilder("This folder contains one analysis script named <a href=\"" + h(script.urlShow()) + "\">'" + h(script.getName()) + "'</a>");
                if (script.hasStep(FlowProtocolStep.calculateCompensation) && script.hasStep(FlowProtocolStep.analysis))
                {
                    statusHTML.append(" It defines both a compensation calculation and an analysis.");
                }
                else if (script.hasStep(FlowProtocolStep.calculateCompensation))
                {
                    statusHTML.append(" It defines just a compensation calculation.");
                }
                else if (script.hasStep(FlowProtocolStep.analysis))
                {
                    statusHTML.append(" It defines an analysis.");
                    if (script.requiresCompensationMatrix(FlowProtocolStep.analysis))
                    {
                        statusHTML.append(" It requires a compensation matrix.");
                    }
                }
                else
                {
                    statusHTML.append(" It is a blank script.  You need to edit it before it can be used.");
                }
                ret.setStatusHTML(statusHTML.toString());
            }
            else
            {
                String statusHTML = "This folder contains <a href=\"" + h(FlowTableType.AnalysisScripts.urlFor(getUser(), getContainer(), QueryAction.executeQuery)) + "\">" + _scripts.length + " analysis scripts</a>.";
                ret.setStatusHTML(statusHTML);
            }
        }
        if (_canInsert)
            ret.addAction(new Action("Create a new Analysis script", new ActionURL(ScriptController.NewProtocolAction.class, getContainer())));
        return ret;
    }

    private Step getCompensationMatrixStep()
    {
        Step.Status status;
        if (!_requiresCompensation)
        {
            status = Step.Status.disabled;
        }
        else
        {
            if (_compensationSeparateStep)
            {
                if (_compensationMatrixCount == 0)
                    status = Step.Status.required;
                else
                    status = Step.Status.normal;
            }
            else
            {
                status = Step.Status.optional;
            }
        }
        Step ret = new Step("Provide Compensation Matrices", status);
        StringBuilder statusHTML = new StringBuilder();
        if (!_requiresCompensation)
        {
            if (_scripts.length != 0)
            {
                statusHTML.append("None of the Analysis Scripts in this folder require a compensation matrix.");
            }
        }
        else
        {
            if (!_compensationSeparateStep)
            {
                statusHTML.append("The Analysis Scripts in this folder define their own compensation calculation.  It is not necessary to calculate the compensation matrix in a separate step.");
            }
            else if (_scriptCompensation == null)
            {
                statusHTML.append("None of the analysis scripts define a compensation calculation.  Compensation matrices may be exported from FlowJo and uploaded.");
            }
        }
        if (_compensationMatrixCount != 0)
        {
            if (statusHTML.length() != 0)
            {
                statusHTML.append("<br>");
            }
            ActionURL urlFlowComp = FlowTableType.CompensationMatrices.urlFor(getUser(), getContainer(), QueryAction.executeQuery);
            statusHTML.append("There are <a href=\"").append(h(urlFlowComp.getLocalURIString())).append("\">").append(_compensationMatrixCount).append(" compensation matrices</a>.");
            if (_compensationRunCount != 0)
            {
                ActionURL urlShowRuns = new ActionURL(RunController.ShowRunsAction.class, getContainer()).addParameter("query.CompensationControlCount~neq", 0);
                if (_compensationRunCount == 1)
                    statusHTML.append(" These have been calculated in <a href=\"").append(h(urlShowRuns)).append("\"> 1 run</a>.");
                else
                    statusHTML.append(" These have been calculated in <a href=\"").append(h(urlShowRuns)).append("\">").append(_compensationRunCount).append(" runs</a>.");
            }
        }
        ret.setStatusHTML(statusHTML.toString());
        if (_canInsert)
        {
            if (_scriptCompensation != null)
            {
                ret.addAction(new Action("Calculate compensation matrices", _scriptCompensation.urlFor(AnalysisScriptController.ChooseRunsToAnalyzeAction.class, FlowProtocolStep.calculateCompensation)));
            }
            ret.addAction(new Action("Upload a compensation matrix", new ActionURL(CompensationController.UploadAction.class, getContainer())));
        }
        return ret;
    }

    private Step getAnalyzeStep()
    {
        Step.Status stepStatus = null;
        StringBuilder statusHTML = new StringBuilder();
        if (_scriptAnalysis == null)
        {
            statusHTML.append("There are no analysis scripts.<br>");
            stepStatus = Step.Status.disabled;
        }
        else if (!_scriptAnalysis.hasStep(FlowProtocolStep.calculateCompensation) && _scriptAnalysis.requiresCompensationMatrix(FlowProtocolStep.analysis))
        {
            if (_compensationMatrixCount == 0)
            {
                statusHTML.append("There are no compensation matrices to be used.<br>");
                stepStatus = Step.Status.disabled;
            }
        }
        if (stepStatus == null)
        {
            if (_fcsAnalysisCount == 0)
            {
                stepStatus = Step.Status.required;
            }
        }
        if (_fcsAnalysisCount != 0)
        {
            stepStatus = Step.Status.normal;
        }
        
        Step ret = new Step("Calculate statistics and generate graphs", stepStatus);
        if (statusHTML.length() == 0 || _fcsAnalysisCount != 0)
        {
            if (_fcsAnalysisCount == 0)
            {
                if (statusHTML.length() == 0)
                    statusHTML.append("No FCS files have been analyzed");
            }
            else
            {
                ActionURL urlShowRuns = new ActionURL(RunController.ShowRunsAction.class, getContainer()).addParameter("query.FCSAnalysisCount~neq", 0);
                statusHTML.append("<a href=\"").append(h(FlowTableType.FCSAnalyses.urlFor(getUser(), getContainer(), QueryAction.executeQuery))).append("\">").append(_fcsAnalysisCount).append(" FCS files</a>");
                if (_fcsAnalysisRunCount == 1)
                    statusHTML.append(" have been analyzed in " + "<a href=\"").append(h(urlShowRuns)).append("\"> 1 run</a>.");
                else
                    statusHTML.append(" have been analyzed in " + "<a href=\"").append(h(urlShowRuns)).append("\">").append(_fcsAnalysisRunCount).append(" runs</a>.");
            }
        }
        ret.setStatusHTML(statusHTML.toString());
        if (_canUpdate && _scriptAnalysis != null)
        {
            ret.addAction(new Action("Choose runs to analyze", _scriptAnalysis.urlFor(AnalysisScriptController.ChooseRunsToAnalyzeAction.class)));
        }
        return ret;
    }

    private Step getSamplesStep()
    {
        FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
        Step.Status status = protocol == null ? Step.Status.disabled : Step.Status.optional;
        Step ret = new Step("Assign additional meanings to keywords", status);
        if (protocol != null)
        {
            ExpSampleSet ss = protocol.getSampleSet();
            if (ss != null)
            {
                StringBuilder sb = new StringBuilder();
                sb.append("There are <a href=\"").append(h(protocol.urlShowSamples(false))).append("\">").append(ss.getSamples(getContainer()).size()).append(" sample descriptions</a> in this folder.");

                ret.setStatusHTML(sb.toString());

                if (_canUpdate)
                {
                    Action uploadAction = new Action("Upload More Samples", protocol.urlUploadSamples(true));
                    ret.addAction(uploadAction);
                    
                    if (protocol.getSampleSetJoinFields().size() != 0)
                    {
                        Action action = new Action("Modify sample description join fields", protocol.urlFor(ProtocolController.JoinSampleSetAction.class));
                        action.setDescriptionHTML("<i>The sample descriptions are linked to the FCS files using keywords.  When new samples are added or FCS files are loaded, new links will be created.</i>");
                        ret.addAction(action);
                    }
                    else
                    {
                        Action action = new Action("Define sample description join fields", protocol.urlFor(ProtocolController.JoinSampleSetAction.class));
                        action.setDescriptionHTML("You can specify how these sample descriptions should be linked to FCS files.");
                        ret.addAction(action);
                    }
                }
            }
            else if (_canUpdate)
            {
                Action action = new Action("Upload Sample Descriptions", protocol.urlUploadSamples(false));
                action.setDescriptionHTML("<i>Additional information about groups of FCS files can be uploaded in spreadsheet, and associated with the FCS files using keywords.</i>");
                ret.addAction(action);
            }

            ret.addAction(new Action("Other settings", protocol.urlShow()));
        }
        return ret;
    }
}
