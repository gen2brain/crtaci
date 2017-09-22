package crtaci

import (
	"context"
	"fmt"
	"net/http"
	"time"

	"github.com/google/google-api-go-client/googleapi/transport"
)

// helper type
type helper struct {
	// Context
	Context context.Context
	// Api key
	Key string
	// Headers
	Headers map[string]string
}

// newHelper returns new helper
func newHelper(ctx context.Context) *helper {
	h := &helper{}
	h.Context = ctx
	return h
}

// Client returns http client
func (h *helper) Client() (*http.Client, error) {
	to := 10 * time.Second

	tr := &http.Transport{
		TLSHandshakeTimeout:   to,
		ResponseHeaderTimeout: to,
		MaxIdleConnsPerHost:   100,
		DisableKeepAlives:     true,
	}

	trg := &transport.APIKey{
		Key:       h.Key,
		Transport: tr,
	}

	cl := &http.Client{
		Transport: tr,
		Timeout:   to,
	}

	if h.Key != "" {
		cl = &http.Client{
			Transport: trg,
			Timeout:   to,
		}
	}

	return cl, nil
}

// Response returns http response
func (h *helper) Response(uri, method string) (*http.Response, error) {
	req, err := http.NewRequest(method, uri, nil)
	if err != nil {
		return nil, err
	}

	req = req.WithContext(h.Context)

	if h.Headers != nil {
		for key, value := range h.Headers {
			req.Header.Set(key, value)
		}
	}

	client, err := h.Client()
	if err != nil {
		return nil, fmt.Errorf("helper: %v", err)
	}

	res, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("helper: %v", err)
	}

	return res, nil
}
