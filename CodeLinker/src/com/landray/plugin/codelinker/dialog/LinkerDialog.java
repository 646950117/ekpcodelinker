package com.landray.plugin.codelinker.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.landray.plugin.codelinker.common.CombineUtils;
import com.landray.plugin.codelinker.common.CustomDialog;
import com.landray.plugin.codelinker.common.ManagerFactory;
import com.landray.plugin.codelinker.common.MessageUtils;
import com.landray.plugin.codelinker.common.ProjectUtils;
import com.landray.plugin.codelinker.common.Utils;
import com.landray.plugin.codelinker.listener.ListenersProvider;

public class LinkerDialog extends CustomDialog {

	public LinkerDialog(Shell parentShell) {
		super(parentShell);
		dialogTitle = MessageUtils.DAILOG_TITLE_LINKER;
	}

	@Override
	protected void afterDialogClosed() throws Throwable {
		// 先去掉监听器
		ListenersProvider.removeListener(ListenersProvider.RESOURCE_CHANGE);
		Map<String, String> combineRtn = CombineUtils
				.combineModules(handleModuleInfos(choosedModuleInfos, ProjectUtils.savedModulesMap), choosedProjectMap);
		for (final IProject prj : choosedProjectMap.values()) {
			if (autoRefresh) {
				ManagerFactory.getRereshManager().addTask(prj.getName(), autoBuild,
						combineRtn.get(prj.getName()));
			}
		}
	}

	private Map<String, Object> handleModuleInfos(Map<String, Map<String, Object>> choosedPrjs,
			Map<String, Map<String, Object>> lastChoosedPrjs) {
		Map<String, Object> rtn = new HashMap<String, Object>();
		rtn.put("choosedModuleNames", Utils.sortEkpModules(choosedPrjs.keySet()));
		rtn.put("choosed", choosedPrjs);
		rtn.put("lastChoosed", lastChoosedPrjs);
		Map<String, Map<String, Object>> addsMap = new HashMap<String, Map<String, Object>>();
		Map<String, Map<String, Object>> delsMap = new HashMap<String, Map<String, Object>>();
		for (String ck : choosedPrjs.keySet()) {
			if (!lastChoosedPrjs.containsKey(ck)) {
				addsMap.put(ck, choosedPrjs.get(ck));
			}
		}
		for (String lck : lastChoosedPrjs.keySet()) {
			if (!choosedPrjs.containsKey(lck)) {
				delsMap.put(lck, lastChoosedPrjs.get(lck));
			}
		}
		rtn.put("adds", addsMap);
		rtn.put("dels", delsMap);
		return rtn;
	}

	private List canChooseList = null;
	private List choosedList = null;
	private Map<String, Map<String, Object>> choosedModuleInfos = null;
	private Map<String, Map<String, Object>> canChooseModuleInfos = null;
	private boolean containsCOREModules = false;
	private Button coreBtn = null;
	private boolean autoRefresh = true;// 默认刷新
	private boolean autoBuild = false;

