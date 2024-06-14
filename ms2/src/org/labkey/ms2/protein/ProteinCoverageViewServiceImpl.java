package org.labkey.ms2.protein;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.annotations.Migrate;
import org.labkey.api.protein.PeptideCharacteristic;
import org.labkey.api.protein.ProteinCoverageViewService;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.protein.Replicate;
import org.labkey.api.view.JspView;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Manager;

import java.util.List;
import java.util.function.Consumer;

@Migrate
public class ProteinCoverageViewServiceImpl implements ProteinCoverageViewService
{
    private WebPartView<?> getProteinCoverageView(int seqId, List<PeptideCharacteristic> peptideCharacteristics, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures, Consumer<ProteinViewBean> beanModifier)
    {
        ProteinViewBean bean = new ProteinViewBean();
        bean.protein = MS2Manager.getProtein(seqId);
        bean.protein.setShowEntireFragmentInCoverage(showEntireFragmentInCoverage);
        bean.protein.setCombinedPeptideCharacteristics(peptideCharacteristics);
        bean.features = ProteinService.get().getProteinFeatures(accessionForFeatures);
        bean.aaRowWidth = aaRowWidth;
        beanModifier.accept(bean);
        return new JspView<>("/org/labkey/ms2/protein/view/proteinCoverageMap.jsp", bean);
    }

    @Override
    public WebPartView<?> getProteinCoverageView(int seqId, List<PeptideCharacteristic> peptideCharacteristics, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures)
    {
        return getProteinCoverageView(seqId, peptideCharacteristics, aaRowWidth, showEntireFragmentInCoverage, accessionForFeatures, bean -> {});
    }

    @Override
    public WebPartView<?> getProteinCoverageViewWithSettings(int seqId, List<PeptideCharacteristic> peptideCharacteristics, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures, List<Replicate> replicates, List<PeptideCharacteristic> modifiedPeptideCharacteristics, boolean showStackedPeptides)
    {
        return getProteinCoverageView(seqId, peptideCharacteristics, aaRowWidth, showEntireFragmentInCoverage, accessionForFeatures, bean -> {
            bean.replicates = replicates;
            bean.showViewSettings = true;
            bean.protein.setModifiedPeptideCharacteristics(modifiedPeptideCharacteristics);
            bean.protein.setShowStakedPeptides(showStackedPeptides);
        });
    }
}
