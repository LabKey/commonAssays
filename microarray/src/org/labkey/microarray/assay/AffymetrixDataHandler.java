/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.microarray.assay;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
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
        AssayProtocolSchema protocolSchema = new AffymetrixAssayProvider().createProtocolSchema(context.getUser(), context.getContainer(), protocol, null);
        TableInfo dataTable = protocolSchema.createDataTable(); // Need the columns from this.
        List<ExpData> dataOutputs = run.getDataOutputs();
        Map<ExpMaterial, String> materialInputs = run.getMaterialInputs();

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

                if (value.equals("hyb_name"))
                {
                    hybNameColumn = columns[i].name;
                }

                if (value.equals("Sample File Path"))
                {
                     sampleFilePathColumn = columns[i].name;
                }

                if (value.equals("Sample File Name"))
                {
                    sampleFileNameColumn = columns[i].name;
                }

                if (dataTable.getColumn(value) != null)
                {
                    columnMap.put(columns[i].name, value);
                }
            }

            for (int i = 0; i < columns.length; i++)
            {
                ColumnDescriptor column = columns[i];
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
                if(rowCounter > 0)
                {
                    Map<String, Object> runDataRow = new HashMap<>();
                    String celFileName = (String) excelRow.get(sampleFileNameColumn);
                    String celFilePath = (String) excelRow.get(sampleFilePathColumn);
                    String sampleName = (String) excelRow.get(hybNameColumn);

                    runDataRow.put("SampleName", sampleName);
                    runDataRow.put("SampleId", getSampleId(sampleName, materialInputs));
                    runDataRow.put("CelFileId", getCelFileId(celFilePath, celFileName, dataOutputs));

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
            if(entry.getValue().equals(sampleName))
                return ((ExpMaterial)entry.getKey()).getRowId();
        }

        return null;
    }

    private Integer getCelFileId(String celFilePath, String celFileName, List<ExpData> dataOutputs)
    {
        for(ExpData data : dataOutputs)
        {
            if(data.getFile().getAbsolutePath().contains(celFilePath + File.separator + celFileName))
                return data.getRowId();
        }

        return null;
    }

    @Override
    public ActionURL getContentURL(Container container, ExpData data)
    {
        return null;
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
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
