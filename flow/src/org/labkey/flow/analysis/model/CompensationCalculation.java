package org.labkey.flow.analysis.model;

import java.util.*;

import org.labkey.flow.analysis.web.SubsetSpec;

/**
 */
public class CompensationCalculation extends ScriptComponent
{
    List<ChannelInfo> _lstChannelInfo = new ArrayList();

    public static class ChannelInfo
    {
        String _name;
        ChannelSubset _subsetPositive;
        ChannelSubset _subsetNegative;

        public ChannelInfo(String name, ChannelSubset positive, ChannelSubset negative)
        {
            _name = name;
            _subsetPositive = positive;
            _subsetNegative = negative;
        }

        public String getName()
        {
            return _name;
        }

        public ChannelSubset getPositive()
        {
            return _subsetPositive;
        }

        public ChannelSubset getNegative()
        {
            return _subsetNegative;
        }
    }

    public static class ChannelSubset
    {
        SampleCriteria _criteria;
        SubsetSpec _subset;

        public ChannelSubset(SampleCriteria criteria, SubsetSpec subset)
        {
            _criteria = criteria;
            _subset = subset;
        }

        public SampleCriteria getCriteria()
        {
            return _criteria;
        }

        public SubsetSpec getSubset()
        {
            return _subset;
        }
    }

    public void addChannel(String name, ChannelSubset positive, ChannelSubset negative)
    {
        _lstChannelInfo.add(new ChannelInfo(name, positive, negative));
    }

    public List<ChannelInfo> getChannels()
    {
        return _lstChannelInfo;
    }

    public int getChannelCount()
    {
        return _lstChannelInfo.size();
    }

    public ChannelInfo getChannelInfo(int i)
    {
        return _lstChannelInfo.get(i);
    }

    public String getChannelName(int i)
    {
        return getChannelInfo(i).getName();
    }
}
