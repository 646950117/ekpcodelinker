package com.landray.plugin.codelinker.common;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landray.plugin.codelinker.log.LinkerLogger;

public class MessageUtils {

	public static void main(String[] args) {
	}

	private static ResourceBundle resourceBundle = null;

	static {
		resourceBundle = ResourceBundle.getBundle("com/landray/plugin/codelinker/messages");
	}

	public static String getMessage(String key) {
		String msg = "";
		try {
			msg = resourceBundle.getString(key);
		} catch (Throwable e) {
			if (e instanceof MissingResourceException) {
				LinkerLogger.log(getParamMessage("bundle.key.empty", key), "file");
			}
		}
		return msg;
	}

	public static String getParamMessage(String msgKey, String... paramValues) {
		StringBuffer rtnBuffer = new StringBuffer();
		String paramMsg = getMessage(msgKey);
		if (paramValues != null && paramValues.length > 0) {
			Matcher m = Pattern.compile("\\{\\d+\\}").matcher(paramMsg);
			int i = 0;
			while (m.find()) {
				String pv = "";
				if (i < paramValues.length) {
					pv = paramValues[i];
				}
				m.appendReplacement(rtnBuffer, pv);
				i++;
			}
			m.appendTail(rtnBuffer);
		}
		return rtnBuffer.toString();
	}

	public static final String OK = getMessage("btn.ok");
	public static final String CANCEL = getMessage("btn.cancel");
	public static final String HINT_INFO = getMessage("hint.info");
	protected static final String HINT_ERR = getMessage("hint.err");
	public static final String DAILOG_TITLE_LINKER = getMessage("dialog.title.linker");
	public static final String DAILOG_TITLE_SYNC = getMessage("dialog.title.sync");
	public static final String DAILOG_TITLE_CLEAR = getMessage("dialog.title.clear");
	public static final String DAILOG_TITLE_UNUSEFUL = getMessage("dialog.title.unuseful");
}