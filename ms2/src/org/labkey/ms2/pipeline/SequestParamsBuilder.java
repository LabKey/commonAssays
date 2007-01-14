package org.labkey.ms2.pipeline;

import java.util.*;
import java.net.URI;
import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.labkey.api.util.AppProps;

/**
 * User: billnelson@uky.edu
 * Date: Sep 7, 2006
 * Time: 8:24:51 PM
 */
public class SequestParamsBuilder
{

    private BioMLInputParser sequestInputParser;
    private URI uriSequenceRoot;
    private char[] _validResidues = {'A', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'Y', 'X', 'B', 'Z', 'O'};
    private HashMap<String, String> _supportedEnzymes = new HashMap<String, String>();
    private SequestParams _params;


    SequestParamsBuilder(SequestInputParser sequestInputParser, URI uriSequenceRoot)
    {
        this.sequestInputParser = sequestInputParser;
        this.uriSequenceRoot = uriSequenceRoot;
        this._params = new SequestParams();

        _supportedEnzymes.put("[X]|[X]", "No_Enzyme\t\t\t\t0\t-\t\t-");
        _supportedEnzymes.put("[KR]|{P}", "Trypsin(KR/P)\t\t\t\t1\tKR\t\tP");
        _supportedEnzymes.put("[K]|{P}", "Trypsin_K\t\t\t\t1\tK\t\tP");
        _supportedEnzymes.put("[R]|{P}", "Trypsin_R\t\t\t\t1\tR\t\tP");
        _supportedEnzymes.put("[KR]|[X]", "Trypsin(KR)\t\t\t\t1\tKR\t\t-");
        _supportedEnzymes.put("[KRLNH]|[X]", "Trypsin(KRLNH)\t\t\t\t1\tKRLNH\t\t-");
        _supportedEnzymes.put("[KRLNH]|{P}", "Trypsin(KRLNH/P)\t\t\t\t1\tKRLNH\t\tP");
        _supportedEnzymes.put("[KMR]|{P}", "Trypsin/CnBr\t\t\t\t1\tKMR\t\tP");
        _supportedEnzymes.put("[FMWY]|{P}", "Chymotrypsin(FMWY/P)\t\t\t\t1\tFMWY\t\tP");
        _supportedEnzymes.put("[FWYL]|[X]", "Chymotrypsin(FWYL)\t\t\t\t1\tFWYL\t\t-");
        _supportedEnzymes.put("[FWY]|{P}", "Chymotrypsin(FWY/P)\t\t\t\t1\tFWY\t\tP");
        _supportedEnzymes.put("[R]|[X]", "Clostripain\t\t\t\t1\tR\t\t-");
        _supportedEnzymes.put("[M]|[X]", "Cyanogen_Bromide(M)\t\t\t\t1\tM\t\t-");
        _supportedEnzymes.put("[M]|{P}", "Cyanogen_Bromide(M/P)\t\t\t\t1\tM\t\tP");
        _supportedEnzymes.put("[W]|[X]", "IodosoBenzoate\t\t\t\t1\tW\t\t-");
        _supportedEnzymes.put("[P]|[X]", "Proline_Endopept\t\t\t\t1\tP\t\t-");
        _supportedEnzymes.put("[E]|[X]", "Staph_Protease\t\t\t\t1\tE\t\t-");
        _supportedEnzymes.put("[ED]|[X]", "GluC(ED)\t\t\t\t1\tED\t\t-");
        _supportedEnzymes.put("[ED]|{P}", "GluC(ED/P)\t\t\t\t1\tED\t\tP");
        _supportedEnzymes.put("[K]|[X]", "LysC\t\t\t\t1\tK\t\t-");
        _supportedEnzymes.put("[X]|[D]", "AspN\t\t\t\t0\tD\t\t-");
        _supportedEnzymes.put("[ALIV]|{P}", "Elastase(ALIV/P)\t\t\t\t1\tALIV\t\tP");
        _supportedEnzymes.put("[AGILV]|{P}", "Elastase(AGILV/P)\t\t\t\t1\tAGILV\t\tP");
        _supportedEnzymes.put("[E]|{P}", "GluC_Bicarb\t\t\t\t1\tE\t\tP");
        _supportedEnzymes.put("[ALIVKRWFY]|{P}", "Elastase/Tryp/Chymo\t\t\t\t1\tALIVKRWFY\t\tP");
    }

