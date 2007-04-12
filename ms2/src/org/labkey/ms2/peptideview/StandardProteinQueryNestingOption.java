package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class StandardProteinQueryNestingOption extends QueryNestingOption
{
    private static final String PROTEIN_PREFIX = "SeqId/";
    private static final String PROTEIN_ROWID = PROTEIN_PREFIX + "SeqId";

    public StandardProteinQueryNestingOption()
    {
        super(PROTEIN_PREFIX, PROTEIN_ROWID);
    }

    public void calculateValues()
    {

    }
}
