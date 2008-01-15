package org.labkey.ms2.pipeline.sequest;

import java.net.URI;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: Jan 23, 2007
 * Time: 4:57:14 PM
 */
public class SequestParamsV1Builder extends SequestParamsBuilder
{

    public SequestParamsV1Builder(Map<String, String> sequestInputParser, URI uriSequenceRoot)
    {
        super(sequestInputParser, uriSequenceRoot);

        this._params = new SequestParamsV1();

        supportedEnzymes.put("[X]|[X]", "No_Enzyme\t\t\t\t0\t-\t\t-");
        supportedEnzymes.put("[KR]|{P}", "Trypsin(KR/P)\t\t\t\t1\tKR\t\tP");
        supportedEnzymes.put("[K]|{P}", "Trypsin_K\t\t\t\t1\tK\t\tP");
        supportedEnzymes.put("[R]|{P}", "Trypsin_R\t\t\t\t1\tR\t\tP");
        supportedEnzymes.put("[KR]|[X]", "Trypsin(KR)\t\t\t\t1\tKR\t\t-");
        supportedEnzymes.put("[KRLNH]|[X]", "Trypsin(KRLNH)\t\t\t\t1\tKRLNH\t\t-");
        supportedEnzymes.put("[KRLNH]|{P}", "Trypsin(KRLNH/P)\t\t\t\t1\tKRLNH\t\tP");
        supportedEnzymes.put("[KMR]|{P}", "Trypsin/CnBr\t\t\t\t1\tKMR\t\tP");
        supportedEnzymes.put("[FMWY]|{P}", "Chymotrypsin(FMWY/P)\t\t\t\t1\tFMWY\t\tP");
        supportedEnzymes.put("[FWYL]|[X]", "Chymotrypsin(FWYL)\t\t\t\t1\tFWYL\t\t-");
        supportedEnzymes.put("[FWY]|{P}", "Chymotrypsin(FWY/P)\t\t\t\t1\tFWY\t\tP");
        supportedEnzymes.put("[R]|[X]", "Clostripain\t\t\t\t1\tR\t\t-");
        supportedEnzymes.put("[M]|[X]", "Cyanogen_Bromide(M)\t\t\t\t1\tM\t\t-");
        supportedEnzymes.put("[M]|{P}", "Cyanogen_Bromide(M/P)\t\t\t\t1\tM\t\tP");
        supportedEnzymes.put("[W]|[X]", "IodosoBenzoate\t\t\t\t1\tW\t\t-");
        supportedEnzymes.put("[P]|[X]", "Proline_Endopept\t\t\t\t1\tP\t\t-");
        supportedEnzymes.put("[E]|[X]", "Staph_Protease\t\t\t\t1\tE\t\t-");
        supportedEnzymes.put("[ED]|[X]", "GluC(ED)\t\t\t\t1\tED\t\t-");
        supportedEnzymes.put("[ED]|{P}", "GluC(ED/P)\t\t\t\t1\tED\t\tP");
        supportedEnzymes.put("[K]|[X]", "LysC\t\t\t\t1\tK\t\t-");
        supportedEnzymes.put("[X]|[D]", "AspN\t\t\t\t0\tD\t\t-");
        supportedEnzymes.put("[ALIV]|{P}", "Elastase(ALIV/P)\t\t\t\t1\tALIV\t\tP");
        supportedEnzymes.put("[AGILV]|{P}", "Elastase(AGILV/P)\t\t\t\t1\tAGILV\t\tP");
        supportedEnzymes.put("[E]|{P}", "GluC_Bicarb\t\t\t\t1\tE\t\tP");
        supportedEnzymes.put("[ALIVKRWFY]|{P}", "Elastase/Tryp/Chymo\t\t\t\t1\tALIVKRWFY\t\tP");

    }

