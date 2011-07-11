/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.ms2.reader;

import org.labkey.api.util.massSpecDataFileType;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * User: jeckels
 * Date: May 8, 2006
 */
public abstract class AbstractMzxmlIterator implements SimpleScanIterator
{
    public static final int NO_SCAN_FILTER = 0;

    final int _msLevelFilter;

    AbstractMzxmlIterator(int msLevelFilter)
    {
        _msLevelFilter = msLevelFilter;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public static AbstractMzxmlIterator createParser(File file, int scanLevelFilter) throws IOException, XMLStreamException
    {
        if (massSpecDataFileType.isMZmlAvailable())
        {
            return new RandomAccessPwizMSDataIterator(file, scanLevelFilter);
        }
        else
        {
            return new SequentialMzxmlIterator(file, scanLevelFilter);
        }
    }
}
