package com.anh.openfire.plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.Blowfish;

public class MessageNotificationServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
		String error=null;
        PrintWriter out = response.getWriter();
        String messageFrom=request.getParameter("messageFrom");
        String messageTo = request.getParameter("messageTo");
        String fileUrl = request.getParameter("fileUrl");
        String token = request.getParameter("token");
        if(messageFrom!=null&& messageTo!=null&& fileUrl!=null && token!=null){
        String decryptedCipher =new Blowfish("him-connect").decryptString(token);
        try{
			 Date original= new SimpleDateFormat().parse(decryptedCipher);
			 Date now = new Date();
			 Date estNow =new Date(now.getTime()-TimeUnit.MINUTES.toMillis((long)2));
			 if(estNow.after(original)&& estNow.before(now)){
				 FileShareNotificationPlugin plugin=(FileShareNotificationPlugin)XMPPServer.getInstance().getPluginManager().getPlugin("filesharemessage");
			      plugin.sendMessage(messageFrom,messageTo,fileUrl);
			 }else{
				 error= new String("Error in code");
			 }
			}catch (Exception e) {
				error= new String("Incorrect Cipher");
			}        
        FileShareNotificationPlugin plugin=(FileShareNotificationPlugin)XMPPServer.getInstance().getPluginManager().getPlugin("filesharemessage");
        plugin.sendMessage(messageFrom,messageTo,fileUrl);
        }else{
        	out.println("not found");
        }
        out.close();
    }


	
}
