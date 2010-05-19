/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.ms2.protein;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.ExprColumn;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
public enum CustomAnnotationType
{
    IPI("IPI", IdentifierType.IPI)
    {
        protected String getIdentifierSelectSQL()
        {
            return ProteinManager.getSchema().getSqlDialect().getSubstringFunction("Identifier", "0", "12");
        }

        public String validateUserLookupString(String lookupString)
        {
            if (!lookupString.startsWith("IPI"))
            {
                return "All IPI identifiers must start with 'IPI'.";
            }
            if (lookupString.indexOf(".") != -1)
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

    public String getFirstSelectForSeqId()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("(SELECT MIN(Identifier) FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers());
        sql.append(" i, ");
        sql.append(ProteinManager.getTableInfoIdentTypes());
        sql.append(" it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
        sql.append(_type.toString());
        sql.append("' AND i.SeqId = ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".SeqId)");
        return sql.toString();
    }

    private final String _description;
    private final IdentifierType _type;

    CustomAnnotationType(String description, IdentifierType type)
    {
        _description = description;
        _type = type;
    }

    public String getLookupStringSelect(ColumnInfo colSeqId)
    {
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(getIdentifierSelectSQL());
        sb.append(" FROM ");
        sb.append(ProteinManager.getTableInfoIdentifiers());
        sb.append(" WHERE SeqId = ");
        sb.append(colSeqId.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        sb.append(" AND IdentTypeId IN ");
        sb.append(getIdentTypeIdSelect());
        return sb.toString();
    }

    public String getSeqIdSelect()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT SeqId, ");
        sb.append(getIdentifierSelectSQL());
        sb.append(" AS Ident FROM ");
        sb.append(ProteinManager.getTableInfoIdentifiers());
        sb.append(" WHERE IdentTypeId IN ");
        sb.append(getIdentTypeIdSelect());
        return sb.toString();
    }

    protected String getIdentTypeIdSelect()
    {
        return "(SELECT IdentTypeId FROM " + ProteinManager.getTableInfoIdentTypes() + " WHERE Name = '" + _type.toString() + "')";
    }

    public String getDescription()
    {
        return _description;
    }
}
