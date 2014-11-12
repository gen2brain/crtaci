package main

import (
	"code.google.com/p/go.mobile/app"

	_ "code.google.com/p/go.mobile/bind/java"
	_ "crtaci/go_crtaci"
)

func main() {
	app.Run(app.Callbacks{})
}
