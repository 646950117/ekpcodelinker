package com.landray.plugin.codelinker.common;

import com.landray.plugin.codelinker.load.LoaderManager;
import com.landray.plugin.codelinker.refresh.RefreshManager;

public class ManagerFactory {
	private static RefreshManager refresher = null;
	private static LoaderManager loader = null;

	public static RefreshManager getRereshManager() {
		if (refresher == null) {
			refresher = new RefreshManager("refresher");
			refresher.start();
		}
		return refresher;
	}

	public static LoaderManager getLoaderManager() {
		if (loader == null) {
			loader = new LoaderManager("loader");
			loader.start();
		}
		return loader;
	}
}