package org.labkey.flow.webparts;

import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.data.Container;
import org.labkey.api.data.CompareType;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineJobData;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.Overview;
import org.labkey.api.query.QueryAction;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.controllers.FlowModule;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.util.PFUtil;

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
        _hasPipelineRoot = pipeRoot != null && pipeRoot.getUri(container) != null;
        _canSetPipelineRoot = isGlobalAdmin() && (pipeRoot == null || getContainer().equals(pipeRoot.getContainer()));
        _canInsert = hasPermission(ACL.PERM_INSERT);
        _canUpdate = hasPermission(ACL.PERM_UPDATE);
        _canCreateFolder = getContainer().getParent() != null && !getContainer().getParent().isRoot() &&
                getContainer().getParent().hasPermission(getUser(), ACL.PERM_ADMIN);

        _fcsFileCount = FlowManager.get().getObjectCount(getContainer(), ObjectType.fcsKeywords);
        _fcsRunCount = FlowManager.get().getRunCount(getContainer(), ObjectType.fcsKeywords);
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
        }

        addStep(getFCSFileStep());
        addStep(getAnalysisScriptStep());
        addStep(getCompensationMatrixStep());
        addStep(getAnalyzeStep());
        addStep(getSamplesStep());
        if (_canUpdate)
        {
            PipelineJobData jobData = PipelineService.get().getPipelineQueue().getJobData(getContainer());
            int jobCount = jobData.getPendingJobs().size() + jobData.getRunningJobs().size();
            if (jobCount != 0)
            {
                Action action = new Action("Show jobs", PFUtil.urlFor(FlowController.Action.showJobs, getContainer()));
                action.setDescriptionHTML("There are " + jobCount + " jobs running in this folder.");
                addAction(action);
            }
        }
        if (_hasPipelineRoot && _canCreateFolder && _protocol != null)
        {
            Action action = new Action("Create new folder", PFUtil.urlFor(FlowController.Action.newFolder, getContainer()));
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
            ViewURLHelper urlPipelineRoot = new ViewURLHelper("Pipeline", "setup", getContainer());
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
        ViewURLHelper urlUploadFCSFiles = new ViewURLHelper("Pipeline", "browse", getContainer());
        return new Action(_fcsFileCount == 0 ? "Browse for FCS files to be loaded" : "Browse for more FCS files to be loaded", urlUploadFCSFiles);
    }

    private Action getUploadFlowJoAnalysisAction()
    {
        ViewURLHelper urlUploadFlowJoAnalysis = getContainer().urlFor(AnalysisScriptController.Action.showUploadWorkspace);
        Action ret = new Action("Upload FlowJo Workspace", urlUploadFlowJoAnalysis);
        ret.setExplanatoryHTML("You can also upload results that have been calculated in FlowJo");
        return ret;
    }

    private Step getFCSFileStep()
    {
        Step ret = new Step("Load FCS Files", _fcsFileCount == 0 ? Step.Status.required : Step.Status.normal);
        if (_fcsFileCount != 0)
        {
            StringBuilder status = new StringBuilder();
            ViewURLHelper urlShowFCSFiles = FlowTableType.FCSFiles.urlFor(getContainer(), QueryAction.executeQuery);
            status.append("<a href=\"" + h(urlShowFCSFiles) + "\">" + _fcsFileCount + " FCS files</a> have been loaded.");
            ViewURLHelper urlShowRuns = FlowTableType.Runs.urlFor(getContainer(), "FCSFileCount", CompareType.NEQ, 0);
            if (_fcsRunCount == 1)
            {
                status.append(" These are in <a href=\"" + h(urlShowRuns) + "\">1 run</a>");
            }
            else
            {
                status.append(" These are in <a href=\"" + h(urlShowRuns) + "\">" + _fcsRunCount + " runs</a>.");
            }
            ret.setStatusHTML(status.toString());
        }
        ret.addAction(getBrowseForFCSFilesAction());
        ret.addAction(getUploadFlowJoAnalysisAction());
        ret.addAction(getPipelineRootAction());
        return ret;
    }

    private Step getAnalysisScriptStep()
    {
        Step.Status status;
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
            else
            {
                status = Step.Status.required;
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
                String statusHTML = "This folder contains <a href=\"" + h(FlowTableType.AnalysisScripts.urlFor(getContainer(), QueryAction.executeQuery)) + "\">" + _scripts.length + " analysis scripts</a>.";
                ret.setStatusHTML(statusHTML);
            }
        }
        ret.addAction(new Action("Create a new Analysis script", PFUtil.urlFor(ScriptController.Action.newProtocol, getContainer())));
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
            statusHTML.append("There are <a href=\"" + h(FlowTableType.CompensationMatrices.urlFor(getContainer(), QueryAction.executeQuery)) + "\">" + _compensationMatrixCount + " compensation matrices</a>.");
            if (_compensationRunCount != 0)
            {
                statusHTML.append(" These have been calculated in <a href=\"" + h(FlowTableType.Runs.urlFor(getContainer(), "CompensationControlCount", CompareType.NEQ, 0)) + "\">" + _compensationRunCount + " runs</a>.");
            }
        }
        ret.setStatusHTML(statusHTML.toString());
        if (_scriptCompensation != null)
        {
            ret.addAction(new Action("Calculate compensation matrices", _scriptCompensation.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze)));
        }
        ret.addAction(new Action("Upload a compensation matrix", PFUtil.urlFor(CompensationController.Action.begin, getContainer())));
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
                ViewURLHelper urlShowRuns = FlowTableType.Runs.urlFor(getContainer(), "FCSAnalysisCount", CompareType.NEQ, 0);
                statusHTML.append("<a href=\"" + h(FlowTableType.FCSAnalyses.urlFor(getContainer(), QueryAction.executeQuery)) + "\">" + _fcsAnalysisCount + " FCS files</a> have been analyzed in " + "<a href=\"" + h(urlShowRuns) + "\">" + _fcsAnalysisRunCount + " runs</a>.");
            }
        }
        ret.setStatusHTML(statusHTML.toString());
        if (_scriptAnalysis != null)
        {
            ret.addAction(new Action("Choose runs to analyze", _scriptAnalysis.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze)));
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
            StringBuilder descriptionHTML = new StringBuilder();
            ExpSampleSet ss = protocol.getSampleSet();
            if (ss != null)
            {
                descriptionHTML.append("There are <a href=\"" + h(ss.detailsURL()) + "\">" + ss.getSamples().length + " sample descriptions</a> in this folder.");
                if (_canUpdate)
                {
                    if (protocol.getSampleSetJoinFields().size() != 0)
                    {
                        Action action = new Action("Modify sample description join fields", protocol.urlFor(ProtocolController.Action.joinSampleSet));
                        descriptionHTML.append("<br><i>The sample descriptions are linked to the FCS files using some keywords.  When new samples are added or FCS files are loaded, new links will be created.</i>");
                        action.setDescriptionHTML(descriptionHTML.toString());
                        ret.addAction(action);
                    }
                    else
                    {
                        Action action = new Action("Define sample description join fields", protocol.urlFor(ProtocolController.Action.joinSampleSet));
                        descriptionHTML.append("<br>You can specify how these sample descriptions should be linked to FCS files.");
                        action.setDescriptionHTML(descriptionHTML.toString());
                        ret.addAction(action);
                    }
                }
            }
            else if (_canUpdate)
            {
                Action action = new Action("Upload Sample Descriptions", protocol.urlUploadSamples());
                action.setDescriptionHTML("<i>Additional information about groups of FCS files can be uploaded in spreadsheet, and associated with the FCS files using keywords.</i>");
                ret.addAction(action);
            }
            ret.addAction(new Action("Other settings", protocol.urlShow()));
        }
        return ret;
    }
}
