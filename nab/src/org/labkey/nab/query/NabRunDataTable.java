package org.labkey.nab.query;

import org.labkey.api.query.*;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpMaterialTable;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.data.*;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.nab.NabDataHandler;
import org.labkey.nab.NabAssayProvider;

import java.util.*;
import java.sql.Types;
import java.sql.SQLException;

/**
 * User: brittp
 * Date: Jul 6, 2007
 * Time: 5:35:15 PM
 */
public class NabRunDataTable extends FilteredTable
{
    public static final String RUN_ID_COLUMN_NAME = "RunId";

    public NabRunDataTable(final QuerySchema schema, String alias, final ExpProtocol protocol)
    {
        super(OntologyManager.getTinfoObject(), schema.getContainer());
        final AssayProvider provider = AssayService.get().getProvider(protocol);

        setAlias(alias);
        List<FieldKey> visibleColumns = new ArrayList<FieldKey>();

        try
        {
            // add material lookup columns to the view first, so they appear at the left:
            String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(protocol, NabAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
            final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
            for (PropertyDescriptor pd : sampleSet.getPropertiesForType())
            {
                visibleColumns.add(FieldKey.fromParts("Properties", NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY,
                        ExpMaterialTable.Column.Property.toString(), pd.getName()));
            }

            // get all the properties from this NAB protocol:
            PropertyDescriptor[] pds = NabSchema.getExistingDataProperties(protocol);

            // add object ID to this tableinfo and set it as a key field:
            ColumnInfo objectIdColumn = addWrapColumn(_rootTable.getColumn("ObjectId"));
            objectIdColumn.setKeyField(true);

            // add object ID again, this time as a lookup to a virtual property table that contains our selected NAB properties:
            ColumnInfo propertyLookupColumn = wrapColumn("Properties", _rootTable.getColumn("ObjectId"));
            propertyLookupColumn.setKeyField(false);
            propertyLookupColumn.setIsUnselectable(true);
            OORAwarePropertyForeignKey fk = new OORAwarePropertyForeignKey(pds, this, schema)
            {
                protected ColumnInfo constructColumnInfo(ColumnInfo parent, String name, PropertyDescriptor pd)
                {
                    ColumnInfo result = super.constructColumnInfo(parent, name, pd);
                    if (NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY.equals(pd.getName()))
                    {
                        result.setIsHidden(true);
                        result.setFk(new LookupForeignKey("LSID")
                        {
                            public TableInfo getLookupTableInfo()
                            {
                                ExpMaterialTable materials = ExperimentService.get().createMaterialTable(null, schema);
                                materials.setSampleSet(sampleSet, true);
                                materials.addColumn(ExpMaterialTable.Column.Property);
                                materials.addColumn(ExpMaterialTable.Column.LSID).setIsHidden(true);
                                return materials;
                            }
                        });
                    }
                    return result;
                }
            };
            propertyLookupColumn.setFk(fk);
            addColumn(propertyLookupColumn);

            Set<String> hiddenCols = new HashSet<String>();
            for (PropertyDescriptor pd : fk.getDefaultHiddenProperties())
                hiddenCols.add(pd.getName());
            hiddenCols.add(NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY);
            // run through the property columns, setting all to be visible by default:
            FieldKey dataKeyProp = new FieldKey(null, propertyLookupColumn.getName());
            for (PropertyDescriptor lookupCol : pds)
            {
                if (!hiddenCols.contains(lookupCol.getName()))
                {
                    FieldKey key = new FieldKey(dataKeyProp, lookupCol.getName());
                    visibleColumns.add(key);
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        SQLFragment filterClause = new SQLFragment("OwnerObjectId IN (\n" +
                "SELECT ObjectId FROM exp.Object o, exp.Data d, exp.ExperimentRun r WHERE o.ObjectURI = d.lsid AND \n" +
                "d.RunId = r.RowId and r.ProtocolLSID = ?)");
        filterClause.add(protocol.getLSID());
        addCondition(filterClause, "OwnerObjectId");

        // TODO - we should have a more reliable (and speedier) way of identifying just the data rows here
        SQLFragment dataRowClause = new SQLFragment("ObjectURI LIKE '%" + NabDataHandler.NAB_DATA_ROW_LSID_PREFIX + "%'");
        addCondition(dataRowClause, "ObjectURI");

        String sqlRunLSID = "(SELECT RunObjects.objecturi FROM exp.Object AS DataRowParents, " +
                "    exp.Object AS RunObjects, exp.Data d, exp.ExperimentRun r WHERE \n" +
                "    DataRowParents.ObjectUri = d.lsid AND\n" +
                "    r.RowId = d.RunId AND\n" +
                "    RunObjects.ObjectURI = r.lsid AND\n" +
                "    DataRowParents.ObjectID IN (SELECT OwnerObjectId FROM exp.Object AS DataRowObjects\n" +
                "    WHERE DataRowObjects.ObjectId = " + ExprColumn.STR_TABLE_ALIAS + ".ObjectId))";

        ExprColumn runColumn = new ExprColumn(this, "Run", new SQLFragment(sqlRunLSID), Types.VARCHAR);
        runColumn.setFk(new LookupForeignKey("LSID")
        {
            public TableInfo getLookupTableInfo()
            {
                return AssayService.get().createRunTable(null, protocol, provider, schema.getUser(), schema.getContainer());
            }
        });
        addColumn(runColumn);

        String sqlRunRowId = "(SELECT r.RowId FROM exp.Object AS DataRowParents, " +
                "    exp.Object AS RunObjects, exp.Data d, exp.ExperimentRun r WHERE \n" +
                "    DataRowParents.ObjectUri = d.lsid AND\n" +
                "    r.RowId = d.RunId AND\n" +
                "    RunObjects.ObjectURI = r.lsid AND\n" +
                "    DataRowParents.ObjectID IN (SELECT OwnerObjectId FROM exp.Object AS DataRowObjects\n" +
                "    WHERE DataRowObjects.ObjectId = " + ExprColumn.STR_TABLE_ALIAS + ".ObjectId))";

        ExprColumn runIdColumn = new ExprColumn(this, RUN_ID_COLUMN_NAME, new SQLFragment(sqlRunRowId), Types.INTEGER);
        ColumnInfo addedRunIdColumn = addColumn(runIdColumn);
        addedRunIdColumn.setIsHidden(true);
        addedRunIdColumn.setKeyField(true);

        Set<String> hiddenProperties = new HashSet<String>();
        hiddenProperties.addAll(Arrays.asList(NabAssayProvider.CUTOFF_PROPERTIES));
        hiddenProperties.add(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
        for (PropertyDescriptor prop : provider.getRunPropertyColumns(protocol))
        {
            if (!hiddenProperties.contains(prop.getName()))
                visibleColumns.add(FieldKey.fromParts("Run", "Run Properties", prop.getName()));
        }
        for (PropertyDescriptor prop : provider.getUploadSetColumns(protocol))
        {
            if (!hiddenProperties.contains(prop.getName()))
                visibleColumns.add(FieldKey.fromParts("Run", "Run Properties", prop.getName()));
        }
        setDefaultVisibleColumns(visibleColumns);
    }


}
