/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.flow.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.flow.analysis.model.SampleIdMap;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL) // Don't serialize null values
@JsonPropertyOrder({ "exportedBy", "importedBy", "label", "guid", "exportedDatetime","exportFormat","exportedFiles" })
public class ExportAnalysisManifest
{
    private String exportedBy;
    private String importedBy;
    private String label;
    private String guid;
    private Date exportedDatetime;
    private String exportFormat;
    private Map<String, Object> exportedFiles;


    public String getExportedBy()
    {
        return exportedBy;
    }

    public void setExportedBy(String exportedBy)
    {
        this.exportedBy = exportedBy;
    }

    public String getImportedBy()
    {
        return importedBy;
    }

    public void setImportedBy(String importedBy)
    {
        this.importedBy = importedBy;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getGuid()
    {
        return guid;
    }

    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    public Date getExportedDatetime()
    {
        return exportedDatetime;
    }

    public void setExportedDatetime(Date exportedDatetime)
    {
        this.exportedDatetime = exportedDatetime;
    }

    public String getExportFormat()
    {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat)
    {
        this.exportFormat = exportFormat;
    }

    public Map<String, Object> getexportedFiles()
    {
        return exportedFiles;
    }

    public void setExportedFiles(Map<String, Object> files)
    {
        this.exportedFiles = files;
    }

    public void addExportedFile(String key, Object value)
    {
        if(exportedFiles == null)
        {
            exportedFiles = new HashMap<>();
        }
        exportedFiles.put(key,value);
    }

    public String toJSON()
    {
        try
        {
            StringWriter sw = new StringWriter();
            JsonGenerator jsonGen = new JsonFactory().createGenerator(sw);
            ObjectMapper oc = new ObjectMapper();
            String dateFormat = "yyyy-MM-dd HH:mm:ss";
            DateFormat df = new SimpleDateFormat(dateFormat);
            oc.setDateFormat(df);
            jsonGen.setCodec(oc);
            jsonGen.writeObject(this);
            jsonGen.useDefaultPrettyPrinter();
            return sw.toString();
        }
        catch (IOException io)
        {
            throw new RuntimeException(io);
        }
    }

    public void setSampleIdMap(@NotNull SampleIdMap<String> sampleIdMap)
    {
        List<FCSFile> fcsFiles = new ArrayList<>();
        for (String id : sampleIdMap.idSet())
        {
            FCSFile fcsFile = new FCSFile(id,sampleIdMap.getNameForId(id),sampleIdMap.getById(id));
            fcsFiles.add(fcsFile);
        }
        addExportedFile("FCSFiles", fcsFiles);
    }

    private class FCSFile{
        private String id;
        private String sample;
        private String path;

        public FCSFile(String id, String sample, String path)
        {
            this.id = id;
            this.sample = sample;
            this.path = path;
        }

        public String getId()
        {
            return id;
        }

        public String getSample()
        {
            return sample;
        }

        public String getPath()
        {
            return path;
        }
    }
    public static class TestCase extends Assert
    {
        @Test
        public void testJSONManifest()
        {

            //test exported by
            JSONObject expected = new JSONObject();
            String eW = "Edie Windsor";
            expected.put("exportedBy", eW);
            ExportAnalysisManifest exportManifest = new ExportAnalysisManifest();

            exportManifest.setExportedBy(eW);

            assertEquals("Wrong JSON", expected.toString(), exportManifest.toJSON());

            exportManifest.setExportedBy(null);

            //test files
            expected = new JSONObject();
            String keywords = "keywords";
            String value = "kwFile.txt";
            JSONObject fileObject = new JSONObject();
            JSONArray files = new JSONArray();
            files.put("file1");
            files.put("file2");
            fileObject.put("FCSFiles", files);
            fileObject.put(keywords, value);
            expected.put("exportedFiles", fileObject);

            List<String> fileList = new ArrayList<>();
            fileList.add("file1");
            fileList.add("file2");

            exportManifest.addExportedFile(keywords, value);
            exportManifest.addExportedFile("FCSFiles",fileList);
            assertEquals("Wrong JSON", expected.toString(), exportManifest.toJSON());

            //test date field
            expected = new JSONObject();
            Calendar calendar = Calendar.getInstance();
            calendar.set(2017,Calendar.SEPTEMBER,18,13,45,33);

            exportManifest.setExportedFiles(null);
            exportManifest.setExportedDatetime(calendar.getTime());

            expected.put("exportedDatetime", "2017-09-18 13:45:33");

            assertEquals("Wrong JSON", expected.toString(), exportManifest.toJSON());
        }

    }

}
