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
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"

	"github.com/gen2brain/crtaci/backend/crtaci"
)

var (
	basedir, _ = filepath.Abs(filepath.Dir(os.Args[0]))
	template   []byte
)

func setHeader(w http.ResponseWriter) {
	w.Header().Set("Server", fmt.Sprintf("crtaci-http/%s", crtaci.Version))
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

func handleAssets(w http.ResponseWriter, r *http.Request) {
	path := filepath.Join(basedir, "public", r.URL.Path)
	if f, err := os.Stat(path); err == nil && !f.IsDir() {
		http.ServeFile(w, r, path)
		return
	}
	http.NotFound(w, r)
}

func handleIndex(w http.ResponseWriter, r *http.Request) {
	list, err := crtaci.List()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	characters := make([]crtaci.Character, 0)
	err = json.Unmarshal([]byte(list), &characters)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	query := r.FormValue("c")
	if query == "" {
		query = characters[0].Name
	}

	search, err := crtaci.Search(query)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if search == "" || search == "empty" {
		http.Error(w, "404 Not Found", http.StatusNotFound)
		return
	}

	cartoons := make([]crtaci.Cartoon, 0)
	err = json.Unmarshal([]byte(search), &cartoons)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	var h1, h2 string
	for idx, c := range characters {
		name := c.Name
		icon := c.Name
		if c.AltName != "" {
			name = c.AltName
			icon = c.AltName
		}

		name = url.QueryEscape(name)
		icon = fmt.Sprintf("assets/icons/%s.png", strings.Replace(icon, " ", "_", -1))

		class := " class=\"odd\""
		if idx%2 == 0 {
			class = " class=\"even\""
		}

		if url.QueryEscape(query) == name {
			class = " class=\"selected\""
		}

		h1 += fmt.Sprintf("<li%s><img src=\"%s\" width=\"48\" height=\"48\" alt=\"\"/><a href=\"?c=%s\" title=\"%s\">%s</a></li>", class, icon, name, c.Name, c.Name)
	}

	for _, c := range cartoons {
		image := c.ThumbLarge
		if c.Service != "youtube" {
			image = c.ThumbMedium
		}

		if !strings.Contains(image, "https") {
			image = strings.Replace(image, "http", "https", -1)
		}

		var se string
		if c.Season != -1 {
			se += fmt.Sprintf("S%02d", c.Season)
		}
		if c.Episode != -1 {
			se += fmt.Sprintf("E%02d", c.Episode)
		}
		if se != "" {
			se = " - " + se
		}

		h2 += fmt.Sprintf("<li><div class=\"image\"><a class=\"media\" href=\"%s\"><img class=\"thumb\" data-original=\"%s\" width=\"240\" height=\"180\"/><div class=\"play\"></div></a></div><div class=\"text\">%s%s (%s)</div></li>", c.Url, image, c.FormattedTitle, se, c.DurationString)
	}

	html := string(template)
	html = strings.Replace(html, "{TITLE}", "Crtaći / "+strings.Title(query), -1)
	html = strings.Replace(html, "{CHARACTERS}", h1, -1)
	html = strings.Replace(html, "{CARTOONS}", h2, -1)

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Write([]byte(html))
}

func ListenAndServe(bind string) {
	http.HandleFunc("/list", handleList)
	http.HandleFunc("/search", handleSearch)
	http.HandleFunc("/extract", handleExtract)
	http.HandleFunc("/assets/", handleAssets)
	http.HandleFunc("/", handleIndex)

	l, err := net.Listen("tcp4", bind)
	if err != nil {
		log.Fatal(err)
	}
	http.Serve(l, nil)
}

func main() {
	bind := flag.String("bind", ":7313", "Bind address")
	flag.Parse()

	var err error
	template, err = ioutil.ReadFile(filepath.Join(basedir, "public", "assets", "view.html"))
	if err != nil {
		log.Fatal(err)
		return
	}

	ListenAndServe(*bind)
}
