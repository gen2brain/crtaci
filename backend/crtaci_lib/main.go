package main

import (
	"golang.org/x/mobile/app"

	_ "crtaci/go_crtaci"
	_ "golang.org/x/mobile/bind/java"
)

func main() {
	app.Run(app.Callbacks{})
}
