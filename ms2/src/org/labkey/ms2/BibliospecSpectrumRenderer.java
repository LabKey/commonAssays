package org.labkey.ms2;

import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ViewContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * Creates a Bibliospec SQLite data file based on a set of peptides/spectra
 * User: jeckels
 * Date: 8/13/13
 */
public class BibliospecSpectrumRenderer implements SpectrumRenderer
{
    private final ViewContext _context;

    private DecimalFormat MODIFICATION_MASS_FORMAT = new DecimalFormat("0.0");

    public BibliospecSpectrumRenderer(ViewContext context)
    {
        _context = context;
    }

    @Override
    public void render(SpectrumIterator iter) throws IOException
    {
        // Run ID->Modification list
        Map<Integer, List<MS2Modification>> modificationsCache = new HashMap<>();
        // LabKey Fraction ID->Bibliospec SpectraSourceFile ID
        Map<Integer, Integer> fractionCache = new HashMap<>();

        String shortName = _context.getContainer().getName() + "SpectraLibrary";
        File tempFile = File.createTempFile(shortName, ".blib");
        try
        {
            Class.forName("org.sqlite.JDBC");
            tempFile.deleteOnExit();
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + tempFile.getAbsolutePath());
                 Statement statement = connection.createStatement())
            {
                statement.execute("CREATE TABLE LibInfo (libLSID TEXT not null, createTime TEXT not null, numSpecs INT not null, majorVersion INT not null, minorVersion INT not null, primary key (libLSID))");
                statement.execute("CREATE TABLE Modifications (id  integer primary key autoincrement, RefSpectraId BIGINT, position INT not null, mass DOUBLE not null, constraint FK928405BDF1ADF3B4 foreign key (RefSpectraId) references RefSpectra);");
                statement.execute("CREATE TABLE RefSpectra (id  integer primary key autoincrement, peptideSeq TEXT not null, peptideModSeq TEXT not null, precursorCharge INT not null, precursorMZ DOUBLE not null, prevAA TEXT, nextAA TEXT, copies SMALLINT not null, numPeaks INTEGER not null);");
                statement.execute("CREATE TABLE RefSpectraPeaks (RefSpectraId BIGINT not null, peakMZ BLOB, peakIntensity BLOB, primary key (RefSpectraId), constraint FKACE51F3F1ADF3B4 foreign key (RefSpectraId) references RefSpectra);");
                statement.execute("CREATE TABLE SpectrumSourceFiles (id  integer primary key autoincrement, fileName TEXT);");

                // We're creating a redundant library, which isn't expected to contain retention times, so we can
                // omit the table
//                statement.execute("CREATE TABLE RetentionTimes (id  integer primary key autoincrement, RefSpectraId BIGINT, RedundantRefSpectraId BIGINT, SpectrumSourceId BIGINT, retentionTime DOUBLE, bestSpectrum INT, constraint FKF3D3B64AF1ADF3B4 foreign key (RefSpectraId) references RefSpectra);");

                // Keep track of the sequence value so that we can set foreign key values
                int spectraCount = 0;

                // Create the statements to insert all of the data
                try (PreparedStatement peptidePS = connection.prepareStatement("INSERT INTO RefSpectra(peptideSeq, peptideModSeq, precursorCharge, precursorMZ, nextAA, prevAA, copies, numPeaks) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                     PreparedStatement spectraPS = connection.prepareStatement("INSERT INTO RefSpectraPeaks(RefSpectraId, PeakMZ, PeakIntensity) VALUES (?, ?, ?)");
                     PreparedStatement modificationPS = connection.prepareStatement("INSERT INTO Modifications (RefSpectraId, Position, Mass) VALUES (?, ?, ?)");
                     PreparedStatement sourceFilePS = connection.prepareStatement("INSERT INTO SpectrumSourceFiles (fileName) VALUES (?)"))
                {
                    // Iterate over all of the spectra
                    while (iter.hasNext())
                    {
                        Spectrum spectrum = iter.next();

                        float[] mzFloats = spectrum.getX();
                        float[] intensityFloats = spectrum.getY();

                        List<MS2Modification> modifications = modificationsCache.get(spectrum.getRun());
                        if (modifications == null)
                        {
                            MS2Run run = MS2Manager.getRun(spectrum.getRun());
                            if (run == null)
                            {
                                throw new IllegalStateException("Could not find run " + spectrum.getRun());
                            }
                            modifications = MS2Manager.getModifications(run);
                            modificationsCache.put(run.getRun(), modifications);
                        }

                        // Make sure we have a record for the .mzXML file in the SpectrumSourceFiles table
                        Integer spectraFileId = fractionCache.get(spectrum.getFraction());
                        if (spectraFileId == null)
                        {
                            MS2Fraction fraction = MS2Manager.getFraction(spectrum.getFraction());
                            if (fraction == null)
                            {
                                throw new IllegalStateException("Could not find fraction " + spectrum.getFraction());
                            }
                            try
                            {
                                sourceFilePS.setString(1, new File(new URI(fraction.getMzXmlURL())).getAbsolutePath());
                                sourceFilePS.execute();
                                spectraFileId = fractionCache.size() + 1;
                                fractionCache.put(fraction.getFraction(), spectraFileId);
                            }
                            catch (URISyntaxException e)
                            {
                                throw new UnexpectedException(e);
                            }
                        }

                        // Hold on to the index->mass difference info so we can insert into the Modifications table as well
                        Map<Integer, Float> peptideModifications = new HashMap<>();

                        peptidePS.setString(1, spectrum.getTrimmedSequence());
                        peptidePS.setString(2, getExportModifiedSequence(spectrum.getSequence(), modifications, peptideModifications));
                        peptidePS.setInt(3, spectrum.getCharge());
                        peptidePS.setDouble(4, spectrum.getMZ());
                        peptidePS.setString(5, "-");
                        peptidePS.setString(6, "-");
                        peptidePS.setInt(7, 1);
                        peptidePS.setInt(8, mzFloats.length);
                        peptidePS.execute();
                        spectraCount++;

                        for (Map.Entry<Integer, Float> entry : peptideModifications.entrySet())
                        {
                            modificationPS.setInt(1, spectraCount);
                            modificationPS.setInt(2, entry.getKey().intValue());
                            modificationPS.setFloat(3, entry.getValue().floatValue());
                            modificationPS.execute();
                        }

                        assert mzFloats.length == intensityFloats.length : "Mismatched spectra arrays";
                        // Write out mz as doubles
                        ByteBuffer mzBuffer = ByteBuffer.allocate(mzFloats.length * Double.SIZE / 8);
                        mzBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        // Write out mz as floats
                        ByteBuffer intensityBuffer = ByteBuffer.allocate(intensityFloats.length * Float.SIZE / 8);
                        intensityBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        for (int i = 0; i < mzFloats.length; i++)
                        {
                            mzBuffer.putDouble(mzFloats[i]);
                            intensityBuffer.putFloat(intensityFloats[i]);
                        }

                        spectraPS.setInt(1, spectraCount);
                        spectraPS.setBytes(2, compress(mzBuffer.array()));
                        spectraPS.setBytes(3, compress(intensityBuffer.array()));
                        spectraPS.execute();
                    }
                }

                // Insert the LibInfo row with summary info
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO LibInfo(libLSID, createTime, numSpecs, majorVersion, minorVersion) VALUES (?, ?, ?, ?, ?)"))
                {
                    ps.setString(1, new Lsid("spectral_library", "bibliospec").setVersion("redundant") + Lsid.encodePart(_context.getContainer().getName()));
                    ps.setString(2, new SimpleDateFormat("EEE MMM HH:mm:ss yyyy").format(new Date()));
                    ps.setInt(3, spectraCount);
                    ps.setInt(4, 1);
                    ps.setInt(5, 1);
                    ps.execute();
                }

                // Create the indices on the end after all the inserts are done
                statement.execute("CREATE INDEX idxPeptide on RefSpectra (peptideSeq);");
                statement.execute("CREATE INDEX idxPeptideMod on RefSpectra (peptideModSeq, precursorCharge);");
            }

            try (InputStream fIn = new FileInputStream(tempFile))
            {
                PageFlowUtil.streamFile(_context.getResponse(), Collections.<String, String>emptyMap(), shortName + ".redundant.blib", fIn, true);
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new UnexpectedException(e);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            tempFile.delete();
        }
    }

