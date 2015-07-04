package com.meta64.mobile;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.jcr.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.request.AddPrivilegeRequest;
import com.meta64.mobile.request.CreateSubNodeRequest;
import com.meta64.mobile.request.GetNodePrivilegesRequest;
import com.meta64.mobile.request.LoginRequest;
import com.meta64.mobile.request.RemovePrivilegeRequest;
import com.meta64.mobile.request.SignupRequest;
import com.meta64.mobile.response.AddPrivilegeResponse;
import com.meta64.mobile.response.CreateSubNodeResponse;
import com.meta64.mobile.response.GetNodePrivilegesResponse;
import com.meta64.mobile.response.LoginResponse;
import com.meta64.mobile.response.RemovePrivilegeResponse;
import com.meta64.mobile.response.SignupResponse;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.user.UserManagerService;
import com.meta64.mobile.util.Convert;
import com.meta64.mobile.util.JcrRunnable;
import com.meta64.mobile.util.ThreadLocals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AppServer.class)
@WebAppConfiguration
public class AppServerTests {

	private static final String fakeCaptcha = "nocaptcha";
	private static final Logger log = LoggerFactory.getLogger(AppServerTests.class);

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@Autowired
	private UserManagerService userManagerService;

	@Autowired
	private AppController controller;

	@Autowired
	private SessionContext sessionContext;

	/*
	 * Test annotation currently commented out. To run this test you will need to edit
	 * application.properties and put in a jcrAdminPassword property. I don't have spring profiles
	 * working yet and I'm currently passing jcrAdminPassword on command line, so it's not set in
	 * properties currently.
	 */
	// @Test
	public void contextLoads() throws Exception {
		adminRunner.run(new JcrRunnable() {
			@Override
			public void run(Session session) throws Exception {
				SignupRequest signupReq = new SignupRequest();

				/* setup fake captcha */
				sessionContext.setCaptcha(fakeCaptcha);

				/*
				 * Signup a new user
				 */
				String userName = "wclayf" + System.currentTimeMillis();
				signupReq.setUserName(userName);
				signupReq.setPassword(userName);
				signupReq.setEmail("xxx");
				signupReq.setCaptcha(fakeCaptcha);
				SignupResponse signupRes = controller.signup(signupReq);
				assertTrue(signupRes.isSuccess());

				/*
				 * Login the new user
				 */
				LoginRequest loginReq = new LoginRequest();
				loginReq.setUserName(userName);
				loginReq.setPassword(userName);
				LoginResponse loginRes = controller.login(loginReq);
				assertTrue(loginRes.isSuccess());

				String userRoot = loginRes.getRootNode().getId();

				/*
				 * Create a new node under the root node for user.
				 */
				CreateSubNodeRequest createSubNodeReq = new CreateSubNodeRequest();
				createSubNodeReq.setNodeId(userRoot);
				createSubNodeReq.setNewNodeName("test-new-node-name");
				CreateSubNodeResponse createSubNodeRes = controller.createSubNode(createSubNodeReq);
				assertTrue(createSubNodeRes.isSuccess());

				String newNodeId = createSubNodeRes.getNewNode().getId();

				/*
				 * Add a public share privilege on the new node
				 */
				AddPrivilegeRequest addPrivReq = new AddPrivilegeRequest();
				addPrivReq.setPrincipal("everyone");
				addPrivReq.setPrivilege("read");
				addPrivReq.setNodeId(newNodeId);
				AddPrivilegeResponse addPrivRes = controller.addPrivilege(addPrivReq);
				assertTrue(addPrivRes.isSuccess());

				/*
				 * Query the privileges to verify that we see the public share
				 */
				GetNodePrivilegesRequest getPrivsReq = new GetNodePrivilegesRequest();
				getPrivsReq.setNodeId(newNodeId);
				GetNodePrivilegesResponse getPrivsRes = controller.getNodePrivileges(getPrivsReq);
				assertTrue(getPrivsRes.isSuccess());

				/*
				 * Verify public share (in a very lazy hacky way for now)
				 */
				String privs = Convert.JsonStringify(getPrivsRes);
				log.debug("***** PRIVS (with public): " + privs);
				assertTrue(privs.contains("read"));

				/*
				 * Now we remove the share we just added
				 */
				RemovePrivilegeRequest removePrivReq = new RemovePrivilegeRequest();
				removePrivReq.setNodeId(newNodeId);
				removePrivReq.setPrincipal("everyone");
				removePrivReq.setPrivilege("jcr:read");
				RemovePrivilegeResponse removePrivRes = controller.removePrivilege(removePrivReq);
				assertTrue(removePrivRes.isSuccess());

				/*
				 * now read back in privileges to verify the one we removed is now gone
				 */
				getPrivsReq = new GetNodePrivilegesRequest();
				getPrivsReq.setNodeId(newNodeId);
				getPrivsRes = controller.getNodePrivileges(getPrivsReq);
				assertTrue(getPrivsRes.isSuccess());

				privs = Convert.JsonStringify(getPrivsRes);
				log.debug("***** PRIVS (with public REMOVED): " + privs);
				assertTrue(!privs.contains("read"));

				// shareNodeTest(newUserName);
			}
		});
	}

	public void shareNodeTest(String userName) throws Exception {
	}

	public String addNewUser(Session session) throws Exception {
		String userName = "wclayf" + System.currentTimeMillis();
		String password = userName;
		String email = "xxx";
		String captcha = null;

		SignupResponse res = new SignupResponse();
		ThreadLocals.setResponse(res);

		if (userManagerService.signup(userName, password, email, captcha)) {
			log.debug("Signup success.");
		}
		else {
			fail("signup failed.");
		}
		return userName;
	}
}
