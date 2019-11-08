package com.landray.plugin.codelinker.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.landray.plugin.codelinker.common.CombineUtils;
import com.landray.plugin.codelinker.common.CustomDialog;
import com.landray.plugin.codelinker.common.MessageUtils;
import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.listener.ListenersProvider;

public class ClearDialog extends CustomDialog {

	public ClearDialog(Shell parentShell) {
		super(parentShell);
		dialogTitle = MessageUtils.DAILOG_TITLE_CLEAR;
	}

	@Override
	protected void afterDialogClosed() throws Throwable {
		ListenersProvider.removeListener(ListenersProvider.RESOURCE_CHANGE);
		CombineUtils.clearModules(choosedProjectMap);
		ListenersProvider.addListener(ListenersProvider.RESOURCE_CHANGE);
	}

	@Override
	protected void createDialogContent(Composite parent) {
		createLabel(parent, MessageUtils.getMessage("clear.hint.0"), 0);
		createPrjNamesArea(parent, ProjectUtils.webProjects, SWT.CHECK);
	}
}