package org.labkey.ms2;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;

import java.util.Set;

/**
 * User: adam
 * Date: Aug 3, 2006
 * Time: 10:42 AM
 */
public class ProteinDisplayColumn extends DataColumn
{
    public ProteinDisplayColumn(ColumnInfo col)
    {
        super(col);
        setLinkTarget("prot");
    }

    public void addQueryColumns(Set<ColumnInfo> set)
    {
        super.addQueryColumns(set);
        set.add(getColumnInfo().getParentTable().getColumn("SeqId"));
        set.add(getColumnInfo().getParentTable().getColumn("Protein"));
    }
}
