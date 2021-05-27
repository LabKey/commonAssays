/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.analysis.model;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

/**
 * Mac FlowJo <= v9.6.x series uses 'version 2.0' file format.
 */
public class Mac2Workspace extends MacWorkspace
{
    public Mac2Workspace(String name, String path, Element elDoc)
    {
       super(name, path, elDoc);
    }

    @Override
    protected void readSamples(Element elDoc)
    {
        String samplesTagName = "SampleList";
        readSamples(elDoc, samplesTagName);
   }

    protected void readSamples(Element elDoc, String samplesTagName)
    {
        for (Element elSamples : getElementsByTagName(elDoc, samplesTagName))
        {
            for (Element elSample : getElementsByTagName(elSamples, "Sample"))
            {
                SampleInfo sampleInfo = readSample(elSample);

                Element elSampleNode = getElementByTagName(elSample, "SampleNode");
                readSampleAnalysis(elSampleNode);
            }
        }
    }

    @Override
    protected SampleInfo readSample(Element elSample)
    {
        String id = elSample.getAttribute("sampleID");

        // read the sample name from the <Sample> element (legacy location for sample name)
        String name = readNameAttribute(elSample);
        if (StringUtils.isBlank(name))
        {
            // read sample name from the child sample node element: <SampleNode nodeName='name'>
            Element elSampleNode = getElementByTagName(elSample, "SampleNode");
            if (elSampleNode != null)
            {
                name = readNameAttribute(elSampleNode);
            }
        }

        boolean deleted = readSampleDeletedFlag(elSample);

        SampleInfo ret = new SampleInfo(id, name, deleted);
        if (elSample.hasAttribute("compensationID"))
        {
            ret._compensationId = elSample.getAttribute("compensationID");
        }
        for (Element elFCSHeader : getElementsByTagName(elSample, "Keywords"))
        {
            readKeywords(ret, elFCSHeader);
        }

        readParameterInfo(elSample);
        addSample(ret);
        return ret;
    }

    @Override
    protected void readGroups(Element elDoc)
    {
        for (Element elGroups : getElementsByTagName(elDoc, "Groups"))
        {
            for (Element elGroupNode : getElementsByTagName(elGroups, "GroupNode"))
            {
                String nameAttr = readNameAttribute(elGroupNode);
                PopulationName groupName = PopulationName.fromString(nameAttr);

                for (Element elGroup : getElementsByTagName(elGroupNode, "Group"))
                {
                    GroupInfo groupInfo = readGroup(elGroup);
                    groupInfo.setGroupName(groupName);
                }

                readGroupAnalysis(elGroupNode);
            }
        }
    }

    @Override
    protected GroupInfo readGroup(Element elGroup)
    {
        GroupInfo ret = new GroupInfo();
        ret._groupId = elGroup.getAttribute("groupID");

        for (Element elSampleList : getElementsByTagName(elGroup, "SampleRefs"))
        {
            for (Element elSample : getElementsByTagName(elSampleList, "SampleRef"))
            {
                String sampleID = elSample.getAttribute("sampleID");
                if (sampleID != null)
                    ret._sampleIds.add(sampleID);
            }
        }

        _groupInfos.put(ret._groupId, ret);
        return ret;
    }
    
    @Override
    protected void readSampleAnalyses(Element elDoc)
    {
        assert false : "readSampleAnalysis() now called form readSamples()";
    }

    @Override
    protected void readGroupAnalyses(Element elDoc)
    {
        assert false : "readGroupAnalysis() now called form readGroups()";
    }

}
