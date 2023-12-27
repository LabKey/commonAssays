package org.labkey.elisa;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.plate.PlateSampleFilePropertyHelper;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PositionImpl;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderFactory;
import org.labkey.api.reader.DataLoaderService;
import org.labkey.api.study.assay.SampleMetadataInputFormat;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElisaSampleFilePropertyHelper extends PlateSampleFilePropertyHelper
{
    public static final String PLATE_COLUMN_NAME = "Plate Name";
    public static final String SAMPLE_COLUMN_NAME = "Sample";
    public static final String WELL_LOCATION_COLUMN_NAME = "Well";
    public static final String SPOT_COLUMN_NAME = "Spot";

    public ElisaSampleFilePropertyHelper(Container container, ExpProtocol protocol, List<? extends DomainProperty> domainProperties, Plate template, SampleMetadataInputFormat inputFormat)
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
        DataLoaderFactory factory = DataLoaderService.get().findFactory(metadataFile, null);
        try (DataLoader loader = factory.createLoader(metadataFile, true))
        {
            validateRequiredColumns(loader.getColumns());

            for (Map<String, Object> row : loader)
            {
                String wellLocation = String.valueOf(row.get(WELL_LOCATION_COLUMN_NAME));
                String plateName = String.valueOf(row.get(PLATE_COLUMN_NAME));
                Integer spot = (Integer)row.get(SPOT_COLUMN_NAME);

                if (wellLocation != null && plateName != null)
                {
                    String sampleWellGroup = getSampleWellGroupFromLocation(plateName, spot, wellLocation);
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

    public static void validateRequiredColumns(ColumnDescriptor[] columns) throws ExperimentException
    {
        List<String> cols = Arrays.asList(PLATE_COLUMN_NAME, SAMPLE_COLUMN_NAME, WELL_LOCATION_COLUMN_NAME, SPOT_COLUMN_NAME);
        for (String column : cols)
        {
            if (Arrays.stream(columns).noneMatch(c -> c.getColumnName().equalsIgnoreCase(column)))
            {
                throw new ExperimentException("Unable to process the data file, the uploaded file must contain these columns: " +
                        cols + ".");
            }
        }
    }

    @Nullable
    private String getSampleWellGroupFromLocation(String plateName, Integer analyteNum, String wellLocation)
    {
        try
        {
            PositionImpl position = new PositionImpl(_protocol.getContainer(), wellLocation);
            // need to adjust the column value to be 0 based to match the template locations
            position.setColumn(position.getColumn()-1);

            for (WellGroup wellGroup : _template.getWellGroups(position))
            {
                if (wellGroup.getType() == WellGroup.Type.SPECIMEN)
                    return HighThroughputImportHelper.getSpecimenGroupKey(plateName, analyteNum, wellGroup.getName());
            }
            return null;
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
