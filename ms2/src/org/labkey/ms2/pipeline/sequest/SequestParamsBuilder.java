/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.labkey.api.pipeline.ParamParser;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.Pair;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
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
public abstract class SequestParamsBuilder
{
    public static final String DUMMY_FASTA_NAME = "~~~~~~~DUMMY_FASTA_NAME_FOR_TESTING~~~~~~~~~~`````.fasta";

    protected Map<String, String> sequestInputParams;
    File sequenceRoot;
    char[] _validResidues = {'A', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'Y', 'X', 'B', 'Z', 'O','[',']'};
    protected HashMap<String, String> supportedEnzymes = new HashMap<>();
    protected final AbstractSequestParams _params;
    protected final AbstractSequestParams.Variant _variant;
    private List<File> _databaseFiles;

    public SequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
        this(sequestInputParams, sequenceRoot, SequestParams.Variant.thermosequest);
    }

    public SequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot, SequestParams.Variant variant)
    {
        this(sequestInputParams, sequenceRoot, variant, null);
    }

    public SequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot, SequestParams.Variant variant, List<File> databaseFiles)
    {
        _variant = variant;
        _params = createSequestParams(variant);

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

        initSubclass();
    }

    protected AbstractSequestParams createSequestParams(AbstractSequestParams.Variant variant)
    {
        return new SequestParams(variant);
    }

    protected abstract void initSubclass();

    public void initXmlValues() throws SequestParamsException
    {
        List<String> errors = new ArrayList<>();
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

    protected List<String> initDatabases()
    {
        List<File> databaseFiles = _databaseFiles;
        if (databaseFiles == null)
        {
            databaseFiles = new ArrayList<>();
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
            return Collections.singletonList("One or two FASTA files is supported, not " + databaseFiles.size());
        }

        Param database1 = _params.getFASTAParam();
        File databaseFile = databaseFiles.get(0);
        if (!databaseFile.exists() && !DUMMY_FASTA_NAME.equals(databaseFile.getName()))
        {
            return Collections.singletonList("pipeline, database; The database does not exist(" + databaseFile + ")");
        }
        database1.setValue(databaseFile.getAbsolutePath());

        if (databaseFiles.size() > 1)
        {
            Param database2 = _params.getParam("second_database_name");
            //check for duplicate database entries
            if (database2 != null && !databaseFile.equals(databaseFiles.get(1)))
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


    protected List<String> initPeptideMassTolerance()
    {
        String plusValueString =
            sequestInputParams.get("spectrum, parent monoisotopic mass error plus");

        String minusValueString =
            sequestInputParams.get("spectrum, parent monoisotopic mass error minus");

        if (plusValueString == null && minusValueString == null)
        {
            String valueString = sequestInputParams.get("spectrum, parent monoisotopic mass error");
            if (valueString != null)
            {
                minusValueString = valueString;
                plusValueString = valueString;
            }
            else
            {
                return Collections.emptyList();
            }
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
    parameters should range from 0.0 (don't use the ion series) to 1.0. The value entered represents the weighting that
    each ion series has (relative to the others). So an ion series with 0.5 contains half the weighting or relevance of
    an ion series with a 1.0 parameter. */
    protected List<String> initIonScoring()
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

        List<String> errors = new ArrayList<>();
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
        String inputXmlEnzyme = sequestInputParams.get(ParameterNames.ENZYME);
        if (inputXmlEnzyme == null) return Collections.emptyList();
        if (inputXmlEnzyme.equals(""))
        {
            return Collections.singletonList(ParameterNames.ENZYME + " did not contain a value.");
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
                return Collections.singletonList(ParameterNames.ENZYME + " parse error:" + e.getMessage());
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
        Set<Character> e1Block = new TreeSet<>();

        Set<Character> e2Block = new TreeSet<>();

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
        Set<Character> block1 = new TreeSet<>();
        Set<Character> block2 = new TreeSet<>();
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
    

    public List<String> initDynamicMods()
    {
        ArrayList<Character> defaultMods = new ArrayList<>();
        ArrayList<ResidueMod> workList = new ArrayList<>();
        // default weight "0.000000"
        defaultMods.add('S');
        defaultMods.add('C');
        defaultMods.add('M');
        defaultMods.add('X');
        defaultMods.add('T');
        defaultMods.add('Y');

        String mods = sequestInputParams.get(ParameterNames.DYNAMIC_MOD);
        if (mods == null || mods.equals("")) return Collections.emptyList();
        mods = removeWhiteSpace(mods);
        ArrayList<Character> residues = new ArrayList<>();
        ArrayList<String> masses = new ArrayList<>();

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


    protected List<String> initStaticMods()
    {
        String mods = sequestInputParams.get(ParameterNames.STATIC_MOD);

        ArrayList<Character> residues = new ArrayList<>();
        ArrayList<String> masses = new ArrayList<>();

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

    protected List<String> parseMods(String mods, List<Character> residues, List<String> masses)
    {
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
            Character residue = Character.toUpperCase(token.charAt(token.length() - 1));
            if (!isValidResidue(residue))
            {
                return Collections.singletonList("modification mass contained an invalid residue(" + residue + ").");
            }
            residues.add(residue);
            String mass = token.substring(0, token.length() - 2);


            try
            {
                Float.parseFloat(mass);
            }
            catch (NumberFormatException e)
            {
                return Collections.singletonList("modification mass contained an invalid mass value (" + mass + ") for residue " + residue);
            }
            if (mass.startsWith("+"))
            {
                mass = mass.substring(1);
            }
            masses.add(mass);
        }
        return Collections.emptyList();
    }


    protected abstract List<String> initDynamicTermMods(char term, String mass);

    protected Map.Entry<String, String> getParameterValue(String... parameterNames)
    {
        for (String parameterName : parameterNames)
        {
            if (sequestInputParams.containsKey(parameterName))
            {
                return new Pair<>(parameterName, sequestInputParams.get(parameterName));
            }
        }
        return new Pair<>(parameterNames[0], null);
    }

    List<String> initMassType()
    {
        Map.Entry<String, String> value = getParameterValue("spectrum, fragment mass type", "sequest, mass_type_fragment");
        String sequestValue;
        String massType = value.getValue();
        if (massType == null)
        {
            return Collections.emptyList();
        }
        if (massType.equals(""))
        {
            return Collections.singletonList("\"" + value.getKey() + "\" contains no value.");
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
            return Collections.singletonList("\"" + value.getKey() + "\" contains an invalid value(" + massType + ").");
        }
        _params.getParam("mass_type_fragment").setValue(sequestValue);
        return Collections.emptyList();
    }

    List<String> initMassUnits()
    {
        String pepMassUnit = sequestInputParams.get("spectrum, parent mass error units");
        if(pepMassUnit == null || pepMassUnit.equals(""))
        {
            //Check deprecated param
            pepMassUnit = sequestInputParams.get("spectrum, parent monoisotopic mass error units");
            if(pepMassUnit == null || pepMassUnit.equals(""))
                return Collections.emptyList();
        }
        if(pepMassUnit.equalsIgnoreCase("daltons") || pepMassUnit.equalsIgnoreCase("amu"))
            _params.getParam("peptide_mass_units").setValue("0");
        else if(pepMassUnit.equalsIgnoreCase("mmu"))
            _params.getParam("peptide_mass_units").setValue("1");
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
        List<String> parserError = new ArrayList<>();
        Collection<SequestParam> passThroughs = _params.getPassThroughs();
        for (SequestParam passThrough : passThroughs)
        {
            String value = passThrough.findValue(sequestInputParams);
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
            sb.append(prop.convert(_variant.getCommentPrefix()));
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
    public AbstractSequestParams getProperties()
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

    public enum PeptideTerminalModificationType
    {
        None, C, N
    }

    protected static class ResidueMod
    {
        private final char res;
        @NotNull
        private final String weight;
        private final PeptideTerminalModificationType type;

        public ResidueMod(char res, @NotNull String weight)
        {
            this(res, weight, PeptideTerminalModificationType.None);
        }

        public ResidueMod(char res, @NotNull String weight, @NotNull PeptideTerminalModificationType type)
        {
            this.res = res;
            this.weight = weight;
            this.type = type;
        }

        public char getRes()
        {
            return res;
        }

        @NotNull
        public String getWeight()
        {
            return weight;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResidueMod that = (ResidueMod) o;

            if (res != that.res) return false;
            return weight.equals(that.weight);
        }

        @Override
        public int hashCode()
        {
            int result = (int) res;
            result = 31 * result + weight.hashCode();
            return result;
        }

        public boolean isCTerminalProtein()
        {
            return res == ']';
        }

        public boolean isNTerminalProtein()
        {
            return res == '[';
        }

        public boolean isTerminalProtein()
        {
            return isCTerminalProtein() || isNTerminalProtein();
        }

        public PeptideTerminalModificationType getType()
        {
            return type;
        }
    }

    public abstract static class AbstractSequestTestCase extends Assert
    {
        protected SequestParamsBuilder spb;
        protected ParamParser ip;
        protected String dbPath;
        protected File root;

        @Before
        public void setUp() throws Exception
        {
            ip = PipelineJobService.get().createParamParser();
            root = JunitUtil.getSampleData(null, "xarfiles/ms2pipe/databases");
            dbPath = root.getCanonicalPath();
            spb = createParamsBuilder();
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
            ip.parse(new ReaderInputStream(new StringReader(xml)));
            spb = createParamsBuilder();
        }

        public abstract SequestParamsBuilder createParamsBuilder();
    }

    public void writeFile(File output) throws SequestParamsException
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output)))
        {
            writer.write(getSequestParamsText());
        }
        catch (IOException e)
        {
            throw new SequestParamsException(e);
        }
    }
}
