package com.landray.plugin.codelinker.load;

import java.util.concurrent.ConcurrentHashMap;

import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.common.Utils;
import com.landray.plugin.codelinker.log.LinkerLogger;

public class LoaderManager extends Thread {
	public LoaderManager(String name) {
		super(name);
		new Thread(notifyRunnable, "loaderNotifier").start();
	}

	private ConcurrentHashMap<String, LoadTask> loadTasks = new ConcurrentHashMap<String, LoadTask>();

	private Runnable notifyRunnable = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(2000);
					synchronized (loadTasks) {
						if (loadTasks.size() > 0) {
							if (isAlive()) {
								loadTasks.notify();
							}
							LinkerLogger.log("notify loader", "file");
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

	public synchronized void addTask(String taskName) {
		loadTasks.put(taskName, new LoadTask(taskName));
		LinkerLogger.log("add load task:" + taskName + " in " + Thread.currentThread().getName(), "file");
	}

	@Override
	public void run() {
		while (true) {
			synchronized (loadTasks) {
				if (loadTasks.size() == 0) {
					LinkerLogger.log("no task to load", "file");
					try {
						loadTasks.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				for (LoadTask task : loadTasks.values()) {
					LinkerLogger.log("ready to load task:" + task.taskName, "file");
					try {
						task.load();
					} catch (Throwable t) {
						LinkerLogger.log(Utils.getErrMessage(t), "file");
					}
				}
			}
		}
	}

	class LoadTask {
		public LoadTask(String taskName) {
			this.taskName = taskName;
		}

		private String taskName = "";

		public void load() throws Throwable {
			try {
				ProjectUtils.loadProjects();
				loadTasks.remove(taskName);
			} catch (Throwable e) {
				// 吃掉
			}
		}
	}
}