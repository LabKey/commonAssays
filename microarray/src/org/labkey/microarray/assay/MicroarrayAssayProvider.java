package org.labkey.microarray.assay;

import org.labkey.api.study.assay.*;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.util.NetworkDrive;
import org.labkey.microarray.pipeline.ArrayPipelineManager;
import org.labkey.microarray.MicroarrayModule;
import org.labkey.microarray.MicroarraySchema;
import org.labkey.microarray.MicroarrayUploadWizardAction;
import org.labkey.microarray.sampleset.client.SampleInfo;
import org.labkey.microarray.sampleset.client.SampleChooser;

import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Jan 2, 2008
 */
public class MicroarrayAssayProvider extends AbstractAssayProvider
{
    public static final String PROTOCOL_PREFIX = "MicroarrayAssayProtocol";
    public static final String NAME = "Microarray";

    public static final int MAX_SAMPLE_COUNT = 2;
    public static final int MIN_SAMPLE_COUNT = 1;

    public MicroarrayAssayProvider()
    {
        super(PROTOCOL_PREFIX, "MicroarrayAssayRun", MicroarrayModule.MAGE_ML_DATA_TYPE);
    }

    public ExpData getDataForDataRow(Object dataRowId)
    {
        throw new UnsupportedOperationException();
    }

    protected Domain createUploadSetDomain(Container c, User user)
    {
        return PropertyService.get().createDomain(c, getPresubstitutionLsid(ExpProtocol.ASSAY_DOMAIN_UPLOAD_SET), "Upload Set Fields");
    }

    public String getName()
    {
        return NAME;
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The MAGEML data file is an XML file that contains the results of the microarray run.");
    }

    public TableInfo createDataTable(QuerySchema schema, String alias, ExpProtocol protocol)
    {
        RunDataTable result = new RunDataTable(schema, alias, protocol);
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
        return result;
    }

    protected Domain createRunDomain(Container c, User user)
    {
        Domain result = super.createRunDomain(c, user);
        result.setDescription(result.getDescription() + " You may enter an XPath expression in the description for the property. If you do, when uploading a run the server will look in the MAGEML file for the value.");
        return result;
    }

    public List<Domain> createDefaultDomains(Container c, User user)
    {
        List<Domain> result = super.createDefaultDomains(c, user);
        Domain dataDomain = PropertyService.get().createDomain(c, "urn:lsid:${LSIDAuthority}:" + ExpProtocol.ASSAY_DOMAIN_DATA + ".Folder-${Container.RowId}:" + ASSAY_NAME_SUBSTITUTION, "Data Properties");
        dataDomain.setDescription("The user is prompted to select a MAGEML file that contains the data values. If the spot-level data within the file contains a column that matches the data column name here, it will be imported.");
        result.add(dataDomain);
        return result;
    }

    public FieldKey getParticipantIDFieldKey()
    {
        throw new UnsupportedOperationException();
    }

    public FieldKey getVisitIDFieldKey(Container targetStudy)
    {
        throw new UnsupportedOperationException();
    }

    public FieldKey getSpecimenIDFieldKey()
    {
        throw new UnsupportedOperationException();
    }

