/*
 * Copyright (c) 2005-2009 LabKey Corporation
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

package org.labkey.flow.analysis.data;

/**
 */
public class SubsetNumberArray implements NumberArray
    {
    NumberArray _array;
    IntArray _subset;
		
    public SubsetNumberArray(NumberArray array, IntArray subset)
        {
        _array = array;
        _subset = subset;
        }

    public Number get(int index)
        {
        return _array.get(_subset.getInt(index));
        }

    public double getDouble(int index)
        {
        return _array.getDouble(_subset.getInt(index));
        }

    public float getFloat(int index)
        {
        return _array.getFloat(_subset.getInt(index));
        }

    public int getInt(int index)
        {
        return _array.getInt(_subset.getInt(index));
        }

    public void set(int index, double value)
        {
        throw new UnsupportedOperationException();
        }

    public void set(int index, float value)
        {
        throw new UnsupportedOperationException();
        }

    public void set(int index, int value)
        {
        throw new UnsupportedOperationException();
        }

    public int size()
        {
        return _subset.size();
        }

    public NumberArray getArray()
        {
        return _array;
        }
		
    public IntArray getSubset()
        {
        return _subset;
        }
    }