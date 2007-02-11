package org.labkey.flow.controllers.editscript;

public class EditGatesForm extends EditScriptForm
{
    public String subset;
    public String xAxis;
    public String yAxis;
    public double[] ptX;
    public double[] ptY;
    
    public void setSubset(String subset)
    {
        this.subset = subset;
    }
    public void setXaxis(String axis)
    {
        this.xAxis = axis;
    }
    public void setYaxis(String axis)
    {
        this.yAxis = axis;
    }

    public void setPtX(double[] ptX)
    {
        this.ptX = ptX;
    }

    public void setPtY(double[] ptY)
    {
        this.ptY = ptY;
    }
}
