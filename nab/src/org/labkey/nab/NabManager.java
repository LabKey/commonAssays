/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.nab;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.log4j.Logger;
import org.apache.struts.upload.FormFile;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.attachments.AttachmentService;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brittp
 * Date: Oct 26, 2006
 * Time: 4:13:59 PM
 */
public class NabManager
{
    private static Logger _log = Logger.getLogger(NabManager.class);
    private static NabManager _instance;

    public static final String CELL_CONTROL_SAMPLE = "CELL_CONTROL_SAMPLE";
    public static final String VIRUS_CONTROL_SAMPLE = "VIRUS_CONTROL_SAMPLE";
    public static final String DATATYPE_RUNPARAMS = "RunParameters";
    public static final String DATATYPE_SPECIMEN = "SpecimenData";

    public static final String DEFAULT_TEMPLATE_NAME = "NAb: 5 specimens in duplicate";

    public enum PlateProperty
    {
        VirusName(PropertyType.STRING),
        VirusId(PropertyType.STRING),
        HostCell(PropertyType.STRING),
        StudyName(PropertyType.STRING),
        ExperimentDate(PropertyType.STRING),
        ExperimentPerformer(PropertyType.STRING),
        ExperimentId(PropertyType.STRING),
        FileId(PropertyType.STRING),
        IncubationTime(PropertyType.STRING),
        PlateNumber(PropertyType.STRING),
        Cutoffs(PropertyType.STRING),
        DataFile(PropertyType.STRING);

        private PropertyType _type;

        private PlateProperty(PropertyType type)
        {
            _type = type;
        }

        public PropertyType getType()
        {
            return _type;
        }
    }

    public enum SampleProperty
    {
        InitialDilution(PropertyType.DOUBLE),
        SampleId(PropertyType.STRING),
        SampleDescription(PropertyType.STRING),
        Factor(PropertyType.DOUBLE),
        Method(PropertyType.STRING),
        FitError(PropertyType.DOUBLE);

        private PropertyType _type;

        private SampleProperty(PropertyType type)
        {
            _type = type;
        }

        public PropertyType getType()
        {
            return _type;
        }
    }

    public static NabManager get()
    {
        if (_instance == null)
            _instance = new NabManager();
        return _instance;
    }
    
    public void deleteContainerData(Container container) throws SQLException
    {
        PlateService.get().deleteAllPlateData(container);
    }

    public Luc5Assay loadFromDatabase(User user, Container container, int rowid) throws Exception
    {
        Plate plate = PlateService.get().getPlate(container, rowid);
        return new Luc5Assay(plate, getCutoffs(plate), DilutionCurve.FitType.FIVE_PARAMETER);
    }

    public void deletePlate(Container container, int rowid) throws SQLException
    {
        Plate plate = PlateService.get().getPlate(container, rowid);
        if (plate != null)
        {
            PlateService.get().deletePlate(container, rowid);
        }
    }

    public DilutionSummary getDilutionSummary(Container container, int wellGroupRowId) throws SQLException
    {
        WellGroup group = PlateService.get().getWellGroup(container, wellGroupRowId);
        if (group == null)
            return null;
        Luc5Assay assay = new Luc5Assay(group.getPlate(), getCutoffs(group.getPlate()), DilutionCurve.FitType.FIVE_PARAMETER);
        for (DilutionSummary summary : assay.getSummaries())
        {
            if (summary.getWellGroup().getRowId().intValue() == wellGroupRowId)
                return summary;
        }
        return null;
    }

    public Luc5Assay saveResults(Container container, User user, String plateTemplate, RunMetadata metadata, SampleInfo[] sampleInfos, int[] cutoffs, FormFile datafile) throws SQLException, IOException, BiffException, ServletException, AttachmentService.DuplicateFilenameException
    {
        return createLuc5Assay(container, user, plateTemplate, metadata, sampleInfos, cutoffs, datafile);
    }

    private int[] getCutoffs(Plate plate)
    {
        String cutoffsString = (String) plate.getProperty(PlateProperty.Cutoffs.name());
        String[] cutoffArray = cutoffsString.split(",");
        int[] cutoffs = new int[cutoffArray.length];
        try
        {
            for (int i = 0; i < cutoffArray.length; i++)
                cutoffs[i] = Integer.parseInt(cutoffArray[i]);
            return cutoffs;
        }
        catch (NumberFormatException e)
        {
            // log, then fall through to return the default:
            _log.error("Invalid cutoff values saved: " + cutoffsString, e);
            throw e;
        }
    }

