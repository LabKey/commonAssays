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
