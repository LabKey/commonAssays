package org.labkey.ms2.protein;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.ExprColumn;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
public enum CustomAnnotationType
{
    IPI("IPI")
    {
        public String getLookupStringSelect(ColumnInfo colSeqId)
        {
            StringBuilder sb = new StringBuilder("SELECT ");
            sb.append(ProteinManager.getSchema().getSqlDialect().getSubstringFunction("Identifier", "0", "12"));
            sb.append(" FROM ");
            sb.append(ProteinManager.getTableInfoIdentifiers());
            sb.append(" WHERE SeqId = ");
            sb.append(colSeqId.getValueSql());
            sb.append(" AND IdentTypeId IN ");
            sb.append(getIdentTypeIdSelect("IPI"));
            return sb.toString();
        }

        public String getSeqIdSelect()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT SeqId, ");
            sb.append(ProteinManager.getSchema().getSqlDialect().getSubstringFunction("Identifier", "0", "12"));
            sb.append(" AS ident FROM ");
            sb.append(ProteinManager.getTableInfoIdentifiers());
            sb.append(" WHERE IdentTypeId IN ");
            sb.append(getIdentTypeIdSelect("IPI"));
            return sb.toString();
        }

        public String getFirstSelectForSeqId()
        {
            return getFirstSelectForSeqId("IPI");
        }
    },
    GENE_NAME("Gene Name")
    {
        public String getLookupStringSelect(ColumnInfo colSeqId)
        {
            StringBuilder sb = new StringBuilder("SELECT Identifier FROM ");
            sb.append(ProteinManager.getTableInfoIdentifiers());
            sb.append(" WHERE SeqId = ");
            sb.append(colSeqId.getValueSql());
            sb.append(" AND IdentTypeId IN ");
            sb.append(getIdentTypeIdSelect("GeneName"));
            return sb.toString();
        }

        public String getSeqIdSelect()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT SeqId, Identifier AS Ident FROM ");
            sb.append(ProteinManager.getTableInfoIdentifiers());
            sb.append(" WHERE IdentTypeId IN ");
            sb.append(getIdentTypeIdSelect("GeneName"));
            return sb.toString();
        }

        public String getFirstSelectForSeqId()
        {
            return getFirstSelectForSeqId("GeneName");
        }
    },
    SWISS_PROT("Swiss Prot")
    {
        public String getFirstSelectForSeqId()
        {
            return getFirstSelectForSeqId("SwissProt");
        }

        public String getLookupStringSelect(ColumnInfo colSeqId)
        {
            StringBuilder sb = new StringBuilder("SELECT Identifier FROM ");
            sb.append(ProteinManager.getTableInfoIdentifiers());
            sb.append(" WHERE SeqId = ");
            sb.append(colSeqId.getValueSql());
            sb.append(" AND IdentTypeId IN ");
            sb.append(getIdentTypeIdSelect("SwissProt"));
            return sb.toString();
        }

        public String getSeqIdSelect()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT SeqId, Identifier AS Ident FROM ");
            sb.append(ProteinManager.getTableInfoIdentifiers());
            sb.append(" WHERE IdentTypeId IN ");
            sb.append(getIdentTypeIdSelect("SwissProt"));
            return sb.toString();
        }
    };

    protected String getFirstSelectForSeqId(String typeName)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("(SELECT MIN(Identifier) FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers());
        sql.append(" i, ");
        sql.append(ProteinManager.getTableInfoIdentTypes());
        sql.append(" it WHERE i.IdentTypeId = it.IdentTypeId AND it.Name = '");
        sql.append(typeName);
        sql.append("' AND i.SeqId = ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".SeqId)");
        return sql.toString();
    }

    private String _description;

    CustomAnnotationType(String description)
    {
        _description = description;
    }

    public abstract String getLookupStringSelect(ColumnInfo colSeqId);

    public abstract String getSeqIdSelect();
    public abstract String getFirstSelectForSeqId();

    protected String getIdentTypeIdSelect(String identTypeName)
    {
        return "(SELECT IdentTypeId FROM " + ProteinManager.getTableInfoIdentTypes() + " WHERE Name = '" + identTypeName + "')";
    }

    public String getDescription()
    {
        return _description;
    }
}
