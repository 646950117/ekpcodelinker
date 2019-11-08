package com.landray.plugin.codelinker.common;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IProgressMonitorAdapter extends IProgressMonitor {
	public boolean isDone();
}