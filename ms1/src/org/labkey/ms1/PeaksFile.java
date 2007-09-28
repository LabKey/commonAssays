package org.labkey.ms1;

/**
 * Represents a particular Peaks file
 *
 * Created by IntelliJ IDEA.
 * User: DaveS
 * Date: Sep 25, 2007
 * Time: 2:44:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeaksFile extends DbBean
{
    public PeaksFile() {}

    public PeaksFile(int expDataFileID, String mzXmlURL)
    {
        _expDataFileID = expDataFileID;
        _mzXmlURL = mzXmlURL;
        setDirty(true);
    }

    public Object getRowID()
    {
        return _peaksFileID;
    }

    public int getPeaksFileID()
    {
        return _peaksFileID;
    }

    public void setPeaksFileID(int peaksFileID)
    {
        _peaksFileID = peaksFileID;
        setDirty(true);
    }

    public int getExpDataFileID()
    {
        return _expDataFileID;
    }

    public void setExpDataFileID(int expDataFileID)
    {
        _expDataFileID = expDataFileID;
        setDirty(true);
    }

    public String getMzXmlURL()
    {
        return _mzXmlURL;
    }

    public void setMzXmlURL(String mzXmlURL)
    {
        _mzXmlURL = mzXmlURL;
        setDirty(true);
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
        setDirty(true);
    }

    public boolean isImported()
    {
        return _imported;
    }

    public void setImported(boolean imported)
    {
        _imported = imported;
        setDirty(true);
    }

    protected int _peaksFileID = -1;
    protected int _expDataFileID = -1;
    protected String _mzXmlURL;
    protected String _description;
    protected boolean _imported = false;
} //class PeaksFile()
