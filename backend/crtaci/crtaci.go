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

package crtaci

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"net/url"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/ChannelMeter/iso8601duration"
	"github.com/google/google-api-go-client/googleapi/transport"
	"google.golang.org/api/youtube/v3"

	"github.com/gen2brain/vidextr"
)

const Version = "1.8"

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

type Character struct {
	Name     string `json:"name"`
	AltName  string `json:"altname"`
	AltName2 string `json:"altname2"`
	Duration string `json:"duration"`
	Query    string `json:"query"`
}

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
	wg       sync.WaitGroup
	cartoons []Cartoon
)

var ctx, cancel = context.WithCancel(context.TODO())
var timeOut time.Duration = time.Duration(10 * time.Second)

var tr = &http.Transport{
	Dial:                func(network, addr string) (net.Conn, error) { return net.DialTimeout(network, addr, timeOut) },
	TLSHandshakeTimeout: timeOut,
	TLSClientConfig:     &tls.Config{InsecureSkipVerify: true},
	MaxIdleConnsPerHost: 10,
}

var client = &http.Client{
	Transport: tr,
	Timeout:   timeOut,
}

var clientGoogle = &http.Client{
	Transport: &transport.APIKey{
		Key:       "AIzaSyCzFjzxxyS_GNEAsyFxd1Ss8CbaJNQAmjs",
		Transport: tr,
	},
	Timeout: timeOut,
}

type multiSorter struct {
	cartoons []Cartoon
}

func (ms *multiSorter) Sort(cartoons []Cartoon) {
	ms.cartoons = cartoons
	sort.Sort(ms)
}

func (ms *multiSorter) Len() int {
	return len(ms.cartoons)
}

func (ms *multiSorter) Swap(i, j int) {
	ms.cartoons[i], ms.cartoons[j] = ms.cartoons[j], ms.cartoons[i]
}

func (ms *multiSorter) Less(i, j int) bool {
	p, q := &ms.cartoons[i], &ms.cartoons[j]

	episode := func(c1, c2 *Cartoon) bool {
		if c1.Episode == -1 {
			return false
		} else if c2.Episode == -1 {
			return true
		}
		return c1.Episode < c2.Episode
	}

	season := func(c1, c2 *Cartoon) bool {
		if c1.Season == -1 {
			return false
		} else if c2.Season == -1 {
			return true
		}
		return c1.Season < c2.Season
	}

	switch {
	case season(p, q):
		return true
	case season(q, p):
		return false
	case episode(p, q):
		return true
	case episode(q, p):
		return false
	}

	return episode(p, q)
}

