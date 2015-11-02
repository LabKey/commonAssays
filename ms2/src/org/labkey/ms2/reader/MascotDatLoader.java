package org.labkey.ms2.reader;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.util.Pair;
import org.labkey.ms2.MS2Modification;
import org.labkey.ms2.PeptideImporter;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by susanh on 10/22/15.
 */
public class MascotDatLoader extends MS2Loader
{
    public static final String DB_PREFIX = "DB=";
    public static final String FASTAFILE_PREFIX = "fastafile=";
    public static final String ENZYME_PREFIX = "CLE=";
    public static final String ENZYME_PREFIX_LC = "cle=";
    public static final String DEFAULT_ENZYME = "trypsin";
    public static final String SEARCH_ENGINE_NAME = "MASCOT";

    // Content-Type: multipart/mixed; boundary=gc0p4Jq0M2Yt08jU534c0p
    private static final Pattern BOUNDARY_MARKER_LINE = Pattern.compile("\\s*Content-Type: multipart/mixed; boundary=(.*)");
    private static final int BOUNDARY_MARKER_GROUP_NUM = 1;
    // Content-Type: application/x-Mascot; name="<name>"
    private static final Pattern CONTENT_TYPE_LINE = Pattern.compile("\\s*Content-Type: application/x-Mascot; name=\"([^\\d]*)(\\d*)\"");
    private static final int SECTION_NAME_GROUP_NUM = 1;
    private static final int QUERY_SECTION_INDEX_GROUP_NUM = 2;


    // For matching generic assignments of values to keys
    //  Key=value
    private static final Pattern KEY_VALUE_LINE = Pattern.compile("([^=]*)=(.*)");
    private static final int KV_KEY_GROUP_NUM = 1;
    private static final int KV_VALUE_GROUP_NUM = 2;
    // Example lines:
    //  delta1=15.233,Me-ester (C-term)
    //  delta2=44.123,ICAT-C:13C(9) (C)
    //  delta4=15.994919,Oxidation (HW)
    private static final Pattern MASS_DELTA_LINE = Pattern.compile("delta(\\d+)=([^,]*),(.*\\((.*)\\))");
    // FixedMod1=57.021464,Carbamidomethyl (C)
    private static final Pattern MASS_FIXED_MOD_LINE = Pattern.compile("FixedMod(\\d+)=([^,]*),(.*\\((.*)\\))");
    // FixedModResidues1=C
    private static final Pattern MASS_FIXED_MOD_RESIDUES_LINE = Pattern.compile("FixedModResidues(\\d+)=(.*)");
    private static final int MASS_INDEX_GROUP_NUM = 1;
    private static final int MASS_DELTA_GROUP_NUM = 2;
    private static final int MASS_FIXED_MOD_RESIDUES_GROUP_NUM = 2;
    private static final int MASS_DELTA_NAME_GROUP_NUM = 3;
    private static final int MASS_RESIDUES_GROUP_NUM = 4;


    // Peptides are on two separate lines:
    // Peptide Line 1:
    //      q1_p1=0,605.641861,-0.122701,4,SASVSR,10,00000000,15.51,00020020000000000,0,0;"gi|15599453|30S_ribosomal_pro":0:49:54:2
    // OR, for multiple protein matches:
    //      q4_p1=1,610.666458,-1.616048,2,RGGGHK,8,00000000,2.94,00000020000000000,0,0;"gi|11036695|putative_L2_ribos":0:54:59:1,"gi|1685374|ribosomal_protein_":0:55:60:2
    // If there was no match for a peptide, it may be represented like this:
    //      q2_p1=-1
    //                                                                                               &first, &mass    , &massdiff,&nextionmatch,nextpeptide,&first,nextmods,&nextionscore,discard          ,&first,&first;"nextprotein"                  :);
    //                                                                            q 1    _p 1    =  0     ,605.641861,-0.122701 ,4             ,SASVSR     ,10    ,00000000,15.51        ,00020020000000000,0     ,0     ;"gi|15599453|30S_ribosomal_pro":0    :49   :54   :2
    //              private static final Pattern PEPTIDE_LINE1 = Pattern.compile("q(\\d+)_p(\\d+)=\\d+    ,(.*)      ,(.*)      ,(\\d*)        ,(.*)       ,\\d+  ,(.*)    ,(.*)         ,.*                             ;"(.*)"                         :\d+:\d+:\d+:\d+"");
    //                                                                              1      2              3          4          5               6                  7        8                                               9                              ");
    private static final Pattern PEPTIDE_PROTEIN = Pattern.compile("\"([^\"]*)\":\\d+:\\d+:\\d+:\\d+");
    private static final int PEPTIDE_PROTEIN_NAME_GROUP_NUM = 1;

