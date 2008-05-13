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

public class Script
{
    int _rowId;
    int _objectId;
    String _text;
    public void setRowId(int rowid)
    {
        _rowId = rowid;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setObjectId(int objectId)
    {
        _objectId = objectId;
    }

    public int getObjectId()
    {
        return _objectId;
    }

    public String getText()
    {
        return _text;
    }

    public void setText(String text)
    {
        _text = text;
    }
}
