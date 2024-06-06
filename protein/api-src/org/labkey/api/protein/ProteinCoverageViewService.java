package org.labkey.api.protein;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.annotations.Migrate;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.view.WebPartView;

import java.util.List;

@Migrate // These methods should be implemented in the Protein module, but they haven't been extricated from MS2 yet
public interface ProteinCoverageViewService
{
    static ProteinCoverageViewService get()
    {
        return ServiceRegistry.get().getService(ProteinCoverageViewService.class);
    }

    static void setInstance(ProteinCoverageViewService impl)
    {
        ServiceRegistry.get().registerService(ProteinCoverageViewService.class, impl);
    }

    /** @param aaRowWidth the number of amino acids to display in a single row */
    WebPartView<?> getProteinCoverageView(int seqId, List<PeptideCharacteristic> peptideCharacteristics, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures);

    WebPartView<?> getProteinCoverageViewWithSettings(int seqId, List<PeptideCharacteristic> combinedPeptideCharacteristics, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures, List<Replicate> replicates, List<PeptideCharacteristic> modifiedPeptideCharacteristics, boolean showStackedPeptides);
}
