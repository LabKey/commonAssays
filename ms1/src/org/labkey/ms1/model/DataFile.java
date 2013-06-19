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

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;

/**
 * Represents a row in the ms1.Files table
 * User: Dave
 * Date: Nov 2, 2007
 * Time: 9:52:10 AM
 */
public class DataFile
{
    private int _fileId;
    private int _expDataFileId;
    private short _type;
    private String _mzXmlUrl;
    private boolean _imported;
    private boolean _deleted;

    public int getFileId()
    {
        return _fileId;
    }

    public void setFileId(int fileId)
    {
        _fileId = fileId;
    }

    public int getExpDataFileId()
    {
        return _expDataFileId;
    }

    public void setExpDataFileId(int expDataFileId)
    {
        _expDataFileId = expDataFileId;
    }

    public short getType()
    {
        return _type;
    }

    public void setType(short type)
    {
        _type = type;
    }

    public String getMzXmlUrl()
    {
        return _mzXmlUrl;
    }

    public void setMzXmlUrl(String mzXmlUrl)
    {
        _mzXmlUrl = mzXmlUrl;
    }

    public boolean isImported()
    {
        return _imported;
    }

    public void setImported(boolean imported)
    {
        _imported = imported;
    }

    public boolean isDeleted()
    {
        return _deleted;
    }

    public void setDeleted(boolean deleted)
    {
        _deleted = deleted;
    }

    public ExpData getExpData()
    {
        return ExperimentService.get().getExpData(getExpDataFileId());
    }
}
