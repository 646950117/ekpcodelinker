package com.landray.plugin.codelinker.refresh;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;

import com.landray.plugin.codelinker.common.IProgressMonitorAdapter;
import com.landray.plugin.codelinker.common.ISyncAction;
import com.landray.plugin.codelinker.common.ProjectHandler;
import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.common.Utils;
import com.landray.plugin.codelinker.log.LinkerLogger;

@SuppressWarnings("unchecked")
public class RefreshManager extends Thread {

	public RefreshManager(String name) {
		super(name);
		new Thread(notifyRunnable, "refreshNotifier").start();
	}

	private Runnable notifyRunnable = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(2000);
					synchronized (changedTasks) {
						if (changedTasks.size() > 0) {
							if (isAlive()) {
								changedTasks.notify();
							}
							LinkerLogger.log("notify refresh manager", "file");
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private ConcurrentHashMap<String, RefreshTask> changedTasks = new ConcurrentHashMap<String, RefreshTask>();

	public synchronized void addTask(String taskName) {
		changedTasks.put(taskName, new RefreshTask(taskName));
		LinkerLogger.log("add refresh task:" + taskName + " in " + Thread.currentThread().getName(), "file");
	}

	public synchronized void addTask(String taskName, boolean build, String buildType) {
		changedTasks.put(taskName, new RefreshTask(taskName, build, buildType));
		LinkerLogger.log("add refresh-build task:" + taskName + " in " + Thread.currentThread().getName(), "file");
	}

	@Override
	public void run() {
		while (true) {
			synchronized (changedTasks) {
				if (changedTasks.size() == 0) {
					LinkerLogger.log("no task to refresh", "file");
					try {
						changedTasks.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				for (RefreshTask task : changedTasks.values()) {
					LinkerLogger.log("ready to refresh task:" + task.taskName, "file");
					try {
						task.refresh();
					} catch (Throwable t) {
						LinkerLogger.log(Utils.getErrMessage(t), "file");
					}
				}
			}
		}
	}

	class RefreshTask {
		public RefreshTask(String taskName) {
			this(taskName, false, "");
		}

		public RefreshTask(String taskName, boolean build, String buildType) {
			this.taskName = taskName;
			this.build = build;
			this.buildType = buildType;
		}

		private String taskName = "";
		private boolean build = false;
		private String buildType = "";

		public void refresh() throws Throwable {
			LinkedList<ProjectHandler> follow = new LinkedList<ProjectHandler>();
			ProjectHandler first = null;
			if ("allekp".equals(taskName)) {
				Map<String, Object> coreModuleInfos = ProjectUtils.validEkpModuleNames.get("CORE");
				Map<String, IProject> ekpProjectsMap = (Map<String, IProject>) coreModuleInfos.get("ekpMap");
				if (ekpProjectsMap != null) {
					for (IProject ekp : ekpProjectsMap.values()) {
						follow.addLast(new ProjectHandler("refresh", ekp));
					}
					first = follow.poll();
				}
			} else {
				Map<String, Object> ekpModuleInfos = ProjectUtils.allProjectsMap.get(taskName);
				first = new ProjectHandler("refresh", (IProject) ekpModuleInfos.get("project"));
			}
			if (first != null) {
				ISyncAction afterRefreshed = new ISyncAction() {
					@Override
					public void doAction() {
						changedTasks.remove(taskName);
					}
				};
				IProgressMonitorAdapter progressMonitor = new RefreshMonitor(System.currentTimeMillis(), first, follow,
						build, buildType, afterRefreshed);
				first.project.refreshLocal(IProject.DEPTH_INFINITE, progressMonitor);
				if (!progressMonitor.isDone()) {
					progressMonitor.done();
				}
			} else {
				changedTasks.remove(taskName);
			}
		}
	}
}