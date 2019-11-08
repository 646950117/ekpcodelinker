package com.landray.plugin.codelinker.common;

public interface IAsyncAction {
	public void doAction() throws Throwable;

	public String getActionName();
}