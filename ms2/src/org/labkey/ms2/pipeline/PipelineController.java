/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.pipeline.*;
import org.labkey.api.security.ACL;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protocol.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

/**
 *
 */
@Jpf.Controller(longLived = true) //PageFlow is long lived so we can stash results of multi-step protocol fill-in
public class PipelineController extends ViewController
{
    private static Logger _log = Logger.getLogger(PipelineController.class);

    public static final String DEFAULT_EXPERIMENT_OBJECTID = "DefaultExperiment";
    
    private Forward _renderInTemplate(HttpView view, String title, String helpTopic) throws Exception
    {
        if (helpTopic == null)
            helpTopic = "ms2";

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext());
        if (title != null)
            trailConfig.setTitle(title);
        trailConfig.setHelpTopic(new HelpTopic(helpTopic, HelpTopic.Area.CPAS));

        return includeView(new HomeTemplate(getViewContext(), view, trailConfig));
    }

    @Jpf.Action
    protected Forward begin() throws URISyntaxException
    {
        ViewURLHelper url = cloneViewURLHelper();
        url.setPageFlow("MS2");
        url.setAction("begin");
        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward upload() throws SQLException, ServletException, URISyntaxException
    {
        requiresPermission(ACL.PERM_INSERT);

        Container c = getContainer();

        String path = getViewURLHelper().getParameter(PipelineService.PARAM_Path);

        PipeRoot pr = PipelineService.get().findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        URI uriUpload = URIUtil.resolve(pr.getUri(c), path);
        if (uriUpload == null)
            HttpView.throwNotFound();
        
        File fileUpload = new File(uriUpload);
        File[] files = new File[] { fileUpload };
        if (fileUpload.isDirectory())
            files = fileUpload.listFiles(MS2PipelineManager.getUploadFilter());

        for (File file : files)
        {
            int extParts = 1;
            if (file.getName().endsWith(".xml"))
                extParts = 2;
            String baseName = FileUtil.getBaseName(file, extParts);
            File dir = file.getParentFile();
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
                dirDataOriginal = file;
                description = MS2PipelineManager.
                        getDataDescription(null, file.getName(), protocolName);
            }
            else
            {
                // If the data was created by our pipeline, try to get the name
                // to look like the normal generated name.
                protocolName = dir.getName();
                dirDataOriginal = dir.getParentFile();
                if (dirDataOriginal != null &&
                        dirDataOriginal.getName().equals(XTandemSearchProtocolFactory.get().getName()))
                {
                    dirDataOriginal = dirDataOriginal.getParentFile();
                }
                description = MS2PipelineManager.
                        getDataDescription(dirDataOriginal, baseName, protocolName);
            }

            PipelineService service = PipelineService.get();
            ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), file);
            try
            {
                if (MS2PipelineManager.isSearchExperimentFile(file))
                {
                    ExperimentPipelineJob job = new ExperimentPipelineJob(info, file, description, false);
                    PipelineService.get().queueJob(job);
                }
                else if (TPPTask.isPepXMLFile(file))
                {
                    MS2Manager.addRunToQueue(info, file, description, false);
                }
                else if (MascotSearchTask.isNativeOutputFile(file))
                {
                    MS2Manager.addMascotRunToQueue(info, file, description, false);
                }
            }
            catch (IOException ie)
            {
                _log.error("Failed trying to load experiment.", ie);
            }
            catch (SQLException es)
            {
                _log.error("Failed trying to load data.", es);
            }
        }

        return new ViewForward(new ViewURLHelper("MS2", "showList", c.getPath()));
    }

    @Jpf.Action
    protected Forward searchXTandem(MS2SearchForm form) throws Exception
    {
        form.setSearchEngine(XTandemCPipelineProvider.name);
        return search(form);
    }

    @Jpf.Action
    protected Forward searchMascot(MS2SearchForm form) throws Exception
    {
        form.setSearchEngine (MascotCPipelineProvider.name);
        return search(form);
    }

    @Jpf.Action
    protected Forward searchSequest(MS2SearchForm form) throws ServletException, SQLException, URISyntaxException, Exception
    {
        form.setSearchEngine(SequestLocalPipelineProvider.name);
        return search(form);
    }

    @Jpf.Action
    protected Forward search(MS2SearchForm form) throws ServletException, SQLException, URISyntaxException, Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        if (!"POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            // Workaround for Struts dealing with checkboxes. Browsers don't send
            // checkbox values as parameters if they're not checked, so the
            // default value needs to be false or they'll always be true. However,
            // we want the value to default to true, and be set to the value the user
            // submitted on a reshow
            form.setSaveProtocol(true);
        }

        Container c = getContainer();
        PipelineService service = PipelineService.get();

        PipeRoot pr = service.findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        String protocolName = form.getProtocol();

        URI uriRoot = pr.getUri();
        URI uriSeqRoot = MS2PipelineManager.getSequenceDatabaseRoot(pr.getContainer());
        URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
        if (uriData == null)
            HttpView.throwNotFound();

        File dirRoot = new File(uriRoot);
        File dirSeqRoot = new File(uriSeqRoot);
        File dirData = new File(uriData);
        if (!NetworkDrive.exists(dirData))
            HttpView.throwNotFound();

        ViewBackgroundInfo info =
                service.getJobBackgroundInfo(getViewBackgroundInfo(), new File(uriData));

        MS2SearchPipelineProvider provider = (MS2SearchPipelineProvider)
                PipelineService.get().getPipelineProvider(form.getSearchEngine());

        AbstractMS2SearchProtocolFactory protocolFactory = provider.getProtocolFactory();

        File dirAnalysis = protocolFactory.getAnalysisDir(dirData, protocolName);

        String error = null;
        MS2SearchPipelineProtocol protocol = null;
        if (protocolName.length() != 0)
        {
            try
            {
                File protocolFile = protocolFactory.getParametersFile(dirData, protocolName);
                if (NetworkDrive.exists(protocolFile))
                {
                    protocol = protocolFactory.loadInstance(protocolFile);

                    // Don't allow the instance file to override the protocol name.
                    protocol.setName(protocolName);
                }
                else
                {
                    protocol = protocolFactory.load(uriRoot, form.getProtocol());
                }

                form.setProtocolName(protocol.getName());
                form.setProtocolDescription(protocol.getDescription());
                form.setSequenceDBs(protocol.getDbNames());
                form.setConfigureXml(protocol.getXml());
            }
            catch (IOException eio)
            {
                error = "Failed to load requested protocol.";
            }
        }

        if (error == null && "POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            try
            {
                provider.ensureEnabled();   // throws exception if not enabled

                if ("".equals(protocolName))
                {
                    protocol = provider.getProtocolFactory().createProtocolInstance(
                            form.getProtocolName(),
                            form.getProtocolDescription(),
                            dirSeqRoot,
                            form.getSequenceDBPath(),
                            form.getSequenceDBs(),
                            form.getConfigureXml());
                    
                    protocol.setEmail(getUser().getEmail());
                    protocol.validate(uriRoot);
                    if (form.isSaveProtocol())
                    {
                        protocol.saveDefinition(uriRoot);
                    }
                }

                File[] annotatedFiles = MS2PipelineManager.getAnalysisFiles(dirData, dirAnalysis, FileStatus.ANNOTATED, c);
                File[] unprocessedFile = MS2PipelineManager.getAnalysisFiles(dirData, dirAnalysis, FileStatus.UNKNOWN, c);
                List<File> mzXMLFileList = new ArrayList<File>();
                mzXMLFileList.addAll(Arrays.asList(annotatedFiles));
                mzXMLFileList.addAll(Arrays.asList(unprocessedFile));
                File[] mzXMLFiles = mzXMLFileList.toArray(new File[mzXMLFileList.size()]);
                if (mzXMLFiles.length == 0)
                    throw new IllegalArgumentException("Analysis for this protocol is already complete.");

                protocol.getFactory().ensureDefaultParameters(dirRoot);

                File fileParameters = protocol.getParametersFile(dirData);
                // Make sure configure.xml file exists for the job when it runs.
                if (!fileParameters.exists())
                {
                    protocol.setEmail(info.getUser().getEmail());
                    protocol.saveInstance(fileParameters, info.getContainer());
                }

                AbstractMS2SearchPipelineJob job =
                        protocol.createPipelineJob(info, dirSeqRoot, mzXMLFiles, fileParameters, false);

                boolean hasStatusFile = (job.getStatusFile() != job.getLogFile());

                // If there are multiple files, and this is not a fractions job, or a Perl driven cluster
                // will do the work, then create the single-file child jobs.
                if (mzXMLFiles.length > 1)
                {
                    for (AbstractMS2SearchPipelineJob jobSingle : job.getSingleFileJobs())
                    {
                        // If fractions or for a Perl cluster, just create a placeholder for the status.
                        if (job.isFractions() || hasStatusFile)
                            jobSingle.setStatus(PipelineJob.WAITING_STATUS);
                        else
                            PipelineService.get().queueJob(jobSingle);
                    }
                }

                // If this is a single file job, or a factions job, then it requires its own processing.
                if (mzXMLFiles.length == 1 || job.isFractions())
                {
                    // If a Perl driven cluster will do the work, then just create a placeholder for the status.
                    if (hasStatusFile)
                        job.setStatus(PipelineJob.WAITING_STATUS);
                    else
                        PipelineService.get().queueJob(job);
                }

                // Forward to the job's container.
                c = info.getContainer();
            }
            catch (IllegalArgumentException ea)
            {
                error = ea.getMessage();
            }
            catch (PipelineValidationException ea)
            {
                error = ea.getMessage();
            }
            catch (IOException eio)
            {
                error = "Failure attempting to write input parameters.  Please try again.";
            }

            if (error == null || error.length() == 0)
            {
                HttpView.throwRedirect(ViewURLHelper.toPathString("Project", "begin", c.getPath()));
            }
        }

        Map<File, FileStatus> mzXmlFileStatus =
                MS2PipelineManager.getAnalysisFileStatus(dirData, dirAnalysis, info.getContainer());
        for (FileStatus status : mzXmlFileStatus.values())
        {
            // Look for unannotated data.
            if (status == FileStatus.UNKNOWN && !form.isSkipDescription())
            {
                ViewURLHelper redirectUrl = cloneViewURLHelper();
                redirectUrl.setAction("showDescribeMS2Run");
                redirectUrl.addParameter ("searchEngine", form.getSearchEngine());
                HttpView.throwRedirect(redirectUrl.getLocalURIString());
            }
        }
        
        Set<ExpRun> creatingRuns = new HashSet<ExpRun>();
        Set<File> annotationFiles = new HashSet<File>();
        try
        {
            for (File mzXMLFile : mzXmlFileStatus.keySet())
            {
                if (mzXmlFileStatus.get(mzXMLFile) == FileStatus.UNKNOWN)
                    continue;
                
                ExpRun run = ExperimentService.get().getCreatingRun(mzXMLFile, c);
                if (run != null)
                {
                    creatingRuns.add(run);
                }
                File annotationFile = MS2PipelineManager.findAnnotationFile(mzXMLFile);
                if (annotationFile != null)
                {
                    annotationFiles.add(annotationFile);
                }
            }
        }
        catch (IOException e)
        {
            String errorMessage = "While attempting to initiate the search on the mzXML file there was an " +
                "error interacting with the file system. " + ((e.getMessage() != null) ? "<br>\nDetails: " + e.getMessage() : "");
            HttpView.throwNotFound(errorMessage);
        }
        if (form.getConfigureXml().length() == 0)
        {
            form.setConfigureXml("<?xml version=\"1.0\"?>\n" +
                    "<bioml>\n" +
                    "<!-- Override default parameters here. -->\n" +
                    "</bioml>");
        }

        Map<String, String[]> sequenceDBs = new HashMap<String, String[]>();
        // If the protocol is being loaded, then the user doesn't need to pick a FASTA file,
        // it will be part of the protocol.
        // CONSIDER: Should we check for the existence of the protocol's FASTA file, since
        //           it may have been deleted?
        if ("".equals(form.getProtocol()))
        {
            try
            {
                sequenceDBs = provider.getSequenceFiles(uriSeqRoot);
                if (0 == sequenceDBs.size() && error == null)
                    error = "No databases available for searching.";
            }
            catch (IOException e)
            {
                if (error == null)
                    error = e.getMessage();
            }
        }

        String[] protocolNames = protocolFactory.getProtocolNames(uriRoot);

        HttpView v = new GroovyView("/org/labkey/ms2/pipeline/search.gm");
        v.addObject("error", error);
        v.addObject("form", form);
        v.addObject("fileStatus", mzXmlFileStatus);
        v.addObject("sequenceDBs", sequenceDBs);
        v.addObject("protocols", protocolNames);
        v.addObject("annotationFiles", annotationFiles);
        v.addObject("creatingRuns", creatingRuns);
        v.addObject("container", c);

        return _renderInTemplate(v, "Search MS2 Data", provider.getHelpTopic());
    }

    @Jpf.Action
    protected Forward updateClusterSequenceDB(SequenceDBRootForm form) throws Exception
    {
        requiresAdmin();

        if ("POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            String newSequenceRoot = form.getLocalPathRoot();
            URI root = null;
            if (newSequenceRoot != null && newSequenceRoot.length() > 0)
            {
                File file = new File(newSequenceRoot);
                root = file.toURI();
            }

            MS2PipelineManager.setSequenceDatabaseRoot(getUser(), form.getContainer(),
                    root, form.isAllowUpload());
        }
        HttpView.throwRedirect(ViewURLHelper.toPathString("Pipeline", "setup.view", form.getContainer().getPath()));
        return null;
    }

    @Jpf.Action
    protected Forward setupClusterSequenceDB() throws Exception
    {
        requiresAdmin();

        ConfigureSequenceDB page = (ConfigureSequenceDB) JspLoader.createPage(getRequest(),
                PipelineController.class, "ConfigureSequenceDB.jsp");

        URI localSequenceRoot = MS2PipelineManager.getSequenceDatabaseRoot(getContainer());
        if (localSequenceRoot != null)
        {
            File fileRoot = new File(localSequenceRoot);
            NetworkDrive.ensureDrive(fileRoot.getPath());
            if (!fileRoot.exists())
                page.setError("Sequence database root does not exist.");
            boolean allowUpload = MS2PipelineManager.allowSequenceDatabaseUploads(getUser(), getContainer());
            page.setAllowUpload(allowUpload);
            page.setLocalPathRoot(fileRoot.toString());
        }
        else
            page.setLocalPathRoot("");

        HttpView v = new JspView(page);
        return _renderInTemplate(v, "Configure Sequence Databases", null);
    }


    @Jpf.Action
    protected Forward setMascotDefaults(SetDefaultsForm form) throws Exception
    {
        return setDefaults(form,
                            MascotCPipelineProvider.name,
                            "/org/labkey/ms2/pipeline/setMascotDefaults.gm",
                            "Set Mascot Defaults",
                            "MS2-Pipeline/setMascotDefaults");
    }

    @Jpf.Action
    protected Forward setTandemDefaults(SetDefaultsForm form) throws Exception
    {
        return setDefaults(form,
                XTandemCPipelineProvider.name,
                            "/org/labkey/ms2/pipeline/setTandemDefaults.gm",
                            "Set X! Tandem Defaults",
                            "MS2-Pipeline/setTandemDefaults");
    }

    @Jpf.Action
    protected Forward setSequestDefaults(SetDefaultsForm form) throws Exception
    {
        return setDefaults(form,
                            SequestLocalPipelineProvider.name,
                            "/org/labkey/ms2/pipeline/setSequestDefaults.gm",
                            "Set Sequest Defaults",
                            "MS2-Pipeline/setSequestDefaults");
    }
    
    public static class SetDefaultsForm extends FormData
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

    protected Forward setDefaults(SetDefaultsForm form, String providerName, String gmResource,
                                  String title, String helpTopic) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        Container c = getContainer();

        File dirRoot = new File(PipelineService.get().getPipelineRootSetting(c));

        MS2SearchPipelineProvider provider = (MS2SearchPipelineProvider)
                PipelineService.get().getPipelineProvider(providerName);

        String error = "";
        if (!"POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            form.setConfigureXml(provider.getProtocolFactory().getDefaultParametersXML(dirRoot));
        }
        else
        {
            try
            {
                provider.getProtocolFactory().setDefaultParametersXML(dirRoot, form.getConfigureXml());
            }
            catch (IllegalArgumentException ea)
            {
                error = ea.getMessage();
            }
            catch (FileNotFoundException efnf)
            {
                if (efnf.getMessage().indexOf("Access") != -1)
                    error = "Access denied attempting to write defaults. Contact the server administrator.";
                else
                    error = "Failure attempting to write defaults.  Please try again.";
            }
            catch (IOException eio)
            {
                error = "Failure attempting to write defaults.  Please try again.";
            }

            if (error.length() == 0)
            {
                HttpView.throwRedirect(ViewURLHelper.toPathString("Project", "begin", c.getPath()));
            }
        }

        HttpView v = new GroovyView(gmResource);
        v.addObject("error", error);
        v.addObject("form", form);
        return _renderInTemplate(v, title, helpTopic);
    }

    public static class SequenceDBForm extends FormData
    {
        private FormFile sequenceDBFile;

        public FormFile getSequenceDBFile()
        {
            return sequenceDBFile;
        }

        public void setSequenceDBFile(FormFile sequenceDBFile)
        {
            this.sequenceDBFile = sequenceDBFile;
        }
    }

    @Jpf.Action
    protected Forward addSequenceDB(SequenceDBForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        Container c = getContainer();
        String error = "";
        if ("POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            FormFile ff = form.getSequenceDBFile();
            String name = (ff == null ? "" : ff.getFileName());
            if (ff == null || ff.getFileSize() == 0)
                error = "Please specify a FASTA file.";
            else if (name.indexOf(File.separatorChar) != -1 || name.indexOf('/') != -1)
                error = "Invalid sequence database name '" + name + "'.";
            else
            {
                BufferedReader reader = null;
                try
                {
                    reader = new BufferedReader(new InputStreamReader(ff.getInputStream()));

                    MS2PipelineManager.addSequenceDB(c, name, reader);
                }
                catch (IllegalArgumentException ea)
                {
                    error = ea.getMessage();
                }
                catch (IOException eio)
                {
                    error = "Failure attempting to write sequence database.  Please try again.";
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

            if (error.length() == 0)
            {
                HttpView.throwRedirect(ViewURLHelper.toPathString("Project", "begin", c.getPath()));
            }
        }

        HttpView v = new GroovyView("/org/labkey/ms2/pipeline/addSequenceDB.gm");
        v.addObject("error", error);
        v.addObject("form", form);
        v.addObject("form", form);
        return _renderInTemplate(v, "Add Sequence Database", "MS2-Pipeline/addSequenceDB");
    }

    public static class LogFileForm extends FormData
    {
        private String file;

        public String getFile()
        {
            return file;
        }

        public void setFile(String file)
        {
            this.file = file;
        }
    }

    @Jpf.Action
    protected Forward showCreateMS2Protocol(MS2ProtocolForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        form.setSearchEngine(this.getRequest().getParameter("searchEngine"));
        PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
        if (pr == null || !URIUtil.exists(pr.getUri()))
            return HttpView.throwNotFound();

        URI uriRoot = pr.getUri();
        String templateName = form.getTemplateName();

        if (null == templateName)
        {
            List<String> templateNames = MassSpecProtocolFactory.get().getTemplateNames(uriRoot);
            if (templateNames.size() == 1)
                templateName = templateNames.get(0);
            else
            {
                PickTemplateView pickView = new PickTemplateView(form, templateNames);
                return _renderInTemplate(pickView, "Create MS2 Protocol", "MS2-Pipeline/showCreateMS2Protocol");
            }
        }

        XarTemplate template = MassSpecProtocolFactory.get().getXarTemplate(uriRoot, templateName);

        PopulateTemplateView ptv = new PopulateTemplateView(form, template);
        return _renderInTemplate(ptv, "Create MS2 Protocol", "MS2-Pipeline/showCreateMS2Protocol");

/*
        GroovyView gv = new GroovyView("/org/labkey/ms2/pipeline/ms2Protocol.gm");
        gv.setTitle("Create MS2 Protocol");

        String name = form.getName();
        if (name != null && name.length() > 0)
        {
            MassSpecProtocol protocol = MassSpecProtocolFactory.get().load(uriRoot, name);
            form.setDescriptionSamplePrep(protocol.getDescriptionSamplePrep());
            form.setDescriptionLcms(protocol.getDescriptionLcms());
        }
        gv.addObject("form", form);

        return _renderInTemplate(gv, "Create MS2 Protocol", "MS2-Pipeline/showCreateMS2Protocol");
*/
    }

    private static class PickTemplateView extends WebPartView
    {
        private final List<String> templates;
        private final MS2ProtocolForm form;

        public PickTemplateView(MS2ProtocolForm form, List<String> templates)
        {
            this.templates = templates;
            this.form = form;
        }

        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            out.write("<span class=\"heading-1\">");
            out.write("To create a protocol, start with a protocol template and fill in values specified by the template.<br><br>");
            out.write("Which protocol template would you like to start with?<br>");
            out.write("<ul>");
            for (String t : templates)
            {
                out.write("<a href=\"");
                out.write("showCreateMS2Protocol.view?templateName=");
                out.write(PageFlowUtil.filter(t));
                out.write("&path=");
                out.write(PageFlowUtil.encode(form.getPath()));
                out.write("&searchEngine=");
                out.write(PageFlowUtil.encode(form.getSearchEngine()));
                out.write("\">");
                out.write(PageFlowUtil.filter(t));
                out.write("</a><br>\n");
            }
            out.write("</ul>");
        }
    }

    private static class PopulateTemplateView extends WebPartView
    {
        private XarTemplate template;
        private MS2ProtocolForm form;

        public PopulateTemplateView(MS2ProtocolForm form, XarTemplate template)
        {
            this.template = template;
            this.form = form;
        }


        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            DisplayColumn nameCol = SimpleInputColumn.create("name", "");
            nameCol.setCaption("Protocol Name");
            DataRegion dr = new DataRegion();
            dr.setName("protocolRegion");
            dr.addColumn(nameCol);
            dr.addHiddenFormField("templateName", template.getName());
            dr.addHiddenFormField("path", form.getPath());
            dr.addHiddenFormField("searchEngine", form.getSearchEngine());
            dr.addColumns(template.getSubstitutionFields());

            ButtonBar bb = new ButtonBar();
            ActionButton ab = new ActionButton("createMS2Protocol.post", "Submit");
            bb.add(ab);
            dr.setButtonBar(bb);

            if (null != form.getError())
                out.write("<span class=\"labkey-error\">" + form.getError() + "</span>");

            RenderContext rc = new RenderContext(getViewContext());
            rc.setMode(DataRegion.MODE_INSERT);

            dr.render(rc, out);
        }
    }


    @Jpf.Action
    protected Forward createMS2Protocol(MS2ProtocolForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        form.setSearchEngine(this.getRequest().getParameter("searchEngine"));
        PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();
        URI uriRoot = pr.getUri();

        MassSpecProtocol protocol = new MassSpecProtocol(form.getName(), form.getTemplateName(), form.getTokenReplacements());
        try
        {
            protocol.validate(uriRoot);
        }
        catch (PipelineValidationException e)
        {
            form.setError(e.getMessage());
            return showCreateMS2Protocol(form);
        }

        try
        {
            protocol.saveDefinition(uriRoot);
        }
        catch (IOException e)
        {
            form.setError(e.getMessage());
            return showCreateMS2Protocol(form);
        }

        ViewURLHelper redirectUrl = cloneViewURLHelper();
        redirectUrl.setAction("showDescribeMS2Run");
        redirectUrl.deleteParameters();
        redirectUrl.addParameter("path", form.getPath());
        redirectUrl.addParameter("searchEngine", form.getSearchEngine());
        HttpView.throwRedirect(redirectUrl.getLocalURIString());

        return null;
    }

    @Jpf.Action
    protected Forward redescribeFiles(MS2ExperimentForm form) throws Exception
    {
        Container c = getContainer(ACL.PERM_DELETE);

        PipelineService service = PipelineService.get();
        PipeRoot pr = service.findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
        if (uriData == null)
            HttpView.throwNotFound();

        File[] mzXMLFiles = new File(uriData).listFiles(MS2PipelineManager.getAnalyzeFilter());
        for (File mzXMLFile : mzXMLFiles)
        {
            ExpRun run = ExperimentService.get().getCreatingRun(mzXMLFile, c);
            if (run != null)
            {
                ExperimentService.get().deleteExperimentRunsByRowIds(c, getUser(), run.getRowId());
            }
            File annotationFile = MS2PipelineManager.findAnnotationFile(mzXMLFile, new HashSet<File>(), new HashSet<File>());
            if (annotationFile != null)
            {
                annotationFile.delete();
            }
        }
        ViewURLHelper redirectURL = getViewURLHelper().clone().setAction("showDescribeMS2Run.view");
        return new ViewForward(redirectURL);
    }

    @Jpf.Action
    protected Forward showDescribeMS2Run(MS2ExperimentForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        form.setSearchEngine(this.getRequest().getParameter("searchEngine"));

        Container c = getContainer();

        PipeRoot pr = PipelineService.get().findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        URI uriRoot = pr.getUri();
        URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
        if (uriData == null)
            HttpView.throwNotFound();

        ExperimentService.get().ensureDefaultSampleSet();
        ExpSampleSet activeMaterialSource = ExperimentService.get().ensureActiveSampleSet(getContainer());

        ViewBackgroundInfo info =
                PipelineService.get().getJobBackgroundInfo(getViewBackgroundInfo(), new File(uriData));

        Map<File, FileStatus> mzXmlFileStatus =
            MS2PipelineManager.getAnalysisFileStatus(new File(uriData), null, info.getContainer());
        String[] protocolNames = MassSpecProtocolFactory.get().getProtocolNames(uriRoot);

        String protocolSharing = form.getProtocolSharing();

        boolean showProtocolPage = false;
        if (null == protocolSharing)
            showProtocolPage = true;
        else if ("none".equals(protocolSharing))
        {
            for (String formProtocolName : form.getProtocolNames())
            {
                if (null == StringUtils.trimToNull(formProtocolName))
                {
                    form.setError(0, "Choose a protocol for each run");
                    showProtocolPage = true;
                    break;
                }
            }
        }
        else if (("share".equals(protocolSharing) && null == StringUtils.trimToNull(form.getSharedProtocol()))
            || ("fractions".equals(protocolSharing) && (null == StringUtils.trimToNull(form.getFractionProtocol()))))
        {
            form.setError(0, "Choose a protocol");
            showProtocolPage = true;
        }

        if (showProtocolPage)
        {
            return showPickProtocol(form, mzXmlFileStatus, protocolNames);
        }
        else
        {
            DescribeRunPage drv = (DescribeRunPage) JspLoader.createPage(getRequest(), PipelineController.class, "describe.jsp");
            drv.setMzXmlFileStatus(mzXmlFileStatus);
            drv.setForm(form);
            ExpSampleSet[] materialSources;
            if (activeMaterialSource.getLSID().equals(ExperimentService.get().getDefaultSampleSetLsid()))
            {
                materialSources = new ExpSampleSet[] { activeMaterialSource };
            }
            else
            {
                materialSources = new ExpSampleSet[] { activeMaterialSource, ExperimentService.get().ensureDefaultSampleSet() };
            }

            Map<Integer, ExpMaterial[]> materialSourceMaterials = new HashMap<Integer, ExpMaterial[]>();
            for (ExpSampleSet source : materialSources)
            {
                ExpMaterial[] materials = ExperimentService.get().getMaterialsForSampleSet(source.getMaterialLSIDPrefix(), source.getContainer());
                materialSourceMaterials.put(source.getRowId(), materials);
            }
            drv.setSampleSets(materialSources);
            drv.setMaterialSourceMaterials(materialSourceMaterials);
            drv.setController(this);
            if ("share".equals(protocolSharing))
            {
                MassSpecProtocol prot = MassSpecProtocolFactory.get().load(uriRoot, form.getSharedProtocol());
                ExperimentArchiveDocument doc = prot.getInstanceXar(uriRoot);
                ExperimentArchiveDocument[] xarDocs = new ExperimentArchiveDocument[mzXmlFileStatus.size()];

                Arrays.fill(xarDocs, doc);
                drv.setXarDocs(xarDocs);
                return _renderInTemplate(new JspView(drv), "Describe MS2 Runs", "pipelineSearch");
            }
            else if ("fractions".equals(protocolSharing))
            {
                //throw new UnsupportedOperationException("Fractions not currently supported");
                MassSpecProtocol prot = MassSpecProtocolFactory.get().load(uriRoot, form.getFractionProtocol());
                ExperimentArchiveDocument doc = prot.getInstanceXar(uriRoot);
                ExperimentArchiveDocument[] xarDocs = new ExperimentArchiveDocument[1];

                Arrays.fill(xarDocs, doc);
                drv.setXarDocs(xarDocs);
                return _renderInTemplate(new JspView(drv), "Describe MS2 Runs", "pipelineSearch");

            }
            else
            {
                MassSpecProtocol[] protocols = getProtocols(form, uriRoot);
                ExperimentArchiveDocument xarDocs[] = new ExperimentArchiveDocument[protocols.length];

                for (int i = 0; i < protocols.length; i++)
                {
                    if (protocols[i] == null)
                    {
                        return showPickProtocol(form, mzXmlFileStatus, protocolNames);
                    }
                    xarDocs[i] = protocols[i].getInstanceXar(uriRoot);
                }

                drv.setXarDocs(xarDocs);
                return _renderInTemplate(new JspView(drv), "Describe MS2 Runs", "pipelineSearch");
            }

        }
    }

    private Forward showPickProtocol(MS2ExperimentForm form, Map<File, FileStatus> mzXmlFileStatus, String[] protocolNames)
        throws Exception
    {
        GroovyView v = new GroovyView("/org/labkey/ms2/pipeline/pickProtocol.gm");
        v.addObject("form", form);
        v.addObject("fileStatus", mzXmlFileStatus);
        v.addObject("protocols", protocolNames);
        //TODO: do we need?
        v.addObject("searchEngine", form.getSearchEngine());

        v.setTitle("Specify Experimental Protocol");
        return _renderInTemplate(v, "Describe MS2 Runs", "pipelineSearch");
    }

    private MassSpecProtocol[] getProtocols(MS2ExperimentForm form, URI uriRoot) throws IOException
    {
        MassSpecProtocol prot = null;

        if ("share".equals(form.getProtocolSharing()))
        {
            try
            {
                prot = MassSpecProtocolFactory.get().load(uriRoot, form.getSharedProtocol());
            }
            catch (IOException eio)
            {
                _log.warn("Loading protocol " + form.getSharedProtocol(), eio);
                form.setError(0, "Failed to Load Protocol" + form.getSharedProtocol() + " - " + eio.toString());
            }

            MassSpecProtocol[] protocols = new MassSpecProtocol[form.getFileNames().length];
            Arrays.fill(protocols, prot);

            return protocols;
        }
        else if ("fractions".equals(form.getProtocolSharing()))
        {
            try
            {
                prot = MassSpecProtocolFactory.get().load(uriRoot, form.getFractionProtocol());
            }
            catch (IOException eio)
            {
                _log.warn("Loading protocol " + form.getSharedProtocol(), eio);
                form.setError(0, "Failed to Load Protocol " + form.getFractionProtocol() + " - " + eio);
            }

            return new MassSpecProtocol[] {prot};
        }
        else
        {
            Map<String, MassSpecProtocol> protMap = new HashMap<String, MassSpecProtocol>();
            MassSpecProtocol[] protocols = new MassSpecProtocol[form.getFileNames().length];
            for (int i =0; i < protocols.length; i++)
            {
                String protocolName = form.getProtocolNames()[i];
                if (null == protocolName)
                    continue;


                prot = protMap.get(protocolName);
                if (null == prot)
                {
                    try
                    {
                        prot  = MassSpecProtocolFactory.get().load(uriRoot, protocolName);
                        protMap.put(protocolName, prot);
                    }
                    catch (IOException eio)
                    {
                        _log.warn("Couldn't load protocol " + protocolName);
                        form.setError(i, "Couldn't load protocol " + protocolName + " - " + eio.toString());
                    }
                }

                protocols[i] = prot;

            }
            return protocols;
        }

    }

    @Jpf.Action
    protected Forward describeMS2Run(MS2ExperimentForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);
        form.setSearchEngine(this.getRequest().getParameter("searchEngine"));

        PipelineService service = PipelineService.get();
        PipeRoot pr = service.findPipelineRoot(getContainer());
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();
        URI uriRoot = pr.getUri();
        URI uriData = URIUtil.resolve(pr.getUri(getContainer()), form.getPath());
        File dirData = new File(uriData);
        int numRuns;
        if ("fractions".equals(form.getProtocolSharing()))
            numRuns=1;
        else
            numRuns= form.getProtocolNames().length;

        for (int i = 0; i < numRuns; i++)
        {
            final String runName = form.getRunNames()[i];

            if (runName == null || runName.length() == 0)
                form.setError(i, "Please specify a run name.");
        }

        MassSpecProtocol[] protocols = getProtocols(form, uriRoot);
        for (int i = 0; i < numRuns; i++)
        {
            final MassSpecProtocol protocol = protocols[i];
            if (protocol != null)
            {
                MassSpecProtocol.RunInfo runInfo = form.getRunInfos()[i];
                String error = protocol.validateSubstitution(uriRoot, runInfo);
                if (null != error)
                    form.setError(i, error);
            }
        }

        if (!form.hasErrors())
        {
            // Kick off jobs, if no form entry errors.
            for (int i = 0; i < numRuns; i++)
            {
                final String runName = form.getRunNames()[i];
                final MassSpecProtocol protocol = protocols[i];

                MassSpecProtocol.RunInfo runInfo = form.getRunInfos()[i];
                runInfo.setRunName(runName);

                //todo- how to handle multi mzxml files per run
                //  need to point MS2Fraction row to a data object representing the search results

                runInfo.setRunFileName(form.getFileNames()[i]);

                File mzXMLFile = new File(dirData, form.getFileNames()[i]);
                String baseName = FileUtil.getBaseName(mzXMLFile);

                // quick hack to get the search to go forward.
                if ("fractions".equals(form.getProtocolSharing()))
                    baseName = MS2PipelineManager._allFractionsMzXmlFileBase;

                File fileInstance = MS2PipelineManager.getAnnotationFile(dirData, baseName);
                try
                {
                    protocol.saveInstance(uriRoot, fileInstance, runInfo);
                }
                catch (IOException e)
                {
                    String message = e.getMessage();
                    if (message == null || message.length() == 0)
                    {
                        message = "Unable to save protocol";
                    }
                    form.setError(0, message);
                    return showDescribeMS2Run(form);
                }

                String dataDescription = MS2PipelineManager.getDataDescription(dirData, baseName, protocol.getName());
                // The experiment needs to be loaded into the right container for where the mzXML file
                // lives on disk, not where the generated XAR file sits. This is important for when
                // the container's pipeline is configured to mirror the file system hierarchy.
                ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), mzXMLFile);
                ExperimentPipelineJob job = new ExperimentPipelineJob(info, fileInstance, dataDescription, false);
                PipelineService.get().queueJob(job);
            }

            ViewURLHelper redirectUrl = cloneViewURLHelper();
            redirectUrl.setAction("search");
            redirectUrl.deleteParameters();
            redirectUrl.addParameter("skipDescription", "true");
            redirectUrl.addParameter("provider", "ms2");
            redirectUrl.addParameter("action", "search");
            redirectUrl.addParameter("path", form.getPath());
            redirectUrl.addParameter("searchEngine", form.getSearchEngine());
            HttpView.throwRedirect(redirectUrl.getLocalURIString());
        }

        return showDescribeMS2Run(form);
    }

    public static class MS2ExperimentForm extends ViewForm
    {
        private String searchEngine = XTandemCPipelineProvider.name;
        private String path = "";
        private String protocolSharing;
        private String sharedProtocol;
        private String fractionProtocol;
        private String[] fileNames;
        private String[] protocolNames;
        private String[] runNames;
        private String[] errors;
        private MassSpecProtocol.RunInfo[] runInfos;

        public void reset(ActionMapping am, HttpServletRequest request)
        {
            super.reset(am, request);
            int size = 0;
            try
            {
                size = Integer.parseInt(request.getParameter("size"));
            }
            catch (Exception e)
            {
            }

            fileNames = new String[size];
            protocolNames = new String[size];
            runNames = new String[size];
            errors = new String[size];
            runInfos = new MassSpecProtocol.RunInfo[size];
            for (int i = 0; i < size; i++)
            {
                String strCount = request.getParameter("parameterCounts[" + i + "]");
                if (null != StringUtils.trimToNull(strCount))
                {
                    int parameterCount = Integer.parseInt(strCount);
                    strCount = request.getParameter("materialCounts[" + i + "]");
                    int materialCount = Integer.parseInt(strCount);

                    runInfos[i] = new MassSpecProtocol.RunInfo(materialCount, parameterCount);
                }
                else
                    runInfos[i] = new MassSpecProtocol.RunInfo(0, 0);
            }
        }


        public boolean hasErrors()
        {
            for (String error : errors)
            {
                if (error != null)
                    return true;
            }
            return false;
        }

        public String getError(int i)
        {
            if (null != errors)
                return errors[i];

            return null;
        }

        public void setError(int i, String error)
        {
            this.errors[i] = error;
        }

        public String getSearchEngine()
        {
            return searchEngine;
        }

        public void setSearchEngine(String searchEngine)
        {
            this.searchEngine = searchEngine;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            if (path == null)
            {
                path = "";
            }
            this.path = path;
        }

        public String[] getFileNames()
        {
            return fileNames;
        }

        public void setFileNames(String[] fileNames)
        {
            this.fileNames = fileNames;
        }

        public String[] getProtocolNames()
        {
            return protocolNames;
        }

        public void setProtocolNames(String[] protocolNames)
        {
            this.protocolNames = protocolNames;
        }

        public String[] getRunNames()
        {
            return runNames;
        }

        public void setRunNames(String[] runNames)
        {
            this.runNames = runNames;
        }

        public MassSpecProtocol.RunInfo[] getRunInfos()
        {
            return runInfos;
        }

        public void setRunInfos(MassSpecProtocol.RunInfo[] runInfos)
        {
            this.runInfos = runInfos;
        }

        public String getProtocolSharing()
        {
            return protocolSharing;
        }

        public void setProtocolSharing(String protocolSharing)
        {
            this.protocolSharing = protocolSharing;
        }

        public String getSharedProtocol()
        {
            return sharedProtocol;
        }

        public void setSharedProtocol(String sharedProtocol)
        {
            this.sharedProtocol = sharedProtocol;
        }

        public String getFractionProtocol() {
            return fractionProtocol;
        }

        public void setFractionProtocol(String fractionProtocol) {
            this.fractionProtocol = fractionProtocol;
        }

    }

    public static class MS2ProtocolForm extends ViewForm
    {
        private String searchEngine;
        private String path;
        private String name;
        private String templateName;
        private String error;

        public String getSearchEngine()
        {
            return searchEngine;
        }

        public void setSearchEngine(String searchEngine)
        {
            this.searchEngine = searchEngine;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Map<String, String> getTokenReplacements() throws SQLException
        {
            PipeRoot pRoot = PipelineService.get().findPipelineRoot(getContainer());
            XarTemplate template = MassSpecProtocolFactory.get().getXarTemplate(pRoot.getUri(), templateName);
            Set<String> tokenNames = template.getTokens();
            Map<String, String> replacements = new HashMap<String, String>();

            for (String token : tokenNames)
            {
                String val = getRequest().getParameter(token);
                if (null != val)
                    replacements.put(token, val);
            }

            return replacements;
        }

        public String getTemplateName()
        {
            return templateName;
        }

        public void setTemplateName(String templateName)
        {
            this.templateName = templateName;
        }

        public String getError()
        {
            return error;
        }

        public void setError(String error)
        {
            this.error = error;
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
