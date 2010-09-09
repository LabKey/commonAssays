package org.labkey.nab;

import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2010 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * User: brittp
 * Date: Sep 2, 2010 11:43:49 AM
 */
public class SinglePlateNabAssayRun extends NabAssayRun
{
    protected Plate _plate;
    private DilutionSummary[] _dilutionSummaries;

    public SinglePlateNabAssayRun(NabAssayProvider provider, ExpRun run, Plate plate,
                                  User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        super(provider, run, user, cutoffs, renderCurveFitType);
        _plate = plate;
        List<? extends WellGroup> specimenGroups = _plate.getWellGroups(WellGroup.Type.SPECIMEN);
        _dilutionSummaries = getDilutionSumariesForWellGroups(specimenGroups);
    }

    @Override
    public DilutionSummary[] getSummaries()
    {
        return _dilutionSummaries;
    }

    @Override
    public List<Plate> getPlates()
    {
        return Collections.singletonList(_plate);
    }
}
