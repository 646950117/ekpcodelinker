package com.landray.plugin.codelinker.common;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;

import com.landray.plugin.codelinker.log.LinkerLogger;

@SuppressWarnings("unchecked")
public class ProjectUtils {
	public static IProject[] allProjects = null;
	public static Map<String, Map<String, Object>> allProjectsMap = null;
	public static Map<String, Map<String, Object>> webProjects = null;
	public static Map<String, Map<String, Object>> notEkpModules = null;
	public static Map<String, Map<String, Object>> invalidEkpModuleNames = null;
	public static Map<String, Map<String, Object>> validEkpModuleNames = null;
	public static Map<String, Map<String, Object>> validEkpModulePathes = null;
	public static IProject coreModule = null;
	public static Map<String, String> missRequiredModels = null;
	public static Map<String, Map<String, Object>> savedModulesMap = null;

	static {
		initRequiredModules();
	}

	private static void initRequiredModules() {
		missRequiredModels = new HashMap<String, String>();
		String[] reqMNs = MessageUtils.getMessage("required.modules").split(";");
		for (String mn : reqMNs) {
			missRequiredModels.put(mn, mn);
		}
	}

	public static void loadProjects() throws Throwable {
		LinkerLogger.log(MessageUtils.getMessage("projectutils.load"), "file");
		allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		allProjectsMap = new HashMap<String, Map<String, Object>>();
		webProjects = new HashMap<String, Map<String, Object>>();
		notEkpModules = new HashMap<String, Map<String, Object>>();
		invalidEkpModuleNames = new HashMap<String, Map<String, Object>>();
		validEkpModuleNames = new HashMap<String, Map<String, Object>>();
		validEkpModulePathes = new HashMap<String, Map<String, Object>>();
		if (allProjects.length > 0) {
			Map<String, Object> moduleInfos = null;
			for (IProject prj : allProjects) {
				String moduleName = prj.getName();
				moduleInfos = new HashMap<String, Object>();
				moduleInfos.put("project", prj);
				allProjectsMap.put(moduleName, moduleInfos);
				if (prj.isOpen() && !prj.getName().equals("RemoteSystemsTempFiles")
						&& !prj.getName().equals("Servers")) {
					if (isRequiredModule(moduleName)) {
						missRequiredModels.remove(moduleName);
					}
					if ("CORE".equals(moduleName)) {
						// CORE组件
						coreModule = prj;
						validEkpModuleNames.put(moduleName, moduleInfos);
					} else if (isEKPModule(prj)) {
						moduleInfos.put("configPath", prj.getFolder("config").getLocation().toOSString());
						moduleInfos.put("jspPath", prj.getFolder("jsp").getLocation().toOSString());
						moduleInfos.put("srcPath", prj.getFolder("src").getLocation().toOSString());
						loadJars(moduleInfos);
						String modulePath = getModulePath(prj);
						if (Utils.isNotNull(modulePath)) {
							// 有效EKP模块
							moduleInfos.put("path", modulePath);
							validEkpModuleNames.put(moduleName, moduleInfos);
							validEkpModulePathes.put(modulePath, moduleInfos);
						} else {
							// 没有description.xml
							invalidEkpModuleNames.put(moduleName, moduleInfos);
						}
					} else {
						notEkpModules.put(moduleName, moduleInfos);
						if (prj.getDescription().hasNature("org.eclipse.wst.common.project.facet.core.nature")) {
							webProjects.put(moduleName, moduleInfos);
							if (isEKP(prj)) {
								// 考虑CORE在该project之后赋值的情况，排序解决，也可以方法最后来设置这些信息
							}
						}
					}
				}
			}
		}
		for (String wpn : webProjects.keySet()) {
			IProject wp = getProject(wpn, webProjects);
			if (isEKP(wp)) {
				addEkpOfCORE(wp);
			}
		}
		int sum = allProjects.length;
		int web = webProjects.keySet().size();
		int ekp = validEkpModuleNames.size();
		int invalid = invalidEkpModuleNames.size();
		String[] pvs = { String.valueOf(sum), String.valueOf(web), String.valueOf(ekp),
				invalid == 0 ? MessageUtils.getMessage("common.no")
						: Utils.arrayToString(invalidEkpModuleNames.keySet().toArray()),
				String.valueOf(sum - web - ekp - invalid) };
		LinkerLogger.log(MessageUtils.getParamMessage("projectutils.hint.1", pvs));
	}

