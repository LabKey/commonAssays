/*
 * Copyright (c) 2006-2016 LabKey Corporation
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
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.RequiresLogin;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.FileNameUniquifier;
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
import org.labkey.flow.analysis.web.FCSAnalyzer;
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

public class RunController extends BaseFlowController
{
    private static final Logger _log = Logger.getLogger(RunController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(RunController.class);

    public RunController()
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

    @RequiresPermission(ReadPermission.class)
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

            run.checkContainer(getContainer(), getUser(), getActionURL());

            return new JspView<>(RunController.class, "showRun.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? null : "Run not found";
            return appendFlowNavTrail(getPageConfig(), root, run, label);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowRunsAction extends SimpleViewAction<RunsForm>
    {
        FlowExperiment experiment;
//        FlowScript script;

        public ModelAndView getView(RunsForm form, BindException errors) throws Exception
        {
            experiment = form.getExperiment();
//            script = form.getScript();

            checkContainer(experiment);

            return new JspView<>(RunController.class, "showRuns.jsp", form, errors);
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
    @RequiresPermission(ReadPermission.class)
    public class DownloadAction extends SimpleViewAction<DownloadRunForm>
    {
        private FlowRun _run;
        private Map<String, File> _files = new TreeMap<>();
        private List<File> _missing = new LinkedList<>();

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
                return new JspView<>("/org/labkey/flow/controllers/run/download.jsp", new DownloadRunBean(_run, _files, _missing), errors);
            }
            else
            {
                HttpServletResponse response = getViewContext().getResponse();
                try (ZipFile zipFile = new ZipFile(response, _run.getName() + ".zip"))
                {
                    exportFCSFiles(zipFile, _run, form.getEventCount() == null ? 0 : form.getEventCount());
                }

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
        Set<String> seen = new HashSet<>();
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
                        is = new ByteArrayInputStream(FCSAnalyzer.get().getFCSBytes(uri, eventCount));
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
    @RequiresPermission(ReadPermission.class)
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
                List<FlowRun> runs = new ArrayList<>();
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
                Set<String> names = new HashSet<>();
                List<FlowWell> wells = new ArrayList<>();
                for (int id : wellId)
                {
                    FlowWell well = FlowWell.fromWellId(id);
                    if (well == null)
                        throw new NotFoundException("Flow well not found");

                    if (names.contains(well.getName()))
                    {
                        errors.rejectValue("wellId", ERROR_MSG, "Duplicate sample name '" + well.getName() + "'.  All exported sample well names must be unique.  Contact LabKey support if you see this error.");
                        return;
                    }
                    names.add(well.getName());

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
                if (errors.hasErrors())
                {
                    return new SimpleErrorView(errors);
                }

                form._renderForm = true;
                return new JspView<>("/org/labkey/flow/view/exportAnalysis.jsp", form, errors);
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

                // Uniquify run names if the same workspace has been imported twice and is now being exported.
                // CONSIDER: Unfortunately, the original run name will be lost -- consider adding a id column to the export format containing the lsid of the run.
                FileNameUniquifier uniquifier = new FileNameUniquifier(false);

                try (ZipFile zipFile = new ZipFile(response, zipName))
                {
                    for (FlowRun run : _runs)
                    {
                        Map<String, AttributeSet> keywords = new TreeMap<>();
                        Map<String, AttributeSet> analysis = new TreeMap<>();
                        Map<String, CompensationMatrix> matrices = new TreeMap<>();
                        getAnalysis(Arrays.asList(run.getWells()), keywords, analysis, matrices, form.isIncludeKeywords(), form.isIncludeGraphs(), form.isIncludeCompensation(), form.isIncludeStatistics());

                        String dirName = getBaseName(run.getName());
                        dirName = uniquifier.uniquify(dirName);

                        VirtualFile dir = zipFile.getDir(dirName);
                        AnalysisSerializer writer = new AnalysisSerializer(_log, dir);
                        writer.writeAnalysis(keywords, analysis, matrices, EnumSet.of(form.getExportFormat()));

                        if (form.isIncludeFCSFiles())
                        {
                            exportFCSFiles(dir.getDir(fcsDirName), run, 0);
                        }
                    }
                }
            }
            else if (_wells != null && _wells.size() > 0)
            {
                String zipName = "ExportedWells.zip";
                if (_wells.size() == 1)
                {
                    FlowWell well = _wells.get(0);
                    zipName = getBaseName(well.getName()) + ".zip";
                }

                Map<String, AttributeSet> keywords = new TreeMap<>();
                Map<String, AttributeSet> analysis = new TreeMap<>();
                Map<String, CompensationMatrix> matrices = new TreeMap<>();
                getAnalysis(_wells, keywords, analysis, matrices, form.isIncludeKeywords(), form.isIncludeGraphs(), form.isIncludeCompensation(), form.isIncludeStatistics());

                try (ZipFile zipFile = new ZipFile(response, zipName))
                {
                    AnalysisSerializer writer = new AnalysisSerializer(_log, zipFile);
                    writer.writeAnalysis(keywords, analysis, matrices, EnumSet.of(form.getExportFormat()));

                    if (form.isIncludeFCSFiles())
                    {
                        exportFCSFiles(zipFile.getDir(fcsDirName), _wells, 0);
                    }
                }
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
    {
        // CONSIDER: Uniquify well names if the same well has been imported into two different runs and is now being exported.
        // CONSIDER: Unfortunately, the original sample name will be lost -- consider adding a id column to the export format containing the lsid of the well.
        //FileNameUniquifier uniquifier = new FileNameUniquifier(false);
        for (FlowWell well : wells)
        {
            //String name = uniquifier.uniquify(FileUtil.getBaseName(well.getName()));
            String name = FileUtil.getBaseName(well.getName());

            if (well instanceof FlowFCSAnalysis && (includeStatistics || includeGraphBytes))
            {
                FlowFCSAnalysis analysis = (FlowFCSAnalysis) well;
                AttributeSet attrs = analysis.getAttributeSet(includeGraphBytes);
                analysisAttrs.put(name, attrs);
            }

            if (includeKeywords)
            {
                FlowFCSFile file = well.getFCSFileInput();
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

    @RequiresPermission(ReadPermission.class)
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
