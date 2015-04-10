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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by klum on 3/23/2015.
 */
public interface PlateInfo
{
    /**
     * Returns the type of measurement the corresponding plate contains
     * @return the measurement name
     */
    @NotNull
    public String getMeasurement();

    /**
     * Returns the name of the analyte used for a corresponding plate data
     * @return the name of the analyte
     */
    @Nullable
    public String getAnalyte();
}
