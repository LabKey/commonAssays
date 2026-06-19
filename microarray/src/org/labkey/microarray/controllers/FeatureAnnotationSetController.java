/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.microarray.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.ValidationException;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AbstractContainerScopingTest;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.roles.EditorRole;
import org.labkey.api.security.roles.ReaderRole;
import org.labkey.api.test.TestWhen;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewForm;
import org.labkey.api.view.WebPartView;
import org.labkey.microarray.MicroarrayManager;
import org.labkey.microarray.MicroarrayModule;
import org.labkey.microarray.query.MicroarrayUserSchema;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 1/15/14
 */
public class FeatureAnnotationSetController extends SpringActionController
{
    private static final SpringActionController.DefaultActionResolver _actionResolver = new SpringActionController.DefaultActionResolver(FeatureAnnotationSetController.class);

    public FeatureAnnotationSetController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public static class BeginAction extends SimpleRedirectAction<Object>
    {
        @Override
        public URLHelper getRedirectURL(Object o)
        {
            return new ActionURL(ManageAction.class, getContainer());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public static class ManageAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            MicroarrayUserSchema schema = new MicroarrayUserSchema(getUser(), getContainer());
            QueryView view = schema.createView(getViewContext(), MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET, MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET, errors);
            view.setFrame(WebPartView.FrameType.NONE);

            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Manage Feature Annotation Sets");
        }
    }

    @RequiresPermission(DeletePermission.class)
    public static class DeleteAction extends FormViewAction<DeleteFeatureAnnotationSetForm>
    {
        @Override
        public boolean handlePost(DeleteFeatureAnnotationSetForm form, BindException errors)
        {
            List<Integer> deletableRowIds = MicroarrayManager.get().getFeatureAnnotationSets(getContainer(), getUser(), form.getIds(false), DeletePermission.class);
            if (!deletableRowIds.isEmpty())
                MicroarrayManager.get().deleteFeatureAnnotationSet(deletableRowIds);

            // TODO catch somewhere on attempting to delete one that is in use, prompt to cascade the delete
            // Similarly, deleting a referenced sample type currently throws an FK exception. again, deal with it
            // gracefully and prompt to cascade.

            DataRegionSelection.clearAll(getViewContext());
            return true;
        }

        @Override
        public void validateCommand(DeleteFeatureAnnotationSetForm target, Errors errors)
        {

        }

