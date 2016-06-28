Type=StaticCode
Version=5.02
ModulesStructureVersion=1
B4A=true
@EndOfDesignText@
'Code module
'Subs in this code module will be accessible from all modules.

Sub Process_Globals
	'These global variables will be declared once when the application starts.
	'These variables can be accessed from all modules.
	
	Dim HEAD As Byte = 0xFE
	Dim ENDBYTE As Byte = 0xFD
	Dim STREAM_MODE_DAB As Int = 0
	Dim STREAM_MODE_FM As Int = 1

	Dim TEXT_BUFFER_LEN As Int= 300

	Dim BIT0 As Byte = 0x01
	Dim BIT1 As Byte = 0x02
	Dim BIT2 As Byte = 0x04
	Dim BIT3 As Byte = 0x08
	Dim BIT4 As Byte = 0x10
	Dim BIT5 As Byte = 0x20
	Dim BIT6 As Byte = 0x40
	Dim BIT7 As Byte = 0x80
	
	Dim MAX_PACKET_PER_GROUP As Int = 36
	Dim MAX_GROUP_PER_OBJECT_BODY As Int = 80
	Dim MAX_OBJECT_SIZE As Int = (1024 * 50)
	Dim MAX_PACKET_PER_OBJECT_BODY As Int = MAX_PACKET_PER_GROUP*MAX_GROUP_PER_OBJECT_BODY
	Dim MAX_PAYLOAD_PER_PACKET As Int = (236+7)
	Dim MAX_OBJECT_NUM As Int = 100
	Dim MAX_PACKET_IN_DATA_POOL As Int = 10240

	Dim MAX_NAME_LENGTH As Int = 100

	Dim FLAG_HEADER_COMPLETE As Byte = BIT0
	Dim FLAG_DIRECTORY_COMPLETE As Byte = BIT1
	Dim FLAG_BODY_COMPLETE As Byte = BIT2


	Dim HDR_FLAG_NAME As Byte = BIT0
	Dim HDR_FLAG_SCOPEID  As Byte = BIT1
	Dim HDR_FLAG_COMPRESSED As Byte = BIT2
	
End Sub