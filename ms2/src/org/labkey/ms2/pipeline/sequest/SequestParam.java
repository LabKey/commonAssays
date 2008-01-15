package org.labkey.ms2.pipeline.sequest;

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Sep 7, 2006
 * Time: 8:26:21 PM
 */
public class SequestParam extends Param
{

    private String comment;
    private boolean passThrough;

    public SequestParam(
        int sortOrder,
        String value,
        String name,
        List<String> inputXmlLabels,
        String comment,
        IInputXMLConverter converter,
        IParamsValidator validator,
        boolean isPassThrough)
    {
        super(sortOrder,
            value,
            name,
            inputXmlLabels,
            converter,
            validator);
        this.comment = comment;
        this.passThrough = isPassThrough;
    }

    public SequestParam(
        int sortOrder,
        String value,
        String name,
        String comment,
        IInputXMLConverter converter,
        IParamsValidator validator,
        boolean isPassThrough)
    {
        super(sortOrder,
            value,
            name,
            converter,
            validator);
        this.comment = comment;
        this.passThrough = isPassThrough;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getComment()
    {
        return comment;
    }


    public boolean isPassThrough()
    {
        return passThrough;
    }

    public void setPassThrough(boolean passThrough)
    {
        this.passThrough = passThrough;
    }
}
