/*
 * Copyright (c) 2005-2012 LabKey Corporation
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
package org.labkey.flow;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.fhcrc.cpas.flow.script.xml.ScriptDef;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.xml.sax.SAXParseException;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public class ScriptParser
{
    List<Error> _errors;
    ScriptDef _script;

    public ScriptParser()
    {
    }

    static public class Error
    {
        String _message;
        int _line;
        int _column;

        public Error(String message)
        {
            this(message, 0, 0);
        }

        public Error(String message, int line, int column)
        {
            _message = message;
            _line = line;
            _column = column;
        }

        public Error(SAXParseException spe)
        {
            this(spe.getLocalizedMessage(), spe.getLineNumber(), spe.getColumnNumber());
        }

        public String getMessage()
        {
            return _message;
        }

        public int getLine()
        {
            return _line;
        }

        public int getColumn()
        {
            return _column;
        }
    }

    public void parse(String script)
    {
        try
        {
            XmlOptions options = new XmlOptions();
            List<XmlError> errors = new ArrayList();
            options.setDocumentType(ScriptDocument.type);
            script = StringUtils.replace(script, "<script>", "<script xmlns=\"" + ScriptDocument.type.getContentModel().getName().getNamespaceURI() + "\">");
            ScriptDocument doc = ScriptDocument.Factory.parse(new StringReader(script), options);
            options.setErrorListener(errors);

            if (!doc.validate(options))
            {
                for (XmlError xmlError : errors)
                {
                    String message = xmlError.getMessage();
                    message = StringUtils.replace(message, "@" + ScriptDocument.type.getContentModel().getName().getNamespaceURI(), "");
                    String location = xmlError.getCursorLocation().xmlText();
                    if (location.length() > 100)
                        location = location.substring(0, 100);
                    addError(new Error("Schema Validation Error: " + message + "\nLocation of invalid XML: " + location));
                }
            }
            _script = doc.getScript();
        }
        catch (XmlException xmlException)
        {
            if (xmlException.getErrors().size() == 0)
            {
                addError(new Error(xmlException.toString()));
            }
            else
            {
                for (XmlError xmlError : (Collection<XmlError>) xmlException.getErrors())
                {
                    addError(new Error(xmlError.getMessage(), xmlError.getLine(), xmlError.getColumn() > 0 ? xmlError.getColumn() : 1));
                }
            }
        }
        catch (Exception e)
        {
            addError(new Error(e.toString()));
        }
    }

    void addError(Error error)
    {
        if (_errors == null)
            _errors = new ArrayList();
        _errors.add(error);
    }

    public Error[] getErrors()
    {
        if (_errors == null || _errors.size() == 0)
            return null;
        return _errors.toArray(new Error[0]);
    }
}
