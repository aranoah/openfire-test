/**
 * $RCSfile$
 * $Revision: 11696 $
 * $Date: 2010-05-13 06:33:23 -0500 (Thu, 13 May 2010) $
 *
 * Copyright (C) 2004-2008 Jive Software. All rights reserved.
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

package org.jivesoftware.database;

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class MongoDbConnectionManager {

	private static final Logger Log = LoggerFactory
			.getLogger(MongoDbConnectionManager.class);
	private static final Object providerLock = new Object();

	private static MongoClient mongoClient;
	private static DB db = null;

	private static boolean initilized;

	private static void init() {

		String hostname = JiveGlobals.getXMLProperty("him.central.dbhost",
				"localhost");
		int port = JiveGlobals.getXMLProperty("him.central.dbport", 27017);
		String login = JiveGlobals.getXMLProperty("him.central.dblogin", null);
		String passwd = JiveGlobals.getXMLProperty("him.central.dbpassword",
				null);
		String dbSchema = JiveGlobals.getXMLProperty("him.central.dbschema",
				"hereiam");
		try {
			mongoClient = new MongoClient(hostname, port);
			db = mongoClient.getDB(dbSchema);
			if (login != null && !db.authenticate(login, passwd.toCharArray())) {
				throw new Exception("invalid credential");
			}
			initilized = true;
		} catch (Exception e) {
			e.printStackTrace();
			mongoClient = null;

		}

	}

	public static DB getConnection() throws Exception {
		if (!initilized || mongoClient == null) {
			synchronized (providerLock) {
				if (mongoClient == null) {
					// Attempt to load the connection provider classname as
					// a Jive property.
					init();
				}
			}
		}
		return db;
	}

	public static synchronized DB reconnect() {
		if (mongoClient != null) {
			mongoClient.close();
		}

		init();
		return db;

	}

}