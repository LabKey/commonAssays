package org.labkey.nab.multiplate;

import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabDataHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/24/13
 */
public class SinglePlateDilutionNabAssayProvider extends HighThroughputNabAssayProvider
{
    private static final String NAB_RUN_LSID_PREFIX = "SinglePlateDilutionNabAssayRun";
    private static final String NAB_ASSAY_PROTOCOL = "SinglePlateDilutionNabAssayProtocol";

    public SinglePlateDilutionNabAssayProvider()
    {
        super(NAB_ASSAY_PROTOCOL, NAB_RUN_LSID_PREFIX, SinglePlateDilutionNabDataHandler.SINGLE_PLATE_DILUTION_DATA_TYPE);
    }

    @Override
    public String getName()
    {
        return "TZM-bl Neutralization (NAb), High-throughput (Single Plate Dilution)";
    }

    @Override
    public String getResourceName()
    {
        return "SinglePlateDilutionNAb";
    }

    public String getDescription()
    {
        return "Imports a specially formatted CSV or XLS file that contains data from multiple plates.  This high-throughput NAb " +
                "assay differs from the standard NAb assay in that samples are identical across plates but with a different virus per plate. " +
                "Dilutions are assumed to occur within a single plate.  Both NAb assay types measure neutralization in TZM-bl cells as a function of a " +
                "reduction in Tat-induced luciferase (Luc) reporter gene expression after a single round of infection. Montefiori, D.C. 2004" +
                PageFlowUtil.helpPopup("NAb", "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/18432938\">" +
                        "Evaluating neutralizing antibodies against HIV, SIV and SHIV in luciferase " +
                        "reporter gene assays</a>.  Current Protocols in Immunology, (Coligan, J.E., " +
                        "A.M. Kruisbeek, D.H. Margulies, E.M. Shevach, W. Strober, and R. Coico, eds.), John Wiley & Sons, 12.11.1-12.11.15.", true);
    }

    public NabDataHandler getDataHandler()
    {
        return new SinglePlateDilutionNabDataHandler();
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createSampleWellGroupDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createSampleWellGroupDomain(c, user);
        Domain sampleWellGroupDomain = result.getKey();

        addProperty(sampleWellGroupDomain, NabAssayProvider.VIRUS_NAME_PROPERTY_NAME, NabAssayProvider.VIRUS_NAME_PROPERTY_NAME, PropertyType.STRING).setRequired(true);
        return result;
    }

    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();

        if (!domainMap.containsKey(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP))
            domainMap.put(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP, new HashSet<String>());

        Set<String> sampleProperties = domainMap.get(NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);

        sampleProperties.add(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME);

        return domainMap;
    }

    @Override
    protected PlateSamplePropertyHelper createSampleFilePropertyHelper(Container c, ExpProtocol protocol, DomainProperty[] sampleProperties, PlateTemplate template)
    {
        return new SinglePlateDilutionSamplePropertyHelper(c, protocol, sampleProperties, template);
    }
}
