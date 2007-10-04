package org.labkey.nab;

import org.labkey.api.study.assay.*;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.list.ListItem;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.User;

import java.util.*;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 2:33:52 PM
 */
public class NabAssayProvider extends PlateBasedAssayProvider
{
    public static final String[] CUTOFF_PROPERTIES = { "Cutoff1", "Cutoff2", "Cutoff3" };
    public static final String SAMPLE_METHOD_PROPERTY_NAME = "Method";
    public static final String SAMPLE_METHOD_PROPERTY_CAPTION = "Method";
    public static final String SAMPLE_INITIAL_DILUTION_PROPERTY_NAME = "InitialDilution";
    public static final String SAMPLE_INITIAL_DILUTION_PROPERTY_CAPTION = "Initial Dilution";
    public static final String SAMPLE_DILUTION_FACTOR_PROPERTY_NAME = "DilutionFactor";
    public static final String SAMPLE_DILUTION_FACTOR_PROPERTY_CAPTION = "Dilution Factor";

    public NabAssayProvider()
    {
        super("NabAssayProtocol", "NabAssayRun", NabDataHandler.NAB_DATA_LSID_PREFIX);
    }

    private ListDefinition createSimpleList(Container lookupContainer, User user, String listName, String displayColumn,
                                            String displayColumnDescription, String... values)
    {
        Map<String, ListDefinition> lists = ListService.get().getLists(lookupContainer);
        ListDefinition sampleMethodList = lists.get(listName);
        if (sampleMethodList == null)
        {
            sampleMethodList = ListService.get().createList(lookupContainer, listName);
            DomainProperty nameProperty = addProperty(sampleMethodList.getDomain(), displayColumn, PropertyType.STRING);
            nameProperty.setPropertyURI(sampleMethodList.getDomain().getTypeURI() + "#" + displayColumn);
            sampleMethodList.setKeyName(nameProperty.getName());
            sampleMethodList.setKeyType(ListDefinition.KeyType.Varchar);
            sampleMethodList.setDescription(displayColumnDescription);
            sampleMethodList.setTitleColumn(displayColumn);
            try
            {
                sampleMethodList.save(user);
                for (String value : values)
                {
                    ListItem concentration = sampleMethodList.createListItem();
                    concentration.setKey(value);
                    concentration.setProperty(nameProperty, value);
                    concentration.save(user);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return sampleMethodList;
    }

    protected Domain createRunDomain(Container c, User user)
    {
        Domain runDomain = super.createRunDomain(c, user);
        boolean first = true;
        for (int i = 0; i < CUTOFF_PROPERTIES.length; i++)
        {
            DomainProperty cutoff = addProperty(runDomain, CUTOFF_PROPERTIES[i], "Cutoff Percentage (" + (i + 1) + ")",
                    PropertyType.INTEGER);
            if (first)
            {
                cutoff.setRequired(true);
                first = false;
            }
        }

        addProperty(runDomain, "VirusName", "Virus Name", PropertyType.STRING);
        addProperty(runDomain, "VirusID", "Virus ID", PropertyType.STRING);
        addProperty(runDomain, "HostCell", "Host Cell", PropertyType.STRING);
        addProperty(runDomain, "StudyName", "Study Name", PropertyType.STRING);
        addProperty(runDomain, "ExperimentPerformer", "Experiment Performer", PropertyType.STRING);
        addProperty(runDomain, "ExperimentID", "Experiment ID", PropertyType.STRING);
        addProperty(runDomain, "IncubationTime", "Incubation Time", PropertyType.STRING);
        addProperty(runDomain, "PlateNumber", "Plate Number", PropertyType.STRING);
        addProperty(runDomain, "ExperimentDate", "Experiment Date", PropertyType.DATE_TIME);
        addProperty(runDomain, "FileID", "File ID", PropertyType.STRING);

        Container lookupContainer = c.getProject();
        ListDefinition curveFitMethodList = createSimpleList(lookupContainer, user, "NabCurveFitMethod", "FitMethod",
                "Method of curve fitting that will be applied to the neutralization data for each sample.", "Four Parameter", "Five Parameter");
        DomainProperty method = addProperty(runDomain, "CurveFitMethod", "Curve Fit Method", PropertyType.STRING);
        method.setLookup(new Lookup(lookupContainer, "lists", curveFitMethodList.getName()));
        method.setRequired(true);
        return runDomain;
    }

    protected Domain createUploadSetDomain(Container c, User user)
    {
        return super.createUploadSetDomain(c, user);
    }

    protected Domain createSampleWellGroupDomain(Container c, User user)
    {
        Domain sampleWellGroupDomain = super.createSampleWellGroupDomain(c, user);
        Container lookupContainer = c.getProject();
        ListDefinition sampleMethodList = createSimpleList(lookupContainer, user, "NabSamplePreparationMethods", "Method",
                "Method of preparation for a sample in a NAb well group.", SampleInfo.Method.Dilution.toString(), SampleInfo.Method.Concentration.toString());
        addProperty(sampleWellGroupDomain, SPECIMENID_PROPERTY_NAME, SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING).setRequired(true);
        addProperty(sampleWellGroupDomain,NabManager.SampleProperty.SampleDescription.toString(), PropertyType.STRING);
        addProperty(sampleWellGroupDomain, SAMPLE_INITIAL_DILUTION_PROPERTY_NAME, SAMPLE_INITIAL_DILUTION_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        addProperty(sampleWellGroupDomain, SAMPLE_DILUTION_FACTOR_PROPERTY_NAME, SAMPLE_DILUTION_FACTOR_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        DomainProperty method = addProperty(sampleWellGroupDomain, SAMPLE_METHOD_PROPERTY_NAME, SAMPLE_METHOD_PROPERTY_CAPTION, PropertyType.STRING);
        method.setLookup(new Lookup(lookupContainer, "lists", sampleMethodList.getName()));
        method.setRequired(true);
        return sampleWellGroupDomain;
    }

    protected Domain createDataDomain(Container c, User user)
    {
        return null;
    }

    public ExpData getDataForDataRow(Object dataRowId)
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public String getName()
    {
        return "Neutralizing Antibodies (NAb)";
    }

    public boolean shouldShowDataDescription(ExpProtocol protocol)
    {
        return false;
    }

    public TableInfo createDataTable(QuerySchema schema, String alias, ExpProtocol protocol)
    {
        return new RunDataTable(schema, alias, protocol);
    }

    public Set<FieldKey> getParticipantIDDataKeys()
    {
        return Collections.singleton(FieldKey.fromParts("Properties", PARTICIPANTID_PROPERTY_NAME));
    }

    public Set<FieldKey> getVisitIDDataKeys()
    {
        return Collections.singleton(FieldKey.fromParts("Properties", VISITID_PROPERTY_NAME));
    }

    public FieldKey getRunIdFieldKeyFromDataRow()
    {
        return FieldKey.fromParts("Run", "RowId");
    }

    public FieldKey getDataRowIdFieldKey()
    {
        return FieldKey.fromParts("ObjectId");
    }

    protected PropertyType getDataRowIdType()
    {
        return PropertyType.INTEGER;
    }
    
    public ViewURLHelper publish(User user, ExpProtocol protocol, Container study, Set<AssayPublishKey> dataKeys, List<String> errors)
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public ViewURLHelper getUploadWizardURL(Container container, ExpProtocol protocol)
    {
        ViewURLHelper url = new ViewURLHelper("NabAssay", "nabUploadWizard.view", container);
        url.addParameter("rowId", protocol.getRowId());
        return url;
    }
    
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }
}
