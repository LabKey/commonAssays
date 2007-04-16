package org.labkey.ms2.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.data.*;
import org.labkey.api.util.CaseInsensitiveHashSet;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.GroupNumberDisplayColumn;
import org.labkey.ms2.ProteinListDisplayColumn;
import org.labkey.ms2.protein.ProteinManager;

import java.util.*;
import java.sql.Types;

/**
 * User: jeckels
 * Date: Feb 8, 2007
 */
public class ProteinGroupTableInfo extends FilteredTable
{
    private static final Set<String> HIDDEN_PROTEIN_GROUP_COLUMN_NAMES = new CaseInsensitiveHashSet(Arrays.asList("RowId", "GroupNumber", "IndistinguishableCollectionId", "Deleted", "HasPeptideProphet"));
    private final MS2Schema _schema;

    public ProteinGroupTableInfo(String alias, MS2Schema schema)
    {
        super(MS2Manager.getTableInfoProteinGroups());
        _schema = schema;
        if (alias != null)
        {
            setAlias(alias);
        }

        ColumnInfo groupNumberColumn = wrapColumn("Group", getRealTable().getColumn("GroupNumber"));
        groupNumberColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new GroupNumberDisplayColumn(colInfo, _schema.getContainer());
            }
        });

        addColumn(groupNumberColumn);

        wrapAllColumns(true);
        addColumn(wrapColumn("ProteinProphet", getRealTable().getColumn("ProteinProphetFileId")));
        getColumn("ProteinProphetFileId").setIsHidden(true);

        ColumnInfo quantitation = wrapColumn("Quantitation", getRealTable().getColumn("RowId"));
        quantitation.setFk(new LookupForeignKey("ProteinGroupId")
        {
            public TableInfo getLookupTableInfo()
            {
                return MS2Manager.getTableInfoProteinQuantitation();
            }
        });
        quantitation.setKeyField(false);
        addColumn(quantitation);

        for (ColumnInfo col : getColumns())
        {
            if (HIDDEN_PROTEIN_GROUP_COLUMN_NAMES.contains(col.getName()))
            {
                col.setIsHidden(true);
            }
        }

        LookupForeignKey foreignKey = new LookupForeignKey("RowId", false)
        {
            public TableInfo getLookupTableInfo()
            {
                return new ProteinProphetFileTableInfo(_schema);
            }
        };
        getColumn("ProteinProphetFileId").setFk(foreignKey);
        getColumn("ProteinProphet").setFk(foreignKey);

        SQLFragment firstProteinSQL = new SQLFragment();
        firstProteinSQL.append("SELECT s.SeqId FROM ");
        firstProteinSQL.append(MS2Manager.getTableInfoProteinGroupMemberships());
        firstProteinSQL.append(" pgm, ");
        firstProteinSQL.append(ProteinManager.getTableInfoSequences());
        firstProteinSQL.append(" s WHERE s.SeqId = pgm.SeqId AND pgm.ProteinGroupId = ");
        firstProteinSQL.append(ExprColumn.STR_TABLE_ALIAS);
        firstProteinSQL.append(".RowId ORDER BY s.Length, s.BestName");
        ProteinManager.getSqlDialect().limitRows(firstProteinSQL, 1);
        firstProteinSQL.insert(0, "(");
        firstProteinSQL.append(")");

        ExprColumn firstProteinColumn = new ExprColumn(this, "FirstProtein", firstProteinSQL, Types.INTEGER);
        firstProteinColumn.setFk(new LookupForeignKey("SeqId")
        {
            public TableInfo getLookupTableInfo()
            {
                return new SequencesTableInfo(null, _schema.getContainer());
            }
        });
        addColumn(firstProteinColumn);

        SQLFragment proteinCountSQL = new SQLFragment();
        proteinCountSQL.append("(SELECT COUNT(SeqId) FROM ");
        proteinCountSQL.append(MS2Manager.getTableInfoProteinGroupMemberships());
        proteinCountSQL.append(" WHERE ProteinGroupId = ");
        proteinCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
        proteinCountSQL.append(".RowId)");
        ExprColumn proteinCountColumn = new ExprColumn(this, "ProteinCount", proteinCountSQL, Types.INTEGER);
        addColumn(proteinCountColumn);

        List<FieldKey> defaultColumns = new ArrayList<FieldKey>();
        defaultColumns.add(FieldKey.fromParts("Group"));
        defaultColumns.add(FieldKey.fromParts("GroupProbability"));
        defaultColumns.add(FieldKey.fromParts("ErrorRate"));
        defaultColumns.add(FieldKey.fromParts("UniquePeptidesCount"));
        defaultColumns.add(FieldKey.fromParts("TotalNumberPeptides"));

        setDefaultVisibleColumns(defaultColumns);
    }

    public void addProteinsColumn()
    {
        ColumnInfo proteinGroup = wrapColumn("Proteins", getRealTable().getColumn("RowId"));
        proteinGroup.setFk(new LookupForeignKey("ProteinGroupId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createProteinGroupMembershipsTable();
            }
        });
        proteinGroup.setKeyField(false);
        addColumn(proteinGroup);
    }

    public void addProteinDetailColumns()
    {
        ColumnInfo rowIdColumn = _rootTable.getColumn("RowId");

        DisplayColumnFactory factory = new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ProteinListDisplayColumn result = new ProteinListDisplayColumn(colInfo.getColumnName(), _schema.getProteinGroupProteins());
                result.setColumnInfo(colInfo);
                return result;
            }
        };

        ColumnInfo proteinNameColumn = wrapColumn("Protein", rowIdColumn);
        proteinNameColumn.setDisplayColumnFactory(factory);
        addColumn(proteinNameColumn);

        ColumnInfo bestNameColumn = wrapColumn("BestName", rowIdColumn);
        bestNameColumn.setDisplayColumnFactory(factory);
        addColumn(bestNameColumn);

        ColumnInfo bestGeneNameColumn = wrapColumn("BestGeneName", rowIdColumn);
        bestGeneNameColumn.setDisplayColumnFactory(factory);
        addColumn(bestGeneNameColumn);

        ColumnInfo massColumn = wrapColumn("SequenceMass", rowIdColumn);
        massColumn.setDisplayColumnFactory(factory);
        addColumn(massColumn);

        ColumnInfo descriptionColumn = wrapColumn("Description", rowIdColumn);
        descriptionColumn.setDisplayColumnFactory(factory);
        addColumn(descriptionColumn);
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("ProteinProphetFileId IN (SELECT ppf.RowId FROM ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(" ppf, ");
        sql.append(MS2Manager.getTableInfoRuns());
        sql.append(" r WHERE ppf.run = r.run AND r.Deleted = ? AND r.Container IN ");
        sql.add(Boolean.FALSE);
        if (includeSubfolders)
        {
            Set<Container> containers = ContainerManager.getAllChildren(c, u, ACL.PERM_READ);
            sql.append(ContainerManager.getIdsAsCsvList(new HashSet<Container>(containers)));
        }
        else
        {
            sql.append("('");
            sql.append(c.getId());
            sql.append("')");
        }
        sql.append(")");
        addCondition(sql);
    }

    public void addProteinNameFilter(String identifier, boolean exactMatch)
    {
        List<String> params = SequencesTableInfo.getIdentifierParameters(identifier);
        SQLFragment sql = new SQLFragment();
        sql.append("RowId IN (\n");
        sql.append("SELECT ProteinGroupId FROM ");
        sql.append(MS2Manager.getTableInfoProteinGroupMemberships());
        sql.append(" pgm WHERE pgm.SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations());
        sql.append(" a WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "a.AnnotVal", exactMatch));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences());
        sql.append(" fs WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "fs.LookupString", exactMatch));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers());
        sql.append(" i WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "i.Identifier", exactMatch));
        sql.append("\n");
        sql.append("))");
        addCondition(sql);
    }

    public void addMinimumProbability(float minProb)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("GroupProbability >= ?");
        sql.add(minProb);
        addCondition(sql);
    }

    public void addMaximumErrorRate(float maxError)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("ErrorRate <= ?");
        sql.add(maxError);
        addCondition(sql);
    }
}
