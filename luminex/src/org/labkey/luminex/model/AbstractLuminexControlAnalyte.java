/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jeckels
 * Date: 8/23/13
 */
public abstract class AbstractLuminexControlAnalyte implements Serializable
{
    private int _analyteId;
    private Integer _guideSetId;
    private boolean _includeInGuideSetCalculation;

    public int getAnalyteId()
    {
        return _analyteId;
    }

    public void setAnalyteId(int analyteId)
    {
        _analyteId = analyteId;
    }

    public void setGuideSetId(Integer guideSetId)
    {
        _guideSetId = guideSetId;
    }

    public Integer getGuideSetId()
    {
        return _guideSetId;
    }

    public boolean isIncludeInGuideSetCalculation()
    {
        return _includeInGuideSetCalculation;
    }

    public void setIncludeInGuideSetCalculation(boolean includeInGuideSetCalculation)
    {
        _includeInGuideSetCalculation = includeInGuideSetCalculation;
    }

    public abstract void updateQCFlags(LuminexProtocolSchema schema);

    public Analyte getAnalyteFromId()
    {
        Analyte analyte = new TableSelector(LuminexProtocolSchema.getTableInfoAnalytes()).getObject(getAnalyteId(), Analyte.class);
        if (analyte == null)
        {
            throw new IllegalStateException("Unable to find referenced analyte: " + getAnalyteId());
        }
        return analyte;
    }

    public ExpRun getRun(int rowId)
    {
        ExpRun run = ExperimentService.get().getExpRun(rowId);
        if (run == null)
        {
            throw new IllegalStateException("Unable to find referenced run: " + rowId);
        }
        return run;
    }

    public Map<String, String> getIsotypeAndConjugate(ExpRun run)
    {
        Map<String, String> isotypeConjugate = new HashMap<>();
        isotypeConjugate.put("Isotype", null);
        isotypeConjugate.put("Conjugate", null);
        Map<String, ObjectProperty> runProps = run.getObjectProperties();
        for (ObjectProperty property : runProps.values())
        {
            if (property.getName().equalsIgnoreCase("Isotype"))
            {
                isotypeConjugate.put("Isotype", property.getStringValue());
            }
            if (property.getName().equalsIgnoreCase("Conjugate"))
            {
                isotypeConjugate.put("Conjugate", property.getStringValue());
            }
        }
        return isotypeConjugate;
    }
}
