/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.nab.qc;

import org.labkey.api.exp.ExpQCFlag;
import org.labkey.nab.NabAssayController;

/**
 * Created by klum on 12/23/2016.
 */
public class NabWellQCFlag extends ExpQCFlag
{
    public static final String FLAG_TYPE = "Well QC";

    public NabWellQCFlag(){}

    public NabWellQCFlag(int runId, NabAssayController.WellExclusion exclusion)
    {
        super(runId, FLAG_TYPE, getDescription(exclusion));
        setComment(exclusion.getComment());
        setEnabled(true);
        setKey1(exclusion.getKey());
    }

    private static String getDescription(NabAssayController.WellExclusion exclusion)
    {
        return String.format("Well group name : %s, location : %s%s plate: %s", exclusion.getSpecimen(), exclusion.getRowLabel(), exclusion.getCol(), exclusion.getPlate());
    }
}
