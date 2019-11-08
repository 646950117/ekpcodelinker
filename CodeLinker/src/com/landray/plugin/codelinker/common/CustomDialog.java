package com.landray.plugin.codelinker.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.landray.plugin.codelinker.log.LinkerLogger;

public abstract class CustomDialog extends Dialog {

	protected CustomDialog(Shell parentShell) {
		super(parentShell);
	}

	private AtomicBoolean doingAfterDialogDisposed = new AtomicBoolean(false);
	private Composite dialogArea = null;
	private boolean isOKPressed = false;

	protected String dialogTitle = "";
	protected Map<String, IProject> choosedProjectMap = null;

	protected abstract void afterDialogClosed() throws Throwable;

	protected abstract void createDialogContent(Composite parent);

	public void beforeDialogOpen() throws Throwable {
		choosedProjectMap = new HashMap<String, IProject>();
	}

	protected String getHintMsg() {
		if (choosedProjectMap.size() == 0) {
			return MessageUtils.getMessage("web.project.empty");
		}
		return "";
	}

	protected Label createLabel(Composite parent, String label, int width) {
		Label labelCom = new Label(parent, SWT.NONE);
		labelCom.setText(label);
		GridData lgd = new GridData(SWT.BORDER);
		labelCom.setLayoutData(lgd);
		if (width > 0) {
			lgd.widthHint = width;
		} else {
			lgd.widthHint = labelCom.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		}
		return labelCom;
	}

	protected void createPrjNamesArea(Composite parent, Map<String, Map<String, Object>> projectsMap, int btnType) {
		final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		GridLayoutFactory.fillDefaults().applyTo(scrolledComposite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		final Composite innerComposite = new Composite(scrolledComposite, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(innerComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(innerComposite);
		scrolledComposite.setContent(innerComposite);
		createPrjNames(innerComposite, projectsMap, btnType);
		Point innerSize = innerComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolledComposite.setMinSize(innerSize);
		GridDataFactory.fillDefaults().grab(true, true).hint(getPrjNamesAreaSize(innerSize)).applyTo(scrolledComposite);
	}

	protected Point getPrjNamesAreaSize(Point innerSize) {
		int y = innerSize.y - 10 > 360 ? 360 : innerSize.y - 10;
		return new Point(innerSize.x + 10, y);
	}

	protected void createPrjNames(Composite parent, Map<String, Map<String, Object>> projectsMap, int btnType) {
		for (String wpn : projectsMap.keySet()) {
			IProject wp = ProjectUtils.getProject(wpn, projectsMap);
			Button btnradio = new Button(parent, btnType);
			btnradio.setText(wp.getName());
			btnradio.setData(wp);
			btnradio.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					Button btn = (Button) evt.widget;
					IProject cprj = (IProject) btn.getData();
					if (btn.getSelection()) {
						choosedProjectMap.put(cprj.getName(), cprj);
					} else {
						choosedProjectMap.remove(cprj.getName());
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent evt) {
				}
			});
			if (choosedProjectMap.size() == 0) {
				choosedProjectMap.put(wp.getName(), wp);
				btnradio.setSelection(true);
			} else if (choosedProjectMap.containsKey(wp.getName())) {
				btnradio.setSelection(true);
			}
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(dialogTitle);
		getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent evt) {
				if (isOKPressed) {
					Utils.asyncExec(new IAsyncAction() {
						@Override
						public String getActionName() {
							return "afterDialogShellDisposed";
						}

						@Override
						public void doAction() throws Throwable {
							try {
								if (doingAfterDialogDisposed.compareAndSet(false, true)) {// 控流
									afterDialogClosed();
								}
							} catch (Throwable t) {
								String errMsg = Utils.getErrMessage(t);
								LinkerLogger.log(errMsg);
								MessageDialog.openError(getParentShell(), MessageUtils.HINT_ERR, errMsg);
							} finally {
								doingAfterDialogDisposed.set(false);
								isOKPressed = false;// 还原回去
							}
						}
					});
				}
			}
		});
		dialogArea = new Composite(parent, SWT.NONE);
		dialogArea.setLayout(new GridLayout(getColNum(), false));
		createDialogContent(dialogArea);
		return dialogArea;
	}

	protected int getColNum() {
		// 默认两列布局
		return 2;
	}

	@Override
	protected void okPressed() {
		String hintMsg = getHintMsg();
		if (Utils.isNotNull(hintMsg)) {
			if (!"nohint".equals(hintMsg)) {
				MessageDialog.openInformation(getShell(), MessageUtils.HINT_INFO, hintMsg);
			}
		} else {
			isOKPressed = true;
			close();
		}
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Rectangle bounds = getParentShell().getBounds();
		int shellw = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		int shellh = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		int x = bounds.x + (bounds.width - shellw) / 2;
		int y = bounds.y + (bounds.height - shellh) / 2;
		return new Point(x, y < 100 ? 100 : y);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		GridData gridData = (GridData) parent.getLayoutData();
		gridData.horizontalAlignment = SWT.CENTER;
		createButton(parent, 0, MessageUtils.OK, true);
		createButton(parent, 1, MessageUtils.CANCEL, false);
	}
}