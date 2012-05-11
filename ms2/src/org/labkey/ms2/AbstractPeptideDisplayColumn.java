/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.ms2;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;

/**
 * User: jeckels
 * Date: Apr 30, 2012
 */
public class AbstractPeptideDisplayColumn extends SimpleDisplayColumn
{
    /** Look for a value based first on a ColumnInfo, and then falling back on alternative aliases it might have in the ResultSet */
    protected Object getColumnValue(RenderContext ctx, ColumnInfo colInfo, String... alternates)
    {
        Object result = null;
        if (colInfo != null)
        {
            result = ctx.get(colInfo.getAlias());
        }
        if (result == null)
        {
            for (String alternate : alternates)
            {
                result = ctx.get(alternate);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return result;
    }

}
