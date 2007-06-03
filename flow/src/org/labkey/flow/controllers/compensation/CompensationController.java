package org.labkey.flow.controllers.compensation;

import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.struts.action.ActionError;
import org.apache.log4j.Logger;
import org.labkey.api.view.ViewForm;
import org.labkey.api.view.ViewForward;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowCompensationMatrix;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.analysis.model.CompensationMatrix;

@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class CompensationController extends BaseFlowController<CompensationController.Action>
{
    static private final Logger _log = Logger.getLogger(CompensationController.class);
    public enum Action
    {
        begin,
        showCompensation,
        upload,
        delete
    }

    @Jpf.Action
    protected Forward begin(ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        return renderInTemplate(FormPage.getView(CompensationController.class, form, "begin.jsp"), null, "Uploaded Compensation Matrices", Action.begin);
    }

    @Jpf.Action
    protected Forward upload(UploadCompensationForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            Forward fwd = doUpload(form);
            if (fwd != null)
            {
                return fwd;
            }
        }
        return renderInTemplate(FormPage.getView(CompensationController.class, form, "upload.jsp"), null, "Upload a new compensation matrix", Action.upload);
    }

    protected Forward doUpload(UploadCompensationForm form) throws Exception
    {
        boolean errors = false;
        ExperimentService.Interface svc = ExperimentService.get();
        if (form.ff_compensationMatrixFile == null)
        {
            errors = addError("No file was uploaded");
        }
        if (form.ff_compensationMatrixName == null)
        {
            errors = addError("You must give the compensation matrix a name.");
        }
        if (errors)
            return null;
        String lsid = svc.generateLSID(getContainer(), FlowDataType.CompensationMatrix, form.ff_compensationMatrixName);
        if (svc.getExpData(lsid) != null)
        {
            errors = addError("The name '" + form.ff_compensationMatrixName + "' is already being used.");
            return null;
        }
        CompensationMatrix comp;
        try
        {
            comp = new CompensationMatrix(form.ff_compensationMatrixFile.getInputStream());
        }
        catch (Exception e)
        {
            errors = addError("Error parsing file:" + e);
            return null;
        }
        boolean fTrans = false;
        FlowCompensationMatrix flowComp;
        try
        {
            AttributeSet attrs = new AttributeSet(comp);
            attrs.prepareForSave();
            if (!svc.isTransactionActive())
            {
                svc.beginTransaction();
                fTrans = true;
            }
            flowComp = FlowCompensationMatrix.create(getUser(), getContainer(), form.ff_compensationMatrixName, attrs);
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
        return new ViewForward(flowComp.urlShow());
    }

    protected boolean addError(String error)
    {
        PageFlowUtil.getActionErrors(getRequest(), true).add("main", new ActionError("Error", error));
        return true;
    }

    @Jpf.Action
    protected Forward showCompensation(ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getViewURLHelper(), getRequest());
        return renderInTemplate(FormPage.getView(CompensationController.class, form, "showCompensation.jsp"), comp, "Show Compensation " + comp.getName(), Action.showCompensation);
    }

    @Jpf.Action
    protected Forward delete(ViewForm form) throws Exception
    {
        requiresPermission(ACL.PERM_DELETE);
        FlowCompensationMatrix comp = FlowCompensationMatrix.fromURL(getViewURLHelper(), getRequest());
        boolean errors = false;
        if (isPost())
        {
            Forward fwd = doDelete(comp);
            if (fwd != null)
                return fwd;
        }
        return renderInTemplate(FormPage.getView(CompensationController.class, form, "delete.jsp"), comp, "Delete Compensation " + comp.getName(), Action.delete);
    }

    protected Forward doDelete(FlowCompensationMatrix comp)
    {
        boolean errors = false;
        if (comp.getRun() != null)
        {
            errors = addError("This matrix cannot be deleted because belongs to a run.");
        }
        ExpRun[] runs = comp.getExpObject().getTargetRuns();
        if (runs.length != 0)
        {
            errors = addError("This matrix cannot be deleted because it has been used in " + runs.length + " analysis runs.  Those runs must be deleted first.");
        }
        if (errors)
        {
            return null;
        }
        try
        {
            comp.getExpObject().delete(getUser());
        }
        catch (Exception e)
        {
            errors = addError("An exception occurred deleting the matrix: " + e);
        }
        if (errors)
            return null;
        return new ViewForward(urlFor(Action.begin));
    }
}

