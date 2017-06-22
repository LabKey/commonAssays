/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
        String samplesTagName = "Samples";
        readSamples(elDoc, samplesTagName);
    }

    // FlowJo v9.7 uses 'nodeName' attribute on Sample, GroupNode, Population, and Statistic elements
    @Override
    protected String readNameAttribute(Element elNamed)
    {
        return elNamed.getAttribute("nodeName");
    }
}
