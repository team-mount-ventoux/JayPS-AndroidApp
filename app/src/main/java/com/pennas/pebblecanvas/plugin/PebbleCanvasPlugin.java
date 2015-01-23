package com.pennas.pebblecanvas.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.util.Log;

public abstract class PebbleCanvasPlugin extends BroadcastReceiver {
	private static final int INTERFACE_VERSION = 2;
	
	private static final String ABS_LOG_TAG = "CANV_PLUG";
	// canvas -> plugins
	public static final String CANVAS_ACTION_REQUEST_DEFINITIONS = "com.pennas.pebblecanvas.plugin.REQUEST_DEFINITIONS";
	public static final String CANVAS_ACTION_REQUEST_UPDATE = "com.pennas.pebblecanvas.plugin.REQUEST_UPDATE";
	// plugins -> canvas
	public static final String CANVAS_ACTION_DEFINITION = "com.pennas.pebblecanvas.plugin.DEFINITION";
	public static final String CANVAS_ACTION_UPDATE = "com.pennas.pebblecanvas.plugin.UPDATE";
	public static final String CANVAS_ACTION_NOTIFY_UPDATE = "com.pennas.pebblecanvas.plugin.NOTIFY_UPDATE";
	public static final String CANVAS_ACTION_SHOW_SCREEN = "com.pennas.pebblecanvas.plugin.SHOW_SCREEN";
	
	// definition fields
	public static final String CANVAS_DEFINITION_ID = "ID";
	public static final String CANVAS_DEFINITION_NAME = "NAME";
	public static final String CANVAS_DEFINITION_PACKAGE = "PACKAGE";
	public static final String CANVAS_DEFINITION_INTERFACE_VERSION = "INTERFACE_VERSION";
	public static final String CANVAS_DEFINITION_PLUGIN_VERSION = "PLUGIN_VERSION";
	public static final String CANVAS_DEFINITION_TYPE = "TYPE";
	public static final String CANVAS_DEFINITION_FORMAT_MASKS = "FORMAT_MASKS";
	public static final String CANVAS_DEFINITION_FORMAT_DESCS = "FORMAT_DESCS";
	public static final String CANVAS_DEFINITION_FORMAT_EXAMPLES = "FORMAT_EXAMPLES";
	public static final String CANVAS_DEFINITION_DEFAULT_FORMAT_STRING = "FORMAT_DEFAULT";
	public static final String CANVAS_DEFINITION_PARAMS_DESC = "PARAMS_DESC";
	
	// value fields
	public static final String CANVAS_VALUE_FORMAT_MASKS = "FORMAT_MASK";
	public static final String CANVAS_VALUE_FORMAT_MASK_VALUES = "MASK_VALUES";
	public static final String CANVAS_VALUE_IMAGE = "IMAGE";
	public static final String CANVAS_VALUE_SCREEN_NAME = "SCREEN_NAME";
	public static final String CANVAS_VALUE_IMAGE_PARAMS = "IMAGE_PARAMS";
	
	// plugin types
	public static final int TYPE_TEXT = 1;
	public static final int TYPE_IMAGE = 2;
	
	private static final String PEBBLE_CANVAS_PACKAGE = "com.pennas.pebblecanvas";
	private static final String PEBBLE_CANVAS_PLUGIN_RECEIVER = PEBBLE_CANVAS_PACKAGE + ".plugin.PluginReceiver";
	
	public static final int NO_VALUE = -999;
	private static ArrayList<PluginDefinition> stored_defs;
	
