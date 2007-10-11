package org.labkey.opensso;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import java.io.IOException;
import java.util.Properties;

public class OpenSSOManager
{
    private static OpenSSOManager _instance;

    private OpenSSOManager()
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
            e.printStackTrace();
        }

    }

    public static synchronized OpenSSOManager get()
    {
        if (_instance == null)
            _instance = new OpenSSOManager();
        return _instance;
    }

    public SSOToken getSSOToken(String tokenID) throws SSOException
    {
        SSOTokenManager manager = SSOTokenManager.getInstance();
        return manager.createSSOToken(tokenID);
    }

    public boolean isValid(SSOToken token) throws SSOException
    {
        return SSOTokenManager.getInstance().isValidToken(token);
    }
}