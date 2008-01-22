package org.labkey.elispot;

import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

public class ElispotController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ElispotController.class,
            ElispotUploadWizardAction.class
        );

    public ElispotController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(new ActionURL("assay", "begin.view", getViewContext().getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }
}