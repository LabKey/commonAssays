package org.labkey.nab.multiplate;

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.util.DateUtil;
import org.labkey.nab.DilutionSummary;
import org.labkey.nab.Luc5Assay;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabAssayRun;
import org.labkey.nab.NabMaterialKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/24/13
 */
public class SinglePlateDilutionNabAssayRun extends NabAssayRun
{
    protected List<Plate> _plates;
    private DilutionSummary[] _dilutionSummaries;

    public SinglePlateDilutionNabAssayRun(NabAssayProvider provider, ExpRun run, List<Plate> plates,
                                  User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        super(provider, run, user, cutoffs, renderCurveFitType);
        _plates = plates;

        Map<String, List<WellGroup>> sampleGroups = new LinkedHashMap<String, List<WellGroup>>();
        for (Plate plate : plates)
        {
            for (WellGroup sample : plate.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                String virusName = plate.getProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME).toString();
                String key = SinglePlateDilutionSamplePropertyHelper.getKey(virusName, sample.getName());
                List<WellGroup> groups = sampleGroups.get(key);
                if (groups == null)
                {
                    groups = new ArrayList<WellGroup>();
                    sampleGroups.put(key, groups);
                }
                groups.add(sample);
            }
        }
        List<DilutionSummary> dilutionSummaries = new ArrayList<DilutionSummary>();

        for (Map.Entry<String, List<WellGroup>> sample : sampleGroups.entrySet())
            dilutionSummaries.add(new MultiVirusDilutionSummary(this, sample.getValue(), null, _renderedCurveFitType));

        _dilutionSummaries = dilutionSummaries.toArray(new DilutionSummary[dilutionSummaries.size()]);
    }

    @Override
    public DilutionSummary[] getSummaries()
    {
        return _dilutionSummaries;
    }

    @Override
    public List<Plate> getPlates()
    {
        return Collections.unmodifiableList(_plates);
    }

    private static class MultiVirusDilutionSummary extends DilutionSummary
    {
        public MultiVirusDilutionSummary(Luc5Assay assay, List<WellGroup> sampleGroups, String lsid, DilutionCurve.FitType curveFitType)
        {
            super(assay, sampleGroups, lsid, curveFitType);
        }

        public NabMaterialKey getMaterialKey()
        {
            if (_materialKey == null)
            {
                WellGroup firstWellGroup = getFirstWellGroup();
                String specimenId = (String) firstWellGroup.getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
                Double visitId = (Double) firstWellGroup.getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
                String participantId = (String) firstWellGroup.getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
                Date visitDate = (Date) firstWellGroup.getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
                String virusName = (String) firstWellGroup.getProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME);

/*
                if (virusName != null)
                    specimenId = String.format("%s (%s)", specimenId, virusName);
*/

                _materialKey = new NabMaterialKey(specimenId, participantId, visitId, visitDate);
            }
            return _materialKey;
        }
    }
}
