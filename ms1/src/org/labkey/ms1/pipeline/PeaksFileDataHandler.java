/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

package org.labkey.ms1.pipeline;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.pipeline.ParamParser;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.security.User;
import org.labkey.api.util.FileType;
import org.labkey.api.util.massSpecDataFileType;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Manager;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Imports the Peaks XML file format used by Ceadars-Sinai
 *
 * User: DaveS
 * Date: Sep 25, 2007
 * Time: 9:15:46 AM
 */
public class PeaksFileDataHandler extends AbstractExperimentDataHandler
{
    private static final String IMPORT_PEAKS_SETTING_NAME = "pipeline, import peaks";


    public static final massSpecDataFileType FT_MZXML = new massSpecDataFileType();
    public static final FileType FT_PEAKS = new FileType(".peaks.xml");

    @Override
    public DataType getDataType()
    {
        return null;
    }

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if(null == data || null == dataFile || null == info || null == log || null == context)
            return;

        try
        {
            if (MS1Manager.get().isAlreadyImported(dataFile,data))
            {
                log.info("The file " + dataFile.toURI() + " has already been imported for this experiment into this container.");
                return;
            }

            //because peak files can be huge, we do not load them under a transaction, which would escallate to a table lock
            //however, this also means that if the server dies during an import, we could have half-imported data
            //sitting in the database for this same experiment data file id.
            //this method will mark those for deletion (see PurgeTask)
            //note that this assumes the pipeline will never allow two user to load the same file at the same time
            MS1Manager.get().deleteFailedImports(data.getRowId(), MS1Manager.FILETYPE_PEAKS);

            File settingsFile = new File(dataFile.getParentFile(), "inspect.xml");
            if (!settingsFile.isFile())
            {
                settingsFile = new File(dataFile.getParentFile(), "pepmatch.xml");
            }
            if (settingsFile.isFile())
            {
                ParamParser parser = PipelineJobService.get().createParamParser();
                parser.parse(new FileInputStream(settingsFile));

                String loadPeaksSetting = parser.getInputParameters().get(IMPORT_PEAKS_SETTING_NAME);
                if ("false".equalsIgnoreCase(loadPeaksSetting) || "no".equalsIgnoreCase(loadPeaksSetting))
                {
                    log.info("Skipping import of data from " + dataFile.getName() + " due to setting of \"" + IMPORT_PEAKS_SETTING_NAME + "\" in protocol parameters.");
                    return;
                }
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            SAXParser parser = factory.newSAXParser();
            try (DbScope.Transaction transaction = DbSchema.get(MS1Manager.SCHEMA_NAME).getScope().ensureTransaction())
            {
                PeaksFileImporter importer = new PeaksFileImporter(data, getMzXmlFilePath(data), info.getUser(), log, transaction);
                parser.parse(dataFile, importer);
                transaction.commit();
            }
        }
        catch(IOException | ParserConfigurationException | SAXException e)
        {
            throw new ExperimentException(e);
        }
    } //importFile()

    /**
     * Returns the master mzXML file path for the data file
     * @param data  Experiment data object
     * @return      Path to the mzXML File
     */
    protected static String getMzXmlFilePath(ExpData data)
    {
        ExpRun run = data.getRun();
        if(null == run)
            return null;

        List<? extends ExpData> inputs = run.getInputDatas(null, null);
        if(null == inputs)
            return null;
        
        for(ExpData input : inputs)
        {
            if(FT_MZXML.isType(input.getDataFileUrl()))
                return input.getDataFileUrl();
        }
        return null;
    } //getMzXmlFilePath()

    public void deleteData(ExpData data, Container container, User user)
    {
        if(null == data || null == container || null == user)
            return;

        MS1Manager.get().deletePeakData(data.getRowId());
    }


    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        if(null == newData || null == user) //anything else?
            return;

        MS1Manager.get().moveFileData(oldDataRowID, newData.getRowId());
    } //runMoved()

    public ActionURL getContentURL(ExpData data)
    {
        ActionURL url = new ActionURL(MS1Controller.ShowPeaksAction.class, data.getContainer());
        url.addParameter("dataRowId", Integer.toString(data.getRowId()));
        return url;
    } //getContentURL()

    public Priority getPriority(ExpData data)
    {
        //we handle only *.peaks.xml files
        return (null != data && null != data.getDataFileUrl() && 
                FT_PEAKS.isType(data.getDataFileUrl())) ? Priority.MEDIUM : null;
    } //Priority()

} //class PeaksFileDataHandler
