package com.meta64.mobile.service;

import java.awt.image.BufferedImage;
import java.net.URLConnection;

import javax.imageio.ImageIO;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.meta64.mobile.config.AppConstant;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.image.ImageUtil;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.DeleteAttachmentRequest;
import com.meta64.mobile.response.DeleteAttachmentResponse;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.ThreadLocals;

/**
 * Service for editing content of nodes.
 */
@Component
@Scope("session")
public class AttachmentService {
	private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	public ResponseEntity<?> upload(Session session, String nodeId, MultipartFile uploadFile) throws Exception {		
		try {
			String fileName = uploadFile.getOriginalFilename();

			log.debug("Uploading onto nodeId: " + nodeId + " file: " + fileName);
			Node node = JcrUtil.findNode(session, nodeId);
			String mimeType = URLConnection.guessContentTypeFromName(fileName);

			String name = AppConstant.JCR_PROP_BIN;

			Node binaryNode = null;
			long version = 0;
			try {
				binaryNode = session.getNode(node.getPath() + "/" + name);

				/*
				 * Based on my reading of the JCR docs, I don't need to remove old properties,
				 * because new property will overwrite. TODO: testing pending
				 */
				Property versionProperty = binaryNode.getProperty(AppConstant.JCR_PROP_BIN_VER);
				if (versionProperty != null) {
					version = versionProperty.getValue().getLong();
				}
			}
			catch (Exception e) {
				// not an error. Indicates this node didn't already have an attachment node.
			}

			/* if no existing node existed we need to create */
			if (binaryNode == null) {
				binaryNode = node.addNode(name, JcrConstants.NT_UNSTRUCTURED);
				JcrUtil.timestampNewNode(session, binaryNode);
			}

			Binary binary = session.getValueFactory().createBinary(uploadFile.getInputStream());

			/*
			 * The above 'createBinary' call will have already read the entire stream so we can now
			 * assume all data is present and width/height of image will ba available.
			 */
			if (ImageUtil.isImageMime(mimeType)) {
				BufferedImage image = ImageIO.read(binary.getStream());
				int width = image.getWidth();
				int height = image.getHeight();
				binaryNode.setProperty(AppConstant.JCR_PROP_IMG_WIDTH, String.valueOf(width));
				binaryNode.setProperty(AppConstant.JCR_PROP_IMG_HEIGHT, String.valueOf(height));
			}

			binaryNode.setProperty(AppConstant.JCR_PROP_BIN_DATA, binary);
			binaryNode.setProperty(AppConstant.JCR_PROP_BIN_MIME, mimeType);
			binaryNode.setProperty(AppConstant.JCR_PROP_BIN_VER, ++version);

			/*
			 * DO NOT DELETE (this code can be used to test uploading) String directory =
			 * "c:/temp-upload"; String filepath = Paths.get(directory, fileName).toString();
			 * BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new
			 * File(filepath))); stream.write(uploadfile.getBytes()); stream.close();
			 */

			session.save();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	public void deleteAttachment(Session session, DeleteAttachmentRequest req, DeleteAttachmentResponse res) throws Exception {
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		Node nodeToRemove = session.getNode(node.getPath() + "/" + AppConstant.JCR_PROP_BIN);
		nodeToRemove.remove();
		session.save();
		res.setSuccess(true);
	}

	/*
	 * Returns data for an attachment (Could be an image request, or any type of request for binary
	 * data from a node)
	 */
	public ResponseEntity<InputStreamResource> getBinary(Session session, String nodeId) throws Exception {
		try {
			// System.out.println("Retrieving binary nodeId: " + nodeId);
			Node node = JcrUtil.findNode(session, nodeId);

			Property mimeTypeProp = node.getProperty(AppConstant.JCR_PROP_BIN_MIME);
			if (mimeTypeProp == null) {
				throw new Exception("unable to find mimeType property");
			}
			// log.debug("Retrieving mime: " + mimeTypeProp.getValue().getString());

			Property dataProp = node.getProperty(AppConstant.JCR_PROP_BIN_DATA);
			if (dataProp == null) {
				throw new Exception("unable to find data property");
			}

			Binary binary = dataProp.getBinary();
			// log.debug("Retrieving binary bytes: " + binary.getSize());

			return ResponseEntity.ok().contentLength(binary.getSize())//
					.contentType(MediaType.parseMediaType(mimeTypeProp.getValue().getString()))//
					.body(new InputStreamResource(binary.getStream()));
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}
