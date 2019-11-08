package com.landray.plugin.codelinker.common;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public abstract class CommonAction implements IWorkbenchWindowActionDelegate {
	private CustomDialog dialog = null;
	private IWorkbenchWindow window = null;

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
		dialog = getDialog(window.getShell());
	}

	protected abstract CustomDialog getDialog(Shell shell);

	@Override
	public void run(IAction action) {
		try {
			dialog.beforeDialogOpen();
			dialog.open();
		} catch (Throwable t) {
			MessageDialog.openError(window.getShell(), MessageUtils.HINT_ERR, Utils.getErrMessage(t));
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}
}