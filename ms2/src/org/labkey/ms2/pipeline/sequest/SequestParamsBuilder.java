/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.ms2.pipeline.sequest;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.pipeline.ParamParser;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.settings.AppProps;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.MS2PipelineManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * User: billnelson@uky.edu
 * Date: Sep 7, 2006
 * Time: 8:24:51 PM
 */
public class SequestParamsBuilder
{
    Map<String, String> sequestInputParams;
    File sequenceRoot;
    char[] _validResidues = {'A', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'Y', 'X', 'B', 'Z', 'O','[',']'};
    protected HashMap<String, String> supportedEnzymes = new HashMap<String, String>();
    protected final SequestParams _params;
    protected final SequestParams.Variant _variant;
    private List<File> _databaseFiles;

    public SequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
        this(sequestInputParams, sequenceRoot, SequestParams.Variant.thermosequest, null);
    }

    public SequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot, SequestParams.Variant variant, List<File> databaseFiles)
    {
        _variant = variant;
        _params = new SequestParams(variant);

        this.sequestInputParams = sequestInputParams;
        this.sequenceRoot = sequenceRoot;
        _databaseFiles = databaseFiles;

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
        supportedEnzymes.put("[X]|[K]", "lysn 1 0 K -");
        supportedEnzymes.put("[X]|[KASR]", "lysn_promisc 1 0 KASR -");
        supportedEnzymes.put("[X]|[X]", "nonspecific 0 0 - -");
        supportedEnzymes.put("[FL]|[X]", "pepsina 1 1 FL -");
        supportedEnzymes.put("[P]|[X]", "protein_endopeptidase 1 1 P -");
        supportedEnzymes.put("[E]|[X]", "staph_protease 1 1 E -");
        supportedEnzymes.put("[KMR]|{P}", "trypsin/cnbr 1 1 KMR P");
        supportedEnzymes.put("[DEKR]|{P}", "trypsin_gluc 1 1 DEKR P");
    }

    public void initXmlValues() throws SequestParamsException
    {
        List<String> errors = new ArrayList<String>();
        errors.addAll(initDatabases());
        errors.addAll(initPeptideMassTolerance());
        errors.addAll(initMassUnits());
        errors.addAll(initMassRange());
        errors.addAll(initIonScoring());
        errors.addAll(initEnzymeInfo());
        errors.addAll(initDynamicMods());
        errors.addAll(initMassType());
        errors.addAll(initStaticMods());
        errors.addAll(initPassThroughs());

        if (!errors.isEmpty())
        {
            throw new SequestParamsException(errors);
        }
    }

    public String getPropertyValue(String property)
    {
        return _params.getParam(property).getValue();
    }

    public char[] getValidResidues()
    {
        return _validResidues;
    }

    List<String> initDatabases()
    {
        List<File> databaseFiles = _databaseFiles;
        if (databaseFiles == null)
        {
            databaseFiles = new ArrayList<File>();
            String value = sequestInputParams.get("pipeline, database");
            if (value == null || value.equals(""))
            {
                return Collections.singletonList("pipeline, database; No value entered for database.");
            }
            StringTokenizer st = new StringTokenizer(value, ",");
            while (st.hasMoreTokens())
            {
                databaseFiles.add(MS2PipelineManager.getSequenceDBFile(sequenceRoot, st.nextToken().trim()));
            }
        }

        if (databaseFiles.size() != 1 && databaseFiles.size() != 2)
        {
            return Collections.singletonList("Sequest search support one or two FASTA files, not " + databaseFiles.size());
        }

        Param database1 = _params.getFASTAParam();
        File databaseFile = databaseFiles.get(0);
        if (!databaseFile.exists())
        {
            return Collections.singletonList("pipeline, database; The database does not exist(" + databaseFile + ")");
        }
        database1.setValue(databaseFile.getAbsolutePath());

        if (databaseFiles.size() > 1)
        {
            Param database2 = _params.getParam("second_database_name");
            //check for duplicate database entries
            if (!databaseFile.equals(databaseFiles.get(1)))
            {
                databaseFile = databaseFiles.get(1);
                if (!databaseFile.exists())
                {
                    return Collections.singletonList("pipeline, database; The database does not exist(" + databaseFile + ")");
                }
                database2.setValue(databaseFile.getAbsolutePath());
            }
        }
        return Collections.emptyList();
    }


    List<String> initPeptideMassTolerance()
    {
        String plusValueString =
            sequestInputParams.get("spectrum, parent monoisotopic mass error plus");

        String minusValueString =
            sequestInputParams.get("spectrum, parent monoisotopic mass error minus");

        if (plusValueString == null && minusValueString == null)
        {
            return Collections.emptyList();
        }
        if (plusValueString == null || minusValueString == null || !plusValueString.equals(minusValueString))
        {
            return Collections.singletonList("Sequest does not support asymmetric parent error ranges (minus=" +
                minusValueString + " plus=" + plusValueString + ").");
        }
        if (plusValueString.equals("") && minusValueString.equals(""))
        {
            return Collections.singletonList("No values were entered for spectrum, parent monoisotopic mass error minus/plus.");
        }
        try
        {
            Float.parseFloat(plusValueString);
        }
        catch (NumberFormatException e)
        {
            return Collections.singletonList("Invalid value for value for  spectrum, parent monoisotopic mass error minus/plus (" + plusValueString + ").");
        }
        if (Float.parseFloat(plusValueString) < 0)
        {
            return Collections.singletonList("Negative values not permitted for parent monoisotopic mass error(" + plusValueString + ").");
        }
        Param pepTol = _params.getParam("peptide_mass_tolerance");
        pepTol.setValue(plusValueString);

        return Collections.emptyList();
    }

    /** The first 3 parameters of that line are integers (0 or 1) that represents whether or not neutral losses (NH3 and H2))
    for a-ions, b-ions and y-ions are considered (0=no, 1=yes) in the correlation analysis. The last 9 parameters are
    floating point values representing a, b, c, d, v, w, x, y, and z ions respectively. The values entered for these
    paramters should range from 0.0 (don't use the ion series) to 1.0. The value entered represents the weighting that
    each ion series has (relative to the others). So an ion series with 0.5 contains half the weighting or relevance of
    an ion series with a 1.0 parameter. */
    List<String> initIonScoring()
    {
        Param ions = _params.getParam("ion_series");

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

        List<String> errors = new ArrayList<String>();
        setIonSeriesParam("scoring, a ions", ionA, errors);
        setIonSeriesParam("scoring, b ions", ionB, errors);
        setIonSeriesParam("scoring, c ions", ionC, errors);
        setIonSeriesParam("scoring, x ions", ionX, errors);
        setIonSeriesParam("scoring, y ions", ionY, errors);
        setIonSeriesParam("scoring, z ions", ionZ, errors);
        setIonSeriesParam("sequest, d ions", ionD, errors);
        setIonSeriesParam("sequest, v ions", ionV, errors);
        setIonSeriesParam("sequest, w ions", ionW, errors);
        setIonSeriesParam("sequest, a neutral loss", neutralLossA, errors);
        setIonSeriesParam("sequest, b neutral loss", neutralLossB, errors);
        setIonSeriesParam("sequest, y neutral loss", neutralLossY, errors);
        if (!errors.isEmpty())
        {
            return errors;
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
        return Collections.emptyList();
    }



    protected List<String> initEnzymeInfo()
    {
        String inputXmlEnzyme = sequestInputParams.get(org.labkey.ms2.pipeline.client.ParamParser.ENZYME);
        if (inputXmlEnzyme == null) return Collections.emptyList();
        if (inputXmlEnzyme.equals(""))
        {
            return Collections.singletonList(org.labkey.ms2.pipeline.client.ParamParser.ENZYME + " did not contain a value.");
        }
        String enzyme = removeWhiteSpace(inputXmlEnzyme);
        String[] enzymeSignatures = enzyme.split(",");
        //sequest doesn't support multiple cut sites with mixed C & N blockers
        if(enzymeSignatures.length > 1)
        {
            try
            {
                enzyme = combineEnzymes(enzymeSignatures);
            }
            catch(SequestParamsException e)
            {
                return Collections.singletonList(org.labkey.ms2.pipeline.client.ParamParser.ENZYME + " parse error:" + e.getMessage());
            }
        }
        try
        {
            String supportedEnzyme = getSupportedEnzyme(enzyme);
            if(supportedEnzyme.equals("")) return Collections.singletonList(inputXmlEnzyme + " is not a pipeline supported enzyme.");
        }
        catch(SequestParamsException e)
        {
            return Collections.singletonList(e.getMessage());
        }
        return Collections.emptyList();
    }

    protected String getSupportedEnzyme(String enzyme) throws SequestParamsException
    {
        Set<String> enzymeSigs = supportedEnzymes.keySet();
        enzyme = removeWhiteSpace(enzyme);
        enzyme = combineEnzymes(enzyme.split(","));
        for(String supportedEnzyme:enzymeSigs)
        {
            if(sameEnzyme(enzyme,supportedEnzyme))
            {
                String paramsString = supportedEnzymes.get(supportedEnzyme);
                _params.getParam("enzyme_info").setValue(paramsString);
                StringTokenizer st = new StringTokenizer(paramsString);
                return st.nextToken();
            }
        }
        return "";
    }

    protected boolean sameEnzyme(String enzyme1, String enzyme2) throws SequestParamsException
    {
        Set<Character> e1Block = new TreeSet<Character>();

        Set<Character> e2Block = new TreeSet<Character>();

        try
        {
            String[] e1Blocks = enzyme1.split("\\|");
            String[] e2Blocks = enzyme2.split("\\|");


            char bracket;
            for(int i = 0; i < 2; i++ )
            {
                if(e1Blocks[i].charAt(0) == '[') bracket = ']';
                else bracket = '}';
                CharSequence residues = e1Blocks[i].subSequence(1,e1Blocks[i].indexOf(bracket));
                for(int y = 0; y < residues.length(); y++)
                {
                   e1Block.add(residues.charAt(y));
                }

                if(e2Blocks[i].indexOf(bracket) == -1) return false;
                residues = e2Blocks[i].subSequence(1,e2Blocks[i].indexOf(bracket));
                for(int y = 0; y < residues.length(); y++)
                {
                   e2Block.add(residues.charAt(y));
                }
                if(!e1Block.equals(e2Block)) return false;
            }
        }
        catch(Exception e)
        {
            throw new SequestParamsException("Invalid enzyme definition:" + enzyme1 + " vs. " + enzyme2);
        }
        return true;
    }

    protected String combineEnzymes(String[] enzymes) throws SequestParamsException
    {
        Set<Character> block1 = new TreeSet<Character>();
        Set<Character> block2 = new TreeSet<Character>();
        char bracketOpen1 = 0;
        char bracketClose1 = 0;
        char bracketOpen2 = 0;
        char bracketClose2 = 0;
        for(String enzyme:enzymes)
        {
            String[] blocks = enzyme.split("\\|");
            if(blocks.length != 2) throw new SequestParamsException("Invalid enzyme definition:" + enzyme);
            if(bracketOpen1 == 0)
            {
                bracketOpen1 = blocks[0].charAt(0);
                if(bracketOpen1 == '[')
                {
                    if(blocks[0].charAt(1) == 'X')
                    {
                        bracketOpen1 = '{';
                        bracketClose1 = '}';
                    }
                    else
                    {
                        bracketClose1 = ']';
                    }
                }
                else if(bracketOpen1 == '{') bracketClose1 = '}';
                else throw new SequestParamsException("Invalid enzyme definition:" + enzyme);
            }
            CharSequence charSeq;
            try
            {
                    charSeq = blocks[0].substring(1,blocks[0].indexOf(bracketClose1));
            }
            catch(IndexOutOfBoundsException e){charSeq = "";}

            for(int i = 0; i < charSeq.length(); i++)
            {
                block1.add(charSeq.charAt(i));
            }
            //start second block
            if(bracketOpen2 == 0)
            {
                bracketOpen2 = blocks[1].charAt(0);
                if(bracketOpen2 == '[')
                {
                    if(blocks[1].charAt(1) == 'X')
                    {
                        bracketOpen2 = '{';
                        bracketClose2 = '}';
                    }
                    else
                    {
                        bracketClose2 = ']';
                    }
                }
                else if(bracketOpen2 == '{') bracketClose2 = '}';
                else throw new SequestParamsException("Invalid enzyme definition:" + enzyme);
            }
            try
            {
                    charSeq = blocks[1].substring(1,blocks[1].indexOf(bracketClose2));
            }
            catch(IndexOutOfBoundsException e)
            {
                charSeq = "";
            }
            for(int i = 0; i < charSeq.length(); i++)
            {
                block2.add(charSeq.charAt(i));
            }
        }

        //write combined enzyme definition
        StringBuilder returnString = new StringBuilder();
        if(block1.size() == 0)
        {
          returnString.append("[X]|");
        }
        else
        {
            returnString.append(bracketOpen1);
            for(char residue:block1)
            {
                returnString.append(residue);
            }
            returnString.append(bracketClose1);
            returnString.append('|');
        }

        if(block2.size() == 0)
        {
          returnString.append("[X]");
        }
        else
        {
            returnString.append(bracketOpen2);
            for(char residue:block2)
            {
                returnString.append(residue);
            }
            returnString.append(bracketClose2);
        }
        return returnString.toString();
    }
    

    List<String> initDynamicMods()
    {
        ArrayList<Character> defaultMods = new ArrayList<Character>();
        ArrayList<ResidueMod> workList = new ArrayList<ResidueMod>();
        // default weight "0.000000"
        defaultMods.add('S');
        defaultMods.add('C');
        defaultMods.add('M');
        defaultMods.add('X');
        defaultMods.add('T');
        defaultMods.add('Y');

        String mods = sequestInputParams.get(org.labkey.ms2.pipeline.client.ParamParser.DYNAMIC_MOD);
        if (mods == null || mods.equals("")) return Collections.emptyList();
        mods = removeWhiteSpace(mods);
        ArrayList<Character> residues = new ArrayList<Character>();
        ArrayList<String> masses = new ArrayList<String>();

        List<String> parserError = parseMods(mods, residues, masses);
        if (!parserError.isEmpty()) return parserError;

        for (int i = 0; i < masses.size(); i++)
        {
            char res = residues.get(i);
            if(res == '['||res == ']')
            {
                parserError = initDynamicTermMods(res, masses.get(i));
                if (parserError != null && !parserError.isEmpty()) return parserError;
            }
            else
            {
                defaultMods.remove(new Character(res));
                workList.add(new ResidueMod(res, masses.get(i)));

            }
        }
        if(workList.size() > 6) Collections.singletonList("Sequest will only accept a max of 6 variable modifications.");
        StringBuilder sb = new StringBuilder();
        for (ResidueMod mod :workList)
        {
            //parse mods function tested for NumberFormatxception
            float weight = Float.parseFloat(mod.getWeight());
            sb.append(weight);
            sb.append(" ");
            sb.append(mod.getRes());
            sb.append(" ");
        }
        int leftover = 6 -  workList.size();
        for(int i = 0; i < leftover; i++)
        {
            sb.append("0.000000 ");
            sb.append(defaultMods.get(i));
            sb.append(" ");
        }
        Param modProp = _params.getParam("diff_search_options");
        modProp.setValue(sb.toString().trim());
        return parserError;
    }


    List<String> initStaticMods()
    {
        String mods = sequestInputParams.get(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD);

        ArrayList<Character> residues = new ArrayList<Character>();
        ArrayList<String> masses = new ArrayList<String>();

        List<String> parserError = parseMods(mods, residues, masses);
        if (!parserError.isEmpty()) return parserError;
        for (int i = 0; i < masses.size(); i++)
        {
            Param modProp;
            if(residues.get(i) == '[')
            {
                modProp = _params.startsWith("add_Nterm_peptide");
            }
            else if(residues.get(i) == ']')
            {
                modProp = _params.startsWith("add_Cterm_peptide");
            }
            else
            {
                modProp = _params.startsWith("add_" + residues.get(i) + "_");
            }
            modProp.setValue(masses.get(i));
        }
        return parserError;
    }

    private List<String> parseMods(String mods, ArrayList<Character> residues, ArrayList<String> masses)
    {
        Float massF;
        if (mods == null || mods.equals("")) return Collections.emptyList();

        StringTokenizer st = new StringTokenizer(mods, ",");
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            token = removeWhiteSpace(token);
            if (token.charAt(token.length() - 2) != '@' && token.length() > 3)
            {
                return Collections.singletonList("modification mass contained an invalid value(" + mods + ").");
            }
            Character residue = token.charAt(token.length() - 1);
            if (!isValidResidue(residue))
            {
                return Collections.singletonList("modification mass contained an invalid residue(" + residue + ").");
            }
            residues.add(residue);
            String mass = token.substring(0, token.length() - 2);


            try
            {
                massF = Float.parseFloat(mass);
            }
            catch (NumberFormatException e)
            {
                return Collections.singletonList("modification mass contained an invalid mass value (" + mass + ")");
            }
            masses.add(massF.toString());
        }
        return Collections.emptyList();
    }


    List<String> initDynamicTermMods(char term, String mass)
    {
        Param termProp = _params.getParam("term_diff_search_options");
        String defaultTermMod = termProp.getValue();
        StringTokenizer st = new StringTokenizer(defaultTermMod);
        String defaultCTerm = st.nextToken();
        String defaultNTerm = st.nextToken();

        if (mass == null|| mass.length() == 0)
        {
            return Collections.singletonList("The mass value for term_diff_search_options is empty.");
        }
        if(term == '[') defaultNTerm = mass;
        else if(term == ']') defaultCTerm = mass;
        termProp.setValue(defaultCTerm + " " + defaultNTerm);
        return Collections.emptyList();
    }

    List<String> initMassType()
    {
        String massType = sequestInputParams.get("spectrum, fragment mass type");
        String sequestValue;
        if (massType == null)
        {
            return Collections.emptyList();
        }
        if (massType.equals(""))
        {
            return Collections.singletonList("spectrum, fragment mass type contains no value.");
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
            return Collections.singletonList("spectrum, fragment mass type contains an invalid value(" + massType + ").");
        }
        _params.getParam("mass_type_fragment").setValue(sequestValue);
        return Collections.emptyList();
    }

    List<String> initMassUnits()
    {
        String pepMassUnit = sequestInputParams.get("spectrum, parent mass error units");
        if(pepMassUnit == null || pepMassUnit.equals(""))
        {
            //Check depricated param
            pepMassUnit = sequestInputParams.get("spectrum, parent monoisotopic mass error units");
            if(pepMassUnit == null || pepMassUnit.equals(""))
                return Collections.emptyList();
        }
        if(pepMassUnit.equalsIgnoreCase("daltons"))
            _params.getParam("peptide_mass_units").setValue("0");
        else if(pepMassUnit.equalsIgnoreCase("ppm"))
            _params.getParam("peptide_mass_units").setValue("2");
        else
            return Collections.singletonList("spectrum, parent monoisotopic mass error units contained an invalid value for Sequest: (" +
                 pepMassUnit + ").");
        return Collections.emptyList();
    }


   List<String> initMassRange()
   {
       if (_variant == SequestParams.Variant.makedb)
       {
           String rangeMin = sequestInputParams.get(AbstractMS2SearchTask.MINIMUM_PARENT_M_H);
           String rangeMax = sequestInputParams.get(AbstractMS2SearchTask.MAXIMUM_PARENT_M_H);

           if (StringUtils.trimToNull(rangeMin) != null)
           {
               try
               {
                    Double.parseDouble(rangeMin);
               }
               catch( NumberFormatException e)
               {
                   return Collections.singletonList(AbstractMS2SearchTask.MINIMUM_PARENT_M_H + " is an invalid value: (" + rangeMin + ").");
               }
               _params.getParam("min_peptide_mass").setValue(rangeMin);
           }

           if (StringUtils.trimToNull(rangeMax) != null)
           {
               try
               {
                    Double.parseDouble(rangeMax);
               }
               catch( NumberFormatException e)
               {
                   return Collections.singletonList(AbstractMS2SearchTask.MAXIMUM_PARENT_M_H + " is an invalid value: (" + rangeMax + ").");
               }
               _params.getParam("max_peptide_mass").setValue(rangeMax);
           }
       }
       return Collections.emptyList();
   }

    List<String> initPassThroughs()
    {
        List<String> parserError = new ArrayList<String>();
        Collection<SequestParam> passThroughs = _params.getPassThroughs();
        for (SequestParam passThrough : passThroughs)
        {
            String label = passThrough.getInputXmlLabels().get(0);
            String value = sequestInputParams.get(label);
            if (value == null)
            {
                continue;
            }
            String defaultValue = passThrough.getValue();
            passThrough.setValue(value);
            String errorString = passThrough.validate();
            if(errorString.length() > 0 )
            {
                passThrough.setValue(defaultValue);
                parserError.add(errorString);
            }
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
            if (prop.getName().equals("sequence_header_filter") ||
                prop.getName().equals("add_W_Tryptophan"))
            {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void setIonSeriesParam(String xmlLabel, StringBuilder labelValue, List<String> errors)
    {
        String value;
        String iPValue = sequestInputParams.get(xmlLabel);
        if (iPValue == null)
        {
            return;
        }
        if (iPValue.equals(""))
        {
            errors.add(xmlLabel + " did not contain a value.");
            return;
        }
        if (iPValue.equalsIgnoreCase("yes"))
        {
            labelValue.delete(0, labelValue.length());
            value = (xmlLabel.endsWith("loss")) ? "1" : "1.0";
            labelValue.append(value);
        }
        else if (sequestInputParams.get(xmlLabel).equalsIgnoreCase("no"))
        {
            labelValue.delete(0, labelValue.length());
            value = (xmlLabel.endsWith("loss")) ? "0" : "0.0";
            labelValue.append(value);
        }
        else
        {
            errors.add(xmlLabel + " contained an invalid value(" + iPValue + ").");
        }
    }

    public boolean isValidResidue(char residue)
    {
        return isValidResidue(Character.toString(residue));
    }

    public boolean isValidResidue(String residueString)
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
    //The Sequest2xml uses an older version of the sequest.params file(version = 1)supported sequest uses version = 2;
    String lookUpEnzyme(String enzyme)
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

        Set<String> supportedEnzymesKes = supportedEnzymes.keySet();
        boolean matches = false;
        for (String lookUp : supportedEnzymesKes)
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
            if (matches) return supportedEnzymes.get(lookUp);
        }
        return null;
    }

    //Used with JUnit
    private SequestParams getProperties()
    {
        return _params;
    }

    protected String removeWhiteSpace(String value)
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

    private class ResidueMod
    {

        private Character res;
        private String weight;

        private ResidueMod(Character res, String weight)
        {
            this.res = res;
            this.weight = weight;
        }

        public Character getRes()
        {
            return res;
        }

        public void setRes(Character res)
        {
            this.res = res;
        }

        public String getWeight()
        {
            return weight;
        }

        public void setWeight(String weight)
        {
            this.weight = weight;
        }

        public boolean equals(ResidueMod o)
        {
            if(this.res.equals(o)) return this.weight.equals(o);
            return false;
        }
    }
    //JUnit TestCase
    public static class TestCase extends Assert
    {
        SequestParamsBuilder spb;
        ParamParser ip;
        String dbPath;
        File root;

        @Before
        public void setUp() throws Exception
        {
            ip = PipelineJobService.get().createParamParser();
            String projectRoot = AppProps.getInstance().getProjectRoot();
            root = new File(new File(projectRoot), "/sampledata/xarfiles/ms2pipe/databases");
            dbPath = root.getCanonicalPath();
            spb = new SequestParamsBuilder(ip.getInputParameters(), root);
        }

        @After
        public void tearDown()
        {
            ip = null;
            spb = null;
        }

        public void fail(List<String> messages)
        {
            fail(StringUtils.join(messages, '\n'));
        }

        public void parseParams(String xml)
        {
            ip.parse(xml);
            spb = new SequestParamsBuilder(ip.getInputParameters(), root);
        }

        @Test
        public void testInitDatabasesNormal() throws IOException
        {
            String value = "Bovine_mini.fasta";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getFASTAParam();
            assertEquals(new File(dbPath + File.separator + value).getCanonicalPath(), new File(sp.getValue()).getCanonicalPath());
        }

        @Test
        public void testInitDatabasesMissingValue()
        {
            String value = "";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertEquals("pipeline, database; No value entered for database.", parserError.get(0));
        }

        @Test
        public void testInitDatabasesMissingInput()
        {
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertEquals("pipeline, database; No value entered for database.", parserError.get(0));
        }

        @Test
        public void testInitDatabasesGarbage()
        {
            String value = "garbage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertTrue(parserError.get(0).contains("pipeline, database; The database does not exist"));
            assertTrue(parserError.get(0).contains("garbage"));

            value = "Bovine_mini.fasta, garbage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertTrue(parserError.get(0).contains("pipeline, database; The database does not exist"));
            assertTrue(parserError.get(0).contains("garbage"));

            value = "garbage, Bovine_mini.fasta";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertTrue(parserError.get(0).contains("pipeline, database; The database does not exist"));
            assertTrue(parserError.get(0).contains("garbage"));
        }

        @Test
        public void testInitPeptideMassToleranceNormal()
        {
            float expected = 30.0f;
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            float actual = Float.parseFloat(sp.getValue());
            assertEquals("peptide_mass_tolerance", expected, actual, 0.00);
        }

        @Test
        public void testInitPeptideMassToleranceMissingValue()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\"></note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">4.0</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=4.0 plus=).", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">4.0</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\"></note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus= plus=4.0).", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\"></note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\"></note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("No values were entered for spectrum, parent monoisotopic mass error minus/plus.", parserError.get(0));
        }

        @Test
        public void testInitPeptideMassToleranceNegative()
        {
            float expected = -30.0f;
            String defaultValue = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            String actual = sp.getValue();
            assertEquals("parameter value changed", defaultValue, actual);
            assertEquals("Negative values not permitted for parent monoisotopic mass error(" + expected + ").", parserError.get(0));
        }

        @Test
        public void testInitPeptideMassToleranceInvalid()
        {
            String expected = "garbage";
            String defaultValue = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            String actual = sp.getValue();
            assertEquals("parameter value changed", defaultValue, actual);
            assertEquals("Invalid value for value for  spectrum, parent monoisotopic mass error minus/plus (garbage).", parserError.get(0));
        }


        @Test
        public void testInitPeptideMassToleranceMissingInput()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">5.0</note>" +
                "</bioml>");
            List<String> parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=null plus=5.0).", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">5.0</note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=5.0 plus=null).", parserError.get(0));
        }

        @Test
        public void testInitPeptideMassToleranceDefault()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");
            List<String> parserError = spb.initPeptideMassTolerance();
            if (!parserError.isEmpty()) fail(parserError);
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
        }

        @Test
        public void testInitMassTypeNormal()
        {
            String expected = "AveRage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", "0", actual);

            expected = "MonoIsotopic";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">" + expected + "</note>" +
                "</bioml>");

            parserError = spb.initMassType();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("mass_type_fragment");
            actual = sp.getValue();
            assertEquals("mass_type_fragment", "1", actual);
        }

        @Test
        public void testInitMassTypeMissingValue()
        {
            String expected = "1";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\"></note>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (parserError.isEmpty()) fail("No error message.");
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
            assertEquals("mass_type_fragment", "spectrum, fragment mass type contains no value.", parserError.get(0));
        }

        @Test
        public void testInitMassTypeDefault()
        {
            String expected = "1";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
        }

        @Test
        public void testInitMassTypeGarbage()
        {
            String expected = "1";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">garbage</note>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (parserError.isEmpty()) fail("No error message.");
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
            assertEquals("mass_type_fragment", "spectrum, fragment mass type contains an invalid value(garbage).", parserError.get(0));
        }

        @Test
        public void testInitIonScoringNormal()
        {
            String expected = "0 0 0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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

            List<String> parserError = spb.initIonScoring();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);

            expected = "1 1 1 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0";
            parseParams("<?xml version=\"1.0\"?>" +
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
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        @Test
        public void testInitIonScoringMissingValue()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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

            List<String> parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, y neutral loss did not contain a value.", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "scoring, c ions did not contain a value.", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, d ions did not contain a value.", parserError.get(0));
        }

        @Test
        public void testInitIonScoringMissingDefault()
        {
            String expected = "0 0 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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

            List<String> parserError = spb.initIonScoring();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        @Test
        public void testInitIonScoringDefault()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initIonScoring();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        @Test
        public void testInitIonScoringGarbage()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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

            List<String> parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, y neutral loss contained an invalid value(garbage).", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "scoring, c ions contained an invalid value(garbage).", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
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
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, d ions contained an invalid value(garbage).", parserError.get(0));
        }


        @Test
        public void testInitEnzymeInfoNormal()
        {
            //Testing no enzyme
            String expected2 = "nonspecific 0 0 - -";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|[X]</note>" +
                "</bioml>");

            List<String> parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);

            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

            expected2 = "trypsin_k 1 1 K P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[K]|{P}</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

            expected2 = "pepsina 1 1 FL -";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[LF]|[X]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
        }

        @Test
        public void testInitEnzymeInfoDefault()
        {
//            String expected2 = "Trypsin(KR/P)\t\t\t\t1\tKR\t\tP";
            String expected2 = "trypsin 1 1 KR P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

           List<String> parserError = spb.initEnzymeInfo();
           if (!parserError.isEmpty()) fail(parserError);
//            Param sp = spb.getProperties().getParam("enzyme_number");
//            String actual = sp.getValue();
//            assertEquals("enzyme_number", expected1, actual);
//
//            sp = spb.getProperties().getParam("enzyme1");
//            actual = sp.getValue();
//            assertEquals("enzyme_description", expected2, actual);

            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
        }

        @Test
        public void testInitEnzymeInfoMissingValue()
        {
            String expected2 = "trypsin 1 1 KR P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\"></note>" +
                "</bioml>");

            List<String> parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("enzyme_description", "protein, cleavage site did not contain a value.", parserError.get(0));
        }

        @Test
        public void testInitEnzymeInfoGarbage()
        {
            String expected2 = "trypsin 1 1 KR P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">foo</note>" +
                "</bioml>");

            List<String> parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");


            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("enzyme_description", "Invalid enzyme definition:foo", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[CV]|{P},[KR]|{P}</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("[CV]|{P},[KR]|{P} is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">{P}|[KR]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("{P}|[KR] is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[a]|[X]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("[a]|[X] is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|[a]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("[X]|[a] is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|P</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("Invalid enzyme definition:[X]|P", parserError.get(0));
        }

        @Test
        public void testInitDynamicModsNormal()
        {
            String expected1 = "16.0 M 0.000000 S 0.000000 C 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+16@M</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);

            expected1 = "16.0 M 0.000000 S 0.000000 C 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">16@M</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);

            expected1 = "-16.0 M 0.000000 S 0.000000 C 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">- 16.0000 @ M</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);


            expected1 = "16.0 M 9.0 C 0.000000 S 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\"> 16@M,9@C </note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        @Test
        public void testInitDynamicModsMissingValue()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\"></note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        @Test
        public void testInitDynamicModsDefault()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        @Test
        public void testInitDynamicModsGarbage()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">16@J</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (parserError.isEmpty()) fail("Error expected.");
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
            assertEquals("diff_search_options", "modification mass contained an invalid residue(J).", parserError.get(0));

            expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">G@18</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (parserError.isEmpty()) fail("Error expected.");
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
            assertEquals("diff_search_options", "modification mass contained an invalid value(G@18).", parserError.get(0));

        }

        @Test
        public void testInitTermDynamicModsNormal()
        {
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String defaultValue = sp.getValue();
            String expected1 = "0.0 42.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+42.0@[</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "-88.0 42.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+ 42.0 @[,-88 @ ]</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "-88.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">-88@]</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

        }

        @Test
        public void testInitTermDynamicModsMissingValue()
        {
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String defaultValue = sp.getValue();
            String expected1 = "0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+0.0@[</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "0.0 42.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+ 42.0 @[</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
        }

        @Test
        public void testInitTermDynamicModsDefault()
        {
            String expected1 = "0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
        }

        @Test
        public void testInitStaticModsNormal()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "227.0";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\">+227@" + residue + "</note>" +
                    "</bioml>");


                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");   
                }
                String actual = sp.getValue();
                assertEquals(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD, expected1, actual);
            }

            for (char residue : validResidues)
            {
                String expected1 = "-9.0";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\">- 9 @ " + residue + "</note>" +
                    "</bioml>");


                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");
                }
                String actual = sp.getValue();
                assertEquals(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD, expected1, actual);
            }

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, modification mass\">227@C,16@M</note>" +
                "</bioml>");

            String expected1 = "16.0";
            List<String> parserError = spb.initStaticMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().startsWith("add_M_");
            String actual = sp.getValue();
            assertEquals(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD, expected1, actual);

            expected1 = "227.0";
            parserError = spb.initStaticMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().startsWith("add_C_");
            actual = sp.getValue();
            assertEquals(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD, expected1, actual);
        }


        @Test
        public void testInitStaticModsMissingValue()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "0.0";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\"></note>" +
                    "</bioml>");


                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");
                }
                String actual = sp.getValue();
                assertEquals(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD, expected1, actual);
            }
        }

        @Test
        public void testInitStaticModsDefault()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "0.0";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "</bioml>");

                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");
                }
                String actual = sp.getValue();
                assertEquals(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD, expected1, actual);
            }
        }

        @Test
        public void testInitStaticModsGarbage()
        {
            String value = "garbage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, modification mass\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initStaticMods();
            if (parserError.isEmpty()) fail("Expected error.");
            assertEquals("modification mass contained an invalid value(" + value + ").", parserError.get(0));
        }

        @Test
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
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    ((ListParamsValidator)passThrough.getValidator()).setList(new String[]{"0","1","2"});
                    passThrough.setValue("2");
                    assertEquals("", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }

        @Test
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
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    String listValue = "";
                    passThrough.setValue(listValue);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", " + "this value is not set.\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }

        @Test
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
                    String value = "-1.3";
                    passThrough.setValue(value);
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    ((ListParamsValidator)passThrough.getValidator()).setList(new String[]{"a","b","c"});
                    passThrough.setValue("-1");
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", " + "this value (-1) is not in the valid list.\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }

        }

        @Test
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
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    ((ListParamsValidator)passThrough.getValidator()).setList(new String[]{"a","b","c"});
                    passThrough.setValue("foo");
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", " + "this value (foo) is not in the valid list.\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }
    }

    public void writeFile(File output) throws SequestParamsException
    {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(output));
            writer.write(getSequestParamsText());
        }
        catch (IOException e)
        {
            throw new SequestParamsException(e);
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException eio) {}
            }
        }
    }
}
