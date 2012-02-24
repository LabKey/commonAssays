package org.labkey.microarray;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.Table;
import org.labkey.api.security.User;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 2/23/12
 * Time: 4:42 PM
 */
public class MicroarrayContainerListener implements ContainerManager.ContainerListener
{
    public void containerCreated(Container c, User user)
    {
    }

    public void containerDeleted(Container c, User user)
    {
        try
        {
            DbSchema ms = MicroarraySchema.getSchema();
            Table.execute(ms, "DELETE FROM " + ms.getTable(MicroarraySchema.TABLE_GEO_PROPS).getSelectName() + " WHERE container = ?", c.getId());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    @Override
    public void containerMoved(Container c, Container oldParent, User user)
    {
    }
}
