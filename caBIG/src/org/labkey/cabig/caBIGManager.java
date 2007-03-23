package org.labkey.cabig;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;

import java.beans.PropertyChangeEvent;

public class caBIGManager implements ContainerManager.ContainerListener
{
    private static caBIGManager _instance;

    private caBIGManager()
    {
        // prevent external construction with a private default constructor
    }

    public static synchronized caBIGManager get()
    {
        if (_instance == null)
            _instance = new caBIGManager();
        return _instance;
    }

    public void publishContainer(Container c)  // ContainerId?
    {

    }

    public void unPublishContainer(Container c) // ContainerId?
    {

    }


    public boolean isPublished(Container c)
    {
        return true;
    }


    public void containerCreated(Container c)
    {
    }


    public void containerDeleted(Container c)
    {
        unPublishContainer(c);
    }


    public void propertyChange(PropertyChangeEvent evt)
    {
    }
}