/*
 * Copyright (c) 2011-2016 LabKey Corporation
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
package org.labkey.flow.analysis.web;

import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.util.Pair;
import org.labkey.flow.analysis.model.AndGate;
import org.labkey.flow.analysis.model.Gate;
import org.labkey.flow.analysis.model.OrGate;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.SubsetRef;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: kevink
 * Date: 9/6/11
 */
public class SubsetTests extends Assert
{
    @Test
    public void testParseSave11_1()
    {
        final String[] statitics11_1 = {
                "-:Count",
                "-:Freq_Of_Parent",
                "Count",
                "Beads/-:Count",
                "Beads/-:Freq_Of_Parent",
                "Beads/MIP-1B PerCP Cy55+:Count",
                "Beads/MIP-1B PerCP Cy55+:Freq_Of_Parent",
                "Beads:Count",
                "Beads:Freq_Of_Parent",
                "FSC-A, SSC-A subset:Count",
                "FSC-A, SSC-A subset:Freq_Of_Parent",
                "HLA-DR Pac Blue:Count",
                "HLA-DR Pac Blue:Freq_Of_Parent",
                "L/-:Count",
                "L/-:Freq_Of_Parent",
                "L/APC CD3+:Count",
                "L/APC CD3+:Freq_Of_Parent",
                "S/-:Count",
                "S/-:Freq_Of_Parent",
                "S/HLA-DR+ Pac Blue:Count",
                "S/HLA-DR+ Pac Blue:Freq_Of_Parent",
                "S/L/+:Count",
                "S/L/+:Freq_Of_Parent",
                "S/L/-:Count",
                "S/L/-:Freq_Of_Parent",
                "S/L/3+/4+/4+ Live:Count",
                "S/L/3+:Count",
                "S/L/3+:Freq_Of_Parent",
                "S/L/3-/3- Live:Count",
                "S/L/3-/3- Live:Freq_Of_Parent",
                "S/L/3-:Count",
                "S/L/3-:Freq_Of_Parent",
                "S/L/<Alexa 680-A>, <APC-A> subset:Count",
                "S/L/<Alexa 680-A>, <APC-A> subset:Freq_Of_Parent",
                "S/L/<PerCP Cy55 Blue-A>, <PE Cy5-A> subset:Count",
                "S/L/<PerCP Cy55 Blue-A>, <PE Cy5-A> subset:Freq_Of_Parent",
                "S/L/APC-Cy7 HLA-DR+:Freq_Of_Parent",
                "S/L/APC-H7 CD4+:Count",
                "S/L/APC-H7 CD4+:Freq_Of_Parent",
                "S/L/AViD (new)+:Count",
                "S/L/AViD (new)+:Freq_Of_Parent",
                "S/L/AViD+:Count",
                "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&!CD57+&D103+):Count",
                "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&!CD57+&D103+):Freq_Of_Parent",
                "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&CD57+&!D103+):Count",
                "S/Lv/L/CD3+/CD4+/(45RO+&CCR5+&CCR7+&CD27+&CD28+&CD57+&!D103+):Freq_Of_Parent",
                "S/Lv/L/CD3+/CD4+/CD38+:Freq_Of_Parent",
                "S/Lv/L/CD3+/CD4+/CD57+:Count",
                "S/Lv/L/CD3+/CD4+/CD57+:Freq_Of_Parent",
                "S/Lv/L/CD3+/CD8+/(!45RO+&CCR5+&!CCR7+&CD27+&CD28+&!CD57+&D103+):Count",
                "S/Lv/L/CD3+/CD8+/(!45RO+&CCR5+&!CCR7+&CD27+&CD28+&!CD57+&D103+):Freq_Of_Parent",
                "S/Lv/L:Freq_Of_Parent",
                "S/Lv/L:Mean(Time)",
                "S/Lv/L:Median(<APC-A>)",
                "S/Lv/L:Median(Pacific Blue-A)",
                "S/Lv/L:Median(SSC-A)",
                "S/Lv/L:Median(Time)",
                "S/Lv:Count",
                "S/Lv:Freq_Of_Parent",
                "S/Lv:Mean(Time)",
                "S/Lv:Median(<Pacific Blue-A>)",
                "S/Lv:Median(APC-A)",
                "S/Lv/L/3+/4+/(!IFNg+&IL2+&Perforin+&!TNFa+):Count",
                "S/Lv/L/3+/4+/(!IFNg+&IL2+&Perforin+&!TNFa+):Freq_Of_Parent",
                "S/Lv/L/3+/8+/(!IFNg+&!IL2+&TNFa+&!Granzyme B+&57+):Freq_Of_Parent",
                "S/Lv/L/3+/8+/(!IFNg+&!IL2+&TNFa+&Granzyme B+&!57+):Count",
                "S:Median(Pacific Blue-A)",
                "S:Median(SSC-A)",
                "S:Median(Time)",
                "SSC-A, FSC-A subset:Count",
                "SSC-A, FSC-A subset:Freq_Of_Parent",
                "Spill(APC Cy7-A:APC Cy7-A)",
                "Std_Dev(APC Cy7-A)",
                "Std_Dev(APC-A)",
                "Std_Dev(Time)",
                "comp:Count",
                "comp:Median(PE-A)",
                "comp:Median(Pacific Blue-A)",
                "comp:Median(PerCP Cy55 Blue-A)",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q10: CD159a (NKG2a)+, HLA Dr+:Count",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q11: CD159a (NKG2a)+, HLA Dr-:Count",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q12: CD159a (NKG2a)-, HLA Dr-:Count",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q5: CD16-, CD335 (NKp46)+:Count",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q6: CD16+, CD335 (NKp46)+:Count",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q7: CD16+, CD335 (NKp46)-:Freq_Of_Parent",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q8: CD16-, CD335 (NKp46)-:Freq_Of_Parent",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q9: CD159a (NKG2a)-, HLA Dr+:Freq_Of_Parent",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/Q9: CD159a, HLA Dr+:Freq_Of_Parent",
                "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/CD8+159a+/PD-1 & 95 +:Freq_Of_Parent"
        };

        assertStatistics(initPopulation(), statitics11_1);
    }

