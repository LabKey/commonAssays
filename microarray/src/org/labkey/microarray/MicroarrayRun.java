/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.data.Container;

/**
 * User: jeckels
 * Date: Jan 4, 2008
 */
public class MicroarrayRun
{
    private int _rowId;
    private String _status;
    private int _dataId;
    private Container _container;
    private int _protocolId;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public String getStatus()
    {
        return _status;
    }

    public void setStatus(String status)
    {
        _status = status;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public int getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(int protocolId)
    {
        _protocolId = protocolId;
    }
}
