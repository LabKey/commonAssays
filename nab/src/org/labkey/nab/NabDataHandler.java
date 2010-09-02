/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.nab;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.Pair;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 3:21:18 PM
 */
public class NabDataHandler extends AbstractNabDataHandler implements TransformDataHandler
{
    public static final DataType NAB_TRANSFORMED_DATA_TYPE = new DataType("AssayRunNabTransformedData"); // a marker data type

    class NabExcelParser implements NabDataFileParser
    {
        private ExpData _data;
        private File _dataFile;
        private ViewBackgroundInfo _info;
        private Logger _log;
        private XarContext _context;

        public NabExcelParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context)
        {
            _data = data;
            _dataFile = dataFile;
            _info = info;
            _log = log;
            _context = context;
        }

        public List<Map<String, Object>> getResults() throws ExperimentException
        {
            try {
                ExpRun run = _data.getRun();
                ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
                Container container = _data.getContainer();
                Luc5Assay assayResults = getAssayResults(run, _info.getUser(), _dataFile, null);
                List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
                Map<Integer, String> cutoffFormats = assayResults.getCutoffFormats();

                for (int summaryIndex = 0; summaryIndex < assayResults.getSummaries().length; summaryIndex++)
                {
                    DilutionSummary dilution = assayResults.getSummaries()[summaryIndex];
                    WellGroup group = dilution.getWellGroup();
                    ExpMaterial sampleInput = assayResults.getMaterial(group);

                    Map<String, Object> props = new HashMap<String, Object>();
                    results.add(props);

                    // generate curve ICs and AUCs for each curve fit type
                    for (DilutionCurve.FitType type : DilutionCurve.FitType.values())
                    {
                        for (Integer cutoff : assayResults.getCutoffs())
                        {
                            double value = dilution.getCutoffDilution(cutoff / 100.0, type);
                            saveICValue(getPropertyName(CURVE_IC_PREFIX, cutoff, type), value,
                                    dilution, protocol, container, cutoffFormats, props, type);

                            if (type == assayResults.getRenderedCurveFitType())
                            {
                                saveICValue(CURVE_IC_PREFIX + cutoff, value,
                                        dilution, protocol, container, cutoffFormats, props, type);
                            }
                        }
                        // compute both normal and positive AUC values
                        double auc = dilution.getAUC(type, DilutionCurve.AUCType.NORMAL);
                        if (!Double.isNaN(auc))
                        {
                            props.put(getPropertyName(AUC_PREFIX, type), auc);
                            if (type == assayResults.getRenderedCurveFitType())
                                props.put(AUC_PREFIX, auc);
                        }

                        double pAuc = dilution.getAUC(type, DilutionCurve.AUCType.POSITIVE);
                        if (!Double.isNaN(pAuc))
                        {
                            props.put(getPropertyName(pAUC_PREFIX, type), pAuc);
                            if (type == assayResults.getRenderedCurveFitType())
                                props.put(pAUC_PREFIX, pAuc);
                        }
                    }

                    // only need one set of interpolated ICs as they would be identical for all fit types
                    for (Integer cutoff : assayResults.getCutoffs())
                    {
                        saveICValue(POINT_IC_PREFIX + cutoff,
                                dilution.getInterpolatedCutoffDilution(cutoff / 100.0, assayResults.getRenderedCurveFitType()),
                                dilution, protocol, container, cutoffFormats, props, assayResults.getRenderedCurveFitType());
                    }
                    props.put(FIT_ERROR_PROPERTY, dilution.getFitError());
                    props.put(NAB_INPUT_MATERIAL_DATA_PROPERTY, sampleInput.getLSID());
                    props.put(WELLGROUP_NAME_PROPERTY, group.getName());
                }
                return results;
            }
            catch (DilutionCurve.FitFailedException e)
            {
                throw new ExperimentException(e.getMessage(), e);
            }
        }
    }

    @Override
    protected NabAssayRun createNabAssayRun(NabAssayProvider provider, ExpRun run, Plate plate, User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        return new SinglePlateNabAssayRun(provider, run, plate, user, cutoffs, renderCurveFitType);
    }

    public NabDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        return new NabExcelParser(data, dataFile, info, log, context);
    }

    public void importTransformDataMap(ExpData data, User user, ExpRun run, ExpProtocol protocol, AssayProvider provider, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        importRows(data, run, protocol, dataMap);
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        NabDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        datas.put(NAB_TRANSFORMED_DATA_TYPE, parser.getResults());

        return datas;
    }

    protected static void saveICValue(String name, double icValue, DilutionSummary dilution, ExpProtocol protocol,
                                      Container container, Map<Integer, String> formats, Map<String, Object> results, DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        String outOfRange = null;
        if (Double.NEGATIVE_INFINITY == icValue)
        {
            outOfRange = "<";
            icValue = dilution.getMinDilution(type);
        }
        else if (Double.POSITIVE_INFINITY == icValue)
        {
            outOfRange = ">";
            icValue = dilution.getMaxDilution(type);
        }
        results.put(name, icValue);
        results.put(name + OORINDICATOR_SUFFIX, outOfRange);
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
