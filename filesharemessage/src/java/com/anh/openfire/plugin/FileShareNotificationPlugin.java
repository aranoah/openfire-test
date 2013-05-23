package com.anh.openfire.plugin;

import java.io.File;
import java.util.TimerTask;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.util.TaskEngine;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;


public class FileShareNotificationPlugin implements Plugin {
   

	   private JID serverAddress;
	   private MessageRouter router;
	   
	   public void initializePlugin(PluginManager manager, File pluginDirectory) {
		      AuthCheckFilter.addExclude("filesharemessage/notify");
		      serverAddress = new JID(XMPPServer.getInstance().getServerInfo().getXMPPDomain());
		      router = XMPPServer.getInstance().getMessageRouter();
		   }

	   
	   public void destroyPlugin() {
		     AuthCheckFilter.removeExclude("filesharemessage/notify");
		      serverAddress = null;
		      router = null;
		   }
	   
	   public void sendMessage(String messageFrom,String messgeTo,String fileUrl){
		   final Message message = new Message();
		   message.setTo(messgeTo);
		   message.setFrom(messageFrom);
		   message.setSubject("notification");
           message.setBody(fileUrl);
           message.setType(Type.chat);
           TimerTask messageTask = new TimerTask() {
               @Override
			public void run() {
                  router.route(message);
               }
            };

            TaskEngine.getInstance().schedule(messageTask, 5);
		   
	   }

}
