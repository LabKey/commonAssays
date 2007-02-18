package org.labkey.ms2.protein.fasta;

import java.util.Date;

/**
 * User: jeckels
 * Date: Feb 16, 2007
 */
public class FastaFile
{
    private int _fastaId;
    private String _filename;
    private Date _loaded;
    private String _fileChecksum;
    private boolean _scoringAnalysis;


    public int getFastaId()
    {
        return _fastaId;
    }

    public void setFastaId(int fastaId)
    {
        _fastaId = fastaId;
    }

    public String getFileChecksum()
    {
        return _fileChecksum;
    }

    public void setFileChecksum(String filechecksum)
    {
        _fileChecksum = filechecksum;
    }

    public String getFilename()
    {
        return _filename;
    }

    public void setFilename(String filename)
    {
        _filename = filename;
    }

    public Date getLoaded()
    {
        return _loaded;
    }

    public void setLoaded(Date loaded)
    {
        _loaded = loaded;
    }

    public boolean isScoringAnalysis()
    {
        return _scoringAnalysis;
    }

    public void setScoringAnalysis(boolean scoringAnalysis)
    {
        _scoringAnalysis = scoringAnalysis;
    }
}
