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
    },
    GENE_NAME("Gene Name", IdentifierType.GeneName),
    SWISS_PROT("Swiss Prot", IdentifierType.SwissProt),
    SWISS_PROT_ACCN("Swiss Prot Accession", IdentifierType.SwissProtAccn);

    protected String getIdentifierSelectSQL()
    {
        return "Identifier";
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
        sb.append(colSeqId.getValueSql());
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
