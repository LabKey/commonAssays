package org.labkey.ms2.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.MS2Manager;

import java.util.*;

/**
 * User: jeckels
 * Date: Feb 9, 2007
 */
public class SequencesTableInfo extends FilteredTable
{
    private final Container _container;

    public SequencesTableInfo(String alias, Container container)
    {
        super(ProteinManager.getTableInfoSequences());
        _container = container;
        setAlias(alias);
        wrapAllColumns(true);

        getColumn("OrgId").setFk(new LookupForeignKey("OrgId", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                return new OrganismTableInfo();
            }
        });

        addColumn(wrapColumn("Source", getRealTable().getColumn("SourceId")));
        getColumn("SourceId").setIsHidden(true);


        ViewURLHelper url = new ViewURLHelper("MS2", "showProtein.view", _container);
        getColumn("BestName").setURL(url + "seqId=${SeqId}");

        List<FieldKey> cols = new ArrayList<FieldKey>();
        cols.add(FieldKey.fromParts("BestName"));
        cols.add(FieldKey.fromParts("Description"));
        cols.add(FieldKey.fromParts("BestGeneName"));
        cols.add(FieldKey.fromParts("Length"));
        cols.add(FieldKey.fromParts("Mass"));
        cols.add(FieldKey.fromParts("OrgId"));
        setDefaultVisibleColumns(cols);
    }

    /*package*/ static List<String> getIdentifierParameters(String identifiers)
    {
        List<String> result = new ArrayList<String>();
        if (identifiers == null || identifiers.trim().equals(""))
        {
            return result;
        }

        StringTokenizer st = new StringTokenizer(identifiers, " \t\n\r,");
        while (st.hasMoreTokens())
        {
            result.add(st.nextToken());
        }
        return result;
    }

    /*package*/ static String getIdentifierClause(List<String> params, String columnName, boolean exactMatch)
    {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        sb.append("(");
        if (params.isEmpty())
        {
            sb.append("1 = 2");
        }
        for (String param : params)
        {
            sb.append(separator);
            sb.append(columnName);
            if (exactMatch)
            {
                sb.append(" = '");
                sb.append(param.replaceAll("'", "''"));
                sb.append("'");
            }
            else
            {
                sb.append(" LIKE '");
                sb.append(param.replaceAll("'", "''"));
                sb.append("%'");
            }
            separator = " OR ";
        }
        sb.append(")");
        return sb.toString();
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        Set<Container> containers = ContainerManager.getAllChildren(c, u, ACL.PERM_READ);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences());
        sql.append(" fs, ");
        sql.append(MS2Manager.getTableInfoRuns());
        sql.append(" r WHERE fs.FastaId = r.FastaId AND r.Deleted = ? AND r.Container IN ");
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

    public void addProteinNameFilter(String identifier, boolean exactMatch)
    {
        List<String> params = getIdentifierParameters(identifier);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations());
        sql.append(" a WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "a.AnnotVal", exactMatch));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences());
        sql.append(" fs WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "fs.lookupstring", exactMatch));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers());
        sql.append(" i WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "i.Identifier", exactMatch));
        sql.append("\n");
        sql.append(")");
        addCondition(sql);
    }

}
