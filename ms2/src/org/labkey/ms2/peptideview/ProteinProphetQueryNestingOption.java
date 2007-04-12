package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class ProteinProphetQueryNestingOption extends QueryNestingOption
{
    private static final String PROTEIN_GROUP_PREFIX = "ProteinProphetData/ProteinGroupId/";
    private static final String PROTEIN_GROUP_ROWID = PROTEIN_GROUP_PREFIX + "RowId";

    public ProteinProphetQueryNestingOption()
    {
        super(PROTEIN_GROUP_PREFIX, PROTEIN_GROUP_ROWID);
    }


    public void calculateValues()
    {

    }
}
