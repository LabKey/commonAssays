package org.labkey.ms2;

/**
 * User: jeckels
 * Date: Jun 20, 2007
 */
public enum MS2RunType
{
    Comet(CometRun.class, "RawScore, DiffScore, ZScore"),
    Mascot(MascotRun.class, "Ion, Identity, Homology, Expect"),
    Phenyx(PhenyxRun.class, "OrigScore, Bogus, ZScore"),
    Sequest(SequestRun.class, "SpRank, SpScore, DeltaCn, XCorr"),
    XComet(XCometRun.class, "RawScore, DiffScore, Expect"),
    XTandem(XTandemRun.class, "Hyper, Next, B, Y, Expect"),
    XTandemcomet(XTandemcometRun.class, "RawScore, DiffScore, Expect");

    private final Class<? extends MS2Run> _runClass;
    private final String _scoreColumnNames;

    private MS2RunType(Class<? extends MS2Run> runClass, String scoreColumnNames)
    {
        _runClass = runClass;
        _scoreColumnNames = scoreColumnNames;
    }

    public Class<? extends MS2Run> getRunClass()
    {
        return _runClass;
    }

    public String getScoreColumnNames()
    {
        return _scoreColumnNames;
    }
}