    public synchronized PlateTemplate ensurePlateTemplate(Container container, User user) throws SQLException
    {
        NabPlateTypeHandler nabHandler = new NabPlateTypeHandler();
        PlateTemplate template;
        PlateTemplate[] templates = PlateService.get().getPlateTemplates(container);
        if (templates == null || templates.length == 0)
        {
            template = nabHandler.createPlate("Default", container);
            template.setName(DEFAULT_TEMPLATE_NAME);
            PlateService.get().save(container, user, template);
        }
        else
            template = templates[0];
        return template;
    }

    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;

    public List<String> isValidNabPlateTemplate(Container container, User user, String plateTemplate)
    {
        try
        {
            PlateTemplate nabTemplate = PlateService.get().getPlateTemplate(container, plateTemplate);
            List<String> errors = new ArrayList<String>();
            if (nabTemplate == null)
                errors.add("Plate template " + plateTemplate + " no longer exists.");
            else
            {
                Set<String> controlGroups = new HashSet<String>();
                int specimenCount = 0;
                for (WellGroupTemplate groupTemplate : nabTemplate.getWellGroups())
                {
                    if (groupTemplate.getType() == WellGroup.Type.CONTROL)
                        controlGroups.add(groupTemplate.getName());
                    if (groupTemplate.getType() == WellGroup.Type.SPECIMEN)
                        specimenCount++;
                }
                if (!controlGroups.contains(CELL_CONTROL_SAMPLE))
                    errors.add("Plate template \"" + plateTemplate + "\" does not contain required cell control group with name \"" + CELL_CONTROL_SAMPLE + "\"");
                if (!controlGroups.contains(VIRUS_CONTROL_SAMPLE))
                    errors.add("Plate template \"" + plateTemplate + "\" does not contain required virus control group with name \"" + VIRUS_CONTROL_SAMPLE + "\"");
                if (specimenCount == 0)
                    errors.add("Plate template \"" + plateTemplate + "\" does not contain any specimen groups.");
            }
            return errors;
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    protected Luc5Assay createLuc5Assay(Container container, User user, String plateTemplate, RunMetadata metadata, SampleInfo[] sampleInfos, int[] cutoffs, String datafileName, InputStream attachmentStream) throws SQLException, IOException, ServletException, BiffException
    {
        PlateTemplate nabTemplate = PlateService.get().getPlateTemplate(container, plateTemplate);

        WorkbookSettings settings = new WorkbookSettings();
        settings.setGCDisabled(true);
        Workbook workbook = Workbook.getWorkbook(attachmentStream, settings);
        double[][] cellValues = new double[nabTemplate.getRows()][nabTemplate.getColumns()];

        if (workbook.getNumberOfSheets() < 2)
            throw new IOException("Invalid file format: plate data was expected on worksheet 2, but only 1 worksheet was found.");
        Sheet plateSheet = workbook.getSheet(1);
        for (int row = 0; row < nabTemplate.getRows(); row++)
        {
            for (int col = 0; col < nabTemplate.getColumns(); col++)
            {
                Cell cell = plateSheet.getCell(col + START_COL, row + START_ROW);

                String cellContents = null;
                if (cell != null)
                    cellContents = cell.getContents();
                if (cellContents == null || cellContents.length() == 0)
                    throw new IOException("Invalid file format: plate data was expected in cell (" +
                            (col + START_COL + 1) + ", " + (row + START_ROW + 1) + "), but was not found.");

                try
                {
                    cellValues[row][col] = Double.parseDouble(cellContents);
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Invalid file format: numeric data was expected in cell (" + (col + START_COL + 1) + ", "
                            + (row + START_ROW + 1) + "), but was not found: " + e.getMessage());
                }
            }
        }

        // create plate, and set its properties:
        Plate plate = PlateService.get().createPlate(nabTemplate, cellValues);

        if (cutoffs == null || cutoffs.length == 0)
            cutoffs = new int[]{50, 80};
        else
            Arrays.sort(cutoffs);
        StringBuilder cutoffString = new StringBuilder();
        for (int i = 0; i < cutoffs.length; i++)
        {
            if (i > 0)
                cutoffString.append(",");
            cutoffString.append(cutoffs[i]);
        }

        plate.setProperty(PlateProperty.VirusName.name(), metadata.getVirusName());
        plate.setProperty(PlateProperty.VirusId.name(), metadata.getVirusId());
        plate.setProperty(PlateProperty.HostCell.name(), metadata.getHostCell());
        plate.setProperty(PlateProperty.StudyName.name(), metadata.getStudyName());
        plate.setProperty(PlateProperty.ExperimentDate.name(), metadata.getExperimentDate());
        plate.setProperty(PlateProperty.ExperimentPerformer.name(), metadata.getExperimentPerformer());
        plate.setProperty(PlateProperty.ExperimentId.name(), metadata.getExperimentId());
        plate.setProperty(PlateProperty.FileId.name(), metadata.getFileId());
        plate.setProperty(PlateProperty.IncubationTime.name(), metadata.getIncubationTime());
        plate.setProperty(PlateProperty.PlateNumber.name(), metadata.getPlateNumber());
        plate.setProperty(PlateProperty.Cutoffs.name(), cutoffString.toString());
        plate.setProperty(PlateProperty.DataFile.name(), datafileName);

        // set sample group properties:
        List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
        for (int i = 0; i < sampleInfos.length; i++)
        {
            SampleInfo info = sampleInfos[i];
            WellGroup group = specimenGroups.get(i);
            group.setProperty(SampleProperty.InitialDilution.name(), info.getInitialDilution());
            group.setProperty(SampleProperty.SampleId.name(), info.getSampleId());
            group.setProperty(SampleProperty.SampleDescription.name(), info.getSampleDescription());
            group.setProperty(SampleProperty.Factor.name(), info.getFactor());
            group.setProperty(SampleProperty.Method.name(), info.getMethod().name());

            List<WellData> wells = group.getWellData(true);
            boolean first = true;
            double dilution = info.getInitialDilution();
            for (int groupIndex = wells.size() - 1; groupIndex >= 0; groupIndex--)
            {
                WellData data = wells.get(groupIndex);
                if (!first)
                {
                    if (info.getMethod() == SampleInfo.Method.Dilution)
                        dilution *= info.getFactor();
                    else if (info.getMethod() == SampleInfo.Method.Concentration)
                        dilution /= info.getFactor();
                }
                else
                    first = false;
                data.setDilution(dilution);
            }
        }

        // unfortunately, we have to double-create the assay object.
        // this is a chicken-or-egg problem; we want to return the object
        // actually retrieved from the database (so all rowids are properly
        // populated), but we also need to save the cutoff dilutions with the
        // plate, which requires creating an assay instance before saving.
        Luc5Assay temp = new Luc5Assay(plate, cutoffs, DilutionCurve.FitType.FIVE_PARAMETER);
        for (DilutionSummary dilution : temp.getSummaries())
        {
            WellGroup group = dilution.getWellGroup();
            try
            {
                assert groupInPlate(plate, group) : "Group not found in plate";
                for (int cutoff : cutoffs)
                {
                    group.setProperty("Curve IC" + cutoff, dilution.getCutoffDilution((double) cutoff / 100.0));
                    group.setProperty("Point IC" + cutoff, dilution.getInterpolatedCutoffDilution((double) cutoff / 100.0));
                }
                group.setProperty(SampleProperty.FitError.name(), dilution.getFitError());
            }
            catch (DilutionCurve.FitFailedException e)
            {
                throw new IOException(e.getMessage());
            }
        }

        int rowid = PlateService.get().save(container, user, plate);
        plate = PlateService.get().getPlate(container, rowid);
        return new Luc5Assay(plate, cutoffs, DilutionCurve.FitType.FIVE_PARAMETER);
    }

    protected Luc5Assay createLuc5Assay(Container container, User user, String plateTemplate, RunMetadata metadata, SampleInfo[] sampleInfos, int[] cutoffs, FormFile datafile) throws SQLException, IOException, ServletException, BiffException, AttachmentService.DuplicateFilenameException
    {
        InputStream attachmentStream = null;
        try
        {
            attachmentStream = datafile.getInputStream();
            Luc5Assay assay = createLuc5Assay(container, user, plateTemplate, metadata,
                    sampleInfos, cutoffs, datafile.getFileName(), attachmentStream);
            PlateService.get().setDataFile(user, assay.getPlate(), datafile);
            return assay;
        }
        finally
        {
            if (attachmentStream != null) try
            {
                attachmentStream.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    public ActionURL getDataFileDownloadLink(Plate plate)
    {
        return PlateService.get().getDataFileURL(plate, "Nab");
    }

    private boolean groupInPlate(Plate plate, WellGroup group)
    {
        for (WellGroup potential : plate.getWellGroups())
        {
            if (potential == group)
                return true;
        }
        return false;
    }

    public void saveAsLastInputs(ViewContext context, NabController.UploadAssayForm form)
    {
        try
        {
            Map<String, String> properties =
                    PropertyManager.getWritableProperties(context.getUser().getUserId(),
                            context.getContainer().getId(), Luc5Assay.class.getName(), true);
            if (form != null)
                settingsToMap(form, properties);
            else
                properties.clear();
            PropertyManager.saveProperties(properties);
        }
        catch (Exception e)
        {
            _log.error("Unable to save last inputs", e);
            ExceptionUtil.logExceptionToMothership(context.getRequest(), e);
            // fall through; it's not critical that we save the last inputs
        }
    }

    public NabController.UploadAssayForm getLastInputs(ViewContext context)
    {
        Map<String, String> properties = PropertyManager.getProperties(context.getUser().getUserId(),
                context.getContainer().getId(), Luc5Assay.class.getName(), false);
        if (properties != null && !properties.isEmpty())
        {
            try
            {
                return settingsFromMap(properties);
            }
            catch (Exception e)
            {
                _log.error("Unable to retrieve last inputs", e);
                ExceptionUtil.logExceptionToMothership(context.getRequest(), e);
            }
        }
        return new NabController.UploadAssayForm(true);
    }

    public Map<String, PropertyType> getPropertyTypes(List<Plate> plates)
    {
        Map<String, PropertyType> types = new HashMap<String, PropertyType>();
        for (SampleProperty property : SampleProperty.values())
            types.put(property.name(), property.getType());
        for (PlateProperty property : PlateProperty.values())
            types.put(property.name(), property.getType());
        for (Plate plate : plates)
        {
            int[] cutoffs = getCutoffs(plate);
            for (int cutoff : cutoffs)
            {
                types.put("Curve IC" + cutoff, PropertyType.DOUBLE);
                types.put("Point IC" + cutoff, PropertyType.DOUBLE);
            }
        }
        return Collections.unmodifiableMap(types);
    }

    private void settingsToMap(NabController.UploadAssayForm form, Map<String, String> targetMap)
    {
        targetMap.put("plateTemplateName", form.getPlateTemplate());
        for (int i = 0; i < form.getSampleInfos().length; i++)
        {
            String prefix = "sampleInfo" + i + ".";
            SampleInfo info = form.getSampleInfos()[i];
            targetMap.put(prefix + "factorText", info.getFactorText() != null ? info.getFactorText() : "" + info.getFactor());
            targetMap.put(prefix + "initialDilution", info.getInitialDilutionText() != null ?
                    info.getInitialDilutionText() : "" + info.getInitialDilution());
            targetMap.put(prefix + "method", info.getMethodName());
            targetMap.put(prefix + "sampleDescription", info.getSampleDescription());
            targetMap.put(prefix + "sampleId", info.getSampleId());
        }
        // file property:
        targetMap.put("fileName", form.getFileName());

        // metadata properties:
        targetMap.put("experimentDate", form.getMetadata().getExperimentDateString());
        targetMap.put("experimentId", form.getMetadata().getExperimentId());
        targetMap.put("experimentPerformer", form.getMetadata().getExperimentPerformer());
        targetMap.put("fileId", form.getMetadata().getFileId());
        targetMap.put("hostCell", form.getMetadata().getHostCell());
        targetMap.put("incubationTime", form.getMetadata().getIncubationTime());
        targetMap.put("plateNumber", form.getMetadata().getPlateNumber());
        targetMap.put("studyName", form.getMetadata().getStudyName());
        targetMap.put("virusId", form.getMetadata().getVirusId());
        targetMap.put("virusName", form.getMetadata().getVirusName());

        // run settings:
        StringBuilder cutoffs = new StringBuilder();
        for (int i = 0; i < form.getRunSettings().getCutoffs().length; i++)
        {
            SafeTextConverter.PercentConverter cutoff = form.getRunSettings().getCutoffs()[i];
            if (cutoff.getText() != null || cutoff.getValue() != null)
            {
                String value = cutoff.getText() != null ? cutoff.getText() : "" + cutoff.getValue();
                cutoffs.append(i > 0 ? "," : "").append(value);
            }
        }
        targetMap.put("cutoffs", cutoffs.toString());
        targetMap.put("inferFromFile", String.valueOf(form.getRunSettings().isInferFromFile()));
        targetMap.put("sameFactor", String.valueOf(form.getRunSettings().isSameFactor()));
        targetMap.put("sameInitialValue", String.valueOf(form.getRunSettings().isSameInitialValue()));
        targetMap.put("sameMethod", String.valueOf(form.getRunSettings().isSameMethod()));
    }

    private NabController.UploadAssayForm settingsFromMap(Map<String, String> properties)
    {
        int sampleCount = 0;
        while (properties.get("sampleInfo" + sampleCount + ".sampleId") != null)
            sampleCount++;
        NabController.UploadAssayForm form = new NabController.UploadAssayForm(false);

        form.setPlateTemplate((String) properties.get("plateTemplateName"));
        SampleInfo[] sampleInfos = new SampleInfo[sampleCount];
        for (int i = 0; i < sampleInfos.length; i++)
        {
            String prefix = "sampleInfo" + i + ".";
            SampleInfo info = new SampleInfo(properties.get(prefix + "sampleId"));
            info.setFactorText(properties.get(prefix + "factorText"));
            info.setInitialDilutionText(properties.get(prefix + "initialDilution"));
            info.setMethodName(properties.get(prefix + "method"));
            info.setSampleDescription(properties.get(prefix + "sampleDescription"));
            sampleInfos[i] = info;
        }
        form.setSampleInfos(sampleInfos);
        // file property:
        form.setFileName(properties.get("fileName"));

        // metadata properties:
        RunMetadata metadata = new RunMetadata();
        if (properties.get("experimentDate") != null)
        {
            metadata.setExperimentDateString(properties.get("experimentDate"));
        }
        if (properties.get("experimentId") != null)
        {
            metadata.setExperimentId(properties.get("experimentId"));
        }
        if (properties.get("experimentPerformer") != null)
        {
            metadata.setExperimentPerformer(properties.get("experimentPerformer"));
        }
        if (properties.get("fileId") != null)
        {
            metadata.setFileId(properties.get("fileId"));
        }
        if (properties.get("hostCell") != null)
        {
            metadata.setHostCell(properties.get("hostCell"));
        }
        if (properties.get("incubationTime") != null)
        {
            metadata.setIncubationTime(properties.get("incubationTime"));
        }
        if (properties.get("plateNumber") != null)
        {
            metadata.setPlateNumber(properties.get("plateNumber"));
        }
        if (properties.get("studyName") != null)
        {
            metadata.setStudyName(properties.get("studyName"));
        }
        if (properties.get("virusId") != null)
        {
            metadata.setVirusId(properties.get("virusId"));
        }
        if (properties.get("virusName") != null)
        {
            metadata.setVirusName(properties.get("virusName"));
        }
        form.setMetadata(metadata);

        // run settings:
        RunSettings settings = form.getRunSettings();
        String cutoffString = properties.get("cutoffs");
        String[] cutoffStrings = cutoffString.split(",");
        SafeTextConverter.PercentConverter[] cutoffConverters = new SafeTextConverter.PercentConverter[RunSettings.MAX_CUTOFF_OPTIONS];
        for (int i = 0; i < RunSettings.MAX_CUTOFF_OPTIONS; i++)
        {
            cutoffConverters[i] = new SafeTextConverter.PercentConverter(null);
            if (i < cutoffStrings.length)
                cutoffConverters[i].setText(cutoffStrings[i]);
        }
        settings.setCutoffs(cutoffConverters);
        settings.setInferFromFile(Boolean.valueOf(properties.get("inferFromFile")));
        settings.setSameFactor(Boolean.valueOf(properties.get("sameFactor")));
        settings.setSameInitialValue(Boolean.valueOf(properties.get("sameInitialValue")));
        settings.setSameMethod(Boolean.valueOf(properties.get("sameMethod")));
        return form;
    }
}
