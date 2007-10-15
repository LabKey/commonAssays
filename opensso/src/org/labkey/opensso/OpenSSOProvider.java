package org.labkey.opensso;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.apache.log4j.Logger;
import org.labkey.api.security.AuthenticationProvider;
import org.labkey.api.security.ValidEmail;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

/**
 * User: adam
 * Date: Oct 10, 2007
 * Time: 7:01:10 PM
 */
public class OpenSSOProvider implements AuthenticationProvider.RequestAuthenticationProvider
{
    private static final Logger _log = Logger.getLogger(OpenSSOProvider.class);

    public OpenSSOProvider()
    {
        addProps("AMConfig.properties");

        // TODO: Do we really need all these?
        addProps("amAuth.properties");
        addProps("amAuthContext.properties");
        addProps("amIdRepo.properties");
        addProps("amNaming.properties");
        addProps("amProfile.properties");
        addProps("amSecurity.properties");
        addProps("amSession.properties");
        addProps("amSSOProvider.properties");
        addProps("amUtilMsgs.properties");
        addProps("clientDefault.properties");
    }

    private void addProps(String filename)
    {
        try
        {
            Properties props = new Properties();
            props.load(OpenSSOManager.class.getResourceAsStream(filename));
            SystemProperties.initializeProperties(props);
        }
        catch (IOException e)
        {
            _log.error("Exception initializing OpenSSO properties", e);
        }
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
            // Ignore this -- opensso token is either expired or doesn't exist
        }

        return null;
    }
}
