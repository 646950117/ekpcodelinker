package com.landray.plugin.codelinker.action;

import org.eclipse.swt.widgets.Shell;

import com.landray.plugin.codelinker.common.CommonAction;
import com.landray.plugin.codelinker.common.CustomDialog;
import com.landray.plugin.codelinker.dialog.LinkerDialog;

public class LinkerAction extends CommonAction {

	@Override
	protected CustomDialog getDialog(Shell shell) {
		return new LinkerDialog(shell);
	}
}