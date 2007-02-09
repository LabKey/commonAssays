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
    private static final Set<String> HIDDEN_PROTEIN_GROUP_COLUMN_NAMES = new CaseInsensitiveHashSet(Arrays.asList("RowId", "GroupNumber", "IndistinguishableCollectionId"));

    private Container _container;

    public ProteinGroupTableInfo(String alias)
    {
        this(alias, true);
    }

    public ProteinGroupTableInfo(String alias, boolean standalone)
    {
        super(MS2Manager.getTableInfoProteinGroups());
        setAlias(alias);

        ColumnInfo groupNumberColumn = wrapColumn("Group", getRealTable().getColumn("GroupNumber"));
        groupNumberColumn.setRenderClass(GroupNumberDisplayColumn.class);
        ViewURLHelper url = new ViewURLHelper("MS2", "showProteinGroup.view", "");
        groupNumberColumn.setURL(url + "proteinGroupId=${RowId}");

        addColumn(groupNumberColumn);

        wrapAllColumns(true);

        for (ColumnInfo col : getColumns())
        {
            if (HIDDEN_PROTEIN_GROUP_COLUMN_NAMES.contains(col.getName()))
            {
                col.setIsHidden(true);
            }
        }
        if (!standalone)
        {
            getColumn("ProteinProphetFileId").setIsHidden(true);
        }

        getColumn("ProteinProphetFileId").setFk(new LookupForeignKey("RowId", false)
        {
            public TableInfo getLookupTableInfo()
            {
                return new ProteinProphetFileTableInfo();
            }
        });

        List<FieldKey> defaultColumns = new ArrayList<FieldKey>();
        defaultColumns.add(FieldKey.fromString("ProteinProphetFileId/Run/Container"));
        defaultColumns.add(FieldKey.fromString("ProteinProphetFileId/Run"));
        defaultColumns.add(FieldKey.fromString("Group"));
        defaultColumns.add(FieldKey.fromString("GroupProbability"));
        defaultColumns.add(FieldKey.fromString("ErrorRate"));
        defaultColumns.add(FieldKey.fromString("UniquePeptides"));
        defaultColumns.add(FieldKey.fromString("TotalPeptides"));

        setDefaultVisibleColumns(defaultColumns);
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        _container = c;
        List<Container> containers = ContainerManager.getChildrenRecusively(c, u, ACL.PERM_READ);
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
            sql.append("(");
            sql.append(c.getId());
            sql.append(")");
        }
        sql.append(")");
        addCondition(sql);
    }

    public void addProteinNameFilter(String identifier)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("RowId IN (\n");
        sql.append("SELECT ProteinGroupId FROM ");
        sql.append(MS2Manager.getTableInfoProteinGroupMemberships());
        sql.append(" pgm WHERE pgm.SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations());
        sql.append(" a WHERE a.AnnotVal = ?\n");
        sql.add(identifier);
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences());
        sql.append(" fs WHERE fs.LookupString = ?\n");
        sql.add(identifier);
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers());
        sql.append(" i WHERE i.Identifier = ?\n");
        sql.add(identifier);
        sql.append("))");
        addCondition(sql);
    }
}
