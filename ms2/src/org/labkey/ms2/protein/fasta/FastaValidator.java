/*
 * Copyright (c) 2009-2019 LabKey Corporation
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

import org.apache.commons.collections4.trie.PatriciaTrie;

import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;

public class FastaValidator
{
    /** Use a trie to get space-efficient storage */
    private final PatriciaTrie<String> _proteinNames = new PatriciaTrie<>();

    public FastaValidator()
    {
    }

    /** Determine if FASTA file has any duplicate protein names **/
    public List<String> validate(File fastaFile)
    {
        List<String> errors = new ArrayList<>();
        Format lineFormat = DecimalFormat.getIntegerInstance();
        ProteinFastaLoader curLoader = new ProteinFastaLoader(fastaFile);

        for (ProteinFastaLoader.ProteinIterator proteinIterator = curLoader.iterator(); proteinIterator.hasNext();)
        {
            FastaProtein protein = proteinIterator.next();
            String lookupString = protein.getLookup().toLowerCase();

            if (_proteinNames.containsKey(lookupString))
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
                _proteinNames.put(lookupString, null);
            }
        }

        return errors;
    }
}
