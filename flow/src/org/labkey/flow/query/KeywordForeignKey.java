package org.labkey.flow.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;

import java.util.Collection;

public class KeywordForeignKey extends AttributeForeignKey<String>
{
    FlowPropertySet _fps;
    public KeywordForeignKey(FlowPropertySet fps)
    {
        super();
        _fps = fps;
    }

    protected String attributeFromString(String field)
    {
        return field;
    }

    protected Collection<String> getAttributes()
    {
        return _fps.getKeywordProperties().keySet();
    }

    protected void initColumn(String attrName, ColumnInfo column)
    {
        column.setSqlTypeName("VARCHAR");
        column.setCaption(attrName);
        if (isHidden(attrName))
        {
            column.setIsHidden(true);
        }
    }

    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, String attrName, int attrId)
    {
        // SQL server 2000 does not allow a TEXT column (i.e. flow.keyword.value) to appear in this subquery.
        // For this reason, we cast it to VARCHAR(4000).
        SQLFragment ret = new SQLFragment("(SELECT CAST(flow.Keyword.Value AS VARCHAR(4000)) FROM flow.Keyword WHERE flow.Keyword.ObjectId = ");
        ret.append(objectIdColumn.getValueSql());
        ret.append(" AND flow.Keyword.KeywordId = ");
        ret.append(attrId);
        ret.append(")");
        return ret;
    }

    static public boolean isHidden(String keyword)
    {
        if (keyword.startsWith("$"))
            return true;
        if (keyword.startsWith("P") && keyword.endsWith("DISPLAY"))
            return true;
        return false;
    }
}
