package com.landray.plugin.codelinker;

import org.eclipse.ui.IStartup;

import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.common.Utils;
import com.landray.plugin.codelinker.listener.ListenersProvider;
import com.landray.plugin.codelinker.log.LinkerLogger;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		try {
			// 插件启动时加载工作空间项目信息，执行一次
			ProjectUtils.loadProjects();
			ProjectUtils.loadSavedModules();
			ListenersProvider.addListeners();
		} catch (Throwable t) {
			LinkerLogger.log(Utils.getErrMessage(t), "file");
		}
	}
}