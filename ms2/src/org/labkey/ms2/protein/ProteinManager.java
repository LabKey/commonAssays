/*
 * Copyright (c) 2005-2010 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.*;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.DomainNotFoundException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.query.FieldKey;
import org.labkey.api.search.SearchService;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.Path;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.webdav.SimpleDocumentResource;
import org.labkey.ms2.*;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: arauch
 * Date: Mar 23, 2005
 * Time: 9:58:17 PM
 */
public class ProteinManager
{
    public static final SearchService.SearchCategory proteinCategory = new SearchService.SearchCategory("protein", "Protein");
    
    private static Logger _log = Logger.getLogger(ProteinManager.class);
    private static final String SCHEMA_NAME = "prot";

    public static final int RUN_FILTER = 1;
    public static final int URL_FILTER = 2;
    public static final int EXTRA_FILTER = 4;
    public static final int PROTEIN_FILTER = 8;
    public static final int ALL_FILTERS = RUN_FILTER + URL_FILTER + EXTRA_FILTER + PROTEIN_FILTER;

    public static String getSchemaName()
    {
        return SCHEMA_NAME;
    }


    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }


    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }


    public static TableInfo getTableInfoFastaFiles()
    {
        return getSchema().getTable("FastaFiles");
    }


    public static TableInfo getTableInfoFastaSequences()
    {
        return getSchema().getTable("FastaSequences");
    }


    public static TableInfo getTableInfoFastaAdmin()
    {
        return getSchema().getTable("FastaAdmin");
    }


    public static TableInfo getTableInfoAnnotInsertions()
    {
        return getSchema().getTable("AnnotInsertions");
    }


    public static TableInfo getTableInfoCustomAnnotation()
    {
        return getSchema().getTable("CustomAnnotation");
    }

    public static TableInfo getTableInfoCustomAnnotationSet()
    {
        return getSchema().getTable("CustomAnnotationSet");
    }

    public static TableInfo getTableInfoAnnotations()
    {
        return getSchema().getTable("Annotations");
    }


    public static TableInfo getTableInfoAnnotationTypes()
    {
        return getSchema().getTable("AnnotationTypes");
    }


    public static TableInfo getTableInfoIdentifiers()
    {
        return getSchema().getTable("Identifiers");
    }


    public static TableInfo getTableInfoIdentTypes()
    {
        return getSchema().getTable("IdentTypes");
    }


    public static TableInfo getTableInfoOrganisms()
    {
        return getSchema().getTable("Organisms");
    }


    public static TableInfo getTableInfoInfoSources()
    {
        return getSchema().getTable("InfoSources");
    }


    public static TableInfo getTableInfoSequences()
    {
        return getSchema().getTable("Sequences");
    }


    public static TableInfo getTableInfoFastaLoads()
    {
        return getSchema().getTable("FastaLoads");
    }


    public static TableInfo getTableInfoSprotOrgMap()
    {
        return getSchema().getTable("SprotOrgMap");
    }

    public static TableInfo getTableInfoGoTerm()
    {
        return getSchema().getTable("GoTerm");
    }

    public static TableInfo getTableInfoGoTerm2Term()
    {
        return getSchema().getTable("GoTerm2Term");
    }

    public static TableInfo getTableInfoGoGraphPath()
    {
        return getSchema().getTable("GoGraphPath");
    }

    public static TableInfo getTableInfoGoTermDefinition()
    {
        return getSchema().getTable("GoTermDefinition");
    }

    public static TableInfo getTableInfoGoTermSynonym()
    {
        return getSchema().getTable("GoTermSynonym");
    }


    public static Protein getProtein(int seqId) throws SQLException
    {
        Protein[] proteins = Table.executeQuery(getSchema(),
                "SELECT SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + getTableInfoSequences() + " WHERE SeqId=?",
                new Object[]{seqId}, Protein.class);

        if (proteins.length < 1)
            _log.error("Protein sequence not found: " + seqId);
        else if (proteins.length > 1)
            _log.error("Duplicate protein sequence found: " + seqId);
        else
            return proteins[0];

        return null;
    }


    public static Protein[] getProteinsContainingPeptide(int fastaId, MS2Peptide peptide) throws SQLException
    {
        if ((null == peptide) || ("".equals(peptide.getTrimmedPeptide())) || (peptide.getProteinHits() < 1))
            return new Protein[0];

        int hits = peptide.getProteinHits();
        SQLFragment sql= new SQLFragment();
        Object[] params;
        if (hits ==1)
        {
            sql.append("SELECT SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + getTableInfoSequences());
            sql.append(" WHERE SeqId = (SELECT SeqId FROM " + getTableInfoFastaSequences() + " WHERE FastaId = ? AND LookupString = ?)" );
            params = new Object[]{fastaId, peptide.getProtein()};
        }
        else
        {
            //TODO:  make search tryptic so that number that match = ProteinHits
            int MAX_PROTEIN_HITS = 20;  //based on observations of 2 larger ms2 databases, TOP 20 causes better query plan generation in SQL Server
            sql.append("SELECT  SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + getTableInfoSequences() );
            sql.append(" WHERE SeqId IN (SELECT SeqId FROM " + getTableInfoFastaSequences() + " WHERE FastaId=?) AND ProtSequence " + getSqlDialect().getCharClassLikeOperator() + " ?" );
            sql = getSchema().getSqlDialect().limitRows(sql, MAX_PROTEIN_HITS);
            params = new Object[]{fastaId, "%" + peptide.getTrimmedPeptide() + "%"};
        }

        // TODO: Retrieve and set the lookupString on each protein so the names in the details match the names in the results
        Protein[] proteins = Table.executeQuery(getSchema(), sql.getSQL(), params, Protein.class);

        if (proteins.length > 0)
            return proteins;

        _log.error("getProteinsContainingPeptide: Could not find peptide " + peptide + " in FASTA file " + fastaId);

        return proteins;
    }


    private static final NumberFormat generalFormat = new DecimalFormat("0.0#");

    public static void addExtraFilter(SimpleFilter filter, MS2Run run, ActionURL currentUrl)
    {
        String paramName = run.getChargeFilterParamName();

        boolean includeChargeFilter = false;
        Float[] values = new Float[3];

        for (int i = 0; i < values.length; i++)
        {
            String threshold = currentUrl.getParameter(paramName + (i + 1));

            if (null != threshold && !"".equals(threshold))
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
            filter.addClause(new ChargeFilter(run.getChargeFilterColumnName(), values));

        String tryptic = currentUrl.getParameter("tryptic");

        // Add tryptic filter
        if ("1".equals(tryptic))
            filter.addClause(new TrypticFilter(1));
        else if ("2".equals(tryptic))
            filter.addClause(new TrypticFilter(2));
    }

    public static Map<String, CustomAnnotationSet> getCustomAnnotationSets(Container container, boolean includeProject)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(getTableInfoCustomAnnotationSet());
        sql.append(" WHERE Container = ? ");
        sql.add(container.getId());
        if (includeProject)
        {
            Container project = container.getProject();
            if (project != null && !project.equals(container))
            {
                sql.append(" OR Container = ? ");
                sql.add(project.getId());
            }
        }
        sql.append(" ORDER BY Name");
        try
        {
            CustomAnnotationSet[] allSets = Table.executeQuery(getSchema(), sql, CustomAnnotationSet.class);

            Set<String> setNames = new CaseInsensitiveHashSet();
            List<CustomAnnotationSet> dedupedSets = new ArrayList<CustomAnnotationSet>(allSets.length);
            // If there are any name collisions, we want sets in this container to mask the ones in the project

            // Take a first pass through to add all the ones from this container
            for (CustomAnnotationSet set : allSets)
            {
                if (set.getContainer().equals(container.getId()))
                {
                    setNames.add(set.getName());
                    dedupedSets.add(set);
                }
            }

            // Take a second pass through to add all the ones from the project that don't collide
            for (CustomAnnotationSet set : allSets)
            {
                if (!set.getContainer().equals(container.getId()) && setNames.add(set.getName()))
                {
                    dedupedSets.add(set);
                }
            }

            Collections.sort(dedupedSets, new Comparator<CustomAnnotationSet>()
            {
                public int compare(CustomAnnotationSet o1, CustomAnnotationSet o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            Map<String, CustomAnnotationSet> result = new LinkedHashMap<String, CustomAnnotationSet>();
            for (CustomAnnotationSet set : dedupedSets)
            {
                result.put(set.getName(), set);
            }
            return result;
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public static void deleteCustomAnnotationSet(CustomAnnotationSet set)
    {
        try
        {
            Container c = ContainerManager.getForId(set.getContainer());
            if (OntologyManager.getDomainDescriptor(set.getLsid(), c) != null)
            {
                OntologyManager.deleteOntologyObject(set.getLsid(), c, true);
                OntologyManager.deleteDomain(set.getLsid(), c);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        catch (DomainNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        boolean existingTransaction = getSchema().getScope().isTransactionActive();
        try
        {
            if (!existingTransaction)
            {
                getSchema().getScope().beginTransaction();
            }
            Table.execute(getSchema(), "DELETE FROM " + getTableInfoCustomAnnotation() + " WHERE CustomAnnotationSetId = ?", new Object[] {set.getCustomAnnotationSetId()});
            Table.delete(getTableInfoCustomAnnotationSet(), set.getCustomAnnotationSetId());
            if (!existingTransaction)
            {
                getSchema().getScope().commitTransaction();
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (!existingTransaction)
            {
                getSchema().getScope().closeConnection();
            }
        }
    }

    public static CustomAnnotationSet getCustomAnnotationSet(Container c, int id, boolean includeProject)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(getTableInfoCustomAnnotationSet());
        sql.append(" WHERE (Container = ?");
        sql.add(c.getId());
        if (includeProject)
        {
            sql.append(" OR Container = ?");
            sql.add(c.getProject().getId());
        }
        sql.append(") AND CustomAnnotationSetId = ?");
        sql.add(id);
        try
        {
            CustomAnnotationSet[] matches = Table.executeQuery(getSchema(), sql, CustomAnnotationSet.class);
            if (matches.length > 1)
            {
                for (CustomAnnotationSet set : matches)
                {
                    if (set.getContainer().equals(c.getId()))
                    {
                        return set;
                    }
                }
                assert false : "More than one matching set was found but none were in the current container";
                return matches[0];
            }
            if (matches.length == 1)
            {
                return matches[0];
            }
            return null;
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

    }

    public static void migrateRuns(int oldFastaId, int newFastaId)
            throws SQLException
    {
        SQLFragment mappingSQL = new SQLFragment("SELECT fs1.seqid AS OldSeqId, fs2.seqid AS NewSeqId\n");
        mappingSQL.append("FROM \n");
        mappingSQL.append("\t(SELECT ff.SeqId, s.Hash, ff.LookupString FROM " + getTableInfoFastaSequences() + " ff, " + getTableInfoSequences() + " s WHERE ff.SeqId = s.SeqId AND ff.FastaId = " + oldFastaId + ") fs1 \n");
        mappingSQL.append("\tLEFT OUTER JOIN \n");
        mappingSQL.append("\t(SELECT ff.SeqId, s.Hash, ff.LookupString FROM " + getTableInfoFastaSequences() + " ff, " + getTableInfoSequences() + " s WHERE ff.SeqId = s.SeqId AND ff.FastaId = " + newFastaId + ") fs2 \n");
        mappingSQL.append("\tON (fs1.Hash = fs2.Hash AND fs1.LookupString = fs2.LookupString)");

        SQLFragment missingCountSQL = new SQLFragment("SELECT COUNT(*) FROM (");
        missingCountSQL.append(mappingSQL);
        missingCountSQL.append(") Mapping WHERE OldSeqId IN (\n");
        missingCountSQL.append("(SELECT p.SeqId FROM " + MS2Manager.getTableInfoPeptides() + " p, " + MS2Manager.getTableInfoRuns() + " r WHERE p.run = r.Run AND r.FastaId = " + oldFastaId + ")\n");
        missingCountSQL.append("UNION\n");
        missingCountSQL.append("(SELECT pgm.SeqId FROM " + MS2Manager.getTableInfoProteinGroupMemberships() + " pgm, " + MS2Manager.getTableInfoProteinGroups() + " pg, " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf, " + MS2Manager.getTableInfoRuns() + " r WHERE pgm.ProteinGroupId = pg.RowId AND pg.ProteinProphetFileId = ppf.RowId AND ppf.Run = r.Run AND r.FastaId = " + oldFastaId + "))\n");
        missingCountSQL.append("AND NewSeqId IS NULL");

        int missingCount = Table.executeSingleton(getSchema(), missingCountSQL.getSQL(), missingCountSQL.getParamsArray(), Integer.class);
        if (missingCount > 0)
        {
            throw new SQLException("There are " + missingCount + " protein sequences in the original FASTA file that are not in the new file");
        }

        try
        {
            MS2Manager.getSchema().getScope().beginTransaction();

            SQLFragment updatePeptidesSQL = new SQLFragment();
            updatePeptidesSQL.append("UPDATE " + MS2Manager.getTableInfoPeptidesData() + " SET SeqId = map.NewSeqId");
            updatePeptidesSQL.append("\tFROM " + MS2Manager.getTableInfoFractions() + " f \n");
            updatePeptidesSQL.append("\t, " + MS2Manager.getTableInfoRuns() + " r\n");
            updatePeptidesSQL.append("\t, (");
            updatePeptidesSQL.append(mappingSQL);
            updatePeptidesSQL.append(") map \n");
            updatePeptidesSQL.append("WHERE f.Fraction = " + MS2Manager.getTableInfoPeptidesData() + ".Fraction\n");
            updatePeptidesSQL.append("\tAND r.Run = f.Run\n");
            updatePeptidesSQL.append("\tAND " + MS2Manager.getTableInfoPeptidesData() + ".SeqId = map.OldSeqId \n");
            updatePeptidesSQL.append("\tAND r.FastaId = " + oldFastaId);

            Table.execute(MS2Manager.getSchema(), updatePeptidesSQL);

            SQLFragment updateProteinsSQL = new SQLFragment();
            updateProteinsSQL.append("UPDATE " + MS2Manager.getTableInfoProteinGroupMemberships() + " SET SeqId= map.NewSeqId\n");
            updateProteinsSQL.append("FROM " + MS2Manager.getTableInfoProteinGroups() + " pg\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoRuns() + " r\n");
            updateProteinsSQL.append("\t, (");
            updateProteinsSQL.append(mappingSQL);
            updateProteinsSQL.append(") map \n");
            updateProteinsSQL.append("WHERE " + MS2Manager.getTableInfoProteinGroupMemberships() + ".ProteinGroupId = pg.RowId\n");
            updateProteinsSQL.append("\tAND pg.ProteinProphetFileId = ppf.RowId\n");
            updateProteinsSQL.append("\tAND r.Run = ppf.Run\n");
            updateProteinsSQL.append("\tAND " + MS2Manager.getTableInfoProteinGroupMemberships() + ".SeqId = map.OldSeqId\n");
            updateProteinsSQL.append("\tAND r.FastaId = " + oldFastaId);

            Table.execute(MS2Manager.getSchema(), updateProteinsSQL);

            Table.execute(MS2Manager.getSchema(), "UPDATE " + MS2Manager.getTableInfoRuns() + " SET FastaID = ? WHERE FastaID = ?", new Object[] { newFastaId, oldFastaId } );
            MS2Manager.getSchema().getScope().commitTransaction();
        }
        finally
        {
            MS2Manager.getSchema().getScope().closeConnection();
        }
    }


    public static class ChargeFilter extends SimpleFilter.FilterClause
    {
        private String _columnName;
        private Float[] _values;

        // At least one value must be non-null
        public ChargeFilter(String columnName, Float[] values)
        {
            _columnName = columnName;
            _values = values;
        }


        public List<String> getColumnNames()
        {
            return Arrays.asList(_columnName);
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        public SQLFragment toSQLFragment(Map<String, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sql = new SQLFragment();

            sql.append(_columnName);
            sql.append(" >= CASE Charge");

            for (int i = 0; i < _values.length; i++)
            {
                if (null != _values[i])
                {
                    sql.append(" WHEN ");
                    sql.append(i + 1);
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
                    sb.append(formatter.format(_columnName));
                    sb.append(" >= ").append(generalFormat.format(_values[i]));
                }
            }
        }
    }


    public static class TrypticFilter extends SimpleFilter.FilterClause
    {
        private int _termini;

        public TrypticFilter(int termini)
        {
            _termini = termini;
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        public SQLFragment toSQLFragment(Map<String, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sql = new SQLFragment();
            switch(_termini)
            {
                case(0):
                    sql.append("");
                    break;

                case(1):
                    sql.append(nTerm(dialect) + " OR " + cTerm(dialect));
                    break;

                case(2):
                    sql.append(nTerm(dialect) + " AND " + cTerm(dialect));
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

        public List<String> getColumnNames()
        {
            return Arrays.asList("StrippedPeptide");
        }

        @Override
        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
        {
            sb.append("Trypic Ends ");
            sb.append(1 == _termini ? ">= " : "= ");
            sb.append(_termini);
        }
    }

    public static Sort getPeptideBaseSort()
    {
        // Always sort peptide lists by Fraction, Scan, Charge
        return new Sort("Fraction,Scan,Charge");
    }

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, List<MS2Run> runs, int mask)
    {
        // Cop-out for now... we've already checked to make sure all runs are the same type
        // TODO: Allow runs of different type, by one of the following:
        // 1) verify that no search-engine-specific scores are used in the filter OR
        // 2) ignore filters that don't apply to a particular run, and provide a warning OR
        // 3) allowing picking one filter per run type
        return getPeptideFilter(currentUrl, mask, runs.get(0));
    }

    public static SimpleFilter reduceToValidColumns(SimpleFilter fullFilter, TableInfo... tables)
    {
        SimpleFilter validFilter = new SimpleFilter();
        for (SimpleFilter.FilterClause clause : fullFilter.getClauses())
        {
            boolean validClause = false;
            for (String columnName : clause.getColumnNames())
            {
                for (TableInfo table : tables)
                {
                    ColumnInfo column = table.getColumn(columnName);
                    if (column == null)
                    {
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
                            if (clause.getParamVals() != null)
                            {
                                for (Object o : clause.getParamVals())
                                {
                                    if (o != null)
                                    {
                                        ConvertUtils.convert(o.toString(), column.getJavaClass());
                                    }
                                }
                            }
                            validClause = true;
                        }
                        catch (ConversionException e) {}
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

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, int mask, MS2Run... runs)
    {
        return getPeptideFilter(currentUrl, mask, null, runs);
    }

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, int mask, String runTableName, MS2Run... runs)
    {
        return getTableFilter(currentUrl, mask, runTableName, MS2Manager.getDataRegionNamePeptides(), runs);
    }

    public static SimpleFilter getProteinFilter(ActionURL currentUrl, int mask, String runTableName, MS2Run... runs)
    {
        return getTableFilter(currentUrl, mask, runTableName, MS2Manager.getDataRegionNameProteins(), runs);
    }

    public static SimpleFilter getProteinGroupFilter(ActionURL currentUrl, int mask, String runTableName, MS2Run... runs)
    {
        return getTableFilter(currentUrl, mask, runTableName, MS2Manager.getDataRegionNameProteinGroups(), runs);
    }

    public static SimpleFilter getTableFilter(ActionURL currentUrl, int mask, String runTableName, String dataRegionName, MS2Run... runs)
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
            String seqId = currentUrl.getParameter("seqId");

            if (null != seqId)
                filter.addCondition("SeqId", Integer.parseInt(seqId));
        }

        return filter;
    }

    public static void addRunCondition(SimpleFilter filter, String runTableName, MS2Run... runs)
    {
        String columnName = runTableName == null ? "Run" : runTableName + ".Run";
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
        filter.addWhereClause(sb.toString(), new Object[0], columnName);
    }


    public static void replaceRunCondition(SimpleFilter filter, String runTableName, MS2Run... runs)
    {
        String columnName = runTableName == null ? "Run" : runTableName + ".Run";
        filter.deleteConditions(columnName);
        addRunCondition(filter, runTableName, runs);
    }


    public static void addProteinQuery(SQLFragment sql, MS2Run run, ActionURL currentUrl, String extraPeptideWhere, int maxRows, boolean peptideQuery)
    {
        // SELECT (TOP n) Protein, SequenceMass, etc.
        StringBuilder proteinSql = new StringBuilder("SELECT Protein");

        // If this query is a subselect of proteins to which we join the peptide table, we need to:
        // 1. Alias Protein to prevent SELECT and ORDER BY ambiguity after joining to the peptides table (easier to do this than to disambiguate outside the subselect)
        // 2. Include SequenceMass since we're not joining again to the ProteinSequence table (we do this in the protein version to get Sequence, but don't need it here)
        if (peptideQuery)
            proteinSql.append(" AS PProtein, prot.BestName, prot.BestGeneName");

        proteinSql.append(", prot.Mass AS SequenceMass, COUNT(Peptide) AS Peptides, COUNT(DISTINCT Peptide) AS UniquePeptides, pep.SeqId AS sSeqId\n");
        proteinSql.append("FROM (SELECT * FROM ");
        proteinSql.append(MS2Manager.getTableInfoPeptides());
        proteinSql.append(' ');

        // Construct Peptide WHERE clause (no need to sort by peptide)
        SimpleFilter peptideFilter = getPeptideFilter(currentUrl, RUN_FILTER + URL_FILTER + EXTRA_FILTER, run);
        peptideFilter = ProteinManager.reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoPeptides());
        if (null != extraPeptideWhere)
            peptideFilter.addWhereClause(extraPeptideWhere, new Object[]{});
        proteinSql.append(peptideFilter.getWhereSQL(getSqlDialect()));
        sql.addAll(peptideFilter.getWhereParams(MS2Manager.getTableInfoPeptides()));

        proteinSql.append(") pep LEFT OUTER JOIN ");
        proteinSql.append(getTableInfoSequences());
        proteinSql.append(" prot ON prot.SeqId = pep.SeqId\n");
        proteinSql.append("GROUP BY Protein, prot.Mass, pep.SeqId, prot.BestGeneName, prot.BestName, prot.Description, prot.SeqId\n");

        // Construct Protein HAVING clause
        SimpleFilter proteinFilter = new SimpleFilter(currentUrl, MS2Manager.getDataRegionNameProteins());
        proteinFilter = ProteinManager.reduceToValidColumns(proteinFilter, MS2Manager.getTableInfoProteins());
        String proteinHaving = proteinFilter.getWhereSQL(getSqlDialect()).replaceFirst("WHERE", "HAVING");

        // Can't use SELECT aliases in HAVING clause, so replace names with aggregate functions & disambiguate Mass
        proteinHaving = proteinHaving.replaceAll("UniquePeptides", "COUNT(DISTINCT Peptide)");
        proteinHaving = proteinHaving.replaceAll("Peptides", "COUNT(Peptide)");
        proteinHaving = proteinHaving.replaceAll("SeqId", "prot.SeqId");
        proteinHaving = proteinHaving.replaceAll("SequenceMass", "prot.Mass");
        proteinHaving = proteinHaving.replaceAll("Description", "prot.Description");
        sql.addAll(proteinFilter.getWhereParams(MS2Manager.getTableInfoProteins()));
        proteinSql.append(proteinHaving);

        if (!"".equals(proteinHaving))
            proteinSql.append('\n');

        // If we're limiting the number of proteins (e.g., no more than 250) we need to add the protein ORDER BY clause so we get the right rows.
        if (maxRows > 0)
        {
            Sort proteinSort = new Sort("Protein");
            proteinSort.addURLSort(currentUrl, MS2Manager.getDataRegionNameProteins());
            String proteinOrderBy = proteinSort.getOrderByClause(getSqlDialect());
            proteinOrderBy = proteinOrderBy.replaceAll("Description", "prot.Description");
            proteinSql.append(proteinOrderBy);

            getSqlDialect().limitRows(proteinSql, maxRows + 1);
        }

        sql.append(proteinSql);
    }

    public static ResultSet getProteinRS(ActionURL currentUrl, MS2Run run, String extraPeptideWhere, int maxRows) throws SQLException
    {
        SQLFragment sql = getProteinSql(currentUrl, run, extraPeptideWhere, maxRows);

        return Table.executeQuery(getSchema(), sql, maxRows);
    }

    public static SQLFragment getProteinSql(ActionURL currentUrl, MS2Run run, String extraPeptideWhere, int maxRows)
    {
        SQLFragment sql = new SQLFragment();

        // Join the selected proteins to ProteinSequences to get the actual Sequence for computing AA coverage
        // We need to do a second join to ProteinSequences because we can't GROUP BY Sequence, a text data type
        sql.append("SELECT Protein, SequenceMass, Peptides, UniquePeptides, SeqId, ProtSequence AS Sequence, Description, BestName, BestGeneName FROM\n(");
        addProteinQuery(sql, run, currentUrl, extraPeptideWhere, maxRows, false);
        sql.append("\n) X LEFT OUTER JOIN ");
        sql.append(getTableInfoSequences(), "seq");
        sql.append(" ON seq.SeqId = sSeqId\n");

        // Have to sort again to ensure correct order after the join
        Sort proteinSort = new Sort("Protein");
        proteinSort.addURLSort(currentUrl, MS2Manager.getDataRegionNameProteins());
        sql.append(proteinSort.getOrderByClause(getSqlDialect()));

        return sql;
    }

    public static ResultSet getProteinProphetRS(ActionURL currentUrl, MS2Run run, String extraPeptideWhere, int maxRows) throws SQLException
    {
        return new ResultSetCollapser(getProteinProphetPeptideRS(currentUrl, run, extraPeptideWhere, maxRows, "Scan"), "ProteinGroupId", maxRows);
    }

    // Combine protein sort and peptide sort into a single ORDER BY.  Must sort by "Protein" before sorting peptides to ensure
    // grouping of peptides is in sync with protein query.  We add the columns in least to most significant order (peptide sort
    // + protein column + protein sort) and the Sort class ensures that only the most significant "Protein" column remains.
    public static String getCombinedOrderBy(ActionURL currentUrl, String orderByColumnName)
    {
        Sort peptideSort = ProteinManager.getPeptideBaseSort();
        peptideSort.addURLSort(currentUrl, MS2Manager.getDataRegionNamePeptides());
        Sort proteinSort = new Sort(currentUrl, MS2Manager.getDataRegionNameProteins());
        Sort combinedSort = new Sort();
        combinedSort.insertSort(peptideSort);
        combinedSort.insertSortColumn(orderByColumnName, false);
        combinedSort.insertSort(proteinSort);
        combinedSort = reduceToValidColumns(combinedSort, MS2Manager.getTableInfoPeptides(), MS2Manager.getTableInfoProteins());
        return combinedSort.getOrderByClause(ProteinManager.getSqlDialect());
    }

    // Combine protein sort and peptide sort into a single ORDER BY.  Must sort by "Protein" before sorting peptides to ensure
    // grouping of peptides is in sync with protein query.  We add the columns in least to most significant order (peptide sort
    // + protein column + protein sort) and the Sort class ensures that only the most significant "Protein" column remains.
    public static String getProteinGroupCombinedOrderBy(ActionURL currentUrl, String orderByColumnName)
    {
        Sort peptideSort = ProteinManager.getPeptideBaseSort();
        peptideSort.addURLSort(currentUrl, MS2Manager.getDataRegionNamePeptides());
        Sort proteinSort = new Sort(currentUrl, MS2Manager.getDataRegionNameProteinGroups());
        Sort combinedSort = new Sort();
        combinedSort.insertSort(peptideSort);
        combinedSort.insertSortColumn(orderByColumnName, false);
        combinedSort.insertSort(proteinSort);
        combinedSort = reduceToValidColumns(combinedSort, MS2Manager.getTableInfoSimplePeptides(), MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        return combinedSort.getOrderByClause(ProteinManager.getSqlDialect());
    }


    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static GroupedResultSet getPeptideRS(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames) throws SQLException
    {
        SQLFragment sql = getPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames);

        ResultSet rs = Table.executeQuery(getSchema(), sql, Table.ALL_ROWS, false, true);
        return new GroupedResultSet(rs, "Protein");
    }

    public static SQLFragment getPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames)
    {
        return getPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames, true);
    }

    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static SQLFragment getPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, boolean addOrderBy)
    {
        SQLFragment sql = new SQLFragment();

        // SELECT TOP n m AS Run, Protein, etc.
        sql.append("SELECT ");
        sql.append(columnNames);
        sql.append(", Fraction AS Fraction$Fraction FROM ");
        sql.append(MS2Manager.getTableInfoPeptides());
        sql.append(" RIGHT OUTER JOIN\n(");

        ProteinManager.addProteinQuery(sql, run, currentUrl, extraWhere, maxProteinRows, true);
        sql.append(") s ON ");
        sql.append(MS2Manager.getTableInfoPeptides());
        sql.append(".SeqId = sSeqId\n");

        // Have to apply the peptide filter again, otherwise we'll just get all peptides mapping to each protein
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, run);
        peptideFilter = reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoPeptides());
        sql.append(peptideFilter.getWhereSQL(getSqlDialect()));
        sql.append('\n');
        sql.addAll(peptideFilter.getWhereParams(MS2Manager.getTableInfoPeptides()));
        if (addOrderBy)
            sql.append(getCombinedOrderBy(currentUrl, "Protein"));

        return sql;
    }

    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static Table.TableResultSet getProteinProphetPeptideRS(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames) throws SQLException
    {
        SQLFragment sql = getProteinProphetPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames);

        return (Table.TableResultSet)Table.executeQuery(getSchema(), sql, 0, false, true);
    }

    public static SQLFragment getProteinProphetPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames)
    {
        return getProteinProphetPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames, true);
    }

    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static SQLFragment getProteinProphetPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, boolean addOrderBy)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(".ProteinGroupId, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".IndistinguishableCollectionId, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupNumber, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupProbability, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".ErrorRate, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".ProteinProbability, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".UniquePeptidesCount, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".TotalNumberPeptides, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".PctSpectrumIds, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".PercentCoverage, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".RatioMean, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".RatioStandardDev, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".RatioNumberPeptides, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".Heavy2LightRatioMean, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".Heavy2LightRatioStandardDev, ");
        sql.append(MS2Manager.getTableInfoSimplePeptides() + ".Fraction AS Fraction$Fraction, ");
        sql.append(columnNames);

        sql.append(" FROM ");
        sql.append(MS2Manager.getTableInfoSimplePeptides());
        sql.append(" INNER JOIN\n");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(" ON ");
        sql.append(MS2Manager.getTableInfoSimplePeptides());
        sql.append(".RowId = ");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(".PeptideId INNER JOIN\n");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        sql.append(" ON ");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(".ProteinGroupId = ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        sql.append(".RowId INNER JOIN\n");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(" ON ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        sql.append(".ProteinProphetFileId = ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(".RowId");
        sql.append(" AND ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(".Run = ");
        sql.append(MS2Manager.getTableInfoSimplePeptides());
        sql.append(".Run\n");

        // Construct Peptide WHERE clause (no need to sort by peptide)
        SimpleFilter peptideFilter = getPeptideFilter(currentUrl, RUN_FILTER + URL_FILTER + EXTRA_FILTER, MS2Manager.getTableInfoSimplePeptides().toString(), run);
        peptideFilter = reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoSimplePeptides());
        if (null != extraWhere)
            peptideFilter.addWhereClause(extraWhere, new Object[]{});
        sql.append(peptideFilter.getWhereSQL(getSqlDialect()));
        sql.addAll(peptideFilter.getWhereParams(MS2Manager.getTableInfoPeptides()));

        sql.append("\n");

        SimpleFilter proteinFilter = new SimpleFilter(currentUrl, MS2Manager.getDataRegionNameProteinGroups());
        proteinFilter = reduceToValidColumns(proteinFilter, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        String proteinWhere = proteinFilter.getWhereSQL(getSqlDialect());
        if (proteinWhere != null && !"".equals(proteinWhere))
        {
            proteinWhere = proteinWhere.replaceFirst("WHERE", "AND");
            sql.addAll(proteinFilter.getWhereParams(MS2Manager.getTableInfoProteinGroupsWithQuantitation()));
            sql.append(proteinWhere);
            sql.append('\n');
        }
        if (addOrderBy)
        {
            // Work around ambiguous column problem on SQL Server 2000.  Need this string replacement hack since Sort
            // doesn't handle schema/table-qualified names correctly and a simple aliasing of the column name breaks
            // expectations in other code.  See #10460.
            String orderBy = getProteinGroupCombinedOrderBy(currentUrl, "ProteinGroupId");
            sql.append(orderBy.replace("ProteinGroupId", MS2Manager.getTableInfoPeptideMemberships() + ".ProteinGroupId"));
        }
        if (maxProteinRows > 0)
        {
            getSqlDialect().limitRows(sql, maxProteinRows + 1);
        }

        return sql;
    }

    public static MultiMap<String, String> getIdentifiersFromId(int seqid) throws SQLException
    {
        ResultSet rs = null;
        try
        {
            MultiMap<String, String> map = new MultiHashMap<String, String>();
            rs = Table.executeQuery(getSchema(),
                    "SELECT T.name AS name, I.identifier\n" +
                    "FROM " + getTableInfoIdentifiers() + " I INNER JOIN " + getTableInfoIdentTypes() + " T ON I.identtypeid = T.identtypeid\n" +
                    "WHERE seqId = ?",
                    new Object[]{seqid});
            while (rs.next())
            {
                String name = rs.getString(1).toLowerCase();
                String id = rs.getString(2);
                if (name.startsWith("go_"))
                    name = "go";
                map.put(name, id);
            }
            return map;
        }
        finally
        {
            ResultSetUtil.close(rs);
        }
    }


    public static Set<String> getOrganismsFromId(int id) throws SQLException
    {
        HashSet<String> retVal = new HashSet<String>();
        Integer paramArr[] = {id};
        String rvString[] = Table.executeArray(getSchema(),
                "SELECT annotVal FROM " + getTableInfoAnnotations() + " WHERE annotTypeId in (SELECT annotTypeId FROM " + getTableInfoAnnotationTypes() + " WHERE name " + getSqlDialect().getCharClassLikeOperator() + " '%Organism%') AND seqID=?",
                paramArr,
                String.class);

        retVal.addAll(Arrays.asList(rvString));

        String org = Table.executeSingleton(getSchema(),
                "SELECT " + getSchema().getSqlDialect().concatenate("genus", "' '", "species") +
                        " FROM " + getTableInfoOrganisms() +
                        " WHERE orgid=(SELECT orgid FROM " + getTableInfoSequences() +
                        " WHERE seqid=?)",
                paramArr, String.class);

        retVal.add(org);
        return retVal;
    }


    public static String makeIdentURLString(String identifier, String infoSourceURLString) throws UnsupportedEncodingException
    {
        if (identifier == null || infoSourceURLString == null)
            return null;

        identifier = java.net.URLEncoder.encode(identifier, "UTF-8");

        return infoSourceURLString.replaceAll("\\{\\}", identifier);
    }


    static final String NOTFOUND = "NOTFOUND";
    static final Map<String, String> cacheURLs = new ConcurrentHashMap<String, String>(200);

    public static String makeIdentURLStringWithType(String identifier, String identType) throws Exception
    {
        if (identifier == null || identType == null)
            return null;

        String url = cacheURLs.get(identType);
        if (url == null)
        {
            url = Table.executeSingleton(getSchema(), 
                    "SELECT S.url\n" +
                    "FROM " + ProteinManager.getTableInfoInfoSources() + " S INNER JOIN " + ProteinManager.getTableInfoIdentTypes() +" T " +
                        "ON S.sourceId = T.cannonicalSourceId\n" +
                    "WHERE T.name=?",
                    new Object[]{identType},
                    String.class);
            cacheURLs.put(identType, null==url ? NOTFOUND : url);
        }
        if (null == url || NOTFOUND == url)
            return null;

        return makeIdentURLString(identifier, url);
    }


    public static String makeFullAnchorString(String url, String target, String txt)
    {
        if (txt == null) return "";
        String retVal = "";
        if (url != null) retVal += "<a ";
        if (url != null && target != null) retVal += "target='" + target + "' ";
        if (url != null) retVal += "href='" + url + "'>";
        retVal += txt;
        if (url != null) retVal += "</a>";
        return retVal;
    }

    public static String[] makeFullAnchorStringArray(Collection<String> idents, String target, String identType) throws Exception
    {
        if (idents == null || idents.isEmpty() || identType == null)
            return new String[0];
        String[] retVal = new String[idents.size()];
        int i = 0;
        for (String ident : idents)
            retVal[i++] = makeFullAnchorString(makeIdentURLStringWithType(ident, identType), target, ident);
        return retVal;
    }

    public static String[] makeFullGOAnchorStringArray(Collection<String> goStrings, String target) throws Exception
    {
        if (goStrings == null) return new String[0];
        String[] retVal = new String[goStrings.size()];
        int i=0;
        for (String go : goStrings)
        {
            String sub = go.indexOf(" ") == -1 ? go : go.substring(0, go.indexOf(" "));
            retVal[i++] = makeFullAnchorString(
                    makeIdentURLStringWithType(sub, "GO"),
                    target,
                    go
            );
        }
        return retVal;
    }


    /** Deletes all ProteinSequences, and the FastaFile record as well */
    public static void deleteFastaFile(int fastaId) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoFastaSequences() + " WHERE FastaId = ?", new Object[]{fastaId});
        Table.execute(getSchema(), "UPDATE " + getTableInfoFastaFiles() + " SET Loaded=NULL WHERE FastaId = ?", new Object[]{fastaId});

        Table.execute(getSchema(), "DELETE FROM " + getTableInfoFastaFiles() + " WHERE FastaId = ?", new Object[]{fastaId});
    }


    public static void deleteAnnotationInsertion(int id) throws SQLException
    {
        SQLFragment sql = new SQLFragment("DELETE FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId = ?");
        sql.add(id);

        Table.execute(ProteinManager.getSchema(), sql);
    }


    public static void indexProteins(SearchService.IndexTask task, Date modifiedSince)
    {
        if (1==1)
            return;
        if (null != modifiedSince)
        {
            try
            {
                Date d = Table.executeSingleton(getSchema(),"SELECT max(insertdate) from prot.annotinsertions",null, Timestamp.class);
                if (null != d && d.compareTo(modifiedSince) <= 0)
                    return;
            }
            catch (SQLException x)
            {
                _log.error("Unexpected sql exception", x);
            }
        }

        if (null == task)
        {
            SearchService ss = ServiceRegistry.get().getService(SearchService.class);
            task = null == ss ? null : ss.createTask("Index Proteins");
            if (null == task)
                return;
        }

        final SearchService.IndexTask t = task;
        task.addRunnable(new Runnable(){
            public void run()
            {
                try
                {
                    Integer[] arr = Table.executeArray(getSchema(), "SELECT seqId FROM prot.sequences", null, Integer.class);
                    indexProteins(t, Arrays.asList(arr));
                }
                catch (SQLException x)
                {
                    throw new RuntimeSQLException(x);
                }
            }
        }, SearchService.PRIORITY.background);
    }
    

    public static void indexProteins(final SearchService.IndexTask task, List<Integer> list)
    {
        int from = 0;
        while (from < list.size())
        {
            final int[] ids = new int[1000];
            int to=0;
            while (to < ids.length && from < list.size())
                ids[to++] = list.get(from++).intValue();
            task.addRunnable(new Runnable(){
                public void run()
                {
                    indexProteins(task,ids);
                }
            }, SearchService.PRIORITY.bulk);
        }
    }


    public static void indexProteins(SearchService.IndexTask task, int[] ids)
    {
        Container c = ContainerManager.getHomeContainer();
        ActionURL url = new ActionURL(MS2Controller.ShowProteinAction.class, c);

        if (0==1) // one at at time
        {
            for (int id : ids)
            {
                if (0==id)
                    continue;
                try
                {
                    Protein p = getProtein(id);
                    MultiMap<String, String> map = getIdentifiersFromId(id);
                    StringBuilder sb = new StringBuilder();
                    sb.append(p.getBestName()).append("\n");
                    sb.append(p.getDescription()).append("\n");
                    for (String v : map.values())
                    {
                        sb.append(v).append(" ");
                    }

                    String docid = "protein:" + id;
                    Map<String,Object> m = new HashMap<String,Object>();
                    m.put(SearchService.PROPERTY.categories.toString(), proteinCategory);
                    m.put(SearchService.PROPERTY.displayTitle.toString(), "Protein " + p.getBestName());
                    SimpleDocumentResource r = new SimpleDocumentResource(
                            new Path(docid),
                            docid,
                            c.getId(), "text/plain",
                            sb.toString().getBytes(),
                            url.clone().addParameter("seqId",id),
                            m);
                    task.addResource(r, SearchService.PRIORITY.item);
                }
                catch (SQLException x)
                {
                    throw new RuntimeSQLException(x);
                }
            }
        }
        else // fast query
        {
            SQLFragment sql = new SQLFragment();
            sql.append("SELECT I.seqid, S.BestName, S.Description, I.identifier\n");
            sql.append("FROM " + getTableInfoSequences() + " S INNER JOIN " + getTableInfoIdentifiers() + " I ON S.seqid = I.seqid\n");
            sql.append("WHERE I.seqid IN (");
            String comma = "";
            int count = 0;
            for (int id : ids)
            {
                if (id == 0) continue;
                count++;
                sql.append(comma);
                sql.append(id);
                comma = ",";
            }
            if (count == 0)
                return;
            sql.append(")\nORDER BY I.seqid");
            ResultSet rs = null;
            try
            {
                rs = Table.executeQuery(getSchema(), sql, 0, false, false);
                int curSeqId = 0;
                StringBuilder sb = null;

                int seqid;
                String bestName = "";
                String description = "";
                String ident = "";

                do {
                    seqid = 0;
                    if (rs.next())
                    {
                        seqid = rs.getInt(1);
                        bestName = rs.getString(2);
                        description = rs.getString(3);
                        ident = rs.getString(4);
                    }
                    if (seqid != curSeqId)
                    {
                        if (curSeqId > 0)
                        {
                            String docid = "protein:" + curSeqId;
                            Map<String,Object> m = new HashMap<String,Object>();
                            m.put(SearchService.PROPERTY.categories.toString(), proteinCategory);
                            m.put(SearchService.PROPERTY.displayTitle.toString(), "Protein " + bestName);
                            SimpleDocumentResource r = new SimpleDocumentResource(
                                    new Path(docid),
                                    docid,
                                    c.getId(), "text/plain",
                                    sb.toString().getBytes(),
                                    url.clone().addParameter("seqId",curSeqId),
                                    m);
                            task.addResource(r, SearchService.PRIORITY.item);
                        }

                        sb = new StringBuilder(bestName + "\n" + description + "\n");
                        curSeqId = seqid;
                    }
                    sb.append(ident + " ");
                } while (seqid > 0);
            }
            catch (SQLException x)
            {
                throw new RuntimeSQLException(x);
            }
            finally
            {
                ResultSetUtil.close(rs);
            }
        }
    }
}
