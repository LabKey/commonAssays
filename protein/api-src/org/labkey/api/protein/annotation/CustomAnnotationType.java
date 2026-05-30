/*
 * Copyright (c) 2007-2026 LabKey Corporation
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

package org.labkey.api.protein.annotation;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.util.SafeToRenderEnum;

public enum CustomAnnotationType implements SafeToRenderEnum
{
    IPI("IPI", IdentifierType.IPI)
    {
        @Override
        protected String getIdentifierSelectSQL()
        {
            return ProteinSchema.getSchema().getSqlDialect().getSubstringFunction("Identifier", "0", "12");
        }

        @Override
        public String validateUserLookupString(String lookupString)
        {
            if (!lookupString.startsWith("IPI"))
            {
                return "All IPI identifiers must start with 'IPI'.";
            }
            if (lookupString.contains("."))
            {
                return "IPI identifiers must not include a version number.";
            }
            return null;
        }
    },
    GENE_NAME("Gene Name", IdentifierType.GeneName),
    SWISS_PROT("Swiss-Prot Name", IdentifierType.SwissProt),
    SWISS_PROT_ACCN("Swiss-Prot Accession", IdentifierType.SwissProtAccn),
    GEN_INFO("GenInfo Identifier", IdentifierType.GI);

    protected String getIdentifierSelectSQL()
    {
        return "Identifier";
    }

    public String validateUserLookupString(String lookupString)
    {
        return null;
    }

    public SQLFragment getFirstSelectForSeqId()
    {
        SQLFragment sql = new SQLFragment();
        sql.append("(SELECT MIN(Identifier) FROM ");
        sql.append(ProteinSchema.getTableInfoIdentifiers());
        sql.append(" i, ");
        sql.append(ProteinSchema.getTableInfoIdentTypes());
        sql.append(" it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = ");
        sql.appendValue(_type);
        sql.append(" AND i.SeqId = ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".SeqId)");
        return sql;
    }

    private final String _description;
    private final IdentifierType _type;

    CustomAnnotationType(String description, IdentifierType type)
    {
        _description = description;
        _type = type;
    }

    public SQLFragment getLookupStringSelect(ColumnInfo colSeqId)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append(getIdentifierSelectSQL());
        sql.append(" FROM ");
        sql.append(ProteinSchema.getTableInfoIdentifiers());
        sql.append(" WHERE SeqId = ");
        sql.append(colSeqId.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        sql.append(" AND IdentTypeId IN ");
        sql.append(getIdentTypeIdSelect());
        return sql;
    }

    public SQLFragment getSeqIdSelect()
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT SeqId, ");
        sql.append(getIdentifierSelectSQL());
        sql.append(" AS Ident FROM ");
        sql.append(ProteinSchema.getTableInfoIdentifiers());
        sql.append(" WHERE IdentTypeId IN ");
        sql.append(getIdentTypeIdSelect());
        return sql;
    }

    protected SQLFragment getIdentTypeIdSelect()
    {
        return new SQLFragment("(SELECT IdentTypeId FROM ").append(ProteinSchema.getTableInfoIdentTypes()).append(" WHERE Name = ").appendValue(_type).append(")");
    }

    public String getDescription()
    {
        return _description;
    }
}
