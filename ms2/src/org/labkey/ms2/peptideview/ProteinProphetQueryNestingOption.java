package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class ProteinProphetQueryNestingOption extends QueryNestingOption
{
    private static final String PREFIX = "ProteinProphetData/ProteinGroupId/";
    private static final String PROTEIN_GROUP_ROWID = PREFIX + "RowId";

    public ProteinProphetQueryNestingOption(boolean allowNesting)
    {
        super(PROTEIN_GROUP_ROWID, allowNesting);
    }

    public int getOuterGroupLimit()
    {
        return _allowNesting ? 1000 : 0;
    }

    public int getResultSetRowLimit()
    {
        return _allowNesting ? 10000 : 0;
    }

    public boolean isOuter(String columnName)
    {
        return columnName.toLowerCase().startsWith(PREFIX.toLowerCase());
    }
}
