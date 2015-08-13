package com.meta64.mobile;

import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.meta64.mobile.annotate.OakSession;
import com.meta64.mobile.config.JcrProp;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.image.CaptchaMaker;
import com.meta64.mobile.repo.OakRepository;
import com.meta64.mobile.request.AddPrivilegeRequest;
import com.meta64.mobile.request.AnonPageLoadRequest;
import com.meta64.mobile.request.ChangePasswordRequest;
import com.meta64.mobile.request.CloseAccountRequest;
import com.meta64.mobile.request.CreateSubNodeRequest;
import com.meta64.mobile.request.DeleteAttachmentRequest;
import com.meta64.mobile.request.DeleteNodesRequest;
import com.meta64.mobile.request.DeletePropertyRequest;
import com.meta64.mobile.request.ExportRequest;
import com.meta64.mobile.request.GetNodePrivilegesRequest;
import com.meta64.mobile.request.ImportRequest;
import com.meta64.mobile.request.InitNodeEditRequest;
import com.meta64.mobile.request.InsertBookRequest;
import com.meta64.mobile.request.InsertNodeRequest;
import com.meta64.mobile.request.LoginRequest;
import com.meta64.mobile.request.LogoutRequest;
import com.meta64.mobile.request.MakeNodeReferencableRequest;
import com.meta64.mobile.request.MoveNodesRequest;
import com.meta64.mobile.request.NodeSearchRequest;
import com.meta64.mobile.request.RemovePrivilegeRequest;
import com.meta64.mobile.request.RenameNodeRequest;
import com.meta64.mobile.request.RenderNodeRequest;
import com.meta64.mobile.request.SaveNodeRequest;
import com.meta64.mobile.request.SavePropertyRequest;
import com.meta64.mobile.request.SaveUserPreferencesRequest;
import com.meta64.mobile.request.SetNodePositionRequest;
import com.meta64.mobile.request.SignupRequest;
import com.meta64.mobile.request.UploadFromUrlRequest;
import com.meta64.mobile.response.AddPrivilegeResponse;
import com.meta64.mobile.response.AnonPageLoadResponse;
import com.meta64.mobile.response.ChangePasswordResponse;
import com.meta64.mobile.response.CloseAccountResponse;
import com.meta64.mobile.response.CreateSubNodeResponse;
import com.meta64.mobile.response.DeleteAttachmentResponse;
import com.meta64.mobile.response.DeleteNodesResponse;
import com.meta64.mobile.response.DeletePropertyResponse;
import com.meta64.mobile.response.ExportResponse;
import com.meta64.mobile.response.GetNodePrivilegesResponse;
import com.meta64.mobile.response.ImportResponse;
import com.meta64.mobile.response.InitNodeEditResponse;
import com.meta64.mobile.response.InsertBookResponse;
import com.meta64.mobile.response.InsertNodeResponse;
import com.meta64.mobile.response.LoginResponse;
import com.meta64.mobile.response.LogoutResponse;
import com.meta64.mobile.response.MakeNodeReferencableResponse;
import com.meta64.mobile.response.MoveNodesResponse;
import com.meta64.mobile.response.NodeSearchResponse;
import com.meta64.mobile.response.RemovePrivilegeResponse;
import com.meta64.mobile.response.RenameNodeResponse;
import com.meta64.mobile.response.RenderNodeResponse;
import com.meta64.mobile.response.SaveNodeResponse;
import com.meta64.mobile.response.SavePropertyResponse;
import com.meta64.mobile.response.SaveUserPreferencesResponse;
import com.meta64.mobile.response.SetNodePositionResponse;
import com.meta64.mobile.response.SignupResponse;
import com.meta64.mobile.response.UploadFromUrlResponse;
import com.meta64.mobile.service.AclService;
import com.meta64.mobile.service.AttachmentService;
import com.meta64.mobile.service.ImportExportService;
import com.meta64.mobile.service.NodeEditService;
import com.meta64.mobile.service.NodeMoveService;
import com.meta64.mobile.service.NodeRenderService;
import com.meta64.mobile.service.NodeSearchService;
import com.meta64.mobile.service.OAuthLoginService;
import com.meta64.mobile.service.UserManagerService;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.BrandingUtil;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.JcrUtil;
import com.meta64.mobile.util.SpringMvcUtil;
import com.meta64.mobile.util.ThreadLocals;
import com.meta64.mobile.util.ValContainer;

