package org.labkey.flow.persist;

import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;

public class FlowContainerListener implements ContainerManager.ContainerListener
{
    static final private Logger _log = Logger.getLogger(FlowContainerListener.class);

    public void containerCreated(Container c)
    {
    }

    /**
     * Delete all Flow data from the container.
     * This code should not really be necessary since all flow data should get deleted when the associated Experiment Data object is deleted.
     * However, sometimes the FlowModule might not get notified when the data is deleted.  The Experiment Module uses the filename of the Data
     * to determine who should be notified when the data is deleted.  If something has gone wrong (i.e. a bad build was used at some point
     * to add data) we still want the user to be able to delete the corrupted container.
     *
     * For this reason, the FlowContainerListener should be registered before the ExperimentContainerListener.
     */
    public void containerDeleted(Container c, User user)
    {
        try
        {
            FlowManager.get().deleteContainer(c);
        }
        catch (SQLException sqlE)
        {
            _log.error("Error deleting container", sqlE);
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }
}
