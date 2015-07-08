package com.meta64.mobile;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.Principal;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.meta64.mobile.annotate.OakSession;
import com.meta64.mobile.config.AppConstant;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.config.SpringContextUtil;
import com.meta64.mobile.image.CaptchaMaker;
import com.meta64.mobile.image.ImageUtil;
import com.meta64.mobile.model.AccessControlEntryInfo;
import com.meta64.mobile.model.PropertyInfo;
import com.meta64.mobile.request.AddPrivilegeRequest;
import com.meta64.mobile.request.AnonPageLoadRequest;
import com.meta64.mobile.request.ChangePasswordRequest;
import com.meta64.mobile.request.CreateSubNodeRequest;
import com.meta64.mobile.request.DeleteAttachmentRequest;
import com.meta64.mobile.request.DeleteNodesRequest;
import com.meta64.mobile.request.DeletePropertyRequest;
import com.meta64.mobile.request.ExportRequest;
import com.meta64.mobile.request.GetNodePrivilegesRequest;
import com.meta64.mobile.request.ImportRequest;
import com.meta64.mobile.request.InsertBookRequest;
import com.meta64.mobile.request.InsertNodeRequest;
import com.meta64.mobile.request.LoginRequest;
import com.meta64.mobile.request.MakeNodeReferencableRequest;
import com.meta64.mobile.request.MoveNodesRequest;
import com.meta64.mobile.request.NodeSearchRequest;
import com.meta64.mobile.request.RemovePrivilegeRequest;
import com.meta64.mobile.request.RenderNodeRequest;
import com.meta64.mobile.request.SaveNodeRequest;
import com.meta64.mobile.request.SavePropertyRequest;
import com.meta64.mobile.request.SetNodePositionRequest;
import com.meta64.mobile.request.SignupRequest;
import com.meta64.mobile.response.AddPrivilegeResponse;
import com.meta64.mobile.response.AnonPageLoadResponse;
import com.meta64.mobile.response.ChangePasswordResponse;
import com.meta64.mobile.response.CreateSubNodeResponse;
import com.meta64.mobile.response.DeleteAttachmentResponse;
import com.meta64.mobile.response.DeleteNodesResponse;
import com.meta64.mobile.response.DeletePropertyResponse;
import com.meta64.mobile.response.ExportResponse;
import com.meta64.mobile.response.GetNodePrivilegesResponse;
import com.meta64.mobile.response.ImportResponse;
import com.meta64.mobile.response.InsertBookResponse;
import com.meta64.mobile.response.InsertNodeResponse;
import com.meta64.mobile.response.LoginResponse;
import com.meta64.mobile.response.MakeNodeReferencableResponse;
import com.meta64.mobile.response.MoveNodesResponse;
import com.meta64.mobile.response.NodeSearchResponse;
import com.meta64.mobile.response.RemovePrivilegeResponse;
import com.meta64.mobile.response.RenderNodeResponse;
import com.meta64.mobile.response.SaveNodeResponse;
import com.meta64.mobile.response.SavePropertyResponse;
import com.meta64.mobile.response.SetNodePositionResponse;
import com.meta64.mobile.response.SignupResponse;
import com.meta64.mobile.service.ImportExportService;
import com.meta64.mobile.service.NodeRenderService;
import com.meta64.mobile.service.NodeSearchService;
import com.meta64.mobile.user.AccessControlUtil;
import com.meta64.mobile.user.UserManagerService;
import com.meta64.mobile.user.UserManagerUtil;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.ImportWarAndPeace;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.ThreadLocals;
import com.meta64.mobile.util.XString;

/**
 * Primary Spring MVC controller, that returns the main page, process REST calls from the client
 * javascript, and also performs the uploading/download, and serving of images. These major areas
 * probably will eventually be broken out onto separate controllers, and much of the business rules
 * in here will be moved out into service or utility classes.
 * 
 * Note, it's critical to understand the OakSession AOP code or else this class will be confusing
 * regarding how the OAK transations are managed and how logging in is done.
 */
@Controller
@Scope("session")
public class AppController {
	private static final Logger log = LoggerFactory.getLogger(AppController.class);

	private static final String REST_PATH = "/mobile/rest";

	/*
	 * Relational Database Connection Info
	 */
	@Value("${anonUserLandingPageNode}")
	private String anonUserLandingPageNode;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private UserManagerService userManagerService;

	@Autowired
	private NodeRenderService nodeRenderService;

	@Autowired
	private NodeSearchService nodeSearchService;

	@Autowired
	private ImportExportService importExportService;

