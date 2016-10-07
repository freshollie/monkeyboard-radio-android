Type=Service
Version=5.02
ModulesStructureVersion=1
B4A=true
@EndOfDesignText@
#Region  Service Attributes 
	#StartAtBoot: False
#End Region

' Documentation
' Service will close when device disconnects due to not being able to communicate with the USB device,
' not due to android device disconnect intents
'
' Service can be started silently in the background using shell scripts
' 

#Region'--------------------Globals---------------------

Sub Process_Globals
	Dim USB As UsbSerial 
	Dim UsbPid As Int = 0xa 
	Dim UsbVid As Int = 0x4D8
	Dim astreams As AsyncStreams
	Dim MyTimer, SysReady, USBTimer, TryAgain As Timer
	Dim Broadcast As BroadCastReceiver
	Dim AudioFocusManager As AudioFocus
	Dim Start, Mute, DABSearch, isDAB, DAB, Connected, FillList, ClearDatabase, Ducked As Boolean 
	Dim Volume, Strength As Byte
	Dim Ack(1024), Frq(21), DFrq(21), Frequenz, iIndex, xIndex, Dev, Ebene, AllDAB, iStep, iLoop As Int
	Dim lstDAB As List
	Dim RadioNotification As NotificationBuilder
	Dim ProgramText, ProgramType, ProgramName, ProgramName2, Esamble, Status As String
	Dim labEventText,labFreqText,labStrengthText,labVolumeText,labProgramText,labProgramTypeText,labProgram2Text,ProgramNameText,StereoModeText,DataRateText As String
	Dim pbStrengthProgress As Double
	Dim MyPath As String
	Dim PreviousNotificationText As String
	Dim PreviousNotificationText2 As String
	Dim ServiceStarted, ForceClosing As Boolean
	Dim Mediakey As MediaController
	Dim DuckVolume, DefaultVolume, LastVolume, part As Int
	Dim MuteResponse As String
	Dim StationCopyResponse As Int
	
	Dim CloseAfterStop As Boolean
	
	Private session As JavaObject
	
	Dim EnterClickedReturnValue As Boolean
	
End Sub

#End Region

#Region'--------------------Functions------------------

Sub Wait(Sekunden As Int)
	Dim Ti As Long
	Ti = DateTime.Now + (Sekunden * 1000)
	Do While DateTime.Now < Ti
	DoEvents
	Loop
End Sub
#End Region

#Region'--------------------SerialStuff---------------------

Sub Evaluate(index As Int)
	' Evaulate does looks at the byte string that was recived and
	' determines what the response is related to
	
	Dim j As Int
	Dim ArgumentsList As List
'	Dim kj = 1 As Int
	'Log("New eval")
