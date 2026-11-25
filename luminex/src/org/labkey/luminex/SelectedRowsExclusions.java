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
import java.util.Set;
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
    public record ExistingExclusion(int rowId, @NotNull ReplicateGroup group, @Nullable String well, @NotNull Map<Long, String> analytes, @Nullable String comment)
    {
        public ExistingExclusion(int rowId, @NotNull ReplicateGroup group, @Nullable String well, @NotNull Map<Long, String> analytes)
        {
            this(rowId, group, well, analytes, null);
        }
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
     * Each selected well gets its own exclusion, except in a replicate group with summary rows, which can only be
     * excluded as a whole, as in WellExclusionPanel. An existing replicate group exclusion absorbs all of the group's
     * selected analytes.
     *
     * @param existing existing well and replicate group exclusions for the selection's replicate groups
     * @param summaryGroups the selection's replicate groups that have summary rows, selected or not
     */
    public static Result exclude(Collection<SelectedRow> rows, Collection<ExistingExclusion> existing, Set<ReplicateGroup> summaryGroups)
    {
        Map<ReplicateGroup, List<ExistingExclusion>> existingByGroup = existing.stream()
                .collect(Collectors.groupingBy(ExistingExclusion::group));

        List<ProposedExclusion> proposed = new ArrayList<>();
        rows.stream().collect(Collectors.groupingBy(SelectedRow::group)).forEach((group, groupRows) -> {
            long runId = groupRows.getFirst().runId();
            List<ExistingExclusion> groupExisting = existingByGroup.getOrDefault(group, List.of());
            ExistingExclusion groupExclusion = groupExisting.stream().filter(e -> e.well() == null).findFirst().orElse(null);

            if (groupExclusion != null)
                addMerged(proposed, runId, groupExclusion, analytesOf(groupRows));
            else if (summaryGroups.contains(group) || groupRows.stream().anyMatch(SelectedRow::summary))
                addInsert(proposed, runId, group, null, analytesOf(groupRows));
            else
            {
                Map<String, ExistingExclusion> wellExclusions = groupExisting.stream()
                        .filter(e -> e.well() != null)
                        .collect(Collectors.toMap(ExistingExclusion::well, e -> e, (a, _) -> a));
                groupRows.stream().collect(Collectors.groupingBy(SelectedRow::well)).forEach((well, wellRows) -> {
                    ExistingExclusion wellExclusion = wellExclusions.get(well);
                    if (wellExclusion != null)
                        addMerged(proposed, runId, wellExclusion, analytesOf(wellRows));
                    else
                        addInsert(proposed, runId, group, well, analytesOf(wellRows));
                });
            }
        });

        proposed.sort(ORDER);
        return new Result(proposed, 0);
    }

    private record Removal(ExistingExclusion exclusion, long runId, Map<Long, String> removed)
    {
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
        Map<Integer, Removal> removals = new HashMap<>();
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
                removals.computeIfAbsent(exclusion.rowId(), _ -> new Removal(exclusion, row.runId(), new HashMap<>()))
                        .removed().put(row.analyteId(), row.analyteName());
        }

        List<ProposedExclusion> proposed = new ArrayList<>();
        removals.values().forEach(removal -> {
            ExistingExclusion exclusion = removal.exclusion();
            Map<Long, String> remaining = new HashMap<>(exclusion.analytes());
            remaining.keySet().removeAll(removal.removed().keySet());
            String command = remaining.isEmpty() ? DELETE : UPDATE;
            proposed.add(new ProposedExclusion(removal.runId(), command, exclusion.rowId(), exclusion.group(),
                    exclusion.well(), sortByName(remaining), sortByName(removal.removed())));
        });

        proposed.sort(ORDER);
        return new Result(proposed, skipped);
    }

    private static Map<Long, String> analytesOf(List<SelectedRow> rows)
    {
        return rows.stream().collect(Collectors.toMap(SelectedRow::analyteId, SelectedRow::analyteName, (a, _) -> a));
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
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1), row(GROUP_X1, "A1", 2), row(GROUP_X1, "B1", 1)), List.of(), Set.of());
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, INSERT, null, GROUP_X1, "A1", analytes(1, 2), analytes(1, 2));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X1, "B1", analytes(1), analytes(1));
        }

        @Test
        public void testSummaryRows()
        {
            Result result = exclude(List.of(row(GROUP_X1, "A1,B1", 1), row(GROUP_X1, "A1", 1), row(GROUP_X1, "A1", 2)), List.of(), Set.of());
            assertEquals("A selected summary row puts the group's raw rows in its replicate group exclusion", 1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, INSERT, null, GROUP_X1, null, analytes(1, 2), analytes(1, 2));
        }

        @Test
        public void testRawRowsInSummaryGroup()
        {
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1), row(GROUP_X1, "B1", 2), row(GROUP_X2, "A2", 1)), List.of(), Set.of(GROUP_X1));
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, INSERT, null, GROUP_X1, null, analytes(1, 2), analytes(1, 2));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X2, "A2", analytes(1), analytes(1));
        }

        @Test
        public void testSummaryGroupLeavesWellExclusions()
        {
            ExistingExclusion wellExclusion = new ExistingExclusion(101, GROUP_X1, "A1", analytes(3));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1)), List.of(wellExclusion), Set.of(GROUP_X1));
            assertEquals("The existing well exclusion is left as is", 1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, INSERT, null, GROUP_X1, null, analytes(1), analytes(1));
        }

        @Test
        public void testRawRowsInSummaryGroupMergeIntoGroupExclusion()
        {
            ExistingExclusion groupExclusion = new ExistingExclusion(100, GROUP_X1, null, analytes(1));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 2), row(GROUP_X1, "B1", 3)), List.of(groupExclusion), Set.of(GROUP_X1));
            assertEquals("The existing replicate group exclusion is updated, not joined by a second one", 1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 100, GROUP_X1, null, analytes(1, 2, 3), analytes(2, 3));
        }

        @Test
        public void testNullDescription()
        {
            ReplicateGroup group = new ReplicateGroup(10, null, "X3");
            ExistingExclusion wellExclusion = new ExistingExclusion(101, new ReplicateGroup(10, null, "X3"), "A3", analytes(1));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1), row(group, "A3", 2)), List.of(wellExclusion), Set.of());
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, UPDATE, 101, group, "A3", analytes(1, 2), analytes(2));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X1, "A1", analytes(1), analytes(1));
        }

        @Test
        public void testMergeIntoGroupExclusion()
        {
            ExistingExclusion groupExclusion = new ExistingExclusion(100, GROUP_X1, null, analytes(1));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 2)), List.of(groupExclusion), Set.of());
            assertEquals(1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 100, GROUP_X1, null, analytes(1, 2), analytes(2));

            result = exclude(List.of(row(GROUP_X1, "A1", 1)), List.of(groupExclusion), Set.of());
            assertTrue("Already excluded analytes need no change", result.exclusions().isEmpty());
        }

        @Test
        public void testSummaryRowMergesIntoGroupExclusion()
        {
            ExistingExclusion groupExclusion = new ExistingExclusion(100, GROUP_X1, null, analytes(1));
            Result result = exclude(List.of(row(GROUP_X1, "A1,B1", 2)), List.of(groupExclusion), Set.of());
            assertEquals(1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 100, GROUP_X1, null, analytes(1, 2), analytes(2));
        }

        @Test
        public void testGroupExclusionAbsorbsWellAnalytes()
        {
            ExistingExclusion groupExclusion = new ExistingExclusion(100, GROUP_X1, null, analytes(1));
            ExistingExclusion wellExclusion = new ExistingExclusion(101, GROUP_X1, "A1", analytes(3));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 2)), List.of(groupExclusion, wellExclusion), Set.of());
            assertEquals("The existing well exclusion is left as is", 1, result.exclusions().size());
            assertExclusion(result.exclusions().getFirst(), 1, UPDATE, 100, GROUP_X1, null, analytes(1, 2), analytes(2));
        }

        @Test
        public void testAlreadyExcludedWell()
        {
            ExistingExclusion wellExclusion = new ExistingExclusion(101, GROUP_X1, "A1", analytes(1, 2));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1)), List.of(wellExclusion), Set.of());
            assertTrue(result.exclusions().isEmpty());
        }

        @Test
        public void testExistingWellExclusionsStayPerWell()
        {
            ExistingExclusion wellExclusion = new ExistingExclusion(101, GROUP_X1, "A1", analytes(3));
            Result result = exclude(List.of(row(GROUP_X1, "A1", 1), row(GROUP_X1, "B1", 1)), List.of(wellExclusion), Set.of());
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, UPDATE, 101, GROUP_X1, "A1", analytes(1, 3), analytes(1));
            assertExclusion(result.exclusions().get(1), 1, INSERT, null, GROUP_X1, "B1", analytes(1), analytes(1));
        }

        @Test
        public void testMultipleGroupsAndRuns()
        {
            Result result = exclude(List.of(row(GROUP_RUN2, "A1", 1), row(GROUP_X2, "A2", 1), row(GROUP_X1, "A1", 1)), List.of(), Set.of());
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
        public void testRemoveAcrossRuns()
        {
            ExistingExclusion run1Group = new ExistingExclusion(104, GROUP_X1, null, analytes(1, 2));
            ExistingExclusion run2Well = new ExistingExclusion(105, GROUP_RUN2, "A1", analytes(1));
            Result result = remove(List.of(row(GROUP_RUN2, "A1", 1), row(GROUP_X1, "B1", 1)), List.of(run1Group, run2Well));

            assertEquals(0, result.skippedRowCount());
            assertEquals(2, result.exclusions().size());
            assertExclusion(result.exclusions().get(0), 1, UPDATE, 104, GROUP_X1, null, analytes(2), analytes(1));
            assertExclusion(result.exclusions().get(1), 2, DELETE, 105, GROUP_RUN2, "A1", analytes(), analytes(1));
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
