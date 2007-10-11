package org.labkey.opensso;

import com.iplanet.sso.SSOToken;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class OpenSSOController extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(OpenSSOController.class);

    public OpenSSOController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class BeginAction extends GetTokenAction
    {
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class GetTokenAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/opensso/view/getToken.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermission(ACL.PERM_ADMIN)
    public class ShowTokenAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            String tokenId = getViewContext().getViewURLHelper().getParameter("token").trim();
            SSOToken token = OpenSSOManager.get().getSSOToken(tokenId);

            if (OpenSSOManager.get().isValid(token))
                return new JspView<SSOToken>("/org/labkey/opensso/view/showToken.jsp", token);
            else
                return new HtmlView(tokenId + " is not a valid token");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }
}