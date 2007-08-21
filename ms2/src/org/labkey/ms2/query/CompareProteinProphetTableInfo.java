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
    private final boolean _forExport;

    public CompareProteinProphetTableInfo(String alias, MS2Schema schema, List<MS2Run> runs, boolean forExport)
    {
        super(alias, schema);

        _schema = schema;
        _runs = runs;
        _forExport = forExport;

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
                sql.append("Run");
                sql.append(run.getRun());
                sql.append("ProteinGroupId");
                ExprColumn proteinGroupIdColumn = new ExprColumn(this, "Run" + run.getRun(), sql, Types.INTEGER);
                proteinGroupIdColumn.setCaption(run.getDescription());
                proteinGroupIdColumn.setIsUnselectable(true);
                runColumns.add(proteinGroupIdColumn);
                LookupForeignKey fk = new LookupForeignKey("RowId")
                {
                    public TableInfo getLookupTableInfo()
                    {
                        return new ProteinGroupTableInfo(null, _schema, false);
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
            ExprColumn proteinGroupIdColumn = new ExprColumn(this, "Run", new SQLFragment("<ILLEGAL STATE>"), Types.INTEGER);
            proteinGroupIdColumn.setIsUnselectable(true);
            defaultCols.add(FieldKey.fromParts("Run", "Group"));
            defaultCols.add(FieldKey.fromParts("Run", "GroupProbability"));
            proteinGroupIdColumn.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return new ProteinGroupTableInfo(null, _schema, false);
                }
            });
            addColumn(proteinGroupIdColumn);
        }

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

        setDefaultVisibleColumns(defaultCols);
    }


    public SQLFragment getFromSQL(String alias)
    {
        String innerAlias = "Inner" + alias;
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
            result.append(" THEN MAX(pg.RowId) ELSE NULL END AS Run");
            result.append(run.getRun());
            result.append("ProteinGroupId");
        }
        result.append( "\nFROM ");
        result.append(MS2Manager.getTableInfoProteinProphetFiles());
        result.append(" ppf, ");
        result.append(MS2Manager.getTableInfoProteinGroups());
        result.append(" pg, ");
        result.append(MS2Manager.getTableInfoProteinGroupMemberships());
        result.append(" pgm WHERE ppf.Run IN(");
        String separator = "";
        for (MS2Run run : _runs)
        {
            result.append(separator);
            separator = ", ";
            result.append(run.getRun());
        }
        result.append(") AND ppf.RowId = pg.ProteinProphetFileId AND pg.RowId = pgm.ProteinGroupId GROUP BY Run, SeqId) x GROUP BY InnerSeqId)\n");
        result.append(" AS RunProteinGroups WHERE RunProteinGroups.InnerSeqId = ");
        result.append(innerAlias);
        result.append(".SeqId) AS ");
        result.append(alias);
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
