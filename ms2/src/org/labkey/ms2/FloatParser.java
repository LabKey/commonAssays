/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
 * User: arauch
 * Date: Oct 26, 2005
 * Time: 6:31:26 PM
 */
public class FloatParser
{
    // Compute static array for exponents
    static double[] ee = new double[199];

    static
    {
        for (int i = -99; i < 99; i++)
            ee[i + 99] = Math.pow(10, i);
    }

    byte[] buffer;
    int length;
    int position;
    double next;
    boolean found = false;

    public FloatParser(byte[] buffer, int start, int length)
    {
        this.buffer = buffer;
        this.length = length;
        position = start;
    }


    public boolean hasNext()
    {
        next = readFloat();
        return found;
    }


    public double nextFloat()
    {
        return next;
    }


    public double readFloat()
    {
        char ch;
        double scale = 0;
        double d = 0;
        found = false;

        // skip white space
        while (position < length)
        {
            ch = (char) buffer[position];
            if (ch >= '0' && ch <= '9')
                break;
            position++;
        }

        // read decimal portion
        while (position < length)
        {
            ch = (char) buffer[position++];
            if (ch >= '0' && ch <= '9')
            {
                found = true;
                d = d * 10 + (ch - '0');
                scale *= 10;
                continue;
            }
            if (ch == '.')
            {
                scale = 1;
                continue;
            }

            // read exponent (if any)
            if (ch == 'e' || ch == 'E')
            {
                int e = 0;
                boolean negE = false;
                while (position < length)
                {
                    ch = (char) buffer[position++];
                    switch (ch)
                    {
                        case('0'):
                        case('1'):
                        case('2'):
                        case('3'):
                        case('4'):
                        case('5'):
                        case('6'):
                        case('7'):
                        case('8'):
                        case('9'):
                            e = e * 10 + (ch - '0');
                            continue;
                        case('-'):
                            negE = true;
                            continue;
                        case('+'):
                            continue;
                    }
                    scale /= ee[99 + (negE ? -e : e)];
                    break;
                }
            }
            break;
        }

        return (scale == 0 ? d : d / scale);
    }
}
