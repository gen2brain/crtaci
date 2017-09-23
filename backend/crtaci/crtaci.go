// Package crtaci searches YouTube, DailyMotion and Vimeo for good old cartoons
package crtaci

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"

	"github.com/ChannelMeter/iso8601duration"
	"github.com/chrisport/go-lang-detector/langdet"
	"github.com/gen2brain/vidextr"
	"google.golang.org/api/youtube/v3"
)

//go:generate go-bindata -nocompress -pkg crtaci ../languages.json

// Version name
const Version = "1.9"

// Cartoon type
type Cartoon struct {
	Id             string  `json:"id"`
	Character      string  `json:"character"`
	Title          string  `json:"title"`
	FormattedTitle string  `json:"formattedTitle"`
	Episode        int     `json:"episode"`
	Season         int     `json:"season"`
	Service        string  `json:"service"`
	Url            string  `json:"url"`
	ThumbSmall     string  `json:"thumbSmall"`
	ThumbMedium    string  `json:"thumbMedium"`
	ThumbLarge     string  `json:"thumbLarge"`
	Duration       float64 `json:"duration"`
	DurationString string  `json:"durationString"`
	Language       string  `json:"language"`
}

// Character type
type Character struct {
	Name      string `json:"name"`
	AltName   string `json:"altname"`
	AltName2  string `json:"altname2"`
	Duration  string `json:"duration"`
	RawQuery  string `json:"query"`
	Languages string `json:"languages"`
}

// Query returns character query to search for
func (c *Character) Query(escape bool) (query string) {
	if c.RawQuery != "" {
		query = c.RawQuery
	} else if c.AltName != "" {
		query = c.AltName
	} else {
		query = c.Name
	}

	if escape {
		query = url.QueryEscape(query)
	}

	return
}

