package com.freshollie.radioapp;

import android.media.session.MediaSession.*;
import android.view.KeyEvent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.content.Intent;
import anywheresoftware.b4a.objects.IntentWrapper;

import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.objects.ServiceHelper;
import anywheresoftware.b4a.debug.*;

public class radioservice extends android.app.Service {
	public static class radioservice_BR extends android.content.BroadcastReceiver {

		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			android.content.Intent in = new android.content.Intent(context, radioservice.class);
			if (intent != null)
				in.putExtra("b4a_internal_intent", intent);
			context.startService(in);
		}

	}
    static radioservice mostCurrent;
	public static BA processBA;
    private ServiceHelper _service;
    public static Class<?> getObject() {
		return radioservice.class;
	}
	@Override
	public void onCreate() {
        mostCurrent = this;
        if (processBA == null) {
		    processBA = new BA(this, null, null, "com.freshollie.radioapp", "com.freshollie.radioapp.radioservice");
            try {
                Class.forName(BA.applicationContext.getPackageName() + ".main").getMethod("initializeProcessGlobals").invoke(null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            processBA.loadHtSubs(this.getClass());
            ServiceHelper.init();
        }
        _service = new ServiceHelper(this);
        processBA.service = this;
        processBA.setActivityPaused(false);
        if (BA.isShellModeRuntimeCheck(processBA)) {
			processBA.raiseEvent2(null, true, "CREATE", true, "com.freshollie.radioapp.radioservice", processBA, _service);
		}
        BA.LogInfo("** Service (radioservice) Create **");
        processBA.raiseEvent(null, "service_create");
        processBA.runHook("oncreate", this, null);
    }
		@Override
	public void onStart(android.content.Intent intent, int startId) {
		handleStart(intent);
    }
    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId) {
    	handleStart(intent);
        processBA.runHook("onstartcommand", this, new Object[] {intent, flags, startId});
		return android.app.Service.START_NOT_STICKY;
    }
    private void handleStart(android.content.Intent intent) {
    	BA.LogInfo("** Service (radioservice) Start **");
    	java.lang.reflect.Method startEvent = processBA.htSubs.get("service_start");
    	if (startEvent != null) {
    		if (startEvent.getParameterTypes().length > 0) {
    			anywheresoftware.b4a.objects.IntentWrapper iw = new anywheresoftware.b4a.objects.IntentWrapper();
    			if (intent != null) {
    				if (intent.hasExtra("b4a_internal_intent"))
    					iw.setObject((android.content.Intent) intent.getParcelableExtra("b4a_internal_intent"));
    				else
    					iw.setObject(intent);
    			}
    			processBA.raiseEvent(null, "service_start", iw);
    		}
    		else {
    			processBA.raiseEvent(null, "service_start");
    		}
    	}
    }
	@Override
	public android.os.IBinder onBind(android.content.Intent intent) {
		return null;
	}
	@Override
	public void onDestroy() {
        BA.LogInfo("** Service (radioservice) Destroy **");
		processBA.raiseEvent(null, "service_destroy");
        processBA.service = null;
		mostCurrent = null;
		processBA.setActivityPaused(true);
        processBA.runHook("ondestroy", this, null);
	}
public anywheresoftware.b4a.keywords.Common __c = null;
public static anywheresoftware.b4a.objects.UsbSerial _usb = null;
public static int _usbpid = 0;
public static int _usbvid = 0;
public static anywheresoftware.b4a.randomaccessfile.AsyncStreams _astreams = null;
public static anywheresoftware.b4a.objects.Timer _mytimer = null;
public static anywheresoftware.b4a.objects.Timer _sysready = null;
public static anywheresoftware.b4a.objects.Timer _usbtimer = null;
public static com.rootsoft.broadcastreceiver.BroadCastReceiver _broadcast = null;
public static com.freshollie.audiofocus.AudioFocus _audiofocusmanager = null;
public static boolean _start = false;
public static boolean _mute = false;
public static boolean _dabsearch = false;
public static boolean _isdab = false;
public static boolean _dab = false;
public static boolean _connected = false;
public static boolean _filllist = false;
public static boolean _cleardatabase = false;
public static boolean _ducked = false;
public static byte _volume = (byte)0;
public static byte _strength = (byte)0;
public static int[] _ack = null;
public static int[] _frq = null;
public static int[] _dfrq = null;
public static int _frequenz = 0;
public static int _iindex = 0;
public static int _xindex = 0;
public static int _dev = 0;
public static int _ebene = 0;
public static int _alldab = 0;
public static int _istep = 0;
public static int _iloop = 0;
public static anywheresoftware.b4a.objects.collections.List _lstdab = null;
public static barxdroid.NotificationBuilder.NotificationBuilder _radionotification = null;
public static String _programtext = "";
public static String _programtype = "";
public static String _programname = "";
public static String _programname2 = "";
public static String _esamble = "";
public static String _status = "";
public static String _labeventtext = "";
public static String _labfreqtext = "";
public static String _labstrengthtext = "";
public static String _labvolumetext = "";
public static String _labprogramtext = "";
public static String _labprogramtypetext = "";
public static String _labprogram2text = "";
public static String _programnametext = "";
public static String _stereomodetext = "";
public static String _dataratetext = "";
public static double _pbstrengthprogress = 0;
public static String _mypath = "";
public static String _previousnotificationtext = "";
public static String _previousnotificationtext2 = "";
public static boolean _servicestarted = false;
public static com.omnicorp.media.MediaController _mediakey = null;
public static int _duckvolume = 0;
public static int _defaultvolume = 0;
public static int _lastvolume = 0;
public static int _part = 0;
public static anywheresoftware.b4j.object.JavaObject _session = null;
public static boolean _enterclickedreturnvalue = false;
public wal.INIFiles.ini _ini = null;
public com.freshollie.radioapp.main _main = null;
public com.freshollie.radioapp.slideshow _slideshow = null;
public static boolean  _application_error(anywheresoftware.b4a.objects.B4AException _error,String _stacktrace) throws Exception{
 //BA.debugLineNum = 1012;BA.debugLine="Sub Application_Error (Error As Exception, StackTr";
 //BA.debugLineNum = 1013;BA.debugLine="Return True";
if (true) return anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 1014;BA.debugLine="End Sub";
return false;
}
public static String  _astreams_error() throws Exception{
 //BA.debugLineNum = 586;BA.debugLine="Sub Astreams_Error";
 //BA.debugLineNum = 587;BA.debugLine="astreams.Close";
_astreams.Close();
 //BA.debugLineNum = 588;BA.debugLine="ExitApp";
_exitapp();
 //BA.debugLineNum = 589;BA.debugLine="End Sub";
return "";
}
public static String  _astreams_newdata(byte[] _buffer) throws Exception{
int _itemp = 0;
int _y = 0;
 //BA.debugLineNum = 562;BA.debugLine="Sub Astreams_NewData (buffer() As Byte)";
 //BA.debugLineNum = 563;BA.debugLine="Dim itemp As Int";
_itemp = 0;
 //BA.debugLineNum = 564;BA.debugLine="Dim y As Int";
_y = 0;
 //BA.debugLineNum = 566;BA.debugLine="For y = 0 To buffer.Length -1";
{
final int step455 = 1;
final int limit455 = (int) (_buffer.length-1);
for (_y = (int) (0); (step455 > 0 && _y <= limit455) || (step455 < 0 && _y >= limit455); _y = ((int)(0 + _y + step455))) {
 //BA.debugLineNum = 567;BA.debugLine="itemp = Bit.And(0xff, buffer(y))";
_itemp = anywheresoftware.b4a.keywords.Common.Bit.And((int) (0xff),(int) (_buffer[_y]));
 //BA.debugLineNum = 568;BA.debugLine="If itemp = 254 Then";
if (_itemp==254) { 
 //BA.debugLineNum = 569;BA.debugLine="Start = True";
_start = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 570;BA.debugLine="xIndex = 0";
_xindex = (int) (0);
 };
 //BA.debugLineNum = 573;BA.debugLine="If Start Then";
if (_start) { 
 //BA.debugLineNum = 574;BA.debugLine="Ack(xIndex) = itemp";
_ack[_xindex] = _itemp;
 //BA.debugLineNum = 575;BA.debugLine="xIndex = xIndex + 1";
_xindex = (int) (_xindex+1);
 };
 //BA.debugLineNum = 578;BA.debugLine="If itemp = 253 Then";
if (_itemp==253) { 
 //BA.debugLineNum = 579;BA.debugLine="Start = False";
_start = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 580;BA.debugLine="Evaluate(xIndex - 1)";
_evaluate((int) (_xindex-1));
 };
 }
};
 //BA.debugLineNum = 584;BA.debugLine="End Sub";
return "";
}
public static String  _astreams_terminated() throws Exception{
 //BA.debugLineNum = 591;BA.debugLine="Sub Astreams_Terminated";
 //BA.debugLineNum = 592;BA.debugLine="ExitApp";
_exitapp();
 //BA.debugLineNum = 593;BA.debugLine="astreams.Close";
_astreams.Close();
 //BA.debugLineNum = 594;BA.debugLine="End Sub";
return "";
}
public static String  _audiofocusmanager_onfocuslost() throws Exception{
 //BA.debugLineNum = 910;BA.debugLine="Sub AudioFocusManager_onFocusLost";
 //BA.debugLineNum = 911;BA.debugLine="Log(\"Focus Lost, closing\")";
anywheresoftware.b4a.keywords.Common.Log("Focus Lost, closing");
 //BA.debugLineNum = 912;BA.debugLine="ExitApp";
_exitapp();
 //BA.debugLineNum = 913;BA.debugLine="End Sub";
return "";
}
public static String  _audiofocusmanager_ongain() throws Exception{
 //BA.debugLineNum = 927;BA.debugLine="Sub AudioFocusManager_onGain";
 //BA.debugLineNum = 928;BA.debugLine="Log(\"Back to normal, gained focus\")";
anywheresoftware.b4a.keywords.Common.Log("Back to normal, gained focus");
 //BA.debugLineNum = 929;BA.debugLine="Ducked = False";
_ducked = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 930;BA.debugLine="UnmuteAudio";
_unmuteaudio();
 //BA.debugLineNum = 931;BA.debugLine="End Sub";
return "";
}
public static String  _audiofocusmanager_ontransient() throws Exception{
 //BA.debugLineNum = 915;BA.debugLine="Sub AudioFocusManager_onTransient";
 //BA.debugLineNum = 916;BA.debugLine="Log(\"Muting, on transient\")";
anywheresoftware.b4a.keywords.Common.Log("Muting, on transient");
 //BA.debugLineNum = 917;BA.debugLine="MuteAudio";
_muteaudio();
 //BA.debugLineNum = 918;BA.debugLine="End Sub";
return "";
}
public static String  _audiofocusmanager_ontransientcanduck() throws Exception{
 //BA.debugLineNum = 920;BA.debugLine="Sub AudioFocusManager_onTransientCanDuck";
 //BA.debugLineNum = 921;BA.debugLine="Log(\"Lowering volume, on transient can duck\")";
anywheresoftware.b4a.keywords.Common.Log("Lowering volume, on transient can duck");
 //BA.debugLineNum = 922;BA.debugLine="LastVolume = Volume";
_lastvolume = (int) (_volume);
 //BA.debugLineNum = 923;BA.debugLine="Ducked = True";
_ducked = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 924;BA.debugLine="SetVolume(DuckVolume)";
_setvolume(_duckvolume);
 //BA.debugLineNum = 925;BA.debugLine="End Sub";
return "";
}
public static String  _broadcastreceiver_onreceive(String _action,Object _i) throws Exception{
anywheresoftware.b4a.objects.IntentWrapper _intent1 = null;
 //BA.debugLineNum = 888;BA.debugLine="Sub BroadcastReceiver_OnReceive(Action As String,i";
 //BA.debugLineNum = 889;BA.debugLine="Dim Intent1 As Intent = i";
_intent1 = new anywheresoftware.b4a.objects.IntentWrapper();
_intent1.setObject((android.content.Intent)(_i));
 //BA.debugLineNum = 890;BA.debugLine="Log(Action)";
anywheresoftware.b4a.keywords.Common.Log(_action);
 //BA.debugLineNum = 892;BA.debugLine="If Intent1.HasExtra(\"device\") Then";
if (_intent1.HasExtra("device")) { 
 //BA.debugLineNum = 893;BA.debugLine="If USB.UsbPresent(Dev) = USB.USB_NONE Then";
if (_usb.UsbPresent(_dev)==_usb.USB_NONE) { 
 //BA.debugLineNum = 894;BA.debugLine="ExitApp";
_exitapp();
 };
 };
 //BA.debugLineNum = 898;BA.debugLine="If Action = \"com.freshollie.radioapp.intent.close";
if ((_action).equals("com.freshollie.radioapp.intent.close")) { 
 //BA.debugLineNum = 899;BA.debugLine="ExitApp";
_exitapp();
 }else if((_action).equals("com.freshollie.radioapp.intent.mute")) { 
 //BA.debugLineNum = 902;BA.debugLine="MuteAudio";
_muteaudio();
 }else if((_action).equals("com.freshollie.radioapp.intent.unmute")) { 
 //BA.debugLineNum = 905;BA.debugLine="UnmuteAudio";
_unmuteaudio();
 };
 //BA.debugLineNum = 908;BA.debugLine="End Sub";
return "";
}
public static String  _changedablevel() throws Exception{
 //BA.debugLineNum = 727;BA.debugLine="Sub ChangeDABLevel";
 //BA.debugLineNum = 728;BA.debugLine="Select Ebene";
switch (_ebene) {
case 0:
 //BA.debugLineNum = 730;BA.debugLine="DFrq(0) = Frequenz";
_dfrq[(int) (0)] = _frequenz;
 break;
case 1:
 //BA.debugLineNum = 732;BA.debugLine="DFrq(7) = Frequenz";
_dfrq[(int) (7)] = _frequenz;
 break;
case 2:
 //BA.debugLineNum = 734;BA.debugLine="DFrq(14) = Frequenz";
_dfrq[(int) (14)] = _frequenz;
 break;
}
;
 //BA.debugLineNum = 736;BA.debugLine="End Sub";
return "";
}
public static String  _changefmlevel() throws Exception{
 //BA.debugLineNum = 701;BA.debugLine="Sub ChangeFMLevel";
 //BA.debugLineNum = 702;BA.debugLine="If Not(isDAB) Then";
if (anywheresoftware.b4a.keywords.Common.Not(_isdab)) { 
 //BA.debugLineNum = 703;BA.debugLine="Select Ebene";
switch (_ebene) {
case 0:
 //BA.debugLineNum = 705;BA.debugLine="Frq(0) = Frequenz";
_frq[(int) (0)] = _frequenz;
 break;
case 1:
 //BA.debugLineNum = 707;BA.debugLine="Frq(7) = Frequenz";
_frq[(int) (7)] = _frequenz;
 break;
case 2:
 //BA.debugLineNum = 709;BA.debugLine="Frq(14) = Frequenz";
_frq[(int) (14)] = _frequenz;
 break;
}
;
 };
 //BA.debugLineNum = 712;BA.debugLine="End Sub";
return "";
}
public static String  _closeradio() throws Exception{
 //BA.debugLineNum = 440;BA.debugLine="Sub CloseRadio";
 //BA.debugLineNum = 441;BA.debugLine="USB.Close()";
_usb.Close();
 //BA.debugLineNum = 442;BA.debugLine="End Sub";
return "";
}
public static String  _dtr(boolean _bool) throws Exception{
anywheresoftware.b4a.agraham.reflection.Reflection _r = null;
 //BA.debugLineNum = 362;BA.debugLine="Sub DTR(Bool As Boolean)";
 //BA.debugLineNum = 363;BA.debugLine="Dim r As Reflector";
_r = new anywheresoftware.b4a.agraham.reflection.Reflection();
 //BA.debugLineNum = 364;BA.debugLine="r.Target = USB";
_r.Target = (Object)(_usb);
 //BA.debugLineNum = 365;BA.debugLine="r.Target = r.getField(\"driver\")";
_r.Target = _r.GetField("driver");
 //BA.debugLineNum = 366;BA.debugLine="r.RunMethod2(\"setDTR\", Bool, \"java.lang.boolean\")";
_r.RunMethod2("setDTR",BA.ObjectToString(_bool),"java.lang.boolean");
 //BA.debugLineNum = 367;BA.debugLine="End Sub";
return "";
}
public static String  _enterfrequency(String _frequencytext) throws Exception{
int _f = 0;
String _d1 = "";
String _d2 = "";
String _d3 = "";
String _d = "";
 //BA.debugLineNum = 659;BA.debugLine="Sub EnterFrequency(FrequencyText As String)";
 //BA.debugLineNum = 660;BA.debugLine="Dim F As Int";
_f = 0;
 //BA.debugLineNum = 662;BA.debugLine="If isDAB And DAB Then";
if (_isdab && _dab) { 
 //BA.debugLineNum = 663;BA.debugLine="F = FrequencyText";
_f = (int)(Double.parseDouble(_frequencytext));
 //BA.debugLineNum = 664;BA.debugLine="If Not(DABSearch) Then SendRadio(Array As Byte(0";
if (anywheresoftware.b4a.keywords.Common.Not(_dabsearch)) { 
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_f),(byte) (0xfd)});};
 //BA.debugLineNum = 665;BA.debugLine="EnterClickedReturnValue = True";
