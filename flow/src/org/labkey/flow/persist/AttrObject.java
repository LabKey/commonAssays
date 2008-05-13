/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.persist;

import org.labkey.api.data.Container;

public class AttrObject
{
    Container _container;
    int _rowId;
    int _dataId;
    int _typeId;
    String _uri;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowid)
    {
        _rowId = rowid;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public int getTypeId()
    {
        return _typeId;
    }

    public void setTypeId(int typeId)
    {
        _typeId = typeId;
    }

    public String getUri()
    {
        return _uri;
    }

    public void setUri(String uri)
    {
        _uri = uri;
    }

    public void setContainer(Container c)
    {
        _container = c;
    }

    public Container getContainer()
    {
        return _container;
    }
}
