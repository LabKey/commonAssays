/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

import java.util.Date;

/**
 * User: jeckels
 * Date: Dec 4, 2007
 */
public class AnnotationInsertion
{
    private int _insertId;
    private String _filename;
    private String _filetype;
    private String _comment;
    private Date _insertDate;
    private Date _changeDate;
    private int _mouthsful;
    private int _recordsProcessed;
    private Date _completionDate;
    private int _sequencesAdded;
    private int _annotationsAdded;
    private int _identifiersAdded;
    private int _organismsAdded;
    private Integer _mrmSize;
    private Integer _mrmSequencesAdded;
    private Integer _mrmAnnotationsAdded;
    private Integer _mrmIdentifiersAdded;
    private Integer _mrmOrganismsAdded;
    private String _defaultOrganism;
    private boolean _organismShouldBeGuessed;

    public int getInsertId()
    {
        return _insertId;
    }

    public void setInsertId(int insertId)
    {
        _insertId = insertId;
    }

    public String getFilename()
    {
        return _filename;
    }

    public void setFilename(String filename)
    {
        _filename = filename;
    }

    public String getFiletype()
    {
        return _filetype;
    }

    public void setFiletype(String filetype)
    {
        _filetype = filetype;
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(String comment)
    {
        _comment = comment;
    }

    public Date getInsertDate()
    {
        return _insertDate;
    }

    public void setInsertDate(Date insertDate)
    {
        _insertDate = insertDate;
    }

    public Date getChangeDate()
    {
        return _changeDate;
    }

    public void setChangeDate(Date changeDate)
    {
        _changeDate = changeDate;
    }

    public int getMouthsful()
    {
        return _mouthsful;
    }

    public void setMouthsful(int mouthsful)
    {
        _mouthsful = mouthsful;
    }

    public int getRecordsProcessed()
    {
        return _recordsProcessed;
    }

    public void setRecordsProcessed(int recordsProcessed)
    {
        _recordsProcessed = recordsProcessed;
    }

    public Date getCompletionDate()
    {
        return _completionDate;
    }

    public void setCompletionDate(Date completionDate)
    {
        _completionDate = completionDate;
    }

    public int getSequencesAdded()
    {
        return _sequencesAdded;
    }

    public void setSequencesAdded(int sequencesAdded)
    {
        _sequencesAdded = sequencesAdded;
    }

    public int getAnnotationsAdded()
    {
        return _annotationsAdded;
    }

    public void setAnnotationsAdded(int annotationsAdded)
    {
        _annotationsAdded = annotationsAdded;
    }

    public int getIdentifiersAdded()
    {
        return _identifiersAdded;
    }

    public void setIdentifiersAdded(int identifiersAdded)
    {
        _identifiersAdded = identifiersAdded;
    }

    public int getOrganismsAdded()
    {
        return _organismsAdded;
    }

    public void setOrganismsAdded(int organismsAdded)
    {
        _organismsAdded = organismsAdded;
    }

    public Integer getMrmSize()
    {
        return _mrmSize;
    }

    public void setMrmSize(Integer mrmSize)
    {
        _mrmSize = mrmSize;
    }

    public Integer getMrmSequencesAdded()
    {
        return _mrmSequencesAdded;
    }

    public void setMrmSequencesAdded(Integer mrmSequencesAdded)
    {
        _mrmSequencesAdded = mrmSequencesAdded;
    }

    public Integer getMrmAnnotationsAdded()
    {
        return _mrmAnnotationsAdded;
    }

    public void setMrmAnnotationsAdded(Integer mrmAnnotationsAdded)
    {
        _mrmAnnotationsAdded = mrmAnnotationsAdded;
    }

    public Integer getMrmIdentifiersAdded()
    {
        return _mrmIdentifiersAdded;
    }

    public void setMrmIdentifiersAdded(Integer mrmIdentifiersAdded)
    {
        _mrmIdentifiersAdded = mrmIdentifiersAdded;
    }

    public Integer getMrmOrganismsAdded()
    {
        return _mrmOrganismsAdded;
    }

    public void setMrmOrganismsAdded(Integer mrmOrganismsAdded)
    {
        _mrmOrganismsAdded = mrmOrganismsAdded;
    }

    public String getDefaultOrganism()
    {
        return _defaultOrganism;
    }

    public void setDefaultOrganism(String defaultOrganism)
    {
        _defaultOrganism = defaultOrganism;
    }

    public boolean isOrganismShouldBeGuessed()
    {
        return _organismShouldBeGuessed;
    }

    public void setOrganismShouldBeGuessed(boolean organismShouldBeGuessed)
    {
        _organismShouldBeGuessed = organismShouldBeGuessed;
    }
}