    @Test
    public void testParseSave11_2()
    {
        List<Pair<String, String>> statistics11_2 = Arrays.asList(
                Pair.of("S/L/APC-H7 CD4+ and AViD (new):Count",
                        "S/L/(APC-H7 CD4+&{AViD (new)+})"),

                Pair.of("S/L/(APC-H7 CD4+&{AViD (new)+}):Count",
                        "S/L/(APC-H7 CD4+&{AViD (new)+})"),

                // NOTE: For 11.1 compat, we only emit the last part of subsets within expressions.  See CleanNameExpressionTransform.
                Pair.of("CD45+/LYMPHS/CD3- & 20-/14- (DR)-/(CD8+159a+/Q9: CD159a, HLA Dr+&CD8+159a+/{PD-1 & 95 +}):Count",
                        "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/(Q9: CD159a; HLA Dr+&{PD-1 & 95 +})"),

                // NOTE: For 11.1 compat, we only emit the last part of subsets within expressions.  See CleanNameExpressionTransform.
                Pair.of("CD45+/LYMPHS/CD3- & 20-/14- (DR)-/Q9 & PD-1:Count",
                        "CD45+/LYMPHS/CD3- & 20-/14- (DR)-/(Q9: CD159a; HLA Dr+&{PD-1 & 95 +})")
        );

        assertStatistics(initPopulation(), statistics11_2);
    }

    @Test
    public void testParseSaveMultipleBooleans()
    {
        List<Pair<String, String>> booleans = Arrays.asList(
                Pair.of("booleans/one and two and three:Count",
                        "booleans/(one&two&three)"),

                Pair.of("booleans/one or two or three:Count",
                        "booleans/(one|two|three)")
        );

        assertStatistics(initPopulation(), booleans);
    }

    private void assertStatistics(Population population, Collection<Pair<String, String>> stats)
    {
        for (Pair<String, String> stat : stats)
        {
            assertStatistic(population, stat.first, stat.second);
        }
    }

    private void assertStatistics(Population population, String[] stats)
    {
        for (String stat : stats)
        {
            String expectedAlias = null;

            // Create an expected alias for non-root level statistic specs
            if (!(stat.startsWith("Count") || stat.startsWith("Std_Dev") || stat.startsWith("Median") || stat.startsWith("Spill")))
            {
                // Get the subset part of the stat and replace ',' with ';'
                int lastColon = stat.lastIndexOf(":");
                expectedAlias = stat.substring(0, lastColon).replaceAll(",", ";");
            }

            assertStatistic(population, stat, expectedAlias);
        }
    }

    private void assertStatistic(Population population, String stat, String expectedAlias)
    {
        StatisticSpec spec = new StatisticSpec(stat);
        assertEquals(stat, spec.toString());

        if (expectedAlias != null && spec.getSubset() != null)
        {
            SubsetSpec alias = FCSAnalyzer.get()._getSubsetAlias(population, spec.getSubset());
            assertNotNull("Failed to create alias for subset '" + spec.getSubset() + "'", alias);
            assertEquals(expectedAlias, alias.toString());
        }
    }