'	Do While kj<index
'		Log(Ack(kj))
'		kj= kj +1
'	Loop
'	
	Select Ack(1)
	Case 0x01 ' If its an acknowledged command
		Select Ack(2) 
		
		Case 0x05 'Stream_GetPlayStatus response
			Select Case Ack(6)
			
			Case 0
				Status = "Playing"
			Case 1
				Status = "Searching"
			Case 2
				Status = "Tunning"
			Case 3
				Status = "Stop"
			Case 4
				Status = "Sorting"
			Case 5
				Status = "Reconfiguration"
			Case Else
				Status = "N/A"
		  	End Select
			
		
		labEventText = Status
			
		Case 0x07 ' Stream_GetPlayIndex()
			If Ack(6) = 0 Then 		
				Frequenz = Bit.ParseInt(Bit.ToHexString(Ack(7)) &  Bit.ToHexString(Ack(8)) &  Bit.ToHexString(Ack(9)), 16)
				If isDAB And DAB Then
					labFreqText = Frequenz
					If FillList Then
						StationCopyResponse = StationCopyResponse + 1
					End If 
				Else
					If Frequenz < 108100 And Frequenz > 87400 Then labFreqText = Frequenz / 1000
				End If	
			End If
			
		Case 0x08 
			Strength = Ack(6) 
			If isDAB Then
				labStrengthText = Strength * 6
				pbStrengthProgress = Strength * 6
			Else
				labStrengthText = Strength
				pbStrengthProgress = Strength
			End If
			
		Case 0x0B 'STREAM_GetStereo Response
			Select Ack(6)
			
			StereoModeText = "N/A"
			
			Case 0
				StereoModeText = "Stereo"
			Case 1
				StereoModeText = "Joint Stereo"
			Case 2
				StereoModeText = "Dual Channel"
			Case 3
				StereoModeText = "Mono"
			
			End Select			
			
		Case 0x0D
			If Mute Then
				labVolumeText = "--"			
			Else	
				labVolumeText = Volume			
			End If	
			
		Case 0x0F
			ProgramName = ""
			For j = 6 To index - 1
				ProgramName = ProgramName & Chr(Ack(j))	
			Next
			ProgramNameText = ProgramName
					
		Case 0x0E
			Select Case Ack(6)
			Case 0
				ProgramType = ""
			Case 1
				ProgramType = "News"
			Case 2
				ProgramType = "Current Affairs"
			Case 3
				ProgramType = "Information"
			Case 4
				ProgramType = "Sport"
			Case 5
				ProgramType = "Education"
			Case 6
				ProgramType = "Drama"
			Case 7
				ProgramType = "Arts"
			Case 8
				ProgramType = "Science"
			Case 9
				ProgramType = "Talk"
			Case 10
				ProgramType = "Pop Music"
			Case 11
				ProgramType = "Rock Music"
			Case 12
				ProgramType = "Easy Listening"
			Case 13
				ProgramType = "Light Classical"
			Case 14
				ProgramType = "Classical Music"
			Case 15
				ProgramType = "Other Music"
			Case 16
				ProgramType = "Weather"
			Case 17
				ProgramType = "Finance"
			Case 18
				ProgramType = "Children"
			Case 19
				ProgramType = "Factual"
			Case 20
				ProgramType = "Religion"
			Case 21
				ProgramType = "Phone In"
			Case 22
				ProgramType = "Travel"
			Case 23
				ProgramType = "Leisure"
			Case 24
				ProgramType = "Jazz and Blues"
			Case 25
				ProgramType = "Country Music"
			Case 26
				ProgramType = "National Music"
			Case 27
				ProgramType = "Oldies Music"
			Case 28
				ProgramType = "Folk Music"
			Case 29
				ProgramType = "Documentary"
			Case 30
				ProgramType = "Undefined"
			Case 31
				ProgramType = "Undefined"
			Case Else
				ProgramType = ""
			End Select
								
			labProgramTypeText = ProgramType
			
		Case 0x10
			ProgramText = ""
			For j = 6 To index - 1
				ProgramText  = ProgramText  & Chr(Ack(j))	
			Next
			labProgramText = ProgramText
			
		Case 0x12 'STREAM_GetDataRate() response
		
			DataRateText = Floor(Ack(6) & Ack(7)) & " Kbps"
			
		Case 0x13 'STREAM_GetSingalQuality() response
			Strength = Ack(6) 
			labStrengthText = Strength
			pbStrengthProgress = Strength
				
		Case 0x14
			If Ack(6) = 0x47 Then
				DABSearch = False
				SendRadio(Array As Byte(0xFE,0x01,0x16,0x01,0x00,0x00,0xFD))
			Else
				labEventText = "DABSearch ...  " & Ack(6)
			End If		
			
		Case 0x15
			Esamble = ""
			For j = 6 To index - 1
				Esamble = Esamble & Chr(Ack(j))	
			Next	
			labProgram2Text = Esamble
			
		Case 0x16
			AllDAB = Bit.ParseInt(Bit.ToHexString(Ack(6)) & Bit.ToHexString(Ack(7)) &  Bit.ToHexString(Ack(8)) &  Bit.ToHexString(Ack(9)), 16)
			labEventText ="Copying " & 0 & "/" & AllDAB
			lstDAB.Clear
			'NameResponded = True
			iStep = 0
			MyTimer.Interval = 500
			Wait(3)
			FillList = True
			
		Case 0x1A
			ProgramName2 = ""
			For j = 6 To index - 1
				If Ack(j) <> 0 Then
					ProgramName2 = ProgramName2 & Chr(Ack(j))	
				End If
			Next
			
			If FillList Then
				Log(ProgramName2)
				Dim lastProgramName As String
				If lstDAB.Size > 0 Then
					lastProgramName = lstDAB.Get(lstDAB.Size-1)
				Else
					lastProgramName = ""
				End If
					
				If lastProgramName <> ProgramName2 Then
					iStep = lstDAB.Size+1
					lstDAB.Add(ProgramName2)
					labEventText ="Copying " & lstDAB.Size & "/" & AllDAB
				End If
				MyTimer_Tick
			Else
				Dim b() As Byte = ProgramName2.GetBytes("UTF8")
  				ProgramName2 = BytesToString(b, 0, b.Length, "UTF8")
				ProgramNameText = ProgramName2
				ProgramName2=ProgramName2.Replace(" ","")				
			End If
			
		End Select
		
	Case 0x00
		If Ack(2) = 0x01 And Ack(3) = 0x01 And ClearDatabase Then 
			iLoop = iLoop +1
			If iLoop > 2 Then
				SysReady.Enabled = False
				SendRadio(Array As Byte(0xFE,0x01,0x03,0x01,0x00,0x02,0x00,0x47,0xFD))
				DABSearch = True
				iLoop = 0
				MyTimer.Enabled = True
				ClearDatabase = False
			End If
		Else 'Reponse from board 
			If MuteResponse = "0" Then
				Log("Mute has been performed, can now close radio service")
				MuteResponse = "1"
				ExitApp
			End If
			
			If iIndex < 3 Then 
				
				Select(iIndex)
				
				Case(0)
					If part = 1 Then iIndex = iIndex + 1
					Log("Responded")
				Case(1)
					If part = 2 Then iIndex = iIndex + 1
					Log("Responded")
				Case(2)
					If part = 3 Then iIndex = iIndex + 1
					Log("Responded")
				End Select
				
			End If
		End If
	End Select
	
	ArgumentsList.Initialize()
	ArgumentsList.Add(labEventText)
	ArgumentsList.Add(labFreqText)
	ArgumentsList.Add(labStrengthText)
	ArgumentsList.Add(pbStrengthProgress)
	ArgumentsList.Add(labVolumeText)
	ArgumentsList.Add(ProgramNameText)
	ArgumentsList.Add(labProgramTypeText)
	ArgumentsList.Add(labProgram2Text)
	ArgumentsList.Add(labProgramText)
	ArgumentsList.Add(StereoModeText)
	ArgumentsList.Add(DataRateText)

	Dim Changed As Boolean = False
	
	If PreviousNotificationText <> ProgramNameText Then
		RadioNotification.ContentText = ProgramNameText
		PreviousNotificationText = ProgramNameText
		Changed = True
	End If
	
	If PreviousNotificationText2 <> labEventText Then
		RadioNotification.ContentInfo = labEventText
		PreviousNotificationText2 = labEventText
		Changed = True
	End If
	
	If Changed Then
		Service.StartForeground(1,RadioNotification.GetNotification)
	End If
	
	If IsPaused(Main) <> True Then
		CallSub2(Main,"SetEvaluatedData",ArgumentsList)
	End If
	
	
	
