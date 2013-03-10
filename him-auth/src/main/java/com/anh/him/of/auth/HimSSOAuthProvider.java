package com.anh.him.of.auth;

import org.jivesoftware.openfire.auth.AuthProvider;
import org.jivesoftware.openfire.auth.ConnectionException;
import org.jivesoftware.openfire.auth.InternalUnauthenticatedException;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveProperties;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HimSSOAuthProvider implements AuthProvider {

	private static Logger log = LoggerFactory
			.getLogger(HimSSOAuthProvider.class);
	private String authSecurityKey = JiveProperties.getInstance().getProperty(
			"him.auth-secret-key", "default-him-key");

	/**
	 * @return the authSecurityKey
	 */
	public String getAuthSecurityKey() {
		return authSecurityKey;
	}

	/**
	 * @param authSecurityKey
	 *            the authSecurityKey to set
	 */
	public void setAuthSecurityKey(String authSecurityKey) {
		this.authSecurityKey = authSecurityKey;
	}

	public boolean isPlainSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isDigestSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	public void authenticate(String username, String password)
			throws UnauthorizedException, ConnectionException,
			InternalUnauthenticatedException {
		// TODO Auto-generated method stub
		if (password == null || password.length() == 0)
			throw new UnauthorizedException(
					"Invalid session, please relogin to auth server");
		String[] tokens = password.split(":");
		if (tokens.length != 3) {
			throw new UnauthorizedException(
					"Invalid session, please relogin to auth server");
		}
		// 0-md5(sessionid:timestamp:password) 1: server token
		authSecurityKey = JiveProperties.getInstance().getProperty(
				"him.auth-secret-key", "default-him-key");
		String sessionPasswd = StringUtils.hash(username + ":" + tokens[0]
				+ ":" + tokens[1] + ":" + authSecurityKey, "MD5");
		if (!sessionPasswd.equals(tokens[2])) {
			throw new UnauthorizedException(
					"Invalid session, please relogin to auth server");
		}
		createUser(username, tokens[1]);

	}

	public void authenticate(String username, String token, String digest)
			throws UnauthorizedException, ConnectionException,
			InternalUnauthenticatedException {
		// TODO Auto-generated method stub

	}

	public String getPassword(String username) throws UserNotFoundException,
			UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setPassword(String username, String password)
			throws UserNotFoundException, UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	public boolean supportsPasswordRetrieval() {
		// TODO Auto-generated method stub
		return false;
	}

	private static void createUser(String username, String sid) {
		// See if the user exists in the database. If not, automatically create
		// them.
		UserManager userManager = UserManager.getInstance();
		User user = null;
		try {
			user = userManager.getUser(username);
			user.getProperties().put("JSESSIONID", sid);
		} catch (UserNotFoundException unfe) {
			try {
				log.debug("JDBCAuthProvider: Automatically creating new user account for "
						+ username);
				user = UserManager.getUserProvider().createUser(username,
						StringUtils.randomString(8), null, null);
				user.getProperties().put("JSESSIONID", sid);
			} catch (UserAlreadyExistsException uaee) {
				try {
					user = userManager.getUser(username);
					user.getProperties().put("JSESSIONID", sid);
				} catch (UserNotFoundException e) {
					log.debug("JDBCAuthProvider: User not found exception to cache "
							+ username);
				
				}

			}
		}
	}

}
