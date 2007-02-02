package org.labkey.ms2;

import org.labkey.api.data.DataColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.FieldKey;

import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class GroupNumberDisplayColumn extends DataColumn
{
    private String _groupNumber;
    private String _collectionId;
    private boolean _fromQuery = false;

    public GroupNumberDisplayColumn(ColumnInfo col)
    {
        this(col, null);
        _fromQuery = true;
    }

    public GroupNumberDisplayColumn(ColumnInfo col, ViewURLHelper url)
    {
        this(col, url, "GroupNumber", "IndistinguishableCollectionId");
    }

    public GroupNumberDisplayColumn(ColumnInfo col, ViewURLHelper url, String groupNumber, String collectionId)
    {
        super(col);
        setName("ProteinGroup");
        _groupNumber = groupNumber;
        _collectionId = collectionId;
        if (url != null)
        {
            ViewURLHelper urlHelper = url.clone();
            urlHelper.setAction("showProteinGroup.view");
            setURL(urlHelper.toString() + "&groupNumber=${" + _groupNumber + "}&indistinguishableCollectionId=${" + _collectionId + "}");
        }
    }

    public String getFormattedValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        int collectionId = ((Integer)row.get(_collectionId)).intValue();
        int groupNumber = ((Integer)row.get(_groupNumber)).intValue();

        StringBuilder sb = new StringBuilder();
        sb.append(groupNumber);
        if (collectionId != 0)
        {
            sb.append("-");
            sb.append(collectionId);
        }
        return sb.toString();
    }

    public void addQueryColumns(Set<ColumnInfo> columns)
    {
        super.addQueryColumns(columns);
        if (_fromQuery)
        {
            FieldKey thisFieldKey = FieldKey.fromString(getColumnInfo().getName());
            FieldKey idiKey = new FieldKey(thisFieldKey.getTable(), "IndistinguishableCollectionId");
            for (ColumnInfo columnInfo : QueryService.get().getColumns(getColumnInfo().getParentTable(), Collections.singleton(idiKey)).values())
            {
                columns.add(columnInfo);
                _groupNumber = getColumnInfo().getAlias();
                _collectionId = columnInfo.getAlias();
            }
        }
    }
}
