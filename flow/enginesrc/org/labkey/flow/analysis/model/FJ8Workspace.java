/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

import org.w3c.dom.Element;

public class FJ8Workspace extends MacWorkspace
{
    public FJ8Workspace(String name, String path, Element elDoc) throws Exception
    {
       super(name, path, elDoc);
    }

    protected void readSamples(Element elDoc)
    {
        for (Element elSampleList : getElementsByTagName(elDoc, "SampleList"))
        {
            for (Element elSample : getElementsByTagName(elSampleList, "Sample"))
            {
                readSample(elSample);

                for (Element elSampleNode : getElementsByTagName(elSample, "SampleNode"))
                {
                    readSampleAnalysis(elSampleNode);
                }
            }
        }
    }

    protected SampleInfo readSample(Element elSample)
    {
        SampleInfo ret = new SampleInfo();
        ret._sampleName = elSample.getAttribute("name");
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

    protected void readGroups(Element elDoc)
    {
        for (Element elGroups : getElementsByTagName(elDoc, "Groups"))
        {
            for (Element elGroupNode : getElementsByTagName(elGroups, "GroupNode"))
            {
                PopulationName groupName = PopulationName.fromString(elGroupNode.getAttribute("name"));

                for (Element elGroup : getElementsByTagName(elGroupNode, "Group"))
                {
                    GroupInfo groupInfo = readGroup(elGroup);
                    groupInfo.setGroupName(groupName);
                }

                readGroupAnalysis(elGroupNode);
            }
        }
    }

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
    
    protected void readSampleAnalyses(Element elDoc)
    {
        assert false : "readSampleAnalysis() now called form readSamples()";
    }

    protected void readGroupAnalyses(Element elDoc)
    {
        assert false : "readGroupAnalysis() now called form readGroups()";
    }
}
