package com.landray.plugin.codelinker.common;

import org.eclipse.core.resources.IProject;

public class ProjectHandler {

	public ProjectHandler(String handleType, IProject project) {
		this.handleType = handleType;
		this.project = project;
	}

	public String handleType = "";
	public IProject project = null;
	public boolean builded = false;
}