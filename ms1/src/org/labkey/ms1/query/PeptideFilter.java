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

import org.labkey.ms1.MS1Manager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.FieldKey;

import java.util.ArrayList;

/**
 * Used to filter for a given set of peptide sequences
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 10:17:45 AM
 */
public class PeptideFilter implements FeaturesFilter
{
    public static final String NAME = "pepSeq";
    public static final String NAME_EXACT = "exact";

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

    public void setFilters(FeaturesTableInfo tinfo)
    {
        //add a condition that selects only features that match the given peptide sequence
        StringBuilder sql = new StringBuilder("FeatureId IN (SELECT fe.FeatureId" +
                " FROM ms2.PeptidesData AS pd" +
                " INNER JOIN ms2.Fractions AS fr ON (fr.fraction=pd.fraction)" +
                " INNER JOIN ms2.Runs AS r ON (fr.Run=r.Run)" +
                " INNER JOIN exp.Data AS d ON (r.Container=d.Container)" +
                " INNER JOIN ms1.Files AS fi ON (fi.MzXmlUrl=fr.MzXmlUrl AND fi.ExpDataFileId=d.RowId)" +
                " INNER JOIN ms1.Features AS fe ON (fe.FileId=fi.FileId AND pd.scan=fe.MS2Scan)" +
                " WHERE ");

        if(_exact || MS1Manager.get().containsModifiers(_sequences))
        {
            sql.append("pd.Peptide IN (");

            for(int idx = 0; idx < _sequences.length; ++idx)
            {
                if(idx > 0)
                    sql.append(",");

                sql.append("'");
                sql.append(_sequences[idx]);
                sql.append("'");
            }

            sql.append(")");
        }
        else
        {
            //need to append multiple LIKE clauses against TrimmedPeptide
            for(int idx = 0; idx < _sequences.length; ++idx)
            {
                if(idx > 0)
                    sql.append(" OR ");

                sql.append("pd.TrimmedPeptide LIKE '");
                sql.append(_sequences[idx]);
                sql.append("%'");
            }
        }

        sql.append(")"); //end paren for sub-select

        SQLFragment sqlf = new SQLFragment(sql.toString());
        tinfo.addCondition(sqlf, "FeatureId");

        //change the default visible columnset
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(tinfo.getDefaultVisibleColumns());
        visibleColumns.add(2, FieldKey.fromParts("FileId","ExpDataFileId","Run","Name"));
        visibleColumns.add(2, FieldKey.fromParts("FileId","ExpDataFileId","Run","Name"));
        visibleColumns.remove(FieldKey.fromParts("AccurateMz"));
        visibleColumns.remove(FieldKey.fromParts("Mass"));
        visibleColumns.remove(FieldKey.fromParts("Charge"));
        visibleColumns.remove(FieldKey.fromParts("Peaks"));
        visibleColumns.remove(FieldKey.fromParts("TotalIntensity"));
        tinfo.setDefaultVisibleColumns(visibleColumns);
    }

}
