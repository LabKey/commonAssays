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
import org.labkey.api.util.NetworkDrive;
import org.labkey.microarray.pipeline.ArrayPipelineManager;
import org.labkey.microarray.MicroarrayModule;

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

    public MicroarrayAssayProvider()
    {
        super(PROTOCOL_PREFIX, "MicroarrayAssayRun", MicroarrayModule.MAGE_ML_DATA_TYPE);
    }

    public ExpData getDataForDataRow(Object dataRowId)
    {
        throw new UnsupportedOperationException();
    }

    public String getName()
    {
        return NAME;
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The MAGEML data file is an XML file.");
    }

    public TableInfo createDataTable(QuerySchema schema, String alias, ExpProtocol protocol)
    {
        return new RunDataTable(schema, alias, protocol);
    }

    public List<Domain> createDefaultDomains(Container c, User user)
    {
        List<Domain> result = super.createDefaultDomains(c, user);
        Domain dataDomain = PropertyService.get().createDomain(c, "urn:lsid:${LSIDAuthority}:" + ExpProtocol.ASSAY_DOMAIN_DATA + ".Folder-${Container.RowId}:" + ASSAY_NAME_SUBSTITUTION, "Data Properties");
        dataDomain.setDescription("The user is prompted to select a MAGEML file that contains the data values.");
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
            ExpData mageData = createData(context.getUser(), context.getContainer(), mageMLFile, MicroarrayModule.MAGE_ML_DATA_TYPE);

            outputDatas.put(mageData, "MageML");
            String baseName = ArrayPipelineManager.getBaseMageName(mageMLFile.getName());
            if (baseName != null)
            {
                File imageFile = new File(mageMLFile.getParentFile(), baseName + ".jpg");
                if (NetworkDrive.exists(imageFile))
                {
                    ExpData imageData = createData(context.getUser(), context.getContainer(), imageFile, MicroarrayModule.IMAGE_DATA_TYPE);
                    outputDatas.put(imageData, "ThumbnailImage");
                }

                File qcFile = new File(mageMLFile.getParentFile(), baseName + ".pdf");
                if (NetworkDrive.exists(qcFile))
                {
                    ExpData qcData = createData(context.getUser(), context.getContainer(), qcFile, MicroarrayModule.QC_REPORT_DATA_TYPE);
                    outputDatas.put(qcData, "QCReport");
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    public FieldKey getSpecimenIDFieldKey()
    {
        throw new UnsupportedOperationException();
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

}
