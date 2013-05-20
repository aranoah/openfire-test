
package com.anh.him.of.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.TaskEngine;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import org.jivesoftware.openfire.auth.*;
/**
 * Default AuthProvider implementation. It authenticates against the
 * <tt>ofUser</tt> database table and supports plain text and digest
 * authentication.
 * 
 * Because each call to authenticate() makes a database connection, the results
 * of authentication should be cached whenever possible.
 * 
 * @author Matt Tucker
 */
public class HimAuthProvider implements AuthProvider {

	private TaskEngine taskManager = TaskEngine.getInstance();

	private String serverId;

	private String password;

	private String himCentralServer;

	private static final String LOAD_PASSWORD = "SELECT plainPassword,encryptedPassword FROM ofUser WHERE username=?";
	private static final String UPDATE_PASSWORD = "UPDATE ofUser SET plainPassword=?, encryptedPassword=? WHERE username=?";

	protected static String SVC_SRV_SESSION_VALIDATE = "/svc/user/validate-session";
	private static Logger logger = LoggerFactory
			.getLogger(HimAuthProvider.class);
	private static Marker logMaker = MarkerFactory.getMarker("HereIam");

	/**
	 * Constructs a new DefaultAuthProvider.
	 */
	public HimAuthProvider() {
		serverId = JiveProperties.getInstance().getProperty(
				"him.auth-server-id", "aniyus");
		password = JiveProperties.getInstance().getProperty(
				"him.auth-server-password", "welcome");
		himCentralServer = JiveProperties.getInstance().getProperty(
				"him.central-host", "http://localhost:8080");
		SVC_SRV_SESSION_VALIDATE = JiveProperties.getInstance().getProperty(
				"him.session-validate-url", SVC_SRV_SESSION_VALIDATE);
	}

	public void authenticate(String username, String password)
			throws UnauthorizedException {
		authenticate(username, password, null);
		// Got this far, so the user must be authorized.
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

	private boolean validateSession(final String uid, final String session) {
		if (himCentralServer == null || serverId == null || password == null) {
			logger.error(logMaker,
					"Server authentication configuration missing[serverId,password,centralserver]");
			return false;
		}
		PostMethod post = new PostMethod(himCentralServer
				+ SVC_SRV_SESSION_VALIDATE);
		long currentTimeMillis = System.currentTimeMillis();
		post.addParameter("userId", uid);
		post.addParameter("sid", session);
		return fetchHttpResonse(post) != null ? true : false;

	}

	public void authenticate(String username, String password, String digest)
			throws UnauthorizedException {
		if (username == null || password == null ) {
			throw new UnauthorizedException();
		}

		int pos;
		username = username.trim().toLowerCase();
		password = ((pos = password.indexOf("sid-")) != -1) ? password
				.substring(pos + 4) : password;
		if (username.contains("@")) {
			// Check that the specified domain matches the server's domain
			int index = username.indexOf("@");
			String domain = username.substring(index + 1);
			if (domain.equals(XMPPServer.getInstance().getServerInfo()
					.getXMPPDomain())) {
				username = username.substring(0, index);
			} else if (domain.indexOf(".hereiamconnect.com") != -1) {
				username = username.substring(0, index);
			} else {
				// Unknown domain. Return authentication failed.
				throw new UnauthorizedException();
			}
		}
		try {
			if (pos != -1 && !validateSession(username, password)) {
				throw new UnauthorizedException();
			} else if (pos == -1) {
				String passwordB = getPassword(username);
				if (digest==null && !passwordB.equals(password)) {
					throw new UnauthorizedException();
				}
				
				if (digest != null) {
					/****
					 * now password is considered to be token
					 */
					String anticipatedDigest = AuthFactory.createDigest(
							password, passwordB);
					if (!digest.equalsIgnoreCase(anticipatedDigest)) {
						throw new UnauthorizedException();
					}
				}
			}

		} catch (Exception unfe) {
			throw new UnauthorizedException();
		}
		// Got this far, so the user must be authorized.
	}

	public boolean isPlainSupported() {
		return true;
	}

	public boolean isDigestSupported() {
		return true;
	}

	public String getPassword(String username) throws UserNotFoundException {
		if (!supportsPasswordRetrieval()) {
			// Reject the operation since the provider is read-only
			throw new UnsupportedOperationException();
		}
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		if (username.contains("@")) {
			// Check that the specified domain matches the server's domain
			int index = username.indexOf("@");
			String domain = username.substring(index + 1);
			if (domain.equals(XMPPServer.getInstance().getServerInfo()
					.getXMPPDomain())) {
				username = username.substring(0, index);
			} else {
				// Unknown domain.
				throw new UserNotFoundException();
			}
		}
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(LOAD_PASSWORD);
			pstmt.setString(1, username);
			rs = pstmt.executeQuery();
			if (!rs.next()) {
				throw new UserNotFoundException(username);
			}
			String plainText = rs.getString(1);
			String encrypted = rs.getString(2);
			if (encrypted != null) {
				try {
					return AuthFactory.decryptPassword(encrypted);
				} catch (UnsupportedOperationException uoe) {
					// Ignore and return plain password instead.
				}
			}
			return plainText;
		} catch (SQLException sqle) {
			throw new UserNotFoundException(sqle);
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
	}

	public void setPassword(String username, String password)
			throws UserNotFoundException {
		// Determine if the password should be stored as plain text or
		// encrypted.
		boolean usePlainPassword = JiveGlobals
				.getBooleanProperty("user.usePlainPassword");
		String encryptedPassword = null;
		if (username.contains("@")) {
			// Check that the specified domain matches the server's domain
			int index = username.indexOf("@");
			String domain = username.substring(index + 1);
			if (domain.equals(XMPPServer.getInstance().getServerInfo()
					.getXMPPDomain())) {
				username = username.substring(0, index);
			} else {
				// Unknown domain.
				throw new UserNotFoundException();
			}
		}
		if (!usePlainPassword) {
			try {
				encryptedPassword = AuthFactory.encryptPassword(password);
				// Set password to null so that it's inserted that way.
				password = null;
			} catch (UnsupportedOperationException uoe) {
				// Encryption may fail. In that case, ignore the error and
				// the plain password will be stored.
			}
		}

		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(UPDATE_PASSWORD);
			if (password == null) {
				pstmt.setNull(1, Types.VARCHAR);
			} else {
				pstmt.setString(1, password);
			}
			if (encryptedPassword == null) {
				pstmt.setNull(2, Types.VARCHAR);
			} else {
				pstmt.setString(2, encryptedPassword);
			}
			pstmt.setString(3, username);
			pstmt.executeUpdate();
		} catch (SQLException sqle) {
			throw new UserNotFoundException(sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
	}

	public boolean supportsPasswordRetrieval() {
		return true;
	}
}
