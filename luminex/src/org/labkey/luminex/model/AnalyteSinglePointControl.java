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

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.query.FieldKey;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.model.AbstractLuminexControlAnalyte;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.model.SinglePointControl;
import org.labkey.luminex.query.AnalyteSinglePointControlTable;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.util.Collections;
import java.util.Map;

/**
 * Simple bean for mapping analytes to single point controls in the database
 * User: jeckels
 * Date: 8/23/13
 */
public class AnalyteSinglePointControl extends AbstractLuminexControlAnalyte
{
    private int _rowId;
    private int _singlePointControlId;

    public AnalyteSinglePointControl() {}

    public AnalyteSinglePointControl(Analyte analyte, SinglePointControl control)
    {
        setAnalyteId(analyte.getRowId());
        _singlePointControlId = control.getRowId();
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getSinglePointControlId()
    {
        return _singlePointControlId;
    }

    public void setSinglePointControlId(int singlePointControlId)
    {
        _singlePointControlId = singlePointControlId;
    }

    @Override
    public void updateQCFlags(LuminexProtocolSchema schema)
    {
        // get the run, isotype, conjugate, and analtye/single point control information in order to update QC Flags
        Analyte analyte = getAnalyteFromId();
        SinglePointControl control = getSinglePointControlFromId();
        ExpRun run = getRun(control.getRunId());
        Map<String, String> runIsotypeConjugate = getIsotypeAndConjugate(run);

        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Analyte"), analyte.getRowId());
        filter.addCondition(FieldKey.fromParts("SinglePointControl"), control.getRowId());
        AnalyteSinglePointControlTable analyteSinglePointControlTable = schema.createAnalyteSinglePointControlTable(true);
        analyteSinglePointControlTable.setContainerFilter(ContainerFilter.EVERYTHING);
        Double average = new TableSelector(analyteSinglePointControlTable, Collections.singleton("AverageFiBkgd"), filter, null).getObject(Double.class);

        LuminexDataHandler.insertOrUpdateAnalyteSinglePointControlQCFlags(schema.getUser(), run, schema.getProtocol(), this, analyte, control, runIsotypeConjugate.get("Isotype"), runIsotypeConjugate.get("Conjugate"), average);
    }

    public SinglePointControl getSinglePointControlFromId()
    {
        SinglePointControl result = new TableSelector(LuminexProtocolSchema.getTableInfoSinglePointControl()).getObject(getSinglePointControlId(), SinglePointControl.class);
        if (result == null)
        {
            throw new IllegalStateException("Unable to find referenced single point control: " + getSinglePointControlId());
        }
        return result;
    }
}
