/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.ms1.model;

/**
 * Represents an MS1 Scan
 * User: Dave
 * Date: Oct 16, 2007
 * Time: 3:01:26 PM
 */
public class Scan
{
    public int getScanId()
    {
        return _scanId;
    }

    public void setScanId(int scanId)
    {
        _scanId = scanId;
    }

    public int getFileId()
    {
        return _fileId;
    }

    public void setFileId(int fileId)
    {
        _fileId = fileId;
    }

    public int getScan()
    {
        return _scan;
    }

    public void setScan(int scan)
    {
        _scan = scan;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Double getObservationDuration()
    {
        return _observationDuration;
    }

    public void setObservationDuration(Double observationDuration)
    {
        _observationDuration = observationDuration;
    }

    public String toString()
    {
        return "Scan (id=" + _scanId + ", scan=" + _scan + ")";
    }

    private int _scanId = -1;
    private int _fileId = -1;
    private int _scan = -1;
    private Double _retentionTime;
    private Double _observationDuration;
}
