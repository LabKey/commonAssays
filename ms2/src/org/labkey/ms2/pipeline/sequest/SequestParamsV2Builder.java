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

        supportedEnzymes.put("[X]|[X]", "No_Enzyme 0 0 - -");
        supportedEnzymes.put("[KR]|{P}", "Trypsin(KR/P) 1 1 KR P");
        supportedEnzymes.put("[K]|{P}", "Trypsin_K 1 1 K P");
        supportedEnzymes.put("[R]|{P}", "Trypsin_R 1 1 R P");
        supportedEnzymes.put("[KR]|[X]", "Trypsin(KR) 1 1 KR -");
        supportedEnzymes.put("[KRLNH]|[X]", "Trypsin(KRLNH) 1 1 KRLNH -");
        supportedEnzymes.put("[KRLNH]|{P}", "Trypsin(KRLNH/P) 1 1 KRLNH P");
        supportedEnzymes.put("[KMR]|{P}", "Trypsin/CnBr 1 1 KMR P");
        supportedEnzymes.put("[FMWY]|{P}", "Chymotrypsin(FMWY/P) 1 1 FMWY P");
        supportedEnzymes.put("[FWYL]|[X]", "Chymotrypsin 1 1 FWYL -");
        supportedEnzymes.put("[FWY]|{P}", "Chymotrypsin(FWY) 1 1 FWY P");
        supportedEnzymes.put("[R]|[X]", "Clostripain 1 1 R -");
        supportedEnzymes.put("[M]|[X]", "Cyanogen_Bromide 1 1 M -");
        supportedEnzymes.put("[M]|{P}", "Cyanogen_Bromide(M/P) 1 1 M P");
        supportedEnzymes.put("[W]|[X]", "IodosoBenzoate 1 1 W -");
        supportedEnzymes.put("[P]|[X]", "Proline_Endopept 1 1 P -");
        supportedEnzymes.put("[E]|[X]", "Staph_Protease 1 1 E -");
        supportedEnzymes.put("[ED]|[X]", "GluC 1 1 ED -");
        supportedEnzymes.put("[ED]|{P}", "GluC(ED/P) 1 1 ED P");
        supportedEnzymes.put("[K]|[X]", "LysC 1 1 K -");
        supportedEnzymes.put("[X]|[D]", "AspN 1 0 D -");
        supportedEnzymes.put("[ALIV]|{P}", "Elastase 1 1 ALIV P");
        supportedEnzymes.put("[AGILV]|{P}", "Elastase(AGILV/P) 1 1 AGILV P");
        supportedEnzymes.put("[E]|{P}", "GluC_Bicarb 1 1 E P");
        supportedEnzymes.put("[ALIVKRWFY]|{P}", "Elastase/Tryp/Chymo 1 1 ALIVKRWFY P");
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
