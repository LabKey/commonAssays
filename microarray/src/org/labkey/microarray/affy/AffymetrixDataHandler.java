/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.microarray.affy;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ImportAliasable;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AffymetrixDataHandler extends AbstractAssayTsvDataHandler
{
    private static final Logger LOG = Logger.getLogger(AffymetrixDataHandler.class);

    @Nullable
    @Override
    public DataType getDataType()
    {
        return AffymetrixAssayProvider.GENE_TITAN_DATA_TYPE;
    }

    @Override
    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = run.getProtocol();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        List<ExpData> dataOutputs = run.getDataOutputs();
        Map<ExpMaterial, String> materialInputs = run.getMaterialInputs();

        Map<String, ? extends DomainProperty> importMap = ImportAliasable.Helper.createImportMap(provider.getResultsDomain(protocol).getProperties(), false);

        try
        {
            ExcelLoader loader = new ExcelLoader(dataFile);
            ColumnDescriptor[] columns = loader.getColumns();
            String[] firstRow = loader.getFirstNLines(1)[0];
            List<Map<String, Object>> runDataRows = new ArrayList<>();
            Integer rowCounter = 0;
            String hybNameColumn = null;
            String sampleFilePathColumn = null;
            String sampleFileNameColumn = null;
            Map<String, String> columnMap = new HashMap<>();

            for (int i = 0; i < firstRow.length; i++)
            {
                String value = firstRow[i];

                // Look for a property that matches based on aliases, labels, etc
                DomainProperty property = importMap.get(value);

                if ("hyb_name".equalsIgnoreCase(value) || (property != null && AffymetrixAssayProvider.SAMPLE_NAME_COLUMN.equalsIgnoreCase(property.getName())))
                {
                    hybNameColumn = columns[i].name;
                }
                else if ("Sample File Path".equalsIgnoreCase(value) || "SampleFilePath".equalsIgnoreCase(value))
                {
                     sampleFilePathColumn = columns[i].name;
                }
                else if ("Sample File Name".equalsIgnoreCase(value) || "SampleFileName".equalsIgnoreCase(value))
                {
                    sampleFileNameColumn = columns[i].name;
                }
                else if (property != null)
                {
                    columnMap.put(columns[i].name, property.getName());
                }
            }

            for (ColumnDescriptor column : columns)
            {
                if (column.name.equals(hybNameColumn) || column.name.equals(sampleFilePathColumn) ||
                        column.name.equals(sampleFileNameColumn) || columnMap.containsKey(column.name))
                {
                    column.load = true;
                }
                else
                    column.load = false;
            }

            for (Map<String, Object> excelRow : loader)
            {
                if (rowCounter > 0)
                {
                    Map<String, Object> runDataRow = new HashMap<>();
                    String celFileName = (String) excelRow.get(sampleFileNameColumn);
                    String celFilePath = StringUtils.trimToEmpty((String) excelRow.get(sampleFilePathColumn));
                    String sampleName = (String) excelRow.get(hybNameColumn);

                    runDataRow.put(AffymetrixAssayProvider.SAMPLE_NAME_COLUMN, sampleName);
                    runDataRow.put(AffymetrixAssayProvider.SAMPLE_ID_COLUMN, getSampleId(sampleName, materialInputs));
                    runDataRow.put(AffymetrixAssayProvider.CEL_FILE_ID_COLUMN, getCelFileId(dataFile, celFilePath, celFileName, dataOutputs));

                    for (String key : excelRow.keySet())
                    {
                        if (columnMap.get(key) != null)
                        {
                            runDataRow.put(columnMap.get(key), excelRow.get(key));
                        }
                    }

                    runDataRows.add(runDataRow);
                }
                rowCounter++;
            }

            LOG.info("Imported " + runDataRows.size() + " rows");
            Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();
            datas.put(getDataType(), runDataRows);
            return datas;
        }
        catch (IOException e)
        {
            throw new ExperimentException("Error loading excel file.", e);
        }
    }

    private Integer getSampleId(String sampleName, Map<ExpMaterial, String> materialInputs)
    {

        for (Map.Entry entry : materialInputs.entrySet())
        {
            if (entry.getValue().equals(sampleName))
                return ((ExpMaterial)entry.getKey()).getRowId();
        }

        return null;
    }

    private Integer getCelFileId(File dataFile, String celFilePath, String celFileName, List<ExpData> dataOutputs) throws ExperimentException
    {
        Path path = Paths.get(celFilePath).resolve(celFileName).normalize();

        if (!path.toFile().exists())
        {
            // This gets the pipeline root several levels above the temp directory. We don't have context this far down
            // the pipeline so just hard coding this. If this is an issue we can update interfaces and classes necessary
            // to pass through the context and then call getRoot() on the collector.
            path = Paths.get(dataFile.getParentFile().getParentFile().getParentFile().getParent()).resolve(celFilePath).resolve(celFileName).normalize();
        }

        String absolutePath = path.toString();

        for (ExpData data : dataOutputs)
        {
            if (data.getFile().getAbsolutePath().contains(absolutePath))
                return data.getRowId();
        }

        throw new ExperimentException("CEL file Id not found.");
    }

    @Override
    public ActionURL getContentURL(ExpData data)
    {
        return null;
    }

    @Override
    protected boolean allowEmptyData()
    {
        return false;
    }

    @Override
    protected boolean shouldAddInputMaterials()
    {
        return false;
    }

}
