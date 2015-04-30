/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.FieldKey;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * User: jeckels
 * Date: Apr 12, 2007
 */
public class CompareProteinProphetTableInfo extends SequencesTableInfo<MS2Schema>
{
    private final List<MS2Run> _runs;
    private final boolean _forExport;
    private final HttpServletRequest _request;
    private final String _peptideViewName;

    public CompareProteinProphetTableInfo(MS2Schema schema, List<MS2Run> runs, boolean forExport, HttpServletRequest request, String peptideViewName)
    {
        super(MS2Schema.HiddenTableType.CompareProteinProphet.toString(), schema);

        _runs = runs;
        _forExport = forExport;
        _request = request;
        _peptideViewName = peptideViewName;

        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts("BestName"));

        List<ColumnInfo> runColumns = new ArrayList<>();

        if (runs != null)
        {
            SQLFragment seqIdCondition = new SQLFragment();
            seqIdCondition.append("SeqId IN (SELECT DISTINCT(SeqId) FROM ");
            seqIdCondition.append(MS2Manager.getTableInfoProteinGroupMemberships(), "pgm");
            seqIdCondition.append(", ");
            seqIdCondition.append(MS2Manager.getTableInfoProteinGroups(), "pg");
            seqIdCondition.append(", ");
            seqIdCondition.append(MS2Manager.getTableInfoProteinProphetFiles(), "ppf");
            seqIdCondition.append("\nWHERE ppf.Run IN (");
            String separator = "";
            for (MS2Run run : runs)
            {
                seqIdCondition.append(separator);
                separator = ", ";
                seqIdCondition.append(run.getRun());
            }
            seqIdCondition.append(") AND ppf.RowId = pg.ProteinProphetFileId AND pg.RowId = pgm.ProteinGroupId)");
            addCondition(seqIdCondition);

            for (MS2Run run : runs)
            {
                SQLFragment sql = new SQLFragment();
                sql.append("Run");
                sql.append(run.getRun());
                sql.append("ProteinGroupId");
                ExprColumn proteinGroupIdColumn = new ExprColumn(this, "Run" + run.getRun(), sql, JdbcType.INTEGER);
                proteinGroupIdColumn.setLabel(run.getDescription());
                proteinGroupIdColumn.setIsUnselectable(true);
                runColumns.add(proteinGroupIdColumn);
                LookupForeignKey fk = new LookupForeignKey("RowId")
                {
                    public TableInfo getLookupTableInfo()
                    {
                        return new ProteinGroupTableInfo(_userSchema, false);
                    }
                };
                if (!_forExport)
                {
                    fk.setPrefixColumnCaption(false);
                }
                proteinGroupIdColumn.setFk(fk);
                addColumn(proteinGroupIdColumn);
            }
        }

        if (runColumns.isEmpty())
        {
            ExprColumn proteinGroupIdColumn = new ExprColumn(this, "Run", new SQLFragment("<ILLEGAL STATE>"), JdbcType.INTEGER);
            proteinGroupIdColumn.setIsUnselectable(true);
            proteinGroupIdColumn.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return new ProteinGroupTableInfo(_userSchema, false);
                }
            });
            addColumn(proteinGroupIdColumn);
        }

        defaultCols.add(FieldKey.fromParts("Run", "Group"));
        defaultCols.add(FieldKey.fromParts("Run", "GroupProbability"));

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
    public SQLFragment getFromSQL(String alias)
    {
        String innerAlias = "_innerCPP";
        SQLFragment result = new SQLFragment();
        result.append("(SELECT * FROM ");
        result.append(super.getFromSQL(innerAlias));
        result.append(", (SELECT InnerSeqId ");
        for (MS2Run run : _runs)
        {
            result.append(",\n");
            result.append("MAX(Run");
            result.append(run.getRun());
            result.append("ProteinGroupId) AS Run");
            result.append(run.getRun());
            result.append("ProteinGroupId");
        }
        result.append("\nFROM (SELECT SeqId AS InnerSeqId");
        for (MS2Run run : _runs)
        {
            result.append(",\n");
            result.append("\tCASE WHEN Run=");
            result.append(run.getRun());
            // There should only be one matching protein group, but we have to aggregate to get it past the GROUP BY clause
            result.append(" THEN MAX(pg.RowId) ELSE NULL END AS Run");
            result.append(run.getRun());
            result.append("ProteinGroupId");
        }
        result.append( "\nFROM ");
        result.append(MS2Manager.getTableInfoProteinProphetFiles(), "ppf");
        result.append(", ");
        result.append(MS2Manager.getTableInfoProteinGroups(), "pg");
        result.append(", ");
        result.append(MS2Manager.getTableInfoPeptideMemberships(), "pm");
        result.append(", ");
        result.append(MS2Manager.getTableInfoProteinGroupMemberships(), "pgm");
        result.append(", (");
        result.append(_userSchema.getPeptideSelectSQL(_request, _peptideViewName, Collections.singletonList(FieldKey.fromParts("RowId")), null));
        result.append(" ) pep WHERE ppf.Run IN (");
        String separator = "";
        for (MS2Run run : _runs)
        {
            result.append(separator);
            separator = ", ";
            result.append(run.getRun());
        }
        result.append(") AND ppf.RowId = pg.ProteinProphetFileId AND pg.RowId = pgm.ProteinGroupId AND pep.RowId = pm.PeptideId AND pm.ProteinGroupId = pg.RowId\n");
        result.append("GROUP BY Run, SeqId) x GROUP BY InnerSeqId)\n");
        result.append(" AS RunProteinGroups WHERE RunProteinGroups.InnerSeqId = ");
        result.append(innerAlias);
        result.append(".SeqId");
        result.append(") ").append(alias);
        return result;
    }
    
    protected ColumnInfo resolveColumn(String name)
    {
        ColumnInfo result = super.resolveColumn(name);
        if (result == null && "Run".equalsIgnoreCase(name) && !_runs.isEmpty())
        {
            result = getColumn("Run" + _runs.get(0).getRun());
        }
        return result;
    }
}
