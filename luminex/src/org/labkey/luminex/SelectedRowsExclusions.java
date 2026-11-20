/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the well and replicate group exclusion commands for a set of selected results grid rows.
 */
public class SelectedRowsExclusions
{
    public static final String INSERT = "insert";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";

    /** A replicate group is the set of wells sharing a data file, description and type. */
    public record ReplicateGroup(long dataId, @Nullable String description, @NotNull String type)
    {
    }

    /** One selected luminex.DataRow. A summary row's well lists every well in its replicate group, e.g. "A1,B1". */
    public record SelectedRow(long runId, @NotNull ReplicateGroup group, @NotNull String well, boolean summary, long analyteId, @NotNull String analyteName)
    {
    }

    /** An existing well (well != null) or replicate group (well == null) exclusion. */
    public record ExistingExclusion(int rowId, @NotNull ReplicateGroup group, @Nullable String well, @NotNull Map<Long, String> analytes)
    {
    }

    /**
     * A proposed insert, update or delete. analytes is the exclusion's resulting analyte set; changedAnalytes is what
     * the selection adds to or removes from it.
     */
    public record ProposedExclusion(long runId, @NotNull String command, @Nullable Integer key, @NotNull ReplicateGroup group,
                                    @Nullable String well, @NotNull Map<Long, String> analytes, @NotNull Map<Long, String> changedAnalytes)
    {
        public LuminexSingleExclusionCommand toCommand()
        {
            LuminexSingleExclusionCommand command = new LuminexSingleExclusionCommand();
            command.setCommand(command());
            command.setKey(key());
            command.setDataId(group().dataId());
            command.setDescription(group().description());
            command.setType(group().type());
            command.setWell(well());
            if (!DELETE.equals(command()))
            {
                command.setAnalyteRowIds(analytes().keySet().stream().map(String::valueOf).collect(Collectors.joining(",")));
                command.setAnalyteNames(String.join(",", analytes().values()));
            }
            return command;
        }
    }

    public record Result(@NotNull List<ProposedExclusion> exclusions, int skippedRowCount)
    {
    }

