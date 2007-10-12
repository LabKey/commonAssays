package org.labkey.ms1;

/**
 * Represents the values needed to show a particular MS2 peptide.
 * The MS1Manager dispenses these objects given an MS1 FeatureID.
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 11, 2007
 * Time: 11:59:28 AM
 */
public class FeaturePeptideLink
{
    public FeaturePeptideLink() {}
    public FeaturePeptideLink(long ms2Run, int peptideId, int scan)
    {
        _ms2Run = ms2Run;
        _peptideId = peptideId;
        _scan = scan;
    }

    public long getMs2Run()
    {
        return _ms2Run;
    }

    public void setMs2Run(long ms2Run)
    {
        _ms2Run = ms2Run;
    }

    public int getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(int peptideId)
    {
        _peptideId = peptideId;
    }

    public int getScan()
    {
        return _scan;
    }

    public void setScan(int scan)
    {
        _scan = scan;
    }

    private long _ms2Run;
    private int _peptideId;
    private int _scan;
}