    private static final Pattern PEPTIDE_LINE1 = Pattern.compile("q(\\d+)_p(\\d+)=\\d+,([^,]*),([^,]*),(\\d*),([^,]*),\\d+,([^,]*),([^,]*),.*;" + PEPTIDE_PROTEIN + "(.*)");
    private static final int QUERY_INDEX_GROUP_NUM = 1;
    private static final int HIT_RANK_GROUP_NUM = 2;
    private static final int PEPTIDE_MASS_GROUP_NUM = 3;
    private static final int PEPTIDE_MASS_DIFF_GROUP_NUM = 4;
    private static final int PEPTIDE_ION_MATCH_GROUP_NUM = 5;
    private static final int PEPTIDE_NAME_GROUP_NUM = 6;
    private static final int PEPTIDE_MODS_GROUP_NUM = 7;
    private static final int PEPTIDE_ION_SCORE_GROUP_NUM = 8;
    private static final int PEPTIDE_PROTEIN_GROUP_NUM = 9;
    private static final int PEPTIDE_ALTERNATE_PROTEIN_GROUP_NUM = 10;


    // Peptide Line 2:
    //      q1_p1_terms=K,I
    // OR, for multiple protein matches
    //      q1_p5_terms=R,I:R,I
    private static final Pattern PEPTIDE_LINE2 = Pattern.compile("q(\\d+)_p(\\d+)_terms=([^,]*),([^:]*)(:([^,]*),([^:]*))*");
    private static final int PEPTIDE_TERMS_PREVIOUS_AA_GROUP_NUM = 3;
    private static final int PEPTIDE_TERMS_NEXT_AA_GROUP_NUM = 4;

    private static final Pattern IDENTITY_SCORE_SUMMARY_LINE = Pattern.compile("qmatch(\\d+)=(.*)");
    private static final Pattern HOMOLOGY_SCORE_SUMMARY_LINE = Pattern.compile("qplughole(\\d+)=(.*)");
    private static final Pattern PRECURSOR_ION_CHARGE_SUMMARY_LINE = Pattern.compile("qexp(\\d+)=([^,]*)(,(\\d).)?");

    private static final int ION_CHARGE_GROUP_NUM = 4;
    private static final int UNKNOWN_ION_CHARGE_VALUE = 4;

    private static final String QUERY_TITLE_PREFIX = "title=";
    private static final String QUERY_SCANS_PREFIX = "scans=";
    private static final String QUERY_RETENTION_TIME_PREFIX = "rtinseconds=";

    // the title line starts out looking like this:
    //  title=CAexample_mini%2e0110%2e0110%2e1
    // once URL decoded, the value looks like this:
    //  CAexample_mini.0110.0110.1
    public static final Pattern QUERY_TITLE_SCAN_REGEX = Pattern.compile("\\.??(\\d{1,6})\\.(\\d{1,6})\\.(\\d{1})\\.??[a-zA-z0-9_]*?$");
    public static final int START_SCAN_GROUP_NUM = 1;
    public static final int END_SCAN_GROUP_NUM = 2;
    public static final int CHARGE_GROUP_NUM = 3; // Not currently used as we extract the charge value elsewhere, left for documentation

