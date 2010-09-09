/*
 * Copyright (c) 2004-2010 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protein.fasta;

import org.junit.Assert;
import org.junit.Test;

/**
 * User: migra
 * Date: Oct 20, 2004
 * Time: 9:59:18 AM
 */
public class PeptideTestCase extends Assert
{
    @Test
    public void testHydrophobicity()
    {
        for (int i = 0; i < peptides.length; i++)
        {
            String peptide = peptides[i];
            double h = Peptide.getHydrophobicity(peptide.getBytes(), 0, peptide.length());
            assertTrue(Math.abs(h - hydrophobicity[i]) < .01);
        }
    }

    private static final double[] hydrophobicity = {
        9.29,
        24.78,
        25.3,
        7.66,
        20.04,
        11.06,
        19.21,
        6.83,
        6.79,
        8.66,
        22.21,
        18.66,
        15.9,
        18.83,
        0.15,
        14.62,
        24.53,
        27.72,
        26.33,
        25.65,
        18.11,
        23.59,
        12.32,
        21.66,
        32.67,
        26.76,
        18.59,
        36.38,
        24.96,
        19.23,
        29.03,
        31.85,
        25.29,
        43.4,
        16.44,
        37.52,
        24.76,
        21.92,
        4.69,
        28.75,
        34.87,
        50.86,
        30.23,
        43.87,
        51.73,
        48.33,
        26.54,
        57.33,
        15.65,
        10.24,
        17.94,
        6.16,
        33.84,
        34.8,
        15.33,
        22.33,
        41.32,
        48.23,
        33.36,
        31.06,
        57.29,
        10.4,
        12.95,
        12.78,
        4.21,
        11.46,
        20.99,
        19.35,
        10.38,
        13.57,
        8.76,
        28.93,
        21.97,
        5.46,
        17.18,
        27.66,
        22.08,
        22.98,
        9.69,
        34.97,
        30.24,
        32.95,
        3.57,
        38.9,
        29.66,
        47.47,
        33.19,
        45.51,
        39.11,
        38.6,
        37.99,
        31.29,
        37.73,
        44.64,
        47.18,
        50.09,
        38.4,
        28.31,
        26.59,
        35.41,
        49.4,
        46.23,
        46.44,
        26.35,
        7.62,
        14.06,
        19.9,
        3.58,
        3.9,
        17.68,
        27.06,
        12.43,
        12.48,
        16.55,
        19.75,
        14.02,
        15.79,
        15.16,
        13.71,
        12.51,
        15.31,
        43.88,
        35.11,
        21.09,
        37.02,
        32.42,
        10.99,
        52.72,
        58.19,
        44.95,
        13.1,
        26.55,
        7.63,
        28.57,
        20.67,
        20.15,
        26.87,
        6.98,
        10.59,
        5.13,
        12.19,
        42.18,
        15.24,
        40.47,
        9.61,
        19.13,
        48.62,
        50.61,
        13.6,
        5.97,
        9.85,
        15.92,
        24.11,
        19.29,
        27.4,
        15.05,
        10.89,
        7.84,
        18.84,
        33.57,
        3.43,
        41.63,
        21.71,
        34.24,
        38.7,
        25.79,
        48.5,
        40.15,
        39.96,
        50.63,
        47.8,
        46.94,
        58.2,
        14.14,
        23.04,
        31.89,
        9.61,
        16.22,
        20.59,
        19.69,
        9.26,
        8.91,
        9.07,
        20.65,
        22.86,
        25.02,
        35.51,
        24.18,
        48.67,
        13.78,
        6.58,
        32.61,
        27.2,
        33.14,
        14.45,
        31.23,
        12.58,
        29.33,
        24.03,
        18.54,
        21.53,
        43.55,
        14.31,
        31.36,
        41.27,
        50.05,
        13.23,
        10.5,
        1.64,
        34.91,
        41.98,
        41.44,
        27.79,
        41.11,
        10.15,
        12.7,
        8.14,
        19.26,
        20.32,
        32.74,
        16.97,
        12.56,
        28.06,
        22.62,
        33.71,
        26.43,
        39.41,
        33.82,
        33.94,
        10.98,
        2.71,
        9.49,
        39.66,
        14.73,
        25.62,
        18.78,
        20.56,
        44.86,
        26.02,
        40.64,
        12.49,
        16.62,
        43.4,
        23.74,
        39.91,
        13.28,
        39.36,
        43.91,
        39.77,
        55.64,
        24.78,
        8.65,
        25.3,
        11.16,
        20.04,
        19.97,
        8.49,
        4.43,
        11.21,
        23.55,
        18.42,
        3.35,
        13.74,
        21.69,
        15.16,
        28.51,
        14.45,
        19.49,
        31.73,
        25.85,
        18.68,
        12.34,
        23.48,
        24.48,
        26.89,
        5.57,
        25.19,
        39.45,
        22.95,
        27.04,
        20.65,
        47.48,
        30.11,
        58.59,
        39.29,
        45.2,
        38.56,
        30.78,
        48.42,
        13.84,
        16.12,
        14.15,
        20.32,
        19.91,
        10.7,
        28.03,
        13.9,
        24.34,
        12.31,
        16.88,
        28.1,
        12.56,
        23.46,
        10.44,
        22.72,
        40.88,
        34.19,
        7.89,
        10.79,
        10.28,
        40.97,
        3.2,
        7.8,
        25.62,
        19.28,
        3.79,
        52.92,
        26.02,
        28.82,
        14.16,
        53.34,
        53.85,
        34.84,
        45.84,
        15.52,
        44.62,
        56.03,
        49.17,
        23.61,
        45.86,
        50.17,
        44.1,
        44.08,
        12.82,
        18.24,
        18.19,
        22.9,
        6.04,
        29.77,
        47.98,
        55.27,
        4.57,
        43.61,
        15.75,
        10.76,
        32.42
    };

