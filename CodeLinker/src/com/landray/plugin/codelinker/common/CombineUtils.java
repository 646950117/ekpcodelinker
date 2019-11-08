package com.landray.plugin.codelinker.common;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;

import com.landray.plugin.codelinker.log.LinkerLogger;
import com.landray.plugin.codelinker.refresh.RefreshMonitor;

import net.sf.json.JSONArray;

public class CombineUtils {
	public static String jarHandleType = "copy";// link，目前默认是拷贝

	public static void clearModules(Map<String, IProject> choosedEkpProject) throws Throwable {
		LinkerLogger.log(MessageUtils.getMessage("clear.module.start"));
		IProject[] ekpprjs = Utils.mapToArray(choosedEkpProject, IProject.class);
		if (ProjectUtils.coreModule != null) {
			File coreSrcFile = ProjectUtils.coreModule.getFolder("src").getLocation().toFile();
			File coreWebFile = ProjectUtils.coreModule.getFolder("WebContent").getLocation().toFile();
			for (int i = 0; i < ekpprjs.length; i++) {
				IProject ekp = ekpprjs[i];
				File ekpSrcFile = ekp.getFolder("src").getLocation().toFile();
				File ekpWebFile = ekp.getFolder("WebContent").getLocation().toFile();
				if (Utils.isLinkFolderFile(ekpSrcFile) && Utils.isLinkFolderFile(ekpWebFile)) {
					Files.deleteIfExists(ekpSrcFile.toPath());
					Files.deleteIfExists(ekpWebFile.toPath());
					ekp.refreshLocal(IProject.DEPTH_ONE, new RefreshMonitor(System.currentTimeMillis(),
							new ProjectHandler("refresh", ekp), new LinkedList<ProjectHandler>()));
					ProjectUtils.removeEkpOfCore(ekp);
				}
			}
			if (ProjectUtils.noEkpOfCore()) {
				clearCoreModules(coreSrcFile, coreWebFile);
				ProjectUtils.savedModulesMap = new HashMap<String, Map<String, Object>>();// 清空已保存的
				File saveFile = new File(new File(Utils.WORKROOT.getLocationURI()), "savedModules.txt");
				Files.deleteIfExists(saveFile.toPath());
				LinkerLogger.log(MessageUtils.getParamMessage("clear.module.del.saved", saveFile.getAbsolutePath()));
			}
			LinkerLogger.log(MessageUtils.getMessage("clear.module.end"));
		} else {
			LinkerLogger.log(MessageUtils.getMessage("core.module.empty"));
		}
	}

