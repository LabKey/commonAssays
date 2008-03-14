package org.labkey.microarray;

import org.labkey.api.data.Container;
import org.labkey.api.data.Entity;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.util.MemTracker;
import org.labkey.api.view.ViewContext;

import javax.ejb.*;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;


@javax.ejb.Entity
@Table(name = "FeatureExtractionRuns")
public class FeatureExtractionRun extends Entity implements Serializable, Cloneable
{
    protected byte[] _ts;
    protected int rowId;
    protected String barcode;
    protected String grid;
    protected String protocol;
    protected String description;

    protected String path;
    protected String lowResImage;
    protected String qcReport;
    protected String mageML;
    protected String feature;
    protected String alignment;
    protected int statusid;

    public FeatureExtractionRun()
    {
        assert MemTracker.put(this);
    }

    @Id(generate = GeneratorType.AUTO)
    public int getRowId()
    {
        return rowId;
    }

    public void setRowId(int rowId)
    {
        this.rowId = rowId;
    }

    @Transient
    public String getName() {
        return barcode;
    }

    @Transient
    public String getCreatedByName(ViewContext context)
    {
        return UserManager.getDisplayName(getCreatedBy(), context);
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getGrid() {
        return grid;
    }

    public void setGrid(String grid) {
        this.grid = grid;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLowResImage() {
        return lowResImage;
    }

    public void setLowResImage(String lowResImage) {
        this.lowResImage = lowResImage;
    }

    public String getQcReport() {
        return qcReport;
    }

    public void setQcReport(String qcReport) {
        this.qcReport = qcReport;
    }

    public String getMageML() {
        return mageML;
    }

    public void setMageML(String mageML) {
        this.mageML = mageML;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public int getStatusId() {
        return statusid;
    }

    public void setStatusId(int statusid) {
        this.statusid = statusid;
    }


}
