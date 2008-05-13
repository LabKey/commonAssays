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

package org.labkey.ms2.protein;

import org.labkey.ms2.protein.fasta.FastaFile;
import org.labkey.api.data.Table;

import java.io.File;

/**
 * User: jeckels
* Date: Feb 12, 2008
*/
class FastaReloader implements Runnable
{
    private int[] _fastaIds;

    FastaReloader(int[] fastaIds)
    {
        _fastaIds = fastaIds;
    }

    public void run()
    {
        for (int oldFastaId : _fastaIds)
        {
            try
            {
                // Update the SeqIds in ProteinSequences table for a previously loaded FASTA file.  This is to help fix up
                // null SeqIds that, up until CPAS 1.4, occurred when a single mouthful contained two or more identical
                // sequences.
                FastaFile fasta = Table.selectObject(ProteinManager.getTableInfoFastaFiles(), oldFastaId, FastaFile.class);
                String filename = fasta.getFilename();

                FastaDbLoader fdbl = new FastaDbLoader(new File(filename));
                fdbl.setComment(new java.util.Date() + " " + filename);
                fdbl.setDefaultOrganism(FastaDbLoader.UNKNOWN_ORGANISM);
                fdbl.setOrganismIsToGuessed(true);
                fdbl.parseFile();

                ProteinManager.migrateRuns(oldFastaId, fdbl.getFastaId());
                ProteinManager.deleteFastaFile(oldFastaId);
            }
            catch(Exception e)
            {
                DefaultAnnotationLoader._log.error("Exception while updating SeqIds for FASTA id " + oldFastaId, e);
            }
        }
    }

}
