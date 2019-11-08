package com.landray.plugin.codelinker.listener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.landray.plugin.codelinker.common.ManagerFactory;
import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.common.Utils;
import com.landray.plugin.codelinker.log.LinkerLogger;

public class ResourceChangeListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent rce) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWidnow = workbench.getActiveWorkbenchWindow();
		if (activeWidnow != null && activeWidnow.getActivePage() != null
				&& activeWidnow.getActivePage().getActiveEditor() != null) {
			IEditorInput editorInput = activeWidnow.getActivePage().getActiveEditor().getEditorInput();
			if (editorInput != null && editorInput instanceof FileEditorInput) {
				FileEditorInput fileEditor = (FileEditorInput) editorInput;
				IFile file = fileEditor.getFile();
				if (file != null) {
					if (ProjectUtils.isEKP(file.getProject())) {
						IProject trueProject = ProjectUtils.getTrueProject(file);
						ProjectUtils.doRefreshRelativeEKPModule(trueProject);
					} else if (ProjectUtils.isEKPModule(file.getProject())) {
						ProjectUtils.doRefreshRelativeEKP();
					}
				}
			}
		} else {
			IResourceDelta rootDelta = rce.getDelta();
			if (rootDelta != null) {
				try {
					rootDelta.accept(new ResourceDeltaVisitor());
				} catch (CoreException e) {
					LinkerLogger.log(Utils.getErrMessage(e));
				}
			}
		}
	}

	private static final class ResourceDeltaVisitor implements IResourceDeltaVisitor {

		@Override
		public boolean visit(IResourceDelta resDelta) throws CoreException {
			int kind = resDelta.getKind();
			int flag = resDelta.getFlags();
			LinkerLogger.log("kind:" + kind, "file");
			LinkerLogger.log("flag:" + flag, "file");
			IResource resource = resDelta.getResource();
			IPath resLocation = null;
			IProject project = null;
			if (resource != null) {
				resLocation = resource.getLocation();
				LinkerLogger.log(resLocation != null ? resLocation.toOSString() : "empty_location", "file");
				project = resource.getProject();
				if (project != null) {
					if ((kind == IResourceDelta.ADDED && flag == IResourceDelta.OPEN)
							|| (kind == IResourceDelta.REMOVED && resLocation == null)) {
						// 新增删除项目
						// 读取项目信息
						ManagerFactory.getLoaderManager().addTask("project");
						return false;
					} else if (notNeedHandle(resource, flag)) {
						// 不做处理
						LinkerLogger.log("no handle", "file");
						return false;
					} else if (IResourceDelta.REMOVED == kind || IResourceDelta.ADDED == kind
							|| (IResourceDelta.CHANGED == kind && IResourceDelta.NO_CHANGE != flag)) {
						LinkerLogger.log("need refresh", "file");
						if (ProjectUtils.isEKP(project)) {
							IProject trueProject = ProjectUtils.getTrueProject(resource);
							ProjectUtils.doRefreshRelativeEKPModule(trueProject);
						} else if (ProjectUtils.isEKPModule(project)) {
							ProjectUtils.doRefreshRelativeEKP();
						}
						resDelta.accept(new IResourceDeltaVisitor() {
							@Override
							public boolean visit(IResourceDelta delta) throws CoreException {
								IResource resource = delta.getResource();
								if (resource != null) {
									if ("jar".equals(resource.getFileExtension())) {
										ProjectUtils.syncJars(delta);
										return false;
									}
								}
								return true;
							}
						});
						return false;
					}
				}
			}
			return true;
		}

		private boolean notNeedHandle(IResource resource, int flag) {
			return "build".equals(resource.getName()) || "classes".equals(resource.getName())
					|| flag == IResourceDelta.MARKERS || flag == IResourceDelta.DESCRIPTION
					|| resource.getName().startsWith(".");
		}

	}
}