    /** Bibliospec wants the modified strings in a different format ("XXX[+YY.Y]XXX") than how we store them ("X.XXX*XXX.X") */
    private String getExportModifiedSequence(String sequence, List<MS2Modification> runModifications, Map<Integer, Float> peptideMods)
    {
        // Grab just the middle part, between the dots
        String[] split = sequence.split("\\.");
        if (split.length != 3)
        {
            throw new IllegalArgumentException("Expected peptide sequence to be of the form X.XXXX.X but was " + sequence);
        }

        String trimmedModifiedSequence = split[1];

        StringBuilder result = new StringBuilder();
        int residueIndex = 0;
        for (int i = 0; i < trimmedModifiedSequence.length(); i++)
        {
            char c = trimmedModifiedSequence.charAt(i);
            // If it's an amino acid, appent it directly
            if (Character.isLetter(c))
            {
                result.append(c);
                residueIndex++;
            }
            else
            {
                // Otherwise, find the modification's mass difference and append that instead
                MS2Modification modification = findModification(c, runModifications);
                result.append("[");
                if (modification.getMassDiff() > 0)
                {
                    result.append("+");
                }
                result.append(MODIFICATION_MASS_FORMAT.format(modification.getMassDiff()));
                result.append("]");
                peptideMods.put(residueIndex, modification.getMassDiff());
            }
        }
        return result.toString();
    }

