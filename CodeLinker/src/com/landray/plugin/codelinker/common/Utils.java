package com.landray.plugin.codelinker.common;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.landray.plugin.codelinker.log.LinkerLogger;

import net.sf.json.JSONArray;

public class Utils {
	public static final IWorkspaceRoot WORKROOT = ResourcesPlugin.getWorkspace().getRoot();

	public static List<String> sortEkpModules(Set<String> modules) {
		List<String> needSortList = convertToList(modules);
		Collections.sort(needSortList, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				if (s2.startsWith("CORE") && !s1.startsWith("CORE")) {
					return 1;
				} else if (s1.startsWith("CORE") && !s2.startsWith("CORE")) {
					return -1;
				}
				return s1.compareTo(s2);
			}
		});
		return needSortList;
	}

	public static void deleteModuleJars(File libFile, FilenameFilter fnf) throws Throwable {
		LinkerLogger.log(MessageUtils.getMessage("del.module.jars.start"));
		LinkerLogger.log(MessageUtils.getMessage("del.module.jars"));
		if (fnf == null) {
			fnf = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("kmss");
				}
			};
		}
		if (libFile.exists()) {
			File[] oldJarFiles = libFile.listFiles(fnf);
			for (File file : oldJarFiles) {
				if (file.exists()) {
					LinkerLogger.log(file.getName() + ";");
					FileUtils.forceDelete(file);
				}
			}
		}
		LinkerLogger.log(MessageUtils.getMessage("del.module.jars.end"));
	}

	public static void deleteDirLinks(File dir) throws Throwable {
		if (dir.exists() && dir.isDirectory()) {
			if (Files.isSymbolicLink(dir.toPath())) {
				LinkerLogger.log(dir.getAbsolutePath() + ";");
				Files.deleteIfExists(dir.toPath());
				delEmptyFolder(dir.getParentFile());
			} else {
				File[] files = dir.listFiles();
				for (File file : files) {
					deleteDirLinks(file);
				}
			}
		}
	}

	public static String getErrMessage(Throwable t) {
		String exceptionStack = "";
		if (t != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			try {
				t.printStackTrace(pw);
				exceptionStack += sw.toString();
			} finally {
				IOUtils.closeQuietly(pw);
				IOUtils.closeQuietly(sw);
			}
		}
		return exceptionStack;
	}

	public static List<String> convertToList(Set<String> sets) {
		String[] prjNsArr = new String[sets.size()];
		sets.toArray(prjNsArr);
		return Arrays.asList(prjNsArr);
	}

	public static boolean isNull(String msg) {
		return msg == null || msg.trim().length() == 0;
	}

	public static boolean isNotNull(String msg) {
		return !isNull(msg);
	}

	public static String[] sort(String[] cItems) {
		List<String> sortList = Arrays.asList(cItems);
		Collections.sort(sortList);
		return sortList.toArray(cItems);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] mapToArray(Map<String, T> map, Class<?> cls) {
		T[] arr = (T[]) Array.newInstance(cls, map.values().size());
		map.values().toArray(arr);
		return arr;
	}

	public static boolean isLinkFolderFile(File file) {
		return file.isDirectory() && Files.isSymbolicLink(file.toPath());
	}

	public static JSONArray getSavedModules() {
		JSONArray savedModules = new JSONArray();
		File oldModulesFile = new File(new File(WORKROOT.getLocationURI()), "savedModules.txt");
		if (oldModulesFile.exists()) {
			try {
				savedModules = JSONArray.fromObject(FileUtils.readFileToString(oldModulesFile, "UTF-8"));
			} catch (IOException e) {
				// 吃掉
			}
		} else {
			IProject coreModule = WORKROOT.getProject("CORE");
			if (coreModule != null) {
				IPath coreSrcPath = coreModule.getFile("src").getLocation();
				LinkerLogger.log(coreSrcPath != null ? coreSrcPath.toOSString() : "kong", "file");
				if (coreSrcPath != null) {
					File coreSrcFile = coreSrcPath.toFile();
					findSavedModules(coreSrcFile, savedModules);
					if (savedModules.size() > 0) {
						savedModules.add(0, "CORE");
					}
				}
			}
		}
		return savedModules;
	}

	private static void findSavedModules(File file, JSONArray savedModules) {
		if (file.exists() && file.isDirectory()) {
			if (Files.isSymbolicLink(file.toPath())) {
				String modulePath = file.getParentFile().getName() + "/" + file.getName();
				Map<String, Object> moduleInfos = ProjectUtils.validEkpModulePathes.get(modulePath);
				if (moduleInfos != null) {
					IProject module = (IProject) moduleInfos.get("project");
					savedModules.add(module.getName());
				} else {
					LinkerLogger.log("modulePath:" + modulePath, "file");
				}
			} else {
				File[] files = file.listFiles();
				for (File f : files) {
					findSavedModules(f, savedModules);
				}
			}
		}
	}

	public static void removeLink(File linkFile) throws Throwable {
		if (isLinkFolderFile(linkFile)) {
			Files.deleteIfExists(linkFile.toPath());
		}
		delEmptyFolder(linkFile.getParentFile());
	}

	private static void delEmptyFolder(File file) throws Throwable {
		File[] files = file.listFiles();
		if (files != null && files.length == 0) {
			Files.deleteIfExists(file.toPath());
		}
	}

	public static void createLink(File linkFile, File srcFile) throws Throwable {
		if (!linkFile.getParentFile().exists()) {
			linkFile.getParentFile().mkdirs();
		}
		if (!linkFile.exists()) {
			Files.createSymbolicLink(linkFile.toPath(), srcFile.toPath());
		}
	}

	public static Map<String, Map<String, Object>> getSavedModulesMap() {
		LinkerLogger.log(MessageUtils.getMessage("get.saved.modules.start"));
		Map<String, Map<String, Object>> rtn = new HashMap<String, Map<String, Object>>();
		JSONArray oldModules = new JSONArray();
		try {
			oldModules = getSavedModules();
			for (Object mk : oldModules) {
				String moduleName = mk.toString();
				rtn.put(moduleName, ProjectUtils.validEkpModuleNames.get(moduleName));
			}

		} catch (Throwable t) {
			LinkerLogger.log(getErrMessage(t), "file");
		}
		LinkerLogger.log(MessageUtils.getParamMessage("modules.saved", oldModules.toString()));
		LinkerLogger.log(MessageUtils.getMessage("get.saved.modules.end"));
		return rtn;
	}

	public static String calcTime(long startT, long endT) {
		return (endT - startT) / 1000 + "s";
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] listToArray(List<T> list, Class<?> cls) {
		T[] arr = (T[]) Array.newInstance(cls, list.size());
		list.toArray(arr);
		return arr;
	}

	public static <T> String arrayToString(T[] array) {
		StringBuilder rtnb = new StringBuilder(MessageUtils.getMessage("hint.["));
		if (array != null && array.length > 0) {
			for (int i = 0; i < array.length; i++) {
				T item = array[i];
				rtnb.append(item.toString());
				if (i < array.length - 1) {
					rtnb.append(";");
				}
			}
		}
		return rtnb.append(MessageUtils.getMessage("hint.]")).toString();
	}

	public static void asyncExec(final IAsyncAction asyncAction) {
		IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
		if (workbenchWindows != null && workbenchWindows.length > 0) {
			Display display = workbenchWindows[0].getShell().getDisplay();
			String actionName = asyncAction.getActionName();
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						asyncAction.doAction();
					} catch (Throwable t) {
						LinkerLogger.log(getErrMessage(t));
					}
				}
			});
			thread.setName(actionName);
			display.asyncExec(thread);
		}
	}

	public static void asyncExecTimeout(IAsyncAction asyncAction, long i) {
		//
	}
}