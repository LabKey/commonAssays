/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.ms2.xarassay;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.common.util.Pair;

import java.io.File;
import java.util.*;

/**
 * User: Peter@labkey.com
 * Date: Oct 17, 2008
 * Time: 5:54:45 PM
 */


public class XarAssayProvider extends AbstractAssayProvider
{
    public static final String PROTOCOL_LSID_NAMESPACE_PREFIX = "MSSampleDescriptionProtocol";
    public static final String NAME = "Mass Spec Sample Description";
    public static final DataType MS_ASSAY_DATA_TYPE = new DataType("MZXMLData");
    public static final String PROTOCOL_LSID_OBJECTID_PREFIX = "FileType.mzXML";
    public static final String RUN_LSID_NAMESPACE_PREFIX = "ExperimentRun";
    public static final String RUN_LSID_OBJECT_ID_PREFIX = "MS2PreSearch";

    public static final String FRACTION_DOMAIN_PREFIX = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Fractions";
    public static final String FRACTION_SET_NAME = "FractionProperties";
    public static final String FRACTION_SET_LABEL = "If fraction properties are defined in this group, all mzXML files in the derctory will be described by fractions derived from the selected sample. ";
    
    public XarAssayProvider(String protocolLSIDPrefix, String runLSIDPrefix, DataType dataType)
    {
        super(protocolLSIDPrefix, runLSIDPrefix, dataType);
    }

    public XarAssayProvider()
    {
        super(PROTOCOL_LSID_NAMESPACE_PREFIX, RUN_LSID_NAMESPACE_PREFIX, MS_ASSAY_DATA_TYPE);
    }
    
    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        // don't call the standard upload set create because we don't want the target study or participant data resolver
        Domain domain = PropertyService.get().createDomain(c, getPresubstitutionLsid(ExpProtocol.ASSAY_DOMAIN_BATCH), "Batch Fields");
        domain.setDescription("The user is prompted for batch properties once for each set of runs they import. The run " +
                "set is a convenience to let users set properties that seldom change in one place and import many runs " +
                "using them. This is the first step of the import process.");

