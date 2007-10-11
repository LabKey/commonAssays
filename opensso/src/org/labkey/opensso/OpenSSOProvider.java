package org.labkey.opensso;

import org.labkey.api.security.AuthenticationProvider;
import org.labkey.api.security.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: adam
 * Date: Oct 10, 2007
 * Time: 7:01:10 PM
 */
public class OpenSSOProvider implements AuthenticationProvider
{
    public User authenticate(HttpServletRequest request, HttpServletResponse response)
    {
        // Look for opensso cookie... if not, look for opensso param... if not, return null

        return null;
    }
}
