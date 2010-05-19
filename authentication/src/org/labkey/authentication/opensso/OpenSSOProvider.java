/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.authentication.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.apache.log4j.Logger;
import org.labkey.api.security.AuthenticationManager;
import org.labkey.api.security.AuthenticationProvider;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.RedirectException;

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
    public static final String NAME = "OpenSSO";

    public boolean isPermanent()
    {
        return false;
    }

    public void activate() throws Exception
    {
        OpenSSOManager.get().activate();
    }

    public void deactivate() throws Exception
    {
    }

    public String getName()
    {
        return NAME;
    }

    public String getDescription()
    {
        return "Connects to OpenSSO, an open-source authentication server, enabling single sign-on solutions with one or more other web sites";
    }

    public ActionURL getConfigurationLink()
    {
        return OpenSSOController.getCurrentSettingsURL();
    }

    @Override
    public AuthenticationResponse authenticate(HttpServletRequest request, HttpServletResponse response, URLHelper returnURL) throws ValidEmail.InvalidEmailException, RedirectException
    {
        try
        {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken token = manager.createSSOToken(request);

            if (SSOTokenManager.getInstance().isValidToken(token))
            {
                String principalName = token.getPrincipal().getName();
                int i = principalName.indexOf(',');
                String email = principalName.substring(3, i);
                return AuthenticationResponse.createSuccessResponse(new ValidEmail(email));
            }

            // TODO: Need special failure response for this case?
        }
        catch (SSOException e)
        {
            _log.debug("Invalid, expired, or missing OpenSSO token", e);
        }

        String referrerPrefix = OpenSSOManager.get().getReferrerPrefix();

        if (null != referrerPrefix)
        {
            // Note to developers: this is difficult to test/debug because (in my experience) "referer" is null when linking
            // to http://localhost.  Use an actual domain name to test this code (e.g., http://dhcp155191.fhcrc.org).
            String referer = request.getHeader("Referer");

            if (null != referer && referer.startsWith(referrerPrefix))
            {
                AuthenticationManager.LinkFactory factory = AuthenticationManager.getLinkFactory(NAME);

                if (null != factory && null != returnURL)
                {
                    String url = factory.getURL(returnURL);
                    throw new RedirectException(url);
                }
            }
        }

        return AuthenticationResponse.createFailureResponse(FailureReason.notApplicable);     // Rely on login screen to present link to OpenSSO
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
