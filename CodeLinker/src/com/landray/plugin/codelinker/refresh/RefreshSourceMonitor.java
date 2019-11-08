package com.landray.plugin.codelinker.refresh;

import java.util.LinkedList;

import com.landray.plugin.codelinker.common.ISyncAction;
import com.landray.plugin.codelinker.common.ProjectHandler;

public class RefreshSourceMonitor extends RefreshMonitor {

	public RefreshSourceMonitor(long startT, ProjectHandler first, LinkedList<ProjectHandler> follow,
			ISyncAction afterAction) {
		super(startT, first, follow, afterAction);
	}

	@Override
	protected String getHintType() {
		return "refresh_source";
	}
}