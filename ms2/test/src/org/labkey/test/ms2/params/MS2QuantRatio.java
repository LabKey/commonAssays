/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
package org.labkey.test.ms2.params;

/**
 * QuantRatio class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
class MS2QuantRatio
{
    private double _ratio;
    private double _stddev;

    public MS2QuantRatio(double ratio, double stddev)
    {
        this._ratio = ratio;
        this._stddev = stddev;
    }

    public boolean isMatch(double ratio, double stddev)
    {
        if (Math.abs(this._ratio - ratio) > 0.0001)
            return false;
        if (Math.abs(this._stddev - stddev) > 0.0001)
            return false;
        return true;
    }

    public double getRatio()
    {
        return _ratio;
    }

    public double getStddev()
    {
        return _stddev;
    }
}
