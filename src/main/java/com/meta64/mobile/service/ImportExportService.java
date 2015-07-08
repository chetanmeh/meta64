package com.meta64.mobile.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.ExportRequest;
import com.meta64.mobile.request.ImportRequest;
import com.meta64.mobile.response.ExportResponse;
import com.meta64.mobile.response.ImportResponse;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.FileTools;
import com.meta64.mobile.util.JcrUtil;

/**
 * Service for searching the repository
 */
@Component
@Scope("session")
public class ImportExportService {
	private static final Logger log = LoggerFactory.getLogger(ImportExportService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@Value("${adminDataFolder}")
	private String adminDataFolder;

	public void exportToXml(Session session, ExportRequest req, ExportResponse res) throws Exception {
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
		String nodeId = req.getNodeId();
		Node importNode = JcrUtil.findNode(session, nodeId);
		log.debug("Import to Node: " + importNode.getPath());

		if (!FileTools.dirExists(adminDataFolder)) {
			throw new Exception("adminDataFolder does not exist");
		}

		String fileName = req.getSourceFileName();
		fileName = fileName.replace(".", "_");
		fileName = fileName.replace(File.separator, "_");
		String fullFileName = adminDataFolder + File.separator + req.getSourceFileName() + ".xml";

		if (!FileTools.fileExists(fullFileName)) {
			throw new Exception("Import file not found.");
		}

		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(fullFileName));
			
			/* This REPLACE_EXISTING option has the effect (in my own words) as meaning that even if the some of the nodes have 
			 * moved around since they were first exported they will be updated 'in their current place' as part of this import.
			 * 
			 * This UUID behavior is so interesting and powerful it really needs to be an option specified at the user level
			 * that determines how this should work.
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
}
