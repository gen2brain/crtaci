package main

import (
	"encoding/json"
	"fmt"
	"log"
	"path/filepath"
	"strings"

	"github.com/therecipe/qt/core"
	"github.com/therecipe/qt/gui"
	"github.com/therecipe/qt/network"
	"github.com/therecipe/qt/widgets"

	"github.com/gen2brain/crtaci/backend/crtaci"
)

// Window type
type Window struct {
	*Object
	*widgets.QWidget

	Movie          *gui.QMovie
	CharactersList *CharactersList
	CartoonsList   *CartoonsList
	ButtonAbout    *widgets.QPushButton
	LabelLoading   *widgets.QLabel
	RotateLeft     *widgets.QCheckBox
	RotateRight    *widgets.QCheckBox
	Manager        *network.QNetworkAccessManager
}

// NewWindow returns new window
func NewWindow() *Window {
	w := widgets.NewQWidget(nil, 0)
	w.SetGeometry2(0, 0, 835, 675)
	w.SetWindowTitle("Crtaći")
	w.SetWindowIcon(gui.NewQIcon5(":/qml/images/crtaci.png"))

	window := &Window{NewObject(w), w, nil, nil, nil, nil, nil, nil, nil, nil}
	window.Manager = network.NewQNetworkAccessManager(w)

	cache := network.NewQNetworkDiskCache(w)
	cache.SetCacheDirectory(filepath.Join(cacheDir(), "images"))
	window.Manager.SetCache(cache)

	return window
}

// Center centers window
func (w *Window) Center() {
	size := w.Size()
	desktop := widgets.QApplication_Desktop()
	width, height := size.Width(), size.Height()
	dwidth, dheight := desktop.Width(), desktop.Height()
	cw, ch := (dwidth/2)-(width/2), (dheight/2)-(height/2)
	w.Move2(cw, ch)
}

// AddWidgets adds widgets to window
func (w *Window) AddWidgets() {
	w.Movie = gui.NewQMovie3(":/qml/images/loading.gif", core.NewQByteArray2("GIF", 3), w.QWidget)
	w.Movie.Start()

	w.CharactersList = NewCharactersList(w.QWidget)
	w.CartoonsList = NewCartoonsList(w.QWidget)

	w.ButtonAbout = widgets.NewQPushButton(w.QWidget)
	w.ButtonAbout.SetIcon(gui.NewQIcon5(":/qml/images/info.png"))

	w.RotateLeft = widgets.NewQCheckBox(w.QWidget)
	w.RotateLeft.SetToolTip("Rotate Left")
	w.RotateRight = widgets.NewQCheckBox(w.QWidget)
	w.RotateRight.SetToolTip("Rotate Right")

	labelLeft := widgets.NewQLabel(w.QWidget, 0)
	labelLeft.SetPixmap(gui.NewQPixmap5(":/qml/images/rotate-left.png", "PNG", core.Qt__AutoColor))
	labelRight := widgets.NewQLabel(w.QWidget, 0)
	labelRight.SetPixmap(gui.NewQPixmap5(":/qml/images/rotate-right.png", "PNG", core.Qt__AutoColor))

	w.LabelLoading = widgets.NewQLabel(w.QWidget, 0)
	w.LabelLoading.SetVisible(false)
	w.LabelLoading.SetMovie(w.Movie)

	layoutV := widgets.NewQVBoxLayout()
	layoutH := widgets.NewQHBoxLayout()

	layoutH.AddWidget(w.ButtonAbout, 0, 0)
	layoutH.AddWidget(w.LabelLoading, 0, 0)
	layoutH.AddSpacerItem(widgets.NewQSpacerItem(20, 0, widgets.QSizePolicy__MinimumExpanding, widgets.QSizePolicy__Preferred))
	layoutH.AddWidget(labelLeft, 0, 0)
	layoutH.AddWidget(w.RotateLeft, 0, 0)
	layoutH.AddWidget(labelRight, 0, 0)
	layoutH.AddWidget(w.RotateRight, 0, 0)

	frame := widgets.NewQFrame(w.QWidget, 0)
	frame.SetLayout(layoutH)

	layoutV.SetSpacing(0)
	layoutV.SetContentsMargins(0, 0, 0, 0)

	layoutV.AddWidget(w.CharactersList, 1, 0)
	layoutV.AddWidget(frame, 0, 0)

	widget := widgets.NewQWidget(w.QWidget, 0)
	widget.SetLayout(layoutV)

	splitter := widgets.NewQSplitter2(core.Qt__Horizontal, w.QWidget)
	splitter.AddWidget(widget)
	splitter.AddWidget(w.CartoonsList)
	splitter.SetStretchFactor(0, 0)
	splitter.SetStretchFactor(1, 1)

	layout := widgets.NewQVBoxLayout()
	layout.AddWidget(splitter, 0, 0)

	w.SetLayout(layout)
}