End Sub

Sub LabelClean
	If Connected Then
		MyTimer.Enabled = False
		Esamble = ""
		ProgramName = ""
		ProgramName2 = ""
		ProgramText = ""
		ProgramType = "" 
		Strength = 0
		iIndex = 3
		MyTimer.Enabled = True
	End If
End Sub

Sub RTS(Bool As Boolean)
	Dim r As Reflector
	r.Target = USB
	r.Target = r.getField("driver")
	r.RunMethod2("setRTS", Bool, "java.lang.boolean")
End Sub

Sub DTR(Bool As Boolean)
	Dim r As Reflector
	r.Target = USB
	r.Target = r.getField("driver")
	r.RunMethod2("setDTR", Bool, "java.lang.boolean")
End Sub


Sub OpenRadio

	Dim UsbMngr As UsbManager  ' USB library
	
	UsbMngr.Initialize
	Dim UsbDevices() As UsbDevice  ' USB library

	UsbDevices = UsbMngr.GetDevices

	'Iterate over USB devices and find the correct one

	If UsbDevices.Length > 0 Then
		Log(UsbDevices.Length)
		
		For i = 0 To UsbDevices.Length - 1
			Dim UsbDvc As UsbDevice
			UsbDvc = UsbDevices(i)
				
	 		If (UsbDvc.ProductId = UsbPid) And (UsbDvc.VendorId = UsbVid) Then
				USB.SetCustomDevice(USB.DRIVER_CDCACM, UsbVid, UsbPid)
				
				If Not(UsbMngr.HasPermission(UsbDvc)) Then 
					UsbMngr.RequestPermission(UsbDvc)
				End If
				
				If UsbMngr.HasPermission(UsbDvc) Then
					Dev = USB.Open(57600, i + 1)	
						
					If Dev <> USB.USB_NONE Then
						USBTimer.Enabled = False
						Log("Connected successfully!")		
						
						astreams.Initialize(USB.GetInputStream, USB.GetOutputStream, "astreams")
						
						RTS(True)	 				
						DTR(False)
						
						If Not(SysReady.IsInitialized) Then SysReady.Initialize("SysReady",500)
						If Not(MyTimer.IsInitialized) Then MyTimer.Initialize("MyTimer",25)
						
						StartMediaKeys
						Mediakey.MediaButton(KeyCodes.KEYCODE_MEDIA_STOP)
						AudioFocusManager.Initialize("AudioFocusManager")
						AudioFocusManager.requestFocus
						Ducked = False
						
						Connected = True
						SetUpNotfication
						Broadcast.sendBroadcast("com.freshollie.radioapp.RUNNING")
						
						DAB = True		
						LoadSettings
						iIndex = 0
						part = 0
						Start = True
						MuteResponse = "Null"
						ForceClosing = False
						CloseAfterStop = False
						Wait(1)	
						
	   					MyTimer.Enabled = True
						Return 
					End If
				End If
				
				Exit
			End If
		Next
	End If
	ForceClosing = True
	ExitApp
End Sub


Sub CloseRadio
	USB.Close()
End Sub

Sub SendRadio(buffer() As Byte)
	If Connected Then astreams.Write(buffer)
End Sub

#End Region

#Region'----------Ticks---------
Sub SysReady_Tick
	SendRadio(Array As Byte(0xFE,0x00,0x00,0x01,0x00,0x00,0xFD))
End Sub