_enterclickedreturnvalue = anywheresoftware.b4a.keywords.Common.True;
 }else {
 //BA.debugLineNum = 667;BA.debugLine="F = FrequencyText * 1000";
_f = (int) ((double)(Double.parseDouble(_frequencytext))*1000);
 //BA.debugLineNum = 668;BA.debugLine="If F > 87400 And F < 108100 Then";
if (_f>87400 && _f<108100) { 
 //BA.debugLineNum = 669;BA.debugLine="Dim d1,d2,d3,D As String";
_d1 = "";
_d2 = "";
_d3 = "";
_d = "";
 //BA.debugLineNum = 670;BA.debugLine="D =  Bit.ToHexString(F)";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_f);
 //BA.debugLineNum = 671;BA.debugLine="d1 = Bit.ParseInt(D.SubString2(0,1), 16)";
_d1 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (0),(int) (1)),(int) (16)));
 //BA.debugLineNum = 672;BA.debugLine="d2 = Bit.ParseInt(D.SubString2(1,3), 16)";
_d2 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (1),(int) (3)),(int) (16)));
 //BA.debugLineNum = 673;BA.debugLine="d3 = Bit.ParseInt(D.SubString2(3,5), 16)";
_d3 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (3),(int) (5)),(int) (16)));
 //BA.debugLineNum = 674;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte)(Double.parseDouble(_d1)),(byte)(Double.parseDouble(_d2)),(byte)(Double.parseDouble(_d3)),(byte) (0xfd)});
 //BA.debugLineNum = 675;BA.debugLine="EnterClickedReturnValue = True";
_enterclickedreturnvalue = anywheresoftware.b4a.keywords.Common.True;
 }else {
 //BA.debugLineNum = 677;BA.debugLine="EnterClickedReturnValue = False";
_enterclickedreturnvalue = anywheresoftware.b4a.keywords.Common.False;
 };
 };
 //BA.debugLineNum = 682;BA.debugLine="End Sub";
return "";
}
public static String  _evaluate(int _index) throws Exception{
int _j = 0;
anywheresoftware.b4a.objects.collections.List _argumentslist = null;
byte[] _b = null;
boolean _changed = false;
 //BA.debugLineNum = 50;BA.debugLine="Sub Evaluate(index As Int)";
 //BA.debugLineNum = 54;BA.debugLine="Dim j As Int";
_j = 0;
 //BA.debugLineNum = 55;BA.debugLine="Dim ArgumentsList As List";
_argumentslist = new anywheresoftware.b4a.objects.collections.List();
 //BA.debugLineNum = 63;BA.debugLine="Select Ack(1)";
switch (BA.switchObjectToInt(_ack[(int) (1)],(int) (0x01),(int) (0x00))) {
case 0:
 //BA.debugLineNum = 65;BA.debugLine="Select Ack(2)";
switch (BA.switchObjectToInt(_ack[(int) (2)],(int) (0x05),(int) (0x07),(int) (0x08),(int) (0x0b),(int) (0x0d),(int) (0x0f),(int) (0x0e),(int) (0x10),(int) (0x12),(int) (0x13),(int) (0x14),(int) (0x15),(int) (0x16),(int) (0x1a))) {
case 0:
 //BA.debugLineNum = 68;BA.debugLine="Select Case Ack(6)";
switch (BA.switchObjectToInt(_ack[(int) (6)],(int) (0),(int) (1),(int) (2),(int) (3),(int) (4),(int) (5))) {
case 0:
 //BA.debugLineNum = 71;BA.debugLine="Status = \"Playing\"";
_status = "Playing";
 break;
case 1:
 //BA.debugLineNum = 73;BA.debugLine="Status = \"Searching\"";
_status = "Searching";
 break;
case 2:
 //BA.debugLineNum = 75;BA.debugLine="Status = \"Tunning\"";
_status = "Tunning";
 break;
case 3:
 //BA.debugLineNum = 77;BA.debugLine="Status = \"Stop\"";
_status = "Stop";
 break;
case 4:
 //BA.debugLineNum = 79;BA.debugLine="Status = \"Sorting\"";
_status = "Sorting";
 break;
case 5:
 //BA.debugLineNum = 81;BA.debugLine="Status = \"Reconfiguration\"";
_status = "Reconfiguration";
 break;
default:
 //BA.debugLineNum = 83;BA.debugLine="Status = \"N/A\"";
_status = "N/A";
 break;
}
;
 //BA.debugLineNum = 86;BA.debugLine="labEventText = Status";
_labeventtext = _status;
 break;
case 1:
 //BA.debugLineNum = 89;BA.debugLine="If Ack(6) = 0 Then";
if (_ack[(int) (6)]==0) { 
 //BA.debugLineNum = 90;BA.debugLine="Frequenz = Bit.ParseInt(Bit.ToHexString(Ack(7)";
_frequenz = anywheresoftware.b4a.keywords.Common.Bit.ParseInt(anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_ack[(int) (7)])+anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_ack[(int) (8)])+anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_ack[(int) (9)]),(int) (16));
 //BA.debugLineNum = 91;BA.debugLine="If isDAB And DAB Then";
if (_isdab && _dab) { 
 //BA.debugLineNum = 92;BA.debugLine="labFreqText = Frequenz";
_labfreqtext = BA.NumberToString(_frequenz);
 }else {
 //BA.debugLineNum = 94;BA.debugLine="If Frequenz < 108100 And Frequenz > 87400 The";
if (_frequenz<108100 && _frequenz>87400) { 
_labfreqtext = BA.NumberToString(_frequenz/(double)1000);};
 };
 };
 break;
case 2:
 //BA.debugLineNum = 99;BA.debugLine="Strength = Ack(6)";
_strength = (byte) (_ack[(int) (6)]);
 //BA.debugLineNum = 100;BA.debugLine="If isDAB Then";
if (_isdab) { 
 //BA.debugLineNum = 101;BA.debugLine="labStrengthText = Strength * 6";
_labstrengthtext = BA.NumberToString(_strength*6);
 //BA.debugLineNum = 102;BA.debugLine="pbStrengthProgress = Strength * 6";
_pbstrengthprogress = _strength*6;
 }else {
 //BA.debugLineNum = 104;BA.debugLine="labStrengthText = Strength";
_labstrengthtext = BA.NumberToString(_strength);
 //BA.debugLineNum = 105;BA.debugLine="pbStrengthProgress = Strength";
_pbstrengthprogress = _strength;
 };
 break;
case 3:
 //BA.debugLineNum = 109;BA.debugLine="Select Ack(6)";
switch (BA.switchObjectToInt(_ack[(int) (6)],(int) (0),(int) (1),(int) (2),(int) (3))) {
case 0:
 break;
case 1:
 //BA.debugLineNum = 114;BA.debugLine="StereoModeText = \"Stereo\"";
_stereomodetext = "Stereo";
 break;
case 2:
 //BA.debugLineNum = 116;BA.debugLine="StereoModeText = \"Joint Stereo\"";
_stereomodetext = "Joint Stereo";
 break;
case 3:
 //BA.debugLineNum = 118;BA.debugLine="StereoModeText = \"Dual Channel\"";
_stereomodetext = "Dual Channel";
 break;
case 4:
 //BA.debugLineNum = 120;BA.debugLine="StereoModeText = \"Mono\"";
_stereomodetext = "Mono";
 break;
}
;
 break;
case 4:
 //BA.debugLineNum = 125;BA.debugLine="If Mute Then";
if (_mute) { 
 //BA.debugLineNum = 126;BA.debugLine="labVolumeText = \"--\"";
_labvolumetext = "--";
 }else {
 //BA.debugLineNum = 128;BA.debugLine="labVolumeText = Volume";
_labvolumetext = BA.NumberToString(_volume);
 };
 break;
case 5:
 //BA.debugLineNum = 132;BA.debugLine="ProgramName = \"\"";
_programname = "";
 //BA.debugLineNum = 133;BA.debugLine="For j = 6 To index - 1";
{
final int step94 = 1;
final int limit94 = (int) (_index-1);
for (_j = (int) (6); (step94 > 0 && _j <= limit94) || (step94 < 0 && _j >= limit94); _j = ((int)(0 + _j + step94))) {
 //BA.debugLineNum = 134;BA.debugLine="ProgramName = ProgramName & Chr(Ack(j))";
_programname = _programname+BA.ObjectToString(anywheresoftware.b4a.keywords.Common.Chr(_ack[_j]));
 }
};
 //BA.debugLineNum = 136;BA.debugLine="ProgramNameText = ProgramName";
_programnametext = _programname;
 break;
case 6:
 //BA.debugLineNum = 139;BA.debugLine="Select Case Ack(6)";
switch (BA.switchObjectToInt(_ack[(int) (6)],(int) (0),(int) (1),(int) (2),(int) (3),(int) (4),(int) (5),(int) (6),(int) (7),(int) (8),(int) (9),(int) (10),(int) (11),(int) (12),(int) (13),(int) (14),(int) (15),(int) (16),(int) (17),(int) (18),(int) (19),(int) (20),(int) (21),(int) (22),(int) (23),(int) (24),(int) (25),(int) (26),(int) (27),(int) (28),(int) (29),(int) (30),(int) (31))) {
case 0:
 //BA.debugLineNum = 141;BA.debugLine="ProgramType = \"\"";
_programtype = "";
 break;
case 1:
 //BA.debugLineNum = 143;BA.debugLine="ProgramType = \"News\"";
_programtype = "News";
 break;
case 2:
 //BA.debugLineNum = 145;BA.debugLine="ProgramType = \"Current Affairs\"";
_programtype = "Current Affairs";
 break;
case 3:
 //BA.debugLineNum = 147;BA.debugLine="ProgramType = \"Information\"";
_programtype = "Information";
 break;
case 4:
 //BA.debugLineNum = 149;BA.debugLine="ProgramType = \"Sport\"";
_programtype = "Sport";
 break;
case 5:
 //BA.debugLineNum = 151;BA.debugLine="ProgramType = \"Education\"";
_programtype = "Education";
 break;
case 6:
 //BA.debugLineNum = 153;BA.debugLine="ProgramType = \"Drama\"";
_programtype = "Drama";
 break;
case 7:
 //BA.debugLineNum = 155;BA.debugLine="ProgramType = \"Arts\"";
_programtype = "Arts";
 break;
case 8:
 //BA.debugLineNum = 157;BA.debugLine="ProgramType = \"Science\"";
_programtype = "Science";
 break;
case 9:
 //BA.debugLineNum = 159;BA.debugLine="ProgramType = \"Talk\"";
_programtype = "Talk";
 break;
case 10:
 //BA.debugLineNum = 161;BA.debugLine="ProgramType = \"Pop Music\"";
_programtype = "Pop Music";
 break;
case 11:
 //BA.debugLineNum = 163;BA.debugLine="ProgramType = \"Rock Music\"";
_programtype = "Rock Music";
 break;
case 12:
 //BA.debugLineNum = 165;BA.debugLine="ProgramType = \"Easy Listening\"";
_programtype = "Easy Listening";
 break;
case 13:
 //BA.debugLineNum = 167;BA.debugLine="ProgramType = \"Light Classical\"";
_programtype = "Light Classical";
 break;
case 14:
 //BA.debugLineNum = 169;BA.debugLine="ProgramType = \"Classical Music\"";
_programtype = "Classical Music";
 break;
case 15:
 //BA.debugLineNum = 171;BA.debugLine="ProgramType = \"Other Music\"";
_programtype = "Other Music";
 break;
case 16:
 //BA.debugLineNum = 173;BA.debugLine="ProgramType = \"Weather\"";
_programtype = "Weather";
 break;
case 17:
 //BA.debugLineNum = 175;BA.debugLine="ProgramType = \"Finance\"";
_programtype = "Finance";
 break;
case 18:
 //BA.debugLineNum = 177;BA.debugLine="ProgramType = \"Children\"";
_programtype = "Children";
 break;
case 19:
 //BA.debugLineNum = 179;BA.debugLine="ProgramType = \"Factual\"";
_programtype = "Factual";
 break;
case 20:
 //BA.debugLineNum = 181;BA.debugLine="ProgramType = \"Religion\"";
_programtype = "Religion";
 break;
case 21:
 //BA.debugLineNum = 183;BA.debugLine="ProgramType = \"Phone In\"";
_programtype = "Phone In";
 break;
case 22:
 //BA.debugLineNum = 185;BA.debugLine="ProgramType = \"Travel\"";
_programtype = "Travel";
 break;
case 23:
 //BA.debugLineNum = 187;BA.debugLine="ProgramType = \"Leisure\"";
_programtype = "Leisure";
 break;
case 24:
 //BA.debugLineNum = 189;BA.debugLine="ProgramType = \"Jazz and Blues\"";
_programtype = "Jazz and Blues";
 break;
case 25:
 //BA.debugLineNum = 191;BA.debugLine="ProgramType = \"Country Music\"";
_programtype = "Country Music";
 break;
case 26:
 //BA.debugLineNum = 193;BA.debugLine="ProgramType = \"National Music\"";
_programtype = "National Music";
 break;
case 27:
 //BA.debugLineNum = 195;BA.debugLine="ProgramType = \"Oldies Music\"";
_programtype = "Oldies Music";
 break;
case 28:
 //BA.debugLineNum = 197;BA.debugLine="ProgramType = \"Folk Music\"";
_programtype = "Folk Music";
 break;
case 29:
 //BA.debugLineNum = 199;BA.debugLine="ProgramType = \"Documentary\"";
_programtype = "Documentary";
 break;
case 30:
 //BA.debugLineNum = 201;BA.debugLine="ProgramType = \"Undefined\"";
_programtype = "Undefined";
 break;
case 31:
 //BA.debugLineNum = 203;BA.debugLine="ProgramType = \"Undefined\"";
_programtype = "Undefined";
 break;
default:
 //BA.debugLineNum = 205;BA.debugLine="ProgramType = \"\"";
_programtype = "";
 break;
}
;
 //BA.debugLineNum = 208;BA.debugLine="labProgramTypeText = ProgramType";
_labprogramtypetext = _programtype;
 break;
case 7:
 //BA.debugLineNum = 211;BA.debugLine="ProgramText = \"\"";
_programtext = "";
 //BA.debugLineNum = 212;BA.debugLine="For j = 6 To index - 1";
{
final int step170 = 1;
final int limit170 = (int) (_index-1);
for (_j = (int) (6); (step170 > 0 && _j <= limit170) || (step170 < 0 && _j >= limit170); _j = ((int)(0 + _j + step170))) {
 //BA.debugLineNum = 213;BA.debugLine="ProgramText  = ProgramText  & Chr(Ack(j))";
_programtext = _programtext+BA.ObjectToString(anywheresoftware.b4a.keywords.Common.Chr(_ack[_j]));
 }
};
 //BA.debugLineNum = 215;BA.debugLine="labProgramText = ProgramText";
_labprogramtext = _programtext;
 break;
