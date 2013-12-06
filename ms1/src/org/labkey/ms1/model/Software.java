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

import org.labkey.ms1.MS1Manager;

/**
 * Represents information about a software package used to produce an MS1 data file
 *
 * User: Dave
 * Date: Oct 10, 2007
 * Time: 1:53:24 PM
 */
public class Software
{
    public SoftwareParam[] getParameters()
    {
        if(_softwareId < 0)
            return new SoftwareParam[0];
        else
            return MS1Manager.get().getSoftwareParams(_softwareId);
    }

    public int getSoftwareId()
    {
        return _softwareId;
    }

    public void setSoftwareId(int softwareId)
    {
        _softwareId = softwareId;
    }

    public int getFileId()
    {
        return _fileId;
    }

    public void setFileId(int fileId)
    {
        _fileId = fileId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getVersion()
    {
        return _version;
    }

    public void setVersion(String version)
    {
        _version = version;
    }

    public String getAuthor()
    {
        return _author;
    }

    public void setAuthor(String author)
    {
        _author = author;
    }

    protected int _softwareId = -1;
    protected int _fileId = -1;
    protected String _name;
    protected String _version;
    protected String _author;
}
