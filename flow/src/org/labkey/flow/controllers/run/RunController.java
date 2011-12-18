/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.RequiresLogin;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.template.PageConfig;
import org.labkey.api.writer.VirtualFile;
import org.labkey.api.writer.ZipFile;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSAnalysis;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.script.MoveRunFromWorkspaceJob;
import org.labkey.flow.view.ExportAnalysisForm;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RunController extends BaseFlowController
{
    private static final Logger _log = Logger.getLogger(RunController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(RunController.class);

    public RunController() throws Exception
    {
        setActionResolver(_actionResolver);
    }


    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm runForm, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(RunController.ShowRunAction.class));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ShowRunAction extends SimpleViewAction<RunForm>
    {
        FlowRun run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            run = form.getRun();
            if (run == null)
            {
                throw new NotFoundException("Run not found: " + PageFlowUtil.filter(form.getRunId()));
            }
            return new JspView<RunForm>(RunController.class, "showRun.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? null : "Run not found";
            return appendFlowNavTrail(getPageConfig(), root, run, label);
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
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


    @RequiresLogin
    @RequiresPermissionClass(ReadPermission.class)
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
                URI uri = well.getFCSURI();
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
                ZipFile zipFile = new ZipFile(response, _run.getName() + ".zip");
                exportFCSFiles(zipFile, _run, form.getEventCount() == null ? 0 : form.getEventCount());

                return null;
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Download Run");
            return root;
        }
    }

    protected void exportFCSFiles(VirtualFile dir, FlowRun run, int eventCount)
            throws Exception
    {
        FlowWell[] wells = run.getWells(true);
        exportFCSFiles(dir, Arrays.asList(wells), eventCount);
    }

    protected void exportFCSFiles(VirtualFile dir, Collection<FlowWell> wells, int eventCount)
            throws Exception
    {
        // 118754: The list of wells may contain both FlowFCSFile and FlowFCSAnalysis wells representing the same FCS file URI.
        // Keep track of which ones we've already seen during export.
        Set<String> seen = new HashSet<String>();
        byte[] buffer = new byte[524288];
        for (FlowWell well : wells)
        {
            URI uri = well.getFCSURI();
            if (uri == null)
                continue;

            File file = new File(uri);
            if (file.canRead())
            {
                String fileName = file.getName();
                if (!seen.contains(fileName))
                {
                    seen.add(fileName);
                    OutputStream os = dir.getOutputStream(fileName);
                    InputStream is;
                    if (eventCount == 0)
                    {
                        is = new FileInputStream(file);
                    }
                    else
                    {
                        is = new ByteArrayInputStream(new FCS(file).getFCSBytes(file, eventCount));
                    }
                    int cb;
                    while((cb = is.read(buffer)) > 0)
                    {
                        os.write(buffer, 0, cb);
                    }
                    os.close();
                }
            }
        }
    }

    @RequiresLogin
    @RequiresPermissionClass(ReadPermission.class)
    public class ExportAnalysis extends FormViewAction<ExportAnalysisForm>
    {
        List<FlowRun> _runs = null;
        List<FlowWell> _wells = null;
        boolean _success = false;

        @Override
        public void validateCommand(ExportAnalysisForm form, Errors errors)
        {
            int[] runId = form.getRunId();
            int[] wellId = form.getWellId();

            // If no run or well IDs were in the request, check for selected rows.
            if ((runId == null || runId.length == 0) && (wellId == null || wellId.length == 0))
            {
                Set<String> selection = DataRegionSelection.getSelected(getViewContext(), form.getDataRegionSelectionKey(), true, true);
                if (form.getSelectionType() == null || form.getSelectionType().equals("runs"))
                    runId = PageFlowUtil.toInts(selection);
                else
                    wellId = PageFlowUtil.toInts(selection);
            }

            if (runId != null && runId.length > 0)
            {
                List<FlowRun> runs = new ArrayList<FlowRun>();
                for (int id : runId)
                {
                    FlowRun run = FlowRun.fromRunId(id);
                    if (run == null)
                        throw new NotFoundException("Flow run not found");

                    runs.add(run);
                }
                _runs = runs;
            }
            else if (wellId != null && wellId.length > 0)
            {
                List<FlowWell> wells = new ArrayList<FlowWell>();
                for (int id : wellId)
                {
                    FlowWell well = FlowWell.fromWellId(id);
                    if (well == null)
                        throw new NotFoundException("Flow well not found");

                    wells.add(well);
                }
                _wells = wells;
            }
            else
            {
                throw new NotFoundException("Flow run or well ids required");
            }
        }

        @Override
        public ModelAndView getView(ExportAnalysisForm form, boolean reshow, BindException errors) throws Exception
        {
            if (_success)
            {
                return null;
            }
            else
            {
                form._renderForm = true;
                return new JspView<ExportAnalysisForm>("/org/labkey/flow/view/exportAnalysis.jsp", form, errors);
            }
        }

        String getBaseName(String runName)
        {
            return FileUtil.getBaseName(runName);
        }

        @Override
        public boolean handlePost(ExportAnalysisForm form, BindException errors) throws Exception
        {
            final String fcsDirName = "FCSFiles";
            final HttpServletResponse response = getViewContext().getResponse();

            if (_runs != null && _runs.size() > 0)
            {
                String zipName = "ExportedRuns.zip";
                if (_runs.size() == 1)
                {
                    FlowRun run = _runs.get(0);
                    zipName = getBaseName(run.getName()) + ".zip";
                }

                ZipFile zipFile = new ZipFile(response, zipName);
                for (FlowRun run : _runs)
                {
                    Map<String, AttributeSet> keywords = new TreeMap<String, AttributeSet>();
                    Map<String, AttributeSet> analysis = new TreeMap<String, AttributeSet>();
                    Map<String, CompensationMatrix> matrices = new TreeMap<String, CompensationMatrix>();
                    getAnalysis(Arrays.asList(run.getWells()), keywords, analysis, matrices, form.isIncludeKeywords(), form.isIncludeGraphs(), form.isIncludeCompensation(), form.isIncludeStatistics());

                    String dirName = getBaseName(run.getName());
                    VirtualFile dir = zipFile.getDir(dirName);
                    AnalysisSerializer writer = new AnalysisSerializer(_log, dir);
                    writer.writeAnalysis(keywords, analysis, matrices, EnumSet.of(form.getExportFormat()));

                    if (form.isIncludeFCSFiles())
                    {
                        exportFCSFiles(dir.getDir(fcsDirName), run, 0);
                    }
                }
                zipFile.close();
            }
            else if (_wells != null && _wells.size() > 0)
            {
                String zipName = "ExportedWells.zip";
                if (_wells.size() == 1)
                {
                    FlowWell well = _wells.get(0);
                    zipName = getBaseName(well.getName()) + ".zip";
                }

                Map<String, AttributeSet> keywords = new TreeMap<String, AttributeSet>();
                Map<String, AttributeSet> analysis = new TreeMap<String, AttributeSet>();
                Map<String, CompensationMatrix> matrices = new TreeMap<String, CompensationMatrix>();
                getAnalysis(_wells, keywords, analysis, matrices, form.isIncludeKeywords(), form.isIncludeGraphs(), form.isIncludeCompensation(), form.isIncludeStatistics());

                ZipFile zipFile = new ZipFile(response, zipName);
                AnalysisSerializer writer = new AnalysisSerializer(_log, zipFile);
                writer.writeAnalysis(keywords, analysis, matrices, EnumSet.of(form.getExportFormat()));

                if (form.isIncludeFCSFiles())
                {
                    exportFCSFiles(zipFile.getDir(fcsDirName), _wells, 0);
                }

                zipFile.close();
            }

            return _success = true;
        }

        @Override
        public URLHelper getSuccessURL(ExportAnalysisForm exportAnalysisForm)
        {
            return null;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, null, "Export Analysis");
        }

    }

    public void getAnalysis(List<FlowWell> wells,
                            Map<String, AttributeSet> keywordAttrs,
                            Map<String, AttributeSet> analysisAttrs,
                            Map<String, CompensationMatrix> matrices,
                            boolean includeKeywords, boolean includeGraphBytes, boolean includeCompMatrices, boolean includeStatistics)
            throws SQLException
    {
        for (FlowWell well : wells)
        {
            String name = well.getName();
            if (well instanceof FlowFCSAnalysis && (includeStatistics || includeGraphBytes))
            {
                FlowFCSAnalysis analysis = (FlowFCSAnalysis) well;
                AttributeSet attrs = analysis.getAttributeSet(includeGraphBytes);
                analysisAttrs.put(name, attrs);
            }

            if (includeKeywords)
            {
                FlowFCSFile file = well.getFCSFile();
                AttributeSet attrs = file.getAttributeSet();
                keywordAttrs.put(name, attrs);
            }

            if (includeCompMatrices)
            {
                FlowCompensationMatrix flowCompMatrix = well.getCompensationMatrix();
                if (flowCompMatrix != null)
                {
                    CompensationMatrix matrix = flowCompMatrix.getCompensationMatrix();
                    if (matrix != null)
                        matrices.put(name, matrix);
                }
            }
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
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
            return form.getRun().urlFor(ScriptController.GateEditorAction.class);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? "Move '" + run.getLabel() + "' to the workspace" : "Run not found";
            return appendFlowNavTrail(getPageConfig(), root, run, label);
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
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
            MoveRunFromWorkspaceJob job = new MoveRunFromWorkspaceJob(getViewBackgroundInfo(), experiment, run, PipelineService.get().findPipelineRoot(getContainer()));
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
            return appendFlowNavTrail(getPageConfig(), root, run, label);
        }
    }


    public static class AttachmentForm extends RunForm
    {
        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        private String name;
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class DownloadImageAction extends SimpleViewAction<AttachmentForm>
    {
        public ModelAndView getView(final AttachmentForm form, BindException errors) throws Exception
        {
            final FlowRun run = form.getRun();
            if (null == run)
            {
                throw new NotFoundException();
            }

            getPageConfig().setTemplate(PageConfig.Template.None);

            return new HttpView()
            {
                protected void renderInternal(Object model, HttpServletRequest request, HttpServletResponse response) throws Exception
                {
                    AttachmentService.get().download(response, run, form.getName());
                }
            };
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

}
