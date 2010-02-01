/*
 * Copyright (c) 2005-2010 LabKey Corporation
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

import junit.framework.Assert;

import java.io.IOException;

/**
 * changed this to an abstract class to allow use of pwiz for mzML and gzipped mzXML
 * see RandomAccessJrapMzxmlIterator for original implementation
 * - bpratt
 */
public abstract class RandomAccessMzxmlIterator extends AbstractMzxmlIterator
{
    RandomAccessMzxmlIterator(int msLevelFilter)
    {
        super(msLevelFilter);
    }

    // helper for unit testing of child classes
    static protected void compare_mzxml(junit.framework.TestCase test, RandomAccessMzxmlIterator mzxmlA,RandomAccessMzxmlIterator mzxmlB)
    {
        try
        {
            while (mzxmlA.hasNext() && mzxmlB.hasNext())
            {
                SimpleScan scanA = mzxmlA.next();
                SimpleScan scanB = mzxmlB.next();
                test.assertEquals(scanA.getScan(),scanB.getScan());
                float [][]dataA = scanA.getData();
                float [][]dataB = scanB.getData();
                // expect that mz and intensity counts are the same all around
                test.assertEquals(dataA[0].length,dataB[1].length);
                test.assertEquals(dataA[1].length,dataB[0].length);
                for (int i=0;i < dataA[0].length; i++)
                {
                    test.assertEquals(dataA[0][i],dataB[0][i]);
                    test.assertEquals(dataA[1][i],dataB[1][i]);
                }
            }
            test.assertEquals("files should have same scan counts",mzxmlA.hasNext(), mzxmlB.hasNext());
        }
        catch (IOException e)
        {
            test.fail(e.toString());
        }
        finally {
        }
    }

}
