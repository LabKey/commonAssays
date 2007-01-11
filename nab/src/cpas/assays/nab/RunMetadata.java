package cpas.assays.nab;

import org.fhcrc.cpas.util.DateUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * User: brittp
 * Date: Aug 18, 2006
 * Time: 2:30:40 PM
 */
public class RunMetadata implements Serializable
{
    private static final long serialVersionUID = -8338877129594854953L;
    private String _virusName;
    private String _virusId;
    private String _hostCell;
    private String _studyName;
    private String _experimentDateString;
    private Date _experimentDate;
    private String _experimentPerformer;
    private String _experimentId;
    private String _fileId;
    private String _incubationTime;
    private String _plateNumber;

    public Date getExperimentDate()
    {
        return _experimentDate;
    }

    public void setExperimentDate(Date experimentDate)
    {
        _experimentDate = experimentDate;
    }

    public String getExperimentDateString()
    {
        if (_experimentDateString == null && _experimentDate != null)
            _experimentDateString = DateUtil.formatDate(_experimentDate);
        return _experimentDateString;
    }

    public void setExperimentDateString(String experimentDateString)
    {
        _experimentDateString = experimentDateString;
    }

    public String getExperimentId()
    {
        return _experimentId;
    }

    public void setExperimentId(String experimentId)
    {
        _experimentId = experimentId;
    }

    public String getExperimentPerformer()
    {
        return _experimentPerformer;
    }

    public void setExperimentPerformer(String experimentPerformer)
    {
        _experimentPerformer = experimentPerformer;
    }

    public String getFileId()
    {
        return _fileId;
    }

    public void setFileId(String fileId)
    {
        _fileId = fileId;
    }

    public String getHostCell()
    {
        return _hostCell;
    }

    public void setHostCell(String hostCell)
    {
        _hostCell = hostCell;
    }

    public String getIncubationTime()
    {
        return _incubationTime;
    }

    public void setIncubationTime(String incubationTime)
    {
        _incubationTime = incubationTime;
    }

    public String getPlateNumber()
    {
        return _plateNumber;
    }

    public void setPlateNumber(String plateNumber)
    {
        _plateNumber = plateNumber;
    }

    public String getStudyName()
    {
        return _studyName;
    }

    public void setStudyName(String studyName)
    {
        _studyName = studyName;
    }

    public String getVirusId()
    {
        return _virusId;
    }

    public void setVirusId(String virusId)
    {
        _virusId = virusId;
    }

    public String getVirusName()
    {
        return _virusName;
    }

    public void setVirusName(String virusName)
    {
        _virusName = virusName;
    }
}
