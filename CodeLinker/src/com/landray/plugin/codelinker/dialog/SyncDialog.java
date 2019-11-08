package com.landray.plugin.codelinker.dialog;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.landray.plugin.codelinker.common.CustomDialog;
import com.landray.plugin.codelinker.common.ManagerFactory;
import com.landray.plugin.codelinker.common.MessageUtils;
import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.common.Utils;
import com.landray.plugin.codelinker.log.LinkerLogger;

public class SyncDialog extends CustomDialog {
	private Map<String, String> canChoose = null;
	private Map<String, String> choosedPrjNameStarts = null;

	public SyncDialog(Shell parentShell) {
		super(parentShell);
		dialogTitle = MessageUtils.DAILOG_TITLE_SYNC;
	}

	private void initCanChoose() {
		canChoose = new HashMap<String, String>();
		for (IProject prj : ProjectUtils.allProjects) {
			String pn = prj.getName().split("-")[0];
			canChoose.put(pn, pn);
		}
	}

	@Override
	protected void createDialogContent(Composite parent) {
		createLabel(parent, MessageUtils.getMessage("dialog.title.sync") + ":", 66);
		createPrjNamesArea(parent, ProjectUtils.webProjects, SWT.CHECK);
	}

	@Override
	protected void createPrjNames(Composite parent, Map<String, Map<String, Object>> projectsMap, int btnType) {
		List<String> prjNames = Utils.convertToList(canChoose.keySet());
		Collections.sort(prjNames);
		for (final String prjn : prjNames) {
			if (prjn.equals("RemoteSystemsTempFiles") || prjn.equals("Servers")) {
				continue;
			}
			Button btnradio = new Button(parent, btnType);
			btnradio.setText(prjn);
			btnradio.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					Button btn = (Button) evt.widget;
					if (btn.getSelection()) {
						choosedPrjNameStarts.put(prjn, prjn);
					} else {
						choosedPrjNameStarts.remove(prjn);
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent evt) {
				}
			});
			if (choosedPrjNameStarts.size() == 0) {
				choosedPrjNameStarts.put(prjn, prjn);
				btnradio.setSelection(true);
			} else if (choosedPrjNameStarts.containsKey(prjn)) {
				btnradio.setSelection(true);
			}
		}
	}

	private void refreshProjects() throws Throwable {
		String cpns = Utils.arrayToString(choosedPrjNameStarts.values().toArray());
		LinkerLogger.log(MessageUtils.getMessage("sync.hint.0") + cpns);
		for (IProject prj : ProjectUtils.allProjects) {
			if (cpns.contains(prj.getName().split("-")[0])) {
				ManagerFactory.getRereshManager().addTask(prj.getName());
			}
		}
	}

	@Override
	protected String getHintMsg() {
		if (choosedPrjNameStarts.size() == 0) {
			return MessageUtils.getMessage("sync.hint.1");
		}
		return "";
	}

	@Override
	protected void afterDialogClosed() throws Throwable {
		refreshProjects();
	}

	@Override
	public void beforeDialogOpen() throws Throwable {
		super.beforeDialogOpen();
		choosedPrjNameStarts = new HashMap<String, String>();
		initCanChoose();
	}
}