        return new Pair<Domain, Map<DomainProperty, Object>>(domain, Collections.<DomainProperty, Object>emptyMap());
    }
    
    protected void addInputMaterials(AssayRunUploadContext context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        int count = SampleChooserDisplayColumn.getSampleCount(context.getRequest(), 1);
        for (int i = 0; i < count; i++)
        {
            ExpMaterial material = SampleChooserDisplayColumn.getMaterial(i, context.getContainer(), context.getRequest());
            if (!material.getContainer().hasPermission(context.getUser(), ACL.PERM_READ))
            {
                throw new ExperimentException("You do not have permission to reference the sample '" + material.getName() + ".");
            }
            if (inputMaterials.containsKey(material))
            {
                throw new ExperimentException("The same material cannot be used multiple times");
            }
            inputMaterials.put(material, "Sample " + (i + 1));
        }
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createFractionDomain(Container c)
    {
        String domainLsid = getPresubstitutionLsid(FRACTION_DOMAIN_PREFIX);
        Domain fractionDomain = PropertyService.get().createDomain(c, domainLsid, FRACTION_SET_NAME);
        fractionDomain.setDescription(FRACTION_SET_LABEL);
        return new Pair<Domain, Map<DomainProperty, Object>>(fractionDomain, Collections.<DomainProperty, Object>emptyMap());
    }
    
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);
        result.add(createFractionDomain(c));
        return result;
    }

    public String getName()
    {
        return NAME;
    }

    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles)
    {
        return Collections.<AssayDataCollector>singletonList(new XarAssayDataCollector());
    }

    public ExpData getDataForDataRow(Object dataRowId)
    {
        throw new UnsupportedOperationException("Whoa how did i get here");
    }

    public TableInfo createDataTable(UserSchema schema, ExpProtocol protocol)
    {
        return new ExpSchema(schema.getUser(), schema.getContainer()).createDatasTable();
    }

    public ActionURL copyToStudy(User user, ExpProtocol protocol, Container study, Map<Integer, AssayPublishKey> dataKeys, List<String> errors)
    {
        throw new UnsupportedOperationException();
    }

    public String getDescription()
    {
        return "Describes metadata for mass spec data files, including mzXML";
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return null;
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return null;
    }

    @Override
    public boolean canCopyToStudy()
    {
        return false;
    }

    public FieldKey getParticipantIDFieldKey()
    {
        return null;
    }

    public FieldKey getVisitIDFieldKey(Container targetStudy)
    {
        return null;
    }

    public FieldKey getSpecimenIDFieldKey()
    {
        return null;
    }

    public FieldKey getRunIdFieldKeyFromDataRow()
    {
        return FieldKey.fromParts("RowId");
    }

    public FieldKey getDataRowIdFieldKey()
    {
        return FieldKey.fromParts("RowId");
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(container, null);
    }

    @NotNull
    protected ExpSampleSet getFractionSampleSet(AssayRunUploadContext context) throws ExperimentException
    {
        String domainURI = getDomainURIForPrefix(context.getProtocol(), FRACTION_DOMAIN_PREFIX);
        ExpSampleSet sampleSet=null;
        if (null != domainURI)
            sampleSet = ExperimentService.get().getSampleSet(domainURI);

        if (sampleSet == null)
        {
            sampleSet = ExperimentService.get().createSampleSet();
            sampleSet.setContainer(context.getProtocol().getContainer());
            sampleSet.setName("Fractions: " + context.getProtocol().getName());
            sampleSet.setLSID(domainURI);

            Lsid sampleSetLSID = new Lsid(domainURI);
            sampleSetLSID.setNamespacePrefix("Sample");
            sampleSetLSID.setNamespaceSuffix(context.getProtocol().getContainer().getRowId() + "." + context.getProtocol().getName());
            sampleSetLSID.setObjectId("");
            String prefix = sampleSetLSID.toString();

            sampleSet.setMaterialLSIDPrefix(prefix);
            sampleSet.save(context.getUser());
        }
        return sampleSet;
    }

    @Override
    protected void resolveExtraRunData(ParticipantVisitResolver resolver, AssayRunUploadContext context, Map<ExpMaterial, String> inputMaterials, Map<ExpData, String> inputDatas, Map<ExpMaterial, String> outputMaterials, Map<ExpData, String> outputDatas) throws ExperimentException
    {
        XarAssayForm form = (XarAssayForm)context;
        if (form.getSelectedDataCollector().getExistingAnnotationStatus(form).getValue().intValue() > 0)
        {
            throw new ExperimentException("You must delete any existing annotations before re-annotating.");
        }
        
        ExpSampleSet fractionSet = getFractionSampleSet(context);
        List<File> files = new ArrayList<File>(form.getUploadedData().values());
        MsFractionPropertyHelper helper = new MsFractionPropertyHelper(fractionSet, files, context.getContainer());
        Map<File, Map<DomainProperty, String>> mapFilesToFractionProperties = helper.getSampleProperties(context.getRequest());

        Map<ExpMaterial, String> derivedSamples = new HashMap<ExpMaterial, String>();

        try
        {
            for (Map.Entry<File,Map<DomainProperty, String>> entry : mapFilesToFractionProperties.entrySet())
            {
                // generate unique lsids for the derived samples
                File mzxmlFile = entry.getKey();
                String fileNameBase = mzxmlFile.getName().substring(0, (mzxmlFile.getName().lastIndexOf('.')));
                Map<DomainProperty, String> properties = entry.getValue();
                Lsid derivedLsid = new Lsid(fractionSet.getMaterialLSIDPrefix() + "OBJECT");
                derivedLsid.setObjectId(GUID.makeGUID());
                int index = 0;
                while(ExperimentService.get().getExpMaterial(derivedLsid.toString()) != null)
                    derivedLsid.setObjectId(derivedLsid.getObjectId() + "-" + ++index);

                ExpMaterial derivedMaterial = ExperimentService.get().createExpMaterial(form.getContainer()
                        , derivedLsid.toString(), "Fraction - " + fileNameBase);
                derivedMaterial.setCpasType(fractionSet.getLSID());
                // could put the fraction properties on the fraction material object or on the run.  decided to do the run

                for (Map.Entry<DomainProperty, String> property : properties.entrySet())
                {
                    String value = property.getValue();
                    derivedMaterial.setProperty(form.getUser(), property.getKey().getPropertyDescriptor(), value);
                }

                derivedSamples.put(derivedMaterial, "Fraction");
            }
            ViewBackgroundInfo info = new ViewBackgroundInfo(form.getContainer(), form.getUser(), form.getActionURL());
            ExperimentService.get().deriveSamples(inputMaterials, derivedSamples, info, null);
            inputMaterials.clear();
            inputMaterials.putAll(derivedSamples);
        }
        catch (ValidationException e)
        {
            throw new ExperimentException(e);
        }
    }
}