// characters contains list of cartoon characters
var characters = []Character{
	{"atomski mrav", "", "", "medium", "", "sr@latin"},
	{"asteriks", "", "", "xlong", "", "sr@latin"},
	{"a je to", "", "", "medium", "a je to crtani", "sr@latin"},
	{"anđeoski prijatelji", "andjeoski prijatelji", "", "medium", "", "sr@latin,hr"},
	{"bananamen", "", "", "medium", "", "sr@latin"},
	{"blinki bil", "", "блинки бил", "long", "", "sr,sr@latin,hr"},
	{"blufonci", "", "", "medium", "", "sr@latin,hr"},
	{"bombončići", "bomboncici", "", "medium", "", "sr@latin,hr,en"},
	{"braća grim", "braca grim", "najlepse bajke", "long", "", "sr@latin,hr"},
	{"brzi gonzales", "", "", "medium", "", "sr@latin,en"},
	{"čarli braun", "carli braun", "", "medium", "", "sr@latin"},
	{"čarobni školski autobus", "carobni skolski autobus", "", "long", "", "sr@latin,hr,pl"},
	{"čili vili", "cili vili", "", "medium", "", "sr@latin"},
	{"cipelići", "cipelici", "", "medium", "", "sr@latin,hr,it"},
	{"denis napast", "", "", "long", "", "sr@latin"},
	{"doživljaji šašave družine", "dozivljaji sasave druzine", "", "long", "", "sr@latin"},
	{"droidi", "", "", "long", "", "sr@latin,it"},
	{"duško dugouško", "dusko dugousko", "dusko 20dugousko", "medium", "", "sr@latin,hr,en"},
	{"džoni test", "dzoni test", "", "long", "", "sr@latin,en,pl"},
	{"elmer", "", "", "medium", "elmer crtani", "sr@latin"},
	{"eustahije brzić", "eustahije brzic", "", "medium", "", "sr@latin,hr,en"},
	{"evoksi", "", "", "long", "", "sr@latin,hr"},
	{"generalova radnja", "", "", "medium", "", "sr@latin,hr"},
	{"grčka mitologija", "grcka mitologija", "", "long", "grcka mitologija crtani", "sr@latin,hr"},
	{"gustav", "gustavus", "", "medium", "gustavus crtani", "sr@latin,en"},
	{"helo kiti", "", "", "medium", "", "sr@latin,hr,en"},
	{"hi men i gospodari svemira", "himen i gospodari svemira", "", "long", "", "sr@latin,hr"},
	{"inspektor radiša", "inspektor radisa", "", "medium", "", "sr@latin,hr"},
	{"iznogud", "", "", "medium", "", "sr@latin,hr"},
	{"jež alfred na zadatku", "jez alfred na zadatku", "", "medium", "", "sr@latin,hr"},
	{"kalimero", "", "kalimero - ", "medium", "kalimero- crtani", "sr@latin,hr"},
	{"kasper", "", "", "medium", "kasper crtani", "sr@latin,hr"},
	{"konanove avanture", "", "", "long", "", "sr@latin,hr"},
	{"kuče dragoljupče", "kuce dragoljupce", "", "medium", "", "sr@latin,hr"},
	{"lale gator", "", "", "medium", "", "sr@latin,hr,en"},
	{"la linea", "", "", "medium", "", "sr@latin,hres,it,en"},
	{"legenda o tarzanu", "", "", "long", "", "sr@latin,hr"},
	{"le piaf", "", "", "short", "", "sr@latin,hr,es"},
	{"liga super zloća", "liga super zloca", "", "medium", "", "sr@latin,hr"},
	{"mali detektivi", "", "", "long", "", "hr"},
	{"mali leteći medvjedići", "mali leteci medvjedici", "", "long", "", "sr@latin,hr"},
	{"masa i medved", "masha i medved", "masa i medvjed", "medium", "masa i medved crtani", "sr@latin,hr,en"},
	{"mačor mika", "macor mika", "", "long", "", "sr@latin,hr"},
	{"mece dobrići", "mece dobrici", "", "medium", "", "sr@latin,hr"},
	{"miki maus", "", "", "medium", "", "sr@latin"},
	{"mornar popaj", "", "", "medium", "", "sr@latin"},
	{"mr. bean", "mr bean", "mr.bean", "medium", "mr bean animated", "en"},
	{"mumijevi", "", "", "medium", "", "sr@latin,hr"},
	{"nindža kornjače", "nindza kornjace", "ninja kornjace", "long", "", "sr@latin,hr,pl"},
	{"ogi i žohari", "ogi i zohari", "", "long", "", "sr@latin,hr"},
	{"otkrića bez granica", "otkrica bez granica", "", "long", "", "sr@latin"},
	{"paja patak", "", "", "medium", "", "sr@latin,hr"},
	{"patak dača", "patak daca", "", "medium", "", "sr@latin"},
	{"pepa prase", "", "", "medium", "", "sr@latin"},
	{"pepe le tvor", "", "", "medium", "", "sr@latin,en"},
	{"pera detlić", "pera detlic", "", "medium", "", "sr@latin,en"},
	{"pera kojot", "", "", "medium", "", "sr@latin,pl"},
	{"pingvini sa madagaskara", "", "", "medium", "", "sr@latin"},
	{"pink panter", "", "", "medium", "pink panter crtani", "sr@latin,en"},
	{"plava princeza", "", "", "long", "", "sr@latin,hr"},
	{"porodica kremenko", "", "", "long", "", "sr@latin,hr"},
	{"poručnik draguljče", "porucnik draguljce", "", "medium", "", "sr@latin,hr"},
	{"princeze sirene", "", "", "long", "", "sr@latin,hr,en"},
	{"profesor baltazar", "", "", "medium", "", "sr@latin,hr,es,en"},
	{"ptica trkačica", "ptica trkacica", "", "medium", "", "sr@latin,hr,en"},
	{"pustolovine sa braćom kret", "pustolovine sa bracom kret", "", "long", "", "sr@latin"},
	{"rakuni", "", "", "long", "", "sr@latin,hr"},
	{"ratnik kišna kap", "ratnik kisna kap", "", "long", "", "sr@latin"},
	{"ren i stimpi", "", "", "medium", "", "sr@latin,hr"},
	{"robotek", "", "robotech", "long", "", "sr@latin,hr"},
	{"šalabajzerići", "salabajzerici", "", "medium", "", "sr@latin,hr"},
	{"silvester", "", "silvester i tviti", "medium", "silvester crtani", "sr@latin,hr"},
	{"šilja", "silja", "", "medium", "silja crtani", "sr@latin,hr"},
	{"snorkijevci", "", "", "medium", "", "sr@latin,hr"},
	{"sofronije", "", "", "medium", "", "sr@latin"},
	{"super miš", "super mis", "", "medium", "super mis crtani", "sr@latin,en"},
	{"supermen", "", "", "medium", "supermen crtani", "sr@latin,en"},
	{"super špijunke", "super spijunke", "", "long", "", "sr@latin,hr"},
	{"sport bili", "", "", "medium", "", "sr@latin,hr"},
	{"srle i pajče", "srle i pajce", "", "medium", "", "sr@latin,hr"},
	{"stanlio i olio", "", "", "medium", "", "sr@latin,it"},
	{"stari crtaći", "stari crtaci", "stari sinhronizovani crtaci", "medium", "", "sr@latin,hr"},
	{"stripi", "", "", "medium", "", "sr@latin,en"},
	{"štrumfovi", "strumpfovi", "strumfovi", "medium", "strumfovi crtani", "sr@latin,hr"},
	{"sundjer bob kockalone", "sundjer bob", "sunđer bob", "medium", "", "sr@latin"},
	{"talični tom", "talicni tom", "", "long", "", "sr@latin,hr"},
	{"tarzan gospodar džungle", "tarzan gospodar dzungle", "", "long", "", "sr@latin,hr"},
	{"tom i džeri", "tom i dzeri", "", "medium", "", "sr@latin"},
	{"transformersi", "", "", "long", "", "sr@latin,hr,it"},
	{"vitez koja", "", "", "medium", "", "sr@latin"},
	{"voltron force", "", "", "long", "voltron force crtani", "sr@latin,hr,it"},
	{"vuk vučko", "vuk vucko", "", "medium", "", "sr@latin,hr,en"},
	{"wumi", "", "wummi", "short", "wumi crtani", "sr@latin,hr,pl"},
	{"zamenik boža", "zamenik boza", "", "medium", "", "sr@latin"},
	{"zemlja konja", "", "", "medium", "", "sr@latin,hr"},
	{"zmajeva kugla", "zmajeva kugla", "zmajeva kugla z", "long", "", "sr@latin,hr"},
}

