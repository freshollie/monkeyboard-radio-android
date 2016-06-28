package com.freshollie.radioapp;


import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BALayout;
import anywheresoftware.b4a.debug.*;

public class slideshow {
private static slideshow mostCurrent = new slideshow();
public static Object getObject() {
    throw new RuntimeException("Code module does not support this method.");
}
 public anywheresoftware.b4a.keywords.Common __c = null;
public static byte _head = (byte)0;
public static byte _endbyte = (byte)0;
public static int _stream_mode_dab = 0;
public static int _stream_mode_fm = 0;
public static int _text_buffer_len = 0;
public static byte _bit0 = (byte)0;
public static byte _bit1 = (byte)0;
public static byte _bit2 = (byte)0;
public static byte _bit3 = (byte)0;
public static byte _bit4 = (byte)0;
public static byte _bit5 = (byte)0;
public static byte _bit6 = (byte)0;
public static byte _bit7 = (byte)0;
public static int _max_packet_per_group = 0;
public static int _max_group_per_object_body = 0;
public static int _max_object_size = 0;
public static int _max_packet_per_object_body = 0;
public static int _max_payload_per_packet = 0;
public static int _max_object_num = 0;
public static int _max_packet_in_data_pool = 0;
public static int _max_name_length = 0;
public static byte _flag_header_complete = (byte)0;
public static byte _flag_directory_complete = (byte)0;
public static byte _flag_body_complete = (byte)0;
public static byte _hdr_flag_name = (byte)0;
public static byte _hdr_flag_scopeid = (byte)0;
public static byte _hdr_flag_compressed = (byte)0;
public wal.INIFiles.ini _ini = null;
public com.freshollie.radioapp.main _main = null;
public com.freshollie.radioapp.radioservice _radioservice = null;
public static String  _getimage(anywheresoftware.b4a.BA _ba) throws Exception{
 //BA.debugLineNum = 67;BA.debugLine="Sub GetImage";
 //BA.debugLineNum = 69;BA.debugLine="End Sub";
return "";
}
public static String  _motassemble(anywheresoftware.b4a.BA _ba) throws Exception{
 //BA.debugLineNum = 55;BA.debugLine="Sub MotAssemble";
 //BA.debugLineNum = 57;BA.debugLine="End Sub";
return "";
}
public static String  _motdirectoryparser(anywheresoftware.b4a.BA _ba) throws Exception{
 //BA.debugLineNum = 63;BA.debugLine="Sub MotDirectoryParser";
 //BA.debugLineNum = 65;BA.debugLine="End Sub";
return "";
}
public static String  _motheaderparser(anywheresoftware.b4a.BA _ba) throws Exception{
 //BA.debugLineNum = 59;BA.debugLine="Sub MotHeaderParser";
 //BA.debugLineNum = 61;BA.debugLine="End Sub";
return "";
}
public static String  _motquery(anywheresoftware.b4a.BA _ba) throws Exception{
 //BA.debugLineNum = 51;BA.debugLine="Sub MOTQuery";
 //BA.debugLineNum = 53;BA.debugLine="End Sub";
return "";
}
public static String  _motreset(anywheresoftware.b4a.BA _ba,Object _enmode) throws Exception{
 //BA.debugLineNum = 47;BA.debugLine="Sub MOTReset(enMode As Object)";
 //BA.debugLineNum = 49;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 3;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 7;BA.debugLine="Dim HEAD As Byte = 0xFE";
_head = (byte) (0xfe);
 //BA.debugLineNum = 8;BA.debugLine="Dim ENDBYTE As Byte = 0xFD";
_endbyte = (byte) (0xfd);
 //BA.debugLineNum = 9;BA.debugLine="Dim STREAM_MODE_DAB As Int = 0";
_stream_mode_dab = (int) (0);
 //BA.debugLineNum = 10;BA.debugLine="Dim STREAM_MODE_FM As Int = 1";
_stream_mode_fm = (int) (1);
 //BA.debugLineNum = 12;BA.debugLine="Dim TEXT_BUFFER_LEN As Int= 300";
_text_buffer_len = (int) (300);
 //BA.debugLineNum = 14;BA.debugLine="Dim BIT0 As Byte = 0x01";
_bit0 = (byte) (0x01);
 //BA.debugLineNum = 15;BA.debugLine="Dim BIT1 As Byte = 0x02";
_bit1 = (byte) (0x02);
 //BA.debugLineNum = 16;BA.debugLine="Dim BIT2 As Byte = 0x04";
_bit2 = (byte) (0x04);
 //BA.debugLineNum = 17;BA.debugLine="Dim BIT3 As Byte = 0x08";
_bit3 = (byte) (0x08);
 //BA.debugLineNum = 18;BA.debugLine="Dim BIT4 As Byte = 0x10";
_bit4 = (byte) (0x10);
 //BA.debugLineNum = 19;BA.debugLine="Dim BIT5 As Byte = 0x20";
_bit5 = (byte) (0x20);
 //BA.debugLineNum = 20;BA.debugLine="Dim BIT6 As Byte = 0x40";
_bit6 = (byte) (0x40);
 //BA.debugLineNum = 21;BA.debugLine="Dim BIT7 As Byte = 0x80";
_bit7 = (byte) (0x80);
 //BA.debugLineNum = 23;BA.debugLine="Dim MAX_PACKET_PER_GROUP As Int = 36";
_max_packet_per_group = (int) (36);
 //BA.debugLineNum = 24;BA.debugLine="Dim MAX_GROUP_PER_OBJECT_BODY As Int = 80";
_max_group_per_object_body = (int) (80);
 //BA.debugLineNum = 25;BA.debugLine="Dim MAX_OBJECT_SIZE As Int = (1024 * 50)";
_max_object_size = (int) ((1024*50));
 //BA.debugLineNum = 26;BA.debugLine="Dim MAX_PACKET_PER_OBJECT_BODY As Int = MAX_PACKE";
_max_packet_per_object_body = (int) (_max_packet_per_group*_max_group_per_object_body);
 //BA.debugLineNum = 27;BA.debugLine="Dim MAX_PAYLOAD_PER_PACKET As Int = (236+7)";
_max_payload_per_packet = (int) ((236+7));
 //BA.debugLineNum = 28;BA.debugLine="Dim MAX_OBJECT_NUM As Int = 100";
_max_object_num = (int) (100);
 //BA.debugLineNum = 29;BA.debugLine="Dim MAX_PACKET_IN_DATA_POOL As Int = 10240";
_max_packet_in_data_pool = (int) (10240);
 //BA.debugLineNum = 31;BA.debugLine="Dim MAX_NAME_LENGTH As Int = 100";
_max_name_length = (int) (100);
 //BA.debugLineNum = 33;BA.debugLine="Dim FLAG_HEADER_COMPLETE As Byte = BIT0";
_flag_header_complete = _bit0;
 //BA.debugLineNum = 34;BA.debugLine="Dim FLAG_DIRECTORY_COMPLETE As Byte = BIT1";
_flag_directory_complete = _bit1;
 //BA.debugLineNum = 35;BA.debugLine="Dim FLAG_BODY_COMPLETE As Byte = BIT2";
_flag_body_complete = _bit2;
 //BA.debugLineNum = 38;BA.debugLine="Dim HDR_FLAG_NAME As Byte = BIT0";
_hdr_flag_name = _bit0;
 //BA.debugLineNum = 39;BA.debugLine="Dim HDR_FLAG_SCOPEID  As Byte = BIT1";
_hdr_flag_scopeid = _bit1;
 //BA.debugLineNum = 40;BA.debugLine="Dim HDR_FLAG_COMPRESSED As Byte = BIT2";
_hdr_flag_compressed = _bit2;
 //BA.debugLineNum = 43;BA.debugLine="End Sub";
return "";
}
public static String  _slideshowtest(anywheresoftware.b4a.BA _ba) throws Exception{
 //BA.debugLineNum = 71;BA.debugLine="Sub SlideShowTest";
 //BA.debugLineNum = 73;BA.debugLine="End Sub";
return "";
}
}
