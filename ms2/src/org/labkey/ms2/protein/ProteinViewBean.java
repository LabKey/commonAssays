package org.labkey.ms2.protein;

import org.labkey.api.protein.Replicate;
import org.labkey.api.protein.ProteinFeature;
import org.labkey.ms2.MS2Run;

import java.util.Collections;
import java.util.List;

public class ProteinViewBean
{
    public Protein protein;
    public boolean showPeptides;
    public MS2Run run;
    public String showRunUrl;
    public boolean enableAllPeptidesFeature;
    public boolean showViewSettings;
    public static final String ALL_PEPTIDES_URL_PARAM = "allPeps";
    public int aaRowWidth;
    public List<ProteinFeature> features = Collections.emptyList();
    public List<Replicate> replicates = Collections.emptyList();
}
