package org.labkey.microarray;

import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

public class MicroarrayController extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(MicroarrayController.class);

    public MicroarrayController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    public static ViewURLHelper getRunsURL(Container c)
    {
        return new ViewURLHelper(ShowRunsAction.class, c);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowRunsAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return ExperimentService.get().createExperimentRunWebPart(getViewContext(), MicroarrayModule.EXP_RUN_FILTER, true);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new HtmlView("Test");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }
}