var (
	wg    sync.WaitGroup
	mutex sync.Mutex

	cartoons []Cartoon

	ctx, cancel = context.WithCancel(context.TODO())

	detector = langdet.NewDetector()
)

func init() {
	languages, _ := Asset("../languages.json")

	customLanguages := []langdet.Language{}
	json.Unmarshal(languages, &customLanguages)

	detector.AddLanguage(customLanguages...)
	detector.MinimumConfidence = 0.45
}

// langValid checks if detected language is valid
func langValid(lang string, langs []string) bool {
	for _, l := range langs {
		if lang == l {
			return true
		}
	}

	return false
}

// youTube searches YouTube
func youTube(char Character) {
	defer func() {
		wg.Done()
		if r := recover(); r != nil {
			log.Print("Recovered in youTube:", r)
		}
	}()

	h := newHelper(ctx)
	h.Key = "YOUR_API_KEY"

	cl, err := h.Client()
	if err != nil {
		log.Print("Error creating YouTube client: ", err)
		return
	}

	yt, err := youtube.New(cl)
	if err != nil {
		log.Print("Error creating YouTube client: ", err)
		return
	}

	name := strings.ToLower(char.Name)
	altname := strings.ToLower(char.AltName)
	altname2 := strings.ToLower(char.AltName2)

	getSearchResponse := func(token string) *youtube.SearchListResponse {
		d := char.Duration
		if char.Duration == "xlong" {
			d = "long"
		}

		apiCall := yt.Search.List("id,snippet").
			Q(char.Query(false)).
			MaxResults(50).
			VideoDuration(d).
			Type("video").
			PageToken(token)

		response, err := apiCall.Do()
		if err != nil {
			log.Print("Error making YouTube API call: ", err.Error())
			return nil
		}

		return response
	}

	getVideoResponse := func(r *youtube.SearchListResponse) *youtube.VideoListResponse {
		ids := make([]string, 0)
		for _, video := range r.Items {
			ids = append(ids, video.Id.VideoId)
		}

		apiCall := yt.Videos.List("id,contentDetails").Id(strings.Join(ids, ","))

		response, err := apiCall.Do()
		if err != nil {
			log.Print("Error making YouTube API call: ", err.Error())
			return nil
		}

		return response
	}

	parseResponse := func(searchResponse *youtube.SearchListResponse, videoResponse *youtube.VideoListResponse) {
		for _, video := range searchResponse.Items {
			videoId := video.Id.VideoId
			videoTitle := strings.ToLower(video.Snippet.Title)
			videoThumbSmall := video.Snippet.Thumbnails.Default.Url
			videoThumbMedium := video.Snippet.Thumbnails.Medium.Url
			videoThumbLarge := video.Snippet.Thumbnails.High.Url

			var videoDuration float64
			for _, v := range videoResponse.Items {
				if videoId == v.Id {
					d, err := duration.FromString(v.ContentDetails.Duration)
					if err != nil {
						break
					}
					videoDuration = d.ToDuration().Seconds()
				}
			}

			vd := newVideoDuration(videoDuration)
			vt := newVideoTitle(videoTitle, filters, censoredWords, censoredIds)

			lang := detector.GetClosestLanguage(vt.Raw)

			if vt.Valid(name, altname, altname2, videoId) && char.Duration == vd.Desc() && langValid(lang, strings.Split(char.Languages, ",")) {
				c := Cartoon{
					videoId,
					name,
					vt.Raw,
					vt.Format(name, altname, altname2),
					vt.Episode(),
					vt.Season(),
					"youtube",
					"https://www.youtube.com/watch?v=" + videoId,
					videoThumbSmall,
					videoThumbMedium,
					videoThumbLarge,
					vd.Duration,
					vd.Format(),
					lang,
				}

				mutex.Lock()
				cartoons = append(cartoons, c)
				mutex.Unlock()
			}
		}
	}

	searchResponse := getSearchResponse("")
	if searchResponse != nil {
		videoResponse := getVideoResponse(searchResponse)
		parseResponse(searchResponse, videoResponse)

		if searchResponse.NextPageToken != "" {
			searchResponse = getSearchResponse(searchResponse.NextPageToken)
			if searchResponse != nil {
				videoResponse = getVideoResponse(searchResponse)
				parseResponse(searchResponse, videoResponse)
			}
		}
	}

}

