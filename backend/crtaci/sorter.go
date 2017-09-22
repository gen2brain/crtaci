package crtaci

import (
	"sort"
)

// multiSorter type
type multiSorter struct {
	cartoons []Cartoon
}

// Sort sorts cartoons
func (ms *multiSorter) Sort(cartoons []Cartoon) {
	ms.cartoons = cartoons
	sort.Sort(ms)
}

// Len returns length of cartoons
func (ms *multiSorter) Len() int {
	return len(ms.cartoons)
}

// Swap swaps cartoons
func (ms *multiSorter) Swap(i, j int) {
	ms.cartoons[i], ms.cartoons[j] = ms.cartoons[j], ms.cartoons[i]
}

// Less compares cartoons season and episode
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
