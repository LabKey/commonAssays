package org.labkey.ms2;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.annotations.Migrate;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.protein.CustomAnnotationSet;
import org.labkey.api.protein.CustomAnnotationSetManager;
import org.labkey.api.security.User;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Collections;

public class MS2ContainerListener implements ContainerManager.ContainerListener
{
    @Override
    public void containerCreated(Container c, User user)
    {
    }

    @Migrate
    @Override
    public void containerDeleted(Container c, User user)
    {
        MS2Manager.markAsDeleted(c, user);
        MS2Manager.deleteExpressionData(c);

        // TODO: Migrate to ProteinContainerListener
        for (CustomAnnotationSet set : CustomAnnotationSetManager.getCustomAnnotationSets(c, false).values())
        {
            CustomAnnotationSetManager.deleteCustomAnnotationSet(set);
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
