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

package org.labkey.protein;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.exp.DomainDescriptor;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.iterator.ValidatingDataRowIterator;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.protein.AnnotationInsertion;
import org.labkey.api.protein.CustomAnnotationImportHelper;
import org.labkey.api.protein.CustomAnnotationSet;
import org.labkey.api.protein.CustomAnnotationSetManager;
import org.labkey.api.protein.CustomAnnotationType;
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
import org.labkey.api.protein.query.CustomAnnotationSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.AdminConsoleAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AbstractActionPermissionTest;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.TestContext;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.GridView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.TabStripView;
import org.labkey.api.view.VBox;
import org.labkey.api.view.template.PageConfig;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ProteinController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ProteinController.class);
    
    public ProteinController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public static class BeginAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            return new CustomProteinListView(getViewContext(), true);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Custom Protein Lists");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowAnnotationSetAction extends ShowSetAction
    {
        public ShowAnnotationSetAction()
        {
            super(false);
        }
    }

    public abstract class ShowSetAction extends SimpleViewAction<Object>
    {
        private final boolean _showSequences;
        private String _setName;

        protected ShowSetAction(boolean showSequences)
        {
            _showSequences = showSequences;
        }

        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            UserSchema schema = new CustomAnnotationSchema(getUser(), getContainer(), _showSequences);
            QuerySettings settings = schema.getSettings(getViewContext(), "CustomAnnotation");
            settings.getQueryDef(schema);
            settings.setAllowChooseQuery(true);
            settings.setAllowChooseView(true);
            _setName = settings.getQueryName();

            // 41640: Expect valid "CustomAnnotation.queryName" URL parameter
            if (StringUtils.isEmpty(_setName))
                throw new NotFoundException("Custom Protein List not found.");

            QueryView queryView = new QueryView(schema, settings, errors)
            {
                @Override
                public DataView createDataView()
                {
                    DataView result = super.createDataView();
                    result.getRenderContext().setBaseSort(new Sort("LookupString"));
                    return result;
                }

                @Override
                public MenuButton createQueryPickerButton(String label)
                {
                    return super.createQueryPickerButton("Custom Protein List");
                }
            };

            queryView.setShowExportButtons(true);
            queryView.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);

            ActionURL url;

            HtmlStringBuilder header = HtmlStringBuilder.of();
            if (_showSequences)
            {
                url = new ActionURL(ShowAnnotationSetAction.class, getContainer());
                url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
                header.append("This view shows your protein list with all the proteins that match. If more than one sequence matches you will get multiple rows. ").append(PageFlowUtil.link("show without proteins").href(url));
            }
            else
            {
                url = new ActionURL(ShowAnnotationSetWithSequencesAction.class, getContainer());
                url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
                header.append("This view shows just the data uploaded as part of the list. ").append(PageFlowUtil.link("show with matching proteins loaded into this server").href(url));
            }

            HtmlView linkView = new HtmlView(header);

            return new VBox(linkView, queryView);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Custom Protein Lists", getBeginURL(getContainer()));
            root.addChild("Custom Protein List: " + _setName);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowAnnotationSetWithSequencesAction extends ShowSetAction
    {
        public ShowAnnotationSetWithSequencesAction()
        {
            super(true);
        }
    }

    @RequiresPermission(DeletePermission.class)
    public static class DeleteCustomAnnotationSetsAction extends FormHandlerAction<Object>
    {
        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(Object o, BindException errors)
        {
            Set<Integer> setIds = DataRegionSelection.getSelectedIntegers(getViewContext(), true);
            for (Integer id : setIds)
            {
                CustomAnnotationSet set = CustomAnnotationSetManager.getCustomAnnotationSet(getContainer(), id, false);
                if (set != null)
                {
                    CustomAnnotationSetManager.deleteCustomAnnotationSet(set);
                }
            }
            return true;
        }

        @Override
        public ActionURL getSuccessURL(Object o)
        {
            return getBeginURL(getContainer());
        }
    }

    public static ActionURL getBeginURL(Container c)
    {
        return new ActionURL(BeginAction.class, c);
    }

    @RequiresPermission(InsertPermission.class)
    public static class UploadCustomProteinAnnotations extends FormViewAction<UploadAnnotationsForm>
    {
        @Override
        public void validateCommand(UploadAnnotationsForm target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(UploadAnnotationsForm form, boolean reshow, BindException errors)
        {
            return new JspView<>("/org/labkey/ms2/protein/uploadCustomProteinAnnotations.jsp", form, errors);
        }

        @Override
        public boolean handlePost(UploadAnnotationsForm form, BindException errors) throws Exception
        {
            if (form.getName().isEmpty())
            {
                errors.addError(new ObjectError("main", null, null, "You must enter a name for the protein list."));
            }
            Map<String, CustomAnnotationSet> sets = CustomAnnotationSetManager.getCustomAnnotationSets(getContainer(), false);
            CaseInsensitiveHashSet names = new CaseInsensitiveHashSet(sets.keySet());
            if (names.contains(form.getName()))
            {
                errors.addError(new ObjectError("main", null, null, "There is already an protein list with the name '" + form.getName() + "' loaded."));
            }

            TabLoader tabLoader = new TabLoader(form.getAnnotationsText(), true);

            List<Map<String, Object>> rows = tabLoader.load();
            ColumnDescriptor[] columns = tabLoader.getColumns();
            String lookupStringColumnName = null;

            CustomAnnotationType type = CustomAnnotationType.valueOf(form.getAnnotationType());

            if (rows.isEmpty())
            {
                errors.addError(new ObjectError("main", null, null, "Your protein list must have at least one protein, plus the header line"));
            }
            else
            {
                Set<String> columnNames = new CaseInsensitiveHashSet();
                for (ColumnDescriptor column : columns)
                {
                    if (!columnNames.add(column.name))
                    {
                        errors.addError(new ObjectError("main", null, null, "Duplicate column name: " + column.name));
                    }
                }

                lookupStringColumnName = columns[0].name;

                Set<String> lookupStrings = new CaseInsensitiveHashSet();
                for (Map<String, Object> row : rows)
                {
                    String lookupString = CustomAnnotationImportHelper.convertLookup(row.get(lookupStringColumnName));
                    if (lookupString == null || lookupString.isEmpty())
                    {
                        errors.addError(new ObjectError("main", null, null, "All rows must contain a protein identifier."));
                        break;
                    }

                    String error = type.validateUserLookupString(lookupString);
                    if (error != null)
                    {
                        errors.addError(new ObjectError("main", null, null, error));
                        break;
                    }

                    if (!lookupStrings.add(lookupString))
                    {
                        errors.addError(new ObjectError("main", null, null, "The input contains multiple entries for the protein " + lookupString));
                        break;
                    }
                    row.put(lookupStringColumnName, lookupString);
                    for (Object o : row.values())
                    {
                        if (o != null && o.toString().length() >= 4000)
                        {
                            errors.addError(new ObjectError("main", null, null, "The input contains a value that is more than 4000 characters long, which is the limit for a single value"));
                            break;
                        }
                    }
                }
            }

            if (errors.getErrorCount() > 0)
            {
                return false;
            }

            DbScope scope = ProteinSchema.getSchema().getScope();

            try (DbScope.Transaction transaction = scope.ensureTransaction())
            {
                Connection connection = transaction.getConnection();
                CustomAnnotationSet annotationSet = new CustomAnnotationSet();
                Date modified = new Date();
                annotationSet.setModified(modified);
                annotationSet.setCreated(modified);
                User user = getUser();
                annotationSet.setCreatedBy(user.getUserId());
                annotationSet.setModifiedBy(user.getUserId());
                annotationSet.setContainer(getContainer().getId());
                annotationSet.setName(form.getName());
                annotationSet.setCustomAnnotationType(type.toString());

                annotationSet = Table.insert(getUser(), ProteinSchema.getTableInfoCustomAnnotationSet(), annotationSet);
                annotationSet.setLsid(new Lsid(CustomAnnotationSet.TYPE, Integer.toString(annotationSet.getCustomAnnotationSetId())).toString());
                annotationSet = Table.update(getUser(), ProteinSchema.getTableInfoCustomAnnotationSet(), annotationSet, annotationSet.getCustomAnnotationSetId());

                @SuppressWarnings("SqlDialectInspection")
                String sb = "INSERT INTO " + ProteinSchema.getTableInfoCustomAnnotation() + "(CustomAnnotationSetId, LookupString, ObjectURI) VALUES (?, ?, ?)";

                PreparedStatement stmt = connection.prepareStatement(sb);
                stmt.setInt(1, annotationSet.getCustomAnnotationSetId());

                List<PropertyDescriptor> descriptors = new ArrayList<>();

                for (int i = 1; i < columns.length; i++)
                {
                    ColumnDescriptor cd = columns[i];
                    PropertyDescriptor pd = new PropertyDescriptor();
                    DomainDescriptor dd = new DomainDescriptor.Builder(annotationSet.getLsid(), getContainer()).build();

                    //todo :  name for domain?
                    pd.setName(cd.name);
                    String legalName = ColumnInfo.legalNameFromName(cd.name);
                    String propertyURI = annotationSet.getLsid() + "#" + legalName;
                    pd.setPropertyURI(propertyURI);
                    pd.setRangeURI(PropertyType.getFromClass(cd.clazz).getTypeUri());
                    pd.setContainer(getContainer());
                    //Change name to be fully qualified string for property
                    pd = OntologyManager.insertOrUpdatePropertyDescriptor(pd, dd, i - 1);

                    cd.name = pd.getPropertyURI();
                    descriptors.add(pd);
                }

                rows = tabLoader.load();

                int ownerObjectId = OntologyManager.ensureObject(getContainer(), annotationSet.getLsid());
                OntologyManager.ImportHelper helper = new CustomAnnotationImportHelper(stmt, connection, annotationSet.getLsid(), lookupStringColumnName);

                OntologyManager.insertTabDelimited(getContainer(), getUser(), ownerObjectId, helper, descriptors, ValidatingDataRowIterator.of(rows.iterator()), false);

                stmt.executeBatch();
                connection.commit();

                transaction.commit();
            }
            catch (ValidationException ve)
            {
                for (ValidationError error : ve.getErrors())
                    errors.reject(SpringActionController.ERROR_MSG, PageFlowUtil.filter(error.getMessage()));
                return false;
            }
            return true;
        }

        @Override
        public ActionURL getSuccessURL(UploadAnnotationsForm uploadAnnotationsForm)
        {
            return getBeginURL(getContainer());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Custom Protein Lists", getBeginURL(getContainer()));
            root.addChild("Upload Custom Protein List");
        }
    }

    public static class UploadAnnotationsForm
    {
        private String _name;
        private String _annotationsText;
        private String _annotationType;

        public String getName()
        {
            return Objects.requireNonNullElse(_name, "");
        }

        @SuppressWarnings("unused")
        public void setName(String name)
        {
            if (name != null)
            {
                name = name.trim();
            }
            _name = name;
        }

        public String getAnnotationsText()
        {
            return Objects.requireNonNullElse(_annotationsText, "");
        }

        @SuppressWarnings("unused")
        public void setAnnotationsText(String annotationsText)
        {
            if (annotationsText != null)
            {
                annotationsText = annotationsText.trim();
            }
            _annotationsText = annotationsText;
        }

        public String getAnnotationType()
        {
            return _annotationType;
        }

        @SuppressWarnings("unused")
        public void setAnnotationType(String annotationType)
        {
            _annotationType = annotationType;
        }
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
            public JspView<Object> getTabView(String tabId)
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

        @SuppressWarnings("unused")
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

            SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("FastaId"), fastaIds));
            filter.addCondition(FieldKey.fromString("FastaId"), 0, CompareType.NEQ);
            Collection<Integer> validIds = ProteinSchema.getValidForFastaDeleteSelectorProvider().apply(filter).getCollection(Integer.class);

            fastaIds.removeAll(validIds);

            if (!fastaIds.isEmpty())
            {
                _message = "Unable to delete FASTA ID(s) " + StringUtils.join(fastaIds, ", ") + " as they " + ProteinSchema.getInvalidForFastaDeleteReason();
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

    public static class AdminForm
    {
        private String _message;

        public String getMessage()
        {
            return _message;
        }

        @SuppressWarnings("unused")
        public void setMessage(String message)
        {
            _message = message;
        }
    }

    @AdminConsoleAction
    @RequiresPermission(AdminOperationsPermission.class)
    public class ShowProteinAdminAction extends SimpleViewAction<AdminForm>
    {
        @Override
        public ModelAndView getView(AdminForm form, BindException errors)
        {
            GridView grid = getFastaAdminGrid();
            grid.setTitle("FASTA Files");
            GridView annots = new GridView(getAnnotInsertsGrid(), errors);
            annots.setTitle("Protein Annotations Loaded");

            QueryView jobsView = PipelineService.get().getPipelineQueryView(getViewContext(), PipelineService.PipelineButtonOption.Standard);
            jobsView.getSettings().setBaseFilter(new SimpleFilter(FieldKey.fromParts("Provider"), ProteinAnnotationPipelineProvider.NAME));
            jobsView.getSettings().setContainerFilterName(ContainerFilter.Type.AllFolders.toString());
            jobsView.setTitle("Protein Annotation Load Jobs");

            VBox vbox = new VBox(grid, annots, jobsView);
            if (form.getMessage() != null)
            {
                HtmlView messageView = new HtmlView("Admin Message", HtmlString.unsafe("<strong><span class=\"labkey-message\">" + PageFlowUtil.filter(form.getMessage()) + "</span></strong>"));
                vbox.addView(messageView, 0);
            }
            return vbox;
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
            delete.setRequiresSelection(true,
                "Are you sure you want to remove this entry from the list? (Note: The protein annotations themselves will not be deleted.)",
                "Are you sure you want to remove these entries from the list? (Note: The protein annotations themselves will not be deleted.)");
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

        @SuppressWarnings("unused")
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

            ProteinController controller = new ProteinController();

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
