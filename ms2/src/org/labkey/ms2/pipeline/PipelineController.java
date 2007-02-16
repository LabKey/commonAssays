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
import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExperimentService;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.labkey.api.jsp.JspLoader;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.RunForm;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Fraction;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.protocol.*;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.util.*;
import org.labkey.api.view.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

import org.labkey.ms2.pipeline.FileStatus;

import org.labkey.ms2.pipeline.MascotClientImpl;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.api.pipeline.PipelineProtocolFactory;
import org.labkey.api.pipeline.PipelineValidationException;

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
        trailConfig.setHelpTopic(new HelpTopic(helpTopic, HelpTopic.Area.COMMON));

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

        PipelineService.PipeRoot pr = PipelineService.get().findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        URI uriUpload = URIUtil.resolve(pr.getUri(c), path);
        File fileUpload = new File(uriUpload);
        File[] files = new File[] { fileUpload };
        if (fileUpload.isDirectory())
            files = fileUpload.listFiles(MS2PipelineManager.getUploadFilter());

        for (File file : files)
        {
            int extParts = 1;
            if (file.getName().endsWith(".xml"))
                extParts = 2;
            String baseName = MS2PipelineManager.getBaseName(file, extParts);
            File dir = file.getParentFile();
            // If the data was created by our pipeline, try to get the name
            // to look like the normal generated name.

            String protocolName;
            File dirDataOriginal;
            String description;
            if (MS2PipelineManager.isMascotResultFile(file))
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
                else if (MS2PipelineManager.isMs2ResultsFile(file))
                {
                    MS2Manager.addRunToQueue(info, file, description, false);
                }
                else if (MS2PipelineManager.isMascotResultFile(file))
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

        return new ViewForward(new ViewURLHelper(getRequest(), "MS2", "showList", c.getPath()));
    }

    public static class SearchForm extends FormData
    {
        private String path;
        private String protocol = "";
        private String protocolName = "";
        private String protocolDescription = "";
        private String sequenceDBPath = "";
        private String[] sequenceDBs = new String[0];
        private String configureXml = "";
        private boolean skipDescription;
        private String searchEngine = "X!Tandem";
        private boolean saveProtocol = false;

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public String[] getSequenceDBs()
        {
            return sequenceDBs;
        }

        public void setSequenceDBs(String[] sequenceDBs)
        {
            this.sequenceDBs = (sequenceDBs == null ? new String[0] : sequenceDBs);
        }

        public String getConfigureXml()
        {
            return configureXml;
        }

        public void setConfigureXml(String configureXml)
        {
            this.configureXml = (configureXml == null ? "" : configureXml);
        }

        public String getProtocol()
        {
            return protocol;
        }

        public void setProtocol(String protocol)
        {
            this.protocol = (protocol == null ? "" : protocol);
        }

        public String getProtocolName()
        {
            return protocolName;
        }

        public void setProtocolName(String protocolName)
        {
            this.protocolName = (protocolName == null ? "" : protocolName);
        }

        public String getProtocolDescription()
        {
            return protocolDescription;
        }

        public void setProtocolDescription(String protocolDescription)
        {
            this.protocolDescription = (protocolDescription == null ? "" : protocolDescription);
        }

        public String getSequenceDBPath()
        {
            return sequenceDBPath;
        }

        public void setSequenceDBPath(String sequenceDBPath)
        {
            this.sequenceDBPath = sequenceDBPath;
        }

        public boolean isSkipDescription()
        {
            return skipDescription;
        }

        public void setSkipDescription(boolean skipDescription)
        {
            this.skipDescription = skipDescription;
        }

        public String getSearchEngine()
        {
            return searchEngine;
        }

        public void setSearchEngine(String searchEngine)
        {
            this.searchEngine = searchEngine;
        }

        public boolean isSaveProtocol()
        {
            return saveProtocol;
        }

        public void setSaveProtocol(boolean saveProtocol)
        {
            this.saveProtocol = saveProtocol;
        }
    }

    @Jpf.Action
    protected Forward searchXTandem(SearchForm form) throws Exception
    {
        form.setSearchEngine ("X!Tandem");
        return search(form);
    }

    @Jpf.Action
    protected Forward searchMascot(SearchForm form) throws Exception
    {
        form.setSearchEngine ("Mascot");
        return search(form);
    }

    @Jpf.Action
    protected Forward searchSequest(SearchForm form) throws ServletException, SQLException, URISyntaxException, Exception
    {
        form.setSearchEngine ("Sequest");
        return search(form);
    }

    @Jpf.Action
    protected Forward search(SearchForm form) throws ServletException, SQLException, URISyntaxException, Exception
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

        PipelineService.PipeRoot pr = service.findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        URI uriRoot = pr.getUri();
        URI sequenceRoot = MS2PipelineManager.getSequenceDatabaseRoot(pr.getContainer());
        URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
        if (uriData == null)
            HttpView.throwNotFound();

        Map<String, String[]> sequenceDBs = MS2PipelineManager.getSequenceDBNames(sequenceRoot, form.getSearchEngine());
        PipelineProtocolFactory searchProtocol = MS2PipelineManager.getSearchProtocolInstance(form.getSearchEngine());
        String[] protocolNames = searchProtocol.getProtocolNames(uriRoot);

        String error = null;
        PipelineProtocol protocol = null;
        String protocolName = form.getProtocol();
        if (protocolName.length() != 0)
        {
            try
            {
                File protocolFile = MS2PipelineManager.getConfigureXMLFile(uriData, protocolName, form.getSearchEngine());
                if (protocolFile.exists())
                {
                    protocol = searchProtocol.loadInstance(protocolFile);

                    // Don't allow the instance file to override the protocol name.
                    protocol.setName(protocolName);
                }
                else
                {
                    protocol = searchProtocol.load(uriRoot, form.getProtocol());
                }

                form.setProtocolName(protocol.getName());

                // TODO: wch - define a class to be inherited
                if ("mascot".equalsIgnoreCase(form.getSearchEngine()))
                {
                    MascotSearchProtocol specificProtocol = (MascotSearchProtocol) protocol;
                    form.setProtocolDescription(specificProtocol.getDescription());
                    form.setSequenceDBs(specificProtocol.getDbNames());
                    form.setConfigureXml(specificProtocol.getXml());
                }
                else if("sequest".equalsIgnoreCase(form.getSearchEngine()))
                {
                    SequestSearchProtocol specificProtocol = (SequestSearchProtocol) protocol;
                    form.setProtocolDescription(specificProtocol.getDescription());
                    form.setSequenceDBs(specificProtocol.getDbNames());
                    form.setConfigureXml(specificProtocol.getXml());
                }
                else
                {
                    // we take X! Tandem as the default case
                    XTandemSearchProtocol specificProtocol = (XTandemSearchProtocol) protocol;
                    form.setProtocolDescription(specificProtocol.getDescription());
                    form.setSequenceDBs(specificProtocol.getDbNames());
                    form.setConfigureXml(specificProtocol.getXml());
                }
            }
            catch (IOException eio)
            {
                error = "Failed to load requested protocol.";
            }
        }

        if (0 == sequenceDBs.size())
        {
            if (null == error && "mascot".equalsIgnoreCase(form.getSearchEngine()))
            {
                AppProps appProps = AppProps.getInstance();
                if (appProps.hasMascotServer())
                {
                    MascotClientImpl mascotClient = new MascotClientImpl(appProps.getMascotServer(), null);
                    mascotClient.setProxyURL(appProps.getMascotHTTPProxy());
                    String connectivityResult = mascotClient.testConnectivity(false);
                    if ("".equals(connectivityResult))
                        error = "Mascot server has not database available for searching.";
                    else
                        error = connectivityResult;
                }
                else
                {
                    error = "Mascot server has not been specified in site customization.";
                }
            }
        }
        
        if (error == null && "POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            try
            {
                if (form.getProtocol().length() == 0)
                {
                    String[] seqDBs = form.getSequenceDBs();
                    if ("mascot".equalsIgnoreCase(form.getSearchEngine()))
                    {
                        // we check that the database exist locally
                        // so that Mascot2XML will work
                        File fileSequenceDB = new File(sequenceRoot.getPath(), seqDBs[0]);
                        if (!fileSequenceDB.exists())
                            throw new IllegalArgumentException("Sequence database '" + seqDBs[0] + "' is not found in local FASTA root");
                    }
                    else
                    {
                        String seqDBPath = form.getSequenceDBPath();
                        if (seqDBPath != null && seqDBPath.length() > 0)
                        {
                            String[] seqFullDBs = new String[seqDBs.length];
                            for (int i = 0; i < seqDBs.length; i++)
                                seqFullDBs[i] = seqDBPath + seqDBs[i];
                            seqDBs = seqFullDBs;
                        }
                    }

                    // TODO: wch - define a class to be inherited
                    if ("mascot".equalsIgnoreCase(form.getSearchEngine()))
                    {
                        AppProps appProps = AppProps.getInstance();
                        String mascotServer = appProps.getMascotServer();
                        String mascotHTTPProxy = appProps.getMascotHTTPProxy();
                        if (!appProps.hasMascotServer() || 0==mascotServer.length())
                            throw new IllegalArgumentException("Mascot server has not been specified in site customization.");

                        MascotSearchProtocol specificProtocol
                            = new MascotSearchProtocol(
                                form.getProtocolName(),
                                form.getProtocolDescription(),
                                seqDBs,
                                form.getConfigureXml());
                        specificProtocol.setEmail(getUser().getEmail());
                        specificProtocol.setMascotServer(mascotServer);
                        specificProtocol.setMascotHTTPProxy(mascotHTTPProxy);
                        protocol = specificProtocol;
                    }
                    else if("sequest".equalsIgnoreCase(form.getSearchEngine()))
                    {
                        SequestSearchProtocol specificProtocol
                            = new SequestSearchProtocol(
                                form.getProtocolName(),
                                form.getProtocolDescription(),
                                seqDBs,
                                form.getConfigureXml());
                        specificProtocol.setEmail(getUser().getEmail());
                        protocol = specificProtocol;
                    }
                    else
                    {
                        // we take X! Tandem as the default case
                        XTandemSearchProtocol specificProtocol
                            = new XTandemSearchProtocol(
                                form.getProtocolName(),
                                form.getProtocolDescription(),
                                seqDBs,
                                form.getConfigureXml());
                        specificProtocol.setEmail(getUser().getEmail());
                        protocol = specificProtocol;
                    }
                    protocol.validate(uriRoot);
                    if (form.isSaveProtocol())
                    {
                        protocol.saveDefinition(uriRoot);
                    }
                }

                ViewBackgroundInfo info =
                        service.getJobBackgroundInfo(getViewBackgroundInfo(), new File(uriData));
                MS2PipelineManager.runAnalysis(info,
                        uriRoot,
                        uriData,
                        sequenceRoot,
                        protocol,
                        form.getSearchEngine());

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

        ViewBackgroundInfo info =
                service.getJobBackgroundInfo(getViewBackgroundInfo(), new File(uriData));
        Map<File, FileStatus> mzXmlFileStatus =
                MS2PipelineManager.getAnalysisFileStatus(uriData, protocolName, info.getContainer(), form.getSearchEngine());
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

        Set<ExperimentRun> creatingRuns = new HashSet<ExperimentRun>();
        Set<File> annotationFiles = new HashSet<File>();
        File[] mzXMLFiles = new File(uriData).listFiles(MS2PipelineManager.getAnalyzeFilter());
        for (File mzXMLFile : mzXMLFiles)
        {
            ExperimentRun run = ExperimentService.get().getCreatingRun(mzXMLFile, c);
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

        if (form.getConfigureXml().length() == 0)
        {
            form.setConfigureXml("<?xml version=\"1.0\"?>\n" +
                    "<bioml>\n" +
                    "<!-- Override default parameters here. -->\n" +
                    "</bioml>");
        }
        HttpView v = new GroovyView("/org/labkey/ms2/pipeline/search.gm");
        v.addObject("error", error);
        v.addObject("form", form);
        v.addObject("fileStatus", mzXmlFileStatus);
        v.addObject("sequenceDBs", sequenceDBs);
        v.addObject("protocols", protocolNames);
        v.addObject("annotationFiles", annotationFiles);
        v.addObject("creatingRuns", creatingRuns);
        v.addObject("container", c);
        return _renderInTemplate(v, "Search MS2 Data", "MS2-Pipeline/search");
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


    public static class MascotDefaultsForm extends FormData
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

    @Jpf.Action
    protected Forward setMascotDefaults(MascotDefaultsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        Container c = getContainer();

        URI uriRoot = PipelineService.get().getPipelineRoot(c);

        String error = "";
        if ("POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            try
            {
                MS2PipelineManager.setDefaultInputXML(uriRoot, form.getConfigureXml(), "mascot");
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
        else
        {
//TODO: need to get the default based on the engine!
//      so, MS2PipelineManager will need to get additional parameters
            form.setConfigureXml(MS2PipelineManager.getDefaultInputXML(uriRoot, "mascot"));
        }

        HttpView v = new GroovyView("/org/labkey/ms2/pipeline/setMascotDefaults.gm");
        v.addObject("error", error);
        v.addObject("form", form);
        return _renderInTemplate(v, "Set Mascot Defaults", "MS2-Pipeline/setMascotDefaults");
    }

    public static class TandemDefaultsForm extends FormData
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

    @Jpf.Action
    protected Forward setTandemDefaults(TandemDefaultsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        Container c = getContainer();

        URI uriRoot = PipelineService.get().getPipelineRoot(c);

        String error = "";
        if ("POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            try
            {
                MS2PipelineManager.setDefaultInputXML(uriRoot, form.getConfigureXml(), "X!Tandem");
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
        else
        {
            form.setConfigureXml(MS2PipelineManager.getDefaultInputXML(uriRoot, "X!Tandem"));
        }

        HttpView v = new GroovyView("/org/labkey/ms2/pipeline/setTandemDefaults.gm");
        v.addObject("error", error);
        v.addObject("form", form);
        return _renderInTemplate(v, "Set X!Tandem Defaults", "MS2-Pipeline/setTandemDefaults");
    }
    public static class SequestDefaultsForm extends FormData
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

    @Jpf.Action
    protected Forward setSequestDefaults(SequestDefaultsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        Container c = getContainer();

        URI uriRoot = PipelineService.get().getPipelineRoot(c);

        String error = "";
        if ("POST".equalsIgnoreCase(getRequest().getMethod()))
        {
            try
            {
                MS2PipelineManager.setDefaultInputXML(uriRoot, form.getConfigureXml(), "sequest");
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
        else
        {
//wch: mascotdev
            form.setConfigureXml(MS2PipelineManager.getDefaultInputXML(uriRoot, "sequest"));
//END-wch: mascotdev
        }

        HttpView v = new GroovyView("/org/labkey/ms2/pipeline/setSequestDefaults.gm");
        v.addObject("error", error);
        v.addObject("form", form);
        return _renderInTemplate(v, "Set Sequest Defaults", "MS2-Pipeline/setSequestDefaults");
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
        PipelineService.PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
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
        PipelineService.PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
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
        
        protocol.saveDefinition(uriRoot);

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
        PipelineService.PipeRoot pr = service.findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
        if (uriData == null)
            HttpView.throwNotFound();

        File[] mzXMLFiles = new File(uriData).listFiles(MS2PipelineManager.getAnalyzeFilter());
        for (File mzXMLFile : mzXMLFiles)
        {
            ExperimentRun run = ExperimentService.get().getCreatingRun(mzXMLFile, c);
            if (run != null)
            {
                ExperimentService.get().deleteExperimentRun(run.getRowId(), c);
            }
            File annotationFile = MS2PipelineManager.findAnnotationFile(mzXMLFile);
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

        PipelineService.PipeRoot pr = PipelineService.get().findPipelineRoot(c);
        if (pr == null || !URIUtil.exists(pr.getUri()))
            HttpView.throwNotFound();

        URI uriRoot = pr.getUri();
        URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
        if (uriData == null)
            HttpView.throwNotFound();

        ExperimentService.get().ensureDefaultMaterialSource();
        MaterialSource activeMaterialSource = ExperimentService.get().ensureActiveMaterialSource(getContainer());

        ViewBackgroundInfo info =
                PipelineService.get().getJobBackgroundInfo(getViewBackgroundInfo(), new File(uriData));
        Map<File, FileStatus> mzXmlFileStatus =
            MS2PipelineManager.getAnalysisFileStatus(uriData, null, info.getContainer(), form.getSearchEngine ());
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
            MaterialSource[] materialSources;
            if (activeMaterialSource.getLSID().equals(ExperimentService.get().getDefaultMaterialSourceLsid()))
            {
                materialSources = new MaterialSource[] { activeMaterialSource };
            }
            else
            {
                materialSources = new MaterialSource[] { activeMaterialSource, ExperimentService.get().ensureDefaultMaterialSource() };
            }

            Map<Integer, Material[]> materialSourceMaterials = new HashMap<Integer, Material[]>();
            for (MaterialSource source : materialSources)
            {
                Material[] materials = ExperimentService.get().getMaterialsForMaterialSource(source.getMaterialLSIDPrefix(), source.getContainer());
                materialSourceMaterials.put(source.getRowId(), materials);
            }
            drv.setMaterialSources(materialSources);
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
        PipelineService.PipeRoot pr = service.findPipelineRoot(getContainer());
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
                String baseName = MS2PipelineManager.getBaseName(mzXMLFile);

                // quick hack to get the search to go forward.
                if ("fractions".equals(form.getProtocolSharing()))
                    baseName = MS2PipelineManager._allFractionsMzXmlFileBase;

                File fileInstance = MS2PipelineManager.getAnnotationFile(dirData, baseName);
                protocol.saveInstance(uriRoot, fileInstance, runInfo);

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
        private String searchEngine = "X!Tandem";
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
            PipelineService.PipeRoot pRoot = PipelineService.get().findPipelineRoot(getContainer());
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


    /**
     * This action is not available yet and is still in progress!
     */
    @Jpf.Action
    protected Forward showMS2Run(RunForm form) throws Exception
    {
        //TODO: Check for error conditions
        int runId = Integer.parseInt(form.getRun());
        MS2Run run = MS2Manager.getRun(runId);

        if (run == null)
            throw new NotFoundException();

        requiresPermission(ACL.PERM_READ, run.getContainer());

        if (null == run.getExperimentRunLSID())
        {
            requiresPermission(ACL.PERM_UPDATE, run.getContainer());
            GroovyView gv = new GroovyView("/Experiment/AnnotateRun.gm");
            gv.addObject("run", run);
            MaterialSource[] sources = ExperimentService.get().getMaterialSources();
            if (sources.length == 0)
            {
                ExperimentService.get().insertMaterialSource(getUser(), getContainer(), "Default");
                sources = ExperimentService.get().getMaterialSources();
            }

            gv.addObject("materialSources", sources);
            //TODO: Get containers right
            Protocol[] samplePreps = ExperimentService.get().getProtocolsByType("SamplePreparation", null);
            gv.addObject("samplePreps", samplePreps);
            Protocol[] protocols = ExperimentService.get().getProtocolsByType("ExperimentRun", null);
            gv.addObject("protocols", protocols);
            return renderInTemplate(gv, getContainer(), "Annotate MS2 Run");
        }
        else
        {
            HttpView.throwRedirect(getViewURLHelper().relativeUrl("showRunGraph.view", "rowId=" + runId));
        }

        return null;
    }

    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showMS2Run.do", name = "validate"))
    protected Forward generateRun(GenerateRunForm form) throws Exception
    {
        int run = form.getRun();

        MS2Run msrun = MS2Manager.getRun(run);
        requiresAdmin(msrun.getContainer());

        MaterialSource source = ExperimentService.get().getMaterialSource(form.getMaterialSource());
        Material mat = new Material();
        //getResponse().setContentType("text/xml;charset=UTF-8");
        GroovyView gv = new GroovyView("/org/labkey/ms2/pipeline/ExperimentTemplate.gm");
        Lsid experimentLSID = getDefaultExperimentLSID(getContainer());
        gv.addObject("experimentLSID", experimentLSID.toString());
        mat.setLSID(source.getMaterialLSIDPrefix() + form.getSampleId());
        mat.setName(form.getSampleName());

        Protocol prepProtocol = null;
        String prepId = form.getSamplePrep();
        if ("_new".equals(prepId))
        {
            Protocol protocol = new Protocol();
            //Create a unique LSID for this new protocol. In this case, based on the runId
            Lsid prepLSID = new Lsid("Protocol", "SamplePreparation", String.valueOf(run));
            protocol.setLSID(prepLSID.toString());
            protocol.setName(form.getPrepName());
            protocol.setProtocolDescription(form.getPrepDescription());
            prepProtocol = protocol;
        }
        else if (!"_none".equals(prepId))
        {
            //TODO: Error out if null
            prepProtocol = ExperimentService.get().getProtocol(Integer.parseInt(prepId));
        }
        MS2Fraction[] fractions = MS2Manager.getFractions(run);
        gv.addObject("sample", mat);
        gv.addObject("prepProtocol", prepProtocol);
        gv.addObject("description", form.getDescription());
        gv.addObject("run", msrun);
        gv.addObject("fractions", fractions);
        gv.addObject("lsidAuthority", AppProps.getInstance().getDefaultLsidAuthority());
        File tempFile = File.createTempFile("xar", ".xml");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        PrintWriter writer = new PrintWriter(fos);
        getView().include(gv, writer);
        writer.close();

        PipelineService service = PipelineService.get();
        ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), tempFile);

        ExperimentPipelineJob job = new ExperimentPipelineJob(info, tempFile, form.getDescription(), false);
        PipelineService.get().queueJob(job);

        //BUGBUG: This should not happen unless upload succeeds!!
        String runLsid = "urn:lsid:" + AppProps.getInstance().getDefaultLsidAuthority() + ":ExperimentRun.MS2:" + run;
        MS2Manager.updateMS2Application(run, runLsid);

        HttpView.throwRedirect(getViewURLHelper().relativeUrl("begin.view", "", "Project"));

        return null;
    }

    public static class GenerateRunForm extends ViewForm
    {
        private int run;
        private String sampleId = "Unspecified";
        private String sampleName = "Unnamed Sample";
        private String description = "MS2 Run";
        private Integer materialSource;
        private String samplePrep;
        private String prepName;
        private String prepDescription;

        public void setRun(int run)
        {
            this.run = run;
        }

        public int getRun()
        {
            return run;
        }

        public void setSampleId(String sampleLSID)
        {
            this.sampleId = sampleLSID;
        }

        @Jpf.ValidatableProperty(displayName = "Sample Id", validateRequired = @Jpf.ValidateRequired())
        public String getSampleId()
        {
            return sampleId;
        }

        public void setSampleName(String sampleName)
        {
            this.sampleName = sampleName;
        }

        public String getSampleName()
        {
            return sampleName;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public String getDescription()
        {
            return description;
        }

        public void setMaterialSource(Integer materialSource)
        {
            this.materialSource = materialSource;
        }

        @Jpf.ValidatableProperty(displayName = "Material Source", validateRequired = @Jpf.ValidateRequired())
        public Integer getMaterialSource()
        {
            return materialSource;
        }

        public void setSamplePrep(String samplePrep)
        {
            this.samplePrep = samplePrep;
        }

        public String getSamplePrep()
        {
            return samplePrep;
        }

        public void setPrepName(String prepName)
        {
            this.prepName = prepName;
        }

        public String getPrepName()
        {
            return prepName;
        }

        public void setPrepDescription(String prepDescription)
        {
            this.prepDescription = prepDescription;
        }

        public String getPrepDescription()
        {
            return prepDescription;
        }
    }

    protected static Lsid getDefaultExperimentLSID(Container c)
    {
        return new Lsid("Experiment", "Folder-" + String.valueOf(c.getRowId()), DEFAULT_EXPERIMENT_OBJECTID);
    }

}