    /**
     * The sections that appear in a .dat file.  It's unclear if they always appear in this order or not, but empirical evidence
     * suggests they do.  If they don't it's not a problem, though.
     */
    public enum Section {
        PARAMETERS,
        MASSES,
        UNIMOD,            // Not currently used
        ENZYME,            // Not currently used
        TAXONOMY,          // Not currently used
        HEADER,
        SUMMARY,
        DECOY_SUMMARY,     // Not currently used
        PEPTIDES,
        DECOY_PEPTIDES,    // Not currently used
        PROTEINS,          // Not currently used
        QUERY,
        INDEX;             // Not currently used
    }

    private String _boundaryMarker = null;
    private BufferedReader _reader = null;
    private String _currentLine = null;
    private Section _currentSection = null;
    private Integer _currentQueryNum = null;
    private long _charactersRead = 0L; // used for indicating progress; not exact, but probably close enough

    private Set<Section> _loadedSections = new HashSet<>();

    private Map<String, Float> _masses = new HashMap<>();

    private static final String _nonStandardAminoAcids = "BJOUXZ";

    public MascotDatLoader(File f, Logger log) throws IOException, XMLStreamException
    {
        init(f, log);
        _reader = new BufferedReader(new FileReader(f));
        findBoundaryMarker();
    }

    public long getCharactersRead()
    {
        return _charactersRead;
    }

    // Looking for the boundary marker in the header like so:
    //    MIME-Version: 1.0 (Generated by Mascot version 1.0)
    //    Content-Type: multipart/mixed; boundary=gc0p4Jq0M2Yt08jU534c0p
    // After this, the current line will be the last line read (i.e., the Content-Type line) or null if EOF was reached.
    private void findBoundaryMarker() throws IOException
    {
        readLine();
        while (!eof() && (_boundaryMarker == null))
        {
            Matcher matcher = BOUNDARY_MARKER_LINE.matcher(_currentLine);
            if (matcher.matches())
            {
                _boundaryMarker = "--" + matcher.group(BOUNDARY_MARKER_GROUP_NUM);
            }
            else
            {
                readLine();
            }
        }
    }

    /**
     * Find the next line that looks like this:
     *       Content-Type: application/x-Mascot; name="<name>"
     * and sets the currentSection member to the value of <name>.  If this is a Query section, the currentQueryNum
     * will also be set to the number after the word "query" in <name>
     * The current line will be the last line read (i.e., the Content-Type line) or null if EOF was reached.
     *
     * @return indicator of whether a section was found or not.
     */
    public boolean findSection() throws IOException
    {
        readLine();
        while (!eof())
        {
            Matcher matcher = CONTENT_TYPE_LINE.matcher(_currentLine);
            if (matcher.matches())
            {
                _currentSection = Section.valueOf(matcher.group(SECTION_NAME_GROUP_NUM).toUpperCase());
                if (!matcher.group(QUERY_SECTION_INDEX_GROUP_NUM).isEmpty())
                {
                    _currentQueryNum = Integer.valueOf(matcher.group(QUERY_SECTION_INDEX_GROUP_NUM));
                }
                else
                {
                    _currentQueryNum = null;
                }
                return true;
            }
            readLine();
        }
        return false;
    }

    public Section getCurrentSection()
    {
        return _currentSection;
    }

    public Integer getCurrentQueryNum()
    {
        return _currentQueryNum;
    }

    @Override
    public void close()
    {
        try
        {
            if (null != _reader)
                _reader.close();
        }
        catch (IOException e)
        {
            _log.error(e);
        }
    }

    public boolean isLoaded(Section section)
    {
        return _loadedSections.contains(section);
    }