    public boolean canPublish()
    {
        return false;
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
            ExpData mageData = createData(context.getContainer(), mageMLFile, MicroarrayModule.MAGE_ML_DATA_TYPE);

            outputDatas.put(mageData, "MageML");
            String baseName = ArrayPipelineManager.getBaseMageName(mageMLFile.getName());
            if (baseName != null)
            {
                File imageFile = new File(mageMLFile.getParentFile(), baseName + ".jpg");
                if (NetworkDrive.exists(imageFile))
                {
                    ExpData imageData = createData(context.getContainer(), imageFile, MicroarrayModule.IMAGE_DATA_TYPE);
                    outputDatas.put(imageData, "ThumbnailImage");
                }

                File qcFile = new File(mageMLFile.getParentFile(), baseName + ".pdf");
                if (NetworkDrive.exists(qcFile))
                {
                    ExpData qcData = createData(context.getContainer(), qcFile, MicroarrayModule.QC_REPORT_DATA_TYPE);
                    outputDatas.put(qcData, "QCReport");
                }

                File featuresFile = new File(mageMLFile.getParentFile(), baseName + "_feat.csv");
                if (NetworkDrive.exists(featuresFile))
                {
                    ExpData featuresData = createData(context.getContainer(), featuresFile, MicroarrayModule.FEATURES_DATA_TYPE);
                    outputDatas.put(featuresData, "Features");
                }

                File gridFile = new File(mageMLFile.getParentFile(), baseName + "_grid.csv");
                if (NetworkDrive.exists(gridFile))
                {
                    ExpData gridData = createData(context.getContainer(), gridFile, MicroarrayModule.GRID_DATA_TYPE);
                    outputDatas.put(gridData, "Grid");
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    public ActionURL publish(User user, ExpProtocol protocol, Container study, Set<AssayPublishKey> dataKeys, List<String> errors)
    {
        throw new UnsupportedOperationException();
    }

    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles)
    {
        return Collections.<AssayDataCollector>singletonList(new PipelineDataCollector());
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new StudyParticipantVisitResolverType(), new ThawListResolverType());
    }

    public ExpRunTable createRunTable(QuerySchema schema, String alias, ExpProtocol protocol)
    {
        ExpRunTable result = new MicroarraySchema(schema.getUser(), schema.getContainer()).createRunsTable(alias);
        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts(ExpRunTable.Column.Flag.name()));
        defaultCols.add(FieldKey.fromParts(ExpRunTable.Column.Links.name()));
        defaultCols.add(FieldKey.fromParts(MicroarraySchema.THUMBNAIL_IMAGE_COLUMN_NAME));
        defaultCols.add(FieldKey.fromParts(MicroarraySchema.QC_REPORT_COLUMN_NAME));
        defaultCols.add(FieldKey.fromParts(ExpRunTable.Column.Name.name()));
        result.setDefaultVisibleColumns(defaultCols);

        return result;
    }

    public ActionURL getUploadWizardURL(Container container, ExpProtocol protocol)
    {
        ActionURL url = new ActionURL(MicroarrayUploadWizardAction.class, container);
        url.addParameter("rowId", protocol.getRowId());
        return url;
    }

    protected void addInputMaterials(AssayRunUploadContext context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        String countString = context.getRequest().getParameter(SampleChooser.SAMPLE_COUNT_ELEMENT_NAME);
        int count;
        if (countString != null)
        {
            try
            {
                count = Integer.parseInt(countString);
            }
            catch (NumberFormatException e)
            {
                count = MAX_SAMPLE_COUNT;
            }
        }
        else
        {
            count = MAX_SAMPLE_COUNT;
        }

        for (int i = 0; i < count; i++)
        {
            String lsid = context.getRequest().getParameter(SampleInfo.getLsidFormElementID(i));
            String name = context.getRequest().getParameter(SampleInfo.getNameFormElementID(i));
            ExpMaterial material;
            if (lsid != null && !"".equals(lsid))
            {
                material = ExperimentService.get().getExpMaterial(lsid);
                if (material != null && !material.getContainer().hasPermission(context.getUser(), ACL.PERM_READ))
                {
                    throw new ExperimentException("You do not have permission to reference the sample with LSID " + lsid);
                }
            }
            else
            {
                if (name == null)
                {
                    name = "Unknown";
                }
                material = createSampleMaterial(context.getContainer(), context.getProtocol(), name);
            }
            if (inputMaterials.containsKey(material))
            {
                throw new ExperimentException("The same material cannot be used multiple times");
            }
            inputMaterials.put(material, "Sample " + (i + 1));
        }
    }
}
