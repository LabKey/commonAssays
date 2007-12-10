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
    private int _mrmSize;
    private int _mrmSequencesAdded;
    private int _mrmAnnotationsAdded;
    private int _mrmIdentifiersAdded;
    private int _mrmOrganismsAdded;
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

    public int getMrmSize()
    {
        return _mrmSize;
    }

    public void setMrmSize(int mrmSize)
    {
        _mrmSize = mrmSize;
    }

    public int getMrmSequencesAdded()
    {
        return _mrmSequencesAdded;
    }

    public void setMrmSequencesAdded(int mrmSequencesAdded)
    {
        _mrmSequencesAdded = mrmSequencesAdded;
    }

    public int getMrmAnnotationsAdded()
    {
        return _mrmAnnotationsAdded;
    }

    public void setMrmAnnotationsAdded(int mrmAnnotationsAdded)
    {
        _mrmAnnotationsAdded = mrmAnnotationsAdded;
    }

    public int getMrmIdentifiersAdded()
    {
        return _mrmIdentifiersAdded;
    }

    public void setMrmIdentifiersAdded(int mrmIdentifiersAdded)
    {
        _mrmIdentifiersAdded = mrmIdentifiersAdded;
    }

    public int getMrmOrganismsAdded()
    {
        return _mrmOrganismsAdded;
    }

    public void setMrmOrganismsAdded(int mrmOrganismsAdded)
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
