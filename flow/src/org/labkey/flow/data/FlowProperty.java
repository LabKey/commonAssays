/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

package org.labkey.flow.data;

import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.property.SystemProperty;

abstract public class FlowProperty
{
    static final public String PROPERTY_BASE = "urn:flow.labkey.org/#";
    static public final SystemProperty SampleSetJoin = new SystemProperty(PROPERTY_BASE + "SampleSetJoin", PropertyType.STRING);
    static public final SystemProperty LogText = new SystemProperty(PROPERTY_BASE + "LogText", PropertyType.STRING);
    static public final SystemProperty FCSAnalysisName = new SystemProperty(PROPERTY_BASE + "FCSAnalysisName", PropertyType.STRING);
    static public final SystemProperty FCSAnalysisFilter = new SystemProperty(PROPERTY_BASE + "FCSAnalysisFilter", PropertyType.STRING);
    static public void register()
    {

    }
}
