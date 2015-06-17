/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.elispot.plate;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.elispot.ElispotDataHandler;

/**
 * Created by klum on 3/23/2015.
 */
public class FluorescentPlateInfo implements PlateInfo
{
    private String _measurement;
    private String _analyte;

    private FluorescentPlateInfo(String measurement, String analyte)
    {
        _measurement = measurement;
        _analyte = analyte;
    }

    @NotNull
    @Override
    public String getMeasurement()
    {
        return _measurement;
    }

    @Nullable
    @Override
    public String getAnalyte()
    {
        return _analyte;
    }

    /**
     * Factory to parse annotations and create PlateInfo instances
     * @return
     */
    @Nullable
    public static FluorescentPlateInfo create(String annotation)
    {
        if (annotation != null)
        {
            String analyte = null;
            String measurement = null;

            int analyteIdx = annotation.indexOf(':');
            int parenIdx = annotation.indexOf(')');
            if (analyteIdx != -1 && parenIdx != -1)
            {
                analyte = annotation.substring(analyteIdx+1, parenIdx);
            }

            if (analyte != null)
            {
                String lcAnnotation = annotation.toLowerCase();

                if (lcAnnotation.contains("spots number"))
                    measurement = ElispotDataHandler.SFU_PROPERTY_NAME;
                else if (lcAnnotation.contains("activity"))
                    measurement = ElispotDataHandler.ACTIVITY_PROPERTY_NAME;
                else if (lcAnnotation.contains("intensity"))
                    measurement = ElispotDataHandler.INTENSITY_PROPERTY_NAME;
                else if (lcAnnotation.contains("av.size"))
                    measurement = ElispotDataHandler.SPOT_SIZE_PROPERTY_NAME;
            }

            if (!StringUtils.isBlank(analyte) && measurement != null)
                return new FluorescentPlateInfo(measurement, StringUtils.trim(analyte));
        }
        return null;
    }
}
