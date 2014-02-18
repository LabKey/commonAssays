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
package org.labkey.nab;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.dilution.SampleProperty;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformResult;
import org.labkey.api.security.User;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.actions.PlateUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jeckels
 * Date: May 8, 2012
 */
public class LegacyNAbUploadContext implements PlateUploadForm<NabAssayProvider>
{
    private final ExpProtocol _protocol;
    private final NabAssayProvider _provider;
    private final Map<String, String> _params;
    private final OldNabAssayRun _legacyRun;
    private final ViewBackgroundInfo _info;
    private File _dataFile;
    private PlateSamplePropertyHelper _samplePropertyHelper;
    public static final String LEGACY_ID_PROPERTY_NAME = "LegacyId";

    public LegacyNAbUploadContext(ExpProtocol protocol, NabAssayProvider provider, int plateRowId, ViewBackgroundInfo info, Map<String, String> params) throws Exception
    {
        _protocol = protocol;
        _info = info;
        _provider = provider;
        _params = params;

        _legacyRun = OldNabManager.get().loadFromDatabase(info.getUser(), info.getContainer(), plateRowId);
    }

    @Override
    public Integer getReRunId()
    {
        return null;
    }

    public PlateSamplePropertyHelper getSamplePropertyHelper()
    {
        if (_samplePropertyHelper == null)
        {
            _samplePropertyHelper = new PlateSamplePropertyHelper(_provider.getSampleWellGroupDomain(_protocol).getProperties(), _provider.getPlateTemplate(_protocol.getContainer(), _protocol))
            {
                @Override
                public Map<String, Map<DomainProperty, String>> getSampleProperties(HttpServletRequest request) throws ExperimentException
                {
                    Map<String, Map<DomainProperty, String>> result = new HashMap<>();
                    int index = 0;
                    for (String sampleName : getSampleNames())
                    {
                        DilutionSummary dilutionSummary = findDilutionSummary(sampleName);
                        if (dilutionSummary.getWellGroups().size() != 1)
                        {
                            throw new IllegalStateException("Expected one well group but found " + dilutionSummary.getWellGroups().size());
                        }
                        Map<DomainProperty, String> props = new HashMap<>();
                        for (DomainProperty property : _provider.getSampleWellGroupDomain(_protocol).getProperties())
                        {
                            if (NabAssayProvider.SAMPLE_INITIAL_DILUTION_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
                            {
                                props.put(property, Double.toString(dilutionSummary.getInitialDilution()));
                            }
                            if (NabAssayProvider.SAMPLE_DILUTION_FACTOR_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
                            {
                                props.put(property, Double.toString(dilutionSummary.getFactor()));
                            }
                            if (NabAssayProvider.SAMPLE_METHOD_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
                            {
                                props.put(property, dilutionSummary.getMethod().name());
                            }
                            if (NabAssayProvider.SAMPLE_DESCRIPTION_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
                            {
                                props.put(property, dilutionSummary.getSampleDescription());
                            }
                            if (NabAssayProvider.SPECIMENID_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
                            {
                                Object value = dilutionSummary.getWellGroups().get(0).getProperty(SampleProperty.SampleId.name());
                                props.put(property, value == null ? null : value.toString());
                            }
                        }

                        result.put(getObject(index++, props), props);
                    }

                    return result;
                }
            };
        }
        return _samplePropertyHelper;
    }

    /** Look through all the DilutionSummaries for this run and find one whose name matches */
    private DilutionSummary findDilutionSummary(String sampleName)
    {
        for (DilutionSummary dilutionSummary : _legacyRun.getSummaries())
        {
            for (WellGroup wellGroup : dilutionSummary.getWellGroups())
            {
                if (wellGroup.getName().equals(sampleName))
                {
                    return dilutionSummary;
                }
            }
        }
        throw new IllegalStateException("Unmatched sample name: " + sampleName);
    }

    public void setSamplePropertyHelper(PlateSamplePropertyHelper helper)
    {
        _samplePropertyHelper = helper;
    }

    public OldNabAssayRun getLegacyRun()
    {
        return _legacyRun;
    }

    @NotNull
    @Override
    public ExpProtocol getProtocol()
    {
        return _protocol;
    }

    @Override
    public Map<DomainProperty, String> getRunProperties() throws ExperimentException
    {
        return findProperties(_provider.getRunDomain(_protocol));
    }

    @Override
    public Map<DomainProperty, String> getBatchProperties()
    {
        return findProperties(_provider.getBatchDomain(_protocol));
    }

    private Map<DomainProperty, String> findProperties(Domain domain)
    {
        Map<DomainProperty, String> result = new HashMap<>();
        for (DomainProperty property : domain.getProperties())
        {
            // Handle all of the "built-in" NAb run/batch properties
            if (AbstractAssayProvider.PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, new AbstractPlateBasedAssayProvider.SpecimenIDLookupResolverType().getName());
            }
            else if (NabAssayProvider.VIRUS_NAME_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getVirusName());
            }
            else if (NabAssayProvider.VIRUS_ID_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getVirusId());
            }
            else if (NabAssayProvider.HOST_CELL_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getHostCell());
            }
            else if (NabAssayProvider.STUDY_NAME_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getStudyName());
            }
            else if (NabAssayProvider.EXPERIMENT_PERFORMER_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getExperimentPerformer());
            }
            else if (NabAssayProvider.EXPERIMENT_ID_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getExperimentId());
            }
            else if (NabAssayProvider.INCUBATION_TIME_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                String value = _legacyRun.getIncubationTime();
                if ( value != null && PropertyType.INTEGER.getTypeUri().equals(property.getRangeURI()))
                {
                    value = parseIncubationTime(value);
                }
                result.put(property, value);
            }
            else if (NabAssayProvider.PLATE_NUMBER_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getPlateNumber());
            }
            else if (NabAssayProvider.EXPERIMENT_DATE_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, DateUtil.formatDate(property.getContainer(), _legacyRun.getExperimentDate()));
            }
            else if (NabAssayProvider.FILE_ID_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getFileId());
            }
            else if (NabAssayProvider.LOCK_AXES_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, Boolean.toString(_legacyRun.isLockAxes()));
            }
            else if (NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getRenderedCurveFitType().getLabel());
            }
            else if (LEGACY_ID_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getPlate().getRowId().toString());
            }
            else
            {
                boolean foundCutoff = false;
                for (int i = 0; i < NabAssayProvider.CUTOFF_PROPERTIES.length && i < _legacyRun.getCutoffs().length && !foundCutoff; i++)
                {
                    if (NabAssayProvider.CUTOFF_PROPERTIES[i].equalsIgnoreCase(property.getName()))
                    {
                        result.put(property, Integer.toString(_legacyRun.getCutoffs()[i]));
                        foundCutoff = true;
                    }
                }
                if (!foundCutoff)
                {
                    result.put(property, _params.get(ColumnInfo.propNameFromName(property.getName())));
                }
            }
        }
        return result;
    }

    private static String parseIncubationTime(String value)
    {
        if (value == null || value.trim().length() == 0)
        {
            return null;
        }
        try
        {
            value = new Integer(value).toString();
        }
        catch (NumberFormatException e)
        {
            StringBuilder digits = new StringBuilder();
            int index = 0;
            while (index < value.length())
            {
                char c = value.charAt(index++);
                if (Character.isDigit(c))
                {
                    digits.append(c);
                }
                else
                {
                    break;
                }
            }
            if (digits.length() == 0)
            {
                throw new NumberFormatException("Unable to parse IncubationTime value '" + value + "'");
            }
            int num = Integer.parseInt(digits.toString());
            if (value.indexOf("day") != -1)
            {
                num *= 24;
            }
            value = Integer.toString(num);
        }
        return value;
    }

    @Override
    public String getComments()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return _legacyRun.getName();
    }

    @Override
    public User getUser()
    {
        return _info.getUser();
    }

    @NotNull
    @Override
    public Container getContainer()
    {
        return _info.getContainer();
    }

    @Override
    public HttpServletRequest getRequest()
    {
        return null;
    }

    @Override
    public ActionURL getActionURL()
    {
        return _info.getURL();
    }

    @NotNull
    @Override
    public Map<String, File> getUploadedData() throws ExperimentException
    {
        return Collections.singletonMap(AssayDataCollector.PRIMARY_FILE, _dataFile);
    }

    public Map<String, String> getSpecimensToLSIDs()
    {
        Map<String, String> result = new HashMap<>();
        for (DilutionSummary dilutionSummary : _legacyRun.getSummaries())
        {
            for (WellGroup wellGroup : dilutionSummary.getWellGroups())
            {
                result.put(wellGroup.getName(), wellGroup.getLSID());
            }
        }
        return result;
    }

    @Override
    public NabAssayProvider getProvider()
    {
        return _provider;
    }

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain, String scope) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveDefaultValues(Map<DomainProperty, String> values, String scope) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveDefaultBatchValues() throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveDefaultRunValues() throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearDefaultValues(Domain domain) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTargetStudy()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransformResult getTransformResult()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransformResult(TransformResult result)
    {
        throw new UnsupportedOperationException();
    }

    public void setFile(File dataFile)
    {
        _dataFile = dataFile;
    }

    @Override
    public void uploadComplete(ExpRun run) throws ExperimentException
    {
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testIncubationTimeParsing()
        {
            assertEquals("25", parseIncubationTime("25.5"));
            assertEquals("25", parseIncubationTime("25.3"));
            assertEquals(null, parseIncubationTime(null));
            assertEquals("", parseIncubationTime(""));
            assertEquals("12", parseIncubationTime("12:15"));
            assertEquals("48", parseIncubationTime("2 days"));
            assertEquals("48", parseIncubationTime("48"));
            assertEquals("48", parseIncubationTime("48hrs"));
            assertEquals("48", parseIncubationTime("48 hrs"));
            assertEquals("48", parseIncubationTime("48 Hrs"));
            assertEquals("48", parseIncubationTime("48 Hours"));
            assertEquals("48", parseIncubationTime("48Hrs."));
            try
            {
                parseIncubationTime("aa");
                fail("Shouldn't have parsed successfully");
            }
            catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public Logger getLogger()
    {
        return null;
    }
}
