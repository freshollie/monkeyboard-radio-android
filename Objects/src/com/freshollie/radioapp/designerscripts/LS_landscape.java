package com.freshollie.radioapp.designerscripts;
import anywheresoftware.b4a.objects.TextViewWrapper;
import anywheresoftware.b4a.objects.ImageViewWrapper;
import anywheresoftware.b4a.BA;


public class LS_landscape{

public static void LS_general(java.util.LinkedHashMap<String, anywheresoftware.b4a.keywords.LayoutBuilder.ViewWrapperAndAnchor> views, int width, int height, float scale) {
anywheresoftware.b4a.keywords.LayoutBuilder.setScaleRate(0.3);
//BA.debugLineNum = 3;BA.debugLine="AutoScaleRate(1)"[landscape/General script]
anywheresoftware.b4a.keywords.LayoutBuilder.setScaleRate(1d);
//BA.debugLineNum = 4;BA.debugLine="AutoScaleAll"[landscape/General script]
anywheresoftware.b4a.keywords.LayoutBuilder.scaleAll(views);

}
}