/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

package org.labkey.api.protein.fasta;

import org.labkey.api.ms2.MS2Service;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.protein.ProteinAnnotationPipelineProvider;
import org.labkey.api.protein.ProteinManager;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;

public class FastaReloaderJob extends PipelineJob
{
    private int[] _fastaIds;

    // For serialization
    protected FastaReloaderJob() {}

    public FastaReloaderJob(int[] fastaIds, ViewBackgroundInfo info, PipeRoot root) throws IOException
    {
        super(ProteinAnnotationPipelineProvider.NAME, info, root);
        _fastaIds = fastaIds;
        setLogFile(FileUtil.createTempFile("FastaReload", ".log", AppProps.getInstance().getFileSystemRoot()));
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

    @Override
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

                    MS2Service service = MS2Service.get();
                    if (null != service)
                        service.migrateRuns(oldFastaId, fdbl.getFastaId());
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
