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

End Sub


Sub Wait(Sekunden As Int)
   Dim Ti As Long
   Ti = DateTime.Now + (Sekunden * 1000)
   Do While DateTime.Now < Ti
   DoEvents
   Loop
End Sub