	@Override
	/**
	 * BroadcastReceiver which will receive messages from Canvas, and process them before calling the plugin callback methods as required
	 */
	public final void onReceive(Context context, Intent intent) {
		//Log.i(ABS_LOG_TAG, "onReceive: " + intent.getAction());
		// Canvas requested definitions - send them
		if (intent.getAction().equals(CANVAS_ACTION_REQUEST_DEFINITIONS)) {
			Log.i(ABS_LOG_TAG, "defs");
			if (stored_defs == null) {
				stored_defs = get_plugin_definitions(context);
			}
			if (stored_defs == null) return;
			for (PluginDefinition def : stored_defs) {
				send_definition(def, context);
			}
		// Canvas requested values for a specific plugin - send them
		} else if (intent.getAction().equals(CANVAS_ACTION_REQUEST_UPDATE)) {
			Log.i(ABS_LOG_TAG, "update");
			String pkg = intent.getStringExtra(CANVAS_DEFINITION_PACKAGE);
			if (pkg == null) return;
			if (!pkg.equals(context.getPackageName())) return;
			
			if (stored_defs == null) {
				Log.i(ABS_LOG_TAG, "call get_plugin_definitions");
				stored_defs = get_plugin_definitions(context);
			}
			if (stored_defs == null) {
				Log.i(ABS_LOG_TAG, "stored_defs == null");
				return;
			}
			
			// which id to get value of?
			
			int def_id = intent.getIntExtra(CANVAS_DEFINITION_ID, NO_VALUE);
			if (def_id == NO_VALUE) {
				Log.i(ABS_LOG_TAG, "def_id == NO_VALUE");
				return;
			}
			
			for (PluginDefinition def : stored_defs) {
				if (def.id == def_id) {
					if (def instanceof TextPluginDefinition) {
						// which format masks are required?
						ArrayList<String> format_masks = intent.getStringArrayListExtra(CANVAS_VALUE_FORMAT_MASKS);
						if (format_masks == null) return;
						
						send_value_string(def_id, format_masks, context);
					} else if (def instanceof ImagePluginDefinition) {
						String params = intent.getStringExtra(CANVAS_VALUE_IMAGE_PARAMS);
						send_value_image(def_id, context, params);
					}
					break;
				} // def id
			} // for
		}
	}
	
	private final void send_definition(PluginDefinition def, Context context) {
		Log.i(ABS_LOG_TAG, "send_definition: " + def.id);
		final Intent intent = new Intent(CANVAS_ACTION_DEFINITION);
		intent.putExtra(CANVAS_DEFINITION_ID, def.id);
		intent.putExtra(CANVAS_DEFINITION_NAME, def.name);
		intent.putExtra(CANVAS_DEFINITION_PACKAGE, context.getPackageName());
		intent.putExtra(CANVAS_DEFINITION_INTERFACE_VERSION, INTERFACE_VERSION);
		intent.putExtra(CANVAS_DEFINITION_PARAMS_DESC, def.params_description);
		
		PackageManager manager = context.getPackageManager();
		PackageInfo info;
		String version = null;
		try {
			info = manager.getPackageInfo(context.getPackageName(), 0);
			version = info.versionName;
		} catch (NameNotFoundException e) { /* */ }
		intent.putExtra(CANVAS_DEFINITION_PLUGIN_VERSION, version);
		
		if (def instanceof TextPluginDefinition) {
        	intent.putExtra(CANVAS_DEFINITION_TYPE, TYPE_TEXT);
        	TextPluginDefinition text_def = (TextPluginDefinition) def;
        	intent.putExtra(CANVAS_DEFINITION_FORMAT_MASKS, text_def.format_masks);
        	intent.putExtra(CANVAS_DEFINITION_FORMAT_DESCS, text_def.format_mask_descriptions);
        	intent.putExtra(CANVAS_DEFINITION_FORMAT_EXAMPLES, text_def.format_mask_examples);
        	intent.putExtra(CANVAS_DEFINITION_DEFAULT_FORMAT_STRING, text_def.default_format_string);
        } else if (def instanceof ImagePluginDefinition) {
        	intent.putExtra(CANVAS_DEFINITION_TYPE, TYPE_IMAGE);
        }
        intent.setClassName(PEBBLE_CANVAS_PACKAGE, PEBBLE_CANVAS_PLUGIN_RECEIVER);
        context.sendBroadcast(intent);
	}
	
