package com.meta64.mobile.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.image.ImageUtil;
import com.meta64.mobile.repo.OakRepositoryBean;
import com.meta64.mobile.request.DeleteAttachmentRequest;
import com.meta64.mobile.request.UploadFromUrlRequest;
import com.meta64.mobile.response.DeleteAttachmentResponse;
import com.meta64.mobile.response.UploadFromUrlResponse;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.LimitedInputStream;

/**
 * Service for editing node attachments
 */
@Component
@Scope("singleton")
public class AttachmentService {
	private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private SessionContext sessionContext;

	/* Upload from User's computer. Standard HTML form-based uploading */
	public ResponseEntity<?> upload(Session session, String nodeId, MultipartFile uploadFile) throws Exception {
		try {
			String fileName = uploadFile.getOriginalFilename();
			log.debug("Uploading onto nodeId: " + nodeId + " file: " + fileName);
			attachBinaryFromStream(session, nodeId, fileName, uploadFile.getInputStream(), null, -1, -1);
			session.save();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public void attachBinaryFromStream(Session session, String nodeId, String fileName, InputStream is, String mimeType, int width, int height) throws Exception {
		Node node = JcrUtil.findNode(session, nodeId);
		JcrUtil.checkNodeCreatedBy(node, session.getUserID());

		/* mimeType can be passed as null if it's not yet determined */
		if (mimeType == null) {
			mimeType = URLConnection.guessContentTypeFromName(fileName);
		}

		/*
		 * Hack/Fix for ms word. Not sure why the URLConnection fails for this, but it's new. I need
		 * to grab my old mime type map from legacy meta64 and put in this project. Clearly the
		 * guessContentTypeFromName implementation provided by URLConnection has a screw loose.
		 */
		if (mimeType == null) {
			if (fileName.toLowerCase().endsWith(".doc")) {
				mimeType = "application/msword";
			}
		}

		long version = System.currentTimeMillis();
		Property binVerProp = JcrUtil.getProperty(node, JcrProp.BIN_VER);
		if (binVerProp != null) {
			version = binVerProp.getValue().getLong();
		}

		Binary binary = session.getValueFactory().createBinary(is);

		/*
		 * The above 'createBinary' call will have already read the entire stream so we can now
		 * assume all data is present and width/height of image will ba available.
		 */
		if (ImageUtil.isImageMime(mimeType)) {
			if (width == -1 || height == -1) {
				BufferedImage image = ImageIO.read(binary.getStream());
				width = image.getWidth();
				height = image.getHeight();
			}

			node.setProperty(JcrProp.IMG_WIDTH, String.valueOf(width));
			node.setProperty(JcrProp.IMG_HEIGHT, String.valueOf(height));
		}

		node.setProperty(JcrProp.BIN_DATA, binary);
		node.setProperty(JcrProp.BIN_MIME, mimeType);
		node.setProperty(JcrProp.BIN_VER, version + 1);

		/*
		 * DO NOT DELETE (this code can be used to test uploading) String directory =
		 * "c:/temp-upload"; String filepath = Paths.get(directory, fileName).toString();
		 * BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new
		 * File(filepath))); stream.write(uploadfile.getBytes()); stream.close();
		 */
	}

	public void deleteAttachment(Session session, DeleteAttachmentRequest req, DeleteAttachmentResponse res) throws Exception {
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		JcrUtil.checkNodeCreatedBy(node, session.getUserID());
		deleteAllBinaryProperties(node);
		session.save();
		res.setSuccess(true);
	}

	public void deleteAllBinaryProperties(Node node) {
		JcrUtil.safeDeleteProperty(node, JcrProp.IMG_WIDTH);
		JcrUtil.safeDeleteProperty(node, JcrProp.IMG_HEIGHT);
		JcrUtil.safeDeleteProperty(node, JcrProp.BIN_DATA);
		JcrUtil.safeDeleteProperty(node, JcrProp.BIN_MIME);
		JcrUtil.safeDeleteProperty(node, JcrProp.BIN_VER);
	}

	/*
	 * Returns data for an attachment (Could be an image request, or any type of request for binary
	 * data from a node)
	 */
	public ResponseEntity<InputStreamResource> getBinary(Session session, String nodeId) throws Exception {
		try {
			// System.out.println("Retrieving binary nodeId: " + nodeId);
			Node node = JcrUtil.findNode(session, nodeId);

			Property mimeTypeProp = node.getProperty(JcrProp.BIN_MIME);
			if (mimeTypeProp == null) {
				throw new Exception("unable to find mimeType property");
			}
			// log.debug("Retrieving mime: " + mimeTypeProp.getValue().getString());

			Property dataProp = node.getProperty(JcrProp.BIN_DATA);
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

	public void uploadFromUrl(Session session, UploadFromUrlRequest req, UploadFromUrlResponse res) throws Exception {
		String nodeId = req.getNodeId();
		String sourceUrl = req.getSourceUrl();
		String FAKE_USER_AGENT = "Mozilla/5.0";
		int maxFileSize = 2 * 1024 * 1024;

		URL url = new URL(sourceUrl);
		InputStream uis = null;
		HttpURLConnection httpcon = null;

		try {
			String mimeType = URLConnection.guessContentTypeFromName(sourceUrl);

			/*
			 * if this is an image extension, handle it in a special way, mainly to extract the
			 * width, height from it
			 */
			if (ImageUtil.isImageMime(mimeType)) {

				/*
				 * DO NOT DELETE
				 * 
				 * Basic version without masquerading as a web browser can cause a 403 error because
				 * some sites don't want just any old stream reading from them. Leave this note here
				 * as a warning and explanation
				 */

				httpcon = (HttpURLConnection) url.openConnection();
				httpcon.addRequestProperty("User-Agent", FAKE_USER_AGENT);
				httpcon.connect();
				InputStream is = httpcon.getInputStream();
				uis = new LimitedInputStream(is, maxFileSize);
				attachBinaryFromStream(session, nodeId, sourceUrl, uis, mimeType, -1, -1);
			}
			/*
			 * if not an image extension, we can just stream directly into the database, but we want
			 * to try to get the mime type first, from calling detectImage so that if we do detect
			 * its an image we can handle it as one.
			 */
			else {
				if (!detectAndSaveImage(session, nodeId, sourceUrl, url)) {
					httpcon = (HttpURLConnection) url.openConnection();
					httpcon.addRequestProperty("User-Agent", FAKE_USER_AGENT);
					httpcon.connect();
					InputStream is = httpcon.getInputStream();
					uis = new LimitedInputStream(is, maxFileSize);
					attachBinaryFromStream(session, nodeId, sourceUrl, is, "", -1, -1);
				}
			}
		}
		/* finally block just for extra safety */
		finally {
			if (uis != null) {
				uis.close();
				uis = null;
			}

			/*
			 * I may not need this after the stream was close, but I'm calling it just in case
			 */
			if (httpcon != null) {
				httpcon.disconnect();
				httpcon = null;
			}
		}
		session.save();
		res.setSuccess(true);
	}

	// FYI: this never worked:
	// String mimeType = URLConnection.guessContentTypeFromStream(uis);
	// log.debug("guessed mime:" + mimeType);
	//
	// but this below works...
	//
	/* returns true if it was detected AND saved as an image */
	private boolean detectAndSaveImage(Session session, String nodeId, String fileName, URL url) throws Exception {
		ImageInputStream is = null;
		InputStream is2 = null;
		ImageReader reader = null;

		try {
			is = ImageIO.createImageInputStream(url.openStream());
			Iterator<ImageReader> readers = ImageIO.getImageReaders(is);

			if (readers.hasNext()) {
				reader = readers.next();
				String formatName = reader.getFormatName();

				if (formatName != null) {
					formatName = formatName.toLowerCase();
					// log.debug("determined format name of image url: " + formatName);
					reader.setInput(is, true, false);
					BufferedImage bufImg = reader.read(0);
					String mimeType = "image/" + formatName;

					ByteArrayOutputStream os = new ByteArrayOutputStream();
					ImageIO.write(bufImg, formatName, os);
					is2 = new ByteArrayInputStream(os.toByteArray());

					attachBinaryFromStream(session, nodeId, fileName, is2, mimeType, bufImg.getWidth(null), bufImg.getHeight(null));
					return true;
				}
			}
		}
		finally {
			if (is != null) {
				is.close();
			}

			if (is2 != null) {
				is2.close();
			}

			if (reader != null) {
				reader.dispose();
			}
		}
		return false;
	}
}
