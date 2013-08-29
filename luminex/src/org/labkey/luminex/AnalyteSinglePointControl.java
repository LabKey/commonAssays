/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.luminex;

/**
 * User: jeckels
 * Date: 8/23/13
 */
public class AnalyteSinglePointControl extends AbstractLuminexControlAnalyte
{
    private int _rowId;
    private int _singlePointControlId;

    public AnalyteSinglePointControl() {}

    public AnalyteSinglePointControl(Analyte analyte, SinglePointControl control)
    {
        setAnalyteId(analyte.getRowId());
        _singlePointControlId = control.getRowId();
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getSinglePointControlId()
    {
        return _singlePointControlId;
    }

    public void setSinglePointControlId(int singlePointControlId)
    {
        _singlePointControlId = singlePointControlId;
    }
}