case 8:
 //BA.debugLineNum = 219;BA.debugLine="DataRateText = Floor(Ack(6) & Ack(7)) & \" Kbps\"";
_dataratetext = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Floor((double)(Double.parseDouble(BA.NumberToString(_ack[(int) (6)])+BA.NumberToString(_ack[(int) (7)])))))+" Kbps";
 break;
case 9:
 //BA.debugLineNum = 222;BA.debugLine="Strength = Ack(6)";
_strength = (byte) (_ack[(int) (6)]);
 //BA.debugLineNum = 223;BA.debugLine="labStrengthText = Strength";
_labstrengthtext = BA.NumberToString(_strength);
 //BA.debugLineNum = 224;BA.debugLine="pbStrengthProgress = Strength";
_pbstrengthprogress = _strength;
 break;
case 10:
 //BA.debugLineNum = 227;BA.debugLine="If Ack(6) = 0x47 Then";
if (_ack[(int) (6)]==0x47) { 
 //BA.debugLineNum = 228;BA.debugLine="DABSearch = False";
_dabsearch = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 229;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x16,0x01,0x";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x16),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 }else {
 //BA.debugLineNum = 231;BA.debugLine="labEventText = \"DABSearch ...  \" & Ack(6)";
_labeventtext = "DABSearch ...  "+BA.NumberToString(_ack[(int) (6)]);
 };
 break;
case 11:
 //BA.debugLineNum = 235;BA.debugLine="Esamble = \"\"";
_esamble = "";
 //BA.debugLineNum = 236;BA.debugLine="For j = 6 To index - 1";
{
final int step189 = 1;
final int limit189 = (int) (_index-1);
for (_j = (int) (6); (step189 > 0 && _j <= limit189) || (step189 < 0 && _j >= limit189); _j = ((int)(0 + _j + step189))) {
 //BA.debugLineNum = 237;BA.debugLine="Esamble = Esamble & Chr(Ack(j))";
_esamble = _esamble+BA.ObjectToString(anywheresoftware.b4a.keywords.Common.Chr(_ack[_j]));
 }
};
 //BA.debugLineNum = 239;BA.debugLine="labProgram2Text = Esamble";
_labprogram2text = _esamble;
 break;
case 12:
 //BA.debugLineNum = 242;BA.debugLine="AllDAB = Bit.ParseInt(Bit.ToHexString(Ack(6)) &";
_alldab = anywheresoftware.b4a.keywords.Common.Bit.ParseInt(anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_ack[(int) (6)])+anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_ack[(int) (7)])+anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_ack[(int) (8)])+anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_ack[(int) (9)]),(int) (16));
 //BA.debugLineNum = 243;BA.debugLine="labEventText = AllDAB & \" please wait !\"";
_labeventtext = BA.NumberToString(_alldab)+" please wait !";
 //BA.debugLineNum = 244;BA.debugLine="lstDAB.Clear";
_lstdab.Clear();
 //BA.debugLineNum = 246;BA.debugLine="iStep = 0";
_istep = (int) (0);
 //BA.debugLineNum = 247;BA.debugLine="MyTimer.Interval = 500";
_mytimer.setInterval((long) (500));
 //BA.debugLineNum = 248;BA.debugLine="Wait(3)";
_wait((int) (3));
 //BA.debugLineNum = 249;BA.debugLine="FillList = True";
_filllist = anywheresoftware.b4a.keywords.Common.True;
 break;
case 13:
 //BA.debugLineNum = 252;BA.debugLine="ProgramName2 = \"\"";
_programname2 = "";
 //BA.debugLineNum = 253;BA.debugLine="For j = 6 To index - 1";
{
final int step203 = 1;
final int limit203 = (int) (_index-1);
for (_j = (int) (6); (step203 > 0 && _j <= limit203) || (step203 < 0 && _j >= limit203); _j = ((int)(0 + _j + step203))) {
 //BA.debugLineNum = 254;BA.debugLine="If Ack(j) <> 0 Then";
if (_ack[_j]!=0) { 
 //BA.debugLineNum = 255;BA.debugLine="ProgramName2 = ProgramName2 & Chr(Ack(j))";
_programname2 = _programname2+BA.ObjectToString(anywheresoftware.b4a.keywords.Common.Chr(_ack[_j]));
 };
 }
};
 //BA.debugLineNum = 259;BA.debugLine="If FillList Then";
if (_filllist) { 
 //BA.debugLineNum = 260;BA.debugLine="Log(ProgramName2)";
anywheresoftware.b4a.keywords.Common.Log(_programname2);
 //BA.debugLineNum = 261;BA.debugLine="iStep = lstDAB.Size+1";
_istep = (int) (_lstdab.getSize()+1);
 //BA.debugLineNum = 262;BA.debugLine="lstDAB.Add(ProgramName2)";
_lstdab.Add((Object)(_programname2));
 }else {
 //BA.debugLineNum = 264;BA.debugLine="Dim b() As Byte = ProgramName2.GetBytes(\"UTF8\"";
_b = _programname2.getBytes("UTF8");
 //BA.debugLineNum = 265;BA.debugLine="ProgramName2 = BytesToString(b, 0, b.Length,";
_programname2 = anywheresoftware.b4a.keywords.Common.BytesToString(_b,(int) (0),_b.length,"UTF8");
 //BA.debugLineNum = 266;BA.debugLine="ProgramNameText = ProgramName2";
_programnametext = _programname2;
 //BA.debugLineNum = 267;BA.debugLine="ProgramName2=ProgramName2.Replace(\" \",\"\")";
_programname2 = _programname2.replace(" ","");
 };
 break;
}
;
 break;
case 1:
 //BA.debugLineNum = 273;BA.debugLine="If Ack(2) = 0x01 And Ack(3) = 0x01 And ClearData";
if (_ack[(int) (2)]==0x01 && _ack[(int) (3)]==0x01 && _cleardatabase) { 
 //BA.debugLineNum = 274;BA.debugLine="iLoop = iLoop +1";
_iloop = (int) (_iloop+1);
 //BA.debugLineNum = 275;BA.debugLine="If iLoop > 2 Then";
if (_iloop>2) { 
 //BA.debugLineNum = 276;BA.debugLine="Log(\"What\")";
anywheresoftware.b4a.keywords.Common.Log("What");
 //BA.debugLineNum = 277;BA.debugLine="SysReady.Enabled = False";
_sysready.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 278;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x03,0x01,0x";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x03),(byte) (0x01),(byte) (0x00),(byte) (0x02),(byte) (0x00),(byte) (0x47),(byte) (0xfd)});
 //BA.debugLineNum = 279;BA.debugLine="DABSearch = True";
_dabsearch = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 280;BA.debugLine="iLoop = 0";
_iloop = (int) (0);
 //BA.debugLineNum = 281;BA.debugLine="MyTimer.Enabled = True";
_mytimer.setEnabled(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 282;BA.debugLine="ClearDatabase = False";
_cleardatabase = anywheresoftware.b4a.keywords.Common.False;
 };
 }else {
 //BA.debugLineNum = 286;BA.debugLine="If iIndex < 3 Then";
if (_iindex<3) { 
 //BA.debugLineNum = 288;BA.debugLine="Select(iIndex)";
switch (BA.switchObjectToInt((_iindex),(int) ((0)),(int) ((1)),(int) ((2)))) {
case 0:
 //BA.debugLineNum = 291;BA.debugLine="If part = 1 Then iIndex = iIndex + 1";
if (_part==1) { 
_iindex = (int) (_iindex+1);};
 //BA.debugLineNum = 292;BA.debugLine="Log(\"Responded\")";
anywheresoftware.b4a.keywords.Common.Log("Responded");
 break;
case 1:
 //BA.debugLineNum = 294;BA.debugLine="If part = 2 Then iIndex = iIndex + 1";
if (_part==2) { 
_iindex = (int) (_iindex+1);};
 //BA.debugLineNum = 295;BA.debugLine="Log(\"Responded\")";
anywheresoftware.b4a.keywords.Common.Log("Responded");
 break;
case 2:
 //BA.debugLineNum = 297;BA.debugLine="If part = 3 Then iIndex = iIndex + 1";
if (_part==3) { 
_iindex = (int) (_iindex+1);};
 //BA.debugLineNum = 298;BA.debugLine="Log(\"Responded\")";
anywheresoftware.b4a.keywords.Common.Log("Responded");
 break;
}
;
 };
 };
 break;
}
;
 //BA.debugLineNum = 305;BA.debugLine="ArgumentsList.Initialize()";
_argumentslist.Initialize();
 //BA.debugLineNum = 306;BA.debugLine="ArgumentsList.Add(labEventText)";
_argumentslist.Add((Object)(_labeventtext));
 //BA.debugLineNum = 307;BA.debugLine="ArgumentsList.Add(labFreqText)";
_argumentslist.Add((Object)(_labfreqtext));
 //BA.debugLineNum = 308;BA.debugLine="ArgumentsList.Add(labStrengthText)";
_argumentslist.Add((Object)(_labstrengthtext));
 //BA.debugLineNum = 309;BA.debugLine="ArgumentsList.Add(pbStrengthProgress)";
_argumentslist.Add((Object)(_pbstrengthprogress));
 //BA.debugLineNum = 310;BA.debugLine="ArgumentsList.Add(labVolumeText)";
_argumentslist.Add((Object)(_labvolumetext));
 //BA.debugLineNum = 311;BA.debugLine="ArgumentsList.Add(ProgramNameText)";
_argumentslist.Add((Object)(_programnametext));
 //BA.debugLineNum = 312;BA.debugLine="ArgumentsList.Add(labProgramTypeText)";
_argumentslist.Add((Object)(_labprogramtypetext));
 //BA.debugLineNum = 313;BA.debugLine="ArgumentsList.Add(labProgram2Text)";
_argumentslist.Add((Object)(_labprogram2text));
 //BA.debugLineNum = 314;BA.debugLine="ArgumentsList.Add(labProgramText)";
_argumentslist.Add((Object)(_labprogramtext));
 //BA.debugLineNum = 315;BA.debugLine="ArgumentsList.Add(StereoModeText)";
_argumentslist.Add((Object)(_stereomodetext));
 //BA.debugLineNum = 316;BA.debugLine="ArgumentsList.Add(DataRateText)";
_argumentslist.Add((Object)(_dataratetext));
 //BA.debugLineNum = 318;BA.debugLine="Dim Changed As Boolean = False";
_changed = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 320;BA.debugLine="If PreviousNotificationText <> ProgramNameText Th";
if ((_previousnotificationtext).equals(_programnametext) == false) { 
 //BA.debugLineNum = 321;BA.debugLine="RadioNotification.ContentText = ProgramNameText";
_radionotification.setContentText(_programnametext);
 //BA.debugLineNum = 322;BA.debugLine="PreviousNotificationText = ProgramNameText";
_previousnotificationtext = _programnametext;
 //BA.debugLineNum = 323;BA.debugLine="Changed = True";
_changed = anywheresoftware.b4a.keywords.Common.True;
 };
 //BA.debugLineNum = 326;BA.debugLine="If PreviousNotificationText2 <> labEventText Then";
if ((_previousnotificationtext2).equals(_labeventtext) == false) { 
 //BA.debugLineNum = 327;BA.debugLine="RadioNotification.ContentInfo = labEventText";
_radionotification.setContentInfo(_labeventtext);
 //BA.debugLineNum = 328;BA.debugLine="PreviousNotificationText2 = labEventText";
_previousnotificationtext2 = _labeventtext;
 //BA.debugLineNum = 329;BA.debugLine="Changed = True";
_changed = anywheresoftware.b4a.keywords.Common.True;
 };
 //BA.debugLineNum = 332;BA.debugLine="If Changed Then";
if (_changed) { 
 //BA.debugLineNum = 333;BA.debugLine="Service.StartForeground(1,RadioNotification.GetN";
mostCurrent._service.StartForeground((int) (1),_radionotification.GetNotification(processBA));
 };
 //BA.debugLineNum = 336;BA.debugLine="CallSub2(Main,\"SetEvaluatedData\",ArgumentsList)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(processBA,(Object)(mostCurrent._main.getObject()),"SetEvaluatedData",(Object)(_argumentslist));
 //BA.debugLineNum = 339;BA.debugLine="End Sub";
return "";
}
public static String  _exitapp() throws Exception{
 //BA.debugLineNum = 596;BA.debugLine="Sub ExitApp";
 //BA.debugLineNum = 597;BA.debugLine="Log(\"Attempting To close app when ServiceStarted";
anywheresoftware.b4a.keywords.Common.Log("Attempting To close app when ServiceStarted = "+BA.ObjectToString(_servicestarted));
 //BA.debugLineNum = 599;BA.debugLine="If ServiceStarted And Connected Then";
if (_servicestarted && _connected) { 
 //BA.debugLineNum = 601;BA.debugLine="SaveSettings";
_savesettings();
 //BA.debugLineNum = 602;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0c),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0xfd)});
 //BA.debugLineNum = 604;BA.debugLine="Wait(0.5)";
_wait((int) (0.5));
 //BA.debugLineNum = 606;BA.debugLine="astreams.Close";
_astreams.Close();
 //BA.debugLineNum = 608;BA.debugLine="CloseRadio";
_closeradio();
 //BA.debugLineNum = 610;BA.debugLine="AudioFocusManager.abandonAudioFocus";
_audiofocusmanager.abandonAudioFocus();
 //BA.debugLineNum = 612;BA.debugLine="Log(\"Abandoning media focus\")";
anywheresoftware.b4a.keywords.Common.Log("Abandoning media focus");
 //BA.debugLineNum = 614;BA.debugLine="session.RunMethod(\"release\",Null)";
_session.RunMethod("release",(Object[])(anywheresoftware.b4a.keywords.Common.Null));
 //BA.debugLineNum = 616;BA.debugLine="Broadcast.sendBroadcast(\"com.freshollie.radioapp";
_broadcast.sendBroadcast("com.freshollie.radioapp.STOPPED");
 //BA.debugLineNum = 617;BA.debugLine="Service.StopForeground(1)";
mostCurrent._service.StopForeground((int) (1));
 //BA.debugLineNum = 618;BA.debugLine="RadioNotification.Cancel(1)";
_radionotification.Cancel(processBA,(int) (1));
 //BA.debugLineNum = 621;BA.debugLine="ServiceStarted = False";
_servicestarted = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 622;BA.debugLine="Connected = False";
_connected = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 624;BA.debugLine="If IsPaused(Main) Then";
if (anywheresoftware.b4a.keywords.Common.IsPaused(processBA,(Object)(mostCurrent._main.getObject()))) { 
 //BA.debugLineNum = 625;BA.debugLine="ExitApplication";
anywheresoftware.b4a.keywords.Common.ExitApplication();
 };
 }else {
 //BA.debugLineNum = 629;BA.debugLine="Log(\"Attempt aborted\")";
anywheresoftware.b4a.keywords.Common.Log("Attempt aborted");
 };
 //BA.debugLineNum = 633;BA.debugLine="End Sub";
return "";
}
public static String  _fmhigher() throws Exception{
int _f = 0;
String _d1 = "";
String _d2 = "";
String _d3 = "";
String _d = "";
 //BA.debugLineNum = 809;BA.debugLine="Sub FMHigher";
 //BA.debugLineNum = 810;BA.debugLine="Dim F As Int";
_f = 0;
 //BA.debugLineNum = 811;BA.debugLine="F = Frequenz";
_f = _frequenz;
 //BA.debugLineNum = 812;BA.debugLine="If F > 87400 And F < 108100 Then";
if (_f>87400 && _f<108100) { 
 //BA.debugLineNum = 813;BA.debugLine="If F > 107900 Then F = 87400";
if (_f>107900) { 
_f = (int) (87400);};
 //BA.debugLineNum = 814;BA.debugLine="Dim d1,d2,d3,D As String";
_d1 = "";
_d2 = "";
_d3 = "";
_d = "";
 //BA.debugLineNum = 815;BA.debugLine="D =  Bit.ToHexString(F + 50)";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString((int) (_f+50));
 //BA.debugLineNum = 816;BA.debugLine="d1 = Bit.ParseInt(D.SubString2(0,1), 16)";
_d1 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (0),(int) (1)),(int) (16)));
 //BA.debugLineNum = 817;BA.debugLine="d2 = Bit.ParseInt(D.SubString2(1,3), 16)";
