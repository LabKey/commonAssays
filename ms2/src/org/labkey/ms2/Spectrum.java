/*
 * Copyright (c) 2006-2016 LabKey Corporation
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
 * User: adam
 * Date: May 10, 2006
 * Time: 11:09:10 AM
 */
public interface Spectrum
{
    float[] getX();
    float[] getY();
    int getCharge();
    int getFraction();
    int getRun();
    double getPrecursorMass();
    double getMZ();
    double getScore(int index);
    Double getRetentionTime();
    String getSequence();
    String getTrimmedSequence();
    String getNextAA();
    String getPrevAA();
}
