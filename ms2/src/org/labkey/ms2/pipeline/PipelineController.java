/*
 * Copyright (c) 2005-2010 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.action.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.PageConfig;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.pipeline.mascot.MascotCPipelineProvider;
import org.labkey.ms2.pipeline.mascot.MascotSearchTask;
import org.labkey.ms2.pipeline.sequest.SequestLocalPipelineProvider;
import org.labkey.ms2.pipeline.tandem.XTandemCPipelineProvider;
import org.labkey.ms2.pipeline.tandem.XTandemSearchProtocolFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

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
        super();
        setActionResolver(_resolver);
    }

    public PageConfig defaultPageConfig()
    {
        PageConfig p = super.defaultPageConfig();    //To change body of overridden methods use File | Settings | File Templates.
        p.setHelpTopic(getHelpTopic("ms2"));
        return p;
    }

    public ActionURL urlProjectStart(Container container)
    {
        return PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(container);
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o)
        {
            return MS2Controller.getBeginURL(getContainer());
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class UploadAction extends RedirectAction<PipelinePathForm>
    {
        public ActionURL getSuccessURL(PipelinePathForm form)
        {
            return MS2Controller.getShowListURL(getContainer());
        }

        public void validateCommand(PipelinePathForm form, Errors errors)
        {
        }

        public boolean doAction(PipelinePathForm form, BindException errors) throws Exception
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
                    description = AbstractFileAnalysisJob.
                            getDataDescription(null, file.getName(), protocolName);
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
                            getDataDescription(dirDataOriginal, baseName, protocolName);
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
                catch (SQLException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class SearchXTandemAction extends SearchAction
    {
        public String getProviderName()
        {
           return XTandemCPipelineProvider.name;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class SearchMascotAction extends SearchAction
    {
        public String getProviderName()
        {
            return MascotCPipelineProvider.name;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class SearchSequestAction extends SearchAction
    {
        public String getProviderName()
        {
            return SequestLocalPipelineProvider.name;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class SearchServiceAction extends GWTServiceAction
    {
        protected BaseRemoteService createService()
        {
            return new SearchServiceImpl(getViewContext());
        }
    }


    @RequiresPermissionClass(InsertPermission.class)
    public class SearchAction extends FormViewAction<MS2SearchForm>
    {
        private PipeRoot _root;
        private File _dirSeqRoot;
        private File _dirData;
        private AbstractMS2SearchPipelineProvider _provider;
        private AbstractMS2SearchProtocol _protocol;

        public String getProviderName()
        {
            return null;
        }

        public Class<? extends Controller> getAction()
        {
            return this.getClass();
        }

        public ActionURL getSuccessURL(MS2SearchForm form)
        {
            return urlProjectStart(getContainer());
        }


        public ModelAndView handleRequest(MS2SearchForm form, BindException errors) throws Exception
        {
            _root = PipelineService.get().findPipelineRoot(getContainer());
            if (_root == null || !_root.isValid())
                return HttpView.throwNotFound();

            _dirSeqRoot = MS2PipelineManager.getSequenceDatabaseRoot(_root.getContainer());

            _dirData = _root.resolvePath(form.getPath());
            if (_dirData == null || !NetworkDrive.exists(_dirData))
                throw new NotFoundException("Path does not exist " + form.getPath());

            if (getProviderName() != null)
                form.setSearchEngine(getProviderName());

            _provider =
                (AbstractMS2SearchPipelineProvider)PipelineService.get().getPipelineProvider(form.getSearchEngine());
            if (_provider == null)
                throw new NotFoundException("No such provider: " + form.getSearchEngine());
            AbstractMS2SearchProtocolFactory protocolFactory = _provider.getProtocolFactory();

            if ("".equals(form.getProtocol()))
            {
                // If protocol is empty check for a saved protocol
                String protocolNameLast = PipelineService.get().getLastProtocolSetting(protocolFactory,
                        getContainer(), getUser());
                if (protocolNameLast != null && !"".equals(protocolNameLast))
                {
                    String[] protocolNames = protocolFactory.getProtocolNames(_root, _dirData);
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
            if ( !protocolName.equals("new") && !protocolName.equals("") )
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
                        _protocol = protocolFactory.load(_root, protocolName);
                    }

                    form.setProtocolName(_protocol.getName());
                    form.setProtocolDescription(_protocol.getDescription());
                    String[] seqDbNames = _protocol.getDbNames();
                    form.setConfigureXml(_protocol.getXml());
                    if (seqDbNames == null || seqDbNames.length == 0)
                        errors.reject(ERROR_MSG, "Protocol must specify a FASTA file.");
                    else if (seqDbNames.length > 1)
                        errors.reject(ERROR_MSG, "Protocol specifies multiple FASTA files.");
                    else
                        form.setSequenceDB(seqDbNames[0]);
                }
                catch (IOException eio)
                {
                    errors.reject(ERROR_MSG, "Failed to load requested protocol.");
                }
            }
            boolean success = errors == null || !errors.hasErrors();
            if ("POST".equals(getViewContext().getRequest().getMethod()) && form.isRunSearch())
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

        public void validateCommand(MS2SearchForm form, Errors errors)
        {
        }

        public boolean handlePost(MS2SearchForm form, BindException errors) throws Exception
        {

            if(!form.isRunSearch())
                return false;

            try
            {
                _provider.ensureEnabled();   // throws exception if not enabled

                // If not a saved protocol, create one from the information in the form.
                if (!"new".equals(form.getProtocol()))
                {
                    _protocol.setDirSeqRoot(_dirSeqRoot);
                    _protocol.setDbPath(form.getSequenceDBPath());
                    _protocol.setDbNames(new String[] {form.getSequenceDB()});
                    PipelineService.get().rememberLastProtocolSetting(_protocol.getFactory(),
                            getContainer(), getUser(), form.getProtocol());
                    PipelineService.get().rememberLastSequenceDbSetting(_protocol.getFactory(), getContainer(),
                            getUser(), form.getSequenceDBPath(), form.getSequenceDB());
                }
                else
                {
                    _protocol = _provider.getProtocolFactory().createProtocolInstance(
                            form.getProtocolName(),
                            form.getProtocolDescription(),
                            form.getConfigureXml());

                    _protocol.setDirSeqRoot(_dirSeqRoot);
                    _protocol.setDbPath(form.getSequenceDBPath());
                    _protocol.setDbNames(new String[] {form.getSequenceDB()});
                    _protocol.setEmail(getUser().getEmail());
                    _protocol.validateToSave(_root);
                    if (form.isSaveProtocol())
                    {
                        _protocol.saveDefinition(_root);
                        PipelineService.get().rememberLastProtocolSetting(_protocol.getFactory(),
                                getContainer(), getUser(), form.getProtocolName());   
                    }
                    PipelineService.get().rememberLastSequenceDbSetting(_protocol.getFactory(),getContainer(),
                                getUser(),form.getSequenceDBPath(), form.getSequenceDB());
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

                AbstractMS2SearchPipelineJob job =
                        _protocol.createPipelineJob(getViewBackgroundInfo(), _root, mzXMLFiles, fileParameters);

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
                    PipelineJobService.get().getStatusWriter().setStatus(job, PipelineJob.WAITING_FOR_FILES, null, true);
                    PipelineJobService.get().getJobStore().storeJob(job);
                    job.getLogger().info("Job created, but not yet submitted because not all files are available");
                }
            }
            catch (IllegalArgumentException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            catch (PipelineProtocol.PipelineValidationException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            catch (IOException e)
            {
                errors.reject(ERROR_MSG, "Failure attempting to write input parameters." + e.getMessage());
                return false;
            }

            return true;      
        }

        public ModelAndView getView(MS2SearchForm form, boolean reshow, BindException errors) throws Exception
        {
            if (!reshow || "".equals(form.getProtocol()))
                form.setSaveProtocol(true);

             //get help topic
            String helpTopic = getHelpTopic(_provider.getHelpTopic()).getHelpTopicLink();
            ActionURL returnURL = PageFlowUtil.urlProvider(PipelineUrls.class).urlReferer(getContainer());
           
            //properties to send to GWT page
            Map<String, String> props = new HashMap<String, String>();
            props.put("errors", getErrors(errors));
            props.put("saveProtocol", Boolean.toString(form.isSaveProtocol()));
            props.put("returnURL", returnURL.getLocalURIString() );
            props.put("helpTopic", helpTopic);
            props.put("file", StringUtils.join(form.getFile(), "/"));
            props.put("searchEngine", form.getSearchEngine());
            props.put("targetAction", SpringActionController.getActionName(getAction()) + ".view");
            props.put("path", form.getPath());
            return new GWTView(org.labkey.ms2.pipeline.client.Search.class, props);
        }

        private String getErrors(BindException errors)
        {
            if(errors == null) return "";
            List errorMessages = errors.getAllErrors();
            StringBuilder errorString = new StringBuilder();
            for (ObjectError errorMessage : (Iterable<ObjectError>) errorMessages)
            {
                errorString.append(errorMessage.getDefaultMessage());
                errorString.append("\n");
            }
            return errorString.toString();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Search MS2 Data");
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class SetupClusterSequenceDBAction extends FormViewAction<SequenceDBRootForm>
    {
        public void validateCommand(SequenceDBRootForm form, Errors errors)
        {
        }

        public boolean handlePost(SequenceDBRootForm form, BindException errors) throws Exception
        {
            boolean success = true;

            String newSequenceRoot = form.getLocalPathRoot();
            URI root = null;
            if (newSequenceRoot != null && newSequenceRoot.length() > 0)
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
                MS2PipelineManager.setSequenceDatabaseRoot(getUser(), getContainer(), root, form.isAllowUpload());
            }

            return success;
        }

        public ActionURL getSuccessURL(SequenceDBRootForm form)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(getContainer());
        }

        public ModelAndView getView(SequenceDBRootForm form, boolean reshow, BindException errors) throws Exception
        {
            ConfigureSequenceDB page = (ConfigureSequenceDB) FormPage.get(
                    PipelineController.class, form, "ConfigureSequenceDB.jsp");

            File fileRoot = MS2PipelineManager.getSequenceDatabaseRoot(getContainer());
            if (fileRoot == null)
                page.setLocalPathRoot("");
            else
            {
                if (!NetworkDrive.exists(fileRoot))
                    errors.reject(ERROR_MSG, "FASTA root \"" + fileRoot + "\" does not exist.");
                boolean allowUpload = MS2PipelineManager.allowSequenceDatabaseUploads(getUser(), getContainer());
                page.setAllowUpload(allowUpload);
                page.setLocalPathRoot(fileRoot.toString());
            }

            return page.createView(errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Configure FASTA Root");
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class SetTandemDefaultsAction extends SetDefaultsActionBase
    {
        public String getProviderName()
        {
            return XTandemCPipelineProvider.name;
        }

        public ModelAndView getJspView(SetDefaultsForm form, BindException errors)
        {
            return new JspView<SetDefaultsForm>(XTandemCPipelineProvider.class, "setTandemDefaults.jsp", form, errors);
        }

        public HelpTopic getHelpTopic()
        {
            return PipelineController.getHelpTopic("MS2-Pipeline/setTandemDefaults");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Set X! Tandem Defaults");
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class SetMascotDefaultsAction extends SetDefaultsActionBase
    {
        public String getProviderName()
        {
            return MascotCPipelineProvider.name;
        }

        public ModelAndView getJspView(SetDefaultsForm form, BindException errors)
        {
            return new JspView<SetDefaultsForm>(MascotCPipelineProvider.class, "setMascotDefaults.jsp", form, errors);
        }

        public HelpTopic getHelpTopic()
        {
            return PipelineController.getHelpTopic("MS2-Pipeline/setMascotDefaults");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Set Mascot Defaults");
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class SetSequestDefaultsAction extends SetDefaultsActionBase
    {
        public String getProviderName()
        {
            return SequestLocalPipelineProvider.name;
        }

        public ModelAndView getJspView(SetDefaultsForm form, BindException errors)
        {
            return new JspView<SetDefaultsForm>(SequestLocalPipelineProvider.class, "setSequestDefaults.jsp", form, errors);
        }

        public HelpTopic getHelpTopic()
        {
            return PipelineController.getHelpTopic("MS2-Pipeline/setSequestDefaults");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Set Sequest Defaults");
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
        public abstract HelpTopic getHelpTopic();
        public abstract ModelAndView getJspView(SetDefaultsForm form, BindException errors);

        public ModelAndView handleRequest(SetDefaultsForm setDefaultsForm, BindException errors) throws Exception
        {
            _root = PipelineService.get().getPipelineRootSetting(getContainer());
            if (_root == null)
                return HttpView.throwNotFound("A pipeline root is not set on this folder.");

            _provider = (AbstractMS2SearchPipelineProvider)
                    PipelineService.get().getPipelineProvider(getProviderName());

            return super.handleRequest(setDefaultsForm, errors);
        }

        public void validateCommand(SetDefaultsForm form, Errors errors)
        {
        }

        public boolean handlePost(SetDefaultsForm form, BindException errors) throws Exception
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
                if (e.getMessage().indexOf("Access") != -1)
                    errors.addError(new LabkeyError("Access denied attempting to write defaults. Contact the server administrator."));
                else
                    errors.addError(new LabkeyError("Failure attempting to write defaults.  Please try again."));
                return false;
            }
            catch (IOException eio)
            {
                errors.addError(new LabkeyError("Failure attempting to write defaults.  Please try again."));
                return false;
            }

            return true;
        }

        public ModelAndView getView(SetDefaultsForm form, boolean reshow, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic());
            if (!reshow)
                form.setConfigureXml(_provider.getProtocolFactory().getDefaultParametersXML(_root));
            return getJspView(form, errors);
        }

        public ActionURL getSuccessURL(SetDefaultsForm form)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(getContainer());
        }
    }

    @RequiresPermissionClass(AdminPermission.class)
    public class AddSequenceDBAction extends FormViewAction<Object>
    {
        public void validateCommand(Object form, Errors errors)
        {
        }

        public ModelAndView getView(Object form, boolean reshow, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic("MS2-Pipeline/addSequenceDB"));
            return new JspView<Object>(PipelineController.class, "addSequenceDB.jsp", form, errors);
        }

        public boolean handlePost(Object form, BindException errors) throws Exception
        {
            Map<String, MultipartFile> fileMap = getFileMap();
            MultipartFile ff = fileMap.get("sequenceDBFile");

            String name = (ff == null ? "" : ff.getOriginalFilename());
            if (ff == null || ff.getSize() == 0)
            {
                errors.addError(new LabkeyError("Please specify a FASTA file."));
                return false;
            }
            else if (name.indexOf(File.separatorChar) != -1 || name.indexOf('/') != -1)
            {
                errors.reject(ERROR_MSG, "Invalid sequence database name '" + name + "'.");
                return false;
            }
            else
            {
                BufferedReader reader = null;
                try
                {
                    reader = new BufferedReader(new InputStreamReader(ff.getInputStream()));

                    MS2PipelineManager.addSequenceDB(getContainer(), name, reader);
                }
                catch (IllegalArgumentException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
                finally
                {
                    if (reader != null)
                    {
                        try
                        {
                            reader.close();
                        }
                        catch (IOException eio)
                        {
                        }
                    }
                }
            }

            return true;
        }

        public ActionURL getSuccessURL(Object form)
        {
            return urlProjectStart(getContainer());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Add Sequence Database");
        }
    }

    public static class SequenceDBRootForm extends ViewForm
    {
        private String _localPathRoot;
        private boolean _allowUpload;

        public boolean isAllowUpload()
        {
            return _allowUpload;
        }

        public void setAllowUpload(boolean allowUpload)
        {
            _allowUpload = allowUpload;
        }

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
