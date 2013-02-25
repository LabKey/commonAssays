/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
package org.labkey.elispot;

import org.apache.log4j.Logger;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.gwt.client.DefaultValueType;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;

import java.util.HashMap;
import java.util.Map;

/**
 * User: klum
 * Date: Jan 9, 2012
 * Time: 1:26:14 PM
 */
public class ElispotUpgradeCode implements UpgradeCode
{
    private static final Logger _log = Logger.getLogger(ElispotUpgradeCode.class);

    /**
     * Recompute sample level statistics so that spot counts are normalized per million cells and take into account
     * the antigen well group cell/well property.
     *
     * invoked from elispot-11.30-11.31.sql
     *
     * @param context
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public void normalizeSpotCounts(ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            _log.info("Starting upgrade task for elispot normalizeSpotCounts");

            // get all the elispot assay instances
            for (ExpProtocol protocol : ExperimentService.get().getAllExpProtocols())
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (provider instanceof ElispotAssayProvider)
                {
                    for (ExpRun run : protocol.getExpRuns())
                    {
                        try
                        {
                            ExperimentService.get().getSchema().getScope().ensureTransaction();

                            ExpData[] data = run.getOutputDatas(ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE));
                            if (data.length != 1)
                                throw new ExperimentException("Elispot should only upload a single file per run.");

                            String dataLsid = data[0].getLSID();
                            PlateTemplate template = ((ElispotAssayProvider)provider).getPlateTemplate(run.getContainer(), run.getProtocol());
                            // create an empty plate, we don't need to read the well information, just the specimen and antigen group info
                            Plate plate = PlateService.get().createPlate(template, new double[template.getRows()][template.getColumns()]);

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
                            ElispotDataHandler.populateAntigenDataProperties(run, plate, propMap, true, false);
                            ElispotDataHandler.populateAntigenRunProperties(run, plate, propMap, true, false);

                            ExperimentService.get().getSchema().getScope().commitTransaction();
                        }
                        catch (Exception e)
                        {
                            // fail upgrading the run but continue on to subsequent runs
                            _log.error("An error occurred upgrading elispot run: " + run.getName() + " in folder: " + run.getContainer().getPath(), e);
                        }
                        finally
                        {
                            ExperimentService.get().getSchema().getScope().closeConnection();
                        }
                    }
                }
            }
        }
    }

    /**
     * Subtract background well mean/median values from the antigen well group mean/median values. We need to add a
     * run level property to selectively enable background well subtraction.
     *
     * invoked from elispot-12.10-12.11.sql
     *
     * @param context
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public void addSubtractBackgroundRunProp(ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            _log.info("Starting upgrade task for elispot addSubtractBackgroundRunProp");

            // get all the elispot assay instances
            for (ExpProtocol protocol : ExperimentService.get().getAllExpProtocols())
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (provider instanceof ElispotAssayProvider)
                {
                    try
                    {
                        ExperimentService.get().getSchema().getScope().ensureTransaction();

                        // add a run property for background well subtraction
                        Domain domain = provider.getRunDomain(protocol);

                        if (domain.getPropertyByName(ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_NAME) == null)
                        {
                            DomainProperty prop = domain.addProperty();
                            prop.setLabel(ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_CAPTION);
                            prop.setName(ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_NAME);
                            prop.setPropertyURI(domain.getTypeURI() + "#" + ElispotAssayProvider.BACKGROUND_WELL_PROPERTY_NAME);

                            prop.setType(PropertyService.get().getType(domain.getContainer(), PropertyType.BOOLEAN.getXmlName()));
                            prop.setDefaultValueTypeEnum(DefaultValueType.LAST_ENTERED);

                            domain.save(context.getUpgradeUser());

                            for (ExpRun run : protocol.getExpRuns())
                            {
                                run.setProperty(context.getUpgradeUser(), prop.getPropertyDescriptor(), false);
                            }
                        }
                        ExperimentService.get().getSchema().getScope().commitTransaction();
                    }
                    catch (Exception e)
                    {
                        // fail upgrading the run but continue on to subsequent runs
                        _log.error("An error occurred upgrading elispot assay : " + protocol.getName() + " in folder: " + protocol.getContainer().getPath(), e);
                    }
                    finally
                    {
                        ExperimentService.get().getSchema().getScope().closeConnection();
                    }
                }
            }
        }
    }
}
