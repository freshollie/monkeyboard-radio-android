package com.freshollie.radioapp;


import anywheresoftware.b4a.B4AMenuItem;
import android.app.Activity;
import android.os.Bundle;
import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BALayout;
import anywheresoftware.b4a.B4AActivity;
import anywheresoftware.b4a.ObjectWrapper;
import anywheresoftware.b4a.objects.ActivityWrapper;
import java.lang.reflect.InvocationTargetException;
import anywheresoftware.b4a.B4AUncaughtException;
import anywheresoftware.b4a.debug.*;
import java.lang.ref.WeakReference;

public class main extends Activity implements B4AActivity{
	public static main mostCurrent;
	static boolean afterFirstLayout;
	static boolean isFirst = true;
    private static boolean processGlobalsRun = false;
	BALayout layout;
	public static BA processBA;
	BA activityBA;
    ActivityWrapper _activity;
    java.util.ArrayList<B4AMenuItem> menuItems;
	public static final boolean fullScreen = false;
	public static final boolean includeTitle = false;
    public static WeakReference<Activity> previousOne;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFirst) {
			processBA = new BA(this.getApplicationContext(), null, null, "com.freshollie.radioapp", "com.freshollie.radioapp.main");
			processBA.loadHtSubs(this.getClass());
	        float deviceScale = getApplicationContext().getResources().getDisplayMetrics().density;
	        BALayout.setDeviceScale(deviceScale);
            
		}
		else if (previousOne != null) {
			Activity p = previousOne.get();
			if (p != null && p != this) {
                BA.LogInfo("Killing previous instance (main).");
				p.finish();
			}
		}
        processBA.runHook("oncreate", this, null);
		if (!includeTitle) {
        	this.getWindow().requestFeature(android.view.Window.FEATURE_NO_TITLE);
        }
        if (fullScreen) {
        	getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,   
        			android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
		mostCurrent = this;
        processBA.sharedProcessBA.activityBA = null;
		layout = new BALayout(this);
		setContentView(layout);
		afterFirstLayout = false;
		BA.handler.postDelayed(new WaitForLayout(), 5);

	}
	private static class WaitForLayout implements Runnable {
		public void run() {
			if (afterFirstLayout)
				return;
			if (mostCurrent == null)
				return;
            
			if (mostCurrent.layout.getWidth() == 0) {
				BA.handler.postDelayed(this, 5);
				return;
			}
			mostCurrent.layout.getLayoutParams().height = mostCurrent.layout.getHeight();
			mostCurrent.layout.getLayoutParams().width = mostCurrent.layout.getWidth();
			afterFirstLayout = true;
			mostCurrent.afterFirstLayout();
		}
	}
	private void afterFirstLayout() {
        if (this != mostCurrent)
			return;
		activityBA = new BA(this, layout, processBA, "com.freshollie.radioapp", "com.freshollie.radioapp.main");
        
        processBA.sharedProcessBA.activityBA = new java.lang.ref.WeakReference<BA>(activityBA);
        anywheresoftware.b4a.objects.ViewWrapper.lastId = 0;
        _activity = new ActivityWrapper(activityBA, "activity");
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        if (BA.isShellModeRuntimeCheck(processBA)) {
			if (isFirst)
				processBA.raiseEvent2(null, true, "SHELL", false);
			processBA.raiseEvent2(null, true, "CREATE", true, "com.freshollie.radioapp.main", processBA, activityBA, _activity, anywheresoftware.b4a.keywords.Common.Density, mostCurrent);
			_activity.reinitializeForShell(activityBA, "activity");
		}
        initializeProcessGlobals();		
        initializeGlobals();
        
        BA.LogInfo("** Activity (main) Create, isFirst = " + isFirst + " **");
        processBA.raiseEvent2(null, true, "activity_create", false, isFirst);
		isFirst = false;
		if (this != mostCurrent)
			return;
        processBA.setActivityPaused(false);
        BA.LogInfo("** Activity (main) Resume **");
        processBA.raiseEvent(null, "activity_resume");
        if (android.os.Build.VERSION.SDK_INT >= 11) {
			try {
				android.app.Activity.class.getMethod("invalidateOptionsMenu").invoke(this,(Object[]) null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	public void addMenuItem(B4AMenuItem item) {
		if (menuItems == null)
			menuItems = new java.util.ArrayList<B4AMenuItem>();
		menuItems.add(item);
	}
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		super.onCreateOptionsMenu(menu);
        try {
            if (processBA.subExists("activity_actionbarhomeclick")) {
                Class.forName("android.app.ActionBar").getMethod("setHomeButtonEnabled", boolean.class).invoke(
                    getClass().getMethod("getActionBar").invoke(this), true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (processBA.runHook("oncreateoptionsmenu", this, new Object[] {menu}))
            return true;
		if (menuItems == null)
			return false;
		for (B4AMenuItem bmi : menuItems) {
			android.view.MenuItem mi = menu.add(bmi.title);
			if (bmi.drawable != null)
				mi.setIcon(bmi.drawable);
            if (android.os.Build.VERSION.SDK_INT >= 11) {
				try {
                    if (bmi.addToBar) {
				        android.view.MenuItem.class.getMethod("setShowAsAction", int.class).invoke(mi, 1);
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			mi.setOnMenuItemClickListener(new B4AMenuItemsClickListener(bmi.eventName.toLowerCase(BA.cul)));
		}
        
		return true;
	}   
 @Override
 public boolean onOptionsItemSelected(android.view.MenuItem item) {
    if (item.getItemId() == 16908332) {
        processBA.raiseEvent(null, "activity_actionbarhomeclick");
        return true;
    }
    else
        return super.onOptionsItemSelected(item); 
}
@Override
 public boolean onPrepareOptionsMenu(android.view.Menu menu) {
    super.onPrepareOptionsMenu(menu);
    processBA.runHook("onprepareoptionsmenu", this, new Object[] {menu});
    return true;
    
 }
 protected void onStart() {
    super.onStart();
    processBA.runHook("onstart", this, null);
}
 protected void onStop() {
    super.onStop();
    processBA.runHook("onstop", this, null);
}
    public void onWindowFocusChanged(boolean hasFocus) {
       super.onWindowFocusChanged(hasFocus);
       if (processBA.subExists("activity_windowfocuschanged"))
           processBA.raiseEvent2(null, true, "activity_windowfocuschanged", false, hasFocus);
    }
	private class B4AMenuItemsClickListener implements android.view.MenuItem.OnMenuItemClickListener {
		private final String eventName;
		public B4AMenuItemsClickListener(String eventName) {
			this.eventName = eventName;
		}
		public boolean onMenuItemClick(android.view.MenuItem item) {
			processBA.raiseEvent(item.getTitle(), eventName + "_click");
			return true;
		}
	}
    public static Class<?> getObject() {
		return main.class;
	}
    private Boolean onKeySubExist = null;
    private Boolean onKeyUpSubExist = null;
	@Override
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		if (onKeySubExist == null)
			onKeySubExist = processBA.subExists("activity_keypress");
		if (onKeySubExist) {
			if (keyCode == anywheresoftware.b4a.keywords.constants.KeyCodes.KEYCODE_BACK &&
					android.os.Build.VERSION.SDK_INT >= 18) {
				HandleKeyDelayed hk = new HandleKeyDelayed();
				hk.kc = keyCode;
				BA.handler.post(hk);
				return true;
			}
			else {
				boolean res = new HandleKeyDelayed().runDirectly(keyCode);
				if (res)
					return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	private class HandleKeyDelayed implements Runnable {
		int kc;
		public void run() {
			runDirectly(kc);
		}
		public boolean runDirectly(int keyCode) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keypress", false, keyCode);
			if (res == null || res == true) {
                return true;
            }
            else if (keyCode == anywheresoftware.b4a.keywords.constants.KeyCodes.KEYCODE_BACK) {
				finish();
				return true;
			}
            return false;
		}
		
	}
    @Override
	public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
		if (onKeyUpSubExist == null)
			onKeyUpSubExist = processBA.subExists("activity_keyup");
		if (onKeyUpSubExist) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keyup", false, keyCode);
			if (res == null || res == true)
				return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	@Override
	public void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
		this.setIntent(intent);
        processBA.runHook("onnewintent", this, new Object[] {intent});
	}
    @Override 
	public void onPause() {
		super.onPause();
        if (_activity == null) //workaround for emulator bug (Issue 2423)
            return;
		anywheresoftware.b4a.Msgbox.dismiss(true);
        BA.LogInfo("** Activity (main) Pause, UserClosed = " + activityBA.activity.isFinishing() + " **");
        processBA.raiseEvent2(_activity, true, "activity_pause", false, activityBA.activity.isFinishing());		
        processBA.setActivityPaused(true);
        mostCurrent = null;
        if (!activityBA.activity.isFinishing())
			previousOne = new WeakReference<Activity>(this);
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        processBA.runHook("onpause", this, null);
	}

	@Override
	public void onDestroy() {
        super.onDestroy();
		previousOne = null;
        processBA.runHook("ondestroy", this, null);
	}
    @Override 
	public void onResume() {
		super.onResume();
        mostCurrent = this;
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        if (activityBA != null) { //will be null during activity create (which waits for AfterLayout).
        	ResumeMessage rm = new ResumeMessage(mostCurrent);
        	BA.handler.post(rm);
        }
        processBA.runHook("onresume", this, null);
	}
    private static class ResumeMessage implements Runnable {
    	private final WeakReference<Activity> activity;
    	public ResumeMessage(Activity activity) {
    		this.activity = new WeakReference<Activity>(activity);
    	}
		public void run() {
			if (mostCurrent == null || mostCurrent != activity.get())
				return;
			processBA.setActivityPaused(false);
            BA.LogInfo("** Activity (main) Resume **");
		    processBA.raiseEvent(mostCurrent._activity, "activity_resume", (Object[])null);
		}
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	      android.content.Intent data) {
		processBA.onActivityResult(requestCode, resultCode, data);
        processBA.runHook("onactivityresult", this, new Object[] {requestCode, resultCode});
	}
	private static void initializeGlobals() {
		processBA.raiseEvent2(null, true, "globals", false, (Object[])null);
	}

public anywheresoftware.b4a.keywords.Common __c = null;
public static anywheresoftware.b4a.objects.preferenceactivity.PreferenceManager _manager = null;
public static anywheresoftware.b4a.objects.preferenceactivity.PreferenceScreenWrapper _screen = null;
public static edsmith.click.sound.ClickSound _myclick = null;
public static anywheresoftware.b4a.objects.collections.List _volumerange = null;
public static com.rootsoft.broadcastreceiver.BroadCastReceiver _broadcastreceiver = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnexit = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnconfig = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnlevel = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnmute = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnfmup = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnfmdn = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnvolup = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnvoldn = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnfmhi = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnfmlo = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnkey = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btnst1 = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btnst2 = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btnst3 = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btnst4 = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btnst5 = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btnst6 = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btnfm = null;
public anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper _btndab = null;
public anywheresoftware.b4a.objects.PanelWrapper _panmain = null;
public anywheresoftware.b4a.objects.PanelWrapper _pankeyboard = null;
public anywheresoftware.b4a.objects.PanelWrapper _panselect = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnenter = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btnclear = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn1 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn2 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn3 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn4 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn5 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn6 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn7 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn8 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn9 = null;
public anywheresoftware.b4a.objects.ButtonWrapper _btn0 = null;
public anywheresoftware.b4a.objects.LabelWrapper _labfreq = null;
public anywheresoftware.b4a.objects.LabelWrapper _labvolume = null;
public anywheresoftware.b4a.objects.LabelWrapper _labstrength = null;
public anywheresoftware.b4a.objects.LabelWrapper _labprogramtype = null;
public anywheresoftware.b4a.objects.LabelWrapper _labprogram = null;
public anywheresoftware.b4a.objects.LabelWrapper _labprogram2 = null;
public anywheresoftware.b4a.objects.LabelWrapper _labprogramtext = null;
public anywheresoftware.b4a.objects.LabelWrapper _labevent = null;
public anywheresoftware.b4a.objects.LabelWrapper _labdatarate = null;
public anywheresoftware.b4a.objects.LabelWrapper _labstereomode = null;
public anywheresoftware.b4a.objects.ListViewWrapper _lvdab = null;
public anywheresoftware.b4a.objects.ProgressBarWrapper _pbstrength = null;
public wal.INIFiles.ini _ini = null;
public com.freshollie.radioapp.radioservice _radioservice = null;
public com.freshollie.radioapp.slideshow _slideshow = null;

public static boolean isAnyActivityVisible() {
    boolean vis = false;
vis = vis | (main.mostCurrent != null);
return vis;}
public static String  _activity_create(boolean _firsttime) throws Exception{
 //BA.debugLineNum = 438;BA.debugLine="Sub Activity_Create(FirstTime As Boolean)";
 //BA.debugLineNum = 440;BA.debugLine="If GetDeviceLayoutValues.Width > GetDeviceLayoutV";
if (anywheresoftware.b4a.keywords.Common.GetDeviceLayoutValues(mostCurrent.activityBA).Width>anywheresoftware.b4a.keywords.Common.GetDeviceLayoutValues(mostCurrent.activityBA).Height) { 
 //BA.debugLineNum = 441;BA.debugLine="Activity.LoadLayout(\"Landscape\")";
mostCurrent._activity.LoadLayout("Landscape",mostCurrent.activityBA);
 }else {
 //BA.debugLineNum = 443;BA.debugLine="Activity.LoadLayout(\"Portrait\")";
mostCurrent._activity.LoadLayout("Portrait",mostCurrent.activityBA);
 };
 //BA.debugLineNum = 446;BA.debugLine="If FirstTime Then";
if (_firsttime) { 
 //BA.debugLineNum = 448;BA.debugLine="If manager.GetAll.Size = 0 Then SetDefaults";
if (_manager.GetAll().getSize()==0) { 
_setdefaults();};
 //BA.debugLineNum = 449;BA.debugLine="If manager.GetString(\"DuckVolume\") = \"\" Or manag";
if ((_manager.GetString("DuckVolume")).equals("") || (_manager.GetString("DefaultVolume")).equals("")) { 
_setdefaults();};
 //BA.debugLineNum = 451;BA.debugLine="VolumeRange.Initialize";
_volumerange.Initialize();
 //BA.debugLineNum = 452;BA.debugLine="VolumeRange.AddAll(Array As String(\"0\", \"1\", \"2\"";
_volumerange.AddAll(anywheresoftware.b4a.keywords.Common.ArrayToList(new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"}));
 //BA.debugLineNum = 453;BA.debugLine="myclick.Initialize";
_myclick.Initialize(processBA);
 //BA.debugLineNum = 454;BA.debugLine="CreatePreferenceScreen";
_createpreferencescreen();
 //BA.debugLineNum = 455;BA.debugLine="RadioService.DuckVolume = manager.GetString(\"Duc";
mostCurrent._radioservice._duckvolume = (int)(Double.parseDouble(_manager.GetString("DuckVolume")));
 //BA.debugLineNum = 456;BA.debugLine="RadioService.DefaultVolume = manager.GetString(\"";
mostCurrent._radioservice._defaultvolume = (int)(Double.parseDouble(_manager.GetString("DefaultVolume")));
 //BA.debugLineNum = 457;BA.debugLine="If RadioService.ServiceStarted <> True Then";
if (mostCurrent._radioservice._servicestarted!=anywheresoftware.b4a.keywords.Common.True) { 
 //BA.debugLineNum = 458;BA.debugLine="StartService(RadioService)";
anywheresoftware.b4a.keywords.Common.StartService(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()));
 }else {
 //BA.debugLineNum = 460;BA.debugLine="updateStationList";
_updatestationlist();
 };
 };
 //BA.debugLineNum = 464;BA.debugLine="End Sub";
return "";
}
public static boolean  _activity_keypress(int _keycode) throws Exception{
 //BA.debugLineNum = 485;BA.debugLine="Sub Activity_KeyPress (KeyCode As Int) As Boolean";
 //BA.debugLineNum = 486;BA.debugLine="Select KeyCode";
switch (BA.switchObjectToInt(_keycode,anywheresoftware.b4a.keywords.Common.KeyCodes.KEYCODE_VOLUME_UP,anywheresoftware.b4a.keywords.Common.KeyCodes.KEYCODE_VOLUME_DOWN)) {
case 0:
 //BA.debugLineNum = 488;BA.debugLine="btnVolUp_Click";
_btnvolup_click();
 //BA.debugLineNum = 489;BA.debugLine="Return True";
if (true) return anywheresoftware.b4a.keywords.Common.True;
 break;
case 1:
 //BA.debugLineNum = 491;BA.debugLine="btnVolDn_Click";
_btnvoldn_click();
 //BA.debugLineNum = 492;BA.debugLine="Return True";
if (true) return anywheresoftware.b4a.keywords.Common.True;
 break;
default:
 //BA.debugLineNum = 494;BA.debugLine="Return False";
if (true) return anywheresoftware.b4a.keywords.Common.False;
 break;
}
;
 //BA.debugLineNum = 496;BA.debugLine="End Sub";
return false;
}
public static String  _activity_pause(boolean _userclosed) throws Exception{
 //BA.debugLineNum = 481;BA.debugLine="Sub Activity_Pause (UserClosed As Boolean)";
 //BA.debugLineNum = 482;BA.debugLine="End Sub";
return "";
}
public static String  _activity_resume() throws Exception{
 //BA.debugLineNum = 466;BA.debugLine="Sub Activity_Resume";
 //BA.debugLineNum = 467;BA.debugLine="RadioService.DuckVolume = manager.GetString(\"Duck";
mostCurrent._radioservice._duckvolume = (int)(Double.parseDouble(_manager.GetString("DuckVolume")));
 //BA.debugLineNum = 468;BA.debugLine="RadioService.DefaultVolume = manager.GetString(\"D";
mostCurrent._radioservice._defaultvolume = (int)(Double.parseDouble(_manager.GetString("DefaultVolume")));
 //BA.debugLineNum = 470;BA.debugLine="If RadioService.Mute = False And RadioService.Duc";
if (mostCurrent._radioservice._mute==anywheresoftware.b4a.keywords.Common.False && mostCurrent._radioservice._ducked==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 471;BA.debugLine="CallSub2(RadioService, \"SetVolume\", RadioService";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SetVolume",(Object)(mostCurrent._radioservice._defaultvolume));
 }else {
 //BA.debugLineNum = 473;BA.debugLine="RadioService.LastVolume = RadioService.DefaultVo";
mostCurrent._radioservice._lastvolume = mostCurrent._radioservice._defaultvolume;
 };
 //BA.debugLineNum = 476;BA.debugLine="If RadioService.ServiceStarted Then";
if (mostCurrent._radioservice._servicestarted) { 
 //BA.debugLineNum = 477;BA.debugLine="updateStationList";
_updatestationlist();
 };
 //BA.debugLineNum = 479;BA.debugLine="End Sub";
return "";
}
public static String  _btn0_click() throws Exception{
 //BA.debugLineNum = 101;BA.debugLine="Sub btn0_Click";
 //BA.debugLineNum = 102;BA.debugLine="ClickNumber(\"0\")";
_clicknumber("0");
 //BA.debugLineNum = 103;BA.debugLine="End Sub";
return "";
}
public static String  _btn1_click() throws Exception{
 //BA.debugLineNum = 105;BA.debugLine="Sub btn1_Click";
 //BA.debugLineNum = 106;BA.debugLine="ClickNumber(\"1\")";
_clicknumber("1");
 //BA.debugLineNum = 107;BA.debugLine="End Sub";
return "";
}
public static String  _btn2_click() throws Exception{
 //BA.debugLineNum = 109;BA.debugLine="Sub btn2_Click";
 //BA.debugLineNum = 110;BA.debugLine="ClickNumber(\"2\")";
_clicknumber("2");
 //BA.debugLineNum = 111;BA.debugLine="End Sub";
return "";
}
public static String  _btn3_click() throws Exception{
 //BA.debugLineNum = 113;BA.debugLine="Sub btn3_Click";
 //BA.debugLineNum = 114;BA.debugLine="ClickNumber(\"3\")";
_clicknumber("3");
 //BA.debugLineNum = 115;BA.debugLine="End Sub";
return "";
}
public static String  _btn4_click() throws Exception{
 //BA.debugLineNum = 117;BA.debugLine="Sub btn4_Click";
 //BA.debugLineNum = 118;BA.debugLine="ClickNumber(\"4\")";
_clicknumber("4");
 //BA.debugLineNum = 119;BA.debugLine="End Sub";
return "";
}
public static String  _btn5_click() throws Exception{
 //BA.debugLineNum = 121;BA.debugLine="Sub btn5_Click";
 //BA.debugLineNum = 122;BA.debugLine="ClickNumber(\"5\")";
_clicknumber("5");
 //BA.debugLineNum = 123;BA.debugLine="End Sub";
return "";
}
public static String  _btn6_click() throws Exception{
 //BA.debugLineNum = 125;BA.debugLine="Sub btn6_Click";
 //BA.debugLineNum = 126;BA.debugLine="ClickNumber(\"6\")";
_clicknumber("6");
 //BA.debugLineNum = 127;BA.debugLine="End Sub";
return "";
}
public static String  _btn7_click() throws Exception{
 //BA.debugLineNum = 129;BA.debugLine="Sub btn7_Click";
 //BA.debugLineNum = 130;BA.debugLine="ClickNumber(\"7\")";
_clicknumber("7");
 //BA.debugLineNum = 131;BA.debugLine="End Sub";
return "";
}
public static String  _btn8_click() throws Exception{
 //BA.debugLineNum = 133;BA.debugLine="Sub btn8_Click";
 //BA.debugLineNum = 134;BA.debugLine="ClickNumber(\"8\")";
_clicknumber("8");
 //BA.debugLineNum = 135;BA.debugLine="End Sub";
return "";
}
public static String  _btn9_click() throws Exception{
 //BA.debugLineNum = 137;BA.debugLine="Sub btn9_Click";
 //BA.debugLineNum = 138;BA.debugLine="ClickNumber(\"9\")";
_clicknumber("9");
 //BA.debugLineNum = 139;BA.debugLine="End Sub";
return "";
}
public static String  _btnclear_click() throws Exception{
 //BA.debugLineNum = 141;BA.debugLine="Sub btnClear_Click";
 //BA.debugLineNum = 142;BA.debugLine="labFreq.Text = \"\"";
mostCurrent._labfreq.setText((Object)(""));
 //BA.debugLineNum = 143;BA.debugLine="End Sub";
return "";
}
public static String  _btnconfig_click() throws Exception{
 //BA.debugLineNum = 163;BA.debugLine="Sub btnConfig_Click";
 //BA.debugLineNum = 164;BA.debugLine="StartActivity(screen.CreateIntent)";
anywheresoftware.b4a.keywords.Common.StartActivity(mostCurrent.activityBA,(Object)(_screen.CreateIntent()));
 //BA.debugLineNum = 165;BA.debugLine="End Sub";
return "";
}
public static String  _btndab_click() throws Exception{
 //BA.debugLineNum = 195;BA.debugLine="Sub btnDAB_Click";
 //BA.debugLineNum = 196;BA.debugLine="CallSub(RadioService, \"SwitchToDAB\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SwitchToDAB");
 //BA.debugLineNum = 198;BA.debugLine="RadioService.isDAB = True";
mostCurrent._radioservice._isdab = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 199;BA.debugLine="labProgram2.Visible = True";
mostCurrent._labprogram2.setVisible(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 200;BA.debugLine="btnFM.Checked = False";
mostCurrent._btnfm.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 201;BA.debugLine="btnDAB.Checked = True";
mostCurrent._btndab.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 202;BA.debugLine="btnSt1.Checked = False";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 203;BA.debugLine="btnSt2.Checked = False";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 204;BA.debugLine="btnSt3.Checked = False";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 205;BA.debugLine="btnSt4.Checked = False";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 206;BA.debugLine="btnSt5.Checked = False";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 207;BA.debugLine="btnSt6.Checked = False";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 208;BA.debugLine="btnDAB.Checked = True";
mostCurrent._btndab.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 209;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 210;BA.debugLine="End Sub";
return "";
}
public static String  _btndab_longclick() throws Exception{
 //BA.debugLineNum = 212;BA.debugLine="Sub btnDAB_LongClick";
 //BA.debugLineNum = 213;BA.debugLine="myclick.standardFx(1)";
_myclick.standardFx((float) (1));
 //BA.debugLineNum = 214;BA.debugLine="CallSub(RadioService, \"ChangeDABLevel\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"ChangeDABLevel");
 //BA.debugLineNum = 215;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 216;BA.debugLine="End Sub";
return "";
}
public static String  _btnenter_click() throws Exception{
 //BA.debugLineNum = 145;BA.debugLine="Sub btnEnter_Click";
 //BA.debugLineNum = 146;BA.debugLine="If labFreq.Text <> \"\" Then";
if ((mostCurrent._labfreq.getText()).equals("") == false) { 
 //BA.debugLineNum = 147;BA.debugLine="CallSub2(RadioService,\"EnterFrequency\",labFreq.T";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"EnterFrequency",(Object)(mostCurrent._labfreq.getText()));
 //BA.debugLineNum = 148;BA.debugLine="If RadioService.EnterClickedReturnValue Then";
if (mostCurrent._radioservice._enterclickedreturnvalue) { 
 //BA.debugLineNum = 149;BA.debugLine="LabelClean";
_labelclean();
 };
 };
 //BA.debugLineNum = 152;BA.debugLine="panKeyboard.Visible = False";
mostCurrent._pankeyboard.setVisible(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 154;BA.debugLine="End Sub";
return "";
}
public static String  _btnexit_click() throws Exception{
 //BA.debugLineNum = 342;BA.debugLine="Sub btnExit_Click";
 //BA.debugLineNum = 343;BA.debugLine="CallSub(RadioService, \"ExitApp\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"ExitApp");
 //BA.debugLineNum = 344;BA.debugLine="End Sub";
return "";
}
public static String  _btnfm_click() throws Exception{
 //BA.debugLineNum = 171;BA.debugLine="Sub btnFM_Click";
 //BA.debugLineNum = 172;BA.debugLine="CallSub(RadioService, \"SwitchToFM\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SwitchToFM");
 //BA.debugLineNum = 174;BA.debugLine="labProgram2.Visible = False";
mostCurrent._labprogram2.setVisible(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 175;BA.debugLine="btnDAB.Checked = False";
mostCurrent._btndab.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 176;BA.debugLine="btnFM.Checked = True";
mostCurrent._btnfm.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 177;BA.debugLine="btnSt1.Checked = False";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 178;BA.debugLine="btnSt2.Checked = False";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 179;BA.debugLine="btnSt3.Checked = False";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 180;BA.debugLine="btnSt4.Checked = False";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 181;BA.debugLine="btnSt5.Checked = False";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 182;BA.debugLine="btnSt6.Checked = False";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 183;BA.debugLine="btnFM.Checked = True";
mostCurrent._btnfm.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 184;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 185;BA.debugLine="End Sub";
return "";
}
public static String  _btnfm_longclick() throws Exception{
 //BA.debugLineNum = 187;BA.debugLine="Sub btnFM_LongClick";
 //BA.debugLineNum = 188;BA.debugLine="myclick.standardFx(1)";
_myclick.standardFx((float) (1));
 //BA.debugLineNum = 190;BA.debugLine="CallSub(RadioService, \"ChangeFMLevel\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"ChangeFMLevel");
 //BA.debugLineNum = 192;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 193;BA.debugLine="End Sub";
return "";
}
public static String  _btnfmdn_click() throws Exception{
 //BA.debugLineNum = 319;BA.debugLine="Sub btnFMDN_Click";
 //BA.debugLineNum = 320;BA.debugLine="CallSub(RadioService,\"RadioChannelDown\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"RadioChannelDown");
 //BA.debugLineNum = 321;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 322;BA.debugLine="End Sub";
return "";
}
public static String  _btnfmhi_click() throws Exception{
 //BA.debugLineNum = 324;BA.debugLine="Sub btnFMHi_Click";
 //BA.debugLineNum = 325;BA.debugLine="CallSub(RadioService,\"FMHigher\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"FMHigher");
 //BA.debugLineNum = 326;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 327;BA.debugLine="End Sub";
return "";
}
public static String  _btnfmlo_click() throws Exception{
 //BA.debugLineNum = 329;BA.debugLine="Sub btnFMLo_Click";
 //BA.debugLineNum = 330;BA.debugLine="CallSub(RadioService,\"FMLower\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"FMLower");
 //BA.debugLineNum = 331;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 332;BA.debugLine="End Sub";
return "";
}
public static String  _btnfmup_click() throws Exception{
 //BA.debugLineNum = 314;BA.debugLine="Sub btnFMUP_Click";
 //BA.debugLineNum = 315;BA.debugLine="CallSub(RadioService,\"RadioChannelUp\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"RadioChannelUp");
 //BA.debugLineNum = 316;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 317;BA.debugLine="End Sub";
return "";
}
public static String  _btnkey_click() throws Exception{
 //BA.debugLineNum = 158;BA.debugLine="Sub btnKey_Click";
 //BA.debugLineNum = 159;BA.debugLine="panKeyboard.Visible = True";
mostCurrent._pankeyboard.setVisible(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 160;BA.debugLine="labFreq.Text = \"\"";
mostCurrent._labfreq.setText((Object)(""));
 //BA.debugLineNum = 161;BA.debugLine="End Sub";
return "";
}
public static String  _btnlevel_click() throws Exception{
 //BA.debugLineNum = 349;BA.debugLine="Sub btnLevel_Click";
 //BA.debugLineNum = 350;BA.debugLine="CallSub(RadioService,\"IncrementLevel\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"IncrementLevel");
 //BA.debugLineNum = 351;BA.debugLine="btnLevel.Text = \"L \" & RadioService.Ebene";
mostCurrent._btnlevel.setText((Object)("L "+BA.NumberToString(mostCurrent._radioservice._ebene)));
 //BA.debugLineNum = 352;BA.debugLine="End Sub";
return "";
}
public static String  _btnmute_click() throws Exception{
 //BA.debugLineNum = 167;BA.debugLine="Sub btnMute_Click";
 //BA.debugLineNum = 168;BA.debugLine="CallSub(RadioService, \"ToggleMute\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"ToggleMute");
 //BA.debugLineNum = 169;BA.debugLine="End Sub";
return "";
}
public static String  _btnst1_click() throws Exception{
 //BA.debugLineNum = 218;BA.debugLine="Sub btnSt1_Click";
 //BA.debugLineNum = 219;BA.debugLine="CallSub2(RadioService, \"SelectChannel\",1)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SelectChannel",(Object)(1));
 //BA.debugLineNum = 220;BA.debugLine="btnSt2.Checked = False";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 221;BA.debugLine="btnSt1.Checked = True";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 222;BA.debugLine="btnSt3.Checked = False";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 223;BA.debugLine="btnSt4.Checked = False";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 224;BA.debugLine="btnSt5.Checked = False";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 225;BA.debugLine="btnSt6.Checked = False";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 226;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 227;BA.debugLine="End Sub";
return "";
}
public static String  _btnst1_longclick() throws Exception{
 //BA.debugLineNum = 229;BA.debugLine="Sub btnSt1_LongClick";
 //BA.debugLineNum = 230;BA.debugLine="myclick.returnFx(1)";
_myclick.returnFx((float) (1));
 //BA.debugLineNum = 231;BA.debugLine="CallSub2(RadioService, \"SetChannel\", 1)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SetChannel",(Object)(1));
 //BA.debugLineNum = 232;BA.debugLine="End Sub";
return "";
}
public static String  _btnst2_click() throws Exception{
 //BA.debugLineNum = 234;BA.debugLine="Sub btnSt2_Click";
 //BA.debugLineNum = 235;BA.debugLine="CallSub2(RadioService, \"SelectChannel\",2)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SelectChannel",(Object)(2));
 //BA.debugLineNum = 236;BA.debugLine="btnSt1.Checked = False";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 237;BA.debugLine="btnSt2.Checked = True";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 238;BA.debugLine="btnSt3.Checked = False";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 239;BA.debugLine="btnSt4.Checked = False";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 240;BA.debugLine="btnSt5.Checked = False";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 241;BA.debugLine="btnSt6.Checked = False";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 242;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 243;BA.debugLine="End Sub";
return "";
}
public static String  _btnst2_longclick() throws Exception{
 //BA.debugLineNum = 245;BA.debugLine="Sub btnSt2_LongClick";
 //BA.debugLineNum = 246;BA.debugLine="myclick.returnFx(1)";
_myclick.returnFx((float) (1));
 //BA.debugLineNum = 247;BA.debugLine="CallSub2(RadioService, \"SetChannel\", 2)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SetChannel",(Object)(2));
 //BA.debugLineNum = 248;BA.debugLine="End Sub";
return "";
}
public static String  _btnst3_click() throws Exception{
 //BA.debugLineNum = 250;BA.debugLine="Sub btnSt3_Click";
 //BA.debugLineNum = 251;BA.debugLine="CallSub2(RadioService, \"SelectChannel\",3)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SelectChannel",(Object)(3));
 //BA.debugLineNum = 252;BA.debugLine="btnSt1.Checked = False";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 253;BA.debugLine="btnSt3.Checked = True";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 254;BA.debugLine="btnSt2.Checked = False";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 255;BA.debugLine="btnSt4.Checked = False";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 256;BA.debugLine="btnSt5.Checked = False";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 257;BA.debugLine="btnSt6.Checked = False";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 258;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 259;BA.debugLine="End Sub";
return "";
}
public static String  _btnst3_longclick() throws Exception{
 //BA.debugLineNum = 261;BA.debugLine="Sub btnSt3_LongClick";
 //BA.debugLineNum = 262;BA.debugLine="myclick.returnFx(1)";
_myclick.returnFx((float) (1));
 //BA.debugLineNum = 263;BA.debugLine="CallSub2(RadioService, \"SetChannel\", 3)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SetChannel",(Object)(3));
 //BA.debugLineNum = 264;BA.debugLine="End Sub";
return "";
}
public static String  _btnst4_click() throws Exception{
 //BA.debugLineNum = 266;BA.debugLine="Sub btnSt4_Click";
 //BA.debugLineNum = 267;BA.debugLine="CallSub2(RadioService, \"SelectChannel\",4)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SelectChannel",(Object)(4));
 //BA.debugLineNum = 268;BA.debugLine="btnSt4.Checked = True";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 269;BA.debugLine="btnSt1.Checked = False";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 270;BA.debugLine="btnSt2.Checked = False";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 271;BA.debugLine="btnSt3.Checked = False";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 272;BA.debugLine="btnSt5.Checked = False";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 273;BA.debugLine="btnSt6.Checked = False";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 274;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 275;BA.debugLine="End Sub";
return "";
}
public static String  _btnst4_longclick() throws Exception{
 //BA.debugLineNum = 277;BA.debugLine="Sub btnSt4_LongClick";
 //BA.debugLineNum = 278;BA.debugLine="myclick.returnFx(1)";
_myclick.returnFx((float) (1));
 //BA.debugLineNum = 279;BA.debugLine="CallSub2(RadioService, \"SetChannel\", 4)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SetChannel",(Object)(4));
 //BA.debugLineNum = 280;BA.debugLine="End Sub";
return "";
}
public static String  _btnst5_click() throws Exception{
 //BA.debugLineNum = 282;BA.debugLine="Sub btnSt5_Click";
 //BA.debugLineNum = 283;BA.debugLine="CallSub2(RadioService, \"SelectChannel\",5)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SelectChannel",(Object)(5));
 //BA.debugLineNum = 284;BA.debugLine="btnSt1.Checked = False";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 285;BA.debugLine="btnSt5.Checked = True";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 286;BA.debugLine="btnSt2.Checked = False";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 287;BA.debugLine="btnSt3.Checked = False";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 288;BA.debugLine="btnSt4.Checked = False";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 289;BA.debugLine="btnSt6.Checked = False";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 290;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 291;BA.debugLine="End Sub";
return "";
}
public static String  _btnst5_longclick() throws Exception{
 //BA.debugLineNum = 293;BA.debugLine="Sub btnSt5_LongClick";
 //BA.debugLineNum = 294;BA.debugLine="myclick.returnFx(1)";
_myclick.returnFx((float) (1));
 //BA.debugLineNum = 295;BA.debugLine="CallSub2(RadioService, \"SetChannel\", 5)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SetChannel",(Object)(5));
 //BA.debugLineNum = 296;BA.debugLine="End Sub";
return "";
}
public static String  _btnst6_click() throws Exception{
 //BA.debugLineNum = 298;BA.debugLine="Sub btnSt6_Click";
 //BA.debugLineNum = 299;BA.debugLine="CallSub2(RadioService, \"SelectChannel\",6)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SelectChannel",(Object)(6));
 //BA.debugLineNum = 300;BA.debugLine="btnSt1.Checked = False";
mostCurrent._btnst1.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 301;BA.debugLine="btnSt2.Checked = False";
mostCurrent._btnst2.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 302;BA.debugLine="btnSt6.Checked = True";
mostCurrent._btnst6.setChecked(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 303;BA.debugLine="btnSt3.Checked = False";
mostCurrent._btnst3.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 304;BA.debugLine="btnSt4.Checked = False";
mostCurrent._btnst4.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 305;BA.debugLine="btnSt5.Checked = False";
mostCurrent._btnst5.setChecked(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 306;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 307;BA.debugLine="End Sub";
return "";
}
public static String  _btnst6_longclick() throws Exception{
 //BA.debugLineNum = 309;BA.debugLine="Sub btnSt6_LongClick";
 //BA.debugLineNum = 310;BA.debugLine="myclick.returnFx(1)";
_myclick.returnFx((float) (1));
 //BA.debugLineNum = 311;BA.debugLine="CallSub2(RadioService, \"SetChannel\", 6)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SetChannel",(Object)(6));
 //BA.debugLineNum = 312;BA.debugLine="End Sub";
return "";
}
public static String  _btnvoldn_click() throws Exception{
 //BA.debugLineNum = 338;BA.debugLine="Sub btnVolDn_Click";
 //BA.debugLineNum = 339;BA.debugLine="CallSub(RadioService,\"VolumeDown\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"VolumeDown");
 //BA.debugLineNum = 340;BA.debugLine="End Sub";
return "";
}
public static String  _btnvolup_click() throws Exception{
 //BA.debugLineNum = 334;BA.debugLine="Sub btnVolUp_Click";
 //BA.debugLineNum = 335;BA.debugLine="CallSub(RadioService,\"VolumeUp\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"VolumeUp");
 //BA.debugLineNum = 336;BA.debugLine="End Sub";
return "";
}
public static String  _clicknumber(String _number) throws Exception{
 //BA.debugLineNum = 87;BA.debugLine="Sub ClickNumber(Number As String)";
 //BA.debugLineNum = 88;BA.debugLine="labFreq.Text = labFreq.Text & Number";
mostCurrent._labfreq.setText((Object)(mostCurrent._labfreq.getText()+_number));
 //BA.debugLineNum = 89;BA.debugLine="If RadioService.isDAB And RadioService.DAB Then";
if (mostCurrent._radioservice._isdab && mostCurrent._radioservice._dab) { 
 }else {
 //BA.debugLineNum = 92;BA.debugLine="Select labFreq.Text.Length";
switch (BA.switchObjectToInt(mostCurrent._labfreq.getText().length(),(int) (2),(int) (3))) {
case 0:
 //BA.debugLineNum = 94;BA.debugLine="If labFreq.Text.SubString2(0,1) = \"8\" Or labFreq";
if ((mostCurrent._labfreq.getText().substring((int) (0),(int) (1))).equals("8") || (mostCurrent._labfreq.getText().substring((int) (0),(int) (1))).equals("9")) { 
mostCurrent._labfreq.setText((Object)(mostCurrent._labfreq.getText()+"."));};
 break;
case 1:
 //BA.debugLineNum = 96;BA.debugLine="labFreq.Text = labFreq.Text & \".\"";
mostCurrent._labfreq.setText((Object)(mostCurrent._labfreq.getText()+"."));
 break;
}
;
 };
 //BA.debugLineNum = 99;BA.debugLine="End Sub";
return "";
}
public static String  _createpreferencescreen() throws Exception{
anywheresoftware.b4a.objects.preferenceactivity.PreferenceCategoryWrapper _cat4 = null;
anywheresoftware.b4a.objects.preferenceactivity.PreferenceCategoryWrapper _cat1 = null;
 //BA.debugLineNum = 66;BA.debugLine="Sub CreatePreferenceScreen";
 //BA.debugLineNum = 67;BA.debugLine="screen.Initialize(\"Settings\", \"\")";
_screen.Initialize("Settings","");
 //BA.debugLineNum = 68;BA.debugLine="Dim cat4,cat1 As PreferenceCategory";
_cat4 = new anywheresoftware.b4a.objects.preferenceactivity.PreferenceCategoryWrapper();
_cat1 = new anywheresoftware.b4a.objects.preferenceactivity.PreferenceCategoryWrapper();
 //BA.debugLineNum = 69;BA.debugLine="cat1.Initialize(\"Volumes\")";
_cat1.Initialize("Volumes");
 //BA.debugLineNum = 70;BA.debugLine="cat1.AddList(\"DefaultVolume\", \"Default Volume\", \"";
_cat1.AddList("DefaultVolume","Default Volume","","4",_volumerange);
 //BA.debugLineNum = 71;BA.debugLine="cat1.AddList(\"DuckVolume\", \"Duck Volume\", \"Volume";
_cat1.AddList("DuckVolume","Duck Volume","Volume when, for example, google maps starts speaking","4",_volumerange);
 //BA.debugLineNum = 72;BA.debugLine="cat4.Initialize(\"DAB\")";
_cat4.Initialize("DAB");
 //BA.debugLineNum = 73;BA.debugLine="cat4.AddCheckBox(\"Clean\",\"Clean\",\"Clean DAB datab";
_cat4.AddCheckBox("Clean","Clean","Clean DAB database on new search",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 75;BA.debugLine="screen.AddPreferenceCategory(cat1)";
_screen.AddPreferenceCategory(_cat1);
 //BA.debugLineNum = 76;BA.debugLine="screen.AddPreferenceCategory(cat4)";
_screen.AddPreferenceCategory(_cat4);
 //BA.debugLineNum = 78;BA.debugLine="End Sub";
return "";
}
public static String  _globals() throws Exception{
 //BA.debugLineNum = 29;BA.debugLine="Sub Globals";
 //BA.debugLineNum = 30;BA.debugLine="Dim btnExit, btnConfig, btnLevel, btnMute, btnFMU";
mostCurrent._btnexit = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnconfig = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnlevel = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnmute = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnfmup = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnfmdn = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnvolup = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnvoldn = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnfmhi = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnfmlo = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnkey = new anywheresoftware.b4a.objects.ButtonWrapper();
 //BA.debugLineNum = 31;BA.debugLine="Dim btnSt1, btnSt2, btnSt3, btnSt4, btnSt5, btnSt";
mostCurrent._btnst1 = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
mostCurrent._btnst2 = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
mostCurrent._btnst3 = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
mostCurrent._btnst4 = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
mostCurrent._btnst5 = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
mostCurrent._btnst6 = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
mostCurrent._btnfm = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
mostCurrent._btndab = new anywheresoftware.b4a.objects.CompoundButtonWrapper.ToggleButtonWrapper();
 //BA.debugLineNum = 32;BA.debugLine="Dim panMain, panKeyboard, panSelect As Panel";
mostCurrent._panmain = new anywheresoftware.b4a.objects.PanelWrapper();
mostCurrent._pankeyboard = new anywheresoftware.b4a.objects.PanelWrapper();
mostCurrent._panselect = new anywheresoftware.b4a.objects.PanelWrapper();
 //BA.debugLineNum = 33;BA.debugLine="Dim btnEnter, btnClear, btn1, btn2, btn3, btn4, b";
mostCurrent._btnenter = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btnclear = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn1 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn2 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn3 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn4 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn5 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn6 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn7 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn8 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn9 = new anywheresoftware.b4a.objects.ButtonWrapper();
mostCurrent._btn0 = new anywheresoftware.b4a.objects.ButtonWrapper();
 //BA.debugLineNum = 34;BA.debugLine="Dim labFreq, labVolume, labStrength, labProgramTy";
mostCurrent._labfreq = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labvolume = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labstrength = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labprogramtype = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labprogram = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labprogram2 = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labprogramtext = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labevent = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labdatarate = new anywheresoftware.b4a.objects.LabelWrapper();
mostCurrent._labstereomode = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 35;BA.debugLine="Dim lvDAB As ListView";
mostCurrent._lvdab = new anywheresoftware.b4a.objects.ListViewWrapper();
 //BA.debugLineNum = 36;BA.debugLine="Dim pbStrength As ProgressBar";
mostCurrent._pbstrength = new anywheresoftware.b4a.objects.ProgressBarWrapper();
 //BA.debugLineNum = 38;BA.debugLine="End Sub";
return "";
}
public static String  _labdabfm_click() throws Exception{
int _i = 0;
anywheresoftware.b4a.objects.LabelWrapper _l = null;
anywheresoftware.b4a.objects.PanelWrapper _s = null;
 //BA.debugLineNum = 374;BA.debugLine="Sub labDABFM_Click";
 //BA.debugLineNum = 375;BA.debugLine="Dim i As Int";
_i = 0;
 //BA.debugLineNum = 376;BA.debugLine="For i = 0 To panMain.NumberOfViews - 1";
{
final int step281 = 1;
final int limit281 = (int) (mostCurrent._panmain.getNumberOfViews()-1);
for (_i = (int) (0); (step281 > 0 && _i <= limit281) || (step281 < 0 && _i >= limit281); _i = ((int)(0 + _i + step281))) {
 //BA.debugLineNum = 377;BA.debugLine="If panMain.GetView(i) Is Label Then";
if (mostCurrent._panmain.GetView(_i).getObjectOrNull() instanceof android.widget.TextView) { 
 //BA.debugLineNum = 379;BA.debugLine="Dim l As Label";
_l = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 380;BA.debugLine="l = panMain.GetView(i)";
_l.setObject((android.widget.TextView)(mostCurrent._panmain.GetView(_i).getObject()));
 //BA.debugLineNum = 381;BA.debugLine="l.Color = Colors.ARGB(0, 128, 128, 128)";
_l.setColor(anywheresoftware.b4a.keywords.Common.Colors.ARGB((int) (0),(int) (128),(int) (128),(int) (128)));
 };
 //BA.debugLineNum = 383;BA.debugLine="If panMain.GetView(i) Is Panel Then";
if (mostCurrent._panmain.GetView(_i).getObjectOrNull() instanceof android.view.ViewGroup) { 
 //BA.debugLineNum = 385;BA.debugLine="Dim s As Panel";
_s = new anywheresoftware.b4a.objects.PanelWrapper();
 //BA.debugLineNum = 386;BA.debugLine="s = panMain.GetView(i)";
_s.setObject((android.view.ViewGroup)(mostCurrent._panmain.GetView(_i).getObject()));
 //BA.debugLineNum = 387;BA.debugLine="s.Color = Colors.ARGB(0, 128, 128, 128)";
_s.setColor(anywheresoftware.b4a.keywords.Common.Colors.ARGB((int) (0),(int) (128),(int) (128),(int) (128)));
 };
 }
};
 //BA.debugLineNum = 390;BA.debugLine="End Sub";
return "";
}
public static String  _labdabfm_longclick() throws Exception{
int _i = 0;
anywheresoftware.b4a.objects.LabelWrapper _l = null;
anywheresoftware.b4a.objects.PanelWrapper _s = null;
 //BA.debugLineNum = 392;BA.debugLine="Sub labDABFM_LongClick";
 //BA.debugLineNum = 393;BA.debugLine="Dim i As Int";
_i = 0;
 //BA.debugLineNum = 394;BA.debugLine="For i = 0 To panMain.NumberOfViews - 1";
{
final int step296 = 1;
final int limit296 = (int) (mostCurrent._panmain.getNumberOfViews()-1);
for (_i = (int) (0); (step296 > 0 && _i <= limit296) || (step296 < 0 && _i >= limit296); _i = ((int)(0 + _i + step296))) {
 //BA.debugLineNum = 395;BA.debugLine="If panMain.GetView(i) Is Label Then";
if (mostCurrent._panmain.GetView(_i).getObjectOrNull() instanceof android.widget.TextView) { 
 //BA.debugLineNum = 397;BA.debugLine="Dim l As Label";
_l = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 398;BA.debugLine="l = panMain.GetView(i)";
_l.setObject((android.widget.TextView)(mostCurrent._panmain.GetView(_i).getObject()));
 //BA.debugLineNum = 399;BA.debugLine="l.Color = Colors.ARGB(100, 128, 128, 128)";
_l.setColor(anywheresoftware.b4a.keywords.Common.Colors.ARGB((int) (100),(int) (128),(int) (128),(int) (128)));
 };
 //BA.debugLineNum = 401;BA.debugLine="If panMain.GetView(i) Is Panel Then";
if (mostCurrent._panmain.GetView(_i).getObjectOrNull() instanceof android.view.ViewGroup) { 
 //BA.debugLineNum = 403;BA.debugLine="Dim s As Panel";
_s = new anywheresoftware.b4a.objects.PanelWrapper();
 //BA.debugLineNum = 404;BA.debugLine="s = panMain.GetView(i)";
_s.setObject((android.view.ViewGroup)(mostCurrent._panmain.GetView(_i).getObject()));
 //BA.debugLineNum = 405;BA.debugLine="s.Color = Colors.ARGB(100, 128, 128, 128)";
_s.setColor(anywheresoftware.b4a.keywords.Common.Colors.ARGB((int) (100),(int) (128),(int) (128),(int) (128)));
 };
 }
};
 //BA.debugLineNum = 408;BA.debugLine="End Sub";
return "";
}
public static String  _labelclean() throws Exception{
 //BA.debugLineNum = 410;BA.debugLine="Sub LabelClean";
 //BA.debugLineNum = 411;BA.debugLine="CallSub(RadioService, \"LabelClean\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"LabelClean");
 //BA.debugLineNum = 412;BA.debugLine="btnLevel.Text = \"L \" & RadioService.Ebene";
mostCurrent._btnlevel.setText((Object)("L "+BA.NumberToString(mostCurrent._radioservice._ebene)));
 //BA.debugLineNum = 413;BA.debugLine="End Sub";
return "";
}
public static String  _labfreq_longclick() throws Exception{
 //BA.debugLineNum = 368;BA.debugLine="Sub labFreq_LongClick";
 //BA.debugLineNum = 369;BA.debugLine="myclick.returnFx(1)";
_myclick.returnFx((float) (1));
 //BA.debugLineNum = 370;BA.debugLine="CallSub2(RadioService, \"StartChannelSearch\", mana";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"StartChannelSearch",(Object)(_manager.GetBoolean("Clean")));
 //BA.debugLineNum = 371;BA.debugLine="LabelClean";
_labelclean();
 //BA.debugLineNum = 372;BA.debugLine="End Sub";
return "";
}
public static String  _lvdab_itemclick(int _position,Object _value) throws Exception{
 //BA.debugLineNum = 415;BA.debugLine="Sub lvDAB_ItemClick (Position As Int, Value As Obj";
 //BA.debugLineNum = 416;BA.debugLine="CallSub2(RadioService,\"SelectDABItem\",Position)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(mostCurrent.activityBA,(Object)(mostCurrent._radioservice.getObject()),"SelectDABItem",(Object)(_position));
 //BA.debugLineNum = 417;BA.debugLine="End Sub";
return "";
}

public static void initializeProcessGlobals() {
    
    if (main.processGlobalsRun == false) {
	    main.processGlobalsRun = true;
		try {
		        wal.INIFiles.ini._process_globals();
main._process_globals();
radioservice._process_globals();
slideshow._process_globals();
		
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
}public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 20;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 21;BA.debugLine="Dim manager As PreferenceManager";
_manager = new anywheresoftware.b4a.objects.preferenceactivity.PreferenceManager();
 //BA.debugLineNum = 22;BA.debugLine="Dim screen As PreferenceScreen";
_screen = new anywheresoftware.b4a.objects.preferenceactivity.PreferenceScreenWrapper();
 //BA.debugLineNum = 23;BA.debugLine="Dim myclick As esClickSound";
_myclick = new edsmith.click.sound.ClickSound();
 //BA.debugLineNum = 24;BA.debugLine="Dim VolumeRange As List";
_volumerange = new anywheresoftware.b4a.objects.collections.List();
 //BA.debugLineNum = 25;BA.debugLine="Dim BroadcastReceiver As BroadCastReceiver";
_broadcastreceiver = new com.rootsoft.broadcastreceiver.BroadCastReceiver();
 //BA.debugLineNum = 27;BA.debugLine="End Sub";
return "";
}
public static String  _selecteddabchannel(int _position) throws Exception{
 //BA.debugLineNum = 419;BA.debugLine="Sub SelectedDABChannel (Position As Int)";
 //BA.debugLineNum = 420;BA.debugLine="lvDAB.SetSelection(Position)";
mostCurrent._lvdab.SetSelection(_position);
 //BA.debugLineNum = 421;BA.debugLine="Log(\"Channel \" & Position & \" selected\")";
anywheresoftware.b4a.keywords.Common.Log("Channel "+BA.NumberToString(_position)+" selected");
 //BA.debugLineNum = 422;BA.debugLine="End Sub";
return "";
}
public static String  _setdefaults() throws Exception{
 //BA.debugLineNum = 59;BA.debugLine="Sub SetDefaults";
 //BA.debugLineNum = 61;BA.debugLine="manager.SetString(\"Threshold\",\"5\")";
_manager.SetString("Threshold","5");
 //BA.debugLineNum = 62;BA.debugLine="manager.SetString(\"DuckVolume\",\"5\")";
_manager.SetString("DuckVolume","5");
 //BA.debugLineNum = 63;BA.debugLine="manager.SetString(\"DefaultVolume\",\"13\")";
_manager.SetString("DefaultVolume","13");
 //BA.debugLineNum = 64;BA.debugLine="End Sub";
return "";
}
public static String  _setevaluateddata(anywheresoftware.b4a.objects.collections.List _arguments) throws Exception{
 //BA.debugLineNum = 44;BA.debugLine="Sub SetEvaluatedData(Arguments As List)";
 //BA.debugLineNum = 45;BA.debugLine="labEvent.text = Arguments.Get(0)";
mostCurrent._labevent.setText(_arguments.Get((int) (0)));
 //BA.debugLineNum = 46;BA.debugLine="If panKeyboard.Visible <> True Then labFreq.text";
if (mostCurrent._pankeyboard.getVisible()!=anywheresoftware.b4a.keywords.Common.True) { 
mostCurrent._labfreq.setText(_arguments.Get((int) (1)));};
 //BA.debugLineNum = 47;BA.debugLine="labStrength.text = Arguments.Get(2)";
mostCurrent._labstrength.setText(_arguments.Get((int) (2)));
 //BA.debugLineNum = 48;BA.debugLine="pbStrength.progress = Arguments.Get(3)";
mostCurrent._pbstrength.setProgress((int)(BA.ObjectToNumber(_arguments.Get((int) (3)))));
 //BA.debugLineNum = 49;BA.debugLine="labVolume.text = Arguments.Get(4)";
mostCurrent._labvolume.setText(_arguments.Get((int) (4)));
 //BA.debugLineNum = 50;BA.debugLine="labProgram.text = Arguments.Get(5)";
mostCurrent._labprogram.setText(_arguments.Get((int) (5)));
 //BA.debugLineNum = 51;BA.debugLine="labProgramType.text = Arguments.Get(6)";
mostCurrent._labprogramtype.setText(_arguments.Get((int) (6)));
 //BA.debugLineNum = 52;BA.debugLine="labProgram2.text = Arguments.Get(7)";
mostCurrent._labprogram2.setText(_arguments.Get((int) (7)));
 //BA.debugLineNum = 53;BA.debugLine="labProgramText.text = Arguments.Get(8)";
mostCurrent._labprogramtext.setText(_arguments.Get((int) (8)));
 //BA.debugLineNum = 54;BA.debugLine="labStereoMode.text = Arguments.Get(9)";
mostCurrent._labstereomode.setText(_arguments.Get((int) (9)));
 //BA.debugLineNum = 55;BA.debugLine="labDataRate.text = Arguments.Get(10)";
mostCurrent._labdatarate.setText(_arguments.Get((int) (10)));
 //BA.debugLineNum = 57;BA.debugLine="End Sub";
return "";
}
public static String  _settextvaluesfromsettings(anywheresoftware.b4a.objects.collections.List _arglist) throws Exception{
 //BA.debugLineNum = 426;BA.debugLine="Sub SetTextValuesFromSettings(Arglist As List)";
 //BA.debugLineNum = 427;BA.debugLine="btnLevel.Text = Arglist.Get(0)";
mostCurrent._btnlevel.setText(_arglist.Get((int) (0)));
 //BA.debugLineNum = 428;BA.debugLine="If Arglist.Get(1) Then";
if (BA.ObjectToBoolean(_arglist.Get((int) (1)))) { 
 //BA.debugLineNum = 429;BA.debugLine="btnDAB.Checked = True";
mostCurrent._btndab.setChecked(anywheresoftware.b4a.keywords.Common.True);
 }else {
 //BA.debugLineNum = 431;BA.debugLine="btnFM.Checked = True";
mostCurrent._btnfm.setChecked(anywheresoftware.b4a.keywords.Common.True);
 };
 //BA.debugLineNum = 433;BA.debugLine="labProgram2.Visible = Arglist.Get(2)";
mostCurrent._labprogram2.setVisible(BA.ObjectToBoolean(_arglist.Get((int) (2))));
 //BA.debugLineNum = 434;BA.debugLine="updateStationList";
_updatestationlist();
 //BA.debugLineNum = 435;BA.debugLine="End Sub";
return "";
}
public static String  _updatestationlist() throws Exception{
int _i = 0;
 //BA.debugLineNum = 354;BA.debugLine="Sub updateStationList";
 //BA.debugLineNum = 355;BA.debugLine="Try";
try { //BA.debugLineNum = 356;BA.debugLine="Dim i As Int";
_i = 0;
 //BA.debugLineNum = 357;BA.debugLine="lvDAB.Clear";
mostCurrent._lvdab.Clear();
 //BA.debugLineNum = 358;BA.debugLine="For i = 0 To RadioService.lstDAB.Size - 1";
{
final int step265 = 1;
final int limit265 = (int) (mostCurrent._radioservice._lstdab.getSize()-1);
for (_i = (int) (0); (step265 > 0 && _i <= limit265) || (step265 < 0 && _i >= limit265); _i = ((int)(0 + _i + step265))) {
 //BA.debugLineNum = 359;BA.debugLine="lvDAB.AddSingleLine(RadioService.lstDAB.Get(i))";
mostCurrent._lvdab.AddSingleLine(BA.ObjectToString(mostCurrent._radioservice._lstdab.Get(_i)));
 }
};
 //BA.debugLineNum = 361;BA.debugLine="If RadioService.isDAB And RadioService.DAB Then";
if (mostCurrent._radioservice._isdab && mostCurrent._radioservice._dab) { 
 //BA.debugLineNum = 362;BA.debugLine="lvDAB.SetSelection(RadioService.Frequenz)";
mostCurrent._lvdab.SetSelection(mostCurrent._radioservice._frequenz);
 };
 } 
       catch (Exception e272) {
			processBA.setLastException(e272); };
 //BA.debugLineNum = 366;BA.debugLine="End Sub";
return "";
}
}