_d2 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (1),(int) (3)),(int) (16)));
 //BA.debugLineNum = 818;BA.debugLine="d3 = Bit.ParseInt(D.SubString2(3,5), 16)";
_d3 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (3),(int) (5)),(int) (16)));
 //BA.debugLineNum = 819;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte)(Double.parseDouble(_d1)),(byte)(Double.parseDouble(_d2)),(byte)(Double.parseDouble(_d3)),(byte) (0xfd)});
 };
 //BA.debugLineNum = 821;BA.debugLine="End Sub";
return "";
}
public static String  _fmlower() throws Exception{
int _f = 0;
String _d1 = "";
String _d2 = "";
String _d3 = "";
String _d = "";
 //BA.debugLineNum = 823;BA.debugLine="Sub FMLower";
 //BA.debugLineNum = 824;BA.debugLine="Dim F As Int";
_f = 0;
 //BA.debugLineNum = 825;BA.debugLine="F = Frequenz";
_f = _frequenz;
 //BA.debugLineNum = 826;BA.debugLine="If F > 87400 And F < 108100 Then";
if (_f>87400 && _f<108100) { 
 //BA.debugLineNum = 827;BA.debugLine="If F < 87600 Then F = 108100";
if (_f<87600) { 
_f = (int) (108100);};
 //BA.debugLineNum = 828;BA.debugLine="Dim d1,d2,d3,D As String";
_d1 = "";
_d2 = "";
_d3 = "";
_d = "";
 //BA.debugLineNum = 829;BA.debugLine="D =  Bit.ToHexString(F - 50)";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString((int) (_f-50));
 //BA.debugLineNum = 830;BA.debugLine="d1 = Bit.ParseInt(D.SubString2(0,1), 16)";
_d1 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (0),(int) (1)),(int) (16)));
 //BA.debugLineNum = 831;BA.debugLine="d2 = Bit.ParseInt(D.SubString2(1,3), 16)";
_d2 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (1),(int) (3)),(int) (16)));
 //BA.debugLineNum = 832;BA.debugLine="d3 = Bit.ParseInt(D.SubString2(3,5), 16)";
_d3 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (3),(int) (5)),(int) (16)));
 //BA.debugLineNum = 833;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte)(Double.parseDouble(_d1)),(byte)(Double.parseDouble(_d2)),(byte)(Double.parseDouble(_d3)),(byte) (0xfd)});
 };
 //BA.debugLineNum = 835;BA.debugLine="End Sub";
return "";
}
public static String  _incrementlevel() throws Exception{
 //BA.debugLineNum = 860;BA.debugLine="Sub IncrementLevel";
 //BA.debugLineNum = 861;BA.debugLine="Ebene = Ebene + 1";
_ebene = (int) (_ebene+1);
 //BA.debugLineNum = 862;BA.debugLine="If Ebene > 2 Then Ebene = 0";
if (_ebene>2) { 
_ebene = (int) (0);};
 //BA.debugLineNum = 863;BA.debugLine="End Sub";
return "";
}
public static String  _labelclean() throws Exception{
 //BA.debugLineNum = 341;BA.debugLine="Sub LabelClean";
 //BA.debugLineNum = 342;BA.debugLine="If Connected Then";
if (_connected) { 
 //BA.debugLineNum = 343;BA.debugLine="MyTimer.Enabled = False";
_mytimer.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 344;BA.debugLine="Esamble = \"\"";
_esamble = "";
 //BA.debugLineNum = 345;BA.debugLine="ProgramName = \"\"";
_programname = "";
 //BA.debugLineNum = 346;BA.debugLine="ProgramName2 = \"\"";
_programname2 = "";
 //BA.debugLineNum = 347;BA.debugLine="ProgramText = \"\"";
_programtext = "";
 //BA.debugLineNum = 348;BA.debugLine="ProgramType = \"\"";
_programtype = "";
 //BA.debugLineNum = 349;BA.debugLine="Strength = 0";
_strength = (byte) (0);
 //BA.debugLineNum = 350;BA.debugLine="iIndex = 2";
_iindex = (int) (2);
 //BA.debugLineNum = 351;BA.debugLine="MyTimer.Enabled = True";
_mytimer.setEnabled(anywheresoftware.b4a.keywords.Common.True);
 };
 //BA.debugLineNum = 353;BA.debugLine="End Sub";
return "";
}
public static String  _loadsettings() throws Exception{
String _lableveltext = "";
boolean _labprogram2visible = false;
boolean _isdabchecked = false;
anywheresoftware.b4a.objects.collections.List _arglist = null;
anywheresoftware.b4a.randomaccessfile.RandomAccessFile _datesfile = null;
 //BA.debugLineNum = 1037;BA.debugLine="Sub LoadSettings";
 //BA.debugLineNum = 1039;BA.debugLine="If Not(lstDAB.IsInitialized) Then lstDAB.Initiali";
if (anywheresoftware.b4a.keywords.Common.Not(_lstdab.IsInitialized())) { 
_lstdab.Initialize();};
 //BA.debugLineNum = 1040;BA.debugLine="INI.Name(MyPath & \"/config.ini\")";
mostCurrent._ini._v5(processBA,_mypath+"/config.ini");
 //BA.debugLineNum = 1041;BA.debugLine="Volume = INI.ReadInt(\"Last\", \"Volume\", DefaultVol";
_volume = (byte) (mostCurrent._ini._v0(processBA,"Last","Volume",_defaultvolume));
 //BA.debugLineNum = 1042;BA.debugLine="Frq(0) = INI.ReadInt(\"Last\", \"Frq0\", 88000)";
_frq[(int) (0)] = mostCurrent._ini._v0(processBA,"Last","Frq0",(int) (88000));
 //BA.debugLineNum = 1043;BA.debugLine="Frq(1) = INI.ReadInt(\"Last\", \"Frq1\", 88000)";
_frq[(int) (1)] = mostCurrent._ini._v0(processBA,"Last","Frq1",(int) (88000));
 //BA.debugLineNum = 1044;BA.debugLine="Frq(2) = INI.ReadInt(\"Last\", \"Frq2\", 88000)";
_frq[(int) (2)] = mostCurrent._ini._v0(processBA,"Last","Frq2",(int) (88000));
 //BA.debugLineNum = 1045;BA.debugLine="Frq(3) = INI.ReadInt(\"Last\", \"Frq3\", 88000)";
_frq[(int) (3)] = mostCurrent._ini._v0(processBA,"Last","Frq3",(int) (88000));
 //BA.debugLineNum = 1046;BA.debugLine="Frq(4) = INI.ReadInt(\"Last\", \"Frq4\", 88000)";
_frq[(int) (4)] = mostCurrent._ini._v0(processBA,"Last","Frq4",(int) (88000));
 //BA.debugLineNum = 1047;BA.debugLine="Frq(5) = INI.ReadInt(\"Last\", \"Frq5\", 88000)";
_frq[(int) (5)] = mostCurrent._ini._v0(processBA,"Last","Frq5",(int) (88000));
 //BA.debugLineNum = 1048;BA.debugLine="Frq(6) = INI.ReadInt(\"Last\", \"Frq6\", 88000)";
_frq[(int) (6)] = mostCurrent._ini._v0(processBA,"Last","Frq6",(int) (88000));
 //BA.debugLineNum = 1049;BA.debugLine="Frq(7) = INI.ReadInt(\"Last\", \"Frq7\", 88000)";
_frq[(int) (7)] = mostCurrent._ini._v0(processBA,"Last","Frq7",(int) (88000));
 //BA.debugLineNum = 1050;BA.debugLine="Frq(8) = INI.ReadInt(\"Last\", \"Frq8\", 88000)";
_frq[(int) (8)] = mostCurrent._ini._v0(processBA,"Last","Frq8",(int) (88000));
 //BA.debugLineNum = 1051;BA.debugLine="Frq(9) = INI.ReadInt(\"Last\", \"Frq9\", 88000)";
_frq[(int) (9)] = mostCurrent._ini._v0(processBA,"Last","Frq9",(int) (88000));
 //BA.debugLineNum = 1052;BA.debugLine="Frq(10) = INI.ReadInt(\"Last\", \"Frq10\", 88000)";
_frq[(int) (10)] = mostCurrent._ini._v0(processBA,"Last","Frq10",(int) (88000));
 //BA.debugLineNum = 1053;BA.debugLine="Frq(11) = INI.ReadInt(\"Last\", \"Frq11\", 88000)";
_frq[(int) (11)] = mostCurrent._ini._v0(processBA,"Last","Frq11",(int) (88000));
 //BA.debugLineNum = 1054;BA.debugLine="Frq(12) = INI.ReadInt(\"Last\", \"Frq12\", 88000)";
_frq[(int) (12)] = mostCurrent._ini._v0(processBA,"Last","Frq12",(int) (88000));
 //BA.debugLineNum = 1055;BA.debugLine="Frq(13) = INI.ReadInt(\"Last\", \"Frq13\", 88000)";
_frq[(int) (13)] = mostCurrent._ini._v0(processBA,"Last","Frq13",(int) (88000));
 //BA.debugLineNum = 1056;BA.debugLine="Frq(14) = INI.ReadInt(\"Last\", \"Frq14\", 88000)";
_frq[(int) (14)] = mostCurrent._ini._v0(processBA,"Last","Frq14",(int) (88000));
 //BA.debugLineNum = 1057;BA.debugLine="Frq(15) = INI.ReadInt(\"Last\", \"Frq15\", 88000)";
_frq[(int) (15)] = mostCurrent._ini._v0(processBA,"Last","Frq15",(int) (88000));
 //BA.debugLineNum = 1058;BA.debugLine="Frq(16) = INI.ReadInt(\"Last\", \"Frq16\", 88000)";
_frq[(int) (16)] = mostCurrent._ini._v0(processBA,"Last","Frq16",(int) (88000));
 //BA.debugLineNum = 1059;BA.debugLine="Frq(17) = INI.ReadInt(\"Last\", \"Frq17\", 88000)";
_frq[(int) (17)] = mostCurrent._ini._v0(processBA,"Last","Frq17",(int) (88000));
 //BA.debugLineNum = 1060;BA.debugLine="Frq(18) = INI.ReadInt(\"Last\", \"Frq18\", 88000)";
_frq[(int) (18)] = mostCurrent._ini._v0(processBA,"Last","Frq18",(int) (88000));
 //BA.debugLineNum = 1061;BA.debugLine="Frq(19) = INI.ReadInt(\"Last\", \"Frq19\", 88000)";
_frq[(int) (19)] = mostCurrent._ini._v0(processBA,"Last","Frq19",(int) (88000));
 //BA.debugLineNum = 1062;BA.debugLine="Frq(20) = INI.ReadInt(\"Last\", \"Frq20\", 88000)";
_frq[(int) (20)] = mostCurrent._ini._v0(processBA,"Last","Frq20",(int) (88000));
 //BA.debugLineNum = 1063;BA.debugLine="DFrq(0) = INI.ReadInt(\"Last\", \"DAB0\", 0)";
_dfrq[(int) (0)] = mostCurrent._ini._v0(processBA,"Last","DAB0",(int) (0));
 //BA.debugLineNum = 1064;BA.debugLine="DFrq(1) = INI.ReadInt(\"Last\", \"DAB1\", 0)";
_dfrq[(int) (1)] = mostCurrent._ini._v0(processBA,"Last","DAB1",(int) (0));
 //BA.debugLineNum = 1065;BA.debugLine="DFrq(2) = INI.ReadInt(\"Last\", \"DAB2\", 0)";
_dfrq[(int) (2)] = mostCurrent._ini._v0(processBA,"Last","DAB2",(int) (0));
 //BA.debugLineNum = 1066;BA.debugLine="DFrq(3) = INI.ReadInt(\"Last\", \"DAB3\", 0)";
_dfrq[(int) (3)] = mostCurrent._ini._v0(processBA,"Last","DAB3",(int) (0));
 //BA.debugLineNum = 1067;BA.debugLine="DFrq(4) = INI.ReadInt(\"Last\", \"DAB4\", 0)";
_dfrq[(int) (4)] = mostCurrent._ini._v0(processBA,"Last","DAB4",(int) (0));
 //BA.debugLineNum = 1068;BA.debugLine="DFrq(5) = INI.ReadInt(\"Last\", \"DAB5\", 0)";
_dfrq[(int) (5)] = mostCurrent._ini._v0(processBA,"Last","DAB5",(int) (0));
 //BA.debugLineNum = 1069;BA.debugLine="DFrq(6) = INI.ReadInt(\"Last\", \"DAB6\", 0)";
_dfrq[(int) (6)] = mostCurrent._ini._v0(processBA,"Last","DAB6",(int) (0));
 //BA.debugLineNum = 1070;BA.debugLine="DFrq(7) = INI.ReadInt(\"Last\", \"DAB7\", 0)";
_dfrq[(int) (7)] = mostCurrent._ini._v0(processBA,"Last","DAB7",(int) (0));
 //BA.debugLineNum = 1071;BA.debugLine="DFrq(8) = INI.ReadInt(\"Last\", \"DAB8\", 0)";
_dfrq[(int) (8)] = mostCurrent._ini._v0(processBA,"Last","DAB8",(int) (0));
 //BA.debugLineNum = 1072;BA.debugLine="DFrq(9) = INI.ReadInt(\"Last\", \"DAB9\", 0)";
_dfrq[(int) (9)] = mostCurrent._ini._v0(processBA,"Last","DAB9",(int) (0));
 //BA.debugLineNum = 1073;BA.debugLine="DFrq(10) = INI.ReadInt(\"Last\", \"DAB10\", 0)";
_dfrq[(int) (10)] = mostCurrent._ini._v0(processBA,"Last","DAB10",(int) (0));
 //BA.debugLineNum = 1074;BA.debugLine="DFrq(11) = INI.ReadInt(\"Last\", \"DAB11\", 0)";
_dfrq[(int) (11)] = mostCurrent._ini._v0(processBA,"Last","DAB11",(int) (0));
 //BA.debugLineNum = 1075;BA.debugLine="DFrq(12) = INI.ReadInt(\"Last\", \"DAB12\", 0)";
_dfrq[(int) (12)] = mostCurrent._ini._v0(processBA,"Last","DAB12",(int) (0));
 //BA.debugLineNum = 1076;BA.debugLine="DFrq(13) = INI.ReadInt(\"Last\", \"DAB13\", 0)";
_dfrq[(int) (13)] = mostCurrent._ini._v0(processBA,"Last","DAB13",(int) (0));
 //BA.debugLineNum = 1077;BA.debugLine="DFrq(14) = INI.ReadInt(\"Last\", \"DAB14\", 0)";
_dfrq[(int) (14)] = mostCurrent._ini._v0(processBA,"Last","DAB14",(int) (0));
 //BA.debugLineNum = 1078;BA.debugLine="DFrq(15) = INI.ReadInt(\"Last\", \"DAB15\", 0)";
_dfrq[(int) (15)] = mostCurrent._ini._v0(processBA,"Last","DAB15",(int) (0));
 //BA.debugLineNum = 1079;BA.debugLine="DFrq(16) = INI.ReadInt(\"Last\", \"DAB16\", 0)";
_dfrq[(int) (16)] = mostCurrent._ini._v0(processBA,"Last","DAB16",(int) (0));
 //BA.debugLineNum = 1080;BA.debugLine="DFrq(17) = INI.ReadInt(\"Last\", \"DAB17\", 0)";
_dfrq[(int) (17)] = mostCurrent._ini._v0(processBA,"Last","DAB17",(int) (0));
 //BA.debugLineNum = 1081;BA.debugLine="DFrq(18) = INI.ReadInt(\"Last\", \"DAB18\", 0)";
_dfrq[(int) (18)] = mostCurrent._ini._v0(processBA,"Last","DAB18",(int) (0));
 //BA.debugLineNum = 1082;BA.debugLine="DFrq(19) = INI.ReadInt(\"Last\", \"DAB19\", 0)";
_dfrq[(int) (19)] = mostCurrent._ini._v0(processBA,"Last","DAB19",(int) (0));
 //BA.debugLineNum = 1083;BA.debugLine="DFrq(20) = INI.ReadInt(\"Last\", \"DAB20\", 0)";
_dfrq[(int) (20)] = mostCurrent._ini._v0(processBA,"Last","DAB20",(int) (0));
 //BA.debugLineNum = 1084;BA.debugLine="Frequenz = INI.ReadInt(\"Last\", \"Frequenz\", 88000)";
_frequenz = mostCurrent._ini._v0(processBA,"Last","Frequenz",(int) (88000));
 //BA.debugLineNum = 1085;BA.debugLine="Ebene = INI.ReadInt(\"Last\", \"Ebene\", 0)";
_ebene = mostCurrent._ini._v0(processBA,"Last","Ebene",(int) (0));
 //BA.debugLineNum = 1087;BA.debugLine="Dim labLevelText As String";
_lableveltext = "";
 //BA.debugLineNum = 1088;BA.debugLine="Dim labProgram2Visible, isDABChecked As Boolean";
_labprogram2visible = false;
_isdabchecked = false;
 //BA.debugLineNum = 1090;BA.debugLine="Dim ArgList As List";
_arglist = new anywheresoftware.b4a.objects.collections.List();
 //BA.debugLineNum = 1092;BA.debugLine="ArgList.Initialize()";
_arglist.Initialize();
 //BA.debugLineNum = 1094;BA.debugLine="labLevelText = \"L \" & Ebene";
_lableveltext = "L "+BA.NumberToString(_ebene);
 //BA.debugLineNum = 1095;BA.debugLine="If File.Exists(MyPath & \"/\",\"DAB.dat\") Then";
if (anywheresoftware.b4a.keywords.Common.File.Exists(_mypath+"/","DAB.dat")) { 
 //BA.debugLineNum = 1096;BA.debugLine="Dim DatesFile As RandomAccessFile";
_datesfile = new anywheresoftware.b4a.randomaccessfile.RandomAccessFile();
 //BA.debugLineNum = 1097;BA.debugLine="DatesFile.Initialize(MyPath,\"/DAB.dat\",False)";
_datesfile.Initialize(_mypath,"/DAB.dat",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 1098;BA.debugLine="lstDAB = DatesFile.ReadObject(0)";
_lstdab.setObject((java.util.List)(_datesfile.ReadObject((long) (0))));
 //BA.debugLineNum = 1099;BA.debugLine="DatesFile.Close";
_datesfile.Close();
 };
 //BA.debugLineNum = 1102;BA.debugLine="If (Frequenz < 87500) And DAB Then";
if ((_frequenz<87500) && _dab) { 
 //BA.debugLineNum = 1103;BA.debugLine="isDAB = True";
_isdab = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 1104;BA.debugLine="labProgram2Visible = True";
_labprogram2visible = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 1105;BA.debugLine="isDABChecked = True";
_isdabchecked = anywheresoftware.b4a.keywords.Common.True;
 }else {
 //BA.debugLineNum = 1107;BA.debugLine="If Frequenz < 87500 Then Frequenz = 87500";
if (_frequenz<87500) { 
_frequenz = (int) (87500);};
 //BA.debugLineNum = 1108;BA.debugLine="isDAB = False";
_isdab = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 1109;BA.debugLine="labProgram2Visible = False";
_labprogram2visible = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 1110;BA.debugLine="isDABChecked = False";
_isdabchecked = anywheresoftware.b4a.keywords.Common.False;
 };
 //BA.debugLineNum = 1113;BA.debugLine="ArgList.Add(labLevelText)";
_arglist.Add((Object)(_lableveltext));
 //BA.debugLineNum = 1114;BA.debugLine="ArgList.Add(isDABChecked)";
_arglist.Add((Object)(_isdabchecked));
 //BA.debugLineNum = 1115;BA.debugLine="ArgList.Add(labProgram2Visible)";
_arglist.Add((Object)(_labprogram2visible));
 //BA.debugLineNum = 1117;BA.debugLine="CallSub2(Main, \"SetTextValuesFromSettings\", ArgLi";
anywheresoftware.b4a.keywords.Common.CallSubNew2(processBA,(Object)(mostCurrent._main.getObject()),"SetTextValuesFromSettings",(Object)(_arglist));
 //BA.debugLineNum = 1120;BA.debugLine="End Sub";
return "";
}
public static boolean  _media_onbutton(int _keycode) throws Exception{
 //BA.debugLineNum = 1020;BA.debugLine="Sub Media_OnButton(KeyCode As Int) As Boolean";
 //BA.debugLineNum = 1021;BA.debugLine="Select(KeyCode)";
switch (BA.switchObjectToInt((_keycode),(anywheresoftware.b4a.keywords.Common.KeyCodes.KEYCODE_MEDIA_NEXT),(anywheresoftware.b4a.keywords.Common.KeyCodes.KEYCODE_MEDIA_PREVIOUS))) {
case 0:
 //BA.debugLineNum = 1024;BA.debugLine="RadioChannelUp";
_radiochannelup();
 break;
case 1:
 //BA.debugLineNum = 1027;BA.debugLine="RadioChannelDown";
_radiochanneldown();
 break;
}
;
 //BA.debugLineNum = 1031;BA.debugLine="Return True";
if (true) return anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 1032;BA.debugLine="End Sub";
return false;
}
public static String  _media_oncommand(String _command) throws Exception{
 //BA.debugLineNum = 1016;BA.debugLine="Sub Media_OnCommand(Command As String)";
 //BA.debugLineNum = 1018;BA.debugLine="End Sub";
return "";
}
public static String  _muteaudio() throws Exception{
 //BA.debugLineNum = 647;BA.debugLine="Sub MuteAudio";
 //BA.debugLineNum = 648;BA.debugLine="Log(\"Muting\")";
anywheresoftware.b4a.keywords.Common.Log("Muting");
 //BA.debugLineNum = 649;BA.debugLine="LastVolume = Volume";
_lastvolume = (int) (_volume);
 //BA.debugLineNum = 650;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0c),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0xfd)});
 //BA.debugLineNum = 651;BA.debugLine="End Sub";
return "";
}
public static String  _mytimer_tick() throws Exception{
String _d1 = "";
String _d2 = "";
String _d3 = "";
String _d = "";
 //BA.debugLineNum = 455;BA.debugLine="Sub MyTimer_Tick";
 //BA.debugLineNum = 459;BA.debugLine="If Connected Then";
if (_connected) { 
 //BA.debugLineNum = 460;BA.debugLine="If DABSearch Then 'If a dab search has been perf";
if (_dabsearch) { 
 //BA.debugLineNum = 461;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x14,0x01,0x0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x14),(byte) (0x01),(byte) (0x00),(byte) (0x04),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 }else {
 //BA.debugLineNum = 463;BA.debugLine="If FillList Then";
if (_filllist) { 
 //BA.debugLineNum = 464;BA.debugLine="If iStep < AllDAB Then";
if (_istep<_alldab) { 
 //BA.debugLineNum = 465;BA.debugLine="Log(iStep)";
anywheresoftware.b4a.keywords.Common.Log(BA.NumberToString(_istep));
 //BA.debugLineNum = 466;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x1A,0x01,0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x1a),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_istep),(byte) (0x01),(byte) (0xfd)});
 }else {
 //BA.debugLineNum = 469;BA.debugLine="iStep = 0";
_istep = (int) (0);
 //BA.debugLineNum = 470;BA.debugLine="FillList = False";
_filllist = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 471;BA.debugLine="MyTimer.Interval = 25 'Go back to normal send";
_mytimer.setInterval((long) (25));
 //BA.debugLineNum = 472;BA.debugLine="CallSub(Main, \"updateStationList\")";
anywheresoftware.b4a.keywords.Common.CallSubNew(processBA,(Object)(mostCurrent._main.getObject()),"updateStationList");
 };
 }else {
 //BA.debugLineNum = 475;BA.debugLine="If isDAB And DAB Then 'If in DAB mode";
if (_isdab && _dab) { 
 //BA.debugLineNum = 478;BA.debugLine="Select iIndex";
switch (_iindex) {
case 0:
 //BA.debugLineNum = 480;BA.debugLine="Log(\"Set to Fequency \" & Frequenz)";
anywheresoftware.b4a.keywords.Common.Log("Set to Fequency "+BA.NumberToString(_frequenz));
 //BA.debugLineNum = 481;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_frequenz),(byte) (0xfd)});
 //BA.debugLineNum = 482;BA.debugLine="part = 1";
_part = (int) (1);
 break;
case 1:
 //BA.debugLineNum = 484;BA.debugLine="Log(\"Setting volume\")";
anywheresoftware.b4a.keywords.Common.Log("Setting volume");
 //BA.debugLineNum = 485;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0c),(byte) (0x01),(byte) (0x00),(byte) (0x01),_volume,(byte) (0xfd)});
 //BA.debugLineNum = 486;BA.debugLine="part = 2";
_part = (int) (2);
 break;
case 2:
 //BA.debugLineNum = 488;BA.debugLine="Log(\"Setting stereoMode\")";
anywheresoftware.b4a.keywords.Common.Log("Setting stereoMode");
 //BA.debugLineNum = 489;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x09,0x01";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x09),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x01),(byte) (0xfd)});
 //BA.debugLineNum = 490;BA.debugLine="part = 3";
_part = (int) (3);
 break;
case 3:
 //BA.debugLineNum = 492;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0D,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0d),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 4:
 //BA.debugLineNum = 494;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x15,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x15),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_frequenz),(byte) (0x01),(byte) (0xfd)});
 break;
case 5:
 //BA.debugLineNum = 496;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x1A,0x01";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x1a),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_frequenz),(byte) (0x01),(byte) (0xfd)});
 break;
case 6:
 //BA.debugLineNum = 498;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x07,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x07),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 7:
 //BA.debugLineNum = 500;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x13,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x13),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 8:
 //BA.debugLineNum = 502;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0E,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0e),(byte) (0x01),(byte) (0x00),(byte) (0x04),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_frequenz),(byte) (0xfd)});
 break;
case 9:
 //BA.debugLineNum = 504;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x05,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 10:
 //BA.debugLineNum = 506;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x10,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x10),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 11:
 //BA.debugLineNum = 508;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0B,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0b),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 12:
 //BA.debugLineNum = 510;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x12,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x12),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 //BA.debugLineNum = 511;BA.debugLine="iIndex = 3";
_iindex = (int) (3);
 //BA.debugLineNum = 512;BA.debugLine="Return";
if (true) return "";
 break;
}
;
 }else {
 //BA.debugLineNum = 515;BA.debugLine="Select iIndex";
switch (_iindex) {
case 0:
 //BA.debugLineNum = 517;BA.debugLine="Dim d1,d2,d3,d As String";
_d1 = "";
_d2 = "";
_d3 = "";
_d = "";
 //BA.debugLineNum = 518;BA.debugLine="d =  Bit.ToHexString(Frequenz)";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_frequenz);
 //BA.debugLineNum = 519;BA.debugLine="d1 = Bit.ParseInt(d.SubString2(0,1), 16)";
_d1 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (0),(int) (1)),(int) (16)));
 //BA.debugLineNum = 520;BA.debugLine="d2 = Bit.ParseInt(d.SubString2(1,3), 16)";
_d2 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (1),(int) (3)),(int) (16)));
 //BA.debugLineNum = 521;BA.debugLine="d3 = Bit.ParseInt(d.SubString2(3,5), 16)";
_d3 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (3),(int) (5)),(int) (16)));
 //BA.debugLineNum = 522;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte)(Double.parseDouble(_d1)),(byte)(Double.parseDouble(_d2)),(byte)(Double.parseDouble(_d3)),(byte) (0xfd)});
 break;
