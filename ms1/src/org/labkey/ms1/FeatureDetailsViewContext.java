package org.labkey.ms1;

import java.sql.SQLException;

/**
 * Used as a context object for the FeatureDetailsView
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 17, 2007
 * Time: 3:03:23 PM
 */
public class FeatureDetailsViewContext
{
    public FeatureDetailsViewContext(Feature feature, int prevFeatureId, int nextFeatureId)
    {
        _feature = feature;
        _prevFeatureId = prevFeatureId;
        _nextFeatureId = nextFeatureId;
    }

    public Feature getFeature()
    {
        return _feature;
    }

    public int getPrevFeatureId()
    {
        return _prevFeatureId;
    }

    public int getNextFeatureId()
    {
        return _nextFeatureId;
    }

    public Integer[] getPrevNextScans(int scan, double mzLow, double mzHigh) throws SQLException
    {
        if(null == _feature)
            return new Integer[0];

        Integer runId = _feature.getRunId();
        if(null == runId)
            return new Integer[0];

        if(_feature.getScanFirst() == null || _feature.getScanLast() == null)
            return new Integer[0];
        
        return MS1Manager.get().getPrevNextScan(_feature.getRunId().intValue(), mzLow, mzHigh,
                                                _feature.getScanFirst().intValue(),
                                                _feature.getScanLast().intValue(), scan);
    }

    private Feature _feature;
    private int _prevFeatureId = -1;
    private int _nextFeatureId = -1;
}
