package com.anh.him.auth;

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xmpp.packet.JID;

public class HimSessionPlugin implements Plugin {
	private static final String SVC_XMPP_SESSION = "/svc/xmpp/session";
	private static final String JSESSIONID = "JSESSIONID";
	private static final String SVC_LOGIN = "/svc/login";
	private static final String SVC_LOGOUT = "/svc/logout";
	private static final String SVC_SRV_LOGOUT = "/svc/srv-logout";
	private static Logger logger = LoggerFactory
			.getLogger(HimSessionPlugin.class);
	private static Marker logMaker = MarkerFactory.getMarker("HereIam");
	private String serverId = null;
	private String password = null;
	private boolean loginInitialized = false;
	private String himCentralServer = null;
	private String sessionId;
	public static  String PLUGIN_NAME="him-auth";
   
	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		// TODO Auto-generated method stub
		// XMPPServer.getInstance().
		serverId = JiveProperties.getInstance().getProperty(
				"him.auth-server-id", "openhim");
		password = JiveProperties.getInstance().getProperty(
				"him.auth-server-password", "#$#$#$#$");
		himCentralServer = JiveProperties.getInstance().getProperty(
				"him.central-host", "http://localhost:8080");
		SessionEventDispatcher.addListener(sessionListener);
		PLUGIN_NAME = pluginDirectory.getName();

	}
	
	public static void restart(){
		HimSessionPlugin plugin=(HimSessionPlugin)XMPPServer.getInstance().getPluginManager().getPlugin(PLUGIN_NAME);
		plugin.register();
	}
	public static void stop(){
		HimSessionPlugin plugin=(HimSessionPlugin)XMPPServer.getInstance().getPluginManager().getPlugin(PLUGIN_NAME);
		plugin.deRegister();
	}

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		sessionId = null;

	}

	private JSONObject fetchHttpResonse(HttpMethod method) {
		HttpClient client = new HttpClient();
		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				String response = method.getResponseBodyAsString();
				JSONObject obj = new JSONObject(response);
				if (obj.getInt("status") != 0) {
					logger.error(
							logMaker,
							"Proper server authentication configuration missing[serverId,password,centralserver]");
					logger.error(logMaker, obj.getString("message"));
					return null;
				}
				return obj.getJSONObject("result");
			} else {
				logger.error(logMaker, method.getStatusText());
			}

		} catch (HttpException e) {
			logger.error(logMaker.toString(), e);
		} catch (Exception e) {
			logger.error(logMaker.toString(), e);
		}

		return null;

	}

	private void deRegister() {
		if (himCentralServer == null || serverId == null || password == null) {
			loginInitialized = false;
			logger.error(logMaker,
					"Server authentication configuration missing[serverId,password,centralserver]");
			return;
		}

		GetMethod get = new GetMethod(himCentralServer + SVC_SRV_LOGOUT);

		long currentTimeMillis = System.currentTimeMillis();
		get.addRequestHeader("user-token", "" + currentTimeMillis);
		get.addRequestHeader("pass-token", getPassKeyToken(currentTimeMillis));
		get.addRequestHeader(JSESSIONID, sessionId);
		loginInitialized = false;
		fetchHttpResonse(get);
		
	}

	private void register() {
		if (himCentralServer == null || serverId == null || password == null) {
			loginInitialized = false;
			logger.error(logMaker,
					"Server authentication configuration missing[serverId,password,centralserver]");
			return;
		}

		PostMethod post = new PostMethod(himCentralServer + SVC_LOGIN);
		post.addParameters(new NameValuePair[] {
				new NameValuePair("userId", serverId),
				new NameValuePair("password", password),
				new NameValuePair("rememberMe", "true") });

		JSONObject result = fetchHttpResonse(post);
		if (result != null) {
			try {
				sessionId = result.getString("sid");
				loginInitialized = true;
			} catch (JSONException e) {
				loginInitialized = false;
				logger.error(logMaker, "no sessionid found for server");
			}

		} else {
			loginInitialized = false;
		}
	}

	private void markPresence(Session session, String presenceType) {
		// TODO Auto-generated method stub
		if (!loginInitialized) {
			String enabled = JiveProperties.getInstance().getProperty(
					"him.session-plugin-enabled", "false");
			if (enabled.equals("false")) {
				return;
			}
			register();
			if (!loginInitialized) {
				return;
			}
		}
		JID address = session.getAddress();
		String clientSid = null;
		try {
			User clientSessionId = UserManager.getInstance().getUser(
					address.toBareJID());
			clientSid = clientSessionId.getProperties().get(JSESSIONID);
		} catch (UserNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String fullJid = address.toFullJID();

		String serverNode = JiveProperties.getInstance().getProperty(
				"him.server-node", "localhost");
		HttpClient client = new HttpClient();

		PostMethod post = null;

		post = new PostMethod(serverId + SVC_XMPP_SESSION);

		long currentTimeMillis = System.currentTimeMillis();
		String passToken = getPassKeyToken(currentTimeMillis);
		post.addRequestHeader("user-token", "" + currentTimeMillis);
		post.addRequestHeader("pass-token", passToken);
		post.addRequestHeader(JSESSIONID, sessionId);
		post.addParameters(new NameValuePair[] {
				new NameValuePair("jidFull", session.getAddress().toFullJID()),
				new NameValuePair("userId", session.getAddress().toBareJID()),
				new NameValuePair("presenceType", presenceType),
				new NameValuePair("jsessionId", clientSid) });
		fetchHttpResonse(post);
	}

	private String getPassKeyToken(long currentTimeMillis) {
		String passKey = JiveProperties.getInstance().getProperty(
				"him.auth-secret-key", "default-him-key");
		String passToken = StringUtils.hash(passKey + ":" + currentTimeMillis,
				"MD5");
		return passToken;
	}

	private SessionEventListener sessionListener = new SessionEventListener() {

		@Override
		public void sessionCreated(Session session) {
			markPresence(session, "session-active");

		}

		@Override
		public void sessionDestroyed(Session session) {
			// TODO Auto-generated method stub
			markPresence(session, "session-destroyed");

		}

		@Override
		public void anonymousSessionCreated(Session session) {
			// TODO Auto-generated method stub

		}

		@Override
		public void anonymousSessionDestroyed(Session session) {
			// TODO Auto-generated method stub

		}

		@Override
		public void resourceBound(Session session) {
			// TODO Auto-generated method stub

		}

	};
}
