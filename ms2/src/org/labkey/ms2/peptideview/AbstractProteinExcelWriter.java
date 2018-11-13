/*
 * Copyright (c) 2006-2018 LabKey Corporation
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

package org.labkey.ms2.peptideview;

import org.apache.poi.ss.usermodel.Sheet;
import org.labkey.api.data.ExcelColumn;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.Results;
import org.labkey.api.util.ResultSetUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public abstract class AbstractProteinExcelWriter extends ExcelWriter
{
    protected boolean _expanded = false;
    protected ExcelWriter _nestedExcelWriter = null;
    protected GroupedResultSet _groupedRS = null;

    @Override
    public void renderGrid(RenderContext ctx, Sheet sheet, List<ExcelColumn> columns, Results rs) throws SQLException, MaxRowsExceededException
    {
        super.renderGrid(ctx, sheet, columns, rs);
        ResultSetUtil.close(_groupedRS);
    }

    public void setExpanded(boolean expanded)
    {
        _expanded = expanded;
    }

    public void setExcelWriter(ExcelWriter nestedExcelWriter)
    {
        _nestedExcelWriter = nestedExcelWriter;
    }

    public void setGroupedResultSet(GroupedResultSet groupedRS)
    {
        _groupedRS = groupedRS;
    }

    @Override
    public void close()
    {
        super.close();
        ResultSetUtil.close(_groupedRS);
    }
}
