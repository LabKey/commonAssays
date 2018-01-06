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
package org.labkey.ms2;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.labkey.api.view.JspView;
import org.labkey.ms2.protein.IdentifierType;
import org.labkey.ms2.protein.ProteinManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
* User: jeckels
* Date: May 14, 2012
*/
public class AnnotationView extends JspView<AnnotationView.AnnotViewBean>
{
    public AnnotationView(Protein protein)
    {
        super("/org/labkey/ms2/protAnnots.jsp", getBean(protein));
        setTitle("Annotations for " + protein.getBestName());
    }

    public static class AnnotViewBean
    {
        public String seqName;
        public String seqDesc;
        public String geneName;
        public Set<String> seqOrgs;
        public Set<String> genBankUrls;
        public String[] swissProtNames;
        public String[] swissProtAccns;
        public String[] GIs;
        public String[] ensemblIds;
        public String[] goCategories;
        public String[] IPI;
    }

    private static AnnotViewBean getBean(Protein protein)
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

        Set<String> allGbURLs = new HashSet<>();

        for (String ident : allGbIds)
        {
            String url = ProteinManager.makeFullAnchorString(
                    ProteinManager.makeIdentURLStringWithType(ident, "Genbank"),
                    "protWindow",
                    ident);
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
        bean.geneName = StringUtils.join(ProteinManager.makeFullAnchorStringArray(GeneNames, "protWindow", "GeneName"), ", ");
        bean.seqOrgs = ProteinManager.getOrganismsFromId(seqId);
        bean.genBankUrls = allGbURLs;
        bean.swissProtNames = ProteinManager.makeFullAnchorStringArray(SwissProtNames, "protWindow", "SwissProt");
        bean.swissProtAccns = ProteinManager.makeFullAnchorStringArray(SwissProtAccns, "protWindow", "SwissProtAccn");
        bean.GIs = ProteinManager.makeFullAnchorStringArray(GIs, "protWindow", "GI");
        bean.ensemblIds = ProteinManager.makeFullAnchorStringArray(EnsemblIDs, "protWindow", "Ensembl");
        bean.goCategories = ProteinManager.makeFullGOAnchorStringArray(GOCategories, "protWindow");
        bean.IPI = ProteinManager.makeFullAnchorStringArray(IPIds, "protWindow", "IPI");

        return bean;
    }
}
