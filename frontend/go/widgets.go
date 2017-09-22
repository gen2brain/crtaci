package main

import (
	"encoding/json"
	"fmt"
	"log"
	"strings"

	"github.com/therecipe/qt/core"
	"github.com/therecipe/qt/gui"
	"github.com/therecipe/qt/network"
	"github.com/therecipe/qt/widgets"

	"github.com/gen2brain/crtaci/backend/crtaci"
)

//go:generate qtmoc

// Object type
type Object struct {
	core.QObject

	_ func(value string) `signal:"finished"`
	_ func(value string) `signal:"finished2"`
}

// Object2 type
type Object2 struct {
	core.QObject

	_ func(value string) `signal:"valueChanged"`
}

// CharactersList type
type CharactersList struct {
	*widgets.QListWidget
}

// NewCharactersList returns new list
func NewCharactersList(w *widgets.QWidget) *CharactersList {
	listWidget := widgets.NewQListWidget(w)
	listWidget.SetUniformItemSizes(true)
	listWidget.SetIconSize(core.NewQSize2(48, 48))
	listWidget.SetResizeMode(widgets.QListView__Adjust)
	listWidget.SetDragEnabled(false)
	listWidget.SetAlternatingRowColors(true)
	listWidget.SetEditTriggers(widgets.QAbstractItemView__NoEditTriggers)
	listWidget.SetSpacing(2)
	listWidget.SetMinimumWidth(250)

	listWidget.SetStyleSheet(`
		QListWidget {
			alternate-background-color: #F0F8FF;
			background: white;
		}

		QListWidget::item {
			color: #27408B;
		}
	`)

	list := &CharactersList{listWidget}
	list.Init()

	return list
}

// Init initializes list
func (l *CharactersList) Init() {
	list, err := crtaci.List()
	if err != nil {
		log.Printf("Error: List: %s\n", err.Error())
		return
	}

	characters := make([]crtaci.Character, 0)
	err = json.Unmarshal([]byte(list), &characters)
	if err != nil {
		log.Printf("Error: Unmarshal: %s\n", err.Error())
		return
	}

	for idx, c := range characters {
		item := NewCharactersListItem(l, c)
		l.InsertItem(idx, item)
	}
}

// CharactersListItem type
type CharactersListItem struct {
	*widgets.QListWidgetItem
}

// NewCharactersListItem returns new item
func NewCharactersListItem(l *CharactersList, c crtaci.Character) *CharactersListItem {
	font := gui.NewQFont()
	font.SetFamily("Comic Sans MS")
	font.SetPixelSize(14)

	icon := c.Name
	if c.AltName != "" {
		icon = c.AltName
	}
	icon = strings.Replace(icon, " ", "_", -1)

	item := widgets.NewQListWidgetItem(l, 0)
	item.SetFont(font)
	item.SetText(strings.Title(c.Name))
	item.SetSizeHint(core.NewQSize2(48, 48))
	item.SetIcon(gui.NewQIcon5(fmt.Sprintf(":/qml/icons/%s.png", icon)))

	character, _ := json.Marshal(c)
	item.SetData(int(core.Qt__UserRole), core.NewQVariant14(string(character[:])))

	return &CharactersListItem{item}
}

// CartoonsList
type CartoonsList struct {
	*Object
	*widgets.QListWidget
}