Sub MyTimer_Tick
	' My timer asks for all of the needed radio infomation for the app
	' a new request is sent every 25ms at normal rate and 500ms when checking for the names of the DAB channels
	
	If Connected Then 
		If DABSearch Then 'If a dab search has been performed then get the current channel index
			SendRadio(Array As Byte(0xFE,0x01,0x14,0x01,0x00,0x04,0x00,0x00,0x00,0x00,0xFD)) ' Get current DAB channel index
		Else
			If FillList Then
				If iStep < AllDAB Then
					Log(iStep)
					StationCopyResponse = 0
					SendRadio(Array As Byte(0xFE,0x01,0x1A,0x01,0x00,0x05,0x00,0x00,0x00,iStep,0x01,0xFD)) ' Get the service name of the dab program STREAM_GetServiceName(Frequenz, iStep) long name
					SendRadio(Array As Byte(0xFE,0x01,0x07,0x01,0x00,0x00,0xFD)) 'Get the channel id
				Else
					iStep = 0
					FillList = False
					MyTimer.Interval = 25 'Go back to normal send and recieve frequency
					CallSub(Main, "updateStationList")
				End If
			Else
				If isDAB And DAB Then 'If in DAB mode
					
					' A different one of these cases is called every 
					Select iIndex
					Case 0
						Log("Set to Fequency " & Frequenz) 
						SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,Frequenz,0xFD)) ' Play DAB Stream_PLAY(0, Frequenz) Only Called once
						part = 1
					Case 1
						Log("Setting volume")
						SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,0x01,Volume,0xFD)) ' Set volume STREAM_SetVolume(Volume) Only called Once
						part = 2
					Case 2	
						Log("Setting stereoMode")
	 					SendRadio(Array As Byte(0xFE,0x01,0x09,0x01,0x00,0x01,0x01,0xFD)) ' Set mode to stereo STREAM_SetStereoMode(1) Only called once
						part = 3 
					Case 3		
						SendRadio(Array As Byte(0xFE,0x01,0x0D,0x01,0x00,0x00,0xFD)) ' Get Volume Stream_GetVolume()
					Case 4
						SendRadio(Array As Byte(0xFE,0x01,0x15,0x01,0x00,0x05,0x00,0x00,0x00,Frequenz,0x01,0xFD)) 'Get Ensemble program name STREAM_GetEnsembleName(Frequenz, 1) long name 
					Case 5		
	 					SendRadio(Array As Byte(0xFE,0x01,0x1A,0x01,0x00,0x05,0x00,0x00,0x00,Frequenz,0x01,0xFD)) 'Get The service name of DAB Program STREAM_GetServiceName(Frequenz, 1) long name
					Case 6	
						SendRadio(Array As Byte(0xFE,0x01,0x07,0x01,0x00,0x00,0xFD)) ' Get current DAB index STREAM_GetPlayIndex()													
					Case 7	
						SendRadio(Array As Byte(0xFE,0x01,0x13,0x01,0x00,0x00,0xFD)) ' Get the signal strength STREAM_GetSignalQuality()
					Case 8
						SendRadio(Array As Byte(0xFE,0x01,0x0E,0x01,0x00,0x04,0x00,0x00,0x00,Frequenz,0xFD)) 'Get the program genre STREAM_GetProgrameType(Frequenz)
					Case 9
						SendRadio(Array As Byte(0xFE,0x01,0x05,0x01,0x00,0x00,0xFD)) ' Get the playing status STREAM_GetPlayStatus()
					Case 10
						SendRadio(Array As Byte(0xFE,0x01,0x10,0x01,0x00,0x00,0xFD)) ' Get the program text STREAM_GetProgrameText()
					Case 11
						SendRadio(Array As Byte(0xFE,0x01,0x0B,0x01,0x00,0x00,0xFD)) ' Get the channel type of the current stream STREAM_GetStereo()
					Case 12
						SendRadio(Array As Byte(0xFE,0x01,0x12,0x01,0x00,0x00,0xFD)) ' Get the data rate of the current stream STREAM_GetDataRate()
						iIndex = 3
						Return
					End Select
				Else							
					Select iIndex
					Case 0
						Dim d1,d2,d3,d As String
						d =  Bit.ToHexString(Frequenz)
						d1 = Bit.ParseInt(d.SubString2(0,1), 16)
						d2 = Bit.ParseInt(d.SubString2(1,3), 16)
						d3 = Bit.ParseInt(d.SubString2(3,5), 16)
						SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x01,0x00,d1,d2,d3,0xFD))
					Case 1
						SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,0x01,Volume,0xFD))
					Case 2		
	 					SendRadio(Array As Byte(0xFE,0x01,0x09,0x01,0x00,0x01,0x01,0xFD))
					Case 3		
	 					SendRadio(Array As Byte(0xFE,0x01,0x0D,0x01,0x00,0x00,0xFD))
					Case 4	
						SendRadio(Array As Byte(0xFE,0x01,0x07,0x01,0x00,0x00,0xFD))
					Case 5	
						SendRadio(Array As Byte(0xFE,0x01,0x08,0x01,0x00,0x00,0xFD))
					Case 6			
	 					SendRadio(Array As Byte(0xFE,0x01,0x0E,0x01,0x00,0x04,0xFF,0xFF,0xFF,0xFF,0xFD)) 
					Case 7		
	 					SendRadio(Array As Byte(0xFE,0x01,0x0F,0x01,0x00,0x05,0xFF,0xFF,0xFF,0xFF,0x01,0xFD))   
					Case 8
						SendRadio(Array As Byte(0xFE,0x01,0x05,0x01,0x00,0x00,0xFD))
					Case 9
						SendRadio(Array As Byte(0xFE,0x01,0x10,0x01,0x00,0x00,0xFD))	
						iIndex = 3
						Return
					End Select
				End If
				If iIndex > 2 Then iIndex = iIndex + 1
				'labVolume.Text = Volume & " " & iIndex
				If iIndex > 12 Then iIndex = 3
			End If
		End If
	End If 
End Sub

Sub USBTimer_Tick
	Log("Trying to connect again")
	If Not (Connected) Then OpenRadio	
End Sub

#End Region

#Region'--------------------Astreams--------------------

Sub Astreams_NewData (buffer() As Byte)
	Dim itemp As Int
	Dim y As Int
	
   	For y = 0 To buffer.Length -1 				
		itemp = Bit.And(0xff, buffer(y))
		If itemp = 254 Then 
			Start = True
			xIndex = 0
		End If
		
		If Start Then 
			Ack(xIndex) = itemp
			xIndex = xIndex + 1
		End If
		
		If itemp = 253 Then 
			Start = False
			Evaluate(xIndex - 1)
		End If
		
   	Next
End Sub

Sub Astreams_Error
	astreams.Close
	CloseService
End Sub

Sub Astreams_Terminated
	CloseService
End Sub

Sub TryAgain_tick
	If MuteResponse = "0" Then
		MuteResponse = "Null"
		ExitApp
	End If
	
	TryAgain.Enabled = False

End Sub