case 1:
 //BA.debugLineNum = 524;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0c),(byte) (0x01),(byte) (0x00),(byte) (0x01),_volume,(byte) (0xfd)});
 break;
case 2:
 //BA.debugLineNum = 526;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x09,0x01";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x09),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x01),(byte) (0xfd)});
 break;
case 3:
 //BA.debugLineNum = 528;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0D,0x01";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0d),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 4:
 //BA.debugLineNum = 530;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x07,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x07),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 5:
 //BA.debugLineNum = 532;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x08,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x08),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 6:
 //BA.debugLineNum = 534;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0E,0x01";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0e),(byte) (0x01),(byte) (0x00),(byte) (0x04),(byte) (0xff),(byte) (0xff),(byte) (0xff),(byte) (0xff),(byte) (0xfd)});
 break;
case 7:
 //BA.debugLineNum = 536;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0F,0x01";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0f),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0xff),(byte) (0xff),(byte) (0xff),(byte) (0xff),(byte) (0x01),(byte) (0xfd)});
 break;
case 8:
 //BA.debugLineNum = 538;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x05,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 break;
case 9:
 //BA.debugLineNum = 540;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x10,0x01,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x10),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 //BA.debugLineNum = 541;BA.debugLine="iIndex = 3";
_iindex = (int) (3);
 //BA.debugLineNum = 542;BA.debugLine="Return";
if (true) return "";
 break;
}
;
 };
 //BA.debugLineNum = 545;BA.debugLine="If iIndex > 2 Then iIndex = iIndex + 1";
if (_iindex>2) { 
_iindex = (int) (_iindex+1);};
 //BA.debugLineNum = 547;BA.debugLine="If iIndex > 12 Then iIndex = 3";
if (_iindex>12) { 
_iindex = (int) (3);};
 };
 };
 };
 //BA.debugLineNum = 551;BA.debugLine="End Sub";
