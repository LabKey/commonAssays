package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 11, 2007
 */
public class ProteinGroupQueryNestingOption extends QueryNestingOption
{
    public ProteinGroupQueryNestingOption(boolean allowNesting)
    {
        super("RowId", allowNesting);
    }
    
    public int getOuterGroupLimit()
    {
        return _allowNesting ? 250 : 0;
    }

    public int getResultSetRowLimit()
    {
        return _allowNesting ? 15000 : 0;
    }

    public boolean isOuter(String columnName)
    {
        return columnName.indexOf("/") < 0;
    }
}
