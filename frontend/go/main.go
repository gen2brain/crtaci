package main

//go:generate goversioninfo -icon=dist/windows/crtaci.ico -o resource_windows.syso

import (
	"os"

	"github.com/therecipe/qt/widgets"
)

func main() {
	widgets.NewQApplication(len(os.Args), os.Args)

	setLocale(lcNumeric, "C")

	window := NewWindow()
	window.Center()
	window.AddWidgets()
	window.ConnectSignals()
	window.Show()
	window.Init()

	widgets.QApplication_Exec()
}
