/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

package org.labkey.flow.analysis.util;

/**
 * User: matthewb
 * Date: Sep 13, 2007
 */
public interface RangeFunction
{
    double LOG_10_FACTOR = 1.0/Math.log(10);

    double getMin();
    double getMax();

    boolean isLogarithmic();
    double compute(double range);
    double invert(double domain);
}

