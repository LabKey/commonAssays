package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

/**
 * User: kevink
 * Date: 11/23/13
 *
 * Mac FlowJo v9.7.x series uses 'version 3.0' file format.
 */
public class Mac3Workspace extends Mac2Workspace
{
    public Mac3Workspace(String name, String path, Element elDoc)
    {
        super(name, path, elDoc);
    }

    @Override
    protected void readSamples(Element elDoc)
    {
        for (Element elSamples : getElementsByTagName(elDoc, "Samples"))
        {
            for (Element elSample : getElementsByTagName(elSamples, "Sample"))
            {
                readSample(elSample);

                for (Element elSampleNode : getElementsByTagName(elSample, "SampleNode"))
                {
                    readSampleAnalysis(elSampleNode);
                }
            }
        }
    }

    // FlowJo v9.7 uses 'nodeName' attribute on Sample, GroupNode, Population, and Statistic elements
    @Override
    protected String readNameAttribute(Element elNamed)
    {
        return elNamed.getAttribute("nodeName");
    }
}
