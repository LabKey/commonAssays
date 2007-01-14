package org.labkey.ms2.protein.tools;

import java.io.ByteArrayOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: tholzman
 * Date: Oct 31, 2005
 * Time: 2:01:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class NullOutputStream extends ByteArrayOutputStream
{
    public NullOutputStream()
    {
        super(1);
    }

    public void write(int i)
    {
    }
}
