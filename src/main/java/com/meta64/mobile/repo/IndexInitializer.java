/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.meta64.mobile.repo;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import com.meta64.mobile.util.JcrRunnable;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexFormatVersion;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IndexInitializer configures the repository with required fulltext index
 */
class IndexInitializer extends JcrRunnable {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void run(Session session) throws RepositoryException {
        if (!session.nodeExists("/oak:index/lucene")) {
            createFullTextIndex(session, "/oak:index/lucene");
        }
        if (!session.nodeExists("/oak:index/meta64")) {
            createPropertyIndexes(session, "/oak:index/meta64");
        }
        session.save();
    }

    private void createPropertyIndexes(Session s, String indexPath) throws RepositoryException {
        Node lucene = createLuceneIndexNode(s, indexPath);

        Node indexRules = lucene.addNode(LuceneIndexConstants.INDEX_RULES, JcrConstants.NT_UNSTRUCTURED);
        Node ntBaseRule = indexRules.addNode(JcrConstants.NT_BASE);

        Node propNode = ntBaseRule.addNode(LuceneIndexConstants.PROP_NODE);

        configureProperty(propNode.addNode("lastModified"), "jcr:lastModified", true);
        configureProperty(propNode.addNode("code"), "code", true);
    }

    private void createFullTextIndex(Session s, String indexPath) throws RepositoryException {
        Node lucene = createLuceneIndexNode(s, indexPath);

        Node indexRules = lucene.addNode(LuceneIndexConstants.INDEX_RULES, JcrConstants.NT_UNSTRUCTURED);
        Node ntBaseRule = indexRules.addNode(JcrConstants.NT_BASE);

        //Fulltext index only includes property of type String and Binary
        ntBaseRule.setProperty(LuceneIndexConstants.INCLUDE_PROPERTY_TYPES,
                new String[]{PropertyType.TYPENAME_BINARY, PropertyType.TYPENAME_STRING});

        Node propNode = ntBaseRule.addNode(LuceneIndexConstants.PROP_NODE);

        Node allPropNode = propNode.addNode("allProps");
        allPropNode.setProperty(LuceneIndexConstants.PROP_ANALYZED, true);
        allPropNode.setProperty(LuceneIndexConstants.PROP_NODE_SCOPE_INDEX, true);
        allPropNode.setProperty(LuceneIndexConstants.PROP_NAME, LuceneIndexConstants.REGEX_ALL_PROPS);
        allPropNode.setProperty(LuceneIndexConstants.PROP_IS_REGEX, true);
        allPropNode.setProperty(LuceneIndexConstants.PROP_USE_IN_SPELLCHECK, true);

        //Create aggregates for nt:file
        Node aggNode = lucene.addNode(LuceneIndexConstants.AGGREGATES);

        Node aggFile = aggNode.addNode(JcrConstants.NT_FILE);
        aggFile.addNode("include0").setProperty(LuceneIndexConstants.AGG_PATH, JcrConstants.JCR_CONTENT);

        log.info("Created fulltext index definition at {}", indexPath);
    }

    private static void configureProperty(Node propNode, String propName, boolean ordered)
            throws RepositoryException {
        propNode.setProperty(LuceneIndexConstants.PROP_NAME, propName);
        propNode.setProperty(LuceneIndexConstants.PROP_ORDERED, ordered);

        if (ordered) {
            propNode.setProperty(LuceneIndexConstants.PROP_INDEX, true);
        }
    }

    private static Node createLuceneIndexNode(Session s, String indexPath) throws RepositoryException {
        Node lucene = JcrUtils.getOrCreateByPath(indexPath, JcrConstants.NT_UNSTRUCTURED,
                "oak:QueryIndexDefinition", s, false);
        lucene.setProperty("async", "async");
        lucene.setProperty(IndexConstants.TYPE_PROPERTY_NAME, "lucene");
        lucene.setProperty(LuceneIndexConstants.EVALUATE_PATH_RESTRICTION, true);
        lucene.setProperty(LuceneIndexConstants.INDEX_PATH, indexPath);
        lucene.setProperty(LuceneIndexConstants.COMPAT_MODE, IndexFormatVersion.V2.getVersion());
        return lucene;
    }


}
