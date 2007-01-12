package org.labkey.ms2;

import org.labkey.api.ms2.MS2Service;
import org.labkey.api.ms2.MS2Manager;
import org.labkey.api.ms2.pipeline.MascotClientImpl;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Jan 9, 2007
 */
public class MS2ServiceImpl implements MS2Service.Service
{
    public String getRunsTableName()
    {
        return MS2Manager.getTableInfoRuns().toString();
    }

    public MascotClientImpl createMascotClient(String url, Logger instanceLogger, String userAccount, String userPassword)
    {
        return new MascotClientImpl(url, instanceLogger, userAccount, userPassword);
    }
}
