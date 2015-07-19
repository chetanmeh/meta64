package com.meta64.mobile.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.AppConstant;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.config.SpringContextUtil;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.ExportRequest;
import com.meta64.mobile.request.ImportRequest;
import com.meta64.mobile.request.InsertBookRequest;
import com.meta64.mobile.response.ExportResponse;
import com.meta64.mobile.response.ImportResponse;
import com.meta64.mobile.response.InsertBookResponse;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.FileTools;
import com.meta64.mobile.util.ImportWarAndPeace;
import com.meta64.mobile.util.JcrUtil;

/**
 * Service for searching the repository
 * 
 * TODO: I really need to separate out the ZIP and XML import/export into dedecated service classes.
 * One for zip and one for xml, instead of this one bigger class.
 * 
 */
@Component
@Scope("session")
public class ImportExportService {
	private static final Logger log = LoggerFactory.getLogger(ImportExportService.class);

	public static final int STANDARD_BUF_SIZE = 1024 * 4;
	private byte[] byteBuf = new byte[STANDARD_BUF_SIZE];

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@Value("${adminDataFolder}")
	private String adminDataFolder;

	/*
	 * This will be made optional in the future, but for my purposes I don't want the zip file name
	 * being the controller of the JCR names. I'd rather just generate JCR names ad GUIDS, so I have
	 * to be able to map them
	 */
	private Map<String, String> zipToJcrNameMap = new HashMap<String, String>();

	public void exportToXml(Session session, ExportRequest req, ExportResponse res) throws Exception {
		if (!sessionContext.isAdmin()) {
			throw new Exception("export is an admin-only feature.");
		}
		
		String nodeId = req.getNodeId();
		Node exportNode = JcrUtil.findNode(session, nodeId);
		log.debug("Export Node: " + exportNode.getPath());

		if (!FileTools.dirExists(adminDataFolder)) {
			throw new Exception("adminDataFolder does not exist");
		}

		String fileName = req.getTargetFileName();
		fileName = fileName.replace(".", "_");
		fileName = fileName.replace(File.separator, "_");
		String fullFileName = adminDataFolder + File.separator + req.getTargetFileName() + ".xml";

		if (FileTools.fileExists(fullFileName)) {
			throw new Exception("File already exists.");
		}

		BufferedOutputStream output = null;
		try {
			output = new BufferedOutputStream(new FileOutputStream(fullFileName));
			session.exportSystemView(exportNode.getPath(), output, false, false);

			/*
			 * Need to investigate whether there is any reason to ever give the user the option to
			 * export as document view instead if system view, and what are the
			 * advantages/disadvantages.
			 */
			// session.exportDocumentView(exportNode.getPath(), output, false, false);
			output.flush();
			res.setSuccess(true);
		}
		finally {
			if (output != null) {
				output.close();
			}
		}
	}

	public void importFromXml(Session session, ImportRequest req, ImportResponse res) throws Exception {
		if (!sessionContext.isAdmin()) {
			throw new Exception("export is an admin-only feature.");
		}
		
		String nodeId = req.getNodeId();
		Node importNode = JcrUtil.findNode(session, nodeId);
		log.debug("Import to Node: " + importNode.getPath());

		if (!FileTools.dirExists(adminDataFolder)) {
			throw new Exception("adminDataFolder does not exist");
		}

		String fileName = req.getSourceFileName();
		fileName = fileName.replace(".", "_");
		fileName = fileName.replace(File.separator, "_");
		String fullFileName = adminDataFolder + File.separator + req.getSourceFileName();

		if (!FileTools.fileExists(fullFileName)) {
			throw new Exception("Import file not found.");
		}

		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(fullFileName));

			/*
			 * This REPLACE_EXISTING option has the effect (in my own words) as meaning that even if
			 * the some of the nodes have moved around since they were first exported they will be
			 * updated 'in their current place' as part of this import.
			 * 
			 * This UUID behavior is so interesting and powerful it really needs to be an option
			 * specified at the user level that determines how this should work.
			 */
			session.getWorkspace().importXML(importNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);

			res.setSuccess(true);