// dailyMotion searches DailyMotion
func dailyMotion(char Character) {
	defer func() {
		wg.Done()
		if r := recover(); r != nil {
			log.Print("Recovered in dailyMotion:", r)
		}
	}()

	uri := "https://api.dailymotion.com/videos?search=%s&fields=id,title,url,duration,thumbnail_120_url,thumbnail_360_url,thumbnail_480_url&limit=50&page=%s&sort=relevance"

	name := strings.ToLower(char.Name)
	altname := strings.ToLower(char.AltName)
	altname2 := strings.ToLower(char.AltName2)

	getResponse := func(page string) ([]interface{}, bool) {
		h := newHelper(ctx)

		res, err := h.Response(fmt.Sprintf(uri, char.Query(true), page), "GET")
		if err != nil {
			log.Print("Error making DailyMotion API call: ", err.Error())
			return nil, false
		}

		if res.StatusCode != http.StatusOK {
			log.Print("Error making DailyMotion API call: ", fmt.Sprintf("response received status code %d", res.StatusCode))
			return nil, false
		}

		body, err := ioutil.ReadAll(res.Body)
		if err != nil {
			log.Print("Error making DailyMotion API call: ", err.Error())
			return nil, false
		}
		res.Body.Close()

		var data map[string]interface{}
		err = json.Unmarshal(body, &data)
		if err != nil {
			log.Print("Error unmarshaling json: ", err.Error())
			return nil, false
		}

		hasMore, ok := data["has_more"].(bool)
		if !ok {
			return nil, false
		}

		response, ok := data["list"].([]interface{})
		if !ok {
			return nil, false
		}

		if len(response) == 0 {
			return nil, false
		}

		return response, hasMore
	}

	parseResponse := func(response []interface{}) {
		for _, obj := range response {
			video, ok := obj.(map[string]interface{})
			if !ok {
				continue
			}

			videoId := video["id"].(string)
			videoTitle := strings.ToLower(video["title"].(string))
			videoUrl := video["url"].(string)
			videoThumbSmall := video["thumbnail_120_url"].(string)
			videoThumbMedium := video["thumbnail_360_url"].(string)
			videoThumbLarge := video["thumbnail_480_url"].(string)
			videoDuration := video["duration"].(float64)

			vd := newVideoDuration(videoDuration)
			vt := newVideoTitle(videoTitle, filters, censoredWords, censoredIds)

			lang := detector.GetClosestLanguage(vt.Raw)

			if vt.Valid(name, altname, altname2, videoId) && char.Duration == vd.Desc() && langValid(lang, strings.Split(char.Languages, ",")) {
				c := Cartoon{
					videoId,
					name,
					vt.Raw,
					vt.Format(name, altname, altname2),
					vt.Episode(),
					vt.Season(),
					"dailymotion",
					videoUrl,
					videoThumbSmall,
					videoThumbMedium,
					videoThumbLarge,
					vd.Duration,
					vd.Format(),
					lang,
				}

				mutex.Lock()
				cartoons = append(cartoons, c)
				mutex.Unlock()
			}
		}
	}

	response, hasMore := getResponse("1")
	if response != nil {
		parseResponse(response)
	}

	if hasMore {
		response, _ := getResponse("2")
		if response != nil {
			parseResponse(response)
		}
	}

}