	private SelectionAdapter btnHandler = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent selEvt) {
			Button btn = (Button) selEvt.widget;
			String btnIden = btn.getData("iden").toString();
			switch (btnIden) {
			case "core":
				handleCore(btn);
				break;
			case "add":
				handleAdd();
				break;
			case "addAll":
				handleAddAll();
				break;
			case "deleteAll":
				handleDeleteAll();
				break;
			case "delete":
				handleDelete();
				break;
			default:
				break;
			}
		}
	};

	private void handleCore(Button btn) {
		String[] ccms = canChooseList.getItems();
		for (String ccm : ccms) {
			if (ProjectUtils.isRequiredModule(ccm)) {
				canChooseList.remove(ccm);
				canChooseModuleInfos.remove(ccm);
				choosedList.add(ccm);
				choosedModuleInfos.put(ccm, ProjectUtils.validEkpModuleNames.get(ccm));
			}
		}
		containsCOREModules = true;
		btn.setVisible(false);
	}

	private void handleAdd() {
		String[] adds = canChooseList.getSelection();
		int addsLength = adds.length;
		if (addsLength > 0) {
			String[] cItems = choosedList.getItems();
			int cLength = cItems.length;
			cItems = Arrays.copyOf(cItems, cLength + addsLength);// 数组扩容
			System.arraycopy(adds, 0, cItems, cLength, addsLength);
			choosedList.setItems(cItems);
			canChooseList.remove(canChooseList.getSelectionIndices());
			for (String add : adds) {
				canChooseModuleInfos.remove(add);
				choosedModuleInfos.put(add, ProjectUtils.validEkpModuleNames.get(add));
			}
			goChoosedBottom();
		}
	}

	private void goChoosedBottom() {
		choosedList.setTopIndex(choosedList.getItemCount() - 1);
	}

	private void handleAddAll() {
		String[] adds = canChooseList.getItems();
		int addsLength = adds.length;
		if (addsLength > 0) {
			String[] cItems = choosedList.getItems();
			int cLength = cItems.length;
			cItems = Arrays.copyOf(cItems, cLength + addsLength);// 数组扩容
			System.arraycopy(adds, 0, cItems, cLength, addsLength);
			choosedList.setItems(cItems);
			canChooseList.setItems(new String[0]);
			for (String add : adds) {
				canChooseModuleInfos.remove(add);
				choosedModuleInfos.put(add, ProjectUtils.validEkpModuleNames.get(add));
			}
			if (!containsCOREModules) {
				coreBtn.setVisible(false);
				containsCOREModules = true;
			}
			goChoosedBottom();
		}
	}

	private void handleDeleteAll() {
		String[] dels = getCanDelItems();
		int delsLength = dels.length;
		if (delsLength > 0) {
			String[] ccItems = canChooseList.getItems();
			int ccLength = ccItems.length;
			ccItems = Arrays.copyOf(ccItems, ccLength + delsLength);// 数组扩容
			System.arraycopy(dels, 0, ccItems, ccLength, delsLength);
			canChooseList.setItems(Utils.sort(ccItems));
			for (String del : dels) {
				canChooseModuleInfos.put(del, ProjectUtils.validEkpModuleNames.get(del));
				choosedModuleInfos.remove(del);
				choosedList.remove(del);
			}
		}
	}

	private String[] getCanDelItems() {
		String[] rtn = null;
		java.util.List<String> canDelsList = new ArrayList<String>();
		String[] all = choosedList.getItems();
		for (String m : all) {
			if (!ProjectUtils.isRequiredModule(m)) {
				canDelsList.add(m);
			}
		}
		rtn = new String[canDelsList.size()];
		canDelsList.toArray(rtn);
		return rtn;
	}

	private void handleDelete() {
		String[] dels = choosedList.getSelection();
		int delsLength = dels.length;
		if (delsLength > 0) {
			String[] ccItems = canChooseList.getItems();
			int ccLength = ccItems.length;
			ccItems = Arrays.copyOf(ccItems, ccLength + delsLength);// 数组扩容
			System.arraycopy(dels, 0, ccItems, ccLength, delsLength);
			canChooseList.setItems(Utils.sort(ccItems));
			choosedList.remove(choosedList.getSelectionIndices());
			for (String del : dels) {
				canChooseModuleInfos.put(del, ProjectUtils.validEkpModuleNames.get(del));
				choosedModuleInfos.remove(del);
			}
		}
	}

	@Override
	protected void createDialogContent(Composite parent) {
		((GridData) createLabel(parent, MessageUtils.getMessage("linker.hint.5"), 0)
				.getLayoutData()).horizontalAlignment = SWT.CENTER;
		((GridData) createLabel(parent, "", 0).getLayoutData()).horizontalAlignment = SWT.CENTER;
		((GridData) createLabel(parent, MessageUtils.getMessage("linker.hint.6"), 0)
				.getLayoutData()).horizontalAlignment = SWT.CENTER;
		createCanChooseModules(parent);
		createBtns(parent);
		createChoosedModules(parent);
		createPrjNamesArea(parent);
	}

	protected void createPrjNamesArea(Composite parent) {
		Composite prjsArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(prjsArea);
		GridDataFactory.swtDefaults().span(3, 0).applyTo(prjsArea);
		((GridData) createLabel(prjsArea, MessageUtils.getMessage("common.hint.target.web"), 0)
				.getLayoutData()).horizontalAlignment = SWT.CENTER;
		super.createPrjNamesArea(prjsArea, ProjectUtils.webProjects, SWT.CHECK);
		createAfterAction(prjsArea);
	}

	private void createAfterAction(Composite prjsArea) {
		((GridData) createLabel(prjsArea, MessageUtils.getMessage("linker.hint.8"), 0)
				.getLayoutData()).horizontalAlignment = SWT.CENTER;
		Composite btns = new Composite(prjsArea, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(btns);
		Button buildBtn = new Button(btns, SWT.CHECK);
		buildBtn.setText(MessageUtils.getMessage("linker.hint.9"));
		buildBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				Button btn = (Button) evt.widget;
				autoBuild = btn.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
	}

	private void createCanChooseModules(Composite parent) {
		canChooseList = createModulseList(parent, MessageUtils.getMessage("linker.hint.12"), canChooseModuleInfos);
		canChooseList.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
			}

			@Override
			public void mouseDown(MouseEvent arg0) {
			}

			@Override
			public void mouseDoubleClick(MouseEvent mevt) {
				handleAdd();
			}
		});
	}

	private void createBtns(Composite parent) {
		GridDataFactory centerFactory = GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER);
		GridDataFactory sizeFactory = GridDataFactory.swtDefaults().hint(90, 26);
		Composite btnsContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(btnsContainer);
		GridDataFactory.swtDefaults().hint(80, getListSize().y).applyTo(btnsContainer);
		centerFactory.applyTo(btnsContainer);
		if (!containsCOREModules) {
			coreBtn = new Button(btnsContainer, SWT.NONE);
			coreBtn.setText(MessageUtils.getMessage("linker.hint.13"));
			coreBtn.setData("iden", "core");
			centerFactory.applyTo(coreBtn);
			sizeFactory.applyTo(coreBtn);
			coreBtn.addSelectionListener(btnHandler);
		}
		Button bt1 = new Button(btnsContainer, SWT.NONE);
		bt1.setText(MessageUtils.getMessage("linker.hint.15"));
		bt1.setData("iden", "add");
		centerFactory.applyTo(bt1);
		sizeFactory.applyTo(bt1);
		bt1.addSelectionListener(btnHandler);
		Button bt2 = new Button(btnsContainer, SWT.NONE);
		bt2.setText(MessageUtils.getMessage("linker.hint.16"));
		bt2.setData("iden", "addAll");
		centerFactory.applyTo(bt2);
		sizeFactory.applyTo(bt2);
		bt2.addSelectionListener(btnHandler);
		Button bt3 = new Button(btnsContainer, SWT.NONE);
		bt3.setText(MessageUtils.getMessage("linker.hint.17"));
		bt3.setData("iden", "deleteAll");
		centerFactory.applyTo(bt3);
		sizeFactory.applyTo(bt3);
		bt3.addSelectionListener(btnHandler);
		Button bt5 = new Button(btnsContainer, SWT.NONE);
		bt5.setText(MessageUtils.getMessage("linker.hint.18"));
		bt5.setData("iden", "delete");
		centerFactory.applyTo(bt5);
		sizeFactory.applyTo(bt5);
		bt5.addSelectionListener(btnHandler);
	}

	private void createChoosedModules(Composite parent) {
		choosedList = createModulseList(parent, MessageUtils.getMessage("linker.hint.19"), choosedModuleInfos);
		choosedList.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent sevt) {
				// 做了处理不能选择必选模块(即：不能删除)
				List list = (List) sevt.widget;
				for (int i : list.getSelectionIndices()) {
					if (ProjectUtils.isRequiredModule(list.getItem(i))) {
						list.deselect(i);
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent sevt) {

			}
		});
		choosedList.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
			}

			@Override
			public void mouseDown(MouseEvent arg0) {
			}

			@Override
			public void mouseDoubleClick(MouseEvent mevt) {
				List list = (List) mevt.widget;
				if (!ProjectUtils.isRequiredModule(list.getItem(list.getSelectionIndex()))) {
					handleDelete();
				}
			}
		});
	}

	private List createModulseList(Composite parent, String title, Map<String, Map<String, Object>> moduleInfos) {
		List list = new List(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		list.setToolTipText(title);
		GridDataFactory.swtDefaults().hint(getListSize()).applyTo(list);
		java.util.List<String> moduleNamesList = Utils.sortEkpModules(moduleInfos.keySet());
		String[] moduleNames = new String[moduleNamesList.size()];
		list.setItems(moduleNamesList.toArray(moduleNames));
		return list;
	}

	private Point getListSize() {
		return new Point(220, 360);
	}

	@Override
	protected String getHintMsg() {
		String rtn = "";
		if (choosedProjectMap.size() == 0) {
			rtn = MessageUtils.getMessage("web.project.empty");
		} else if (!containsCOREModules) {
			rtn = MessageUtils.getMessage("linker.hint.20");
		} else if (isOnlyCoreModules()) {
			if (!MessageDialog.openConfirm(getShell(), MessageUtils.OK, MessageUtils.getMessage("linker.hint.21"))) {
				rtn = "nohint";
			}
		}
		return rtn;
	}

	private boolean isOnlyCoreModules() {
		for (String cm : choosedList.getItems()) {
			if (!ProjectUtils.isRequiredModule(cm)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected int getColNum() {
		return 3;
	}

	@Override
	public void beforeDialogOpen() throws Throwable {
		super.beforeDialogOpen();
		autoBuild = false;
		initModulesInfo();
	}

	private void initModulesInfo() {
		containsCOREModules = ProjectUtils.savedModulesMap.size() > 0;
		choosedModuleInfos = new HashMap<String, Map<String, Object>>();
		canChooseModuleInfos = new HashMap<String, Map<String, Object>>();
		for (String prjN : ProjectUtils.validEkpModuleNames.keySet()) {
			if (ProjectUtils.savedModulesMap.containsKey(prjN)) {
				choosedModuleInfos.put(prjN, ProjectUtils.savedModulesMap.get(prjN));
			} else {
				canChooseModuleInfos.put(prjN, ProjectUtils.validEkpModuleNames.get(prjN));
			}
		}
	}
}