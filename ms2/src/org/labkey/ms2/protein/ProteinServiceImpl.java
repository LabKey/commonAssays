/*
 * Copyright (c) 2012-2018 LabKey Corporation
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.TableInfo;
import org.labkey.api.protein.ProteinFeature;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.QueryViewProvider;
import org.labkey.api.reader.Readers;
import org.labkey.api.util.HtmlString;
import org.labkey.api.view.JspView;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.AnnotationView;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.fasta.Protein;
import org.labkey.ms2.protein.organism.GuessOrgByParsing;
import org.labkey.ms2.protein.organism.GuessOrgBySharedHash;
import org.labkey.ms2.protein.organism.GuessOrgBySharedIdents;
import org.labkey.ms2.protein.organism.OrganismGuessStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    private final List<QueryViewProvider<ProteinSearchForm>> _proteinSearchViewProviders = new CopyOnWriteArrayList<>();
    private final List<QueryViewProvider<PeptideSearchForm>> _peptideSearchViewProviders = new CopyOnWriteArrayList<>();
    private final List<FormViewProvider<ProteinSearchForm>> _proteinSearchFormViewProviders = new CopyOnWriteArrayList<>();

    private static final Logger LOG = LogManager.getLogger(ProteinServiceImpl.class);

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

    @Override
    public int ensureProtein(String sequence, String organism, String name, String description)
    {
        organism = guessOrganism(sequence, organism, name, description);

        return ProteinManager.ensureProtein(sequence, organism, name, description);
    }

    @Override
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

    @Override
    public void ensureIdentifiers(int seqId, Map<String, Set<String>> typeAndIdentifiers)
    {
        ProteinManager.ensureIdentifiers(seqId, typeAndIdentifiers);
    }

    @Override
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

    @Override
    public void registerProteinSearchView(QueryViewProvider<ProteinSearchForm> provider)
    {
        _proteinSearchViewProviders.add(provider);
    }

    @Override
    public void registerPeptideSearchView(QueryViewProvider<PeptideSearchForm> provider)
    {
        _peptideSearchViewProviders.add(provider);
    }

    @Override
    public void registerProteinSearchFormView(FormViewProvider<ProteinSearchForm> provider)
    {
        _proteinSearchFormViewProviders.add(provider);
    }

    @Override
    public List<QueryViewProvider<PeptideSearchForm>> getPeptideSearchViews()
    {
        return Collections.unmodifiableList(_peptideSearchViewProviders);
    }

    @Override
    public WebPartView<?> getProteinCoverageView(int seqId, String[] peptides, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures)
    {
        MS2Controller.ProteinViewBean bean = new MS2Controller.ProteinViewBean();
        bean.protein = ProteinManager.getProtein(seqId);
        bean.protein.setShowEntireFragmentInCoverage(showEntireFragmentInCoverage);
        bean.protein.setPeptides(peptides);
        bean.features = getProteinFeatures(accessionForFeatures);
        bean.aaRowWidth = aaRowWidth;
        return new JspView<>("/org/labkey/ms2/proteinCoverageMap.jsp", bean);
    }

    @Override
    public WebPartView<?> getAnnotationsView(int seqId, Map<String, Collection<HtmlString>> extraAnnotations)
    {
        org.labkey.ms2.Protein protein = ProteinManager.getProtein(seqId);
        return new AnnotationView(protein, extraAnnotations);
    }

    @Override
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

    @Override
    public TableInfo getSequencesTable()
    {
        return ProteinManager.getTableInfoSequences();
    }

    public List<QueryViewProvider<ProteinSearchForm>> getProteinSearchViewProviders()
    {
        return Collections.unmodifiableList(_proteinSearchViewProviders);
    }

    public List<FormViewProvider<ProteinSearchForm>> getProteinSearchFormViewProviders()
    {
        return Collections.unmodifiableList(_proteinSearchFormViewProviders);
    }

    public static ProteinServiceImpl getInstance()
    {
        return (ProteinServiceImpl) ProteinService.get();
    }

    private static final Cache<String, List<ProteinFeature>> FEATURE_CACHE =
            CacheManager.getBlockingCache(100, CacheManager.DAY, "Uniprot protein features", new FeatureLoader());

    @Override
    public List<ProteinFeature> getProteinFeatures(String accession)
    {
        if (accession == null)
        {
            return Collections.emptyList();
        }
        return FEATURE_CACHE.get(accession);
    }

    private static class FeatureLoader implements CacheLoader<String, List<ProteinFeature>>
    {
        private void setIndices(ProteinFeature f, Element location)
        {
            if (location.getElementsByTagName("begin").getLength() == 1)
            {
                int begin = Integer.parseInt(((Element) location.getElementsByTagName("begin").item(0)).getAttribute("position"));
                int end = Integer.parseInt(((Element) location.getElementsByTagName("end").item(0)).getAttribute("position"));
                f.setStartIndex(begin);
                f.setEndIndex(end);
            }
            else
            {
                int loc = Integer.parseInt(((Element) location.getElementsByTagName("position").item(0)).getAttribute("position"));
                f.setStartIndex(loc);
                f.setEndIndex(loc);
            }
        }

        @Override
        public List<ProteinFeature> load(@NotNull String accession, @Nullable Object argument)
        {
            List<ProteinFeature> result = new ArrayList<>();

            try
            {
                String url = "https://www.ebi.ac.uk/proteins/api/features/" + accession;

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestProperty("Accept", "application/xml");
                con.setRequestMethod("GET");
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK)
                { // success
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader in = Readers.getReader(con.getInputStream()))
                    {
                        String inputLine;

                        while ((inputLine = in.readLine()) != null)
                        {
                            response.append(inputLine);
                        }
                    }

                    DocumentBuilderFactory dbf =
                            DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    InputSource is = new InputSource();
                    is.setCharacterStream(new StringReader(response.toString()));

                    Document doc = db.parse(is);
                    Element entry = (Element) doc.getFirstChild();
                    NodeList featureElements = entry.getElementsByTagName("feature");
                    for (int i = 0; i < featureElements.getLength(); i++)
                    {
                        try
                        {
                            Element feature = (Element) featureElements.item(i);
                            ProteinFeature f = new ProteinFeature();
                            f.setType(feature.getAttribute("type"));
                            f.setDescription(feature.getAttribute("description"));
                            Element location = (Element) feature.getElementsByTagName("location").item(0);
                            if (f.isVariation())
                            {
                                if (location.getChildNodes().getLength() == 1)
                                {
                                    int loc = Integer.parseInt(((Element) location.getElementsByTagName("position").item(0)).getAttribute("position"));
                                    f.setStartIndex(loc);
                                    f.setEndIndex(loc);
                                    if (feature.getElementsByTagName("original").getLength() == 0 || feature.getElementsByTagName("variation").getLength() == 0)
                                    {
                                        continue;
                                    }
                                    String original = feature.getElementsByTagName("original").item(0).getFirstChild().getNodeValue();
                                    String variation = feature.getElementsByTagName("variation").item(0).getFirstChild().getNodeValue();
                                    f.setOriginal(original);
                                    f.setVariation(variation);
                                }
                                else if (location.getChildNodes().getLength() == 2)
                                {
                                    setIndices(f, location);
                                }
                            }
                            else
                            {
                                setIndices(f, location);
                            }
                            result.add(f);
                        }
                        catch (Exception e)
                        {
                            // we don't really care at the moment but exception is likely if xml is formatted differently than expected or given in the spec which happens sometimes
                            LOG.debug("Error parsing Uniprot response", e);
                        }
                    }
                }
                else
                {
                    LOG.error("HTTP GET failed to " + url + " with error code " + responseCode);
                }
            }
            catch (IOException | SAXException | ParserConfigurationException e)
            {
                LOG.warn("Failed querying Uniprot for " + accession, e);
            }

            result.sort(Comparator.comparingInt(ProteinFeature::getStartIndex));
            return Collections.unmodifiableList(result);
        }
    }

}