func youTube(char Character) {
	defer wg.Done()
	defer func() {
		if r := recover(); r != nil {
			log.Print("Recovered in youTube:", r)
		}
	}()

	yt, err := youtube.New(clientGoogle)
	if err != nil {
		log.Print("Error creating YouTube client:", err)
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
			Q(getQuery(char, false)).
			MaxResults(50).
			VideoDuration(d).
			Type("video").
			PageToken(token)

		response, err := apiCall.Do()
		if err != nil {
			log.Print("Error making YouTube API call:", err.Error())
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
			log.Print("Error making YouTube API call:", err.Error())
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

			if isValidTitle(videoTitle, name, altname, altname2, videoId) && char.Duration == getDuration(videoDuration) {
				formattedTitle := getFormattedTitle(videoTitle, name, altname, altname2)

				c := Cartoon{
					videoId,
					name,
					videoTitle,
					formattedTitle,
					getEpisode(videoTitle),
					getSeason(videoTitle),
					"youtube",
					"https://www.youtube.com/watch?v=" + videoId,
					videoThumbSmall,
					videoThumbMedium,
					videoThumbLarge,
					videoDuration,
					formatDuration(videoDuration),
				}

				cartoons = append(cartoons, c)
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

func dailyMotion(char Character) {
	defer wg.Done()
	defer func() {
		if r := recover(); r != nil {
			log.Print("Recovered in dailyMotion:", r)
		}
	}()

	uri := "https://api.dailymotion.com/videos?search=%s&fields=id,title,url,duration,thumbnail_120_url,thumbnail_360_url,thumbnail_480_url&limit=50&page=%s&sort=relevance"

	name := strings.ToLower(char.Name)
	altname := strings.ToLower(char.AltName)
	altname2 := strings.ToLower(char.AltName2)

	getResponse := func(page string) ([]interface{}, bool) {
		req, err := http.NewRequest("GET", fmt.Sprintf(uri, getQuery(char, true), page), nil)
		if err != nil {
			log.Print("Error making DailyMotion API call: %v", err.Error())
			return nil, false
		}

		res, err := client.Do(req)
		if err != nil {
			log.Print("Error making DailyMotion API call: %v", err.Error())
			return nil, false
		}

		body, err := ioutil.ReadAll(res.Body)
		if err != nil {
			log.Print("Error making DailyMotion API call: %v", err.Error())
			return nil, false
		}
		res.Body.Close()

		var data map[string]interface{}
		err = json.Unmarshal(body, &data)
		if err != nil {
			log.Print("Error unmarshaling json:", err.Error())
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

			if isValidTitle(videoTitle, name, altname, altname2, videoId) && char.Duration == getDuration(videoDuration) {
				formattedTitle := getFormattedTitle(videoTitle, name, altname, altname2)

				c := Cartoon{
					videoId,
					name,
					videoTitle,
					formattedTitle,
					getEpisode(videoTitle),
					getSeason(videoTitle),
					"dailymotion",
					videoUrl,
					videoThumbSmall,
					videoThumbMedium,
					videoThumbLarge,
					videoDuration,
					formatDuration(videoDuration),
				}

				cartoons = append(cartoons, c)
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

func vimeo(char Character) {
	defer wg.Done()
	defer func() {
		if r := recover(); r != nil {
			log.Print("Recovered in vimeo:", r)
		}
	}()

	const apiKey = "e0ebf580f00d1345ea7a934c3703e2d9"
	uri := "https://api.vimeo.com/videos?query=%s&page=%s&per_page=100&sort=relevant"

	name := strings.ToLower(char.Name)
	altname := strings.ToLower(char.AltName)
	altname2 := strings.ToLower(char.AltName2)

	getResponse := func(page string) []interface{} {
		req, err := http.NewRequest("GET", fmt.Sprintf(uri, getQuery(char, true), page), nil)
		if err != nil {
			log.Print("Error making Vimeo API call: %v", err.Error())
			return nil
		}

		req = req.WithContext(ctx)
		req.Header.Set("Authorization", "bearer "+apiKey)
		req.Header.Set("Accept", "application/vnd.vimeo.video+json;version=3.2")
		res, err := client.Do(req)
		if err != nil {
			log.Print("Error making Vimeo API call:", err.Error())
			return nil
		}
		body, _ := ioutil.ReadAll(res.Body)
		res.Body.Close()

		var data map[string]interface{}
		err = json.Unmarshal(body, &data)
		if err != nil {
			log.Print("Error unmarshaling json:", err.Error())
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

			if isValidTitle(videoTitle, name, altname, altname2, videoId) && char.Duration == getDuration(videoDuration) {
				formattedTitle := getFormattedTitle(videoTitle, name, altname, altname2)

				c := Cartoon{
					videoId,
					name,
					videoTitle,
					formattedTitle,
					getEpisode(videoTitle),
					getSeason(videoTitle),
					"vimeo",
					videoUrl,
					videoThumbSmall,
					videoThumbMedium,
					videoThumbLarge,
					videoDuration,
					formatDuration(videoDuration),
				}

				cartoons = append(cartoons, c)
			}
		}
	}

	response := getResponse("1")
	if response != nil {
		parseResponse(response)
	}

}

func formatDuration(videoDuration float64) string {
	s := int(videoDuration)
	minutes := s / 60
	seconds := s - (minutes * 60)
	return fmt.Sprintf("%02d:%02d", minutes, seconds)
}

func getDuration(videoDuration float64) string {
	minutes := videoDuration / 60
	switch {
	case minutes < 4 && minutes > 0:
		return "short"
	case minutes >= 4 && minutes <= 20:
		return "medium"
	case minutes > 20 && minutes <= 50:
		return "long"
	case minutes > 50:
		return "xlong"
	default:
		return "any"
	}
}

func getFormattedTitle(videoTitle string, name string, altname string, altname2 string) string {

	title := videoTitle

	part := ""
	p := rePart.FindAllStringSubmatch(title, -1)
	if len(p) > 0 {
		part = p[0][1]
	}

	p2 := rePart2.FindAllStringSubmatch(title, -1)
	if len(p2) > 0 {
		part = p2[0][1]
	}

	title = reYear.ReplaceAllString(title, "")

	re20 := reTitle20.FindAllStringSubmatch(title, -1)
	if len(re20) > 1 {
		title = reTitle20.ReplaceAllString(title, " ")
	}

	for _, filter := range filters {
		title = strings.Replace(title, filter, " ", -1)
	}

	for _, re := range []*regexp.Regexp{
		reDesc, reExt, reRip, reChars, reTime, rePart, rePart2,
		reE1, reS1, reS4, reE2, reE5, reE6, reE3, reS2, reS3} {
		title = re.ReplaceAllString(title, "")
	}

	matches := reTitle.FindAllString(title, -1)
	title = strings.Join(matches, " ")
	title = strings.Replace(title, "_", " ", -1)

	name = strings.Replace(name, "-", "", -1)
	name = strings.TrimRight(name, " ")

	if altname2 != "" {
		title = strings.Replace(title, altname2, "", 1)
	}
	if altname != "" {
		title = strings.Replace(title, altname+" ", "", 1)
	}
	title = strings.Replace(title, name, "", 1)

	title = strings.TrimLeft(title, " ")
	title = strings.TrimRight(title, " ")

	title = reTitleR.ReplaceAllString(title, "$3")

	if strings.HasPrefix(title, "i ") || strings.HasPrefix(title, "and ") || strings.HasPrefix(title, " i ") || strings.HasPrefix(title, "u ") || strings.HasPrefix(title, "u epizodi") {
		title = fmt.Sprintf("%s %s", name, title)
	}

	if !reAlpha.MatchString(title) {
		title = name
	}

	if part != "" {
		title = fmt.Sprintf("%s - %s deo", title, part)
	}

	return title
}

func getEpisode(videoTitle string) int {
	title := videoTitle

	title = reYear.ReplaceAllString(title, "")

	re20 := reTitle20.FindAllStringSubmatch(title, -1)
	if len(re20) > 1 {
		title = reTitle20.ReplaceAllString(title, " ")
	}

	for _, filter := range filters {
		title = strings.Replace(title, filter, " ", -1)
	}
	for _, re := range []*regexp.Regexp{reDesc, reYear, reTime, rePart, rePart2, reS1, reS4} {
		title = re.ReplaceAllString(title, "")
	}

	ep := -1
	e1 := reE1.FindAllStringSubmatch(title, -1)
	if len(e1) > 0 {
		ep, _ = strconv.Atoi(e1[0][1])
		return ep
	}

	e2 := reE2.FindAllStringSubmatch(title, -1)
	if len(e2) > 0 {
		ep, _ = strconv.Atoi(e2[0][1])
		return ep
	}

	e5 := reE5.FindAllStringSubmatch(title, -1)
	if len(e5) > 0 {
		ep, _ = strconv.Atoi(e5[0][1])
		return ep
	}

	e6 := reE6.FindAllStringSubmatch(title, -1)
	if len(e6) > 0 {
		ep, _ = strconv.Atoi(e6[0][1])
		return ep
	}

	e3 := reE3.FindAllStringSubmatch(title, -1)
	if len(e3) > 0 {
		ep, _ = strconv.Atoi(e3[0][1])
		return ep
	}

	e4 := reE4.FindAllStringSubmatch(title, -1)
	notEp := reTitleNotEp.MatchString(title)
	if len(e4) > 0 && !notEp {
		ep, _ = strconv.Atoi(e4[0][1])
		if ep > 100 || ep == 0 {
			return -1
		}
		return ep
	}

	return ep
}

func getSeason(videoTitle string) int {
	title := videoTitle

	title = reYear.ReplaceAllString(title, "")

	re20 := reTitle20.FindAllStringSubmatch(title, -1)
	if len(re20) > 1 {
		title = reTitle20.ReplaceAllString(title, " ")
	}

	for _, re := range []*regexp.Regexp{reDesc, reYear, reTime, rePart, rePart2, reE1} {
		title = re.ReplaceAllString(title, "")
	}

	s := -1
	s1 := reS1.FindAllStringSubmatch(title, -1)
	if len(s1) > 0 {
		s, _ = strconv.Atoi(s1[0][1])
		return s
	}

	s2 := reS2.FindAllStringSubmatch(title, -1)
	if len(s2) > 0 {
		s, _ = strconv.Atoi(s2[0][1])
		return s
	}

	s3 := reS3.FindAllStringSubmatch(title, -1)
	if len(s3) > 0 {
		s, _ = strconv.Atoi(s3[0][1])
		if s >= 20 || s == 0 {
			return -1
		}
		return s
	}

	s4 := reS4.FindAllStringSubmatch(title, -1)
	if len(s4) > 0 {
		s, _ = strconv.Atoi(s4[0][1])
		if s >= 20 || s == 0 {
			return -1
		}
		return s
	}

	return s
}

func getQuery(char Character, escape bool) string {
	query := ""
	if char.Query != "" {
		query = char.Query
	} else if char.AltName != "" {
		query = char.AltName
	} else {
		query = char.Name
	}

	if escape {
		query = url.QueryEscape(query)
	}

	return query
}

func isCensored(videoTitle string, videoId string) bool {
	for _, word := range censoredWords {
		if strings.Contains(videoTitle, word) {
			return true
		}
	}

	for _, id := range censoredIds {
		if id == videoId {
			return true
		}
	}

	return false
}

func isValidTitle(videoTitle string, name string, altname string, altname2 string, videoId string) bool {
	videoTitle = reTitleR.ReplaceAllString(videoTitle, "$3")
	videoTitle = strings.TrimLeft(videoTitle, " ")

	if strings.HasPrefix(videoTitle, name) {
		if !isCensored(videoTitle, videoId) {
			return true
		}
	}

	if altname != "" {
		if strings.HasPrefix(videoTitle, altname) {
			if !isCensored(videoTitle, videoId) {
				return true
			}
		}
	}

	if altname2 != "" {
		if strings.HasPrefix(videoTitle, altname2) {
			if !isCensored(videoTitle, videoId) {
				return true
			}
		}
	}

	return false
}

func Cancel() {
	cancel()
}

func List() (string, error) {
	js, err := json.MarshalIndent(characters, "", "    ")
	if err != nil {
		return "empty", err
	}

	return string(js[:]), nil
}

func Search(query string) (string, error) {
	char := new(Character)
	for _, c := range characters {
		if query == c.Name || query == c.AltName {
			char = &c
			break
		}
	}

	ctx, cancel = context.WithCancel(context.TODO())

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

		return string(js[:]), nil
	} else {
		return "empty", nil
	}
}

func Extract(service string, videoId string) (string, error) {
	var url string
	var err error

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

func CheckUpdate() bool {
	ver, _ := strconv.ParseFloat(Version, 64)
	req, err := http.NewRequest("HEAD", fmt.Sprintf("https://crtaci.rs/download/crtaci-%.1f.apk", ver+0.1), nil)
	if err != nil {
		return false
	}

	res, err := client.Do(req)
	if err != nil {
		return false
	}

	if res.StatusCode == http.StatusOK {
		return true
	}

	return false
}
