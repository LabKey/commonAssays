package org.labkey.microarray;

import java.sql.SQLException;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.security.User;
import org.labkey.api.util.ContainerUtil;

public class ArrayManager {
    private static ArrayManager _instance;

    private ArrayManager() {
        // prevent external construction with a private default constructor
    }

    public static synchronized ArrayManager get() {
        if (_instance == null)
            _instance = new ArrayManager();
        return _instance;
    }

    public static TableInfo getTableInfoExtractionRuns() {
        return MicroarraySchema.getSchema().getTable("ExtractionRuns");
    }

    public static FeatureExtractionRun getFeatureExtractionRun(Container c, int runId) throws SQLException {
        FeatureExtractionRun[] featureExtractionRuns = Table.selectForDisplay(
                getTableInfoExtractionRuns(),
                Table.ALL_COLUMNS,
                new SimpleFilter("rowId", new Integer(runId))
                .addCondition("container", c.getId()),
                null, FeatureExtractionRun.class);
        if (null == featureExtractionRuns || featureExtractionRuns.length < 1)
            return null;
        FeatureExtractionRun featureExtractionRun = featureExtractionRuns[0];

        return featureExtractionRun;

    }

    public static void saveFeatureExtractionRun(User user, Container c, FeatureExtractionRun run) throws SQLException {
        try {
            if (run.rowId == 0) {
                run.beforeInsert(user, c.getId());
                Table.insert(user, getTableInfoExtractionRuns(), run);
            } else {
                run.beforeUpdate(user);
                Table.update(user, getTableInfoExtractionRuns(), run, new Integer(run.getRowId()), null);
            }
        } finally {
        }
    }

    public static void deleteFeatureExtractionRun(int runId, Container container, User user)  throws SQLException {
        FeatureExtractionRun run = getFeatureExtractionRun(container, runId);
        run.setStatusId(0);
        saveFeatureExtractionRun(user, container, run);
    }

    public static void deleteFeatureExtractionRunsByRowIds(String[] runIds, Container container, User user) throws SQLException {
        try {
            MicroarraySchema.getSchema().getScope().beginTransaction();
            for (String runId : runIds) {
                deleteFeatureExtractionRun(Integer.parseInt(runId), container, user);
            }
            MicroarraySchema.getSchema().getScope().commitTransaction();
        } finally {
            if (MicroarraySchema.getSchema().getScope().isTransactionActive())
                MicroarraySchema.getSchema().getScope().rollbackTransaction();
        }
    }

    public static void purgeContainer(Container c) {
        try {
            MicroarraySchema.getSchema().getScope().beginTransaction();
            ContainerUtil.purgeTable(getTableInfoExtractionRuns(), c, null);
            MicroarraySchema.getSchema().getScope().commitTransaction();
        } catch (SQLException x) {
        } finally {
            MicroarraySchema.getSchema().getScope().closeConnection();
        }
    }


    public static String purge() throws SQLException {
        String message = "";

        try {
            MicroarraySchema.getSchema().getScope().beginTransaction();
            int runsDeleted = ContainerUtil.purgeTable(getTableInfoExtractionRuns(), null);
            MicroarraySchema.getSchema().getScope().commitTransaction();

            message = "deleted " + runsDeleted + " feature extraction runs<br>\n";
        } finally {
            if (MicroarraySchema.getSchema().getScope().isTransactionActive())
                MicroarraySchema.getSchema().getScope().rollbackTransaction();
        }

        return message;
    }

    public static String getDataRegionNameExtractionRuns() {
        return "FeatureExtractionRuns";
    }

    public static Sort getRunsBaseSort() {
        return new Sort("-RowId");
    }

    public static Sort getRunsBarcodeSort() {
        return new Sort("-Barcode");
    }

    public static SimpleFilter getRunsNotDeletedFilter() {
        return new SimpleFilter("StatusId", new Integer(0));
    }
}