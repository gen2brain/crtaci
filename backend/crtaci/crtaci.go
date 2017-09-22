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
	"github.com/gen2brain/vidextr"
	"google.golang.org/api/youtube/v3"
)

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
}

// Character type
type Character struct {
	Name     string `json:"name"`
	AltName  string `json:"altname"`
	AltName2 string `json:"altname2"`
	Duration string `json:"duration"`
	RawQuery string `json:"query"`
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
	{"atomski mrav", "", "", "medium", ""},
	{"asteriks", "", "asterix", "xlong", ""},
	{"a je to", "", "", "medium", "a je to crtani"},
	{"anđeoski prijatelji", "andjeoski prijatelji", "", "medium", ""},
	{"bananamen", "", "", "medium", ""},
	{"blinki bil", "", "блинки бил", "long", ""},
	{"blufonci", "", "", "medium", ""},
	{"bombončići", "bomboncici", "", "medium", ""},
	{"braća grim", "braca grim", "najlepse bajke", "long", ""},
	{"brzi gonzales", "", "", "medium", ""},
	{"čarli braun", "carli braun", "", "medium", ""},
	{"čarobni školski autobus", "carobni skolski autobus", "", "long", ""},
	{"čili vili", "cili vili", "", "medium", ""},
	{"cipelići", "cipelici", "", "medium", ""},
	{"denis napast", "", "", "long", ""},
	{"doživljaji šašave družine", "dozivljaji sasave druzine", "", "long", ""},
	{"droidi", "", "", "long", ""},
	{"duško dugouško", "dusko dugousko", "dusko 20dugousko", "medium", ""},
	{"džoni test", "dzoni test", "", "long", ""},
	{"elmer", "", "", "medium", "elmer crtani"},
	{"eustahije brzić", "eustahije brzic", "", "medium", ""},
	{"evoksi", "", "", "long", ""},
	{"generalova radnja", "", "", "medium", ""},
	{"grčka mitologija", "grcka mitologija", "", "long", "grcka mitologija crtani"},
	{"gustav", "gustavus", "", "medium", "gustavus crtani"},
	{"helo kiti", "", "", "medium", ""},
	{"hi men i gospodari svemira", "himen i gospodari svemira", "", "long", ""},
	{"inspektor radiša", "inspektor radisa", "", "medium", ""},
	{"iznogud", "", "", "medium", ""},
	{"jež alfred na zadatku", "jez alfred na zadatku", "", "medium", ""},
	{"kalimero", "", "kalimero - ", "medium", "kalimero- crtani"},
	{"kasper", "", "", "medium", "kasper crtani"},
	{"konanove avanture", "", "", "long", ""},
	{"kuče dragoljupče", "kuce dragoljupce", "", "medium", ""},
	{"lale gator", "", "", "medium", ""},
	{"la linea", "", "", "medium", ""},
	{"legenda o tarzanu", "", "", "long", ""},
	{"le piaf", "", "", "short", ""},
	{"liga super zloća", "liga super zloca", "", "medium", ""},
	{"mali detektivi", "", "", "long", ""},
	{"mali leteći medvjedići", "mali leteci medvjedici", "", "long", ""},
	{"masa i medved", "masha i medved", "masa i medvjed", "medium", "masa i medved crtani"},
	{"mačor mika", "macor mika", "", "long", ""},
	{"mece dobrići", "mece dobrici", "", "medium", ""},
	{"miki maus", "", "", "medium", ""},
	{"mornar popaj", "", "", "medium", ""},
	{"mr. bean", "mr bean", "mr.bean", "medium", "mr bean animated"},
	{"mumijevi", "", "", "medium", ""},
	{"nindža kornjače", "nindza kornjace", "ninja kornjace", "long", ""},
	{"ogi i žohari", "ogi i zohari", "", "long", ""},
	{"otkrića bez granica", "otkrica bez granica", "", "long", ""},
	{"paja patak", "", "", "medium", ""},
	{"patak dača", "patak daca", "", "medium", ""},
	{"pepa prase", "", "", "medium", ""},
	{"pepe le tvor", "", "", "medium", ""},
	{"pera detlić", "pera detlic", "", "medium", ""},
	{"pera kojot", "", "", "medium", ""},
	{"pingvini sa madagaskara", "", "", "medium", ""},
	{"pink panter", "", "", "medium", "pink panter crtani"},
	{"plava princeza", "", "", "long", ""},
	{"porodica kremenko", "", "", "long", ""},
	{"poručnik draguljče", "porucnik draguljce", "", "medium", ""},
	{"princeze sirene", "", "", "long", ""},
	{"profesor baltazar", "", "", "medium", ""},
	{"ptica trkačica", "ptica trkacica", "", "medium", ""},
	{"pustolovine sa braćom kret", "pustolovine sa bracom kret", "", "long", ""},
	{"rakuni", "", "", "long", ""},
	{"ratnik kišna kap", "ratnik kisna kap", "", "long", ""},
	{"ren i stimpi", "", "", "medium", ""},
	{"robotek", "", "robotech", "long", ""},
	{"šalabajzerići", "salabajzerici", "", "medium", ""},
	{"silvester", "", "silvester i tviti", "medium", "silvester crtani"},
	{"šilja", "silja", "", "medium", "silja crtani"},
	{"snorkijevci", "", "", "medium", ""},
	{"sofronije", "", "", "medium", ""},
	{"super miš", "super mis", "", "medium", "super mis crtani"},
	{"supermen", "", "", "medium", "supermen crtani"},
	{"super špijunke", "super spijunke", "", "long", ""},
	{"sport bili", "", "", "medium", ""},
	{"srle i pajče", "srle i pajce", "", "medium", ""},
	{"stanlio i olio", "", "", "medium", ""},
	{"stari crtaći", "stari crtaci", "stari sinhronizovani crtaci", "medium", ""},
	{"stripi", "", "", "medium", ""},
	{"štrumfovi", "strumpfovi", "strumfovi", "medium", "strumfovi crtani"},
	{"sundjer bob kockalone", "sundjer bob", "sunđer bob", "medium", ""},
	{"talični tom", "talicni tom", "", "long", ""},
	{"tarzan gospodar džungle", "tarzan gospodar dzungle", "", "long", ""},
	{"tom i džeri", "tom i dzeri", "", "medium", ""},
	{"transformersi", "", "", "long", ""},
	{"vitez koja", "", "", "medium", ""},
	{"voltron force", "", "", "long", "voltron force crtani"},
	{"vuk vučko", "vuk vucko", "", "medium", ""},
	{"wumi", "", "wummi", "short", "wumi crtani"},
	{"zamenik boža", "zamenik boza", "", "medium", ""},
	{"zemlja konja", "", "", "medium", ""},
	{"zmajeva kugla", "zmajeva kugla", "zmajeva kugla z", "long", ""},
}

var (
	wg    sync.WaitGroup
	mutex sync.Mutex

	cartoons []Cartoon

	ctx, cancel = context.WithCancel(context.TODO())
)

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

			if vt.Valid(name, altname, altname2, videoId) && char.Duration == vd.Desc() {
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

			if vt.Valid(name, altname, altname2, videoId) && char.Duration == vd.Desc() {
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

			if vt.Valid(name, altname, altname2, videoId) && char.Duration == vd.Desc() {
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
