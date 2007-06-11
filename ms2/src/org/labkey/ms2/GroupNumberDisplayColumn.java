package org.labkey.ms2;

import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;

import java.util.*;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class GroupNumberDisplayColumn extends DataColumn
{
    private String _groupNumber;
    private String _collectionId;
    private boolean _fromQuery;
    private Container _container;
    private ColumnInfo _collectionIdColumn;

    public GroupNumberDisplayColumn(ColumnInfo col, Container c)
    {
        super(col);
        _fromQuery = true;
        _container = c;
    }

    public GroupNumberDisplayColumn(ColumnInfo col, ViewURLHelper url, String groupNumber, String collectionId)
    {
        super(col);
        setName("ProteinGroup");
        _groupNumber = groupNumber;
        _collectionId = collectionId;
        ViewURLHelper urlHelper = url.clone();
        urlHelper.setAction("showProteinGroup.view");
        setURL(urlHelper.toString() + "&groupNumber=${" + _groupNumber + "}&indistinguishableCollectionId=${" + _collectionId + "}");
        setWidth("50");
    }

    public Object getDisplayValue(RenderContext ctx)
    {
        return getFormattedValue(ctx);
    }

    public Class getDisplayValueClass()
    {
        return String.class;
    }

    public String getFormattedValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        long collectionId;
        long groupNumber;
        if (_fromQuery)
        {
            if (row.get(_collectionIdColumn.getAlias()) == null)
            {
                return "";
            }
            collectionId = ((Number)row.get(_collectionIdColumn.getAlias())).longValue();
            groupNumber = ((Number)row.get(getColumnInfo().getAlias())).longValue();
        }
        else
        {
            if (row.get(_collectionId) == null)
            {
                return "";
            }
            collectionId = ((Number)row.get(_collectionId)).longValue();
            groupNumber = ((Number)row.get(_groupNumber)).longValue();
        }

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
            List<FieldKey> keys = new ArrayList<FieldKey>();

            FieldKey idiKey = new FieldKey(thisFieldKey.getTable(), "IndistinguishableCollectionId");
            keys.add(idiKey);
            FieldKey groupIdKey = new FieldKey(thisFieldKey.getTable(), "RowId");
            keys.add(groupIdKey);
            Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(getColumnInfo().getParentTable(), keys);

            _collectionIdColumn = cols.get(idiKey);
            columns.add(_collectionIdColumn);

            ColumnInfo groupIdCol = cols.get(groupIdKey);
            columns.add(groupIdCol);

            ViewURLHelper urlHelper = new ViewURLHelper("MS2", "showProteinGroup.view", _container);
            setURL(urlHelper.toString() + "&grouping=proteinprophet&proteinGroupId=${" + groupIdCol.getAlias() + "}");
        }
    }
}