Sub RunCloseProcesses
	astreams.Close

	CloseRadio

	AudioFocusManager.abandonAudioFocus

	Log("Abandoning media focus")
	
	session.RunMethod("release",Null)

	Broadcast.sendBroadcast("com.freshollie.radioapp.STOPPED")
	Service.StopForeground(1)
	RadioNotification.Cancel(1)
		
	
	ServiceStarted = False
	Connected = False
	ForceClosing = False
	
End Sub

Sub CloseService ' Close the background service but not the app
	If ServiceStarted And Connected Then
		SaveSettings
		RunCloseProcesses
	End If
End Sub

Sub ExitApp 'Close the whole app
	Log("Attempting To close app when ServiceStarted = " & ServiceStarted)
		
	If ServiceStarted And Connected Then

		If MuteResponse = "Null" Then
			SaveSettings
			
			
			If ForceClosing = False Then
				Log("Attempting to mute")
			
				MuteResponse = "0"
			
				SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,0x01,0x00,0xFD)) 'Set volume 0
				
				If Not(TryAgain.IsInitialized) Then TryAgain.Initialize("TryAgain",500)
				TryAgain.Enabled = True
			End If
		End If
		
		If MuteResponse = "1" Or ForceClosing = True Then 'CLose the radio if power has been disconnected
				
			RunCloseProcesses

			
			If IsPaused(Main) = False Or CloseAfterStop = True Then
				ExitApplication
			End If
		End If
	Else
		Log("Attempt aborted")
		If IsPaused(Main) = False Then
			ExitApplication
		End If
		
	End If
	
End Sub
#End Region

#Region'-------------ButtonInputs-------------
Sub ToggleMute
	If Mute Then
		Mute = False
		UnmuteAudio
	Else	
		Mute = True
		MuteAudio
	End If
End Sub

Sub MuteAudio
	Log("Muting")
	LastVolume = Volume
	SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,0x01,0x00,0xFD))
End Sub

Sub UnmuteAudio
	Log("Unmuting")
	Volume = LastVolume
	SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,0x01,LastVolume,0xFD))
End Sub

Sub EnterFrequency(FrequencyText As String)	
	Dim F As Int
	
	If isDAB And DAB Then
		F = FrequencyText
		If Not(DABSearch) Then SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,F,0xFD))
		EnterClickedReturnValue = True
	Else	
		F = FrequencyText * 1000
		If F > 87400 And F < 108100 Then
			Dim d1,d2,d3,D As String
			D =  Bit.ToHexString(F)
			d1 = Bit.ParseInt(D.SubString2(0,1), 16)
			d2 = Bit.ParseInt(D.SubString2(1,3), 16)
			d3 = Bit.ParseInt(D.SubString2(3,5), 16)
			SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x01,0x00,d1,d2,d3,0xFD))
			EnterClickedReturnValue = True 
		Else
			EnterClickedReturnValue = False 
		End If
	End If
	
	
End Sub

Sub SwitchToFM	
	Dim d1,d2,d3,D As String
	Select Ebene
	Case 0	
		D =  Bit.ToHexString(Frq(0))	
	Case 1	
		D =  Bit.ToHexString(Frq(7))	
	Case 2
		D =  Bit.ToHexString(Frq(14))	
	End Select	
	d1 = Bit.ParseInt(D.SubString2(0,1), 16)
	d2 = Bit.ParseInt(D.SubString2(1,3), 16)
	d3 = Bit.ParseInt(D.SubString2(3,5), 16)
	SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x01,0x00,d1,d2,d3,0xFD))
	isDAB = False
End Sub

Sub ChangeFMLevel	
	If Not(isDAB) Then 
		Select Ebene
		Case 0	
			Frq(0) = Frequenz	
		Case 1	
			Frq(7) = Frequenz
		Case 2
			Frq(14) = Frequenz
		End Select	
	End If
End Sub

Sub SwitchToDAB
	If Not(DABSearch) And DAB Then
		Select Ebene
		Case 0	
			SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,DFrq(0),0xFD))
		Case 1	
			SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,DFrq(7),0xFD))	
		Case 2
			SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,DFrq(14),0xFD))
		End Select		
	End If
End Sub

Sub ChangeDABLevel
	Select Ebene
		Case 0	
			DFrq(0) = Frequenz	
		Case 1	
			DFrq(7) = Frequenz
		Case 2
			DFrq(14) = Frequenz
	End Select
End Sub

Sub SelectChannel(ChannelNum As Int)
	Dim d1,d2,d3,D As String
	Dim I As Int
	
	Select Ebene
	Case 0	
		I = 1
	Case 1	
		I = 8
	Case 2
		I = 15
	End Select
	
	I = I+ChannelNum-1
	If isDAB And DAB Then
		If Not(DABSearch) Then 
			SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,DFrq(I),0xFD))
			CallSub2(Main, "SelectedDABChannel", DFrq(I))	
		End If 
	
	Else	
		D =  Bit.ToHexString(Frq(I))
		d1 = Bit.ParseInt(D.SubString2(0,1), 16)
		d2 = Bit.ParseInt(D.SubString2(1,3), 16)
		d3 = Bit.ParseInt(D.SubString2(3,5), 16)
		SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x01,0x00,d1,d2,d3,0xFD))
	End If
End Sub

Sub SetChannel(ChannelNum As Int)
	Dim I As Int
	
	Select Ebene
	Case 0	
		I = 1	
	Case 1	
		I = 8
	Case 2
		I = 15
	End Select
	
	I = I+ChannelNum-1
	If isDAB And DAB Then
		DFrq(I) = Frequenz
		CallSub2(Main, "SelectedDABChannel", I)

	Else
		Frq(I) = Frequenz
	End If
