package org.labkey.flow.data;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.TableKey;
import org.labkey.api.data.ColumnInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FieldSubstitution
{
    static final private Logger _log = Logger.getLogger(FieldSubstitution.class); 
    Object[] _parts;
    static public FieldSubstitution fromString(String s)
    {
        List<Object> parts = new ArrayList();
        int ichCur = 0;
        while (true)
        {
            int ichExprStart = StringUtils.indexOf(s, "${", ichCur);
            if (ichExprStart < 0)
                break;
            int ichExprEnd = StringUtils.indexOf(s, "}", ichExprStart);
            if (ichExprEnd < 0)
                break;
            if (ichExprStart > ichCur)
            {
                parts.add(s.substring(ichCur, ichExprStart));
            }
            parts.add(FieldKey.fromString(s.substring(ichExprStart + 2, ichExprEnd)));
            ichCur = ichExprEnd + 1;
        }
        if (ichCur < s.length())
        {
            parts.add(s.substring(ichCur, s.length()));
        }
        return new FieldSubstitution(parts.toArray());
    }

    public FieldSubstitution(Object[] parts)
    {
        _parts = parts;
    }

    public Object[] getParts()
    {
        return _parts;
    }

    public FieldKey[] getFieldKeys()
    {
        List<FieldKey> ret = new ArrayList();
        for (Object part : _parts)
        {
            if (part instanceof FieldKey)
            {
                ret.add((FieldKey) part);
            }
        }
        return ret.toArray(new FieldKey[0]);
    }

    public String toString()
    {
        StringBuilder ret = new StringBuilder();
        for (Object part : _parts)
        {
            if (part instanceof FieldKey)
            {
                ret.append("${");
                ret.append(part);
                ret.append("}");
            }
            else
            {
                ret.append(part);
            }
        }
        return ret.toString();
    }

    public String eval(Map<FieldKey, ColumnInfo> columns, ResultSet rs)
    {
        StringBuilder ret = new StringBuilder();
        for (Object part : _parts)
        {
            Object value;
            if (part instanceof FieldKey)
            {
                FieldKey field = (FieldKey) part;
                ColumnInfo column = columns.get(field);
                if (column == null)
                {
                    value = null;
                }
                else
                {
                    try
                    {
                        value = column.getValue(rs);
                    }
                    catch (SQLException e)
                    {
                        _log.error("Error", e);
                        value = null;
                    }
                }
            }
            else
            {
                value = part;
            }
            if (value != null)
            {
                ret.append(value);
            }
        }
        return ret.toString();
    }

    /**
     * If this expression is of the form ${field1}<separator>${field2}<separator>${field3}
     * then return [field1, field2, field3].
     * Return null if not of that form.
     */
    public FieldKey[] getFields(String separator)
    {
        if (0 == (_parts.length & 1))
            return null;

        List<FieldKey> ret = new ArrayList();
        for (int i = 0; i < _parts.length; i += 2)
        {
            if (!(_parts[i] instanceof FieldKey))
                return null;
            ret.add((FieldKey) _parts[i]);
            if (i < _parts.length - 1)
            {
                if (!(_parts[i + 1] instanceof String))
                {
                    return null;
                }
                if (!separator.equals(_parts[i + 1]))
                    return null;
            }
        }
        return ret.toArray(new FieldKey[0]);
    }

    public void insertParent(TableKey key)
    {
        Object[] newParts = new Object[_parts.length];
        for (int i = 0; i < _parts.length; i ++)
        {
            Object part = _parts[i];
            if (part instanceof FieldKey)
            {
                List<String> fieldParts = new ArrayList(key.getParts());
                fieldParts.addAll(((FieldKey) part).getParts());
                part = TableKey.fromParts(fieldParts).toFieldKey();
            }
            newParts[i] = part;
        }
        _parts = newParts;
    }
}
