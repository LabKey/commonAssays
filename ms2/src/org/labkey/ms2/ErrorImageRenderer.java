/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

public class ErrorImageRenderer
{
    private static final String imageType = "png";

    protected final Color _foregroundColor = Color.black;
    private final String _message;
    private final int _width;
    private final int _height;

    public ErrorImageRenderer(String message, int width, int height)
    {
        _message = message;
        _width = width;
        _height = height;
    }


    public void render(HttpServletResponse response)
            throws IOException
    {
        BufferedImage bi = new BufferedImage(_width, _height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, _width, _height);
        g.setColor(_foregroundColor);
        OutputStream outputStream = response.getOutputStream();
        response.setContentType("image/png");

        StringTokenizer st = new StringTokenizer(_message, "\n", false);
        int lineCount = st.countTokens();
        int lineHeight = g.getFontMetrics().getHeight();
        int lineIndex = 1;
        while (st.hasMoreTokens())
        {
            String line = st.nextToken();
            g.drawString(line, (_width - g.getFontMetrics().stringWidth(line)) / 2, (_height - lineHeight) / 2 - (lineCount - lineIndex++) * lineHeight);
        }
        ImageIO.write(bi, imageType, outputStream);
        outputStream.flush();
    }
}
