/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.luminex.model.Analyte;

import java.util.List;
import java.util.Map;

/**
 * Saves analyte specific properties and handles re-run
 * User: jeckels
 * Date: Feb 13, 2012
 */
public class LuminexRunCreator extends DefaultAssayRunCreator<LuminexAssayProvider>
{
    /** Lock object to only allow one thread to be running a Luminex transform script and importing its results at a time, issue 17424 */
    public static final Object LOCK_OBJECT = new Object();

    public LuminexRunCreator(LuminexAssayProvider provider)
    {
        super(provider);
    }

    @Override
    public ExpExperiment saveExperimentRun(AssayRunUploadContext<LuminexAssayProvider> uploadContext, @Nullable ExpExperiment batch, @NotNull ExpRun run, boolean forceSaveBatchProps) throws ExperimentException, ValidationException
    {
        // Only allow one thread to be running a Luminex transform script and importing its results at a time
        // See issue 17424
        synchronized (LOCK_OBJECT)
        {
            LuminexRunContext context = (LuminexRunContext)uploadContext;
            batch = super.saveExperimentRun(context, batch, run, forceSaveBatchProps);
            Container container = context.getContainer();

            // Save the analyte properties
            List<ExpData> outputs = run.getDataOutputs();
            for (ExpData output : outputs)
            {
                int dataId = output.getRowId();

                for (Analyte analyte : LuminexManager.get().getAnalytes(dataId))
                {
                    Map<DomainProperty, String> properties = context.getAnalyteProperties(analyte.getName());

                    ObjectProperty[] objProperties = new ObjectProperty[properties.size()];
                    int i = 0;
                    for (Map.Entry<DomainProperty, String> entry : properties.entrySet())
                    {
                        ObjectProperty property = new ObjectProperty(analyte.getLsid(),
                                container, entry.getKey().getPropertyURI(),
                                entry.getValue(), entry.getKey().getPropertyDescriptor().getPropertyType());
                        objProperties[i++] = property;
                    }
                    removeExistingAnalyteProperties(container, analyte);
                    OntologyManager.insertProperties(container, analyte.getLsid(), objProperties);
                }
            }
        }

        handleReRun(uploadContext, run);
        return batch;
    }

    private void handleReRun(AssayRunUploadContext<LuminexAssayProvider> uploadContext, ExpRun run) throws ValidationException
    {
        if (uploadContext.getReRunId() != null)
        {
            ExpRun replacedRun = ExperimentService.get().getExpRun(uploadContext.getReRunId().intValue());

            if (replacedRun != null)
            {
                // Migrate the original Created and CreatedBy values from the old run to the new run
                run.setCreated(replacedRun.getCreated());
                run.setCreatedBy(replacedRun.getCreatedBy());
                run.save(uploadContext.getUser());

                if (uploadContext instanceof LuminexRunContext && ((LuminexRunContext)uploadContext).getRetainExclusions())
                    LuminexManager.get().retainExclusions((LuminexRunContext)uploadContext, replacedRun, run);

                // Delete the old run, which has been replaced
                if (replacedRun.getContainer().hasPermission(uploadContext.getUser(), DeletePermission.class))
                {
                    replacedRun.delete(uploadContext.getUser());
                }
            }
        }
    }

    private void removeExistingAnalyteProperties(Container container, Analyte analyte) throws ExperimentException
    {
        OntologyObject analyteObject = OntologyManager.getOntologyObject(container, analyte.getLsid());
        if (analyteObject != null)
        {
            OntologyManager.deleteProperties(container, analyteObject.getObjectId());
        }
    }
}
