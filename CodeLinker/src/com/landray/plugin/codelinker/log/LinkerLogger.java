package com.landray.plugin.codelinker.log;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.landray.plugin.codelinker.Activator;

public class LinkerLogger implements IConsoleFactory {
	private static MessageConsole console = new MessageConsole("CodeLinker", null);
	private static ILog fileLog = null;
	private static boolean exists = false;

	public static void setFileLog(ILog fileLog) {
		LinkerLogger.fileLog = fileLog;
	}

	@Override
	public void openConsole() {
		showConsole();
	}

	private static void showConsole() {
		if (console != null) {
			// 得到默认控制台管理器
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			// 得到所有的控制台实例
			IConsole[] existing = manager.getConsoles();
			exists = false;
			// 新创建的MessageConsole实例不存在就加入到控制台管理器，并显示出来
			for (int i = 0; i < existing.length; i++) {
				if (console == existing[i]) {
					exists = true;
				}
			}
			if (!exists) {
				manager.addConsoles(new IConsole[] { console });
			}
		}
	}

	/**
	 * 描述:关闭控制台
	 **/
	public static void closeConsole() {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		if (console != null) {
			manager.removeConsoles(new IConsole[] { console });
		}
	}

	/**
	 * 获取控制台
	 * 
	 * @return
	 **/
	public static MessageConsole getConsole() {
		showConsole();
		return console;
	}

	/**
	 * 向控制台打印一条信息，并激活控制台。
	 * 
	 * @param message
	 * @param activate 是否激活控制台
	 **/
	private static void logToConsole(Object msg, boolean activate) {
		if (msg != null) {
			MessageConsoleStream printer = getConsole().newMessageStream();
			printer.setActivateOnWrite(activate);
			printer.println(msg.toString());
		}
	}

	public static void log(Object msg) {
		logToConsole(msg, false);// 不激活窗口
		logToLogFile(msg);
	}

	private static void logToLogFile(Object msg) {
		if (fileLog != null && msg != null) {
			fileLog.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, msg.toString()));
		}
	}

	public static void log(Object msg, String target) {
		if ("console".equals(target)) {
			logToConsole(msg, true);
		} else if ("file".equals(target)) {
			logToLogFile(msg);
		}
	}
}