    /**
     * Loads data from the parameters section and stores it in the given peptide fraction.
     * @param fraction the object to contain the data loaded from the parameters
     * @param container the container in which we look for database files
     * @throws IOException if there are problems reading from the .dat file
     */
    public void loadParameters(PeptideFraction fraction, Container container) throws IOException
    {
        fraction.setSearchEngine(SEARCH_ENGINE_NAME);
        fraction.setSearchEnzyme(DEFAULT_ENZYME); // use the default unless we find something else
        readLine();
        while (!atEndOfSection())
        {
            if (_currentLine.startsWith(DB_PREFIX))
            {
                try
                {
                    File databaseFile = PeptideImporter.getDatabaseFile(container, _currentLine.substring(DB_PREFIX.length()).trim(), null);
                    fraction.setDatabaseLocalPath(databaseFile.getAbsolutePath());
                }
                catch (FileNotFoundException e)
                {
                    // Do nothing.  If we can't find the file from the fastafile value in the header section, it will throw an exception.
                    // Note that this assumes the header comes after the parameters section, which seems to be the case (despite what the names
                    // might suggest).
                }
            }
            else if (_currentLine.startsWith(ENZYME_PREFIX) || _currentLine.startsWith(ENZYME_PREFIX_LC))
            {
                fraction.setSearchEnzyme(_currentLine.substring(ENZYME_PREFIX.length()).trim());
            }
            readLine();
        }
//        _log.info("Loaded parameters");
        _loadedSections.add(Section.PARAMETERS);
    }

    public void loadMasses(PeptideFraction fraction) throws IOException
    {
        readLine();
        while (!atEndOfSection())
        {
            if (!_currentLine.trim().isEmpty())
            {
                Matcher matcher = MASS_DELTA_LINE.matcher(_currentLine);
                if (matcher.matches()) // These are the variable mods
                {
                    String name = matcher.group(MASS_DELTA_NAME_GROUP_NUM);
                    Float massDelta = Float.parseFloat(matcher.group(MASS_DELTA_GROUP_NUM));
                    boolean isProteinTerm = name.contains("Protein");

                    // CONSIDER: This index is possibly important for matching up modifications for individual proteins, but is not used now ('cause I don't know how)
                    int deltaNum = Integer.parseInt(matcher.group(MASS_INDEX_GROUP_NUM));
                    String residues = matcher.group(MASS_RESIDUES_GROUP_NUM);
                    boolean isNTerm = residues.contains("N-term");
                    boolean isCTerm = residues.contains("C-term");
                    if (isNTerm && isCTerm)
                    {
                        _log.error("Both c and n term modification detected. Skipping delta " + deltaNum + " in masses.");
                    }
                    else if ((((residues.length() == 7 || residues.length() == 14) && isProteinTerm)) ||
                             (residues.length() == 6 && isNTerm))
                    {
                        addVariableModifiedMass(fraction, "n", massDelta);
                    }
                    else if (residues.length() == 6 && isCTerm)
                    {
                        addVariableModifiedMass(fraction, "c", massDelta);
                    }
                    else
                    {
                        if (isCTerm || isNTerm) // presumably the [N|C]-term string comes at the beginning
                            residues = residues.substring(7);

                        for (int i = 0; i < residues.length(); i++)
                        {
                            addVariableModifiedMass(fraction, residues.substring(i, i + 1), massDelta);
                        }
                    }
                }
                else if ((matcher = MASS_FIXED_MOD_LINE.matcher(_currentLine)).matches()) // these are static mods
                {
                    Float massDelta = Float.parseFloat(matcher.group(MASS_DELTA_GROUP_NUM));
                    String residues = matcher.group(MASS_RESIDUES_GROUP_NUM);
                    residues = residues.replace("N-term", "");
                    residues = residues.replace("C-term", "");

                    for (int i = 0; i < residues.length(); i++)
                    {
                        MS2Modification mod = new MS2Modification();
                        mod.setAminoAcid(residues.substring(i, i+1));
                        mod.setMass(massDelta);  // I think it is correct to set mass and massDelta the same here
                        mod.setVariable(false);
                        mod.setMassDiff(massDelta);
                        mod.setSymbol("?");
                        fraction.addModification(mod);
                    }
                }
                else if ((matcher = KEY_VALUE_LINE.matcher(_currentLine)).matches())
                {
                    String massName = matcher.group(KV_KEY_GROUP_NUM);

                    if (massName.length() == 1)
                    {
                        Float mass = Float.parseFloat(matcher.group(KV_VALUE_GROUP_NUM));
                        _masses.put(massName, mass);
                        if (_nonStandardAminoAcids.contains(massName)) // These are the fixed mods
                        {
                            MS2Modification mod = new MS2Modification();
                            mod.setAminoAcid(massName);
                            mod.setMass(mass);
                            mod.setVariable(false);
                            mod.setMassDiff(mass); // for these non-standard AA's, the delta and the mass are the same.
                            mod.setSymbol("?");
                            fraction.addModification(mod);
                        }
                    }
                    else if (massName.toLowerCase().equals("c_term"))
                    {
                        _masses.put("c", Float.parseFloat(matcher.group(KV_VALUE_GROUP_NUM)));
                    }
                    else if (massName.toLowerCase().equals("n_term"))
                    {
                        _masses.put("n", Float.parseFloat(matcher.group(KV_VALUE_GROUP_NUM)));
                    }
                }
            }
            readLine();
        }
        ((MS2ModificationList) fraction.getModifications()).initializeSymbols();
//        _log.info("Loaded masses");
        _loadedSections.add(Section.MASSES);
    }