    public String initXmlValues()
    {
        StringBuilder parserError = new StringBuilder();
        parserError.append(initDatabases(false));
        parserError.append(initPeptideMassTolerance());
        parserError.append(initMassUnits());
        parserError.append(initIonScoring());
        parserError.append(initEnzymeInfo());
        parserError.append(initDynamicMods());
        parserError.append(initTermDynamicMods());
        parserError.append(initMassType());
//        parserError.append(initMassRange());
        parserError.append(initStaticMods());
        parserError.append(initPassThroughs());

        return parserError.toString();
    }

    String initEnzymeInfo()
    {
        String parserError = "";
        String cleavageSites;
        String cleavageBlockers = "-";
        String offset;
        String enzyme = sequestInputParams.get("protein, cleavage site");
        if (enzyme == null) return parserError;
        if (enzyme.equals(""))
        {
            parserError = "protein, cleavage site did not contain a value.\n";
            return parserError;
        }
        enzyme = enzyme.trim();
        if (enzyme.startsWith("{"))
        {
            parserError = "protein, cleavage site does not support n-terminal blocking AAs(" + enzyme + ").\n";
            return parserError;
        }
        String supportedEnzyme = lookUpEnzyme(enzyme);
        if (supportedEnzyme != null)
        {
            if (supportedEnzyme.equals("No_Enzyme\t\t\t\t0\t-\t\t-"))
            {
                _params.getParam("enzyme_number").setValue("0");
                return parserError;
            }
            else
            {
                _params.getParam("enzyme_number").setValue("1");
                _params.getParam("enzyme1").setValue(supportedEnzyme);
                return parserError;
            }
        }

        if (enzyme.indexOf(',') != -1)
        {
            parserError = "protein, cleavage site contained more than one cleavage site(" + enzyme + ").\n";
            return parserError;
        }
        StringTokenizer sites = new StringTokenizer(enzyme, "|");
        if (sites.countTokens() != 2)
        {
            parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
            return parserError;
        }
        String cTermInfo = sites.nextToken().trim();
        String nTermInfo = sites.nextToken().trim();

        if (cTermInfo.startsWith("[") & cTermInfo.endsWith("]"))
        {
            cTermInfo = cTermInfo.substring(1, (cTermInfo.length() - 1)).trim();
            if (cTermInfo.equals("X"))
            {
                // [X]|[X]   is looked up in the hashtable above.
                if (nTermInfo.startsWith("[") & nTermInfo.endsWith("]"))
                {
                    nTermInfo = nTermInfo.substring(1, (nTermInfo.length() - 1)).trim();
                    if (isValidResidue(nTermInfo))
                    {
                        cleavageSites = nTermInfo;
                        offset = "0";
                    }
                    else
                    {
                        parserError = "protein, cleavage site contained invalid residue(" + nTermInfo + ").\n";
                        return parserError;
                    }

                }
                else
                {
                    parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
                    return parserError;
                }
            }
            else
            {
                if (isValidResidue(cTermInfo))
                {
                    cleavageSites = cTermInfo;
                    offset = "1";
                    if (nTermInfo.startsWith("{") & nTermInfo.endsWith("}"))
                    {
                        nTermInfo = nTermInfo.substring(1, (nTermInfo.length() - 1)).trim();
                        if (isValidResidue(nTermInfo))
                        {
                            cleavageBlockers = nTermInfo;
                        }
                        else
                        {
                            parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
                            return parserError;
                        }
                    }
                    else if (nTermInfo.equals("[X]"))
                    {
                        cleavageBlockers = "-";
                    }
                }
                else
                {
                    parserError = "protein, cleavage site contained invalid residue(" + cTermInfo + ").\n";
                    return parserError;
                }
            }

        }
        else
        {
            parserError = "protein, cleavage site contained invalid format(" + enzyme + ").\n";
            return parserError;
        }
        _params.getParam("enzyme1").setValue("Unknown(" + enzyme + ")\t\t\t\t" + offset + "\t" + cleavageSites + "\t\t" + cleavageBlockers);
        return parserError;
    }
}
