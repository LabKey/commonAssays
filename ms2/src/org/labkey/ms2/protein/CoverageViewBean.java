package org.labkey.ms2.protein;

import org.labkey.api.protein.ProteinFeature;
import org.labkey.api.protein.Replicate;
import org.labkey.ms2.MS2Run;

import java.util.Collections;
import java.util.List;

public class CoverageViewBean
{
    public CoverageProtein coverageProtein;
    public MS2Run run = null;
    public String showRunUrl;
    public boolean showViewSettings;
    public int aaRowWidth;
    public List<ProteinFeature> features = Collections.emptyList();
    public List<Replicate> replicates = Collections.emptyList();
}
