/**
 * $RCSfile: DefaultVCardProvider.java,v $
 * $Revision: 3062 $
 * $Date: 2005-11-11 13:26:30 -0300 (Fri, 11 Nov 2005) $
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

package org.jivesoftware.openfire.vcard;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.SAXReader;
import org.jivesoftware.database.MongoDbConnectionManager;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.Base64;
import org.jivesoftware.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


/**
 * Default implementation of the VCardProvider interface, which reads and writes
 * data from the <tt>ofVCard</tt> database table.
 * 
 * @author Gaston Dombiak
 */
public class DefaultVCardProvider implements VCardProvider {

	private static final Logger Log = LoggerFactory
			.getLogger(DefaultVCardProvider.class);

	private static final int POOL_SIZE = 10;
	/**
	 * Pool of SAX Readers. SAXReader is not thread safe so we need to have a
	 * pool of readers.
	 */
	private BlockingQueue<SAXReader> xmlReaders = new LinkedBlockingQueue<SAXReader>(
			POOL_SIZE);
	private DB db;

	public DefaultVCardProvider() {
		super();
		// Initialize the pool of sax readers
		try {
			db = MongoDbConnectionManager.getConnection();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < POOL_SIZE; i++) {
			SAXReader xmlReader = new SAXReader();
			xmlReader.setEncoding("UTF-8");
			xmlReaders.add(xmlReader);
		}
	}

	public Element loadVCard(String username) {

		synchronized (username.intern()) {
			if (db == null) {
				db = MongoDbConnectionManager.reconnect();
			}
			if (db == null)
				return null;
			
			Element vCardElement = null;
			
			
			SAXReader xmlReader = null;
			try {
				
				DBCollection coll = db.getCollection("gUser");
				BasicDBObject doc = new BasicDBObject("photoId", 1).append("vcard", 1);
			
				BasicDBObject q = new BasicDBObject("himId", username);
				DBObject res = coll.findOne(q, doc);
				
				if (res != null) {
					BasicDBObject photo = (BasicDBObject) res.get("photoId");
					BasicDBObject vcardXml = (BasicDBObject) res.get("vcard");
					if(vcardXml!=null){
						vCardElement =new DOMElement("vCard", new Namespace(null,"vcard-temp-ext"));
						String url = (String) vcardXml.getString("url");
						Element urlE = new DOMElement("url");
						Element type = new DOMElement("type");
						type.setText((String) vcardXml.getString("type"));
						urlE.setText(url);
						vCardElement.add(urlE);
					}
					else if (photo != null) {
						Element photoEl = new DOMElement("PHOTO");
						Element bin = new DOMElement("BINVAL");
						Element type = new DOMElement("type");
						vCardElement =new DOMElement("vCard", new Namespace(null,"vcard-temp"));
						String url = (String) photo.getString("url");
						type.setText(photo.getString("contentType"));
						HttpClient client = new HttpClient();
						GetMethod method = new GetMethod(url);
						client.executeMethod(method);
						if(method.getStatusCode()==HttpStatus.SC_OK){
							InputStream ios= method.getResponseBodyAsStream();
							byte[] data = new byte[2048];
							int len =0;
							StringBuilder builder = new StringBuilder();
							while((len= ios.read(data))!=-1){
							     builder.append(Base64.encodeBytes(data, 0, len, Base64.NO_OPTIONS));
							}
							bin.setText(builder.toString());
							ios.close();
						}
						photoEl.add(type);
						photoEl.add(bin);
						vCardElement.add(photoEl);
					}

				}
			

			} catch (Exception e) {
				Log.error("Error loading vCard of username: " + username, e);
			} finally {
				
			}
			return vCardElement;
		}
	}

	public Element createVCard(String username, Element vCardElement)
			throws AlreadyExistsException {
		return vCardElement;
	}

	public Element updateVCard(String username, Element vCardElement)
			throws NotFoundException {
		return vCardElement;
	}

	public void deleteVCard(String username) {
		
	}

	public boolean isReadOnly() {
		return false;
	}
}
