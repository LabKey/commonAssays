package org.labkey.ms2;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.security.User;
import org.labkey.api.protein.CustomAnnotationSet;
import org.labkey.ms2.protein.ProteinManager;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Collections;

public class MS2ContainerListener implements ContainerManager.ContainerListener
{
    @Override
    public void containerCreated(Container c, User user)
    {
    }

    @Override
    public void containerDeleted(Container c, User user)
    {
        MS2Manager.markAsDeleted(c, user);
        MS2Manager.deleteExpressionData(c);

        for (CustomAnnotationSet set : ProteinManager.getCustomAnnotationSets(c, false).values())
        {
            ProteinManager.deleteCustomAnnotationSet(set);
        }
    }

    @Override
    public void containerMoved(Container c, Container oldParent, User user)
    {
    }

    @NotNull
    @Override
    public Collection<String> canMove(Container c, Container newParent, User user)
    {
        return Collections.emptyList();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
    }
}
