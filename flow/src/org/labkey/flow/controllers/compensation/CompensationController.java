/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

package org.labkey.flow.controllers.compensation;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.ConfirmAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.DbScope;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QueryForm;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewForm;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.query.FlowSchema;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class CompensationController extends BaseFlowController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(CompensationController.class);

    public CompensationController()
    {
        setActionResolver(_actionResolver);
    }


    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction<QueryForm>
    {
        public ModelAndView getView(QueryForm form, BindException errors) throws Exception
        {
            QuerySettings settings = getFlowSchema().getSettings(getViewContext(), "comp");
            return new CompensationListView(settings);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Compensation Matrices", new ActionURL(BeginAction.class, getContainer()));
            return root;
        }
    }


    @RequiresPermission(UpdatePermission.class)
    public class UploadAction extends FormViewAction<UploadCompensationForm>
    {
        FlowCompensationMatrix _flowComp = null;

        public void validateCommand(UploadCompensationForm target, Errors errors)
        {
        }

        public ModelAndView getView(UploadCompensationForm form, boolean reshow, BindException errors) throws Exception
        {
            return FormPage.getView(CompensationController.class, form, errors, "upload.jsp");
        }


        public boolean handlePost(UploadCompensationForm form, BindException errors) throws Exception
        {
            boolean hasErrors = false;
            ExperimentService svc = ExperimentService.get();
            MultipartFile compensationMatrixFile = getFileMap().get("ff_compensationMatrixFile");

            if (compensationMatrixFile.isEmpty() || compensationMatrixFile.getSize() == 0)
                hasErrors = addError(errors, "No file was uploaded");
            if (StringUtils.trimToNull(form.ff_compensationMatrixName) == null)
                hasErrors = addError(errors, "You must give the compensation matrix a name.");
            if (hasErrors)
                return false;

            String lsid = svc.generateLSID(getContainer(), FlowDataType.CompensationMatrix, form.ff_compensationMatrixName);
            if (svc.getExpData(lsid) != null)
            {
                addError(errors, "The name '" + form.ff_compensationMatrixName + "' is already being used.");
                return false;
            }
            CompensationMatrix comp;
            try
            {
                comp = new CompensationMatrix(compensationMatrixFile.getInputStream());
            }
            catch (Exception e)
            {
                addError(errors, "Error parsing file:" + e);
                return false;
            }
            AttributeSet attrs = new AttributeSet(comp);
            AttributeSetHelper.prepareForSave(attrs, getContainer(), true);
            try (DbScope.Transaction transaction = svc.ensureTransaction())
            {
                _flowComp = FlowCompensationMatrix.create(getUser(), getContainer(), form.ff_compensationMatrixName, attrs);
                transaction.commit();
            }
            return true;
        }

        public ActionURL getSuccessURL(UploadCompensationForm uploadCompensationForm)
        {
            return _flowComp.urlShow();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, null, "Upload a new compensation matrix");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class DownloadAction extends SimpleViewAction<ViewForm>
    {
        FlowCompensationMatrix _comp;

        public ModelAndView getView(ViewForm form, BindException errors) throws Exception
        {
            _comp = FlowCompensationMatrix.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
            if (_comp == null)
                throw new NotFoundException("Compensation Matrix not found");

            CompensationMatrix comp = _comp.getCompensationMatrix();
            if (comp == null)
                throw new NotFoundException("Compensation Matrix has no channels");

            FlowRun run = _comp.getRun();
            String fileName = comp.getName();
            if (fileName.endsWith(" " + run.getName()))
                fileName = fileName.substring(0, fileName.length() - run.getName().length() - 1);

            String result = comp.toExportFormat();

            HttpServletResponse response = getViewContext().getResponse();
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + FileUtil.makeLegalName(fileName) + "\";");
            response.getWriter().write(result);

            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresPermission(ReadPermission.class)
    public class ShowCompensationAction extends SimpleViewAction<ViewForm>
    {
        FlowCompensationMatrix _comp;

        public ModelAndView getView(ViewForm form, BindException errors) throws Exception
        {
            _comp = FlowCompensationMatrix.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
            return FormPage.getView(CompensationController.class, form, "showCompensation.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (null == _comp)
                return root;
            // show run this compensation was derived from
            if (_comp.getParent() != null)
                return appendFlowNavTrail(getPageConfig(), root, _comp, "Show Compensation " + _comp.getName());
            // fall back on showing compensaion query 
            else
                return (new BeginAction().appendNavTrail(root)).addChild(_comp.getLabel(), _comp.urlShow());
        }
    }



    @RequiresPermission(DeletePermission.class)
    public class DeleteAction extends ConfirmAction<ViewForm>
    {
        public void validateCommand(ViewForm target, Errors errors)
        {
        }

        public ModelAndView getConfirmView(ViewForm form, BindException errors) throws Exception
        {
            FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
            if (null == comp)
            {
                throw new NotFoundException();
            }
            return FormPage.getView(CompensationController.class, form, "delete.jsp");
        }

        public boolean handlePost(ViewForm viewForm, BindException errors) throws Exception
        {
            FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getActionURL(), getRequest(), getContainer(), getUser());
            if (null == comp)
            {
                throw new NotFoundException();
            }

            boolean hasErrors = false;
            if (comp.getRun() != null)
            {
                hasErrors = addError(errors, "This matrix cannot be deleted because belongs to a run.");
            }
            List<? extends ExpRun> runs = comp.getExpObject().getTargetRuns();
            if (runs.size() != 0)
            {
                hasErrors = addError(errors, "This matrix cannot be deleted because it has been used in " + runs.size() + " analysis runs.  Those runs must be deleted first.");
            }
            if (hasErrors)
            {
                return false;
            }
            try
            {
                comp.getExpObject().delete(getUser());
            }
            catch (Exception e)
            {
                hasErrors = addError(errors, "An exception occurred deleting the matrix: " + e);
            }
            return !hasErrors;
        }


        public ActionURL getFailURL(ViewForm viewForm, BindException errors)
        {
            return urlFor(BeginAction.class);
        }

        public ActionURL getSuccessURL(ViewForm viewForm)
        {
            return urlFor(BeginAction.class);
        }
    }


    protected boolean addError(BindException errors, String error)
    {
        errors.addError(new ObjectError("form", new String[]{"Error"}, new Object[] {error}, error));
        return true;
    }


    UserSchema _flowUserSchema = null;

    UserSchema getFlowSchema()
    {
        if (null == _flowUserSchema)
            _flowUserSchema = (UserSchema) DefaultSchema.get(getUser(), getContainer()).getSchema(FlowSchema.SCHEMANAME);
        return _flowUserSchema;
    }


    public class CompensationListView extends QueryView
    {
        CompensationListView(QuerySettings settings)
        {
            super(getFlowSchema());
            settings.setQueryName("CompensationMatrices");
            setSettings(settings);
        }
    }
}