	private static void loadJars(Map<String, Object> moduleInfos) {
		IProject prj = (IProject) moduleInfos.get("project");
		IFolder jarFolder = prj.getFolder("jar");
		if (jarFolder.exists()) {
			File jarFolderFile = jarFolder.getLocation().toFile();
			moduleInfos.put("jarsDir", jarFolderFile);
			File[] jarFiles = jarFolderFile.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar") && name.startsWith("kmss_");
				}
			});
			if (jarFiles != null && jarFiles.length > 0) {
				Map<String, File> jars = new HashMap<String, File>();
				moduleInfos.put("jars", jars);
				for (File jf : jarFiles) {
					jars.put(jf.getName(), jf);
				}
			}
		}
	}

	public static String getModulePath(IProject module) {
		String rtn = "";
		try {
			File descFile = getDescFile(module);
			if (descFile != null) {
				Document descriptionXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(descFile);
				rtn = descriptionXml.getElementsByTagName("module-path").item(0).getFirstChild().getNodeValue();
			} else if (!"CORE".equals(module.getName())) {
				LinkerLogger.log(MessageUtils.getParamMessage("projectutils.hint.0", module.getName()));
			}
		} catch (Throwable t) {
			// 吃掉
		}
		return rtn;
	}

	private static File getDescFile(IProject module) {
		File descFile = null;
		File configFolder = new File(module.getFolder("config").getLocation().toOSString());
		if (configFolder.exists()) {
			descFile = getDescFile(configFolder);
		}
		return descFile;
	}

	private static File getDescFile(File file) {
		File rtn = null;
		if (file.isFile() && file.getName().equals("description.xml")) {
			rtn = file;
		} else if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				if (rtn == null) {
					rtn = getDescFile(f);
				}
			}
		}
		return rtn;
	}

	public static boolean isEKP(IProject prj) {
		boolean rtn = false;
		File prjFile = prj.getLocation().toFile();
		try {
			// 排除CORE
			rtn = !prj.getName().equals("CORE") && Paths.get(new File(prjFile, "src").toPath().toUri()).toRealPath()
					.toString().equals(coreModule.getFolder("src").getLocation().toOSString());
		} catch (IOException e) {

		}
		return rtn;
	}

	public static boolean isEKPModule(IProject prj) {
		Map<String, Object> moduleInfos = validEkpModuleNames.get(prj.getName());
		if (moduleInfos != null) {
			return true;
		}
		try {
			String[] nids = prj.getDescription().getNatureIds();
			if (nids.length == 0 && prj.getFolder("src").exists() && prj.getFolder("jsp").exists()
					&& prj.getFolder("config").exists()) {
				return true;
			}
		} catch (CoreException e) {
			// 吃掉
		}
		return false;
	}

	public static boolean isRequiredModule(String moduleName) {
		return moduleName.startsWith("CORE") || moduleName.startsWith("Third-008-J");
	}

	private static String getResourceModulePath(String resPath) {
		String rtn = "";
		String[] tmpSplit = null;
		if (resPath.matches(".*KmssConfig/.*/.*")) {
			tmpSplit = resPath.substring(resPath.indexOf("KmssConfig")).split("/");
			rtn = tmpSplit[1] + "/" + tmpSplit[2];
		} else if (resPath.matches(".*WebContent/[^WEB-INF].*/[^lib].*")) {
			tmpSplit = resPath.substring(resPath.indexOf("WebContent")).split("/");
			rtn = tmpSplit[1] + "/" + tmpSplit[2];
		} else if (resPath.matches(".*kmss/.*/.*")) {
			tmpSplit = resPath.substring(resPath.indexOf("kmss")).split("/");
			rtn = tmpSplit[1] + "/" + tmpSplit[2];
		}
		return rtn;
	}

	public static IProject getTrueProject(IResource resource) {
		IProject rtn = null;
		try {
			String resourcePath = resource.getLocation().toOSString();
			String modulePath = "";
			if (resource.getName().endsWith("jar")) {
				return getJarModule(resource.getLocation().toFile());
			} else {
				modulePath = getResourceModulePath(resourcePath);
			}
			if (Utils.isNotNull(modulePath)) {
				Map<String, Object> moduleInfos = ProjectUtils.validEkpModulePathes.get(modulePath);
				if (moduleInfos != null) {
					rtn = (IProject) moduleInfos.get("project");
				}
			}
			if (rtn == null) {
				// 为了找CORE//getParentFile为了处理删除时的
				String truePath = Paths.get(new File(resourcePath).getParentFile().toURI()).toRealPath().toString();
				IFile[] trueFiles = Utils.WORKROOT.findFilesForLocationURI(new File(truePath).toURI());
				if (trueFiles.length > 0) {
					rtn = trueFiles[0].getProject();
				}
			}
		} catch (Throwable t) {
			LinkerLogger.log(Utils.getErrMessage(t), "file");
		}
		return rtn;
	}

	public static void doRefreshRelativeEKPModule(final IProject ekpModule) {
		if (ekpModule != null) {
			ManagerFactory.getRereshManager().addTask(ekpModule.getName());
		}
	}

	public static void doRefreshRelativeEKP() {
		ManagerFactory.getRereshManager().addTask("allekp");
	}

	public static void loadSavedModules() {
		savedModulesMap = Utils.getSavedModulesMap();
	}

	public static void syncJars(IResourceDelta resDelta) {
		if (resDelta != null) {
			// 资源变更触发
			String handleType = IResourceDelta.ADDED == resDelta.getKind() ? "add" : "remove";
			IResource resource = resDelta.getResource();
			IProject project = null;
			if (resource != null) {
				project = resource.getProject();
				if (project != null) {
					File jarFile = resource.getLocation().toFile();
					if (isEKP(project)) {
						handleEKPJarToModule(handleType, jarFile);
					} else if (isEKPModule(project)) {
						handleModuleJarToEKP(handleType, jarFile, project);
					}
				}
			}
		}
	}

	private static void handleModuleJarToEKP(String handleType, final File jarFile, final IProject jarModule) {
		try {
			if (jarModule.getName().equals("CORE")) {
			} else {
				File libFile = new File(new StringBuilder(coreModule.getFolder("WebContent").getLocation().toOSString())
						.append(File.separator).append("WEB-INF").append(File.separator).append("lib").toString());
				Map<String, Object> moduleInfos = validEkpModuleNames.get(jarModule.getName());
				Map<String, File> jars = (Map<String, File>) moduleInfos.get("jars");
				if (jars == null) {
					jars = new HashMap<String, File>();
					moduleInfos.put("jars", jars);
				}
				String[] hintMsg = { MessageUtils.getMessage("common." + handleType), jarFile.getName() };
				if ("add".equals(handleType)) {
					File jarDir = (File) moduleInfos.get("jarsDir");
					if (jarDir == null) {
						moduleInfos.put("jarsDir", jarFile.getParentFile());
					}
					FileUtils.copyFileToDirectory(jarFile, libFile);
					jars.put(jarFile.getName(), jarFile);
					LinkerLogger.log(MessageUtils.getParamMessage("handle.module.jar.to.ekp", hintMsg), "file");
				} else if ("remove".equals(handleType)) {
					Utils.deleteModuleJars(libFile, new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.equals(jarFile.getName());
						}
					});
					jars.remove(jarFile.getName());
					LinkerLogger.log(MessageUtils.getParamMessage("handle.module.jar.to.ekp", hintMsg), "file");
				}
			}
		} catch (Throwable t) {
			LinkerLogger.log(Utils.getErrMessage(t), "file");
		} finally {
			// 需要刷新EKP
			doRefreshRelativeEKP();
		}
	}

	private static void handleEKPJarToModule(String handleType, File jarFile) {
		final IProject jarModule = getJarModule(jarFile);
		if (jarModule != null) {
			try {
				String[] hintMsg = { MessageUtils.getMessage("common." + handleType), jarFile.getName() };
				if ("CORE".equals(jarModule.getName())) {
					//
				} else {
					Map<String, Object> moduleInfos = validEkpModuleNames.get(jarModule.getName());
					Map<String, File> jars = (Map<String, File>) moduleInfos.get("jars");
					if (jars == null) {
						jars = new HashMap<String, File>();
						moduleInfos.put("jars", jars);
					}
					File jarDir = jarModule.getFolder("jar").getLocation().toFile();
					if (!jarDir.exists()) {
						jarDir.mkdir();
						moduleInfos.put("jarsDir", jarDir);
					}
					if ("add".equals(handleType)) {
						FileUtils.copyFileToDirectory(jarFile, jarDir);
						jars.put(jarFile.getName(), new File(jarDir, jarFile.getName()));
						LinkerLogger.log(MessageUtils.getParamMessage("handle.ekp.jar.to.module", hintMsg), "file");
					} else if ("remove".equals(handleType)) {
						jars.get(jarFile.getName()).delete();
						jars.remove(jarFile.getName());
						LinkerLogger.log(MessageUtils.getParamMessage("handle.ekp.jar.to.module", hintMsg), "file");
					}
				}
			} catch (Throwable t) {
				LinkerLogger.log(Utils.getErrMessage(t), "file");
			} finally {
				Utils.asyncExec(new IAsyncAction() {
					@Override
					public String getActionName() {
						return "jarChange";
					}

					@Override
					public void doAction() throws Throwable {
						ManagerFactory.getRereshManager().addTask(jarModule.getName());
					}
				});
			}
		}
	}

	private static IProject getJarModule(File jarFile) {
		IProject jarModule = null;
		String jarName = jarFile.getName();
		String[] jarNameSplit = jarName.split("_");
		if (jarNameSplit.length >= 3) {
			// 符合jar包命名规范
			Map<String, Object> jarModuleInfos = validEkpModulePathes.get(jarNameSplit[1] + "/" + jarNameSplit[2]);
			if (jarModuleInfos != null) {
				jarModule = (IProject) jarModuleInfos.get("project");
			}
		} else {
			jarModule = coreModule;// 被认为EKP项目(实际上就是CORE)
		}
		return jarModule;
	}

	public static IProject getProject(String projectName, Map<String, Map<String, Object>> projectInfosMap) {
		Map<String, Object> projedtInfos = projectInfosMap.get(projectName);
		return (IProject) projedtInfos.get("project");
	}

	public static void addEkpOfCORE(IProject ekp) {
		Map<String, Object> coreModuleInfos = ProjectUtils.validEkpModuleNames.get("CORE");
		Map<String, IProject> ekpMap = (Map<String, IProject>) coreModuleInfos.get("ekpMap");
		if (ekpMap == null) {
			ekpMap = new HashMap<String, IProject>();
			coreModuleInfos.put("ekpMap", ekpMap);
		}
		ekpMap.put(ekp.getName(), ekp);
	}

	public static void removeEkpOfCore(IProject ekp) {
		Map<String, Object> coreModuleInfos = ProjectUtils.validEkpModuleNames.get("CORE");
		Map<String, IProject> ekpMap = (Map<String, IProject>) coreModuleInfos.get("ekpMap");
		if (ekpMap != null) {
			ekpMap.remove(ekp.getName());
		}
	}

	public static boolean noEkpOfCore() {
		Map<String, Object> coreModuleInfos = ProjectUtils.validEkpModuleNames.get("CORE");
		if (coreModuleInfos != null) {
			Map<String, IProject> ekpMap = (Map<String, IProject>) coreModuleInfos.get("ekpMap");
			return ekpMap == null || ekpMap.size() == 0;
		}
		return false;
	}
}