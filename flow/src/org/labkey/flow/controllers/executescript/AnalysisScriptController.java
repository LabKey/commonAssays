/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.controllers.executescript;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.*;
import org.labkey.common.util.Pair;
import org.labkey.flow.FlowPreference;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.*;
import org.labkey.flow.script.AddRunsJob;
import org.labkey.flow.script.AnalyzeJob;
import org.labkey.flow.script.FlowPipelineProvider;
import org.labkey.flow.script.WorkspaceJob;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

public class AnalysisScriptController extends SpringFlowController<AnalysisScriptController.Action>
{
    static Logger _log = Logger.getLogger(SpringActionController.class);

    public enum Action
    {
        begin,

        showUploadRuns,
        chooseRunsToUpload,
        uploadRuns,

        chooseRunsToAnalyze,
        chooseAnalysisName,
        analyzeSelectedRuns,
    }

    static SpringActionController.DefaultActionResolver _actionResolver = new SpringActionController.DefaultActionResolver(AnalysisScriptController.class);

    public AnalysisScriptController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        FlowScript script;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            script = FlowScript.fromURL(getActionURL(), getRequest());
            if (script == null)
            {
                return HttpView.redirect(getContainer().urlFor(FlowController.Action.begin));
            }
            FlowPreference.showRuns.updateValue(getRequest());
            return new JspView<FlowScript>(AnalysisScriptController.class, "showScript.jsp", script, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, script, null, Action.begin);
        }
    }

    protected Page getPage(String name) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        ret.setScript(getScript());
        return ret;
    }

    public abstract class BaseAnalyzeRunsAction extends SimpleViewAction<ChooseRunsToAnalyzeForm>
    {
        FlowScript script;
        Pair<String, Action> nav;

        protected ModelAndView chooseRunsToAnalyze(ChooseRunsToAnalyzeForm form, BindException errors)
        {
            nav = new Pair<String, Action>("Choose runs", Action.chooseRunsToAnalyze);
            form.populate(errors);
            return new JspView<ChooseRunsToAnalyzeForm>(AnalysisScriptController.class, "chooseRunsToAnalyze.jsp", form, errors);
        }

        protected ModelAndView chooseAnalysisName(ChooseRunsToAnalyzeForm form, BindException errors)
        {
            nav = new Pair<String, Action>("Choose new analysis name", Action.chooseAnalysisName);
            return new JspView<ChooseRunsToAnalyzeForm>(AnalysisScriptController.class, "chooseAnalysisName.jsp", form, errors);
        }

        protected ModelAndView analyzeRuns(ChooseRunsToAnalyzeForm form, BindException errors,
                                         int[] runIds, String experimentLSID)
                throws Exception
        {
            nav = new Pair<String, Action>(null, Action.analyzeSelectedRuns);
            DataRegionSelection.clearAll(getViewContext());

            FlowExperiment experiment = FlowExperiment.fromLSID(experimentLSID);
            String experimentName = form.ff_analysisName;
            if (experiment != null)
            {
                experimentName = experiment.getName();
            }
            FlowScript analysis = form.getProtocol();
            AnalyzeJob job = new AnalyzeJob(getViewBackgroundInfo(), experimentName, experimentLSID, FlowProtocol.ensureForContainer(getUser(), getContainer()), analysis, form.getProtocolStep(), runIds);
            if (form.getCompensationMatrixId() != 0)
            {
                job.setCompensationMatrix(FlowCompensationMatrix.fromCompId(form.getCompensationMatrixId()));
            }
            job.setCompensationExperimentLSID(form.getCompensationExperimentLSID());
            return HttpView.redirect(executeScript(job));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, script, nav.first, nav.second);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ChooseRunsToAnalyzeAction extends BaseAnalyzeRunsAction
    {
        FlowScript script;

        public ModelAndView getView(ChooseRunsToAnalyzeForm form, BindException errors) throws Exception
        {
            script = form.getProtocol();
            return chooseRunsToAnalyze(form, errors);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class AnalyzeSelectedRunsAction extends BaseAnalyzeRunsAction
    {
        public ModelAndView getView(ChooseRunsToAnalyzeForm form, BindException errors) throws Exception
        {
            script = form.getProtocol();
            int[] runIds = form.getSelectedRunIds();
            if (runIds.length == 0)
            {
                errors.reject(ERROR_MSG, "Please select at least one run to analyze.");
                return chooseRunsToAnalyze(form, errors);
            }
            String experimentLSID = form.getAnalysisLSID();
            if (experimentLSID == null)
            {
                return chooseAnalysisName(form, errors);
            }
            return analyzeRuns(form, errors, runIds, experimentLSID);
        }
    }

    protected Map<String, String> getNewPaths(ChooseRunsToUploadForm form, Errors errors) throws Exception
    {
        PipelineService service = PipelineService.get();
        PipeRoot root = service.findPipelineRoot(getContainer());
        if (root == null)
        {
            errors.reject(ERROR_MSG, "The pipeline root is not set.");
            return Collections.EMPTY_MAP;
        }

        String displayPath;
        if (StringUtils.isEmpty(form.path))
        {
            displayPath = "this directory";
        }
        else
        {
            displayPath = "'" + PageFlowUtil.decode(form.path) + "'";
        }

        URI uri = URIUtil.resolve(root.getUri(), form.path);
        if (null == uri)
        {
            errors.reject(ERROR_MSG, "The path " + displayPath + " is invalid.");
            return Collections.EMPTY_MAP;
        }
        File directory = new File(uri);
        if (!root.isUnderRoot(directory))
        {
            errors.reject(ERROR_MSG, "The path " + displayPath + " is invalid.");
            return Collections.EMPTY_MAP;
        }

        if (!directory.isDirectory())
        {
            errors.reject(ERROR_MSG, displayPath + " is not a directory.");
            return Collections.EMPTY_MAP;
        }
        List<File> files = new ArrayList<File>();
        files.add(directory);
        File[] dirFiles = directory.listFiles((java.io.FileFilter)DirectoryFileFilter.INSTANCE);
        if (dirFiles != null)
            files.addAll(Arrays.asList(dirFiles));

        Set<String> usedPaths = new HashSet<String>();
        for (FlowRun run : FlowRun.getRunsForContainer(getContainer(), FlowProtocolStep.keywords))
        {
            usedPaths.add(run.getExperimentRun().getFilePathRoot());
        }

        Map<String, String> ret = new TreeMap<String, String>();
        boolean anyFCSDirectories = false;
        for (File file : files)
        {
            File[] fcsFiles = file.listFiles((java.io.FileFilter)FCS.FCSFILTER);
            if (fcsFiles.length > 0)
            {
                anyFCSDirectories = true;
                if (!usedPaths.contains(file.toString()))
                {
                    String displayName;
                    if (file.equals(directory))
                    {
                        displayName = "This Directory (" + fcsFiles.length + " fcs files)";
                    }
                    else
                    {
                        displayName = file.getName() + " (" + fcsFiles.length + " fcs files)";
                    }
                    ret.put(URIUtil.relativize(root.getUri(), file.toURI()).toString(), displayName);
                }
            }
        }
        if (ret.isEmpty())
        {
            if (anyFCSDirectories)
            {
                errors.reject(ERROR_MSG, "All of the directories in " + displayPath + " have already been uploaded.");
            }
            else
            {
                errors.reject(ERROR_MSG, "No FCS files were found in " + displayPath + " or its children.");
            }
        }
        return ret;
    }

    public abstract class BaseUploadRunsAction extends SimpleViewAction<ChooseRunsToUploadForm>
    {
        Pair<String, Action> nav;

        protected ModelAndView chooseRunsToUpload(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            nav = new Pair<String, Action>("Choose Runs to Upload", Action.chooseRunsToUpload);
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            root.requiresPermission(getContainer(), getUser(), ACL.PERM_INSERT);

            JspView<ChooseRunsToUploadForm> view = new JspView<ChooseRunsToUploadForm>(AnalysisScriptController.class, "chooseRunsToUpload.jsp", form, errors);
            form.setNewPaths(getNewPaths(form, errors));
            form.setPipeRoot(root);
            return view;
        }

        protected ModelAndView uploadRuns(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            nav = new Pair<String, Action>(null, Action.uploadRuns);
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            root.requiresPermission(getContainer(), getUser(), ACL.PERM_INSERT);
            if (form.ff_path == null || form.ff_path.length == 0)
            {
                errors.reject(ERROR_MSG, "You did not select any runs.");
                return chooseRunsToUpload(form, errors);
            }
            List<File> paths = new ArrayList<File>();
            List<String> skippedPaths = new ArrayList<String>();
            for (String path : form.ff_path)
            {
                File file;
                if (path == null)
                {
                    file = root.getRootPath();
                }
                else
                {
                    file = new File(URIUtil.resolve(root.getUri(), path));
                }
                if (file == null)
                {
                    skippedPaths.add(path);
                    continue;
                }

                paths.add(file);
            }

            ViewBackgroundInfo vbi = getViewBackgroundInfo();
            AddRunsJob job = new AddRunsJob(vbi, FlowProtocol.ensureForContainer(getUser(), vbi.getContainer()), paths);
            for (String path : skippedPaths)
            {
                job.addStatus("Skipping path '" + path + "' because it is invalid.");
            }
            return HttpView.redirect(executeScript(job));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, null, nav.first, nav.second);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ChooseRunsToUploadAction extends BaseUploadRunsAction
    {
        public ModelAndView getView(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            return chooseRunsToUpload(form, errors);
        }
    }


    @RequiresPermission(ACL.PERM_INSERT)
    public class UploadRunsAction extends BaseUploadRunsAction
    {
        public ModelAndView getView(ChooseRunsToUploadForm form, BindException errors) throws Exception
        {
            return uploadRuns(form, errors);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ShowUploadRunsAction extends RedirectAction
    {
        public ActionURL getSuccessURL(Object o)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(getContainer(),
                FlowPipelineProvider.NAME);
        }

        public boolean doAction(Object o, BindException errors) throws Exception
        {
            return true;
        }

        public void validateCommand(Object target, Errors errors)
        {
        }
    }

    abstract static public class Page extends FlowPage
    {
        FlowScript _analysisScript;
        public void setScript(FlowScript script)
        {
            _analysisScript = script;
        }
        public FlowScript getScript()
        {
            return _analysisScript;
        }
    }

    public enum ImportAnalysisStep
    {
        INIT("Start"),
        UPLOAD_WORKSPACE("Upload Workspace"),
        ASSOCIATE_FCSFILES("Associate FCS Files"),
        CHOOSE_ANALYSIS("Choose Analysis Folder"),
        CONFIRM("Confirm");

        String title;

        ImportAnalysisStep(String title)
        {
            this.title = title;
        }

        public String getTitle()
        {
            return title;
        }

        public int getNumber()
        {
            return ordinal();
        }

        public static ImportAnalysisStep fromNumber(int number)
        {
            for (ImportAnalysisStep step : values())
                if (step.ordinal() == number)
                    return step;
            return INIT;
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class ImportAnalysisAction extends FormViewAction<ImportAnalysisForm>
    {
        String title;
        PipeRoot root;
        boolean foundRoot;

        public void validateCommand(ImportAnalysisForm form, Errors errors)
        {
            if (form.getWizardStep().getNumber() > ImportAnalysisStep.INIT.getNumber())
            {
                getWorkspace(form, errors);
                if (errors.hasErrors())
                    form.setWizardStep(ImportAnalysisStep.UPLOAD_WORKSPACE);
            }
        }

        public ModelAndView getView(ImportAnalysisForm form, boolean reshow, BindException errors) throws Exception
        {
            return new JspView<ImportAnalysisForm>(AnalysisScriptController.class, "importAnalysis.jsp", form, errors);
//            return new GroovyView<ImportAnalysisForm>("/org/labkey/flow/controllers/executescript/importAnalysis.jsp", form, errors);
        }

        public boolean handlePost(ImportAnalysisForm form, BindException errors) throws Exception
        {
            if (form.getWizardStep() == ImportAnalysisStep.INIT)
            {
                form.setWizardStep(ImportAnalysisStep.UPLOAD_WORKSPACE);
            }
            else
            {
                // wizard step is the last step shown to the user.
                // Handle the post and setup the form for the next wizard step.
                switch (form.getWizardStep())
                {
                    case UPLOAD_WORKSPACE:
                        stepUploadWorkspace(form, errors);
                        break;

                    case ASSOCIATE_FCSFILES:
                        stepAssociateFCSFiles(form, errors);
                        break;

                    case CHOOSE_ANALYSIS:
                        stepChooseAnalysis(form, errors);
                        break;

                    case CONFIRM:
                        stepConfirm(form, errors);
                        break;
                }
            }

            title = form.getWizardStep().getTitle();

            return false;
        }

        private PipeRoot getPipeRoot()
        {
            if (foundRoot)
                return root;
            foundRoot = true;
            try
            {
                root = PipelineService.get().findPipelineRoot(getContainer());
            }
            catch (SQLException e)
            {
                _log.error("resolving pipeline root", e);
            }
            return root;
        }

        // reads uploaded workspace.file or workspace.path from pipeline
        private void getWorkspace(ImportAnalysisForm form, Errors errors)
        {
            WorkspaceData workspace = form.getWorkspace();
            Map<String, MultipartFile> files = getFileMap();
            MultipartFile file = files.get("workspace.file");
            if (file != null)
                form.getWorkspace().setFile(file);
            workspace.validate(getContainer(), errors, getRequest());
        }

        private File getRunPathRoot(ImportAnalysisForm form, Errors errors) throws Exception
        {
            if (form.getRunFilePathRoot() != null)
            {
                PipeRoot root = getPipeRoot();
                File runFilePathRoot = root.resolvePath(form.getRunFilePathRoot());

                if (runFilePathRoot == null)
                {
                    errors.reject(ERROR_MSG, "The directory containing FCS files wasn't found.");
                    return null;
                }
                if (!runFilePathRoot.isDirectory())
                {
                    errors.reject(ERROR_MSG, "The path specified must be a directory containing FCS files.");
                    return null;
                }
                return runFilePathRoot;
            }
            return null;
        }

        private void stepUploadWorkspace(ImportAnalysisForm form, BindException errors) throws Exception
        {
            // guess the FCS files are in the same directory as the workspace
            if (form.getRunFilePathRoot() == null)
            {
                PipeRoot root = null;
                WorkspaceData workspaceData = form.getWorkspace();
                String path = workspaceData.getPath();
                File workspaceFile = null;
                if (path != null)
                {
                    root = getPipeRoot();
                    workspaceFile = root.resolvePath(path);
                }

                if (workspaceFile != null)
                {
                    FlowJoWorkspace workspace = workspaceData.getWorkspaceObject();
                    File runFilePathRoot = null;
                    for (FlowJoWorkspace.SampleInfo sampleInfo : workspace.getSamples())
                    {
                        File sampleFile = new File(workspaceFile.getParent(), sampleInfo.getLabel());
                        if (sampleFile.exists())
                        {
                            runFilePathRoot = workspaceFile.getParentFile();
                            break;
                        }
                    }

                    if (runFilePathRoot != null)
                    {
                        String relPath = root.relativePath(runFilePathRoot);
                        if (relPath != null)
                        {
                            String[] parts = StringUtils.split(relPath, File.separatorChar);
                            form.setRunFilePathRoot(StringUtils.join(parts, "/"));
                        }
                    }
                }
            }
            form.setWizardStep(ImportAnalysisStep.ASSOCIATE_FCSFILES);
        }

        private void stepAssociateFCSFiles(ImportAnalysisForm form, BindException errors) throws Exception
        {
            File runFilePathRoot = getRunPathRoot(form, errors);
            if (errors.hasErrors())
                return;

            if (runFilePathRoot != null)
            {
                boolean found = false;
                WorkspaceData workspaceData = form.getWorkspace();
                FlowJoWorkspace workspace = workspaceData.getWorkspaceObject();
                for (FlowJoWorkspace.SampleInfo sampleInfo : workspace.getSamples())
                {
                    File sampleFile = new File(runFilePathRoot, sampleInfo.getLabel());
                    if (sampleFile.exists())
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    errors.reject(ERROR_MSG, "No samples from workspace found in selected directory");
                    return;
                }
            }
            form.setWizardStep(ImportAnalysisStep.CHOOSE_ANALYSIS);
        }

        private void stepChooseAnalysis(ImportAnalysisForm form, BindException errors) throws Exception
        {
            FlowExperiment experiment;
            if (StringUtils.isEmpty(form.getNewAnalysisName()))
            {
                experiment = FlowExperiment.fromExperimentId(form.getExistingAnalysisId());
            }
            else
            {
                experiment = FlowExperiment.getForName(getUser(), getContainer(), form.getNewAnalysisName());
            }
            if (experiment != null)
            {
                if (!experiment.getContainer().equals(getContainer()))
                    throw new IllegalArgumentException("Wrong container");

                File runFilePathRoot = getRunPathRoot(form, errors);
                if (runFilePathRoot != null)
                {
                    FlowRun[] existing = experiment.findRun(runFilePathRoot, null);
                    if (existing.length != 0)
                    {
                        errors.reject(ERROR_MSG, "This analysis folder already contains this path.");
                        return;
                    }
                }
            }
            form.setWizardStep(ImportAnalysisStep.CONFIRM);
        }

        private void stepConfirm(ImportAnalysisForm form, BindException errors) throws Exception
        {
            PipeRoot root = null;
            WorkspaceData workspaceData = form.getWorkspace();
            String path = workspaceData.getPath();
            File workspaceFile;
            if (path != null)
            {
                root = getPipeRoot();
                workspaceFile = root.resolvePath(path);
            }
            else
            {
                workspaceFile = new File(FlowSettings.getWorkingDirectory(), form.getWorkspace().getName());
            }

            FlowExperiment experiment;
            if (StringUtils.isEmpty(form.getNewAnalysisName()))
            {
                experiment = FlowExperiment.fromExperimentId(form.getExistingAnalysisId());
            }
            else
            {
                experiment = FlowExperiment.createForName(getUser(), getContainer(), form.getNewAnalysisName());
            }
            if (!experiment.getContainer().equals(getContainer()))
                throw new IllegalArgumentException("Wrong container");

            File runFilePathRoot = getRunPathRoot(form, errors);
            ViewBackgroundInfo info = getViewBackgroundInfo();
            if (root == null)
            {
                // root-less pipeline job for workapce uploaded via the browser
                info.setUrlHelper(null);
            }

            WorkspaceJob job = new WorkspaceJob(info, workspaceData, experiment, workspaceFile, runFilePathRoot);
            HttpView.throwRedirect(executeScript(job));
        }

        public ActionURL getSuccessURL(ImportAnalysisForm form)
        {
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String display = "Import Analysis";
            if (title != null)
                display += ": " + title;
            return root.addChild(display);
        }
    }

}
