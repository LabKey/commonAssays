/*
 * Copyright (c) 2007 LabKey Corporation
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