			/*
			 * since importXML is documented to close the inputstream we set it to null here,
			 * because there's nothing left for us to do with it. In the exception case we go ahead
			 * and try to close it.
			 */
			in = null;
		}
		finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public void importFromZip(Session session, ImportRequest req, ImportResponse res) throws Exception {
		if (!sessionContext.isAdmin()) {
			throw new Exception("export is an admin-only feature.");
		}
		
		String nodeId = req.getNodeId();
		Node importNode = JcrUtil.findNode(session, nodeId);
		log.debug("Import to Node: " + importNode.getPath());

		if (!FileTools.dirExists(adminDataFolder)) {
			throw new Exception("adminDataFolder does not exist");
		}

		String fileName = req.getSourceFileName();
		fileName = fileName.replace(".", "_");
		fileName = fileName.replace(File.separator, "_");
		String fullFileName = adminDataFolder + File.separator + req.getSourceFileName();

		if (!FileTools.fileExists(fullFileName)) {
			throw new Exception("Import file not found.");
		}

		ZipInputStream zis = null;

		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fullFileName));
			ZipEntry entry;

			zis = new ZipInputStream(bis);

			while ((entry = zis.getNextEntry()) != null) {
				importZipEntry(zis, entry, importNode, session);
			}
		}
		finally {

			if (zis != null) {
				zis.close();
			}
		}

		res.setSuccess(true);
	}

	public void importZipEntry(ZipInputStream zis, ZipEntry zipEntry, Node importNode, Session session) throws Exception {
		String name = zipEntry.getName();

		if (zipEntry.isDirectory()) {
			/*
			 * We are using an approach where we ignore folder entries in the zip file, because
			 * folders have no actual content
			 */
			// log.debug("ZIP D: " + name);
			// ensureNodeExistsForZipFolder(importNode, name, session);
		}
		else {
			log.debug("ZIP F: " + name);
			importFileFromZip(zis, zipEntry, importNode, session);
		}
	}

	private void importFileFromZip(ZipInputStream zis, ZipEntry zipEntry, Node importNode, Session session) throws Exception {
		String name = zipEntry.getName();

		/*
		 * This is a special hack just for Clay Ferguson's machine because the type of zip files i'm
		 * importing from are the old format export zips from the legacy meta64 zip format, and so I
		 * focus specifically pulling in files that are named content.html or content.txt.
		 * 
		 * This is still alpha testing code but this code runs perfect. Eventually we can remove
		 * this file name hack and let the system be smarter and actually check MIME types using
		 * file name extensions and import binaries properly etc.
		 */
		// if (name.endsWith("/content.html") || name.endsWith("/content.txt")) {

		StringBuilder buffer = new StringBuilder();
		synchronized (byteBuf) {
			/*
			 * todo: got this code snippet online. someday look to see if there is a more efficient
			 * way
			 */
			for (int n; (n = zis.read(byteBuf)) != -1;) {
				if (n > 0) {
					buffer.append(new String(byteBuf, 0, n));
				}
			}
		}

		Node newNode = ensureNodeExistsForZipFolder(importNode, name, session);
		String val = buffer.toString();

		/*
		 * I had a special need to rip HTML tags out of the data I was importing, so I'm commenting
		 * out this hack but leaving it in place so show where and how you can do some processing of
		 * the data as it's imported. Ideally of course this capability would be some kind of
		 * "extension point" (Eclipse plugin terminology) in a production JCR Browder for
		 * filteringinput data, or else this entire class could be pluggable via inteface and IoC.
		 */
		// val = val.replace("<p>", "\n\n");
		// val = val.replace("<br>", "\n");
		// val = ripTags(val);
		newNode.setProperty("jcr:content", val.trim());
		// }
	}

	public static String ripTags(String text) {
		StringTokenizer t = new StringTokenizer(text, "<>", true);
		String token;
		boolean inTag = false;
		StringBuilder ret = new StringBuilder();

		while (t.hasMoreTokens()) {
			token = t.nextToken();
			if (token.equals("<")) {
				inTag = true;
			}
			else if (token.equals(">")) {
				inTag = false;
			}
			else {
				if (!inTag) {
					ret.append(token);
				}
			}
		}

		return ret.toString();
	}

	/*
	 * Builds a node assuming root is a starting path, and 'path' is a ZipFile folder name.
	 * 
	 * Revision: the path is not a file name path.
	 */
	private Node ensureNodeExistsForZipFolder(Node root, String path, Session session) throws Exception {
		String[] tokens = path.split("/");
		String curPath = root.getPath();
		Node curNode = root;
		int tokenIdx = 0;
		int maxTokenIdx = tokens.length - 1;
		for (String token : tokens) {

			/*
			 * This actually is assuming that the path is a file name, and we ignore the file name
			 * part
			 */
			if (tokenIdx >= maxTokenIdx) {
				break;
			}
			String guid = zipToJcrNameMap.get(token);
			if (guid == null) {
				guid = JcrUtil.getGUID();
				zipToJcrNameMap.put(token, guid);
			}

			String jcrName = AppConstant.NAMESPACE + ":" + guid;
			curPath += "/" + jcrName;
			try {
				// log.debug("Checking for path: " + curPath);
				curNode = session.getNode(curPath);
			}
			catch (Exception e) {
				// log.debug("path not found, creating");
				// not an error condition. Simply indicates note at curPath does not exist, so we
				// create it and continue as part of the algorithm. We will actually build as many
				// parents as we need to here.
				curNode = createChildNode(curNode, jcrName, token, session);

				// log.debug("new node now has path: " + curNode.getPath());
			}
			tokenIdx++;
		}
		return curNode;
	}

	private Node createChildNode(Node node, String jcrName, String content, Session session) throws Exception {
		try {
			Node newNode = node.addNode(jcrName, JcrConstants.NT_UNSTRUCTURED);
			/*
			 * Note we don't set content here, but instead set it in the method that calls this one.
			 */
			// newNode.setProperty("jcr:content", content);
			JcrUtil.timestampNewNode(session, newNode);
			return newNode;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void insertBook(Session session, InsertBookRequest req, InsertBookResponse res) throws Exception {
		
		if (!sessionContext.isAdmin()) {
			throw new Exception("insertBook is an admin-only feature.");
		}

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		/* for now we don't check book name. Only one book exists: War and Peace */
		// String name = req.getBookName();

		ImportWarAndPeace iwap = SpringContextUtil.getApplicationContext().getBean(ImportWarAndPeace.class);
		iwap.importBook(session, "classpath:war-and-peace.txt", node);

		session.save();
		res.setSuccess(true);
	}

}
