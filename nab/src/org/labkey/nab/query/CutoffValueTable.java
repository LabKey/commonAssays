/*
 * Copyright (c) 2013 LabKey Corporation
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

package org.labkey.nab.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.OORDisplayColumnFactory;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabManager;

public class CutoffValueTable extends FilteredTable<NabProtocolSchema>
{
    private static final FieldKey CONTAINER_FIELD_KEY = FieldKey.fromParts("Container");

    public CutoffValueTable(NabProtocolSchema schema)
    {
        super(NabProtocolSchema.getTableInfoCutoffValue(), schema);

        addWrapColumn(getRealTable().getColumn("RowId")).setHidden(true);
        addWrapColumn(getRealTable().getColumn("NAbSpecimenID")).setFk(new LookupForeignKey()
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getTable(NabProtocolSchema.NAB_SPECIMEN_TABLE_NAME);
            }
        });
        addWrapColumn(getRealTable().getColumn("Cutoff"));

        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("Point"), getRealTable().getColumn("PointOORIndicator"));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("IC_4pl"), getRealTable().getColumn("IC_4plOORIndicator"));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("IC_5pl"), getRealTable().getColumn("IC_5plOORIndicator"));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("IC_Poly"), getRealTable().getColumn("IC_PolyOORIndicator"));

        ColumnInfo selectedIC = new ExprColumn(this, "IC", getSelectedCurveFitIC(false), JdbcType.DECIMAL);
        ColumnInfo selectedICOOR = new ExprColumn(this, "ICOORIndicator", getSelectedCurveFitIC(true), JdbcType.VARCHAR);
        if (!NabManager.useNewNab)
        {
            addColumn(selectedIC);
            addColumn(selectedICOOR);
        }
        else
        {
            OORDisplayColumnFactory.addOORColumns(this, selectedIC, selectedICOOR, selectedIC.getLabel(), false);
        }

        SQLFragment protocolSQL = new SQLFragment("NAbSpecimenID IN (SELECT RowId FROM ");
        protocolSQL.append(NabProtocolSchema.getTableInfoNAbSpecimen(), "s");
        protocolSQL.append(" WHERE ProtocolId = ?)");
        protocolSQL.add(_userSchema.getProtocol().getRowId());
        addCondition(protocolSQL, FieldKey.fromParts("ProtocolID"));
    }

    private SQLFragment getSelectedCurveFitIC(boolean oorIndicator)
    {
        String suffix = oorIndicator ? "OORIndicator" : "";
        SQLFragment defaultICSQL = new SQLFragment("CASE (SELECT op.StringValue FROM ");
        defaultICSQL.append(OntologyManager.getTinfoObject(), "o");
        defaultICSQL.append(", ");
        defaultICSQL.append(OntologyManager.getTinfoObjectProperty(), "op");
        defaultICSQL.append(", ");
        defaultICSQL.append(OntologyManager.getTinfoPropertyDescriptor(), "pd");
        defaultICSQL.append(", ");
        defaultICSQL.append(NabProtocolSchema.getTableInfoNAbSpecimen(), "ns");
        defaultICSQL.append(", ");
        defaultICSQL.append(ExperimentService.get().getTinfoExperimentRun(), "er");
        defaultICSQL.append(" WHERE op.PropertyId = pd.PropertyId AND pd.PropertyURI LIKE '%#" + NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME + "' AND ns.RowId = ");
        defaultICSQL.append(ExprColumn.STR_TABLE_ALIAS);
        defaultICSQL.append(".NAbSpecimenID AND er.LSID = o.ObjectURI AND o.ObjectId = op.ObjectId AND er.RowId = ns.RunId)");
        defaultICSQL.append("\nWHEN 'Polynomial' THEN ");
        if (NabManager.useNewNab)
            defaultICSQL.append(ExprColumn.STR_TABLE_ALIAS + ".");
        defaultICSQL.append("IC_Poly");
        defaultICSQL.append(suffix);
        defaultICSQL.append("\nWHEN 'Five Parameter' THEN ");
        if (NabManager.useNewNab)
            defaultICSQL.append(ExprColumn.STR_TABLE_ALIAS + ".");
        defaultICSQL.append("IC_5pl");
        defaultICSQL.append(suffix);
        defaultICSQL.append("\nWHEN 'Four Parameter' THEN ");
        if (NabManager.useNewNab)
            defaultICSQL.append(ExprColumn.STR_TABLE_ALIAS + ".");
        defaultICSQL.append("IC_4pl");
        defaultICSQL.append(suffix);
        defaultICSQL.append("\nEND\n");
        return defaultICSQL;
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        // We need to do our filtering based on the run since we don't have a container column of our own
        clearConditions(CONTAINER_FIELD_KEY);
        SQLFragment sql = new SQLFragment("NabSpecimenID IN (SELECT ns.RowId FROM ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(", ");
        sql.append(NabProtocolSchema.getTableInfoNAbSpecimen(), "ns");
        sql.append(" WHERE r.RowId = ns.RunId AND ");
        sql.append(filter.getSQLFragment(getSchema(), CONTAINER_FIELD_KEY, _userSchema.getContainer()));
        sql.append(")");
        addCondition(sql, CONTAINER_FIELD_KEY);
    }


}