End Sub

Sub RadioChannelUp
	If isDAB And DAB Then
		SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,Frequenz + 1,0xFD))
		CallSub2(Main, "SelectedDABChannel", Frequenz + 1)		
	Else
		SendRadio(Array As Byte(0xFE,0x01,0x02,0x01,0x00,0x01,0x01,0xFD))
	End If
End Sub

Sub RadioChannelDown
	If isDAB And DAB Then
		If Frequenz > 0 Then 
			SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,Frequenz - 1,0xFD))
			CallSub2(Main, "SelectedDABChannel", Frequenz - 1)	 
		End If			
	Else
		SendRadio(Array As Byte(0xFE,0x01,0x02,0x01,0x00,0x01,0x00,0xFD))
	End If
End Sub

Sub FMHigher	
	Dim F As Int
	F = Frequenz
	If F > 87400 And F < 108100 Then
		If F > 107900 Then F = 87400
		Dim d1,d2,d3,D As String
		D =  Bit.ToHexString(F + 50)
		d1 = Bit.ParseInt(D.SubString2(0,1), 16)
		d2 = Bit.ParseInt(D.SubString2(1,3), 16)
		d3 = Bit.ParseInt(D.SubString2(3,5), 16)
		SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x01,0x00,d1,d2,d3,0xFD))
	End If
End Sub

Sub FMLower	
	Dim F As Int
	F = Frequenz
	If F > 87400 And F < 108100 Then
		If F < 87600 Then F = 108100
		Dim d1,d2,d3,D As String
		D =  Bit.ToHexString(F - 50)
		d1 = Bit.ParseInt(D.SubString2(0,1), 16)
		d2 = Bit.ParseInt(D.SubString2(1,3), 16)
		d3 = Bit.ParseInt(D.SubString2(3,5), 16)
		SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x01,0x00,d1,d2,d3,0xFD))
	End If
End Sub

Sub VolumeUp	
	If Volume < 16 Then
		Volume = Volume + 1
		SetVolume(Volume)
	End If
End Sub

Sub VolumeDown	
	If Volume > 0 Then
		Volume = Volume - 1
		SetVolume(Volume)
	End If
End Sub

Sub SetVolume(Volume1 As Int)
	Log("Setting Volume " & Volume1)
	Volume = Volume1
	SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,0x01,Volume1,0xFD))
	Mute = False
End Sub
#End Region

#Region'--------------------Labels----------------------
Sub IncrementLevel
	Ebene = Ebene + 1
	If Ebene > 2 Then Ebene = 0
End Sub

Sub SelectDABItem (Position As Int)   
	SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,Position,0xFD))
	End Sub
#End Region

Sub StartChannelSearch(ShouldClean As Boolean)
 	If isDAB And DAB Then
		If Not(DABSearch) Then
			If ShouldClean Then
				MyTimer.Enabled = False
				SendRadio(Array As Byte(0xFE,0x00,0x01,0x01,0x00,0x01,0x01,0xFD))
				ClearDatabase = True
				SysReady.Enabled = True
			Else
				SendRadio(Array As Byte(0xFE,0x01,0x03,0x01,0x00,0x02,0x00,0x47,0xFD))
				DABSearch = True
			End If
 		End If
	End If
End Sub

#Region'----------IntentReceiver--------

Sub BroadcastReceiver_OnReceive(Action As String,i As Object)
	Dim Intent1 As Intent = i
	Log(Action)

	If Intent1.HasExtra("device") Then
		If USB.UsbPresent(Dev) = USB.USB_NONE Then
			CloseService
		End If
	End If
	
	If Action = "com.freshollie.radioapp.intent.close" Then
		ExitApp
	
	Else If Action = "com.freshollie.radioapp.intent.mute" Then
		MuteAudio
		
	Else If Action = "com.freshollie.radioapp.intent.unmute" Then
		UnmuteAudio
		
 End If
End Sub

Sub AudioFocusManager_onFocusLost
	Log("Focus Lost, closing")
	CloseAfterStop = True
	ExitApp
End Sub

Sub AudioFocusManager_onTransient
	Log("Muting, on transient")
	MuteAudio
End Sub

Sub AudioFocusManager_onTransientCanDuck
	Log("Lowering volume, on transient can duck")
	LastVolume = Volume
	Ducked = True
	SetVolume(DuckVolume)
End Sub

Sub AudioFocusManager_onGain
	Log("Back to normal, gained focus")
	Ducked = False
	UnmuteAudio
End Sub

#End Region

#Region'-----------------Service-----------------
Sub Service_Create
	RadioNotification.Initialize
	Broadcast.Initialize("BroadcastReceiver")
	'Broadcast.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
	Broadcast.addAction("com.freshollie.radioapp.intent.close")
	Broadcast.addAction("com.freshollie.radioapp.intent.mute")
	Broadcast.addAction("com.freshollie.radioapp.intent.unmute")
	Broadcast.registerReceiver("")
End Sub

Sub SetUpNotfication
	RadioNotification.AddAction2("ic_skip_previous_black_24dp", "","Previous Channel", Me)
	RadioNotification.AddAction2("ic_stop_black_24dp", "","Stop", Me)
	RadioNotification.AddAction2("ic_skip_next_black_24dp", "","Next Channel", Me)
	RadioNotification.DefaultLight = True
	RadioNotification.DefaultVibrate = False
	RadioNotification.DefaultSound = False
	RadioNotification.setActivity(Main)
	RadioNotification.SmallIcon = "ic_radio_black_24dp"
	RadioNotification.ContentTitle = "Radio Running"
	RadioNotification.AutoCancel = False
	
	Service.StartForeground(1, RadioNotification.GetNotification)
	
