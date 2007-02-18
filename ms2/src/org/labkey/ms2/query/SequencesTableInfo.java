package org.labkey.ms2.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
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

        ViewURLHelper url = new ViewURLHelper("MS2", "showProtein.view", _container);
        getColumn("BestName").setURL(url + "seqId=${SeqId}");

        List<FieldKey> cols = new ArrayList<FieldKey>();
        cols.add(FieldKey.fromString("BestName"));
        cols.add(FieldKey.fromString("Description"));
        cols.add(FieldKey.fromString("BestGeneName"));
        cols.add(FieldKey.fromString("Length"));
        cols.add(FieldKey.fromString("Mass"));
        cols.add(FieldKey.fromString("OrgId"));
        setDefaultVisibleColumns(cols);
    }

    /*package*/ static String getIdentifierInClause(String identifiers)
    {
        if (identifiers == null || identifiers.trim().equals(""))
        {
            return "(NULL)";
        }
        StringTokenizer st = new StringTokenizer(identifiers, " \t\n\r,");
        StringBuilder sb = new StringBuilder();
        String separator = "";
        sb.append("(");
        if (!st.hasMoreTokens())
        {
            sb.append("NULL");
        }
        while (st.hasMoreTokens())
        {
            sb.append(separator);
            sb.append("'");
            sb.append(st.nextToken().replaceAll("'", "''"));
            sb.append("'");
            separator = ", ";
        }
        sb.append(")");
        return sb.toString();
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        Set<Container> containers = ContainerManager.getChildrenRecusively(c, u, ACL.PERM_READ);
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
            sql.append("(");
            sql.append(c.getId());
            sql.append(")");
        }
        sql.append(")");
        addCondition(sql);
    }

    public void addProteinNameFilter(String identifier)
    {
        String inClause = getIdentifierInClause(identifier);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations());
        sql.append(" a WHERE a.AnnotVal IN ");
        sql.append(inClause);
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences());
        sql.append(" fs WHERE fs.LookupString IN ");
        sql.append(inClause);
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers());
        sql.append(" i WHERE i.Identifier IN ");
        sql.append(inClause);
        sql.append("\n");
        sql.append(")");
        addCondition(sql);
    }

}
