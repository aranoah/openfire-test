package com.anh.openfire.plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.openfire.XMPPServer;

public class MessageNotificationServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
		
        PrintWriter out = response.getWriter();
        if(request.getParameter("test")!=null){
        out.println(request.getParameter("test"));
        FileShareNotificationPlugin plugin=(FileShareNotificationPlugin)XMPPServer.getInstance().getPluginManager().getPlugin("filesharemessage");
        out.println(plugin);
        out.println("Sending notification");
        plugin.sendMessage();
        }else{
        	out.println("not found");
        }
        out.close();
    }


	
}
