<?php

function get_icon($character) {
    if(!empty($character["AltName"])) {
        $char = $character["AltName"];
    } else {
        $char = $character["Name"];
    }
    $char = str_replace(" - ", "", $char);
    $char = str_replace("-", "", $char);
    $char = str_replace(" ", "_", $char);
    return sprintf("assets/icons/%s.png", $char);
}

function get_season($cartoon) {
    $se = "";
    if($cartoon["Season"] != -1) {
        $se .= sprintf("S%02d", $cartoon["Season"]);
    }
    if($cartoon["Episode"] != -1) {
        $se .= sprintf("E%02d", $cartoon["Episode"]);
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
        if(!empty($character["AltName"])) {
            $name = $character["AltName"];
        } else {
            $name = $character["Name"];
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

        $n = str_replace(" - ", "", $character["Name"]);
        $n = str_replace("-", "", $n);

        $ch .= sprintf($li, $class, get_icon($character), $a, ucwords($n), $n);
    }

    foreach($cartoons as $cartoon) {
        if($cartoon["Service"] == "youtube") {
            $image = $cartoon["Thumbnails"]["Large"];
        } else {
            $image = $cartoon["Thumbnails"]["Medium"];
        }

        $se = get_season($cartoon);
        $li = <<<EOF
            <li>
                <div class="image"><a class="media" href="%s"><img class="thumb" data-original="%s" width="240" height="180"/><div class="play"></div></a></div>
                <div class="text">%s%s</div>
            </li>\n
EOF;
        $ca .= sprintf($li, $cartoon["Url"], $image, $cartoon["FormattedTitle"], $se);
    }

    $tpl = str_replace("{TITLE}", "Crtaći / " . ucwords($query), $tpl);
    $tpl = str_replace("{CHARACTERS}", $ch, $tpl);
    $tpl = str_replace("{CARTOONS}", $ca, $tpl);
    return $tpl;

}
