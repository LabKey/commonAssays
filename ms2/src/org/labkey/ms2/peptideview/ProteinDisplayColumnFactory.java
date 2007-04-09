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
    public static final ProteinDisplayColumnFactory INSTANCE = new ProteinDisplayColumnFactory();

    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new ProteinDisplayColumn(colInfo);
    }
}
