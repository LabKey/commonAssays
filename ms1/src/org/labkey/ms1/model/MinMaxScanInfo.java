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
 * Simple bean to hold info about the real min/max scan and retention time within a scan range
 * Used by the feature detail charts
 * User: Dave
 * Date: Oct 25, 2007
 * Time: 1:56:59 PM
 */
public class MinMaxScanInfo
{
    private int _minScan = 0;
    private int _maxScan = 0;
    private double _minRetentionTime = 0;
    private double _maxRetentionTime = 0;

    public int getMinScan()
    {
        return _minScan;
    }

    public void setMinScan(int minScan)
    {
        _minScan = minScan;
    }

    public int getMaxScan()
    {
        return _maxScan;
    }

    public void setMaxScan(int maxScan)
    {
        _maxScan = maxScan;
    }

    public double getMinRetentionTime()
    {
        return _minRetentionTime;
    }

    public void setMinRetentionTime(double minRetentionTime)
    {
        _minRetentionTime = minRetentionTime;
    }

    public double getMaxRetentionTime()
    {
        return _maxRetentionTime;
    }

    public void setMaxRetentionTime(double maxRetentionTime)
    {
        _maxRetentionTime = maxRetentionTime;
    }
}
