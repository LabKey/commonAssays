/*
 * Copyright (c) 2011-2014 LabKey Corporation
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

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.microarray.MicroarrayModule;
import org.labkey.microarray.MicroarrayRunUploadForm;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;

/**
 * User: jeckels
 * Date: Oct 12, 2011
 */
public class MicroarrayRunCreator extends DefaultAssayRunCreator<MicroarrayAssayProvider>
{
    public MicroarrayRunCreator(MicroarrayAssayProvider provider)
    {
        super(provider);
    }

    protected void addInputMaterials(AssayRunUploadContext<MicroarrayAssayProvider> context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        MicroarrayRunUploadForm form = (MicroarrayRunUploadForm)context;
        int count = form.getSampleCount(form.getCurrentMageML());
        for (int i = 0; i < count; i++)
        {
            ExpMaterial material = form.getSample(i);
            if (!material.getContainer().hasPermission(context.getUser(), ReadPermission.class))
            {
                throw new ExperimentException("You do not have permission to reference the sample '" + material.getName() + ".");
            }
            if (inputMaterials.containsKey(material))
            {
                throw new ExperimentException("The same material, '" + material.getName() + "', cannot be used multiple times for a single run");
            }
            inputMaterials.put(material, "Sample " + (i + 1));
        }
    }

    @Override
    protected void addInputDatas(AssayRunUploadContext<MicroarrayAssayProvider> context, Map<ExpData, String> inputDatas, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        super.addInputDatas(context, inputDatas, resolverType);

        try
        {
            File mageMLFile = getMageMLFile(context);
            // Look up two directories for a TIFF file that matches the naming convention
            if (mageMLFile.getParentFile() != null && mageMLFile.getParentFile().getParentFile() != null
                    && mageMLFile.getParentFile().getParentFile().getParentFile() != null)
            {
                File dir = mageMLFile.getParentFile().getParentFile().getParentFile();
                File[] files = dir.listFiles(new PipelineProvider.FileTypesEntryFilter(MicroarrayModule.TIFF_INPUT_TYPE.getFileType()));
                if (files != null)
                {
                    for (File file : files)
                    {
                        // MageML files are named with <TIFF_FILE_BASE_NAME>_<PROTOCOL_NAME>.mageML (or other file extension)
                        if (mageMLFile.getName().startsWith(MicroarrayModule.TIFF_INPUT_TYPE.getFileType().getBaseName(file) + "_"))
                        {
                            // Found a match, add it as an input to this run
                            ExpData tiffData = createData(context.getContainer(), file, file.getName(), MicroarrayModule.TIFF_INPUT_TYPE, true);
                            inputDatas.put(tiffData, MicroarrayModule.TIFF_INPUT_TYPE.getRole());
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    private File getMageMLFile(AssayRunUploadContext context)
            throws IOException, ExperimentException
    {
        Map<String, File> files = context.getUploadedData();
        assert files.containsKey(AssayDataCollector.PRIMARY_FILE);
        return files.get(AssayDataCollector.PRIMARY_FILE);
    }

    protected void addOutputDatas(AssayRunUploadContext<MicroarrayAssayProvider> context, Map<ExpData, String> inputDatas, Map<ExpData, String> outputDatas, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        try
        {
            File mageMLFile = getMageMLFile(context);
            ExpData mageData = DefaultAssayRunCreator.createData(context.getContainer(), mageMLFile, mageMLFile.getName(), MicroarrayModule.MAGE_ML_INPUT_TYPE, true);

            outputDatas.put(mageData, MicroarrayModule.MAGE_ML_INPUT_TYPE.getRole());
            addRelatedOutputDatas(context, inputDatas, outputDatas, mageMLFile);
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    @Override
    protected FileFilter getRelatedOutputDataFileFilter(final File primaryFile, final String baseName)
    {
        return new FileFilter()
        {
            public boolean accept(File f)
            {
                // Microarray, unlike the other assay providers, wants to associate myrun_FEATURES.tsv
                // with myrun.tsv, so we don't include the "." when comparing other files against the base file name.
                return f.getName().startsWith(baseName) && !primaryFile.equals(f);
            }
        };
    }
}