/**
 * Primary Spring MVC controller. All application logic from the browser connects directly to this
 * controller which is the only controller. Importantly the main SPA page is retrieved thru this
 * controller, and the binary attachments are also served up thru this interface.
 * 
 * Note, it's critical to understand the OakSession AOP code or else this class will be confusing
 * regarding how the OAK transations are managed and how logging in is done.
 * 
 * This class has no documentation on the methods because it's a wrapper around the service methods
 * which is where the documentation can be found. It's a better architecture to have all the AOP for
 * any given aspect be in one particular layer, because of how Spring AOP uses Proxies. Things can
 * get pretty ugly when you have various proxied objects calling other proxies objects, so I have
 * all the AOP for a service call in this controller and then all the services are pure and simple
 * Spring Singletons.
 */
@Controller
public class AppController {
	private static final Logger log = LoggerFactory.getLogger(AppController.class);

	private static final String API_PATH = "/mobile/api";

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
	private OakRepository oak;

	@Autowired
	private BrandingUtil brandingUtil;

	@Autowired
	private SpringMvcUtil springMvcUtil;

	@Autowired
	private OAuthLoginService oauthLoginService;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@RequestMapping(value = "/twitterLogin", method = RequestMethod.GET)
	public ModelAndView twitterLogin(Model model) throws Exception {
		logRequest("twitterLogin", null);
		String url = oauthLoginService.getTwitterLoginUrl();
		return new ModelAndView("redirect:" + url);
	}