    public String initXmlValues()
    {
        StringBuilder parserError = new StringBuilder();
        parserError.append(initDatabases());
        parserError.append(initPeptideMassTolerance());
//        parserError.append(initMassUnits());
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

    public String getPropertyValue(String property)
    {
        return _params.getParam(property).getValue();
    }

    public char[] getValidResidues()
    {
        return _validResidues;
    }

    private String initDatabases()
    {
        String parserError = "";
        ArrayList<String> databases = new ArrayList<String>();
        String value = sequestInputParser.getInputParameter("pipeline, database");
        if (value == null || value.equals(""))
        {
            parserError = "pipeline, database; No value entered for database.\n";
            return parserError;
        }
        StringTokenizer st = new StringTokenizer(value, ",");
        if (st.countTokens() > 2)
        {
            parserError = "The max number of databases for one sequest search is two.\n";
            return parserError;
        }
        while (st.hasMoreTokens())
        {
            databases.add(st.nextToken().trim());
        }

        Param database1 = _params.getParam("first_database_name");
        File databaseFile = MS2PipelineManager.getSequenceDBFile(uriSequenceRoot, databases.get(0));
        if (!databaseFile.exists())
        {
            parserError = "pipeline, database; The database does not exist on the local server (" + databases.get(0) + ").\n";
            return parserError;
        }
        database1.setValue(databaseFile.getAbsolutePath());
        if (databases.size() > 1)
        {
            Param database2 = _params.getParam("second_database_name");
            //check for duplicate database entries
            if (!database1.getValue().equals(databases.get(1)))
            {
                databaseFile = MS2PipelineManager.getSequenceDBFile(uriSequenceRoot, databases.get(1));
                if (!databaseFile.exists())
                {
                    parserError = "pipeline, database; The database does not exist(" + databases.get(1) + ").\n";
                    return parserError;
                }
                database2.setValue(databaseFile.getAbsolutePath());
            }
        }
        return parserError;
    }


    private String initPeptideMassTolerance()
    {
        String parserError = "";
        String plusValueString =
            sequestInputParser.getInputParameter("spectrum, parent monoisotopic mass error plus");

        String minusValueString =
            sequestInputParser.getInputParameter("spectrum, parent monoisotopic mass error minus");

        if (plusValueString == null && minusValueString == null)
        {
            return parserError;
        }
        if (plusValueString == null || minusValueString == null || !plusValueString.equals(minusValueString))
        {
            parserError = "Sequest does not support asymmetric parent error ranges (minus=" +
                minusValueString + " plus=" + plusValueString + ").\n";
            return parserError;
        }
        if (plusValueString.equals("") && minusValueString.equals(""))
        {
            parserError = "No values were entered for spectrum, parent monoisotopic mass error minus/plus.\n";
            return parserError;
        }
        try
        {
            Float.parseFloat(plusValueString);
        }
        catch (NumberFormatException e)
        {
            parserError = "Invalid value for value for  spectrum, parent monoisotopic mass error minus/plus (" + plusValueString + ").\n";
            return parserError;
        }
        if (Float.parseFloat(plusValueString) < 0)
        {
            parserError = "Negative values not permitted for parent monoisotopic mass error(" + plusValueString + ").\n";
            return parserError;
        }
        Param pepTol = _params.getParam("peptide_mass_tolerance");
        pepTol.setValue(plusValueString);
        return parserError;

    }
    /*The first 3 parameters of that line are integers (0 or 1) that represents whether or not neutral losses (NH3 and H2))
    for a-ions, b-ions and y-ions are considered (0=no, 1=yes) in the correlation analysis. The last 9 parameters are
    floating point values representing a, b, c, d, v, w, x, y, and z ions respectively. The values entered for these
    paramters should range from 0.0 (don't use the ion series) to 1.0. The value entered represents the weighting that
    each ion series has (relative to the others). So an ion series with 0.5 contains half the weighting or relevance of
    an ion series with a 1.0 parameter. */

    private String initIonScoring()
    {
        Param ions = _params.getParam("ion_series");
        StringBuilder parserError = new StringBuilder();

        StringBuilder neutralLossA = new StringBuilder();
        StringBuilder neutralLossB = new StringBuilder();
        StringBuilder neutralLossY = new StringBuilder();
        StringBuilder ionA = new StringBuilder();
        StringBuilder ionB = new StringBuilder();
        StringBuilder ionC = new StringBuilder();
        StringBuilder ionD = new StringBuilder();
        StringBuilder ionV = new StringBuilder();
        StringBuilder ionW = new StringBuilder();
        StringBuilder ionX = new StringBuilder();
        StringBuilder ionY = new StringBuilder();
        StringBuilder ionZ = new StringBuilder();

        StringTokenizer st = new StringTokenizer(ions.getValue(), " ");

        while (st.hasMoreTokens())
        {

            neutralLossA.append(st.nextToken());
            neutralLossB.append(st.nextToken());
            neutralLossY.append(st.nextToken());
            ionA.append(st.nextToken());
            ionB.append(st.nextToken());
            ionC.append(st.nextToken());
            ionD.append(st.nextToken());
            ionV.append(st.nextToken());
            ionW.append(st.nextToken());
            ionX.append(st.nextToken());
            ionY.append(st.nextToken());
            ionZ.append(st.nextToken());
        }

        parserError.append(setIonSeriesParam("scoring, a ions", ionA));
        parserError.append(setIonSeriesParam("scoring, b ions", ionB));
        parserError.append(setIonSeriesParam("scoring, c ions", ionC));
        parserError.append(setIonSeriesParam("scoring, x ions", ionX));
        parserError.append(setIonSeriesParam("scoring, y ions", ionY));
        parserError.append(setIonSeriesParam("scoring, z ions", ionZ));
        parserError.append(setIonSeriesParam("sequest, d ions", ionD));
        parserError.append(setIonSeriesParam("sequest, v ions", ionV));
        parserError.append(setIonSeriesParam("sequest, w ions", ionW));
        parserError.append(setIonSeriesParam("sequest, a neutral loss", neutralLossA));
        parserError.append(setIonSeriesParam("sequest, b neutral loss", neutralLossB));
        parserError.append(setIonSeriesParam("sequest, y neutral loss", neutralLossY));
        if (!parserError.toString().equals(""))
        {
            return parserError.toString();
        }

        StringBuilder sb = new StringBuilder().
            append(neutralLossA).append(" ").
            append(neutralLossB).append(" ").
            append(neutralLossY).append(" ").
            append(ionA).append(" ").
            append(ionB).append(" ").
            append(ionC).append(" ").
            append(ionD).append(" ").
            append(ionV).append(" ").
            append(ionW).append(" ").
            append(ionX).append(" ").
            append(ionY).append(" ").
            append(ionZ);

        Param pepTol = _params.getParam("ion_series");
        pepTol.setValue(sb.toString());
        return parserError.toString();
    }

    private String initEnzymeInfo()
    {
        String parserError = "";
        String cleavageSites;
        String cleavageBlockers = "-";
        String offset;
        String enzyme = sequestInputParser.getInputParameter("protein, cleavage site");
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

    private String initDynamicMods()
    {
        HashMap<Character,String> defaultMods = new HashMap<Character, String>();
        defaultMods.put('S', "0.000000");
        defaultMods.put('C', "0.000000");
        defaultMods.put('M', "0.000000");
        defaultMods.put('X', "0.000000");
        defaultMods.put('T', "0.000000");
        defaultMods.put('Y', "0.000000");

        String parserError = "";
        String mods = sequestInputParser.getInputParameter("residue, potential modification mass");
        if (mods == null || mods.equals("")) return parserError;
        mods = removeWhiteSpace(mods);
        ArrayList<Character> residues = new ArrayList<Character>();
        ArrayList<String> masses = new ArrayList<String>();

        parserError = parseMods(mods, residues, masses);
        if (parserError != null && !parserError.equals("")) return parserError;

        for (int i = 0; i < masses.size(); i++)
        {
            defaultMods.put(residues.get(i), masses.get(i));
        }

        StringBuilder sb = new StringBuilder();
        Set<Character> modKeys = defaultMods.keySet();
        TreeSet<Character> sortedMods = new TreeSet<Character>(modKeys);
        for (Character aa :sortedMods)
        {
            sb.append(defaultMods.get(aa));
            sb.append(" ");
            sb.append(aa);
            sb.append(" ");
        }
        Param modProp = _params.getParam("diff_search_options");
        modProp.setValue(sb.toString().trim());
        return parserError;
    }


    private String initStaticMods()
    {
        String parserError;
        String mods = sequestInputParser.getInputParameter("residue, modification mass");

        ArrayList<Character> residues = new ArrayList<Character>();
        ArrayList<String> masses = new ArrayList<String>();

        parserError = parseMods(mods, residues, masses);
        if (parserError != null && !parserError.equals("")) return parserError;
        Param modProp;
        for (int i = 0; i < masses.size(); i++)
        {
            modProp = _params.startsWith("add_" + residues.get(i) + "_");
            modProp.setValue(masses.get(i));
        }
        return parserError;
    }

    private String parseMods(String mods, ArrayList<Character> residues, ArrayList<String> masses)
    {
        String parserError = "";
        Float massF;
        if (mods == null || mods.equals("")) return parserError;

        StringTokenizer st = new StringTokenizer(mods, ",");
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            token = removeWhiteSpace(token);
            if (token.charAt(token.length() - 2) != '@' && token.length() > 3)
            {
                parserError = "modification mass contained an invalid value(" + mods + ").\n";
                return parserError;
            }
            Character residue = token.charAt(token.length() - 1);
            if (!isValidResidue(residue))
            {
                parserError = "modification mass contained an invalid residue(" + residue + ").\n";
                return parserError;
            }
            residues.add(residue);
            String mass = token.substring(0, token.length() - 2);


            try
            {
                massF = Float.parseFloat(mass);
            }
            catch (NumberFormatException e)
            {
                parserError = "modification mass contained an invalid mass value (" + mass + ")\n";
                return parserError;
            }
            masses.add(massF.toString());
        }
        return parserError;
    }


    private String initTermDynamicMods()
    {
        String parserError = "";
        String nMod = sequestInputParser.getInputParameter("potential N-terminus modifications");
        String cMod = sequestInputParser.getInputParameter("potential C-terminus modifications");
        Param termProp = _params.getParam("term_diff_search_options");
        String defaultTermMod = termProp.getValue();
        StringTokenizer st = new StringTokenizer(defaultTermMod);
        String defaultCTerm = st.nextToken();
        String defaultNTerm = st.nextToken();

        if (nMod == null && cMod == null)
        {
            return parserError;
        }
        StringBuilder cValue = new StringBuilder(defaultCTerm);
        StringBuilder nValue = new StringBuilder(defaultNTerm);

        if (cMod != null)
            parserError = parseTermMods(cMod, cValue, "C");

        if (!parserError.equals(""))
            return parserError;


        if (nMod != null)
            parserError = parseTermMods(nMod, nValue, "N");

        if (!parserError.equals(""))
            return parserError;

        defaultCTerm = cValue.toString();
        defaultNTerm = nValue.toString();
        termProp.setValue(defaultNTerm + " " + defaultCTerm);
        return parserError;
    }

    private String initMassType()
    {
        String parserError = "";
        String massType = sequestInputParser.getInputParameter("spectrum, fragment mass type");
        String sequestValue;
        if (massType == null)
        {
            return parserError;
        }
        if (massType.equals(""))
        {
            parserError = "spectrum, fragment mass type contains no value.\n";
            return parserError;
        }
        if (massType.equalsIgnoreCase("average"))
        {
            sequestValue = "0";
        }
        else if (massType.equalsIgnoreCase("monoisotopic"))
        {
            sequestValue = "1";
        }
        else
        {
            parserError = "spectrum, fragment mass type contains an invalid value(" + massType + ").\n";
            return parserError;
        }
        _params.getParam("mass_type_fragment").setValue(sequestValue);
        return parserError;
    }

//    private String initMassUnits()
//    {
//        String parserError = "";
//        String pepMassUnit = sequestInputParser.getInputParameter("spectrum, parent mass error units");
//        if(pepMassUnit == null || pepMassUnit.equals(""))
//        {
//            //Check depricated param
//            pepMassUnit = sequestInputParser.getInputParameter("spectrum, parent monoisotopic mass error units");
//            if(pepMassUnit == null || pepMassUnit.equals("")) return parserError;
//        }
//        if(pepMassUnit.equalsIgnoreCase("dalton"))
//            _params.getParam("peptide_mass_units").setValue("0");
//        if(pepMassUnit.equalsIgnoreCase("ppm"))
//            _params.getParam("peptide_mass_units").setValue("2");
//        else
//            parserError = "spectrum, parent monoisotopic mass error units contained an invalid value for Sequest: (" +
//                 pepMassUnit + ").\n";
//        return parserError;
//    }

//
//   private void initMassRange()
//   {
//       String rangeMin = sequestInputParser.getInputParameter("spectrum, minimum parent m+h");
//       String rangeMax = sequestInputParser.getInputParameter("sequest, maximum parent m+h");
//       String defaultMin = null;
//       String defaultMax = null;
//       SequestParam sequestProp = _params.getParam("digest_mass_range");
//       String defaultValue = sequestProp.getValue();
//       StringTokenizer st = new StringTokenizer(defaultValue);
//       defaultMin = st.nextToken();
//       defaultMax = st.nextToken();
//       if(rangeMin == null || rangeMin.equals(""))
//       {
//            rangeMin = defaultMin;
//       }
//       else
//       {
//           try
//           {
//                Double.parseDouble(rangeMin);
//           }
//           catch( NumberFormatException e)
//           {
//                _log.info("spectrum, minimum parent m+h is an invalid value: (" +
//                 rangeMin + "). Using default value.");
//               rangeMin = defaultMin;
//           }
//       }
//       if(rangeMax == null || rangeMax.equals(""))
//       {
//            rangeMax = defaultMax;
//       }
//       else
//       {
//           try
//           {
//                Double.parseDouble(rangeMax);
//           }
//           catch( NumberFormatException e)
//           {
//                _log.info("spectrum, maximum parent m+h is an invalid value: (" +
//                 rangeMax + "). Using default value.");
//               rangeMax = defaultMax;
//           }
//       }
//       sequestProp.setValue(rangeMin + " " + rangeMax);
//   }

    private String initPassThroughs()
    {
        String parserError = "";
        Collection<SequestParam> passThroughs = _params.getPassThroughs();
        for (SequestParam passThrough : passThroughs)
        {
            String label = passThrough.getInputXmlLabels().get(0);
            String value = sequestInputParser.getInputParameter(label);
            if (value == null)
            {
                continue;
            }
            passThrough.setValue(value);
            parserError = passThrough.validate();
        }
        return parserError;
    }


    public String getSequestParamsText() throws SequestParamsException
    {
        StringBuilder sb = new StringBuilder();
        for (Param prop : _params.getParams())
        {
            sb.append(prop.convert());
            sb.append("\n");
            if (prop.getName().equals("first_database_name") ||
                prop.getName().equals("sequence_header_filter") ||
                prop.getName().equals("add_W_Tryptophan"))
            {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String setIonSeriesParam(String xmlLabel, StringBuilder labelValue)
    {

        String parserError = "";
        String value;
        String iPValue = sequestInputParser.getInputParameter(xmlLabel);
        if (iPValue == null)
        {
            return parserError;
        }
        if (iPValue.equals(""))
        {
            parserError = xmlLabel + " did not contain a value.\n";
            return parserError;
        }
        if (iPValue.equalsIgnoreCase("yes"))
        {
            labelValue.delete(0, labelValue.length());
            value = (xmlLabel.endsWith("loss")) ? "1" : "1.0";
            labelValue.append(value);
        }
        else if (sequestInputParser.getInputParameter(xmlLabel).equalsIgnoreCase("no"))
        {
            labelValue.delete(0, labelValue.length());
            value = (xmlLabel.endsWith("loss")) ? "0" : "0.0";
            labelValue.append(value);
        }
        else
        {
            parserError = xmlLabel + " contained an invalid value(" + iPValue + ").\n";
        }
        return parserError;
    }

    private boolean isValidResidue(char residue)
    {
        return isValidResidue(Character.toString(residue));
    }

    private boolean isValidResidue(String residueString)
    {
        char[] residues = residueString.toCharArray();
        boolean isValid;
        for (char residue : residues)
        {
            isValid = false;
            for (char valid : _validResidues)
            {
                if (residue == valid)
                {
                    isValid = true;
                    break;
                }
            }
            if (!isValid)
                return false;

        }
        return true;
    }

    private String lookUpEnzyme(String enzyme)
    {
        char bracket2a = '{';
        char bracket2b = '}';
        int offset = 0;
        CharSequence cutSites;
        CharSequence blockSites;

        try
        {
            cutSites = enzyme.subSequence(enzyme.indexOf('[') + 1, enzyme.indexOf(']'));
        }
        catch (IndexOutOfBoundsException e)
        {
            cutSites = new StringBuilder();
        }
        if (enzyme.lastIndexOf('[') != enzyme.indexOf('['))
        {
            bracket2a = '[';
            bracket2b = ']';
            offset = enzyme.indexOf(']') + 1;
        }

        try
        {
            int startIndex = enzyme.indexOf(bracket2a, offset) + 1;
            int endIndex = enzyme.indexOf(bracket2b, offset);
            blockSites = enzyme.substring(startIndex, endIndex);
        }
        catch (IndexOutOfBoundsException e)
        {
            blockSites = new StringBuilder();
        }

        Set<String> supportedEnzymes = _supportedEnzymes.keySet();
        boolean matches = false;
        for (String lookUp : supportedEnzymes)
        {
            String lookUpBlocks;
            String lookUpCuts;

            try
            {
                lookUpCuts = lookUp.substring(lookUp.indexOf('[') + 1, lookUp.indexOf(']'));
            }
            catch (IndexOutOfBoundsException e)
            {
                lookUpCuts = "";
            }


            try
            {
                int startIndex = lookUp.indexOf(bracket2a, offset) + 1;
                int endIndex = lookUp.indexOf(bracket2b, offset);
                lookUpBlocks = lookUp.substring(startIndex, endIndex);
            }
            catch (IndexOutOfBoundsException e)
            {
                lookUpBlocks = "";
            }

            if (lookUpCuts.length() == cutSites.length())
            {
                matches = true;
                for (int i = 0; i < cutSites.length(); i++)
                {
                    if (lookUpCuts.indexOf(cutSites.charAt(i)) < 0)
                    {
                        matches = false;
                    }
                }
                if (matches &&
                    lookUpBlocks.length() == blockSites.length())
                {
                    if (blockSites.length() == 0) break;
                    for (int i = 0; i < blockSites.length(); i++)
                    {
                        if (lookUpBlocks.indexOf(blockSites.charAt(i)) < 0)
                        {
                            matches = false;
                        }
                    }
                }
                else
                {
                    matches = false;
                }
            }
            if (matches) return _supportedEnzymes.get(lookUp);
        }
        return null;
    }

    //Used with JUnit
    private SequestParams getProperties()
    {
        return _params;
    }

    private String removeWhiteSpace(String value)
    {
        StringBuilder dirty = new StringBuilder(value);
        StringBuilder clean = new StringBuilder();
        char c;
        for (int i = 0; i < dirty.length(); i++)
        {
            c = dirty.charAt(i);
            if (Character.isWhitespace(c)) continue;
            clean.append(c);
        }
        return clean.toString();
    }

    private String parseTermMods(String mod, StringBuilder value, String term)
    {

        String parserError = "";
        String key = "@[";
        mod = removeWhiteSpace(mod);
        if (term.equals("C")) key = "@]";
        int modCount = new StringTokenizer(mod, ",").countTokens();
        if (modCount > 1)
        {
            parserError = "potential " + term + "-terminus modifications has more than one value(" + mod + ").\n";
            return parserError;
        }
        try
        {
            if (mod.equals(""))
            {
                return parserError;
            }
            else if (mod.endsWith(key))
            {
                double d = Double.parseDouble(mod.substring(0, mod.indexOf(key)));
                String temp = Double.toString(d);
                value.replace(0, value.length(), temp);
            }
            else
            {
                parserError = "potential " + term + "-terminus modifications has an invalid format(" + mod + ").\n";
                return parserError;
            }
        }
        catch (NumberFormatException e)
        {
            parserError = "potential " + term + "-terminus modifications has an invalid format(" + mod + ").\n";
            return parserError;
        }
        return parserError;
    }


    //JUnit TestCase
    public static class TestCase extends junit.framework.TestCase
    {

        SequestParamsBuilder spb;
        SequestInputParser ip;
        String dbPath;

        TestCase(String name)
        {
            super(name);
        }

        public static Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest(new TestCase("testInitPeptideMassToleranceNormal"));
            suite.addTest(new TestCase("testInitPeptideMassToleranceMissingValue"));
            suite.addTest(new TestCase("testInitPeptideMassToleranceMissingInput"));
            suite.addTest(new TestCase("testInitPeptideMassToleranceDefault"));
            suite.addTest(new TestCase("testInitPeptideMassToleranceNegative"));
            suite.addTest(new TestCase("testInitPeptideMassToleranceInvalid"));
            suite.addTest(new TestCase("testInitMassTypeNormal"));
            suite.addTest(new TestCase("testInitMassTypeMissingValue"));
            suite.addTest(new TestCase("testInitMassTypeDefault"));
            suite.addTest(new TestCase("testInitMassTypeGarbage"));
            suite.addTest(new TestCase("testInitIonScoringNormal"));
            suite.addTest(new TestCase("testInitIonScoringMissingValue"));
            suite.addTest(new TestCase("testInitIonScoringMissingDefault"));
            suite.addTest(new TestCase("testInitIonScoringDefault"));
            suite.addTest(new TestCase("testInitIonScoringGarbage"));
            suite.addTest(new TestCase("testInitEnzymeInfoNormal"));
            suite.addTest(new TestCase("testInitEnzymeInfoUnsupported"));
            suite.addTest(new TestCase("testInitEnzymeInfoDefault"));
            suite.addTest(new TestCase("testInitEnzymeInfoMissingValue"));
            suite.addTest(new TestCase("testInitEnzymeInfoGarbage"));
            suite.addTest(new TestCase("testInitDynamicModsNormal"));
            suite.addTest(new TestCase("testInitDynamicModsMissingValue"));
            suite.addTest(new TestCase("testInitDynamicModsDefault"));
            suite.addTest(new TestCase("testInitDynamicModsGarbage"));
            suite.addTest(new TestCase("testInitTermDynamicModsNormal"));
            suite.addTest(new TestCase("testInitTermDynamicModsMissingValue"));
            suite.addTest(new TestCase("testInitTermDynamicModsDefault"));
            suite.addTest(new TestCase("testInitTermDynamicModsGarbage"));
            suite.addTest(new TestCase("testInitStaticModsNormal"));
            suite.addTest(new TestCase("testInitStaticModsMissingValue"));
            suite.addTest(new TestCase("testInitStaticModsDefault"));
            suite.addTest(new TestCase("testInitStaticModsGarbage"));
            suite.addTest(new TestCase("testInitPassThroughsNormal"));
            suite.addTest(new TestCase("testInitPassThroughsMissingValue"));
            suite.addTest(new TestCase("testInitPassThroughsNegative"));
            suite.addTest(new TestCase("testInitPassThroughsGarbage"));
            suite.addTest(new TestCase("testInitDatabasesNormal"));
            suite.addTest(new TestCase("testInitDatabasesMissingValue"));
            suite.addTest(new TestCase("testInitDatabasesMissingInput"));
            suite.addTest(new TestCase("testInitDatabasesGarbage"));
            return suite;
        }

        protected void setUp() throws Exception
        {
            ip = new SequestInputParser();
            String projectRoot = AppProps.getInstance().getProjectRoot();
            if (projectRoot == null || projectRoot.equals("")) projectRoot = "C:/CPAS";
            File root = new File(projectRoot);
            root = new File(root, "/sampledata/xarfiles/ms2pipe/databases");
            dbPath = root.getCanonicalPath();
            spb = new SequestParamsBuilder(ip, root.toURI());
        }

        protected void tearDown()
        {
            ip = null;
            spb = null;
        }

        public void testInitDatabasesNormal() throws IOException
        {
            String value = "Bovine_mini.fasta";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            String parserError = spb.initDatabases();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("first_database_name");
            assertEquals(new File(dbPath + File.separator + value).getCanonicalPath(), new File(sp.getValue()).getCanonicalPath());

            value = "Bovine_mini.fasta";
            String value2 = "Bovine_mini.fasta.hdr";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + ", " + value2 + "</note>" +
                "</bioml>");



//            Commenting out as Bovine_mini.fasta.hdr is not checked into source control 

//            parserError = spb.initDatabases();
//            if (!parserError.equals("")) fail(parserError);
//            sp = spb.getProperties().getParam("first_database_name");
//            assertEquals(dbPath + File.separator + value, sp.getValue());
//            sp = spb.getProperties().getParam("second_database_name");
//            assertEquals(dbPath + File.separator + value2, sp.getValue());
        }

        public void testInitDatabasesMissingValue()
        {
            String value = "";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            String parserError = spb.initDatabases();
            if (parserError.equals("")) fail("Expected error.");
            assertEquals("pipeline, database; No value entered for database.\n", parserError);
        }

        public void testInitDatabasesMissingInput()
        {
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            String parserError = spb.initDatabases();
            if (parserError.equals("")) fail("Expected error.");
            assertEquals("pipeline, database; No value entered for database.\n", parserError);
        }

        public void testInitDatabasesGarbage()
        {
            String value = "garbage";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            String parserError = spb.initDatabases();
            if (parserError.equals("")) fail("Expected error.");
            assertEquals("pipeline, database; The database does not exist on the local server (" + value + ").\n", parserError);

            value = "Bovine_mini.fasta, garbage";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            parserError = spb.initDatabases();
            if (parserError.equals("")) fail("Expected error.");
            assertEquals("pipeline, database; The database does not exist(garbage).\n", parserError);

            value = "garbage, Bovine_mini.fasta";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            parserError = spb.initDatabases();
            if (parserError.equals("")) fail("Expected error.");
            assertEquals("pipeline, database; The database does not exist on the local server (garbage).\n", parserError);
        }

        public void testInitPeptideMassToleranceNormal()
        {
            float expected = 30.0f;
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            String parserError = spb.initPeptideMassTolerance();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            float actual = Float.parseFloat(sp.getValue());
            assertEquals("peptide_mass_tolerance", expected, actual, 0.00);
        }

        public void testInitPeptideMassToleranceMissingValue()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\"></note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">4.0</note>" +
                "</bioml>");

            String parserError = spb.initPeptideMassTolerance();
            if (parserError.equals("")) fail("No error message.");
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=4.0 plus=).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">4.0</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\"></note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.equals("")) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus= plus=4.0).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\"></note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\"></note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.equals("")) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("No values were entered for spectrum, parent monoisotopic mass error minus/plus.\n", parserError);
        }

