package com.landray.plugin.codelinker.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.landray.plugin.codelinker.common.CustomDialog;
import com.landray.plugin.codelinker.common.MessageUtils;
import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.common.Utils;
import com.landray.plugin.codelinker.log.LinkerLogger;

public class UnusefulDialog extends CustomDialog {

	public UnusefulDialog(Shell parentShell) {
		super(parentShell);
		dialogTitle = MessageUtils.DAILOG_TITLE_UNUSEFUL;
	}

	private String unuseName = "";
	private boolean fuzzySearch = false;

	@Override
	protected void afterDialogClosed() throws Throwable {
		delUnuseFiles();
	}

	private void delUnuseFiles() throws Throwable {
		for (IProject cp : choosedProjectMap.values()) {
			int delNums = delUnuseFiles(cp);
			if (delNums == 0) {
				LinkerLogger.log(MessageUtils.getParamMessage("unuseful.file.find.empty",
						fuzzySearch ? MessageUtils.getMessage("unuseful.search.like")
								: MessageUtils.getMessage("unuseful.search.equal"),
						Utils.arrayToString(choosedProjectMap.keySet().toArray()), unuseName));
			}
		}
	}

	private int delUnuseFiles(IProject cp) throws Throwable {
		final List<String> delNames = new ArrayList<String>();
		cp.accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource res) throws CoreException {
				if (res.getName().equals(unuseName) || (fuzzySearch && res.getName().contains(unuseName))) {
					LinkerLogger.log(MessageUtils.getParamMessage("unuseful.del.file", res.getLocation().toOSString()));
					res.delete(true, null);
					delNames.add(res.getName());
				}
				return true;
			}
		});
		return delNames.size();
	}

	@Override
	protected void createDialogContent(Composite parent) {
		((GridData) createLabel(parent, MessageUtils.getMessage("unuseful.file.name"), 0)
				.getLayoutData()).horizontalAlignment = SWT.CENTER;
		Text text = new Text(parent, SWT.NONE);
		text.addListener(SWT.CHANGED, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				Text txt = (Text) evt.widget;
				unuseName = txt.getText();
			}
		});
		GridDataFactory.swtDefaults().hint(200, 20).applyTo(text);
		((GridData) createLabel(parent, MessageUtils.getMessage("unuseful.search.like"), 0)
				.getLayoutData()).horizontalAlignment = SWT.CENTER;
		new Button(parent, SWT.CHECK).addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent sevt) {
				Button btn = (Button) sevt.widget;
				fuzzySearch = btn.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				//
			}
		});
		((GridData) createLabel(parent, MessageUtils.getMessage("common.hint.target.web"), 0)
				.getLayoutData()).horizontalAlignment = SWT.CENTER;
		createPrjNamesArea(parent, ProjectUtils.notEkpModules, SWT.CHECK);
	}

	@Override
	protected String getHintMsg() {
		String rtn = super.getHintMsg();
		if (Utils.isNull(rtn)) {
			if (Utils.isNull(unuseName)) {
				rtn = MessageUtils.getMessage("unuseful.name.empty");
			} else if (!MessageDialog.openConfirm(getShell(), MessageUtils.OK,
					MessageUtils.getParamMessage("unusefull.del.hint",
							fuzzySearch ? MessageUtils.getMessage("unuseful.search.like")
									: MessageUtils.getMessage("unuseful.search.equal"),
							Utils.arrayToString(choosedProjectMap.keySet().toArray()), unuseName))) {
				rtn = "nohint";
			}
		}
		return rtn;
	}

	@Override
	public void beforeDialogOpen() throws Throwable {
		super.beforeDialogOpen();
		unuseName = "";
		fuzzySearch = false;
	}

}