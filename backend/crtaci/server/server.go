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

// Server struct
type Server struct {
	Bind         string
	BaseDir      string
	Template     []byte
	ReadTimeout  int
	WriteTimeout int
}

// NewServer returns new Server
func NewServer() *Server {
	s := &Server{}
	return s
}

// Header sets server headers
func (s *Server) Header(w http.ResponseWriter) {
	w.Header().Set("Server", fmt.Sprintf("crtaci-http/%s", crtaci.Version))
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
}

// List lists cartoon characters
func (s *Server) List(w http.ResponseWriter, r *http.Request) {
	s.Header(w)

	js, err := crtaci.List()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Write([]byte(js))
	return
}

// Search searches for cartoons
func (s *Server) Search(w http.ResponseWriter, r *http.Request) {
	s.Header(w)

	query := r.FormValue("q")

	if query != "" {
		js, err := crtaci.Search(query)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		if js == "" || js == "empty" {
			http.Error(w, "404 Not Found", http.StatusNotFound)
			return
		}

		w.Write([]byte(js))
		return
	} else {
		http.Error(w, "400 Bad Request", http.StatusBadRequest)
		return
	}
}

// Extract extracts video uri
func (s *Server) Extract(w http.ResponseWriter, r *http.Request) {
	s.Header(w)

	service := r.FormValue("srv")
	videoId := r.FormValue("id")

	if service != "" && videoId != "" {
		js, err := crtaci.Extract(service, videoId)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		if js == "" || js == "empty" {
			http.Error(w, "", http.StatusNotFound)
			return
		} else {
			w.Write([]byte(js))
			return
		}
	} else {
		http.Error(w, "400 Bad Request", http.StatusBadRequest)
		return
	}
}

// Public handles public files
func (s *Server) Public(w http.ResponseWriter, r *http.Request) {
	path := filepath.Join(s.BaseDir, "public", r.URL.Path)
	if f, err := os.Stat(path); err == nil && !f.IsDir() {
		http.ServeFile(w, r, path)
		return
	}

	http.NotFound(w, r)
}

// Index handles index
func (s *Server) Index(w http.ResponseWriter, r *http.Request) {
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

	html := string(s.Template)
	html = strings.Replace(html, "{TITLE}", "Crtaći / "+strings.Title(query), -1)
	html = strings.Replace(html, "{CHARACTERS}", s.Characters(characters, query), -1)
	html = strings.Replace(html, "{CARTOONS}", s.Cartoons(cartoons), -1)

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Write([]byte(html))
}

// Characters returns characters html
func (s *Server) Characters(characters []crtaci.Character, query string) (h string) {
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

		h += fmt.Sprintf("<li%s><img src=\"%s\" width=\"48\" height=\"48\" alt=\"%s\"/><a href=\"?c=%s\" title=\"%s\">%s</a></li>", class, icon, c.Name, name, c.Name, c.Name)
	}

	return
}

// Cartoons returns cartoons html
func (s *Server) Cartoons(cartoons []crtaci.Cartoon) (h string) {
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

		h += fmt.Sprintf("<li><div class=\"image\"><a class=\"media\" href=\"%s\"><img class=\"thumb\" data-original=\"%s\" width=\"240\" height=\"180\"/><div class=\"play\"></div></a></div><div class=\"text\">%s%s (%s)</div></li>",
			c.Url, image, c.FormattedTitle, se, c.DurationString)
	}

	return
}

// ListenAndServe listens on the TCP address and serves requests
func (s *Server) ListenAndServe() {
	http.HandleFunc("/list", s.List)
	http.HandleFunc("/search", s.Search)
	http.HandleFunc("/extract", s.Extract)
	http.HandleFunc("/assets/", s.Public)
	http.HandleFunc("/download/", s.Public)
	http.HandleFunc("/", s.Index)

	l, err := net.Listen("tcp4", s.Bind)
	if err != nil {
		log.Fatal(err)
	}

	http.Serve(l, nil)
}

func main() {
	srv := NewServer()

	flag.StringVar(&srv.Bind, "bind-addr", ":7313", "Bind address")
	flag.IntVar(&srv.ReadTimeout, "read-timeout", 5, "Read timeout (seconds)")
	flag.IntVar(&srv.WriteTimeout, "write-timeout", 15, "Write timeout (seconds)")
	flag.Parse()

	var err error

	srv.BaseDir, err = filepath.Abs(filepath.Dir(os.Args[0]))
	if err != nil {
		log.Fatal(err)
		return
	}

	srv.Template, err = ioutil.ReadFile(filepath.Join(srv.BaseDir, "public", "assets", "view.html"))
	if err != nil {
		log.Fatal(err)
		return
	}

	srv.ListenAndServe()
}