    private MS2Modification findModification(char c, List<MS2Modification> modifications)
    {
        for (MS2Modification modification : modifications)
        {
            if (modification.getSymbol().equals(Character.toString(c)))
            {
                return modification;
            }
        }
        throw new IllegalArgumentException("Could not find a matching modification for " + c);
    }

    /** Do zlib compression of the bytes */
    private byte[] compress(byte[] bytes)
    {
        Deflater deflater = new Deflater();
        deflater.setInput(bytes);
        deflater.finish();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        // Operate on 4k chunks
        byte[] b = new byte[4096];
        int i;
        while ((i = deflater.deflate(b)) > 0)
        {
            bOut.write(b, 0, i);
        }
        deflater.end();
        return bOut.toByteArray();
    }

    @Override
    public void close() throws IOException
    {
        // We handle the closing inside the render() method
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testParsing()
        {
            BibliospecSpectrumRenderer renderer = new BibliospecSpectrumRenderer(null);

            MS2Modification mod1 = new MS2Modification();
            mod1.setAminoAcid("C");
            mod1.setMassDiff(57);
            mod1.setSymbol("*");

            MS2Modification mod2 = new MS2Modification();
            mod2.setAminoAcid("A");
            mod2.setMassDiff(-40.3f);
            mod2.setSymbol("'");

            HashMap<Integer, Float> peptideMods = new HashMap<>();
            assertEquals("ABFDSC[+57.0]CC", renderer.getExportModifiedSequence("X.ABFDSC*CC.X", Arrays.asList(mod1), peptideMods));
            assertEquals(Collections.singletonMap(6, 57.0f), peptideMods);

            peptideMods.clear();
            assertEquals("ABFDSC[+57.0]CC[+57.0]", renderer.getExportModifiedSequence("X.ABFDSC*CC*.X", Arrays.asList(mod1), peptideMods));
            assertEquals(peptideMods.size(), 2);
            assertEquals(57.0f, peptideMods.get(6), 0.0);
            assertEquals(57.0f, peptideMods.get(8), 0.0);

            peptideMods.clear();
            assertEquals("ABFDSC[+57.0]CC[+57.0]", renderer.getExportModifiedSequence("X.ABFDSC*CC*.X", Arrays.asList(mod1, mod2), peptideMods));
            assertEquals(peptideMods.size(), 2);
            assertEquals(57.0f, peptideMods.get(6), 0.0);
            assertEquals(57.0f, peptideMods.get(8), 0.0);

            peptideMods.clear();
            assertEquals("A[-40.3]BFDSC[+57.0]CC[+57.0]", renderer.getExportModifiedSequence("X.A'BFDSC*CC*.X", Arrays.asList(mod1, mod2), peptideMods));
            assertEquals(peptideMods.size(), 3);
            assertEquals(-40.3f, peptideMods.get(1), 0.0);
            assertEquals(57.0f, peptideMods.get(6), 0.0);
            assertEquals(57.0f, peptideMods.get(8), 0.0);
        }
    }
}
