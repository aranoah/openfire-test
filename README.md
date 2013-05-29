him-openfire
============

openfire plugin development


configuration property<br>
  xmpp.httpbind.client.requests.ignoreOveractivity = true <br>
  provider.auth.className = com.anh.him.of.auth.HimAuthProvider<br>
  xmpp.domain = hereiamconnect.com <br>
  passwordKey = him-connect     [ default ]<br>
  log.httpbind.enabled = true    [ if httpbinding operation /activity logs you want to see]
  him.central-host = http://hereiamconnect.com  [ default: localhost:8080]
Library & plugins:
  1. copy auth-provider-0.0.1-SNAPSHOT.jar to OpenFireHome/lib/
  2. install him file sharing plugin
  3. install presence plugin
  
  