	private static void logRequest(String url, Object req) throws Exception {
		log.debug("REQ=" + url + " " + (req == null ? "none" : Convert.JsonStringify(req)));
	}

	/*
	 * This is the actual app page loading request, which is a thymeleaf-model-based call.
	 * 
	 * ID is optional url parameter that user can specify to access a specific node in the
	 * repository by uuid.
	 */
	@RequestMapping("/mobile")
	public String mobile(@RequestParam(value = "id", required = false) String id, Model model) throws Exception {
		logRequest("mobile", null);

		sessionContext.setUrlId(id);
		// model.addAttribute("id", name);
		return "index";
	}

	@RequestMapping(value = REST_PATH + "/captcha", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
	public @ResponseBody byte[] captcha() throws Exception {
		logRequest("captcha", null);
		String captcha = CaptchaMaker.createCaptchaString();
		sessionContext.setCaptcha(captcha);
		return CaptchaMaker.makeCaptcha(captcha);
	}

	@RequestMapping(value = REST_PATH + "/signup", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SignupResponse signup(@RequestBody SignupRequest req) throws Exception {
		logRequest("signup", req);

		SignupResponse res = new SignupResponse();
		ThreadLocals.setResponse(res);

		String userName = req.getUserName();
		if (userName.equalsIgnoreCase("admin")) {
			throw new Exception("Sorry, you can't be the new admin.");
		}

		if (userName.equalsIgnoreCase("everyone")) {
			throw new Exception("Sorry, you can't be everyone.");
		}

		String password = req.getPassword();
		String email = req.getEmail();
		String captcha = req.getCaptcha();

		/* lever let null be used */
		if (captcha == null) {
			captcha = "";
		}

		if (userManagerService.signup(userName, password, email, captcha)) {
			res.setMessage("success: " + String.valueOf(++sessionContext.counter));
			res.setSuccess(true);
		}

		return res;
	}

	/*
	 * Login mechanism is a bit tricky because the OakSession ASPECT (AOP) actually detects the
	 * LoginRequest and performs authentication BEFORE this 'login' method evern gets called, so by
	 * the time we are in this method we can safely assume the userName and password resulted in a
	 * successful login. If login fails the getJcrSession() call below will return null also.
	 * 
	 * @see OakSessionAspect.loginFromJoinPoint()
	 */
	@RequestMapping(value = REST_PATH + "/login", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody LoginResponse login(@RequestBody LoginRequest req) throws Exception {
		logRequest("login", req);

		String userName = req.getUserName();
		String password = req.getPassword();

		sessionContext.setUserName(userName);
		sessionContext.setPassword(password);

		LoginResponse res = new LoginResponse();
		ThreadLocals.setResponse(res);
		res.setMessage("success: " + String.valueOf(++sessionContext.counter));

		Session session = ThreadLocals.getJcrSession();

		res.setRootNode(UserManagerUtil.getRootNodeRefInfoForUser(session, userName));
		res.setUserName(userName);
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/renderNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody RenderNodeResponse renderNode(@RequestBody RenderNodeRequest req) throws Exception {
		logRequest("renderNode", req);

		RenderNodeResponse res = new RenderNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		nodeRenderService.renderNode(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/getNodePrivileges", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody GetNodePrivilegesResponse getNodePrivileges(@RequestBody GetNodePrivilegesRequest req) throws Exception {
		logRequest("getNodePrivileges", req);

		GetNodePrivilegesResponse res = new GetNodePrivilegesResponse();
		ThreadLocals.setResponse(res);

		Session session = ThreadLocals.getJcrSession();
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		AccessControlEntry[] aclEntries = AccessControlUtil.getAccessControlEntries(session, node);
		List<AccessControlEntryInfo> aclEntriesInfo = Convert.convertToAclListInfo(aclEntries);
		res.setAclEntries(aclEntriesInfo);
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/addPrivilege", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody AddPrivilegeResponse addPrivilege(@RequestBody AddPrivilegeRequest req) throws Exception {
		logRequest("addPrivilege", req);

		AddPrivilegeResponse res = new AddPrivilegeResponse();
		ThreadLocals.setResponse(res);

		Session session = ThreadLocals.getJcrSession();
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		String principal = req.getPrincipal();
		String privilege = req.getPrivilege();
		Principal principalObj = null;

		if (principal.equalsIgnoreCase("everyone")) {
			principalObj = EveryonePrincipal.getInstance();
		}
		else {
			throw new Exception("Code not yet able to handle non-everyone principle privilege adds.");
		}

		boolean success = false;
		try {
			success = AccessControlUtil.grantPrivileges(session, node, principalObj, privilege);
		}
		catch (Exception e) {
			// leave success==false and continue.
		}

		if (success) {
			session.save();
		}
		else {
			res.setMessage("Unable to alter privileges on node.");
		}
		res.setSuccess(success);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/removePrivilege", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody RemovePrivilegeResponse removePrivilege(@RequestBody RemovePrivilegeRequest req) throws Exception {
		logRequest("removePrivilege", req);

		RemovePrivilegeResponse res = new RemovePrivilegeResponse();
		ThreadLocals.setResponse(res);

		Session session = ThreadLocals.getJcrSession();
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		String principal = req.getPrincipal();
		String privilege = req.getPrivilege();

		boolean success = AccessControlUtil.removeAclEntry(session, node, principal, privilege);
		session.save();
		res.setSuccess(success);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/exportToXml", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ExportResponse exportToXml(@RequestBody ExportRequest req) throws Exception {
		logRequest("exportToXml", req);

		ExportResponse res = new ExportResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		if (!sessionContext.isAdmin()) {
			throw new Exception("export is an admin-only feature.");
		}

		importExportService.exportToXml(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/importFromXml", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ImportResponse importFromXml(@RequestBody ImportRequest req) throws Exception {
		logRequest("importFromXml", req);

		ImportResponse res = new ImportResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		if (!sessionContext.isAdmin()) {
			throw new Exception("export is an admin-only feature.");
		}

		importExportService.importFromXml(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/setNodePosition", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SetNodePositionResponse setNodePosition(@RequestBody SetNodePositionRequest req) throws Exception {
		logRequest("setNodePosition", req);

		SetNodePositionResponse res = new SetNodePositionResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String parentNodeId = req.getParentNodeId();
		Node parentNode = JcrUtil.findNode(session, parentNodeId);
		// System.out.println("Moving using parent: " + parentNodeId);
		// System.out.println("orderBefore: " + req.getNodeId() + " sibling=" + req.getSiblingId());
		parentNode.orderBefore(req.getNodeId(), req.getSiblingId());
		session.save();
		res.setSuccess(true);
		return res;
	}

	/*
	 * http://stackoverflow.com/questions/5567905/jackrabbit-jcr-organisation-of-text-content-data
	 */
	@RequestMapping(value = REST_PATH + "/createSubNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody CreateSubNodeResponse createSubNode(@RequestBody CreateSubNodeRequest req) throws Exception {
		logRequest("createSubNode", req);

		CreateSubNodeResponse res = new CreateSubNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		// session.checkPermission(absPath, actions);

		String name = XString.isEmpty(req.getNewNodeName()) ? JcrUtil.getGUID() : req.getNewNodeName();

		/* NT_UNSTRUCTURED IS ORDERABLE */
		Node newNode = node.addNode(AppConstant.NAMESPACE + ":" + name, JcrConstants.NT_UNSTRUCTURED);
		newNode.setProperty("jcr:content", "");
		JcrUtil.timestampNewNode(session, newNode);
		session.save();
		// res.setNewChildNodeId(newNode.getIdentifier());

		res.setNewNode(Convert.convertToNodeInfo(session, newNode));
		res.setSuccess(true);

		return res;
	}

	/* Inserts node 'inline' at the position specified in the InsertNodeRequest.targetName */
	@RequestMapping(value = REST_PATH + "/insertNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody InsertNodeResponse insertNode(@RequestBody InsertNodeRequest req) throws Exception {
		logRequest("insertNode", req);

		InsertNodeResponse res = new InsertNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String parentNodeId = req.getParentId();
		// System.out.println("Inserting under parent: " + parentNodeId);
		Node parentNode = JcrUtil.findNode(session, parentNodeId);

		String name = XString.isEmpty(req.getNewNodeName()) ? JcrUtil.getGUID() : req.getNewNodeName();

		/* NT_UNSTRUCTURED IS ORDERABLE */
		Node newNode = parentNode.addNode(AppConstant.NAMESPACE + ":" + name, JcrConstants.NT_UNSTRUCTURED);
		newNode.setProperty("jcr:content", "");
		JcrUtil.timestampNewNode(session, newNode);
		session.save();

		if (!XString.isEmpty(req.getTargetName())) {
			parentNode.orderBefore(newNode.getName(), req.getTargetName());
		}

		session.save();
		res.setNewNode(Convert.convertToNodeInfo(session, newNode));
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/insertBook", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody InsertBookResponse insertBook(@RequestBody InsertBookRequest req) throws Exception {
		logRequest("insertBook", req);
		if (sessionContext.isAdmin()) {
			throw new Exception("insertBook is an admin-only feature.");
		}

		InsertBookResponse res = new InsertBookResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		/* for now we don't check book name. Only one book exists: War and Peace */
		// String name = req.getBookName();

		ImportWarAndPeace iwap = SpringContextUtil.getApplicationContext().getBean(ImportWarAndPeace.class);
		iwap.importBook(session, "classpath:/com/meta64/mobile/util/war-and-peace.txt", node);

		session.save();
		res.setSuccess(true);

		return res;
	}

	@RequestMapping(value = REST_PATH + "/deleteNodes", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeleteNodesResponse deleteNodes(@RequestBody DeleteNodesRequest req) throws Exception {
		logRequest("deleteNodes", req);

		DeleteNodesResponse res = new DeleteNodesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		for (String nodeId : req.getNodeIds()) {
			log.debug("Deleting ID: " + nodeId);
			try {
				Node node = JcrUtil.findNode(session, nodeId);
				node.remove();
			}
			catch (Exception e) {
				// silently ignore if node cannot be found.
			}
		}
		session.save();
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/moveNodes", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody MoveNodesResponse moveNodes(@RequestBody MoveNodesRequest req) throws Exception {
		logRequest("moveNodes", req);

		MoveNodesResponse res = new MoveNodesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String targetId = req.getTargetNodeId();
		Node targetNode = JcrUtil.findNode(session, targetId);
		String targetPath = targetNode.getPath();
		String targetChildId = req.getTargetChildId();

		for (String nodeId : req.getNodeIds()) {
			log.debug("Moving ID: " + nodeId);
			try {
				Node node = JcrUtil.findNode(session, nodeId);

				/*
				 * This code moves the copied nodes to the bottom of child list underneath the
				 * target node (i.e. targetNode being the parent) for the new node locations.
				 */

				String srcPath = node.getPath();
				String dstPath = targetPath + "/" + node.getName();
				// log.debug("MOVE: srcPath[" + srcPath + "] targetPath[" + dstPath + "]");
				session.move(srcPath, dstPath);

				// session.save();

				/*
				 * This code did not work as expected (or at all). This is supposed to move the new
				 * nodes into the proper ordinal position, and doesn't work. Since this is lower
				 * priority, i'm not even going to try to figure this out for now, and will just
				 * leave it as technical debt, TODO
				 */
				// if (targetChildId != null) {
				// targetNode.orderBefore(dstPath, targetChildId);
				// //session.save();
				// }
			}
			catch (Exception e) {
				// silently ignore if node cannot be found.
			}
		}
		session.save();
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/deleteAttachment", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeleteAttachmentResponse deleteAttachment(@RequestBody DeleteAttachmentRequest req) throws Exception {
		logRequest("deleteAttachment", req);

		DeleteAttachmentResponse res = new DeleteAttachmentResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		Node nodeToRemove = session.getNode(node.getPath() + "/" + AppConstant.JCR_PROP_BIN);
		nodeToRemove.remove();
		session.save();
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/deleteProperty", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeletePropertyResponse deleteProperty(@RequestBody DeletePropertyRequest req) throws Exception {
		logRequest("deleteProperty", req);

		DeletePropertyResponse res = new DeletePropertyResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		String propertyName = req.getPropName();
		try {
			Property prop = node.getProperty(propertyName);
			if (prop != null) {
				// System.out.println("Deleting property: " + propertyName);
				prop.remove();
			}
			else {
				throw new Exception("Unable to find property to delete: " + propertyName);
			}
		}
		catch (Exception e) {
			/*
			 * Don't rethrow this exception. We want to keep processing any properties we can
			 * successfully process
			 */
			log.info("Failed to delete property: " + propertyName + " Reason: " + e.getMessage());
		}

		session.save();
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/saveNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SaveNodeResponse saveNode(@RequestBody SaveNodeRequest req) throws Exception {
		logRequest("saveNode", req);
		SaveNodeResponse res = new SaveNodeResponse();
		ThreadLocals.setResponse(res);

		Session session = ThreadLocals.getJcrSession();
		String nodeId = req.getNodeId();
		log.debug("saveNode. nodeId=" + nodeId);
		Node node = JcrUtil.findNode(session, nodeId);

		if (req.getProperties() != null && req.getProperties().size() > 0) {
			for (PropertyInfo property : req.getProperties()) {

				/*
				 * save only if server determines the property is savable. Just protection. Client
				 * shouldn't be trying to save stuff that is illegal to save, but we have to assume
				 * the worst behaviour from client code, for security and robustness.
				 */
				if (JcrUtil.isSavableProperty(property.getName())) {
					log.debug("Property to save: " + property.getName() + "=" + property.getValue());
					node.setProperty(property.getName(), property.getValue());
				}
				else {
					log.debug("Ignoring rogue save attempt on prop: " + property.getName());
				}
			}

			session.save();
		}
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/makeNodeReferencable", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody MakeNodeReferencableResponse makeNodeReferencable(@RequestBody MakeNodeReferencableRequest req) throws Exception {
		logRequest("makeNodeReferencable", req);

		MakeNodeReferencableResponse res = new MakeNodeReferencableResponse();
		ThreadLocals.setResponse(res);

		Session session = ThreadLocals.getJcrSession();
		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);

		if (node != null) {
			/* if node already has uuid then we can do nothing here, we just silently return success */
			if (!node.hasProperty("jcr:uuid")) {
				node.addMixin(JcrConstants.MIX_REFERENCEABLE);
				session.save();
			}
			res.setSuccess(true);
		}

		return res;
	}

	@RequestMapping(value = REST_PATH + "/saveProperty", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SavePropertyResponse saveProperty(@RequestBody SavePropertyRequest req) throws Exception {
		logRequest("saveProperty", req);
		SavePropertyResponse res = new SavePropertyResponse();
		ThreadLocals.setResponse(res);

		Session session = ThreadLocals.getJcrSession();

		String nodeId = req.getNodeId();
		Node node = JcrUtil.findNode(session, nodeId);
		node.setProperty(req.getPropertyName(), req.getPropertyValue());
		session.save();

		PropertyInfo propertySaved = new PropertyInfo(-1, req.getPropertyName(), req.getPropertyValue(), null);
		res.setPropertySaved(propertySaved);

		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/changePassword", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ChangePasswordResponse changePassword(@RequestBody ChangePasswordRequest req) throws Exception {
		logRequest("changePassword", req);
		ChangePasswordResponse res = new ChangePasswordResponse();
		ThreadLocals.setResponse(res);

		Session session = ThreadLocals.getJcrSession();
		UserManagerUtil.changePassword(session, req.getNewPassword());
		session.save();
		sessionContext.setPassword(req.getNewPassword());
		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/bin", method = RequestMethod.GET)
	@OakSession
	public @ResponseBody ResponseEntity<InputStreamResource> getBinary(@RequestParam("nodeId") String nodeId) throws Exception {
		logRequest("bin", null);
		try {
			Session session = ThreadLocals.getJcrSession();
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

	// http://blog.netgloo.com/2015/02/08/spring-boot-file-upload-with-ajax/
	@RequestMapping(value = REST_PATH + "/upload", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ResponseEntity<?> upload(@RequestParam("nodeId") String nodeId, @RequestParam("file") MultipartFile uploadfile) throws Exception {

		logRequest("upload", null);
		try {
			String fileName = uploadfile.getOriginalFilename();
			Session session = ThreadLocals.getJcrSession();
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

			Binary binary = session.getValueFactory().createBinary(uploadfile.getInputStream());

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

	@RequestMapping(value = REST_PATH + "/anonPageLoad", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody AnonPageLoadResponse anonPageLoad(@RequestBody AnonPageLoadRequest req) throws Exception {
		logRequest("anonPageLoad", req);
		AnonPageLoadResponse res = new AnonPageLoadResponse();
		Session session = ThreadLocals.getJcrSession();

		String id = sessionContext.getUrlId() != null ? sessionContext.getUrlId() : anonUserLandingPageNode;

		if (!XString.isEmpty(id)) {
			RenderNodeResponse renderNodeRes = new RenderNodeResponse();
			RenderNodeRequest renderNodeReq = new RenderNodeRequest();

			/*
			 * if user specified an ID= parameter on the url, we display that immediately, or else
			 * we display the node that the admin has configured to be the default landing page
			 * node.
			 */
			renderNodeReq.setNodeId(id);
			nodeRenderService.renderNode(session, renderNodeReq, renderNodeRes);
			res.setRenderNodeResponse(renderNodeRes);
		}
		else {
			res.setContent("Hello Everyone!");
		}

		res.setSuccess(true);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/nodeSearch", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody NodeSearchResponse nodeSearch(@RequestBody NodeSearchRequest req) throws Exception {
		logRequest("nodeSearch", req);

		NodeSearchResponse res = new NodeSearchResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		nodeSearchService.search(session, req, res);
		return res;
	}
}
