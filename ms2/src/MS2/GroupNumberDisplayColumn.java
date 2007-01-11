package MS2;

import org.labkey.api.data.DataColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ViewURLHelper;

import java.util.Map;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class GroupNumberDisplayColumn extends DataColumn
{
    private final String _groupNumber;
    private final String _collectionId;

    public GroupNumberDisplayColumn(ColumnInfo col, ViewURLHelper url)
    {
        this(col, url, "GroupNumber", "IndistinguishableCollectionId");
    }

    public GroupNumberDisplayColumn(ColumnInfo col, ViewURLHelper url, String groupNumber, String collectionId)
    {
        super(col);
        _groupNumber = groupNumber;
        _collectionId = collectionId;
        ViewURLHelper urlHelper = url.clone();
        urlHelper.setAction("showProteinGroup.view");
        setURL(urlHelper.toString() + "&groupNumber=${" + _groupNumber + "}&indistinguishableCollectionId=${" + _collectionId + "}");
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
}
