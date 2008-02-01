package org.labkey.ms2.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public class CompareResult implements IsSerializable
{
    private GWTExperimentRun[] _runs;
    private GWTRunGroup[] _runGroups;
    private boolean[][] _hits;

    public CompareResult()
    {
    }

    public CompareResult(GWTExperimentRun[] runs, GWTRunGroup[] runGroups, boolean[][] hits)
    {
        _runs = runs;
        _runGroups = runGroups;
        _hits = hits;
    }

    public boolean[][] getHits()
    {
        return _hits;
    }

    public GWTExperimentRun[] getRuns()
    {
        return _runs;
    }

    public GWTRunGroup[] getRunGroups()
    {
        return _runGroups;
    }
}
