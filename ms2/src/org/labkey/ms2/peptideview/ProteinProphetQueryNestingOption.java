package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class ProteinProphetQueryNestingOption extends QueryNestingOption
{
    private static final String PREFIX = "ProteinProphetData/ProteinGroupId/";
    private static final String PROTEIN_GROUP_ROWID = PREFIX + "RowId";

    public ProteinProphetQueryNestingOption()
    {
        super(PROTEIN_GROUP_ROWID);
    }

    public int getOuterGroupLimit()
    {
        return 1000;
    }

    public int getResultSetRowLimit()
    {
        return 10000;
    }

    public boolean isOuter(String columnName)
    {
        return columnName.toLowerCase().startsWith(PREFIX.toLowerCase());
    }
}
