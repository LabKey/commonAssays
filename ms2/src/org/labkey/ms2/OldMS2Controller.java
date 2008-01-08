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

package org.labkey.ms2;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.labkey.api.data.*;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.*;
import org.labkey.api.security.ACL;
import org.labkey.api.util.*;
import org.labkey.api.view.*;
import org.labkey.api.view.template.DialogTemplate;
import org.labkey.api.view.template.FastTemplate;
import org.labkey.api.view.template.HomeTemplate;
import org.labkey.ms2.pipeline.*;
import org.labkey.ms2.protein.*;
import org.labkey.ms2.protein.tools.PieJChartHelper;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;
import org.labkey.ms2.protocol.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.protocol.MS2SearchPipelineProtocol;
import org.labkey.ms2.protocol.MascotSearchProtocolFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Jpf.Controller(longLived = true)
public class OldMS2Controller extends ViewController
{
    private static Logger _log = Logger.getLogger(OldMS2Controller.class);


    @Jpf.Action
    protected Forward begin()
    {
        return null;        // Placeholder -- we'll never get here, but Beehive compile requires a begin action
    }


    private enum Template
    {
        home
        {
            public HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail)
            {
                return new HomeTemplate(viewContext, view, navTrail);
            }
        },
        fast
        {
            public HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail)
            {
                return new FastTemplate(viewContext, view, navTrail);
            }
        };

        public abstract HttpView createTemplate(ViewContext viewContext, HttpView view, NavTrailConfig navTrail);
    }


    private Forward _renderInTemplate(HttpView view, boolean fastTemplate, String title, String helpTopic, NavTree... navTrailChildren) throws Exception
    {
        return _renderInTemplate(view, fastTemplate ? Template.fast : Template.home, title, helpTopic, false, navTrailChildren);
    }

    private Forward _renderInTemplate(HttpView view, Template templateType, String title, String helpTopic, boolean exploratoryFeatures, NavTree... navTrailChildren) throws Exception
    {
        if (helpTopic == null)
            helpTopic = "ms2";

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext());
        if (title != null)
            trailConfig.setTitle(title);
        trailConfig.setHelpTopic(new HelpTopic(helpTopic, HelpTopic.Area.CPAS));
        trailConfig.setExploratoryFeatures(exploratoryFeatures);
        trailConfig.setExtraChildren(navTrailChildren);

        HttpView template = templateType.createTemplate(getViewContext(), view, trailConfig);

        return includeView(template);
    }

    private boolean isAuthorized(int runId) throws ServletException
    {
        String message = null;
        boolean success = false;

        Container c = getContainer();
        MS2Run run = MS2Manager.getRun(runId);

        if (null == run)
            message = "Run not found";
        else if (run.isDeleted())
            message = "Run has been deleted.";
        else if (run.getStatusId() == MS2Importer.STATUS_RUNNING)
            message = "Run is still loading.  Current status: " + run.getStatus();
        else if (run.getStatusId() == MS2Importer.STATUS_FAILED)
            message = "Run failed loading.  Status: " + run.getStatus();
        else
        {
            String cId = run.getContainer();

            if (null != cId && cId.equals(c.getId()))
                success = true;
            else
            {
                ViewURLHelper url = getViewURLHelper().clone();
                url.setExtraPath(ContainerManager.getForId(run.getContainer()).getPath());
                HttpView.throwRedirect(url);
            }
        }

        if (null != message)
        {
            HttpView.throwNotFound(message);
        }

        return success;
    }


    private Forward _renderErrors(List<String> messages) throws Exception
    {
        StringBuilder sb = new StringBuilder("<table class=\"DataRegion\">");

        for (String message : messages)
        {
            sb.append("<tr><td>");
            sb.append(message);
            sb.append("</td></tr>");
        }
        sb.append("</table>");
        HtmlView view = new HtmlView(sb.toString());
        return _renderInTemplate(view, false, "Error", null,
                getBaseNavTree(getContainer()));
    }


    private NavTree getBaseNavTree(Container c)
    {
        return new NavTree("MS2 Runs", MS2Controller.getShowListUrl(c));
    }


    private Forward _renderError(String message) throws Exception
    {
        List<String> messages = new ArrayList<String>(1);
        messages.add(message);
        return _renderErrors(messages);
    }


    public static class ImportProteinProphetForm extends ViewForm
    {
        protected String _path;

        public String getPath()
        {
            return _path;
        }

        public void setPath(String path)
        {
            _path = path;
        }
    }


    @Jpf.Action
    protected Forward importProteinProphet(ImportProteinProphetForm form) throws Exception
    {
        Container c = getContainer(ACL.PERM_INSERT);
        PipelineService service = PipelineService.get();

        File f = null;
        String path = form.getPath();
        if (path != null)
        {
            PipeRoot pr = service.findPipelineRoot(c);
            if (pr != null)
            {
                URI uriData = URIUtil.resolve(pr.getUri(c), path);
                if (uriData != null)
                {
                    f = new File(uriData);
                }
            }
        }


        if (null != f && f.exists() && f.isFile())
        {
            ProteinProphetPipelineJob job = new ProteinProphetPipelineJob(getViewBackgroundInfo(), f);
            service.queueJob(job);
        }
        else
        {
            HttpView.throwNotFound("Unable to open the file '" + form.getPath() + "' to load as a ProteinProphet file");
        }

        ViewURLHelper url = new ViewURLHelper("Project", "begin", c.getPath());
        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward applyRunView(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        // Redirect: this lets struts fill in the form and ensures that the JavaScript sees the showRun action
        return new ViewForward(getApplyViewForwardUrl(form, "showRun"));
    }


    @Jpf.Action
    protected Forward discriminateScore(RunForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper url = new ViewURLHelper("MS2-Scoring", "discriminate", getViewURLHelper().getExtraPath());
        url.addParameter("runId", form.getRun());
        return new ViewForward(url);
    }

    private ViewURLHelper getApplyViewForwardUrl(MS2ViewForm form, String action)
    {
        // Add the "view params" (which were posted as a single param) to the URL params.
        ViewURLHelper forwardUrl = cloneViewURLHelper();
        forwardUrl.setRawQuery(forwardUrl.getRawQuery() + (null == form.viewParams ? "" : "&" + form.viewParams));
        return forwardUrl.setAction(action);
    }


    @Jpf.Action
    protected Forward processAnnots(LoadAnnotForm form) throws Exception
    {
        requiresGlobalAdmin();

        if (null != form)
        {
            String fname = form.getFileName();
            String comment = form.getComment();

            DefaultAnnotationLoader loader;

            //TODO: this style of dealing with different file types must be repaired.
            if ("uniprot".equalsIgnoreCase(form.getFileType()))
            {
                loader = new XMLProteinLoader(fname);
            }
            else if ("fasta".equalsIgnoreCase(form.getFileType()))
            {
                FastaDbLoader fdbl = new FastaDbLoader(new File(fname));
                fdbl.setDefaultOrganism(form.getDefaultOrganism());
                fdbl.setOrganismIsToGuessed(form.getShouldGuess() != null);
                loader = fdbl;
            }
            else
            {
                throw new IllegalArgumentException("Unknown annotation file type: " + form.getFileType());
            }

            loader.setComment(comment);

            try
            {
                loader.validate();
            }
            catch (IOException e)
            {
                PageFlowUtil.getActionErrors(getRequest(), true).add("main", new ActionMessage(e.getMessage()));
                return insertAnnots(form);
            }

            loader.parseInBackground();
        }

        return new ViewForward(MS2Controller.getShowProteinAdminUrl());
    }

    @Jpf.Action
    protected Forward insertAnnots(LoadAnnotForm form) throws Exception
    {
        requiresGlobalAdmin();
        HttpView v = new JspView<LoadAnnotForm>("/org/labkey/ms2/insertAnnots.jsp", form);
        return _renderInTemplate(v, true, "Load Protein Annotations", null);
    }


    @Jpf.Action
    protected Forward doOnePeptideChart() throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        HttpServletRequest req = getRequest();
        HttpServletResponse response = getResponse();
        response.setContentType("image/png");
        OutputStream out = response.getOutputStream();

        String helperName = req.getParameter("helpername");
        PieJChartHelper pjch = (PieJChartHelper) Cache.getShared().get(helperName);

        try
        {
            pjch.renderAsPNG(out);
        }
        catch (Exception e)
        {
            _log.error("Chart rendering failed: " + e);
        }
        finally
        {
            Cache.getShared().remove(helperName);
        }

        return null;
    }


    @Jpf.Action
    protected Forward annotThreadControl() throws Exception
    {
        requiresGlobalAdmin();
        ViewContext ctx = getViewContext();
        HttpServletRequest req = ctx.getRequest();
        String commandType = req.getParameter("button");
        int threadId = Integer.parseInt(req.getParameter("id"));

        AnnotationLoader annotLoader = AnnotationUploadManager.getInstance().getActiveLoader(threadId);

        if (annotLoader != null)
        {
            if (commandType.equalsIgnoreCase("kill"))
            {
                annotLoader.requestThreadState(AnnotationLoader.Status.KILLED);
            }

            if (commandType.equalsIgnoreCase("pause"))
            {
                annotLoader.requestThreadState(AnnotationLoader.Status.PAUSED);
            }

            if (commandType.equalsIgnoreCase("continue"))
            {
                annotLoader.requestThreadState(AnnotationLoader.Status.RUNNING);
            }
        }

        if (commandType.equalsIgnoreCase("recover"))
        {
            String fnameToRecover = Table.executeSingleton(ProteinManager.getSchema(), "SELECT FileName FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" + threadId, null, String.class);
            String ftypeToRecover = Table.executeSingleton(ProteinManager.getSchema(), "SELECT FileType FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" + threadId, null, String.class);
            //TODO:  Major kludge.  This will have to be done correctly.  Possibly with generics, which is what they're for
            if (ftypeToRecover.equalsIgnoreCase("uniprot"))
            {
                XMLProteinLoader xpl = new XMLProteinLoader(fnameToRecover);
                xpl.parseInBackground(threadId);
                Thread.sleep(2001);
            }
            if (ftypeToRecover.equalsIgnoreCase("fasta"))
            {
                FastaDbLoader fdbl = new FastaDbLoader(new File(fnameToRecover));
                fdbl.parseInBackground(threadId);
                Thread.sleep(2002);
            }
        }

        return new ViewForward(MS2Controller.getShowProteinAdminUrl());
    }


    @Jpf.Action
    protected Forward reloadSPOM() throws Exception
    {
        requiresGlobalAdmin();

        ProteinDictionaryHelpers.loadProtSprotOrgMap();

        return new ViewForward(MS2Controller.getShowProteinAdminUrl());
    }


    @Jpf.Action
    protected Forward deleteAnnotInsertEntries() throws Exception
    {
        requiresGlobalAdmin();

        String[] deleteAIEs = getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
        String idList = StringUtils.join(deleteAIEs, ',');
        Table.execute(ProteinManager.getSchema(), "DELETE FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId in (" + idList + ")", null);

        return new ViewForward(MS2Controller.getShowProteinAdminUrl());
    }


    @Jpf.Action
    protected Forward showAnnotInsertDetails() throws Exception
    {
        Container c = ContainerManager.getForPath("home");
        if (!c.hasPermission(getUser(), ACL.PERM_READ))
            HttpView.throwUnauthorized();
        ViewURLHelper urlhelp = cloneViewURLHelper();
        String insertIdStr = urlhelp.getParameter("insertId");
        int insertId = Integer.parseInt(insertIdStr);
        AnnotationInsertion[] insertions = Table.executeQuery(ProteinManager.getSchema(), "SELECT * FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE insertId=?", new Object[] { insertId }, AnnotationInsertion.class);
        if (insertions.length == 0)
        {
            return HttpView.throwNotFound();
        }
        assert insertions.length == 1;
        AnnotationInsertion insertion = insertions[0];
        JspView view = new JspView<AnnotationInsertion>("/org/labkey/ms2/annotLoadDetails.jsp", insertion);
        return _renderInTemplate(view, true, insertion.getFiletype() + " Annotation Insertion Details: " + insertion.getFilename(), null);
    }


    public static class AddRunForm extends ViewForm
    {
        private String fileName;
        private String protocol;
        private String dataDir;
        private String description;
        private String error;
        private boolean auto;
        private boolean experiment;

        public String getFileName()
        {
            return fileName;
        }

        public void setFileName(String fileName)
        {
            this.fileName = fileName;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public boolean isAuto()
        {
            return auto;
        }

        public void setAuto(boolean auto)
        {
            this.auto = auto;
        }

        public String getError()
        {
            return error;
        }

        public void setError(String error)
        {
            this.error = error;
        }

        public String getProtocol()
        {
            return protocol;
        }

        public void setProtocol(String protocol)
        {
            this.protocol = protocol;
        }

        public boolean isExperiment()
        {
            return experiment;
        }

        public void setExperiment(boolean experiment)
        {
            this.experiment = experiment;
        }

        public String getDataDir()
        {
            return dataDir;
        }

        public void setDataDir(String dataDir)
        {
            this.dataDir = dataDir;
        }
    }


    @Jpf.Action
    protected Forward addRun(AddRunForm form) throws Exception
    {
        Container c = null;
        ViewURLHelper url;
        File f = null;

        try
        {
            c = getContainer();
        }
        catch (ServletException e)
        {
            // null container handled below
        }

        if ("Show Runs".equals(getViewURLHelper().getParameter("list")))
        {
            if (c == null)
                return HttpView.throwNotFound();

            return new ViewForward(MS2Controller.getShowListUrl(c));
        }

        if (null != form.getFileName())
        {
            f = new File(form.getFileName());

            if (!f.exists())
                NetworkDrive.ensureDrive(f.getPath());
        }

        if (null != f && f.exists())
        {
            if (!form.isAuto())
            {
                PipelineService service = PipelineService.get();
                ViewBackgroundInfo info = getViewBackgroundInfo();

                // TODO: Clean this up.
                boolean mascotFile = MascotSearchProtocolFactory.get().equals(AbstractMS2SearchProtocolFactory.fromFile(f));
                int run;
                if (mascotFile)
                {
                    run = MS2Manager.addMascotRunToQueue(info,
                            f, form.getDescription(), false).getRunId();
                }
                else
                {
                    run = MS2Manager.addRunToQueue(info,
                            f, form.getDescription(), false).getRunId();
                }

                if (run == -1)
                    return HttpView.throwNotFound();

                url = MS2Controller.getShowListUrl(c);
                url.addParameter(MS2Manager.getDataRegionNameExperimentRuns() + ".Run~eq", Integer.toString(run));
            }
            else if (!AppProps.getInstance().hasPipelineCluster())
            {
                url = new ViewURLHelper("MS2", "addFileRunStatus", "");
                url.addParameter("error", "Automated upload disabled.");
            }
            else
            {
                if (!form.isExperiment())
                {
                    int run = MS2Manager.addRunToQueue(getViewBackgroundInfo(),
                            f, form.getDescription(), true).getRunId();
                    if (run == -1)
                        HttpView.throwNotFound();

                    url = new ViewURLHelper("MS2", "addFileRunStatus", "");
                    url.addParameter("run", Integer.toString(run));
                }
                else
                {
                    // Make sure container exists.
                    c = ContainerManager.ensureContainer(getViewURLHelper().getExtraPath());
                    if (null == c)
                        return HttpView.throwNotFound();

                    PipelineService service = PipelineService.get();
                    PipeRoot pr = service.findPipelineRoot(c);
                    if (pr == null)
                        return HttpView.throwUnauthorized();

                    String protocolName = form.getProtocol();
                    File dirData = new File(form.getDataDir());
                    if (!NetworkDrive.exists(dirData))
                        return HttpView.throwNotFound();

                    AbstractMS2SearchProtocolFactory protocolFactory = AbstractMS2SearchProtocolFactory.fromFile(f);

                    File dirSeqRoot = new File(MS2PipelineManager.getSequenceDatabaseRoot(pr.getContainer()));
                    File dirAnalysis = protocolFactory.getAnalysisDir(dirData, protocolName);
                    File fileParameters = protocolFactory.getParametersFile(dirData, protocolName);
                    String baseName = FileUtil.getBaseName(f, 2);

                    File[] filesMzXML;
                    if (!"all".equals(baseName))
                    {
                        filesMzXML = new File[] { MS2PipelineManager.getMzXMLFile(dirData, baseName) };
                    }
                    else
                    {
                        // Anything that is running or complete.
                        Map<File, FileStatus> mzXMLFileStatus = MS2PipelineManager.getAnalysisFileStatus(dirData, dirAnalysis, getContainer());
                        List<File> fileList = new ArrayList<File>();
                        for (File fileMzXML : mzXMLFileStatus.keySet())
                        {
                            FileStatus status = mzXMLFileStatus.get(fileMzXML);
                            if (status.equals(FileStatus.COMPLETE) || status.equals(FileStatus.RUNNING))
                                fileList.add(fileMzXML);
                        }
                        filesMzXML = fileList.toArray(new File[fileList.size()]);

                        if (filesMzXML.length == 0)
                            return HttpView.throwNotFound();
                    }

                    MS2SearchPipelineProtocol protocol = protocolFactory.loadInstance(fileParameters);

                    PipelineJob job = protocol.createPipelineJob(getViewBackgroundInfo(),
                            dirSeqRoot,
                            filesMzXML,
                            fileParameters,
                            true);

                    PipelineService.get().queueJob(job);

                    url = new ViewURLHelper("MS2", "addFileRunStatus", "");
                    url.addParameter("path", job.getLogFile().getAbsolutePath());
                }
            }
        }
        else
        {
            url = new ViewURLHelper("MS2", "addFileRunStatus", "");
        }

        return new ViewForward(url);
    }


    public static class RunStatus
    {
        int statusId;
        String status;
        String description;
        boolean deleted;

        public int getStatusId()
        {
            return statusId;
        }

        public void setStatusId(int statusId)
        {
            this.statusId = statusId;
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public boolean getDeleted()
        {
            return deleted;
        }

        public void setDeleted(boolean deleted)
        {
            this.deleted = deleted;
        }
    }

    @Jpf.Action
    protected Forward addFileRunStatus() throws Exception
    {
        String status = null;
        HttpServletResponse response = getResponse();
        response.setContentType("text/plain");

        String path = getViewURLHelper().getParameter("path");
        if (path != null)
        {
            path = PipelineStatusManager.getStatusFilePath(path);
            PipelineStatusFile sf = PipelineStatusManager.getStatusFile(path);
            if (sf == null)
                status = "ERROR->path=" + path + ",message=Job not found in database";
/*            else if (run.getDeleted())
                status = "ERROR->run=" + runId + ",message=Run deleted"; */
            else
            {
                String[] parts = (sf.getInfo() == null ?
                        new String[0] : sf.getInfo().split(","));
                StringBuffer sb = new StringBuffer(sf.getStatus());
                sb.append("->path=").append(sf.getFilePath());
                for (String part : parts)
                {
                    if (part.startsWith("path="))
                        continue;
                    sb.append(",").append(part);
                }

                status = sb.toString();
            }
        }
        else if (getViewURLHelper().getParameter("error") != null)
        {
            status = "ERROR->message=" + getViewURLHelper().getParameter("error");            
        }
        else
        {
            // Old MS2-only code.  Still supports Comet searches.
            int runId = 0;
            String runParam = getViewURLHelper().getParameter("run");
            if (runParam != null)
            {
                try
                {
                    runId = Integer.parseInt(runParam);
                }
                catch (NumberFormatException e)
                {
                    _log.error(e);
                }
            }

            if (runId > 0)
            {
                TableInfo info = MS2Manager.getTableInfoRuns();
                RunStatus run = Table.selectObject(info, runId, RunStatus.class);
                if (run == null)
                    status = "ERROR->run=" + runId + ",message=Run not found in database";
                else if (run.getDeleted())
                    status = "ERROR->run=" + runId + ",message=Run deleted";
                else if (run.getStatusId() == 1)
                    status = "SUCCESS->run=" + runId;
                else if (run.getStatusId() == 2)
                    status = "FAILED->run=" + runId;
                else if (run.getStatusId() == 0)
                {
                    status = "LOADING->run=" + runId + ",status=" + run.getStatus()
                            + ",description=" + run.getDescription();
                }
            }
        }

        if (status == null)
        {
            response.setStatus(400);    // Bad request.
            status = "ERROR->File not found";
        }

        response.getWriter().println(status);

        return null;
    }


    @Jpf.Action
    protected Forward deleteRuns() throws URISyntaxException, ServletException, IOException
    {
        requiresPermission(ACL.PERM_DELETE);

        Container c = getContainer();
        ViewURLHelper currentUrl = cloneViewURLHelper();

        String[] deleteRuns = currentUrl.getParameters(DataRegion.SELECT_CHECKBOX_NAME);

        if (null != deleteRuns)
        {
            MS2Manager.markAsDeleted(deleteRuns, c, getUser());
        }

        currentUrl.setAction("showList");
        currentUrl.deleteParameter("x");
        currentUrl.deleteParameter("y");
        currentUrl.deleteParameter(DataRegion.SELECT_CHECKBOX_NAME);

        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward selectMoveLocation() throws URISyntaxException, ServletException
    {
        requiresPermission(ACL.PERM_DELETE);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        String[] moveRuns = getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);

        currentUrl.setAction("pickMoveLocation");
        currentUrl.deleteParameters();
        if ("true".equals(getRequest().getParameter("ExperimentRunIds")))
        {
            currentUrl.addParameter("ExperimentRunIds", "true");
        }
        currentUrl.addParameter("moveRuns", StringUtils.join(moveRuns, ','));
        return new ViewForward(currentUrl);
    }


    @Jpf.Action
    protected Forward pickMoveLocation() throws Exception
    {
        requiresPermission(ACL.PERM_DELETE);

        ViewURLHelper currentUrl = cloneViewURLHelper();
        currentUrl.setAction("moveRuns");
        final Container originalContainer = getContainer();
        ContainerTree ct = new ContainerTree("/", getUser(), ACL.PERM_INSERT, currentUrl)
        {
            protected void renderCellContents(StringBuilder html, Container c, ViewURLHelper url)
            {
                boolean hasRoot = false;
                try
                {
                    hasRoot = PipelineService.get().findPipelineRoot(c) != null;
                }
                catch (SQLException e)
                {
                    _log.error("Unable to determine pipeline root", e);
                }

                if (hasRoot && !c.equals(originalContainer))
                {
                    super.renderCellContents(html, c, url);
                }
                else
                {
                    html.append(PageFlowUtil.filter(c.getName()));
                }
            }
        };

        StringBuilder html = new StringBuilder("<table class=\"dataRegion\"><tr><td>Please select the destination folder. Folders that are not configured with a pipeline root are not valid destinations. They are shown in the list, but are not linked.</td></tr><tr><td>&nbsp;</td></tr>");
        ct.render(html);
        html.append("</table>");

        return _renderInTemplate(new HtmlView(html.toString()), false, "Move Runs", "ms2RunsList",
                getBaseNavTree(getContainer()));
    }


    @Jpf.Action
    protected Forward applyExportRunsView(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        // Forward without redirect: this lets Spring fill in the form and preserves the post data
//        return new ViewForward(getApplyViewForwardUrl(form, "exportRuns"), false);
        ViewServlet.forwardActionURL(getRequest(), getResponse(), getApplyViewForwardUrl(form, "exportRuns"));

        return null;
    }


    @Jpf.Action
    protected Forward editElutionGraph(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());
        Quantitation quant = peptide.getQuantitation();

        EditElutionGraphContext ctx = new EditElutionGraphContext(quant.getLightElutionProfile(peptide.getCharge()), quant.getHeavyElutionProfile(peptide.getCharge()), quant, getViewURLHelper(), peptide);
        JspView v = new JspView<EditElutionGraphContext>("/org/labkey/ms2/editElution.jsp", ctx);
        includeView(v);
        return null;
    }

    @Jpf.Action
    protected Forward saveElutionProfile(ElutionProfileForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());
        if (peptide == null)
        {
            throw new NotFoundException();
        }
        Quantitation quant = peptide.getQuantitation();
        if (quant == null)
        {
            throw new NotFoundException();
        }

        boolean validRanges = quant.resetRanges(form.getLightFirstScan(), form.getLightLastScan(), form.getHeavyFirstScan(), form.getHeavyLastScan(), peptide.getCharge());
        Table.update(getUser(), MS2Manager.getTableInfoQuantitation(), quant, quant.getPeptideId(), null);

        ViewURLHelper url = getViewURLHelper().clone();
        url.setAction("showPeptide.view");
        if (!validRanges)
        {
            url.addParameter("elutionProfileError", "Invalid elution profile range");
        }

        return new ViewForward(url);
    }

    protected Forward showElutionGraph(DetailsForm form, boolean showLight, boolean showHeavy) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

        HttpServletResponse response = getResponse();
        if (null != peptide)
        {
            Quantitation quantitation = peptide.getQuantitation();
            response.setDateHeader("Expires", 0);
            response.setContentType("image/png");

            File f = quantitation.findScanFile();
            if (f != null)
            {
                ElutionGraph g = new ElutionGraph();
                int charge = form.getQuantitationCharge() == Integer.MIN_VALUE ? peptide.getCharge() : form.getQuantitationCharge();
                if (charge < 1 || charge > Quantitation.MAX_CHARGE)
                {
                    return renderErrorImage("Invalid charge state: " + charge, response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
                }
                if (showLight)
                {
                    g.addInfo(quantitation.getLightElutionProfile(charge), quantitation.getLightFirstScan(), quantitation.getLightLastScan(), quantitation.getMinDisplayScan(), quantitation.getMaxDisplayScan(), Color.RED);
                }
                if (showHeavy)
                {
                    g.addInfo(quantitation.getHeavyElutionProfile(charge), quantitation.getHeavyFirstScan(), quantitation.getHeavyLastScan(), quantitation.getMinDisplayScan(), quantitation.getMaxDisplayScan(), Color.BLUE);
                }
                if (quantitation.isNoScansFound())
                {
                    return renderErrorImage("No relevant MS1 scans found in spectra file", response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
                }
                else
                {
                    g.render(response.getOutputStream());
                    return null;
                }
            }
            else
            {
                return renderErrorImage("Could not open spectra file to get MS1 scans", response, ElutionGraph.WIDTH, ElutionGraph.HEIGHT);
            }
        }
        else
        {
            return HttpView.throwNotFound("Could not find peptide with id " + form.getPeptideIdLong());
        }
    }

    private Forward renderErrorImage(String errorMessage, HttpServletResponse response, int width, int height)
            throws IOException
    {
        Graph g = new Graph(new float[0], new float[0], width, height)
        {
            protected void initializeDataPoints(Graphics2D g) {}
            protected void renderDataPoint(Graphics2D g, double x, double y) {}
        };
        g.setNoDataErrorMessage(errorMessage);
        g.render(response.getOutputStream());
        return null;
    }

    @Jpf.Action
    protected Forward showLightElutionGraph(DetailsForm form) throws Exception
    {
        return showElutionGraph(form, true, false);
    }

    @Jpf.Action
    protected Forward showHeavyElutionGraph(DetailsForm form) throws Exception
    {
        return showElutionGraph(form, false, true);
    }

    @Jpf.Action
    protected Forward showCombinedElutionGraph(DetailsForm form) throws Exception
    {
        return showElutionGraph(form, true, true);
    }

    @Jpf.Action
    protected Forward updateShowPeptide() throws Exception
    {
        ViewContext ctx = getViewContext();

        ViewURLHelper fwdUrl = cloneViewURLHelper().setAction("showPeptide");
        String queryString = (String)ctx.get("queryString");
        fwdUrl.setRawQuery(queryString);

        String xStart = (String)ctx.get("xStart");
        String xEnd = (String)ctx.get("xEnd");

        if ("".equals(xStart))
            fwdUrl.deleteParameter("xStart");
        else
            fwdUrl.replaceParameter("xStart", xStart);

        if ("".equals(xEnd))
            fwdUrl.deleteParameter("xEnd");
        else
            fwdUrl.replaceParameter("xEnd", xEnd);

        return new ViewForward(fwdUrl);
    }


    @Jpf.Action
    protected Forward showParamsFile(DetailsForm form) throws IOException, ServletException
    {
        requiresPermission(ACL.PERM_READ);

        MS2Run run = MS2Manager.getRun(form.run);

        try
        {
            // TODO: Ensure drive?
            PageFlowUtil.streamFile(getResponse(), run.getPath() + "/" + run.getParamsFileName(), false);
        }
        catch (Exception e)
        {
            getResponse().getWriter().print("Error retrieving file: " + e.getMessage());
        }

        return null;
    }


    @Jpf.Action
    protected Forward showGZFile(DetailsForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        if (!isAuthorized(form.run))
            return null;

        MS2Peptide peptide = MS2Manager.getPeptide(form.getPeptideIdLong());

        if (null == peptide)
        {
            // This should only happen if an old, cached link is being used... a saved favorite or google bot with fraction=x&scan=y&charge=z instead of peptideId
            // Log an error just to make sure.
            _log.error("Couldn't find peptide " + form.getPeptideIdLong() + ". " + getViewURLHelper().toString());
            return _renderError("Peptide not found");
        }

        getResponse().setContentType("text/plain");
        PrintWriter out = getResponse().getWriter();

        MS2GZFileRenderer renderer = new MS2GZFileRenderer(peptide, form.getExtension());

        if (!renderer.render(out))
        {
            MS2GZFileRenderer.renderFileHeader(out, MS2GZFileRenderer.getFileNameInGZFile(MS2Manager.getFraction(peptide.getFraction()), peptide.getScan(), peptide.getCharge(), form.extension));
            out.println(renderer.getLastErrorMessage());
        }

        return null;
    }


    @Jpf.Action
    protected Forward addExtraFilter() throws URISyntaxException, ServletException
    {
        requiresPermission(ACL.PERM_READ);

        ViewContext ctx = getViewContext();
        ViewURLHelper url = cloneViewURLHelper();
        url.setAction("showRun.view");

        MS2Run run = MS2Manager.getRun(getRequest().getParameter("run"));
        String paramName = run.getChargeFilterParamName();

        // Stick posted values onto showRun URL and forward.  URL shouldn't have any rawScores or tryptic (they are
        // deleted from the button URL and get posted instead).  Don't bother adding "0" since it's the default.

        // Verify that charge filter scroes are valid floats and, if so, add as URL params
        parseChargeScore(ctx, url, "1", paramName);
        parseChargeScore(ctx, url, "2", paramName);
        parseChargeScore(ctx, url, "3", paramName);

        String tryptic = (String) ctx.get("tryptic");

        if (!"0".equals(tryptic))
            url.addParameter("tryptic", tryptic);

        if (getRequest().getParameter("grouping") != null)
        {
            url.addParameter("grouping", getRequest().getParameter("grouping"));
        }

        if (getRequest().getParameter("expanded") != null)
        {
            url.addParameter("expanded", "1");
        }

        return new ViewForward(url);
    }


    // Parse parameter to float, returning 0 for any parsing exceptions
    private void parseChargeScore(ViewContext ctx, ViewURLHelper url, String digit, String paramName)
    {
        float value = 0;
        String score = (String)ctx.get("charge" + digit);

        try
        {
            if (score != null)
            {
                value = Float.parseFloat(score);
            }
        }
        catch(NumberFormatException e)
        {
            // Can't parse... just use default
        }

        if (0.0 != value)
            url.addParameter(paramName + digit, Formats.chargeFilter.format(value));
    }


    @Jpf.Action
    protected Forward applyCompareView(MS2ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        ViewURLHelper forwardUrl = getApplyViewForwardUrl(form, "showCompare");

        forwardUrl.deleteParameter("submit.x");
        forwardUrl.deleteParameter("submit.y");
        forwardUrl.deleteParameter("viewParams");

        // Forward with redirect: this lets struts fill in the form and ensures that the JavaScript sees the showCompare action
        return new ViewForward(forwardUrl);
    }


    @Jpf.Action
    protected Forward testMascot(TestMascotForm form) throws Exception
    {
        requiresGlobalAdmin();

        String originalMascotServer = form.getMascotServer();
        MascotClientImpl mascotClient = new MascotClientImpl(form.getMascotServer(), null,
            form.getUserAccount(), form.getPassword());
        mascotClient.setProxyURL(form.getHTTPProxyServer());
        mascotClient.findWorkableSettings(true);
        form.setStatus(mascotClient.getErrorCode());

        String message;
        if (0 == mascotClient.getErrorCode())
        {
            message = "Test passed.";
            form.setParameters(mascotClient.getParameters());
        }
        else
        {
            message = "Test failed.";
            message = message + "<br>" + mascotClient.getErrorString();
        }

        form.setMessage(message);
        form.setMascotServer(originalMascotServer);

        HttpView view = new GroovyView("/org/labkey/core/admin/testMascot.gm");
        form.setPassword(("".equals(form.getPassword())) ? "" : "***");  // do not show password in clear
        view.addObject("form", form);
        return includeView(new DialogTemplate(view));
    }

    @Jpf.Action
    protected Forward showMascotTest(TestMascotForm form) throws Exception
    {
        return testMascot (form);
    }

    @Jpf.Action
    protected Forward showUpgradeMascotTest(TestMascotForm form) throws Exception
    {
        return testMascot (form);
    }

    public static class TestMascotForm extends FormData
    {
        private String mascotserver;
        private String useraccount;
        private String password;
        private String httpproxyserver;
        private int status;
        private String parameters;
        private String message;

        private String trimSafe(String s)
        {
            return (s == null ? "" : s.trim());
        }

        public void reset(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
        {
            setMascotServer(trimSafe(httpServletRequest.getParameter("mascotServer")));
            setUserAccount(trimSafe(httpServletRequest.getParameter("mascotUserAccount")));
            setPassword(trimSafe(httpServletRequest.getParameter("mascotUserPassword")));
            setHTTPProxyServer(trimSafe(httpServletRequest.getParameter("mascotHTTPProxy")));
            super.reset(actionMapping, httpServletRequest);
        }

        public String getUserAccount()
        {
            return (null == useraccount ? "" : useraccount);
        }

        public void setUserAccount(String useraccount)
        {
            this.useraccount = useraccount;
        }

        public String getPassword()
        {
            return (null == password ? "" : password);
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String getMascotServer()
        {
            return (null == mascotserver ? "" : mascotserver);
        }

        public void setMascotServer(String mascotserver)
        {
            this.mascotserver = mascotserver;
        }

        public String getHTTPProxyServer()
        {
            return (null == httpproxyserver ? "" : httpproxyserver);
        }

        public void setHTTPProxyServer(String httpproxyserver)
        {
            this.httpproxyserver = httpproxyserver;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public int getStatus()
        {
            return status;
        }

        public void setStatus(int status)
        {
            this.status = status;
        }

        public String getParameters()
        {
            return (null == parameters ? "" : parameters);
        }

        public void setParameters(String parameters)
        {
            this.parameters = parameters;
        }
    }

    @Jpf.Action
    protected Forward testSequest(TestSequestForm form) throws Exception
    {
        requiresGlobalAdmin();

        String originalSequestServer = form.getSequestServer();
        SequestClientImpl sequestClient = new SequestClientImpl(form.getSequestServer());
        String html = sequestClient.testConnectivity();
        if (sequestClient.getErrorCode() == 0)
                html = sequestClient.getEnvironmentConf();

        String message;
        if(sequestClient.getErrorCode() != 0)
        {
            form.setStatus(sequestClient.getErrorCode());
            message = "Test failed.";
            message = message + "<br>" + html;
        }
        else
        {
            message = "Connection test passed.";
            form.setParameters(html);
        }

        form.setMessage(message);
        form.setSequestServer(originalSequestServer);

        HttpView view = new GroovyView("org/labkey/core/admin/testSequest.gm");
        view.addObject("form", form);
        return includeView(new DialogTemplate(view));
    }

    @Jpf.Action
    protected Forward showSequestTest(TestSequestForm form) throws Exception
    {
        return testSequest(form);
    }

    @Jpf.Action
    protected Forward showUpgradeSequestTest(TestSequestForm form) throws Exception
    {
        return testSequest (form);
    }

    public static class TestSequestForm extends FormData
    {
        private String sequestserver;
        private int status;
        private String parameters;
        private String message;

        public void reset(ActionMapping actionMapping, HttpServletRequest httpServletRequest)
        {
            setSequestServer(httpServletRequest.getParameter("sequestServer").trim());
            super.reset(actionMapping, httpServletRequest);
        }

        public String getSequestServer()
        {
            return (null == sequestserver ? "" : sequestserver);
        }

        public void setSequestServer(String sequestserver)
        {
            this.sequestserver = sequestserver;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public int getStatus()
        {
            return status;
        }

        public void setStatus(int status)
        {
            this.status = status;
        }

        public String getParameters()
        {
            return (null == parameters ? "" : parameters);
        }

        public void setParameters(String parameters)
        {
            this.parameters = parameters;
        }
    }

    public static class LoadAnnotForm extends FormData
    {
        private String fileType;

        public void setFileType(String ft)
        {
            this.fileType = ft;
        }

        public String getFileType()
        {
            return this.fileType;
        }

        private String fileName;

        public void setFileName(String file)
        {
            this.fileName = file;
        }

        public String getFileName()
        {
            return this.fileName;
        }

        private String comment;

        public void setComment(String s)
        {
            this.comment = s;
        }

        public String getComment()
        {
            return this.comment;
        }

        private String defaultOrganism = "Unknown unknown";

        public String getDefaultOrganism()
        {
            return this.defaultOrganism;
        }

        public void setDefaultOrganism(String o)
        {
            this.defaultOrganism = o;
        }

        private String shouldGuess = "1";

        public String getShouldGuess()
        {
            return shouldGuess;
        }

        public void setShouldGuess(String shouldGuess)
        {
            this.shouldGuess = shouldGuess;
        }
    }


    public static class RunForm extends FormData
    {
        enum PARAMS
        {
            run
        }
        
        int run;
        int tryptic;
        boolean expanded;
        boolean exportAsWebPage;
        String grouping;
        String columns;
        String proteinColumns;
        String proteinGroupingId;

        ArrayList<String> errors;

        // Set form default values; will be overwritten by any params included on the url
        public void reset(ActionMapping arg0, HttpServletRequest arg1)
        {
            super.reset(arg0, arg1);
            run = 0;
            expanded = false;
            errors = new ArrayList<String>();
        }

        private int toInt(String s, String field)
        {
            try
            {
                return Integer.parseInt(s);
            }
            catch (NumberFormatException e)
            {
                errors.add("Error: " + s + " is not a valid value for " + field + ".");
                return 0;
            }
        }

        public List<String> getErrors()
        {
            return errors;
        }

        public void setExpanded(boolean expanded)
        {
            this.expanded = expanded;
        }

        public boolean getExpanded()
        {
            return this.expanded;
        }

        public void setRun(String run)
        {
            this.run = toInt(run, "Run");
        }

        public String getRun()
        {
            return String.valueOf(this.run);
        }

        public void setTryptic(String tryptic)
        {
            this.tryptic = toInt(tryptic, "Tryptic");
        }

        public String getTryptic()
        {
            return null;
        }

        public void setGrouping(String grouping)
        {
            this.grouping = grouping;
        }

        public String getGrouping()
        {
            return grouping;
        }

        public void setExportAsWebPage(boolean exportAsWebPage)
        {
            this.exportAsWebPage = exportAsWebPage;
        }

        public boolean isExportAsWebPage()
        {
            return exportAsWebPage;
        }

        public String getColumns()
        {
            return columns;
        }

        public void setColumns(String columns)
        {
            this.columns = columns;
        }

        public String getProteinColumns()
        {
            return proteinColumns;
        }

        public void setProteinColumns(String proteinColumns)
        {
            this.proteinColumns = proteinColumns;
        }

        public String getProteinGroupingId()
        {
            return proteinGroupingId;
        }

        public void setProteinGroupingId(String proteinGroupingId)
        {
            this.proteinGroupingId = proteinGroupingId;
        }
    }

    public static class ElutionProfileForm extends DetailsForm
    {
        private int _lightFirstScan;
        private int _lightLastScan;
        private int _heavyFirstScan;
        private int _heavyLastScan;

        public int getLightFirstScan()
        {
            return _lightFirstScan;
        }

        public void setLightFirstScan(int lightFirstScan)
        {
            _lightFirstScan = lightFirstScan;
        }

        public int getLightLastScan()
        {
            return _lightLastScan;
        }

        public void setLightLastScan(int lightLastScan)
        {
            _lightLastScan = lightLastScan;
        }

        public int getHeavyFirstScan()
        {
            return _heavyFirstScan;
        }

        public void setHeavyFirstScan(int heavyFirstScan)
        {
            _heavyFirstScan = heavyFirstScan;
        }

        public int getHeavyLastScan()
        {
            return _heavyLastScan;
        }

        public void setHeavyLastScan(int heavyLastScan)
        {
            _heavyLastScan = heavyLastScan;
        }
    }

    public static class DetailsForm extends RunForm
    {
        private long peptideId;
        private int rowIndex;
        private int height;
        private int width;
        private double tolerance;
        private double xEnd;
        private double xStart;
        private int seqId;
        private String extension;
        private String protein;
        private int quantitationCharge;
        private int groupNumber;
        private int indistinguishableCollectionId;
        private Integer proteinGroupId;

        public Integer getProteinGroupId()
        {
            return proteinGroupId;
        }

        public void setProteinGroupId(Integer proteinGroupId)
        {
            this.proteinGroupId = proteinGroupId;
        }

        public int getGroupNumber()
        {
            return groupNumber;
        }

        public void setGroupNumber(int groupNumber)
        {
            this.groupNumber = groupNumber;
        }

        public int getIndistinguishableCollectionId()
        {
            return indistinguishableCollectionId;
        }

        public void setIndistinguishableCollectionId(int indistinguishableCollectionId)
        {
            this.indistinguishableCollectionId = indistinguishableCollectionId;
        }

        public void setPeptideId(String peptideId)
        {
            try
            {
                this.peptideId = Long.parseLong(peptideId);
            }
            catch (NumberFormatException e) {}
        }

        public String getPeptideId()
        {
            return Long.toString(peptideId);
        }

        public long getPeptideIdLong()
        {
            return this.peptideId;
        }

        public void setxStart(String xStart)
        {
            try
            {
                this.xStart = Double.parseDouble(xStart);
            }
            catch (NumberFormatException e) {}
        }

        public String getxStart()
        {
            return Double.toString(xStart);
        }

        public double getxStartDouble()
        {
            return this.xStart;
        }

        public String getStringXStart()
        {
            return Double.MIN_VALUE == xStart ? "" : Formats.fv2.format(xStart);
        }

        public void setxEnd(double xEnd)
        {
            this.xEnd = xEnd;
        }

        public double getxEnd()
        {
            return this.xEnd;
        }

        public String getStringXEnd()
        {
            return Double.MAX_VALUE == xEnd ? "" : Formats.fv2.format(xEnd);
        }

        public void setTolerance(double tolerance)
        {
            this.tolerance = tolerance;
        }

        public double getTolerance()
        {
            return this.tolerance;
        }

        public void setWidth(int width)
        {
            this.width = width;
        }

        public int getWidth()
        {
            return this.width;
        }

        public void setHeight(int height)
        {
            this.height = height;
        }

        public int getHeight()
        {
            return this.height;
        }

        // Set form default values for graphs and peptides; these will be overwritten by
        // any params included on the url
        public void reset(ActionMapping arg0, HttpServletRequest arg1)
        {
            super.reset(arg0, arg1);
            rowIndex = -1;
            xStart = Double.MIN_VALUE;
            xEnd = Double.MAX_VALUE;
            tolerance = 1.0;
            height = 400;
            width = 600;
            quantitationCharge = Integer.MIN_VALUE;
        }

        public void setRowIndex(int rowIndex)
        {
            this.rowIndex = rowIndex;
        }

        public int getRowIndex()
        {
            return this.rowIndex;
        }

        public void setSeqId(String seqId)
        {
            try
            {
                this.seqId = Integer.parseInt(seqId);
            }
            catch (NumberFormatException e) {}
        }

        public String getSeqId()
        {
            return Integer.toString(this.seqId);
        }

        public int getSeqIdInt()
        {
            return this.seqId;
        }

        public String getExtension()
        {
            return extension;
        }

        public void setExtension(String extension)
        {
            this.extension = extension;
        }

        public String getProtein()
        {
            return protein;
        }

        public void setProtein(String protein)
        {
            this.protein = protein;
        }

        public int getQuantitationCharge()
        {
            return quantitationCharge;
        }

        public void setQuantitationCharge(int quantitationCharge)
        {
            this.quantitationCharge = quantitationCharge;
        }

    }


    public static class MS2ViewForm extends FormData
    {
        private String viewParams;
        private String returnUrl;
        private String name;
        private int run;
        private boolean shared;

        public void setName(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }

        public void setReturnUrl(String returnUrl)
        {
            this.returnUrl = returnUrl;
        }

        public String getReturnUrl()
        {
            return this.returnUrl;
        }

        public void setViewParams(String viewParams)
        {
            this.viewParams = viewParams;
        }

        public String getViewParams()
        {
            return this.viewParams;
        }

        public void setRun(String run)
        {
            try
            {
                this.run = Integer.parseInt(run);
            }
            catch (NumberFormatException e) {}
        }

        public String getRun()
        {
            return Integer.toString(run);
        }

        public boolean isShared()
        {
            return shared;
        }

        public void setShared(boolean shared)
        {
            this.shared = shared;
        }
    }
}
