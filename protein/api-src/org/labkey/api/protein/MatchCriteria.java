package org.labkey.api.protein;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.SQLFragment;

import java.util.List;
import java.util.Map;

public enum MatchCriteria
{
    EXACT("Exact")
            {
                @Override
                public void appendMatchClause(SQLFragment sqlFragment, String param)
                {
                    sqlFragment.append(" = ?");
                    sqlFragment.add(param);
                }
            },
    PREFIX("Prefix")
            {
                @Override
                public void appendMatchClause(SQLFragment sqlFragment, String param)
                {
                    sqlFragment.append(" LIKE ?");
                    sqlFragment.add(ProteinSchema.getSqlDialect().encodeLikeOpSearchString(param) + "%");
                }
            },
    SUFFIX("Suffix")
            {
                @Override
                public void appendMatchClause(SQLFragment sqlFragment, String param)
                {
                    sqlFragment.append(" LIKE ?");
                    sqlFragment.add("%" + ProteinSchema.getSqlDialect().encodeLikeOpSearchString(param));
                }
            },
    SUBSTRING("Substring")
            {
                @Override
                public void appendMatchClause(SQLFragment sqlFragment, String param)
                {
                    sqlFragment.append(" LIKE ?");
                    sqlFragment.add("%" + ProteinSchema.getSqlDialect().encodeLikeOpSearchString(param) + "%");
                }
            };


    private String label;

    private static final Map<String, MatchCriteria> _criteriaMap = new CaseInsensitiveHashMap<>();

    MatchCriteria(String label)
    {
        this.label = label;
    }

    static
    {
        for (MatchCriteria criteria : MatchCriteria.values())
        {
            if (criteria != null)
                _criteriaMap.put(criteria.getLabel(), criteria);
        }
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    @Nullable
    public static MatchCriteria getMatchCriteria(String label)
    {
        if (label == null)
            return null;
        return _criteriaMap.get(label);
    }

    public void appendMatchClause(SQLFragment sqlFragment, String param)
    {
    }

    /**
     * Build up a SQLFragment that filters identifiers based on a set of possible values. Passing in an empty
     * list will result in no matches
     */
    public SQLFragment getIdentifierClause(List<String> params, String columnName)
    {
        SQLFragment sqlFragment = new SQLFragment();
        String separator = "";
        sqlFragment.append("(");
        if (params.isEmpty())
        {
            sqlFragment.append("1 = 2");
        }
        for (String param : params)
        {
            sqlFragment.append(separator);
            sqlFragment.append(columnName);
            appendMatchClause(sqlFragment, param);
            separator = " OR ";
        }
        sqlFragment.append(")");
        return sqlFragment;
    }
}
