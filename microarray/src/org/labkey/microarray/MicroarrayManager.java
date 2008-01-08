package org.labkey.microarray;

import org.labkey.api.data.*;
import org.labkey.api.security.User;

import java.sql.SQLException;

public class MicroarrayManager
{
    private static MicroarrayManager _instance;

    private MicroarrayManager()
    {
        // prevent external construction with a private default constructor
    }

    public static synchronized MicroarrayManager get()
    {
        if (_instance == null)
            _instance = new MicroarrayManager();
        return _instance;
    }

    public TableInfo getTableInfoRun()
    {
        return MicroarraySchema.getSchema().getTable("Run");
    }

    public MicroarrayRun getRun(Container c, int dataId, int protocolId)
    {
        try
        {
            MicroarrayRun[] runs = Table.executeQuery(MicroarraySchema.getSchema(),
                    "SELECT * FROM " + getTableInfoRun() + " WHERE Container = ? AND DataId = ? AND ProtocolId = ?",
                    new Object[] {c.getId(), dataId, protocolId},
                    MicroarrayRun.class);
            assert runs.length <= 1;
            return runs.length == 0 ? null : runs[0];
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public MicroarrayRun getRun(Container c, int id)
    {
        try
        {
            MicroarrayRun[] runs = Table.selectForDisplay(getTableInfoRun(), Table.ALL_COLUMNS, new SimpleFilter("rowId", new Integer(id)).addCondition("container", c.getId()),
                    null, MicroarrayRun.class);
            if (null == runs || runs.length < 1)
                return null;
            return runs[0];
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void saveRun(User user, MicroarrayRun run)
    {
        try
        {
            if (run.getRowId() == 0)
            {
                Table.insert(user, getTableInfoRun(), run);
            }
            else
            {
                Table.update(user, getTableInfoRun(), run, new Integer(run.getRowId()), null);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }
}