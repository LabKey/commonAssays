/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.luminex.model;

import org.labkey.luminex.model.Titration;

import java.io.Serializable;

/**
 * User: jeckels
 * Date: 8/23/13
 */
public abstract class AbstractLuminexControl implements Serializable
{
    private int _rowId;
    private int _runId;
    private String _name;

    public AbstractLuminexControl() {}

    public AbstractLuminexControl(Titration titration)
    {
        setRunId(titration.getRunId());
        setName(titration.getName());
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        name = name == null || name.trim().isEmpty() ? "Standard" : name;

        _name = name;
    }


}
