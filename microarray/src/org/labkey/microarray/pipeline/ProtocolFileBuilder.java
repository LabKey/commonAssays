/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
package org.labkey.microarray.pipeline;

import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.pipeline.PipelineJobException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


public class ProtocolFileBuilder
{
    private static final String xdrTag = "DateTime";
    private static final String sidTag = "ImageDescription";
    private static final String bitsPerSampleTag = "BitsPerSample";

    private static String sqlDesignId = "SELECT DesignId FROM GridTemplate";
    private static String sqlGridName = "SELECT GridName FROM GridTemplate WHERE DesignId = ?";
    private static String sqlProtocolName = "SELECT Protocol.Name FROM Protocol, GridTemplate WHERE Protocol.Id = GridTemplate.ProtocolId AND GridTemplate.GridName = ?";
    private static String sqlOneColorProtocolName = "SELECT Protocol.Name FROM Protocol, GridTemplate WHERE Protocol.Id = GridTemplate.OneColorProtocolId AND GridTemplate.GridName = ?";

    public void build(File protocolFile, FeatureExtractorTask.Factory factory, Collection<File> images) throws IOException, PipelineJobException
    {
        try
        {
            Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }

        String resultsDirectory = protocolFile.getParent();

        try (PrintWriter outputWriter = new PrintWriter(new FileWriter(protocolFile)); Connection conn = DriverManager.getConnection(factory.getJdbcURL(), factory.getJdbcUser(), factory.getJdbcPassword()))
        {
            //Determine extraction sets by finding unique extraction names
            HashSet<String> extractions = new HashSet<>();

            for (File image : images)
            {
                extractions.add(getExtractionName(image));
            }

            outputWriter.println(getFEMLTagStart());
            outputWriter.println(getPMLVerInfo());
            outputWriter.println(getFEProjectTagStart(resultsDirectory));

            for (String extraction : extractions)
            {
                outputWriter.println(getExtractionTagStart(extraction));

                File hiImage = getHighIntensityImage(extraction, images);
                File loImage = getLowIntensityImage(extraction, images);

                if (loImage != null)
                    outputWriter.println(getXDRScanIdTag(getXDRScanId(hiImage)));
                outputWriter.println(getImageTag(hiImage.getAbsolutePath()));
                if (loImage != null)
                    outputWriter.println(getImageXDR2Tag(loImage.getAbsolutePath()));
                String gridName = getGridName(hiImage, conn);
                outputWriter.println(getGridTag(gridName));
                outputWriter.println(getProtocolTag(getProtocolName(gridName, hiImage, conn)));
                outputWriter.println(getExtractionTagEnd());
            }

            outputWriter.println(getFEProjectTagEnd());
            outputWriter.println(getFEMLTagEnd());
            outputWriter.println();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private String getTiffMetaTagValue(String tagName, File image) throws IOException
    {
        String value = null;
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("tiff");

        if (it.hasNext())
        {
            ImageReader ir = it.next();
            ImageInputStream iis = ImageIO.createImageInputStream(image);
            ir.setInput(iis, true);
            IIOMetadata iiom = ir.getImageMetadata(0);
            TIFFDirectory ifd = TIFFDirectory.createFromMetadata(iiom);
            TIFFField[] allFields = ifd.getTIFFFields();
            for (TIFFField field : allFields)
            {
                String name = field.getTag().getName();
                if (name.equals(tagName))
                {
                    value = field.getValueAsString(0);
                    if (value != null)
                    {
                        value = value.trim();
                    }
                }
            }
            iis.close();
            ir.dispose();
        }
        return value;
    }

    protected static String getExtractionName(File image)
    {
        String fileName = image.getName();
        fileName = fileName.replaceFirst(".[tT][iI][fF]+$", "");
        fileName = fileName.replaceFirst("_[hHlL]$", "");

        return fileName;
    }

    protected String getXDRScanId(File image) throws IOException, PipelineJobException
    {
        String tagValue = getTiffMetaTagValue(xdrTag, image);
        SimpleDateFormat inFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        SimpleDateFormat outFormat = new SimpleDateFormat("MMddyyyyHHmmss");
        Date parsed = new Date();
        try
        {
            parsed = inFormat.parse(tagValue);
        }
        catch (ParseException ex)
        {
            //Try to get by with using the current date
        }

        String scanId = outFormat.format(parsed);

        if (scanId == null)
            throw new PipelineJobException("The XDRScanID could not be determined for the image " + image.getAbsolutePath());

        return scanId;
    }

    protected String getProtocolName(String gridName, File image, Connection connection) throws IOException, SQLException, PipelineJobException
    {
        ResultSet rs = null;
        String queryName;
        String description = getTiffMetaTagValue(sidTag, image);
        String bitsPerSample = getTiffMetaTagValue(bitsPerSampleTag, image);

        if (description == null)
            throw new PipelineJobException("The scan channel information could not be determined for the image " + image.getAbsolutePath());

        if (description.equalsIgnoreCase("Red") || "32".equals(bitsPerSample))
        {
            // 32-bit images have both channels embedded in the same file
            queryName = sqlProtocolName;
        }
        else if (description.equalsIgnoreCase("Green"))
        {
            queryName = sqlOneColorProtocolName;
        }
        else
        {
            throw new PipelineJobException("The scan channel information could not be determined from the tiff meta info: " + sidTag + " = " + description + ", " + bitsPerSampleTag + " = " + bitsPerSample);
        }

        try
        {
            PreparedStatement stat = connection.prepareStatement(queryName);
            stat.setString(1, gridName);
            rs = stat.executeQuery();

            // this assumes there is only one row in the result set
            if (!rs.next())
            {
                throw new PipelineJobException("No protocol found for scan channel " + description + " with grid name " + gridName + ". The template may not have a default protocol associated with it in the Feature Extractor application.");
            }
            return rs.getString(1);
        }
        finally
        {
            if (rs != null)
            {
                try
                {
                    rs.close();
                }
                catch (SQLException e)
                {
                }
            }
        }
    }

    protected String getGridName(File image, Connection connection) throws PipelineJobException, SQLException
    {
        ResultSet rs = null;

        try
        {
            List<String> designIds = getDesignIds(connection);
            String designId = null;

            for (String id : designIds)
            {
                if (image.getName().matches("^US[0-9]+_[0-9]{2}" + id.substring(1) + ".+$"))
                {
                    designId = id;
                }
            }

            if (designId == null)
            {
                throw new PipelineJobException("No matching designs for image " + image.getName());
            }

            PreparedStatement stat = connection.prepareStatement(sqlGridName);
            stat.setString(1, designId);
            rs = stat.executeQuery();

            // this assumes there is only one row in the result set
            if (!rs.next())
            {
                throw new PipelineJobException("No matching GridTemplates for designId " + designId);
            }
            return rs.getString(1);
        }
        finally
        {
            if (rs != null) { try { rs.close(); } catch (SQLException e) {} }
        }
    }

    protected List<String> getDesignIds(Connection connection) throws PipelineJobException, SQLException
    {
        ResultSet rs = null;
        List<String> list = new ArrayList<>();

        try
        {
            PreparedStatement stat = connection.prepareStatement(sqlDesignId);
            rs = stat.executeQuery();

            while (rs.next())
            {
                list.add(rs.getString(1));
            }
            return list;
        }
        finally
        {
            if (rs != null) { try { rs.close(); } catch (SQLException e) {} }
        }
    }

    protected static File getHighIntensityImage(String extraction, Collection<File> images)
    {
        File name = null;

        for (File image : images)
        {
            if (image.getName().matches(extraction + ".+$") && !image.getName().matches(extraction + "_[lL].+$"))
                name = image;
        }
        return name;
    }

    protected static File getLowIntensityImage(String extraction, Collection<File> images)
    {
        File name = null;

        for (File image : images)
        {
            if (image.getName().matches(extraction + "_[lL].+$"))
                name = image;
        }
        return name;
    }

    private String getFEMLTagStart()
    {
        return "<FeatureExtractionML>";
    }

    private String getFEMLTagEnd()
    {
        return "</FeatureExtractionML>";
    }

    private String getPMLVerInfo()
    {
        return "<FEPMLVerInfo VerMaj=\"2\" VerMin=\"0\"/>";
    }

    
    private String getFEProjectTagStart(String resultsDir)
    {
        return "<FEProject Operator=\"FeatureExtractionQueue\" ResultsDirectory=\"" + resultsDir + "\" " +
		"ResultsLocationSameAsImage=\"False\" OutputMAGE=\"True\" MAGEOutPkgType=\"Full\" " +
		"OutputMAGECompressed=\"False\" OutputJPEG=\"True\" OutputText=\"True\" TextOutPkgType=\"Full\" " +
		"OutputVisualResults=\"True\" OutputGRID=\"True\" OutputArrayQCReport=\"True\" " +
		"FTPSendTiffFile=\"False\" OverWritePreviousResults=\"True\" UseGridFileIfAvailable=\"False\" " +
		"UseProjDefProtocolFirst=\"False\">";
    }

    private String getFEProjectTagEnd()
    {
        return "</FEProject>";
    }

    private String getExtractionTagStart(String name)
    {
        return "<Extraction Name=\"" + name + "\">";
    }

    private String getExtractionTagEnd()
    {
        return "</Extraction>";
    }

    private String getXDRScanIdTag(String name)
    {
        return "<XDRScanID Name=\"" + name + "\"/>";
    }

    private String getImageTag(String name)
    {
        return "<Image Name=\"" + name + "\"/>";
    }

    private String getImageXDR2Tag(String name)
    {
        return "<ImageXDR2 Name=\"" + name +"\"/>";
    }

    private String getGridTag(String name)
    {
        return "<Grid Name=\"" + name + "\" IsGridFile=\"False\"/>";
    }

    private String getProtocolTag(String name)
    {
        return "<Protocol Name=\"" + name + "\"/>";
    }
}