	private static final Pattern pat_param = Pattern.compile("(%.+?)#(.+?)#");
	private final void send_value_string(int def_id, ArrayList<String> format_masks, Context context) {
		Log.i(ABS_LOG_TAG, "send_value_string: " + def_id);
		ArrayList<String> value_items = new ArrayList<String>();
		for (String mask : format_masks) {
			String params = null;
			Matcher match = pat_param.matcher(mask);
			if (match.find()) {
				mask = match.group(1);
				params = match.group(2);
			}
			value_items.add(get_format_mask_value(def_id, mask, context, params));
		}
		
		final Intent intent = new Intent(CANVAS_ACTION_UPDATE);
		intent.putExtra(CANVAS_DEFINITION_ID, def_id);
		intent.putExtra(CANVAS_DEFINITION_PACKAGE, context.getPackageName());
		intent.putExtra(CANVAS_VALUE_FORMAT_MASKS, format_masks);
		intent.putExtra(CANVAS_VALUE_FORMAT_MASK_VALUES, value_items);
		intent.setClassName(PEBBLE_CANVAS_PACKAGE, PEBBLE_CANVAS_PLUGIN_RECEIVER);
        context.sendBroadcast(intent);
	}
	
	private static int filename_i = 0;
	private static final int NUM_FILES = 5;
	private static final String FILENAME_PREFIX = "img_tmp_";
	
	private final void send_value_image(int def_id, Context context, String params) {
		final Intent intent = new Intent(CANVAS_ACTION_UPDATE);
		intent.putExtra(CANVAS_DEFINITION_ID, def_id);
		intent.putExtra(CANVAS_DEFINITION_PACKAGE, context.getPackageName());
		intent.setClassName(PEBBLE_CANVAS_PACKAGE, PEBBLE_CANVAS_PLUGIN_RECEIVER);
        Bitmap b = get_bitmap_value(def_id, context, params);
		
		filename_i++;
		if (filename_i >= NUM_FILES) {
			filename_i = 0;
		}
		
		if (b == null) {
			context.sendBroadcast(intent);
		} else {
			// store on shared storage; don't send directly in intent
			File f = new File(context.getExternalFilesDir(null), FILENAME_PREFIX + filename_i);
			f.delete();
			FileOutputStream fOut = null;
			try {
				fOut = new FileOutputStream(f);
				b.compress(Bitmap.CompressFormat.PNG, 85, fOut);
			    fOut.flush();
			    
			    Log.i(ABS_LOG_TAG, "send_value_image: " + def_id + " / " + f.getAbsolutePath());
				intent.putExtra(CANVAS_VALUE_IMAGE, f.getAbsolutePath());
				context.sendBroadcast(intent);
			} catch (FileNotFoundException e) {
				Log.i(ABS_LOG_TAG, e.toString());
			} catch (IOException e) {
				Log.i(ABS_LOG_TAG, e.toString());
			} finally {
				try {
					if (fOut != null) {
						fOut.close();
					}
				} catch (IOException e) { /* */ }
			}
		}
	}
	
	/**
	 * Abstract class representing a plugin definition. Many of these may be provided by a single plugin application (or just one)
	 * 
	 * Plugins must define each PluginDefinition using a concrete implementation: either {@link TextPluginDefinition} or {@link ImagePluginDefinition}
	 */
	public abstract class PluginDefinition {
		/**
		 * Identifier (to uniquely identify multiple plugins from the same app)
		 */
		public int id;
		/**
		 * Name, as presented to the Canvas user in the Plugins dropdown
		 */
		public String name;
		/**
		 * Optional (leave null if not required). Description of parameter for user to enter in Canvas editor for this plugin
		 */
		public String params_description;
	}
	
