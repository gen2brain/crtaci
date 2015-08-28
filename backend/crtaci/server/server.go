// Author: Milan Nikolic <gen2brain@gmail.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package main

import (
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"

	"crtaci"
)

var (
	appName    = "crtaci-http"
	appVersion = "1.7"
)

func setHeader(w http.ResponseWriter) {
	w.Header().Set("Server", fmt.Sprintf("%s/%s", appName, appVersion))
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
}

func handleList(w http.ResponseWriter, r *http.Request) {
	setHeader(w)
	js, err := crtaci.List()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.Write([]byte(js))
}

func handleSearch(w http.ResponseWriter, r *http.Request) {
	setHeader(w)

	query := r.FormValue("q")

	if query != "" {
		js, err := crtaci.Search(query)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		if js == "" {
			http.Error(w, "404 Not Found", http.StatusNotFound)
			return
		}
		w.Write([]byte(js))
	} else {
		http.Error(w, "403 Forbidden", http.StatusForbidden)
		return
	}
}

func handleExtract(w http.ResponseWriter, r *http.Request) {
	setHeader(w)

	service := r.FormValue("srv")
	videoId := r.FormValue("id")

	if service != "" && videoId != "" {
		js, err := crtaci.Extract(service, videoId)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		if js == "empty" {
			http.Error(w, "", http.StatusNotFound)
			return
		} else {
			w.Write([]byte(js))
			return
		}
	} else {
		http.Error(w, "", http.StatusForbidden)
		return
	}
}

func ListenAndServe(bind string) {
	http.HandleFunc("/list", handleList)
	http.HandleFunc("/search", handleSearch)
	http.HandleFunc("/extract", handleExtract)

	l, err := net.Listen("tcp4", bind)
	if err != nil {
		log.Fatal(err)
	}
	http.Serve(l, nil)
}

func main() {
	bind := flag.String("bind", ":7313", "Bind address")
	flag.Parse()
	ListenAndServe(*bind)
}
