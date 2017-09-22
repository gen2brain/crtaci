package main

// #include <mpv/client.h>
// #cgo darwin CFLAGS: -I/usr/local/include
import "C"

import (
	"fmt"
	"log"
	"runtime"
	"strings"

	"github.com/therecipe/qt/core"
	"github.com/therecipe/qt/widgets"
	"gitlab.com/hannahxy/go-mpv"
)

// Object3 type
type Object3 struct {
	core.QObject

	_ func() `signal:"paused"`
	_ func() `signal:"unpaused"`
	_ func() `signal:"startFile"`
	_ func() `signal:"fileLoaded"`
	_ func() `signal:"endFile"`
	_ func() `signal:"shutdown"`
}

// Player type
type Player struct {
	*Object3

	Mpv    *mpv.Mpv
	Window *widgets.QWidget

	paused  bool
	started bool
}

// NewPlayer returns new player
func NewPlayer(w *widgets.QWidget) *Player {
	return &Player{NewObject3(w), nil, w, false, false}
}

// Init initialize player
func (p *Player) Init() {
	p.Mpv = mpv.Create()
	p.Mpv.RequestLogMessages("no")
	//p.Mpv.RequestLogMessages("info")

	x := (widgets.QApplication_Desktop().Width() / 2) - (640 / 2)
	p.SetOptionString("geometry", fmt.Sprintf("640+%d+%d", x, p.Window.Y()+100))

	p.SetOptionString("osc", "yes")
	p.SetOptionString("ytdl", "no")

	switch runtime.GOOS {
	case "windows":
		p.SetOptionString("vo", "direct3d,opengl,sdl,null")
		p.SetOptionString("ao", "wasapi,sdl,null")
	case "linux":
		p.SetOptionString("vo", "opengl,sdl,xv,null")
		p.SetOptionString("ao", "alsa,sdl,null")
	case "darwin":
		p.SetOptionString("vo", "opengl,sdl,null")
		p.SetOptionString("ao", "coreaudio,sdl,null")
	}

	p.SetOption("cache-default", mpv.FORMAT_INT64, 128)
	p.SetOption("cache-seek-min", mpv.FORMAT_INT64, 32)
	p.SetOption("cache-secs", mpv.FORMAT_DOUBLE, 1.0)

	p.SetOptionString("input-default-bindings", "yes")
	p.SetOptionString("input-vo-keyboard", "yes")
	p.SetOptionString("stop-screensaver", "yes")

	err := p.Mpv.Initialize()
	if err != nil {
		log.Printf("Error: Initialize: %s\n", err.Error())
	}
}

// SetOption sets option
func (p *Player) SetOption(name string, format mpv.Format, data interface{}) {
	err := p.Mpv.SetOption(name, format, data)
	if err != nil {
		log.Printf("Error: SetOption %s: %s\n", name, err.Error())
	}
}

// SetOptionString sets string option
func (p *Player) SetOptionString(name, value string) {
	err := p.Mpv.SetOptionString(name, value)
	if err != nil {
		log.Printf("Error: SetOptionString %s: %s\n", name, err.Error())
	}
}

// Rotate rotates video
func (p *Player) Rotate(r int64) {
	err := p.Mpv.SetOption("video-rotate", mpv.FORMAT_INT64, r)
	if err != nil {
		log.Printf("Error: video-rotate: %s\n", err.Error())
	}
}

// Play plays video
func (p *Player) Play(url string, title string) {
	if title != "" {
		p.SetOptionString("force-media-title", title)
	}

	err := p.Mpv.Command([]string{"loadfile", url})
	if err != nil {
		log.Printf("Error: loadfile: %s\n", err.Error())
	}

	for {
		e := p.Mpv.WaitEvent(10000)
		p.handleEvent(e)

		if e.Event_Id == mpv.EVENT_SHUTDOWN || e.Event_Id == mpv.EVENT_END_FILE {
			p.paused = false
			p.started = false
			break
		}
	}

	p.Mpv.TerminateDestroy()
	p.Shutdown()
}

// Stop stops video
func (p *Player) Stop() {
	if p.IsStarted() {
		err := p.Mpv.Command([]string{"stop"})
		if err != nil {
			log.Printf("Error: stop: %s\n", err.Error())
		}
	}
}

// IsPaused checks if player is paused
func (p *Player) IsPaused() bool {
	return p.paused
}

// IsStarted checks if player is started
func (p *Player) IsStarted() bool {
	return p.started
}

// handleEvent handles player events
func (p *Player) handleEvent(e *mpv.Event) {
	switch e.Event_Id {
	case mpv.EVENT_PAUSE:
		p.paused = true
		p.Paused()
	case mpv.EVENT_UNPAUSE:
		p.paused = false
		p.Unpaused()
	case mpv.EVENT_START_FILE:
		p.started = true
		p.StartFile()
	case mpv.EVENT_FILE_LOADED:
		p.FileLoaded()
	case mpv.EVENT_END_FILE:
		p.EndFile()
	case mpv.EVENT_LOG_MESSAGE:
		s := (*C.struct_mpv_event_log_message)(e.Data)
		msg := C.GoString((*C.char)(s.text))
		log.Printf("Mpv: %s\n", strings.TrimSpace(msg))
	}
}
