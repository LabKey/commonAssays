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
package org.labkey.flow.controllers.run;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatusJsonHelper
{

    protected static String getStatusUrl(List<String> lines)
    {
        String url = null;
        StringBuilder stringBuilder = new StringBuilder();
        boolean addingLines = false;
        for (String line : lines)
        {
            String statusUrl = "statusUrl";
            if (line.contains(statusUrl) || addingLines)
            {
                int beginJsonIndex = line.indexOf("{");
                int infoIndex = line.indexOf("INFO :");
                int beginIndex = beginJsonIndex >= 0?beginJsonIndex:infoIndex;
                if(beginIndex > 0){
                    line = line.substring(beginIndex);
                }
                stringBuilder.append(line);
                addingLines = true;
            }
            if(addingLines && line.contains("}"))
            {
                break;
            }
         }
         if(stringBuilder.length() > 0)
         {
             ObjectMapper mapper = new ObjectMapper();
             mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
             try
             {
                 StatusJson statusJson = mapper.readValue(stringBuilder.toString(), StatusJson.class);
                 url = statusJson.getStatusUrl();
             }
             catch (IOException e)
             {
                 e.printStackTrace();
             }
         }
        return url;
    }

    public static class StatusJson{
        private String statusUrl;
        private String status;

        public String getStatusUrl()
        {
            return statusUrl;
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatusUrl(String statusUrl)
        {
            this.statusUrl = statusUrl;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testGetStatusUrl()
        {
            List<String> lines = new ArrayList<>();
            lines.add("{\"statusUrl\": \"theUrl\",\"status\": \"text description\"}");
            String result = getStatusUrl(lines);
            assertEquals("Wrong url", "theUrl",result);

            lines = new ArrayList<>();
            lines.add("{\"statusUrl\": \"theUrl\",");
            lines.add("\"status\": \"text description\"}");
            result = getStatusUrl(lines);
            assertEquals("Wrong url", "theUrl",result);


            lines = new ArrayList<>();
            lines.add("[21 Sep 2017 15:25:26,657 INFO : Executing script: python flow-export-script.py --timeout ${timeout} --guid 4718a37e-8148-1035-b06b-50beff068c18 --location ZipFile stream. --exportFormat ${exportFormat}");
            lines.add("21 Sep 2017 15:25:26,657 INFO : python output");
            lines.add("21 Sep 2017 15:25:26,673 INFO : =======================================");
            lines.add("21 Sep 2017 15:25:26,673 INFO : Working directory is D:\\Flow\\Medimmune");
            lines.add("21 Sep 2017 15:25:26,673 INFO : running: python flow-export-script.py --timeout ${timeout} --guid 4718a37e-8148-1035-b06b-50beff068c18 --location ZipFile stream. --exportFormat ${exportFormat}");
            lines.add("21 Sep 2017 15:25:26,724 INFO : hello world");
            lines.add("21 Sep 2017 15:25:26,740 INFO : ");
            lines.add("21 Sep 2017 15:25:26,740 INFO : flow-export-script.py");
            lines.add("21 Sep 2017 15:25:26,740 INFO : --timeout");
            lines.add("21 Sep 2017 15:25:26,740 INFO : ${timeout}");
            lines.add("21 Sep 2017 15:25:26,755 INFO : --guid");
            lines.add("21 Sep 2017 15:25:26,755 INFO : 4718a37e-8148-1035-b06b-50beff068c18");
            lines.add("21 Sep 2017 15:25:26,755 INFO : --location");
            lines.add("21 Sep 2017 15:25:26,755 INFO : ZipFile stream.");
            lines.add("21 Sep 2017 15:25:26,771 INFO : --exportFormat");
            lines.add("21 Sep 2017 15:25:26,771 INFO : ${exportFormat}");
            lines.add("21 Sep 2017 15:25:26,771 INFO : status json: {'statusUrl': 'log url','status': 'text description'}");
            lines.add("21 Sep 2017 15:25:26,771 INFO : Deleting temp directory: ZipFile stream.]");
            result = getStatusUrl(lines);
            assertEquals("Wrong url", "log url",result);

        }
    }

}
