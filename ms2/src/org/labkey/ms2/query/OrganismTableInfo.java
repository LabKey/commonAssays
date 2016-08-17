/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

package org.labkey.ms2.query;

import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.UserSchema;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.query.ProteinUserSchema;

/**
 * User: jeckels
 * Date: Feb 22, 2007
 */
public class OrganismTableInfo extends FilteredTable<UserSchema>
{
    public OrganismTableInfo(UserSchema schema)
    {
        super(ProteinManager.getTableInfoOrganisms(), schema);
        setPublicSchemaName(ProteinUserSchema.NAME);

        wrapAllColumns(true);

        SQLFragment sql = new SQLFragment();
        sql.append("CASE WHEN ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".CommonName IS NULL THEN ");
        sql.append(getSqlDialect().concatenate(ExprColumn.STR_TABLE_ALIAS + ".Genus", "' '", ExprColumn.STR_TABLE_ALIAS + ".Species"));
        sql.append(" ELSE ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".CommonName END");
        ExprColumn descriptionColumn = new ExprColumn(this, "Description", sql, JdbcType.VARCHAR);
        addColumn(descriptionColumn);
        setTitleColumn("Description");

        removeColumn(getColumn("IdentId"));
    }
}
