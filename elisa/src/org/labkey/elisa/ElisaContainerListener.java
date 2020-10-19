package org.labkey.elisa;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.security.User;
import org.labkey.elisa.query.ElisaManager;

public class ElisaContainerListener extends ContainerManager.AbstractContainerListener
{
    @Override
    public void containerDeleted(Container c, User user)
    {
        ElisaManager.deleteContainerData(c);
    }
}
