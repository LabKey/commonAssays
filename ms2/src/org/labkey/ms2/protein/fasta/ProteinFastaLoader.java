/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.protein.fasta.FastaProtein;
import org.labkey.api.reader.FastaLoader;

import java.io.File;

public class ProteinFastaLoader extends FastaLoader<FastaProtein> implements Iterable<FastaProtein>
{
    public ProteinFastaLoader(File fastaFile)
    {
        super(fastaFile, FastaProtein::new);
    }

    @Override
    public @NotNull ProteinIterator iterator()
    {
        return new ProteinIterator();
    }

    public class ProteinIterator extends FastaIterator
    {
        private ProteinIterator()
        {
        }
    }
}
