/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.luminex;

import java.io.Serializable;

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
}
