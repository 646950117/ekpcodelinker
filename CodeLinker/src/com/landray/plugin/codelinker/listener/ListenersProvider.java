package com.landray.plugin.codelinker.listener;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

public final class ListenersProvider {
	public static String RESOURCE_CHANGE = "resourcechange";
	private static IResourceChangeListener resourceChangeListener;

	static {
		resourceChangeListener = new ResourceChangeListener();
	}

	public static void addListeners() {
		addResourceChangeListener();
	}

	public static void removeListeners() {
		removeResourceChangeListener();
	}

	public static void addListener(String eventType) {
		if (RESOURCE_CHANGE.equals(eventType)) {
			addResourceChangeListener();
		}
	}

	private static void addResourceChangeListener() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
				IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);
	}

	public static void removeListener(String eventType) {
		if (RESOURCE_CHANGE.equals(eventType)) {
			removeResourceChangeListener();
		}
	}

	private static void removeResourceChangeListener() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
	}
}