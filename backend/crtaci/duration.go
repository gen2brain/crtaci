package crtaci

import (
	"fmt"
)

// videoDuration type
type videoDuration struct {
	Duration float64
}

// newDuration returns new duration
func newVideoDuration(dur float64) *videoDuration {
	d := &videoDuration{}
	d.Duration = dur
	return d
}

// Desc returns duration as descriptive string
func (d *videoDuration) Desc() string {
	minutes := d.Duration / 60
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

// Format formats duration to string
func (d *videoDuration) Format() string {
	s := int(d.Duration)
	minutes := s / 60
	seconds := s - (minutes * 60)
	return fmt.Sprintf("%02d:%02d", minutes, seconds)
}
