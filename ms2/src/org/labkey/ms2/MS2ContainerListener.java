package org.labkey.ms2;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.security.User;

public class MS2ContainerListener implements ContainerManager.ContainerListener
{
    @Override
    public void containerDeleted(Container c, User user)
    {
        MS2Manager.markAsDeleted(c, user);
        MS2Manager.deleteExpressionData(c);
    }
}
