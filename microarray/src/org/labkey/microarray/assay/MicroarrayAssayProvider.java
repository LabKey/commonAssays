/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import org.fhcrc.cpas.exp.xml.SimpleTypeNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.LsidManager;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractTsvAssayProvider;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PipelineDataCollector;
import org.labkey.api.study.assay.StudyParticipantVisitResolverType;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.microarray.controllers.MicroarrayController;
import org.labkey.microarray.MicroarrayModule;
import org.labkey.microarray.MicroarrayUploadWizardAction;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.springframework.web.servlet.mvc.Controller;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jan 2, 2008
 */
public class MicroarrayAssayProvider extends AbstractTsvAssayProvider
{
    public static final String PROTOCOL_PREFIX = "MicroarrayAssayProtocol";
    public static final String NAME = "Microarray";

    public static final int MAX_SAMPLE_COUNT = 2;
    public static final int MIN_SAMPLE_COUNT = 1;

    private static final String DEFAULT_CHANNEL_COUNT_XPATH = "/MAGE-ML/BioAssay_package/BioAssay_assnlist/MeasuredBioAssay/FeatureExtraction_assn/FeatureExtraction/ProtocolApplications_assnlist/ProtocolApplication/SoftwareApplications_assnlist/SoftwareApplication/ParameterValues_assnlist/ParameterValue[ParameterType_assnref/Parameter_ref/@identifier='Agilent.BRS:Parameter:Scan_NumChannels']/@value";
    private static final String DEFAULT_BARCODE_XPATH = "/MAGE-ML/BioAssay_package/BioAssay_assnlist/MeasuredBioAssay/FeatureExtraction_assn/FeatureExtraction/ProtocolApplications_assnlist/ProtocolApplication/SoftwareApplications_assnlist/SoftwareApplication/ParameterValues_assnlist/ParameterValue[ParameterType_assnref/Parameter_ref/@identifier='Agilent.BRS:Parameter:FeatureExtractor_Barcode']/@value";

    public MicroarrayAssayProvider()
    {
        super(PROTOCOL_PREFIX, "MicroarrayAssayRun", MicroarrayModule.MAGE_ML_INPUT_TYPE, ModuleLoader.getInstance().getModule(MicroarrayModule.class));
    }

