package org.labkey.ms2.pipeline.sequest;

import java.net.URI;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: Jan 23, 2007
 * Time: 4:33:29 PM
 */
public class SequestParamsV2Builder extends SequestParamsBuilder
{


    public SequestParamsV2Builder(Map<String, String> sequestInputParser, URI uriSequenceRoot)
    {
        super(sequestInputParser, uriSequenceRoot);

        this._params = new SequestParamsV2();

        supportedEnzymes.put("[KR]|{P}", "trypsin 1 1 KR P");
        supportedEnzymes.put("[KR]|[X]", "stricttrypsin 1 1 KR -");
        supportedEnzymes.put("[R]|{P}", "argc 1 1 R P");
        supportedEnzymes.put("[X]|[D]", "aspn 1 0 D -");
        supportedEnzymes.put("[FMWY]|{P}", "chymotrypsin 1 1 FMWY P");
        supportedEnzymes.put("[R]|[X]", "clostripain 1 1 R -");
        supportedEnzymes.put("[M]|{P}", "cnbr 1 1 M P");
        supportedEnzymes.put("[AGILV]|{P}", "elastase 1 1 AGILV P");
        supportedEnzymes.put("[D]|{P}", "formicacid 1 1 D P");
        supportedEnzymes.put("[K]|{P}", "trypsin_k 1 1 K P");
        supportedEnzymes.put("[ED]|{P}", "gluc 1 1 ED P");
        supportedEnzymes.put("[E]|{P}", "gluc_bicarb 1 1 E P");
        supportedEnzymes.put("[W]|[X]", "iodosobenzoate 1 1 W -");
        supportedEnzymes.put("[K]|[X]", "lysc 1 1 K P");
        supportedEnzymes.put("[K]|[X]", "lysc-p 1 1 K -");
        supportedEnzymes.put("[X]|[D]", "lysn 1 0 K -");
        supportedEnzymes.put("[X]|[KASR]", "lysn_promisc 1 0 KASR -");
        supportedEnzymes.put("[X]|[X]", "nonspecific 0 0 - -");
        supportedEnzymes.put("[FL]|[X]", "pepsina 1 1 FL -");
        supportedEnzymes.put("[P]|[X]", "protein_endopeptidase 1 1 P -");
        supportedEnzymes.put("[E]|[X]", "staph_protease 1 1 E -");
        supportedEnzymes.put("[KMR]|{P}", "trypsin/cnbr 1 1 KMR P");
        supportedEnzymes.put("[DEKR]|{P}", "trypsin_gluc 1 1 DEKR P");
    }

    public String initXmlValues()
    {
        StringBuilder parserError = new StringBuilder();
        parserError.append(initDatabases(true));
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
           _params.getParam("enzyme_info").setValue(supportedEnzyme);
           return parserError;
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
       //_params.getParam("enzyme_info").setValue("Unknown(" + enzyme + ")\t\t\t\t" + offset + "\t" + cleavageSites + "\t\t" + cleavageBlockers);
       _params.getParam("enzyme_info").setValue("Unknown(" + enzyme + ") 1 " + offset + " " + cleavageSites + " " + cleavageBlockers);

       return parserError;
   }

}
