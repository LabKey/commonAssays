/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

package org.labkey.ms2.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 21, 2007
 */
public class ComparePeptideTableInfo extends VirtualTable
{
    private final MS2Schema _schema;
    private final List<MS2Run> _runs;
    private final HttpServletRequest _request;
    private final String _peptideViewName;

    public ComparePeptideTableInfo(MS2Schema schema, List<MS2Run> runs, boolean forExport, HttpServletRequest request, String peptideViewName)
    {
        super(MS2Manager.getSchema(), "PeptideComparision");

        _schema = schema;
        _runs = runs;
        _request = request;
        _peptideViewName = peptideViewName;

        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts("PeptideSequence"));

        List<ColumnInfo> runColumns = new ArrayList<>();

        if (runs != null)
        {
            for (MS2Run run : runs)
            {
                SQLFragment sql = new SQLFragment();
                sql.append("Run");
                sql.append(run.getRun());
                sql.append("PeptideId");
                ExprColumn peptideIdColumn = new ExprColumn(this, "Run" + run.getRun(), sql, JdbcType.INTEGER);
                peptideIdColumn.setLabel(run.getDescription());
                peptideIdColumn.setIsUnselectable(true);
                runColumns.add(peptideIdColumn);
                LookupForeignKey fk = new LookupForeignKey("RowId")
                {
                    public TableInfo getLookupTableInfo()
                    {
                        return new PeptidesTableInfo(_schema);
                    }
                };
                if (!forExport)
                {
                    fk.setPrefixColumnCaption(false);
                }
                peptideIdColumn.setFk(fk);
                addColumn(peptideIdColumn);
            }
        }

        addColumn(new ExprColumn(this, "PeptideSequence", new SQLFragment("InnerPeptide"), JdbcType.VARCHAR));

        ExprColumn peptideIdColumn = new ExprColumn(this, "Run", new SQLFragment("<ILLEGAL STATE>"), JdbcType.INTEGER);
        peptideIdColumn.setIsUnselectable(true);
        defaultCols.add(FieldKey.fromParts("Run", "PeptideProphet"));
        peptideIdColumn.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return new PeptidesTableInfo(_schema);
            }
        });
        addColumn(peptideIdColumn);

        SQLFragment runCountSQL = new SQLFragment("(");
        String separator = "";
        for (ColumnInfo runCol : runColumns)
        {
            runCountSQL.append(separator);
            separator = " + ";
            runCountSQL.append("CASE WHEN " + runCol.getAlias() + "$.RowId IS NULL THEN 0 ELSE 1 END ");
        }
        runCountSQL.append(")");
        ExprColumn runCount = new ExprColumn(this, "RunCount", runCountSQL, JdbcType.INTEGER, runColumns.toArray(new ColumnInfo[runColumns.size()]));
        addColumn(runCount);

        SQLFragment patternSQL = new SQLFragment("(");
        separator = "";
        int offset = 0;
        for (ColumnInfo runCol : runColumns)
        {
            patternSQL.append(separator);
            separator = " + ";
            patternSQL.append("CASE WHEN " + runCol.getAlias() + "$.RowId IS NULL THEN 0 ELSE ");
            patternSQL.append(1 << offset);
            patternSQL.append(" END ");
            offset++;
            if (offset >= 64)
            {
                break;
            }
        }
        patternSQL.append(")");
        ExprColumn patternColumn = new ExprColumn(this, "Pattern", patternSQL, JdbcType.INTEGER, runColumns.toArray(new ColumnInfo[runColumns.size()]));
        addColumn(patternColumn);

        defaultCols.add(FieldKey.fromParts("RunCount"));

        setDefaultVisibleColumns(defaultCols);
    }


    @Override @NotNull
    public SQLFragment getFromSQL()
    {
        SQLFragment result = new SQLFragment();
        result.append("SELECT InnerPeptide");
        for (MS2Run run : _runs)
        {
            result.append(",\n");
            result.append("MAX(Run");
            result.append(run.getRun());
            result.append("PeptideId) AS Run");
            result.append(run.getRun());
            result.append("PeptideId");
        }
        result.append("\nFROM (SELECT Peptide AS InnerPeptide");
        for (MS2Run run : _runs)
        {
            result.append(",\n");
            result.append("\tCASE WHEN Run=");
            result.append(run.getRun());
            result.append(" THEN MAX(p.RowId) ELSE NULL END AS Run");
            result.append(run.getRun());
            result.append("PeptideId");
        }
        result.append( "\nFROM ");
        result.append(MS2Manager.getTableInfoFractions(), "f");
        result.append(", (");
        List<FieldKey> fieldKeys = Arrays.asList(
                FieldKey.fromParts("Fraction"),
                FieldKey.fromParts("Peptide"),
                FieldKey.fromParts("RowId")
        );
        result.append(_schema.getPeptideSelectSQL(_request, _peptideViewName, fieldKeys, null));
        result.append(") ");
        result.append(" p WHERE f.Run IN(");
        String separator = "";
        for (MS2Run run : _runs)
        {
            result.append(separator);
            separator = ", ";
            result.append(run.getRun());
        }
        result.append(") AND p.Fraction = f.Fraction GROUP BY f.Run, p.Peptide, p.RowId) x GROUP BY InnerPeptide");
        return result;
    }
}