        @Override
        public ModelAndView getView(DeleteFeatureAnnotationSetForm deleteFeatureAnnotationSetForm, boolean reshow, BindException errors)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public URLHelper getSuccessURL(DeleteFeatureAnnotationSetForm form)
        {
            return form.getReturnActionURL(new ActionURL(ManageAction.class, getContainer()));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermission(InsertPermission.class)
    public static class UploadAction extends FormViewAction<FeatureAnnotationSetForm>
    {
        @Override
        public void validateCommand(FeatureAnnotationSetForm form, Errors errors)
        {
            Map<String, MultipartFile> fileMap = getFileMap();
            MultipartFile annotationFile = fileMap.get("annotationFile");

            if (form.getName() == null || StringUtils.trimToNull(form.getName()) == null)
            {
                errors.reject(ERROR_MSG, "Name is required.");
            }

            if (form.getVendor() == null || StringUtils.trimToNull(form.getVendor()) == null)
            {
                errors.reject(ERROR_MSG, "Vendor is required.");
            }

            if (null == annotationFile)
            {
                errors.reject(ERROR_MSG, "An annotation file is required.");
            }

            if (null != annotationFile && annotationFile.getSize() == 0)
            {
                errors.reject(ERROR_MSG, "The annotation file cannot be blank");
            }

            Container targetContainer = ContainerManager.getForRowId(form.getTargetContainer());
            if (targetContainer == null)
            {
                errors.reject(ERROR_MSG, "Target folder doesn't exist.");
            }

            if (targetContainer != null && !targetContainer.hasPermission(getUser(), InsertPermission.class))
            {
                errors.reject(ERROR_MSG, "You do not have insert permissions to the target folder.");
            }

            // TODO check if feature set with name already exists.
        }

        @Override
        public ModelAndView getView(FeatureAnnotationSetForm form, boolean reshow, BindException errors)
        {
            return new JspView<>("/org/labkey/microarray/view/uploadFeatureAnnotation.jsp", form, errors);
        }

        @Override
        public boolean handlePost(FeatureAnnotationSetForm form, BindException errors) throws Exception
        {
            Map<String, MultipartFile> fileMap = getFileMap();
            MultipartFile annotationFile = fileMap.get("annotationFile");
            DataLoader loader = DataLoader.get().createLoader(annotationFile, true, null, null);

            Container targetContainer = ContainerManager.getForRowId(form.getTargetContainer());
            if (targetContainer == null || !targetContainer.hasPermission(getUser(), InsertPermission.class))
                throw new UnauthorizedException();

            DbScope scope = MicroarrayUserSchema.getSchema().getScope();

            try (DbScope.Transaction transaction = scope.ensureTransaction())
            {
                BatchValidationException batchErrors = new BatchValidationException();
                Integer rowsInserted = MicroarrayManager.get().createFeatureAnnotationSet(getUser(), targetContainer, form, loader, batchErrors);

                if (batchErrors.hasErrors())
                {
                    addErrors(batchErrors, errors);
                    return false;
                }

                if (rowsInserted <= 0)
                {
                    errors.reject(ERROR_MSG, "Error: No rows inserted into FeatureAnnotation table.");
                }

                if (!errors.hasErrors() && !batchErrors.hasErrors())
                {
                    transaction.commit();
                }
            }
            catch (SQLException | DuplicateKeyException | BatchValidationException | QueryUpdateServiceException e)
            {
                errors.reject(ERROR_MSG, e.getMessage());
            }

            return !errors.hasErrors();
        }

        private void addErrors(BatchValidationException batchErrors, BindException errors)
        {
            for (ValidationException batchError : batchErrors.getRowErrors())
            {
                errors.reject(ERROR_MSG, batchError.getMessage());
            }
        }

        @Override
        public URLHelper getSuccessURL(FeatureAnnotationSetForm form)
        {
            return form.getReturnActionURL(new ActionURL(ManageAction.class, getContainer()));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            setHelpTopic("featureAnnotationSets");
            ActionURL url = new ActionURL(ManageAction.class, getContainer());
            root.addChild("Feature Annotation Sets", url);
            root.addChild("Upload Annotation Set");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public static class DetailsAction extends SimpleViewAction<FeatureAnnotationSetForm>
    {
        @Override
        public ModelAndView getView(FeatureAnnotationSetForm form, BindException errors)
        {
            Integer rowId = form.getRowId();
            if (rowId == null)
                throw new NotFoundException("Feature annotation set rowId required");

            DataRegion dr = new DataRegion();
            DetailsView dv = new DetailsView(dr, form.getRowId());
            MicroarrayUserSchema schema = new MicroarrayUserSchema(getUser(), getContainer());
            TableInfo featureAnnotationSetTable = schema.getAnnotationSetTable();
            dr.setTable(featureAnnotationSetTable);

            Collection<FieldKey> fasColumns = new ArrayList<>();
            fasColumns.add(FieldKey.fromParts("Name"));
            fasColumns.add(FieldKey.fromParts("Vendor"));
            fasColumns.add(FieldKey.fromParts("Description"));
            fasColumns.add(FieldKey.fromParts("Created"));
            fasColumns.add(FieldKey.fromParts("CreatedBy"));
            fasColumns.add(FieldKey.fromParts("Modified"));
            fasColumns.add(FieldKey.fromParts("ModifiedBy"));
            Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(featureAnnotationSetTable, fasColumns);
            dr.addColumns(new ArrayList<>(columns.values()));

            ButtonBar bb = new ButtonBar();
            ActionURL editURL = QueryService.get().urlDefault(getContainer(), QueryAction.updateQueryRow, schema.getSchemaPath().toString(), featureAnnotationSetTable.getName());
            editURL.addParameter("RowId", form.getRowId());
            editURL.addReturnUrl(getViewContext().getActionURL());

            ActionButton edit = new ActionButton(editURL, "Edit");
            edit.setActionType(ActionButton.Action.LINK);
            edit.setDisplayPermission(UpdatePermission.class);
            bb.add(edit);
            bb.setStyle(ButtonBar.Style.separateButtons);
            dr.setButtonBar(bb);
            dr.setShowBorders(true);
            dr.setShowSurroundingBorder(true);

            QuerySettings settings = schema.getSettings(getViewContext(), "featureAnnotations", MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION);
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("FeatureAnnotationSetId"), form.getRowId()));

            QueryView grid = schema.createView(getViewContext(), settings, errors);

            grid.setTitle("Feature Annotations");
            grid.setShowDetailsColumn(false);
            grid.setShowUpdateColumn(false);
            grid.setShowInsertNewButton(false);
            grid.setShowImportDataButton(false);
            grid.setShowDeleteButton(false);
            grid.setAllowableContainerFilterTypes();

            return new VBox(dv, grid);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            setHelpTopic("featureAnnotationSets");
            ActionURL url = new ActionURL(ManageAction.class, getContainer());
            root.addChild("Feature Annotation Sets", url);
            root.addChild("Feature Annotation Set");
        }
    }

    public static class DeleteFeatureAnnotationSetForm extends ViewForm implements DataRegionSelection.DataSelectionKeyForm
    {
        Integer _rowId;

        public Integer getRowId()
        {
            return _rowId;
        }

        public void setRowId(Integer rowId)
        {
            _rowId = rowId;
        }

        private boolean _forceDelete;
        private String _dataRegionSelectionKey;
        private Long _singleObjectRowId;

        public Set<Long> getIds(boolean clear)
        {
            if (_singleObjectRowId != null)
            {
                return Set.of(_singleObjectRowId);
            }

            return DataRegionSelection.getSelectedIntegers(getViewContext(), clear);
        }

        public Long getSingleObjectRowId()
        {
            return _singleObjectRowId;
        }

        public void setSingleObjectRowId(Long singleObjectRowId)
        {
            _singleObjectRowId = singleObjectRowId;
        }

        public boolean isForceDelete()
        {
            return _forceDelete;
        }

        public void setForceDelete(boolean forceDelete)
        {
            _forceDelete = forceDelete;
        }

        @Override
        public String getDataRegionSelectionKey()
        {
            return _dataRegionSelectionKey;
        }

        @Override
        public void setDataRegionSelectionKey(String dataRegionSelectionKey)
        {
            _dataRegionSelectionKey = dataRegionSelectionKey;
        }
    }

    public static class FeatureAnnotationSetForm extends ViewForm
    {
        private String _name;
        private String _vendor;
        private String _description;
        private String _comment;
        private Integer _rowId;
        private int _targetContainer;

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getVendor()
        {
            return _vendor;
        }

        public void setVendor(String vendor)
        {
            _vendor = vendor;
        }

        public String getDescription()
        {
            return _description;
        }

        public void setDescription(String description)
        {
            _description = description;
        }

        public String getComment()
        {
            return _comment;
        }

        public void setComment(String comment)
        {
            _comment = comment;
        }

        public Integer getRowId()
        {
            return _rowId;
        }

        public void setRowId(Integer rowId)
        {
            _rowId = rowId;
        }

        public int getTargetContainer()
        {
            return _targetContainer;
        }

        public void setTargetContainer(int container)
        {
            _targetContainer = container;
        }
    }

    @TestWhen(TestWhen.When.BVT)
    public static class ContainerScopingTestCase extends AbstractContainerScopingTest
    {
        // Executed in its own project due to folder scoping rules for microarray
        private static final String PROJECT_NAME = "FeatureAnnotationContainerScopingTestCase Project";

        private Container project;
        private Container folderA;
        private int projectSetRowId;

        @Before
        public void setUp() throws Exception
        {
            deleteProject();
            project = activateModules(ContainerManager.ensureContainer(PROJECT_NAME, getAdmin()));
            folderA = activateModules(ContainerManager.ensureContainer(project, "Folder A", getAdmin()));
            projectSetRowId = insertFeatureAnnotationSet(project, "Project Set");
        }

        @After
        public void tearDown()
        {
            deleteProject();
        }

        private void deleteProject()
        {
            Container c = ContainerManager.getForPath(PROJECT_NAME);
            if (c != null)
                ContainerManager.deleteAll(c, getAdmin());
        }

        private Container activateModules(Container c)
        {
            Set<Module> modules = new HashSet<>(c.getActiveModules(getAdmin()));
            modules.add(ModuleLoader.getInstance().getModule(MicroarrayModule.NAME));
            c.setActiveModules(modules, getAdmin());
            return c;
        }

        @Test
        public void testDeleteActionCannotDeleteForeignSet() throws Exception
        {
            // The caller is an Editor (has DeletePermission) in folderA only, with no access to the project.
            User editor = createUserInRole(folderA, EditorRole.class);

            // Negative control: the caller can't read the project, so the project set is out of scope and survives.
            post(deleteUrl(folderA, projectSetRowId), editor);
            assertTrue("A feature annotation set the caller cannot see must not be deleted",
                    featureAnnotationSetExists(projectSetRowId));

            // Positive control: deleting a set that actually lives in the caller's own folder succeeds.
            int folderASetRowId = insertFeatureAnnotationSet(folderA, "Folder A Set");
            post(deleteUrl(folderA, folderASetRowId), editor);
            assertFalse("A feature annotation set in the caller's own folder should be deleted",
                    featureAnnotationSetExists(folderASetRowId));
        }

        @Test
        public void testDeleteActionRejectsInScopeSetWithoutDeletePermission() throws Exception
        {
            // Editor in folderA (passes the action's DeletePermission check) plus Reader in the project.
            User editor = createUserInRole(folderA, EditorRole.class);
            grantRole(editor, project, ReaderRole.class);

            // The project set is now in scope, but the caller can't delete it there, so the action rejects the request.
            assertStatus(HttpServletResponse.SC_FORBIDDEN, post(deleteUrl(folderA, projectSetRowId), editor));
            assertTrue("A set the caller may read but not delete must not be deleted",
                    featureAnnotationSetExists(projectSetRowId));
        }

        @Test
        public void testDeleteActionRejectsBatchContainingUndeletableSet() throws Exception
        {
            User editor = createUserInRole(folderA, EditorRole.class);
            grantRole(editor, project, ReaderRole.class);
            int folderASetRowId = insertFeatureAnnotationSet(folderA, "Folder A Set");

            // A multi-select delete mixing a deletable set with one in a Reader-only folder must be rejected wholesale.
            assertStatus(HttpServletResponse.SC_FORBIDDEN, post(deleteUrl(folderA, folderASetRowId, projectSetRowId), editor));

            // Fail-closed: neither set is deleted
            assertTrue("Deletable set in a rejected batch must be preserved", featureAnnotationSetExists(folderASetRowId));
            assertTrue("Undeletable set in another container must be preserved", featureAnnotationSetExists(projectSetRowId));
        }

        private static ActionURL deleteUrl(Container c, int rowId)
        {
            return new ActionURL(DeleteAction.class, c).addParameter("singleObjectRowId", rowId);
        }

        private static ActionURL deleteUrl(Container c, int... rowIds)
        {
            ActionURL url = new ActionURL(DeleteAction.class, c);
            for (int rowId : rowIds)
                url.addParameter(DataRegion.SELECT_CHECKBOX_NAME, rowId);
            return url;
        }

        private int insertFeatureAnnotationSet(Container c, String name) throws Exception
        {
            BatchValidationException errors = new BatchValidationException();
            Integer rowId = MicroarrayManager.get().insertFeatureAnnotationSet(getAdmin(), c, name, "Test", null, null, errors);
            if (errors.hasErrors())
                throw errors;
            return rowId;
        }

        private boolean featureAnnotationSetExists(int rowId)
        {
            return new TableSelector(MicroarrayManager.getAnnotationSetSchemaTableInfo(), new SimpleFilter(FieldKey.fromParts("RowId"), rowId), null).exists();
        }
    }
}
