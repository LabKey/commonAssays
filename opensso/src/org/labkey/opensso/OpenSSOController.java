package org.labkey.opensso;

import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.ACL;
import org.labkey.api.security.AuthenticationManager;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ActionURL;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

public class OpenSSOController extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(OpenSSOController.class);

    public OpenSSOController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }


    public static ActionURL getUrl(String action, ActionURL returnUrl)
    {
        return new ActionURL("opensso", action, "").addParameter("returnUrl", returnUrl.getLocalURIString());
    }


    public static ActionURL getConfigureUrl(ActionURL returnUrl)
    {
        return getUrl("configure", returnUrl);
    }


    @RequiresPermission(ACL.PERM_ADMIN)
    public class ConfigureAction extends FormViewAction<ConfigProperties>
    {
        public ActionURL getSuccessURL(ConfigProperties form)
        {
            return new ActionURL(form.getReturnUrl());
        }

        public ModelAndView getView(ConfigProperties form, boolean reshow, BindException errors) throws Exception
        {
            form.props = OpenSSOManager.get().getSystemSettings();
            return new JspView<ConfigProperties>("/org/labkey/opensso/view/configure.jsp", form);
        }

        public boolean handlePost(ConfigProperties form, BindException errors) throws Exception
        {
            Map<String, String> props = new HashMap<String, String>(getViewContext().getExtendedProperties());
            props.remove("x");
            props.remove("y");
            props.remove("returnUrl");

            OpenSSOManager.get().writeSystemSettings(props);
            OpenSSOManager.get().initialize();

            return true;
        }

        public void validateCommand(ConfigProperties target, Errors errors)
        {
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }


    public static ActionURL getCurrentSettingsUrl(ActionURL returnUrl)
    {
        return getUrl("currentSettings.view", returnUrl);
    }


    @RequiresPermission(ACL.PERM_ADMIN)
    public class CurrentSettingsAction extends SimpleViewAction<ConfigProperties>
    {
        public ModelAndView getView(ConfigProperties form, BindException errors) throws Exception
        {
            form.props = OpenSSOManager.get().getSystemSettings();
            form.authLogoUrl = getPickAuthLogoUrl(getViewContext().getActionURL());
            return new JspView<ConfigProperties>("/org/labkey/opensso/view/currentSettings.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }


    public static class ConfigProperties
    {
        public String returnUrl;
        public Map<String, String> props;
        public ActionURL authLogoUrl;

        public String getReturnUrl()
        {
            return returnUrl;
        }

        public void setReturnUrl(String returnUrl)
        {
            this.returnUrl = returnUrl;
        }
    }


    public static ActionURL getPickAuthLogoUrl(ActionURL returnUrl)
    {
        ActionURL url = getUrl("pickAuthLogo.view", returnUrl);
        url.addParameter("name", "OpenSSO");   // TODO: Add this as getting in action instead? 
        return url;
    }


    @RequiresPermission(ACL.PERM_ADMIN)
    public class PickAuthLogoAction extends AuthenticationManager.PickAuthLogoAction
    {

    }
}