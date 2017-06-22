/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.protein.fasta.FastaFile;

import java.io.File;
import java.io.IOException;

/**
 * User: jeckels
* Date: Feb 12, 2008
*/
public class FastaReloaderJob extends PipelineJob
{
    private int[] _fastaIds;

    public FastaReloaderJob(int[] fastaIds, ViewBackgroundInfo info, PipeRoot root) throws IOException
    {
        super(ProteinAnnotationPipelineProvider.NAME, info, root);
        _fastaIds = fastaIds;
        setLogFile(File.createTempFile("FastaReload", ".log", AppProps.getInstance().getFileSystemRoot()));
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "FASTA reload of " + _fastaIds.length + " files";
    }

    public void run()
    {
        info("Starting to reload FASTAs");
        for (int oldFastaId : _fastaIds)
        {
            try
            {
                info("Processing FASTA with ID " + oldFastaId);
                // Update the SeqIds in ProteinSequences table for a previously loaded FASTA file.  This is to help fix up
                // null SeqIds that, up until CPAS 1.4, occurred when a single mouthful contained two or more identical
                // sequences.
                FastaFile fasta = ProteinManager.getFastaFile(oldFastaId);
                if (fasta != null)
                {
                    String filename = fasta.getFilename();
                    info("Processing FASTA " + filename);

                    FastaDbLoader fdbl = new FastaDbLoader(new File(filename), getInfo(), getPipeRoot());
                    fdbl.setComment(new java.util.Date() + " " + filename);
                    fdbl.setDefaultOrganism(FastaDbLoader.UNKNOWN_ORGANISM);
                    fdbl.setOrganismIsToGuessed(true);
                    fdbl.parseFile(getLogger());

                    ProteinManager.migrateRuns(oldFastaId, fdbl.getFastaId());
                    ProteinManager.deleteFastaFile(oldFastaId);
                    info("Completed processing FASTA " + filename);
                }
                else
                {
                    error("Could not find FASTA id " + oldFastaId);
                    setStatus(TaskStatus.error);
                    return;
                }
            }
            catch(Exception e)
            {
                error("Exception while updating SeqIds for FASTA id " + oldFastaId, e);
                setStatus(TaskStatus.error);
                return;
            }
            info("Completed successfully");
            setStatus(TaskStatus.complete);
        }
    }

}
