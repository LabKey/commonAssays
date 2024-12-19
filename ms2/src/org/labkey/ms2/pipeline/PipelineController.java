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
package org.labkey.ms2.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.GWTServiceAction;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GWTView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewForm;
import org.labkey.api.view.template.PageConfig;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.pipeline.client.Search;
import org.labkey.ms2.pipeline.comet.CometPipelineJob;
import org.labkey.ms2.pipeline.comet.CometPipelineProvider;
import org.labkey.ms2.pipeline.mascot.MascotCPipelineProvider;
import org.labkey.ms2.pipeline.mascot.MascotPipelineJob;
import org.labkey.ms2.pipeline.mascot.MascotSearchTask;
import org.labkey.ms2.pipeline.rollup.FractionRollupPipelineJob;
import org.labkey.ms2.pipeline.rollup.FractionRollupPipelineProvider;
import org.labkey.ms2.pipeline.sequest.SequestPipelineJob;
import org.labkey.ms2.pipeline.sequest.SequestPipelineProvider;
import org.labkey.ms2.pipeline.tandem.XTandemPipelineJob;
import org.labkey.ms2.pipeline.tandem.XTandemPipelineProvider;
import org.labkey.ms2.pipeline.tandem.XTandemSearchProtocolFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>PipelineController</code>
 */