    private Population initPopulation()
    {
        return p(null,
                p("-"),
                p("Beads", p("-"), p("MIP-1B PerCP Cy55+")),
                p("FSC-A, SSC-A subset"),
                p("HLA-DR Pac Blue"),
                p("L", p("-"), p("APC CD3+")),
                p("S",
                        p("-"),
                        p("HLA-DR+ Pac Blue"),
                        p("L",
                                p("+"),
                                p("-"),
                                p("3+", p("4+", p("4+ Live"))),
                                p("3-", p("3- Live")),
                                p("<Alexa 680-A>, <APC-A> subset"),
                                p("<PerCP Cy55 Blue-A>, <PE Cy5-A> subset"),
                                p("APC-Cy7 HLA-DR+"),
                                p("APC-H7 CD4+"),
                                p("AViD (new)+"),
                                p("AViD+"),
                                p("APC-H7 CD4+ and AViD (new)",
                                        and("APC-H7 CD4+", "AViD (new)+"))),
                        p("Lv",
                                p("L",
                                        p("CD3+",
                                                p("CD4+",
                                                        p("45RO+"),
                                                        p("CCR5+"),
                                                        p("CCR7+"),
                                                        p("CD27+"),
                                                        p("CD28+"),
                                                        p("D103+"),
                                                        p("CD38+"),
                                                        p("CD57+")),
                                                p("CD8+",
                                                        p("45RO+"),
                                                        p("CCR5+"),
                                                        p("CCR7+"),
                                                        p("CD27+"),
                                                        p("CD28+"),
                                                        p("D103+"),
                                                        p("CD38+"),
                                                        p("CD57+"))),
                                        p("3+",
                                                p("4+",
                                                        p("IFNg+"),
                                                        p("IL2+"),
                                                        p("Perforin"),
                                                        p("TNFa+"),
                                                        p("Granzyme B+"),
                                                        p("57+")),
                                                p("8+",
                                                        p("IFNg+"),
                                                        p("IL2+"),
                                                        p("Perforin"),
                                                        p("TNFa+"),
                                                        p("Granzyme B+"),
                                                        p("57+")))))),
                p("SSC-A, FSC-A subset"),
                p("Singlets"),
                p("booleans",
                        p("one and two and three",
                                and("one", "two", "three")),
                        p("one or two or three",
                                or("one", "two", "three"))),
                p("comp"),
                p("CD45+",
                        p("LYMPHS",
                                p("CD3- & 20-",
                                        p("14- (DR)-",
                                                p("CD8+159a+",
                                                        p("Q10: CD159a (NKG2a)+, HLA Dr+"),
                                                        p("Q11: CD159a (NKG2a)+, HLA Dr-"),
                                                        p("Q12: CD159a (NKG2a)-, HLA Dr-"),
                                                        p("Q5: CD16-, CD335 (NKp46)+"),
                                                        p("Q6: CD16+, CD335 (NKp46)+"),
                                                        p("Q7: CD16+, CD335 (NKp46)-"),
                                                        p("Q8: CD16-, CD335 (NKp46)-"),
                                                        p("Q9: CD159a (NKG2a)-, HLA Dr+"),
                                                        p("Q9: CD159a, HLA Dr+"),
                                                        p("PD-1 & 95 +")),
                                                p("Q9 & PD-1",
                                                        and("CD8+159a+/Q9: CD159a, HLA Dr+", "CD8+159a+/PD-1 & 95 +")))))));


    }

    private Population p(String name, Object... children)
    {
        Population p = new Population();
        p.setName(PopulationName.fromString(name));

        for (Object child : children)
        {
            if (child instanceof Population)
            {
                p.addPopulation((Population) child);
            }
            else if (child instanceof Gate)
            {
                p.addGate((Gate) child);
            }
        }
        return p;
    }

    private Gate and(String... names)
    {
        AndGate gate = new AndGate();

        for (String name : names)
        {
            SubsetSpec subset = SubsetSpec.fromEscapedString(name);
            SubsetRef ref = new SubsetRef(subset);
            gate.getGates().add(ref);
        }

        return gate;
    }

    private Gate or(String... names)
    {
        OrGate gate = new OrGate();

        for (String name : names)
        {
            SubsetSpec subset = SubsetSpec.fromEscapedString(name);
            SubsetRef ref = new SubsetRef(subset);
            gate.getGates().add(ref);
        }

        return gate;
    }
}