    private static final String[] peptides = {
        "AVGNLR",
        "DLLFK",
        "DLLFR",
        "CQSFR",
        "GDVAFVK",
        "NPDPWAK",
        "DSAHGFLK",
        "KPVDEYK",
        "SCHTGLGR",
        "APNHAVVTR",
        "DGAGDVAFVK",
        "ASYLDCIR",
        "YLGEEYVK",
        "KASYLDCIR",
        "HQTVPQNTGGK",
        "WCALSHHER",
        "DSGFQMNQLR",
        "SASDLTWDNLK",
        "HSTIFENLANK",
        "EFQLFSSPHGK",
        "CDEWSVNSVGK",
        "EGYYGYTGAFR",
        "WCAVSEHEATK",
        "KDSGFQMNQLR",
        "DYELLCLDGTR",
        "KSASDLTWDNLK",
        "SVIPSDGPSVACVK",
        "MYLGYEYVTAIR",
        "SKEFQLFSSPHGK",
        "LKCDEWSVNSVGK",
        "CSTSSLLEACTFR",
        "DQYELLCLDNTR",
        "FDEFFSEGCAPGSK",
        "TAGWNIPMGLLYNK",
        "KPVEEYANCHLAR",
        "EDPQTFYYAVAVVK",
        "DCHLAQVPSHTVVAR",
        "LCMGSGLNLCEPNNK",
        "EGTCPEAPTDECKPVK",
        "ADRDQYELLCLDNTR",
        "NLNEKDYELLCLDGTR",
        "EDLIWELLNQAQEHFGK",
        "SDNCEDTPEAGYFAVAVVK",
        "IMNGEADAMSLDGGFVYIAGK",
        "SAGWNIPIGLLYCDLPEPR",
        "SMGGKEDLIWELLNQAQEHFGK",
        "KPVDEYKDCHLAQVPSHTVVAR",
        "AIAANEADAVTLDAGLVYDAYLAPNNLKPVVAEFYGSK",
        "DGPLTGTYR",
        "VGDANPALQK",
        "DFPIANGER",
        "HNGPEHWHK",
        "YAAELHLVHWNTK",
        "KYAAELHLVHWNTK",
        "MVNNGHSFNVEYDDSQDK",
        "HNGPEHWHKDFPIANGER",
        "AVVQDPALKPLALVYGEATSR",
        "YGDFGTAAQQPDGLAVVGVFLK",
        "LVQFHFHWGSSDDQGSEHTVDR",
        "LVQFHFHWGSSDDQGSEHTVDRK",
        "TLNFNAEGEPELLMLANWRPAQPLK",
        "EWTR",
        "FVVPR",
        "IPELR",
        "NVATPR",
        "TIAQYAR",
        "VLVDLER",
        "FAAYLER",
        "YGNPWEK",
        "NLAENISR",
        "FGCRDPVR",
        "VIFLENYR",
        "EIWGVEPSR",
        "TQQHYYEK",
        "VAAAFPGDVDR",
        "YEFGIFNQK",
        "DFYELEPHK",
        "VFADYEEYVK",
        "GYNAQEYYDR",
        "DYYFALAHTVR",
        "DIVNMLMHHDR",
        "HLQIIYEINQR",
        "TQQHYYEKDPK",
        "LLSYVDDEAFIR",
        "VLYPNDNFFEGK",
        "WPVHLLETLLPR",
        "IGEEYISDLDQLR",
        "IYYLSLEFYMGR",
        "DFNVGGYIQAVLDR",
        "QIIEQLSSGFFSPK",
        "TIFKDFYELEPHK",
        "IGEEYISDLDQLRK",
        "ARPEFTLPVHFYGR",
        "QLLNCLHVITLYNR",
        "QEYFVVAATLQDIIR",
        "WLVLCNPGLAEIIAER",
        "ICGGWQMEEADDWLR",
        "TCAYTNHTVIPEALER",
        "GYNAQEYYDRIPELR",
        "LITAIGDVVNHDPVVGDR",
        "LKQEYFVVAATLQDIIR",
        "VAIQLNDTHPSLAIPELMR",
        "WVDTQVVLAMPYDTPVPGYR",
        "VIPAADLSEQISTAGTEASGTGNMK",
        "FHYK",
        "LVLNR",
        "TFYLK",
        "VANYQR",
        "GIPDGHR",
        "LNSLTVGPR",
        "DAQLFIQK",
        "NLSVEDAAR",
        "THFSGDVQR",
        "LFAYPDTHR",
        "LCENIAGHLK",
        "HMDGYGSHTFK",
        "LAHEDPDYGLR",
        "LVNADGEAVYCK",
        "NFSDVHPEYGSR",
        "FNSANDDNVTQVR",
        "FSTVAGESGSADTVR",
        "DALLFPSFIHSQK",
        "VWPHGDYPLIPVGK",
        "IQALLDKYNEEKPK",
        "GAGAFGYFEVTHDITR",
        "LGPNYLQIPVNCPYR",
        "AAQKPDVLTTGGGNPVGDK",
        "GPLLVQDVVFTDEMAHFDR",
        "FYTEDGNWDLVGNNTPIFFIR",
        "NPVNYFAEVEQLAFDPSNMPPGIEPSPDK",
        "ITSDFR",
        "IGFGSFVEK",
        "GICECGVCK",
        "GEVFNELVGK",
        "LSEGVTISYK",
        "SGEPQTFTLK",
        "SLGTDLMNEMR",
        "GCPPDDIENPR",
        "RDNTNEIYSGK",
        "KGCPPDDIENPR",
        "FCECDNFNCDR",
        "LLVFSTDAGFHFAGDGK",
        "DKLPQPVQPDPVSHCK",
        "LKPEDITQIQPQQLVLR",
        "HCECSTDEVNSEDMDAYCR",
        "FQGQTCEMCQTCLGVCAEHK",
        "LSENNIQTIFAVTEEFQPVYK",
        "IRPLGFTEEVEVILQYICECECQSEGIPESPK",
        "IYLR",
        "MPYR",
        "AVVYR",
        "TFLQR",
        "ALFLASR",
        "FTVPHLR",
        "AGASLWGGLR",
        "ILEYAPCR",
        "CPEAECFR",
        "AQLKPPATSDA",
        "QVATAVQWTK",
        "SLQWFGATVR",
        "TPDGRPQEVGR",
        "TIQFDFQILSK",
        "VTAPPEAEYSGLVR",
        "QATLTQTLLIQNGAR",
        "AHGSSILACAPLYSWR",
        "LLESSLSSSEGEEPVEYK",
        "HVADSIGFTVELQLDWQK",
        "CELGPLHQQESQSLQLHFR",
        "SDFSWAAGQGYCQGGFSAEFTK",
        "GRPIVSASASLTIFPAMFNPEER",
        "VYVYLQHPAGIEPTPTLTLTGHDEFGR",
        "QASSIYDDSYLGYSVAVGEFSGDDTEDFVAGVPK",
        "AQILLDCGEDNICVPDLQLEVFGEQNHVYLGDK",
        "CEVFR",
        "LDQWLCEK",
        "VGINYWLAHK",
        "DDQNPHSSNICNISCDK",
        "IWCKDDQNPHSSNICNISCDK",
        "LKDFLK",
        "QYYTVFDR",
        "DPANIK",
        "FHGTVK",
        "YDNSLK",
        "LTGMAFR",
        "AITIFQER",
        "VPTPNVSVVDLTCR",
        "LISWYDNEFGYSNR",
        "IVSNASCTTNCLAPLAK",
        "VIHDHFGIVEGLMTTVHAITATQK",
        "IFVQK",
        "YIPGTK",
        "EDLIAYLK",
        "TGPNLHGLFGR",
        "TEREDLIAYLK",
        "TGQAPGFTYTDANK",
        "EETLMEYLENPK",
        "KTGQAPGFTYTDANK",
        "EETLMEYLENPKK",
        "ALELFR",
        "YKELGFQG",
        "LFTGHPETLEK",
        "HGTVVLTALGGILK",
        "HPGDFGADAQGAMTK",
        "VEADIAGHGQEVLIR",
        "GLSDGEWQQVLNVWGK",
        "YLEFISDAIIHVLHSK",
        "KGHHEAELKPLAQSHATK",
        "KYWGTK",
        "YTNANTPDR",
        "CGVPAIQPVLSGLSR",
        "YNSLTINNDITLLK",
        "VTALVNWVQQTLAAN",
        "LQQASLPLLSNTNCK",
        "IVNGEEAVPGSWPWQVSLQDK",
        "IETMR",
        "AWSVAR",
        "VLTSSAR",
        "AEFVEVTK",
        "YLYEIAR",
        "QTALVELLK",
        "NECFLSHK",
        "CCTESLVNR",
        "LVNELTEFAK",
        "FKDLGEEHFK",
        "HPEYAVSVLLR",
        "HLVDEPQNLIK",
        "TVMENFVAFVDK",
        "SLHTLFGDELCK",
        "RHPEYAVSVLLR",
        "YICDNQDTISSK",
        "TCVADESHAGCEK",
        "ETYGDMADCCEK",
        "LGEYGFQNALIVR",
        "EYEATLEECCAK",
        "VPQVSTPTLVEVSR",
        "LKECCDKPLLEK",
        "DDPHACYSTVFDK",
        "DAFLGSFLYEYSR",
        "KVPQVSTPTLVEVSR",
        "MPCTEDYLSLILNR",
        "YNGVFQECCQAEDK",
        "ECCHGDLLECADDR",
        "HPYFYAPELLYYANK",
        "NECFLSHKDDSPDLPK",
        "LFTFHADICTLPDTEK",
        "CCAADDKEACFAVEGPK",
        "DAIPENLPPLTADFAEDK",
        "RHPYFYAPELLYYANK",
        "DAIPENLPPLTADFAEDKDVCK",
        "GLVLIAFSQYLQQCPFDEHVK",
        "DLLFK",
        "CASFR",
        "DLLFR",
        "AMTNLR",
        "GDVAFVK",
        "DSADGFLK",
        "GPNHAVVSR",
        "SCHTAVDR",
        "LCQLCAGK",
        "ENFEVLCK",
        "YYGYTGAFR",
        "ECVPNSNER",
        "WCAIGHQER",
        "LLEACTFHKP",
        "ELPDPQESIQR",
        "ILESGPFVSCVK",
        "WCTISTHEANK",
        "NYELLCGDNTR",
        "CGLVPVLAENYK",
        "TSDANINWNNLK",
        "KNYELLCGDNTR",
        "KPVTDAENCHLAR",
        "KTYDSYLGDDYVR",
        "DNPQTHYYAVAVVK",
        "FDEFFSAGCAPGSPR",
        "ESKPPDSSKDECMVK",
        "DKPDNFQLFQSPHGK",
        "GEADAMSLDGGYLYIAGK",
        "CACSNHEPYFGYSGAFK",
        "SVTDCTSNFCLFQSNSK",
        "DQTVIQNTDGNNNEAWAK",
        "EDVIWELLNHAQEHFGK",
        "AAANFFSASCVPCADQSSFPK",
        "MDFELYLGYEYVTALQNLR",
        "SVDDYQECYLAMVPSHAVVAR",
        "TVGGKEDVIWELLNHAQEHFGK",
        "KSVDDYQECYLAMVPSHAVVAR",
        "WSGFSGGAIECETAENTEECIAK",
        "AISNNEADAVTLDGGLVYEAGLKPNNLKPVVAEFHGTK",
        "AWAVAR",
        "AACLLPK",
        "AEFAEVSK",
        "YLYEIAR",
        "LCTVATLR",
        "DDNPNLPR",
        "FQNALLVR",
        "TYETTLEK",
        "SLHTLFGDK",
        "LDELRDEGK",
        "NECFLQHK",
        "KQTALVELVK",
        "CCTESLVNR",
        "LVNEVTEFAK",
        "ETCFAEEGKK",
        "FKDLGEENFK",
        "HPDYSVVLLLR",
        "AVMDDFAAFVEK",
        "AAFTECCQAADK",
        "ETYGEMADCCAK",
        "YICENQDSISSK",
        "RHPDYSVVLLLR",
        "TCVADESAENCDK",
        "ADDKETCFAEEGK",
        "VPQVSTPTLVEVSR",
        "LKECCEKPLLEK",
        "CCAAADPHECYAK",
        "DVFLGMFLYEYAR",
        "KVPQVSTPTLVEVSR",
        "QNCELFEQLGEYK",
        "QEPERNECFLQHK",
        "HPYFYAPELLFFAK",
        "RHPYFYAPELLFFAK",
        "RPCFSALEVDETYVPK",
        "VFDEFKPLVEEPQNLIK",
        "VHTECCHGDLLECADDR",
        "EFNAETFTFHADICTLSEK",
        "ALVLIAFAQYLQQCPFEDHVK",
        "MPCAEDYLSVVLNQLCVLHEK",
        "VHTECCHGDLLECADDRADLAK",
        "LVRPEVDVMCTAFHDNEETFLK",
        "RMPCAEDYLSVVLNQLCVLHEK",
        "LVRPEVDVMCTAFHDNEETFLKK",
        "SHCIAEVENDEMPADLPSLAADFVESK",
        "GLDIQK",
        "IPAVFK",
        "ALPMHIR",
        "TKIPAVFK",
        "WENGECAQK",
        "LSFNPTQLEEQCHI",
        "VYVEELKPTPEGDLEILLQK",
        "VAGTWYSLAMAASDISLLDAQSAPLR",
        "YPNCAYK",
        "HIIVACEGNPYVPVHFDASV",
        "NGQTNCYQSYSTMSITDCR",
        "QHMDSSTSAASSSNYCNQMMK",
        "CKPVNTFVHESLADVQAVCSQK"
    };
}
