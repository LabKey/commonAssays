package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class StandardProteinQueryNestingOption extends QueryNestingOption
{
    private static final String PREFIX = "SeqId/";
    private static final String PROTEIN_ROWID = PREFIX + "SeqId";

    public StandardProteinQueryNestingOption()
    {
        super(PROTEIN_ROWID);
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
        return columnName.toLowerCase().startsWith(PREFIX.toLowerCase());
    }
}
