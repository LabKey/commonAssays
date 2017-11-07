/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.ms1.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.query.FieldKey;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Used to filter for a given set of peptide sequences
 *
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 10:17:45 AM
 */
public class PeptideFilter extends SimpleFilter.FilterClause implements FeaturesFilter
{
    private String[] _sequences;
    private boolean _exact = false;
    private final String _sequenceColumnName;
    private final String _exactSequenceColumnName;

    public PeptideFilter(String sequenceList, boolean exact, String sequenceColumnName, String exactSequenceColumnName)
    {
        _sequences = sequenceList.split(",");
        _exact = exact;
        _sequenceColumnName = sequenceColumnName;
        _exactSequenceColumnName = exactSequenceColumnName;
    }

    public PeptideFilter(String sequenceList, boolean exact)
    {
        this(sequenceList, exact, "TrimmedPeptide", "Peptide");
    }

    private String normalizeSequence(String sequence)
    {
        //strip off the bits outside the first and last .
        //and remove all non-alpha characters
        char[] trimmed = new char[sequence.length()];
        int len = 0;
        char ch;

        for(int idx = Math.max(0, sequence.indexOf('.') + 1);
            idx < sequence.length() && sequence.charAt(idx) != '.';
            ++idx)
        {
            ch = sequence.charAt(idx);
            if((ch >= 'A' && ch <= 'Z') || '?' == ch || '*' == ch) //allow wildcards
            {
                //translate user wildcards to SQL
                if('?' == ch)
                    ch = '_';
                if('*' == ch)
                    ch = '%';
                
                trimmed[len] = ch;
                ++len;
            }
        }
        
        return new String(trimmed, 0, len);
    }

    public List<FieldKey> getFieldKeys()
    {
        if(_exact)
            return Arrays.asList(FieldKey.fromParts("TrimmedPeptide"), FieldKey.fromParts("Peptide"));
        else
            return Arrays.asList(FieldKey.fromParts("TrimmedPeptide"));
    }

    public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
    {
        if(null == _sequences)
            return null;

        // OR together the sequence conditions
        StringBuilder sql = new StringBuilder();
        for(int idx = 0; idx < _sequences.length; ++idx)
        {
            if(idx > 0)
                sql.append(" OR ");

            sql.append(genSeqPredicate(_sequences[idx], null));
        }
        return new SQLFragment(sql.toString());
    }

    @Override
    public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
    {
        throw new UnsupportedOperationException();
    }

    public SQLFragment getWhereClause(Map<String, String> aliasMap, SqlDialect dialect)
    {
        if (null == _sequences)
            return null;

        String pepDataAlias = aliasMap.get("ms2.PeptidesData");
        assert(null != pepDataAlias);

        // OR together the sequence conditions
        StringBuilder sql = new StringBuilder();
        for (int idx = 0; idx < _sequences.length; ++idx)
        {
            if (idx > 0)
                sql.append(" OR ");

            sql.append(genSeqPredicate(_sequences[idx], pepDataAlias));
        }
        return new SQLFragment(sql.toString());
    }

    private String genSeqPredicate(String sequence, String pepDataAlias)
    {
        //force sequence to upper-case for case-sensitive DBs like PostgreSQL
        sequence = sequence.toUpperCase();

        //always add a condition for pd.TrimmedPeptide using normalized version of sequence
        StringBuilder sql = new StringBuilder(null == pepDataAlias ? "(" + _sequenceColumnName
                : "(" + pepDataAlias + "." + _sequenceColumnName);

        if (_exact)
        {
            sql.append("='");
            sql.append(normalizeSequence(sequence));
            sql.append("'");
        }
        else
        {
            sql.append(" LIKE '");
            sql.append(normalizeSequence(sequence));
            sql.append("%'");
        }

        //if _exact, AND another contains condition against pd.Peptide
        if(_exact && _exactSequenceColumnName != null)
        {
            sql.append(null == pepDataAlias ? " AND " + _exactSequenceColumnName + " LIKE '%" : " AND " + pepDataAlias + "." + _exactSequenceColumnName + " LIKE '%");
            sql.append(sequence.replace("'", "''").trim()); //FIX: 6679
            sql.append("%'");
        }

        sql.append(")");

        return sql.toString();
    }
}
