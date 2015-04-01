package org.labkey.elispot.query;

import org.labkey.api.collections.ConcurrentCaseInsensitiveSortedSet;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.CrosstabDimension;
import org.labkey.api.data.CrosstabMeasure;
import org.labkey.api.data.CrosstabMember;
import org.labkey.api.data.CrosstabSettings;
import org.labkey.api.data.CrosstabTable;
import org.labkey.api.data.Sort;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.study.assay.AssayService;
import org.labkey.elispot.ElispotAssayProvider;
import org.labkey.elispot.ElispotManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by davebradlee on 3/23/15.
 */
public class ElispotAntigenCrosstabTable extends CrosstabTable
{
    private final Set<String> _nonBasePropertyNames;

    public static ElispotAntigenCrosstabTable create(ElispotRunAntigenTable elispotRunAntigenTable, ExpProtocol protocol)
    {
        CrosstabSettings crosstabSettings = new CrosstabSettings(elispotRunAntigenTable);
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromParts("Run", "Container"));
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("Run"));

        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("RunId"));
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("WellgroupName"));
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromString("SpecimenLsid"));
        crosstabSettings.getRowAxis().addDimension(FieldKey.fromParts("SpecimenLsid", "Property", "ParticipantId"));
        CrosstabDimension colDim = crosstabSettings.getColumnAxis().addDimension(FieldKey.fromString("AntigenWellgroupName"));
        crosstabSettings.addMeasure(FieldKey.fromParts("Mean"), CrosstabMeasure.AggregateFunction.AVG, "Mean");
        crosstabSettings.addMeasure(FieldKey.fromParts("Median"), CrosstabMeasure.AggregateFunction.AVG, "Median");

        Set<String> antigenHeadings = new ConcurrentCaseInsensitiveSortedSet();
        for (String antigenWellgroupName: ElispotManager.get().getAntigenWellgoupNames(elispotRunAntigenTable.getContainer(), protocol))
        {
            if (null != antigenWellgroupName)
                antigenHeadings.add(antigenWellgroupName);
        }

        ArrayList<CrosstabMember> members = new ArrayList<>();
        for (String antigenHeading : antigenHeadings)
            members.add(new CrosstabMember(antigenHeading, FieldKey.fromString("AntigenWellgroupName"), antigenHeading));

        Set<String> nonBasePropertyNames = new HashSet<>();
        ElispotAssayProvider provider = (ElispotAssayProvider)AssayService.get().getProvider(protocol);
        assert null != provider;
        assert null != provider.getAntigenWellGroupDomain(protocol);
        for (DomainProperty property : provider.getAntigenWellGroupDomain(protocol).getNonBaseProperties())
        {
            nonBasePropertyNames.add(property.getName());
            crosstabSettings.addMeasure(FieldKey.fromString(property.getName()), CrosstabMeasure.AggregateFunction.GROUP_CONCAT, property.getName());
        }

        return new ElispotAntigenCrosstabTable(crosstabSettings, members, nonBasePropertyNames);
    }
    public ElispotAntigenCrosstabTable(CrosstabSettings crosstabSettings, ArrayList<CrosstabMember> members, Set<String> nonBasePropertyNames)
    {
        super(crosstabSettings, members);
        _nonBasePropertyNames = nonBasePropertyNames;
        getColumn("InstanceCount").setHidden(true);
        getColumn("Run").setHidden(true);
        getColumn("SpecimenLsid").setHidden(true);
        setTitle("AntigenStats");
    }

    @Override
    public ContainerFilter getContainerFilter()
    {
        return getGroupTable().getSourceTable().getContainerFilter();
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        List<FieldKey> fieldKeys = new ArrayList<>();
        for (FieldKey fieldKey : super.getDefaultVisibleColumns())
            if (0 != fieldKey.compareTo(FieldKey.fromString("InstanceCount")) &&
                0 != fieldKey.compareTo(FieldKey.fromString("Run")) &&
                0 != fieldKey.compareTo(FieldKey.fromString("SpecimenLsid")) &&
                0 != fieldKey.compareTo(FieldKey.fromString("Run_fs_folder")))

            {
                boolean addName = true;
                for (String name : _nonBasePropertyNames)
                    if (fieldKey.getName().toLowerCase().endsWith(name.toLowerCase()))
                    {
                        addName = false;
                        break;
                    }

                if (addName)
                    fieldKeys.add(fieldKey);
            }
        return fieldKeys;
    }

    public Sort getDefaultSort()
    {
        return new Sort("+" + "RunId" + ",+" + "WellgroupName");
    }

}
