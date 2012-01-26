/*
 * Copyright (c) 2008-2012 LabKey Corporation
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
package org.labkey.ms2.protein.fasta;

import org.ardverk.collection.ByteArrayKeyAnalyzer;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.labkey.api.util.UnexpectedException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: adam
 * Date: Jan 12, 2008
 * Time: 7:43:36 PM
 */
public class FastaValidator
{
    private File _fastaFile;


    public FastaValidator(File fastaFile)
    {
        _fastaFile = fastaFile;
    }


    public List<String> validate()
    {
        // Use a trie to get space-efficient storage
        Map<byte[], Object> proteinNames = new PatriciaTrie<byte[], Object>(ByteArrayKeyAnalyzer.VARIABLE);
        List<String> errors = new ArrayList<String>();
        Format lineFormat = DecimalFormat.getIntegerInstance();
        ProteinFastaLoader curLoader = new ProteinFastaLoader(_fastaFile);

        try
        {
            //noinspection ForLoopReplaceableByForEach
            for (ProteinFastaLoader.ProteinIterator proteinIterator = curLoader.iterator(); proteinIterator.hasNext();)
            {
                Protein protein = (Protein)proteinIterator.next();

                // Use UTF-8 encoding so that we only use a single byte for ASCII characters
                String lookupString = protein.getLookup().toLowerCase();
                byte[] lookup = lookupString.getBytes("UTF-8");

                if (proteinNames.containsKey(lookup))
                {
                    errors.add("Line " + lineFormat.format(proteinIterator.getLastHeaderLineNum()) + ": " + lookupString + " is a duplicate protein name");

                    if (errors.size() > 999)
                    {
                        errors.add("Stopped validating after 1,000 errors");
                        break;
                    }
                }
                else
                {
                    proteinNames.put(lookup, null);
                }
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new UnexpectedException(e);
        }

        return errors;
    }
}
