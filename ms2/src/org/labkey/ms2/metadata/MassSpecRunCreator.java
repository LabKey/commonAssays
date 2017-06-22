/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
package org.labkey.ms2.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationException;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PipelineDataCollector;
import org.labkey.api.util.GUID;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jeckels
 * Date: Oct 12, 2011
 */
public class MassSpecRunCreator extends DefaultAssayRunCreator<MassSpecMetadataAssayProvider>
{
    public static final String FRACTION_INPUT_ROLE = "Fraction";

    public MassSpecRunCreator(MassSpecMetadataAssayProvider provider)
    {
        super(provider);
    }

    @Override
    public ExpExperiment saveExperimentRun(AssayRunUploadContext<MassSpecMetadataAssayProvider> context, @Nullable ExpExperiment batch, @NotNull ExpRun run, boolean forceSaveBatchProps) throws ExperimentException, ValidationException
    {
        MassSpecMetadataAssayForm form = (MassSpecMetadataAssayForm)context;
        if (form.isFractions())
        {
            // If this is a fractions search, first derive the fraction samples
            deriveFractions(form);
            // Then upload a bunch of runs
            while (!PipelineDataCollector.getFileQueue(form).isEmpty())
            {
                batch = super.saveExperimentRun(context, batch, run, forceSaveBatchProps);
                form.clearUploadedData();
                form.getSelectedDataCollector().uploadComplete(form, run);
            }
            return batch;
        }
        else
        {
            return super.saveExperimentRun(context, batch, run, forceSaveBatchProps);
        }
    }

    private ExpRun deriveFractions(MassSpecMetadataAssayForm form) throws ExperimentException
    {
        ExpSampleSet fractionSet = MassSpecMetadataAssayProvider.getFractionSampleSet(form);

        Map<File, Map<DomainProperty, String>> mapFilesToFractionProperties = form.getFractionProperties(fractionSet);

        Map<ExpMaterial, String> derivedSamples = new HashMap<>();

        try
        {
            Lsid.LsidBuilder builder = new Lsid.LsidBuilder(fractionSet.getMaterialLSIDPrefix() + "OBJECT");
            for (Map.Entry<File,Map<DomainProperty, String>> entry : mapFilesToFractionProperties.entrySet())
            {
                // generate unique lsids for the derived samples
                File mzxmlFile = entry.getKey();
                String fileNameBase = mzxmlFile.getName().substring(0, (mzxmlFile.getName().lastIndexOf('.')));
                Map<DomainProperty, String> properties = entry.getValue();

                ExpMaterial derivedMaterial = ExperimentService.get().createExpMaterial(form.getContainer()
                        , builder.setObjectId(GUID.makeGUID()).toString(), "Fraction - " + fileNameBase);
                derivedMaterial.setCpasType(fractionSet.getLSID());
                // could put the fraction properties on the fraction material object or on the run.  decided to do the run

                for (Map.Entry<DomainProperty, String> property : properties.entrySet())
                {
                    String value = property.getValue();
                    derivedMaterial.setProperty(form.getUser(), property.getKey().getPropertyDescriptor(), value);
                }

                derivedSamples.put(derivedMaterial, FRACTION_INPUT_ROLE);
                form.getFileFractionMap().put(mzxmlFile, derivedMaterial);
            }
            ViewBackgroundInfo info = new ViewBackgroundInfo(form.getContainer(), form.getUser(), form.getActionURL());
            Map<ExpMaterial, String> startingMaterials = form.getStartingMaterials();
            ExpRun run = ExperimentService.get().deriveSamples(startingMaterials, derivedSamples, info, null);

            // Change the run's name
            StringBuilder sb = new StringBuilder("Fractionate ");
            String separator = "";
            for (ExpMaterial inputMaterial : startingMaterials.keySet())
            {
                sb.append(separator);
                sb.append(inputMaterial.getName());
                separator = ", ";
            }
            sb.append(" into ");
            sb.append(derivedSamples.size());
            sb.append(" fractions");
            run.setName(sb.toString());
            run.save(form.getUser());

            return run;
        }
        catch (ValidationException e)
        {
            throw new ExperimentException(e);
        }
    }

    @Override
    protected void addInputMaterials(AssayRunUploadContext<MassSpecMetadataAssayProvider> context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        MassSpecMetadataAssayForm form = (MassSpecMetadataAssayForm)context;
        if (form.isFractions())
        {
            Map<String, File> files = form.getUploadedData();
            assert files.containsKey(AssayDataCollector.PRIMARY_FILE);
            File mzXMLFile = files.get(AssayDataCollector.PRIMARY_FILE);
            ExpMaterial sample = form.getFileFractionMap().get(mzXMLFile);
            assert sample != null;
            inputMaterials.put(sample, FRACTION_INPUT_ROLE);
        }
        else
        {
            inputMaterials.putAll(form.getStartingMaterials());
        }
    }


}
