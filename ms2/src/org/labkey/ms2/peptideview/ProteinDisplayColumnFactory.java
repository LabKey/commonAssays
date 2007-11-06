package org.labkey.ms2.peptideview;

import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.ms2.ProteinDisplayColumn;

/**
 * User: jeckels
* Date: Apr 6, 2007
*/
public class ProteinDisplayColumnFactory implements DisplayColumnFactory
{
    private final String _url;

    public ProteinDisplayColumnFactory()
    {
        this(null);
    }

    public ProteinDisplayColumnFactory(String url)
    {
        _url = url;
    }

    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        ProteinDisplayColumn result = new ProteinDisplayColumn(colInfo);
        if (_url != null)
        {
            result.setURL(_url);
        }
        return result;
    }
}
