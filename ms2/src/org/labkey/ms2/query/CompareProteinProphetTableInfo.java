package org.labkey.ms2.query;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.FieldKey;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;

import java.util.List;
import java.util.ArrayList;
import java.sql.Types;

/**
 * User: jeckels
 * Date: Apr 12, 2007
 */
public class CompareProteinProphetTableInfo extends SequencesTableInfo
{
    private final MS2Schema _schema;
    private final List<MS2Run> _runs;

    public CompareProteinProphetTableInfo(String alias, MS2Schema schema, List<MS2Run> runs)
    {
        super(alias, schema.getContainer());

        _schema = schema;
        _runs = runs;

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts("BestName"));

        List<ColumnInfo> runColumns = new ArrayList<ColumnInfo>();

        if (runs != null)
        {
            SQLFragment seqIdCondition = new SQLFragment();
            seqIdCondition.append("SeqId IN (SELECT DISTINCT(SeqId) FROM ");
            seqIdCondition.append(MS2Manager.getTableInfoProteinGroupMemberships());
            seqIdCondition.append(" pgm, ");
            seqIdCondition.append(MS2Manager.getTableInfoProteinGroups());
            seqIdCondition.append(" pg, ");
            seqIdCondition.append(MS2Manager.getTableInfoProteinProphetFiles());
            seqIdCondition.append(" ppf\nWHERE ppf.Run IN (");
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
                sql.append("(SELECT MIN(ProteinGroupId) FROM ");
                sql.append(MS2Manager.getTableInfoProteinGroupMemberships());
                sql.append(" pgm, ");
                sql.append(MS2Manager.getTableInfoProteinGroups());
                sql.append(" pg, ");
                sql.append(MS2Manager.getTableInfoProteinProphetFiles());
                sql.append(" ppf\nWHERE ppf.Run = ");
                sql.append(run.getRun());
                sql.append(" AND pgm.ProteinGroupId = pg.RowId AND ppf.RowId = pg.ProteinProphetFileId AND pgm.SeqId = ");
                sql.append(ExprColumn.STR_TABLE_ALIAS);
                sql.append(".SeqId)");
                ExprColumn proteinGroupIdColumn = new ExprColumn(this, "Run" + run.getRun(), sql, Types.INTEGER);
                proteinGroupIdColumn.setIsUnselectable(true);
                runColumns.add(proteinGroupIdColumn);
                proteinGroupIdColumn.setFk(new LookupForeignKey("RowId", false)
                {
                    public TableInfo getLookupTableInfo()
                    {
                        return new ProteinGroupTableInfo(null, _schema);
                    }
                });
                addColumn(proteinGroupIdColumn);
            }
        }

        ExprColumn proteinGroupIdColumn = new ExprColumn(this, "Run", new SQLFragment("<ILLEGAL STATE>"), Types.INTEGER);
        proteinGroupIdColumn.setIsUnselectable(true);
        defaultCols.add(FieldKey.fromParts("Run", "Group"));
        defaultCols.add(FieldKey.fromParts("Run", "GroupProbability"));
        proteinGroupIdColumn.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return new ProteinGroupTableInfo(null, _schema);
            }
        });
        addColumn(proteinGroupIdColumn);

        SQLFragment runCountSQL = new SQLFragment("(");
        String separator = "";
        for (ColumnInfo runCol : runColumns)
        {
            runCountSQL.append(separator);
            separator = " + ";
            runCountSQL.append("CASE WHEN " + runCol.getAlias() + "$.RowId IS NULL THEN 0 ELSE 1 END ");
        }
        runCountSQL.append(")");
        ExprColumn runCount = new ExprColumn(this, "RunCount", runCountSQL, Types.INTEGER, runColumns.toArray(new ColumnInfo[runColumns.size()]));
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
        ExprColumn patternColumn = new ExprColumn(this, "Pattern", patternSQL, Types.INTEGER, runColumns.toArray(new ColumnInfo[runColumns.size()]));
        addColumn(patternColumn);

        defaultCols.add(FieldKey.fromParts("RunCount"));
        defaultCols.add(FieldKey.fromParts("Pattern"));

        setDefaultVisibleColumns(defaultCols);
    }
}
