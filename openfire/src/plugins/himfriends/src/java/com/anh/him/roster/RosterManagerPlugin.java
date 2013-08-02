package com.anh.him.roster;

import java.io.File;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;

public class RosterManagerPlugin implements Plugin {

	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		// TODO Auto-generated method stub
		 AuthCheckFilter.addExclude("himfriends/roster");
	}

	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		 AuthCheckFilter.removeExclude("himfriends/roster");
	}

	public String getPassworkKey() {
		return JiveGlobals.getProperty("passwordKey", "him-connect");
	}
}
