package com.landray.plugin.codelinker.common;

import com.landray.plugin.codelinker.listener.ListenersProvider;
import com.landray.plugin.codelinker.log.LinkerLogger;

public abstract class ProgressMonitorAdapter implements IProgressMonitorAdapter {

	public ProgressMonitorAdapter(long startTime) {
		this.startTime = startTime;
	}

	protected long startTime = 0l;
	private boolean done = false;

	@Override
	public void beginTask(String arg0, int arg1) {
		LinkerLogger.log(getStartHint(), "file");
		ListenersProvider.removeListener(ListenersProvider.RESOURCE_CHANGE);
	}

	protected abstract String getStartHint();

	protected abstract String getEndHint();

	protected abstract void childDone(ISyncAction action);

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public void done() {
		done = true;
		LinkerLogger.log(getEndHint(), "file");
		childDone(new ISyncAction() {
			@Override
			public void doAction() {
				ListenersProvider.addListener(ListenersProvider.RESOURCE_CHANGE);
			}
		});
	}

	@Override
	public void internalWorked(double arg0) {

	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void setCanceled(boolean arg0) {

	}

	@Override
	public void setTaskName(String arg0) {

	}

	@Override
	public void subTask(String arg0) {

	}

	@Override
	public void worked(int arg0) {

	}
}