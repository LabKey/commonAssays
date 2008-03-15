package org.labkey.flow.analysis.model;

import org.labkey.api.data.XMLWriter;

import java.io.OutputStream;

/**
 * User: adam
 * Date: Mar 14, 2008
 * Time: 9:02:48 PM
 */
public class FlowJoWorkspaceWriter
{
    private XMLWriter _writer;

    private enum Tag implements XMLWriter.Tag
    {
        Foo(0), Bar(2), Blick(0, 2), Workspace(3, 3), Wicked;

        private int _minAttributes;
        private int _maxAttributes;

        private Tag(int minAttributes, int maxAttributes)
        {
            _minAttributes = minAttributes;
            _maxAttributes = maxAttributes;
        }

        private Tag()
        {
            this(0, Integer.MAX_VALUE);
        }

        private Tag(int attributeCount)
        {
            this(attributeCount, attributeCount);
        }

        public int getMinimumAttributeCount()
        {
            return _minAttributes;
        }

        public int getMaximumAttributeCount()
        {
            return _maxAttributes;
        }
    }

    public FlowJoWorkspaceWriter()
    {
    }

    public void write(OutputStream os)
    {
        _writer = new XMLWriter(os);
        writeDocument();
        _writer.close();
    }

    private void writeDocument()
    {
        _writer.startTag(Tag.Workspace, "this", "1", "that", "2", "theother", "3");
        _writer.startTag(Tag.Foo);
        _writer.endTag();
        _writer.startTag(Tag.Bar, "one", "1", "two", "2");
        _writer.startTag(Tag.Blick, "fum", "flow");
        _writer.endTag();
        _writer.startTag(Tag.Wicked, "this", "1", "that", "2", "theother", "3");
        _writer.endTag();
        _writer.startTag(Tag.Wicked);
        _writer.endTag();
        _writer.endTag();
        _writer.endTag();
    }

    public void close()
    {
        _writer.close();
    }
}
