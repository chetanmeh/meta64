package com.meta64.mobile.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.meta64.mobile.SpringContextUtil;

@Component
@Scope("prototype")
public class ImportWarAndPeace {
	private static final Logger log = LoggerFactory.getLogger(ImportWarAndPeace.class);

	private String resourceName;
	private boolean debug = true;
	private Node root;
	private Node curBook;
	private Node curChapter;

	StringBuilder paragraph = new StringBuilder();

	private int globalBook = 0;
	private int globalChapter = 0;
	private int globalVerse = 0;
	private boolean halt;

	public void run() throws Exception {
		internalRun();
	}

	private void internalRun() throws Exception {

		Resource resource = SpringContextUtil.getApplicationContext().getResource(resourceName); 
		
//		Resource resource = 
//		          appContext.getResource("url:http://www.yourdomain.com/testing.txt");
		
		InputStream is = resource.getInputStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(is));

		try {
			String line;

			while (!halt && (line = in.readLine()) != null) {
				line = line.trim();

				/*
				 * if we see a blank line we add the current paragraph text as a node and continue
				 */
				if (line.length() == 0) {
					if (paragraph.length() > 0) {
						addParagraph();
					}
					continue;
				}

				if (debug) {
					log.debug("INPUT: " + line);
				}

				/*
				 * if we processed the chapter, the last paragraph is also added before starting the
				 * new chapter
				 */
				if (processChapter(line)) {
					continue;
				}

				/*
				 * if we processed the book, the last paragraph is also added before starting the
				 * new book
				 */
				if (processBook(line)) {
					continue;
				}

				/* keep appending each line to the current paragraph */
				if (paragraph.length() > 0) {
					paragraph.append(" ");
				}
				paragraph.append(line);
			}
		}
		finally {
			if (in != null) {
				in.close();
			}
		}
		log.debug("book import successful.");
	}

	private boolean processChapter(String line) throws Exception {

		if (line.startsWith("CHAPTER ")) {
			globalChapter++;
			log.debug("Processing Chapter: " + line);
			if (curBook == null) throw new Exception("book is null.");

			addParagraph();

			// InsertResultInfo info = Factory.createNodeService().attachNode(0, "C" +
			// String.valueOf(globalChapter) + ". " + line,
			// curBook.getRecordId(), AllTypes.TYPE_BOOK_CHAPTER, false, InsertType.ATTACH_BOTTOM,
			// true, null, null, null, null, null,
			// false, null, null, null, true, false, true, false);

			// curChapter = info.getModel();
			// itemInserted(info);

			return true;
		}

		return false;
	}

	private boolean addParagraph() throws Exception {
		String line = paragraph.toString();

		/*
		 * remove any places where my algorithm stuffed an extra space that just happened to be at a
		 * sentence end
		 */
		line = line.replace(".   ", ".  ");

		if (line.length() == 0) return false;

		if (curChapter == null || curBook == null) return false;

		globalVerse++;

		// line = XString.injectForQuotations(line);
		// InsertResultInfo info = Factory.createNodeService().attachNode(0, "VS" + globalVerse +
		// ". " + line, curChapter.getRecordId(),
		// AllTypes.TYPE_BOOK_CHAPTER_VERSE, false, InsertType.ATTACH_BOTTOM, true, null, null,
		// null, null, null, false, null, null,
		// null, true, false, true, false);

		// itemInserted(info);

		paragraph.setLength(0);
		return true;
	}

	private boolean anyEpilogue(String line) {
		return line.startsWith("FIRST EPILOGUE") || //
				line.startsWith("SECOND EPILOGUE") || //
				line.startsWith("THIRD EPILOGUE") || //
				line.startsWith("FOURTH EPILOGUE");
	}

	private boolean processBook(String line) throws Exception {
		if (line.startsWith("BOOK ") || anyEpilogue(line)) {
			globalBook++;
			addParagraph();
			// InsertResultInfo info = Factory.createNodeService().attachNode(0, "B" +
			// String.valueOf(globalBook) + ". " + line,
			// root.getRecordId(), AllTypes.TYPE_BOOK, false, InsertType.ATTACH_BOTTOM, true, null,
			// null, null, null, null, false,
			// null, null, null, true, false, true, false);
			// curBook = info.getModel();

			// itemInserted(info);

			return true;
		}
		return false;
	}

	// private void itemInserted(InsertResultInfo info) throws Exception {
	// int id = info.getModel().getRecordId();
	// if (idList != null) {
	// idList.add(id);
	// }
	// }
}