return "";
}
public static String  _openradio() throws Exception{
anywheresoftware.b4a.objects.usb.UsbManagerWrapper _usbmngr = null;
anywheresoftware.b4a.objects.usb.UsbManagerWrapper.UsbDeviceWrapper[] _usbdevices = null;
int _i = 0;
anywheresoftware.b4a.objects.usb.UsbManagerWrapper.UsbDeviceWrapper _usbdvc = null;
 //BA.debugLineNum = 370;BA.debugLine="Sub OpenRadio";
 //BA.debugLineNum = 372;BA.debugLine="Dim UsbMngr As UsbManager  ' USB library";
_usbmngr = new anywheresoftware.b4a.objects.usb.UsbManagerWrapper();
 //BA.debugLineNum = 374;BA.debugLine="UsbMngr.Initialize";
_usbmngr.Initialize();
 //BA.debugLineNum = 375;BA.debugLine="Dim UsbDevices() As UsbDevice  ' USB library";
_usbdevices = new anywheresoftware.b4a.objects.usb.UsbManagerWrapper.UsbDeviceWrapper[(int) (0)];
{
int d0 = _usbdevices.length;
for (int i0 = 0;i0 < d0;i0++) {
_usbdevices[i0] = new anywheresoftware.b4a.objects.usb.UsbManagerWrapper.UsbDeviceWrapper();
}
}
;
 //BA.debugLineNum = 377;BA.debugLine="UsbDevices = UsbMngr.GetDevices";
_usbdevices = _usbmngr.GetDevices();
 //BA.debugLineNum = 381;BA.debugLine="If UsbDevices.Length > 0 Then";
if (_usbdevices.length>0) { 
 //BA.debugLineNum = 382;BA.debugLine="Log(UsbDevices.Length)";
anywheresoftware.b4a.keywords.Common.Log(BA.NumberToString(_usbdevices.length));
 //BA.debugLineNum = 384;BA.debugLine="For i = 0 To UsbDevices.Length - 1";
{
final int step307 = 1;
final int limit307 = (int) (_usbdevices.length-1);
for (_i = (int) (0); (step307 > 0 && _i <= limit307) || (step307 < 0 && _i >= limit307); _i = ((int)(0 + _i + step307))) {
 //BA.debugLineNum = 385;BA.debugLine="Dim UsbDvc As UsbDevice";
_usbdvc = new anywheresoftware.b4a.objects.usb.UsbManagerWrapper.UsbDeviceWrapper();
 //BA.debugLineNum = 386;BA.debugLine="UsbDvc = UsbDevices(i)";
_usbdvc = _usbdevices[_i];
 //BA.debugLineNum = 388;BA.debugLine="If (UsbDvc.ProductId = UsbPid) And (UsbDvc.Ven";
if ((_usbdvc.getProductId()==_usbpid) && (_usbdvc.getVendorId()==_usbvid)) { 
 //BA.debugLineNum = 389;BA.debugLine="USB.SetCustomDevice(USB.DRIVER_CDCACM, UsbVid,";
_usb.SetCustomDevice(_usb.DRIVER_CDCACM,_usbvid,_usbpid);
 //BA.debugLineNum = 391;BA.debugLine="If Not(UsbMngr.HasPermission(UsbDvc)) Then";
if (anywheresoftware.b4a.keywords.Common.Not(_usbmngr.HasPermission((android.hardware.usb.UsbDevice)(_usbdvc.getObject())))) { 
 //BA.debugLineNum = 392;BA.debugLine="UsbMngr.RequestPermission(UsbDvc)";
_usbmngr.RequestPermission((android.hardware.usb.UsbDevice)(_usbdvc.getObject()));
 };
 //BA.debugLineNum = 395;BA.debugLine="If UsbMngr.HasPermission(UsbDvc) Then";
if (_usbmngr.HasPermission((android.hardware.usb.UsbDevice)(_usbdvc.getObject()))) { 
 //BA.debugLineNum = 396;BA.debugLine="Dev = USB.Open(57600, i + 1)";
_dev = _usb.Open(processBA,(int) (57600),(int) (_i+1));
 //BA.debugLineNum = 398;BA.debugLine="If Dev <> USB.USB_NONE Then";
if (_dev!=_usb.USB_NONE) { 
 //BA.debugLineNum = 399;BA.debugLine="USBTimer.Enabled = False";
_usbtimer.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 400;BA.debugLine="Log(\"Connected successfully!\")";
anywheresoftware.b4a.keywords.Common.Log("Connected successfully!");
 //BA.debugLineNum = 402;BA.debugLine="astreams.Initialize(USB.GetInputStream, USB.";
_astreams.Initialize(processBA,_usb.GetInputStream(),_usb.GetOutputStream(),"astreams");
 //BA.debugLineNum = 404;BA.debugLine="RTS(True)";
_rts(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 405;BA.debugLine="DTR(False)";
_dtr(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 407;BA.debugLine="If Not(SysReady.IsInitialized) Then SysReady";
if (anywheresoftware.b4a.keywords.Common.Not(_sysready.IsInitialized())) { 
_sysready.Initialize(processBA,"SysReady",(long) (500));};
 //BA.debugLineNum = 408;BA.debugLine="If Not(MyTimer.IsInitialized) Then MyTimer.I";
if (anywheresoftware.b4a.keywords.Common.Not(_mytimer.IsInitialized())) { 
_mytimer.Initialize(processBA,"MyTimer",(long) (25));};
 //BA.debugLineNum = 410;BA.debugLine="StartMediaKeys";
_startmediakeys();
 //BA.debugLineNum = 411;BA.debugLine="Mediakey.MediaButton(KeyCodes.KEYCODE_MEDIA_";
_mediakey.MediaButton(processBA,anywheresoftware.b4a.keywords.Common.KeyCodes.KEYCODE_MEDIA_STOP);
 //BA.debugLineNum = 412;BA.debugLine="AudioFocusManager.Initialize(\"AudioFocusMana";
_audiofocusmanager.Initialize(processBA,"AudioFocusManager");
 //BA.debugLineNum = 413;BA.debugLine="AudioFocusManager.requestFocus";
_audiofocusmanager.requestFocus();
 //BA.debugLineNum = 414;BA.debugLine="Ducked = False";
_ducked = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 416;BA.debugLine="Connected = True";
_connected = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 417;BA.debugLine="SetUpNotfication";
_setupnotfication();
 //BA.debugLineNum = 418;BA.debugLine="Broadcast.sendBroadcast(\"com.freshollie.radi";
_broadcast.sendBroadcast("com.freshollie.radioapp.RUNNING");
 //BA.debugLineNum = 420;BA.debugLine="DAB = True";
_dab = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 421;BA.debugLine="LoadSettings";
_loadsettings();
 //BA.debugLineNum = 422;BA.debugLine="iIndex = 0";
_iindex = (int) (0);
 //BA.debugLineNum = 423;BA.debugLine="part = 0";
_part = (int) (0);
 //BA.debugLineNum = 424;BA.debugLine="Start = True";
_start = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 425;BA.debugLine="Wait(1)";
_wait((int) (1));
 //BA.debugLineNum = 427;BA.debugLine="MyTimer.Enabled = True";
_mytimer.setEnabled(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 428;BA.debugLine="Return";
if (true) return "";
 };
 };
 //BA.debugLineNum = 432;BA.debugLine="Exit";
if (true) break;
 };
 }
};
 };
 //BA.debugLineNum = 436;BA.debugLine="ExitApp";
_exitapp();
 //BA.debugLineNum = 437;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 7;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 8;BA.debugLine="Dim USB As UsbSerial";
_usb = new anywheresoftware.b4a.objects.UsbSerial();
 //BA.debugLineNum = 9;BA.debugLine="Dim UsbPid As Int = 0xa";
_usbpid = (int) (0xa);
 //BA.debugLineNum = 10;BA.debugLine="Dim UsbVid As Int = 0x4D8";
_usbvid = (int) (0x4d8);
 //BA.debugLineNum = 11;BA.debugLine="Dim astreams As AsyncStreams";
_astreams = new anywheresoftware.b4a.randomaccessfile.AsyncStreams();
 //BA.debugLineNum = 12;BA.debugLine="Dim MyTimer, SysReady, USBTimer As Timer";
_mytimer = new anywheresoftware.b4a.objects.Timer();
_sysready = new anywheresoftware.b4a.objects.Timer();
_usbtimer = new anywheresoftware.b4a.objects.Timer();
 //BA.debugLineNum = 13;BA.debugLine="Dim Broadcast As BroadCastReceiver";
_broadcast = new com.rootsoft.broadcastreceiver.BroadCastReceiver();
 //BA.debugLineNum = 14;BA.debugLine="Dim AudioFocusManager As AudioFocus";
_audiofocusmanager = new com.freshollie.audiofocus.AudioFocus();
 //BA.debugLineNum = 15;BA.debugLine="Dim Start, Mute, DABSearch, isDAB, DAB, Connected";
_start = false;
_mute = false;
_dabsearch = false;
_isdab = false;
_dab = false;
_connected = false;
_filllist = false;
_cleardatabase = false;
_ducked = false;
 //BA.debugLineNum = 16;BA.debugLine="Dim Volume, Strength As Byte";
_volume = (byte)0;
_strength = (byte)0;
 //BA.debugLineNum = 17;BA.debugLine="Dim Ack(1024), Frq(21), DFrq(21), Frequenz, iInde";
_ack = new int[(int) (1024)];
;
_frq = new int[(int) (21)];
;
_dfrq = new int[(int) (21)];
;
_frequenz = 0;
_iindex = 0;
_xindex = 0;
_dev = 0;
_ebene = 0;
_alldab = 0;
_istep = 0;
_iloop = 0;
 //BA.debugLineNum = 18;BA.debugLine="Dim lstDAB As List";
_lstdab = new anywheresoftware.b4a.objects.collections.List();
 //BA.debugLineNum = 19;BA.debugLine="Dim RadioNotification As NotificationBuilder";
_radionotification = new barxdroid.NotificationBuilder.NotificationBuilder();
 //BA.debugLineNum = 20;BA.debugLine="Dim ProgramText, ProgramType, ProgramName, Progra";
_programtext = "";
_programtype = "";
_programname = "";
_programname2 = "";
_esamble = "";
_status = "";
 //BA.debugLineNum = 21;BA.debugLine="Dim labEventText,labFreqText,labStrengthText,labV";
_labeventtext = "";
_labfreqtext = "";
_labstrengthtext = "";
_labvolumetext = "";
_labprogramtext = "";
_labprogramtypetext = "";
_labprogram2text = "";
_programnametext = "";
_stereomodetext = "";
_dataratetext = "";
 //BA.debugLineNum = 22;BA.debugLine="Dim pbStrengthProgress As Double";
_pbstrengthprogress = 0;
 //BA.debugLineNum = 23;BA.debugLine="Dim MyPath As String";
_mypath = "";
 //BA.debugLineNum = 24;BA.debugLine="Dim PreviousNotificationText As String";
_previousnotificationtext = "";
 //BA.debugLineNum = 25;BA.debugLine="Dim PreviousNotificationText2 As String";
_previousnotificationtext2 = "";
 //BA.debugLineNum = 26;BA.debugLine="Dim ServiceStarted As Boolean";
_servicestarted = false;
 //BA.debugLineNum = 27;BA.debugLine="Dim Mediakey As MediaController";
_mediakey = new com.omnicorp.media.MediaController();
 //BA.debugLineNum = 28;BA.debugLine="Dim DuckVolume, DefaultVolume, LastVolume, part A";
_duckvolume = 0;
_defaultvolume = 0;
_lastvolume = 0;
_part = 0;
 //BA.debugLineNum = 29;BA.debugLine="Private session As JavaObject";
_session = new anywheresoftware.b4j.object.JavaObject();
 //BA.debugLineNum = 31;BA.debugLine="Dim EnterClickedReturnValue As Boolean";
_enterclickedreturnvalue = false;
 //BA.debugLineNum = 33;BA.debugLine="End Sub";
return "";
}
public static String  _radiochanneldown() throws Exception{
 //BA.debugLineNum = 798;BA.debugLine="Sub RadioChannelDown";
 //BA.debugLineNum = 799;BA.debugLine="If isDAB And DAB Then";
if (_isdab && _dab) { 
 //BA.debugLineNum = 800;BA.debugLine="If Frequenz > 0 Then";
if (_frequenz>0) { 
 //BA.debugLineNum = 801;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_frequenz-1),(byte) (0xfd)});
 //BA.debugLineNum = 802;BA.debugLine="CallSub2(Main, \"SelectedDABChannel\", Frequenz -";
anywheresoftware.b4a.keywords.Common.CallSubNew2(processBA,(Object)(mostCurrent._main.getObject()),"SelectedDABChannel",(Object)(_frequenz-1));
 };
 }else {
 //BA.debugLineNum = 805;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x02,0x01,0x00";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x02),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0xfd)});
 };
 //BA.debugLineNum = 807;BA.debugLine="End Sub";
return "";
}
public static String  _radiochannelup() throws Exception{
 //BA.debugLineNum = 789;BA.debugLine="Sub RadioChannelUp";
 //BA.debugLineNum = 790;BA.debugLine="If isDAB And DAB Then";
if (_isdab && _dab) { 
 //BA.debugLineNum = 791;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_frequenz+1),(byte) (0xfd)});
 //BA.debugLineNum = 792;BA.debugLine="CallSub2(Main, \"SelectedDABChannel\", Frequenz +";
anywheresoftware.b4a.keywords.Common.CallSubNew2(processBA,(Object)(mostCurrent._main.getObject()),"SelectedDABChannel",(Object)(_frequenz+1));
 }else {
 //BA.debugLineNum = 794;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x02,0x01,0x00";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x02),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x01),(byte) (0xfd)});
 };
 //BA.debugLineNum = 796;BA.debugLine="End Sub";
return "";
}
public static String  _rts(boolean _bool) throws Exception{
anywheresoftware.b4a.agraham.reflection.Reflection _r = null;
 //BA.debugLineNum = 355;BA.debugLine="Sub RTS(Bool As Boolean)";
 //BA.debugLineNum = 356;BA.debugLine="Dim r As Reflector";
_r = new anywheresoftware.b4a.agraham.reflection.Reflection();
 //BA.debugLineNum = 357;BA.debugLine="r.Target = USB";
_r.Target = (Object)(_usb);
 //BA.debugLineNum = 358;BA.debugLine="r.Target = r.getField(\"driver\")";
_r.Target = _r.GetField("driver");
 //BA.debugLineNum = 359;BA.debugLine="r.RunMethod2(\"setRTS\", Bool, \"java.lang.boolean\")";
_r.RunMethod2("setRTS",BA.ObjectToString(_bool),"java.lang.boolean");
 //BA.debugLineNum = 360;BA.debugLine="End Sub";
return "";
}
public static String  _savesettings() throws Exception{
anywheresoftware.b4a.randomaccessfile.RandomAccessFile _datesfile = null;
 //BA.debugLineNum = 1122;BA.debugLine="Sub SaveSettings";
 //BA.debugLineNum = 1123;BA.debugLine="INI.Name(MyPath & \"/config.ini\")";
mostCurrent._ini._v5(processBA,_mypath+"/config.ini");
 //BA.debugLineNum = 1124;BA.debugLine="INI.WriteInt(\"Last\", \"Frequenz\", Frequenz)";
mostCurrent._ini._vv4(processBA,"Last","Frequenz",_frequenz);
 //BA.debugLineNum = 1125;BA.debugLine="INI.WriteInt(\"Last\", \"Volume\", Volume)";
mostCurrent._ini._vv4(processBA,"Last","Volume",(int) (_volume));
 //BA.debugLineNum = 1126;BA.debugLine="INI.WriteInt(\"Last\", \"Frq0\", Frq(0))";
mostCurrent._ini._vv4(processBA,"Last","Frq0",_frq[(int) (0)]);
 //BA.debugLineNum = 1127;BA.debugLine="INI.WriteInt(\"Last\", \"Frq1\", Frq(1))";
mostCurrent._ini._vv4(processBA,"Last","Frq1",_frq[(int) (1)]);
 //BA.debugLineNum = 1128;BA.debugLine="INI.WriteInt(\"Last\", \"Frq2\", Frq(2))";
mostCurrent._ini._vv4(processBA,"Last","Frq2",_frq[(int) (2)]);
 //BA.debugLineNum = 1129;BA.debugLine="INI.WriteInt(\"Last\", \"Frq3\", Frq(3))";
mostCurrent._ini._vv4(processBA,"Last","Frq3",_frq[(int) (3)]);
 //BA.debugLineNum = 1130;BA.debugLine="INI.WriteInt(\"Last\", \"Frq4\", Frq(4))";
mostCurrent._ini._vv4(processBA,"Last","Frq4",_frq[(int) (4)]);
 //BA.debugLineNum = 1131;BA.debugLine="INI.WriteInt(\"Last\", \"Frq5\", Frq(5))";
mostCurrent._ini._vv4(processBA,"Last","Frq5",_frq[(int) (5)]);
 //BA.debugLineNum = 1132;BA.debugLine="INI.WriteInt(\"Last\", \"Frq6\", Frq(6))";
mostCurrent._ini._vv4(processBA,"Last","Frq6",_frq[(int) (6)]);
 //BA.debugLineNum = 1133;BA.debugLine="INI.WriteInt(\"Last\", \"Frq7\", Frq(7))";
mostCurrent._ini._vv4(processBA,"Last","Frq7",_frq[(int) (7)]);
 //BA.debugLineNum = 1134;BA.debugLine="INI.WriteInt(\"Last\", \"Frq8\", Frq(8))";
mostCurrent._ini._vv4(processBA,"Last","Frq8",_frq[(int) (8)]);
 //BA.debugLineNum = 1135;BA.debugLine="INI.WriteInt(\"Last\", \"Frq9\", Frq(9))";
mostCurrent._ini._vv4(processBA,"Last","Frq9",_frq[(int) (9)]);
 //BA.debugLineNum = 1136;BA.debugLine="INI.WriteInt(\"Last\", \"Frq10\", Frq(10))";
mostCurrent._ini._vv4(processBA,"Last","Frq10",_frq[(int) (10)]);
 //BA.debugLineNum = 1137;BA.debugLine="INI.WriteInt(\"Last\", \"Frq11\", Frq(11))";
mostCurrent._ini._vv4(processBA,"Last","Frq11",_frq[(int) (11)]);
 //BA.debugLineNum = 1138;BA.debugLine="INI.WriteInt(\"Last\", \"Frq12\", Frq(12))";
mostCurrent._ini._vv4(processBA,"Last","Frq12",_frq[(int) (12)]);
 //BA.debugLineNum = 1139;BA.debugLine="INI.WriteInt(\"Last\", \"Frq13\", Frq(13))";
mostCurrent._ini._vv4(processBA,"Last","Frq13",_frq[(int) (13)]);
 //BA.debugLineNum = 1140;BA.debugLine="INI.WriteInt(\"Last\", \"Frq14\", Frq(14))";
mostCurrent._ini._vv4(processBA,"Last","Frq14",_frq[(int) (14)]);
 //BA.debugLineNum = 1141;BA.debugLine="INI.WriteInt(\"Last\", \"Frq15\", Frq(15))";
mostCurrent._ini._vv4(processBA,"Last","Frq15",_frq[(int) (15)]);
 //BA.debugLineNum = 1142;BA.debugLine="INI.WriteInt(\"Last\", \"Frq16\", Frq(16))";
mostCurrent._ini._vv4(processBA,"Last","Frq16",_frq[(int) (16)]);
 //BA.debugLineNum = 1143;BA.debugLine="INI.WriteInt(\"Last\", \"Frq17\", Frq(17))";
mostCurrent._ini._vv4(processBA,"Last","Frq17",_frq[(int) (17)]);
 //BA.debugLineNum = 1144;BA.debugLine="INI.WriteInt(\"Last\", \"Frq18\", Frq(18))";
mostCurrent._ini._vv4(processBA,"Last","Frq18",_frq[(int) (18)]);
 //BA.debugLineNum = 1145;BA.debugLine="INI.WriteInt(\"Last\", \"Frq19\", Frq(19))";
mostCurrent._ini._vv4(processBA,"Last","Frq19",_frq[(int) (19)]);
 //BA.debugLineNum = 1146;BA.debugLine="INI.WriteInt(\"Last\", \"Frq20\", Frq(20))";
mostCurrent._ini._vv4(processBA,"Last","Frq20",_frq[(int) (20)]);
 //BA.debugLineNum = 1147;BA.debugLine="INI.WriteInt(\"Last\", \"DAB0\", DFrq(0))";
mostCurrent._ini._vv4(processBA,"Last","DAB0",_dfrq[(int) (0)]);
 //BA.debugLineNum = 1148;BA.debugLine="INI.WriteInt(\"Last\", \"DAB1\", DFrq(1))";
mostCurrent._ini._vv4(processBA,"Last","DAB1",_dfrq[(int) (1)]);
 //BA.debugLineNum = 1149;BA.debugLine="INI.WriteInt(\"Last\", \"DAB2\", DFrq(2))";
mostCurrent._ini._vv4(processBA,"Last","DAB2",_dfrq[(int) (2)]);
 //BA.debugLineNum = 1150;BA.debugLine="INI.WriteInt(\"Last\", \"DAB3\", DFrq(3))";
mostCurrent._ini._vv4(processBA,"Last","DAB3",_dfrq[(int) (3)]);
 //BA.debugLineNum = 1151;BA.debugLine="INI.WriteInt(\"Last\", \"DAB4\", DFrq(4))";
mostCurrent._ini._vv4(processBA,"Last","DAB4",_dfrq[(int) (4)]);
 //BA.debugLineNum = 1152;BA.debugLine="INI.WriteInt(\"Last\", \"DAB5\", DFrq(5))";
mostCurrent._ini._vv4(processBA,"Last","DAB5",_dfrq[(int) (5)]);
 //BA.debugLineNum = 1153;BA.debugLine="INI.WriteInt(\"Last\", \"DAB6\", DFrq(6))";
mostCurrent._ini._vv4(processBA,"Last","DAB6",_dfrq[(int) (6)]);
 //BA.debugLineNum = 1154;BA.debugLine="INI.WriteInt(\"Last\", \"DAB7\", DFrq(7))";
mostCurrent._ini._vv4(processBA,"Last","DAB7",_dfrq[(int) (7)]);
 //BA.debugLineNum = 1155;BA.debugLine="INI.WriteInt(\"Last\", \"DAB8\", DFrq(8))";
mostCurrent._ini._vv4(processBA,"Last","DAB8",_dfrq[(int) (8)]);
 //BA.debugLineNum = 1156;BA.debugLine="INI.WriteInt(\"Last\", \"DAB9\", DFrq(9))";
mostCurrent._ini._vv4(processBA,"Last","DAB9",_dfrq[(int) (9)]);
 //BA.debugLineNum = 1157;BA.debugLine="INI.WriteInt(\"Last\", \"DAB10\", DFrq(10))";
mostCurrent._ini._vv4(processBA,"Last","DAB10",_dfrq[(int) (10)]);
 //BA.debugLineNum = 1158;BA.debugLine="INI.WriteInt(\"Last\", \"DAB11\", DFrq(11))";
mostCurrent._ini._vv4(processBA,"Last","DAB11",_dfrq[(int) (11)]);
 //BA.debugLineNum = 1159;BA.debugLine="INI.WriteInt(\"Last\", \"DAB12\", DFrq(12))";
mostCurrent._ini._vv4(processBA,"Last","DAB12",_dfrq[(int) (12)]);
 //BA.debugLineNum = 1160;BA.debugLine="INI.WriteInt(\"Last\", \"DAB13\", DFrq(13))";
mostCurrent._ini._vv4(processBA,"Last","DAB13",_dfrq[(int) (13)]);
 //BA.debugLineNum = 1161;BA.debugLine="INI.WriteInt(\"Last\", \"DAB14\", DFrq(14))";
mostCurrent._ini._vv4(processBA,"Last","DAB14",_dfrq[(int) (14)]);
 //BA.debugLineNum = 1162;BA.debugLine="INI.WriteInt(\"Last\", \"DAB15\", DFrq(15))";
mostCurrent._ini._vv4(processBA,"Last","DAB15",_dfrq[(int) (15)]);
 //BA.debugLineNum = 1163;BA.debugLine="INI.WriteInt(\"Last\", \"DAB16\", DFrq(16))";
mostCurrent._ini._vv4(processBA,"Last","DAB16",_dfrq[(int) (16)]);
 //BA.debugLineNum = 1164;BA.debugLine="INI.WriteInt(\"Last\", \"DAB17\", DFrq(17))";
mostCurrent._ini._vv4(processBA,"Last","DAB17",_dfrq[(int) (17)]);
 //BA.debugLineNum = 1165;BA.debugLine="INI.WriteInt(\"Last\", \"DAB18\", DFrq(18))";
mostCurrent._ini._vv4(processBA,"Last","DAB18",_dfrq[(int) (18)]);
 //BA.debugLineNum = 1166;BA.debugLine="INI.WriteInt(\"Last\", \"DAB19\", DFrq(19))";
mostCurrent._ini._vv4(processBA,"Last","DAB19",_dfrq[(int) (19)]);
 //BA.debugLineNum = 1167;BA.debugLine="INI.WriteInt(\"Last\", \"DAB20\", DFrq(20))";
mostCurrent._ini._vv4(processBA,"Last","DAB20",_dfrq[(int) (20)]);
 //BA.debugLineNum = 1168;BA.debugLine="INI.WriteInt(\"Last\", \"Ebene\", Ebene)";
mostCurrent._ini._vv4(processBA,"Last","Ebene",_ebene);
 //BA.debugLineNum = 1169;BA.debugLine="INI.Store";
mostCurrent._ini._vv2(processBA);
 //BA.debugLineNum = 1170;BA.debugLine="Dim DatesFile As RandomAccessFile";
_datesfile = new anywheresoftware.b4a.randomaccessfile.RandomAccessFile();
 //BA.debugLineNum = 1171;BA.debugLine="DatesFile.Initialize(MyPath,\"/DAB.dat\",False)";
_datesfile.Initialize(_mypath,"/DAB.dat",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 1172;BA.debugLine="DatesFile.WriteObject(lstDAB,True,0)";
_datesfile.WriteObject((Object)(_lstdab.getObject()),anywheresoftware.b4a.keywords.Common.True,(long) (0));
 //BA.debugLineNum = 1173;BA.debugLine="DatesFile.Close";
_datesfile.Close();
 //BA.debugLineNum = 1174;BA.debugLine="End Sub";
return "";
}
public static String  _selectchannel(int _channelnum) throws Exception{
String _d1 = "";
String _d2 = "";
String _d3 = "";
String _d = "";
int _i = 0;
 //BA.debugLineNum = 738;BA.debugLine="Sub SelectChannel(ChannelNum As Int)";
 //BA.debugLineNum = 739;BA.debugLine="Dim d1,d2,d3,D As String";
_d1 = "";
_d2 = "";
_d3 = "";
_d = "";
 //BA.debugLineNum = 740;BA.debugLine="Dim I As Int";
_i = 0;
 //BA.debugLineNum = 742;BA.debugLine="Select Ebene";
switch (_ebene) {
case 0:
 //BA.debugLineNum = 744;BA.debugLine="I = 1";
_i = (int) (1);
 break;
case 1:
 //BA.debugLineNum = 746;BA.debugLine="I = 8";
_i = (int) (8);
 break;
case 2:
 //BA.debugLineNum = 748;BA.debugLine="I = 15";
_i = (int) (15);
 break;
}
;
 //BA.debugLineNum = 751;BA.debugLine="I = I+ChannelNum-1";
_i = (int) (_i+_channelnum-1);
 //BA.debugLineNum = 752;BA.debugLine="If isDAB And DAB Then";
if (_isdab && _dab) { 
 //BA.debugLineNum = 753;BA.debugLine="If Not(DABSearch) Then";
if (anywheresoftware.b4a.keywords.Common.Not(_dabsearch)) { 
 //BA.debugLineNum = 754;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_dfrq[_i]),(byte) (0xfd)});
 //BA.debugLineNum = 755;BA.debugLine="CallSub2(Main, \"SelectedDABChannel\", DFrq(I))";
anywheresoftware.b4a.keywords.Common.CallSubNew2(processBA,(Object)(mostCurrent._main.getObject()),"SelectedDABChannel",(Object)(_dfrq[_i]));
 };
 }else {
 //BA.debugLineNum = 759;BA.debugLine="D =  Bit.ToHexString(Frq(I))";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_frq[_i]);
 //BA.debugLineNum = 760;BA.debugLine="d1 = Bit.ParseInt(D.SubString2(0,1), 16)";
_d1 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (0),(int) (1)),(int) (16)));
 //BA.debugLineNum = 761;BA.debugLine="d2 = Bit.ParseInt(D.SubString2(1,3), 16)";
_d2 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (1),(int) (3)),(int) (16)));
 //BA.debugLineNum = 762;BA.debugLine="d3 = Bit.ParseInt(D.SubString2(3,5), 16)";