    private void addVariableModifiedMass(PeptideFraction fraction, String aminoAcid, Float massDelta)
    {
        if (!_masses.containsKey(aminoAcid))
            _log.error("Trying to store a variable AA modification for '" + aminoAcid + "' without any prior info for this AA.");
        else
        {
            MS2Modification mod = new MS2Modification();
            mod.setAminoAcid(aminoAcid);
            mod.setMass(_masses.get(aminoAcid) + massDelta);
            mod.setVariable(true);
            // we don't set the symbols here; they are set by the initializeSymbols method on the modifications list
            // once all the modifications have been collected.
            mod.setMassDiff(massDelta);
            fraction.addModification(mod);
        }
    }

    public void loadPeptides(Map<Integer, DatPeptide> peptides, PeptideFraction fraction)
    {
        PeptideIterator iterator = new PeptideIterator(fraction);
        DatPeptide peptide;
        while (iterator.hasNext())
        {
            peptide = iterator.next();
            if (peptide.getHitRank() == 1)
            {
                if (peptides.containsKey(peptide.getIndex()))
                {
                    DatPeptide existingPeptide = peptides.get(peptide.getIndex());
                    existingPeptide.merge(peptide);
                }
                else
                {
                    peptides.put(peptide.getIndex(), peptide);
                }
            }
        }
//        _log.info("Loaded peptides");
        _loadedSections.add(Section.PEPTIDES);
    }