        public void testInitPeptideMassToleranceNegative()
        {
            float expected = -30.0f;
            String defaultValue = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            String parserError = spb.initPeptideMassTolerance();
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            String actual = sp.getValue();
            assertEquals("parameter value changed", defaultValue, actual);
            assertEquals("Negative values not permitted for parent monoisotopic mass error(" + expected + ").\n", parserError);
        }

        public void testInitPeptideMassToleranceInvalid()
        {
            String expected = "garbage";
            String defaultValue = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            String parserError = spb.initPeptideMassTolerance();
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            String actual = sp.getValue();
            assertEquals("parameter value changed", defaultValue, actual);
            assertEquals("Invalid value for value for  spectrum, parent monoisotopic mass error minus/plus (garbage).\n", parserError);
        }


        public void testInitPeptideMassToleranceMissingInput()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">5.0</note>" +
                "</bioml>");
            String parserError = spb.initPeptideMassTolerance();
            if (parserError.equals("")) fail("No error message.");
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=null plus=5.0).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">5.0</note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.equals("")) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=5.0 plus=null).\n", parserError);
        }

        public void testInitPeptideMassToleranceDefault()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");
            String parserError = spb.initPeptideMassTolerance();
            if (!parserError.equals("")) fail(parserError);
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
        }

        public void testInitMassTypeNormal()
        {
            String expected = "AveRage";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">" + expected + "</note>" +
                "</bioml>");

            String parserError = spb.initMassType();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", "0", actual);

            expected = "MonoIsotopic";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">" + expected + "</note>" +
                "</bioml>");

            parserError = spb.initMassType();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("mass_type_fragment");
            actual = sp.getValue();
            assertEquals("mass_type_fragment", "1", actual);
        }

        public void testInitMassTypeMissingValue()
        {
            String expected = "1";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\"></note>" +
                "</bioml>");

            String parserError = spb.initMassType();
            if (parserError.equals("")) fail("No error message.");
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
            assertEquals("mass_type_fragment", "spectrum, fragment mass type contains no value.\n", parserError);
        }

        public void testInitMassTypeDefault()
        {
            String expected = "1";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            String parserError = spb.initMassType();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
        }

        public void testInitMassTypeGarbage()
        {
            String expected = "1";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">garbage</note>" +
                "</bioml>");

            String parserError = spb.initMassType();
            if (parserError.equals("")) fail("No error message.");
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
            assertEquals("mass_type_fragment", "spectrum, fragment mass type contains an invalid value(garbage).\n", parserError);
        }

        public void testInitIonScoringNormal()
        {
            String expected = "0 0 0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">NO</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            String parserError = spb.initIonScoring();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);

            expected = "1 1 1 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">Yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yEs</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yeS</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">YEs</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yES</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">YeS</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">YES</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">yes</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">yes</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        public void testInitIonScoringMissingValue()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\"></note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            String parserError = spb.initIonScoring();
            if (parserError.equals("")) fail("Expected error");
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, y neutral loss did not contain a value.\n", parserError);

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\"></note>" +
                "<note type=\"input\" label=\"sequest, d ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.equals("")) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "scoring, c ions did not contain a value.\n", parserError);

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, d ions\"></note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.equals("")) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, d ions did not contain a value.\n", parserError);
        }

        public void testInitIonScoringMissingDefault()
        {
            String expected = "0 0 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            String parserError = spb.initIonScoring();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);


        }

        public void testInitIonScoringDefault()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            String parserError = spb.initIonScoring();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        public void testInitIonScoringGarbage()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">garbage</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            String parserError = spb.initIonScoring();
            if (parserError.equals("")) fail("Expected error");
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, y neutral loss contained an invalid value(garbage).\n", parserError);

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">garbage</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.equals("")) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "scoring, c ions contained an invalid value(garbage).\n", parserError);

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">garbage</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.equals("")) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, d ions contained an invalid value(garbage).\n", parserError);
        }


        public void testInitEnzymeInfoNormal()
        {
            //Testing no enzyme
            String expected1 = "0";
            String expected2 = "No_Enzyme\t\t\t\t0\t0\t-\t\t-";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|[X]</note>" +
                "</bioml>");

            String parserError = spb.initEnzymeInfo();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("enzyme_number");
            String actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme0");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

            expected1 = "1";
            expected2 = "Trypsin_K\t\t\t\t1\tK\t\tP";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[K]|{P}</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

            expected1 = "1";
            expected2 = "Trypsin(KRLNH)\t\t\t\t1\tKRLNH\t\t-";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[KRLNH]|[X]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
        }

        public void testInitEnzymeInfoUnsupported()
        {
            String expected1 = "1";
            String expected2 = "Unknown([QND]|[X])\t\t\t\t1\tQND\t\t-";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[QND]|[X]</note>" +
                "</bioml>");

            String parserError = spb.initEnzymeInfo();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("enzyme_number");
            String actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

            expected1 = "1";
            expected2 = "Unknown([PGY]|{W})\t\t\t\t1\tPGY\t\tW";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[PGY]|{W}</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);


            expected1 = "1";
            expected2 = "Unknown([X]|[W])\t\t\t\t0\tW\t\t-";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|[W]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

        }

        public void testInitEnzymeInfoDefault()
        {
            String expected1 = "1";
            String expected2 = "Trypsin(KR/P)\t\t\t\t1\tKR\t\tP";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            String parserError = spb.initEnzymeInfo();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("enzyme_number");
            String actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
        }

        public void testInitEnzymeInfoMissingValue()
        {
            String expected1 = "1";
            String expected2 = "Trypsin(KR/P)\t\t\t\t1\tKR\t\tP";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\"></note>" +
                "</bioml>");

            String parserError = spb.initEnzymeInfo();
            if (parserError.equals("")) fail("Expected error message");
            Param sp = spb.getProperties().getParam("enzyme_number");
            String actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("enzyme_description", "protein, cleavage site did not contain a value.\n", parserError);

        }

        public void testInitEnzymeInfoGarbage()
        {
            String expected1 = "1";
            String expected2 = "Trypsin(KR/P)\t\t\t\t1\tKR\t\tP";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">foo</note>" +
                "</bioml>");

            String parserError = spb.initEnzymeInfo();
            if (parserError.equals("")) fail("Expected error message");
            Param sp = spb.getProperties().getParam("enzyme_number");
            String actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("enzyme_description", "protein, cleavage site contained invalid format(foo).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[CV]|{P},[KR]|{P}</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.equals("")) fail("Expected error message");
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("protein, cleavage site contained more than one cleavage site([CV]|{P},[KR]|{P}).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">{P}|[KR]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.equals("")) fail("Expected error message");
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("protein, cleavage site does not support n-terminal blocking AAs({P}|[KR]).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[a]|[X]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.equals("")) fail("Expected error message");
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("protein, cleavage site contained invalid residue(a).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|[a]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.equals("")) fail("Expected error message");
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("protein, cleavage site contained invalid residue(a).\n", parserError);

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|P</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.equals("")) fail("Expected error message");
            sp = spb.getProperties().getParam("enzyme_number");
            actual = sp.getValue();
            assertEquals("enzyme_number", expected1, actual);

            sp = spb.getProperties().getParam("enzyme1");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("protein, cleavage site contained invalid format([X]|P).\n", parserError);
        }

        public void testInitDynamicModsNormal()
        {
            String expected1 = "0.000000 C 16.0 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+16@M</note>" +
                "</bioml>");

            String parserError = spb.initDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);


            expected1 = "0.000000 C 16.0 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">16@M</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);

            expected1 = "0.000000 C -16.0 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">- 16.0000 @ M</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);


            expected1 = "9.0 C 16.0 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\"> 16@M,9@C </note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        public void testInitDynamicModsMissingValue()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\"></note>" +
                "</bioml>");

            String parserError = spb.initDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        public void testInitDynamicModsDefault()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            String parserError = spb.initDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        public void testInitDynamicModsGarbage()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">16@J</note>" +
                "</bioml>");

            String parserError = spb.initDynamicMods();
            if (parserError.equals("")) fail("Error expected.");
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
            assertEquals("diff_search_options", "modification mass contained an invalid residue(J).\n", parserError);

            expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">G@18</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (parserError.equals("")) fail("Error expected.");
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
            assertEquals("diff_search_options", "modification mass contained an invalid value(G@18).\n", parserError);

        }

        public void testInitTermDynamicModsNormal()
        {
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String defaultValue = sp.getValue();
            String expected1 = "42.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"potential N-terminus modifications\">+42.0@[</note>" +
                "</bioml>");

            String parserError = spb.initTermDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "42.0 -88.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"potential C-terminus modifications\">-88 @ ]</note>" +
                "<note type=\"input\" label=\"potential N-terminus modifications\">+ 42.0 @[</note>" +
                "</bioml>");

            parserError = spb.initTermDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "0.0 -88.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"potential C-terminus modifications\">-88@]</note>" +
                "</bioml>");

            parserError = spb.initTermDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

        }

        public void testInitTermDynamicModsMissingValue()
        {
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String defaultValue = sp.getValue();
            String expected1 = "0.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"potential N-terminus modifications\">+0.0@[</note>" +
                "<note type=\"input\" label=\"potential C-terminus modifications\"></note>" +
                "</bioml>");

            String parserError = spb.initTermDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "42.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"potential C-terminus modifications\"></note>" +
                "<note type=\"input\" label=\"potential N-terminus modifications\">+ 42.0 @[</note>" +
                "</bioml>");

            parserError = spb.initTermDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
        }

        public void testInitTermDynamicModsDefault()
        {
            String expected1 = "0.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            String parserError = spb.initTermDynamicMods();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
        }

        public void testInitTermDynamicModsGarbage()
        {
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String defaultValue = sp.getValue();
            String expected1 = "0.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"potential C-terminus modifications\">foo</note>" +
                "<note type=\"input\" label=\"potential N-terminus modifications\"></note>" +
                "</bioml>");

            String parserError = spb.initTermDynamicMods();
            if (parserError.equals("")) fail("Expected error.");
            sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
            assertEquals("term_diff_search_options", "potential C-terminus modifications has an invalid format(foo).\n", parserError);

            sp.setValue(defaultValue);
            expected1 = "0.0 0.0";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"potential C-terminus modifications\"></note>" +
                "<note type=\"input\" label=\"potential N-terminus modifications\">bar</note>" +
                "</bioml>");

            parserError = spb.initTermDynamicMods();
            if (parserError.equals("")) fail("Expected error.");
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
            assertEquals("term_diff_search_options", "potential N-terminus modifications has an invalid format(bar).\n", parserError);
        }

        public void testInitStaticModsNormal()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "227.0";
                ip.parse("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\">+227@" + residue + "</note>" +
                    "</bioml>");


                String parserError = spb.initStaticMods();
                if (!parserError.equals("")) fail(parserError);
                Param sp = spb.getProperties().startsWith("add_" + residue + "_");
                String actual = sp.getValue();
                assertEquals("residue, modification mass", expected1, actual);
            }

            for (char residue : validResidues)
            {
                String expected1 = "-9.0";
                ip.parse("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\">- 9 @ " + residue + "</note>" +
                    "</bioml>");


                String parserError = spb.initStaticMods();
                if (!parserError.equals("")) fail(parserError);
                Param sp = spb.getProperties().startsWith("add_" + residue + "_");
                String actual = sp.getValue();
                assertEquals("residue, modification mass", expected1, actual);
            }

            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, modification mass\">227@C,16@M</note>" +
                "</bioml>");

            String expected1 = "16.0";
            String parserError = spb.initStaticMods();
            if (!parserError.equals("")) fail(parserError);
            Param sp = spb.getProperties().startsWith("add_M_");
            String actual = sp.getValue();
            assertEquals("residue, modification mass", expected1, actual);

            expected1 = "227.0";
            parserError = spb.initStaticMods();
            if (!parserError.equals("")) fail(parserError);
            sp = spb.getProperties().startsWith("add_C_");
            actual = sp.getValue();
            assertEquals("residue, modification mass", expected1, actual);
        }


        public void testInitStaticModsMissingValue()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "0.0";
                ip.parse("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\"></note>" +
                    "</bioml>");


                String parserError = spb.initStaticMods();
                if (!parserError.equals("")) fail(parserError);
                Param sp = spb.getProperties().startsWith("add_" + residue + "_");
                String actual = sp.getValue();
                assertEquals("residue, modification mass", expected1, actual);
            }
        }

        public void testInitStaticModsDefault()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "0.0";
                ip.parse("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "</bioml>");


                String parserError = spb.initStaticMods();
                if (!parserError.equals("")) fail(parserError);
                Param sp = spb.getProperties().startsWith("add_" + residue + "_");
                String actual = sp.getValue();
                assertEquals("residue, modification mass", expected1, actual);
            }
        }

        public void testInitStaticModsGarbage()
        {
            String value = "garbage";
            ip.parse("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, modification mass\">" + value + "</note>" +
                "</bioml>");


            String parserError = spb.initStaticMods();
            if (parserError.equals("")) fail("Expected error.");
            assertEquals("modification mass contained an invalid value(" + value + ").\n", parserError);
        }

        public void testInitPassThroughsNormal()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    passThrough.setValue("1");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("22");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("2.2");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("1");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("1.4");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("-2");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveIntegerParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());

                    passThrough.setValue("2");
                    assertEquals("", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }

        public void testInitPassThroughsMissingValue()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a real number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveIntegerParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive integer(" + value + ").\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }

        public void testInitPassThroughsNegative()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    String value = "-3";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    String value = "-3.4";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveIntegerParamsValidator.class)
                {
                    String value = "-3";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive integer(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    String value = "-1";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a real number(" + value + ").\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }

        }

        public void testInitPassThroughsGarbage()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    String value = "foo";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    String value = "bar";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    String value = "true";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    String value = "blue";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a real number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveIntegerParamsValidator.class)
                {
                    String value = "blue";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive integer(" + value + ").\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }
    }
}
