package com.landray.plugin.codelinker.refresh;

import java.util.LinkedList;

import com.landray.plugin.codelinker.common.ISyncAction;
import com.landray.plugin.codelinker.common.ProjectHandler;
import com.landray.plugin.codelinker.common.ProjectMonitorAdapter;

public class RefreshMonitor extends ProjectMonitorAdapter {
	public RefreshMonitor(long startT, ProjectHandler first, LinkedList<ProjectHandler> follow,
			ISyncAction afterAction) {
		this(startT, first, follow, false, "", afterAction);
	}

	public RefreshMonitor(long startT, ProjectHandler first, LinkedList<ProjectHandler> follow, boolean build,
			String buildType, ISyncAction afterAction) {
		super(startT, first, follow, build, buildType, afterAction);
	}

	public RefreshMonitor(long startT, ProjectHandler first, LinkedList<ProjectHandler> follow) {
		this(startT, first, follow, null);
	}

	@Override
	protected String getHintType() {
		return "refresh";
	}
}