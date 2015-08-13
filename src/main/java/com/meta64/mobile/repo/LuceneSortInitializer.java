package com.meta64.mobile.repo;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.oak.api.Type.NAME;
import static org.apache.jackrabbit.oak.api.Type.NAMES;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NODE_TYPE;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.REINDEX_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.TYPE_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants.INDEX_RULES;
import static org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants.TYPE_LUCENE;
import static org.apache.jackrabbit.oak.plugins.tree.impl.TreeConstants.OAK_CHILD_ORDER;

import java.util.Set;

import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexFormatVersion;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants;
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneInitializerHelper;
import org.apache.jackrabbit.oak.plugins.tree.impl.TreeConstants;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/*
 * This type of index configuration is very good for "ORDER BY" sorting or also for exact match 
 * queries that lookup an exact value.
 */
class LuceneSortInitializer extends LuceneInitializerHelper {
	private static final Logger log = LoggerFactory.getLogger(LuceneSortInitializer.class);

	private final String name;
	private final String propertyName;

	public LuceneSortInitializer(String name, String propertyName, Set<String> propertyTypes) {
		super(name, propertyTypes);
		this.name = name;
		this.propertyName = propertyName;
	}

	@Override
	public void initialize(NodeBuilder builder) {
		if (builder.hasChildNode(INDEX_DEFINITIONS_NAME) && //
				builder.getChildNode(INDEX_DEFINITIONS_NAME).hasChildNode(name)) {
			log.debug("Index node already exists: " + INDEX_DEFINITIONS_NAME + "/" + name + " so it will not be created.");
		}
		else {
			log.debug("Creating lucene indexing node: " + INDEX_DEFINITIONS_NAME + "/" + name);
			NodeBuilder index = builder.child(INDEX_DEFINITIONS_NAME).child(name);
			index.setProperty(JCR_PRIMARYTYPE, INDEX_DEFINITIONS_NODE_TYPE, NAME)//
					.setProperty(LuceneIndexConstants.PROP_NAME, name)//
					.setProperty(TYPE_PROPERTY_NAME, TYPE_LUCENE)//
					.setProperty(REINDEX_PROPERTY_NAME, true)
					// .setProperty(LuceneIndexConstants.TEST_MODE, true)
					.setProperty(LuceneIndexConstants.EVALUATE_PATH_RESTRICTION, true)//
					.setProperty(LuceneIndexConstants.SUGGEST_UPDATE_FREQUENCY_MINUTES, 30)//
					.setProperty(LuceneIndexConstants.COMPAT_MODE, IndexFormatVersion.V2.getVersion());

			NodeBuilder rulesNode = index.child(LuceneIndexConstants.INDEX_RULES);
			rulesNode.setProperty(TreeConstants.OAK_CHILD_ORDER, ImmutableList.of("nt:unstructured"), Type.NAMES);

			NodeBuilder props = rulesNode.child("nt:unstructured").child(LuceneIndexConstants.PROP_NODE);
			props.setProperty(TreeConstants.OAK_CHILD_ORDER, ImmutableList.of("nt:unstructured"), Type.NAMES);
			enableFulltextIndex(props.child("allProps"));
		}
	}

	private void enableFulltextIndex(NodeBuilder propNode) {
		propNode.setProperty(LuceneIndexConstants.PROP_NODE_SCOPE_INDEX, true);
		propNode.setProperty(LuceneIndexConstants.PROP_PROPERTY_INDEX, true);
		propNode.setProperty(LuceneIndexConstants.PROP_ORDERED, true);
		propNode.setProperty(LuceneIndexConstants.PROP_NAME, propertyName);
		propNode.setProperty(LuceneIndexConstants.PROP_IS_REGEX, false);
	}
}
