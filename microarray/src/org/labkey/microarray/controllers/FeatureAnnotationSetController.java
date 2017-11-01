/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
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
import org.labkey.microarray.query.MicroarrayUserSchema;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
    public class BeginAction extends RedirectAction
    {
        @Override
        public URLHelper getSuccessURL(Object o)
        {
            return new ActionURL(ManageAction.class, getContainer());
        }

        @Override
        public boolean doAction(Object o, BindException errors) throws Exception
        {
            return true;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ManageAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            MicroarrayUserSchema schema = new MicroarrayUserSchema(getUser(), getContainer());
            QueryView view = schema.createView(getViewContext(), MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET, MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET, errors);
            view.setFrame(WebPartView.FrameType.NONE);

            return view;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Manage Feature Annotation Sets");
        }
    }

    @RequiresPermission(DeletePermission.class)
    public class DeleteAction extends FormViewAction<DeleteFeatureAnnotationSetForm>
    {
        @Override
        public boolean handlePost(DeleteFeatureAnnotationSetForm form, BindException errors) throws Exception
        {
            DbSchema schema = MicroarrayUserSchema.getSchema();
            DbScope scope = schema.getScope();

            int rowsDeleted = MicroarrayManager.get().deleteFeatureAnnotationSet(form.getIds(false));

            // TODO catch somewhere on attempting to delete one that is in use, prompt to cascade the delete
            // Similarly, deleting a referenced sample set currently throws an FK exception. again, deal with it
            // gracefully and prompt to cascade.

            DataRegionSelection.clearAll(getViewContext());
            return true;
        }

        @Override
        public void validateCommand(DeleteFeatureAnnotationSetForm target, Errors errors)
        {

        }

        @Override
        public ModelAndView getView(DeleteFeatureAnnotationSetForm deleteFeatureAnnotationSetForm, boolean reshow, BindException errors) throws Exception
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public URLHelper getSuccessURL(DeleteFeatureAnnotationSetForm form)
        {
            return form.getReturnActionURL(new ActionURL(ManageAction.class, getContainer()));
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class UploadAction extends FormViewAction<FeatureAnnotationSetForm>
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
        public ModelAndView getView(FeatureAnnotationSetForm form, boolean reshow, BindException errors) throws Exception
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
        public NavTree appendNavTrail(NavTree root)
        {
            setHelpTopic("featureAnnotationSets");
            ActionURL url = new ActionURL(ManageAction.class, getContainer());
            return root.addChild("Feature Annotation Sets", url).addChild("Upload Annotation Set");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class DetailsAction extends SimpleViewAction<FeatureAnnotationSetForm>
    {
        @Override
        public ModelAndView getView(FeatureAnnotationSetForm form, BindException errors) throws Exception
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
            ActionURL editURL = QueryService.get().urlDefault(getContainer(), QueryAction.updateQueryRow, schema.getPath().toString(), featureAnnotationSetTable.getName());
            editURL.addParameter("RowId", form.getRowId());
            editURL.addReturnURL(getViewContext().getActionURL());

            ActionButton edit = new ActionButton(editURL, "Edit", DataRegion.MODE_DETAILS);
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

            return new VBox(dv, grid);
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            setHelpTopic("featureAnnotationSets");
            ActionURL url = new ActionURL(ManageAction.class, getContainer());
            return root.addChild("Feature Annotation Sets", url).addChild("Feature Annotation Set");
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
        private Integer _singleObjectRowId;

        public int[] getIds(boolean clear)
        {
            if (_singleObjectRowId != null)
            {
                return new int[] {_singleObjectRowId};
            }
            return PageFlowUtil.toInts(DataRegionSelection.getSelected(getViewContext(), clear));
        }

        public Integer getSingleObjectRowId()
        {
            return _singleObjectRowId;
        }

        public void setSingleObjectRowId(Integer singleObjectRowId)
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

        public String getDataRegionSelectionKey()
        {
            return _dataRegionSelectionKey;
        }

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

}