End Sub

Sub Service_Start (StartingIntent As Intent)
	Dim intentExtra As String
	
	If MuteResponse = "0" And Connected = True Then
		RunCloseProcesses
	End If
	
	If StartingIntent.HasExtra("Notification_Action_Tag") Then
		intentExtra = StartingIntent.GetExtra("Notification_Action_Tag")
		Log("Notification intent to " & intentExtra)
		If intentExtra = "Stop" Then
			ExitApp
		Else if intentExtra = "Previous Channel" Then
			RadioChannelDown
		Else if intentExtra = "Next Channel" Then
			RadioChannelUp
		End If
		
	Else if Connected = False Then
		ServiceStarted = True
		
		If Not(File.Exists(MyPath, "")) Then File.MakeDir(File.DirRootExternal, "dabmonkey")
		
		MyPath = File.DirRootExternal & "/dabmonkey"
		
		Connected = False
		
		OpenRadio
	
	End If

End Sub

Sub Service_Destroy
	Service.StopForeground(1)
	Broadcast.unregisterReceiver
End Sub

Sub StartMediaKeys()
	Dim context As JavaObject
	context.InitializeContext
	session.InitializeNewInstance("android.media.session.MediaSession", Array(context, "tag"))

	Dim cb As JavaObject
	cb.InitializeNewInstance(Application.PackageName & ".radioservice.MyCallback", Null)
	session.RunMethod("setCallback", Array(cb))

	session.RunMethod("setFlags",Array(3))
	session.RunMethod("setActive", Array(True))
	Log("Accquired media session")
End Sub

'Return true to allow the OS default exceptions handler to handle the uncaught exception.
Sub Application_Error (Error As Exception, StackTrace As String) As Boolean
    Return True
End Sub

Sub Media_OnCommand(Command As String)
   'Log(Command)
End Sub

Sub Media_OnButton(KeyCode As Int) As Boolean
	Select(KeyCode)
	
	Case(KeyCodes.KEYCODE_MEDIA_NEXT)
		RadioChannelUp
	
	Case(KeyCodes.KEYCODE_MEDIA_PREVIOUS)
		RadioChannelDown
		
	End Select
	
    Return True
End Sub
	
#End Region

#Region--------------------SettingsLoad------------------
Sub LoadSettings
	#Region'--------------------Settings--------------------	
	If Not(lstDAB.IsInitialized) Then lstDAB.Initialize
	INI.Name(MyPath & "/config.ini")
	Volume = INI.ReadInt("Last", "Volume", DefaultVolume)
	Frq(0) = INI.ReadInt("Last", "Frq0", 88000)
	Frq(1) = INI.ReadInt("Last", "Frq1", 88000)
	Frq(2) = INI.ReadInt("Last", "Frq2", 88000)
	Frq(3) = INI.ReadInt("Last", "Frq3", 88000)
	Frq(4) = INI.ReadInt("Last", "Frq4", 88000)
	Frq(5) = INI.ReadInt("Last", "Frq5", 88000)
	Frq(6) = INI.ReadInt("Last", "Frq6", 88000)
	Frq(7) = INI.ReadInt("Last", "Frq7", 88000)
	Frq(8) = INI.ReadInt("Last", "Frq8", 88000)
	Frq(9) = INI.ReadInt("Last", "Frq9", 88000)
	Frq(10) = INI.ReadInt("Last", "Frq10", 88000)
	Frq(11) = INI.ReadInt("Last", "Frq11", 88000)
	Frq(12) = INI.ReadInt("Last", "Frq12", 88000)
	Frq(13) = INI.ReadInt("Last", "Frq13", 88000)
	Frq(14) = INI.ReadInt("Last", "Frq14", 88000)
	Frq(15) = INI.ReadInt("Last", "Frq15", 88000)
	Frq(16) = INI.ReadInt("Last", "Frq16", 88000)
	Frq(17) = INI.ReadInt("Last", "Frq17", 88000)
	Frq(18) = INI.ReadInt("Last", "Frq18", 88000)
	Frq(19) = INI.ReadInt("Last", "Frq19", 88000)
	Frq(20) = INI.ReadInt("Last", "Frq20", 88000)
	DFrq(0) = INI.ReadInt("Last", "DAB0", 0)
	DFrq(1) = INI.ReadInt("Last", "DAB1", 0)
	DFrq(2) = INI.ReadInt("Last", "DAB2", 0)
	DFrq(3) = INI.ReadInt("Last", "DAB3", 0)
	DFrq(4) = INI.ReadInt("Last", "DAB4", 0)
	DFrq(5) = INI.ReadInt("Last", "DAB5", 0)
	DFrq(6) = INI.ReadInt("Last", "DAB6", 0)
	DFrq(7) = INI.ReadInt("Last", "DAB7", 0)
	DFrq(8) = INI.ReadInt("Last", "DAB8", 0)
	DFrq(9) = INI.ReadInt("Last", "DAB9", 0)
	DFrq(10) = INI.ReadInt("Last", "DAB10", 0)
	DFrq(11) = INI.ReadInt("Last", "DAB11", 0)
	DFrq(12) = INI.ReadInt("Last", "DAB12", 0)
	DFrq(13) = INI.ReadInt("Last", "DAB13", 0)
	DFrq(14) = INI.ReadInt("Last", "DAB14", 0)
	DFrq(15) = INI.ReadInt("Last", "DAB15", 0)
	DFrq(16) = INI.ReadInt("Last", "DAB16", 0)
	DFrq(17) = INI.ReadInt("Last", "DAB17", 0)
	DFrq(18) = INI.ReadInt("Last", "DAB18", 0)
	DFrq(19) = INI.ReadInt("Last", "DAB19", 0)
	DFrq(20) = INI.ReadInt("Last", "DAB20", 0)
	Frequenz = INI.ReadInt("Last", "Frequenz", 88000)	
	Ebene = INI.ReadInt("Last", "Ebene", 0)
	
	Dim labLevelText As String
	Dim labProgram2Visible, isDABChecked As Boolean
	
	Dim ArgList As List
	
	ArgList.Initialize()
	
	labLevelText = "L " & Ebene
	If File.Exists(MyPath & "/","DAB.dat") Then 
		Dim DatesFile As RandomAccessFile
		DatesFile.Initialize(MyPath,"/DAB.dat",False)
		lstDAB = DatesFile.ReadObject(0)
		DatesFile.Close
	End If
	#End Region	
	If (Frequenz < 87500) And DAB Then
		isDAB = True
		labProgram2Visible = True
		isDABChecked = True	
	Else
		If Frequenz < 87500 Then Frequenz = 87500
		isDAB = False
		labProgram2Visible = False
		isDABChecked = False
	End If
	
	ArgList.Add(labLevelText)
	ArgList.Add(isDABChecked)
	ArgList.Add(labProgram2Visible)
	
	CallSub2(Main, "SetTextValuesFromSettings", ArgList)
		
		
