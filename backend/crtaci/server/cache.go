package main

import (
	"crypto/md5"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// cache type
type cache struct {
	CacheDir string
}

// newCache returns new cache
func newCache(cacheDir string) *cache {
	c := &cache{}
	c.CacheDir = filepath.Join(cacheDir, "crtaci")

	if _, err := os.Stat(c.CacheDir); os.IsNotExist(err) {
		os.MkdirAll(c.CacheDir, os.ModePerm)
	}

	return c
}

// Write saves to cache file
func (c *cache) Write(key string, data []byte) {
	md5key := md5.Sum([]byte(strings.ToLower(key)))
	file := filepath.Join(c.CacheDir, fmt.Sprintf("%x.json", md5key))

	err := ioutil.WriteFile(file, data, 0644)
	if err != nil {
		log.Printf("cache: %v\n", err)
	}
}

// Read reads from cache file
func (c *cache) Read(key string, seconds int64) []byte {
	md5key := md5.Sum([]byte(strings.ToLower(key)))
	file := filepath.Join(c.CacheDir, fmt.Sprintf("%x.json", md5key))

	info, err := os.Stat(file)
	if err != nil {
		return nil
	}

	if seconds != 0 && seconds > 0 {
		mtime := info.ModTime().Unix()
		if time.Now().Unix()-mtime > seconds {
			return nil
		}
	}

	data, err := ioutil.ReadFile(file)
	if err != nil {
		log.Printf("cache: %v\n", err)
		return nil
	}

	return data
}
