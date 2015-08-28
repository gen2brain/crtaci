#!/usr/bin/env bash

mkdir -p build

GOOS=linux GOARCH=amd64 go build -o build/crtaci-http.linux.amd64
strip build/crtaci-http.linux.amd64

GOOS=linux GOARCH=386 go build -o build/crtaci-http.linux.386
strip build/crtaci-http.linux.386

#GOOS=windows GOARCH=386 go build -o build/crtaci-http.exe -ldflags -H=windowsgui
#i686-pc-mingw32-strip build/crtaci-http.exe

#GOOS=darwin GOARCH=amd64 go build -o build/crtaci-http.darwin

cp -f build/crtaci-http.linux.amd64 ../../crtaci-http
