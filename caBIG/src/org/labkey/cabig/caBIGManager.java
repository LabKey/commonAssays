package org.labkey.cabig;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Table;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;

public class caBIGManager implements ContainerManager.ContainerListener // TODO: Get rid of this
{
    private static caBIGManager _instance;
    private static Logger _log = Logger.getLogger(caBIGManager.class);

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

    public void publish(Container c) throws SQLException
    {
        Table.execute(caBIGSchema.getInstance().getSchema(), "INSERT INTO cabig.temp_containers (EntityId) VALUES (?)", new Object[]{c.getId()});
    }

    public void unpublish(Container c) throws SQLException
    {
        Table.execute(caBIGSchema.getInstance().getSchema(), "DELETE FROM cabig.temp_containers WHERE EntityId = ?", new Object[]{c.getId()});
    }


    public boolean isPublished(Container c) throws SQLException
    {
        Long rows = Table.executeSingleton(caBIGSchema.getInstance().getSchema(), "SELECT COUNT(*) FROM " + caBIGSchema.getInstance().getTableInfoContainers() + " WHERE EntityId = ?", new Object[]{c.getId()}, Long.class);

        return (1 == rows);
    }


    public void containerCreated(Container c)
    {
    }


    public void containerDeleted(Container c)
    {
        try
        {
            unpublish(c);
        } catch (SQLException e)
        {
            _log.error(e);
        }
    }


    public void propertyChange(PropertyChangeEvent evt)
    {
    }
}