/**
 * $RCSfile$
 * $Revision: 1751 $
 * $Date: 2005-08-07 20:08:47 -0300 (Sun, 07 Aug 2005) $
 *
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.roster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.MongoDbConnectionManager;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

/**
 * Defines the provider methods required for creating, reading, updating and
 * deleting roster items.
 * <p>
 * 
 * Rosters are another user resource accessed via the user or chatbot's long ID.
 * A user/chatbot may have zero or more roster items and each roster item may
 * have zero or more groups. Each roster item is additionaly keyed on a XMPP
 * jid. In most cases, the entire roster will be read in from memory and
 * manipulated or sent to the user. However some operations will need to retrive
 * specific roster items rather than the entire roster.
 * 
 * @author Aniyus
 */
public class DefaultRosterItemProvider implements RosterItemProvider {

	private static final Logger Log = LoggerFactory
			.getLogger(DefaultRosterItemProvider.class);
	boolean initilized = false;

	private MongoClient mongoClient;
	private DB db = null;

	public DefaultRosterItemProvider() {
		// TODO Auto-generated constructor stub
		init();
	}

	private void init()  {
		try {
			db = MongoDbConnectionManager.getConnection();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jivesoftware.openfire.roster.RosterItemProvider#createItem(java.lang
	 * .String, org.jivesoftware.openfire.roster.RosterItem)
	 */
	public RosterItem createItem(String username, RosterItem item)
			throws UserAlreadyExistsException {

		try {
			init();
			item.setCurrVersion(System.currentTimeMillis());
			DBCollection coll = db.getCollection("gUser");

			BasicDBObject doc = new BasicDBObject("name", item.getNickname())
					.append("jid", item.getJid().toBareJID())
					.append("himId", item.getJid().getNode())
					.append("sub", item.getSubStatus().getValue())
					.append("ask", item.getAskStatus().getValue())
					.append("recv", item.getRecvStatus().getValue())
					.append("ver", item.getCurrVersion())
					.append("groupName", item.getGroups());

			BasicDBObject q = new BasicDBObject("himId", username);

			DBCursor res = coll.find(q, new BasicDBObject("_id", 0).append(
					"friends",
					new BasicDBObject("$elemMatch", new BasicDBObject("himId",
							item.getJid().getNode()))));
			Iterator<DBObject> iter = res.iterator();
			while (iter.hasNext()) {
				BasicDBList result = (BasicDBList) ((BasicDBObject) iter
						.next()).get("friends");
				if (result != null && result.size()>0) {
					new UserAlreadyExistsException(item.getJid().toBareJID());
				}
			}
			WriteResult writeRes = coll.update(new BasicDBObject("himId",
					username), new BasicDBObject("$push", new BasicDBObject(
					"friends", doc)).append("$inc", new BasicDBObject(
					"friendsCount", 1)));
			if (!((Boolean) writeRes.getField("updatedExisting"))) {
				new UserAlreadyExistsException(item.getJid().toBareJID());
			}

		} catch (Exception e) {
			Log.warn("Error trying to insert a new row in ofRoster", e);
			throw new UserAlreadyExistsException(item.getJid().toBareJID());
		}

		return item;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jivesoftware.openfire.roster.RosterItemProvider#updateItem(java.lang
	 * .String, org.jivesoftware.openfire.roster.RosterItem)
	 */
	public void updateItem(String username, RosterItem item)
			throws UserNotFoundException {
		// "UPDATE ofRoster SET sub=?, ask=?, recv=?, nick=?,version=? WHERE rosterID=?"
		try {
			init();
			item.setCurrVersion(System.currentTimeMillis());
			DBCollection coll = db.getCollection("gUser");
			BasicDBObject doc = new BasicDBObject("friends.$.name",
					item.getNickname())
					.append("friends.$.sub", item.getSubStatus().getValue())
					.append("friends.$.ask", item.getAskStatus().getValue())
					.append("friends.$.recv", item.getRecvStatus().getValue())
					.append("friends.$.ver", item.getCurrVersion())
					.append("friends.$.groupName", item.getGroups());

			BasicDBObject q = new BasicDBObject("himId", username).append(
					"friends.himId", item.getJid().getNode());
			WriteResult res = coll.update(q, new BasicDBObject("$set", doc));
			if (!((Boolean) res.getField("updatedExisting"))) {
				Log.warn("Unable to update roster item");

			}

		} catch (Exception e) {
			Log.warn("Error trying to insert a new row in ofRoster", e);

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jivesoftware.openfire.roster.RosterItemProvider#deleteItem(java.lang
	 * .String, long)
	 */
	public void deleteItem(String username, String friendId) {

		try {
			init();

			DBCollection coll = db.getCollection("gUser");
			BasicDBObject doc = new BasicDBObject("$pull", new BasicDBObject(
					"friends",new BasicDBObject("himId", friendId)));
			BasicDBObject q = new BasicDBObject("himId", username);
			WriteResult res = coll.update(q, doc.append("$inc", new BasicDBObject(
					"friendsCount", -1)));
			if (!((Boolean) res.getField("updatedExisting"))) {
				Log.warn("Unable to update roster item");
			}

		} catch (Exception e) {
			Log.warn("Error trying to insert a new row in ofRoster", e);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jivesoftware.openfire.roster.RosterItemProvider#getUsernames(java
	 * .lang.String)
	 */
	public Iterator<String> getUsernames(String jid) {
		List<String> answer = new ArrayList<String>();
		try {
			init();

			DBCollection coll = db.getCollection("gUser");
			BasicDBObject doc = new BasicDBObject("himId", 1);
			BasicDBObject q = new BasicDBObject("friends.jid", jid);
			DBCursor res = coll.find(q, doc);
			Iterator<DBObject> iter = res.iterator();
			while (iter.hasNext()) {
				answer.add((String) iter.next().get("himId"));
			}

		} catch (Exception e) {
			Log.warn("Error trying to insert a new row in ofRoster", e);

		}
		return answer.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jivesoftware.openfire.roster.RosterItemProvider#getItemCount(java
	 * .lang.String)
	 */
	public int getItemCount(String username) {
		int count = 0;

		try {
			init();

			DBCollection coll = db.getCollection("gUser");
			BasicDBObject doc = new BasicDBObject("friendsCount", 1);
			BasicDBObject q = new BasicDBObject("himId", username);
			DBCursor res = coll.find(q, doc);
			Iterator<DBObject> iter = res.iterator();
			while (iter.hasNext()) {
				Integer size = (Integer) iter.next().get("friendsCount");
				if (size != null)
					return size;
			}

		} catch (Exception e) {
			Log.warn("Error trying to get rows in ofRoster", e);

		}

		return count;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jivesoftware.openfire.roster.RosterItemProvider#getItems(java.lang
	 * .String)
	 */
	public Iterator<RosterItem> getItems(String username) {
		LinkedList<RosterItem> itemList = new LinkedList<RosterItem>();
		try {
			init();

			DBCollection coll = db.getCollection("gUser");
			BasicDBObject doc = new BasicDBObject("friends", 1);
			BasicDBObject q = new BasicDBObject("himId", username);
			DBCursor res = coll.find(q, doc);
			Iterator<DBObject> iter = res.iterator();
			while (iter.hasNext()) {
				BasicDBList o = (BasicDBList) iter.next().get("friends");
				for (int i = 0; i < o.size(); i++) {
					BasicDBObject dbo = (BasicDBObject) o.get(i);
					// information

					// SELECT jid, rosterID, sub, ask, recv, nick,version FROM
					// ofRoster WHERE username=?
					RosterItem item = new RosterItem(
							i + 1,
							new JID(dbo.getString("jid")),
							RosterItem.SubType.getTypeFromInt(dbo.getInt("sub")),
							RosterItem.AskType.getTypeFromInt(dbo.getInt("ask")),
							RosterItem.RecvType.getTypeFromInt(dbo
									.getInt("recv")), dbo.getString("name"),
							null);
					item.setCurrVersion(dbo.getLong("ver"));
					// Add the loaded RosterItem (ie. user contact) to the
					// result
					itemList.add(item);

				}
				System.out.println("sdsd");
			}

		} catch (Exception e) {
			Log.warn("Error trying to get rows in ofRoster", e);

		}

		return itemList.iterator();
	}

	

	@Override
	public void close() {
		try{
			if ( db != null){
				mongoClient.close();
			}
		}catch(Exception e){
			
		}
		
	}
}
