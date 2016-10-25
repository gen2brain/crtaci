#!/usr/bin/env bash

mkdir -p build

GOOS=linux GOARCH=amd64 go build -o build/crtaci-http.linux.amd64 -ldflags "-s -w"

GOOS=linux GOARCH=386 go build -o build/crtaci-http.linux.386 -ldflags "-s -w"

GOOS=windows GOARCH=386 go build -o build/crtaci-http.exe -ldflags "-H=windowsgui -s -w"
