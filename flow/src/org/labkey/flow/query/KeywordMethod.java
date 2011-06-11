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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.snapshot.AbstractTableMethodInfo;

public class KeywordMethod extends AbstractTableMethodInfo
{
    Container _container;
    ColumnInfo _objectIdColumn;

    public KeywordMethod(Container c, ColumnInfo objectIdColumn)
    {
        super(JdbcType.VARCHAR);
        _container = c;
        _objectIdColumn = objectIdColumn;
    }

    public SQLFragment getSQL(String tableAlias, DbSchema schema, SQLFragment[] arguments)
    {
        if (arguments.length != 1)
        {
            throw new IllegalArgumentException("The keyword method requires 1 argument");
        }
        SQLFragment ret = new SQLFragment("(SELECT flow.keyword.value FROM flow.keyword");
        ret.append("\nINNER JOIN flow.KeywordAttr ON flow.keyword.keywordid = flow.KeywordAttr.id AND flow.KeywordAttr.name = ");
        ret.append(arguments[0]);
        ret.append("\nWHERE flow.keyword.objectId = " + tableAlias + ".Keyword");
        ret.append(" AND flow.KeywordAttr.container = ?");
        ret.add(_container.getId());
//        ret.append(_objectIdColumn.getValueSql(tableAlias));
        ret.append(")");
        return ret;
    }
}
