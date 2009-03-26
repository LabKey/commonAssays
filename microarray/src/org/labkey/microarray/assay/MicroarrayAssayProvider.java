/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

import org.labkey.api.study.assay.*;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.microarray.pipeline.ArrayPipelineManager;
import org.labkey.microarray.*;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.labkey.common.util.Pair;
import org.fhcrc.cpas.exp.xml.SimpleTypeNames;
import org.springframework.web.servlet.mvc.Controller;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.io.File;
import java.io.IOException;

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
        super(PROTOCOL_PREFIX, "MicroarrayAssayRun", MicroarrayModule.MAGE_ML_DATA_TYPE);
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        return new Pair<Domain, Map<DomainProperty, Object>>(
            PropertyService.get().createDomain(c, getPresubstitutionLsid(ExpProtocol.ASSAY_DOMAIN_BATCH), "Batch Fields"),
            Collections.<DomainProperty, Object>emptyMap());
    }

    public String getName()
    {
        return NAME;
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The MAGEML data file is an XML file that contains the results of the microarray run.");
    }

    public TableInfo createDataTable(UserSchema schema, ExpProtocol protocol)
    {
        RunDataTable result = new RunDataTable(schema, protocol);
        if (getDomainByPrefix(protocol, ExpProtocol.ASSAY_DOMAIN_DATA).getProperties().length > 0)
        {
            List<FieldKey> cols = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
            Iterator<FieldKey> iterator = cols.iterator();
            while (iterator.hasNext())
            {
                FieldKey key = iterator.next();
                if ("Run".equals(key.getParts().get(0)))
                {
                    iterator.remove();
                }
            }
            result.setDefaultVisibleColumns(cols);
        }

        return result;
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
        result.add(new Pair<Domain, Map<DomainProperty,  Object>>(dataDomain, Collections.<DomainProperty, Object>emptyMap()));
        return result;
    }

    public FieldKey getParticipantIDFieldKey()
    {
        return FieldKey.fromParts(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
    }

    public FieldKey getVisitIDFieldKey(Container targetStudy)
    {
        return FieldKey.fromParts(AbstractAssayProvider.VISITID_PROPERTY_NAME);
    }

    public FieldKey getSpecimenIDFieldKey()
    {
        return FieldKey.fromParts("SpecimenId");
    }

    public boolean canCopyToStudy()
    {
        return true;
    }

    public FieldKey getRunIdFieldKeyFromDataRow()
    {
        return FieldKey.fromParts("Run", "RowId");
    }

    public FieldKey getDataRowIdFieldKey()
    {
        return FieldKey.fromParts("ObjectId");
    }

    protected void addOutputDatas(AssayRunUploadContext context, Map<ExpData, String> outputDatas, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        try
        {
            Map<String, File> files = context.getUploadedData();
            assert files.size() == 1;
            File mageMLFile = files.values().iterator().next();
            ExpData mageData = createData(context.getContainer(), mageMLFile, mageMLFile.getName(), MicroarrayModule.MAGE_ML_DATA_TYPE);

            outputDatas.put(mageData, "MageML");
            String baseName = ArrayPipelineManager.getBaseMageName(mageMLFile.getName());
            if (baseName != null)
            {
                File imageFile = new File(mageMLFile.getParentFile(), baseName + ".jpg");
                if (NetworkDrive.exists(imageFile))
                {
                    ExpData imageData = createData(context.getContainer(), imageFile, imageFile.getName(), MicroarrayModule.IMAGE_DATA_TYPE);
                    outputDatas.put(imageData, "ThumbnailImage");
                }

                File qcFile = new File(mageMLFile.getParentFile(), baseName + ".pdf");
                if (NetworkDrive.exists(qcFile))
                {
                    ExpData qcData = createData(context.getContainer(), qcFile, qcFile.getName(), MicroarrayModule.QC_REPORT_DATA_TYPE);
                    outputDatas.put(qcData, "QCReport");
                }

                File featuresFile = new File(mageMLFile.getParentFile(), baseName + "_feat.csv");
                if (NetworkDrive.exists(featuresFile))
                {
                    ExpData featuresData = createData(context.getContainer(), featuresFile, featuresFile.getName(), MicroarrayModule.FEATURES_DATA_TYPE);
                    outputDatas.put(featuresData, "Features");
                }

                File gridFile = new File(mageMLFile.getParentFile(), baseName + "_grid.csv");
                if (NetworkDrive.exists(gridFile))
                {
                    ExpData gridData = createData(context.getContainer(), gridFile, gridFile.getName(), MicroarrayModule.GRID_DATA_TYPE);
                    outputDatas.put(gridData, "Grid");
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles)
    {
        return Collections.<AssayDataCollector>singletonList(new PipelineDataCollector());
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new StudyParticipantVisitResolverType(), new ThawListResolverType());
    }

    public ExpRunTable createRunTable(UserSchema schema, ExpProtocol protocol)
    {
        ExpRunTable result = new MicroarraySchema(schema.getUser(), schema.getContainer()).createRunsTable();
        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts(ExpRunTable.Column.Flag.name()));
        defaultCols.add(FieldKey.fromParts(ExpRunTable.Column.Links.name()));
        defaultCols.add(FieldKey.fromParts(MicroarraySchema.THUMBNAIL_IMAGE_COLUMN_NAME));
        defaultCols.add(FieldKey.fromParts(MicroarraySchema.QC_REPORT_COLUMN_NAME));
        defaultCols.add(FieldKey.fromParts(ExpRunTable.Column.Name.name()));
        result.setDefaultVisibleColumns(defaultCols);

        return result;
    }

    public Map<String, Class<? extends Controller>> getImportActions()
    {
        return Collections.<String, Class<? extends Controller>>singletonMap(IMPORT_DATA_LINK_NAME, MicroarrayUploadWizardAction.class);
    }

    protected void addInputMaterials(AssayRunUploadContext context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        MicroarrayRunUploadForm form = (MicroarrayRunUploadForm)context;
        int count = form.getSampleCount(form.getCurrentMageML());
        for (int i = 0; i < count; i++)
        {
            ExpMaterial material = form.getSample(i);
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

    public boolean allowUpload(User user, Container container, ExpProtocol protocol)
    {
        // Microarray module expects MageML files to already be on the server's file system
        return false;
    }

    public HttpView getDisallowedUploadMessageView(User user, Container container, ExpProtocol protocol)
    {
        HttpView result = super.getDisallowedUploadMessageView(user, container, protocol);
        if (result == null)
        {
            String message = "To upload Microarray runs, browse to the MageML files using the <a href=\"" +
                    PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(container, null) +
                    "\">data pipeline</a> or use the <a href=\"" +
                    MicroarrayController.getPendingMageMLFilesURL(container)
                    + "\">pending MageML files list</a>.";
            result = new HtmlView(message);
        }
        return result;
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
        List<ProtocolParameter> params = new ArrayList<ProtocolParameter>(result.getKey().getProtocolParameters().values());

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
        Map<DomainProperty, XPathExpression> result = new HashMap<DomainProperty, XPathExpression>();

        Domain domain = getRunInputDomain(protocol);
        for (DomainProperty runPD : domain.getProperties())
        {
            String expression = runPD.getDescription();
            if (expression != null)
            {
                // We use the description of the property descriptor as the XPath. Far from ideal.
                try
                {
                    XPathFactory factory = XPathFactory.newInstance();
                    XPath xPath = factory.newXPath();
                    XPathExpression xPathExpression = xPath.compile(expression);

                    result.put(runPD, xPathExpression);
                }
                catch (XPathExpressionException e)
                {
                    // User isn't required to use the description as an XPath
                }
            }
        }
        return result;
    }

    @Override
    public RunListQueryView createRunQueryView(ViewContext context, ExpProtocol protocol)
    {
        RunListQueryView result = super.createRunQueryView(context, protocol);
        result.setShowAddToRunGroupButton(true);
        return result;
    }
}
