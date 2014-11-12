<?php

class Crtaci {

    public $server= "127.0.0.1:7313";

    public function getCharacters() {
        $response = $this->httpGet("list");
        $json = json_decode($response, true);
        if(json_last_error() === JSON_ERROR_NONE) {
            return $json;
        }
        return null;
    }

    public function getCartoons($character) {
        $response = $this->httpGet("search?q=" . rawurlencode($character));
        $json = json_decode($response, true);
        if(json_last_error() === JSON_ERROR_NONE) {
            return $json;
        }
        return null;
    }

    private function httpGet($endPoint) {
        $ch = curl_init();
        $url = sprintf("http://%s/%s", $this->server, $endPoint);

        curl_setopt_array($ch, array(
                CURLOPT_URL => $url,
                CURLOPT_CONNECTTIMEOUT => 3,
                CURLOPT_TIMEOUT => 10,
                CURLOPT_RETURNTRANSFER => true,
            )
        );

        $response = curl_exec($ch);
        if($response === false) {
            return;
        }

        curl_close($ch);
        return $response;
    }

}