_d3 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (3),(int) (5)),(int) (16)));
 //BA.debugLineNum = 763;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte)(Double.parseDouble(_d1)),(byte)(Double.parseDouble(_d2)),(byte)(Double.parseDouble(_d3)),(byte) (0xfd)});
 };
 //BA.debugLineNum = 765;BA.debugLine="End Sub";
return "";
}
public static String  _selectdabitem(int _position) throws Exception{
 //BA.debugLineNum = 865;BA.debugLine="Sub SelectDABItem (Position As Int)";
 //BA.debugLineNum = 866;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_position),(byte) (0xfd)});
 //BA.debugLineNum = 867;BA.debugLine="End Sub";
return "";
}
public static String  _sendradio(byte[] _buffer) throws Exception{
 //BA.debugLineNum = 444;BA.debugLine="Sub SendRadio(buffer() As Byte)";
 //BA.debugLineNum = 445;BA.debugLine="If Connected Then astreams.Write(buffer)";
if (_connected) { 
_astreams.Write(_buffer);};
 //BA.debugLineNum = 446;BA.debugLine="End Sub";
return "";
}
public static String  _service_create() throws Exception{
 //BA.debugLineNum = 936;BA.debugLine="Sub Service_Create";
 //BA.debugLineNum = 937;BA.debugLine="RadioNotification.Initialize";
_radionotification.Initialize(processBA);
 //BA.debugLineNum = 938;BA.debugLine="Broadcast.Initialize(\"BroadcastReceiver\")";
_broadcast.Initialize(processBA,"BroadcastReceiver");
 //BA.debugLineNum = 940;BA.debugLine="Broadcast.addAction(\"com.freshollie.radioapp.inte";
_broadcast.addAction("com.freshollie.radioapp.intent.close");
 //BA.debugLineNum = 941;BA.debugLine="Broadcast.addAction(\"com.freshollie.radioapp.inte";
_broadcast.addAction("com.freshollie.radioapp.intent.mute");
 //BA.debugLineNum = 942;BA.debugLine="Broadcast.addAction(\"com.freshollie.radioapp.inte";
_broadcast.addAction("com.freshollie.radioapp.intent.unmute");
 //BA.debugLineNum = 943;BA.debugLine="Broadcast.registerReceiver(\"\")";
_broadcast.registerReceiver("");
 //BA.debugLineNum = 944;BA.debugLine="End Sub";
return "";
}
public static String  _service_destroy() throws Exception{
 //BA.debugLineNum = 992;BA.debugLine="Sub Service_Destroy";
 //BA.debugLineNum = 993;BA.debugLine="Service.StopForeground(1)";
mostCurrent._service.StopForeground((int) (1));
 //BA.debugLineNum = 994;BA.debugLine="Broadcast.unregisterReceiver";
_broadcast.unregisterReceiver();
 //BA.debugLineNum = 995;BA.debugLine="End Sub";
return "";
}
public static String  _service_start(anywheresoftware.b4a.objects.IntentWrapper _startingintent) throws Exception{
String _intentextra = "";
 //BA.debugLineNum = 962;BA.debugLine="Sub Service_Start (StartingIntent As Intent)";
 //BA.debugLineNum = 963;BA.debugLine="Dim intentExtra As String";
_intentextra = "";
 //BA.debugLineNum = 966;BA.debugLine="If StartingIntent.HasExtra(\"Notification_Action_T";
if (_startingintent.HasExtra("Notification_Action_Tag")) { 
 //BA.debugLineNum = 967;BA.debugLine="intentExtra = StartingIntent.GetExtra(\"Notificat";
_intentextra = BA.ObjectToString(_startingintent.GetExtra("Notification_Action_Tag"));
 //BA.debugLineNum = 968;BA.debugLine="Log(\"Notification intent to \" & intentExtra)";
anywheresoftware.b4a.keywords.Common.Log("Notification intent to "+_intentextra);
 //BA.debugLineNum = 969;BA.debugLine="If intentExtra = \"Stop\" Then";
if ((_intentextra).equals("Stop")) { 
 //BA.debugLineNum = 970;BA.debugLine="ExitApp";
_exitapp();
 }else if((_intentextra).equals("Previous Channel")) { 
 //BA.debugLineNum = 972;BA.debugLine="RadioChannelDown";
_radiochanneldown();
 }else if((_intentextra).equals("Next Channel")) { 
 //BA.debugLineNum = 974;BA.debugLine="RadioChannelUp";
_radiochannelup();
 };
 }else if(_connected==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 978;BA.debugLine="ServiceStarted = True";
_servicestarted = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 980;BA.debugLine="If Not(File.Exists(MyPath, \"\")) Then File.MakeDi";
if (anywheresoftware.b4a.keywords.Common.Not(anywheresoftware.b4a.keywords.Common.File.Exists(_mypath,""))) { 
anywheresoftware.b4a.keywords.Common.File.MakeDir(anywheresoftware.b4a.keywords.Common.File.getDirRootExternal(),"dabmonkey");};
 //BA.debugLineNum = 982;BA.debugLine="MyPath = File.DirRootExternal & \"/dabmonkey\"";
_mypath = anywheresoftware.b4a.keywords.Common.File.getDirRootExternal()+"/dabmonkey";
 //BA.debugLineNum = 984;BA.debugLine="Connected = False";
_connected = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 986;BA.debugLine="OpenRadio";
_openradio();
 };
 //BA.debugLineNum = 990;BA.debugLine="End Sub";
return "";
}
public static String  _setchannel(int _channelnum) throws Exception{
int _i = 0;
 //BA.debugLineNum = 767;BA.debugLine="Sub SetChannel(ChannelNum As Int)";
 //BA.debugLineNum = 768;BA.debugLine="Dim I As Int";
_i = 0;
 //BA.debugLineNum = 770;BA.debugLine="Select Ebene";
switch (_ebene) {
case 0:
 //BA.debugLineNum = 772;BA.debugLine="I = 1";
_i = (int) (1);
 break;
case 1:
 //BA.debugLineNum = 774;BA.debugLine="I = 8";
_i = (int) (8);
 break;
case 2:
 //BA.debugLineNum = 776;BA.debugLine="I = 15";
_i = (int) (15);
 break;
}
;
 //BA.debugLineNum = 779;BA.debugLine="I = I+ChannelNum-1";
_i = (int) (_i+_channelnum-1);
 //BA.debugLineNum = 780;BA.debugLine="If isDAB And DAB Then";
if (_isdab && _dab) { 
 //BA.debugLineNum = 781;BA.debugLine="DFrq(I) = Frequenz";
_dfrq[_i] = _frequenz;
 //BA.debugLineNum = 782;BA.debugLine="CallSub2(Main, \"SelectedDABChannel\", I)";
anywheresoftware.b4a.keywords.Common.CallSubNew2(processBA,(Object)(mostCurrent._main.getObject()),"SelectedDABChannel",(Object)(_i));
 }else {
 //BA.debugLineNum = 785;BA.debugLine="Frq(I) = Frequenz";
_frq[_i] = _frequenz;
 };
 //BA.debugLineNum = 787;BA.debugLine="End Sub";
return "";
}
public static String  _setupnotfication() throws Exception{
 //BA.debugLineNum = 946;BA.debugLine="Sub SetUpNotfication";
 //BA.debugLineNum = 947;BA.debugLine="RadioNotification.AddAction2(\"ic_skip_previous_bl";
_radionotification.AddAction2(processBA,"ic_skip_previous_black_24dp","","Previous Channel",radioservice.getObject());
 //BA.debugLineNum = 948;BA.debugLine="RadioNotification.AddAction2(\"ic_stop_black_24dp\"";
_radionotification.AddAction2(processBA,"ic_stop_black_24dp","","Stop",radioservice.getObject());
 //BA.debugLineNum = 949;BA.debugLine="RadioNotification.AddAction2(\"ic_skip_next_black_";
_radionotification.AddAction2(processBA,"ic_skip_next_black_24dp","","Next Channel",radioservice.getObject());
 //BA.debugLineNum = 950;BA.debugLine="RadioNotification.DefaultLight = True";
_radionotification.setDefaultLight(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 951;BA.debugLine="RadioNotification.DefaultVibrate = False";
_radionotification.setDefaultVibrate(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 952;BA.debugLine="RadioNotification.DefaultSound = False";
_radionotification.setDefaultSound(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 953;BA.debugLine="RadioNotification.setActivity(Main)";
_radionotification.setActivity(processBA,(Object)(mostCurrent._main.getObject()));
 //BA.debugLineNum = 954;BA.debugLine="RadioNotification.SmallIcon = \"ic_radio_black_24d";
_radionotification.setSmallIcon("ic_radio_black_24dp");
 //BA.debugLineNum = 955;BA.debugLine="RadioNotification.ContentTitle = \"Radio Running\"";
_radionotification.setContentTitle("Radio Running");
 //BA.debugLineNum = 956;BA.debugLine="RadioNotification.AutoCancel = False";
_radionotification.setAutoCancel(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 958;BA.debugLine="Service.StartForeground(1, RadioNotification.GetN";
mostCurrent._service.StartForeground((int) (1),_radionotification.GetNotification(processBA));
 //BA.debugLineNum = 960;BA.debugLine="End Sub";
return "";
}
public static String  _setvolume(int _volume1) throws Exception{
 //BA.debugLineNum = 851;BA.debugLine="Sub SetVolume(Volume1 As Int)";
 //BA.debugLineNum = 852;BA.debugLine="Log(\"Setting Volume \" & Volume1)";
anywheresoftware.b4a.keywords.Common.Log("Setting Volume "+BA.NumberToString(_volume1));
 //BA.debugLineNum = 853;BA.debugLine="Volume = Volume1";
_volume = (byte) (_volume1);
 //BA.debugLineNum = 854;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0c),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (_volume1),(byte) (0xfd)});
 //BA.debugLineNum = 855;BA.debugLine="Mute = False";
_mute = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 856;BA.debugLine="End Sub";
return "";
}
public static String  _startchannelsearch(boolean _shouldclean) throws Exception{
 //BA.debugLineNum = 870;BA.debugLine="Sub StartChannelSearch(ShouldClean As Boolean)";
 //BA.debugLineNum = 871;BA.debugLine="If isDAB And DAB Then";
if (_isdab && _dab) { 
 //BA.debugLineNum = 872;BA.debugLine="If Not(DABSearch) Then";
if (anywheresoftware.b4a.keywords.Common.Not(_dabsearch)) { 
 //BA.debugLineNum = 873;BA.debugLine="If ShouldClean Then";
if (_shouldclean) { 
 //BA.debugLineNum = 874;BA.debugLine="MyTimer.Enabled = False";
_mytimer.setEnabled(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 875;BA.debugLine="SendRadio(Array As Byte(0xFE,0x00,0x01,0x01,0x";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x00),(byte) (0x01),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x01),(byte) (0xfd)});
 //BA.debugLineNum = 876;BA.debugLine="ClearDatabase = True";
_cleardatabase = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 877;BA.debugLine="SysReady.Enabled = True";
_sysready.setEnabled(anywheresoftware.b4a.keywords.Common.True);
 }else {
 //BA.debugLineNum = 879;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x03,0x01,0x";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x03),(byte) (0x01),(byte) (0x00),(byte) (0x02),(byte) (0x00),(byte) (0x47),(byte) (0xfd)});
 //BA.debugLineNum = 880;BA.debugLine="DABSearch = True";
_dabsearch = anywheresoftware.b4a.keywords.Common.True;
 };
 };
 };
 //BA.debugLineNum = 884;BA.debugLine="End Sub";
return "";
}
public static String  _startmediakeys() throws Exception{
anywheresoftware.b4j.object.JavaObject _context = null;
anywheresoftware.b4j.object.JavaObject _cb = null;
 //BA.debugLineNum = 997;BA.debugLine="Sub StartMediaKeys()";
 //BA.debugLineNum = 998;BA.debugLine="Dim context As JavaObject";
_context = new anywheresoftware.b4j.object.JavaObject();
 //BA.debugLineNum = 999;BA.debugLine="context.InitializeContext";
_context.InitializeContext(processBA);
 //BA.debugLineNum = 1000;BA.debugLine="session.InitializeNewInstance(\"android.media.sess";
_session.InitializeNewInstance("android.media.session.MediaSession",new Object[]{(Object)(_context.getObject()),(Object)("tag")});
 //BA.debugLineNum = 1002;BA.debugLine="Dim cb As JavaObject";
_cb = new anywheresoftware.b4j.object.JavaObject();
 //BA.debugLineNum = 1003;BA.debugLine="cb.InitializeNewInstance(Application.PackageName";
_cb.InitializeNewInstance(anywheresoftware.b4a.keywords.Common.Application.getPackageName()+".radioservice.MyCallback",(Object[])(anywheresoftware.b4a.keywords.Common.Null));
 //BA.debugLineNum = 1004;BA.debugLine="session.RunMethod(\"setCallback\", Array(cb))";
_session.RunMethod("setCallback",new Object[]{(Object)(_cb.getObject())});
 //BA.debugLineNum = 1006;BA.debugLine="session.RunMethod(\"setFlags\",Array(3))";
_session.RunMethod("setFlags",new Object[]{(Object)(3)});
 //BA.debugLineNum = 1007;BA.debugLine="session.RunMethod(\"setActive\", Array(True))";
_session.RunMethod("setActive",new Object[]{(Object)(anywheresoftware.b4a.keywords.Common.True)});
 //BA.debugLineNum = 1008;BA.debugLine="Log(\"Accquired media session\")";
anywheresoftware.b4a.keywords.Common.Log("Accquired media session");
 //BA.debugLineNum = 1009;BA.debugLine="End Sub";
return "";
}
public static String  _switchtodab() throws Exception{
 //BA.debugLineNum = 714;BA.debugLine="Sub SwitchToDAB";
 //BA.debugLineNum = 715;BA.debugLine="If Not(DABSearch) And DAB Then";
if (anywheresoftware.b4a.keywords.Common.Not(_dabsearch) && _dab) { 
 //BA.debugLineNum = 716;BA.debugLine="Select Ebene";
switch (_ebene) {
case 0:
 //BA.debugLineNum = 718;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_dfrq[(int) (0)]),(byte) (0xfd)});
 break;
case 1:
 //BA.debugLineNum = 720;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_dfrq[(int) (7)]),(byte) (0xfd)});
 break;
case 2:
 //BA.debugLineNum = 722;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x0";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (0x00),(byte) (_dfrq[(int) (14)]),(byte) (0xfd)});
 break;
}
;
 };
 //BA.debugLineNum = 725;BA.debugLine="End Sub";
return "";
}
public static String  _switchtofm() throws Exception{
String _d1 = "";
String _d2 = "";
String _d3 = "";
String _d = "";
 //BA.debugLineNum = 684;BA.debugLine="Sub SwitchToFM";
 //BA.debugLineNum = 685;BA.debugLine="Dim d1,d2,d3,D As String";
_d1 = "";
_d2 = "";
_d3 = "";
_d = "";
 //BA.debugLineNum = 686;BA.debugLine="Select Ebene";
switch (_ebene) {
case 0:
 //BA.debugLineNum = 688;BA.debugLine="D =  Bit.ToHexString(Frq(0))";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_frq[(int) (0)]);
 break;
case 1:
 //BA.debugLineNum = 690;BA.debugLine="D =  Bit.ToHexString(Frq(7))";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_frq[(int) (7)]);
 break;
case 2:
 //BA.debugLineNum = 692;BA.debugLine="D =  Bit.ToHexString(Frq(14))";
_d = anywheresoftware.b4a.keywords.Common.Bit.ToHexString(_frq[(int) (14)]);
 break;
}
;
 //BA.debugLineNum = 694;BA.debugLine="d1 = Bit.ParseInt(D.SubString2(0,1), 16)";
_d1 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (0),(int) (1)),(int) (16)));
 //BA.debugLineNum = 695;BA.debugLine="d2 = Bit.ParseInt(D.SubString2(1,3), 16)";
