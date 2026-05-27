/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.elisa.query;

import org.labkey.api.data.Entity;

public class CurveFitDb extends Entity
{
    private Integer _rowId;
    private Long _runId;
    private Long _protocolId;
    private String _plateName;
    private Integer _spot;
    private Double _rSquared;
    private String _fitParameters;

    public boolean isNew()
    {
        return _rowId == null;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public void setRowId(Integer rowId)
    {
        _rowId = rowId;
    }

    public Long getRunId()
    {
        return _runId;
    }

    public void setRunId(Long runId)
    {
        _runId = runId;
    }

    public Long getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(Long protocolId)
    {
        _protocolId = protocolId;
    }

    public String getPlateName()
    {
        return _plateName;
    }

    public void setPlateName(String plateName)
    {
        _plateName = plateName;
    }

    public Integer getSpot()
    {
        return _spot;
    }

    public void setSpot(Integer spot)
    {
        _spot = spot;
    }

    public Double getrSquared()
    {
        return _rSquared;
    }

    public void setrSquared(Double rSquared)
    {
        _rSquared = rSquared;
    }

    public String getFitParameters()
    {
        return _fitParameters;
    }

    public void setFitParameters(String fitParameters)
    {
        _fitParameters = fitParameters;
    }
}
