package com.meta64.mobile.service;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.social.connect.Connection;
import org.springframework.social.oauth1.AuthorizedRequestToken;
import org.springframework.social.oauth1.OAuth1Operations;
import org.springframework.social.oauth1.OAuthToken;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.connect.TwitterConnectionFactory;
import org.springframework.stereotype.Component;

import com.meta64.mobile.config.ConstantsProvider;
import com.meta64.mobile.config.SessionContext;
import com.meta64.mobile.repo.OakRepository;
import com.meta64.mobile.user.RunAsJcrAdmin;
import com.meta64.mobile.util.XString;

/**
 * Service methods for processing user management functions. Login, logout, signup, user
 * preferences, and settings persisted per-user
 * 
 */
@Component
@Scope("singleton")
public class OAuthLoginService {
	private static final Logger log = LoggerFactory.getLogger(OAuthLoginService.class);

	@Value("${anonUserLandingPageNode}")
	private String anonUserLandingPageNode;

	@Autowired
	private OakRepository oak;

	@Autowired
	private SessionContext sessionContext;

	@Autowired
	private RunAsJcrAdmin adminRunner;

	@Autowired
	private NodeSearchService nodeSearchService;

	@Autowired
	private ConstantsProvider constProvider;

	@Value("${spring.social.twitter.app-id}")
	private String twitterAppId;

	@Value("${spring.social.twitter.app-secret}")
	private String twitterAppSecret;

	private TwitterConnectionFactory twitterConnectionFactory;
	private static final Object twitterConnectionFactoryLock = new Object();

	/*
	 * TODO: this map, and the twitterLogin and twitterCallback methods will be moved into a service
	 * class (out of this class), very soon.
	 */
	private final HashMap<String, OAuthToken> oauthTokenMap = new HashMap<String, OAuthToken>();

	public String getTwitterLoginUrl() throws Exception {
		if (XString.isEmpty(twitterAppId) || XString.isEmpty(twitterAppSecret)) {
			throw new Exception("Meta64 instance is not configured for twitter logins.");
		}

		TwitterConnectionFactory connectionFactory = getTwitterConnectionFactory();
		OAuth1Operations oauthOperations = connectionFactory.getOAuthOperations();
		OAuthToken token = oauthOperations.fetchRequestToken(constProvider.getHostAndPort() + "/twitterAuth", null);

		oauthTokenMap.put(token.getValue(), token);

		String url = oauthOperations.buildAuthenticateUrl(token.getValue(), null);
		return url;
	}

	public String completeAuthenticaion(String oauthToken, String oauthVerifier) throws Exception {
		if (XString.isEmpty(twitterAppId) || XString.isEmpty(twitterAppSecret)) {
			throw new Exception("Meta64 instance is not configured for twitter logins.");
		}

		log.debug("TwitterCallback: Sent: " + oauthToken);
		if (!oauthTokenMap.containsKey(oauthToken)) {
			throw new Exception("Bad oauth_token");
		}
		OAuthToken token = oauthTokenMap.get(oauthToken);
		/*
		 * TODO: Very slight risk of memory leak here. Should we only remove it after a certain age,
		 * like 2 hours, or even attach it to the invalidation of this session also?
		 */
		oauthTokenMap.remove(oauthToken);

		TwitterConnectionFactory connectionFactory = getTwitterConnectionFactory();
		OAuth1Operations oauthOperations = connectionFactory.getOAuthOperations();
		OAuthToken accessToken = oauthOperations.exchangeForAccessToken(new AuthorizedRequestToken(token, oauthVerifier), null);
		Connection<Twitter> twitterConnection = connectionFactory.createConnection(accessToken);

		String userName = twitterConnection.fetchUserProfile().getUsername();
		log.debug("Twitter Login successful as: UserName: " + userName);
		return userName;
	}

	private TwitterConnectionFactory getTwitterConnectionFactory() {
		synchronized (twitterConnectionFactoryLock) {
			if (twitterConnectionFactory == null) {
				twitterConnectionFactory = new TwitterConnectionFactory(twitterAppId, twitterAppSecret);
			}
			return twitterConnectionFactory;
		}
	}
}
