/*
 * Copyright (c) 2011-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.luminex.model;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.query.FieldKey;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Sep 6, 2011
 */
public class AnalyteTitration extends AbstractLuminexControlAnalyte
{
    private int _titrationId;
    private Double _maxFI;

    public int getTitrationId()
    {
        return _titrationId;
    }

    public void setTitrationId(int titrationId)
    {
        _titrationId = titrationId;
    }

    public Double getMaxFI()
    {
        return _maxFI;
    }

    public void setMaxFI(Double maxFI)
    {
        _maxFI = maxFI;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalyteTitration that = (AnalyteTitration) o;

        return (getAnalyteId() == that.getAnalyteId() && _titrationId == that._titrationId);
    }

    @Override
    public int hashCode()
    {
        int result = getAnalyteId();
        result = 31 * result + _titrationId;
        return result;
    }

    @Override
    public void updateQCFlags(LuminexProtocolSchema schema)
    {
        // get the run, isotype, conjugate, and analtye/titration curvefit information in order to update QC Flags
        Analyte analyte = getAnalyteFromId();
        Titration titration = getTitrationFromId();
        ExpRun run = getRun(titration.getRunId());
        Map<String, String> runIsotypeConjugate = getIsotypeAndConjugate(run);

        SimpleFilter curveFitFilter = new SimpleFilter(FieldKey.fromParts("AnalyteId"), getAnalyteId());
        curveFitFilter.addCondition(FieldKey.fromParts("TitrationId"), getTitrationId());
        List<CurveFit> curveFits = new TableSelector(LuminexProtocolSchema.getTableInfoCurveFit(), curveFitFilter, null).getArrayList(CurveFit.class);

        if (titration.isStandard() || titration.isQcControl())
            LuminexDataHandler.insertOrUpdateAnalyteTitrationQCFlags(schema.getUser(), run, schema.getProtocol(), this, analyte, titration, runIsotypeConjugate.get("Isotype"), runIsotypeConjugate.get("Conjugate"), curveFits);
    }

    public Titration getTitrationFromId()
    {
        Titration titration = new TableSelector(LuminexProtocolSchema.getTableInfoTitration()).getObject(getTitrationId(), Titration.class);
        if (titration == null)
        {
            throw new IllegalStateException("Unable to find referenced titration: " + getTitrationId());
        }
        return titration;
    }
}