// NewCartoonsList returns new cartoon list
func NewCartoonsList(w *widgets.QWidget) *CartoonsList {
	listWidget := widgets.NewQListWidget(w)
	listWidget.SetUniformItemSizes(true)
	listWidget.SetViewMode(widgets.QListView__IconMode)
	listWidget.SetIconSize(core.NewQSize2(240, 180))
	listWidget.SetResizeMode(widgets.QListView__Adjust)
	listWidget.SetSizePolicy2(widgets.QSizePolicy__Expanding, widgets.QSizePolicy__Expanding)
	listWidget.SetVerticalScrollMode(widgets.QAbstractItemView__ScrollPerPixel)
	listWidget.SetDragEnabled(false)

	listWidget.SetStyleSheet(`
		QListWidget {
			border: 0;
			background-color: #87CEEB;
		}

		QListWidget::item {
			border: 0;
			padding-top: 10px;
			color: #27408B;
			background-color: #87CEEB;
		}

		QListWidget::item:selected {
			color: white;
			font-weight: bold;
		}
	`)

	var filterObject = core.NewQObject(w)
	filterObject.ConnectEventFilter(func(watched *core.QObject, event *core.QEvent) bool {
		if event.Type() == core.QEvent__Wheel {
			wheel := gui.NewQWheelEventFromPointer(event.Pointer())
			delta := wheel.AngleDelta()
			if delta.IsNull() {
				return false
			}

			if delta.Y() > 0 {
				listWidget.VerticalScrollBar().SetValue(listWidget.VerticalScrollBar().Value() - 100)
			} else if delta.Y() < 0 {
				listWidget.VerticalScrollBar().SetValue(listWidget.VerticalScrollBar().Value() + 100)
			}
			return true
		}
		return false
	})

	listWidget.VerticalScrollBar().InstallEventFilter(filterObject)

	return &CartoonsList{NewObject(w), listWidget}
}

// Init initializes list
func (l *CartoonsList) Init(manager *network.QNetworkAccessManager, data string) {
	var cartoons []crtaci.Cartoon
	err := json.Unmarshal([]byte(data), &cartoons)
	if err != nil {
		log.Printf("Error: Unmarshal: %s\n", err.Error())
		return
	}

	if l.Count() > 0 {
		l.Clear()
	}

	for idx, c := range cartoons {
		var item = NewCartoonsListItem(l, c)
		l.InsertItem(idx, item)

		reply := manager.Get(network.NewQNetworkRequest(core.NewQUrl3(c.ThumbMedium, core.QUrl__TolerantMode)))
		reply.ConnectFinished(func() {
			defer reply.DeleteLater()

			if reply.IsReadable() && reply.Error() == network.QNetworkReply__NoError {
				data := reply.ReadAll()
				if data.ConstData() != "" {
					pixmap := gui.NewQPixmap()
					ok := pixmap.LoadFromData2(data, "JPG", core.Qt__AutoColor)
					if ok {
						item.SetIcon(gui.NewQIcon2(pixmap.Scaled2(240, 180, core.Qt__IgnoreAspectRatio, core.Qt__SmoothTransformation)))
					}
				}
			}
		})
	}
}

// CartoonsListItem type
type CartoonsListItem struct {
	*Object
	*widgets.QListWidgetItem
}

// NewCartoonsListItem returns new cartoons list item
func NewCartoonsListItem(l *CartoonsList, c crtaci.Cartoon) *CartoonsListItem {
	desc := ""
	if c.Season != -1 {
		desc += fmt.Sprintf("S%02d", c.Season)
	}
	if c.Episode != -1 {
		desc += fmt.Sprintf("E%02d", c.Episode)
	}
	if desc != "" {
		desc = " - " + desc
	}

	font := gui.NewQFont()
	font.SetFamily("Comic Sans MS")
	font.SetPixelSize(14)

	item := widgets.NewQListWidgetItem(l, 0)
	item.SetFont(font)
	item.SetText(fmt.Sprintf("%s%s (%s)", strings.Title(c.FormattedTitle), desc, c.DurationString))
	item.SetSizeHint(core.NewQSize2(260, 220))

	cartoon, _ := json.Marshal(c)
	item.SetData(int(core.Qt__UserRole), core.NewQVariant14(string(cartoon[:])))

	return &CartoonsListItem{NewObject(l), item}
}

