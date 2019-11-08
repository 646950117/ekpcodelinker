package com.landray.plugin.codelinker.build;

import java.util.LinkedList;

import com.landray.plugin.codelinker.common.ISyncAction;
import com.landray.plugin.codelinker.common.ProjectHandler;
import com.landray.plugin.codelinker.common.ProjectMonitorAdapter;

public class BuildMonitor extends ProjectMonitorAdapter {

	public BuildMonitor(long startTime, ProjectHandler first, LinkedList<ProjectHandler> follow,
			ISyncAction afterAction) {
		super(startTime, first, follow, afterAction);
	}

	@Override
	protected String getHintType() {
		return "build";
	}

	@Override
	protected void childDone(ISyncAction parentAction) {
		trgger.builded = true;
		super.childDone(parentAction);
	}
}