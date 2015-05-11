/*
 * Copyright (c) 2006-2015 LabKey Corporation
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

package org.labkey.ms2.protein;

import org.labkey.ms2.protein.uniprot.uniprot;
import org.labkey.ms2.protein.uniprot.*;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;

/**
 * User: jeckels
 * Date: Jan 6, 2006
 */
public class ParserTree
{
    private ParserTreeNode _root = new ParserTreeNode(null, "ROOT");
    private ParserTreeNode _currentNode = _root;

    private Logger _log;

    public ParserTree(Logger log)
    {
        _log = log;

        ParserTreeNode uniprotNode = new ParserTreeNode(_root, "uniprot", new uniprot(log));
        ParserTreeNode entryNode = new ParserTreeNode(uniprotNode, "entry", new uniprot_entry());
        new ParserTreeNode(entryNode, "accession", new uniprot_entry_accession());
        ParserTreeNode dbReferenceNode = new ParserTreeNode(entryNode, "dbReference", new uniprot_entry_dbReference());
        new ParserTreeNode(dbReferenceNode, "property", new uniprot_entry_dbReference_property());
        ParserTreeNode featureNode = new ParserTreeNode(entryNode, "feature", new uniprot_entry_feature());
        ParserTreeNode locationNode = new ParserTreeNode(featureNode, "location");
        new ParserTreeNode(locationNode, "begin", new uniprot_entry_feature_location_begin());
        new ParserTreeNode(locationNode, "end", new uniprot_entry_feature_location_end());
        ParserTreeNode geneNode = new ParserTreeNode(entryNode, "gene");
        new ParserTreeNode(geneNode, "name", new uniprot_entry_gene_name());
        new ParserTreeNode(entryNode, "keyword", new uniprot_entry_keyword());
        new ParserTreeNode(entryNode, "name", new uniprot_entry_name());
        ParserTreeNode organismNode = new ParserTreeNode(entryNode, "organism", new uniprot_entry_organism());
        new ParserTreeNode(organismNode, "dbReference", new uniprot_entry_organism_dbReference());
        new ParserTreeNode(organismNode, "name", new uniprot_entry_organism_name(log));
        ParserTreeNode proteinNode = new ParserTreeNode(entryNode, "protein");
        new ParserTreeNode(proteinNode, "name", new uniprot_entry_protein_name());
        ParserTreeNode recommendedNameNode = new ParserTreeNode(proteinNode, "recommendedName", new uniprot_entry_protein_recommendedName());
        new ParserTreeNode(recommendedNameNode, "fullName", new uniprot_entry_protein_recommendedName_fullName());
        new ParserTreeNode(recommendedNameNode, "shortName", new uniprot_entry_protein_recommendedName_shortName());
        new ParserTreeNode(entryNode, "sequence", new uniprot_entry_sequence());
    }

    public ParseActions push(String elementName)
    {
        _currentNode = _currentNode.getChild(elementName);
        return _currentNode._parseActions;
    }

    public ParseActions getCurrent()
    {
        return _currentNode._parseActions;
    }

    public String getCurrentDescription()
    {
        return _currentNode.getDescription();
    }

    public void pop()
    {
        _currentNode = _currentNode._parent;
        assert _currentNode != null : "Popped one too many elements off the stack";
    }

    private class ParserTreeNode
    {
        private final Map<String, ParserTreeNode> _children = new HashMap<>();
        private final ParserTreeNode _parent;
        private final String _elementName;
        private final ParseActions _parseActions;

        public ParserTreeNode(ParserTreeNode parent, String name)
        {
            this(parent, name, null);
        }

        public ParserTreeNode(ParserTreeNode parent, String name, ParseActions parseActions)
        {
            _parent = parent;
            _elementName = name;
            _parseActions = parseActions;
            if (_parent != null)
            {
                _parent._children.put(_elementName, this);
            }
        }

        public String getDescription()
        {
            ParserTreeNode node = this;
            StringBuilder sb = new StringBuilder();
            while (node != _root)
            {
                if (sb.length() > 0)
                {
                    sb.insert(0, "_");
                }
                sb.insert(0, node._elementName);
                node = node._parent;
            }
            return sb.toString();
        }

        public ParserTreeNode getChild(String elementName)
        {
            ParserTreeNode result = _children.get(elementName);
            if (result == null)
            {
                result = new ParserTreeNode(this, elementName);
                _log.debug("Detected " + result.getDescription() + " element, but not parsing it.");
                _children.put(elementName, result);
            }
            return result;
        }
    }

}
