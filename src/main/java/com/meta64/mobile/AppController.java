package com.meta64.mobile;

import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
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
import com.meta64.mobile.config.ConstantsProviderImpl;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.image.CaptchaMaker;
import com.meta64.mobile.repo.OakRepositoryBean;
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
import com.meta64.mobile.request.LogoutRequest;
import com.meta64.mobile.request.MakeNodeReferencableRequest;
import com.meta64.mobile.request.MoveNodesRequest;
import com.meta64.mobile.request.NodeSearchRequest;
import com.meta64.mobile.request.RemovePrivilegeRequest;
import com.meta64.mobile.request.RenderNodeRequest;
import com.meta64.mobile.request.SaveNodeRequest;
import com.meta64.mobile.request.SavePropertyRequest;
import com.meta64.mobile.request.SaveUserPreferencesRequest;
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
import com.meta64.mobile.response.LogoutResponse;
import com.meta64.mobile.response.MakeNodeReferencableResponse;
import com.meta64.mobile.response.MoveNodesResponse;
import com.meta64.mobile.response.NodeSearchResponse;
import com.meta64.mobile.response.RemovePrivilegeResponse;
import com.meta64.mobile.response.RenderNodeResponse;
import com.meta64.mobile.response.SaveNodeResponse;
import com.meta64.mobile.response.SavePropertyResponse;
import com.meta64.mobile.response.SaveUserPreferencesResponse;
import com.meta64.mobile.response.SetNodePositionResponse;
import com.meta64.mobile.response.SignupResponse;
import com.meta64.mobile.service.AclService;
import com.meta64.mobile.service.AttachmentService;
import com.meta64.mobile.service.ImportExportService;
import com.meta64.mobile.service.NodeEditService;
import com.meta64.mobile.service.NodeMoveService;
import com.meta64.mobile.service.NodeRenderService;
import com.meta64.mobile.service.NodeSearchService;
import com.meta64.mobile.service.UserManagerService;
import com.meta64.mobile.util.BrandingUtil;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.SpringMvcUtil;
import com.meta64.mobile.util.ThreadLocals;

/**
 * Primary Spring MVC controller, that returns the main page, process REST calls from the client
 * javascript, and also performs the uploading/download, and serving of images. All of the business
 * logic in here will eventually be moved out into service or utility classes.
 * 
 * Note, it's critical to understand the OakSession AOP code or else this class will be confusing
 * regarding how the OAK transations are managed and how logging in is done.
 */
@Controller
@Scope("session")
public class AppController {
	private static final Logger log = LoggerFactory.getLogger(AppController.class);

	private static final String REST_PATH = "/mobile/rest";

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

	@Autowired
	private NodeEditService nodeEditService;

	@Autowired
	private NodeMoveService nodeMoveService;

	@Autowired
	AttachmentService attachmentService;

	@Autowired
	private AclService aclService;

	@Autowired
	private OakRepositoryBean oak;

	@Autowired
	private BrandingUtil brandingUtil;

	/*
	 * Each time the server restarts we have a new version number here and will cause clients to
	 * download new version of JS files into their local browser cache. For now the assumption is
	 * that this is better then having to remember to update version numbers to invalidate client
	 * caches, but in production systems we may not want to push new JS just because of a server
	 * restart so this will change in the future. That is, the 'currentTimeMillis' part will change
	 * to some kind of an actual version number or something, that will be part of managed releases.
	 */
	public static final long jsVersion = System.currentTimeMillis();
	public static final long cssVersion = jsVersion; // match jsVersion for now, why not.

	/*
	 * This is an acceptable hack to reference the Impl class directly like this.
	 */
	static {
		ConstantsProviderImpl.setCacheVersion(String.valueOf(jsVersion));
	}

	private static void logRequest(String url, Object req) throws Exception {
		log.debug("REQ=" + url + " " + (req == null ? "none" : Convert.JsonStringify(req)));
	}

