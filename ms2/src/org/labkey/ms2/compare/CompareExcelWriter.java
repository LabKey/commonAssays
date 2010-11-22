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

package org.labkey.ms2.compare;

import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.ExcelColumn;
import org.labkey.api.data.DisplayColumn;

import java.util.List;
import java.sql.ResultSet;

import jxl.write.*;
import org.labkey.api.data.Results;

/**
 * User: adam
 * Date: Jul 12, 2006
 * Time: 5:53:56 PM
 */
public class CompareExcelWriter extends ExcelWriter
{
    private List<String> _multiColumnCaptions;
    private int _offset = 0;
    private int _colSpan;

    public CompareExcelWriter(Results rs, List<DisplayColumn> displayColumns)
    {
        super(rs, displayColumns);
    }

    public void setMultiColumnCaptions(List<String> multiColumnCaptions)
    {
        _multiColumnCaptions = multiColumnCaptions;
    }

    public void setColSpan(int colSpan)
    {
        _colSpan = colSpan;
    }

    public void setOffset(int offset)
    {
        _offset = offset;
    }

    @Override
    public void renderColumnCaptions(WritableSheet sheet, List<ExcelColumn> visibleColumns) throws WriteException, MaxRowsExceededException
    {
        int column = _offset;

        for (String caption : _multiColumnCaptions)
        {
            sheet.addCell(new Label(column, getCurrentRow(), caption, getBoldFormat()));
            column += _colSpan;
        }

        incrementRow();

        super.renderColumnCaptions(sheet, visibleColumns);
    }
}