	private static void clearCoreModules(File srcFile, File webFile) throws Throwable {
		LinkerLogger.log(MessageUtils.getMessage("del.module.links.start"));
		if (srcFile.exists()) {
			for (File srcF : srcFile.listFiles()) {
				Utils.deleteDirLinks(srcF);
			}
		}
		if (webFile.exists()) {
			for (File webF : webFile.listFiles()) {
				Utils.deleteDirLinks(webF);
			}
		}
		LinkerLogger.log(MessageUtils.getMessage("del.module.links.end"));
		File libFile = new File(new StringBuilder(webFile.getAbsolutePath()).append(File.separator).append("WEB-INF")
				.append(File.separator).append("lib").toString());
		if (libFile.exists()) {
			Utils.deleteModuleJars(libFile, null);
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> combineModules(Map<String, Object> moduleInfos,
			Map<String, IProject> choosedEkpProject) throws Throwable {
		LinkerLogger.log(MessageUtils.getMessage("combine.module.start"));
		Map<String, String> rtn = new HashMap<String, String>();
		IProject[] ekpprjs = Utils.mapToArray(choosedEkpProject, IProject.class);
		File coreSrcFile = ProjectUtils.coreModule.getFolder("src").getLocation().toFile();
		File coreWebContentFile = ProjectUtils.coreModule.getFolder("WebContent").getLocation().toFile();
		Map<String, Map<String, Object>> choosedMap = (Map<String, Map<String, Object>>) moduleInfos.get("choosed");
		Map<String, Map<String, Object>> addsMap = (Map<String, Map<String, Object>>) moduleInfos.get("adds");
		List<String> addModuleNames = Utils.sortEkpModules(addsMap.keySet());
		for (String mn : addModuleNames) {
			if (!mn.equals("CORE")) {
				LinkerLogger.log(MessageUtils.getParamMessage("combine.module.add.start", mn));
				handleModuleOfCORE("add", addsMap.get(mn), coreSrcFile, coreWebContentFile);
				LinkerLogger.log(MessageUtils.getParamMessage("combine.module.add.end", mn));
			}
		}
		Map<String, Map<String, Object>> delsMap = (Map<String, Map<String, Object>>) moduleInfos.get("dels");
		List<String> delModuleNames = Utils.sortEkpModules(delsMap.keySet());
		for (String mn : delModuleNames) {
			LinkerLogger.log(MessageUtils.getParamMessage("combine.module.del.start", mn));
			handleModuleOfCORE("del", delsMap.get(mn), coreSrcFile, coreWebContentFile);
			LinkerLogger.log(MessageUtils.getParamMessage("combine.module.del.end", mn));
		}
		syncModuleJars(choosedMap);
		for (int i = 0; i < ekpprjs.length; i++) {
			final IProject ekp = ekpprjs[i];
			LinkerLogger.log(MessageUtils.getParamMessage("combine.ekp.start", ekp.getName()));
			File ekpSrcFile = ekp.getFolder("src").getLocation().toFile();
			File ekpWebContentFile = ekp.getFolder("WebContent").getLocation().toFile();
			if (!Utils.isLinkFolderFile(ekpSrcFile)) {
				FileUtils.deleteQuietly(ekpSrcFile);
				Utils.createLink(ekpSrcFile, coreSrcFile);
				rtn.put(ekp.getName(), "full");
			} else {
				rtn.put(ekp.getName(), "increment");
			}
			if (!Utils.isLinkFolderFile(ekpWebContentFile)) {
				FileUtils.deleteQuietly(ekpWebContentFile);
				Utils.createLink(ekpWebContentFile, coreWebContentFile);
			}
			ProjectUtils.addEkpOfCORE(ekp);
			LinkerLogger.log(MessageUtils.getParamMessage("combine.ekp.end", ekp.getName()));
		}
		try {
			File saveFile = new File(new File(Utils.WORKROOT.getLocationURI()), "savedModules.txt");
			FileUtils.writeStringToFile(saveFile,
					JSONArray.fromObject(moduleInfos.get("choosedModuleNames")).toString(), "UTF-8");
			LinkerLogger.log(MessageUtils.getParamMessage("combine.module.save", saveFile.getAbsolutePath()));
		} catch (IOException e) {
			// 吃掉
		}
		Utils.asyncExec(new IAsyncAction() {
			@Override
			public String getActionName() {
				return "combineFinish";
			}

			@Override
			public void doAction() throws Throwable {
				ProjectUtils.savedModulesMap = Utils.getSavedModulesMap();
			}
		});
		LinkerLogger.log(MessageUtils.getMessage("combine.module.end"));
		return rtn;
	}

	private static void handleModuleOfCORE(String handelType, Map<String, Object> moduleInfo, File coreSrcFile,
			File coreWebContentFile) throws Throwable {
		String modulePath = (String) moduleInfo.get("path");
		if (Utils.isNotNull(modulePath)) {
			LinkerLogger.log(MessageUtils.getParamMessage("common.module.path", modulePath));
			String[] mppath = modulePath.split("/");
			handleSrcLink(handelType, moduleInfo, mppath, coreSrcFile);
			handleJspLink(handelType, moduleInfo, mppath, coreWebContentFile);
			handleConfigLink(handelType, moduleInfo, mppath, coreWebContentFile);
			handleJar(handelType, moduleInfo, mppath, coreWebContentFile);
		}
	}

	@SuppressWarnings("unchecked")
	private static void handleJar(String handelType, Map<String, Object> moduleInfo, String[] mppath,
			File coreWebContentFile) throws Throwable {
		File jarFolder = (File) moduleInfo.get("jarsDir");
		String libFilePath = new StringBuilder(coreWebContentFile.getAbsolutePath()).append(File.separator)
				.append("WEB-INF").append(File.separator).append("lib").toString();
		if (jarFolder != null && jarFolder.exists() && jarFolder.listFiles().length > 0) {
			if ("add".equals(handelType)) {
				if ("copy".equals(jarHandleType)) {
					FileUtils.copyDirectory(jarFolder, new File(libFilePath));
				} else if ("link".equals(jarHandleType)) {
					Map<String, File> jarFiles = (Map<String, File>) moduleInfo.get("jars");
					for (String jarName : jarFiles.keySet()) {
						Utils.createLink(new File(libFilePath, jarName), jarFiles.get(jarName));
					}
				}
			} else if ("del".equals(handelType)) {
				if ("copy".equals(jarHandleType)) {
					final Map<String, File> jars = (Map<String, File>) moduleInfo.get("jars");
					Utils.deleteModuleJars(new File(libFilePath), new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return jars.containsKey(name);
						}
					});
				} else if ("link".equals(jarHandleType)) {
					//
				}
			}
			LinkerLogger.log(MessageUtils.getParamMessage("handle.type", "jar"));
		}
	}

