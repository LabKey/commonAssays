/*
 * Copyright (c) 2007 LabKey Software Foundation
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
import org.labkey.api.data.SqlDialect;
import org.labkey.api.query.FieldKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Used to filter for a given set of peptide sequences
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 10:17:45 AM
 */
public class PeptideFilter extends SimpleFilter.FilterClause implements FeaturesFilter
{
    private String[] _sequences;
    private boolean _exact = false;

    public PeptideFilter(String sequenceList, boolean exact)
    {
        _sequences = sequenceList.split(",");
        _exact = exact;
    }

    public PeptideFilter(String[] sequences, boolean exact)
    {
        _sequences = sequences;
        _exact = exact;
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

    public List<String> getColumnNames()
    {
        if(_exact)
            return Arrays.asList("TrimmedPeptide", "Peptide");
        else
            return Arrays.asList("TrimmedPeptide");
    }

    public SQLFragment toSQLFragment(Map<String, ? extends ColumnInfo> columnMap, SqlDialect dialect)
    {
        if(null == _sequences)
            return null;

        // OR together the sequence conditions
        StringBuilder sql = new StringBuilder();
        for(int idx = 0; idx < _sequences.length; ++idx)
        {
            if(idx > 0)
                sql.append(" OR ");

            sql.append(genSeqPredicate(_sequences[idx]));
        }
        return new SQLFragment(sql.toString(), (List<Object>)null);
    }

    public void setFilters(FeaturesTableInfo tinfo)
    {
        if(null == _sequences)
            return;

        //add a condition that selects only features that match the given peptide sequence
        StringBuilder sql = new StringBuilder("FeatureId IN (SELECT fe.FeatureId" +
                " FROM ms2.PeptidesData AS pd" +
                " INNER JOIN ms2.Fractions AS fr ON (fr.fraction=pd.fraction)" +
                " INNER JOIN ms2.Runs AS r ON (fr.Run=r.Run)" +
                " INNER JOIN exp.Data AS d ON (r.Container=d.Container)" +
                " INNER JOIN ms1.Files AS fi ON (fi.MzXmlUrl=fr.MzXmlUrl AND fi.ExpDataFileId=d.RowId)" +
                " INNER JOIN ms1.Features AS fe ON (fe.FileId=fi.FileId AND pd.scan=fe.MS2Scan)" +
                " WHERE ");

        // OR together the sequence conditions
        for(int idx = 0; idx < _sequences.length; ++idx)
        {
            if(idx > 0)
                sql.append(" OR ");

            sql.append(genSeqPredicate(_sequences[idx]));
        }

        sql.append(")"); //end paren for sub-select

        SQLFragment sqlf = new SQLFragment(sql.toString());
        tinfo.addCondition(sqlf, "FeatureId");

        //change the default visible columnset
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(tinfo.getDefaultVisibleColumns());
        visibleColumns.add(2, FieldKey.fromParts("FileId","ExpDataFileId","Run","Name"));
        visibleColumns.remove(FieldKey.fromParts("AccurateMz"));
        visibleColumns.remove(FieldKey.fromParts("Mass"));
        visibleColumns.remove(FieldKey.fromParts("Peaks"));
        visibleColumns.remove(FieldKey.fromParts("TotalIntensity"));
        tinfo.setDefaultVisibleColumns(visibleColumns);
    }

    private String genSeqPredicate(String sequence)
    {
        //force sequence to upper-case for case-sensitive DBs like PostgreSQL
        sequence = sequence.toUpperCase();
        
        //always add a condition for pd.TrimmedPeptide using normalized version of sequence
        StringBuilder sql = new StringBuilder("(TrimmedPeptide");

        if(_exact)
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
        if(_exact)
        {
            sql.append(" AND Peptide LIKE '%");
            sql.append(sequence.trim());
            sql.append("%'");
        }

        sql.append(")");

        return sql.toString();
    }

}
