package org.labkey.nab.multiplate;

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.WellGroup;
import org.labkey.nab.DilutionSummary;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabAssayRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 2/24/13
 */
public class CrossPlateDilutionNabAssayRun extends NabAssayRun
{
    protected List<Plate> _plates;
    private DilutionSummary[] _dilutionSummaries;

    public CrossPlateDilutionNabAssayRun(NabAssayProvider provider, ExpRun run, List<Plate> plates,
                                         User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        super(provider, run, user, cutoffs, renderCurveFitType);
        _plates = plates;

        int sampleCount = plates.get(0).getWellGroupCount(WellGroup.Type.SPECIMEN);
        _dilutionSummaries = new DilutionSummary[sampleCount];
        Map<String, List<WellGroup>> sampleGroups = new LinkedHashMap<String, List<WellGroup>>();
        for (Plate plate : plates)
        {
            for (WellGroup sample : plate.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                List<WellGroup> groups = sampleGroups.get(sample.getName());
                if (groups == null)
                {
                    groups = new ArrayList<WellGroup>();
                    sampleGroups.put(sample.getName(), groups);
                }
                groups.add(sample);
            }
        }
        int index = 0;
        for (Map.Entry<String, List<WellGroup>> sample : sampleGroups.entrySet())
            _dilutionSummaries[index++] = new DilutionSummary(this, sample.getValue(), null, _renderedCurveFitType);
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
}
