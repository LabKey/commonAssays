package org.labkey.luminex;

import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ActionURL;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

/**
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class LuminexController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(LuminexController.class,
            LuminexUploadWizardAction.class
        );

    public LuminexController()
    {
        super();
        setActionResolver(_resolver);
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
            throw new UnsupportedOperationException();
        }
    }
}
