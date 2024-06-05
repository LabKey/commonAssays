package org.labkey.protein;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.annotations.Migrate;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.protein.AnnotationInsertion;
import org.labkey.api.protein.DefaultAnnotationLoader;
import org.labkey.api.protein.ProtSprotOrgMap;
import org.labkey.api.protein.ProteinAnnotationPipelineProvider;
import org.labkey.api.protein.ProteinManager;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.XMLProteinLoader;
import org.labkey.api.protein.fasta.FastaDbLoader;
import org.labkey.api.protein.fasta.FastaParsingForm;
import org.labkey.api.protein.fasta.FastaReloaderJob;
import org.labkey.api.protein.go.GoLoader;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.AdminConsoleAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AbstractActionPermissionTest;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.TestContext;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GridView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.TabStripView;
import org.labkey.api.view.VBox;
import org.labkey.api.view.template.PageConfig;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotController extends SpringActionController
{
    private static final SpringActionController.DefaultActionResolver _actionResolver = new SpringActionController.DefaultActionResolver(AnnotController.class);

    public AnnotController()
    {
        setActionResolver(_actionResolver);
    }

    public static void registerAdminConsoleLinks()
    {
        AdminConsole.addLink(AdminConsole.SettingsLinkType.Premium, "protein databases", getShowProteinAdminUrl(), AdminOperationsPermission.class);
    }

    private void addAdminNavTrail(NavTree root, ActionURL adminPageURL, String title, PageConfig page, String helpTopic)
    {
        page.setHelpTopic(null == helpTopic ? "ms2" : helpTopic);
        root.addChild("Admin Console", urlProvider(AdminUrls.class).getAdminConsoleURL());
        root.addChild("Protein Database Admin", adminPageURL);
        root.addChild(title);
    }

    private void addProteinAdminNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        addAdminNavTrail(root, getShowProteinAdminUrl(), title, page, helpTopic);
    }

    public static ActionURL getLoadGoURL()
    {
        return new ActionURL(LoadGoAction.class, ContainerManager.getRoot());
    }

    @RequiresSiteAdmin
    public class LoadGoAction extends FormViewAction<Object>
    {
        private String _message = null;

        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(Object o, boolean reshow, BindException errors)
        {
            return new GoView();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addProteinAdminNavTrail(root, (GoLoader.isGoLoaded().booleanValue() ? "Reload" : "Load") + " GO Annotations", getPageConfig(), "annotations");
        }

        @Override
        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            GoLoader loader;

            if ("1".equals(getViewContext().get("manual")))
            {
                Map<String, MultipartFile> fileMap = getFileMap();
                MultipartFile goFile = fileMap.get("gofile");                       // TODO: Check for NULL and display error
                loader = GoLoader.getStreamLoader(goFile.getInputStream());
            }
            else
            {
                loader = GoLoader.getHttpLoader();
            }

            if (null != loader)
            {
                loader.load();
                Thread.sleep(2000);
            }
            else
            {
                _message = "Can't load GO annotations, a GO annotation load is already in progress.  See below for details.";
            }

            return true;
        }

        @Override
        public ActionURL getSuccessURL(Object o)
        {
            return getGoStatusURL(_message);
        }

        private static class GoView extends TabStripView
        {
            @Override
            public List<NavTree> getTabList()
            {
                return Arrays.asList(new TabInfo("Automatic", "automatic", getLoadGoURL()), new TabInfo("Manual", "manual", getLoadGoURL()));
            }

            @Override
            public HttpView getTabView(String tabId)
            {
                if ("manual".equals(tabId))
                    return new JspView<>("/org/labkey/protein/view/loadGoManual.jsp");
                else
                    return new JspView<>("/org/labkey/protein/view/loadGoAutomatic.jsp");
            }
        }
    }

    private ActionURL getGoStatusURL(String message)
    {
        ActionURL url = new ActionURL(GoStatusAction.class, ContainerManager.getRoot());
        if (null != message)
            url.addParameter("message", message);
        return url;
    }

    @RequiresSiteAdmin
    public class GoStatusAction extends SimpleViewAction<GoForm>
    {
        @Override
        public ModelAndView getView(GoForm form, BindException errors)
        {
            return GoLoader.getCurrentStatus(form.getMessage());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addProteinAdminNavTrail(root, "GO Load Status", getPageConfig(), "annotations");
        }
    }

    public static class GoForm
    {
        String _message = null;

        public String getMessage()
        {
            return _message;
        }

        public void setMessage(String message)
        {
            _message = message;
        }
    }

    @RequiresSiteAdmin
    public class ReloadFastaAction extends FormHandlerAction<Object>
    {
        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            int[] ids = PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), true));

            FastaReloaderJob job = new FastaReloaderJob(ids, getViewBackgroundInfo(), null);

            PipelineService.get().queueJob(job);

            return true;
        }

        @Override
        public ActionURL getSuccessURL(Object o)
        {
            return getShowProteinAdminUrl("FASTA reload queued. Monitor its progress using the job list at the bottom of this page.");
        }
    }

    @RequiresSiteAdmin
    public static class DeleteDataBasesAction extends FormHandlerAction<Object>
    {
        private String _message;

        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Migrate // TODO: Don't reference FastaAdmin directly (ms2 could register a call-back)
        @Override
        public boolean handlePost(Object o, BindException errors)
        {
            Set<String> fastaIdStrings = DataRegionSelection.getSelected(getViewContext(), true);
            Set<Integer> fastaIds = new HashSet<>();
            for (String fastaIdString : fastaIdStrings)
            {
                try
                {
                    fastaIds.add(Integer.parseInt(fastaIdString));
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Invalid FASTA ID: " + fastaIdString);
                }
            }
            String idList = StringUtils.join(fastaIds, ',');
            List<Integer> validIds = new SqlSelector(ProteinSchema.getSchema(), "SELECT FastaId FROM " + ProteinSchema.getTableInfoFastaAdmin() + " WHERE (FastaId <> 0) AND (Runs IS NULL) AND (FastaId IN (" + idList + "))").getArrayList(Integer.class);

            fastaIds.removeAll(validIds);

            if (!fastaIds.isEmpty())
            {
                _message = "Unable to delete FASTA ID(s) " + StringUtils.join(fastaIds, ", ") + " as they are still referenced by runs";
            }
            else
            {
                _message = "Successfully deleted " + validIds.size() + " FASTA record(s)";
            }

            for (int id : validIds)
                ProteinManager.deleteFastaFile(id);

            return true;
        }

        @Override
        public ActionURL getSuccessURL(Object o)
        {
            return getShowProteinAdminUrl(_message);
        }
    }

    @RequiresSiteAdmin
    public class TestFastaParsingAction extends SimpleViewAction<FastaParsingForm>
    {
        @Override
        public ModelAndView getView(FastaParsingForm form, BindException errors)
        {
            return new JspView<>("/org/labkey/protein/view/testFastaParsing.jsp", form);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addProteinAdminNavTrail(root, "Test FASTA header parsing", getPageConfig(), null);
        }
    }

    public static ActionURL getShowProteinAdminUrl()
    {
        return getShowProteinAdminUrl(null);
    }

    public static ActionURL getShowProteinAdminUrl(String message)
    {
        ActionURL url = new ActionURL(ShowProteinAdminAction.class, ContainerManager.getRoot());
        if (message != null)
        {
            url.addParameter("message", message);
        }
        return url;
    }

    @AdminConsoleAction
    @RequiresPermission(AdminOperationsPermission.class)
    public class ShowProteinAdminAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            GridView grid = getFastaAdminGrid();
            grid.setTitle("FASTA Files");
            GridView annots = new GridView(getAnnotInsertsGrid(), errors);
            annots.setTitle("Protein Annotations Loaded");

            QueryView jobsView = PipelineService.get().getPipelineQueryView(getViewContext(), PipelineService.PipelineButtonOption.Standard);
            jobsView.getSettings().setBaseFilter(new SimpleFilter(FieldKey.fromParts("Provider"), ProteinAnnotationPipelineProvider.NAME));
            jobsView.getSettings().setContainerFilterName(ContainerFilter.Type.AllFolders.toString());
            jobsView.setTitle("Protein Annotation Load Jobs");

            return new VBox(grid, annots, jobsView);
        }

        private DataRegion getAnnotInsertsGrid()
        {
            String columnNames = "InsertId, FileName, FileType, Comment, InsertDate, CompletionDate, RecordsProcessed";
            DataRegion rgn = new DataRegion();

            rgn.addColumns(ProteinSchema.getTableInfoAnnotInsertions(), columnNames);
            rgn.getDisplayColumn("fileType").setWidth("20");
            rgn.getDisplayColumn("insertId").setCaption("ID");
            rgn.getDisplayColumn("insertId").setWidth("5");
            ActionURL showURL = new ActionURL(ShowAnnotInsertDetailsAction.class, getContainer())
                    .addParameter("insertId", "${InsertId}");
            rgn.getDisplayColumn("insertId").setURL(showURL);
            rgn.setShowRecordSelectors(true);

            ButtonBar bb = new ButtonBar();

            ActionButton delete = new ActionButton(DeleteAnnotInsertEntriesAction.class, "Delete");
            delete.setRequiresSelection(true, "Are you sure you want to remove this entry from the list?\\n(Note: The protein annotations themselves will not be deleted.)", "Are you sure you want to remove these entries from the list?\\n(Note: The protein annotations themselves will not be deleted.)");
            delete.setActionType(ActionButton.Action.POST);
            bb.add(delete);

            ActionButton insertAnnots = new ActionButton(new ActionURL(InsertAnnotsAction.class, getContainer()), "Import Data");
            insertAnnots.setActionType(ActionButton.Action.LINK);
            bb.add(insertAnnots);

            ActionButton testFastaHeader = new ActionButton(new ActionURL(TestFastaParsingAction.class, getContainer()), "Test FASTA Header Parsing");
            testFastaHeader.setActionType(ActionButton.Action.LINK);
            bb.add(testFastaHeader);

            // Note: This button POSTs (default type)
            bb.add(new ActionButton(ReloadSPOMAction.class, "Reload SWP Org Map"));

            ActionButton reloadGO = new ActionButton(LoadGoAction.class, (GoLoader.isGoLoaded().booleanValue() ? "Reload" : "Load") + " Gene Ontology Data");
            reloadGO.setActionType(ActionButton.Action.LINK);
            bb.add(reloadGO);

            rgn.setButtonBar(bb);
            return rgn;
        }

        private GridView getFastaAdminGrid()
        {
            DataRegion rgn = new DataRegion();

            // FASTA data region modifier determines the table and columns displayed on the admin page
            ProteinManager.getFastaDataRegionModifier().accept(rgn);
            rgn.setShowRecordSelectors(true);

            GridView result = new GridView(rgn, (BindException)null);
            result.getRenderContext().setBaseSort(new Sort("FastaId"));

            ButtonBar bb = new ButtonBar();

            ActionButton delete = new ActionButton(new ActionURL(DeleteDataBasesAction.class, getContainer()), "Delete");
            delete.setActionType(ActionButton.Action.POST);
            delete.setRequiresSelection(true, "Are you sure you want to delete this FASTA record?", "Are you sure you want to delete these FASTA records?");
            bb.add(delete);

            ActionButton reload = new ActionButton(ReloadFastaAction.class, "Reload FASTA");
            reload.setActionType(ActionButton.Action.POST);
            reload.setRequiresSelection(true);
            bb.add(reload);

            ActionButton testFastaHeader = new ActionButton(new ActionURL(TestFastaParsingAction.class, getContainer()), "Test FASTA Header Parsing");
            testFastaHeader.setActionType(ActionButton.Action.LINK);
            bb.add(testFastaHeader);

            MenuButton setBestNameMenu = new MenuButton("Set Protein Best Name...");
            ActionURL setBestNameURL = new ActionURL(SetBestNameAction.class, getContainer());

            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.LOOKUP_STRING.toString());
            setBestNameMenu.addMenuItem("to name from FASTA", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.IPI.toString());
            setBestNameMenu.addMenuItem("to IPI (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.SWISS_PROT.toString());
            setBestNameMenu.addMenuItem("to Swiss-Prot Name (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.SWISS_PROT_ACCN.toString());
            setBestNameMenu.addMenuItem("to Swiss-Prot Accession (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));
            setBestNameURL.replaceParameter("nameType", SetBestNameForm.NameType.GEN_INFO.toString());
            setBestNameMenu.addMenuItem("to GI number (if available)", result.createVerifySelectedScript(setBestNameURL, "FASTA files"));

            bb.add(setBestNameMenu);

            rgn.setButtonBar(bb, DataRegion.MODE_GRID);
            return result;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            urlProvider(AdminUrls.class).addAdminNavTrail(root, "Protein Database Admin", getClass(), getContainer());
        }
    }

    public static class SetBestNameForm
    {
        public enum NameType
        { LOOKUP_STRING, IPI, SWISS_PROT, SWISS_PROT_ACCN, GEN_INFO }

        private String _nameType;

        public String getNameType()
        {
            return _nameType;
        }

        public NameType lookupNameType()
        {
            return NameType.valueOf(getNameType());
        }

        public void setNameType(String nameType)
        {
            _nameType = nameType;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public static class SetBestNameAction extends FormHandlerAction<SetBestNameForm>
    {
        @Override
        public void validateCommand(SetBestNameForm form, Errors errors)
        {
        }

        @Override
        public boolean handlePost(SetBestNameForm form, BindException errors)
        {
            int[] fastaIds = PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), true));
            SetBestNameRunnable runnable = new SetBestNameRunnable(fastaIds, form.lookupNameType());
            JobRunner.getDefault().execute(runnable);
            return true;
        }

        @Override
        public ActionURL getSuccessURL(SetBestNameForm form)
        {
            return getShowProteinAdminUrl();
        }
    }

    @RequiresSiteAdmin
    public static class ReloadSPOMAction extends FormHandlerAction<Object>
    {
        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(Object o, BindException errors) throws SQLException
        {
            ProtSprotOrgMap.loadProtSprotOrgMap();

            return true;
        }

        @Override
        public URLHelper getSuccessURL(Object o)
        {
            return getShowProteinAdminUrl("SWP organism map reload successful");
        }
    }

    @RequiresSiteAdmin
    public class InsertAnnotsAction extends FormViewAction<LoadAnnotForm>
    {
        @Override
        public void validateCommand(LoadAnnotForm target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(LoadAnnotForm form, boolean reshow, BindException errors)
        {
            return new JspView<>("/org/labkey/protein/view/insertAnnots.jsp", form, errors);
        }

        @Override
        public boolean handlePost(LoadAnnotForm form, BindException errors) throws Exception
        {
            String fname = form.getFileName();
            if (fname == null)
            {
                errors.addError(new LabKeyError("Please enter a file path."));
                return false;
            }
            File file = FileUtil.getAbsoluteCaseSensitiveFile(new File(fname));

            try
            {
                DefaultAnnotationLoader loader;

                //TODO: this style of dealing with different file types must be repaired.
                if ("uniprot".equalsIgnoreCase(form.getFileType()))
                {
                    loader = new XMLProteinLoader(file, getViewBackgroundInfo(), null, form.isClearExisting());
                }
                else if ("fasta".equalsIgnoreCase(form.getFileType()))
                {
                    FastaDbLoader fdbl = new FastaDbLoader(file, getViewBackgroundInfo(), null);
                    fdbl.setDefaultOrganism(form.getDefaultOrganism());
                    fdbl.setOrganismIsToGuessed(form.getShouldGuess() != null);
                    loader = fdbl;
                }
                else
                {
                    throw new IllegalArgumentException("Unknown annotation file type: " + form.getFileType());
                }

                loader.setComment(form.getComment());
                loader.validate();
                PipelineService.get().queueJob(loader);

                return true;
            }
            catch (IOException e)
            {
                errors.addError(new LabKeyError(e.getMessage()));
                return false;
            }
        }

        @Override
        public ActionURL getSuccessURL(LoadAnnotForm loadAnnotForm)
        {
            return getShowProteinAdminUrl("Annotation load queued. Monitor its progress using the job list at the bottom of this page.");
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addProteinAdminNavTrail(root, "Load Protein Annotations", getPageConfig(), null);
        }
    }

    public static class LoadAnnotForm
    {
        private String _fileType = "uniprot";
        private String _comment;
        private String _fileName;
        private String _defaultOrganism = "Unknown unknown";
        private String _shouldGuess = "1";
        private boolean _clearExisting;

        public String getFileType()
        {
            return _fileType;
        }

        @SuppressWarnings("unused")
        public void setFileType(String ft)
        {
            _fileType = ft;
        }

        public String getFileName()
        {
            return _fileName;
        }

        @SuppressWarnings("unused")
        public void setFileName(String file)
        {
            _fileName = file;
        }

        public String getComment()
        {
            return _comment;
        }

        @SuppressWarnings("unused")
        public void setComment(String s)
        {
            _comment = s;
        }

        public String getDefaultOrganism()
        {
            return _defaultOrganism;
        }

        @SuppressWarnings("unused")
        public void setDefaultOrganism(String o)
        {
            _defaultOrganism = o;
        }

        public String getShouldGuess()
        {
            return _shouldGuess;
        }

        @SuppressWarnings("unused")
        public void setShouldGuess(String shouldGuess)
        {
            _shouldGuess = shouldGuess;
        }

        public boolean isClearExisting()
        {
            return _clearExisting;
        }

        @SuppressWarnings("unused")
        public void setClearExisting(boolean clearExisting)
        {
            _clearExisting = clearExisting;
        }
    }

    @RequiresSiteAdmin
    public static class DeleteAnnotInsertEntriesAction extends FormHandlerAction<Object>
    {
        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(Object o, BindException errors)
        {
            int[] ids = PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), true));

            for (int id : ids)
                ProteinManager.deleteAnnotationInsertion(id);

            return true;
        }

        @Override
        public URLHelper getSuccessURL(Object o)
        {
            return getShowProteinAdminUrl();
        }
    }

    public static class AnnotationInsertionForm
    {
        private int _insertId;

        public int getInsertId()
        {
            return _insertId;
        }

        @SuppressWarnings("unused")
        public void setInsertId(int insertId)
        {
            _insertId = insertId;
        }
    }

    @RequiresSiteAdmin
    public class ShowAnnotInsertDetailsAction extends SimpleViewAction<AnnotationInsertionForm>
    {
        private AnnotationInsertion _insertion;

        @Override
        public ModelAndView getView(AnnotationInsertionForm form, BindException errors)
        {
            _insertion = new SqlSelector(ProteinSchema.getSchema(), "SELECT * FROM " + ProteinSchema.getTableInfoAnnotInsertions() + " WHERE InsertId = ?", form.getInsertId()).getObject(AnnotationInsertion.class);

            return new JspView<>("/org/labkey/protein/view/annotLoadDetails.jsp", _insertion);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addProteinAdminNavTrail(root, _insertion.getFiletype() + " Annotation Insertion Details: " + _insertion.getFilename(), getPageConfig(), null);
        }
    }

    public static class TestCase extends AbstractActionPermissionTest
    {
        @Override
        public void testActionPermissions()
        {
            User user = TestContext.get().getUser();
            assertTrue(user.hasSiteAdminPermission());

            AnnotController controller = new AnnotController();

            // @RequiresPermission(AdminPermission.class)
            assertForAdminPermission(user,
                new SetBestNameAction()
            );

            // @RequiresSiteAdmin
            assertForRequiresSiteAdmin(user,
                controller.new LoadGoAction(),
                controller.new GoStatusAction(),
                controller.new ReloadFastaAction(),
                new DeleteDataBasesAction(),
                controller.new TestFastaParsingAction(),
                new ReloadSPOMAction(),
                controller.new InsertAnnotsAction(),
                new DeleteAnnotInsertEntriesAction(),
                controller.new ShowAnnotInsertDetailsAction()
            );

            // @AdminConsoleAction
            // @RequiresPermission(AdminOperationsPermission.class)
            assertForAdminOperationsPermission(ContainerManager.getRoot(), user,
                controller.new ShowProteinAdminAction()
            );
        }
    }
}
