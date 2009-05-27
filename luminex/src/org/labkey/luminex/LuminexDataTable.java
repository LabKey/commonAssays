package org.labkey.luminex;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.OORDisplayColumnFactory;
import org.labkey.api.study.assay.SpecimenForeignKey;
import org.labkey.api.study.assay.AssayService;

import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: May 22, 2009
 */
public class LuminexDataTable extends FilteredTable
{
    private final LuminexSchema _schema;

    public LuminexDataTable(LuminexSchema schema)
    {
        super(LuminexSchema.getTableInfoDataRow());
        _schema = schema;

        addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));
        ColumnInfo dataColumn = addColumn(wrapColumn("Data", getRealTable().getColumn("DataId")));
        dataColumn.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createDataTable();
            }
        });
        addColumn(wrapColumn(getRealTable().getColumn("RowId"))).setIsHidden(true);
        getColumn("RowId").setKeyField(true);
        addColumn(wrapColumn(getRealTable().getColumn("Type")));
        addColumn(wrapColumn(getRealTable().getColumn("Well")));
        addColumn(wrapColumn(getRealTable().getColumn("Outlier")));
        addColumn(wrapColumn(getRealTable().getColumn("Description")));
        ColumnInfo specimenColumn = wrapColumn(getRealTable().getColumn("SpecimenID"));
        specimenColumn.setFk(new SpecimenForeignKey(_schema, AssayService.get().getProvider(_schema.getProtocol()), _schema.getProtocol()));
        addColumn(specimenColumn);
        addColumn(wrapColumn(getRealTable().getColumn("ExtraSpecimenInfo")));
        addColumn(wrapColumn(getRealTable().getColumn("FIString"))).setCaption("FI String");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("FI"), getRealTable().getColumn("FIOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("FIBackgroundString"))).setCaption("FI-Bkgd String");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("FIBackground"), getRealTable().getColumn("FIBackgroundOORIndicator"), "FI-Bkgd");
        addColumn(wrapColumn(getRealTable().getColumn("StdDevString")));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("StdDev"), getRealTable().getColumn("StdDevOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ObsConcString")));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("ObsConc"), getRealTable().getColumn("ObsConcOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ExpConc")));
        addColumn(wrapColumn(getRealTable().getColumn("ObsOverExp"))).setCaption("(Obs/Exp)*100");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("ConcInRange"), getRealTable().getColumn("ConcInRangeOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ConcInRangeString")));
        addColumn(wrapColumn(getRealTable().getColumn("Dilution")));
        addColumn(wrapColumn("Group", getRealTable().getColumn("DataRowGroup")));
        addColumn(wrapColumn(getRealTable().getColumn("Ratio")));
        addColumn(wrapColumn(getRealTable().getColumn("SamplingErrors")));

        addColumn(wrapColumn("ParticipantID", getRealTable().getColumn("PTID")));
        addColumn(wrapColumn(getRealTable().getColumn("VisitID")));
        addColumn(wrapColumn(getRealTable().getColumn("Date")));

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("Type"));
        defaultCols.add(FieldKey.fromParts("Well"));
        defaultCols.add(FieldKey.fromParts("Description"));
        defaultCols.add(FieldKey.fromParts("SpecimenID"));
        defaultCols.add(FieldKey.fromParts("ParticipantID"));
        defaultCols.add(FieldKey.fromParts("VisitID"));
        defaultCols.add(FieldKey.fromParts("FI"));
        defaultCols.add(FieldKey.fromParts("FIBackground"));
        defaultCols.add(FieldKey.fromParts("StdDev"));
        defaultCols.add(FieldKey.fromParts("ObsConc"));
        defaultCols.add(FieldKey.fromParts("ExpConc"));
        defaultCols.add(FieldKey.fromParts("ObsOverExp"));
        defaultCols.add(FieldKey.fromParts("ConcInRange"));
        defaultCols.add(FieldKey.fromParts("Dilution"));
        setDefaultVisibleColumns(defaultCols);

        getColumn("Analyte").setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createAnalyteTable();
            }
        });
        _schema.addDataFilter(this);
    }
}
