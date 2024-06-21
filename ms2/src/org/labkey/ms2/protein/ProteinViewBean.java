package org.labkey.ms2.protein;

import org.labkey.api.protein.CoverageViewBean;
import org.labkey.ms2.MS2Run;

public class ProteinViewBean extends CoverageViewBean
{
    public static final String ALL_PEPTIDES_URL_PARAM = "allPeps";

    public MS2Run run = null;
    public Protein protein;
    public boolean showPeptides;
    public boolean enableAllPeptidesFeature;
}
