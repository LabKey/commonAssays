/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

package org.labkey.flow.controllers.run;

import org.apache.log4j.Logger;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.data.*;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.script.MoveRunFromWorkspaceJob;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RunController extends SpringFlowController<RunController.Action>
{
    static private final Logger _log = Logger.getLogger(RunController.class);
    public enum Action
    {
        begin,
        showRun,
        showRuns,
        showCompensation,
        export,
        details,
        download,
        moveToWorkspace,
        moveToAnalysis
    }

    static DefaultActionResolver _actionResolver = new DefaultActionResolver(RunController.class);

    public RunController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }


    @RequiresPermission(ACL.PERM_NONE)
    public class BeginAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm runForm, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(RunController.Action.showRun));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowRunAction extends SimpleViewAction<RunForm>
    {
        FlowRun run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            run = form.getRun();
            if (run == null)
                return new HtmlView("Run not found: " + PageFlowUtil.filter(form.getRunId()));
            return new JspView<RunForm>(RunController.class, "showRun.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? null : "Run not found";
            return appendFlowNavTrail(root, run, label, Action.showRun);
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowRunsAction extends SimpleViewAction<RunsForm>
    {
        FlowExperiment experiment;
//        FlowScript script;

        public ModelAndView getView(RunsForm form, BindException errors) throws Exception
        {
            experiment = form.getExperiment();
//            script = form.getScript();
            return new JspView<RunsForm>(RunController.class, "showRuns.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (experiment == null)
                root.addChild("All Analysis Folders", new ActionURL(ShowRunsAction.class, getContainer()));
            else
                root.addChild(experiment.getLabel(), experiment.urlShow());

//            if (script != null)
//                root.addChild(script.getLabel(), script.urlShow());

            root.addChild("Runs");
            return root;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class DownloadAction extends SimpleViewAction<DownloadRunForm>
    {
        private FlowRun _run;
        private Map<String, File> _files = new TreeMap<String, File>();
        private List<File> _missing = new LinkedList<File>();

        @Override
        public void validate(DownloadRunForm form, BindException errors)
        {
            _run = form.getRun();
            if (_run == null)
            {
                errors.reject(ERROR_MSG, "run not found");
                return;
            }

            FlowWell[] wells = _run.getWells(true);
            if (wells.length == 0)
            {
                errors.reject(ERROR_MSG, "no wells in run: " + _run.getName());
                return;
            }

            for (FlowWell well : wells)
            {
                URI uri = FlowAnalyzer.getFCSUri(well);
                File file = new File(uri);
                if (file.exists() && file.canRead())
                    _files.put(file.getName(), file);
                else
                    _missing.add(file);
            }

            if (_missing.size() > 0 && !form.isSkipMissing())
            {
                errors.reject(ERROR_MSG, "files missing from run: " + _run.getName());
            }
        }

        public ModelAndView getView(DownloadRunForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
            {
                return new JspView<DownloadRunBean>("/org/labkey/flow/controllers/run/download.jsp", new DownloadRunBean(_run, _files, _missing), errors);
            }
            else
            {
                HttpServletResponse response = getViewContext().getResponse();

                response.reset();
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + _run.getName() + ".zip\"");
                ZipOutputStream stream = new ZipOutputStream(response.getOutputStream());
                byte[] buffer = new byte[524288];
                for (File file : _files.values())
                {
                    if (file.canRead())
                    {
                        ZipEntry entry = new ZipEntry(file.getName());
                        stream.putNextEntry(entry);
                        InputStream is;
                        if (form.getEventCount() == null)
                        {
                            is = new FileInputStream(file);
                        }
                        else
                        {
                            is = new ByteArrayInputStream(new FCS(file).getFCSBytes(file, form.getEventCount().intValue()));
                        }
                        int cb;
                        while((cb = is.read(buffer)) > 0)
                        {
                            stream.write(buffer, 0, cb);
                        }
                    }
                }
                stream.close();

                return null;
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Download Run");
            return root;
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class MoveToWorkspaceAction extends FormViewAction<RunForm>
    {
        FlowRun run;

        public void validateCommand(RunForm target, Errors errors)
        {
        }

        public ModelAndView getView(RunForm form, boolean reshow, BindException errors) throws Exception
        {
            run = form.getRun();
            return new JspView<RunForm>(RunController.class, "moveToWorkspace.jsp", form, errors);
        }

        public boolean handlePost(RunForm form, BindException errors) throws Exception
        {
            FlowRun run = form.getRun();
            if (run.getStep() != FlowProtocolStep.analysis)
            {
                errors.reject(ERROR_MSG, "This run cannot be moved to the workspace because it is not the analysis step.");
                return false;
            }
            FlowExperiment workspace = FlowExperiment.ensureWorkspace(getUser(), getContainer());
            FlowRun[] existing = workspace.findRun(new File(run.getPath()), FlowProtocolStep.analysis);
            if (existing.length != 0)
            {
                errors.reject(ERROR_MSG, "This run cannot be moved to the workspace because the workspace already contains a run from this directory.");
                return false;
            }
            run.moveToWorkspace(getUser());
            return true;
        }

        public ActionURL getSuccessURL(RunForm form)
        {
            return form.getRun().urlFor(ScriptController.Action.gateEditor);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? "Move '" + run.getLabel() + "' to the workspace" : "Run not found";
            return appendFlowNavTrail(root, run, label, Action.moveToWorkspace);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class MoveToAnalysisAction extends FormViewAction<MoveToAnalysisForm>
    {
        FlowRun run;
        ActionURL successURL;

        public void validateCommand(MoveToAnalysisForm target, Errors errors)
        {
        }

        public ModelAndView getView(MoveToAnalysisForm form, boolean reshow, BindException errors) throws Exception
        {
            run = form.getRun();
            return new JspView<MoveToAnalysisForm>(RunController.class, "moveToAnalysis.jsp", form, errors);
        }

        public boolean handlePost(MoveToAnalysisForm form, BindException errors) throws Exception
        {
            FlowRun run = form.getRun();
            if (!run.isInWorkspace())
            {
                errors.reject(ERROR_MSG, "This run is not in the workspace");
                return false;
            }

            FlowExperiment experiment = FlowExperiment.fromExperimentId(form.getExperimentId());
            if (experiment.hasRun(new File(run.getPath()), FlowProtocolStep.analysis))
            {
                errors.reject(ERROR_MSG, "This run cannot be moved to this analysis because there is already a run there.");
                return false;
            }
            MoveRunFromWorkspaceJob job = new MoveRunFromWorkspaceJob(getViewBackgroundInfo(), experiment, run);
            successURL = executeScript(job);
            return true;
        }

        public ActionURL getSuccessURL(MoveToAnalysisForm form)
        {
            return successURL;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? "Move '" + run.getLabel() + "' to an analysis" : "Run not found";
            return appendFlowNavTrail(root, run, label, Action.moveToWorkspace);
        }
    }

}