    public void loadSummary(Map<Integer, DatPeptide> peptides) throws IOException
    {
        readLine();
        while (!atEndOfSection())
        {
            Matcher matcher = IDENTITY_SCORE_SUMMARY_LINE.matcher(_currentLine);
            if (matcher.matches())
            {
                Integer index = Integer.valueOf(matcher.group(QUERY_INDEX_GROUP_NUM));
                if (!peptides.containsKey(index))
                {
                    peptides.put(index, new DatPeptide());
                }
                DatPeptide peptide = peptides.get(index);
                if (matcher.group(KV_VALUE_GROUP_NUM).isEmpty())
                    peptide.setScore("identityscore", "100.0");
                else
                {
                    Float matchValue = Float.valueOf(matcher.group(KV_VALUE_GROUP_NUM));
                    double valueLog10 = 10 * Math.log(matchValue)/Math.log(10);
                    peptide.setScore("identityscore", String.format("%.2f", valueLog10));
                }
            }
            else if ((matcher = HOMOLOGY_SCORE_SUMMARY_LINE.matcher(_currentLine)).matches())
            {
                Integer index = Integer.valueOf(matcher.group(QUERY_INDEX_GROUP_NUM));
                if (!peptides.containsKey(index))
                {
                    peptides.put(index, new DatPeptide());
                }
                DatPeptide peptide = peptides.get(index);
                if (matcher.group(KV_VALUE_GROUP_NUM).isEmpty())
                    peptide.setScore("homologyscore", "100.0");
                else
                {
                    Float scoreValue = Float.valueOf(matcher.group(KV_VALUE_GROUP_NUM));
                    peptide.setScore("homologyscore", String.format("%.2f", scoreValue));
                }
            }
            else if ((matcher = PRECURSOR_ION_CHARGE_SUMMARY_LINE.matcher(_currentLine)).matches())
            {
                Integer index = Integer.valueOf(matcher.group(QUERY_INDEX_GROUP_NUM));
                if (!peptides.containsKey(index))
                {
                    peptides.put(index, new DatPeptide());
                }
                DatPeptide peptide = peptides.get(index);
                if (matcher.group(ION_CHARGE_GROUP_NUM).isEmpty())
                    peptide.setCharge(UNKNOWN_ION_CHARGE_VALUE);
                else
                    peptide.setCharge(Integer.valueOf(matcher.group(ION_CHARGE_GROUP_NUM)));


            }
            readLine();
        }
//        _log.info("Loaded summary data");
        _loadedSections.add(Section.SUMMARY);
    }

    public void loadQuery(Map<Integer, DatPeptide> peptides) throws IOException
    {
        readLine();
        DatPeptide peptide = peptides.get(_currentQueryNum);
        String title = null;
        while (!atEndOfSection())
        {
            if (_currentLine.startsWith(QUERY_TITLE_PREFIX))
            {
                if (!peptides.containsKey(_currentQueryNum))
                    peptides.put(_currentQueryNum, new DatPeptide());

                title = URLDecoder.decode(_currentLine.substring(QUERY_TITLE_PREFIX.length()), "UTF-8");
                Matcher matcher = QUERY_TITLE_SCAN_REGEX.matcher(title);
                if (matcher.find())
                {
                    peptide.setScan(Integer.parseInt(matcher.group(START_SCAN_GROUP_NUM)));
                    peptide.setEndScan(Integer.parseInt(matcher.group(END_SCAN_GROUP_NUM)));
                }
            }
            else if (_currentLine.startsWith(QUERY_SCANS_PREFIX)) // N.B. This line is not parsed in the Mascot2XML code, but it seems a good place to get the start scan data; the syntax is unknown for multiple scans, though
            {
                peptide.setScan(Integer.parseInt(_currentLine.substring(QUERY_SCANS_PREFIX.length())));
                peptide.setEndScan(peptide.getScan());
            }
            else if (_currentLine.startsWith(QUERY_RETENTION_TIME_PREFIX)) // N.B. This line is not parsed in the Mascot2Xml code, but it seems a reasonable place to get the retention time
            {
                peptide.setRetentionTime(Double.parseDouble(_currentLine.substring(QUERY_RETENTION_TIME_PREFIX.length())));
            }
            readLine();
        }
        if (peptide.getScan() == null)
        {
            _log.debug("Scan for peptide " + peptide.getTrimmedPeptide() + " in query " + _currentQueryNum + " not found.  Title parsing may be necessary.");
            findScanFromTitle(title, peptide);
        }
    }

