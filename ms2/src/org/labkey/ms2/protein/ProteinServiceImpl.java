/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.view.JspView;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.AnnotationView;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.fasta.Protein;
import org.labkey.ms2.protein.organism.GuessOrgByParsing;
import org.labkey.ms2.protein.organism.GuessOrgBySharedHash;
import org.labkey.ms2.protein.organism.GuessOrgBySharedIdents;
import org.labkey.ms2.protein.organism.OrganismGuessStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * User: jeckels
 * Date: May 3, 2012
 */
public class ProteinServiceImpl implements ProteinService
{
    private List<OrganismGuessStrategy> _strategies;
    private List<QueryViewProvider<ProteinSearchForm>> _proteinSearchViewProviders = new CopyOnWriteArrayList<>();
    private List<QueryViewProvider<PeptideSearchForm>> _peptideSearchViewProviders = new CopyOnWriteArrayList<>();

    public ProteinServiceImpl()
    {
    }

    private synchronized List<OrganismGuessStrategy> getStrategies()
    {
        // Populate lazily since some implementations need access to DbSchemas, etc
        if (_strategies == null)
        {
            _strategies = new ArrayList<>();
            _strategies.add(new GuessOrgByParsing());
            _strategies.add(new GuessOrgBySharedHash());
            _strategies.add(new GuessOrgBySharedIdents());
        }
        return _strategies;
    }

    public int ensureProtein(String sequence, String organism, String name, String description)
    {
        organism = guessOrganism(sequence, organism, name, description);

        return ProteinManager.ensureProtein(sequence, organism, name, description);
    }

    public int ensureProtein(String sequence, int orgId, String name, String description)
    {
        return ProteinManager.ensureProtein(sequence, orgId, name, description);
    }

    private String guessOrganism(String sequence, String organism, String name, String description)
    {
        String fullHeader = getWholeHeader(name, description);
        ProteinPlus pp = new ProteinPlus(new Protein(fullHeader, sequence.getBytes()));
        if (organism == null)
        {
            for (OrganismGuessStrategy strategy : getStrategies())
            {
                organism = strategy.guess(pp);
                if (organism != null)
                {
                    break;
                }
            }
            if (organism == null)
            {
                organism = FastaDbLoader.UNKNOWN_ORGANISM;
            }
        }
        return organism;
    }

    public void ensureIdentifiers(int seqId, Map<String, Set<String>> typeAndIdentifiers)
    {
        ProteinManager.ensureIdentifiers(seqId, typeAndIdentifiers);
    }

    public Map<String, Set<String>> getIdentifiers(String description, String... names)
    {
        String combinedNames = StringUtils.join(names, "|");
        String wholeHeader = getWholeHeader(combinedNames, description);
        return Protein.getIdentifierMap(combinedNames, wholeHeader);
    }

    private String getWholeHeader(String identifier, String description)
    {
        return identifier != null ? (description != null ? (identifier + " " + description) : identifier)
                                  : description;
    }

    public void registerProteinSearchView(QueryViewProvider<ProteinSearchForm> provider)
    {
        _proteinSearchViewProviders.add(provider);
    }

    public void registerPeptideSearchView(QueryViewProvider<PeptideSearchForm> provider)
    {
        _peptideSearchViewProviders.add(provider);
    }

    public List<QueryViewProvider<PeptideSearchForm>> getPeptideSearchViews()
    {
        return Collections.unmodifiableList(_peptideSearchViewProviders);
    }

    public WebPartView getProteinCoverageView(int seqId, String[] peptides, int aaRowWidth, boolean showEntireFragmentInCoverage)
    {
        MS2Controller.ProteinViewBean bean = new MS2Controller.ProteinViewBean();
        bean.protein = ProteinManager.getProtein(seqId);
        bean.protein.setShowEntireFragmentInCoverage(showEntireFragmentInCoverage);
        bean.protein.setPeptides(peptides);
        bean.aaRowWidth = aaRowWidth;
        return new JspView<>("/org/labkey/ms2/proteinCoverageMap.jsp", bean);
    }

    public WebPartView getAnnotationsView(int seqId)
    {
        org.labkey.ms2.Protein protein = ProteinManager.getProtein(seqId);
        return new AnnotationView(protein);
    }

    public String getProteinSequence(int seqId)
    {
        return ProteinManager.getProteinSequence(seqId);
    }

    @Override
    public Integer getProteinSeqId(String sequence, int organismId)
    {
        org.labkey.ms2.Protein protein = ProteinManager.getProtein(sequence, organismId);
        return protein != null ? protein.getSeqId() : null;
    }

    @Override
    public List<Integer> getProteinSeqId(String sequence)
    {
        return ProteinManager.getProtein(sequence)
                .stream()
                .map(org.labkey.ms2.Protein::getSeqId)
                .collect(Collectors.toList());
    }

    public List<QueryViewProvider<ProteinSearchForm>> getProteinSearchViewProviders()
    {
        return Collections.unmodifiableList(_proteinSearchViewProviders);
    }

    public static ProteinServiceImpl getInstance()
    {
        return (ProteinServiceImpl) ServiceRegistry.get().getService(ProteinService.class);
    }
}
