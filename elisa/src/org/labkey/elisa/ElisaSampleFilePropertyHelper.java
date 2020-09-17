package org.labkey.elisa;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.plate.PlateSampleFilePropertyHelper;
import org.labkey.api.assay.plate.PlateTemplate;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.PositionImpl;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.assay.plate.WellGroupTemplate;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderFactory;
import org.labkey.api.reader.DataLoaderService;
import org.labkey.api.study.assay.SampleMetadataInputFormat;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElisaSampleFilePropertyHelper extends PlateSampleFilePropertyHelper
{
    public ElisaSampleFilePropertyHelper(Container container, ExpProtocol protocol, List<? extends DomainProperty> domainProperties, PlateTemplate template, SampleMetadataInputFormat inputFormat)
    {
        super(container, protocol, domainProperties, template, inputFormat);
    }

    @Override
    public Map<String, Map<DomainProperty, String>> getSampleProperties(HttpServletRequest request) throws ExperimentException
    {
        if (_sampleProperties != null)
            return _sampleProperties;

        File metadataFile = getSampleMetadata(request);
        if (metadataFile == null)
            throw new ExperimentException("No metadata or data file provided");

        Map<String, Map<DomainProperty, String>> allProperties = new HashMap<>();
        try
        {
            DataLoaderFactory factory = DataLoaderService.get().findFactory(metadataFile, null);
            DataLoader loader = factory.createLoader(metadataFile, true);

            String plateColumnName = "Plate Name";
            String sampleColumnName = "Sample";
            String wellLocationColumnName = "Well";

            boolean hasPlateColumn = Arrays.stream(loader.getColumns()).anyMatch(c -> c.getColumnName().equalsIgnoreCase(plateColumnName));
            boolean hasSampleColumn = Arrays.stream(loader.getColumns()).anyMatch(c -> c.getColumnName().equalsIgnoreCase(sampleColumnName));
            boolean hasWellLocationColumn = Arrays.stream(loader.getColumns()).anyMatch(c -> c.getColumnName().equalsIgnoreCase(wellLocationColumnName));

            if (!hasPlateColumn)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + plateColumnName + "\".");
            if (!hasSampleColumn)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + sampleColumnName + "\".");
            if (!hasWellLocationColumn)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + hasWellLocationColumn + "\".");

            for (Map<String, Object> row : loader)
            {
                String wellLocation = String.valueOf(row.get(wellLocationColumnName));
                String plateName = String.valueOf(row.get(plateColumnName));

                if (wellLocation != null && plateName != null)
                {
                    String sampleWellGroup = getSampleWellGroupFromLocation(plateName, wellLocation);
                    if (sampleWellGroup != null)
                    {
                        Map<DomainProperty, String> sampleProperties = allProperties.computeIfAbsent(sampleWellGroup, k -> new HashMap<>());
                        for (DomainProperty property : _domainProperties)
                        {
                            Object value = getValue(row, property);
                            sampleProperties.put(property, value != null ? value.toString() : null);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException("Unable to parse sample properties file.  Please verify that the file is a valid Excel workbook.", e);
        }
        _sampleProperties = allProperties;
        return _sampleProperties;
    }

    @Nullable
    private String getSampleWellGroupFromLocation(String plateName, String wellLocation)
    {
        try
        {
            PositionImpl position = new PositionImpl(_protocol.getContainer(), wellLocation);
            // need to adjust the column value to be 0 based to match the template locations
            position.setColumn(position.getColumn()-1);

            for (WellGroupTemplate wellGroup : _template.getWellGroups(position))
            {
                if (wellGroup.getType() == WellGroup.Type.SPECIMEN)
                    return HighThroughputImportHelper.getSpecimenGroupKey(plateName, wellGroup.getName());
            }
            return null;
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
