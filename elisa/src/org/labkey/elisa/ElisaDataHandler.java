/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

package org.labkey.elisa;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.AssayUploadXarContext;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateBasedAssayProvider;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.data.statistics.DoublePoint;
import org.labkey.api.data.statistics.FitFailedException;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.dataiterator.DataIteratorBuilder;
import org.labkey.api.dataiterator.MapDataIterator;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.assay.transform.TransformDataHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.elisa.query.CurveFitDb;
import org.labkey.elisa.query.ElisaManager;
import org.labkey.vfs.FileLike;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: klum
 * Date: 10/6/12
 */
public class ElisaDataHandler extends AbstractAssayTsvDataHandler implements TransformDataHandler
{
    private static final Logger LOG = LogManager.getLogger(ElisaDataHandler.class);
    public static final String NAMESPACE = "ElisaDataType";
    public static final AssayDataType DATA_TYPE;
    public static final String ELISA_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String STANDARDS_WELL_GROUP_NAME = "Standards";

    static
    {
        DATA_TYPE = new AssayDataType(NAMESPACE, new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));
    }

    @Override
    public DataType getDataType()
    {
        return DATA_TYPE;
    }

    @Override
    protected boolean allowEmptyData()
    {
        return false;
    }

    @Override
    protected boolean shouldAddInputMaterials()
    {
        return true;
    }

    private ElisaImportHelper getImportHelper(AssayUploadXarContext context, PlateBasedAssayProvider provider, ExpProtocol protocol, File dataFile) throws ExperimentException
    {
        if (provider.getMetadataInputFormat(protocol).equals(SampleMetadataInputFormat.MANUAL))
        {
            return new ManualImportHelper(context, provider, protocol, dataFile);
        }
        else if (provider.getMetadataInputFormat(protocol).equals(SampleMetadataInputFormat.COMBINED))
        {
            return new HighThroughputImportHelper(context, provider, protocol, dataFile);
        }
        return null;
    }

    @Override
    public Map<DataType, DataIteratorBuilder> getValidationDataMap(ExpData data, FileLike dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        List<Map<String, Object>> results = new ArrayList<>();
        ExpProtocol protocol = data.getRun().getProtocol();
        ExpRun run = data.getRun();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        if (provider instanceof PlateBasedAssayProvider plateProvider && context instanceof AssayUploadXarContext xarContext)
        {
            Domain runDomain = provider.getRunDomain(protocol);
            Domain resultDomain = provider.getResultsDomain(protocol);
            Map<String, DomainProperty> sampleProperties = plateProvider.getSampleWellGroupDomain(protocol)
                    .getProperties().stream()
                    .collect(Collectors.toMap(DomainProperty::getName, dp -> dp));
            ElisaImportHelper importHelper = getImportHelper(xarContext, plateProvider, protocol, dataFile.toNioPathForRead().toFile());

            for (String plateName : importHelper.getPlates())
            {
                for (Map.Entry<Integer, Plate> analytePlateEntry : importHelper.getAnalyteToPlate(plateName).entrySet())
                {
                    try
                    {
                        Plate plate = analytePlateEntry.getValue();
                        Integer spot = analytePlateEntry.getKey();
                        SimpleRegression regression = new SimpleRegression(true);
                        Map<String, Double> standardConcentrations = importHelper.getStandardConcentrations(plateName, analytePlateEntry.getKey());

                        CurveFit standardCurve = calculateStandardCurve(run, plate, regression, standardConcentrations, runDomain);
                        if (standardCurve != null && standardCurve.getParameters() == null)
                            throw new ExperimentException("Unable to fit the standard concentrations to a curve, please check the input data and try again");

                        Map<String, ExpMaterial> materialMap = data.getRun().getMaterialInputs().entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

                        Map<Position, String> specimenGroupMap = new HashMap<>();
                        for (WellGroup sample : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            for (Position pos : sample.getPositions())
                                specimenGroupMap.put(pos, sample.getName());
                        }

                        // create entries for the control wells
                        for (WellGroup controlGroup : plate.getWellGroups(WellGroup.Type.CONTROL))
                        {
                            for (WellGroup replicate : controlGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                            {
                                // gather concentrations and recovery for the replicate group so we can compute mean and CV stats
                                List<Double> concentrations = new ArrayList<>();
                                List<Double> recoveries = new ArrayList<>();
                                List<Map<String, Object>> replicateRows = new ArrayList<>();

                                for (Position position : replicate.getPositions())
                                {
                                    Well well = plate.getWell(position.getRow(), position.getColumn());
                                    Map<String, Object> row = importHelper.createWellRow(resultDomain, plateName, spot, controlGroup,
                                            replicate, well, position, standardCurve, materialMap);
                                    // don't record empty records
                                    if (isRowEmptyOrNull(row))
                                        continue;

                                    Double conc = (Double) row.get(ElisaAssayProvider.CONCENTRATION_PROPERTY);
                                    if (conc != null)
                                        concentrations.add(conc);

                                    if (standardConcentrations.containsKey(replicate.getPositionDescription()))
                                    {
                                        Double stdConc = standardConcentrations.get(replicate.getPositionDescription());
                                        // the assigned standard concentration
                                        if (stdConc != null)
                                            row.put(ElisaAssayProvider.STANDARD_CONCENTRATION_PROPERTY, stdConc);

                                        // percent recovery is the ratio of computed concentration to assigned
                                        if (conc != null && stdConc != null)
                                        {
                                            Double recovery = conc / stdConc;
                                            if (!recovery.isInfinite() && !recovery.isNaN())
                                            {
                                                row.put(ElisaAssayProvider.PERCENT_RECOVERY, recovery);
                                                recoveries.add(recovery);
                                            }
                                        }
                                    }
                                    replicateRows.add(row);
                                }

                                if (!concentrations.isEmpty())
                                {
                                    // compute stats on the replicate values
                                    MathStat concStat = StatsService.get().getStats(concentrations);
                                    MathStat recovStat = StatsService.get().getStats(recoveries);
                                    Double concCV = concStat.getStdDev() / concStat.getMean();

                                    // update rows and add to main collection
                                    for (Map<String, Object> row : replicateRows)
                                    {
                                        row.put(ElisaAssayProvider.CV_CONCENTRATION_PROPERTY, concCV);
                                        row.put(ElisaAssayProvider.MEAN_CONCENTRATION_PROPERTY, concStat.getMean());
                                        if (!recoveries.isEmpty())
                                            row.put(ElisaAssayProvider.PERCENT_RECOVERY_MEAN, recovStat.getMean());
                                    }
                                }
                                results.addAll(replicateRows);
                            }
                        }

                        // create entries for the sample wells
                        for (WellGroup sampleGroup : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                        {
                            List<DoublePoint> samplePoints = new ArrayList<>();
                            for (WellGroup replicate : sampleGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                            {
                                // gather concentrations for the replicate group so we can compute mean and CV stats
                                List<Double> concentrations = new ArrayList<>();
                                List<Map<String, Object>> replicateRows = new ArrayList<>();

                                for (Position position : replicate.getPositions())
                                {
                                    Well well = plate.getWell(position.getRow(), position.getColumn());
                                    Map<String, Object> row = importHelper.createWellRow(resultDomain, plateName, spot, sampleGroup,
                                            replicate, well, position, standardCurve, materialMap);
                                    // don't record empty records
                                    if (isRowEmptyOrNull(row))
                                        continue;

                                    Double conc = (Double)row.get(ElisaAssayProvider.CONCENTRATION_PROPERTY);
                                    if (conc != null)
                                    {
                                        concentrations.add(conc);
                                        samplePoints.add(new DoublePoint(conc, well.getValue()));
                                    }
                                    replicateRows.add(row);
                                }

                                if (!concentrations.isEmpty())
                                {
                                    // compute concentration mean and CV values
                                    MathStat stat = StatsService.get().getStats(concentrations);
                                    Double concCV = stat.getStdDev() / stat.getMean();

                                    // update rows and add to main collection
                                    for (Map<String, Object> row : replicateRows)
                                    {
                                        row.put(ElisaAssayProvider.CV_CONCENTRATION_PROPERTY, concCV);
                                        row.put(ElisaAssayProvider.MEAN_CONCENTRATION_PROPERTY, stat.getMean());
                                    }
                                }
                                results.addAll(replicateRows);
                            }
                            // compute sample scoped statistics
                            String materialKey = importHelper.getMaterialKey(plateName, spot, sampleGroup.getName());
                            calculateSampleStats(context.getUser(), materialMap.get(materialKey), sampleProperties, standardCurve, samplePoints);
                        }

                        // record the fit parameters and the r squared value for the standard curve
                        if (standardCurve != null && !Double.isNaN(standardCurve.getFitError()))
                        {
                            if (standardCurve.getParameters() != null)
                            {
                                CurveFitDb curveFitDb = new CurveFitDb();
                                curveFitDb.setRunId(run.getRowId());
                                curveFitDb.setProtocolId(protocol.getRowId());
                                curveFitDb.setPlateName(plateName);
                                curveFitDb.setSpot(spot);
                                curveFitDb.setFitParameters(standardCurve.getParameters().toJSON().toString());
                                curveFitDb.setrSquared(regression.getRSquare());

                                ElisaManager.saveCurveFit(context.getContainer(), context.getUser(), curveFitDb);
                            }
                        }
                    }
                    catch (FitFailedException | ValidationException e)
                    {
                        throw new ExperimentException(e);
                    }
                }
            }
        }
        Map<DataType, DataIteratorBuilder> datas = new HashMap<>();
        datas.put(getDataType(), MapDataIterator.of(results));

        return datas;
    }

    private boolean isRowEmptyOrNull(Map<String, Object> row)
    {
        // check if all values are null
        for (Object value : row.values())
        {
            if (value != null)
                return false;
        }
        return true;
    }

    /**
     * Calculates a curve fit to represent the calibration curve, the type of curve fit is determined by
     * the run level property. A simple regression object can be passed in to be populated by the same input
     * data and can be used to generate an R squared value.
     */
    @Nullable
    private CurveFit calculateStandardCurve(ExpRun run, Plate plate, @Nullable SimpleRegression regression, Map<String, Double> standardConcentrations,
                                            Domain runDomain) throws ExperimentException
    {
        // compute the calibration curve, there could be multiple control groups but one contains the standards
        WellGroup stdWellGroup = plate.getWellGroup(WellGroup.Type.CONTROL, STANDARDS_WELL_GROUP_NAME);
        if (stdWellGroup != null)
        {
            List<DoublePoint> points = new ArrayList<>();
            double maxValue = 0d;

            for (WellGroup replicate : stdWellGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
            {
                maxValue = replicate.getMean() > maxValue ? replicate.getMean() : maxValue;
            }

            for (WellGroup replicate : stdWellGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
            {
                double mean = replicate.getMean(); // / maxValue;
                double concentration;

                String key = replicate.getPositionDescription();
                if (standardConcentrations.containsKey(key))
                {
                    concentration = standardConcentrations.get(key);
                    points.add(new DoublePoint(concentration, mean));
                    regression.addData(concentration, mean);
                }
                else
                    LOG.info("Unable to find a standard concentration for the replicate well group : " + key);
            }

            if (!points.isEmpty())
            {
                points.sort(Comparator.comparing(o -> o.first));

                // Compute curve fit parameters based on the selected curve fit (default to linear for legacy assay designs)
                StatsService.CurveFitType curveFitType = ElisaManager.getRunCurveFitType(runDomain, run);
                CurveFit curveFit = StatsService.get().getCurveFit(curveFitType, points.toArray(DoublePoint[]::new));
                curveFit.setLogXScale(false);
                curveFit.setAssumeCurveDecreasing(false);

                return curveFit;
            }
            else
                return null;
        }
        else
            throw new ExperimentException("Standards well group does not exists in the plate template : " + plate.getName());
    }

    /**
     * Generate sample scoped stats for this run
     * @param material the sample to compute stats for
     * @param sampleProps map of sample domain properties
     * @param sampleValues the list of x,y pairs (concentration, absorption) for the sample wells
     */
    private void calculateSampleStats(User user, @Nullable ExpMaterial material, Map<String, DomainProperty> sampleProps,
                                      CurveFit curveFit, List<DoublePoint> sampleValues) throws FitFailedException, ValidationException
    {
        if (material != null)
        {
            if (sampleProps.containsKey(ElisaAssayProvider.AUC_PROPERTY) && curveFit != null)
            {
                CurveFit sampleCurveFit = StatsService.get().getCurveFit(curveFit.getType(), sampleValues.toArray(DoublePoint[]::new));
                // initialize with the standard curve params
                sampleCurveFit.setParameters(curveFit.getParameters());
                sampleCurveFit.setLogXScale(false);

                material.setProperty(user, sampleProps.get(ElisaAssayProvider.AUC_PROPERTY).getPropertyDescriptor(), sampleCurveFit.calculateAUC(StatsService.AUCType.NORMAL));
            }

            List<Double> absorption = sampleValues.stream()
                    .map(dp -> dp.second)
                    .collect(Collectors.toList());
            MathStat absStat = StatsService.get().getStats(absorption);
            if (sampleProps.containsKey(ElisaAssayProvider.MEAN_ABSORPTION_PROPERTY))
            {
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.MEAN_ABSORPTION_PROPERTY).getPropertyDescriptor(), absStat.getMean());
            }

            if (sampleProps.containsKey(ElisaAssayProvider.CV_ABSORPTION_PROPERTY))
            {
                double cv = absStat.getStdDev() / absStat.getMean();
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.CV_ABSORPTION_PROPERTY).getPropertyDescriptor(), cv);
            }

            List<Double> concentration = sampleValues.stream()
                    .map(dp -> dp.first)
                    .collect(Collectors.toList());
            MathStat concStat = StatsService.get().getStats(concentration);
            if (sampleProps.containsKey(ElisaAssayProvider.MEAN_CONCENTRATION_PROPERTY))
            {
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.MEAN_CONCENTRATION_PROPERTY).getPropertyDescriptor(), concStat.getMean());
            }

            if (sampleProps.containsKey(ElisaAssayProvider.CV_CONCENTRATION_PROPERTY))
            {
                double cv = concStat.getStdDev() / concStat.getMean();
                material.setProperty(user, sampleProps.get(ElisaAssayProvider.CV_CONCENTRATION_PROPERTY).getPropertyDescriptor(), cv);
            }
        }
    }

    @Override
    public void beforeDeleteData(List<ExpData> datas, User user) throws ExperimentException
    {
        try (DbScope.Transaction transaction = ElisaProtocolSchema.getSchema().getScope().ensureTransaction())
        {
            super.beforeDeleteData(datas, user);

            Set<Integer> runIds = new HashSet<>();
            for (ExpData data : datas)
            {
                if (null != data.getRunId())
                    runIds.add(data.getRunId());
            }

            if (!runIds.isEmpty())
            {
                SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("RunId"), runIds));
                Table.delete(ElisaProtocolSchema.getTableInfoCurveFit(), filter);
            }
            transaction.commit();
        }
    }
}
