package org.labkey.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.apache.log4j.Logger;
import org.labkey.api.security.AuthenticationProvider;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.view.ViewURLHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: adam
 * Date: Oct 10, 2007
 * Time: 7:01:10 PM
 */
public class OpenSSOProvider implements AuthenticationProvider.RequestAuthenticationProvider
{
    private static final Logger _log = Logger.getLogger(OpenSSOProvider.class);

    public OpenSSOProvider() throws Exception
    {
        OpenSSOManager.get().initialize();
    }

    public String getName()
    {
        return "OpenSSO";
    }

    public ViewURLHelper getConfigurationLink(ViewURLHelper returnUrl)
    {
        return OpenSSOController.getCurrentSettingsUrl(returnUrl);
    }

    public ValidEmail authenticate(HttpServletRequest request, HttpServletResponse response) throws ValidEmail.InvalidEmailException
    {
        // TODO: If no valid opensso cookie look for opensso param and automatically redirect
        // ... if not, return null, and rely on login screen to present link to opensso

        try
        {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken token = manager.createSSOToken(request);
            if (SSOTokenManager.getInstance().isValidToken(token))
            {
                String principalName = token.getPrincipal().getName();
                int i = principalName.indexOf(',');
                String email = principalName.substring(3, i);
                return new ValidEmail(email);
            }
        }
        catch (SSOException e)
        {
            SSOException sso = e;
            // Ignore this -- opensso token is either expired or doesn't exist
        }

        return null;
    }

    public void logout(HttpServletRequest request)
    {
        try
        {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken token = manager.createSSOToken(request);
            manager.destroyToken(token);
        }
        catch (SSOException e)
        {
            // Ignore
        }
    }
}
