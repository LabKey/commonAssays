package org.labkey.ms2;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.SimpleProtein;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.settings.PreferenceService;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.ms2.protein.Protein;
import org.labkey.ms2.protein.ProteinViewBean;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PeptideManager
{
    private static final Logger LOG = LogHelper.getLogger(PeptideManager.class, "Peptide warnings");
    // Hard-code package name to maintain backward-compatibility with saved preferences
    private static final String ALL_PEPTIDES_PREFERENCE_NAME = "org.labkey.ms2.protein.ProteinManager." + ProteinViewBean.ALL_PEPTIDES_URL_PARAM;

    public static final int RUN_FILTER = 1;
    public static final int URL_FILTER = 2;
    public static final int EXTRA_FILTER = 4;
    public static final int PROTEIN_FILTER = 8;
    public static final int ALL_FILTERS = RUN_FILTER + URL_FILTER + EXTRA_FILTER + PROTEIN_FILTER;

    public static void addExtraFilter(SimpleFilter filter, MS2Run run, ActionURL currentUrl)
    {
        String paramName = run.getChargeFilterParamName();

        boolean includeChargeFilter = false;
        Float[] values = new Float[3];

        for (int i = 0; i < values.length; i++)
        {
            String threshold = currentUrl.getParameter(paramName + (i + 1));

            if (null != threshold && !threshold.isEmpty())
            {
                try
                {
                    values[i] = Float.parseFloat(threshold);  // Make sure this parses to a float
                    includeChargeFilter = true;
                }
                catch(NumberFormatException e)
                {
                    // Ignore any values that can't be converted to float -- leave them null
                }
            }
        }

        // Add charge filter only if there's one or more valid values
        if (includeChargeFilter && run.getChargeFilterColumnName() != null)
            filter.addClause(new ChargeFilter(FieldKey.fromString(run.getChargeFilterColumnName()), values));

        String tryptic = currentUrl.getParameter("tryptic");

        // Add tryptic filter
        if ("1".equals(tryptic))
            filter.addClause(new TrypticFilter(1));
        else if ("2".equals(tryptic))
            filter.addClause(new TrypticFilter(2));
    }

    public static Sort getPeptideBaseSort()
    {
        // Always sort peptide lists by Fraction, Scan, HitRank, Charge
        return new Sort("Fraction,Scan,HitRank,Charge");
    }

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, List<MS2Run> runs, int mask, User user)
    {
        // Cop-out for now... we've already checked to make sure all runs are the same type
        // TODO: Allow runs of different type, by one of the following:
        // 1) verify that no search-engine-specific scores are used in the filter OR
        // 2) ignore filters that don't apply to a particular run, and provide a warning OR
        // 3) allowing picking one filter per run type
        return getPeptideFilter(currentUrl, mask, user, runs.get(0));
    }

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, int mask, User user, MS2Run... runs)
    {
        return getPeptideFilter(currentUrl, mask, null, user, runs);
    }

    private static SimpleFilter getPeptideFilter(ActionURL currentUrl, int mask, String runTableName, User user, MS2Run... runs)
    {
        return getTableFilter(currentUrl, mask, runTableName, MS2Manager.getDataRegionNamePeptides(), user, runs);
    }

    private static SimpleFilter getTableFilter(ActionURL currentUrl, int mask, String runTableName, String dataRegionName, User user, MS2Run... runs)
    {
        SimpleFilter filter = new SimpleFilter();

        if ((mask & RUN_FILTER) != 0)
        {
            addRunCondition(filter, runTableName, runs);
        }

        if ((mask & URL_FILTER) != 0)
            filter.addUrlFilters(currentUrl, dataRegionName);

        if ((mask & EXTRA_FILTER) != 0)
            addExtraFilter(filter, runs[0], currentUrl);

        if ((mask & PROTEIN_FILTER) != 0)
        {
            String groupNumber = currentUrl.getParameter("groupNumber");
            String indistId = currentUrl.getParameter("indistinguishableCollectionId");
            if (null != groupNumber)
            {
                try
                {
                    filter.addClause(new ProteinGroupFilter(Integer.parseInt(groupNumber), null == indistId ? 0 : Integer.parseInt(indistId)));
                    return filter;
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Bad groupNumber or indistinguishableCollectionId " + groupNumber + ", " + indistId);
                }
            }

            String groupRowId = currentUrl.getParameter("proteinGroupId");
            if (groupRowId != null)
            {
                try
                {
                    filter.addCondition(FieldKey.fromParts("ProteinProphetData", "ProteinGroupId", "RowId"), Integer.parseInt(groupRowId));
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Bad proteinGroupId " + groupRowId);
                }
                return filter;
            }

            String seqId = currentUrl.getParameter("seqId");
            if (null != seqId)
            {
                try
                {
                    // if "all peptides" flag is set, add a filter to match peptides to the seqid on the url
                    // rather than just filtering for search engine protein.
                    if (showAllPeptides(currentUrl, user))
                    {
                        filter.addClause(new SequenceFilter(Integer.parseInt(seqId)));
                    }
                    else
                        filter.addCondition(FieldKey.fromParts("SeqId"), Integer.parseInt(seqId));
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Bad seqId " + seqId);
                }
            }
        }
        return filter;
    }

    private static final NumberFormat generalFormat = new DecimalFormat("0.0#");

    public static List<Protein> getProteinsContainingPeptide(MS2Peptide peptide, int... fastaIds)
    {
        if ((null == peptide) || ("".equals(peptide.getTrimmedPeptide())) || (peptide.getProteinHits() < 1))
            return Collections.emptyList();

        int hits = peptide.getProteinHits();
        SQLFragment sql = new SQLFragment();
        if (hits == 1 && peptide.getSeqId() != null)
        {
            sql.append("SELECT SeqId, ProtSequence AS Sequence, Mass, Description, ? AS BestName, BestGeneName FROM ");
            sql.append(ProteinSchema.getTableInfoSequences(), "s");
            sql.append(" WHERE SeqId = ?");
            sql.add(peptide.getProtein());
            sql.add(peptide.getSeqId());
        }
        else
        {
            // TODO: make search tryptic so that number that match = ProteinHits.
            sql.append("SELECT s.SeqId, s.ProtSequence AS Sequence, s.Mass, s.Description, fs.LookupString AS BestName, s.BestGeneName FROM ");
            sql.append(ProteinSchema.getTableInfoSequences(), "s");
            sql.append(", ");
            sql.append(ProteinSchema.getTableInfoFastaSequences(), "fs");
            sql.append(" WHERE fs.SeqId = s.SeqId AND fs.FastaId IN (");
            sql.append(StringUtils.repeat("?", ", ", fastaIds.length));
            sql.append(") AND ProtSequence ");
            sql.append(ProteinSchema.getSqlDialect().getCharClassLikeOperator());
            sql.append(" ?" );
            for (int fastaId : fastaIds)
            {
                sql.add(fastaId);
            }
            sql.add("%" + peptide.getTrimmedPeptide() + "%");

            //based on observations of 2 larger ms2 databases, TOP 20 causes better query plan generation in SQL Server
            sql = ProteinSchema.getSchema().getSqlDialect().limitRows(sql, Math.max(20, hits));
        }

        List<Protein> proteins = new SqlSelector(ProteinSchema.getSchema(), sql).getArrayList(Protein.class);

        if (proteins.isEmpty())
            LOG.warn("getProteinsContainingPeptide: Could not find peptide " + peptide + " in FASTA files " + Arrays.asList(fastaIds));

        return proteins;
    }

    public static void addRunCondition(SimpleFilter filter, @Nullable String runTableName, MS2Run... runs)
    {
        String columnName = (runTableName == null ? "Run" : runTableName + ".Run");
        StringBuilder sb = new StringBuilder();
        sb.append(columnName);
        sb.append(" IN (");
        String separator = "";
        for (MS2Run run : runs)
        {
            sb.append(separator);
            separator = ", ";
            sb.append(run.getRun());
        }
        sb.append(")");
        filter.addWhereClause(sb.toString(), new Object[0], FieldKey.fromString("Run"));
    }

    // TODO: runTableName is null in all cases... remove parameter?
    public static void replaceRunCondition(SimpleFilter filter, @Nullable String runTableName, MS2Run... runs)
    {
        filter.deleteConditions(runTableName == null ? FieldKey.fromParts("Run") : FieldKey.fromParts(runTableName, "Run"));
        addRunCondition(filter, runTableName, runs);
    }

    public static SimpleFilter.FilterClause getSequencesFilter(List<Integer> targetSeqIds)
    {
        SimpleFilter.FilterClause[] proteinClauses = new SimpleFilter.FilterClause[targetSeqIds.size()];
        int seqIndex = 0;
        for (Integer targetSeqId : targetSeqIds)
        {
            proteinClauses[seqIndex++] = (new SequenceFilter(targetSeqId));
        }
        return new SimpleFilter.OrClause(proteinClauses);
    }

    public static boolean showAllPeptides(ActionURL url, User user)
    {
        // First look for a value on the URL
        String param = url.getParameter(ProteinViewBean.ALL_PEPTIDES_URL_PARAM);
        if (param != null)
        {
            boolean result = Boolean.parseBoolean(param);
            // Stash as the user's preference
            try (var ignored = SpringActionController.ignoreSqlUpdates())
            {
                PreferenceService.get().setProperty(ALL_PEPTIDES_PREFERENCE_NAME, Boolean.toString(result), user);
            }
            return result;
        }
        // Next check if the user has a preference stored
        param = PreferenceService.get().getProperty(ALL_PEPTIDES_PREFERENCE_NAME, user);
        if (param != null)
        {
            return Boolean.parseBoolean(param);
        }
        // Otherwise go with the default
        return false;
    }

    public static Sort reduceToValidColumns(Sort fullSort, TableInfo... tables)
    {
        Sort validSort = new Sort();
        List<Sort.SortField> sortList = fullSort.getSortList();
        for (int i = sortList.size() - 1; i >=0; i--)
        {
            Sort.SortField field = sortList.get(i);
            boolean validClause = false;
            String columnName = field.getColumnName();
            for (TableInfo table : tables)
            {
                if (table.getColumn(columnName) != null)
                {
                    validClause = true;
                }
                else
                {
                    int index = columnName.lastIndexOf('.');
                    if (index != -1 && table.getColumn(columnName.substring(index + 1)) != null)
                    {
                        validClause = true;
                    }
                }
            }
            if (validClause)
            {
                validSort.insertSort(new Sort(field.getSortDirection().getDir() + field.getColumnName()));
            }
        }
        return validSort;
    }

    public static SimpleFilter reduceToValidColumns(SimpleFilter fullFilter, TableInfo... tables)
    {
        SimpleFilter validFilter = new SimpleFilter();
        for (SimpleFilter.FilterClause clause : fullFilter.getClauses())
        {
            boolean validClause = false;
            for (FieldKey fieldKey : clause.getFieldKeys())
            {
                for (TableInfo table : tables)
                {
                    ColumnInfo column = table.getColumn(fieldKey);
                    if (column == null)
                    {
                        String columnName = fieldKey.toString();
                        int index = columnName.lastIndexOf('.');
                        if (index != -1)
                        {
                            column = table.getColumn(columnName.substring(index + 1));
                        }
                    }

                    if (column != null)
                    {
                        try
                        {
                            // Coerce data types
                            Object[] values = clause.getParamVals();
                            if (values != null)
                            {
                                for (int i = 0; i < values.length; i++)
                                {
                                    if (values[i] != null)
                                    {
                                        values[i] = ConvertUtils.convert(values[i].toString(), column.getJavaClass());
                                    }
                                }
                            }
                            validClause = true;
                        }
                        catch (ConversionException ignored) {}
                    }
                }
            }
            if (validClause)
            {
                validFilter.addClause(clause);
            }
        }
        return validFilter;
    }

    public static class ChargeFilter extends SimpleFilter.FilterClause
    {
        private final FieldKey _fieldKey;
        private final Float[] _values;

        // At least one value must be non-null
        public ChargeFilter(FieldKey fieldKey, Float[] values)
        {
            _fieldKey = fieldKey;
            _values = values;
        }

        @Override
        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(_fieldKey, FieldKey.fromParts("Charge"));
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            ColumnInfo colInfo = columnMap != null ? columnMap.get(_fieldKey) : null;
            String name = colInfo != null ? colInfo.getAlias() : _fieldKey.getName();
            String alias = dialect.getColumnSelectName(name);

            SQLFragment sql = new SQLFragment();
            sql.append(alias);
            sql.append(" >= CASE Charge");

            for (int i = 0; i < _values.length; i++)
            {
                if (null != _values[i])
                {
                    sql.append(" WHEN ");
                    sql.appendValue(i + 1);
                    sql.append(" THEN ");
                    sql.append(generalFormat.format(_values[i]));
                }
            }

            return sql.append(" ELSE 0 END");
        }

        @Override
        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
        {
            String sep = "";

            for (int i = 0; i < _values.length; i++)
            {
                if (null != _values[i])
                {
                    sb.append(sep);
                    sep = ", ";
                    sb.append('+').append(i + 1).append(':');
                    sb.append(formatter.format(_fieldKey));
                    sb.append(" >= ").append(generalFormat.format(_values[i]));
                }
            }
        }
    }

    public static class TrypticFilter extends SimpleFilter.FilterClause
    {
        private final int _termini;

        public TrypticFilter(int termini)
        {
            _termini = termini;
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sql = new SQLFragment();
            switch(_termini)
            {
                case(0):
                    sql.append("");
                    break;

                case(1):
                    sql.append(nTerm(dialect)).append(" OR ").append(cTerm(dialect));
                    break;

                case(2):
                    sql.append(nTerm(dialect)).append(" AND ").append(cTerm(dialect));
                    break;

                default:
                    throw new IllegalArgumentException("INVALID PARAMETER: TERMINI = " + _termini);
            }
            sql.addAll(getParamVals());
            return sql;
        }

        private String nTerm(SqlDialect dialect)
        {
            return "(StrippedPeptide " + dialect.getCharClassLikeOperator() + " '[KR][^P]%' OR StrippedPeptide " + dialect.getCharClassLikeOperator() + " '-%')";
        }

        private String cTerm(SqlDialect dialect)
        {
            return "(StrippedPeptide " + dialect.getCharClassLikeOperator() + " '%[KR][^P]' OR StrippedPeptide " + dialect.getCharClassLikeOperator() + " '%-')";
        }

        @Override
        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(FieldKey.fromParts("StrippedPeptide"));
        }

        @Override
        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
        {
            sb.append("Trypic Ends ");
            sb.append(1 == _termini ? ">= " : "= ");
            sb.append(_termini);
        }
    }

    public static class ProteinGroupFilter extends SimpleFilter.FilterClause
    {
        int _groupNum;
        int _indistinguishableProteinId;

        public ProteinGroupFilter(int groupNum, int indistId)
        {
            _groupNum = groupNum;
            _indistinguishableProteinId = indistId;
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sqlf = new SQLFragment();
            sqlf.append(" RowId IN (SELECT pm.PeptideId FROM ").append(String.valueOf(MS2Manager.getTableInfoPeptideMemberships())).append(" pm ");
            sqlf.append(" INNER JOIN ").append(String.valueOf(MS2Manager.getTableInfoProteinGroups())).append(" pg  ON (pm.ProteinGroupId = pg.RowId) \n");
            sqlf.append(" WHERE pg.GroupNumber = ").append(String.valueOf(_groupNum)).append("  and pg.IndistinguishableCollectionId = ").append(String.valueOf(_indistinguishableProteinId)).append(" ) ");
            return sqlf;
        }

        @Override
        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(FieldKey.fromParts("RowId"));
        }
         @Override
         protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
         {
             sb.append("Peptide member of ProteinGroup ").append(_groupNum);
             if (_indistinguishableProteinId > 0)
             {
                 sb.append("-");
                 sb.append(_indistinguishableProteinId);
             }
         }
     }

    public static class SequenceFilter extends SimpleFilter.FilterClause
    {
        int _seqid;
        String _sequence;
        String _bestName;

        public SequenceFilter(int seqid)
        {
            _seqid = seqid;
            SimpleProtein prot = org.labkey.api.protein.ProteinManager.getSimpleProtein(seqid);
            _sequence = prot.getSequence();
            _bestName = prot.getBestName();
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sqlf = new SQLFragment();
            sqlf.append(dialect.getStringIndexOfFunction(new SQLFragment("TrimmedPeptide"), new SQLFragment("?", _sequence)));
            sqlf.append( " > 0 ");
            return sqlf;
        }

        @Override
        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(FieldKey.fromParts("TrimmedPeptide"));
        }

        @Override
        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
        {
            sb.append("Matches sequence of ");
            sb.append(_bestName);
        }
    }
}
