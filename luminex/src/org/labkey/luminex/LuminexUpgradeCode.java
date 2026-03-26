package org.labkey.luminex;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.assay.AssayService;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.BeanObjectFactory;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.ObjectFactory;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.dataiterator.DataIteratorContext;
import org.labkey.api.dataiterator.MapDataIterator;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.GUID;
import org.labkey.luminex.model.LuminexDataRow;
import org.labkey.luminex.query.LuminexDataTable;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuminexUpgradeCode implements UpgradeCode
{
    private static final Logger _log = LogManager.getLogger(LuminexUpgradeCode.class);

    /**
     * GitHub Issue #875: Upgrade code to check for Luminex assay runs that have both summary and raw data but are missing summary rows.
     * NOTE: this upgrade code is not called from a SQL upgrade script. It is meant to be run manually from the admin console SQL Scripts page.
     */
    public static void checkForMissingSummaryRows(ModuleContext ctx)
    {
        if (ctx.isNewInstall())
            return;

        DbScope scope = LuminexProtocolSchema.getSchema().getScope();
        try (DbScope.Transaction tx = scope.ensureTransaction())
        {
            // For any Luminex dataids (input files) that have both summary and raw data rows,
            // find Luminex raw data rows (summary = false) that don't have a corresponding summary data row (summary = true)
            SQLFragment missingSummaryRowsSql = new SQLFragment("""
                SELECT DISTINCT d.runid, dr_false.dataid, dr_false.analyteid, dr_false.type
                FROM luminex.datarow dr_false
                LEFT JOIN exp.data d ON d.rowid = dr_false.dataid
                WHERE dr_false.summary = false
                  AND EXISTS (SELECT 1 FROM luminex.datarow WHERE dataid = dr_false.dataid AND summary = true)
                  AND EXISTS (SELECT 1 FROM luminex.datarow WHERE dataid = dr_false.dataid AND summary = false)
                  AND NOT EXISTS (SELECT 1 FROM luminex.datarow dr_true
                    WHERE dr_true.summary = true
                      AND dr_true.dataid = dr_false.dataid
                      AND dr_true.analyteid = dr_false.analyteid
                      AND dr_true.type = dr_false.type
                )
            """);

            int missingSummaryRowCount = new SqlSelector(scope, new SQLFragment("SELECT COUNT(*) FROM (").append(missingSummaryRowsSql).append(")")).getObject(Integer.class);
            if (missingSummaryRowCount == 0)
            {
                _log.info("No missing summary rows found for Luminex assay data.");
                return;
            }

            new SqlSelector(scope, missingSummaryRowsSql).forEach(rs -> {
                int runid = rs.getInt("runid");
                int dataId = rs.getInt("dataid");
                int analyteId = rs.getInt("analyteid");
                String type = rs.getString("type");

                ExpRun expRun = ExperimentService.get().getExpRun(runid);
                if (expRun == null)
                {
                    _log.warn("Could not find run for runid: " + runid + ", skipping missing summary row check for Luminex dataId: " + dataId + ", analyteId: " + analyteId + ", type: " + type);
                    return;
                }

                _log.info("Missing summary row for Luminex dataId: " + dataId + ", analyteId: " + analyteId + ", type: " + type + " in run: " + expRun.getName() + " (" + expRun.getRowId() + ")");

                // currently only inserting summary rows for Background (type = B) data rows
                if (!"B".equals(type))
                {
                    _log.warn("...not inserting missing summary row for Luminex dataId: " + dataId + ", analyteId: " + analyteId + ", type: " + type + " because type is not 'B' (Background)");
                    return;
                }

                // Query for existing raw data rows with the same dataId, analyteId, and type
                StatsService service = StatsService.get();
                User user = ctx.getUpgradeUser();
                ExpProtocol protocol = expRun.getProtocol();
                LuminexDataTable tableInfo = ((LuminexProtocolSchema)AssayService.get().getProvider(protocol).createProtocolSchema(user, expRun.getContainer(), protocol, null)).createDataTable(null, false);
                SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Data"), dataId);
                filter.addCondition(FieldKey.fromParts("Analyte"), analyteId);
                filter.addCondition(FieldKey.fromParts("Type"), type);

                // keep track of the set of wells for the given dataId/analyteId/type/standard combinations
                record WellGroupKey(long dataId, int analyteId, String type, Object standard) {}
                Map<WellGroupKey, List<LuminexDataRow>> rowsByWellGroup = new HashMap<>();
                for (Map<String, Object> databaseMap : new TableSelector(tableInfo, filter, null).getMapCollection())
                {
                    LuminexDataRow existingRow = BeanObjectFactory.Registry.getFactory(LuminexDataRow.class).fromMap(databaseMap);
                    existingRow._setExtraProperties(new CaseInsensitiveHashMap<>(databaseMap));

                    WellGroupKey groupKey = new WellGroupKey(
                            existingRow.getData(),
                            existingRow.getAnalyte(),
                            existingRow.getType(),
                            existingRow._getExtraProperties().get("Standard")
                    );
                    rowsByWellGroup.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(existingRow);
                }

                // calculate summary stats and well information for the new summary rows that we will insert into the database
                for (Map.Entry<WellGroupKey, List<LuminexDataRow>> wellGroupEntry : rowsByWellGroup.entrySet())
                {
                    WellGroupKey groupKey = wellGroupEntry.getKey();
                    LuminexDataRow newRow = new LuminexDataRow();
                    newRow.setSummary(true);
                    newRow.setData(groupKey.dataId);
                    newRow.setAnalyte(groupKey.analyteId);
                    newRow.setType(groupKey.type);

                    List<Double> fis = new ArrayList<>();
                    List<Double> fiBkgds = new ArrayList<>();
                    List<String> wells = new ArrayList<>();
                    for (LuminexDataRow existingRow : wellGroupEntry.getValue())
                    {
                        // keep track of well, FI, and FI Bkgd values from existing raw data rows to use in calculating summary stats for the new summary row
                        wells.add(existingRow.getWell());
                        if (existingRow.getFi() != null)
                            fis.add(existingRow.getFi());
                        if (existingRow.getFiBackground() != null)
                            fiBkgds.add(existingRow.getFiBackground());

                        // clone the following properties from the existing row to the newRow:
                        //      extraProperties, container, protocolid, description, wellrole, extraSpecimenInfo,
                        //      specimenID, participantID, visitID, date, dilution, tittration, singlepointcontrol
                        // note: don't clone rowid, beadcount, lsid
                        newRow._setExtraProperties(existingRow._getExtraProperties());
                        newRow.setWellRole(existingRow.getWellRole());
                        newRow.setContainer(existingRow.getContainer());
                        newRow.setProtocol(existingRow.getProtocol());
                        newRow.setDescription(existingRow.getDescription());
                        newRow.setSpecimenID(existingRow.getSpecimenID());
                        newRow.setParticipantID(existingRow.getParticipantID());
                        newRow.setVisitID(existingRow.getVisitID());
                        newRow.setDate(existingRow.getDate());
                        newRow.setExtraSpecimenInfo(existingRow.getExtraSpecimenInfo());
                        newRow.setDilution(existingRow.getDilution());
                        newRow.setTitration(existingRow.getTitration());
                        newRow.setSinglePointControl(existingRow.getSinglePointControl());

                        // we can clone stdev and cv from existing raw rows because LuminexDataHandler ensureSummaryStats() calculates them
                        newRow.setStdDev(existingRow.getStdDev());
                        newRow.setCv(existingRow.getCv());
                    }

                    // Calculate FI and FI-BKGD values for the new summary row based on the existing raw data rows with the same dataId, analyteId, type, and standard.
                    // similar to LuminexDataHandler ensureSummaryStats()
                    MathStat statsFi = service.getStats(ArrayUtils.toPrimitive(fis.toArray(new Double[0])));
                    newRow.setFi(Math.abs(statsFi.getMean()));
                    newRow.setFiString(newRow.getFi().toString());
                    MathStat statsFiBkgd = service.getStats(ArrayUtils.toPrimitive(fiBkgds.toArray(new Double[0])));
                    newRow.setFiBackground(Math.abs(statsFiBkgd.getMean()));
                    newRow.setFiBackgroundString(newRow.getFiBackground().toString());

                    // Calculate well to be a comma-separated list of wells from the existing raw data rows
                    newRow.setWell(String.join(",", wells));

                    // Generate an LSID for the new summary row
                    Lsid.LsidBuilder builder = new Lsid.LsidBuilder(LuminexAssayProvider.LUMINEX_DATA_ROW_LSID_PREFIX,"");
                    newRow.setLsid(builder.setObjectId(GUID.makeGUID()).toString());

                    // Insert the new summary row into the database.
                    // similar to LuminexDataHandler saveDataRows()
                    LuminexImportHelper helper = new LuminexImportHelper();
                    Map<String, Object> row = new CaseInsensitiveHashMap<>(newRow._getExtraProperties());
                    ObjectFactory<LuminexDataRow> f = ObjectFactory.Registry.getFactory(LuminexDataRow.class);
                    row.putAll(f.toMap(newRow, null));
                    try
                    {
                        OntologyManager.insertTabDelimited(tableInfo, expRun.getContainer(), user, helper, MapDataIterator.of(List.of(row)).getDataIterator(new DataIteratorContext()), true, _log, null);
                        String comment = "Inserted missing summary row for Luminex runId: " + runid + ", dataId: " + dataId + ", analyteId: " + analyteId + ", type: " + type + ", standard: " + groupKey.standard;
                        ExperimentService.get().auditRunEvent(user, protocol, expRun, null, "LuminexUpgradeCode.checkForMissingSummaryRows: " + comment, null);
                        _log.info("..." + comment);
                    }
                    catch (BatchValidationException e)
                    {
                        _log.warn("...failed to insert missing summary row for Luminex dataId: " + dataId + ", analyteId: " + analyteId + ", type: " + type + ", standard: " + groupKey.standard, e);
                    }
                }
            });

            tx.commit();
        }
    }
}