package org.labkey.nab;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformResult;
import org.labkey.api.security.User;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
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
import java.io.IOException;
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
    private final OldNabAssayRun _legacyRun;
    private final ViewBackgroundInfo _info;
    private File _dataFile;
    private PlateSamplePropertyHelper _samplePropertyHelper;

    public LegacyNAbUploadContext(ExpProtocol protocol, NabAssayProvider provider, int plateRowId, ViewBackgroundInfo info) throws Exception
    {
        _protocol = protocol;
        _info = info;
        _provider = provider;

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
                public Map<WellGroupTemplate, Map<DomainProperty, String>> getSampleProperties(HttpServletRequest request) throws ExperimentException
                {
                    Map<WellGroupTemplate, Map<DomainProperty, String>> result = new HashMap<WellGroupTemplate, Map<DomainProperty, String>>();
                    int index = 0;
                    for (String sampleName : getSampleNames())
                    {
                        DilutionSummary dilutionSummary = findDilutionSummary(sampleName);
                        if (dilutionSummary.getWellGroups().size() != 1)
                        {
                            throw new IllegalStateException("Expected one well group but found " + dilutionSummary.getWellGroups().size());
                        }
                        Map<DomainProperty, String> props = new HashMap<DomainProperty, String>();
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
                if (wellGroup.getName().equals(wellGroup.getName()))
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
        Map<DomainProperty, String> result = new HashMap<DomainProperty, String>();
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
                result.put(property, _legacyRun.getIncubationTime());
            }
            else if (NabAssayProvider.PLATE_NUMBER_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getPlateNumber());
            }
            else if (NabAssayProvider.EXPERIMENT_DATE_PROPERTY_NAME.equalsIgnoreCase(property.getName()))
            {
                result.put(property, DateUtil.formatDate(_legacyRun.getExperimentDate()));
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
            else if ("LegacyId".equalsIgnoreCase(property.getName()))
            {
                result.put(property, _legacyRun.getPlate().getRowId().toString());
            }
            else
            {
                for (int i = 0; i < NabAssayProvider.CUTOFF_PROPERTIES.length && i < _legacyRun.getCutoffs().length; i++)
                {
                    if (NabAssayProvider.CUTOFF_PROPERTIES[i].equalsIgnoreCase(property.getName()))
                    {
                        result.put(property, Integer.toString(_legacyRun.getCutoffs()[i]));
                    }
                }
            }
        }
        return result;
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
    public Map<String, File> getUploadedData() throws IOException, ExperimentException
    {
        return Collections.singletonMap(AssayDataCollector.PRIMARY_FILE, _dataFile);
    }

    public Map<String, String> getSpecimensToLSIDs()
    {
        Map<String, String> result = new HashMap<String, String>();
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
}