// vimeo searches Vimeo
func vimeo(char Character) {
	defer func() {
		wg.Done()
		if r := recover(); r != nil {
			log.Print("Recovered in vimeo:", r)
		}
	}()

	apiKey := "YOUR_API_KEY"
	uri := "https://api.vimeo.com/videos?query=%s&page=%s&per_page=100&sort=relevant&fields=link,name,duration,pictures"

	name := strings.ToLower(char.Name)
	altname := strings.ToLower(char.AltName)
	altname2 := strings.ToLower(char.AltName2)

	getResponse := func(page string) []interface{} {
		h := newHelper(ctx)

		h.Headers = map[string]string{
			"Authorization": "bearer " + apiKey,
			"Accept":        "application/vnd.vimeo.video+json;version=3.2",
		}

		res, err := h.Response(fmt.Sprintf(uri, char.Query(true), page), "GET")
		if err != nil {
			log.Print("Error making Vimeo API call: ", err.Error())
			return nil
		}

		if res.StatusCode != http.StatusOK {
			log.Print("Error making Vimeo API call: ", fmt.Sprintf("response received status code %d", res.StatusCode))
			return nil
		}

		body, err := ioutil.ReadAll(res.Body)
		if err != nil {
			log.Print("Error making Vimeo API call: ", err.Error())
			return nil
		}
		res.Body.Close()

		var data map[string]interface{}
		err = json.Unmarshal(body, &data)
		if err != nil {
			log.Print("Error unmarshaling json: ", err.Error())
			return nil
		}

		response, ok := data["data"].([]interface{})
		if !ok {
			return nil
		}

		if len(response) == 0 {
			return nil
		}

		return response
	}

	parseResponse := func(response []interface{}) {
		for _, obj := range response {
			video, ok := obj.(map[string]interface{})
			if !ok {
				continue
			}

			videoId := strings.Replace(video["link"].(string), "https://vimeo.com/", "", -1)
			videoTitle := strings.ToLower(video["name"].(string))
			videoUrl := video["link"].(string)

			pictures, ok := video["pictures"].(map[string]interface{})
			if !ok {
				continue
			}

			sizes := pictures["sizes"].([]interface{})

			if len(sizes) < 4 {
				continue
			}

			videoThumbSmall := sizes[3].(map[string]interface{})["link"].(string)
			videoThumbMedium := sizes[2].(map[string]interface{})["link"].(string)
			videoThumbLarge := sizes[1].(map[string]interface{})["link"].(string)
			videoDuration := video["duration"].(float64)

			vd := newVideoDuration(videoDuration)
			vt := newVideoTitle(videoTitle, filters, censoredWords, censoredIds)

			lang := detector.GetClosestLanguage(vt.Raw)

			if vt.Valid(name, altname, altname2, videoId) && char.Duration == vd.Desc() && langValid(lang, strings.Split(char.Languages, ",")) {
				c := Cartoon{
					videoId,
					name,
					vt.Raw,
					vt.Format(name, altname, altname2),
					vt.Episode(),
					vt.Season(),
					"vimeo",
					videoUrl,
					videoThumbSmall,
					videoThumbMedium,
					videoThumbLarge,
					vd.Duration,
					vd.Format(),
					lang,
				}

				mutex.Lock()
				cartoons = append(cartoons, c)
				mutex.Unlock()
			}
		}
	}

	response := getResponse("1")
	if response != nil {
		parseResponse(response)
	}

}

