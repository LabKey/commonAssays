package org.labkey.ms2;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;

import java.util.Arrays;
import java.util.Map;
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
        FieldKey key = FieldKey.fromString(getColumnInfo().getName());

        FieldKey parentKey = key.getParent();
        FieldKey seqIdKey;
        FieldKey proteinKey;
        if (parentKey != null)
        {
            seqIdKey = new FieldKey(parentKey, "SeqId");
            proteinKey = new FieldKey(parentKey, "Protein");
        }
        else
        {
            seqIdKey = FieldKey.fromParts("SeqId");
            proteinKey = FieldKey.fromParts("Protein");
        }
        
        Map<FieldKey, ColumnInfo> colMap = QueryService.get().getColumns(getColumnInfo().getParentTable(), Arrays.asList(seqIdKey, proteinKey));
        set.addAll(colMap.values());
    }
}