	private static void handleConfigLink(String handelType, Map<String, Object> moduleInfo, String[] mppath,
			File coreWebContentFile) throws Throwable {
		String configTargetPath = new StringBuilder(coreWebContentFile.getAbsolutePath()).append(File.separator)
				.append("WEB-INF").append(File.separator).append("KmssConfig").append(File.separator).append(mppath[0])
				.append(File.separator).append(mppath[1]).toString();
		File configTargetFile = new File(configTargetPath);
		String configSourcePath = new StringBuilder((String) moduleInfo.get("configPath")).append(File.separator)
				.append(mppath[0]).append(File.separator).append(mppath[1]).toString();
		File configSourceFile = new File(configSourcePath);
		if ("add".equals(handelType)) {
			Utils.createLink(configTargetFile, configSourceFile);
		} else if ("del".equals(handelType)) {
			Utils.removeLink(configTargetFile);
		}
		LinkerLogger.log(MessageUtils.getParamMessage("handle.type", "config"));
	}

	private static void handleJspLink(String handelType, Map<String, Object> moduleInfo, String[] mppath,
			File coreWebContentFile) throws Throwable {
		String jspTargetPath = new StringBuilder(coreWebContentFile.getAbsolutePath()).append(File.separator)
				.append(mppath[0]).append(File.separator).append(mppath[1]).toString();
		File jspTargetFile = new File(jspTargetPath);
		String jspSourcePath = new StringBuilder((String) moduleInfo.get("jspPath")).append(File.separator)
				.append(mppath[0]).append(File.separator).append(mppath[1]).toString();
		File jspSourceFile = new File(jspSourcePath);
		if ("add".equals(handelType)) {
			Utils.createLink(jspTargetFile, jspSourceFile);
		} else if ("del".equals(handelType)) {
			Utils.removeLink(jspTargetFile);
		}
		LinkerLogger.log(MessageUtils.getParamMessage("handle.type", "jsp"));
	}

	private static void handleSrcLink(String handelType, Map<String, Object> moduleInfo, String[] mppath,
			File coreSrcFile) throws Throwable {
		String srcTargetPath = new StringBuilder(coreSrcFile.getAbsolutePath()).append(File.separator).append("com")
				.append(File.separator).append("landray").append(File.separator).append("kmss").append(File.separator)
				.append(mppath[0]).append(File.separator).append(mppath[1]).toString();
		File srcTargetFile = new File(srcTargetPath);
		String srcSourcePath = new StringBuilder((String) moduleInfo.get("srcPath")).append(File.separator)
				.append(mppath[0]).append(File.separator).append(mppath[1]).toString();
		File srcSourceFile = new File(srcSourcePath);
		if ("add".equals(handelType)) {
			Utils.createLink(srcTargetFile, srcSourceFile);
		} else if ("del".equals(handelType)) {
			Utils.removeLink(srcTargetFile);
		}
		LinkerLogger.log(MessageUtils.getParamMessage("handle.type", "src"));
	}

	public static void syncModuleJars(Map<String, Map<String, Object>> choosedModuleInfos) {
		//
	}
}