// ConnectSignals connects signals
func (w *Window) ConnectSignals() {
	w.CharactersList.ConnectCurrentItemChanged(func(item *widgets.QListWidgetItem, previous *widgets.QListWidgetItem) {
		w.LabelLoading.SetVisible(true)

		data := item.Data(int(core.Qt__UserRole)).ToString()

		var c crtaci.Character
		err := json.Unmarshal([]byte(data), &c)
		if err != nil {
			log.Printf("ERROR: Unmarshal: %s\n", err.Error())
			return
		}

		go func() {
			crtaci.Cancel()
			data, err := crtaci.Search(c.Name)
			if err != nil {
				log.Printf("ERROR: Search: %s\n", err.Error())
				w.CartoonsList.Finished("")
				return
			}
			w.CartoonsList.Finished(data)
		}()
	})

	w.CartoonsList.ConnectFinished(func(data string) {
		w.LabelLoading.SetVisible(false)

		if data != "" && data != "empty" {
			w.CartoonsList.Init(w.Manager, data)
			w.CartoonsList.ScrollToTop()
		}
	})

	w.CartoonsList.ConnectItemActivated(func(item *widgets.QListWidgetItem) {
		w.LabelLoading.SetVisible(true)

		data := item.Data(int(core.Qt__UserRole)).ToString()
		var c crtaci.Cartoon
		err := json.Unmarshal([]byte(data), &c)
		if err != nil {
			log.Printf("ERROR: Unmarshal: %s\n", err.Error())
			return
		}

		player := NewPlayer(w.QWidget)
		player.Init()

		if w.RotateRight.IsChecked() {
			player.Rotate(90)
		} else if w.RotateLeft.IsChecked() {
			player.Rotate(270)
		}

		player.ConnectFileLoaded(func() {
			w.LabelLoading.SetVisible(false)
		})

		player.ConnectShutdown(func() {
			w.LabelLoading.SetVisible(false)
		})

		go func() {
			uri, err := crtaci.Extract(c.Service, c.Id)
			if err != nil {
				log.Printf("ERROR: Search: %s\n", err.Error())
				return
			}

			if uri != "" && uri != "empty" {
				player.Play(uri, fmt.Sprintf("%s - %s", strings.Title(c.Character), strings.Title(c.FormattedTitle)))
			}
		}()
	})

	w.RotateLeft.ConnectClicked(func(checked bool) {
		if checked {
			w.RotateRight.SetChecked(false)
		}
	})

	w.RotateRight.ConnectClicked(func(checked bool) {
		if checked {
			w.RotateLeft.SetChecked(false)
		}
	})

	w.ButtonAbout.ConnectClicked(func(bool) {
		NewAbout(w.QWidget_PTR()).Show()
	})

	w.ConnectFinished(func(data string) {
		if data == "true" {
			reply := widgets.QMessageBox_Question(w.QWidget_PTR(), "New Version Available", "Do you want to download new version?", widgets.QMessageBox__Yes|widgets.QMessageBox__No, widgets.QMessageBox__Yes)
			if reply == widgets.QMessageBox__Yes {
				gui.QDesktopServices_OpenUrl(core.NewQUrl3("https://crtaci.rs", core.QUrl__TolerantMode))
			}
		}
	})
}

// Init initialize window
func (w *Window) Init() {
	w.CharactersList.SetCurrentRow(0)

	go func() {
		ok := crtaci.UpdateExists()
		if ok {
			w.Finished("true")
		} else {
			w.Finished("false")
		}
	}()
}
