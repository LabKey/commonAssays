/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.ms1.model;

/**
 * Represents a parameter setting for a software package used to create an MS1 data file
 *
 * User: Dave
 * Date: Oct 10, 2007
 * Time: 2:31:27 PM
 */
public class SoftwareParam
{

    public int getSoftwareId()
    {
        return SoftwareId;
    }

    public void setSoftwareId(int softwareId)
    {
        SoftwareId = softwareId;
    }

    public String getName()
    {
        return Name;
    }

    public void setName(String name)
    {
        Name = name;
    }

    public String getValue()
    {
        return Value;
    }

    public void setValue(String value)
    {
        Value = value;
    }

    protected int SoftwareId = -1;
    protected String Name;
    protected String Value;
}
