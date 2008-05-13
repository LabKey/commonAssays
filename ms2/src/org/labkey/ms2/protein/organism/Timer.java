/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.ms2.protein.organism;

/**
 * User: arauch
 * Date: Jan 11, 2006
 * Time: 9:38:57 PM
 */
abstract public class Timer
{
    long _elapsedTime = 0;
    long _startTime;

    void resetTimer()
    {
        _elapsedTime = 0;
    }

    public float getElapsedTime()
    {
        return _elapsedTime / 1000;
    }

    void startTimer()
    {
        _startTime = System.currentTimeMillis();
    }

    void stopTimer()
    {
        _elapsedTime += System.currentTimeMillis() - _startTime;
    }
}
