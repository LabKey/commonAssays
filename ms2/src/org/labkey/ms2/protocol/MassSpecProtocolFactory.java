/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protocol;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipelineProtocolFactory;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.PageFlowUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * MassSpecProtocolFactory class
 * <p/>
 * Created: Oct 7, 2005
 *
 * @author bmaclean
 */
public class MassSpecProtocolFactory extends PipelineProtocolFactory<MassSpecProtocol>
{
    private static final Logger _log = Logger.getLogger(MassSpecProtocolFactory.class);
    public static MassSpecProtocolFactory instance = new MassSpecProtocolFactory();
    public static String DEFAULT_TEMPLATE_NAME = "SimpleProtocolTemplate";
    public static String DEFAULT_FRACTIONATION_TEMPLATE_NAME = "SimpleFractionationProtocolTemplate";

    public static MassSpecProtocolFactory get()
    {
        return instance;
    }

    public String getName()
    {
        return "mass_spec";
    }

    @Override
    public List<String> getTemplateNames(URI uriRoot)
    {
        List<String> templateNames = super.getTemplateNames(uriRoot);

        templateNames.add(DEFAULT_TEMPLATE_NAME);
        templateNames.add(DEFAULT_FRACTIONATION_TEMPLATE_NAME);

        return templateNames;
    }

    public static String upperCaseTokenNameToPropName(String token)
    {
        StringBuffer sb = new StringBuffer(token.length());
        boolean capLetter = true;
        for (char c : token.toCharArray())
        {
            if ('_' == c)
            {
                capLetter = true;
                continue;
            }

            if (capLetter)
                sb.append(c);
            else
                sb.append(Character.toLowerCase(c));

            capLetter = !Character.isLetter(c);

        }

        return sb.toString();
    }

    public static final Pattern upperCaseTokenPattern = Pattern.compile("[A-Z0-9_]+");
    public static boolean isUpperCaseTokenName(String name)
    {
        return upperCaseTokenPattern.matcher(name).matches();
    }


    public XarTemplate getXarTemplate(URI uriRoot, String name)
    {
        return new XarTemplate(name, getTemplateText(uriRoot, name));
    }

    public String getTemplateText(URI uriRoot, String name)
    {
        BufferedReader reader = null;
        if (null != name && !DEFAULT_TEMPLATE_NAME.equals(name) && !DEFAULT_FRACTIONATION_TEMPLATE_NAME.equals(name))
        {
            return PageFlowUtil.getFileContentsAsString(getTemplateFile(uriRoot, name));
        }
        else
        {
            String resourceName;
            if (DEFAULT_FRACTIONATION_TEMPLATE_NAME.equals(name))
            {
                resourceName = "org/labkey/ms2/protocol/MassSpecFractionationProtocol.xml";
            }
            else
            {
                resourceName = "org/labkey/ms2/protocol/MassSpecProtocol.xml";
            }
            try
            {
                reader = new BufferedReader(new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream(resourceName)));

                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line);
                    sb.append("\n");
                }
                return sb.toString();
            }
            catch (IOException eio)
            {
                _log.error("Failed to load template resource.", eio);
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException eio)
                    {
                    }
                }
            }
            return "";
        }
    }



}
