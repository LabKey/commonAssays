/*
 * Copyright (c) 2005-2013 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protein.tools;

import java.util.*;
import java.util.regex.*;
import java.io.*;

/**
 * User: tholzman
 * Date: Mar 10, 2005
 * Time: 5:26:38 PM
 */
public class REProperties extends java.util.Properties
{
    public Set<String> REGetValues(String pattern)
    {
        Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
        return REGetValues(p);
    }

    private Set<String> REGetValues(Pattern pattern)
    {
        Set<String> retVal = new HashSet<>();
        for (Enumeration e = this.propertyNames(); e.hasMoreElements();)
        {
            String s = (String) e.nextElement();
            Matcher m = pattern.matcher(s);
            boolean b = m.matches();
            if (b)
            {
                String val = this.getProperty(s);
                retVal.add(val);
            }
        }
        return retVal;
    }

    private Map<String, String> REGetProperties(String pattern)
    {
        Pattern p = Pattern.compile(pattern);
        return REGetProperties(p);
    }

    private Map<String, String> REGetProperties(Pattern pattern)
    {
        Map<String,String> retVal = new HashMap<>();
        for (Enumeration e = this.propertyNames(); e.hasMoreElements();)
        {
            String s = (String) e.nextElement();
            if (pattern.matcher(s).matches())
            {
                retVal.put(s, (String) this.get(s));
            }
        }
        return retVal;
    }

    private void loadStream(InputStream is) throws IOException
    {
        if (is != null)
        {
            this.load(is);
            is.close();
        }
    }

    private void loadFile(File file) throws IOException
    {
        if (file != null)
        {
            FileInputStream fis = new FileInputStream(file);
            loadStream(fis);
        }
    }

    // This routine obtains properties needed to run a program.  It
    // has a sequence of places it looks.  Each new properties file
    // or resource that it finds supercedes the duplicate properties
    // of the former ones.  The sequence:
    //
    // (1) Look in the classpath for a resource named '<prefix>.properties'.
    // (2) Look in the home directory for a file named <prefix>.properties
    // (3) Look in the working directory for a file named <prefix.properties>
    // (4) Look for a System property (representing a file or URL) called
    //     (you guessed it) <prefix>.properties, and use properties from
    //     that file or URL
    // (5) Look for System properties (other than <prefix>.properties) that
    //     that start with "<prefix>.".
    //
    // So, command line definitions take highest precedence; resources
    // visible on the classpath take lowest priority.


    public static REProperties loadREProperties(String prefix) throws IOException
    {
        REProperties retVal = new REProperties();
        // (1)
        String myResource = prefix + ".properties";
        InputStream is;
        is = REProperties.class.getResourceAsStream("/META-INF/" + myResource);
//         java.net.URL url = ClassLoader.getSystemResource("/ediproperties/" + myResource);
            //        is = url.openStream();

        if (is != null) retVal.loadStream(is);
        // (2)
        String homeDir = System.getProperty("user.home");
        String fsep = System.getProperty("file.separator");
        if (!homeDir.endsWith(fsep)) homeDir += fsep;
        File propFile = new File(homeDir + prefix + ".properties");
        if (propFile.exists())
            retVal.loadFile(propFile);
        // (3)
        String workingDir = System.getProperty("user.dir");
        if (!workingDir.endsWith(fsep)) workingDir += fsep;
        propFile = new File(workingDir + prefix + ".properties");
        if (propFile.exists())
            retVal.loadFile(propFile);
        // (4)
        String propFileName = System.getProperty(prefix + ".properties");
        if (propFileName != null)
        {
            propFile = new File(propFileName);
            if (propFile.exists())
                retVal.loadFile(propFile);
        }
        // (5)
        REProperties tmp = new REProperties();
        for (Enumeration e = System.getProperties().keys(); e.hasMoreElements();)
        {
            String key = (String) e.nextElement();
            tmp.put(key, System.getProperty(key));
        }
        Map<String,String> h = tmp.REGetProperties(prefix + "\\..*");
        retVal.putAll(h);

        return retVal;
    }
}
