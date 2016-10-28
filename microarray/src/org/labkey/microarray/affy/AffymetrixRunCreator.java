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
import org.labkey.api.data.ImportAliasable;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.files.FileContentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.reader.ExcelLoader;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class AffymetrixRunCreator extends DefaultAssayRunCreator<AffymetrixAssayProvider>
{
    AffymetrixRunCreator(AffymetrixAssayProvider provider)
    {
        super(provider);
    }

    @Override
    protected void addOutputDatas(AssayRunUploadContext<AffymetrixAssayProvider> context, Map<ExpData, String> inputDatas, Map<ExpData, String> outputDatas, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        try
        {
            File excelFile = getExcelFile(context);
            ExpData excelData = DefaultAssayRunCreator.createData(context.getContainer(), excelFile, excelFile.getName(), AffymetrixAssayProvider.GENE_TITAN_DATA_TYPE, true);
            outputDatas.put(excelData, AffymetrixAssayProvider.GENE_TITAN_DATA_TYPE.getRole());

            if (outputDatas.size() == 0)
                throw new ExperimentException("NO EXCEL FILE ADDED");

            ExcelLoader loader = new ExcelLoader(excelFile);
            String fileNameColumn = null;
            String filePathColumn = null;
            int rowCounter = 0;

            for (Map<String, Object> rowData : loader)
            {
                if (rowCounter == 0)
                {
                    for (Map.Entry column : rowData.entrySet())
                    {
                        if (column.getValue().equals("Sample File Path"))
                        {
                            filePathColumn = (String) column.getKey();
                        }

                        if (column.getValue().equals("Sample File Name"))
                        {
                            fileNameColumn = (String) column.getKey();
                        }
                    }

                    if (filePathColumn == null)
                        throw new ExperimentException("Sample File Path column not found.");

                    if (fileNameColumn == null)
                        throw new ExperimentException("Sample File column not found.");
                }
                else
                {
                    String fileName = (String) rowData.get(fileNameColumn);
                    String filePath = StringUtils.trimToEmpty((String) rowData.get(filePathColumn)); //Issue 17800

                    if (fileName == null || fileName.equals(""))
                        throw new ExperimentException("Sample File Name column cannot be blank or null");

                    // First assume the filePath and fileName are an absolute path.
                    // Must normalize path to prevent something like /path/to/file/root/../../../../etc/passwd from being
                    // recognized as being within the folder/pipeline root.
                    File celFile = Paths.get(filePath).resolve(fileName).normalize().toFile();

                    if (!celFile.exists())
                    {
                        // Issue 17798:Support relative paths for Affy import
                        // If the filePath + fileName does not work, then filePath may be relative to the directory of the uploaded excel file.
                        if(null != ((AssayRunUploadForm) context).getSelectedDataCollector())
                        {
                            celFile = Paths.get(((AssayRunUploadForm) context).getSelectedDataCollector().getRoot(excelData.getRun(), excelFile)
                                    .getAbsolutePath()).resolve(filePath).resolve(fileName).normalize().toFile();
                        }
                    }

                    if (!celFile.exists())
                    {
                        // If the file still doesn't exist then we can assume that it doesn't exist, or the data in
                        // the excel document is incorrect.
                        throw new ExperimentException("File with path: \"" + celFile.getAbsolutePath() + "\" was not found.");
                    }

                    //Issue 17799:Check file paths for Affy import to be sure they're under the file/pipeline root
                    FileContentService fileService = ServiceRegistry.get().getService(FileContentService.class);
                    Boolean inFileRoot = fileService != null && celFile.getAbsolutePath().startsWith(fileService.getFileRoot(context.getContainer()).getAbsolutePath());
                    PipeRoot pipelineRoot = PipelineService.get().getPipelineRootSetting(context.getContainer());
                    Boolean inPipelineRoot = celFile.getAbsolutePath().startsWith(pipelineRoot.getRootPath().getAbsolutePath());

                    if (!inFileRoot && !inPipelineRoot)
                    {
                        throw new ExperimentException("File must be within Pipeline Root or Folder Root path.");
                    }

                    ExpData celData = DefaultAssayRunCreator.createData(context.getContainer(), celFile, fileName, AffymetrixAssayProvider.CEL_DATA_TYPE, false);
                    outputDatas.put(celData, AffymetrixAssayProvider.CEL_DATA_TYPE.getRole());
                }
                rowCounter++;
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    @Override
    protected void addInputMaterials(AssayRunUploadContext<AffymetrixAssayProvider> context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        try
        {
            ExcelLoader loader = new ExcelLoader(getExcelFile(context));
            String hybColumn = null;
            Integer rowCounter = 0;

            // Check if there are aliases
            Map<String, ? extends DomainProperty> importMap = ImportAliasable.Helper.createImportMap(context.getProvider().getResultsDomain(context.getProtocol()).getProperties(), false);

            for (Map<String, Object> rowData : loader)
            {
                if (rowCounter == 0)
                {
                    for (Map.Entry<String, Object> column : rowData.entrySet())
                    {
                        DomainProperty domainProperty = importMap.get(column.getValue());
                        // Support original Excel column name, or any alias for the "SampleName" field in the assay design
                        if ((column.getValue() instanceof String && "hyb_name".equalsIgnoreCase((String)column.getValue())) ||
                                (domainProperty != null && AffymetrixAssayProvider.SAMPLE_NAME_COLUMN.equalsIgnoreCase(domainProperty.getName())))
                        {
                            hybColumn = column.getKey();
                        }
                    }

                    if (hybColumn == null)
                        throw new ExperimentException("hyb_name column not found.");
                }
                else
                {
                    String hybName = (String) rowData.get(hybColumn);
                    if (hybName == null || hybName.equals(""))
                        throw new ExperimentException("hyb_name column cannot be blank or null");

                    ExpMaterial sample = resolveSample(context, hybName);

                    inputMaterials.put(sample, sample.getName());

                }
                rowCounter++;
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    private File getExcelFile(AssayRunUploadContext context) throws IOException, ExperimentException
    {
        Map<String, File> files = context.getUploadedData();
        assert files.containsKey(AssayDataCollector.PRIMARY_FILE);
        return files.get(AssayDataCollector.PRIMARY_FILE);
    }

    // NOTE:
    private ExpMaterial resolveSample(AssayRunUploadContext context, String name) throws ExperimentException
    {
        List<? extends ExpMaterial> materials = ExperimentService.get().getExpMaterialsByName(name, context.getContainer(), context.getUser());
        if (materials.size() == 1)
        {
            return materials.get(0);
        }
        // Couldn't find exactly one match, check if it might be of the form <SAMPLE_SET_NAME>.<SAMPLE_NAME>
        int dotIndex = name.indexOf(".");
        if (dotIndex != -1)
        {
            String sampleSetName = name.substring(0, dotIndex);
            String sampleName = name.substring(dotIndex + 1);
            // Could easily do some caching here, but probably not a significant perf issue
            for (ExpSampleSet sampleSet : ExperimentService.get().getSampleSets(context.getContainer(), context.getUser(), true))
            {
                // Look for a sample set with the right name
                if (sampleSetName.equals(sampleSet.getName()))
                {
                    for (ExpMaterial sample : sampleSet.getSamples())
                    {
                        // Look for a sample with the right name
                        if (sample.getName().equals(sampleName))
                        {
                            return sample;
                        }
                    }
                }
            }
        }

        // If we can't find a <SAMPLE_SET_NAME>.<SAMPLE_NAME> match, then fall back on the original results
        if (materials.isEmpty())
        {
            throw new ExperimentException("No sample with name '" + name + "' was found.");
        }
        // Must be more than one match
        throw new ExperimentException("Found samples with name '" + name + "' in multiple sample sets. Please prefix the name with the desired sample set, in the format 'SAMPLE_SET.SAMPLE'.");
    }
}
