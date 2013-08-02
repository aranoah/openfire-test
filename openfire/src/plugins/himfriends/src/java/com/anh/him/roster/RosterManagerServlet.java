package com.anh.him.roster;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.openfire.RoutingTable;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.roster.RosterItem;
import org.jivesoftware.openfire.roster.RosterItem.AskType;
import org.jivesoftware.openfire.roster.RosterItem.RecvType;
import org.jivesoftware.openfire.roster.RosterItem.SubType;
import org.jivesoftware.openfire.roster.RosterItemProvider;
import org.jivesoftware.openfire.roster.RosterManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.Blowfish;
import org.json.JSONObject;
import org.xmpp.packet.JID;

public class RosterManagerServlet extends HttpServlet {
	RosterManagerPlugin plugin = null;
	SessionManager sessionManager = null;

	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);
		plugin = (RosterManagerPlugin) XMPPServer.getInstance()
				.getPluginManager().getPlugin("himfriends");
		sessionManager = (SessionManager) XMPPServer.getInstance()
				.getSessionManager();
	}

	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject error = new JSONObject();

		PrintWriter out = response.getWriter();
		String username = request.getParameter("himId");
		String friendId = request.getParameter("friendId");
		String friendName = request.getParameter("friendName");
		String myName = request.getParameter("himName");
		String token = request.getParameter("token");
		String operation = request.getParameter("action");

		String passwd = plugin.getPassworkKey();
		try {
			error.put("status", 0);
			error.put("message", "OK");
			if (operation != null && username != null && token != null) {
				String decryptedCipher = new Blowfish(passwd)
						.decryptString(token);
				JID fjid = new JID(friendId);
				JID ujid = new JID(username);
				Date original = new Date();// new
											// SimpleDateFormat("mm/dd/yyyy").parse(decryptedCipher);
				Date now = new Date();
				Date estNow = new Date(now.getTime()
						- TimeUnit.MINUTES.toMillis((long) 2));
				if (true || estNow.after(original) && estNow.before(now)) {
					RosterManager roster = XMPPServer.getInstance()
							.getRosterManager();
					RoutingTable routing = XMPPServer.getInstance()
							.getRoutingTable();

					RosterItemProvider rosterItemProv = roster
							.getRosterItemProvider();
					if (rosterItemProv != null) {
						if (operation.equalsIgnoreCase("add")) {

							RosterItem item = new RosterItem(fjid,
									SubType.getTypeFromInt(3),
									AskType.getTypeFromInt(-1),
									RecvType.getTypeFromInt(-1), friendName,
									null);
							rosterItemProv.createItem(ujid.getNode(), item);
							roster.getRoster(ujid.getNode()).updateRosterItem(
									item);

							item = new RosterItem(ujid,
									SubType.getTypeFromInt(3),
									AskType.getTypeFromInt(-1),
									RecvType.getTypeFromInt(-1), myName, null);
							rosterItemProv.createItem(fjid.getNode(), item);
							roster.getRoster(fjid.getNode()).updateRosterItem(
									item);
							Collection<ClientSession> ujidSession = sessionManager
									.getSessions(ujid.getNode());
							Collection<ClientSession> fjidSession = sessionManager
									.getSessions(fjid.getNode());
							if (ujidSession.size() > 0) {
								sessionManager.userBroadcast(fjid.getNode(),
										ujidSession.iterator().next()
												.getPresence());
							}
							if (fjidSession.size() > 0)
								sessionManager.userBroadcast(ujid.getNode(),
										fjidSession.iterator().next()
												.getPresence());

						} else if (operation.equalsIgnoreCase("delete")) {
							roster.getRoster(ujid.getNode()).deleteRosterItem(
									new JID(friendId), true);
							roster.getRoster(fjid.getNode()).deleteRosterItem(
									new JID(friendId), true);
						} else if (operation.equalsIgnoreCase("update")) {
							RosterItem item = roster.getRoster(ujid.getNode())
									.getRosterItem(new JID(friendId));
							item.setNickname(friendName);
							roster.getRoster(ujid.getNode()).updateRosterItem(
									item);

						}
					} else {
						error.put("status", 404);
						error.put("message", "User not found");
					}

				} else {
					error.put("status", 401);
					error.put("message", "Not Authorized");
				}

			} else {
				error.put("status", 404);
				error.put("message", "User not found");
			}
		} catch (Exception e) {

		}
		out.print(error.toString());
	}

}