_d2 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (1),(int) (3)),(int) (16)));
 //BA.debugLineNum = 696;BA.debugLine="d3 = Bit.ParseInt(D.SubString2(3,5), 16)";
_d3 = BA.NumberToString(anywheresoftware.b4a.keywords.Common.Bit.ParseInt(_d.substring((int) (3),(int) (5)),(int) (16)));
 //BA.debugLineNum = 697;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x05),(byte) (0x01),(byte) (0x00),(byte)(Double.parseDouble(_d1)),(byte)(Double.parseDouble(_d2)),(byte)(Double.parseDouble(_d3)),(byte) (0xfd)});
 //BA.debugLineNum = 698;BA.debugLine="isDAB = False";
_isdab = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 699;BA.debugLine="End Sub";
return "";
}
public static String  _sysready_tick() throws Exception{
 //BA.debugLineNum = 451;BA.debugLine="Sub SysReady_Tick";
 //BA.debugLineNum = 452;BA.debugLine="SendRadio(Array As Byte(0xFE,0x00,0x00,0x01,0x00,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x00),(byte) (0x00),(byte) (0x01),(byte) (0x00),(byte) (0x00),(byte) (0xfd)});
 //BA.debugLineNum = 453;BA.debugLine="End Sub";
return "";
}
public static String  _togglemute() throws Exception{
 //BA.debugLineNum = 637;BA.debugLine="Sub ToggleMute";
 //BA.debugLineNum = 638;BA.debugLine="If Mute Then";
if (_mute) { 
 //BA.debugLineNum = 639;BA.debugLine="Mute = False";
_mute = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 640;BA.debugLine="UnmuteAudio";
_unmuteaudio();
 }else {
 //BA.debugLineNum = 642;BA.debugLine="Mute = True";
_mute = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 643;BA.debugLine="MuteAudio";
_muteaudio();
 };
 //BA.debugLineNum = 645;BA.debugLine="End Sub";
return "";
}
public static String  _unmuteaudio() throws Exception{
 //BA.debugLineNum = 653;BA.debugLine="Sub UnmuteAudio";
 //BA.debugLineNum = 654;BA.debugLine="Log(\"Unmuting\")";
anywheresoftware.b4a.keywords.Common.Log("Unmuting");
 //BA.debugLineNum = 655;BA.debugLine="Volume = LastVolume";
_volume = (byte) (_lastvolume);
 //BA.debugLineNum = 656;BA.debugLine="SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,";
_sendradio(new byte[]{(byte) (0xfe),(byte) (0x01),(byte) (0x0c),(byte) (0x01),(byte) (0x00),(byte) (0x01),(byte) (_lastvolume),(byte) (0xfd)});
 //BA.debugLineNum = 657;BA.debugLine="End Sub";
return "";
}
public static String  _usbtimer_tick() throws Exception{
 //BA.debugLineNum = 553;BA.debugLine="Sub USBTimer_Tick";
 //BA.debugLineNum = 554;BA.debugLine="Log(\"Trying to connect again\")";
anywheresoftware.b4a.keywords.Common.Log("Trying to connect again");
 //BA.debugLineNum = 555;BA.debugLine="If Not (Connected) Then OpenRadio";
if (anywheresoftware.b4a.keywords.Common.Not(_connected)) { 
_openradio();};
 //BA.debugLineNum = 556;BA.debugLine="End Sub";
return "";
}
public static String  _volumedown() throws Exception{
 //BA.debugLineNum = 844;BA.debugLine="Sub VolumeDown";
 //BA.debugLineNum = 845;BA.debugLine="If Volume > 0 Then";
if (_volume>0) { 
 //BA.debugLineNum = 846;BA.debugLine="Volume = Volume - 1";
_volume = (byte) (_volume-1);
 //BA.debugLineNum = 847;BA.debugLine="SetVolume(Volume)";
_setvolume((int) (_volume));
 };
 //BA.debugLineNum = 849;BA.debugLine="End Sub";
return "";
}
public static String  _volumeup() throws Exception{
 //BA.debugLineNum = 837;BA.debugLine="Sub VolumeUp";
 //BA.debugLineNum = 838;BA.debugLine="If Volume < 16 Then";
if (_volume<16) { 
 //BA.debugLineNum = 839;BA.debugLine="Volume = Volume + 1";
_volume = (byte) (_volume+1);
 //BA.debugLineNum = 840;BA.debugLine="SetVolume(Volume)";
_setvolume((int) (_volume));
 };
 //BA.debugLineNum = 842;BA.debugLine="End Sub";
return "";
}
public static String  _wait(int _sekunden) throws Exception{
long _ti = 0L;
 //BA.debugLineNum = 39;BA.debugLine="Sub Wait(Sekunden As Int)";
 //BA.debugLineNum = 40;BA.debugLine="Dim Ti As Long";
_ti = 0L;
 //BA.debugLineNum = 41;BA.debugLine="Ti = DateTime.Now + (Sekunden * 1000)";
_ti = (long) (anywheresoftware.b4a.keywords.Common.DateTime.getNow()+(_sekunden*1000));
 //BA.debugLineNum = 42;BA.debugLine="Do While DateTime.Now < Ti";
while (anywheresoftware.b4a.keywords.Common.DateTime.getNow()<_ti) {
 //BA.debugLineNum = 43;BA.debugLine="DoEvents";
anywheresoftware.b4a.keywords.Common.DoEvents();
 }
;
 //BA.debugLineNum = 45;BA.debugLine="End Sub";
return "";
}
public static class MyCallback extends Callback {
   public MyCallback() {
   }
   public void onCommand(String command, Bundle args, ResultReceiver cb) {
   BA.Log(command);
     processBA.raiseEventFromUI(null, "media_oncommand", command);
   }
   public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
   	KeyEvent event = (KeyEvent)mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
    IntentWrapper baIntent = new IntentWrapper();
    baIntent.setObject(mediaButtonIntent);
	if (event.getAction() == KeyEvent.ACTION_UP){
   		Boolean b = (Boolean) processBA.raiseEvent(null, "media_onbutton", event.getKeyCode());
    	return b == null ? false : b;
	} else {
	return false;
	}
   }
}
}
