package org.labkey.luminex;

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpDataTable;
import org.labkey.api.exp.Protocol;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.study.AssayService;
import org.labkey.api.study.DefaultAssayProvider;

import java.util.*;
import java.sql.Types;

public class LuminexSchema extends UserSchema
{
    private static final String ANALYTE_TABLE_NAME = "Analyte";
    private static final String DATA_ROW_TABLE_NAME = "DataRow";
    private final Protocol _protocol;

    public LuminexSchema(User user, Container container)
    {
        this(user, container, null);
    }

    public LuminexSchema(User user, Container container, Protocol protocol)
    {
        super("Luminex", user, container, getSchema());
        _protocol = protocol;
    }
    
    public Set<String> getTableNames()
    {
        return new HashSet<String>(Arrays.asList(ANALYTE_TABLE_NAME, DATA_ROW_TABLE_NAME));
    }
    
    public TableInfo getTable(String name, String alias)
    {
        if (ANALYTE_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createAnalyteTable(alias);
        }
        else if (DATA_ROW_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createDataRowTable(alias);
        }
        return super.getTable(name, alias);
    }

    private TableInfo createAnalyteTable(String alias)
    {
        FilteredTable result = new FilteredTable(getTableInfoAnalytes());
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Name")));
        result.addColumn(result.wrapColumn("Data", result.getRealTable().getColumn("DataId"))).setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createDataTable(null);
            }
        });
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RowId"))).setIsHidden(true);
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("FitProb")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ResVar")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RegressionType")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("StdCurve")));
        ColumnInfo lsidColumn = result.addColumn(result.wrapColumn(result.getRealTable().getColumn("LSID")));
        lsidColumn.setIsHidden(true);

        String sqlObjectId = "(SELECT objectid FROM " + OntologyManager.getTinfoObject() + " o WHERE o.objecturi = " +
                ExprColumn.STR_TABLE_ALIAS + ".lsid)";

        ColumnInfo colProperty = new ExprColumn(result, "Properties", new SQLFragment(sqlObjectId), Types.INTEGER);
        PropertyDescriptor[] pds = DefaultAssayProvider.getPropertiesForDomainPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        Map<String, PropertyDescriptor> map = new TreeMap<String, PropertyDescriptor>();
        for(PropertyDescriptor pd : pds)
        {
            map.put(pd.getName(), pd);
        }
        colProperty.setFk(new PropertyForeignKey(map, this));
        colProperty.setIsUnselectable(true);
        result.addColumn(colProperty);

        addDataFilter(result);
        result.setTitleColumn("Name");
        result.setAlias(alias);
        return result;
    }

    public ExpDataTable createDataTable(String alias)
    {
        ExpDataTable ret = ExperimentService.get().createDataTable(alias);
        ret.setContainer(getContainer());
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setIsHidden(true);

        ColumnInfo runCol = ret.addColumn(ExpDataTable.Column.Run);
        if (_protocol != null)
        {
            runCol.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return AssayService.get().createRunTable(null, _protocol, AssayService.get().getProvider(_protocol), _user, _container);
                }
            });
        }

        return ret;
    }

    public FilteredTable createDataRowTable(String alias)
    {
        final FilteredTable result = new FilteredTable(getTableInfoDataRow());
        result.addColumn(result.wrapColumn("Analyte", result.getRealTable().getColumn("AnalyteId")));
        ColumnInfo dataColumn = result.addColumn(result.wrapColumn("Data", result.getRealTable().getColumn("DataId")));
        dataColumn.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createDataTable(null);
            }
        });
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RowId"))).setIsHidden(true);
        result.getColumn("RowId").setKeyField(true);
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Type")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Well")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Outlier")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Description")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("FIString")));
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("FI"), result.getRealTable().getColumn("FIOORIndicator"));
        result.addColumn(result.wrapColumn("FI-BkgdString", result.getRealTable().getColumn("FIBackgroundString")));
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("FIBackground"), result.getRealTable().getColumn("FIBackgroundOORIndicator"), "FI-Bkgd");
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("StdDevString")));
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("StdDev"), result.getRealTable().getColumn("StdDevOORIndicator"));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ObsConcString")));
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("ObsConc"), result.getRealTable().getColumn("ObsConcOORIndicator"));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ExpConc")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ObsOverExp"))).setCaption("(Obs/Exp)*100");
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("ConcInRange"), result.getRealTable().getColumn("ConcInRangeOORIndicator"));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ConcInRangeString")));

        FieldKey studyFieldKey = FieldKey.fromParts("Data", "Run", "Run Properties", DefaultAssayProvider.TARGET_STUDY_PROPERTY_NAME);
        ColumnInfo studyColumn = QueryService.get().getColumns(result, Collections.singleton(studyFieldKey)).get(studyFieldKey);

        ExprColumn participantCol;
        ExprColumn visitCol;

        if (studyColumn != null)
        {
            SQLFragment participantSQL = new SQLFragment("(SELECT ptid FROM study.specimen s WHERE s.GlobalUniqueId = " + ExprColumn.STR_TABLE_ALIAS + ".Description AND CAST(s.container AS VARCHAR) = " + studyColumn.getValueSql() +  ")");
            participantCol = new ExprColumn(result, "ParticipantID", participantSQL, Types.VARCHAR, studyColumn);

            SQLFragment visitSQL = new SQLFragment("(SELECT visitvalue FROM study.specimen s WHERE s.GlobalUniqueId = " + ExprColumn.STR_TABLE_ALIAS + ".Description AND CAST(s.container AS VARCHAR) = " + studyColumn.getValueSql() +  ")");
            visitCol = new ExprColumn(result, "VisitID", visitSQL, Types.REAL, studyColumn);
        }
        else
        {
            participantCol = new ExprColumn(result, "ParticipantID", new SQLFragment("NULL"), Types.VARCHAR);
            visitCol = new ExprColumn(result, "VisitID", new SQLFragment("NULL"), Types.REAL);
        }

        result.addColumn(participantCol);
        result.addColumn(visitCol);

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("Type"));
        defaultCols.add(FieldKey.fromParts("Well"));
        defaultCols.add(FieldKey.fromParts("Description"));
        defaultCols.add(FieldKey.fromParts("FI"));
        defaultCols.add(FieldKey.fromParts("FIBackground"));
        defaultCols.add(FieldKey.fromParts("StdDev"));
        defaultCols.add(FieldKey.fromParts("ObsConc"));
        defaultCols.add(FieldKey.fromParts("ExpConc"));
        defaultCols.add(FieldKey.fromParts("ObsOverExp"));
        defaultCols.add(FieldKey.fromParts("ConcInRange"));
        result.setDefaultVisibleColumns(defaultCols);

        result.getColumn("Analyte").setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createAnalyteTable(null);
            }
        });
        addDataFilter(result);
        result.setAlias(alias);
        return result;
    }

    private void addDataFilter(FilteredTable result)
    {
        SQLFragment filter = new SQLFragment("DataId IN (SELECT d.RowId FROM " + ExperimentService.get().getTinfoData() + " d, " + ExperimentService.get().getTinfoExperimentRun() + " r WHERE d.RunId = r.RowId AND d.Container = ?");
        filter.add(getContainer().getId());
        if (_protocol != null)
        {
            filter.append(" AND r.ProtocolLSID = ?");
            filter.add(_protocol.getLSID());
        }
        filter.append(")");
        result.addCondition(filter);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get("luminex");
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoAnalytes()
    {
        return getSchema().getTable(ANALYTE_TABLE_NAME);
    }

    public static TableInfo getTableInfoDataRow()
    {
        return getSchema().getTable(DATA_ROW_TABLE_NAME);
    }


}
