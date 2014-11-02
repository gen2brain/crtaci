<?php

function get_icon($character) {
    if(!empty($character["altname"])) {
        $char = $character["altname"];
    } else {
        $char = $character["name"];
    }
    $char = str_replace(" ", "_", $char);
    return sprintf("assets/icons/%s.png", $char);
}

function get_season($cartoon) {
    $se = "";
    if($cartoon["season"] != -1) {
        $se .= sprintf("S%02d", $cartoon["season"]);
    }
    if($cartoon["episode"] != -1) {
        $se .= sprintf("E%02d", $cartoon["episode"]);
    }
    if(!empty($se)) {
        $se = " - " . $se;
    }
    return $se;
}

function get_html($query, $characters, $cartoons) {
    $ca = $ch = "";
    $tpl = file_get_contents("assets/view.html");

    $c = true;
    foreach($characters as $character) {
        if(!empty($character["altname"])) {
            $name = $character["altname"];
        } else {
            $name = $character["name"];
        }
        $a = urlencode($name);

        $class = ($c = !$c)?' class="odd"':' class="even"';
        if($query == $name) {
            $class = ' class="selected"';
        }

        $li = <<<EOF
            <li%s>
                <img src="%s" width="48" height="48" alt=""/><a href="%s" title="%s">%s</a>
            </li>\n
EOF;

        $ch .= sprintf($li, $class, get_icon($character), $a, ucwords($n), $character["name"]);
    }

    foreach($cartoons as $cartoon) {
        if($cartoon["service"] == "youtube") {
            $image = $cartoon["thumbnails"]["large"];
        } else {
            $image = $cartoon["thumbnails"]["medium"];
        }

        $se = get_season($cartoon);
        $li = <<<EOF
            <li>
                <div class="image"><a class="media" href="%s"><img class="thumb" data-original="%s" width="240" height="180"/><div class="play"></div></a></div>
                <div class="text">%s%s</div>
            </li>\n
EOF;
        $ca .= sprintf($li, $cartoon["url"], $image, $cartoon["formattedTitle"], $se);
    }

    $tpl = str_replace("{TITLE}", "Crtaći / " . ucwords($query), $tpl);
    $tpl = str_replace("{CHARACTERS}", $ch, $tpl);
    $tpl = str_replace("{CARTOONS}", $ca, $tpl);
    return $tpl;

}
