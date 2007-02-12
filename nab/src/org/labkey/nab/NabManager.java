package org.labkey.nab;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.log4j.Logger;
import org.apache.struts.upload.FormFile;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;

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

    public static final String PLATE_TEMPLATE_NAME = "NAB";

    public enum PlateProperty
    {
        VirusName(PropertyType.STRING),
        VirusId(PropertyType.STRING),
        HostCell(PropertyType.STRING),
        StudyName(PropertyType.STRING),
        ExperimentDate(PropertyType.STRING),
        ExperimentPerformer(PropertyType.STRING),
        ExperimentId(PropertyType.STRING),
        FileId(PropertyType.INTEGER),
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
        EndpointsOptional(PropertyType.BOOLEAN),
        Slope(PropertyType.DOUBLE),
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
        return new Luc5Assay(plate, getCutoffs(plate));
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
        Luc5Assay assay = new Luc5Assay(group.getPlate(), getCutoffs(group.getPlate()));
        for (DilutionSummary summary : assay.getSummaries())
        {
            if (summary.getWellGroup().getRowId().intValue() == wellGroupRowId)
                return summary;
        }
        return null;
    }

    public Luc5Assay saveResults(Container container, User user, RunMetadata metadata, SampleInfo[] sampleInfos, int[] cutoffs, FormFile datafile) throws SQLException, IOException, BiffException, ServletException
    {
        return createLuc5Assay(container, user, metadata, sampleInfos, cutoffs, datafile);
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

    public PlateTemplate getPlateTemplate(Container container, User user) throws SQLException
    {
        PlateTemplate template = PlateService.get().getPlateTemplate(container, user, PLATE_TEMPLATE_NAME);
        if (template == null)
        {
            template = PlateService.get().createPlateTemplate(container, user, PLATE_TEMPLATE_NAME);
            for (PlateProperty prop : PlateProperty.values())
                template.setProperty(prop.name(), "");

            template.addWellGroup(CELL_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                    PlateService.get().createPosition(container, 0, 0),
                    PlateService.get().createPosition(container, template.getRows() - 1, 0));
            template.addWellGroup(VIRUS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                    PlateService.get().createPosition(container, 0, 1),
                    PlateService.get().createPosition(container, template.getRows() - 1, 1));
            for (int sample = 0; sample < 5; sample++)
            {
                int firstCol = (sample * 2) + 2;
                // create the overall specimen group, consisting of two adjacent columns:
                WellGroupTemplate sampleGroup = template.addWellGroup("Specimen " + (sample + 1), WellGroup.Type.SPECIMEN,
                        PlateService.get().createPosition(container, 0, firstCol),
                        PlateService.get().createPosition(container, 7, firstCol + 1));
                for (SampleProperty prop : SampleProperty.values())
                    sampleGroup.setProperty(prop.name(), "");
                for (int replicate = 0; replicate < template.getRows(); replicate++)
                {
                    template.addWellGroup("Specimen " + (sample + 1) + ", Replicate " + (replicate + 1), WellGroup.Type.REPLICATE,
                            PlateService.get().createPosition(container, replicate, firstCol),
                            PlateService.get().createPosition(container, replicate, firstCol + 1));
                }
            }

            PlateService.get().save(container, user, template);
            template = PlateService.get().getPlateTemplate(container, user, PLATE_TEMPLATE_NAME);
        }
        return template;
    }

    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;

    protected Luc5Assay createLuc5Assay(Container container, User user, RunMetadata metadata, SampleInfo[] sampleInfos, int[] cutoffs, String datafileName, InputStream attachmentStream) throws SQLException, IOException, ServletException, BiffException
    {
        PlateTemplate nabTemplate = getPlateTemplate(container, user);

        Workbook workbook = Workbook.getWorkbook(attachmentStream);
        double[][] cellValues = new double[nabTemplate.getRows()][nabTemplate.getColumns()];

        Sheet plateSheet = workbook.getSheet(1);
        for (int row = 0; row < nabTemplate.getRows(); row++)
        {
            for (int col = 0; col < nabTemplate.getColumns(); col++)
            {
                Cell cell = plateSheet.getCell(col + START_COL, row + START_ROW);
                String cellContents = cell.getContents();
                cellValues[row][col] = Double.parseDouble(cellContents);
            }
        }

        // create plate, and set its properties:
        Plate plate = PlateService.get().createPlate(nabTemplate, user, cellValues);

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
            group.setProperty(SampleProperty.EndpointsOptional.name(), info.isEndpointsOptional());
            group.setProperty(SampleProperty.Factor.name(), info.getFactor());
            group.setProperty(SampleProperty.Method.name(), info.getMethod().name());
            group.setProperty(SampleProperty.Slope.name(), info.getFixedSlope());


            List<WellData> wells = group.getWellData(true);
            boolean first = true;
            double dilution = info.getInitialDilution();
            for (int row = plate.getRows() - 1; row >= 0; row--)
            {
                WellData data = wells.get(row);
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
        Luc5Assay temp = new Luc5Assay(plate, cutoffs);
        for (DilutionSummary dilution : temp.getSummaries())
        {
            WellGroup group = dilution.getWellGroup();
            assert groupInPlate(plate, group) : "Group not found in plate";
            for (int cutoff : cutoffs)
            {
                group.setProperty("Curve IC" + cutoff, dilution.getCutoffDilution((double) cutoff / 100.0));
                group.setProperty("Point IC" + cutoff, dilution.getInterpolatedCutoffDilution((double) cutoff / 100.0));
            }
            group.setProperty(SampleProperty.FitError.name(), dilution.getFitError());
            group.setProperty(SampleProperty.Slope.name(), dilution.getSlope());
        }

        int rowid = PlateService.get().save(container, user, plate);
        plate = PlateService.get().getPlate(container, rowid);
        return new Luc5Assay(plate, cutoffs);
    }

    protected Luc5Assay createLuc5Assay(Container container, User user, RunMetadata metadata, SampleInfo[] sampleInfos, int[] cutoffs, FormFile datafile) throws SQLException, IOException, ServletException, BiffException
    {
        InputStream attachmentStream = null;
        try
        {
            attachmentStream = datafile.getInputStream();
            Luc5Assay assay = createLuc5Assay(container, user, metadata, sampleInfos, cutoffs, datafile.getFileName(), attachmentStream);
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

    public ViewURLHelper getDataFileDownloadLink(Plate plate)
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
            Map<String, Object> properties =
                    PropertyManager.getWritableProperties(context.getUser().getUserId(),
                            context.getContainer().getId(), Luc5Assay.class.getName(), true);
            if (form != null)
                settingsToMap(form, properties);
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
        Map<String, Object> properties = PropertyManager.getProperties(context.getUser().getUserId(),
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

    private void settingsToMap(NabController.UploadAssayForm form, Map<String, Object> targetMap)
    {
        for (int i = 0; i < form.getSampleInfos().length; i++)
        {
            String prefix = "sampleInfo" + i + ".";
            SampleInfo info = form.getSampleInfos()[i];
            targetMap.put(prefix + "factorText", info.getFactorText() != null ? info.getFactorText() : "" + info.getFactor());
            targetMap.put(prefix + "fixedSlope", info.getFixedSlope());
            targetMap.put(prefix + "initialDilution", info.getInitialDilutionText() != null ?
                    info.getInitialDilutionText() : "" + info.getInitialDilution());
            targetMap.put(prefix + "method", info.getMethodName());
            targetMap.put(prefix + "sampleDescription", info.getSampleDescription());
            targetMap.put(prefix + "sampleId", info.getSampleId());
            targetMap.put(prefix + "endpointsOptional", info.isEndpointsOptional());
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
        targetMap.put("slope", form.getRunSettings().getSlopeText() != null ?
                form.getRunSettings().getSlopeText() : form.getRunSettings().getSlope());
        targetMap.put("autoSlope", form.getRunSettings().isAutoSlope());
        targetMap.put("endpointsOptional", form.getRunSettings().isEndpointsOptional());
        targetMap.put("inferFromFile", form.getRunSettings().isInferFromFile());
        targetMap.put("sameFactor", form.getRunSettings().isSameFactor());
        targetMap.put("sameInitialValue", form.getRunSettings().isSameInitialValue());
        targetMap.put("sameMethod", form.getRunSettings().isSameMethod());
    }

    private NabController.UploadAssayForm settingsFromMap(Map<String, Object> properties)
    {
        int sampleCount = 0;
        while (properties.get("sampleInfo" + sampleCount + ".sampleId") != null)
            sampleCount++;
        NabController.UploadAssayForm form = new NabController.UploadAssayForm();
        SampleInfo[] sampleInfos = new SampleInfo[sampleCount];
        for (int i = 0; i < sampleInfos.length; i++)
        {
            String prefix = "sampleInfo" + i + ".";
            SampleInfo info = new SampleInfo((String) properties.get(prefix + "sampleId"));
            info.setFactorText((String) properties.get(prefix + "factorText"));
            try
            {
                String slopeString = (String) properties.get(prefix + "fixedSlope");
                if (slopeString != null)
                    info.setFixedSlope(Double.parseDouble(slopeString));
            }
            catch (NumberFormatException e)
            {
                // fall through and continue: we'll fail to populate the fixed slope, but that's okay.
            }
            info.setInitialDilutionText((String) properties.get(prefix + "initialDilution"));
            info.setMethodName((String) properties.get(prefix + "method"));
            info.setSampleDescription((String) properties.get(prefix + "sampleDescription"));
            info.setEndpointsOptional(Boolean.valueOf((String) properties.get(prefix + "endpointsOptional")));
            sampleInfos[i] = info;
        }
        form.setSampleInfos(sampleInfos);
        // file property:
        form.setFileName((String) properties.get("fileName"));

        // metadata properties:
        RunMetadata metadata = new RunMetadata();
        metadata.setExperimentDateString((String) properties.get("experimentDate"));
        metadata.setExperimentId((String) properties.get("experimentId"));
        metadata.setExperimentPerformer((String) properties.get("experimentPerformer"));
        metadata.setFileId((String) properties.get("fileId"));
        metadata.setHostCell((String) properties.get("hostCell"));
        metadata.setIncubationTime((String) properties.get("incubationTime"));
        metadata.setPlateNumber((String) properties.get("plateNumber"));
        metadata.setStudyName((String) properties.get("studyName"));
        metadata.setVirusId((String) properties.get("virusId"));
        metadata.setVirusName((String) properties.get("virusName"));
        form.setMetadata(metadata);

        // run settings:
        RunSettings settings = form.getRunSettings();
        String cutoffString = (String) properties.get("cutoffs");
        String[] cutoffStrings = cutoffString.split(",");
        SafeTextConverter.PercentConverter[] cutoffConverters = new SafeTextConverter.PercentConverter[RunSettings.MAX_CUTOFF_OPTIONS];
        for (int i = 0; i < RunSettings.MAX_CUTOFF_OPTIONS; i++)
        {
            cutoffConverters[i] = new SafeTextConverter.PercentConverter(null);
            if (i < cutoffStrings.length)
                cutoffConverters[i].setText(cutoffStrings[i]);
        }
        settings.setCutoffs(cutoffConverters);
        settings.setSlopeText((String) properties.get("slope"));
        settings.setAutoSlope(Boolean.valueOf((String) properties.get("autoSlope")));
        settings.setEndpointsOptional(Boolean.valueOf((String) properties.get("endpointsOptional")));
        settings.setInferFromFile(Boolean.valueOf((String) properties.get("inferFromFile")));
        settings.setSameFactor(Boolean.valueOf((String) properties.get("sameFactor")));
        settings.setSameInitialValue(Boolean.valueOf((String) properties.get("sameInitialValue")));
        settings.setSameMethod(Boolean.valueOf((String) properties.get("sameMethod")));
        return form;
    }
}