End Sub
	
Sub SaveSettings
	INI.Name(MyPath & "/config.ini")
	INI.WriteInt("Last", "Frequenz", Frequenz)
	INI.WriteInt("Last", "Volume", Volume)
	INI.WriteInt("Last", "Frq0", Frq(0))
	INI.WriteInt("Last", "Frq1", Frq(1))
	INI.WriteInt("Last", "Frq2", Frq(2))
	INI.WriteInt("Last", "Frq3", Frq(3))
	INI.WriteInt("Last", "Frq4", Frq(4))
	INI.WriteInt("Last", "Frq5", Frq(5))
	INI.WriteInt("Last", "Frq6", Frq(6))
	INI.WriteInt("Last", "Frq7", Frq(7))
	INI.WriteInt("Last", "Frq8", Frq(8))
	INI.WriteInt("Last", "Frq9", Frq(9))
	INI.WriteInt("Last", "Frq10", Frq(10))
	INI.WriteInt("Last", "Frq11", Frq(11))
	INI.WriteInt("Last", "Frq12", Frq(12))
	INI.WriteInt("Last", "Frq13", Frq(13))
	INI.WriteInt("Last", "Frq14", Frq(14))
	INI.WriteInt("Last", "Frq15", Frq(15))
	INI.WriteInt("Last", "Frq16", Frq(16))
	INI.WriteInt("Last", "Frq17", Frq(17))
	INI.WriteInt("Last", "Frq18", Frq(18))
	INI.WriteInt("Last", "Frq19", Frq(19))
	INI.WriteInt("Last", "Frq20", Frq(20))
	INI.WriteInt("Last", "DAB0", DFrq(0))
	INI.WriteInt("Last", "DAB1", DFrq(1))
	INI.WriteInt("Last", "DAB2", DFrq(2))
	INI.WriteInt("Last", "DAB3", DFrq(3))
	INI.WriteInt("Last", "DAB4", DFrq(4))
	INI.WriteInt("Last", "DAB5", DFrq(5))
	INI.WriteInt("Last", "DAB6", DFrq(6))
	INI.WriteInt("Last", "DAB7", DFrq(7))
	INI.WriteInt("Last", "DAB8", DFrq(8))
	INI.WriteInt("Last", "DAB9", DFrq(9))
	INI.WriteInt("Last", "DAB10", DFrq(10))
	INI.WriteInt("Last", "DAB11", DFrq(11))
	INI.WriteInt("Last", "DAB12", DFrq(12))
	INI.WriteInt("Last", "DAB13", DFrq(13))
	INI.WriteInt("Last", "DAB14", DFrq(14))
	INI.WriteInt("Last", "DAB15", DFrq(15))
	INI.WriteInt("Last", "DAB16", DFrq(16))
	INI.WriteInt("Last", "DAB17", DFrq(17))
	INI.WriteInt("Last", "DAB18", DFrq(18))
	INI.WriteInt("Last", "DAB19", DFrq(19))
	INI.WriteInt("Last", "DAB20", DFrq(20))
	INI.WriteInt("Last", "Ebene", Ebene)
	INI.Store  	
	Dim DatesFile As RandomAccessFile
 	DatesFile.Initialize(MyPath,"/DAB.dat",False)
	DatesFile.WriteObject(lstDAB,True,0)
	DatesFile.Close
	End Sub
#End Region

#if java
import android.media.session.MediaSession.*;
import android.view.KeyEvent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.content.Intent;
import anywheresoftware.b4a.objects.IntentWrapper;

public static class MyCallback extends Callback {
	public MyCallback() {
	}
   
	public void onCommand(String command, Bundle args, ResultReceiver cb)
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

#end if
