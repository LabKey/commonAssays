/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.action.ConfirmAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.query.*;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.view.*;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.query.FlowSchema;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;


public class CompensationController extends SpringFlowController<CompensationController.Action>
{
    public enum Action
    {
        begin,
        showCompensation,
        upload,
        delete
    }

    static DefaultActionResolver _actionResolver = new DefaultActionResolver(CompensationController.class);

    public CompensationController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }


    private ActionURL actionURL(Action action)
    {
        return new ActionURL("flow-compensation", action.name(), getContainer());
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction<QueryForm>
    {
        public ModelAndView getView(QueryForm form, BindException errors) throws Exception
        {
            QuerySettings settings = getFlowSchema().getSettings(getViewContext(), "comp");
            return new CompensationListView(settings);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Compensation Matrices", actionURL(Action.begin));
            return root;
        }
    }


    @RequiresPermissionClass(UpdatePermission.class)
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
            ExperimentService.Interface svc = ExperimentService.get();
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
            boolean fTrans = false;
            try
            {
                AttributeSet attrs = new AttributeSet(comp);
                attrs.prepareForSave();
                if (!svc.isTransactionActive())
                {
                    svc.beginTransaction();
                    fTrans = true;
                }
                _flowComp = FlowCompensationMatrix.create(getUser(), getContainer(), form.ff_compensationMatrixName, attrs);
                if (fTrans)
                {
                    svc.commitTransaction();
                    fTrans = false;
                }
            }
            finally
            {
                if (fTrans)
                {
                    svc.rollbackTransaction();
                }
            }
            return true;
        }

        public ActionURL getSuccessURL(UploadCompensationForm uploadCompensationForm)
        {
            return _flowComp.urlShow();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, null, "Upload a new compensation matrix", Action.upload);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowCompensationAction extends SimpleViewAction<ViewForm>
    {
        FlowCompensationMatrix _comp;

        public ModelAndView getView(ViewForm form, BindException errors) throws Exception
        {
            _comp = FlowCompensationMatrix.fromURL(getActionURL(), getRequest());
            return FormPage.getView(CompensationController.class, form, "showCompensation.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (null == _comp)
                return root;
            // show run this compensation was derived from
            if (_comp.getParent() != null)
                return appendFlowNavTrail(root, _comp, "Show Compensation " + _comp.getName(), Action.showCompensation);
            // fall back on showing compensaion query 
            else
                return (new BeginAction().appendNavTrail(root)).addChild(_comp.getLabel(), _comp.urlShow());
        }
    }



    @RequiresPermissionClass(DeletePermission.class)
    public class DeleteAction extends ConfirmAction<ViewForm>
    {
        public void validateCommand(ViewForm target, Errors errors)
        {
        }

        public ModelAndView getConfirmView(ViewForm form, BindException errors) throws Exception
        {
            FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getActionURL(), getRequest());
            if (null == comp)
                HttpView.throwNotFound();
            return FormPage.getView(CompensationController.class, form, "delete.jsp");
        }

        public boolean handlePost(ViewForm viewForm, BindException errors) throws Exception
        {
            FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getActionURL(), getRequest());
            if (null == comp)
            {
                HttpView.throwNotFound();
                return false;
            }

            boolean hasErrors = false;
            if (comp.getRun() != null)
            {
                hasErrors = addError(errors, "This matrix cannot be deleted because belongs to a run.");
            }
            ExpRun[] runs = comp.getExpObject().getTargetRuns();
            if (runs.length != 0)
            {
                hasErrors = addError(errors, "This matrix cannot be deleted because it has been used in " + runs.length + " analysis runs.  Those runs must be deleted first.");
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
            return urlFor(Action.begin);
        }

        public ActionURL getSuccessURL(ViewForm viewForm)
        {
            return urlFor(Action.begin);
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
            _flowUserSchema = (UserSchema) DefaultSchema.get(getUser(), getContainer()).getSchema(FlowSchema.SCHEMANAME.toString());
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

