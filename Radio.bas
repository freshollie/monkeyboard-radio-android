Type=Class
Version=5.02
ModulesStructureVersion=1
B4A=true
@EndOfDesignText@
'Class module
Sub Class_Globals
	Dim USB As UsbSerial 
	Dim UsbPid As Int = 0xa 
	Dim UsbVid As Int = 0x4D8
	Dim astreams As AsyncStreams
	Dim MyTimer, SysReady, USBTimer As Timer
	Dim Broadcast As BroadCastReceiver

	Dim Start, Mute, DABSearch, isDAB, DAB, Connected, FillList, ClearDatabase, NameResponded As Boolean 
	Dim Volume, Strength As Byte
	Dim Ack(1024), Frq(21), DFrq(21), Frequenz, iIndex, xIndex, Dev, Ebene, AllDAB, iStep, iLoop As Int
	Dim lstDAB As List
	Dim RadioNotification As NotificationBuilder
	Dim AudioFocusManager As AudioFocus
	Dim ProgramText, ProgramType, ProgramName, ProgramName2, Esamble, Status As String
	Dim labEventText,labFreqText,labStrengthText,labVolumeText,labProgramText,labProgramTypeText,labProgram2Text,ProgramNameText,StereoModeText,DataRateText As String
	Dim pbStrengthProgress As Double
	Dim MyPath As String
	Dim PreviousNotificationText As String
	Dim PreviousNotificationText2 As String
	Dim ServiceStarted As Boolean
	Dim Mediakey As MediaController
	Dim DuckVolume As Int
	Private session As JavaObject
	
End Sub

'Initializes the object. You can add parameters to this method if needed.
Public Sub Initialize
	Open()
End Sub

Public Sub Open

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
						
						Connected = True
						
						Wait(1)
						
	   					MyTimer.Enabled = True
						Return 
					End If
				End If
				
				Exit
			End If
		Next
	End If
	ExitApp
End Sub

Public Sub Close
	If Dev <> USB.USB_NONE Then USB.Close
End Sub

Public Sub Send(buffer() As Byte)
	If Connected Then astreams.Write(buffer)
End Sub

Private Sub Evaluate(index As Int)
	' Evaulate does looks at the byte string that was recived and
	' determines what the response is related to
	
	Dim j As Int
	Dim ArgumentsList As List
	
	Select Ack(1)
	Case 0x01 ' If its an acknolodged command
	
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
			
		Case 0x07
			If Ack(6) = 0 Then 		
				Frequenz = Bit.ParseInt(Bit.ToHexString(Ack(7)) &  Bit.ToHexString(Ack(8)) &  Bit.ToHexString(Ack(9)), 16)
				If isDAB And DAB Then
					labFreqText = Frequenz
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
			labEventText = AllDAB & " please wait !"
			lstDAB.Clear
			NameResponded = True
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
				iStep = lstDAB.Size+1
				lstDAB.Add(ProgramName2)
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
	
	CallSub2(Main,"SetEvaluatedData",ArgumentsList)
	
	
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
					SendRadio(Array As Byte(0xFE,0x01,0x1A,0x01,0x00,0x05,0x00,0x00,0x00,iStep,0x01,0xFD)) ' Get the service name of the dab program STREAM_GetServiceName(Frequenz, iStep) long name
				
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
						SendRadio(Array As Byte(0xFE,0x01,0x00,0x01,0x00,0x05,0x00,0x00,0x00,0x00,Frequenz,0xFD)) ' Play DAB Steam_PLAY(0, Frequenz) Only Called once
					Case 1
						SendRadio(Array As Byte(0xFE,0x01,0x0C,0x01,0x00,0x01,Volume,0xFD)) ' Set volume STREAM_SetVolume(Volume) Only called Once
					Case 2		
	 					SendRadio(Array As Byte(0xFE,0x01,0x09,0x01,0x00,0x01,0x01,0xFD)) ' Set mode to stereo STREAM_SetStereoMode(1) Only called once
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
						iIndex = 2
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
						iIndex = 2
					End Select
				End If
				iIndex = iIndex + 1
				'labVolume.Text = Volume & " " & iIndex
				If iIndex > 12 Then iIndex = 2
			End If
		End If
	End If 
End Sub

Public Sub StartChannelSearch(ShouldClean As Boolean)
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


