/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;

/**
 * User: jeckels
 * Date: Oct 19, 2009
 */
public class UnknownMS2Run extends MS2Run
{
    public String getChargeFilterColumnName()
    {
        return null;
    }

    public String getChargeFilterParamName()
    {
        return null;
    }

    public String getDiscriminateExpressions()
    {
        return null;
    }

    public String[] getGZFileExtensions()
    {
        return new String[0];
    }

    public String getParamsFileName()
    {
        return null;
    }

    public MS2RunType getRunType()
    {
        return MS2RunType.Unknown;
    }
}
