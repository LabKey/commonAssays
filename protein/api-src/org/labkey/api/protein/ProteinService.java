/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.api.protein;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.protein.search.PeptideSearchForm;
import org.labkey.api.protein.search.ProteinSearchForm;
import org.labkey.api.query.QueryViewProvider;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.HtmlString;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic support for importing and fetching individual proteins
 */
public interface ProteinService
{
    String NEGATIVE_HIT_PREFIX = "rev_";

    static ProteinService get()
    {
        return ServiceRegistry.get().getService(ProteinService.class);
    }

    static void setInstance(ProteinService impl)
    {
        ServiceRegistry.get().registerService(ProteinService.class, impl);
    }

    int ensureProtein(String sequence, String organism, String name, String description);

    /**
     *
     * @param seqId
     * @param typeAndIdentifiers A map of identifier types to identifiers.
     * Identifier type (e.g. SwissProtAccn) --> set of identifiers (e.g. B7Z1V4, P80404)
     */
    void ensureIdentifiers(int seqId, Map<String, Set<String>> typeAndIdentifiers);

    /**
     * Identifier type (e.g. SwissProtAccn) --> set of identifiers (e.g. B7Z1V4, P80404)
     * @return  A map of identifier types to identifiers
     */
    Map<String, Set<String>> getIdentifiers(String description, String... names);

    void registerProteinSearchView(QueryViewProvider<ProteinSearchForm> provider);
    void registerPeptideSearchView(QueryViewProvider<PeptideSearchForm> provider);
    void registerProteinSearchFormView(FormViewProvider<ProteinSearchForm> provider);

    List<QueryViewProvider<PeptideSearchForm>> getPeptideSearchViews();
    List<QueryViewProvider<ProteinSearchForm>> getProteinSearchViewProviders();
    List<FormViewProvider<ProteinSearchForm>> getProteinSearchFormViewProviders();

    /** @param aaRowWidth the number of amino acids to display in a single row */
    WebPartView<?> getProteinCoverageView(int seqId, List<PeptideCharacteristic> peptideCharacteristics, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures);

    WebPartView<?> getProteinCoverageViewWithSettings(int seqId, List<PeptideCharacteristic> combinedPeptideCharacteristics, int aaRowWidth, boolean showEntireFragmentInCoverage, @Nullable String accessionForFeatures, List<Replicate> replicates, List<PeptideCharacteristic> modifiedPeptideCharacteristics, boolean showStackedPeptides);

    /** @return a web part with all the annotations and identifiers we know for a given protein */
    WebPartView<?> getAnnotationsView(int seqId, Map<String, Collection<HtmlString>> extraAnnotations);

    TableInfo getSequencesTable();

    interface FormViewProvider<FormType>
    {
        WebPartView createView(ViewContext viewContext, FormType form);
    }

    ActionURL getProteinBeginUrl(Container c);

    List<ProteinFeature> getProteinFeatures(String accession);

    ActionURL getPeptideSearchUrl(Container c);

    ActionURL getPeptideSearchUrl(Container c, String sequence);
}
