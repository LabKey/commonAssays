package org.labkey.ms2.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.data.*;
import org.labkey.api.util.CaseInsensitiveHashSet;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.GroupNumberDisplayColumn;
import org.labkey.ms2.protein.ProteinManager;

import java.util.*;

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
        this(alias, true, schema);
    }


    public ProteinGroupTableInfo(String alias, boolean standalone, MS2Schema schema)
    {
        super(MS2Manager.getTableInfoProteinGroups());
        _schema = schema;
        setAlias(alias);

        ColumnInfo groupNumberColumn = wrapColumn("Group", getRealTable().getColumn("GroupNumber"));
        groupNumberColumn.setAlias("GroupAlias");
        groupNumberColumn.setRenderClass(GroupNumberDisplayColumn.class);
        ViewURLHelper url = new ViewURLHelper("MS2", "showProteinGroup.view", "");
        groupNumberColumn.setURL(url + "proteinGroupId=${RowId}");

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
        if (!standalone)
        {
            getColumn("ProteinProphet").setIsHidden(true);
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

        List<FieldKey> defaultColumns = new ArrayList<FieldKey>();
        defaultColumns.add(FieldKey.fromParts("ProteinProphet", "Run", "Container"));
        defaultColumns.add(FieldKey.fromParts("ProteinProphet","Run"));
        defaultColumns.add(FieldKey.fromParts("Group"));
        defaultColumns.add(FieldKey.fromParts("GroupProbability"));
        defaultColumns.add(FieldKey.fromParts("ErrorRate"));
        defaultColumns.add(FieldKey.fromParts("UniquePeptidesCount"));
        defaultColumns.add(FieldKey.fromParts("TotalNumberPeptides"));

        setDefaultVisibleColumns(defaultColumns);
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        Set<Container> containers = ContainerManager.getChildrenRecusively(c, u, ACL.PERM_READ);
        SQLFragment sql = new SQLFragment();
        sql.append("ProteinProphetFileId IN (SELECT ppf.RowId FROM ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(" ppf, ");
        sql.append(MS2Manager.getTableInfoRuns());
        sql.append(" r WHERE ppf.run = r.run AND r.Deleted = ? AND r.Container IN ");
        sql.add(Boolean.FALSE);
        if (includeSubfolders)
        {
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

    public void addProteinNameFilter(String identifier)
    {
        List<Parameter> params = SequencesTableInfo.getIdentifierParameters(identifier);
        String inClause = SequencesTableInfo.getIdentifierInClause(params);
        SQLFragment sql = new SQLFragment();
        sql.append("RowId IN (\n");
        sql.append("SELECT ProteinGroupId FROM ");
        sql.append(MS2Manager.getTableInfoProteinGroupMemberships());
        sql.append(" pgm WHERE pgm.SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations());
        sql.append(" a WHERE a.AnnotVal IN ");
        sql.append(inClause);
        sql.addAll(params);
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences());
        sql.append(" fs WHERE fs.LookupString IN ");
        sql.append(inClause);
        sql.addAll(params);
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers());
        sql.append(" i WHERE i.Identifier IN ");
        sql.append(inClause);
        sql.addAll(params);
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
