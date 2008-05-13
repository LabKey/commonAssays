/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray.assay;

import java.io.*;

/**
 * User: jeckels
 * Date: Jan 3, 2008
 */
public class TrimmedFileInputStream extends InputStream
{
    private final InputStream _in;
    private int _currentOffset;
    private final int _endingOffset;

    public TrimmedFileInputStream(File file, int startingOffset, int endingOffset) throws IOException
    {
        _endingOffset = endingOffset;
        _in = new BufferedInputStream(new FileInputStream(file));
        _in.skip(startingOffset);
        _currentOffset = startingOffset;
    }

    public int read() throws IOException
    {
        if (_currentOffset < _endingOffset)
        {
            _currentOffset++;
            return _in.read();
        }
        return -1;
    }

    public void close() throws IOException
    {
        _in.close();
    }
}
