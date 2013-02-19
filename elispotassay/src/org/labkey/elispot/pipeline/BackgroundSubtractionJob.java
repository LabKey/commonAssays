/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.elispot.pipeline;

import org.apache.commons.lang3.math.NumberUtils;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.PlateBasedAssayProvider;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.study.assay.plate.PlateReaderService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.elispot.ElispotAssayProvider;
import org.labkey.elispot.ElispotDataHandler;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Jun 14, 2012
 */
public class BackgroundSubtractionJob extends PipelineJob
{
    public static final String PROCESSING_STATUS = "Processing";
    Set<String> _runs = new HashSet<String>();

    public BackgroundSubtractionJob(String provider, ViewBackgroundInfo info, PipeRoot root, Set<String> runs) throws IOException, SQLException
    {
        super(provider, info, root);

        File logFile = File.createTempFile("backgroundSubtractionJob", ".log", root.getRootPath());
        setLogFile(logFile);

        _runs = runs;
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "ELISpot Background Well Subtraction";
    }

    public void run()
    {
        setStatus(PROCESSING_STATUS, "Job started at: " + DateUtil.nowISO());

        DataType elipotDataType = ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE);

        for (String runId : _runs)
        {
            int rowId = NumberUtils.toInt(runId, -1);

            if (rowId != -1)
            {
                ExpRun run = ExperimentService.get().getExpRun(rowId);

                try
                {
                    ExperimentService.get().getSchema().getScope().ensureTransaction();
                    info("Starting background substraction for run : " + runId);

                    AssayProvider provider = AssayService.get().getProvider(run.getProtocol());
                    ExpData[] data = run.getOutputDatas(elipotDataType);
                    if (data.length != 1)
                        throw new ExperimentException("Elispot should only upload a single file per run.");

                    Map<String, DomainProperty> runPropMap = new HashMap<String, DomainProperty>();
                    for (DomainProperty column : provider.getRunDomain(run.getProtocol()).getProperties())
                        runPropMap.put(column.getName(), column);

                    Plate plate = initializePlate((PlateBasedAssayProvider)provider, run, runPropMap.get(ElispotAssayProvider.READER_PROPERTY_NAME));

                    if (plate != null)
                    {
                        String dataLsid = data[0].getLSID();
                        Map<String, Object> propMap = new HashMap<String, Object>();
                        Domain antigenDomain = AbstractAssayProvider.getDomainByPrefix(run.getProtocol(), ElispotAssayProvider.ASSAY_DOMAIN_ANTIGEN_WELLGROUP);

                        DomainProperty cellWellProp = antigenDomain.getPropertyByName(ElispotAssayProvider.CELLWELL_PROPERTY_NAME);
                        DomainProperty antigenNameProp = antigenDomain.getPropertyByName(ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME);

                        String cellWellURI = ElispotDataHandler.createPropertyLsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, run, ElispotAssayProvider.CELLWELL_PROPERTY_NAME).toString();
                        String antigenNameURI = ElispotDataHandler.createPropertyLsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, run, ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME).toString();

                        // populate the property maps with cells per well and antigen name information (to simulate data upload)
                        for (WellGroup group : plate.getWellGroups(WellGroup.Type.ANTIGEN))
                        {
                            Position groupPos = group.getPositions().get(0);
                            Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(dataLsid, groupPos);
                            Map<String, ObjectProperty> dataRow = OntologyManager.getPropertyObjects(run.getContainer(), dataRowLsid.toString());

                            if (dataRow.containsKey(cellWellURI))
                            {
                                ObjectProperty o = dataRow.get(cellWellURI);
                                propMap.put(UploadWizardAction.getInputName(cellWellProp, group.getName()), o.getFloatValue().intValue());
                            }
                            if (dataRow.containsKey(antigenNameURI))
                            {
                                ObjectProperty o = dataRow.get(antigenNameURI);
                                propMap.put(UploadWizardAction.getInputName(antigenNameProp, group.getName()), o.getStringValue());
                            }
                        }
                        ElispotDataHandler.populateAntigenRunProperties(run, plate, propMap, true, true);

                        // set the run property for background subtraction
                        DomainProperty subtractBackground = runPropMap.get(ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_NAME);
                        if (subtractBackground != null)
                            run.setProperty(getUser(), subtractBackground.getPropertyDescriptor(), true);
                    }
                    ExperimentService.get().getSchema().getScope().commitTransaction();
                    setStatus(PipelineJob.COMPLETE_STATUS, "Job finished at: " + DateUtil.nowISO());
                }
                catch (Exception e)
                {
                    error("Error occurred running the background subtraction job", e);
                    setStatus(PipelineJob.ERROR_STATUS, "Job finished at: " + DateUtil.nowISO());
                }
                finally
                {
                    ExperimentService.get().getSchema().getScope().closeConnection();
                }
            }
            else
                warn("Invalid run number: " + runId);
        }
    }

    private Plate initializePlate(PlateBasedAssayProvider provider, ExpRun run, DomainProperty plateReaderProp) throws ExperimentException
    {
        PlateTemplate template = provider.getPlateTemplate(run.getContainer(), run.getProtocol());
        ExpData[] data = run.getOutputDatas(ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE));
        Plate plate = null;

        Object plateReader = run.getProperty(plateReaderProp);
        if (plateReader != null)
        {
            File dataFile = data[0].getFile();

            if (dataFile.exists())
            {
                PlateReader reader = PlateReaderService.getPlateReaderFromName(plateReader.toString(), getContainer(), provider);
                plate = ElispotDataHandler.initializePlate(data[0].getFile(), template, reader);
            }
            else
                error("The original run data file does not exist: " + dataFile.getName());
        }
        return plate;
    }
}
