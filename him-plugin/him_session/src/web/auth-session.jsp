<%@ page import="java.util.*,
                 java.net.URLEncoder,
                 org.jivesoftware.util.*,
                 org.jivesoftware.openfire.PresenceManager,
                 org.jivesoftware.openfire.user.*,
                 org.jivesoftware.openfire.XMPPServer,
                 org.xmpp.packet.JID,
                 com.anh.him.auth.HimSessionPlugin"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<html>
    <head>
        <title><fmt:message key="advance.user.search.title" /></title>
        <meta name="pageID" content="advance-user-search"/>
    </head>
    <body>

<%
  String action = request.getParameter("action");
  if(action==null){
  }else if(action.equals("restart")){
    HimSessionPlugin.restart();
  }else if(action.equals("stop")){
	  HimSessionPlugin.stop();
  }
  %>


   
    <div class="jive-contentBoxHeader"><fmt:message key="advance.user.search.search_user" /></div>
    <div class="jive-contentBox">
        <table cellpadding="3" cellspacing="1" border="0" width="600">
        <tr class="c1">
            <td width="50%" colspan="2" nowrap>
              <a href="auth-session.jsp?action=restart">Restart Session Manager</a>
            </td>
        </tr>
        <tr class="c1">
            <td width="50%" colspan="2" nowrap>
              <a href="auth-session.jsp?action=stop">Stop Session Manager</a>
            </td>
        </tr>
        </table>
      </div></div>
        
</body>
</html>
