package com.landray.plugin.codelinker.common;

import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

import com.landray.plugin.codelinker.build.BuildMonitor;
import com.landray.plugin.codelinker.refresh.RefreshMonitor;

public abstract class ProjectMonitorAdapter extends ProgressMonitorAdapter {

	public ProjectMonitorAdapter(long startTime, ProjectHandler first, LinkedList<ProjectHandler> follow,
			ISyncAction afterAction) {
		this(startTime, first, follow, false, "", afterAction);
	}

	public ProjectMonitorAdapter(long startTime, ProjectHandler first, LinkedList<ProjectHandler> follow, boolean build,
			String buildType, ISyncAction afterAction) {
		super(startTime);
		this.trgger = first;
		this.follow = follow;
		this.build = build;
		this.buildType = buildType;
		this.afterAction = afterAction;
	}

	protected ProjectHandler trgger = null;
	protected LinkedList<ProjectHandler> follow = null;
	protected ISyncAction afterAction = null;
	protected boolean build = false;
	protected String buildType = "";

	protected abstract String getHintType();

	@Override
	protected String getStartHint() {
		return MessageUtils.getParamMessage("common." + getHintType() + ".start", trgger.project.getName());
	}

	@Override
	protected String getEndHint() {
		return MessageUtils.getParamMessage("common." + getHintType() + ".end", trgger.project.getName())
				+ Utils.calcTime(startTime, System.currentTimeMillis());
	}

	@Override
	protected void childDone(ISyncAction parentAction) {
		if (build && !trgger.builded) {
			trgger.handleType = "build";
			follow.addFirst(trgger);
		}
		ProjectHandler handler = follow.poll();
		if (handler != null) {
			IProgressMonitorAdapter progressMonitor = null;
			try {
				if ("build".equals(handler.handleType)) {
					int bt = "full".equals(buildType) ? IncrementalProjectBuilder.FULL_BUILD
							: IncrementalProjectBuilder.INCREMENTAL_BUILD;
					progressMonitor = new BuildMonitor(System.currentTimeMillis(), handler, follow, afterAction);
					handler.project.build(bt, progressMonitor);
					if (!progressMonitor.isDone()) {
						progressMonitor.done();
					}
				} else if ("refresh".equals(handler.handleType)) {
					progressMonitor = new RefreshMonitor(System.currentTimeMillis(), handler, follow, afterAction);
					handler.project.refreshLocal(IProject.DEPTH_INFINITE, progressMonitor);
					if (!progressMonitor.isDone()) {
						progressMonitor.done();
					}
				}
			} catch (CoreException e) {
				//
			}
		} else {
			if (afterAction != null) {
				afterAction.doAction();
			}
			if (parentAction != null) {
				parentAction.doAction();
			}
		}
	}
}
