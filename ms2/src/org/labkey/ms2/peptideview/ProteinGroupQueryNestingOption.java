package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 11, 2007
 */
public class ProteinGroupQueryNestingOption extends QueryNestingOption
{
    public ProteinGroupQueryNestingOption()
    {
        super("RowId");
    }
    
    public int getOuterGroupLimit()
    {
        return 250;
    }

    public int getResultSetRowLimit()
    {
        return 15000;
    }

    public boolean isOuter(String columnName)
    {
        return columnName.indexOf("/") < 0;
    }
}
