package org.labkey.ms1.maintenance;

import org.apache.log4j.Logger;
import org.labkey.api.util.SystemMaintenance;
import org.labkey.ms1.MS1Manager;

import java.sql.SQLException;

/**
 * System maintenance tasks for purging deleted data files
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Nov 1, 2007
 * Time: 10:15:38 AM
 */
public class PurgeTask implements SystemMaintenance.MaintenanceTask
{
    public void run()
    {
        try
        {
            long msStart = System.currentTimeMillis();
            _log.info("MS1 Purge Task starting cycle...");

            MS1Manager mgr = MS1Manager.get();

            Integer fileId = mgr.getNextPurgeFile();
            while(null != fileId)
            {
                _log.info("Purging MS1 file id " + fileId.intValue() + "...");
                mgr.purgeFile(fileId.intValue());
                _log.info("MS1 file id " + fileId.intValue() + " successfully purged.");
                
                fileId = mgr.getNextPurgeFile();
            }

            _log.info("MS1 Purge Task finished cycle in " + String.valueOf((System.currentTimeMillis() - msStart) / 1000) + " seconds.");
        }
        catch(SQLException e)
        {
            _log.warn("Unable to purge MS1 data due to the following SQLException: " + MS1Manager.get().getAllErrors(e));
        }
    }

    public String getMaintenanceTaskName()
    {
        return "MS1 Data File Purge Task";
    }

    private Logger _log = Logger.getLogger(PurgeTask.class);
}