    private void findScanFromTitle(String title, DatPeptide peptide)
    {
        Pair<Integer, Integer> range = new Pair<>(-1,-1);
        Double retentionTime = null;

        String scanPrefix = "scansinrange";
        Integer rangePosition = title.indexOf(scanPrefix); //sum of several scans

        // get end scan if we haven't already
        if (rangePosition != -1) {  // was scansinrange: end scan after 'to'
            int endIndexStart = title.indexOf("to", rangePosition + scanPrefix.length());
            if (endIndexStart != -1)
                range.second = Integer.parseInt(title.substring(endIndexStart));
        }
        else
        {
            scanPrefix = "Scan";
            rangePosition = title.indexOf(scanPrefix); // single scan
        }

        if (title.startsWith("ScanNumber:"))
        {
            range.first = range.second = Integer.parseInt(title.substring("ScanNumber:".length())); // single scan
        }
        else if (title.startsWith("spectrumId="))
        {
            range.first = range.second = Integer.parseInt(title.substring("specturmId=".length())); // single scan
            int index = title.indexOf("TimeInSeconds=");
            if (index >= 0)
            {
                index = index + "TimeInSections=".length();
                retentionTime = Double.parseDouble(title.substring(index, index+14));
            }
        }     // if we had a match and after the match still some characters and the first of them is a number, get start scan
        else if (rangePosition >= 0 && title.length() > rangePosition + scanPrefix.length() && Character.isDigit(title.charAt(rangePosition + scanPrefix.length())))
        {
            range.first = Integer.parseInt(title.substring(rangePosition+scanPrefix.length()));
        }
        else if (title.contains("FinneganScanNumber:"))
        {
            range.first = range.second = Integer.parseInt(title.substring("FinneganScanNumber:".length()));
        }

        // CONSIDER: Mascot2XML code has some logic here about parsing scan data from a mzXML file if all the above fails

        if (range.first < 0) { // no scan numbers found: set start scan to 0000
            range.first = 0;
        }

        //no end scan: either was single scan or "to" not found or no scan numbers at all
        // set end scan to start scan
        if (range.first > range.second) {
            range.second = range.first;
        }
        peptide.setScan(range.first);
        peptide.setEndScan(range.second);
        if (peptide.getRetentionTime() == null)
            peptide.setRetentionTime(retentionTime);

    }

    public void loadHeader(PeptideFraction fraction, Container container) throws IOException
    {
        readLine();
        while (!atEndOfSection())
        {
            if (_currentLine.startsWith(FASTAFILE_PREFIX) && fraction.getDatabaseLocalPath() == null)
            {
                File databaseFile = PeptideImporter.getDatabaseFile(container, null, _currentLine.substring(FASTAFILE_PREFIX.length()).trim());
                fraction.setDatabaseLocalPath(databaseFile.getAbsolutePath());
            }
            readLine();
        }
        _loadedSections.add(Section.HEADER);
    }

    private boolean eof()
    {
        return _currentLine == null;
    }

    private void readLine() throws IOException
    {
        // Consider: perhaps use the LineIterator from FileUtils instead
        _currentLine = _reader.readLine();
        if (_currentLine != null)
            _charactersRead += _currentLine.length();
    }

    private boolean atEndOfSection()
    {
        return eof() || _currentLine.matches(_boundaryMarker);
    }

    public class DatPeptide extends Peptide
    {

        private int _index;

        public int getIndex()
        {
            return _index;
        }

        public void setIndex(int index)
        {
            _index = index;
        }

        private String getPeptideWithModifications(MS2ModificationList modificationList, String modificationsMask)
        {
            StringBuffer peptide = new StringBuffer(_trimmedPeptide);
            char[] modChars = new char[_trimmedPeptide.length()];
            _unknownModArray = new boolean[_trimmedPeptide.length()];
            boolean isModified = false;
            // the mask is a mask on the un-trimmed peptide, so we adjust indexes in one on either end
            for (int i = 1; i < modificationsMask.length()-1; i++)
            {
                if (modificationsMask.charAt(i) != '0')
                {

                    String aminoAcid = peptide.substring(i-1, i);
                    MS2Modification mod = modificationList.get(aminoAcid);

                    // If null, it's either one of the mods that X! Tandem looks for on N-terminal amino acids Q, E, and C, and Tandem2XML isn't spitting out
                    // amino-acid tags OR it's a problem we don't understand
                    if (null == mod)
                    {
                        //record the unknown modification, but don't print out anything yet
                        _unknownModArray[i-1] = true;
                        _log.debug("Unknown modification at scan " + getScan() + ": " + aminoAcid);
                    }
                    else if (mod.getVariable())
                    {
                        isModified = true;
                        modChars[i - 1] = mod.getSymbol().charAt(0);
                    }
                }
            }
            if (isModified)
            {
                // Iterate in reverse order, so inserts don't invalidate future positions
                for (int i = modChars.length - 1; i >= 0; i--)
                    if (0 != modChars[i])
                        peptide.insert(i + 1, modChars[i]);
            }
            return peptide.toString();
        }

