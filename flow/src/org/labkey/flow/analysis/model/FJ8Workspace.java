package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.util.Set;

public class FJ8Workspace extends MacWorkspace
{
    public FJ8Workspace(Element elDoc, Set<StatisticSet> statisticSets) throws Exception
    {
        super(elDoc, statisticSets);
    }

    protected void readSamples(Element elDoc)
    {
        for (Element elSampleList : getElementsByTagName(elDoc, "SampleList"))
        {
            for (Element elSample : getElementsByTagName(elSampleList, "Sample"))
            {
                readSample(elSample);
            }
        }
    }

    protected SampleInfo readSample(Element elSample)
    {
        SampleInfo ret = new SampleInfo();
        ret._sampleId = elSample.getAttribute("sampleID");
        for (Element elFCSHeader : getElementsByTagName(elSample, "Keywords"))
        {
            for (Element elKeyword : getElementsByTagName(elFCSHeader, "Keyword"))
            {
                ret._keywords.put(elKeyword.getAttribute("name"), elKeyword.getAttribute("value"));
            }
            for (Element elParameter : getElementsByTagName(elSample, "Parameter"))
            {
                String name = elParameter.getAttribute("name");
                if (_parameters.containsKey(name))
                    continue;
                ParameterInfo pi = new ParameterInfo();
                pi.name = name;
                pi.multiplier = findMultiplier(elParameter);
                _parameters.put(name, pi);
            }
        }
        _sampleInfos.put(ret._sampleId, ret);
        return ret;
    }

    protected void readSampleAnalyses(Element elDoc)
    {
        for (Element elSampleList : getElementsByTagName(elDoc, "SampleList"))
        {
            for (Element elSample : getElementsByTagName(elSampleList, "Sample"))
            {
                for (Element elSampleNode : getElementsByTagName(elSample, "SampleNode"))
                {
                    readSampleAnalysis(elSampleNode);
                }
            }
        }
    }

    protected void readGroupAnalyses(Element elDoc)
    {
        for (Element elGroups : getElementsByTagName(elDoc, "Groups"))
        {
            for (Element elGroupNode : getElementsByTagName(elGroups, "GroupNode"))
            {
                readGroupAnalysis(elGroupNode);
            }
        }
    }
}
