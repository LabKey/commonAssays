package org.labkey.ms1.model;

import org.labkey.ms1.model.SoftwareParam;
import org.labkey.ms1.MS1Manager;

import java.sql.SQLException;

/**
 * Represents information about a software package used to produce an MS1 data file
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 10, 2007
 * Time: 1:53:24 PM
 */
public class Software
{
    public SoftwareParam[] getParameters() throws SQLException
    {
        if(_softwareId < 0)
            return new SoftwareParam[0];
        else
            return MS1Manager.get().getSoftwareParams(_softwareId);
    }

    public int getSoftwareId()
    {
        return _softwareId;
    }

    public void setSoftwareId(int softwareId)
    {
        _softwareId = softwareId;
    }

    public int getFileId()
    {
        return _fileId;
    }

    public void setFileId(int fileId)
    {
        _fileId = fileId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getVersion()
    {
        return _version;
    }

    public void setVersion(String version)
    {
        _version = version;
    }

    public String getAuthor()
    {
        return _author;
    }

    public void setAuthor(String author)
    {
        _author = author;
    }

    protected int _softwareId = -1;
    protected int _fileId = -1;
    protected String _name;
    protected String _version;
    protected String _author;
}