public class PipelineController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(PipelineController.class);

    private static HelpTopic getHelpTopic(String topic)
    {
        return new HelpTopic(topic);
    }

    public PipelineController()
    {
        setActionResolver(_resolver);
    }

    @Override
    public PageConfig defaultPageConfig()
    {
        PageConfig p = super.defaultPageConfig();
        p.setHelpTopic("ms2");
        return p;
    }

    public ActionURL urlProjectStart(Container container)
    {
        return urlProvider(ProjectUrls.class).getStartURL(container);
    }

    @RequiresPermission(ReadPermission.class)
    public static class BeginAction extends SimpleRedirectAction<Object>
    {
        @Override
        public ActionURL getRedirectURL(Object o)
        {
            return MS2Controller.getBeginURL(getContainer());
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class UploadAction extends FormHandlerAction<PipelinePathForm>
    {
        @Override
        public void validateCommand(PipelinePathForm target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(PipelinePathForm form, BindException errors)
        {
            for (File file : form.getValidatedFiles(getContainer()))
            {
                if (!file.isFile())
                {
                    throw new NotFoundException("Expected a file but found a directory: " + file.getName());
                }
                int extParts = 1;
                if (file.getName().endsWith(".xml"))
                    extParts = 2;
                String baseName = FileUtil.getBaseName(file, extParts);
                // If the data was created by our pipeline, try to get the name
                // to look like the normal generated name.

                String protocolName;
                File dirDataOriginal;
                String description;
                if (MascotSearchTask.isNativeOutputFile(file))
                {
                    //TODO: wch: use an appropriate protocol
                    //      after all, this is what the Mascot search processing is doing
                    // mascot .dat result file does not follow that of pipeline
                    protocolName = "none";
                    // dirDataOriginal = file;
                    description = file.getName();
                }
                else
                {
                    // If the data was created by our pipeline, try to get the name
                    // to look like the normal generated name.
                    protocolName = file.getParentFile().getName();
                    dirDataOriginal = file.getParentFile().getParentFile();
                    if (dirDataOriginal != null &&
                            dirDataOriginal.getName().equals(XTandemSearchProtocolFactory.get().getName()))
                    {
                        dirDataOriginal = dirDataOriginal.getParentFile();
                    }
                    description = AbstractFileAnalysisJob.
                            getDataDescription(dirDataOriginal, baseName, AbstractFileAnalysisProtocol.LEGACY_JOINED_BASENAME, protocolName);
                }

                ViewBackgroundInfo info = getViewBackgroundInfo();
                try
                {
                    if (AbstractMS2SearchProtocol.FT_SEARCH_XAR.isType(file))
                    {
                        ExperimentService.get().importXarAsync(info, file, description, form.getPipeRoot(getContainer()));
                    }
                    else if (TPPTask.isPepXMLFile(file))
                    {
                        MS2Manager.addRunToQueue(info, file, description, form.getPipeRoot(getContainer()));
                    }
                    else if (MascotSearchTask.isNativeOutputFile(file))
                    {
                        MS2Manager.addMascotRunToQueue(info, file, description, form.getPipeRoot(getContainer()));
                    }
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }

        @Override
        public ActionURL getSuccessURL(PipelinePathForm form)
        {
            return urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class SearchXTandemAction extends SearchAction
    {
        @Override
        public String getProviderName()
        {
           return XTandemPipelineProvider.name;
        }

        @Override
        protected TaskId getTaskId()
        {
            return new TaskId(XTandemPipelineJob.class);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class FractionRollupAction extends SearchAction
    {
        @Override
        public String getProviderName()
        {
           return FractionRollupPipelineProvider.NAME;
        }

        @Override
        protected TaskId getTaskId()
        {
            return new TaskId(FractionRollupPipelineJob.class);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class SearchMascotAction extends SearchAction
    {
        @Override
        public String getProviderName()
        {
            return MascotCPipelineProvider.name;
        }

        @Override
        protected TaskId getTaskId()
        {
            return new TaskId(MascotPipelineJob.class);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class SearchSequestAction extends SearchAction
    {
        @Override
        public String getProviderName()
        {
            return SequestPipelineProvider.name;
        }


        @Override
        protected TaskId getTaskId()
        {
            return new TaskId(SequestPipelineJob.class);
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class SearchCometAction extends SearchAction
    {
        @Override
        public String getProviderName()
        {
            return CometPipelineProvider.NAME;
        }


        @Override
        protected TaskId getTaskId()
        {
            return new TaskId(CometPipelineJob.class);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public static class SearchServiceAction extends GWTServiceAction
    {
        @Override
        protected BaseRemoteService createService()
        {
            return new SearchServiceImpl(getViewContext());
        }
    }


    @RequiresPermission(InsertPermission.class)
    public abstract class SearchAction extends FormViewAction<MS2SearchForm>
    {
        private PipeRoot _root;
        private File _dirSeqRoot;
        private File _dirData;
        private AbstractMS2PipelineProvider _provider;
        private AbstractMS2SearchProtocol _protocol;

        public abstract String getProviderName();

        public Class<? extends Controller> getAction()
        {
            return this.getClass();
        }

        @Override
        public ActionURL getSuccessURL(MS2SearchForm form)
        {
            return urlProjectStart(getContainer());
        }


        @Override
        public ModelAndView handleRequest(MS2SearchForm form, BindException errors) throws Exception
        {
            _root = PipelineService.get().findPipelineRoot(getContainer());
            if (_root == null || !_root.isValid())
                throw new NotFoundException();

            _dirSeqRoot = MS2PipelineManager.getSequenceDatabaseRoot(_root.getContainer(), false);

            if (form.getPath() == null)
            {
                throw new NotFoundException("No path specified");
            }
            _dirData = _root.resolvePath(form.getPath());
            if (!NetworkDrive.exists(_dirData))
                throw new NotFoundException("Path does not exist " + form.getPath());

            if (getProviderName() != null)
                form.setSearchEngine(getProviderName());

            _provider =
                (AbstractMS2PipelineProvider)PipelineService.get().getPipelineProvider(form.getSearchEngine());
            if (_provider == null)
                throw new NotFoundException("No such provider: " + form.getSearchEngine());
            AbstractMS2SearchProtocolFactory protocolFactory = _provider.getProtocolFactory();

            if ("".equals(form.getProtocol()))
            {
                // If protocol is empty check for a saved protocol
                String protocolNameLast = PipelineService.get().getLastProtocolSetting(protocolFactory,
                        getContainer(), getUser());
                if (protocolNameLast != null && !protocolNameLast.isEmpty())
                {
                    String[] protocolNames = protocolFactory.getProtocolNames(_root, _dirData.toPath(), false);
                    // Make sure it is still around.
                    if (Arrays.asList(protocolNames).contains(protocolNameLast))
                        form.setProtocol(protocolNameLast);
                }
            }
            // New protocol chosen from form
            else if ("<New Protocol>".equals(form.getProtocol()))
            {
                form.setProtocol("");
            }

            String protocolName = form.getProtocol();
            if ( !protocolName.equals("new") && !protocolName.isEmpty())
            {
                try
                {
                    File protocolFile = protocolFactory.getParametersFile(_dirData, protocolName, _root);
                    if (NetworkDrive.exists(protocolFile))
                    {
                        _protocol = protocolFactory.loadInstance(protocolFile);

                        // Don't allow the instance file to override the protocol name.
                        _protocol.setName(protocolName);
                    }
                    else
                    {
                        _protocol = protocolFactory.load(_root, protocolName, false);
                    }

                    form.setProtocolName(_protocol.getName());
                    form.setProtocolDescription(_protocol.getDescription());
                    String[] seqDbNames = _protocol.getDbNames();
                    form.setConfigureXml(_protocol.getXml());
                    if (seqDbNames == null || seqDbNames.length == 0)
                        errors.reject(ERROR_MSG, "Protocol must specify a FASTA file.");
                    else
                        form.setSequenceDB(seqDbNames);
                }
                catch (IOException eio)
                {
                    errors.reject(ERROR_MSG, "Failed to load requested protocol '" + protocolName + "', it may not exist. " + (eio.getMessage() == null ? "" : eio.getMessage()));
                }
            }
            boolean success = errors == null || !errors.hasErrors();
            if (isPost() && form.isRunSearch())
            {
                getPageConfig().setTemplate(PageConfig.Template.None);

                if (success)
                    validate(form, errors);
                success = errors == null || !errors.hasErrors();

                if (success)
                    success = handlePost(form, errors);

                if (success)
                {
                    ActionURL url = getSuccessURL(form);
                    if (null != url)
                    {
                        getViewContext().getResponse().getOutputStream().print("SUCCESS=" + url.getLocalURIString());
                        return null;
                    }
                }
                getViewContext().getResponse().getOutputStream().print("ERROR=" + getErrors(errors));
                return null;
            }

            return getView(form, getReshow(), errors);
        }

        @Override
        public void validateCommand(MS2SearchForm form, Errors errors)
        {
        }

        @Override
        public boolean handlePost(MS2SearchForm form, BindException errors) throws Exception
        {

            if(!form.isRunSearch())
                return false;

            try
            {
                _provider.ensureEnabled(getContainer());   // throws exception if not enabled

                // If not a saved protocol, create one from the information in the form.
                if (!"new".equals(form.getProtocol()))
                {
                    if (_protocol == null)
                    {
                        throw new NotFoundException("Protocol '" + form.getProtocol() + "' could not be found");
                    }
                    _protocol.setDirSeqRoot(_dirSeqRoot);
                    _protocol.setDbPath(form.getSequenceDBPath());
                    _protocol.setDbNames(form.getSequenceDB());
                    PipelineService.get().rememberLastProtocolSetting(_protocol.getFactory(),
                            getContainer(), getUser(), form.getProtocol());
                    PipelineService.get().rememberLastSequenceDbSetting(_protocol.getFactory(), getContainer(),
                            getUser(), form.getSequenceDBPath(), AbstractMS2SearchProtocolFactory.joinSequenceFiles(form.getSequenceDB()));
                }
                else
                {
                    _protocol = _provider.getProtocolFactory().createProtocolInstance(
                            form.getProtocolName(),
                            form.getProtocolDescription(),
                            form.getConfigureXml());

                    _protocol.setDirSeqRoot(_dirSeqRoot);
                    _protocol.setDbPath(form.getSequenceDBPath());
                    _protocol.setDbNames(form.getSequenceDB());
                    _protocol.setEmail(getUser().getEmail());
                    _protocol.validateToSave(_root, true, true);
                    if (form.isSaveProtocol())
                    {
                        _protocol.saveDefinition(_root);
                        PipelineService.get().rememberLastProtocolSetting(_protocol.getFactory(),
                                getContainer(), getUser(), form.getProtocolName());   
                    }
                    PipelineService.get().rememberLastSequenceDbSetting(_protocol.getFactory(),getContainer(),
                                getUser(),form.getSequenceDBPath(), AbstractMS2SearchProtocolFactory.joinSequenceFiles(form.getSequenceDB()));
                }

                if (form.getFile().length == 0)
                {
                    throw new NotFoundException("No files specified");
                }
                List<File> mzXMLFiles = form.getValidatedFiles(getContainer(), true);

                _protocol.getFactory().ensureDefaultParameters(_root);

                File fileParameters = _protocol.getParametersFile(_dirData, _root);
                // Make sure parameters XML file exists for the job when it runs.
                if (!fileParameters.exists())
                {
                    _protocol.setEmail(getUser().getEmail());
                    _protocol.saveInstance(fileParameters, getContainer());
                }

                AbstractMS2SearchPipelineJob job = _protocol.createPipelineJob(getViewBackgroundInfo(), _root,
                        mzXMLFiles.stream().map((f) -> f.toPath()).toList(), fileParameters.toPath(), null);

                // Check for existing job
                PipelineStatusFile existingJobStatusFile = PipelineService.get().getStatusFile(job.getLogFile());
                if (existingJobStatusFile != null && existingJobStatusFile.getJobStore() != null)
                {
                    PipelineJob existingJob = PipelineJob.deserializeJob(existingJobStatusFile.getJobStore());
                    if (null != existingJob)
                    {
                        if (existingJob instanceof AbstractMS2SearchPipelineJob)
                        {
                            job = (AbstractMS2SearchPipelineJob) existingJob;
                            // Add any new files
                            List<File> inputFiles = job.getInputFiles();
                            for (File mzXMLFile : mzXMLFiles)
                            {
                                if (!inputFiles.contains(mzXMLFile))
                                {
                                    inputFiles.add(mzXMLFile);
                                }
                            }
                        }
                        existingJob.setActiveTaskId(existingJob.getTaskPipeline().getTaskProgression()[0]);
                    }
                }

                boolean allFilesReady = true;

                for (File mzXMLFile : mzXMLFiles)
                {
                    if (!NetworkDrive.exists(mzXMLFile))
                    {
                        allFilesReady = false;
                        break;
                    }
                }

                if (allFilesReady)
                {
                    PipelineService.get().queueJob(job);
                }
                else
                {
                    PipelineJobService.get().getStatusWriter().setStatus(job, PipelineJob.TaskStatus.waitingForFiles.toString(), null, true);
                    PipelineJobService.get().getJobStore().storeJob(job);
                    job.getLogger().info("Job created, but not yet submitted because not all files are available");
                }
            }
            catch (IllegalArgumentException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            catch (PipelineValidationException e)
            {
                errors.reject(ERROR_MSG, Search.VALIDATION_FAILURE_PREFIX + e.getMessage());
                return false;
            }
            catch (IOException e)
            {
                errors.reject(ERROR_MSG, "Failure attempting to write input parameters." + e.getMessage());
                return false;
            }
            catch (NotFoundException e)
            {
                // Let this pass through normally
                throw e;
            }
            catch (RuntimeException e)
            {
                ExceptionUtil.logExceptionToMothership(getViewContext().getRequest(), e);
                errors.reject(ERROR_MSG, "Failure when attempting to submit search. " + e.getMessage());
                return false;
            }

            return true;      
        }

        @Override
        public ModelAndView getView(MS2SearchForm form, boolean reshow, BindException errors)
        {
            if (!reshow || "".equals(form.getProtocol()))
                form.setSaveProtocol(true);

             //get help topic
            String helpTopic = getHelpTopic(_provider.getHelpTopic()).getHelpTopicHref();
            ActionURL returnURL = form.getReturnActionURL(getContainer().getStartURL(getUser()));

            form.getValidatedFiles(getContainer(), true);

            //properties to send to GWT page
            Map<String, String> props = new HashMap<>();
            props.put("errors", getErrors(errors));
            props.put("saveProtocol", Boolean.toString(form.isSaveProtocol()));
            props.put(ActionURL.Param.returnUrl.name(), returnURL.getLocalURIString() );
            props.put("helpTopic", helpTopic);
            props.put("file", StringUtils.join(form.getFile(), "/"));
            props.put("searchEngine", form.getSearchEngine());
            props.put("pipelineId", getTaskId().toString());
            ActionURL targetURL = new ActionURL(getAction(), getContainer());
            props.put("targetAction", targetURL.toString());
            props.put("path", form.getPath());
            return new GWTView(org.labkey.ms2.pipeline.client.Search.class, props);
        }

        protected abstract TaskId getTaskId();

        private String getErrors(BindException errors)
        {
            if(errors == null) return "";
            List<ObjectError> errorMessages = errors.getAllErrors();
            StringBuilder errorString = new StringBuilder();
            for (ObjectError errorMessage : errorMessages)
            {
                errorString.append(errorMessage.getDefaultMessage());
                errorString.append("\n");
            }
            return errorString.toString();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("File List", urlProvider(PipelineUrls.class).urlBrowse(getContainer()));
            root.addChild("Search MS2 Data");
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SetupClusterSequenceDBAction extends FormViewAction<SequenceDBRootForm>
    {
        @Override
        public void validateCommand(SequenceDBRootForm form, Errors errors)
        {
        }

        @Override
        public boolean handlePost(SequenceDBRootForm form, BindException errors) throws Exception
        {
            boolean success = true;

            String newSequenceRoot = form.getLocalPathRoot();
            URI root = null;
            if (newSequenceRoot != null && !newSequenceRoot.isEmpty())
            {
                File file = new File(newSequenceRoot);
                if (!NetworkDrive.exists(file))
                {
                    success = false;    // Reshow the form, if non-existent.
                    errors.reject(ERROR_MSG, "FASTA root \"" + newSequenceRoot + "\" does not exist.");
                }

                try
                {
                    String canonicalPath = file.getCanonicalPath();
                    String absolutePath = file.getAbsolutePath();
                    if (!canonicalPath.equals(absolutePath) && canonicalPath.equalsIgnoreCase(absolutePath))
                    {
                        // On Windows, fix up case-only differences in the user's specified path compared to the canonical path
                        file = file.getCanonicalFile();
                    }
                }
                catch (IOException e)
                {
                    // OK, just leave the user's path unaltered
                }

                root = file.toURI();
            }

            if (success)
            {
                MS2PipelineManager.setSequenceDatabaseRoot(getUser(), getContainer(), root);
            }

            return success;
        }

        @Override
        public ActionURL getSuccessURL(SequenceDBRootForm form)
        {
            return urlProvider(PipelineUrls.class).urlSetup(getContainer());
        }

        @Override
        public ModelAndView getView(SequenceDBRootForm form, boolean reshow, BindException errors)
        {
            File fileRoot = MS2PipelineManager.getSequenceDatabaseRoot(getContainer(), false);
            final String localPathRoot;

            if (fileRoot == null)
            {
                localPathRoot = "";
            }
            else
            {
                if (!NetworkDrive.exists(fileRoot))
                    errors.reject(ERROR_MSG, "FASTA root \"" + fileRoot + "\" does not exist.");
                localPathRoot = fileRoot.toString();
            }

            return new JspView<>("/org/labkey/ms2/pipeline/ConfigureSequenceDB.jsp", localPathRoot, errors);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Configure FASTA Root");
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SetTandemDefaultsAction extends SetDefaultsActionBase
    {
        @Override
        public String getProviderName()
        {
            return XTandemPipelineProvider.name;
        }

        @Override
        public ModelAndView getJspView(SetDefaultsForm form, BindException errors)
        {
            return new JspView<>("/org/labkey/ms2/pipeline/tandem/setTandemDefaults.jsp", form, errors);
        }

        @Override
        public String getHelpTopic()
        {
            return "pipelineXTandem";
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Set X! Tandem Defaults");
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SetMascotDefaultsAction extends SetDefaultsActionBase
    {
        @Override
        public String getProviderName()
        {
            return MascotCPipelineProvider.name;
        }

        @Override
        public ModelAndView getJspView(SetDefaultsForm form, BindException errors)
        {
            return new JspView<>("/org/labkey/ms2/pipeline/mascot/setMascotDefaults.jsp", form, errors);
        }

        @Override
        public String getHelpTopic()
        {
            return "pipelineMascot";
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Set Mascot Defaults");
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SetSequestDefaultsAction extends SetDefaultsActionBase
    {
        @Override
        public String getProviderName()
        {
            return SequestPipelineProvider.name;
        }

        @Override
        public ModelAndView getJspView(SetDefaultsForm form, BindException errors)
        {
            return new JspView<>("/org/labkey/ms2/pipeline/sequest/setSequestDefaults.jsp", form, errors);
        }

        @Override
        public String getHelpTopic()
        {
            return "pipelineSequest";
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Set Sequest Defaults");
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SetCometDefaultsAction extends SetDefaultsActionBase
    {
        @Override
        public String getProviderName()
        {
            return CometPipelineProvider.NAME;
        }

        @Override
        public ModelAndView getJspView(SetDefaultsForm form, BindException errors)
        {
            return new JspView<>("/org/labkey/ms2/pipeline/comet/setCometDefaults.jsp", form, errors);
        }

        @Override
        public String getHelpTopic()
        {
            return "pipelineComet";
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Set Comet Defaults");
        }
    }

    public static class SetDefaultsForm
    {
        private String configureXml;

        public String getConfigureXml()
        {
            return configureXml;
        }

        public void setConfigureXml(String configureXml)
        {
            this.configureXml = configureXml;
        }
    }

    protected abstract class SetDefaultsActionBase extends FormViewAction<SetDefaultsForm>
    {
        private PipeRoot _root;
        private AbstractMS2SearchPipelineProvider _provider;

        public abstract String getProviderName();
        public abstract String getHelpTopic();
        public abstract ModelAndView getJspView(SetDefaultsForm form, BindException errors);

        @Override
        public ModelAndView handleRequest(SetDefaultsForm setDefaultsForm, BindException errors) throws Exception
        {
            _root = PipelineService.get().getPipelineRootSetting(getContainer());
            if (_root == null)
                throw new NotFoundException("A pipeline root is not set on this folder.");

            _provider = (AbstractMS2SearchPipelineProvider)
                    PipelineService.get().getPipelineProvider(getProviderName());

            return super.handleRequest(setDefaultsForm, errors);
        }

        @Override
        public void validateCommand(SetDefaultsForm form, Errors errors)
        {
        }

        @Override
        public boolean handlePost(SetDefaultsForm form, BindException errors)
        {
            try
            {
                _provider.getProtocolFactory().setDefaultParametersXML(_root, form.getConfigureXml());
            }
            catch (IllegalArgumentException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            catch (FileNotFoundException e)
            {
                if (e.getMessage().contains("Access"))
                    errors.addError(new LabKeyError("Access denied attempting to write defaults. Contact the server administrator."));
                else
                    errors.addError(new LabKeyError("Failure attempting to write defaults.  Please try again."));
                return false;
            }
            catch (IOException eio)
            {
                errors.addError(new LabKeyError("Failure attempting to write defaults.  Please try again."));
                return false;
            }

            return true;
        }

        @Override
        public ModelAndView getView(SetDefaultsForm form, boolean reshow, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic());
            if (!reshow)
                form.setConfigureXml(_provider.getProtocolFactory().getDefaultParametersXML(_root));
            return getJspView(form, errors);
        }

        @Override
        public ActionURL getSuccessURL(SetDefaultsForm form)
        {
            return urlProvider(PipelineUrls.class).urlSetup(getContainer());
        }
    }

    public static class SequenceDBRootForm extends ViewForm
    {
        private String _localPathRoot;

        public void setLocalPathRoot(String localPathRoot)
        {
            _localPathRoot = localPathRoot;
        }

        public String getLocalPathRoot()
        {
            return _localPathRoot;
        }
    }
}
