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
package org.labkey.api.protein.annotation;

import org.apache.commons.collections4.MultiValuedMap;
import org.labkey.api.protein.ProteinManager;
import org.labkey.api.protein.SimpleProtein;
import org.labkey.api.util.HtmlString;
import org.labkey.api.view.JspView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationView extends JspView<AnnotationView.AnnotViewBean>
{
    public AnnotationView(SimpleProtein protein)
    {
        this(protein, Collections.emptyMap());
    }

    public AnnotationView(SimpleProtein protein, Map<String, Collection<HtmlString>> extraAnnotations)
    {
        super("/org/labkey/protein/view/protAnnots.jsp", getBean(protein, extraAnnotations));
        setTitle("Annotations for " + protein.getBestName());
    }

    public static class AnnotViewBean
    {
        public String seqName;
        public String seqDesc;
        public List<HtmlString> geneNameLinks;
        public Set<String> seqOrgs;

        public Map<String, Collection<HtmlString>> annotations = new LinkedHashMap<>();
    }

    private static AnnotViewBean getBean(SimpleProtein protein, Map<String, Collection<HtmlString>> extraAnnotations)
    {
        int seqId = protein.getSeqId();

        MultiValuedMap<String, String> identifiers = ProteinManager.getIdentifiersFromId(seqId);

        /* collect header info */
        String SeqName = protein.getBestName(); // ProteinManager.getSeqParamFromId("BestName", seqId);
        String SeqDesc = protein.getDescription(); // ProteinManager.getSeqParamFromId("Description", seqId);
        Collection<String> GeneNames = identifiers.get("genename");
        /* collect first table info */
        Collection<String> GenBankIds = identifiers.get("genbank");
        Collection<String> SwissProtNames = identifiers.get("swissprot");
        Collection<String> EnsemblIDs = identifiers.get("ensembl");
        Collection<String> GIs = identifiers.get("gi");
        Collection<String> SwissProtAccns = identifiers.get(IdentifierType.SwissProtAccn.name().toLowerCase());
        Collection<String> IPIds = identifiers.get("ipi");
        Collection<String> RefSeqIds = identifiers.get("refseq");
        Collection<String> GOCategories = identifiers.get("go");

        HashSet<String> allGbIds = new HashSet<>();
        if (null != GenBankIds)
            allGbIds.addAll(GenBankIds);
        if (null != RefSeqIds)
            allGbIds.addAll(RefSeqIds);

        Set<HtmlString> allGbURLs = new HashSet<>();

        for (String ident : allGbIds)
        {
            HtmlString url = ProteinManager.makeFullAnchorLink(ProteinManager.makeIdentURLStringWithType(ident, "Genbank"),"protWindow", ident);
            allGbURLs.add(url);
        }

        // It is convenient to strip the version numbers from the IPI identifiers
        // and this may cause some duplications.  Use a hash-set to compress
        // duplicates
        if (null != IPIds && !IPIds.isEmpty())
        {
            Set<String> IPIset = new HashSet<>();

            for (String idWithoutVersion : IPIds)
            {
                int dotIndex = idWithoutVersion.indexOf(".");
                if (dotIndex != -1) idWithoutVersion = idWithoutVersion.substring(0, dotIndex);
                IPIset.add(idWithoutVersion);
            }
            IPIds = IPIset;
        }

        AnnotViewBean bean = new AnnotViewBean();

        /* info from db into view */
        bean.seqName = SeqName;
        bean.seqDesc = SeqDesc;
        bean.geneNameLinks = ProteinManager.makeFullAnchorLinks(GeneNames, "protWindow", "GeneName");
        bean.seqOrgs = ProteinManager.getOrganismsFromId(seqId);
        bean.annotations.put("Genbank IDs", allGbURLs);
        bean.annotations.put("GIs", ProteinManager.makeFullAnchorLinks(GIs, "protWindow", "GI"));
        bean.annotations.put("Swiss-Prot Accessions", ProteinManager.makeFullAnchorLinks(SwissProtAccns, "protWindow", "SwissProtAccn"));
        bean.annotations.put("Swiss-Prot Names", ProteinManager.makeFullAnchorLinks(SwissProtNames, "protWindow", "SwissProt"));
        bean.annotations.put("Ensembl", ProteinManager.makeFullAnchorLinks(EnsemblIDs, "protWindow", "Ensembl"));
        bean.annotations.put("IPI Numbers", ProteinManager.makeFullAnchorLinks(IPIds, "protWindow", "IPI"));
        bean.annotations.put("GO Categories", ProteinManager.makeFullGOAnchorLinks(GOCategories, "protWindow"));

        bean.annotations.putAll(extraAnnotations);

        return bean;
    }
}