    @NotNull
    @Override
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                null,
                FieldKey.fromParts("Run"),
                FieldKey.fromParts(AbstractTsvAssayProvider.ROW_ID_COLUMN_NAME)
        );
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        return new Pair<>(
            PropertyService.get().createDomain(c, getPresubstitutionLsid(ExpProtocol.ASSAY_DOMAIN_BATCH), "Batch Fields"),
            Collections.emptyMap());
    }

    public String getName()
    {
        return NAME;
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The MAGE-ML data file is an XML file that contains the results of the microarray run.");
    }

    public void registerLsidHandler()
    {
        // Since we don't usually load the actual data from microarray runs, send the user to the text
        // view of the experiment run so that they can download the files directly to analyte locally
        LsidManager.get().registerHandler(_runLSIDPrefix, new LsidManager.ExpRunLsidHandler()
        {
            @Override
            protected ActionURL getDisplayURL(Container c, ExpProtocol protocol, ExpRun run)
            {
                return PageFlowUtil.urlProvider(ExperimentUrls.class).getRunTextURL(run);
            }
        });
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createRunDomain(c, user);
        result.getKey().setDescription(result.getKey().getDescription() + " You may enter an XPath expression in the description for the property. If you do, when uploading a run the server will look in the MAGEML file for the value.");
        return result;
    }

    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);
        Domain dataDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ExpProtocol.ASSAY_DOMAIN_DATA + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":" + ASSAY_NAME_SUBSTITUTION, "Data Properties");
        dataDomain.setDescription("The user is prompted to select a MAGEML file that contains the data values. If the spot-level data within the file contains a column that matches the data column name here, it will be imported.");
        result.add(new Pair<>(dataDomain, Collections.emptyMap()));
        return result;
    }

    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles, AssayRunUploadForm context)
    {
        return Collections.singletonList(new PipelineDataCollector());
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new StudyParticipantVisitResolverType(), new ThawListResolverType());
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new MicroarrayProtocolSchema(user, container, this, protocol, targetStudy);
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, MicroarrayUploadWizardAction.class);
    }

    @Override
    public AssayRunCreator getRunCreator()
    {
        return new MicroarrayRunCreator(this);
    }

    public Class<? extends Controller> getDesignerAction()
    {
        return MicroarrayController.DesignerAction.class;
    }

    public String getDescription()
    {
        return "Imports microarray runs from MageML files.";
    }

    public Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> getAssayTemplate(User user, Container targetContainer)
    {
        Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> result = super.getAssayTemplate(user, targetContainer);
        List<ProtocolParameter> params = new ArrayList<>(result.getKey().getProtocolParameters().values());

        ProtocolParameter channelCountXPathParam = new ProtocolParameter();
        channelCountXPathParam.setOntologyEntryURI(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI);
        channelCountXPathParam.setName("ChannelCountXPath");
        channelCountXPathParam.setValue(SimpleTypeNames.STRING, DEFAULT_CHANNEL_COUNT_XPATH);
        params.add(channelCountXPathParam);

        ProtocolParameter barcodeXPathParam = new ProtocolParameter();
        barcodeXPathParam.setOntologyEntryURI(MicroarrayAssayDesigner.BARCODE_PARAMETER_URI);
        barcodeXPathParam.setName("BarcodeXPath");
        barcodeXPathParam.setValue(SimpleTypeNames.STRING, DEFAULT_BARCODE_XPATH);
        params.add(barcodeXPathParam);

        ProtocolParameter barcodeFieldNameParam = new ProtocolParameter();
        barcodeFieldNameParam.setOntologyEntryURI(MicroarrayAssayDesigner.BARCODE_FIELD_NAMES_PARAMETER_URI);
        barcodeFieldNameParam.setName("BarcodeFieldNames");
        barcodeFieldNameParam.setValue(SimpleTypeNames.STRING, "Barcode");
        params.add(barcodeFieldNameParam);

        ProtocolParameter cy3Param = new ProtocolParameter();
        cy3Param.setOntologyEntryURI(MicroarrayAssayDesigner.CY3_SAMPLE_NAME_COLUMN_PARAMETER_URI);
        cy3Param.setName("Cy3SampleNameColumn");
        cy3Param.setValue(SimpleTypeNames.STRING, "ProbeID_Cy3");
        params.add(cy3Param);

        ProtocolParameter cy5Param = new ProtocolParameter();
        cy5Param.setOntologyEntryURI(MicroarrayAssayDesigner.CY5_SAMPLE_NAME_COLUMN_PARAMETER_URI);
        cy5Param.setName("Cy5SampleNameColumn");
        cy5Param.setValue(SimpleTypeNames.STRING, "ProbeID_Cy5");
        params.add(cy5Param);

        result.getKey().setProtocolParameters(params);
        return result;
    }

    public Map<DomainProperty, XPathExpression> getXpathExpressions(ExpProtocol protocol)
    {
        Map<DomainProperty, XPathExpression> result = new HashMap<>();

        Domain domain = getRunDomain(protocol);
        for (DomainProperty runPD : domain.getProperties())
        {
            XPathExpression xpath = getXPath(runPD);
            if (xpath != null)
            {
                result.put(runPD, xpath);
            }
        }
        return result;
    }

    public static XPathExpression getXPath(DomainProperty runPD)
    {
        String expression = runPD.getDescription();
        if (expression != null)
        {
            // We use the description of the property descriptor as the XPath. Far from ideal.
            try
            {
                XPathFactory factory = XPathFactory.newInstance();
                XPath xPath = factory.newXPath();
                return xPath.compile(expression);
            }
            catch (XPathExpressionException e)
            {
                // User isn't required to use the description as an XPath
            }
        }
        return null;
    }

    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(MicroarrayModule.class,
                new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()), this, "Import MAGE-ML");
    }

    @Override
    public boolean isEditableRuns(ExpProtocol protocol)
    {
        // Override to make default value true
        Boolean b = getBooleanProperty(protocol, EDITABLE_RUNS_PROPERTY_SUFFIX);
        return b == null || b.booleanValue();
    }

    @NotNull
    @Override
    public List<AssayDataType> getRelatedDataTypes()
    {
        return MicroarrayModule.RELATED_INPUT_TYPES;
    }
}
