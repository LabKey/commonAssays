/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
package org.labkey.ms2;

/**
* User: jeckels
* Date: Aug 20, 2010
*/
public class IcatProteinQuantitation extends AbstractProteinQuantitation
{
    private float _ratioMean;
    private float _ratioStandardDev;
    private int _ratioNumberPeptides;
    private float _heavy2lightRatioMean;
    private float _heavy2lightRatioStandardDev;

    public float getRatioMean()
    {
        return _ratioMean;
    }

    public void setRatioMean(float ratioMean)
    {
        _ratioMean = ratioMean;
    }

    public float getRatioStandardDev()
    {
        return _ratioStandardDev;
    }

    public void setRatioStandardDev(float ratioStandardDev)
    {
        _ratioStandardDev = ratioStandardDev;
    }

    public int getRatioNumberPeptides()
    {
        return _ratioNumberPeptides;
    }

    public void setRatioNumberPeptides(int ratioNumberPeptides)
    {
        _ratioNumberPeptides = ratioNumberPeptides;
    }

    public float getHeavy2lightRatioMean()
    {
        return _heavy2lightRatioMean;
    }

    public void setHeavy2lightRatioMean(float heavy2lightRatioMean)
    {
        _heavy2lightRatioMean = heavy2lightRatioMean;
    }

    public float getHeavy2lightRatioStandardDev()
    {
        return _heavy2lightRatioStandardDev;
    }

    public void setHeavy2lightRatioStandardDev(float heavy2lightRatioStandardDev)
    {
        _heavy2lightRatioStandardDev = heavy2lightRatioStandardDev;
    }
}
