/*
 * Copyright (c) 2008-2010 LabKey Corporation
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
package org.labkey.authentication.ldap;

import org.apache.commons.lang.StringUtils;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.security.LoginUrls;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.User;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.template.PageConfig;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

public class LdapController extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(LdapController.class);

    public LdapController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }


    public static ActionURL getConfigureURL(boolean reshow)
    {
        ActionURL url = new ActionURL(ConfigureAction.class, ContainerManager.getRoot());

        if (reshow)
            url.addParameter("reshow", "1");

        return url;
    }


    @RequiresSiteAdmin
    public class ConfigureAction extends FormViewAction<Config>
    {
        public ModelAndView getView(Config form, boolean reshow, BindException errors) throws Exception
        {
            form.setSASL(LdapAuthenticationManager.useSASL());
            return new JspView<Config>("/org/labkey/authentication/ldap/configure.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            PageFlowUtil.urlProvider(LoginUrls.class).appendAuthenticationNavTrail(root).addChild("Configure LDAP Authentication");
            return root;
        }

        public void validateCommand(Config target, Errors errors)
        {
        }

        public boolean handlePost(Config config, BindException errors) throws Exception
        {
            LdapAuthenticationManager.saveProperties(config);
            return true;
        }

        public ActionURL getSuccessURL(Config config)
        {
            return getConfigureURL(true);  // Redirect to same action -- want to reload props from database
        }
    }


    public static class Config extends ReturnUrlForm
    {
        public String helpLink = "<a href=\"" + (new HelpTopic("configLdap", HelpTopic.Area.SERVER)).getHelpTopicLink() + "\" target=\"labkey\">More information about LDAP authentication</a>";
        public boolean reshow = false;

        private String servers = StringUtils.join(LdapAuthenticationManager.getServers(), ";");
        private String domain = LdapAuthenticationManager.getDomain();
        private String principalTemplate = LdapAuthenticationManager.getPrincipalTemplate();
        private boolean useSASL = false;   // Always initialize to false because of checkbox behavior

        public String getServers()
        {
            return servers;
        }

        public void setServers(String servers)
        {
            this.servers = servers;
        }

        public String getDomain()
        {
            return domain;
        }

        public void setDomain(String domain)
        {
            this.domain = domain;
        }

        public String getPrincipalTemplate()
        {
            return principalTemplate;
        }

        public void setPrincipalTemplate(String principalTemplate)
        {
            this.principalTemplate = principalTemplate;
        }

        public boolean getSASL()
        {
            return useSASL;
        }

        public void setSASL(boolean useSASL)
        {
            this.useSASL = useSASL;
        }

        public boolean isReshow()
        {
            return reshow;
        }

        public void setReshow(boolean reshow)
        {
            this.reshow = reshow;
        }
    }


    @RequiresSiteAdmin
    public class TestLdapAction extends FormViewAction<TestLdapForm>
    {
        public void validateCommand(TestLdapForm target, Errors errors)
        {
        }

        public ModelAndView getView(TestLdapForm form, boolean reshow, BindException errors) throws Exception
        {
            if (!reshow)
                form.setSASL(LdapAuthenticationManager.useSASL());

            HttpView view = new JspView<TestLdapForm>("/org/labkey/authentication/ldap/testLdap.jsp", form, errors);
            PageConfig page = getPageConfig();
            if (null == form.getMessage() || form.getMessage().length() < 200)
                page.setFocusId("server");
            page.setTemplate(PageConfig.Template.Dialog);
            return view;
        }

        public boolean handlePost(TestLdapForm form, BindException errors) throws Exception
        {
            try
            {
                boolean success = LdapAuthenticationManager.connect(form.getServer(), form.getPrincipal(), form.getPassword(), form.getSASL());
                form.setMessage("<b>Connected to server.  Authentication " + (success ? "succeeded" : "failed") + ".</b>");
            }
            catch(Exception e)
            {
                String message = "<b>Failed to connect with these settings.  Error was:</b><br>" + ExceptionUtil.renderException(e);
                form.setMessage(message);
            }
            return false;
        }

        public ActionURL getSuccessURL(TestLdapForm testLdapAction)
        {
            return null;   // Always reshow form
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    public static class TestLdapForm extends ReturnUrlForm
    {
        private String server = LdapAuthenticationManager.getServers()[0];
        private String principal;
        private String password;
        private String message;
        private boolean useSASL = false;  // Always initialize to false because of checkbox behavior

        public TestLdapForm()
        {
            User user = HttpView.currentContext().getUser();
            ValidEmail email;

            try
            {
                email = new ValidEmail(user.getEmail());
            }
            catch(ValidEmail.InvalidEmailException e)
            {
                throw new RuntimeException(e);
            }

            principal = LdapAuthenticationManager.emailToLdapPrincipal(email);

            if ("null".equals(principal))
                principal = null;
        }

        public String getPrincipal()
        {
            return (null == principal ? "" : principal);
        }

        public void setPrincipal(String principal)
        {
            this.principal = principal;
        }

        public String getPassword()
        {
            return (null == password ? "" : password);
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String getServer()
        {
            return (null == server ? "" : server);
        }

        public void setServer(String server)
        {
            this.server = server;
        }

        public boolean getSASL()
        {
            return useSASL;
        }

        public void setSASL(boolean useSASL)
        {
            this.useSASL = useSASL;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }
    }    
}