package org.labkey.ms2;

import org.labkey.ms2.pipeline.tandem.XCometRun;
import org.labkey.ms2.pipeline.tandem.XTandemcometRun;
import org.labkey.ms2.pipeline.tandem.XTandemRun;
import org.labkey.ms2.pipeline.phenyx.PhenyxRun;
import org.labkey.ms2.pipeline.phenyx.MascotRun;
import org.labkey.ms2.pipeline.comet.CometRun;
import org.labkey.ms2.pipeline.sequest.SequestRun;

import java.util.Arrays;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 20, 2007
 */
public enum MS2RunType
{
    Comet(CometRun.class, "RawScore", "DiffScore", "ZScore"),
    Mascot(MascotRun.class, "Ion", "Identity", "Homology", "Expect"),
    Phenyx(PhenyxRun.class, "OrigScore", "Bogus", "ZScore"),
    Sequest(SequestRun.class, "SpRank", "SpScore", "DeltaCn", "XCorr"),
    XComet(XCometRun.class, "RawScore", "DiffScore", "Expect"),
    XTandem(XTandemRun.class, "Hyper", "Next", "B", "Y", "Expect"),
    XTandemcomet(XTandemcometRun.class, "RawScore", "DiffScore", "Expect");

    private final Class<? extends MS2Run> _runClass;
    private final String _scoreColumnNames;
    private final List<String> _scoreColumnList;

    private MS2RunType(Class<? extends MS2Run> runClass, String... scoreColumnNames)
    {
        _runClass = runClass;
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (String name : scoreColumnNames)
        {
            sb.append(separator);
            separator = ", ";
            sb.append(name);
        }
        _scoreColumnNames = sb.toString();
        _scoreColumnList = Arrays.asList(scoreColumnNames);
    }

    public Class<? extends MS2Run> getRunClass()
    {
        return _runClass;
    }

    public String getScoreColumnNames()
    {
        return _scoreColumnNames;
    }

    public List<String> getScoreColumnList()
    {
        return _scoreColumnList;
    }
}
