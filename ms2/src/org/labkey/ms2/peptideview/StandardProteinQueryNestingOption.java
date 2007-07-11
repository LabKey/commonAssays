package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class StandardProteinQueryNestingOption extends QueryNestingOption
{
    private static final String PREFIX = "SeqId/";
    private static final String PROTEIN_ROWID = PREFIX + "SeqId";

    public StandardProteinQueryNestingOption(boolean allowNesting)
    {
        super(PROTEIN_ROWID, allowNesting);
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
        return columnName.toLowerCase().startsWith(PREFIX.toLowerCase());
    }
}
