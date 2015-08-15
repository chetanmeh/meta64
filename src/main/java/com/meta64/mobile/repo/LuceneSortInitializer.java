package com.meta64.mobile.repo;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.oak.api.Type.NAME;

import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexFormatVersion;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This type of index configuration is very good for "ORDER BY" sorting or also for exact match 
 * queries that lookup an exact value.
 */
class LuceneSortInitializer implements RepositoryInitializer {

	private static final Logger log = LoggerFactory.getLogger(LuceneSortInitializer.class);

	private final String name;
	private final String propertyName;

	public LuceneSortInitializer(String name, String propertyName) {
		this.name = name;
		this.propertyName = propertyName;
	}

	@Override
	public void initialize(NodeBuilder builder) {
		if (builder.hasChildNode(IndexConstants.INDEX_DEFINITIONS_NAME) && //
				builder.getChildNode(IndexConstants.INDEX_DEFINITIONS_NAME).hasChildNode(name)) {
			log.debug("Index node already exists: " + IndexConstants.INDEX_DEFINITIONS_NAME + "/" + name + " so it will not be created.");
			return;
		}

		log.debug("Creating lucene indexing node: " + IndexConstants.INDEX_DEFINITIONS_NAME + "/" + name);
		NodeBuilder index = builder.child(IndexConstants.INDEX_DEFINITIONS_NAME).child(name);

		index.setProperty(JCR_PRIMARYTYPE, IndexConstants.INDEX_DEFINITIONS_NODE_TYPE, NAME)//
				.setProperty(LuceneIndexConstants.PROP_NAME, name)//
				.setProperty(IndexConstants.TYPE_PROPERTY_NAME, LuceneIndexConstants.TYPE_LUCENE)//
				.setProperty(IndexConstants.REINDEX_PROPERTY_NAME, true)//

				/*
				 * Using ASYNC appears to completely disable the index. Not sure what else I need to
				 * do here or how badly this will impact performance, to have 'synchronous'.
				 */
				// .setProperty(IndexConstants.ASYNC_PROPERTY_NAME, "async") //

				// .setProperty(LuceneIndexConstants.TEST_MODE, true)
				.setProperty(LuceneIndexConstants.EVALUATE_PATH_RESTRICTION, true)//
				.setProperty(LuceneIndexConstants.SUGGEST_UPDATE_FREQUENCY_MINUTES, 30)//
				.setProperty(LuceneIndexConstants.COMPAT_MODE, IndexFormatVersion.V2.getVersion());

		NodeBuilder rulesNode = index.child(LuceneIndexConstants.INDEX_RULES);
		// rulesNode.setProperty(TreeConstants.OAK_CHILD_ORDER, ImmutableList.of("nt:unstructured"),
		// Type.NAMES);

		NodeBuilder props = rulesNode.child("nt:base").child(LuceneIndexConstants.PROP_NODE);
		// props.setProperty(TreeConstants.OAK_CHILD_ORDER, ImmutableList.of("nt:unstructured"),
		// Type.NAMES);
		enableFulltextIndex(props.child("allProps"));
	}

	private void enableFulltextIndex(NodeBuilder propNode) {
		propNode.setProperty(LuceneIndexConstants.PROP_NODE_SCOPE_INDEX, true);
		propNode.setProperty(LuceneIndexConstants.PROP_PROPERTY_INDEX, true);
		propNode.setProperty(LuceneIndexConstants.PROP_ORDERED, true);
		propNode.setProperty(LuceneIndexConstants.PROP_NAME, propertyName);
		propNode.setProperty(LuceneIndexConstants.PROP_IS_REGEX, false);
	}
}
