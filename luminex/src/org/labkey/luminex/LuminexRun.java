package org.labkey.luminex;

import java.util.*;

/**
 * User: jeckels
 * Date: Jun 26, 2007
 */
public class LuminexRun
{
    private String _fileName;
    private List<AnalyteInfo> _analyteInfos = new ArrayList<AnalyteInfo>();
    private List<SpecimenInfo> _specimenInfos;
    private String _createdOn;
    private String _lab;
    private String _createdBy;

    public LuminexRun(String fileName)
    {
        _fileName = fileName;
    }

    public String getFileName()
    {
        return _fileName;
    }

    public void addAnalyteInfo(AnalyteInfo info)
    {
        _specimenInfos = null;
        _analyteInfos.add(info);
    }

    public List<AnalyteInfo> getAnalyteInfos()
    {
        return Collections.unmodifiableList(_analyteInfos);
    }

    public List<SpecimenInfo> getNonControlSpecimenInfos()
    {
        List<SpecimenInfo> result = new ArrayList<SpecimenInfo>();
        for (SpecimenInfo specimen : getSpecimenInfos())
        {
            if (!specimen.isControl())
            {
                result.add(specimen);
            }
        }
        return result;
    }

    public List<SpecimenInfo> getSpecimenInfos()
    {
        List<SpecimenInfo> result = _specimenInfos;
        if (result != null)
        {
            return result;
        }
        result = new ArrayList<SpecimenInfo>();
        Map<String, SpecimenInfo> map = new HashMap<String, SpecimenInfo>();
        LuminexColorGenerator colorGenerator = new LuminexColorGenerator();
        for (AnalyteInfo info : _analyteInfos)
        {
            for (LuminexDataRow dataRow : info.getDataRows())
            {
                String specimenName = dataRow.getSampleId();
                SpecimenInfo specimenInfo = map.get(specimenName);
                if (specimenInfo == null)
                {
                    specimenInfo = new SpecimenInfo(specimenName);
                    specimenInfo.setColor(colorGenerator.next(specimenName));
                    result.add(specimenInfo);
                    map.put(specimenName, specimenInfo);
                }
                dataRow.setSpecimen(specimenInfo);
                specimenInfo.addAnalyteValue(dataRow);
            }
        }
        _specimenInfos = result;
        return result;
    }


    public String getCreatedOn()
    {
        return _createdOn;
    }

    public String getLab()
    {
        return _lab;
    }

    public String getCreatedBy()
    {
        return _createdBy;
    }
    
    public void setCreatedOn(String createdOn)
    {
        _createdOn = createdOn;
    }

    public void setLab(String lab)
    {
        _lab = lab;
    }

    public void setCreatedBy(String createdBy)
    {
        _createdBy = createdBy;
    }
}