        public boolean load(MS2ModificationList modificationList) throws IOException
        {
            readLine();
            while (!atEndOfSection())
            {
                Matcher matcher = PEPTIDE_LINE1.matcher(_currentLine);
                if (matcher.matches())
                {
                    setIndex(Integer.parseInt(matcher.group(QUERY_INDEX_GROUP_NUM)));
                    setHitRank(Integer.parseInt(matcher.group(HIT_RANK_GROUP_NUM)));
                    setCalculatedNeutralMass(Rounder.round(Double.parseDouble(matcher.group(PEPTIDE_MASS_GROUP_NUM)), 4));
                    setDeltaMass(Rounder.round(Float.parseFloat(matcher.group(PEPTIDE_MASS_DIFF_GROUP_NUM)), 4));

                    setMatchedIons(Integer.parseInt(matcher.group(PEPTIDE_ION_MATCH_GROUP_NUM)));
                    setTrimmedPeptide(matcher.group(PEPTIDE_NAME_GROUP_NUM));
                    setPeptide(getPeptideWithModifications(modificationList, matcher.group(PEPTIDE_MODS_GROUP_NUM)));
                    setScore("ionscore", matcher.group(PEPTIDE_ION_SCORE_GROUP_NUM));
                    setProtein(matcher.group(PEPTIDE_PROTEIN_GROUP_NUM).trim());
                    String alternateProteins = matcher.group(PEPTIDE_ALTERNATE_PROTEIN_GROUP_NUM);
                    Matcher proteinMatcher = PEPTIDE_PROTEIN.matcher(alternateProteins);
                    while (proteinMatcher.find())
                    {
                        if (!matcher.group(PEPTIDE_PROTEIN_NAME_GROUP_NUM).isEmpty())
                        {
                            addAlternativeProtein(matcher.group(PEPTIDE_PROTEIN_NAME_GROUP_NUM));
                        }
                    }
                    setProteinHits(getAlternativeProteins().size() + 1);

                    // Read the "terms" line to get the previous and next amino acid
                    // q1_p1_terms=K,I
                    // q1_p5_terms=R,I:R,I
                    readLine();
                    Matcher termsMatcher = PEPTIDE_LINE2.matcher(_currentLine);
                    if (termsMatcher.find())
                    {
                        setNextAA(termsMatcher.group(PEPTIDE_TERMS_NEXT_AA_GROUP_NUM));
                        setPrevAA(termsMatcher.group(PEPTIDE_TERMS_PREVIOUS_AA_GROUP_NUM));
                        // skipping the terms for alternate proteins (at least for now)
                    }
                    return true;
                }
                readLine();
            }

            return false;
        }

        public void merge(DatPeptide otherPeptide)
        {
            setIndex(otherPeptide.getIndex());
            super.merge((Peptide) otherPeptide);
        }


    }

    public class PeptideIterator implements Iterator<DatPeptide>
    {
        private DatPeptide _peptide = null;
        private PeptideFraction _fraction = null;

        public PeptideIterator(PeptideFraction fraction)
        {
            _fraction = fraction;
        }

        @Override
        public boolean hasNext()
        {
            try
            {
                DatPeptide peptide = new DatPeptide();
                boolean success = peptide.load((MS2ModificationList) _fraction.getModifications());

                if (success)
                    _peptide = peptide;
                else
                    _peptide = null;
            }
            catch (IOException e)
            {
                _log.error(e);
                _peptide = null;
            }

            return _peptide != null;
        }

        @Override
        public DatPeptide next()
        {
            return _peptide;
        }

    }
}