// NewAbout returns new about dialog
func NewAbout(parent *widgets.QWidget) *widgets.QDialog {
	dialog := widgets.NewQDialog(parent, 0)
	dialog.SetWindowTitle("About")
	dialog.Resize2(450, 250)

	textBrowser := widgets.NewQTextBrowser(dialog)
	textBrowser.SetOpenExternalLinks(true)
	textBrowser.Append("<center>Crtaći " + crtaci.Version + "</center>")
	textBrowser.Append("<center><a href=\"https://crtaci.rs\">https://crtaci.rs</a></center>")
	textBrowser.Append("<center>Author: Milan Nikolić (gen2brain)</center>")
	textBrowser.Append("<br/><center><i>Za Nayu i Noama &#10084;</i></center>")
	textBrowser.Append("<br/><center>This program is released under the terms of the</center>")
	textBrowser.Append("<center><a href=\"http://www.gnu.org/licenses/gpl-3.0.txt\">GNU General Public License version 3</a></center><br/>")

	label := widgets.NewQLabel(dialog, 0)
	label.SetPixmap(gui.NewQPixmap5(":/qml/images/crtaci.png", "PNG", core.Qt__AutoColor))

	buttonBox := widgets.NewQDialogButtonBox3(widgets.QDialogButtonBox__Close|widgets.QDialogButtonBox__Help, dialog)
	buttonBox.ConnectRejected(func() { dialog.Close() })
	buttonBox.ConnectHelpRequested(func() { NewHelp(dialog.QWidget_PTR()).Show() })

	hlayout := widgets.NewQHBoxLayout()
	hlayout.AddWidget(label, 0, 0)
	hlayout.AddWidget(textBrowser, 0, 0)

	vlayout := widgets.NewQVBoxLayout()
	vlayout.AddLayout(hlayout, 0)
	vlayout.AddWidget(buttonBox, 0, 0)

	dialog.SetLayout(vlayout)

	return dialog
}

// NewAbout returns new help dialog
func NewHelp(parent *widgets.QWidget) *widgets.QDialog {
	dialog := widgets.NewQDialog(parent, 0)
	dialog.SetWindowTitle("Shortcuts (mpv)")
	dialog.Resize2(400, 450)

	font := gui.NewQFont()
	font.SetFamily("Monospace")
	font.SetFixedPitch(true)
	font.SetPointSize(10)

	textBrowser := widgets.NewQTextBrowser(dialog)
	textBrowser.SetFont(font)

	textBrowser.Append("<ul type=\"none\"><li><b>p</b>\t\tPause/playback mode</li>")
	textBrowser.Append("<li><b>f</b>\t\tToggle fullscreen</li>")
	textBrowser.Append("<li><b>m</b>\t\tMute/unmute audio</li>")
	textBrowser.Append("<li><b>A</b>\t\tCycle aspect ratio</li>")
	textBrowser.Append("<br/>")
	textBrowser.Append("<li><b>ctrl++</b>\t\tIncrease audio delay</li>")
	textBrowser.Append("<li><b>ctrl+-</b>\t\tDecrease audio delay</li>")
	textBrowser.Append("<br/>")
	textBrowser.Append("<li><b>Right/Left</b>\t\tSeek 5 seconds</li>")
	textBrowser.Append("<li><b>Up/Down</b>\t\tSeek 60 seconds</li>")
	textBrowser.Append("<br/>")
	textBrowser.Append("<li><b>1/2</b>\t\tDecrease/increase contrast</li>")
	textBrowser.Append("<li><b>3/4</b>\t\tDecrease/increase brightness</li>")
	textBrowser.Append("<li><b>5/6</b>\t\tDecrease/increase gamma</li>")
	textBrowser.Append("<li><b>7/8</b>\t\tDecrease/increase saturation</li>")
	textBrowser.Append("<li><b>9/0</b>\t\tDecrease/increase audio volume</li></ul>")

	buttonBox := widgets.NewQDialogButtonBox3(widgets.QDialogButtonBox__Close, dialog)
	buttonBox.ConnectRejected(func() { dialog.Close() })

	vlayout := widgets.NewQVBoxLayout()
	vlayout.AddWidget(textBrowser, 0, 0)
	vlayout.AddWidget(buttonBox, 0, 0)

	dialog.SetLayout(vlayout)

	return dialog
}
