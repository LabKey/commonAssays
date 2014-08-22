package org.labkey.nab;

import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.assay.PlateSampleFilePropertyHelper;
import org.labkey.api.study.assay.SampleMetadataInputFormat;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Created by cnathe on 8/18/14.
 */
public class NabVirusFilePropertyHelper extends PlateSampleFilePropertyHelper
{
    public static final String VIRUS_WELLGROUP_COLUMN = "VirusWellGroup";

    public NabVirusFilePropertyHelper(Container c, ExpProtocol protocol, List<? extends DomainProperty> sampleProperties, PlateTemplate template)
    {
        super(c, protocol, sampleProperties, template, SampleMetadataInputFormat.FILE_BASED);
        _wellgroupType = WellGroup.Type.VIRUS;
        _metadataNoun = "Virus";
        _wellGroupColumnName = VIRUS_WELLGROUP_COLUMN;
    }

    @Override
    public Map<String, Map<DomainProperty, String>> getSampleProperties(HttpServletRequest request) throws ExperimentException
    {
        Map<String, Map<DomainProperty, String>> virusProperties = super.getSampleProperties(request);

        for (String virusWellGroupName : getSampleWellGroupNameMap().keySet())
        {
            if (!virusProperties.containsKey(virusWellGroupName))
                throw new ExperimentException("Virus Wellgroup \"" + virusWellGroupName + "\" does not exist in the metadata file. Was the plate template edited?");
        }

        return virusProperties;
    }

    @Override
    protected void validateMetadataRow(Map<String, Object> row, String wellGroupName, WellGroupTemplate wellgroup) throws ExperimentException
    {
        // no-op
    }
}
