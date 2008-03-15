package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

public class FJ8Workspace extends MacWorkspace
{
    public FJ8Workspace(Element elDoc) throws Exception
    {
        super(elDoc);
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
        if (elSample.hasAttribute("compensationID"))
        {
            ret._compensationId = elSample.getAttribute("compensationID");
        }
        for (Element elFCSHeader : getElementsByTagName(elSample, "Keywords"))
        {
            readKeywords(ret, elFCSHeader);
        }

        readParameterInfo(elSample);
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
