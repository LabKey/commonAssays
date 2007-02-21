package org.labkey.ms2;

import org.labkey.api.ms2.MS2Service;
import org.labkey.api.ms2.SearchClient;
import org.labkey.ms2.pipeline.MascotClientImpl;
import org.labkey.ms2.pipeline.SequestClientImpl;
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

    public SearchClient createSearchClient(String server, String url, Logger instanceLogger, String userAccount, String userPassword)
    {
        if(server.equalsIgnoreCase("mascot"))
            return new MascotClientImpl(url, instanceLogger, userAccount, userPassword);
        if(server.equalsIgnoreCase("sequest"))
            return new SequestClientImpl(url, instanceLogger);
        return null;
    }
}