    private static final Comparator<ProposedExclusion> ORDER = Comparator
            .comparingLong(ProposedExclusion::runId)
            // Issue 33266: deletes first, as in WellExclusionPanel, in case a batch ever pairs a delete with an insert on the same key
            .thenComparing(e -> !DELETE.equals(e.command()))
            .thenComparingLong(e -> e.group().dataId())
            .thenComparing(e -> e.group().description(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(e -> e.group().type())
            .thenComparing(ProposedExclusion::well, Comparator.nullsFirst(Comparator.naturalOrder()));

    /**
     * Each selected well gets its own exclusion, except summary rows, which can only be excluded with a replicate group
     * exclusion. An existing replicate group exclusion absorbs all of the group's selected analytes.
     *
     * @param existing existing well and replicate group exclusions for the selection's replicate groups
     */
    public static Result exclude(Collection<SelectedRow> rows, Collection<ExistingExclusion> existing)
    {
        Map<ReplicateGroup, Long> runIds = new LinkedHashMap<>();
        Map<ReplicateGroup, Map<Long, String>> summaryAnalytesByGroup = new HashMap<>();
        Map<ReplicateGroup, Map<String, Map<Long, String>>> wellAnalytesByGroup = new HashMap<>();
        for (SelectedRow row : rows)
        {
            runIds.put(row.group(), row.runId());
            if (row.summary())
                summaryAnalytesByGroup.computeIfAbsent(row.group(), _ -> new HashMap<>()).put(row.analyteId(), row.analyteName());
            else
                wellAnalytesByGroup.computeIfAbsent(row.group(), _ -> new HashMap<>())
                        .computeIfAbsent(row.well(), _ -> new HashMap<>())
                        .put(row.analyteId(), row.analyteName());
        }
        Map<ReplicateGroup, List<ExistingExclusion>> existingByGroup = existing.stream()
                .collect(Collectors.groupingBy(ExistingExclusion::group));

        List<ProposedExclusion> proposed = new ArrayList<>();
        runIds.forEach((group, runId) -> {
            Map<Long, String> summaryAnalytes = summaryAnalytesByGroup.getOrDefault(group, Map.of());
            Map<String, Map<Long, String>> selectedWells = wellAnalytesByGroup.getOrDefault(group, Map.of());
            List<ExistingExclusion> groupExisting = existingByGroup.getOrDefault(group, List.of());
            ExistingExclusion groupExclusion = groupExisting.stream().filter(e -> e.well() == null).findFirst().orElse(null);
            Map<String, ExistingExclusion> wellExclusions = groupExisting.stream()
                    .filter(e -> e.well() != null)
                    .collect(Collectors.toMap(ExistingExclusion::well, e -> e, (a, _) -> a));

            if (groupExclusion != null)
            {
                Map<Long, String> all = new HashMap<>(summaryAnalytes);
                selectedWells.values().forEach(all::putAll);
                addMerged(proposed, runId, groupExclusion, all);
                return;
            }

            if (!summaryAnalytes.isEmpty())
                addInsert(proposed, runId, group, null, summaryAnalytes);

            selectedWells.forEach((well, analytes) -> {
                // The new replicate group exclusion already covers these analytes for every well
                Map<Long, String> wellAnalytes = new HashMap<>(analytes);
                wellAnalytes.keySet().removeAll(summaryAnalytes.keySet());
                if (wellAnalytes.isEmpty())
                    return;

                ExistingExclusion wellExclusion = wellExclusions.get(well);
                if (wellExclusion != null)
                    addMerged(proposed, runId, wellExclusion, wellAnalytes);
                else
                    addInsert(proposed, runId, group, well, wellAnalytes);
            });
        });

        proposed.sort(ORDER);
        return new Result(proposed, 0);
    }

    /**
     * Removes each row's analyte from every well or replicate group exclusion covering it, deleting any left empty.
     * Rows with no covering exclusion count as skipped.
     *
     * @param existing existing well and replicate group exclusions for the selection's replicate groups
     */
    public static Result remove(Collection<SelectedRow> rows, Collection<ExistingExclusion> existing)
    {
        Map<ReplicateGroup, List<ExistingExclusion>> existingByGroup = existing.stream()
                .collect(Collectors.groupingBy(ExistingExclusion::group));
        Map<ExistingExclusion, Long> runIds = new HashMap<>();
        Map<ExistingExclusion, Map<Long, String>> removals = new LinkedHashMap<>();
        int skipped = 0;

        for (SelectedRow row : rows)
        {
            List<ExistingExclusion> covering = existingByGroup.getOrDefault(row.group(), List.of()).stream()
                    .filter(e -> e.well() == null || e.well().equals(row.well()))
                    .filter(e -> e.analytes().containsKey(row.analyteId()))
                    .toList();
            if (covering.isEmpty())
                skipped++;

            for (ExistingExclusion exclusion : covering)
            {
                runIds.put(exclusion, row.runId());
                removals.computeIfAbsent(exclusion, _ -> new HashMap<>()).put(row.analyteId(), row.analyteName());
            }
        }

        List<ProposedExclusion> proposed = new ArrayList<>();
        removals.forEach((exclusion, removed) -> {
            Map<Long, String> remaining = new HashMap<>(exclusion.analytes());
            remaining.keySet().removeAll(removed.keySet());
            String command = remaining.isEmpty() ? DELETE : UPDATE;
            proposed.add(new ProposedExclusion(runIds.get(exclusion), command, exclusion.rowId(), exclusion.group(),
                    exclusion.well(), sortByName(remaining), sortByName(removed)));
        });

        proposed.sort(ORDER);
        return new Result(proposed, skipped);
    }

    private static void addInsert(List<ProposedExclusion> proposed, long runId, ReplicateGroup group, @Nullable String well, Map<Long, String> analytes)
    {
        Map<Long, String> sorted = sortByName(analytes);
        proposed.add(new ProposedExclusion(runId, INSERT, null, group, well, sorted, sorted));
    }

    private static void addMerged(List<ProposedExclusion> proposed, long runId, ExistingExclusion exclusion, Map<Long, String> selected)
    {
        Map<Long, String> added = new HashMap<>(selected);
        added.keySet().removeAll(exclusion.analytes().keySet());
        if (added.isEmpty())
            return;

        Map<Long, String> merged = new HashMap<>(exclusion.analytes());
        merged.putAll(added);
        proposed.add(new ProposedExclusion(runId, UPDATE, exclusion.rowId(), exclusion.group(), exclusion.well(), sortByName(merged), sortByName(added)));
    }

    private static Map<Long, String> sortByName(Map<Long, String> analytes)
    {
        return analytes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, _) -> a, LinkedHashMap::new));
    }

    public static class TestCase extends Assert
    {
        private static final ReplicateGroup GROUP_X1 = new ReplicateGroup(10, "Sample 1", "X1");
        private static final ReplicateGroup GROUP_X2 = new ReplicateGroup(10, "Sample 2", "X2");
        private static final ReplicateGroup GROUP_RUN2 = new ReplicateGroup(20, "Sample 1", "X1");
        private static SelectedRow row(ReplicateGroup group, String well, long analyteId)
        {
            long runId = group == GROUP_RUN2 ? 2 : 1;
            return new SelectedRow(runId, group, well, well.contains(","), analyteId, "ENV" + analyteId);
        }

        private static Map<Long, String> analytes(long... ids)
        {
            Map<Long, String> map = new HashMap<>();
            for (long id : ids)
                map.put(id, "ENV" + id);
            return sortByName(map);
        }

        private static void assertExclusion(ProposedExclusion actual, long runId, String command, @Nullable Integer key,
                                            ReplicateGroup group, @Nullable String well, Map<Long, String> analytes, Map<Long, String> changed)
        {
            assertEquals(runId, actual.runId());
            assertEquals(command, actual.command());
            assertEquals(key, actual.key());
            assertEquals(group, actual.group());
            assertEquals(well, actual.well());
            assertEquals(List.copyOf(analytes.keySet()), List.copyOf(actual.analytes().keySet()));
            assertEquals(List.copyOf(changed.keySet()), List.copyOf(actual.changedAnalytes().keySet()));
        }

        @Test
        public void testWellExclusions()
        {
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1), row(GROUP_X1, "A1", 2), row(GROUP_X1, "B1", 1)), List.of());
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, INSERT, null, GROUP_X1, "A1", analytes(1, 2), analytes(1, 2));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X1, "B1", analytes(1), analytes(1));
        }

        @Test
        public void testSummaryRows()
        {
            Result result = exclude(List.of(row(GROUP_X1, "A1,B1", 1), row(GROUP_X1, "A1", 1), row(GROUP_X1, "A1", 2)), List.of());
            assertEquals("Raw rows already covered by the summary row's replicate group exclusion need no well exclusion", 2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, INSERT, null, GROUP_X1, null, analytes(1), analytes(1));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X1, "A1", analytes(2), analytes(2));
        }

        @Test
        public void testMergeIntoGroupExclusion()
        {
            ExistingExclusion groupExclusion = new ExistingExclusion(100, GROUP_X1, null, analytes(1));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 2)), List.of(groupExclusion));
            assertEquals(1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 100, GROUP_X1, null, analytes(1, 2), analytes(2));

            result = exclude(List.of(row(GROUP_X1, "A1", 1)), List.of(groupExclusion));
            assertTrue("Already excluded analytes need no change", result.exclusions().isEmpty());
        }

        @Test
        public void testSummaryRowMergesIntoGroupExclusion()
        {
            ExistingExclusion groupExclusion = new ExistingExclusion(100, GROUP_X1, null, analytes(1));
            Result result = exclude(List.of(row(GROUP_X1, "A1,B1", 2)), List.of(groupExclusion));
            assertEquals(1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 100, GROUP_X1, null, analytes(1, 2), analytes(2));
        }

        @Test
        public void testGroupExclusionAbsorbsWellAnalytes()
        {
            ExistingExclusion groupExclusion = new ExistingExclusion(100, GROUP_X1, null, analytes(1));
            ExistingExclusion wellExclusion = new ExistingExclusion(101, GROUP_X1, "A1", analytes(3));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 2)), List.of(groupExclusion, wellExclusion));
            assertEquals("The existing well exclusion is left as is", 1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 100, GROUP_X1, null, analytes(1, 2), analytes(2));
        }

        @Test
        public void testAlreadyExcludedWell()
        {
            ExistingExclusion wellExclusion = new ExistingExclusion(101, GROUP_X1, "A1", analytes(1, 2));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1)), List.of(wellExclusion));
            assertTrue(result.exclusions().isEmpty());
        }

        @Test
        public void testExistingWellExclusionsStayPerWell()
        {
            ExistingExclusion wellExclusion = new ExistingExclusion(101, GROUP_X1, "A1", analytes(3));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1), row(GROUP_X1, "B1", 1)), List.of(wellExclusion));
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, UPDATE, 101, GROUP_X1, "A1", analytes(1, 3), analytes(1));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X1, "B1", analytes(1), analytes(1));
        }

        @Test
        public void testMultipleGroupsAndRuns()
        {
            Result result = exclude(List.of(row(GROUP_RUN2, "A1", 1), row(GROUP_X2, "A2", 1), row(GROUP_X1, "A1", 1)), List.of());
            assertEquals(3, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, INSERT, null, GROUP_X1, "A1", analytes(1), analytes(1));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X2, "A2", analytes(1), analytes(1));
            assertExclusion(result.exclusions().get(2), 2, INSERT, null, GROUP_RUN2, "A1", analytes(1), analytes(1));
        }

        @Test
        public void testRemove()
        {
            ExistingExclusion wellA1 = new ExistingExclusion(101, GROUP_X1, "A1", analytes(1, 2));
            ExistingExclusion wellB1 = new ExistingExclusion(102, GROUP_X1, "B1", analytes(1));
            ExistingExclusion groupX2 = new ExistingExclusion(103, GROUP_X2, null, analytes(1, 2));

            Result result = remove(List.of(
                    row(GROUP_X1, "A1", 1),     // A1 keeps analyte 2
                    row(GROUP_X1, "B1", 1),     // B1 has none left
                    row(GROUP_X2, "B2", 2),     // removed from the whole replicate group
                    row(GROUP_X1, "A1", 3),     // analyte not excluded
                    row(GROUP_RUN2, "A1", 1)),  // no exclusion
                    List.of(wellA1, wellB1, groupX2));

            assertEquals(2, result.skippedRowCount());
            assertEquals(3, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, DELETE, 102, GROUP_X1, "B1", analytes(), analytes(1));
            assertExclusion(result.exclusions().get(1), 1, UPDATE, 101, GROUP_X1, "A1", analytes(2), analytes(1));
            assertExclusion(result.exclusions().get(2), 1, UPDATE, 103, GROUP_X2, null, analytes(1), analytes(2));
        }

        @Test
        public void testRemoveFromWellAndGroupExclusion()
        {
            ExistingExclusion wellA1 = new ExistingExclusion(101, GROUP_X1, "A1", analytes(1));
            ExistingExclusion groupX1 = new ExistingExclusion(104, GROUP_X1, null, analytes(1, 2));
            Result result = remove(List.of(row(GROUP_X1, "A1", 1)), List.of(wellA1, groupX1));

            assertEquals(0, result.skippedRowCount());
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, DELETE, 101, GROUP_X1, "A1", analytes(), analytes(1));
            assertExclusion(result.exclusions().get(1), 1, UPDATE, 104, GROUP_X1, null, analytes(2), analytes(1));
        }

        @Test
        public void testRemoveLastAnalyteFromGroupExclusion()
        {
            ExistingExclusion groupX1 = new ExistingExclusion(104, GROUP_X1, null, analytes(1));
            Result result = remove(List.of(row(GROUP_X1, "B1", 1)), List.of(groupX1));
            assertEquals(1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, DELETE, 104, GROUP_X1, null, analytes(), analytes(1));
        }

        @Test
        public void testRemoveRowsSharingGroupExclusion()
        {
            ExistingExclusion groupX1 = new ExistingExclusion(104, GROUP_X1, null, analytes(1, 2, 3));
            Result result = remove(List.of(row(GROUP_X1, "A1", 1), row(GROUP_X1, "B1", 2)), List.of(groupX1));
            assertEquals(1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 104, GROUP_X1, null, analytes(3), analytes(1, 2));
        }

        @Test
        public void testRemoveSummaryRowWithOnlyWellExclusions()
        {
            ExistingExclusion wellA1 = new ExistingExclusion(101, GROUP_X1, "A1", analytes(1));
            ExistingExclusion wellB1 = new ExistingExclusion(102, GROUP_X1, "B1", analytes(1));
            Result result = remove(List.of(row(GROUP_X1, "A1,B1", 1)), List.of(wellA1, wellB1));
            assertEquals("Single-well exclusions don't apply to summary rows", 1, result.skippedRowCount());
            assertTrue(result.exclusions().isEmpty());
        }

        @Test
        public void testRemoveSummaryRow()
        {
            ExistingExclusion groupX1 = new ExistingExclusion(104, GROUP_X1, null, analytes(1, 2));
            Result result = remove(List.of(row(GROUP_X1, "A1,B1", 1)), List.of(groupX1));

            assertEquals(0, result.skippedRowCount());
            assertEquals(1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 104, GROUP_X1, null, analytes(2), analytes(1));
        }

        @Test
        public void testToCommand()
        {
            LuminexSingleExclusionCommand command = new ProposedExclusion(1, INSERT, null, GROUP_X1, "A1", analytes(2, 1), Map.of()).toCommand();
            assertEquals(INSERT, command.getCommand());
            assertEquals(Long.valueOf(10), command.getDataId());
            assertEquals("Sample 1", command.getDescription());
            assertEquals("X1", command.getType());
            assertEquals("A1", command.getWell());
            assertEquals("1,2", command.getAnalyteRowIds());
            assertEquals("ENV1,ENV2", command.getAnalyteNames());
            assertEquals(LuminexManager.ExclusionType.WellExclusion, command.getExclusionType());
            assertNull(command.getKey());

            command = new ProposedExclusion(1, DELETE, 102, GROUP_X1, "B1", analytes(), analytes(1)).toCommand();
            assertEquals(DELETE, command.getCommand());
            assertEquals(Integer.valueOf(102), command.getKey());
            assertEquals("B1", command.getWell());
            assertEquals("", command.getAnalyteRowIds());
            assertEquals("", command.getAnalyteNames());
        }
    }
}
