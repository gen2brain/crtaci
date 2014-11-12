#!/usr/bin/env bash

mkdir -p build

GOOS=linux GOARCH=amd64 go build -o build/crtaci-http.linux.amd64 crtaci.go
strip build/crtaci-http.linux.amd64

GOOS=linux GOARCH=386 go build -o build/crtaci-http.linux.386 crtaci.go
strip build/crtaci-http.linux.386

GOOS=windows GOARCH=386 go build -o build/crtaci-http.exe -ldflags -H=windowsgui crtaci.go
i686-pc-mingw32-strip build/crtaci-http.exe

GOOS=darwin GOARCH=386 go build -o build/crtaci-http.darwin.386 crtaci.go
GOOS=darwin GOARCH=amd64 go build -o build/crtaci-http.darwin.amd64 crtaci.go

cp -f build/crtaci-http.linux.amd64 ../crtaci-http
