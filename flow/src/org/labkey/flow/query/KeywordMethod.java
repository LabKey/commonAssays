/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.flow.query;

import org.labkey.api.data.*;
import org.labkey.api.query.AbstractMethodInfo;
import org.labkey.api.query.snapshot.AbstractTableMethodInfo;

import java.sql.Types;

public class KeywordMethod extends AbstractTableMethodInfo
{
    ColumnInfo _objectIdColumn;
    public KeywordMethod(ColumnInfo objectIdColumn)
    {
        super(JdbcType.VARCHAR);
        _objectIdColumn = objectIdColumn;
    }

    public SQLFragment getSQL(String tableAlias, DbSchema schema, SQLFragment[] arguments)
    {
        if (arguments.length != 1)
        {
            throw new IllegalArgumentException("The keyword method requires 1 argument");
        }
        SQLFragment ret = new SQLFragment("(SELECT flow.keyword.value FROM flow.keyword" +
                "\nINNER JOIN flow.attribute ON flow.keyword.keywordid = flow.attribute.rowid AND flow.attribute.name = ");
        ret.append(arguments[0]);
        ret.append("\nWHERE flow.keyword.objectId = " + tableAlias + ".Keyword");
//        ret.append(_objectIdColumn.getValueSql(tableAlias));
        ret.append(")");
        return ret;
    }
}