	/**
	 * Class representing a text plugin definition.
	 * 
	 * format_mask_examples is optional, but all other fields are mandatory
	 * 
	 * All ArrayList fields should contain the same number of elements
	 */
	public final class TextPluginDefinition extends PluginDefinition {
		/**
		 * List of format masks provided by the plugin, each in the format %m
		 */
		public ArrayList<String> format_masks;
		/**
		 * Description of format masks provided by the plugin, to be presented to user iun the format mask editor dialog
		 */
		public ArrayList<String> format_mask_descriptions;
		/**
		 * Optional: initial value for each format mask, to be used by Canvas if no data has been received yet for each mask
		 */
		public ArrayList<String> format_mask_examples;
		/**
		 * Default format string to be populated in Canvas editor when the user creates a layer using this plugin.
		 * May use more than one format mask and static text, e.g. "SMS: %S Missed: %M"
		 */
		public String default_format_string;
	}
	
	/**
	 * Abstract class representing an image plugin definition.
	 */
	public final class ImagePluginDefinition extends PluginDefinition {
		// no extra fields
	}
	
	/**
	 * Notify Canvas that updates are available for the specified plugin def ID
	 * 
	 * @param def_id Plugin ID for which update is available
	 * @param context Calling context
	 */
	public static final void notify_canvas_updates_available(int def_id, Context context) {
		Log.i(ABS_LOG_TAG, "notify_canvas_updates_available: " + def_id);
		if (context == null) return;
		
		final Intent intent = new Intent(CANVAS_ACTION_NOTIFY_UPDATE);
		intent.putExtra(CANVAS_DEFINITION_ID, def_id);
		intent.putExtra(CANVAS_DEFINITION_PACKAGE, context.getPackageName());
		intent.setClassName(PEBBLE_CANVAS_PACKAGE, PEBBLE_CANVAS_PLUGIN_RECEIVER);
        context.sendBroadcast(intent);
	}
	
	/**
	 * Request that Canvas displays this screen on the connected Pebble
	 * 
	 * @param screen_name Name of the screen to show
	 * @param context Calling context
	 */
	public static final void show_screen(String screen_name, Context context) {
		Log.i(ABS_LOG_TAG, "show_screen: '" + screen_name + "'");
		if (context == null) return;
		if ( (screen_name == null) || (screen_name.length() == 0) ) return;
		
		final Intent intent = new Intent(CANVAS_ACTION_SHOW_SCREEN);
		intent.putExtra(CANVAS_VALUE_SCREEN_NAME, screen_name);
		intent.setClassName(PEBBLE_CANVAS_PACKAGE, PEBBLE_CANVAS_PLUGIN_RECEIVER);
        context.sendBroadcast(intent);
	}
	
	//
	// methods which must be overridden in subclass to provide plugin functionality:-
	//
	
	/**
	 * Canvas requests definitions from this plugin
	 * 
	 * @param context Calling context
	 * 
	 * @return A list of {@link PluginDefinition} instances (may only be one)
	 */
	protected abstract ArrayList<PluginDefinition> get_plugin_definitions(Context context);
	/**
	 * Canvas requests the current text value for a specific format mask
	 * 
	 * @param def_id ID of plugin definition which is being queried
	 * @param format_mask Mask to return current value of
	 * @param context Calling context
	 * @param param User-entered parameter (if specified by plugin definition). Plugins should deal gracefully with this being null/empty
	 * 
	 * @return The current String value of the format mask
	 */
	protected abstract String get_format_mask_value(int def_id, String format_mask, Context context, String param);
	/**
	 * Canvas requests the current bitmap for this plugin
	 * 
	 * @param def_id ID of plugin definition which is being queried
	 * @param context Calling context
	 * @param param User-entered parameter (if specified by plugin definition). Plugins should deal gracefully with this being null/empty
	 * 
	 * @return The current image value (Bitmap) of the format mask
	 */
	protected abstract Bitmap get_bitmap_value(int def_id, Context context, String param);
}