	@RequestMapping("/twitterAuth")
	public String twitterCallback(//
			@RequestParam(value = "oauth_token", required = true) String oauthToken, //
			@RequestParam(value = "oauth_verifier", required = true) String oauthVerifier, //
			Model model) throws Exception {
		logRequest("twitterCallback", null);

		log.debug("Twitter auth callback running.");
		final String userName = oauthLoginService.completeAuthenticaion(oauthToken, oauthVerifier);
		final ValContainer<String> passwordContainer = new ValContainer<String>();

		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				if (!userManagerService.userExists(session, userName, JcrProp.VAL_TWITTER, passwordContainer)) {
					String _password = JcrUtil.getGUID();
					userManagerService.initNewUser(session, userName, _password, null, JcrProp.VAL_TWITTER);
					passwordContainer.setVal(_password);
					log.debug("twitter user created and initialized.");
				}
				else {
					log.debug("twitter account did already exist. Logging in now.");
					// passwordContainer will alredy have correct value here from userExists.
				}
			}
		});

		String password = passwordContainer.getVal();

		/*
		 * Setting credentials into sessionContext should be enough to set to "logged in" state.
		 */
		model.addAttribute("loginSessionReady", "true");

		sessionContext.setUserName(userName);
		sessionContext.setPassword(password);

		log.debug("Session now has credentials attached. pwd=" + password);

		brandingUtil.addBrandingAttributes(model);

		springMvcUtil.addJsFileNameProp(model, "scriptLoaderJs", "/js/scriptLoader");
		springMvcUtil.addCssFileNameProp(model, "meta64Css", "/css/meta64");
		springMvcUtil.addThirdPartyLibs(model);

		return "index";
	}

	/*
	 * This is the actual app page loading request, for his SPA (Single Page Application) this is
	 * the request to load the page.
	 * 
	 * ID is optional url parameter that user can specify to access a specific node in the
	 * repository by uuid.
	 */
	@RequestMapping("/")
	public String mobile(@RequestParam(value = "id", required = false) String id, //
			@RequestParam(value = "signupCode", required = false) String signupCode, //
			Model model) throws Exception {
		logRequest("mobile", null);
		
		if (signupCode != null) {
			userManagerService.processSignupCode(signupCode, model);
		}

		log.debug("Rendering main page: current userName: " + sessionContext.getUserName() + " id=" + id);

		brandingUtil.addBrandingAttributes(model);

		springMvcUtil.addJsFileNameProp(model, "scriptLoaderJs", "/js/scriptLoader");
		springMvcUtil.addCssFileNameProp(model, "meta64Css", "/css/meta64");
		springMvcUtil.addThirdPartyLibs(model);

		sessionContext.setUrlId(id);
		return "index";
	}

	@RequestMapping(value = API_PATH + "/captcha", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
	public @ResponseBody byte[] captcha() throws Exception {
		logRequest("captcha", null);
		String captcha = CaptchaMaker.createCaptchaString();
		sessionContext.setCaptcha(captcha);
		return CaptchaMaker.makeCaptcha(captcha);
	}

	@RequestMapping(value = API_PATH + "/signup", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SignupResponse signup(@RequestBody SignupRequest req) throws Exception {
		logRequest("signup", req);
		SignupResponse res = new SignupResponse();
		ThreadLocals.setResponse(res);
		userManagerService.signup(req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/login", method = RequestMethod.POST)
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

	@RequestMapping(value = API_PATH + "/closeAccount", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody CloseAccountResponse closeAccount(@RequestBody CloseAccountRequest req) throws Exception {
		logRequest("closeAccount", req);
		CloseAccountResponse res = new CloseAccountResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		userManagerService.closeAccount(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/logout", method = RequestMethod.POST)
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

	@RequestMapping(value = API_PATH + "/renderNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody RenderNodeResponse renderNode(@RequestBody RenderNodeRequest req) throws Exception {
		logRequest("renderNode", req);
		RenderNodeResponse res = new RenderNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeRenderService.renderNode(session, req, res, true);
		return res;
	}
	
	@RequestMapping(value = API_PATH + "/initNodeEdit", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody InitNodeEditResponse initNodeEdit(@RequestBody InitNodeEditRequest req) throws Exception {
		logRequest("initNodeEdit", req);
		InitNodeEditResponse res = new InitNodeEditResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeRenderService.initNodeEdit(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/getNodePrivileges", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody GetNodePrivilegesResponse getNodePrivileges(@RequestBody GetNodePrivilegesRequest req) throws Exception {
		logRequest("getNodePrivileges", req);
		GetNodePrivilegesResponse res = new GetNodePrivilegesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		aclService.getNodePrivileges(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/addPrivilege", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody AddPrivilegeResponse addPrivilege(@RequestBody AddPrivilegeRequest req) throws Exception {
		logRequest("addPrivilege", req);
		AddPrivilegeResponse res = new AddPrivilegeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		aclService.addPrivilege(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/removePrivilege", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody RemovePrivilegeResponse removePrivilege(@RequestBody RemovePrivilegeRequest req) throws Exception {
		logRequest("removePrivilege", req);
		RemovePrivilegeResponse res = new RemovePrivilegeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		aclService.removePrivilege(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/exportToXml", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ExportResponse exportToXml(@RequestBody ExportRequest req) throws Exception {
		logRequest("exportToXml", req);
		ExportResponse res = new ExportResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		importExportService.exportToXml(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/import", method = RequestMethod.POST)
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

	@RequestMapping(value = API_PATH + "/setNodePosition", method = RequestMethod.POST)
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
	@RequestMapping(value = API_PATH + "/createSubNode", method = RequestMethod.POST)
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
	@RequestMapping(value = API_PATH + "/insertNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody InsertNodeResponse insertNode(@RequestBody InsertNodeRequest req) throws Exception {
		logRequest("insertNode", req);
		InsertNodeResponse res = new InsertNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.insertNode(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/renameNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody RenameNodeResponse renameNode(@RequestBody RenameNodeRequest req) throws Exception {
		logRequest("renameNode", req);
		RenameNodeResponse res = new RenameNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.renameNode(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/insertBook", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody InsertBookResponse insertBook(@RequestBody InsertBookRequest req) throws Exception {
		logRequest("insertBook", req);
		InsertBookResponse res = new InsertBookResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		importExportService.insertBook(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/deleteNodes", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeleteNodesResponse deleteNodes(@RequestBody DeleteNodesRequest req) throws Exception {
		logRequest("deleteNodes", req);
		DeleteNodesResponse res = new DeleteNodesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeMoveService.deleteNodes(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/moveNodes", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody MoveNodesResponse moveNodes(@RequestBody MoveNodesRequest req) throws Exception {
		logRequest("moveNodes", req);
		MoveNodesResponse res = new MoveNodesResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeMoveService.moveNodes(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/deleteAttachment", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeleteAttachmentResponse deleteAttachment(@RequestBody DeleteAttachmentRequest req) throws Exception {
		logRequest("deleteAttachment", req);
		DeleteAttachmentResponse res = new DeleteAttachmentResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		attachmentService.deleteAttachment(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/deleteProperty", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody DeletePropertyResponse deleteProperty(@RequestBody DeletePropertyRequest req) throws Exception {
		logRequest("deleteProperty", req);
		DeletePropertyResponse res = new DeletePropertyResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.deleteProperty(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/saveNode", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SaveNodeResponse saveNode(@RequestBody SaveNodeRequest req) throws Exception {
		logRequest("saveNode", req);
		SaveNodeResponse res = new SaveNodeResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.saveNode(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/makeNodeReferencable", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody MakeNodeReferencableResponse makeNodeReferencable(@RequestBody MakeNodeReferencableRequest req) throws Exception {
		logRequest("makeNodeReferencable", req);
		MakeNodeReferencableResponse res = new MakeNodeReferencableResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.makeNodeReferencable(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/saveProperty", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody SavePropertyResponse saveProperty(@RequestBody SavePropertyRequest req) throws Exception {
		logRequest("saveProperty", req);
		SavePropertyResponse res = new SavePropertyResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeEditService.saveProperty(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/changePassword", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ChangePasswordResponse changePassword(@RequestBody ChangePasswordRequest req) throws Exception {
		logRequest("changePassword", req);
		ChangePasswordResponse res = new ChangePasswordResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		userManagerService.changePassword(session, req, res);
		return res;
	}

	/*
	 * TODO: need to persist the real filename when uploaded, and then make the links actually
	 * reference that filename on this type of path. Will have to add to binary info property sent
	 * to client in JSON.
	 */
	@RequestMapping(value = API_PATH + "/bin/{fileName}", method = RequestMethod.GET)
	@OakSession
	public @ResponseBody ResponseEntity<InputStreamResource> getBinary(@PathVariable("fileName") String fileName, @RequestParam("nodeId") String nodeId) throws Exception {
		logRequest("bin", null);
		Session session = ThreadLocals.getJcrSession();
		return attachmentService.getBinary(session, nodeId);
	}

	// http://blog.netgloo.com/2015/02/08/spring-boot-file-upload-with-ajax/
	@RequestMapping(value = API_PATH + "/upload", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody ResponseEntity<?> upload(@RequestParam("nodeId") String nodeId, @RequestParam("file") MultipartFile uploadFile) throws Exception {
		logRequest("upload", null);
		Session session = ThreadLocals.getJcrSession();
		return attachmentService.upload(session, nodeId, uploadFile);
	}

	@RequestMapping(value = API_PATH + "/uploadFromUrl", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody UploadFromUrlResponse uploadFromUrl(@RequestBody UploadFromUrlRequest req) throws Exception {
		logRequest("uploadFromUrl", req);
		UploadFromUrlResponse res = new UploadFromUrlResponse();
		Session session = ThreadLocals.getJcrSession();
		attachmentService.uploadFromUrl(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/anonPageLoad", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody AnonPageLoadResponse anonPageLoad(@RequestBody AnonPageLoadRequest req) throws Exception {
		logRequest("anonPageLoad", req);
		AnonPageLoadResponse res = new AnonPageLoadResponse();
		Session session = ThreadLocals.getJcrSession();
		nodeRenderService.anonPageLoad(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/nodeSearch", method = RequestMethod.POST)
	@OakSession
	public @ResponseBody NodeSearchResponse nodeSearch(@RequestBody NodeSearchRequest req) throws Exception {
		logRequest("nodeSearch", req);
		NodeSearchResponse res = new NodeSearchResponse();
		ThreadLocals.setResponse(res);
		Session session = ThreadLocals.getJcrSession();
		nodeSearchService.search(session, req, res);
		return res;
	}

	@RequestMapping(value = API_PATH + "/saveUserPreferences", method = RequestMethod.POST)
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

	private static void logRequest(String url, Object req) throws Exception {
		log.debug("REQ=" + url + " " + (req == null ? "none" : Convert.JsonStringify(req)));
	}
}
