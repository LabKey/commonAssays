/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
import org.labkey.api.exp.api.ExpSampleType;
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
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.Overview;
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

        if (_canUpdate)
        {
            addStep(getSamplesStep());

            int jobCount = PipelineService.get().getQueuedStatusFiles(getContainer()).size();
            if (jobCount != 0)
            {
                ActionURL runningJobsURL = PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlBegin(getContainer(), true);
                Action action = new Action("Show jobs", runningJobsURL);
                action.setDescriptionHTML(HtmlString.of("There are " + jobCount + " jobs running in this folder."));
                addAction(action);
            }
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
        ret.setExplanatoryHTML(HtmlString.of("You can also import statistics that have been calculated by FlowJo"));
        return ret;
    }

    private Step getFCSFileStep()
    {
        Step ret = new Step("Import FCS Files", _fcsFileCount == 0 ? Step.Status.required : Step.Status.normal);
        if (_fcsFileCount != 0)
        {
            HtmlStringBuilder status = HtmlStringBuilder.of();
            ActionURL urlShowFCSFiles = FlowTableType.FCSFiles.urlFor(getUser(), getContainer(), QueryAction.executeQuery)
                    .addParameter("query.Original~eq", "true");
            status.unsafeAppend("<a href=\"").append(urlShowFCSFiles.toString()).unsafeAppend("\">").append(_fcsFileCount).append(" FCS files</a> have been imported.");
            ActionURL urlShowRuns = RunController.ShowRunsAction.getFcsFileRunsURL(getContainer());
            if (_fcsRunCount == 1)
            {
                status.unsafeAppend(" These are in <a href=\"").append(urlShowRuns.toString()).unsafeAppend("\">1 run</a>.");
            }
            else
            {
                status.unsafeAppend(" These are in <a href=\"").append(urlShowRuns.toString()).unsafeAppend("\">").append(_fcsRunCount).unsafeAppend(" runs</a>.");
            }
            ret.setStatusHTML(status.getHtmlString());
        }
        else
        {
            ret.setStatusHTML(HtmlString.of(" No FCS files have been imported yet."));
        }
        ret.addAction(getBrowseForFCSFilesAction());
        ret.addAction(getImportFlowJoAnalysisAction());
        return ret;
    }

    private Step getSamplesStep()
    {
        FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
        Step.Status status = protocol == null ? Step.Status.disabled : Step.Status.optional;
        Step ret = new Step("Assign additional meanings to keywords", status);
        if (protocol != null)
        {
            ExpSampleType st = protocol.getSampleType(getUser());
            if (st != null)
            {
                HtmlStringBuilder sb = HtmlStringBuilder.of();
                sb.unsafeAppend("There are <a href=\"").append(protocol.urlShowSamples().toString()).unsafeAppend("\">").append(protocol.getSamples(st, getUser()).size()).unsafeAppend(" sample descriptions</a> in this folder.");

                ret.setStatusHTML(sb.getHtmlString());

                if (_canUpdate)
                {
                    Action uploadAction = new Action("Upload More Samples", protocol.urlUploadSamples());
                    ret.addAction(uploadAction);
                    
                    if (!protocol.getSampleTypeJoinFields().isEmpty())
                    {
                        Action action = new Action("Modify sample description join fields", protocol.urlFor(ProtocolController.JoinSampleTypeAction.class));
                        action.setDescriptionHTML(HtmlString.unsafe("<i>The sample descriptions are linked to the FCS files using keywords.  When new samples are added or FCS files are loaded, new links will be created.</i>"));
                        ret.addAction(action);
                    }
                    else
                    {
                        Action action = new Action("Define sample description join fields", protocol.urlFor(ProtocolController.JoinSampleTypeAction.class));
                        action.setDescriptionHTML(HtmlString.of("You can specify how these sample descriptions should be linked to FCS files."));
                        ret.addAction(action);
                    }
                }
            }
            else if (_canUpdate)
            {
                Action action = new Action("Upload Sample Descriptions", protocol.urlCreateSampleType());
                action.setDescriptionHTML(HtmlString.unsafe("<i>Additional information about groups of FCS files can be uploaded in spreadsheet, and associated with the FCS files using keywords.</i>"));
                ret.addAction(action);
            }

            ret.addAction(new Action("Other settings", protocol.urlShow()));
        }
        return ret;
    }
}