// Cancel cancels http context
func Cancel() {
	cancel()
}

// List lists cartoon characters
func List() (string, error) {
	js, err := json.MarshalIndent(characters, "", "    ")
	if err != nil {
		return "empty", err
	}

	return string(js), nil
}

// Search searches for cartoons
func Search(query string) (string, error) {
	ctx, cancel = context.WithCancel(context.TODO())

	char := new(Character)
	for _, c := range characters {
		if query == c.Name || query == c.AltName {
			char = &c
			break
		}
	}

	if char.Name != "" {
		wg.Add(3)
		cartoons = make([]Cartoon, 0)
		go youTube(*char)
		go dailyMotion(*char)
		go vimeo(*char)
		wg.Wait()

		ms := multiSorter{}
		ms.Sort(cartoons)

		js, err := json.MarshalIndent(cartoons, "", "    ")
		if err != nil {
			return "empty", err
		}

		return string(js), nil
	} else {
		return "empty", nil
	}
}

// Extract extracts video uri
func Extract(service string, videoId string) (url string, err error) {
	switch {
	case service == "youtube":
		url, err = vidextr.YouTube(videoId)
	case service == "dailymotion":
		url, err = vidextr.DailyMotion(videoId)
	case service == "vimeo":
		url, err = vidextr.Vimeo(videoId)
	}

	if err != nil {
		return "empty", err
	}

	if url == "" {
		return "empty", nil
	}

	return url, nil
}

// UpdateExists checks if new version is available
func UpdateExists() bool {
	ctx, cancel = context.WithCancel(context.TODO())

	h := newHelper(ctx)
	ver, _ := strconv.ParseFloat(Version, 64)

	res, err := h.Response(fmt.Sprintf("https://crtaci.rs/download/crtaci-%.1f.apk", ver+0.1), "HEAD")
	if err != nil {
		return false
	}

	if res.StatusCode == http.StatusOK {
		return true
	}

	return false
}
