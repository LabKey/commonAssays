package org.labkey.ms2.reader;

import org.labkey.ms2.PepXmlImporter;

import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Aug 16, 2011
 */
public abstract class AbstractQuantAnalysisResult extends PepXmlAnalysisResultHandler.PepXmlAnalysisResult
{
    private long peptideId;
    private int quantId;

    public int getQuantId()
    {
        return quantId;
    }

    public void setQuantId(int quantId)
    {
        this.quantId = quantId;
    }

    public long getPeptideId()
    {
        return peptideId;
    }

    public void setPeptideId(long peptideId)
    {
        this.peptideId = peptideId;
    }

    public abstract void insert(PepXmlImporter pepXmlImporter) throws SQLException;
}