	/*
	 * This is the actual app page loading request, which is a thymeleaf-model-based call.
	 * 
	 * ID is optional url parameter that user can specify to access a specific node in the
	 * repository by uuid.
	 */
	@RequestMapping("/")
	public String mobile(@RequestParam(value = "id", required = false) String id, Model model) throws Exception {
		logRequest("mobile", null);

		log.debug("Rendering main page: current userName: " + sessionContext.getUserName() + " id=" + id);

		brandingUtil.addBrandingAttributes(model);

		SpringMvcUtil.addJsFileNameProps(model, String.valueOf(jsVersion), "scriptLoader");
		SpringMvcUtil.addCssFileNameProps(model, String.valueOf(cssVersion), "meta64");
		
		sessionContext.setUrlId(id);
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
		userManagerService.signup(req, res);
		return res;
	}

	/*
	 * Login mechanism is a bit tricky because the OakSession ASPECT (AOP) actually detects the
	 * LoginRequest and performs authentication BEFORE this 'login' method even gets called, so by
	 * the time we are in this method we can safely assume the userName and password resulted in a
	 * successful login. If login fails the getJcrSession() call below will return null also.
	 * 
	 * IMPORTANT: this method DOES get called even on a fresh page load when the user hasn't logged
	 * in yet, and this is done passing "{session}" in place of userName/password, which tells this
	 * login method to get a username/password from the session. So a valid session that's already
	 * logged in will simply return the correct login information from here as if it were logging in
	 * the first time. For an SPA (Single Page App), handling page reloads needs to do something
	 * like this because we aren't just having session beans embedded on some JSP the old-school
	 * way, this is different and this is better! This is the proper way for an SPA to handle page
	 * reloads.
	 * 
	 * @see OakSessionAspect.loginFromJoinPoint()
	 */
	@RequestMapping(value = REST_PATH + "/login", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody LoginResponse login(@RequestBody LoginRequest req) throws Exception {
		logRequest("login", req);
		LoginResponse res = new LoginResponse();
		ThreadLocals.setResponse(res);
		res.setMessage("success: " + String.valueOf(++sessionContext.counter));
		Session session = ThreadLocals.getJcrSession();
		userManagerService.login(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/logout", method = RequestMethod.POST)
	// @OakSession // commenting since we currently don't touch the DB during a logout.
	public @ResponseBody LogoutResponse logout(@RequestBody LogoutRequest req, HttpSession session) throws Exception {
		logRequest("logout", req);

		/*
		 * DO NOT DELETE:
		 * 
		 * We are defining this method with a 'session' parameter, because Spring will automatically
		 * autowire that correctly, but here is another good way to do it:
		 * 
		 * ServletRequestAttributes attr = (ServletRequestAttributes)
		 * RequestContextHolder.currentRequestAttributes(); HttpSession session =
		 * attr.getRequest().getSession();
		 */

		session.invalidate();
		LogoutResponse res = new LogoutResponse();
		ThreadLocals.setResponse(res);
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
		aclService.getNodePrivileges(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/addPrivilege", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody AddPrivilegeResponse addPrivilege(@RequestBody AddPrivilegeRequest req) throws Exception {
		logRequest("addPrivilege", req);
		AddPrivilegeResponse res = new AddPrivilegeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		aclService.addPrivilege(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/removePrivilege", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody RemovePrivilegeResponse removePrivilege(@RequestBody RemovePrivilegeRequest req) throws Exception {
		logRequest("removePrivilege", req);
		RemovePrivilegeResponse res = new RemovePrivilegeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		aclService.removePrivilege(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/exportToXml", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ExportResponse exportToXml(@RequestBody ExportRequest req) throws Exception {
		logRequest("exportToXml", req);
		ExportResponse res = new ExportResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		importExportService.exportToXml(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/import", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ImportResponse importFromFile(@RequestBody ImportRequest req) throws Exception {
		logRequest("import", req);
		ImportResponse res = new ImportResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();

		String fileName = req.getSourceFileName();
		if (fileName.toLowerCase().endsWith(".xml")) {
			importExportService.importFromXml(session, req, res);
			// It is not a mistake that there is no session.save() here. The import is using the
			// workspace object
			// which specifically documents that the saving on the session is not needed.
		}
		else if (fileName.toLowerCase().endsWith(".zip")) {
			importExportService.importFromZip(session, req, res);
			session.save();
		}
		else {
			throw new Exception("Unable to import from file with unknown extension: " + fileName);
		}
		return res;
	}

	@RequestMapping(value = REST_PATH + "/setNodePosition", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SetNodePositionResponse setNodePosition(@RequestBody SetNodePositionRequest req) throws Exception {
		logRequest("setNodePosition", req);
		SetNodePositionResponse res = new SetNodePositionResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeMoveService.setNodePosition(session, req, res);
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
		nodeEditService.createSubNode(session, req, res);
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
		nodeEditService.insertNode(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/insertBook", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody InsertBookResponse insertBook(@RequestBody InsertBookRequest req) throws Exception {
		logRequest("insertBook", req);
		InsertBookResponse res = new InsertBookResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		importExportService.insertBook(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/deleteNodes", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeleteNodesResponse deleteNodes(@RequestBody DeleteNodesRequest req) throws Exception {
		logRequest("deleteNodes", req);
		DeleteNodesResponse res = new DeleteNodesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeMoveService.deleteNodes(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/moveNodes", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody MoveNodesResponse moveNodes(@RequestBody MoveNodesRequest req) throws Exception {
		logRequest("moveNodes", req);
		MoveNodesResponse res = new MoveNodesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeMoveService.moveNodes(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/deleteAttachment", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeleteAttachmentResponse deleteAttachment(@RequestBody DeleteAttachmentRequest req) throws Exception {
		logRequest("deleteAttachment", req);
		DeleteAttachmentResponse res = new DeleteAttachmentResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		attachmentService.deleteAttachment(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/deleteProperty", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeletePropertyResponse deleteProperty(@RequestBody DeletePropertyRequest req) throws Exception {
		logRequest("deleteProperty", req);
		DeletePropertyResponse res = new DeletePropertyResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.deleteProperty(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/saveNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SaveNodeResponse saveNode(@RequestBody SaveNodeRequest req) throws Exception {
		logRequest("saveNode", req);
		SaveNodeResponse res = new SaveNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.saveNode(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/makeNodeReferencable", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody MakeNodeReferencableResponse makeNodeReferencable(@RequestBody MakeNodeReferencableRequest req) throws Exception {
		logRequest("makeNodeReferencable", req);
		MakeNodeReferencableResponse res = new MakeNodeReferencableResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.makeNodeReferencable(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/saveProperty", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SavePropertyResponse saveProperty(@RequestBody SavePropertyRequest req) throws Exception {
		logRequest("saveProperty", req);
		SavePropertyResponse res = new SavePropertyResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.saveProperty(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/changePassword", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ChangePasswordResponse changePassword(@RequestBody ChangePasswordRequest req) throws Exception {
		logRequest("changePassword", req);
		ChangePasswordResponse res = new ChangePasswordResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		userManagerService.changePassword(session, req, res);
		return res;
	}

	@RequestMapping(value = REST_PATH + "/bin", method = RequestMethod.GET)
	@OakSession
	public @ResponseBody ResponseEntity<InputStreamResource> getBinary(@RequestParam("nodeId") String nodeId) throws Exception {
		logRequest("bin", null);
		Session session = ThreadLocals.getJcrSession();
		return attachmentService.getBinary(session, nodeId);
	}

	// http://blog.netgloo.com/2015/02/08/spring-boot-file-upload-with-ajax/
	@RequestMapping(value = REST_PATH + "/upload", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ResponseEntity<?> upload(@RequestParam("nodeId") String nodeId, @RequestParam("file") MultipartFile uploadFile) throws Exception {
		logRequest("upload", null);
		Session session = ThreadLocals.getJcrSession();
		return attachmentService.upload(session, nodeId, uploadFile);
	}

	@RequestMapping(value = REST_PATH + "/anonPageLoad", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody AnonPageLoadResponse anonPageLoad(@RequestBody AnonPageLoadRequest req) throws Exception {
		logRequest("anonPageLoad", req);
		AnonPageLoadResponse res = new AnonPageLoadResponse();
		Session session = ThreadLocals.getJcrSession();
		nodeRenderService.anonPageLoad(session, req, res);
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

	@RequestMapping(value = REST_PATH + "/saveUserPreferences", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SaveUserPreferencesResponse saveUserPreferences(@RequestBody SaveUserPreferencesRequest req) throws Exception {
		logRequest("saveUserPreferences", req);
		SaveUserPreferencesResponse res = new SaveUserPreferencesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		userManagerService.saveUserPreferences(session, req, res);
		session.save();
		res.setSuccess(true);
		return res;
	}
}
