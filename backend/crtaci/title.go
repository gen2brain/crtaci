package crtaci

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"
)

// videoTitle type
type videoTitle struct {
	Raw           string
	Filters       []string
	CensoredWords []string
	CensoredIds   []string
}

// newVideoTitle returns new videoTitle
func newVideoTitle(raw string, filters, words, ids []string) *videoTitle {
	t := &videoTitle{}
	t.Raw = raw
	t.Filters = filters
	t.CensoredWords = words
	t.CensoredIds = ids
	return t
}

// Format formats title
func (t *videoTitle) Format(name, altname, altname2 string) string {
	title := t.Raw

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

	for _, filter := range t.Filters {
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

	if strings.HasPrefix(title, "i ") || strings.HasPrefix(title, "and ") || strings.HasPrefix(title, " i ") ||
		strings.HasPrefix(title, "u ") || strings.HasPrefix(title, "u epizodi") {
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

// Episode returns cartoon episode
func (t *videoTitle) Episode() int {
	title := t.Raw

	title = reYear.ReplaceAllString(title, "")

	re20 := reTitle20.FindAllStringSubmatch(title, -1)
	if len(re20) > 1 {
		title = reTitle20.ReplaceAllString(title, " ")
	}

	for _, filter := range t.Filters {
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

// Season returns cartoon season
func (t *videoTitle) Season() int {
	title := t.Raw

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

// Valid checks if title is valid
func (t *videoTitle) Valid(name, altname, altname2, videoId string) bool {
	title := t.Raw
	title = reTitleR.ReplaceAllString(title, "$3")
	title = strings.TrimLeft(title, " ")

	if strings.HasPrefix(title, name) {
		if !t.Censored(videoId) {
			return true
		}
	}

	if altname != "" {
		if strings.HasPrefix(title, altname) {
			if !t.Censored(videoId) {
				return true
			}
		}
	}

	if altname2 != "" {
		if strings.HasPrefix(title, altname2) {
			if !t.Censored(videoId) {
				return true
			}
		}
	}

	return false
}

// Censored checks if title is censored
func (t *videoTitle) Censored(videoId string) bool {
	for _, word := range t.CensoredWords {
		if strings.Contains(t.Raw, word) {
			return true
		}
	}

	for _, id := range t.CensoredIds {
		if id == videoId {
			return true
		}
	}

	return false
}
