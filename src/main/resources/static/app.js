(() => {
globalThis.JUSIC_API_BASE = "";
/******************************************************************************
Copyright (c) Microsoft Corporation.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
***************************************************************************** */

var __assign = function() {
    __assign = Object.assign || function __assign(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p)) t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};

typeof SuppressedError === "function" ? SuppressedError : function (error, suppressed, message) {
    var e = new Error(message);
    return e.name = "SuppressedError", e.error = error, e.suppressed = suppressed, e;
};

// @ts-nocheck
var defaultBalloonColor = "#84A332";
var defaultLightColor = "#C0F381";
var balloonColorProperty = "--balloon-color";
var lightColorProperty = "--light-color";
var widthProperty = "--balloon-width";
var heightProperty = "--balloon-height";
var balloonDefaultSize = {
    width: 233,
    height: 609,
};
var createBallonElement = function (_a) {
    var balloonColor = _a.balloonColor, lightColor = _a.lightColor, width = _a.width;
    var balloon = document.createElement("balloon");
    balloon.innerHTML = balloonSvgHTML;
    Object.assign(balloon.style, {
        position: "absolute",
        overflow: "hidden",
        top: "0",
        left: "0",
        display: "inline-block",
        isolation: "isolate",
        transformStyle: "preserve-3d",
        backfaceVisibility: "hidden",
        opacity: "0.001",
        transform: "translate(calc(-100% + 1px), calc(-100% + 1px))",
        contain: "style, layout, paint",
        transformOrigin: "".concat(width / 2, "px ").concat(width / 2, "px"),
        willChange: "transform", // Improves rendering performance in Safari
    });
    balloon.style.setProperty(balloonColorProperty, balloonColor);
    balloon.style.setProperty(lightColorProperty, lightColor);
    balloon.style.setProperty(widthProperty, width + "px");
    balloon.style.setProperty(heightProperty, (width * 609) / 223 + "px");
    return balloon;
};
var balloonSvgHTML = "\n<svg\n\nstyle=\"width: var(".concat(widthProperty, "); height: var(").concat(heightProperty, ");\"\nviewBox=\"0 0 223 609\"\nfill=\"none\"\nxmlns=\"http://www.w3.org/2000/svg\"\n>\n<g opacity=\"0.8\" filter=\"url(#filter0_f_102_49)\" >\n  <path\n    d=\"M117.5 253C136.167 294.5 134.7 395 125.5 453C116.3 511 133.833 578.167 125.5 606\"\n    stroke=\"url(#paint0_linear_102_49)\"\n    stroke-width=\"2\"\n  />\n</g>\n<g opacity=\"0.85\" filter=\"url(#filter1_ii_102_49)\">\n  <path\n    fill-rule=\"evenodd\"\n    clip-rule=\"evenodd\"\n    d=\"M176.876 204.032C181.934 198.064 209.694 160.262 210.899 127.619C213.023 70.1236 176.876 13 118.337 13C55.7949 13 18.5828 69.332 22.2724 127.619C24.0956 156.423 38.9766 178.5 51.7922 195.372C57.7811 203.257 90.0671 238.749 112.15 245.044C111.698 248.246 112.044 253.284 116.338 254H121.838V245.71C143.277 242.292 172.085 209.686 176.876 204.032Z\"\n    fill=\"var(").concat(balloonColorProperty, ", ").concat(defaultBalloonColor, ")\"\n  />\n</g>\n<g filter=\"url(#filter2_f_102_49)\">\n  <path\n    d=\"M125 256.5C125 258.433 122.09 260 118.5 260C114.91 260 112 258.433 112 256.5C112 254.567 114.91 255 118.5 255C122.09 255 125 254.567 125 256.5Z\"\n    fill=\"var(").concat(balloonColorProperty, ", ").concat(defaultBalloonColor, ")\"\n  />\n</g>\n<g opacity=\"0.2\" filter=\"url(#filter3_f_102_49)\">\n  <path\n    d=\"M178.928 128.12C178.011 152.146 172.137 162.97 154.623 184.2C141.594 199.992 128.28 215 112.805 215C104.349 215 92.739 215 65.2673 177.844C56.1123 165.461 45.4818 149.259 44.1794 128.12C41.5436 85.3424 68.1267 44 112.805 44C154.623 44 180.55 85.6242 178.928 128.12Z\"\n    fill=\"url(#paint1_radial_102_49)\"\n  />\n</g>\n<g\n  style=\"mix-blend-mode: lighten\"\n  opacity=\"0.7\"\n  filter=\"url(#filter4_df_102_49)\"\n>\n  <path\n    d=\"M72.7992 108.638L74.0985 87.5247C74.3145 84.0152 77.4883 81.4427 80.9664 81.958L94.8619 84.0166C98.4018 84.541 100.699 88.0277 99.7828 91.4871L94.0502 113.144C93.1964 116.369 89.8758 118.278 86.659 117.394L77.1969 114.792C74.4599 114.039 72.6249 111.471 72.7992 108.638Z\"\n    fill=\"var(").concat(lightColorProperty, ", ").concat(defaultLightColor, ")\"\n  />\n</g>\n<g\n  style=\"mix-blend-mode: lighten\"\n  opacity=\"0.5\"\n  filter=\"url(#filter5_f_102_49)\"\n>\n  <path\n    d=\"M147.76 88.7366L144.842 67.9855C144.378 64.687 141.316 62.3976 138.021 62.8858L123.638 65.0166C120.098 65.541 117.801 69.0277 118.717 72.4871L124.462 94.1891C125.311 97.3967 128.602 99.3061 131.808 98.4512L143.364 95.3695C146.296 94.5878 148.182 91.7409 147.76 88.7366Z\"\n    fill=\"var(").concat(lightColorProperty, ", ").concat(defaultLightColor, ")\"\n  />\n</g>\n<g style=\"mix-blend-mode: lighten\" filter=\"url(#filter6_f_102_49)\">\n  <path\n    d=\"M46.4087 131.164C38.1642 111.726 43.2454 91.2599 47.4381 82.0988C47.7504 81.4164 48.5574 80.8601 48.8712 81.5418C48.9711 81.7589 48.9188 82.1169 48.8357 82.3409C41.2341 102.832 45.5154 122.958 47.3397 130.925C47.8434 133.124 47.2898 133.242 46.4087 131.164Z\"\n    fill=\"var(").concat(lightColorProperty, ", ").concat(defaultLightColor, ")\"\n  />\n</g>\n<g style=\"mix-blend-mode: lighten\" filter=\"url(#filter7_f_102_49)\">\n  <path\n    d=\"M46.4087 131.164C38.1642 111.726 43.2454 91.2599 47.4381 82.0988C47.7504 81.4164 48.5574 80.8601 48.8712 81.5418C48.9711 81.7589 48.9188 82.1169 48.8357 82.3409C41.2341 102.832 45.5154 122.958 47.3397 130.925C47.8434 133.124 47.2898 133.242 46.4087 131.164Z\"\n    fill=\"var(").concat(lightColorProperty, ", ").concat(defaultLightColor, ")\"\n  />\n</g>\n<g opacity=\"0.3\">\n  <g style=\"mix-blend-mode: lighten\" filter=\"url(#filter8_f_102_49)\">\n    <path\n      d=\"M190.817 150.078C196.906 136.754 196.503 119.258 195.396 111.05C195.318 110.475 194.888 109.925 194.734 110.403C194.704 110.495 194.689 110.697 194.699 110.807C196.396 129.344 191.942 144.593 190.447 149.824C190.122 150.959 190.349 151.104 190.817 150.078Z\"\n      fill=\"var(").concat(lightColorProperty, ", ").concat(defaultLightColor, ")\"\n    />\n  </g>\n  <g style=\"mix-blend-mode: lighten\" filter=\"url(#filter9_f_102_49)\">\n    <path\n      d=\"M190.817 150.078C196.906 136.754 196.503 119.258 195.396 111.05C195.318 110.475 194.888 109.925 194.734 110.403C194.704 110.495 194.689 110.697 194.699 110.807C196.396 129.344 191.942 144.593 190.447 149.824C190.122 150.959 190.349 151.104 190.817 150.078Z\"\n      fill=\"var(").concat(lightColorProperty, ", ").concat(defaultLightColor, ")\"\n    />\n  </g>\n</g>\n</svg>\n");
var svgFiltersHtml = "\n<svg>\n  <defs>\n    <filter\n      id=\"filter0_f_102_49\"\n      x=\"114.588\"\n      y=\"250.59\"\n      width=\"20.5082\"\n      height=\"357.697\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"1\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter1_ii_102_49\"\n      x=\"22.0213\"\n      y=\"13\"\n      width=\"188.967\"\n      height=\"241\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feColorMatrix\n        in=\"SourceAlpha\"\n        type=\"matrix\"\n        values=\"0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0\"\n        result=\"hardAlpha\"\n      />\n      <feOffset />\n      <feGaussianBlur stdDeviation=\"4.5\" />\n      <feComposite in2=\"hardAlpha\" operator=\"arithmetic\" k2=\"-1\" k3=\"1\" />\n      <feColorMatrix\n        type=\"matrix\"\n        values=\"0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 0.4 0\"\n      />\n      <feBlend\n        mode=\"normal\"\n        in2=\"shape\"\n        result=\"effect1_innerShadow_102_49\"\n      />\n      <feColorMatrix\n        in=\"SourceAlpha\"\n        type=\"matrix\"\n        values=\"0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0\"\n        result=\"hardAlpha\"\n      />\n      <feOffset />\n      <feGaussianBlur stdDeviation=\"18\" />\n      <feComposite in2=\"hardAlpha\" operator=\"arithmetic\" k2=\"-1\" k3=\"1\" />\n      <feColorMatrix\n        type=\"matrix\"\n        values=\"0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 1 0\"\n      />\n      <feBlend\n        mode=\"overlay\"\n        in2=\"effect1_innerShadow_102_49\"\n        result=\"effect2_innerShadow_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter2_f_102_49\"\n      x=\"111\"\n      y=\"253.959\"\n      width=\"15\"\n      height=\"7.04138\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"0.5\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter3_f_102_49\"\n      x=\"0\"\n      y=\"0\"\n      width=\"223\"\n      height=\"259\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"22\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter4_df_102_49\"\n      x=\"46.7878\"\n      y=\"59.8922\"\n      width=\"79.1969\"\n      height=\"87.7179\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feColorMatrix\n        in=\"SourceAlpha\"\n        type=\"matrix\"\n        values=\"0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0\"\n        result=\"hardAlpha\"\n      />\n      <feOffset dy=\"4\" />\n      <feGaussianBlur stdDeviation=\"13\" />\n      <feComposite in2=\"hardAlpha\" operator=\"out\" />\n      <feColorMatrix\n        type=\"matrix\"\n        values=\"0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 0.8 0\"\n      />\n      <feBlend\n        mode=\"overlay\"\n        in2=\"BackgroundImageFix\"\n        result=\"effect1_dropShadow_102_49\"\n      />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"effect1_dropShadow_102_49\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"5.5\"\n        result=\"effect2_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter5_f_102_49\"\n      x=\"102.515\"\n      y=\"46.8202\"\n      width=\"61.3035\"\n      height=\"67.8351\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"8\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter6_f_102_49\"\n      x=\"34\"\n      y=\"73.2313\"\n      width=\"22.9258\"\n      height=\"67.4198\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"4\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter7_f_102_49\"\n      x=\"40\"\n      y=\"79.2313\"\n      width=\"10.9258\"\n      height=\"55.4198\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"1\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter8_f_102_49\"\n      x=\"186.419\"\n      y=\"106.345\"\n      width=\"13.5106\"\n      height=\"48.2987\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"1.93775\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <filter\n      id=\"filter9_f_102_49\"\n      x=\"189.326\"\n      y=\"109.252\"\n      width=\"7.69731\"\n      height=\"42.4855\"\n      filterUnits=\"userSpaceOnUse\"\n      color-interpolation-filters=\"sRGB\"\n    >\n      <feFlood flood-opacity=\"0\" result=\"BackgroundImageFix\" />\n      <feBlend\n        mode=\"normal\"\n        in=\"SourceGraphic\"\n        in2=\"BackgroundImageFix\"\n        result=\"shape\"\n      />\n      <feGaussianBlur\n        stdDeviation=\"0.484439\"\n        result=\"effect1_foregroundBlur_102_49\"\n      />\n    </filter>\n    <linearGradient\n      id=\"paint0_linear_102_49\"\n      x1=\"124.798\"\n      y1=\"253\"\n      x2=\"124.798\"\n      y2=\"606\"\n      gradientUnits=\"userSpaceOnUse\"\n    >\n      <stop stop-color=\"white\" />\n      <stop offset=\"0.474934\" stop-color=\"grey\" stop-opacity=\"0.1\" />\n      <stop offset=\"0.722707\" stop-color=\"white\" stop-opacity=\"0.6\" />\n      <stop offset=\"0.93469\" stop-color=\"grey\" stop-opacity=\"0.7\" />\n      <stop offset=\"1\" stop-color=\"white\" stop-opacity=\"0\" />\n    </linearGradient>\n    <radialGradient\n      id=\"paint1_radial_102_49\"\n      cx=\"0\"\n      cy=\"0\"\n      r=\"1\"\n      gradientUnits=\"userSpaceOnUse\"\n      gradientTransform=\"translate(134 149.5) rotate(-123.69) scale(82.9277 65.4692)\"\n    >\n      <stop />\n      <stop offset=\"1\" stop-opacity=\"0\" />\n    </radialGradient>\n  </defs>\n</svg>\n";

var easings = [
    // easeOutQuint
    "cubic-bezier(0.22, 1, 0.36, 1)",
    // easeOutCubic
    "cubic-bezier(0.33, 1, 0.68, 1)",
];
var colorPairs = [
    // yellow
    ["#ffec37ee", "#f8b13dff"],
    // red
    ["#f89640ee", "#c03940ff"],
    //blue
    ["#3bc0f0ee", "#0075bcff"],
    // green
    ["#b0cb47ee", "#3d954bff"],
    // purple
    ["#cf85b8ee", "#a3509dff"],
];
function createBalloonAnimation(_a) {
    var balloon = _a.balloon, x = _a.x, y = _a.y, z = _a.z, targetX = _a.targetX, targetY = _a.targetY, targetZ = _a.targetZ, zIndex = _a.zIndex;
    balloon.style.zIndex = zIndex.toString();
    // Add blur to the closes ballons for bokeh effect
    balloon.style.filter = "blur(".concat(zIndex > 7 ? 8 : 0, "px)");
    var getAnimation = function () {
        var tiltAngle = Math.random() * (15 - 8) + 8; // Random tilt angle between 8 and 15 degrees
        var tiltDirection = Math.random() < 0.5 ? 1 : -1; // Random tilt direction
        return balloon.animate([
            {
                transform: "translate(-50%, 0%) translate3d(".concat(x, "px, ").concat(y, "px, ").concat(z, "px) rotate3d(0, 0, 1, ").concat(tiltDirection * -tiltAngle, "deg)"),
                opacity: 1,
            },
            {
                transform: "translate(-50%, 0%) translate3d(".concat(x + (targetX - x) / 2, "px, ").concat(y + (y + targetY * 5 - y) / 2, "px, ").concat(z + (targetZ - z) / 2, "px) rotate3d(0, 0, 1, ").concat(tiltDirection * tiltAngle, "deg)"),
                opacity: 1,
                offset: 0.5,
            },
            {
                transform: "translate(-50%, 0%) translate3d(".concat(targetX, "px, ").concat(y + targetY * 5, "px, ").concat(targetZ, "px) rotate3d(0, 0, 1, ").concat(tiltDirection * -tiltAngle, "deg)"),
                opacity: 1,
            },
        ], {
            duration: (Math.random() * 1000 + 5000) * 5,
            easing: easings[Math.floor(Math.random() * easings.length)],
            delay: zIndex * 200,
        });
    };
    return { balloon: balloon, getAnimation: getAnimation };
}
function balloons() {
    return new Promise(function (resolve) {
        var balloonsContainer = document.createElement("balloons");
        Object.assign(balloonsContainer.style, {
            overflow: "hidden",
            position: "fixed",
            inset: "0",
            zIndex: "999",
            display: "inline-block",
            pointerEvents: "none",
            perspective: "1500px",
            perspectiveOrigin: "50vw 100vh",
            contain: "style, layout, paint",
        });
        document.documentElement.appendChild(balloonsContainer);
        var sceneSize = { width: window.innerWidth, height: window.innerHeight };
        // make balloon height relative to screen size for this nice bokeh/perspective effect
        var balloonHeight = Math.floor(Math.min(sceneSize.width, sceneSize.height) * 1);
        var balloonWidth = (balloonDefaultSize.width / balloonDefaultSize.height) * balloonHeight;
        var amount = Math.max(7, Math.round(window.innerWidth / (balloonWidth / 2)));
        // make max dist depend on number of balloons and their size for realistic effect
        // we dont want them to be too separated or too squeezed together
        var maxDist = Math.max((amount * balloonWidth) / 2, (balloonWidth / 2) * 10);
        var balloonPositions = [];
        for (var i = 0; i < amount; i++) {
            var x = Math.round(sceneSize.width * Math.random());
            // make sure balloons first render below the bottom of the screen
            var y = window.innerHeight;
            var z = Math.round(-1 * (Math.random() * maxDist));
            var targetX = Math.round(x + Math.random() * balloonWidth * 6 * (Math.random() > 0.5 ? 1 : -1));
            var targetY = -window.innerHeight;
            // balloons don't move in the Z direction
            var targetZ = z;
            balloonPositions.push({
                x: x,
                y: y,
                z: z,
                targetX: targetX,
                targetY: targetY,
                targetZ: targetZ,
            });
        }
        balloonPositions = balloonPositions.sort(function (a, b) { return a.z - b.z; });
        var closestBallonPosition = balloonPositions[balloonPositions.length - 1];
        balloonPositions[0];
        // console.log({ closestBallonPosition, farthestBallonPosition });
        balloonPositions = balloonPositions.map(function (pos) { return (__assign(__assign({}, pos), { z: pos.z - closestBallonPosition.z, targetZ: pos.z - closestBallonPosition.z })); });
        var filtersElement = document.createElement("div");
        filtersElement.innerHTML = svgFiltersHtml;
        balloonsContainer.appendChild(filtersElement);
        var currentZIndex = 1;
        var animations = balloonPositions.map(function (pos, index) {
            var colorPair = colorPairs[index % colorPairs.length];
            var balloon = createBallonElement({
                balloonColor: colorPair[1],
                lightColor: colorPair[0],
                width: balloonWidth,
            });
            balloonsContainer.appendChild(balloon);
            return createBalloonAnimation(__assign(__assign({ balloon: balloon }, pos), { zIndex: currentZIndex++ }));
        });
        // Wait a bit for the balloon prerender
        requestAnimationFrame(function () {
            var animationPromises = animations.map(function (_a) {
                var balloon = _a.balloon, getAnimation = _a.getAnimation;
                var a = getAnimation();
                return a.finished.then(function () {
                    balloon.remove();
                });
            });
            Promise.all(animationPromises).then(function () {
                balloonsContainer.remove();
                resolve();
            });
        });
    });
}



const MESSAGE_TYPES = new Set([
  'NOTICE', 'ONLINE', 'CHAT', 'PICK', 'MUSIC', 'SETTING_NAME', 'AUTH', 'AUTH_ROOT', 'AUTH_ADMIN',
  'SEARCH', 'SEARCH_PICTURE', 'VOLUMN', 'GOODMODEL', 'SEARCH_HOUSE', 'ENTER_HOUSE',
  'ENTER_HOUSE_START', 'ADD_HOUSE', 'ADD_HOUSE_START', 'SEARCH_SONGLIST', 'SEARCH_USER',
  'ANNOUNCEMENT', 'HOUSE_USER', 'CIRCLEMODEL', 'LISTMODEL', 'ROOM_STATE', 'DEFAULT_PLAYLIST', 'DEFAULT_PLAYLIST_CHANGED'
]);

const SOURCE_CODES = { '网易': 'wy', 'QQ': 'qq', '酷我': 'kw', '酷狗': 'kg', '咪咕': 'mg' };
const cleanNickname = (value = '') => {
  if (value == null) return '';
  const name = String(value).trim().replace(/(?:\(\*\))+$/g, '').trim();
  if (/^(?:null|undefined|anonymous)$/i.test(name)) return '';
  const ip = '(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)';
  if (new RegExp(`^${ip}$`).test(name)) return '';
  return name.replace(new RegExp(`(?:\\(${ip}\\))+$`), '').trim();
};

class JusicBackend {
  constructor(options = {}) {
    this.baseUrl = (options.baseUrl || this.resolveBaseUrl()).replace(/\/$/, '');
    this.listeners = new Map();
    this.socket = null;
    this.stomp = null;
    this.room = null;
    this.connected = false;
    this.manualClose = false;
    this.reconnectTimer = null;
    this.reconnectAttempts = 0;
    this.lastAuthCommand = null;
    this.lastSocketActivityAt = 0;
    this.resumePromise = null;
  }

  resolveBaseUrl() {
    const configured = new URLSearchParams(location.search).get('api');
    if (configured) return new URL(configured, location.href).href;
    if (typeof globalThis.JUSIC_API_BASE === 'string') {
      const packaged = globalThis.JUSIC_API_BASE.trim();
      return new URL(packaged || './', location.href).href;
    }
    return ['localhost', '127.0.0.1'].includes(location.hostname)
      ? 'http://localhost:8888'
      : 'https://tx.alang.run/api';
  }

  on(event, listener) {
    if (!this.listeners.has(event)) this.listeners.set(event, new Set());
    this.listeners.get(event).add(listener);
    return () => this.listeners.get(event)?.delete(listener);
  }

  emit(event, payload) {
    this.listeners.get(event)?.forEach((listener) => listener(payload));
    this.listeners.get('*')?.forEach((listener) => listener({ event, payload }));
  }

  async api(path, payload = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);
    try {
      const response = await fetch(`${this.baseUrl}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', AccessToken: 'token' },
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const result = await response.json();
      if (String(result.code) !== '20000') throw new Error(result.message || '服务器请求失败');
      return result.data;
    } catch (error) {
      if (error?.name === 'AbortError') throw new Error('服务器请求超时');
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }

  async getRooms() { return this.api('/house/search', {}); }
  async getRoom(id) { return this.api('/house/get', { id }); }
  async getMiniCode(id) { return this.api('/house/getMiniCode', { id }); }
  async createRoom(payload) { return this.api('/house/add', payload); }
  async verifyRoom(id, password = '') { return this.api('/house/enter', { id, password }); }

  adoptRoom(room) {
    this.room = { ...(this.room || {}), ...room };
  }

  async connect(room) {
    if (!window.SockJS || !window.Stomp) throw new Error('实时通信组件加载失败');
    await this.disconnect(true);
    this.room = { ...room };
    this.manualClose = false;
    this.emit('connection', { status: 'connecting', room: this.room });
    const query = new URLSearchParams({
      houseId: room.id || '', housePwd: room.password || '', connectType: room.connectType || 'enter'
    });
    const socketSessionId = Array.from({ length: 8 }, () => Math.floor(Math.random() * 36).toString(36)).join('');
    const socket = new window.SockJS(`${this.baseUrl}/server?${query}`, null, { sessionId: () => socketSessionId });
    this.sessionId = socketSessionId;
    const stomp = window.Stomp.over(socket);
    stomp.debug = () => {};
    // Disable STOMP heartbeat timers (outgoing/incoming = 0) so browser background
    // timer throttling when minimized does not trigger server-side ping timeout disconnects.
    stomp.heartbeat.outgoing = 0;
    stomp.heartbeat.incoming = 0;
    this.socket = socket;
    this.stomp = stomp;
    this.lastSocketActivityAt = Date.now();
    socket.addEventListener('message', (event) => {
      if (this.socket !== socket) return;
      this.lastSocketActivityAt = Date.now();
      this.handleRawMessage(event.data);
    });
    socket.addEventListener('close', (event) => { if (this.socket === socket) this.handleClose(event); });

    return new Promise((resolve, reject) => {
      let settled = false;
      const timeout = setTimeout(() => {
        if (settled) return;
        settled = true;
        if (this.socket === socket) { this.socket = null; this.stomp = null; }
        try { socket.close(); } catch (_) {}
        reject(new Error('连接房间超时'));
      }, 10000);
      socket.addEventListener('close', (event) => {
        if (settled) return;
        settled = true; clearTimeout(timeout);
        const duplicate = Number(event?.code) === 4001;
        const error = new Error(duplicate ? '此浏览器已有窗口进入听歌房' : '房间连接失败');
        if (duplicate) error.code = 'DUPLICATE_CLIENT';
        reject(error);
      }, { once: true });
      stomp.connect({}, () => {
        if (settled) return;
        settled = true;
        clearTimeout(timeout);
        this.connected = true;
        this.reconnectAttempts = 0;
        this.lastSocketActivityAt = Date.now();
        stomp.subscribe('/topic/chat', (response) => {
          try {
            const body = JSON.parse(response.body);
            if (body?.data || body?.message) this.emit('notice', body.data || body.message);
          } catch (_) { }
        });
        stomp.subscribe('/topic/music/order', () => { });
        this.emit('connection', { status: 'connected', room: this.room, sessionId: this.sessionId });
        const nickname = cleanNickname(localStorage.getItem('USER_NAME'));
        if (nickname) this.command('setting.name', { name: nickname }).catch(() => { });
        if (this.lastAuthCommand) {
          this.command(this.lastAuthCommand.type, this.lastAuthCommand.payload).catch(() => { });
        }
        resolve(this.room);
      }, (error) => {
        setTimeout(() => {
          if (settled) return;
          settled = true;
          clearTimeout(timeout);
          this.connected = false;
          this.emit('connection', { status: 'error', error });
          reject(new Error('房间连接失败'));
        }, 0);
      });
    });
  }

  async disconnect(manual = true) {
    this.manualClose = manual;
    clearTimeout(this.reconnectTimer);
    const stomp = this.stomp;
    const socket = this.socket;
    this.stomp = null;
    this.socket = null;
    this.connected = false;
    if (stomp?.connected) {
      await new Promise((resolve) => {
        let done=false;const finish=()=>{if(done)return;done=true;clearTimeout(timer);resolve();};const timer=setTimeout(finish,1200);
        try { stomp.disconnect(finish); } catch (_) { finish(); }
      });
    }
    try { socket?.close(); } catch (_) { }
    if (manual) this.emit('connection', { status: 'disconnected', room: this.room });
  }

  handleClose(event) {
    this.connected = false;
    if(Number(event?.code)===4001){
      this.manualClose = true;
      this.room = null;
      clearTimeout(this.reconnectTimer);
      this.emit('connection',{status:'duplicate',room:this.room,event});
      return;
    }
    this.emit('connection', { status: 'disconnected', room: this.room, event });
    if (this.manualClose || !this.room) return;
    clearTimeout(this.reconnectTimer);
    const wait = Math.min(8000, 800 * (2 ** this.reconnectAttempts++));
    this.emit('connection', { status: 'reconnecting', delay: wait, room: this.room });
    this.reconnectTimer = setTimeout(() => this.connect(this.room).catch(() => { }), wait);
  }

  ensureActiveConnection({ force = false } = {}) {
    if (!this.room || (this.manualClose && !this.connected && !force)) return Promise.resolve(false);
    const socketOpen = this.socket?.readyState === 1;
    const stompOpen = Boolean(this.stomp?.connected);
    const stale = Date.now() - this.lastSocketActivityAt > 20000;
    if (!force && !stale && this.connected && socketOpen && stompOpen) return Promise.resolve(true);
    if (this.resumePromise) return this.resumePromise;
    clearTimeout(this.reconnectTimer);
    const room = { ...this.room, connectType: 'enter' };
    this.resumePromise = this.connect(room)
      .then(() => true)
      .catch(() => false)
      .finally(() => { this.resumePromise = null; });
    return this.resumePromise;
  }

  handleRawMessage(source) {
    if (typeof source !== 'string') return;
    const lines = source.split('\n');
    const type = lines[0];
    if (!MESSAGE_TYPES.has(type)) return;
    let content;
    try { content = JSON.parse(lines[lines.length - 1]); } catch (_) { return; }
    const data = content?.data;
    const map = {
      DEFAULT_PLAYLIST:'default-playlist', DEFAULT_PLAYLIST_CHANGED:'default-playlist-changed', ROOM_STATE:'room-state', ONLINE:'online', CHAT:'chat', PICK:'queue', MUSIC:'music', VOLUMN:'volume',
      NOTICE:'notice-message', ANNOUNCEMENT:'announcement', SEARCH:'search-results',
      SEARCH_SONGLIST:'playlist-results', SEARCH_USER:'user-results',
      SEARCH_HOUSE:'rooms', SEARCH_PICTURE:'picture-results', HOUSE_USER:'members',
      SETTING_NAME:'nickname', AUTH_ADMIN:'auth-admin', AUTH_ROOT:'auth-root',
      GOODMODEL:'good-mode', CIRCLEMODEL:'circle-mode', LISTMODEL:'list-mode',
      ADD_HOUSE:'room-created', ADD_HOUSE_START:'room-create-start', ENTER_HOUSE:'room-entered'
    };
    this.emit(map[type] || type.toLowerCase(), { ...content, data });
  }

  requireConnection() {
    if (!this.connected || !this.stomp?.connected) throw new Error('请先进入并连接房间');
  }

  send(destination, body = '') {
    this.requireConnection();
    this.stomp.send(destination, {}, typeof body === 'string' ? body : JSON.stringify(body));
    return Promise.resolve({ ok: true, destination });
  }

  command(type, payload = {}) {
    const source = SOURCE_CODES[payload.source] || payload.source || 'wy';
    const now = Date.now();
    if (type === 'auth.admin' || type === 'auth.root') {
      this.lastAuthCommand = { type, payload: { ...payload } };
    }
    const commands = {
      'chat.send': ['/chat', { content: payload.text, sendTime: now }],
      'chat.notice': ['/chat/announce', { content: payload.text }],
      'chat.globalNotice': [`/chat/notice/${String(payload.text || '')}`, ''],
      'chat.contactAdmin': ['/mail/send', { content: payload.text, sendTime: now }],
      'chat.picture': ['/chat', { content: `picture:${payload.url}`, sendTime: now }],
      'picture.search': ['/chat/picture/search', { content: payload.keyword || '', pageSize: payload.pageSize || 12, pageIndex: payload.pageIndex || 1, sendTime: now }],
      'music.skipVote': ['/music/skip/vote', {}],
      'music.search': ['/music/search', { name: payload.keyword || '', source, pageIndex: payload.pageIndex || 1, pageSize: payload.pageSize || 20, sendTime: now }],
      'music.pick': ['/music/pick', { name: payload.name, id: payload.id, source, quality: payload.quality || '320k', sendTime: now }],
      // 旧版删除面板始终把输入框中的歌曲 ID 作为字符串发送；后端的点歌归属表也按字符串键匹配。
      'music.delete': ['/music/delete', { id: String(payload.id ?? ''), sendTime: now }],
      'music.top': ['/music/top', { id: payload.id, sendTime: now }],
      'music.good': [`/music/good/${payload.id}`, {}],
      'music.clear': ['/music/clear', ''],
      'player.volume': [`/music/volumn/${payload.value}`, ''],
      'setting.name': ['/setting/name', { name: cleanNickname(payload.name), sendTime: now }],
      'house.create': ['/house/add', { name: payload.name, desc: payload.desc, needPwd: Boolean(payload.needPwd), password: payload.password || '', enableStatus: Boolean(payload.enableStatus), retainKey: payload.retainKey || '' }],
      'house.members': ['/house/houseuser', {}],
      'playlist.search': ['/music/searchsonglist', { name: payload.keyword || '', source, pageIndex: payload.pageIndex || 1, pageSize: payload.pageSize || 20, sendTime: now }],
      'user.search': ['/music/searchuser', { nickname: payload.keyword || '', source, pageIndex: payload.pageIndex || 1, pageSize: payload.pageSize || 20, sendTime: now }],
      'playlist.default': ['/music/defaultPlaylist', { pageIndex: payload.pageIndex || 1, pageSize: payload.pageSize || 30, requestId: payload.requestId }],
      'playlist.append': ['/music/setDefaultPlaylist', { id: payload.id, source }],
      'house.enter': ['/house/enter', { houseId: payload.id || payload.houseId, housePwd: payload.password || payload.housePwd || '', id: payload.id || payload.houseId, password: payload.password || payload.housePwd || '', sendTime: now }],
      'auth.admin': ['/auth/admin', { password: payload.password, sendTime: now }],
      'auth.root': ['/auth/root', { password: payload.password, sendTime: now }],
      'auth.adminPassword': [`/auth/adminpwd/${encodeURIComponent(payload.password)}`, ''],
      'blacklist.music': ['/music/blackmusic', (payload.musicId || payload.id) ? { musicId: payload.musicId || payload.id, musicName: payload.musicName || payload.name || '', sendTime: now } : ''],
      'blacklist.user': ['/chat/blackuser', (payload.userId || payload.id) ? { userId: payload.userId || payload.id, sendTime: now } : ''],
      'admin.vote': [`/music/vote/${payload.value}`, ''],
      'admin.pickWait': [`/music/pickwait/${payload.minutes}`, ''],
      'admin.clearQueueOnLeave': [`/music/clearqueueonleave/${payload.enabled}`, ''],
      'admin.defaultPlaylistMode': [`/music/defaultplaylistmode/${payload.random}`, ''],
      'admin.raw': [payload.destination, payload.body || '']
    };
    const config = commands[type];
    if (!config) return Promise.reject(new Error(`未知命令：${type}`));
    return this.send(config[0], config[1]);
  }
}

function sortRoomsLikeLegacy(rooms = []) {
  return [...rooms].sort((left, right) => {
    const leftActive = Number(left?.population || left?.count || 0) > 0;
    const rightActive = Number(right?.population || right?.count || 0) > 0;
    if (leftActive !== rightActive) return leftActive ? -1 : 1;
    const leftLocked = Boolean(left?.needPwd ?? left?.locked);
    const rightLocked = Boolean(right?.needPwd ?? right?.locked);
    if (leftLocked !== rightLocked) return leftLocked ? 1 : -1;
    return Number(right?.population || right?.count || 0) - Number(left?.population || left?.count || 0);
  });
}

function playlistSourceFor(platform='wy',category='playlist') {
  const selectedPlatform=platform==='qq'?'qq':'wy';
  if(category==='radio')return selectedPlatform==='wy'?'wy_userdj':'qq';
  return category==='user'?`${selectedPlatform}_user`:selectedPlatform;
}

function normalizeDefaultPlaylistIds(value='') {
  return String(value).split(/[,，\s]+/).map((id)=>id.trim()).filter(Boolean).join(',');
}

const MASKED_IPV4_SOURCE='(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)\\.(?:\\d{1,3}|\\*)';
const RAW_MASKED_IPV4=new RegExp(`^${MASKED_IPV4_SOURCE}$`);
const WRAPPED_MASKED_IPV4=new RegExp(`\\((${MASKED_IPV4_SOURCE})\\)`,'g');
const MASKED_IPV4_SUFFIX=new RegExp(`((?:\\(${MASKED_IPV4_SOURCE}\\))+)$`);

function splitGeneratedIdentity(value=''){
  const name=String(value).trim().replace(/(?:\(\*\))+$/g,'').trim();
  if(RAW_MASKED_IPV4.test(name))return {base:'',ip:name,wrapped:false};
  const suffix=name.match(MASKED_IPV4_SUFFIX)?.[1] || '';
  if(!suffix)return null;
  const ips=[...suffix.matchAll(WRAPPED_MASKED_IPV4)].map((match)=>match[1]);
  if(!ips.length)return null;
  return {base:name.slice(0,-suffix.length).trim(),ip:ips.at(-1),wrapped:true};
}

function generatedIdentity(value = '') {
  if(value==null)return '';
  const identity=splitGeneratedIdentity(value);
  if(!identity)return '';
  return identity.base?`${identity.base}(${identity.ip})`:(identity.wrapped?`(${identity.ip})`:identity.ip);
}

function normalizedNickname(value = '') {
  if (value == null) return '';
  const name=String(value).trim().replace(/(?:\(\*\))+$/g, '').trim();
  if(/^(?:null|undefined|anonymous)$/i.test(name))return '';
  return generatedIdentity(name) || name;
}

function persistentNickname(value = '') {
  if(value==null)return '';
  const raw=String(value).trim().replace(/(?:\(\*\))+$/g,'').trim();
  const identity=splitGeneratedIdentity(raw);
  const name=identity?identity.base:normalizedNickname(raw);
  return /^(?:null|undefined|anonymous)$/i.test(name)?'':name;
}

function historyBelongsToUser(song = {}, nickname = '') {
  if (song.mine === true) return true;
  const current = normalizedNickname(nickname);
  const picker = normalizedNickname(song.nickName || song.picker || song.userName || '');
  return Boolean(current && picker && current === picker);
}

function formatHistoryTime(value) {
  if (!value) return '';
  const timestamp = Number.isFinite(Number(value)) ? Number(value) : Date.parse(value);
  if (!timestamp || Number.isNaN(timestamp)) return '';
  const date = new Date(timestamp < 1e11 ? timestamp * 1000 : timestamp);
  if (Number.isNaN(date.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  const now = new Date();
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());
  if (year === now.getFullYear()) {
    return `${month}-${day} ${hours}:${minutes}`;
  }
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function filterHistoryItems(items = [], { mode = 'all', nickname = '', query = '' } = {}) {
  const keyword = String(query).trim().toLowerCase();
  return items.filter((song) => {
    if (mode === 'mine' && !historyBelongsToUser(song, nickname)) return false;
    const picker = song.nickName || song.picker || song.userName || '';
    const timeStr = formatHistoryTime(song.pickTime || song.sendTime || song.pushTime);
    return `${song.name || ''} ${song.artist || ''} ${picker} ${timeStr}`.toLowerCase().includes(keyword);
  });
}

function isMusicChatMessage(message = {}) {
  const content = String(message.content ?? message.text ?? '');
  return message.category === 'music' || content.startsWith('点歌 ') || content === '点歌成功';
}

function filterChatMessages(messages = [], mode = 'all') {
  if (mode === 'all') return messages;
  if (mode === 'music') return messages.filter(isMusicChatMessage);
  if (mode === 'chat') return messages.filter((message) => !isMusicChatMessage(message));
  return messages;
}

function isSelfChatMessage(message = {}, { sessionId = '', nickname = '' } = {}) {
  if (message.me === true) return true;
  const currentSession = String(sessionId || '').trim();
  if (currentSession && message.sessionId) return String(message.sessionId).trim() === currentSession;
  const currentName = normalizedNickname(nickname);
  const messageName = normalizedNickname(message.name || message.nickName || '');
  return Boolean(currentName && messageName && currentName === messageName);
}

function sessionIdFromPickedQueue(queue = [], pickedIds = new Set(), nickname = '') {
  const ownIds = new Set([...pickedIds].map(String));
  const matches = (Array.isArray(queue) ? queue : []).filter((song) =>
    song?.sessionId && ((song?.id != null && ownIds.has(String(song.id))) || (song?.name && ownIds.has(String(song.name))))
  );
  const ownName = normalizedNickname(nickname);
  const namedMatches = ownName
    ? matches.filter((song) => persistentNickname(song.nickName || song.picker) === ownName || normalizedNickname(song.nickName || song.picker) === ownName)
    : matches;
  const sessions = [...new Set(namedMatches.map((song) => String(song.sessionId).trim()).filter(Boolean))];
  return sessions.length === 1 ? sessions[0] : '';
}

function matchesPendingPickChat(message = {}, pick = {}, now = Date.now()) {
  if (!pick || !Number.isFinite(pick.at) || now - pick.at < 0 || now - pick.at > 15000) return false;
  const content = String(message.content ?? message.text ?? '').trim();
  if (!/^点歌\s+/.test(content)) return false;
  const detail = content.replace(/^点歌\s+/, '').trim();
  const id = String(pick.id ?? '').trim();
  const name = String(pick.name ?? '').trim();
  return Boolean((id && (detail === id || detail.startsWith(`${id} `) || detail.includes(`《${id}》`))) ||
    (name && (detail === name || detail.startsWith(`${name} `) || detail.includes(`《${name}》`))));
}

function matchesPendingVoteChat(message = {}, vote = {}, identity = {}, now = Date.now()) {
  if (!vote || !Number.isFinite(vote.at) || now - vote.at < 0 || now - vote.at > 10000) return false;
  if (String(vote.roomId ?? '') !== String(identity.roomId ?? '')) return false;
  if (String(message.content ?? message.text ?? '').trim() !== '投票切歌') return false;
  const ownSession=String(identity.sessionId || '').trim();
  const incomingSession=String(message.sessionId || '').trim();
  if (ownSession && incomingSession && ownSession !== incomingSession) return false;
  const ownName=normalizedNickname(identity.nickname);
  const incomingName=normalizedNickname(message.nickName || message.name);
  if (ownName && incomingName && ownName !== incomingName) return false;
  return true;
}

function matchesPendingLikeChat(message = {}, like = {}, now = Date.now()) {
  if (!like || !Number.isFinite(like.at) || now - like.at < 0 || now - like.at > 10000) return false;
  const content = String(message.content ?? message.text ?? '').trim();
  if (!/(?:点赞|赞了|喜欢)/.test(content)) return false;
  const id = String(like.id ?? '').trim();
  const name = String(like.name ?? '').trim();
  if (!id && !name) return true;
  return Boolean((id && content.includes(id)) || (name && content.includes(name)) || /^(?:点赞|点赞成功)$/.test(content));
}

function chatIsNearBottom(scrollTop = 0, scrollHeight = 0, clientHeight = 0, threshold = 32) {
  return scrollHeight - clientHeight - scrollTop <= threshold;
}

function waitingQueueItems(queue = []) {
  return Array.isArray(queue) ? queue.slice(1) : [];
}

function extractUserIP(user = {}) {
  const direct = String(user.remoteAddress || user.userIp || user.ip || user.clientIp || '').trim();
  if (direct && direct !== '—' && direct !== 'null' && direct !== 'undefined') return direct;
  const raw = String(user.nickName || user.name || user.sessionId || user.userName || '').trim();
  const identity = splitGeneratedIdentity(raw);
  if (identity?.ip) return identity.ip;
  const ipMatch = raw.match(/(?:[0-9]{1,3}|\*)\.(?:[0-9]{1,3}|\*)\.(?:[0-9]{1,3}|\*)\.(?:[0-9]{1,3}|\*)/);
  return ipMatch ? ipMatch[0] : '';
}

function computeTopIPs(usersGroupObj = {}, topCount = 10) {
  const groups = usersGroupObj && typeof usersGroupObj === 'object' ? Object.entries(usersGroupObj) : [];
  const ipMap = new Map();

  for (const [roomLabel, users] of groups) {
    if (!Array.isArray(users)) continue;
    for (const user of users) {
      if (!user) continue;
      const ip = extractUserIP(user) || '未知 IP';
      if (!ipMap.has(ip)) {
        ipMap.set(ip, { ip, users: [], rooms: new Map() });
      }
      const entry = ipMap.get(ip);
      entry.users.push({ ...user, roomLabel });
      const currentRoomCount = entry.rooms.get(roomLabel) || 0;
      entry.rooms.set(roomLabel, currentRoomCount + 1);
    }
  }

  const allIPs = Array.from(ipMap.values()).map((item) => ({
    ip: item.ip,
    userCount: item.users.length,
    roomCount: item.rooms.size,
    users: item.users,
    rooms: Array.from(item.rooms.entries()).map(([label, count]) => ({ label, count }))
  }));

  allIPs.sort((a, b) => b.userCount - a.userCount || b.roomCount - a.roomCount);

  return allIPs.slice(0, topCount);
}

const escape = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const defaultSongKey = song => (song.source || 'wy') + ':' + String(song.id);
function defaultPlaylistRows(songs, favorites = []) {
  if (!songs.length) return '<div class="system-message">当前页没有默认歌曲</div>';
  return songs.map((song,index) => {
    const platform = song.source === 'qq' ? 'QQ' : '网易';
    const name = song.name || '歌曲 ID：' + song.id;
    const album = typeof song.album === 'string' ? song.album : song.album?.name;
    const duration = Number(song.duration);
    const time = duration > 0 ? Math.floor(duration/60000) + ':' + String(Math.floor(duration/1000)%60).padStart(2,'0') : '';
    const meta = [platform, song.artist, album, time].filter(Boolean).join(' · ');
    const picture = /^https?:\/\//i.test(song.pictureUrl || '') ? song.pictureUrl : '';
    const favorite = favorites.some(item => defaultSongKey(item) === defaultSongKey(song));
    return '<div class="result-row"><span class="result-art">' + (picture ? '<img loading="lazy" src="' + escape(picture) + '" alt="歌曲封面">' : '♪')
      + '</span><span><span class="song-title-inline"><strong title="' + escape(name) + '">' + escape(name) + '</strong>'
      + '<button class="song-id-copy compact" type="button" data-copy-song-id="' + escape(song.id) + '" aria-label="复制歌曲 ID ' + escape(song.id) + '" title="歌曲 ID：' + escape(song.id) + '，点击复制">ID</button></span><small>' + escape(meta) + '</small></span>'
      + '<span class="form-actions" style="margin:0"><button class="primary-button" type="button" data-command="default-pick" data-index="' + index + '">点歌</button>'
      + '<button class="secondary-button" type="button" data-command="default-favorite" data-index="' + index + '">' + (favorite ? '取消收藏' : '收藏') + '</button></span></div>';
  }).join('');
}

function describeRoomState(settings) {
  if (!settings) return '房间状态待同步';
  const rate = typeof settings.voteRate === 'number' && Number.isFinite(settings.voteRate)
    ? String(Math.round(settings.voteRate * 10000) / 100) + '%' : '待同步';
  const modes=[['goodModel','点赞排序'],['randomModel','随机播放'],['musicCircleModel','单曲循环'],['listCircleModel','列表循环']]
    .filter(([key])=>settings[key]===true).map(([,label])=>label);
  return '切歌率 ' + rate + ' · ' + (modes.length?modes.join(' · '):'普通播放')
    + ' · ' + (settings.enableSearch === true ? '允许点歌' : settings.enableSearch === false ? '禁止点歌' : '点歌待同步')
    + ' · ' + (settings.enableSwitch === true ? '允许切歌' : settings.enableSwitch === false ? '禁止切歌' : '切歌待同步')
    + ' · ' + (Number(settings.pickWaitMinutes)>0?'进房 '+Number(settings.pickWaitMinutes)+' 分钟可点歌':'点歌无需等待')
    + ' · ' + (settings.clearQueuedOnLeave===false?'离房保留点歌':'离房清理点歌')
    + ' · 默认列表' + (settings.defaultPlaylistRandom===false?'顺序播放':'随机不重复');
}

const OK = '20000';

class SystemAdminApi {
  constructor(baseUrl, fetcher = (...args) => globalThis.fetch(...args)) {
    this.baseUrl = String(baseUrl).replace(/\/$/, '');
    this.fetcher = fetcher;
    this.authorization = '';
  }

  get loggedIn() { return Boolean(this.authorization); }

  logout() { this.authorization = ''; }

  async login(username, password) {
    if (!username || !password) throw new Error('请输入系统用户名和密码');
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    const authorization = `Basic ${btoa(String.fromCharCode(...bytes))}`;
    const data = await this.request('/house/showRetainKey', 'POST', {}, authorization);
    this.authorization = authorization;
    return data;
  }

  async request(path, method = 'POST', body, authorization = this.authorization, includeResponse = false) {
    if (!authorization) throw new Error('请先登录系统管理员账号');
    const options = {
      method,
      headers: { Authorization: authorization, ...(method === 'POST' ? { 'Content-Type': 'application/json' } : {}) },
      ...(method === 'POST' ? { body: JSON.stringify(body ?? {}) } : {})
    };
    let response;
    const controller=new AbortController();
    options.signal=controller.signal;
    const timeout=setTimeout(()=>controller.abort(),12000);
    try { response = await this.fetcher(`${this.baseUrl}${path}`, options); }
    catch (error) {
      if(error?.name==='AbortError') throw new Error('系统接口请求超时，请检查后端地址和网络');
      throw new Error('系统接口连接失败，请检查后端地址、跨域和网络');
    } finally { clearTimeout(timeout); }
    if (response.status === 401 || response.status === 403) {
      if (authorization === this.authorization) this.logout();
      throw new Error('系统账号验证失败或无权限');
    }
    if (!response.ok) throw new Error(`系统接口返回 HTTP ${response.status}`);
    let result;
    try { result = await response.json(); }
    catch (_) { throw new Error('系统接口未返回有效 JSON'); }
    if (String(result.code) !== OK) throw new Error(result.message || '系统操作失败');
    return includeResponse ? result : result.data;
  }

  listRetainKeys() { return this.request('/house/showRetainKey', 'POST'); }
  addRetainKey(key) { return this.request(`/house/addRetainKey/${encodeURIComponent(key)}`, 'POST'); }
  removeRetainKey(key) { return this.request(`/house/removeRetainKey/${encodeURIComponent(key)}`, 'GET'); }
  getUsers(houseId = '') { return this.request('/user/get', 'POST', { houseId }); }
  setRoomLimit(size) { return this.request(`/house/setSize/${size}`, 'GET', undefined, this.authorization, true); }
  editRoom(changes) { return this.request('/house/edit', 'POST', changes); }
  setNeteaseCookie(cookie) { return this.request('/netease/setCookiePost', 'POST', { cookie }); }
}

function roomEditPayload(values, original = {}) {
  const id = String(values.id || '').trim();
  if (!id) throw new Error('请先选择房间');
  if (values.canDestroy === 'true') return { id, canDestroy: true };
  const payload = { id };
  const name = String(values.name || '').trim();
  const desc = String(values.desc || '').trim();
  if (!name || name.length > 33) throw new Error('房间名称不能为空且不能超过 33 个字符');
  if (desc.length > 133) throw new Error('房间描述不能超过 133 个字符');
  if (name !== original.name) payload.name = name;
  if (desc !== original.desc) payload.desc = desc;
  for (const field of ['needPwd', 'enableStatus', 'forbiddenModiPwd']) {
    if (values[field] === 'true' || values[field] === 'false') payload[field] = values[field] === 'true';
  }
  const password = String(values.password || '').trim();
  if (values.needPwd === 'true' && !password) throw new Error('启用房间密码时，请输入新密码');
  if (values.needPwd === 'false' && password) throw new Error('关闭房间密码时，请清空新密码输入框');
  if (password) {
    if (/[\s?%#&=+]/.test(password)) throw new Error('房间密码不能包含空格、?、%、#、&、=、+');
    payload.password = password;
  }
  const adminPwd = String(values.adminPwd || '').trim();
  if (adminPwd) payload.adminPwd = adminPwd;
  if (Object.keys(payload).length === 1) throw new Error('请先修改至少一项房间设置');
  if (values.canDestroy === 'false') payload.canDestroy = false;
  return payload;
}









const backend = new JusicBackend();
const systemAdminApi = new SystemAdminApi(backend.baseUrl);
const $ = (selector, scope = document) => scope.querySelector(selector);
const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];
const ICON_PATHS = {
  menu: 'M3 6h18v2H3V6zm0 5h18v2H3v-2zm0 5h18v2H3v-2z', graphic_eq: 'M7 18h2V6H7v12zm4 4h2V2h-2v20zm4-7h2V9h-2v6zm4 4h2V5h-2v14z',
  expand_more: 'm7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z', chevron_left:'m15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12l4.58-4.59z', chevron_right:'m8.59 16.59L10 18l6-6-6-6-1.41 1.41L13.17 12l-4.58 4.59z', search: 'M9.5 3a6.5 6.5 0 1 0 3.98 11.64L19.85 21 21 19.85l-6.36-6.37A6.5 6.5 0 0 0 9.5 3zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9z',
  ios_share: 'M12 2 8 6h3v7h2V6h3l-4-4zM5 10v10h14V10h-4v2h2v6H7v-6h2v-2H5z', album: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zm0 7a2 2 0 1 1 0 4 2 2 0 0 1 0-4z',
  favorite: 'M12 21s-7-4.35-9.33-8.5C.84 9.25 2.2 5.5 5.7 4.35 7.74 3.68 10 4.38 12 6.5c2-2.12 4.26-2.82 6.3-2.15 3.5 1.15 4.86 4.9 3.03 8.15C19 16.65 12 21 12 21z', favorite_border: 'M16.5 3.5c-1.74 0-3.41.81-4.5 2.09A6.02 6.02 0 0 0 7.5 3.5C4.42 3.5 2 5.92 2 9c0 4.72 5.44 8.96 10 12 4.56-3.04 10-7.28 10-12 0-3.08-2.42-5.5-5.5-5.5zM12 18.55C7.8 15.65 4 12.32 4 9a3.48 3.48 0 0 1 3.5-3.5c1.94 0 3.54 1.12 4.5 2.4.96-1.28 2.56-2.4 4.5-2.4A3.48 3.48 0 0 1 20 9c0 3.32-3.8 6.65-8 9.55z',
  history: 'M13 3a9 9 0 0 0-8.8 7H1l4 4 4-4H6.25A7 7 0 1 1 8 17.75l-1.42 1.43A9 9 0 1 0 13 3zm-1 5v5l4.25 2.52.75-1.23-3.5-2.04V8H12z', more_horiz: 'M6 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm6 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm6 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4z',
  lock: 'M17 8h-1V6a4 4 0 0 0-8 0v2H7a2 2 0 0 0-2 2v10h14V10a2 2 0 0 0-2-2zm-7-2a2 2 0 0 1 4 0v2h-4V6zm7 12H7v-8h10v8z', add: 'M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5z', help: 'M11 18h2v2h-2v-2zm1-16a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16zm0-14a4 4 0 0 0-4 4h2a2 2 0 1 1 2.5 1.94c-1.5.38-1.5 1.81-1.5 3.06h2c0-.72 0-1.14.75-1.44A4 4 0 0 0 12 6z',
  admin_panel_settings: 'M12 2 4 5v6c0 5.05 3.41 9.77 8 11 4.59-1.23 8-5.95 8-11V5l-8-3zm0 4a3 3 0 1 1 0 6 3 3 0 0 1 0-6zm0 13.9a7.7 7.7 0 0 1-5-4.1c.04-1.66 3.34-2.57 5-2.57 1.65 0 4.96.91 5 2.57a7.7 7.7 0 0 1-5 4.1z', pause: 'M6 5h4v14H6V5zm8 0h4v14h-4V5z', play_arrow: 'M8 5v14l11-7L8 5z',
  skip_next: 'M6 18 14.5 12 6 6v12zm10-12v12h2V6h-2z', thumb_up: 'M2 20h4V9H2v11zm20-10c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13.17 1 6.59 7.59A2 2 0 0 0 6 9v9c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-1z',
  volume_up: 'M3 9v6h4l5 5V4L7 9H3zm11.5 3a3.5 3.5 0 0 0-2-3.16v6.32a3.5 3.5 0 0 0 2-3.16zM12.5 3.23v2.06a7 7 0 0 1 0 13.42v2.06a9 9 0 0 0 0-17.54z', volume_off: 'M16.5 12a4.5 4.5 0 0 0-1.3-3.17l-1.42 1.42c.46.45.72 1.07.72 1.75 0 .26-.04.51-.11.75l1.52 1.52c.38-.68.59-1.45.59-2.27zM4.27 3 3 4.27 7.73 9H3v6h4l5 5v-6.73L16.73 18 18 16.73 4.27 3zM10 15.17 7.83 13H5v-2h2.73L10 13.27v1.9zm2-6.9L9.91 6.18 12 4v4.27zm4.77 7.68 1.45 1.45A8.93 8.93 0 0 0 21 12c0-3.87-2.47-7.17-5.93-8.43v2.06A7 7 0 0 1 19 12c0 1.48-.46 2.85-1.23 3.95z', tune: 'M3 17v2h6v-2H3zm0-12v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zm-6-8v-2h14V9H7V7H5v6h2z',
  arrow_upward: 'm4 12 1.41 1.41L11 7.83V20h2V7.83l5.59 5.58L20 12l-8-8-8 8z', arrow_back:'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.42-1.41L7.83 13H20v-2z', arrow_forward:'m12 4-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z', swipe:'M18.89 13.77 15.8 12.23a2 2 0 0 0-1.67.03l-1.17.58V7a2 2 0 0 0-4 0v10l-2.4-1.2a2 2 0 0 0-2.82 2.42L5.5 22h11.17a3 3 0 0 0 2.92-2.34l.62-2.76a3 3 0 0 0-1.32-3.13z', close: 'M18.3 5.71 12 12l6.3 6.29-1.41 1.42L10.59 13.41 4.29 19.71 2.88 18.3 9.17 12 2.88 5.71 4.29 4.29 10.59 10.59 16.89 4.29 18.3 5.71z',
  group: 'M16 11c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3 1.34 3 3 3zM8 11c1.66 0 3-1.34 3-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z', delete_sweep: 'M15 16h4v2h-4v-2zm0-8h7v2h-7V8zm0 4h6v2h-6v-2zM3 18a2 2 0 0 0 2 2h6V8H3v10zm2-8h4v8H5v-8zm5-5L9 4H5L4 5H1v2h12V5h-3z',
  sentiment_satisfied: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM8.5 8A1.5 1.5 0 1 1 7 9.5 1.5 1.5 0 0 1 8.5 8zm7 0A1.5 1.5 0 1 1 14 9.5 1.5 1.5 0 0 1 15.5 8zM12 18c-2.33 0-4.31-1.46-5.11-3.5h10.22C16.31 16.54 14.33 18 12 18z', image: 'M21 19V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14c0 1.1.9 2 2 2h14a2 2 0 0 0 2-2zM8.5 11.5 11 14.51 14.5 10l4.5 6H5l3.5-4.5z',
  send: 'M2 21 23 12 2 3v7l15 2-15 2v7z', music_note: 'M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z', queue_music: 'M3 10h11v2H3v-2zm0-4h11v2H3V6zm0 8h7v2H3v-2zm13-1.82V5h2v6.18A3 3 0 1 1 16 14v-1.82z',
  apps: 'M4 4h6v6H4V4zm10 0h6v6h-6V4zM4 14h6v6H4v-6zm10 0h6v6h-6v-6z', meeting_room: 'M19 19V5h-7V3h9v18h-9v-2h7zM3 4h10v16H3V4zm7 7H8v2h2v-2z', chat_bubble: 'M4 4h16v12H6l-4 4V6a2 2 0 0 1 2-2z',
  person: 'M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0 2c-4.42 0-8 2.24-8 5v2h16v-2c0-2.76-3.58-5-8-5z', shield: 'M12 2 4 5v6c0 5.05 3.41 9.77 8 11 4.59-1.23 8-5.95 8-11V5l-8-3z', block:'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM4 12c0-1.85.63-3.55 1.69-4.9L16.9 18.31A7.96 7.96 0 0 1 4 12zm14.31 4.9L7.1 5.69A7.96 7.96 0 0 1 18.31 16.9z', content_copy:'M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z',
  download: 'M11 3h2v10l4-4 1.4 1.4L12 17l-6.4-6.6L7 9l4 4V3zM5 19h14v2H5v-2z', upload: 'M11 21V11l-4 4-1.4-1.4L12 7l6.4 6.6L17 15l-4-4v10h-2zM5 3h14v2H5V3z', keyboard: 'M3 5h18v14H3V5zm2 2v10h14V7H5zm1 1h2v2H6V8zm3 0h2v2H9V8zm3 0h2v2h-2V8zm3 0h2v2h-2V8zm-9 4h2v2H6v-2zm3 0h8v2H9v-2z', live_tv: 'M9 2h6l-3 3-3-3zM3 6h18v14H3V6zm2 2v10h14V8H5z', grid_view: 'M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z', public: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm6.9 6h-3.05a15.7 15.7 0 0 0-1.38-3.56A8.04 8.04 0 0 1 18.9 8zM12 4c.83 1.2 1.46 2.54 1.82 4h-3.64A13.7 13.7 0 0 1 12 4zM4.26 14a8.2 8.2 0 0 1 0-4h3.43a16.5 16.5 0 0 0 0 4H4.26zm.84 2h3.05a15.7 15.7 0 0 0 1.38 3.56A8.04 8.04 0 0 1 5.1 16zM8.15 8H5.1a8.04 8.04 0 0 1 4.43-3.56A15.7 15.7 0 0 0 8.15 8zM12 20a13.7 13.7 0 0 1-1.82-4h3.64A13.7 13.7 0 0 1 12 20zm2.26-6H9.74a14.7 14.7 0 0 1 0-4h4.52a14.7 14.7 0 0 1 0 4zm.21 5.56A15.7 15.7 0 0 0 15.85 16h3.05a8.04 8.04 0 0 1-4.43 3.56zM16.31 14a16.5 16.5 0 0 0 0-4h3.43a8.2 8.2 0 0 1 0 4h-3.43z', fullscreen: 'M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zm-3-12v2h3v3h2V5h-5z', fullscreen_exit: 'M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z'
};
const icon = (name) => `<svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="${ICON_PATHS[name] || ICON_PATHS.apps}"></path></svg>`;
function hydrateIcons(scope = document) {
  $$('.material-symbols-rounded', scope).forEach((node) => { node.outerHTML = icon(node.textContent.trim()); });
  annotateButtons(scope);
}
const escapeHTML = (value) => String(value).replace(/[&<>'"]/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
const readJSON = (key, fallback) => { try { return JSON.parse(localStorage.getItem(key) || '') || fallback; } catch (_) { return fallback; } };
const initialNickname=persistentNickname(localStorage.getItem('USER_NAME') || '');
const initialHomeMode=localStorage.getItem('JUSIC_HOME_MODE')==='galaxy'?'galaxy':'simple';
const initialPlayerTheme=['light','night','black'].includes(localStorage.getItem('JUSIC_PLAYER_THEME'))?localStorage.getItem('JUSIC_PLAYER_THEME'):'light';
const storedSidebarCollapsed=localStorage.getItem('JUSIC_SIDEBAR_COLLAPSED');
const initialSidebarCollapsed=storedSidebarCollapsed===null?matchMedia('(max-width:1180px)').matches:storedSidebarCollapsed==='true';
if(initialNickname)localStorage.setItem('USER_NAME',initialNickname);else localStorage.removeItem('USER_NAME');
document.body.dataset.playerTheme=initialPlayerTheme;

function syncAdminUI() {
  const actions = $('#queue-settings-actions');
  if (actions) actions.hidden = !(state.isAdmin || state.isRoot);
  const blacklistCurrent=$('#current-song-blacklist');
  if(blacklistCurrent)blacklistCurrent.hidden=!(state.isAdmin && state.currentMusic?.id!=null);
}

const state = {
  playing: false,
  connected: false,
  connectionStatus: 'offline',
  roomId: '',
  duplicateConnection: false,
  roomPassword: '',
  requestedRoomId: '',
  requestedRoomPassword: '',
  sharedAutoplayPending: false,
  online: 0,
  chatHidden: false,
  sidebarCollapsed: initialSidebarCollapsed,
  chatExpanded: false,
  focusMode: false,
  goodMode: false,
  likedQueueIds: new Set(),
  roomState: null,
  roomAnnouncement: null,
  currentMusic: null,
  favorite: false,
  source: '网易',
  mobileView: 'player',
  messageFilter: 'all',
  mySessionId: '',
  lastSentMessage: null,
  room: '尚未进入房间',
  roomFilter: '',
  hideEmptyRooms: false,
  hideLockedRooms: false,
  hideUnlockedRooms: false,
  roomPage: 1,
  galaxyPage: 0,
  galaxyFilter: 'all',
  galaxyKeyword: '',
  homeMode: initialHomeMode,
  playerTheme: initialPlayerTheme,
  rooms: [],
  queue: [],
  ownPickedIds: new Set(),
  pendingPicks: [],
  pendingVote: null,
  pendingLikes: [],
  messages: [{ type: 'system', text: '正在连接一起听歌吧服务…' }],
  searchResults: [],
  searchKeyword: '',
  searchPage: 1,
  searchTotal: 0,
  searchLoading: false,
  defaultPlaylist: {songs:[],page:1,total:0,scope:'',loading:false,error:''},
  defaultRequestSeq: 0,
  hotResults: [],
  hotSource: 'wy',
  hotPage: 1,
  hotTotal: 0,
  hotLoading: false,
  playlistResults: [],
  playlistPage: 1,
  playlistTotal: 0,
  playlistLoading: false,
  playlistKind: 'playlist',
  playlistKeyword: '',
  playlistSource: 'wy',
  playlistView: 'search',
  adminPlaylistSource: 'wy',
  adminPlaylistStatus: '',
  pendingDefaultAppend: null,
  userResults: [],
  userSearchKeyword: '',
  userPage: 1,
  userTotal: 0,
  userLoading: false,
  pictureResults: [],
  picturePage: 1,
  pictureTotal: 0,
  pictureLoading: false,
  pictureKeyword: '',
  members: [],
  selectedUser: null,
  pendingRoom: null,
  awaitingRoomEntry: false,
  pendingQueueSnapshot: null,
  pendingCreatedRoom: null,
  lyrics: [],
  currentLyricIndex: -1,
  historyMode: 'all',
  historyFilter: '',
  favorites: Object.values(readJSON('collectMusic', {})),
  history: readJSON('pickHistory', []),
  nickname: initialNickname,
  countdownEndsAt: 0,
  isAdmin: false,
  isRoot: false,
  roomPage: 1,
  systemAdmin: {tab:'rooms',selectedRoomId:'',retainKeys:[],users:null,pendingRemovalKey:'',roomLimitResult:null,loading:false,selectedIp:'',userIpFilter:''},
  blacklistRequest: '',
  musicBlacklist: [],
  userBlacklist: [],
  roomDescription: '',
  miniCode: '', miniCodeError: false
};

const audio = $('#audio-player');
let playbackWakeLock=null;
const ROOM_LEASE_KEY='JUSIC_ACTIVE_ROOM_TAB_V1';
const roomTabId=globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`;
let roomLeaseTimer=null,roomLeaseStartedAt=0;
function readRoomLease(){try{return JSON.parse(localStorage.getItem(ROOM_LEASE_KEY) || 'null');}catch(_){return null;}}
function leasePrecedes(left={},right={}){return Number(left.startedAt || Infinity)<Number(right.startedAt || Infinity) || (Number(left.startedAt || Infinity)===Number(right.startedAt || Infinity) && String(left.owner || '')<String(right.owner || ''));}
function writeRoomLease(roomId){
  const lease={owner:roomTabId,roomId:String(roomId || ''),startedAt:roomLeaseStartedAt || Date.now(),expiresAt:Date.now()+45000};
  try{localStorage.setItem(ROOM_LEASE_KEY,JSON.stringify(lease));}catch(_){}
  return lease;
}
function refreshRoomLease(){const lease=readRoomLease();if(lease?.owner===roomTabId)writeRoomLease(state.roomId || lease.roomId);}
function claimRoomLease(roomId){
  const existing=readRoomLease(),now=Date.now();
  if(existing?.owner && existing.owner!==roomTabId && Number(existing.expiresAt)>now)return false;
  roomLeaseStartedAt=roomLeaseStartedAt || now;writeRoomLease(roomId);
  const claimed=readRoomLease()?.owner===roomTabId;
  if(claimed&&!roomLeaseTimer)roomLeaseTimer=setInterval(refreshRoomLease,10000);
  return claimed;
}
function releaseRoomLease(){
  clearInterval(roomLeaseTimer);roomLeaseTimer=null;
  try{if(readRoomLease()?.owner===roomTabId)localStorage.removeItem(ROOM_LEASE_KEY);}catch(_){}
  roomLeaseStartedAt=0;
}
let currentPanel = '';
let qrView = null;
function persistNickname(value){const name=persistentNickname(value);state.nickname=name;if(name)localStorage.setItem('USER_NAME',name);else localStorage.removeItem('USER_NAME');return name;}
const userDisplayName=(data={},fallback='匿名用户')=>normalizedNickname(data.nickName ?? data.nickname ?? data.userName ?? data.name) || String(data.sessionId || data.userId || data.uid || data.senderId || data.fromSessionId || data.id || fallback);
const isCurrentMember=(user={})=>Boolean(user.me===true || user.isMe===true || user.self===true || (state.mySessionId && String(user.sessionId || user.userId || user.id || '')===String(state.mySessionId)) || (state.nickname && normalizedNickname(user.nickName || user.nickname || user.name)===normalizedNickname(state.nickname)));
const sourceCode = (label) => ({ '网易':'wy','QQ':'qq','电台':'wydt','禁歌':'lz' }[label] || label || 'wy');
function updateSongSearchHint() {
  const input = $('#song-search');
  if (input) input.placeholder = sourceCode(state.source) === 'lz' ? '可留空浏览禁歌，或输入关键词' : '输入歌名、歌手或歌曲 ID';
}
const sourceLabel = (code) => ({wy:'网易',qq:'QQ',wydt:'电台',lz:'禁歌'}[code] || code || '网易');
const plantFor = (value) => ['🌿','🌵','🪴','🍀','🌱','🌳'][Math.abs(String(value).split('').reduce((a,c)=>a+c.charCodeAt(0),0))%6];
const ANIMAL_AVATARS=['🐼','🐻','🐰','🦊','🐱','🐶','🐨','🦁','🐯','🐸','🐵','🐧','🐮','🐷','🐹','🐭','🐺','🦄','🐙','🦝','🦦','🐿️','🦔','🐳'];
const stableUserHash=(value)=>{let hash=2166136261;for(const char of String(value || 'anonymous')){hash^=char.codePointAt(0);hash=Math.imul(hash,16777619);}return hash>>>0;};
const animalFor = (userId) => ANIMAL_AVATARS[stableUserHash(userId)%ANIMAL_AVATARS.length];
const chatUserId = (data={}) => String(data.sessionId || data.userId || data.uid || data.senderId || data.fromSessionId || data.id || data.nickName || 'anonymous');
const formatTime = (seconds) => {
  const safe = Number.isFinite(seconds) ? Math.max(0, seconds) : 0;
  return `${String(Math.floor(safe / 60)).padStart(2,'0')}:${String(Math.floor(safe % 60)).padStart(2,'0')}`;
};
function parseTimedLyrics(raw=''){
  const result=[]; String(raw).split('\n').forEach((line)=>{const matches=[...line.matchAll(/\[(\d+):(\d+(?:\.\d+)?)\]/g)];const text=line.replace(/\[[^\]]+\]/g,'').trim();matches.forEach((match)=>{if(text)result.push({time:Number(match[1])*60+Number(match[2]),text});});});return result.sort((a,b)=>a.time-b.time);
}
function syncFocusLyrics(index=-1){
  const empty=!state.lyrics.length;const current=empty?'暂无歌词':(state.lyrics[index]?.text || state.lyrics[0]?.text || '');
  $('#focus-previous-lyric').textContent=empty?'':(state.lyrics[index-1]?.text || '');$('#focus-current-lyric').textContent=current;$('#focus-next-lyric').textContent=empty?'':(state.lyrics[index+1]?.text || '');
}
function updateCurrentLyric(time){
  if(!state.lyrics.length){$('#current-lyric').textContent='暂无歌词';$('#next-lyric').textContent='';syncFocusLyrics();return;}
  let index=state.lyrics.findIndex((line,i)=>time>=line.time && (!state.lyrics[i+1] || time<state.lyrics[i+1].time));if(index<0)index=0;if(index===state.currentLyricIndex)return;state.currentLyricIndex=index;
  $('#current-lyric').textContent=state.lyrics[index]?.text || '';$('#next-lyric').textContent=state.lyrics[index+1]?.text || '';
  syncFocusLyrics(index);
  $$('#lyrics-view p').forEach((node,i)=>node.classList.toggle('current',i===index));const active=$(`#lyrics-view p[data-lyric-index="${index}"]`);const box=$('#lyrics-view');if(active&&box)box.scrollTo({top:Math.max(0,active.offsetTop-box.offsetTop-box.clientHeight/2),behavior:'smooth'});
}
const albumName = (album) => typeof album === 'string' ? album : (album?.name || '未知专辑');
const isFavorite = (id) => state.favorites.some((item) => String(item.id) === String(id));
const roomShareUrl = () => {
  const url = new URL(location.href);
  const api = url.searchParams.get('api');
  url.search = '';
  url.hash = '';
  if (api) url.searchParams.set('api', api);
  url.searchParams.set('houseId', String(state.roomId || ''));
  url.searchParams.set('housePwd', String(state.roomPassword || ''));
  return url.href;
};
const canManage = () => state.isAdmin || state.isRoot;
const roomAnnouncementCache = new Map();
function announcementFromRoom(room) {
  if (!room || typeof room !== 'object' || (!('announce' in room) && !('announcement' in room))) return null;
  const announcement = room.announce ?? room.announcement;
  return String(typeof announcement === 'object' && announcement !== null ? (announcement.content ?? '') : (announcement ?? ''));
}
function setRoomAnnouncement(content, options = {}) {
  const roomId = String(options.roomId ?? state.roomId ?? '');
  const source = options.source || 'room';
  const cached = roomAnnouncementCache.get(roomId);
  if (source === 'room' && cached && cached.source !== 'room') content = cached.content;
  if (source === 'room' && content === '' && cached?.content) content = cached.content;
  if (content == null) content = cached?.content ?? null;
  if (roomId && content != null) {
    roomAnnouncementCache.set(roomId, {content: String(content),source: source === 'room' && cached && cached.source !== 'room' ? cached.source : source});
    while(roomAnnouncementCache.size>100)roomAnnouncementCache.delete(roomAnnouncementCache.keys().next().value);
  }
  if (roomId !== String(state.roomId ?? '')) return;
  state.roomAnnouncement = content == null ? null : String(content);
  const input = $('#admin-announcement');
  if (input && document.activeElement !== input) input.value = state.roomAnnouncement ?? '';
}
async function refreshRoomAnnouncement() {
  if (!state.roomId) return;
  const roomId = String(state.roomId);
  try {
    const room = await backend.getRoom(state.roomId);
    if (String(state.roomId) !== roomId) return;
    const content = announcementFromRoom(Array.isArray(room) ? room[0] : room);
    if (content !== null) setRoomAnnouncement(content);
  } catch (_) { /* 房间列表或公告广播仍可提供回显。 */ }
}
const songIdButton = (id) => id == null || String(id).trim() === '' ? '' : `<button class="song-id-copy compact" type="button" data-copy-song-id="${escapeHTML(id)}" aria-label="复制歌曲 ID ${escapeHTML(id)}" title="歌曲 ID：${escapeHTML(id)}，点击复制">${icon('content_copy')}</button>`;
const playlistIdButton = (id) => id == null || String(id).trim() === '' ? '' : `<button class="song-id-copy compact" type="button" data-copy-playlist-id="${escapeHTML(id)}" aria-label="复制歌单 ID ${escapeHTML(id)}" title="歌单 ID：${escapeHTML(id)}，点击复制">${icon('content_copy')}</button>`;
const isEmptyBlacklistValue = (value) => /^(?:无|暂无(?:数据|记录|拉黑列表|黑名单|用户黑名单|音乐黑名单|歌曲黑名单)|暂无任何(?:拉黑记录|黑名单数据)|无(?:数据|记录|拉黑列表|黑名单))$/.test(String(value || '').trim());
const songTitleHTML = (name, id) => `<span class="song-title-inline"><strong title="${escapeHTML(name || '未知歌曲')}">${escapeHTML(name || '未知歌曲')}</strong>${songIdButton(id)}</span>`;
const ROOMS_BATCH_SIZE = 30;
const visibleRooms = () => state.rooms.filter((room)=>(!state.hideEmptyRooms || Number(room.count)>0) && (!state.hideLockedRooms || !room.locked) && (!state.hideUnlockedRooms || room.locked) && `${room.name} ${room.desc}`.toLowerCase().includes(state.roomFilter.toLowerCase()));
const roomResultsHTML = () => {
  const rooms = visibleRooms();
  if (!rooms.length) return '<div class="system-message">没有匹配的房间</div>';
  const slice = rooms.slice(0, (state.roomPage || 1) * ROOMS_BATCH_SIZE);
  const rows = slice.map((room) => { const current=String(room.id)===String(state.roomId);const meta=`${room.desc} · ${room.count} 人`; return `<div class="result-row room-result-card"><span class="result-art">${plantFor(room.id)}</span><span><strong title="${escapeHTML(room.name)}">${escapeHTML(room.name)}${room.locked ? ' 🔒' : ''}</strong><small title="${escapeHTML(meta)}">${escapeHTML(meta)}</small></span><button class="${current?'secondary-button':'primary-button'}" type="button" data-enter-room="${room.id}" ${current?'disabled':''}>${current?'当前房间':'进入'}</button></div>`; }).join('');
  const remaining = rooms.length - slice.length;
  const hint = remaining > 0 ? `<div class="system-message compact-hint" id="room-load-more-hint">向下滑动自动加载剩余 ${remaining} 个房间</div>` : '';
  return rows + hint;
};
const filteredHistory = () => filterHistoryItems(state.history,{mode:state.historyMode,nickname:state.nickname,query:state.historyFilter});
const historyResultsHTML = () => filteredHistory().map((song) => {
  const timeStr = formatHistoryTime(song.pickTime || song.sendTime || song.pushTime || song.createTime);
  const user = userDisplayName({ ...song, nickName: song.nickName || song.picker }, '历史记录');
  const meta = [song.artist || '', user, timeStr].filter(Boolean).join(' · ');
  return resultRow(song.name, meta, '点歌', 'pick-song', { id: song.id, source: song.source || 'wy' });
}).join('') || `<div class="system-message">${state.historyMode==='mine'&&!state.nickname?'请先在个人设置中填写昵称':'没有匹配的历史记录'}</div>`;
function syncHistoryPanel(){
  if(currentPanel!=='history')return;
  const songs=filteredHistory(),box=$('#history-results'),count=$('#history-count');
  if(box)box.innerHTML=historyResultsHTML();
  if(count)count.textContent=`当前 ${songs.length} 首`;
}
const favoriteResultsHTML = () => state.favorites.map((song)=>`<div class="result-row"><span class="result-art">${coverOf(song)?`<img src="${escapeHTML(coverOf(song))}" alt="${escapeHTML(song.name || '歌曲')}封面" loading="lazy" decoding="async">`:plantFor(song.id)}</span><span>${songTitleHTML(song.name,song.id)}<small>${escapeHTML(`${song.artist || '未知歌手'} · ${albumName(song.album)}`)}</small></span><span class="form-actions" style="margin:0"><button class="primary-button" type="button" data-command="pick-song" data-name="${escapeHTML(song.name)}" data-artist="${escapeHTML(song.artist || '')}" data-id="${escapeHTML(song.id)}" data-source="${escapeHTML(song.source || 'wy')}">播放</button><button class="secondary-button" type="button" data-command="remove-favorite" data-id="${escapeHTML(song.id)}">取消收藏</button></span></div>`).join('') || '<div class="system-message">暂无收藏歌曲</div>';
const coverOf = (item={}) => item.pictureUrl || item.picture_url || item.picUrl || item.coverUrl || item.cover || item.album?.picUrl || item.album?.pictureUrl || item.album?.picture_url || item.al?.picUrl || '';
const coverCache = new Map();
const coverRequests = new Map();
async function fetchCoverUrl(key){
  if(coverCache.has(key))return coverCache.get(key);
  if(coverRequests.has(key))return coverRequests.get(key);
  const request=(async()=>{
    const controller=new AbortController();const timeout=setTimeout(()=>controller.abort(),10000);
    try{const response=await fetch(`https://api.injahow.cn/meting/?server=netease&type=song&id=${encodeURIComponent(key)}`,{signal:controller.signal});if(!response.ok)return '';const data=await response.json();const url=data?.[0]?.pic || '';if(url)coverCache.set(key,url);return url;}
    catch(_){return '';}
    finally{clearTimeout(timeout);coverRequests.delete(key);}
  })();
  coverRequests.set(key,request);return request;
}
async function resolveMissingCovers(items=[],source='wy'){
  if(coverCache.size>300)coverCache.clear();
  if(source!=='wy')return;
  const missing=items.filter((song)=>song?.id && !coverOf(song));
  for(let start=0;start<missing.length;start+=4){
    await Promise.allSettled(missing.slice(start,start+4).map(async(song)=>{
      const key=String(song.id);const url=await fetchCoverUrl(key);
      if(!url)return;song.picture_url=url;const target=$(`[data-cover-id="${CSS.escape(key)}"]`);if(target&&!target.querySelector('img')){const img=document.createElement('img');img.loading='lazy';img.decoding='async';img.src=url;img.alt=`${song.name || '歌曲'}封面`;target.replaceChildren(img);}
    }));
  }
}
const totalHTML = (shown,total,label='条结果') => `<div class="list-summary"><span>已显示 <b>${shown}</b> ${label}</span><span>共 <b>${Number(total ?? shown)}</b> 条</span></div>`;
const roomTotalHTML = () => `<div class="list-summary room-total-summary">共 <b id="room-total-count">${visibleRooms().length}</b> 个房间</div>`;
const blacklistHTML = (kind) => { const list=kind==='music'?state.musicBlacklist:state.userBlacklist;if(!list.length)return `<div class="system-message">${state.blacklistRequest===kind?'正在向服务器读取…':'无'}</div>`;return list.map((item,index)=>{const value=typeof item==='object'?(item.id || item.sessionId || item.musicId || item.name || JSON.stringify(item)):item;return `<div class="result-row blacklist-row"><span class="result-art">${kind==='music'?'🎵':animalFor(value)}</span><span><strong title="${escapeHTML(value)}">${escapeHTML(value)}</strong><small>${kind==='music'?'已拉黑音乐':'已拉黑用户'} #${index+1}</small></span><div class="row-actions"><button class="secondary-button" type="button" data-copy-text="${escapeHTML(value)}" aria-label="复制${kind==='music'?'音乐':'用户'}黑名单内容 ${escapeHTML(value)}" title="复制">复制</button><button class="danger-button" type="button" data-command="delete-blacklist" data-kind="${kind}" data-value="${escapeHTML(value)}" aria-label="从黑名单删除 ${escapeHTML(value)}" title="删除">删除</button></div></div>`;}).join(''); };
function updateSummary(shown,total,label){const box=$('.modal-body .list-summary');if(box)box.outerHTML=totalHTML(shown,total,label);}

let toastTimer;
function notify(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 1800);
}

let celebratedRoomId='';
function celebrateRoomEntry(roomId){
  const key=String(roomId || '');if(!key || key===celebratedRoomId)return;
  celebratedRoomId=key;window.setTimeout(()=>{balloons().catch(()=>{});},650);
}

const centerNoticeTimers=new Map();
function dismissCenterNotice(kind){
  const key=String(kind || 'notice');
  clearTimeout(centerNoticeTimers.get(key));centerNoticeTimers.delete(key);
  $(`#center-notices [data-center-notice="${CSS.escape(key)}"]`)?.remove();
}
function showCenterNotice(kind,title,message,{duration=30000,actionLabel='',action=''}={}){
  const key=String(kind || 'notice');dismissCenterNotice(key);
  const card=document.createElement('section');card.className='center-notice-card';card.dataset.centerNotice=key;card.setAttribute('role','dialog');card.setAttribute('aria-label',title);
  card.innerHTML=`<button class="center-notice-close" type="button" data-close-center-notice="${escapeHTML(key)}" aria-label="关闭${escapeHTML(title)}">${icon('close')}</button><span class="center-notice-kicker">一起听歌吧</span><strong>${escapeHTML(title)}</strong><p>${escapeHTML(String(message || ''))}</p>${actionLabel?`<button class="primary-button center-notice-action" type="button" data-center-notice-action="${escapeHTML(action)}">${escapeHTML(actionLabel)}</button>`:''}`;
  $('#center-notices').append(card);requestAnimationFrame(()=>card.classList.add('show'));
  if(duration>0)centerNoticeTimers.set(key,setTimeout(()=>dismissCenterNotice(key),duration));
}
function showPlaybackPrompt(){showCenterNotice('playback','开始播放','浏览器已阻止自动播放，请点击播放按钮开始听歌。',{duration:30000,actionLabel:'播放音乐',action:'play'});}
function showRoomAnnouncement(content){showCenterNotice('announcement','房间公告',content,{duration:30000});}

async function copyToClipboard(value) {
  const text=String(value ?? '');
  if(window.plus?.android){
    try{
      const Context=plus.android.importClass('android.content.Context');
      const activity=plus.android.runtimeMainActivity();
      const clipboard=activity.getSystemService(Context.CLIPBOARD_SERVICE);
      plus.android.invoke(clipboard,'setText',text);
      return;
    }catch(_){}
  }
  if(navigator.clipboard?.writeText){try{await navigator.clipboard.writeText(text);return;}catch(_){} }
  const input=document.createElement('textarea');input.value=text;input.setAttribute('readonly','');input.style.cssText='position:fixed;left:-9999px;top:0;opacity:0';document.body.appendChild(input);input.focus();input.select();input.setSelectionRange(0,text.length);const copied=document.execCommand('copy');input.remove();if(!copied)throw new Error('copy failed');
}

function updateDocumentTitle(){
  const music=state.currentMusic;
  document.title=music?`${music.name || '未知歌曲'} · ${music.artist || '未知歌手'} · ${state.room || '一起听歌吧'}`:'一起听歌吧';
}

function updateMediaSession(music,cover=''){
  if(!('mediaSession' in navigator)||!window.MediaMetadata)return;
  try{navigator.mediaSession.metadata=new MediaMetadata({title:music?.name || '一起听歌吧',artist:music?.artist || state.room || '',album:albumName(music?.album),artwork:cover?[{src:cover}]:[]});}catch(_){}
}

async function syncPlaybackRetention(){
  if('mediaSession' in navigator)navigator.mediaSession.playbackState=state.playing?'playing':'paused';
  if(!('wakeLock' in navigator))return;
  if(!state.playing||document.hidden){if(playbackWakeLock){try{await playbackWakeLock.release();}catch(_){}playbackWakeLock=null;}return;}
  if(playbackWakeLock)return;
  try{playbackWakeLock=await navigator.wakeLock.request('screen');playbackWakeLock.addEventListener('release',()=>{playbackWakeLock=null;},{once:true});}catch(_){}
}

function configureMediaSession(){
  if(!('mediaSession' in navigator))return;
  try{navigator.mediaSession.setActionHandler('play',()=>audio.play());}catch(_){}
  try{navigator.mediaSession.setActionHandler('pause',()=>audio.pause());}catch(_){}
  try{navigator.mediaSession.setActionHandler('seekbackward',(details)=>{audio.currentTime=Math.max(0,audio.currentTime-(details.seekOffset || 10));});}catch(_){}
  try{navigator.mediaSession.setActionHandler('seekforward',(details)=>{audio.currentTime=Math.min(Number.isFinite(audio.duration)?audio.duration:audio.currentTime+10,audio.currentTime+(details.seekOffset || 10));});}catch(_){}
}

function annotateButtons(scope=document){
  const buttons=[...(scope.matches?.('button')?[scope]:[]),...$$('button',scope)];
  buttons.forEach((button)=>{const label=button.getAttribute('aria-label') || button.title || button.textContent.trim().replace(/\s+/g,' ');if(label){button.setAttribute('aria-label',label);button.title=label;}});
}

function countdownStatus(){
  if(!state.countdownEndsAt)return '尚未设置定时退出';
  return `预计 ${new Date(state.countdownEndsAt).toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'})} 自动退出当前房间`;
}

async function performTimedExit(){
  clearTimeout(window.jusicCloseTimer);state.countdownEndsAt=0;audio.pause();audio.removeAttribute('src');audio.load();state.currentMusic=null;state.roomId='';celebratedRoomId='';state.roomPassword='';state.room='请选择听歌房';state.roomDescription='';state.queue=[];state.lyrics=[];state.isAdmin=false;const currentSongId=$('#current-song-id');if(currentSongId){currentSongId.hidden=true;currentSongId.dataset.copySongId='';currentSongId.textContent='';}syncAdminUI();await backend.disconnect(true);releaseRoomLease();renderRooms();renderQueue();updateDocumentTitle();await loadRooms();openPanel('rooms');notify('倒计时结束，已退出听歌房');
}

function setCountdownExit(minutes){
  const value=Number(minutes);if(!Number.isInteger(value)||value<=0){notify('请输入正确的倒计时分钟数');return false;}
  clearTimeout(window.jusicCloseTimer);state.countdownEndsAt=Date.now()+value*60000;window.jusicCloseTimer=setTimeout(performTimedExit,value*60000);notify(`设置成功，将在 ${value} 分钟后退出`);return true;
}

function renderRooms() {
  if(!state.connected)renderGalaxy();
  const roomList=$('#room-list');
  if (!state.rooms.length) {
    if(roomList)roomList.innerHTML = '<div class="system-message">暂无可用房间</div>';
    $('#current-room-name').textContent = state.room;
    $('#current-room-name').title = state.room;
    return;
  }
  if(roomList)roomList.innerHTML = state.rooms.slice(0,3).map((room) => `
    <button class="room-item ${String(room.id) === String(state.roomId) ? 'active' : ''}" type="button" data-enter-room="${room.id}" ${String(room.id)===String(state.roomId)?'disabled':''}>
      <span class="room-art">${plantFor(room.id)}</span><span class="room-copy"><strong title="${escapeHTML(room.name)}">${escapeHTML(room.name)}</strong><small title="${escapeHTML(room.desc)}">${escapeHTML(room.desc)}</small></span>
      <span class="room-count">${room.locked ? icon('lock') : room.count || 0}</span>
    </button>`).join('');
  $('#current-room-name').textContent = state.room;
  $('#current-room-name').title = state.room;
  if(roomList)hydrateIcons(roomList);
}

function syncEntryExperience(){
  const home=$('#galaxy-home'), workspace=$('#room-workspace');
  if(home)home.hidden=state.connected;if(workspace)workspace.hidden=!state.connected;
  if(state.connected)releaseHomeVisuals();
  else if(state.rooms.length&&!$('#galaxy-system')?.childElementCount)renderGalaxy();
  document.body.classList.remove('entry-pending');
  document.body.classList.toggle('galaxy-active',!state.connected);
  document.body.classList.toggle('simple-home-active',!state.connected&&state.homeMode==='simple');
  syncSystemBarTheme();
}

function showDuplicateWindowHome(){
  state.duplicateConnection=true;state.connectionStatus='duplicate';state.connected=false;state.roomId='';state.roomPassword='';state.room='请选择听歌房';state.roomDescription='';state.pendingRoom=null;state.awaitingRoomEntry=false;state.pendingQueueSnapshot=null;state.pendingCreatedRoom=null;state.roomState=null;state.queue=[];state.currentMusic=null;state.mySessionId='';state.ownPickedIds.clear();state.likedQueueIds.clear();state.pendingPicks=[];state.pendingVote=null;state.pendingLikes=[];state.isAdmin=false;state.isRoot=false;
  audio.pause();audio.removeAttribute('src');renderRoomState();renderQueue();renderRooms();updateDocumentTitle();syncAdminUI();syncEntryExperience();currentPanel='';closePanel();
  $('#online-count').textContent='未连接';notify('另一窗口已加入听歌房，本窗口已返回首页');
}

const filteredGalaxyRooms=()=>[...state.rooms].filter((room)=>{
  if(state.galaxyFilter==='lively' && Number(room.count)<=0)return false;
  if(state.galaxyFilter==='locked' && !room.locked)return false;
  if(state.galaxyFilter==='unlocked' && room.locked)return false;
  const query=state.galaxyKeyword.trim().toLowerCase();return !query || `${room.name} ${room.desc}`.toLowerCase().includes(query);
}).sort((a,b)=>Number(b.count)-Number(a.count));

const earthMotion={yaw:-.65,pitch:-.13,dragging:false};
let earthView=null;
let gridTwinklesReady=false;
const SIMPLE_GRID_TWINKLES=20,GALAXY_GRID_TWINKLES=6,FIRST_SCREEN_TWINKLES=3;
function homeContentHeight(){const home=$('#galaxy-home'),content=$('.globe-explorer');if(!home||!content)return innerHeight;return Math.max(innerHeight,Math.ceil(content.getBoundingClientRect().bottom-home.getBoundingClientRect().top+48));}
function positionGridTwinkle(star,index=0){
  const step=58,home=$('#galaxy-home'),width=home?.clientWidth || innerWidth,height=homeContentHeight(),columns=Math.max(2,Math.floor(width/step)),galaxy=state.homeMode==='galaxy',activeCount=galaxy?GALAXY_GRID_TWINKLES:SIMPLE_GRID_TWINKLES;
  const column=Math.floor(Math.random()*columns);let row;
  if(galaxy||index<FIRST_SCREEN_TWINKLES){row=Math.floor(Math.random()*Math.max(3,Math.floor(innerHeight/step)));}
  else{const belowCount=Math.max(1,activeCount-FIRST_SCREEN_TWINKLES),band=(height-innerHeight)/belowCount,bandStart=innerHeight+(index-FIRST_SCREEN_TWINKLES)*band,target=bandStart+Math.random()*Math.max(step,band);row=Math.min(Math.floor((height-step)/step),Math.ceil(target/step));}
  star.style.setProperty('--star-x',`${column*step}px`);star.style.setProperty('--star-y',`${row*step}px`);
  star.style.setProperty('--star-size',Math.random()>.7?'3px':'2px');
  star.style.setProperty('--star-time',`${(3.8+Math.random()*2.8).toFixed(2)}s`);
}
function distributeGridTwinkles(){$$('.twinkle-star-field i:not([hidden])').forEach((star,index)=>positionGridTwinkle(star,index));}
function configureGridTwinkles(){const activeCount=state.homeMode==='galaxy'?GALAXY_GRID_TWINKLES:SIMPLE_GRID_TWINKLES;$$('.twinkle-star-field i').forEach((star,index)=>{star.hidden=index>=activeCount;if(!star.hidden)positionGridTwinkle(star,index);});}
function distributeSimpleMeteors(){
  const field=$('.simple-meteor-field'),home=$('#galaxy-home');if(!field||!home)return;
  while(field.children.length<16)field.append(document.createElement('i'));
  const height=homeContentHeight(),count=field.children.length;
  [...field.children].forEach((meteor,index)=>{meteor.style.setProperty('--meteor-x',`${4+Math.floor(Math.random()*82)}%`);meteor.style.setProperty('--meteor-y',`${Math.round((index+.35)*height/count)}px`);meteor.style.setProperty('--meteor-delay',`${-(index*.73%9).toFixed(2)}s`);meteor.style.setProperty('--meteor-time',`${(6.8+Math.random()*3).toFixed(2)}s`);});
}
function setupGridTwinkles(){
  if(gridTwinklesReady)return;gridTwinklesReady=true;
  const activeCount=SIMPLE_GRID_TWINKLES;
  $$('.twinkle-star-field i').forEach((star,index)=>{
    star.hidden=index>=activeCount;if(star.hidden)return;
    star.dataset.twinkleIndex=String(index);positionGridTwinkle(star,index);star.style.setProperty('--star-delay',`${-(Math.random()*2.8).toFixed(2)}s`);
    star.addEventListener('animationiteration',()=>positionGridTwinkle(star,Number(star.dataset.twinkleIndex)));
  });
  configureGridTwinkles();
  let resizeTimer=0;window.addEventListener('resize',()=>{clearTimeout(resizeTimer);resizeTimer=setTimeout(()=>{distributeGridTwinkles();distributeSimpleMeteors();},120);},{passive:true});
}
function createEarthRenderer(canvas){
  const gl=canvas.getContext('webgl',{alpha:true,antialias:true,premultipliedAlpha:true});
  if(!gl){canvas.hidden=true;return {rotate(){},destroy(){}};}
  const vertex=`attribute vec2 a_position;varying vec2 v_uv;void main(){v_uv=a_position*.5+.5;gl_Position=vec4(a_position,0.,1.);}`;
  const fragment=`precision highp float;varying vec2 v_uv;uniform sampler2D u_texture;uniform float u_yaw;uniform float u_pitch;
  void main(){vec2 point=(v_uv-.5)*2.;point.y=-point.y;float radius=dot(point,point);if(radius>1.)discard;
    float depth=sqrt(max(0.,1.-radius));vec3 normal=normalize(vec3(point.x,point.y,depth));float cp=cos(u_pitch),sp=sin(u_pitch);
    vec3 mapped=vec3(normal.x,normal.y*cp-normal.z*sp,normal.y*sp+normal.z*cp);float longitude=atan(mapped.x,mapped.z)+u_yaw;float latitude=asin(clamp(mapped.y,-1.,1.));
    vec2 earthUV=vec2(fract(longitude/6.2831853+.5),clamp(.5-latitude/3.1415926,0.,1.));vec3 surface=texture2D(u_texture,earthUV).rgb;
    vec3 light=normalize(vec3(-.42,.52,.82));float diffuse=max(dot(normal,light),0.);float dusk=smoothstep(-.18,.22,dot(normal,light));
    vec3 color=surface*(.22+.86*diffuse)*(.55+.45*dusk);float fresnel=pow(1.-depth,2.35);color+=vec3(.08,.43,.78)*fresnel*.72;
    float specular=pow(max(dot(reflect(-light,normal),vec3(0.,0.,1.)),0.),34.);color+=vec3(.72,.9,1.)*specular*.24;
    float alpha=1.-smoothstep(.965,1.,radius);gl_FragColor=vec4(color,alpha);}`;
  const compile=(type,source)=>{const shader=gl.createShader(type);gl.shaderSource(shader,source);gl.compileShader(shader);if(!gl.getShaderParameter(shader,gl.COMPILE_STATUS)){const message=gl.getShaderInfoLog(shader);gl.deleteShader(shader);throw new Error(message);}return shader;};
  let program,vertexShader,fragmentShader;
  try{program=gl.createProgram();vertexShader=compile(gl.VERTEX_SHADER,vertex);fragmentShader=compile(gl.FRAGMENT_SHADER,fragment);gl.attachShader(program,vertexShader);gl.attachShader(program,fragmentShader);gl.linkProgram(program);if(!gl.getProgramParameter(program,gl.LINK_STATUS))throw new Error(gl.getProgramInfoLog(program));gl.deleteShader(vertexShader);gl.deleteShader(fragmentShader);vertexShader=null;fragmentShader=null;}catch(error){if(vertexShader)gl.deleteShader(vertexShader);if(fragmentShader)gl.deleteShader(fragmentShader);if(program)gl.deleteProgram(program);console.warn('3D Earth unavailable',error);canvas.hidden=true;return {rotate(){},destroy(){gl.getExtension('WEBGL_lose_context')?.loseContext();}};}
  gl.useProgram(program);const buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,1,-1,-1,1,-1,1,1,-1,1,1]),gl.STATIC_DRAW);
  const position=gl.getAttribLocation(program,'a_position');gl.enableVertexAttribArray(position);gl.vertexAttribPointer(position,2,gl.FLOAT,false,0,0);
  const yawUniform=gl.getUniformLocation(program,'u_yaw'),pitchUniform=gl.getUniformLocation(program,'u_pitch');
  const texture=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,texture);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.LINEAR);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.LINEAR);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,1,1,0,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array([18,74,112,255]));
  let stopped=false,frame=0,lastTime=performance.now();const image=new Image();image.decoding='async';image.onload=()=>{if(stopped)return;gl.bindTexture(gl.TEXTURE_2D,texture);gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL,false);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,image);canvas.closest('.room-globe')?.classList.add('earth-ready');};image.src='./assets/earth-surface-v2.png';
  const resize=()=>{const rect=canvas.getBoundingClientRect(),scale=Math.min(2,devicePixelRatio||1),width=Math.max(2,Math.round(rect.width*scale)),height=Math.max(2,Math.round(rect.height*scale));if(canvas.width!==width||canvas.height!==height){canvas.width=width;canvas.height=height;gl.viewport(0,0,width,height);}};
  const observer=new ResizeObserver(resize);observer.observe(canvas);resize();
  const scheduleDraw=()=>{if(!stopped&&!document.hidden&&!frame)frame=requestAnimationFrame(draw);};
  const draw=(time)=>{frame=0;if(stopped||document.hidden)return;const elapsed=Math.min(50,time-lastTime);lastTime=time;if(!earthMotion.dragging && document.body.classList.contains('galaxy-active'))earthMotion.yaw+=elapsed*.000018;gl.useProgram(program);gl.uniform1f(yawUniform,earthMotion.yaw);gl.uniform1f(pitchUniform,earthMotion.pitch);gl.clearColor(0,0,0,0);gl.clear(gl.COLOR_BUFFER_BIT);gl.drawArrays(gl.TRIANGLES,0,6);scheduleDraw();};
  scheduleDraw();
  const onVisChange=()=>{if(!document.hidden&&!stopped){lastTime=performance.now();scheduleDraw();}};
  document.addEventListener('visibilitychange',onVisChange);
  return {rotate(dx,dy){earthMotion.yaw-=dx*.0065;earthMotion.pitch=Math.max(-.55,Math.min(.48,earthMotion.pitch+dy*.004));},destroy(){stopped=true;cancelAnimationFrame(frame);frame=0;document.removeEventListener('visibilitychange',onVisChange);observer.disconnect();image.onload=null;image.src='';gl.deleteTexture(texture);gl.deleteBuffer(buffer);gl.deleteProgram(program);gl.getExtension('WEBGL_lose_context')?.loseContext();}};
}

function releaseHomeVisuals(){
  earthView?.destroy();earthView=null;
  const system=$('#galaxy-system');
  if(system?.childElementCount)system.replaceChildren();
}

function renderGalaxy(){
  const system=$('#galaxy-system');if(!system)return;
  const all=filteredGalaxyRooms();
  const roomCount=$('#galaxy-room-count');if(roomCount){const filtered=Boolean(state.galaxyKeyword.trim() || state.galaxyFilter!=='all');roomCount.textContent=filtered?`筛选 ${all.length} / 共 ${state.rooms.length} 间房`:`共 ${state.rooms.length} 间房`;}
  const simple=state.homeMode==='simple';
  $('#galaxy-home')?.classList.toggle('simple-home',simple);
  document.documentElement.classList.remove('home-mode-pending');
  document.body.classList.toggle('simple-home-active',!state.connected&&simple);
  configureGridTwinkles();
  $$('[data-home-mode]').forEach((button)=>{const active=button.dataset.homeMode===state.homeMode;button.classList.toggle('active',active);button.setAttribute('aria-pressed',String(active));});
  const legend=$('.planet-legend'),pagination=$('.globe-pagination');if(legend)legend.hidden=simple;if(pagination)pagination.hidden=simple;
  const main=state.rooms.find((room)=>String(room.id)==='DEFAULT') || state.rooms[0];const announcementContent=main?.raw?.announce?.content || main?.desc || '欢迎来到一起听歌吧';$$('.home-announcement').forEach((announcement)=>{announcement.innerHTML=`<span>一起听歌吧公告</span><strong title="${escapeHTML(announcementContent)}">${escapeHTML(announcementContent)}</strong>`;});
  earthView?.destroy();earthView=null;
  if(simple){
    system.className='galaxy-system simple-room-grid';
    system.innerHTML=all.length?all.map((room)=>{const description=room.desc || '暂无房间描述';return `<button class="simple-room-card" type="button" data-enter-room="${escapeHTML(room.id)}" aria-label="进入 ${escapeHTML(room.name)}，${room.count} 人在线"><span class="simple-room-icon">${plantFor(room.id)}</span><span class="simple-room-copy"><strong title="${escapeHTML(room.name)}">${escapeHTML(room.name)}</strong><small>${room.locked?'🔒 密码房':'公开房间'} · ${room.count} 人在线</small><em title="${escapeHTML(description)}">${escapeHTML(description)}</em></span><span class="simple-room-enter">进入</span></button>`;}).join(''):'<div class="simple-room-empty">没有找到符合条件的房间</div>';
    requestAnimationFrame(()=>{distributeGridTwinkles();distributeSimpleMeteors();});
    const pageLabel=$('#galaxy-page-label');if(pageLabel)pageLabel.textContent=`${all.length} 个房间`;
    $$('.globe-filters button').forEach((button)=>button.classList.toggle('active',button.dataset.galaxyFilter===state.galaxyFilter));
    return;
  }
  const pageSize=7,pages=Math.max(1,Math.ceil(all.length/pageSize));state.galaxyPage=(state.galaxyPage%pages+pages)%pages;const rooms=all.slice(state.galaxyPage*pageSize,(state.galaxyPage+1)*pageSize);
  const positions=[[56,9],[76,14],[94,37],[91,75],[72,88],[55,79],[55,27]];
  system.className='galaxy-system solar-system-mode';
  const solarDecor=`<div class="solar-decor" aria-hidden="true"><div class="deep-nebula"></div><i class="orbit-path orbit-one"></i><i class="orbit-path orbit-two"></i><i class="orbit-path orbit-three"></i><div class="celestial-body sun"><span class="planet-surface"></span><span class="solar-corona"></span><span class="planet-name">太阳</span></div><div class="celestial-body mercury"><span class="planet-surface"></span><span class="planet-name">水星</span></div><div class="celestial-body venus"><span class="planet-surface"></span><span class="planet-name">金星</span></div><div class="celestial-body mars"><span class="planet-surface"></span><span class="planet-name">火星</span></div><div class="celestial-body jupiter"><span class="planet-surface"></span><span class="planet-name">木星</span></div><div class="celestial-body saturn"><span class="saturn-ring"></span><span class="planet-surface"></span><span class="saturn-ring-front"></span><span class="planet-name">土星</span></div><div class="celestial-body uranus"><span class="uranus-ring"></span><span class="planet-surface"></span><span class="planet-name">天王星</span></div><div class="celestial-body neptune"><span class="planet-surface"></span><span class="planet-name">海王星</span></div><div class="meteor-field"><i style="--meteor-x:8%;--meteor-y:16%;--meteor-delay:-2s;--meteor-time:7s"></i><i style="--meteor-x:48%;--meteor-y:3%;--meteor-delay:-7s;--meteor-time:11s"></i><i style="--meteor-x:75%;--meteor-y:38%;--meteor-delay:-4s;--meteor-time:9s"></i><i style="--meteor-x:20%;--meteor-y:70%;--meteor-delay:-10s;--meteor-time:13s"></i></div><div class="warp-streaks"><i></i><i></i><i></i></div></div>`;
  system.innerHTML=solarDecor+`<div class="room-globe" aria-hidden="true"><span class="planet-name earth-name">地球</span><div class="earth-atmosphere"></div><div class="earth-fallback"></div><canvas class="earth-canvas"></canvas><div class="earth-shadow"></div><div class="room-globe-glow"></div></div>`+(rooms.length?rooms.map((room,index)=>{const [x,y]=positions[index];const description=room.desc || '暂无房间描述';return `<button class="cosmos-room orbit-signal orbit-signal-${index%4} ${index<2?'is-warm':''}" type="button" data-enter-room="${escapeHTML(room.id)}" style="--x:${x}%;--y:${y}%;--delay:${index*-.13}s;--hue:${(stableUserHash(room.id)%80)+170}" aria-label="进入 ${escapeHTML(room.name)}，${room.count} 人在线"><span class="signal-pulse"></span><span class="cosmos-room-icon">${plantFor(room.id)}</span><span class="cosmos-room-copy"><strong title="${escapeHTML(room.name)}">${escapeHTML(room.name)}</strong><small>${room.locked?'🔒 · ':''}${room.count} 人正在同频</small><em title="${escapeHTML(description)}">${escapeHTML(description)}</em></span></button>`;}).join(''):'<div class="globe-empty">没有找到符合条件的房间</div>');
  earthView=createEarthRenderer(system.querySelector('.earth-canvas'));
  const pageLabel=$('#galaxy-page-label');if(pageLabel)pageLabel.textContent=`第 ${state.galaxyPage+1} / ${pages} 页 · ${all.length} 个房间`;
  $$('.globe-filters button').forEach((button)=>button.classList.toggle('active',button.dataset.galaxyFilter===state.galaxyFilter));
}

function renderQueue() {
  const waiting=waitingQueueItems(state.queue);
  $('#queue-count').textContent = waiting.length;
  $('#queue-view').innerHTML = waiting.map((song, index) => {
    const actualIndex=index+1;
    const hasOwner=Boolean(song.sessionId || song.userId || normalizedNickname(song.nickName || song.picker));
    const ownBySession=Boolean(state.mySessionId && song.sessionId && String(song.sessionId)===String(state.mySessionId));
    const ownByName=Boolean(!(state.mySessionId && song.sessionId) && state.nickname && normalizedNickname(song.nickName || song.picker)===normalizedNickname(state.nickname));
    const own=ownBySession || ownByName || (!hasOwner && state.ownPickedIds.has(String(song.id)));
    return `
    <article class="queue-row" data-song-id="${song.id}" data-queue-index="${actualIndex}">
      <span class="queue-number">${String(index + 1).padStart(2, '0')}</span>
      <span class="song-title-cell"><span class="song-name" title="${escapeHTML(song.name || '未知歌曲')}">${escapeHTML(song.name || '未知歌曲')}</span>${songIdButton(song.id)}</span><span class="song-artist" title="${escapeHTML(song.artist || '未知歌手')}">${escapeHTML(song.artist || '未知歌手')}</span><span class="song-picker" title="${escapeHTML(userDisplayName({...song,nickName:song.picker || song.nickName}))} 点播">${escapeHTML(userDisplayName({...song,nickName:song.picker || song.nickName}))} 点播</span>
      <span class="song-actions">
        <button type="button" data-queue-action="favorite" aria-label="收藏 ${song.name}">${icon(song.favorite ? 'favorite' : 'favorite_border')}</button>
        ${state.goodMode ? `<button type="button" data-queue-action="like" class="${state.likedQueueIds.has(String(song.id))?'is-liked':''}" aria-pressed="${state.likedQueueIds.has(String(song.id))}" aria-label="点赞 ${song.name}" title="${state.likedQueueIds.has(String(song.id))?'已点赞':'点赞'}">${icon('thumb_up')}</button>` : ''}
        ${state.isAdmin ? `<button type="button" data-queue-action="up" aria-label="置顶 ${song.name}">${icon('arrow_upward')}</button><button type="button" class="blacklist-song-action" data-queue-action="black" aria-label="拉黑 ${song.name}" title="拉黑歌曲">${icon('block')}</button>` : ''}
        ${(state.isAdmin || own) ? `<button type="button" data-queue-action="remove" aria-label="移除 ${song.name}">${icon('close')}</button>` : ''}
      </span>
    </article>`;}).join('') || `<div class="queue-empty-state"><div class="empty-note">${icon('music_note')}<strong>待播队列还是空的</strong><p>搜索喜欢的歌曲，把它加入当前听歌房。</p></div><button class="primary-button" type="button" data-panel="search">去点歌</button></div>`;
  hydrateIcons($('#queue-view'));
}

const CHAT_STATE_LIMIT=300,CHAT_DOM_LIMIT=300;
let renderedMessageCount=0,chatHasUnread=false,chatFollowLatest=true;
function scrollChatToLatest(){
  const box=$('#messages');if(!box||!chatFollowLatest)return;
  box.scrollTop=box.scrollHeight;chatHasUnread=false;
}
function updateChatJumpButton(){
  const box=$('#messages'),button=$('#chat-jump-latest');if(!box||!button)return;
  const nearBottom=chatIsNearBottom(box.scrollTop,box.scrollHeight,box.clientHeight);
  if(chatFollowLatest||nearBottom)chatHasUnread=false;
  button.hidden=chatFollowLatest||nearBottom;
  button.textContent=chatHasUnread?'有新消息 · 回到最新':'回到最新';
}
function renderMessages() {
  if (state.messages.length > CHAT_STATE_LIMIT) state.messages = state.messages.slice(-CHAT_STATE_LIMIT);
  const box=$('#messages');
  const previousTop=box.scrollTop;
  const reset=state.messages.length<renderedMessageCount;
  const follow=reset || chatFollowLatest;
  if(!follow && state.messages.length>renderedMessageCount)chatHasUnread=true;
  if(reset){chatHasUnread=false;chatFollowLatest=true;}
  const filtered = filterChatMessages(state.messages,state.messageFilter).slice(-CHAT_DOM_LIMIT);
  box.innerHTML = filtered.map((message) => message.type === 'system'
    ? `<div class="system-message">${escapeHTML(message.text)}</div>`
    : (()=>{const me=isSelfChatMessage(message,{sessionId:state.mySessionId,nickname:state.nickname});const visual=chatVisual(message.sessionId || message.name,me);const userAttrs=me?`data-chat-self="true" data-chat-id="${escapeHTML(message.sessionId || state.mySessionId || '')}" data-chat-name="${escapeHTML(message.name)}"`:`data-chat-user="${escapeHTML(message.sessionId || '')}" data-chat-name="${escapeHTML(message.name)}"`;return `<article class="message ${me ? 'me' : 'other'}"><button class="message-avatar" type="button" style="background:${visual.background};border-color:${visual.border}" aria-label="查看${escapeHTML(message.name)}的个人信息" ${userAttrs}>${animalFor(message.sessionId || message.name)}</button><div class="message-content"><div class="message-meta" title="${escapeHTML(message.name)}" style="color:${visual.name}" ${!me ? userAttrs : ''}>${escapeHTML(message.name)} · ${message.time}</div><div class="bubble" style="background:${visual.background};border-color:${visual.border};color:${visual.text}">${escapeHTML(message.text)}${(message.images || []).map((url)=>`<button class="chat-picture-button" type="button" data-preview-image="${escapeHTML(url)}" aria-label="放大查看聊天图片"><img class="chat-picture" src="${escapeHTML(url)}" alt="聊天图片" loading="lazy" decoding="async"></button>`).join('')}</div></div></article>`;})()
  ).join('');
  box.scrollTop=follow?box.scrollHeight:previousTop;
  if(follow)requestAnimationFrame(()=>{scrollChatToLatest();requestAnimationFrame(scrollChatToLatest);});
  renderedMessageCount=state.messages.length;
  updateChatJumpButton();
}

function setPlayerTheme(theme){
  if(!['light','night','black'].includes(theme))return;
  state.playerTheme=theme;document.body.dataset.playerTheme=theme;localStorage.setItem('JUSIC_PLAYER_THEME',theme);
  $$('[data-player-theme]').forEach((button)=>{const selected=button.dataset.playerTheme===theme;button.classList.toggle('active',selected);button.setAttribute('aria-pressed',String(selected));});
  syncSystemBarTheme();
  renderMessages();
}

function setSidebarCollapsed(collapsed,{persist=true}={}){
  state.sidebarCollapsed=Boolean(collapsed);
  const app=$('#app');
  app.classList.toggle('sidebar-collapsed',state.sidebarCollapsed);
  app.classList.toggle('sidebar-expanded',!state.sidebarCollapsed);
  const button=$('[data-action="toggle-sidebar"]');
  if(button){
    button.innerHTML=icon(state.sidebarCollapsed?'chevron_right':'chevron_left');
    button.setAttribute('aria-label',state.sidebarCollapsed?'展开左侧菜单':'收起左侧菜单');
    button.setAttribute('aria-expanded',String(!state.sidebarCollapsed));
    button.title=button.getAttribute('aria-label');
  }
  if(persist)localStorage.setItem('JUSIC_SIDEBAR_COLLAPSED',String(state.sidebarCollapsed));
}

function syncSystemBarTheme(){
  const theme=state.focusMode?'focus':(!state.connected?'home':state.playerTheme);
  const colors={home:'#03050d',focus:'#090b12',light:'#f8f6f1',night:'#102330',black:'#100e19'};
  const color=colors[theme] || colors.home;
  let meta=document.querySelector('meta[name="theme-color"]');
  if(!meta){meta=document.createElement('meta');meta.name='theme-color';document.head.append(meta);}
  meta.content=color;
  document.documentElement.style.backgroundColor=color;
  window.JusicNative?.setSystemBars?.(color,theme==='light');
}

function chatVisual(identity='',own=false){
  const key=`${identity || 'user'}_${state.roomId || 'default'}`;let hash=0;for(let i=0;i<key.length;i++)hash=((hash<<5)-hash)+key.charCodeAt(i);
  const hue=Math.abs(hash)%360, hue2=(hue+35)%360;
  const dark=state.playerTheme!=='light';
  return own?{name:`hsl(${hue} 72% ${dark?'76%':'34%'})`,background:`linear-gradient(135deg,hsl(${hue} 67% 42%),hsl(${hue2} 72% 36%))`,border:`hsl(${hue} 55% 35%)`,text:'#fff'}:{name:`hsl(${hue} 60% ${dark?'76%':'34%'})`,background:dark?`linear-gradient(135deg,hsl(${hue} 30% 23%),hsl(${hue2} 25% 20%))`:`linear-gradient(135deg,hsl(${hue} 52% 94%),hsl(${hue2} 48% 90%))`,border:dark?`hsl(${hue} 25% 38%)`:`hsl(${hue} 35% 78%)`,text:dark?'#eef3fb':'#211e1a'};
}

const featureCard = (panel, symbol, title, copy) => `<button class="feature-card" type="button" data-panel="${panel}">${icon(symbol)}<strong>${title}</strong><p>${copy}</p></button>`;
const actionCard = (action, symbol, title, copy) => `<button class="feature-card" type="button" data-action="${action}">${icon(symbol)}<strong>${title}</strong><p>${copy}</p></button>`;
const linkCard = (href, symbol, title, copy) => `<a class="feature-card" href="${href}" target="_blank" rel="noopener" aria-label="${title}（新窗口打开）">${icon(symbol)}<strong>${title}</strong><p>${copy}</p></a>`;
const resultRow = (name, meta, action = '点歌', command = 'pick-song', data = {}) => {
  const attrs = Object.entries(data).map(([key,value]) => ` data-${key}="${escapeHTML(value ?? '')}"`).join('');
  return `<div class="result-row"><span class="result-art"></span><span>${songTitleHTML(name,data.id)}<small title="${escapeHTML(meta)}">${escapeHTML(meta)}</small></span><button class="primary-button" type="button" data-command="${command}" data-name="${escapeHTML(name)}"${attrs}>${action}</button></div>`;
};

function searchResultsHTML() {
  if (!state.searchResults.length) return '<div class="system-message">输入关键词后搜索真实音乐库</div>';
  return state.searchResults.map((song) => {
    const unavailable=Number(song.fl)===0 || Number(song.st)<0; const art=coverOf(song);
    return `<div class="result-row"><span class="result-art" data-cover-id="${escapeHTML(song.id)}">${art?`<img src="${escapeHTML(art)}" alt="${escapeHTML(song.name || '歌曲')}封面" loading="lazy" decoding="async">`:plantFor(song.id)}</span><span>${songTitleHTML(song.name,song.id)}<small>${escapeHTML(`${song.artist || '未知歌手'} · ${albumName(song.album)}`)}</small></span><span class="form-actions" style="margin:0">${unavailable?'<button class="secondary-button" disabled>不可用</button>':`<button class="secondary-button" type="button" data-command="pick-song" data-name="${escapeHTML(song.name)}" data-artist="${escapeHTML(song.artist || '')}" data-id="${escapeHTML(song.id)}" data-source="${escapeHTML(song.source || sourceCode(state.source))}" data-quality="320k">标准</button><button class="primary-button" type="button" data-command="pick-song" data-name="${escapeHTML(song.name)}" data-artist="${escapeHTML(song.artist || '')}" data-id="${escapeHTML(song.id)}" data-source="${escapeHTML(song.source || sourceCode(state.source))}" data-quality="flac">高清</button>`}</span></div>`;
  }).join('');
}

function hotResultsHTML(){
  if(!state.hotResults.length)return '<div class="system-message">正在读取热歌榜…</div>';
  return state.hotResults.map((song)=>{const art=coverOf(song);return `<div class="result-row"><span class="result-art" data-cover-id="${escapeHTML(song.id)}">${art?`<img src="${escapeHTML(art)}" alt="${escapeHTML(song.name || '歌曲')}封面" loading="lazy" decoding="async">`:plantFor(song.id)}</span><span>${songTitleHTML(song.name,song.id)}<small>${escapeHTML(`${song.artist || '未知歌手'} · ${albumName(song.album)}`)}</small></span><span class="form-actions" style="margin:0"><button class="secondary-button" type="button" data-command="pick-song" data-name="${escapeHTML(song.name)}" data-artist="${escapeHTML(song.artist || '')}" data-id="${escapeHTML(song.id)}" data-source="${escapeHTML(song.source || state.hotSource)}" data-quality="320k">标准</button><button class="primary-button" type="button" data-command="pick-song" data-name="${escapeHTML(song.name)}" data-artist="${escapeHTML(song.artist || '')}" data-id="${escapeHTML(song.id)}" data-source="${escapeHTML(song.source || state.hotSource)}" data-quality="flac">高清</button></span></div>`;}).join('');
}

function playlistResultsHTML(){
  const radioMode=state.playlistKind==='radio' || String(state.playlistSource).endsWith('_userdj');
  const rows=state.playlistResults.map((item)=>{const art=coverOf(item) || item.coverImgUrl || item.pictureUrl || '';const source=radioMode?'wydt':(state.playlistSource || 'wy').replace(/_user(?:dj)?$/,'');const count=item.songCount || item.trackCount || item.programCount || 0;const creator=item.creator?.nickname || item.creator || item.dj?.nickname || '';return `<div class="result-row"><span class="result-art">${art?`<img src="${escapeHTML(art)}" alt="${radioMode?'电台':'歌单'}封面" loading="lazy" decoding="async">`:plantFor(item.id)}</span><span><span class="song-title-inline"><strong title="${escapeHTML(item.name || (radioMode?'未命名电台':'未命名歌单'))}">${escapeHTML(item.name || (radioMode?'未命名电台':'未命名歌单'))}</strong>${playlistIdButton(item.id)}</span><small title="${escapeHTML(`${count} ${radioMode?'期节目':'首'} · ${creator}`)}">${escapeHTML(`${count} ${radioMode?'期节目':'首'} · ${creator}`)}</small></span><span class="form-actions" style="margin:0"><button class="secondary-button" type="button" data-command="view-playlist-songs" data-id="${escapeHTML(item.id)}" data-source="${escapeHTML(source)}">${radioMode?'查看节目':'查看歌曲'}</button>${canManage()&&!radioMode?`<button class="secondary-button" type="button" data-command="append-playlist" data-id="${escapeHTML(item.id)}" data-source="${escapeHTML(source)}">追加</button><button class="primary-button" type="button" data-command="replace-playlist" data-id="${escapeHTML(item.id)}" data-source="${escapeHTML(source)}">覆盖</button>`:''}</span></div>`;}).join('');
  return rows || '<div class="system-message">没有结果</div>';
}
function userResultsHTML(){
  return state.userResults.map((item)=>`<div class="result-row"><span class="result-art">${coverOf(item)||item.avatarUrl?`<img src="${escapeHTML(coverOf(item)||item.avatarUrl)}" alt="用户头像" loading="lazy" decoding="async">`:animalFor(item.userId)}</span><span><strong>${escapeHTML(userDisplayName(item))}</strong><small>${escapeHTML(item.signature || item.userId || '')}</small></span><span class="form-actions" style="margin:0"><button class="secondary-button" type="button" data-command="view-user-playlists" data-user="${escapeHTML(item.userId || '')}" data-source="wy">查看歌单</button><button class="primary-button" type="button" data-command="view-user-radio" data-user="${escapeHTML(item.userId || '')}" data-source="wy">查看电台</button></span></div>`).join('') || '<div class="system-message">没有结果</div>';
}

function renderCurrentPicker(music=state.currentMusic){
  if(!music)return;
  const pickerId=String(music.sessionId || music.userId || music.uid || '').trim();
  const picker=normalizedNickname(music.nickName || music.picker || music.userName) || pickerId;
  const label=$('#track-picker');
  if(label){label.textContent=picker?`由 ${picker} 点播`:'房间播放';label.title=pickerId?`点歌人 ID：${pickerId}`:(picker || '房间播放');}
}

function updatePlayer(music) {
  if (!music) return;
  state.currentMusic = music;
  syncAdminUI();
  state.favorite=isFavorite(music.id);$('.favorite-toggle')?.classList.toggle('active',state.favorite);
  $('#track-title').textContent = music.name || '未知歌曲';
  const currentSongId=$('#current-song-id');if(currentSongId){const hasId=music.id!=null&&String(music.id).trim()!=='';currentSongId.hidden=!hasId;currentSongId.dataset.copySongId=hasId?String(music.id):'';currentSongId.textContent=hasId?`ID ${music.id}`:'';}
  $('#track-artist').textContent = music.artist || '未知歌手';
  $('#track-album').textContent = albumName(music.album);
  renderCurrentPicker(music);
  $('#focus-track-title').textContent=music.name || '未知歌曲';$('#focus-track-artist').textContent=music.artist || '未知歌手';$('#focus-track-album').textContent=albumName(music.album);
  updateDocumentTitle();
  const currentCover=coverOf(music);
  updateMediaSession(music,currentCover);
  const focusCover=$('#focus-album-cover');
  if (currentCover) {
    $('#album-cover').style.backgroundImage = `linear-gradient(rgba(20,18,15,.04),rgba(20,18,15,.04)),url("${String(currentCover).replace(/"/g,'%22')}")`;
    $('#album-cover').style.backgroundSize = 'cover';
    $('#album-cover').style.backgroundPosition = 'center';
    focusCover.style.backgroundImage=`linear-gradient(rgba(10,10,16,.05),rgba(10,10,16,.05)),url("${String(currentCover).replace(/"/g,'%22')}")`;focusCover.style.backgroundSize='cover';focusCover.style.backgroundPosition='center';
  } else {
    focusCover.style.backgroundImage='';
  }
  if (music.url) {
    const directEntry=Boolean(state.sharedAutoplayPending);
    state.sharedAutoplayPending=false;
    const url = String(music.url).replace(/\\u0026/gi,'&').replace(/&amp;/gi,'&').replace(/^http:/,'https:');
    if(directEntry)setAudioMuted(false);
    audio.src = url;
    audio.volume = Number($('#volume').value) / 100;
    audio.load();
    const syncPosition=()=>{const pushed=Number(music.pushTime || music.sendTime);if(!pushed || !Number.isFinite(audio.duration))return;const elapsed=Math.max(0,(Date.now()-pushed)/1000);const target=Math.min(elapsed,Math.max(0,audio.duration-.5));if(Math.abs(audio.currentTime-target)>1)audio.currentTime=target;};
    if(audio.readyState>=1)syncPosition();else audio.addEventListener('loadedmetadata',syncPosition,{once:true});
    let started=false,startPending=false;
    const startPlayback=async()=>{if(started||startPending)return started;startPending=true;try{await audio.play();started=true;syncPlayButtons(true);return true;}catch(_){return false;}finally{startPending=false;}};
    const initialPlay=startPlayback();
    if(directEntry){
      ['loadeddata','canplay','canplaythrough'].forEach((eventName)=>audio.addEventListener(eventName,()=>{startPlayback();},{once:true}));
      [350,900,1800,3200].forEach((delay)=>window.setTimeout(()=>{startPlayback();},delay));
      window.setTimeout(async()=>{if(!(await startPlayback()))showPlaybackPrompt();},1200);
    }
  }
  const previousHistory=state.history.find((item)=>String(item.id)===String(music.id));
  const pickTime = previousHistory?.pickTime || music.pickTime || music.sendTime || music.pushTime || music.createTime || Date.now();
  const historyItem = { ...music, pickTime, lyric: '', mine:Boolean(previousHistory?.mine || historyBelongsToUser(music,state.nickname)) };
  state.history = [historyItem, ...state.history.filter((item) => String(item.id) !== String(music.id))].slice(0,400);
  localStorage.setItem('pickHistory', JSON.stringify(state.history));
  syncHistoryPanel();
}

function ensureCoverRotation(playing,{restart=false}={}){
  [$('#album-cover'),$('#focus-album-cover')].filter(Boolean).forEach((cover)=>{
    if(!playing){cover.classList.remove('is-playing');cover._jusicSpinAnimation?.cancel();cover._jusicSpinAnimation=null;return;}
    if(restart){cover._jusicSpinAnimation?.cancel();cover._jusicSpinAnimation=null;cover.classList.remove('is-playing');}
    cover.classList.add('is-playing');
    const resumeAnimation=()=>{
      if(!state.playing||document.hidden)return;
      cover.classList.add('is-playing');
      const animation=cover.getAnimations?.().find((item)=>item.animationName==='record-spin') || cover._jusicSpinAnimation;
      if(animation){if(animation.playState!=='running')animation.play();return;}
      if(!cover.animate)return;
      const duration=cover.id==='focus-album-cover'?16000:12000;
      cover._jusicSpinAnimation=cover.animate(
        [{transform:'translateZ(0) rotate(0deg)'},{transform:'translateZ(0) rotate(360deg)'}],
        {duration,iterations:Infinity,easing:'linear'}
      );
    };
    resumeAnimation();
    if(restart)requestAnimationFrame(resumeAnimation);
  });
}

function syncPlayButtons(forcePlaying,{restartCover=false}={}) {
  const actuallyPlaying=typeof forcePlaying==='boolean'?forcePlaying:Boolean(!audio.paused && !audio.ended);
  state.playing=actuallyPlaying;
  $$('[data-action="toggle-play"]').forEach((button) => {
    button.innerHTML = icon(state.playing ? 'pause' : 'play_arrow');
    button.setAttribute('aria-label', state.playing ? '暂停' : '播放');
  });
  ensureCoverRotation(state.playing,{restart:restartCover});
  syncPlaybackRetention();
}


function defaultPlaylistHTML() {
  const list = state.defaultPlaylist;
  const scope = list.scope === 'room' ? (list.total > 0 ? '当前房间默认列表' : '当前房间未设置默认列表') : '读取当前房间默认列表';
  return '<div class="system-message" id="default-scope">' + escapeHTML(scope) + '</div>'
    + totalHTML(list.songs.length,list.total,'首歌曲')
    + '<div class="result-list" id="default-results">' + (list.songs.length ? defaultPlaylistRows(list.songs, state.favorites) : '') + '</div>'
    + '<div class="system-message" id="default-load-state">' + escapeHTML(list.error || (list.loading ? '正在读取默认列表…' : '')) + '</div>';
}
async function loadDefaultPlaylist(page = 1) {
  const previous = state.defaultPlaylist;
  const append = page > 1 && previous.houseId === String(state.roomId);
  const requestId = String(++state.defaultRequestSeq);
  state.defaultPlaylist = append ? {...previous,page,loading:true,error:'',requestId} : {songs:[],page:1,total:0,scope:'',loading:true,error:'',requestId,houseId:String(state.roomId)};
  if (currentPanel === 'default-list') {
    if (append) { const status=$('#default-load-state');if(status)status.textContent='正在读取更多歌曲…'; }
    else refreshCurrentPanelBody();
  }
  const fail = (message) => {
    if (state.defaultPlaylist.requestId !== requestId) return;
    state.defaultPlaylist.loading = false;
    state.defaultPlaylist.error = message;
    if (append) state.defaultPlaylist.page = previous.page;
    if (currentPanel === 'default-list') {
      const status=$('#default-load-state');if(status)status.textContent=message;
    }
  };
  window.setTimeout(() => { if (state.defaultPlaylist.requestId === requestId && state.defaultPlaylist.loading) fail('读取超时，请刷新列表；请确认后端已更新'); }, 8000);
  try { await backend.command('playlist.default', {pageIndex:page,pageSize:30,requestId}); }
  catch(error) { fail(error.message); }
}

const systemAdminDate = (value) => {
  if (value == null || value === '') return '—';
  const timestamp = Number(value);
  const date = new Date(Number.isFinite(timestamp) ? (timestamp < 1e11 ? timestamp * 1000 : timestamp) : value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('zh-CN');
};
function systemRoomLimitText(result) {
  const limit=Number(result?.data);
  const remaining=String(result?.message ?? '').match(/(-?\d+)\s*$/)?.[1];
  return `当前上限：${Number.isFinite(limit)?`${limit} 间`:'—'} · 剩余可创建：${remaining?`${remaining} 间`:(result?.message || '后端未返回数量')}`;
}
function systemAdminUserHTML(user) {
  const name=escapeHTML(user.name || user.nickName || user.sessionId || '匿名用户');
  const ip=escapeHTML(user.remoteAddress || '—');
  const activity=escapeHTML(systemAdminDate(user.lastMessageTime));
  return `<div class="system-admin-item system-admin-user"><div><strong title="${name}">${name}</strong><small title="IP：${ip}">IP：${ip}</small><small title="最后活动：${activity}">最后活动：${activity}</small><small title="会话：${escapeHTML(user.sessionId || '—')}">会话：${escapeHTML(user.sessionId || '—')} · 角色：${escapeHTML(user.role || 'default')}</small></div></div>`;
}
function systemAdminHTML() {
  if (!state.isRoot) return '<div class="system-message">请先在聊天框输入 root 密码，验证超级管理员身份。</div>';
  const session=state.systemAdmin;
  const intro='<div class="system-admin-intro"><span>系统级操作</span><strong>管理员控制台</strong><p>HTTP 系统账号与房间 Root 身份分别验证。账号仅保存在当前页面内存中，刷新页面即退出。</p></div>';
  if (!systemAdminApi.loggedIn) return intro+'<form class="admin-control-card system-admin-login" id="system-admin-login-form" novalidate><div class="admin-control-copy"><strong>登录系统管理员账号</strong><p>请输入后端 HTTP Basic Auth 的用户名与密码，验证后才能查看或修改系统配置。</p></div><label class="form-field">用户名<input id="system-admin-user" class="field" autocomplete="username" aria-label="系统管理员用户名"></label><label class="form-field">密码<input id="system-admin-password" class="field" type="password" autocomplete="current-password" aria-label="系统管理员密码"></label><p class="system-admin-login-status" id="system-admin-login-status" role="status" aria-live="polite"></p><div class="form-actions"><button class="primary-button" type="submit">验证并登录</button><button class="secondary-button" type="button" data-panel="admin">返回房管控制台</button></div></form>';
  const tabs=[['rooms','房间管理'],['retain','永存码'],['users','房间用户'],['service','系统与音乐']];
  const nav='<div class="system-admin-nav" role="tablist" aria-label="系统管理分类">'+tabs.map(([id,label])=>`<button type="button" role="tab" data-command="system-tab" data-tab="${id}" aria-selected="${session.tab===id}" class="${session.tab===id?'active':''}">${label}</button>`).join('')+'</div>';
  let body='';
  if (session.tab==='rooms') {
    const room=state.rooms.find(item=>String(item.id)===String(session.selectedRoomId));
    const options=state.rooms.map(item=>`<option value="${escapeHTML(item.id)}">${escapeHTML(item.name)} · ${escapeHTML(item.id)}</option>`).join('');
    body='<section class="admin-control-card"><div class="admin-control-copy"><strong>修改房间</strong><p>输入房间名称或 ID 可筛选候选房间；选中后名称与介绍会回填。</p></div><div class="inline-control"><input id="system-room-select" class="field" list="system-room-options" value="'+escapeHTML(room?.id || '')+'" placeholder="输入房间名称或 ID 筛选" aria-label="筛选并选择要修改的房间"><datalist id="system-room-options">'+options+'</datalist><button class="secondary-button" type="button" data-command="system-refresh-rooms">刷新房间</button></div>';
    if (room) body+=`<div class="system-admin-room-meta">房间 ID：<button type="button" class="text-button" data-copy-text="${escapeHTML(room.id)}" aria-label="复制房间 ID">${escapeHTML(room.id)}</button> · ${room.count} 人 · 当前${room.locked?'有':'无'}访客密码</div><div class="system-admin-edit-grid"><label class="form-field">房间名称<input id="system-room-name" class="field" maxlength="33" value="${escapeHTML(room.name)}"></label><label class="form-field">房间介绍<input id="system-room-desc" class="field" maxlength="133" value="${escapeHTML(room.raw?.desc ?? '')}"></label><label class="form-field">访客密码状态<select id="system-room-need-pwd" class="field"><option value="">保持原值</option><option value="true">启用密码</option><option value="false">关闭密码</option></select></label><label class="form-field">新访客密码<input id="system-room-password" class="field" type="password" autocomplete="new-password" placeholder="不修改则留空"></label><label class="form-field">永久保留状态<select id="system-room-permanent" class="field"><option value="">保持原值</option><option value="true">启用</option><option value="false">关闭</option></select></label><label class="form-field">禁止房管修改密码<select id="system-room-forbid-pwd" class="field"><option value="">保持原值</option><option value="true">禁止</option><option value="false">允许</option></select></label><label class="form-field">新房管密码<input id="system-room-admin-pwd" class="field" type="password" autocomplete="new-password" placeholder="不修改则留空"></label><label class="form-field">销毁房间（canDestroy）<select id="system-room-can-destroy" class="field" aria-label="是否销毁所选房间"><option value="false">false · 不销毁</option><option value="true">true · 立即销毁</option></select></label></div><p class="system-admin-destroy-hint">选择 true 后保存会立即尝试销毁房间，且不会保存其他修改；有人在线时后端会拒绝。false 仅表示本次不销毁，不是永久设置。</p><div class="form-actions"><button class="primary-button" type="button" data-command="system-save-room">保存房间修改</button></div>`;
    body+='</section>';
  } else if (session.tab==='retain') {
    const keys=Array.isArray(session.retainKeys)?session.retainKeys:[];
    body='<section class="admin-control-card"><div class="admin-control-copy"><strong>添加永存码</strong><p>新永存码可用于创建永久保留的房间。</p></div><div class="inline-control"><input id="system-retain-key" class="field" maxlength="160" placeholder="输入新的永存码" aria-label="新永存码"><button class="primary-button" type="button" data-command="system-add-key">添加永存码</button></div></section><section class="admin-control-card"><div class="admin-control-copy"><strong>所有永存码</strong><p>共 '+keys.length+' 个。移除前需要二次确认。</p></div><div class="form-actions"><button class="secondary-button" type="button" data-command="system-refresh-keys">刷新列表</button></div><div class="system-admin-list">'+(keys.length?keys.map(item=>{const key=String(item?.key ?? item ?? '');if(!key)return '';const pending=session.pendingRemovalKey===key;return `<div class="system-admin-item"><div><strong title="${escapeHTML(key)}">${escapeHTML(key)}</strong><small>${item?.isUsed?'已使用':'未使用'} · 创建于 ${escapeHTML(systemAdminDate(item?.createTime))}${item?.houseId?' · 房间 '+escapeHTML(item.houseId):''}</small></div><div class="form-actions"><button class="secondary-button" type="button" data-copy-text="${escapeHTML(key)}" aria-label="复制永存码">复制</button>${pending?`<button class="danger-button" type="button" data-command="system-confirm-remove-key" data-key="${escapeHTML(key)}">确认移除</button><button class="secondary-button" type="button" data-command="system-cancel-remove-key">取消</button>`:`<button class="danger-button" type="button" data-command="system-remove-key" data-key="${escapeHTML(key)}">移除</button>`}</div></div>`;}).join(''):'<div class="system-message">暂无永存码</div>')+'</div></section>';
  } else if (session.tab==='users') {
    const groups=session.users && typeof session.users==='object'?Object.entries(session.users):[];
    const count=groups.reduce((sum,[,users])=>sum+(Array.isArray(users)?users.length:0),0);
    const topIPs = computeTopIPs(session.users || {}, 10);
    const activeIp = session.selectedIp || (topIPs[0]?.ip ?? '');
    const activeIPGroup = topIPs.find((item) => item.ip === activeIp) || topIPs[0];

    const ipCardHTML = `<section class="admin-control-card system-admin-ip-card"><div class="admin-control-copy"><strong>IP 相同用户 Top 10 统计</strong><p>自动汇总同 IP 在线用户。点击下方的 IP 卡片，可查看该 IP 对应的所有用户详细信息及所在房间。</p></div><div class="system-admin-ip-grid">${topIPs.length ? topIPs.map((item) => `<button type="button" class="system-admin-ip-badge ${item.ip === activeIPGroup?.ip ? 'active' : ''}" data-command="select-system-ip" data-ip="${escapeHTML(item.ip)}"><strong>${escapeHTML(item.ip)}</strong><small>${item.userCount} 位用户 · ${item.roomCount} 个房间</small></button>`).join('') : '<div class="system-message">暂无 IP 相同数据</div>'}</div>${activeIPGroup ? `<div class="system-admin-ip-detail"><div class="system-admin-ip-header"><strong>IP 详情：${escapeHTML(activeIPGroup.ip)}</strong><span>共 ${activeIPGroup.userCount} 位用户，分布在 ${activeIPGroup.roomCount} 个房间</span></div><div class="system-admin-ip-rooms"><strong>关联房间：</strong>${activeIPGroup.rooms.map((r) => `<span class="room-chip">${escapeHTML(r.label)} (${r.count}人)</span>`).join(' ')}</div><div class="system-admin-ip-users"><strong>用户列表：</strong><div class="system-admin-list">${activeIPGroup.users.map(systemAdminUserHTML).join('')}</div></div></div>` : ''}</section>`;

    const filterKeyword = String(session.userIpFilter || '').trim().toLowerCase();
    const filteredGroups = groups.map(([label, users]) => {
      if (!Array.isArray(users)) return [label, []];
      if (!filterKeyword) return [label, users];
      const matched = users.filter((u) => {
        const ip = extractUserIP(u);
        const name = u.name || u.nickName || u.sessionId || '';
        return `${ip} ${name}`.toLowerCase().includes(filterKeyword);
      });
      return [label, matched];
    }).filter(([, users]) => users.length > 0 || !filterKeyword);

    const userListHTML = '<section class="admin-control-card"><div class="admin-control-copy"><strong>所有房间用户</strong><p>共 '+groups.length+' 个房间、'+count+' 位用户。按房间分组查看。</p></div><div class="inline-control"><input id="system-user-ip-filter" class="field" placeholder="按 IP 或用户名筛选" value="'+escapeHTML(session.userIpFilter || '')+'" aria-label="筛选房间用户"><button class="secondary-button" type="button" data-command="system-refresh-users">刷新用户</button></div><div class="system-admin-list">'+(filteredGroups.length?filteredGroups.map(([label,users])=>`<section class="system-admin-user-group"><h3 title="${escapeHTML(label)}">${escapeHTML(label)} · ${users.length} 人</h3>${users.length?users.map(systemAdminUserHTML).join(''):'<div class="system-message">没有匹配的用户</div>'}</section>`).join(''):'<div class="system-message">暂无匹配的用户数据</div>')+'</div></section>';

    body = ipCardHTML + userListHTML;
  } else {
    body='<section class="admin-control-card"><div class="admin-control-copy"><strong>房间数量上限</strong><p>设置系统允许同时存在的最多房间数；此设置可能由后端重启配置覆盖。</p></div><div class="inline-control"><input id="system-room-limit" class="field" type="number" min="1" max="10000" step="1" placeholder="输入房间数量" aria-label="最多房间数量"><button class="primary-button" type="button" data-command="system-set-limit">保存上限</button></div><p class="system-admin-limit-status" id="system-room-limit-status" role="status">'+(session.roomLimitResult?escapeHTML(systemRoomLimitText(session.roomLimitResult)):'保存后显示剩余可创建房间数')+'</p></section><section class="admin-control-card"><div class="admin-control-copy"><strong>网易云音乐 Cookie</strong><p>提交新 Cookie 后只显示结果，不在页面或本地存储中回显。</p></div><div class="inline-control"><input id="system-netease-cookie" class="field" type="password" autocomplete="off" placeholder="粘贴 Cookie 串" aria-label="网易云音乐 Cookie"><button class="primary-button" type="button" data-command="system-set-cookie">更新 Cookie</button></div></section>';
  }
  return intro+'<div class="system-admin-session"><span>系统账号已验证</span><button class="text-button" type="button" data-command="system-logout">退出系统账号</button></div>'+nav+(session.loading?'<div class="system-message">正在读取数据…</div>':'')+body;
}

function playlistPanelHTML(){
  const source=state.playlistSource;
  const platform=source.startsWith('qq')?'qq':'wy';
  const category=source.endsWith('_userdj')?'radio':source.endsWith('_user')?'user':'playlist';
  const placeholder=category==='user'?(platform==='qq'?'输入 QQ 号搜索该用户的歌单':'输入网易用户 ID；不知道 ID 可点“网易用户”搜索昵称'):category==='radio'?'输入网易用户 ID 搜索其电台':'可留空搜索平台歌单';
  const header='<div class="panel-switcher"><strong>平台歌单</strong><span class="panel-switcher-actions"><button class="secondary-button" type="button" data-panel="playlist-users">网易用户</button><button class="secondary-button" type="button" data-panel="search">前往歌曲搜索</button></span></div>';
  const navigation=`${state.playlistView!=='search'?'<div class="form-actions"><button class="secondary-button" type="button" data-command="playlist-back">返回网易用户</button></div>':''}<div class="playlist-source-picker" role="group" aria-label="歌单搜索来源"><div class="playlist-choice-row"><span>平台</span><button class="${platform==='wy'?'active':''}" type="button" data-playlist-platform="wy" aria-pressed="${platform==='wy'}">网易</button><button class="${platform==='qq'?'active':''}" type="button" data-playlist-platform="qq" aria-pressed="${platform==='qq'}">QQ</button></div><div class="playlist-choice-row"><span>类型</span><button class="${category==='playlist'?'active':''}" type="button" data-playlist-kind="playlist" aria-pressed="${category==='playlist'}">歌单</button><button class="${category==='user'?'active':''}" type="button" data-playlist-kind="user" aria-pressed="${category==='user'}">用户</button>${platform==='wy'?`<button class="${category==='radio'?'active':''}" type="button" data-playlist-kind="radio" aria-pressed="${category==='radio'}">电台</button>`:''}</div></div><div class="search-bar"><input id="playlist-search" value="${escapeHTML(state.playlistKeyword)}" placeholder="${placeholder}" aria-label="搜索歌单"><button class="primary-button" type="button" data-command="search-playlist">搜索</button></div>`;
  return header+navigation+totalHTML(state.playlistResults.length,state.playlistTotal,category==='radio'?'个电台':'个歌单')+`<div class="result-list" id="playlist-results">${playlistResultsHTML()}</div>`;
}
function playlistUsersPanelHTML(){
  return `<div class="panel-switcher"><strong>网易用户</strong><button class="secondary-button" type="button" data-command="playlist-home">返回平台歌单</button></div><div class="search-bar"><input id="playlist-user-search" value="${escapeHTML(state.userSearchKeyword)}" placeholder="输入网易用户昵称" aria-label="搜索网易用户"><button class="primary-button" type="button" data-command="search-netease-user">搜索用户</button></div>${totalHTML(state.userResults.length,state.userTotal,'位用户')}<div class="result-list" id="playlist-user-results">${userResultsHTML()}</div>`;
}

const panels = {
  'default-list': {kicker:'DEFAULT PLAYLIST',title:'默认列表',description:'没有人点歌时使用的歌曲列表。',body:defaultPlaylistHTML},
  search: {
    kicker: 'DISCOVER', title: '搜索点歌', description: '跨音乐源搜索歌曲，并选择标准或无损音质。',
    body: () => `<div class="panel-switcher"><strong>歌曲搜索</strong><span class="panel-switcher-actions"><button class="secondary-button" type="button" data-panel="hot">热歌榜</button><button class="secondary-button" type="button" data-panel="playlist">前往平台歌单</button></span></div><div class="panel-tabs" data-tab-group="source">${['网易','QQ','电台','禁歌'].map((item) => `<button class="${item === state.source ? 'active' : ''}" type="button" data-local-tab data-source="${item}">${item}</button>`).join('')}</div>
      <div class="search-bar"><input id="song-search" placeholder="输入歌名、歌手或歌曲 ID" aria-label="搜索歌曲或输入歌曲 ID"><button class="primary-button" type="button" data-command="search-song">${icon('search')}搜索</button><button class="secondary-button" type="button" data-command="pick-by-id" title="不搜索，直接按当前音源点歌">ID 点歌</button></div>
      ${totalHTML(state.searchResults.length,state.searchTotal,'首歌曲')}<div class="result-list" id="search-results">${searchResultsHTML()}</div>`
  },
  rooms: {
    kicker: 'ROOMS', title: '发现听歌房', description: '切换房间、搜索房间，或创建属于自己的空间。',
    body: () => `<div class="search-bar room-list-toolbar"><input id="room-search" value="${escapeHTML(state.roomFilter)}" placeholder="搜索房间名称或介绍" aria-label="搜索房间"><button class="secondary-button" type="button" data-command="refresh-rooms">刷新</button><button class="secondary-button room-list-random" type="button" data-action="time-warp">随机进入</button><button class="primary-button" type="button" data-panel="create-room">创建房间</button></div><div class="room-filters"><label><input id="hide-empty-rooms" type="checkbox" ${state.hideEmptyRooms?'checked':''}>隐藏空房</label><label><input id="hide-locked-rooms" type="checkbox" ${state.hideLockedRooms?'checked':''}>隐藏密码房</label><label><input id="hide-unlocked-rooms" type="checkbox" ${state.hideUnlockedRooms?'checked':''}>隐藏无密码房</label></div>${roomTotalHTML()}
      <div class="result-list room-results-grid" id="room-search-results">${roomResultsHTML()}</div>`
  },
  'create-room': {
    kicker: 'NEW ROOM', title: '创建听歌房', description: '设置名称、介绍、访问权限和房间保留方式。',
    body: () => `<form data-form="create-room"><div class="form-grid"><label class="form-field">房间名称<input class="field" name="name" required placeholder="例如：深夜唱片馆"></label><label class="form-field">房间介绍<input class="field" name="desc" required placeholder="告诉大家这里听什么"></label></div><div class="conditional-inline"><label><input id="toggle-room-password" name="needPwd" type="checkbox">设置房间密码</label><input class="field" data-conditional="room-password" name="password" type="password" placeholder="输入访客密码" hidden></div><div class="conditional-inline"><label><input id="toggle-room-permanent" name="enableStatus" type="checkbox">永久保留房间</label><input class="field" data-conditional="room-permanent" name="retain" placeholder="填写永久房间订单号" hidden><a class="sponsor-heart" href="https://tx.alang.run/sponsor" target="_blank" rel="noopener" aria-label="赞赏支持永久房间" title="赞赏支持">♥</a></div><div class="form-actions"><button class="primary-button" type="submit">创建并进入</button><button class="secondary-button" type="button" data-panel="rooms">返回房间列表</button></div></form>`
  },
  'room-password': {
    kicker:'PRIVATE ROOM', title:()=>state.pendingRoom?.name || '密码房间', description:'输入房间密码后建立连接。',
    body:()=>`<form data-form="room-password"><label class="form-field">房间密码<input class="field" name="password" type="password" required autofocus placeholder="请输入房间密码"></label><div class="form-actions"><button class="primary-button" type="submit">验证并进入</button><button class="secondary-button" type="button" data-panel="rooms">返回房间列表</button></div></form>`
  },
  favorites: {
    kicker: 'COLLECTION', title: '我的收藏', description: '管理收藏歌曲，支持播放、导入与导出。',
    body: () => `<input id="favorite-import" type="file" accept="application/json,.json" hidden><div class="form-actions"><button class="primary-button" type="button" data-action="play-all">${icon('play_arrow')}播放全部</button><button class="secondary-button" type="button" data-command="export-favorites">${icon('download')}导出</button><button class="secondary-button" type="button" data-command="import-favorites">${icon('upload')}导入</button><button class="danger-button" type="button" data-command="clear-favorites">清空</button></div>${totalHTML(state.favorites.length,state.favorites.length,'首收藏')}<div class="result-list">${favoriteResultsHTML()}</div>`
  },
  history: {
    kicker: 'HISTORY', title: '点歌历史', description: '查看自己的点歌记录或整个房间的历史。',
    body: () => `<div class="panel-tabs history-tabs"><button class="${state.historyMode==='mine'?'active':''}" type="button" data-local-tab data-history-mode="mine">只看自己</button><button class="${state.historyMode==='all'?'active':''}" type="button" data-local-tab data-history-mode="all">查看全部</button><button type="button" data-command="clear-history">清空</button><span class="history-count" id="history-count" role="status">当前 ${filteredHistory().length} 首</span></div><div class="search-bar"><input id="history-search" value="${escapeHTML(state.historyFilter)}" placeholder="搜索点歌人、歌手或歌名"></div><div class="result-list" id="history-results">${historyResultsHTML()}</div>`
  },
  playlist: {
    kicker: 'PLAYLISTS', title: '平台歌单', description: '搜索网易、QQ 平台歌单、用户歌单与网易电台节目。',
    body: playlistPanelHTML
  },
  'playlist-users': {
    kicker: 'NETEASE USERS', title: '网易用户', description: '搜索网易用户，再查看其歌单或电台。',
    body: playlistUsersPanelHTML
  },
  share: {
    kicker: 'SHARE', title: '分享房间', description: '邀请朋友加入当前房间，一起实时听歌。',
    body: () => `<p class="share-description"><strong>${escapeHTML(state.room)}</strong><br>${escapeHTML(state.roomDescription || '欢迎来到这个听歌房，一起实时听歌和聊天。')}</p><div class="share-codes"><div class="feature-card centered"><strong>网页链接二维码</strong><div id="web-qr" class="share-code-image" aria-label="网页链接二维码"></div><p>${escapeHTML(roomShareUrl())}</p></div><div class="feature-card centered"><strong>微信小程序码</strong>${state.miniCode?`<img class="share-code-image" src="data:image/jpeg;base64,${state.miniCode}" alt="微信小程序码">`:state.miniCodeError?'<div class="system-message">小程序码服务暂不可用，请稍后重试</div>':'<div class="system-message">正在读取小程序码…</div>'}</div></div><div class="form-actions" style="justify-content:center"><button class="primary-button" type="button" data-command="copy-room-link">复制房间链接</button><button class="secondary-button" type="button" data-command="open-room-link">打开直达链接</button></div>`
  },
  profile: {
    kicker: 'PROFILE', title: '个人设置', description: '修改昵称、发起私聊或联系房间管理员。',
    body: () => `<form class="user-control-console" data-form="profile"><section class="user-control-card identity-card"><div class="user-control-copy"><strong>我的用户 ID</strong><p>${escapeHTML(state.mySessionId || '会话建立后显示')}</p></div>${state.mySessionId?`<button class="secondary-button" type="button" data-copy-text="${escapeHTML(state.mySessionId)}">复制 ID</button>`:''}</section><section class="user-control-card"><div class="user-control-copy"><strong>个人昵称</strong><p>设置你在当前听歌房和聊天区域显示的名称。</p></div><label class="form-field">昵称<div class="inline-control"><input class="field" name="nickname" value="${escapeHTML(state.nickname)}" placeholder="输入你的昵称"><button class="primary-button" type="submit">保存昵称</button></div></label></section><section class="user-control-card"><div class="user-control-copy"><strong>发送私聊</strong><p>输入对方用户 ID 和消息，只发送给指定成员。</p></div><div class="form-grid"><label class="form-field">私聊用户 ID<input class="field" name="userId" placeholder="例如 ju2etxv2"></label><label class="form-field">私聊内容<textarea class="field" name="privateMessage" placeholder="输入私聊消息"></textarea></label></div><div class="control-action-row"><button class="primary-button" type="button" data-command="private-message">发送私聊</button></div></section><section class="user-control-card"><div class="user-control-copy"><strong>联系管理员</strong><p>向当前房间管理员反馈问题或建议。</p></div><label class="form-field">留言内容<textarea class="field" name="adminMessage" placeholder="输入问题或建议"></textarea></label><div class="control-action-row"><button class="secondary-button" type="button" data-command="contact-admin">发送给管理员</button></div></section></form>`
  },
  members: {
    kicker: 'ONLINE', title: () => `在线成员 · ${state.online}`, description: '查看成员状态，发起私聊或执行房间管理操作。',
    body: () => `<div class="form-actions"><button class="secondary-button" type="button" data-command="load-members">刷新成员</button></div>${totalHTML(state.members.length,state.online,'位成员')}<div class="result-list" id="members-results">${state.members.length ? state.members.map((user) => {const me=isCurrentMember(user);return `<div class="result-row ${me?'is-current-user':''}"><span class="result-art">${animalFor(user.sessionId)}</span><span><strong>${escapeHTML(userDisplayName(user))}${me?'<em class="member-self-badge">本人</em>':''}</strong><small>${escapeHTML(user.sessionId || '')} · 在线</small></span><button class="secondary-button" type="button" data-command="open-user-actions" data-user="${escapeHTML(user.sessionId || '')}" data-name="${escapeHTML(userDisplayName(user))}">${me?'查看 ID':'操作'}</button></div>`;}).join('') : '<div class="system-message">点击刷新成员获取在线列表</div>'}</div>`
  },
  'user-actions': {
    kicker:'MEMBER', title:()=>state.selectedUser?.name || '成员操作', description:'发起私聊；管理员可拉黑、漂白或设置用户权限。',
    body:()=>`<div class="feature-card"><span class="result-art" style="display:grid;place-items:center;font-size:28px">${animalFor(state.selectedUser?.id)}</span><strong>${escapeHTML(state.selectedUser?.name || '匿名')}</strong><p>${escapeHTML(state.selectedUser?.id || '')}</p></div><div class="form-actions"><button class="primary-button" type="button" data-command="prepare-private" data-user="${escapeHTML(state.selectedUser?.id || '')}">发起私聊</button>${state.isAdmin?`<button class="danger-button" type="button" data-command="manage-chat-user" data-operation="black">拉黑用户</button><button class="secondary-button" type="button" data-command="manage-chat-user" data-operation="unblack">漂白用户</button><button class="secondary-button" type="button" data-command="manage-chat-user" data-operation="picker">设为点歌人</button><button class="secondary-button" type="button" data-command="manage-chat-user" data-operation="voter">设为切歌人</button>`:''}</div>`
  },
  emoji: {
    kicker: 'EMOJI', title: '选择表情', description: '点击表情即可插入聊天输入框。',
    body: () => `<div class="emoji-grid">${['😀','😃','😄','😁','😆','😂','🤣','😊','😍','🥰','😘','😎','🤩','🥳','😴','😭','😤','😡','🤔','🤗','🤭','🫡','😇','🙃','👍','👎','👏','🙌','🤝','🙏','💪','✌️','❤️','🧡','💛','💚','💙','💜','🖤','🤍','🔥','✨','🌙','☀️','🌊','🍻','☕','🎵','🎧','🎤','🎸','🎹','🥁','💿','📻','🎉','🌿','🌵','🐱','🐶','🐰','🦊','🐼','🐨'].map((emoji) => `<button type="button" data-emoji="${emoji}" aria-label="插入 ${emoji}">${emoji}</button>`).join('')}</div><div class="form-actions" style="justify-content:center"><button class="secondary-button" type="button" data-close-modal>关闭表情</button></div>`
  },
  picture: {
    kicker: 'MEMES', title: '斗图搜索', description: '搜索图片并放入聊天发送框。',
    body: () => `<div class="search-bar"><input id="picture-search" value="${escapeHTML(state.pictureKeyword)}" aria-label="斗图关键词"><button class="primary-button" type="button" data-command="search-picture">搜索</button></div>${totalHTML(state.pictureResults.length,state.pictureTotal,'张图片')}<div class="image-grid" id="picture-results" style="margin-top:14px">${state.pictureResults.length ? state.pictureResults.map((item)=>{const url=escapeHTML(item.url || item.path || '');return `<div class="image-card"><button class="image-card-preview" type="button" data-preview-image="${url}" data-preview-send="true" aria-label="放大查看图片"><img src="${url}" alt="斗图图片" loading="lazy" decoding="async"></button><button class="image-card-send" type="button" data-command="send-picture" data-url="${url}" aria-label="发送图片到聊天">发送</button></div>`;}).join('') : '<div class="system-message">输入关键词搜索图片</div>'}</div>`
  },
  hot: {
    kicker: 'TRENDING', title: '热歌榜', description: '选择网易或 QQ，读取最新热歌榜。',
    body: () => `<div class="panel-tabs"><button class="${state.hotSource==='wy'?'active':''}" type="button" data-local-tab data-hot-source="wy">网易</button><button class="${state.hotSource==='qq'?'active':''}" type="button" data-local-tab data-hot-source="qq">QQ</button></div>${totalHTML(state.hotResults.length,state.hotTotal,'首歌曲')}<div class="result-list" id="hot-results">${hotResultsHTML()}</div>`
  },
  'music-blacklist': { kicker:'SECURITY',title:'音乐黑名单',description:'查看当前房间已拉黑的歌曲。',body:()=>`<div class="form-actions"><button class="primary-button" type="button" data-command="load-blacklist" data-kind="music">刷新音乐黑名单</button><button class="secondary-button" type="button" data-panel="admin">返回房管控制台</button></div><div class="result-list">${blacklistHTML('music')}</div>` },
  'user-blacklist': { kicker:'SECURITY',title:'用户黑名单',description:'查看当前房间已拉黑的成员。',body:()=>`<div class="form-actions"><button class="primary-button" type="button" data-command="load-blacklist" data-kind="user">刷新用户黑名单</button><button class="secondary-button" type="button" data-panel="admin">返回房管控制台</button></div><div class="result-list">${blacklistHTML('user')}</div>` },
  'queue-settings': {
    kicker: 'QUEUE', title: '队列设置', description: '调整播放方式和队列展示。',
    body: () => state.isAdmin?`<div class="panel-grid"><button class="feature-card" type="button" data-command="admin-raw" data-destination="/music/goodmodel/true"><strong>点赞排序</strong><p>按点赞数调整播放顺序</p></button><button class="feature-card" type="button" data-command="admin-raw" data-destination="/music/randommodel/true"><strong>随机播放</strong><p>开启随机播放模式</p></button><button class="feature-card" type="button" data-command="admin-raw" data-destination="/music/musiccirclemodel/true"><strong>单曲循环</strong><p>循环当前歌曲</p></button><button class="feature-card" type="button" data-command="admin-raw" data-destination="/music/musiclistmodel/true"><strong>列表循环</strong><p>循环默认播放列表</p></button></div><div class="form-actions"><button class="danger-button" type="button" data-command="clear-queue">清空待播队列</button></div>`:'<div class="system-message">请先到房管控制台登录，登录后显示队列管理操作。</div>'
  },
  'confirm-clear-queue': {
    kicker:'DANGER ZONE', title:'确认清空待播队列', description:'这是房间级操作，请确认后再继续。',
    body:()=>`<div class="confirm-action-card"><span class="confirm-action-icon">${icon('delete_sweep')}</span><div><strong>清空所有待播放歌曲？</strong><p>正在播放的歌曲不会受到影响，但待播队列中的歌曲会被全部移除，此操作无法撤销。</p></div></div><div class="confirm-action-buttons"><button class="secondary-button" type="button" data-panel="queue-settings">取消，返回设置</button><button class="danger-button" type="button" data-command="confirm-clear-queue">确认清空队列</button></div>`
  },
  more: {
    kicker: 'TOOLBOX', title: '全部功能', description: '原网站功能的统一入口。',
    body: () => `<div class="panel-grid">${featureCard('favorites','favorite','我的收藏','快速播放和管理收藏')}${featureCard('hot','graphic_eq','热歌榜','网易与 QQ 热门歌曲')}${featureCard('search','search','搜索点歌','四个音乐源与无损音质')}${featureCard('playlist','queue_music','平台歌单','歌单、用户和电台节目')}${featureCard('history','history','点歌历史','个人与全房历史记录')}${featureCard('rooms','meeting_room','听歌房','搜索、切换和创建房间')}${featureCard('members','group','在线成员','私聊与成员管理')}${featureCard('picture','image','斗图搜索','搜索并发送聊天图片')}${featureCard('guide','person','用户控制台','个人工具与倒计时退出')}${featureCard('admin','admin_panel_settings','房管控制台','播放、权限和黑名单')}${linkCard('https://tx.alang.run/sponsor','favorite','赞赏支持','支持一起听歌吧持续维护')}${linkCard('https://tx.alang.run/release','download','下载客户端','获取最新版桌面与移动客户端')}</div>`
  },
  guide: {
    kicker: 'USER CONSOLE', title: '用户控制台', description: '个人资料、房间工具与倒计时退出。',
    body: () => `<div class="panel-grid">${featureCard('profile','person','个人资料','修改昵称、私聊与联系管理员')}${featureCard('members','group','在线成员','查看成员并发起私聊')}${featureCard('share','ios_share','分享房间','复制链接与生成二维码')}</div><section class="countdown-card"><div><strong>倒计时退出当前房间</strong><p id="countdown-status">${escapeHTML(countdownStatus())}</p></div><div class="search-bar"><input id="countdown-minutes" type="number" min="1" step="1" value="5" aria-label="倒计时分钟数"><button class="primary-button" type="button" data-command="set-countdown">设置定时</button><button class="danger-button" type="button" data-command="cancel-countdown">取消定时</button></div></section>`
  },
  admin: {
    kicker: 'ADMIN', title: '房管控制台', description: '管理房间播放、访客权限、队列、歌曲和成员。',
    body: () => `<div class="admin-console"><section class="admin-control-card admin-login-card"><div class="admin-control-copy"><strong>管理员身份</strong><p>${state.isAdmin?'当前已登录，可重新验证或修改管理员密码。':'输入当前房间管理员密码后登录。'}</p></div><label class="form-field">管理员密码<div class="inline-control"><input id="admin-password" class="field" type="password" placeholder="输入管理员密码"><button class="primary-button" type="button" data-command="admin-login">${state.isAdmin?'重新验证':'管理员登录'}</button>${state.isAdmin?'<button class="secondary-button" type="button" data-command="admin-password">修改密码</button>':''}</div></label></section>${state.isAdmin?`<div class="admin-control-grid"><section class="admin-control-card"><div class="admin-control-copy"><strong>房间公告</strong><p>更新进入房间后展示给所有成员的公告。</p></div><label class="form-field">公告内容<div class="inline-control"><input id="admin-announcement" class="field" placeholder="输入新公告"><button class="primary-button" type="button" data-command="admin-announce">发布公告</button></div></label></section><section class="admin-control-card"><div class="admin-control-copy"><strong>切歌票数比例</strong><p>设置投票切歌需要达到的在线成员比例。</p></div><label class="form-field">票数比例<div class="inline-control"><input id="admin-vote" class="field" type="number" value="${state.roomState?.voteRate ?? ''}" placeholder="状态待同步" step="0.01" min="0.01" max="1"><button class="primary-button" type="button" data-command="admin-vote">保存比例</button></div></label></section><section class="admin-control-card"><div class="admin-control-copy"><strong>播放模式</strong><p>选择房间队列的播放与排序方式。</p></div><div class="admin-option-actions"><button class="secondary-button" type="button" data-command="admin-raw" data-destination="/music/goodmodel/true">点赞排序</button><button class="secondary-button" type="button" data-command="admin-raw" data-destination="/music/randommodel/true">随机播放</button><button class="secondary-button" type="button" data-command="admin-raw" data-destination="/music/musiccirclemodel/true">单曲循环</button><button class="secondary-button" type="button" data-command="admin-raw" data-destination="/music/musiclistmodel/true">列表循环</button></div></section><section class="admin-control-card"><div class="admin-control-copy"><strong>访客权限</strong><p>控制普通成员能否点歌或发起切歌。</p></div><div class="admin-option-actions"><button class="danger-button" type="button" data-command="admin-raw" data-destination="/music/banchoose/true">禁止点歌</button><button class="secondary-button" type="button" data-command="admin-raw" data-destination="/music/banchoose/false">允许点歌</button><button class="danger-button" type="button" data-command="admin-raw" data-destination="/music/banswitch/true">禁止切歌</button><button class="secondary-button" type="button" data-command="admin-raw" data-destination="/music/banswitch/false">启用切歌</button></div></section><section class="admin-control-card"><div class="admin-control-copy"><strong>默认队列</strong><p>追加歌单、查看默认列表歌曲数，或清空默认队列。</p></div><label class="form-field">默认歌单 ID<div class="inline-control"><input id="admin-playlist" class="field" placeholder="输入歌单 ID"><button class="primary-button" type="button" data-command="admin-playlist">追加歌单</button></div></label><div class="admin-option-actions"><button class="secondary-button" type="button" data-command="admin-raw" data-destination="/music/playlistSize">查看歌曲数</button><button class="danger-button" type="button" data-command="admin-raw" data-destination="/music/clearDefaultPlayList">清空默认</button></div></section><section class="admin-control-card"><div class="admin-control-copy"><strong>歌曲管理</strong><p>输入歌曲 ID 后置顶、拉黑或解除拉黑。</p></div><label class="form-field">歌曲 ID<input id="admin-song" class="field" placeholder="输入歌曲 ID"></label><div class="admin-option-actions"><button class="secondary-button" type="button" data-command="admin-song" data-operation="top">置顶歌曲</button><button class="danger-button" type="button" data-command="admin-song" data-operation="black">拉黑歌曲</button><button class="secondary-button" type="button" data-command="admin-song" data-operation="unblack">解除拉黑</button></div></section><section class="admin-control-card"><div class="admin-control-copy"><strong>成员权限</strong><p>输入用户 ID 后调整该成员的房间权限。</p></div><label class="form-field">用户 ID<input id="admin-user" class="field" placeholder="输入用户 ID"></label><div class="admin-option-actions"><button class="danger-button" type="button" data-command="admin-user" data-operation="black">拉黑用户</button><button class="secondary-button" type="button" data-command="admin-user" data-operation="picker">设为点歌人</button><button class="secondary-button" type="button" data-command="admin-user" data-operation="voter">设为切歌人</button></div></section></div>`:'<div class="system-message">登录成功后才会显示房间管理操作</div>'}</div>`
  },
  'system-admin': {
    kicker:'SYSTEM', title:'管理员控制台', description:'仅供 Root 身份与系统账号双重验证后使用。',
    body:systemAdminHTML
  },
  about: {
    kicker: 'ABOUT', title: '一起听歌吧', description: '实时音乐聊天室的新一代前端。',
    body: () => `<div class="panel-grid">${featureCard('profile','person','个人资料','昵称、私聊与联系管理员')}${featureCard('share','ios_share','分享房间','链接、二维码和分享海报')}${featureCard('guide','person','用户控制台','成员工具与倒计时退出')}${featureCard('admin','shield','房间管理','房管控制台')}</div>`
  }
};

function setAdminPlaylistStatus(text){
  state.adminPlaylistStatus=text;
  const status=$('#admin-playlist-status');if(status)status.textContent=text;
}

function enhancePairedAdminActions(scope) {
  if(currentPanel==='more'&&!$('[data-action="toggle-focus"]',scope))$('.panel-grid',scope)?.insertAdjacentHTML('beforeend',actionCard('toggle-focus','fullscreen','简洁模式','沉浸显示当前歌曲与大字同步歌词'));
  if(currentPanel==='admin'){
    setRoomAnnouncement(state.roomAnnouncement);
    const playlistInput=$('#admin-playlist',scope);
    if(playlistInput && !$('.admin-playlist-source',scope))playlistInput.closest('.form-field')?.insertAdjacentHTML('beforebegin',`<div class="admin-playlist-source" role="radiogroup" aria-label="歌单来源"><strong>歌单来源</strong><label><input type="radio" name="admin-playlist-source" value="wy" ${state.adminPlaylistSource==='wy'?'checked':''}><span>网易</span></label><label><input type="radio" name="admin-playlist-source" value="qq" ${state.adminPlaylistSource==='qq'?'checked':''}><span>QQ</span></label></div>`);
    if(playlistInput){playlistInput.placeholder='歌单 ID，或 *歌曲 ID';if(!$('#admin-playlist-status',scope))playlistInput.closest('.form-field')?.insertAdjacentHTML('afterend',`<p class="admin-playlist-hint">多个歌单 ID 用逗号分隔；单曲输入 *歌曲ID，QQ 单曲请使用 songmid。</p><p id="admin-playlist-status" class="admin-playlist-status" role="status">${escapeHTML(state.adminPlaylistStatus)}</p>`);}
    for (const [destination, label] of [['/music/banchoose/true', '禁止点歌'], ['/music/banchoose/false', '允许点歌'], ['/music/banswitch/true', '禁止切歌'], ['/music/banswitch/false', '允许切歌']]) {
      const button = $(`[data-destination="${destination}"]`, scope);
      if (!button) continue;
      button.textContent = label;
      button.classList.remove('danger-button');
      button.classList.add('secondary-button');
    }
    const consoleBox=$('.admin-console',scope);
    if(state.isRoot && consoleBox) consoleBox.insertAdjacentHTML('afterbegin',`<section class="admin-control-card root-control-card"><div class="admin-control-copy"><strong>超级管理员</strong><p>Root 权限已验证，可管理当前房间、同步全房间音量并发送全站通知。</p></div><button class="secondary-button" type="button" data-panel="system-admin">进入管理员控制台</button><label class="form-field">全房间音量<div class="inline-control"><input id="root-volume" class="field" type="number" min="0" max="100" step="1" value="${Math.round(audio.volume*100)}" aria-label="全房间音量"><button class="primary-button" type="button" data-command="root-volume">同步音量</button></div></label><label class="form-field">全站通知<div class="inline-control"><input id="root-notice" class="field" placeholder="输入发送给所有房间的通知"><button class="primary-button" type="button" data-command="root-notice">发送通知</button></div></label></section>`);
    const grid=$('.admin-control-grid',scope);
    if(grid)grid.insertAdjacentHTML('beforeend',`<section class="admin-control-card"><div class="admin-control-copy"><strong>进房点歌等待</strong><p>普通成员进入当前房间后需等待指定分钟才能点歌；房管和 Root 不受限制，设为 0 表示关闭。</p></div><label class="form-field">等待分钟数<div class="inline-control"><input id="admin-pick-wait" class="field" type="number" min="0" max="1440" step="1" value="${state.roomState?.pickWaitMinutes ?? 0}"><button class="primary-button" type="button" data-command="admin-pick-wait">保存等待时间</button></div></label></section>`);
    if(grid)grid.insertAdjacentHTML('beforeend',`<section class="admin-control-card"><div class="admin-control-copy"><strong>离房清理点歌</strong><p>开启后，普通成员刷新、断网、WebSocket 断开或切换房间时自动删除其待播歌曲；房管和 Root 不受影响。</p></div><label class="form-field">清理策略<div class="inline-control"><select id="admin-clear-queue-on-leave" class="field"><option value="true" ${state.roomState?.clearQueuedOnLeave!==false?'selected':''}>开启</option><option value="false" ${state.roomState?.clearQueuedOnLeave===false?'selected':''}>关闭</option></select><button class="primary-button" type="button" data-command="admin-clear-queue-on-leave">保存策略</button></div></label></section>`);
    if(grid)grid.insertAdjacentHTML('beforeend',`<section class="admin-control-card"><div class="admin-control-copy"><strong>默认列表播放顺序</strong><p>随机模式一轮内不会重复，全部播完后重新洗牌；顺序模式按歌单导入顺序循环。</p></div><label class="form-field">播放方式<div class="inline-control"><select id="admin-default-playlist-mode" class="field"><option value="true" ${state.roomState?.defaultPlaylistRandom!==false?'selected':''}>随机不重复</option><option value="false" ${state.roomState?.defaultPlaylistRandom===false?'selected':''}>顺序播放</option></select><button class="primary-button" type="button" data-command="admin-default-playlist-mode">保存模式</button></div></label></section>`);
    if(canManage() && grid) grid.insertAdjacentHTML('beforeend',`<section class="admin-control-card"><div class="admin-control-copy"><strong>黑名单查询</strong><p>读取当前房间的用户黑名单与音乐黑名单。</p></div><div class="admin-option-actions"><button class="secondary-button" type="button" data-panel="user-blacklist">查看用户黑名单</button><button class="secondary-button" type="button" data-panel="music-blacklist">查看音乐黑名单</button></div></section>`);
    if(state.isRoot && !state.isAdmin){$$('.system-message',scope).forEach((node)=>node.remove());}
  }
  const pairs=[
    ['[data-command="admin-raw"][data-destination="/music/goodmodel/true"]','destination','/music/goodmodel/false','退出点赞排序'],
    ['[data-command="admin-raw"][data-destination="/music/randommodel/true"]','destination','/music/randommodel/false','退出随机播放'],
    ['[data-command="admin-raw"][data-destination="/music/musiccirclemodel/true"]','destination','/music/musiccirclemodel/false','取消单曲循环'],
    ['[data-command="admin-raw"][data-destination="/music/musiclistmodel/true"]','destination','/music/musiclistmodel/false','取消列表循环'],
    ['[data-command="manage-chat-user"][data-operation="picker"]','operation','picker-off','取消点歌人'],
    ['[data-command="manage-chat-user"][data-operation="voter"]','operation','voter-off','取消切歌人'],
    ['[data-command="admin-user"][data-operation="picker"]','operation','picker-off','取消点歌人'],
    ['[data-command="admin-user"][data-operation="voter"]','operation','voter-off','取消切歌人']
  ];
  pairs.forEach(([selector,key,value,label])=>{
    const source=$(selector,scope);if(!source)return;
    const command=source.dataset.command;
    if($(`[data-command="${command}"][data-${key}="${value}"]`,scope))return;
    const button=source.cloneNode(true);button.dataset[key]=value;
    if(source.classList.contains('feature-card')){
      const strong=button.querySelector('strong');const p=button.querySelector('p');
      if(strong)strong.textContent=label;
      if(p)p.textContent=label.startsWith('退出')||label.startsWith('取消')?`关闭${label.replace(/^[退取]消?/,'')}模式`:label;
    }else{
      button.textContent=label;button.classList.remove('primary-button','danger-button');button.classList.add('secondary-button');
    }
    source.insertAdjacentElement('afterend',button);
  });
  renderRoomState();
}


function renderRoomState() {
  const settings = state.roomState;
  const summary = $('#room-state-summary');
  if (summary) {
    summary.textContent = settings ? describeRoomState(settings) : (state.connected ? '房间状态待同步' : '连接房间后显示切歌率与播放模式');
    if (settings && typeof document.createElement === 'function' && typeof summary.replaceChildren === 'function') {
      const label = document.createElement('span');
      label.className = 'room-state-label';
      label.textContent = '房间状态';
      const rate = document.createElement('strong');
      rate.className = 'room-state-rate';
      rate.textContent = typeof settings.voteRate === 'number' && Number.isFinite(settings.voteRate)
        ? `切歌率 ${Math.round(settings.voteRate * 10000) / 100}%` : '切歌率待同步';
      const modes = document.createElement('span');
      modes.className = 'room-state-modes';
      const activePlaybackMode=['goodModel','randomModel','musicCircleModel','listCircleModel'].some((key)=>settings[key]===true);
      if(!activePlaybackMode){const chip=document.createElement('span');chip.className='room-state-chip';chip.textContent='普通播放';modes.appendChild(chip);}
      for (const [key, name] of [['goodModel', '点赞排序'], ['randomModel', '随机播放'], ['musicCircleModel', '单曲循环'], ['listCircleModel', '列表循环'], ['enableSearch', '访客点歌'], ['enableSwitch', '访客切歌']]) {
        const permission = key === 'enableSearch' || key === 'enableSwitch';
        if(!permission && settings[key]!==true)continue;
        const chip = document.createElement('span');
        const status = settings[key] === true ? (permission ? '允许' : '开启') : settings[key] === false ? (permission ? '禁止' : '关闭') : '待同步';
        chip.className = `room-state-chip ${settings[key] === true ? 'is-on' : settings[key] === false ? 'is-off' : 'is-pending'}`;
        chip.textContent = permission ? (status === '待同步' ? name.slice(2) + '待同步' : status + name.slice(2)) : name;
        chip.title = `${name}：${status}`;
        chip.setAttribute('aria-label', chip.title);
        modes.appendChild(chip);
      }
      const waitChip=document.createElement('span');
      const waitMinutes=Number(settings.pickWaitMinutes);
      waitChip.className=`room-state-chip ${Number.isFinite(waitMinutes)&&waitMinutes>0?'is-on':'is-off'}`;
      waitChip.textContent=Number.isFinite(waitMinutes)&&waitMinutes>0?`进房 ${waitMinutes} 分钟可点歌`:'点歌无需等待';
      waitChip.title=waitChip.textContent;
      modes.appendChild(waitChip);
      const leaveChip=document.createElement('span');
      leaveChip.className=`room-state-chip ${settings.clearQueuedOnLeave===false?'is-off':'is-on'}`;
      leaveChip.textContent=settings.clearQueuedOnLeave===false?'离房保留点歌':'离房清理点歌';
      leaveChip.title=leaveChip.textContent;
      modes.appendChild(leaveChip);
      const defaultModeChip=document.createElement('span');
      defaultModeChip.className='room-state-chip is-on';
      defaultModeChip.textContent=settings.defaultPlaylistRandom===false?'默认列表顺序播放':'默认列表随机不重复';
      defaultModeChip.title=defaultModeChip.textContent;
      modes.appendChild(defaultModeChip);
      summary.replaceChildren(label, rate, modes);
    }
  }
  const voteInput = $('#admin-vote');
  if (voteInput && document.activeElement !== voteInput) voteInput.value = settings?.voteRate ?? '';
  const pickWaitInput=$('#admin-pick-wait');
  if(pickWaitInput&&document.activeElement!==pickWaitInput)pickWaitInput.value=settings?.pickWaitMinutes ?? 0;
  const clearQueueOnLeave=$('#admin-clear-queue-on-leave');
  if(clearQueueOnLeave&&document.activeElement!==clearQueueOnLeave)clearQueueOnLeave.value=settings?.clearQueuedOnLeave===false?'false':'true';
  const defaultPlaylistMode=$('#admin-default-playlist-mode');
  if(defaultPlaylistMode&&document.activeElement!==defaultPlaylistMode)defaultPlaylistMode.value=settings?.defaultPlaylistRandom===false?'false':'true';
  const modeFields = {goodmodel:'goodModel', randommodel:'randomModel', musiccirclemodel:'musicCircleModel', musiclistmodel:'listCircleModel', banchoose:'enableSearch', banswitch:'enableSwitch'};
  $$('[data-command="admin-raw"][data-destination]').forEach((button) => {
    const match = button.dataset.destination.match(/^\/music\/(goodmodel|randommodel|musiccirclemodel|musiclistmodel|banchoose|banswitch)\/(true|false)$/);
    if (!match) return;
    const enabled = settings?.[modeFields[match[1]]];
    const permission = match[1] === 'banchoose' || match[1] === 'banswitch';
    const desiredEnabled = permission ? match[2] === 'false' : match[2] === 'true';
    const selected = typeof enabled === 'boolean' && enabled === desiredEnabled;
    button.setAttribute('aria-pressed', String(selected));
    button.classList.toggle('room-mode-selected', !permission && selected);
    button.classList.toggle('room-permission-allowed', permission && selected && enabled === true);
    button.classList.toggle('room-permission-denied', permission && selected && enabled === false);
    button.title = typeof enabled === 'boolean' ? (permission ? (enabled ? '当前允许' : '当前禁止') : (enabled ? '当前已开启' : '当前已关闭')) : '状态待同步';
  });
}

const shouldLockRoomPanel=(name=currentPanel)=>!state.connected&&(name==='room-password'||(name==='rooms'&&Boolean(state.roomId)));
function syncRoomPanelGate(){
  $('#modal-layer').classList.toggle('room-gate',shouldLockRoomPanel());
}

function openPanel(name) {
  if(name==='system-admin' && !state.isRoot){notify('请先在聊天框验证 Root 身份');return;}
  currentPanel = name;
  const panel = panels[name] || panels.more;
  $('#modal-kicker').textContent = panel.kicker;
  $('#modal-title').textContent = typeof panel.title === 'function' ? panel.title() : panel.title;
  $('#modal-description').textContent = typeof panel.description === 'function' ? panel.description() : panel.description;
  $('#modal-body').innerHTML = panel.body();
  prepareStickyPanel();
  enhancePairedAdminActions($('#modal-body'));
  hydrateIcons($('#modal-body'));
  if (name === 'rooms') state.roomPage = 1;
  if (name === 'search') updateSongSearchHint();
  $('#modal-layer').hidden = false;
  syncRoomPanelGate();
  document.body.style.overflow = 'hidden';
  requestAnimationFrame(() => $('.modal [data-close-modal]')?.focus());
  if(name==='share') requestAnimationFrame(renderWebQr);
  if (name === 'default-list') loadDefaultPlaylist(1);
  if (name === 'system-admin' && systemAdminApi.loggedIn) loadSystemAdminTab(state.systemAdmin.tab);
  if (name === 'members' && state.connected) backend.command('house.members').catch((error) => notify(error.message));
  if((name==='music-blacklist'||name==='user-blacklist') && state.connected){
    if(!canManage()){
      notify('仅房管或管理员可查看黑名单');
      return;
    }
    const kind=name.startsWith('music')?'music':'user';
    state.blacklistRequest=kind;
    refreshCurrentPanelBody();
    const fetchBlacklist = async () => {
      if (backend.lastAuthCommand) {
        try { await backend.command(backend.lastAuthCommand.type, backend.lastAuthCommand.payload); } catch (_) {}
      }
      await backend.command(kind==='music'?'blacklist.music':'blacklist.user');
    };
    fetchBlacklist().catch((error)=>{state.blacklistRequest='';notify(error.message);});
  }
  if (name === 'share' && state.roomId && !state.miniCode && !state.miniCodeError) backend.getMiniCode(state.roomId).then((code)=>{state.miniCode=code;if(currentPanel==='share')refreshCurrentPanelBody();}).catch(()=>{state.miniCodeError=true;if(currentPanel==='share')refreshCurrentPanelBody();notify('小程序码读取失败');});
  if (name === 'rooms' && state.connected) loadRooms().then(()=>{if(currentPanel==='rooms') refreshCurrentPanelBody();});
  if (name === 'admin' && state.isAdmin) refreshRoomAnnouncement();
  if (name === 'hot' && state.connected) { state.hotResults=[];state.hotPage=1;state.hotTotal=0;state.hotLoading=true; backend.command('music.search',{keyword:'*热歌榜',source:state.hotSource,pageIndex:1,pageSize:20}).catch((error)=>{state.hotLoading=false;notify(error.message);}); }
}

function renderWebQr(){
  const target=$('#web-qr');if(!target || !window.Vue || !window.QrcodeVue)return;
  if(qrView){qrView.$destroy();qrView=null;} target.innerHTML='';
  qrView=new window.Vue({render:(h)=>h(window.QrcodeVue,{props:{value:roomShareUrl(),size:210,level:'H',renderAs:'canvas'}})}).$mount(target);
}

function closePanel() {
  if(shouldLockRoomPanel())return false;
  currentPanel = '';
  $('#modal-layer').hidden = true;
  if(qrView){qrView.$destroy();qrView=null;}
  document.body.style.overflow = '';
  return true;
}

let imagePreviewFocus=null;
function openImagePreview(url,sendable=false){
  if(!/^https?:\/\//i.test(url)){notify('图片地址不可预览');return;}
  imagePreviewFocus=document.activeElement;
  const layer=$('#image-preview'),image=$('#image-preview-image'),send=$('#image-preview-send');
  image.src=url;send.dataset.url=url;send.hidden=!sendable;layer.hidden=false;
  $('.image-preview-close')?.focus();
}
function closeImagePreview(){
  const layer=$('#image-preview');if(layer.hidden)return;
  layer.hidden=true;$('#image-preview-image').removeAttribute('src');$('#image-preview-send').dataset.url='';
  if(imagePreviewFocus?.isConnected)imagePreviewFocus.focus();imagePreviewFocus=null;
}

function refreshCurrentPanelBody() {
  const panel=panels[currentPanel]; if(!panel) return;
  $('#modal-title').textContent=typeof panel.title==='function'?panel.title():panel.title;
  $('#modal-body').innerHTML=panel.body();prepareStickyPanel();enhancePairedAdminActions($('#modal-body'));hydrateIcons($('#modal-body'));if(currentPanel==='search')updateSongSearchHint();if(currentPanel==='share')requestAnimationFrame(renderWebQr);
}

function prepareStickyPanel(){
  if(!['rooms','search','hot','history','playlist','playlist-users','default-list','favorites','picture'].includes(currentPanel))return;
  const body=$('#modal-body'),summary=$('.list-summary',body) || (currentPanel==='history' ? $('.search-bar',body) : null);
  if(!summary)return;
  const toolbar=document.createElement('div');toolbar.className='panel-sticky-controls';
  body.insertBefore(toolbar,body.firstChild);
  while(toolbar.nextSibling){const node=toolbar.nextSibling;toolbar.appendChild(node);if(node===summary)break;}
}

async function safeCommand(type, payload = {}, success = '') {
  try {
    await backend.command(type, payload);
    if (success) notify(success);
    return true;
  } catch (error) {
    notify(error.message || '操作失败');
    return false;
  }
}

async function loadSystemAdminTab(tab=state.systemAdmin.tab) {
  if (!state.isRoot || !systemAdminApi.loggedIn) return;
  const session=state.systemAdmin;
  session.tab=tab;
  session.loading=true;
  if(currentPanel==='system-admin')refreshCurrentPanelBody();
  try {
    if(tab==='rooms') {
      await loadRooms();
      if(!state.rooms.some(room=>String(room.id)===String(session.selectedRoomId)))session.selectedRoomId='';
    } else if(tab==='retain') session.retainKeys=await systemAdminApi.listRetainKeys() || [];
    else if(tab==='users') session.users=await systemAdminApi.getUsers() || {};
  } catch(error) {
    notify(error.message || '系统数据读取失败');
  } finally {
    session.loading=false;
    if(currentPanel==='system-admin')refreshCurrentPanelBody();
  }
}

async function submitSystemAdminLogin(form) {
  const button=form.querySelector('[type="submit"]');
  const status=form.querySelector('#system-admin-login-status');
  if(!state.isRoot){status.textContent='请先在聊天框验证 Root 身份';return;}
  const username=form.querySelector('#system-admin-user')?.value.trim() || '';
  const password=form.querySelector('#system-admin-password')?.value || '';
  if(!username || !password){status.textContent='请输入系统用户名和密码，尚未向后端发送请求';return;}
  button.disabled=true;
  button.textContent='正在验证…';
  status.textContent='正在请求系统接口 /house/showRetainKey…';
  try {
    state.systemAdmin.retainKeys=await systemAdminApi.login(username,password) || [];
    form.querySelector('#system-admin-password').value='';
    notify('系统管理员登录成功');
    await loadSystemAdminTab('rooms');
  } catch(error) {
    status.textContent=error.message || '系统管理员登录失败，请查看浏览器 Network 面板';
    notify(status.textContent);
    button.disabled=false;
    button.textContent='验证并登录';
  }
}

async function handleSystemAdminCommand(button) {
  const command=button.dataset.command;
  if(!state.isRoot){notify('请先验证 Root 身份');return;}
  const session=state.systemAdmin;
  if(command==='system-logout') {
    systemAdminApi.logout();session.retainKeys=[];session.users=null;session.pendingRemovalKey='';session.roomLimitResult=null;
    refreshCurrentPanelBody();notify('已退出系统管理员账号');return;
  }
  if(!systemAdminApi.loggedIn){notify('请先登录系统管理员账号');refreshCurrentPanelBody();return;}
  try {
    if(command==='system-tab') {await loadSystemAdminTab(button.dataset.tab);return;}
    if(command==='system-refresh-rooms'){await loadSystemAdminTab('rooms');return;}
    if(command==='system-refresh-keys'){await loadSystemAdminTab('retain');return;}
    if(command==='system-refresh-users'){await loadSystemAdminTab('users');return;}
    if(command==='system-add-key'){
      const key=$('#system-retain-key')?.value.trim() || '';
      if(!key || key.length>160 || /[\\/\s]/.test(key))throw new Error('永存码不能为空、超过 160 字符，或包含空格和斜杠');
      await systemAdminApi.addRetainKey(key);notify('永存码已添加');await loadSystemAdminTab('retain');return;
    }
    if(command==='system-remove-key'){
      session.pendingRemovalKey=button.dataset.key;refreshCurrentPanelBody();return;
    }
    if(command==='system-cancel-remove-key'){
      session.pendingRemovalKey='';refreshCurrentPanelBody();return;
    }
    if(command==='system-confirm-remove-key'){
      const key=button.dataset.key;
      if(!key || key!==session.pendingRemovalKey)return;
      await systemAdminApi.removeRetainKey(key);session.pendingRemovalKey='';notify('永存码已移除');await loadSystemAdminTab('retain');return;
    }
    if(command==='system-save-room'){
      const originalRoom=state.rooms.find(room=>String(room.id)===String(session.selectedRoomId));
      const payload=roomEditPayload({
        id:session.selectedRoomId,name:$('#system-room-name')?.value,desc:$('#system-room-desc')?.value,
        needPwd:$('#system-room-need-pwd')?.value,password:$('#system-room-password')?.value,
        enableStatus:$('#system-room-permanent')?.value,forbiddenModiPwd:$('#system-room-forbid-pwd')?.value,
        canDestroy:$('#system-room-can-destroy')?.value,
        adminPwd:$('#system-room-admin-pwd')?.value
      },{name:originalRoom?.raw?.name ?? originalRoom?.name,desc:originalRoom?.raw?.desc ?? ''});
      if(payload.canDestroy===true && button.dataset.confirmDestroy!==String(payload.id)){
        button.dataset.confirmDestroy=String(payload.id);
        button.textContent='确认立即销毁房间';
        button.classList.remove('primary-button');button.classList.add('danger-button');
        notify('请再次点击确认销毁房间；其他修改不会一并保存');return;
      }
      await systemAdminApi.editRoom(payload);notify(payload.canDestroy===true?'房间已销毁':'房间信息已保存');await loadSystemAdminTab('rooms');return;
    }
    if(command==='system-set-limit'){
      const size=Number($('#system-room-limit')?.value);
      if(!Number.isSafeInteger(size)||size<1||size>10000)throw new Error('房间数量请输入 1 到 10000 的整数');
      const result=await systemAdminApi.setRoomLimit(size);
      session.roomLimitResult=result;
      const status=$('#system-room-limit-status');if(status)status.textContent=systemRoomLimitText(result);
      notify(systemRoomLimitText(result));return;
    }
    if(command==='system-set-cookie'){
      const cookie=$('#system-netease-cookie')?.value.trim() || '';
      if(!cookie)throw new Error('请先粘贴网易云音乐 Cookie');
      await systemAdminApi.setNeteaseCookie(cookie);
      const input=$('#system-netease-cookie');if(input)input.value='';
      notify('网易云音乐 Cookie 已更新');return;
    }
  } catch(error) {
    notify(error.message || '系统操作失败');
    if(!systemAdminApi.loggedIn)refreshCurrentPanelBody();
  }
}

async function requestMusicPick(payload, success = '') {
  const pending={id:payload.id,name:payload.name,roomId:state.roomId,at:Date.now()};
  state.pendingPicks=state.pendingPicks.filter((pick)=>Date.now()-pick.at<15000);
  state.pendingPicks.push(pending);
  const ok=await safeCommand('music.pick',payload,success);
  if(!ok)state.pendingPicks=state.pendingPicks.filter((pick)=>pick!==pending);
  return ok;
}

async function enterSelectedRoom(room,password=''){
  const previous={id:state.roomId,password:state.roomPassword,name:state.room,description:state.roomDescription};
  let claimedHere=false;
  try {
    state.duplicateConnection=false;
    if(state.connected && String(room.id)===String(state.roomId)){closePanel();notify('已经在当前房间');return true;}
    roomAnnouncementCache.delete(String(room.id));
    state.isAdmin=state.isRoot;state.goodMode=false;state.miniCode='';state.miniCodeError=false;state.online=0;syncAdminUI();renderQueue();
    if(state.connected){state.pendingRoom={...room,password};state.awaitingRoomEntry=true;state.pendingQueueSnapshot=null;await backend.command('house.enter',{id:room.id,password});notify(`正在切换到 ${room.name}…`);return true;}
    if(!state.connected){claimedHere=claimRoomLease(room.id);if(!claimedHere){showDuplicateWindowHome();return false;}}
    const roomDetails=await backend.verifyRoom(room.id,password);
    state.roomId=room.id;state.roomPassword=password;state.room=room.name;state.roomDescription=room.desc || '';setRoomAnnouncement(announcementFromRoom(roomDetails) ?? announcementFromRoom(room.raw || room));state.mySessionId='';state.ownPickedIds.clear();state.likedQueueIds.clear();state.pendingPicks=[];state.pendingVote=null;state.pendingLikes=[];updateDocumentTitle();
    await backend.connect({id:room.id,password,connectType:'enter',name:room.name});
    state.pendingRoom=null;renderRooms();syncEntryExperience();closePanel();notify(`已进入 ${room.name}`);return true;
  }catch(error){state.awaitingRoomEntry=false;state.pendingQueueSnapshot=null;if(claimedHere&&!state.connected)releaseRoomLease();if(error.code==='DUPLICATE_CLIENT'||state.duplicateConnection)return false;state.roomId=previous.id;state.roomPassword=previous.password;state.room=previous.name;state.roomDescription=previous.description;renderRooms();notify(error.message || '进入房间失败');return false;}
}

function setFocusMode(enabled){
  state.focusMode=Boolean(enabled);$('#focus-player').hidden=!state.focusMode;document.body.classList.toggle('focus-mode-open',state.focusMode);
  $$('[data-action="toggle-focus"]').forEach((button)=>button.setAttribute('aria-pressed',String(state.focusMode)));
  syncSystemBarTheme();
  if(state.focusMode)syncFocusLyrics(state.currentLyricIndex);
}

function setMobileView(view) {
  if(view!=='chat' && state.chatExpanded)setChatExpanded(false);
  state.mobileView = view;
  $('#app').dataset.mobileView = view;
  $$('.mobile-nav [data-mobile-view]').forEach((button) => button.classList.toggle('active', button.dataset.mobileView === view));
  $$('[data-view]').forEach((button) => button.classList.toggle('active', button.dataset.view === view));
  if(view==='chat'&&chatFollowLatest)requestAnimationFrame(()=>requestAnimationFrame(scrollChatToLatest));
}

function setChatExpanded(enabled){
  state.chatExpanded=Boolean(enabled);$('#app').classList.toggle('chat-expanded',state.chatExpanded);
  const button=$('[data-action="toggle-chat-expand"]');
  if(button){button.innerHTML=icon(state.chatExpanded?'fullscreen_exit':'fullscreen');button.setAttribute('aria-label',state.chatExpanded?'还原聊天':'放大聊天');button.title=state.chatExpanded?'还原聊天':'放大聊天';}
}

let warpInProgress=false;
const waitFor=(milliseconds)=>new Promise((resolve)=>window.setTimeout(resolve,milliseconds));
const waitForPaint=()=>new Promise((resolve)=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
function waitForWarpAnimation(element,timeout){
  return new Promise((resolve)=>{
    let settled=false;const finish=()=>{if(settled)return;settled=true;element?.removeEventListener('animationend',finish);clearTimeout(timer);resolve();};
    const timer=setTimeout(finish,timeout);element?.addEventListener('animationend',finish,{once:true});
  });
}
async function startRandomWarp(trigger){
  if(warpInProgress)return;
  if(!state.rooms.length){try{await loadRooms();}catch(_){}}
  let candidates=state.rooms.filter((room)=>!room.locked && String(room.id)!==String(state.roomId || ''));
  if(!candidates.length && state.rooms.length){
    const otherRooms=state.rooms.filter((room)=>String(room.id)!==String(state.roomId || ''));
    if(!otherRooms.length){notify('目前全站只有您所在的这间房间');return;}
    candidates=state.rooms.filter((room)=>!room.locked);
  }
  if(!candidates.length){notify('暂时没有可以穿梭的免密房间');return;}
  const room=candidates[Math.floor(Math.random()*candidates.length)],overlay=$('#warp-transition'),destination=$('#warp-destination');
  if(state.connected && String(room.id)===String(state.roomId)){notify('已处于当前唯一可用房间');return;}
  warpInProgress=true;if(trigger)trigger.disabled=true;if(destination)destination.textContent=`目的地：${room.name}`;
  overlay.classList.remove('active','leaving');overlay.hidden=false;overlay.setAttribute('aria-busy','true');document.body.classList.add('time-warp-active');void overlay.offsetWidth;await waitForPaint();overlay.classList.add('active');await waitForPaint();
  try{
    await waitForWarpAnimation($('.warp-spacecraft',overlay),matchMedia('(prefers-reduced-motion: reduce)').matches?260:1800);
    const entered=await enterSelectedRoom(room,'');
    if(!entered)notify(`未能抵达 ${room.name}，请重新穿梭`);
    await waitFor(entered?420:120);
  }finally{
    overlay.classList.remove('active');overlay.classList.add('leaving');overlay.removeAttribute('aria-busy');
    await waitFor(280);overlay.hidden=true;overlay.classList.remove('leaving');document.body.classList.remove('time-warp-active');warpInProgress=false;if(trigger)trigger.disabled=false;
  }
}

async function runAction(action, trigger) {
  switch (action) {
    case 'time-warp':
      await startRandomWarp(trigger);
      break;
    case 'toggle-play': {
      if (!audio.src) { notify('当前房间还没有正在播放的歌曲'); break; }
      if (audio.paused) await audio.play(); else audio.pause();
      state.playing = !audio.paused;
      syncPlayButtons(!audio.paused);
      notify(state.playing ? '继续播放' : '已暂停');
      break;
    }
    case 'favorite': {
      if (!state.currentMusic) { notify('当前没有可收藏的歌曲'); break; }
      const existing = state.favorites.findIndex((item) => String(item.id) === String(state.currentMusic.id));
      if (existing >= 0) state.favorites.splice(existing,1); else state.favorites.unshift({ ...state.currentMusic, lyric: '' });
      state.favorite = existing < 0;
      localStorage.setItem('collectMusic', JSON.stringify(Object.fromEntries(state.favorites.map((item) => [item.id,item]))));
      state.queue.forEach((song)=>{song.favorite=isFavorite(song.id);}); renderQueue();
      trigger.classList.toggle('active', state.favorite);
      notify(state.favorite ? '已收藏这首歌' : '已取消收藏');
      break;
    }
    case 'like':
      if (state.currentMusic) {
        const pending={id:String(state.currentMusic.id || ''),name:state.currentMusic.name || '',roomId:state.roomId,at:Date.now()};state.pendingLikes.push(pending);
        if (await safeCommand('music.good',{ id: state.currentMusic.id })) {
        $('#like-count').textContent = Number($('#like-count').textContent) + 1;
        notify('点赞请求已提交');
        } else state.pendingLikes=state.pendingLikes.filter((item)=>item!==pending);
      }
      break;
    case 'black-current-song':
      if(!state.isAdmin){notify('请先登录房管');break;}
      if(!state.currentMusic?.id){notify('当前没有可拉黑的歌曲');break;}
      await safeCommand('admin.raw',{destination:'/music/black',body:JSON.stringify({id:state.currentMusic.id,sendTime:Date.now()})},`《${state.currentMusic.name || '当前歌曲'}》已加入黑名单`);
      break;
    case 'skip-vote':
      {const pending={at:Date.now(),roomId:state.roomId};state.pendingVote=pending;
      if(!(await safeCommand('music.skipVote',{},'切歌投票已提交')) && state.pendingVote===pending)state.pendingVote=null;}
      break;
    case 'clear-chat':
      state.messages = [{ type: 'system', text: '聊天记录已在当前界面清空' }];
      renderMessages();
      notify('聊天已清空');
      break;
    case 'toggle-chat-panel':
      state.chatHidden=!state.chatHidden; $('#app').classList.toggle('chat-hidden',state.chatHidden); notify(state.chatHidden?'聊天区域已隐藏':'聊天区域已显示');
      break;
    case 'toggle-sidebar':
      setSidebarCollapsed(!state.sidebarCollapsed);
      break;
    case 'toggle-chat-expand':
      setChatExpanded(!state.chatExpanded);
      break;
    case 'jump-chat-latest':
      chatFollowLatest=true;scrollChatToLatest();updateChatJumpButton();
      break;
    case 'toggle-mute': {
      const enabling=audio.muted;setAudioMuted(!audio.muted);
      if(enabling&&audio.paused&&audio.src){try{await audio.play();}catch(_){} }
      notify(audio.muted?'声音已关闭':'声音已开启');
      break;
    }
    case 'switch-source':
      state.source = state.source === '网易' ? 'QQ' : '网易';
      $('#chat-source').textContent = state.source;
      notify(`已切换到 ${state.source} 音乐源`);
      break;
    case 'play-all':
      for (const song of state.favorites) await requestMusicPick({...song,source:song.source || 'wy'});
      notify('收藏歌曲已发送到播放队列');
      break;
    case 'show-lyrics': {
      const tab=$('[data-library="lyrics"]'); if(tab){tab.click();setMobileView('queue');}
      break;
    }
    case 'toggle-focus': {
      const next=!state.focusMode;if(next&&!$('#modal-layer').hidden)closePanel();setFocusMode(next);notify(next?'已进入简洁模式':'已退出简洁模式');
      break;
    }
  }
}

const truncatedTextSelector='.room-copy strong,.room-copy small,.result-row strong,.result-row small,.song-name,.song-artist,.song-picker,.message-meta,#current-room-name';
function exposeTruncatedText(event){
  const target=event.target.closest?.(truncatedTextSelector);
  if(target && target.scrollWidth>target.clientWidth && !target.title)target.title=target.textContent.trim();
}
document.addEventListener('mouseover',exposeTruncatedText);
document.addEventListener('focusin',exposeTruncatedText);
const changeGalaxyPage=(delta)=>{earthMotion.yaw+=delta*.72;state.galaxyPage+=delta;renderGalaxy();};
const globeSystem=$('#galaxy-system');let globeDrag=null;
globeSystem?.addEventListener('pointerdown',(event)=>{if(event.target.closest('button'))return;globeDrag={start:event.clientX,last:event.clientX,lastY:event.clientY};earthMotion.dragging=true;globeSystem.setPointerCapture?.(event.pointerId);globeSystem.classList.add('is-dragging');});
globeSystem?.addEventListener('pointermove',(event)=>{if(!globeDrag)return;earthView?.rotate(event.clientX-globeDrag.last,event.clientY-globeDrag.lastY);globeDrag.last=event.clientX;globeDrag.lastY=event.clientY;});
const finishGlobeDrag=(event)=>{if(!globeDrag)return;const distance=globeDrag.last-globeDrag.start;globeDrag=null;earthMotion.dragging=false;globeSystem.classList.remove('is-dragging');if(Math.abs(distance)>68)changeGalaxyPage(distance<0?1:-1);globeSystem.releasePointerCapture?.(event.pointerId);};
globeSystem?.addEventListener('pointerup',finishGlobeDrag);globeSystem?.addEventListener('pointercancel',finishGlobeDrag);

document.addEventListener('click', async (event) => {
  if(event.target.closest('[data-close-image-preview]')){closeImagePreview();return;}
  const imagePreviewTrigger=event.target.closest('[data-preview-image]');
  if(imagePreviewTrigger){openImagePreview(imagePreviewTrigger.dataset.previewImage,imagePreviewTrigger.dataset.previewSend==='true');return;}
  const closeCenterNotice=event.target.closest('[data-close-center-notice]');
  if(closeCenterNotice){dismissCenterNotice(closeCenterNotice.dataset.closeCenterNotice);return;}
  const centerNoticeAction=event.target.closest('[data-center-notice-action]');
  if(centerNoticeAction){
    if(centerNoticeAction.dataset.centerNoticeAction==='play'&&audio.src){try{await audio.play();dismissCenterNotice('playback');}catch(_){notify('播放失败，请检查浏览器媒体权限');}}
    return;
  }
  const galaxyPage=event.target.closest('[data-galaxy-page]');if(galaxyPage){changeGalaxyPage(galaxyPage.dataset.galaxyPage==='next'?1:-1);return;}
  const galaxyFilter=event.target.closest('[data-galaxy-filter]');if(galaxyFilter){state.galaxyFilter=galaxyFilter.dataset.galaxyFilter;state.galaxyPage=0;renderGalaxy();return;}
  const homeMode=event.target.closest('[data-home-mode]');if(homeMode){state.homeMode=homeMode.dataset.homeMode==='galaxy'?'galaxy':'simple';localStorage.setItem('JUSIC_HOME_MODE',state.homeMode);state.galaxyPage=0;renderGalaxy();return;}
  const songIdTrigger=event.target.closest('[data-copy-song-id]');
  if(songIdTrigger){const id=songIdTrigger.dataset.copySongId;if(id){try{await copyToClipboard(id);notify(`歌曲 ID ${id} 已复制`);}catch(_){notify(`复制失败，歌曲 ID：${id}`);}}return;}
  const playlistIdTrigger=event.target.closest('[data-copy-playlist-id]');
  if(playlistIdTrigger){const id=playlistIdTrigger.dataset.copyPlaylistId;try{await copyToClipboard(id);notify(`歌单 ID ${id} 已复制`);}catch(_){notify(`复制失败，歌单 ID：${id}`);}return;}
  const copyTextTrigger=event.target.closest('[data-copy-text]');
  if(copyTextTrigger){try{await copyToClipboard(copyTextTrigger.dataset.copyText);notify('内容已复制');}catch(_){notify('复制失败');}return;}
  const chatSelf=event.target.closest('[data-chat-self]');
  if(chatSelf){if(chatSelf.dataset.chatId)state.mySessionId=chatSelf.dataset.chatId;openPanel('profile');return;}
  const chatUser=event.target.closest('[data-chat-user]');
  if(chatUser){ state.selectedUser={id:chatUser.dataset.chatUser,name:chatUser.dataset.chatName || '匿名'}; openPanel('user-actions'); return; }
  const panelTrigger = event.target.closest('[data-panel]');
  if (panelTrigger) { openPanel(panelTrigger.dataset.panel); return; }
  if (event.target.closest('[data-close-modal]')) { closePanel(); return; }

  const viewTrigger = event.target.closest('[data-view]');
  if (viewTrigger) { setMobileView(viewTrigger.dataset.view); return; }
  const mobileView = event.target.closest('.mobile-nav [data-mobile-view]');
  if (mobileView) { setMobileView(mobileView.dataset.mobileView); return; }

  const actionTrigger = event.target.closest('[data-action]');
  if (actionTrigger) { await runAction(actionTrigger.dataset.action, actionTrigger); return; }
  const toastTrigger = event.target.closest('[data-toast]');
  if (toastTrigger) { notify(toastTrigger.dataset.toast); return; }

  const roomTrigger = event.target.closest('[data-enter-room]');
  if (roomTrigger) {
    const room = state.rooms.find((item) => String(item.id) === String(roomTrigger.dataset.enterRoom));
    if (!room) return;
    if(state.connected && String(room.id)===String(state.roomId)){notify('已经在当前房间');return;}
    if (room.locked) {
      const sharedPassword=String(state.requestedRoomId)===String(room.id)?state.requestedRoomPassword:'';
      if(sharedPassword){await enterSelectedRoom(room,sharedPassword);}else{state.pendingRoom=room;openPanel('room-password');}
      return;
    }
    await enterSelectedRoom(room,'');
    return;
  }

  const libraryTrigger = event.target.closest('[data-library]');
  if (libraryTrigger) {
    $$('[data-library]').forEach((button) => { button.classList.toggle('active', button === libraryTrigger); button.setAttribute('aria-selected', String(button === libraryTrigger)); });
    $('#queue-view').hidden = libraryTrigger.dataset.library !== 'queue';
    $('#lyrics-view').hidden = libraryTrigger.dataset.library !== 'lyrics';
    return;
  }

  const queueAction = event.target.closest('[data-queue-action]');
  if (queueAction) {
    const row = queueAction.closest('[data-song-id]');
    const index = Number(row.dataset.queueIndex);
    if (!Number.isInteger(index) || index < 1 || !state.queue[index]) return;
    if (queueAction.dataset.queueAction === 'remove') { const song=state.queue[index];const removed=await safeCommand('music.delete',{id:String(song.name || song.id)},`已发送移除《${song.name}》请求`);if(removed)state.ownPickedIds.delete(String(song.id)); }
    if (queueAction.dataset.queueAction === 'up') await safeCommand('music.top',{id:state.queue[index].id},`已发送置顶《${state.queue[index].name}》请求`);
    if (queueAction.dataset.queueAction === 'like') {const song=state.queue[index],id=String(song.id);if(!state.likedQueueIds.has(id)){const pending={id,name:song.name || '',roomId:state.roomId,at:Date.now()};state.pendingLikes.push(pending);if(await safeCommand('music.good',{id},`已点赞《${song.name}》`))state.likedQueueIds.add(id);else state.pendingLikes=state.pendingLikes.filter((item)=>item!==pending);}}
    if (queueAction.dataset.queueAction === 'black' && state.isAdmin) await safeCommand('admin.raw',{destination:'/music/black',body:JSON.stringify({id:state.queue[index].id,sendTime:Date.now()})},'歌曲已加入黑名单');
    if (queueAction.dataset.queueAction === 'unblack' && state.isAdmin) await safeCommand('admin.raw',{destination:'/music/unblack',body:JSON.stringify({id:state.queue[index].id,sendTime:Date.now()})},'歌曲已移出黑名单');
    if (queueAction.dataset.queueAction === 'favorite') {
      const song = state.queue[index]; const existing = state.favorites.findIndex((item) => String(item.id) === String(song.id));
      if (existing >= 0) state.favorites.splice(existing,1); else state.favorites.unshift({...song});
      song.favorite = existing < 0;
      if(state.currentMusic && String(state.currentMusic.id)===String(song.id)){state.favorite=song.favorite;$('.favorite-toggle')?.classList.toggle('active',state.favorite);}
      localStorage.setItem('collectMusic',JSON.stringify(Object.fromEntries(state.favorites.map((item)=>[item.id,item]))));
      notify(song.favorite ? '已收藏' : '已取消收藏');
    }
    renderQueue();
    return;
  }

  const filterTrigger = event.target.closest('[data-filter]');
  if (filterTrigger) {
    state.messageFilter = filterTrigger.dataset.filter;
    $$('.chat-filters button').forEach((button) => button.classList.toggle('active', button === filterTrigger));
    renderMessages();
    return;
  }

  const playlistPlatform=event.target.closest('[data-playlist-platform]');
  const playlistCategory=event.target.closest('[data-playlist-kind]');
  if(playlistPlatform || playlistCategory){
    const platform=playlistPlatform?.dataset.playlistPlatform || (state.playlistSource.startsWith('qq')?'qq':'wy');
    const currentCategory=state.playlistSource.endsWith('_userdj')?'radio':state.playlistSource.endsWith('_user')?'user':'playlist';
    const category=playlistCategory?.dataset.playlistKind || (platform==='qq'&&currentCategory==='radio'?'playlist':currentCategory);
    const nextSource=playlistSourceFor(platform,category);
    if(nextSource!==state.playlistSource){state.playlistKeyword=$('#playlist-search')?.value.trim() || '';if(playlistPlatform)state.playlistKeyword='';state.playlistSource=nextSource;state.playlistKind=category==='radio'?'radio':'playlist';state.playlistView='search';state.playlistPage=1;state.playlistResults=[];state.playlistTotal=0;state.playlistLoading=false;refreshCurrentPanelBody();$('#playlist-search')?.focus();}
    return;
  }
  const localTab = event.target.closest('[data-local-tab]');
  if (localTab) {
    $$('.panel-tabs button').forEach((button) => button.classList.toggle('active', button === localTab));
    if (localTab.dataset.source) { state.source = localTab.dataset.source; $('#chat-source').textContent = state.source; updateSongSearchHint(); }
    if (localTab.dataset.hotSource) { state.hotSource=localTab.dataset.hotSource; state.hotResults=[];state.hotPage=1;state.hotTotal=0;state.hotLoading=true; refreshCurrentPanelBody(); await safeCommand('music.search',{keyword:'*热歌榜',source:state.hotSource,pageIndex:1,pageSize:20}); }
    if (localTab.dataset.historyMode) { state.historyMode=localTab.dataset.historyMode; refreshCurrentPanelBody(); }
    return;
  }
  const themeTrigger=event.target.closest('button[data-player-theme]');
  if(themeTrigger){setPlayerTheme(themeTrigger.dataset.playerTheme);return;}
  const emoji = event.target.closest('[data-emoji]');
  if (emoji) { $('#message-input').value += emoji.dataset.emoji; closePanel(); setMobileView('chat'); notify('表情已插入聊天框'); return; }
  const picture = event.target.closest('[data-picture]');
  if (picture) { $('#message-input').value = `[图片：${picture.dataset.picture}]`; closePanel(); setMobileView('chat'); notify('图片已放入发送框'); return; }

  const command = event.target.closest('[data-command]');
  if (command) {
    if (command.dataset.command === 'select-system-ip') {
      state.systemAdmin.selectedIp = command.dataset.ip || '';
      refreshCurrentPanelBody();
      return;
    }
    if(command.dataset.command.startsWith('system-')){await handleSystemAdminCommand(command);return;}
    const name = command.dataset.name || '';
    if (command.dataset.command === 'default-pick' || command.dataset.command === 'default-favorite') {
      const song = state.defaultPlaylist.songs[Number(command.dataset.index)];
      if (!song || state.defaultPlaylist.houseId !== String(state.roomId)) return;
      if (command.dataset.command === 'default-pick') {
        await requestMusicPick({id:song.id,source:song.source || 'wy',name:song.name || String(song.id),quality:'320k'}, '点歌请求已发送');
      } else {
        const key = defaultSongKey(song);
        const index = state.favorites.findIndex(item => defaultSongKey(item) === key);
        if (index >= 0) state.favorites.splice(index,1);
        else state.favorites.unshift({...song,name:song.name || song.id});
        localStorage.setItem('collectMusic', JSON.stringify(Object.fromEntries(state.favorites.map(item => [defaultSongKey(item),item]))));
        state.queue.forEach(item => { item.favorite = isFavorite(item.id); });
        renderQueue(); refreshCurrentPanelBody();
        notify(index >= 0 ? '已取消收藏' : '已收藏');
      }
    } else if (command.dataset.command === 'pick-song') {
      const ok = await requestMusicPick({ name, id:command.dataset.id, source:command.dataset.source || sourceCode(state.source), quality:command.dataset.quality || '320k' });
      if (ok) { state.ownPickedIds.add(String(command.dataset.id));const own={name,id:command.dataset.id,source:command.dataset.source || sourceCode(state.source),artist:command.dataset.artist || '',nickName:state.nickname || '',mine:true,pickTime:Date.now()};state.history=[own,...state.history.filter((item)=>String(item.id)!==String(own.id))].slice(0,400);localStorage.setItem('pickHistory',JSON.stringify(state.history));command.textContent = '已发送'; command.disabled = true;syncHistoryPanel(); notify(`《${name}》点歌请求已发送`); }
    } else if (command.dataset.command === 'pick-by-id') {
      const id=$('#song-search')?.value.trim();if(!id){notify('请输入歌曲 ID');return;}const ok=await requestMusicPick({name:id,source:sourceCode(state.source)},'歌曲 ID 点歌请求已发送');if(ok){state.ownPickedIds.add(String(id));command.textContent='已发送';window.setTimeout(()=>{command.textContent='ID 点歌';},1200);}
    } else if (command.dataset.command === 'search-song') {
      const keyword = $('#song-search')?.value.trim();
      if (!keyword && sourceCode(state.source) !== 'lz') { notify('请输入搜索关键词'); return; }
      state.searchKeyword=keyword; state.searchPage=1; state.searchResults=[]; state.searchLoading=true;
      command.disabled = true; command.textContent = '搜索中…';
      const ok = await safeCommand('music.search',{keyword,source:sourceCode(state.source),pageIndex:1,pageSize:20});
      if (!ok) { command.disabled = false; command.innerHTML = `${icon('search')}搜索`; }
      else setTimeout(()=>{if(state.searchLoading&&state.searchPage===1){state.searchLoading=false;const button=$('[data-command="search-song"]');if(button){button.disabled=false;button.innerHTML=`${icon('search')}搜索`;}notify('搜索响应超时，请重试或切换音源');}},8000);
    } else if (command.dataset.command === 'clear-queue') {
      openPanel('confirm-clear-queue');
    } else if (command.dataset.command === 'confirm-clear-queue') {
      if(await safeCommand('music.clear',{},'清空请求已发送'))closePanel();
    } else if (command.dataset.command === 'refresh-rooms') {
      await loadRooms(); openPanel('rooms');
    } else if (command.dataset.command === 'clear-favorites') {
      state.favorites = []; localStorage.removeItem('collectMusic'); openPanel('favorites'); notify('收藏已清空');
    } else if(command.dataset.command==='remove-favorite'){
      state.favorites=state.favorites.filter((item)=>String(item.id)!==String(command.dataset.id));localStorage.setItem('collectMusic',JSON.stringify(Object.fromEntries(state.favorites.map((item)=>[item.id,item]))));state.queue.forEach((song)=>song.favorite=isFavorite(song.id));if(state.currentMusic&&String(state.currentMusic.id)===String(command.dataset.id)){state.favorite=false;$('.favorite-toggle')?.classList.remove('active');}renderQueue();refreshCurrentPanelBody();notify('已取消收藏');
    } else if (command.dataset.command === 'clear-history') {
      state.history = []; localStorage.removeItem('pickHistory'); openPanel('history'); notify('历史已清空');
    } else if (command.dataset.command === 'export-favorites') {
      const data = JSON.stringify(Object.fromEntries(state.favorites.map((item)=>[item.id,item])),null,2);
      const link = document.createElement('a'); link.href = `data:application/json;charset=utf-8,${encodeURIComponent(data)}`; link.download = `jusic-favorites-${Date.now()}.json`; link.click();
    } else if (command.dataset.command === 'import-favorites') {
      $('#favorite-import')?.click();
    } else if (command.dataset.command === 'load-members') {
      await safeCommand('house.members',{},'正在读取在线成员');
    } else if (command.dataset.command === 'prepare-private') {
      closePanel(); setMobileView('chat'); $('#message-input').value = `@${command.dataset.user} `; $('#message-input').focus();
    } else if (command.dataset.command === 'black-member') {
      if (!state.isAdmin) { notify('请先登录管理员'); return; }
      await safeCommand('admin.raw',{destination:'/chat/black',body:JSON.stringify({sessionId:command.dataset.user,sendTime:Date.now()})},'拉黑请求已发送');
    } else if (command.dataset.command === 'open-user-actions') {
      state.selectedUser={id:command.dataset.user,name:command.dataset.name}; openPanel('user-actions');
    } else if (command.dataset.command === 'manage-chat-user') {
      if(!state.isAdmin || !state.selectedUser) return;
      const id=state.selectedUser.id, op=command.dataset.operation;
      const destination={black:'/chat/black',unblack:'/chat/unblack',picker:`/auth/setPicker/${encodeURIComponent(id)}`,'picker-off':`/auth/setNoPicker/${encodeURIComponent(id)}`,voter:`/auth/setVoter/${encodeURIComponent(id)}`,'voter-off':`/auth/setNoVoter/${encodeURIComponent(id)}`}[op];
      if(!destination)return;
      const body=(op==='black'||op==='unblack')?JSON.stringify({sessionId:id,sendTime:Date.now()}):'';
      await safeCommand('admin.raw',{destination,body},'成员权限请求已发送');
    } else if (command.dataset.command === 'copy-room-link') {
      try { await copyToClipboard(roomShareUrl()); notify('房间链接已复制'); } catch (_) { notify(`复制失败，请长按复制：${roomShareUrl()}`); }
    } else if (command.dataset.command === 'open-room-link') {
      location.href=roomShareUrl();
    } else if (command.dataset.command === 'private-message') {
      const form = command.closest('form'); const user = new FormData(form).get('userId'); const text = new FormData(form).get('privateMessage');
      if (user && text) await safeCommand('chat.send',{text:`@${user} ${text}`},'私聊已发送');
    } else if (command.dataset.command === 'contact-admin') {
      const text = new FormData(command.closest('form')).get('adminMessage'); if (text) await safeCommand('chat.contactAdmin',{text},'留言已发送');
    } else if (command.dataset.command === 'cancel-countdown') {
      clearTimeout(window.jusicCloseTimer);state.countdownEndsAt=0;const status=$('#countdown-status');if(status)status.textContent=countdownStatus();notify('已取消倒计时退出');
    } else if(command.dataset.command==='set-countdown'){
      if(setCountdownExit(Number($('#countdown-minutes')?.value))){const status=$('#countdown-status');if(status)status.textContent=countdownStatus();}
    } else if (command.dataset.command === 'search-playlist') {
      const keyword = $('#playlist-search').value.trim(); const source = state.playlistSource;
      state.playlistPage=1; state.playlistResults=[]; state.playlistLoading=true; state.playlistKind=source.endsWith('_userdj')?'radio':'playlist'; state.playlistKeyword=keyword;
      refreshCurrentPanelBody();await safeCommand('playlist.search',{keyword,source,pageIndex:1,pageSize:20},'搜索请求已发送');
      setTimeout(()=>{if(state.playlistLoading&&state.playlistPage===1){state.playlistLoading=false;notify('歌单搜索响应超时，请重试');}},8000);
    } else if (command.dataset.command === 'search-netease-user') {
      const keyword=$('#playlist-user-search')?.value.trim() || '';
      state.userSearchKeyword=keyword;state.userPage=1;state.userResults=[];state.userTotal=0;state.userLoading=true;
      refreshCurrentPanelBody();await safeCommand('user.search',{keyword,source:'wy',pageIndex:1,pageSize:20},'正在搜索网易用户');
    } else if (command.dataset.command === 'search-picture') {
      state.pictureKeyword=$('#picture-search').value.trim();state.picturePage=1;state.pictureResults=[];state.pictureTotal=0;state.pictureLoading=true;await safeCommand('picture.search',{keyword:state.pictureKeyword,pageIndex:1,pageSize:12},'图片搜索请求已发送');
    } else if (command.dataset.command === 'send-picture') {
      const pending={text:`picture:${command.dataset.url}`,time:Date.now()};state.lastSentMessage=pending;
      if (await safeCommand('chat.picture',{url:command.dataset.url},'图片已发送')){closeImagePreview();closePanel();}
      else if(state.lastSentMessage===pending)state.lastSentMessage=null;
    } else if (command.dataset.command === 'append-playlist') {
      await safeCommand('playlist.append',{id:command.dataset.id,source:command.dataset.source},'追加歌单请求已发送');
    } else if(command.dataset.command==='replace-playlist'){
      if(!state.isAdmin){notify('请先登录管理员');return;}
      const cleared=await safeCommand('admin.raw',{destination:'/music/clearDefaultPlayList'},'默认列表已清空');if(cleared)await safeCommand('playlist.append',{id:command.dataset.id,source:command.dataset.source},'已覆盖默认歌单');
    } else if(command.dataset.command==='view-user-playlists'){
      state.playlistView='user-playlists';state.playlistKind='playlist';state.playlistKeyword=command.dataset.user;state.playlistSource=`${command.dataset.source || 'wy'}_user`;state.playlistPage=1;state.playlistResults=[];state.playlistTotal=0;state.playlistLoading=true;
      openPanel('playlist');
      await safeCommand('playlist.search',{keyword:command.dataset.user,source:state.playlistSource,pageIndex:1,pageSize:20},'正在读取用户歌单');
    } else if(command.dataset.command==='view-user-radio'){
      state.playlistView='user-radio';state.playlistKind='radio';state.playlistKeyword=command.dataset.user;state.playlistSource=`${command.dataset.source || 'wy'}_userdj`;state.playlistPage=1;state.playlistResults=[];state.playlistTotal=0;state.playlistLoading=true;
      openPanel('playlist');
      await safeCommand('playlist.search',{keyword:command.dataset.user,source:state.playlistSource,pageIndex:1,pageSize:20},'正在读取用户电台');
    } else if(command.dataset.command==='view-playlist-songs'){
      state.source=sourceLabel(command.dataset.source);state.searchKeyword=`*${command.dataset.id}`;state.searchPage=1;state.searchResults=[];state.searchTotal=0;state.searchLoading=true;openPanel('search');
      await safeCommand('music.search',{keyword:state.searchKeyword,source:command.dataset.source,pageIndex:1,pageSize:20},'正在读取歌单歌曲');
    } else if(command.dataset.command==='playlist-back'){
      openPanel('playlist-users');
    } else if(command.dataset.command==='playlist-home'){
      state.playlistView='search';state.playlistKind='playlist';state.playlistSource='wy';state.playlistKeyword='';state.playlistResults=[];state.playlistTotal=0;openPanel('playlist');
    } else if (command.dataset.command === 'admin-login') {
      await safeCommand('auth.admin',{password:$('#admin-password').value},'管理员登录请求已发送');
    } else if(command.dataset.command==='root-notice'){
      const text=$('#root-notice')?.value.trim();if(text)await safeCommand('chat.globalNotice',{text},'全站通知已发送');
    } else if(command.dataset.command==='root-volume'){
      if(!state.isRoot){notify('仅超级管理员可调整全房间音量');return;}const value=Number($('#root-volume')?.value);if(!Number.isFinite(value)||value<0||value>100){notify('音量必须是 0 到 100 的数字');return;}await safeCommand('player.volume',{value:Math.round(value)},`已请求将全房间音量调整为 ${Math.round(value)}`);
    } else if(command.dataset.command==='load-blacklist'){
      if(!canManage()){
        notify('仅房管或管理员可查看黑名单');
        return;
      }
      const kind=command.dataset.kind;
      state.blacklistRequest=kind;
      refreshCurrentPanelBody();
      if (backend.lastAuthCommand) {
        try { await backend.command(backend.lastAuthCommand.type, backend.lastAuthCommand.payload); } catch (_) {}
      }
      await safeCommand(kind==='music'?'blacklist.music':'blacklist.user',{},'黑名单读取请求已发送');
    } else if(command.dataset.command==='delete-blacklist'){
      if(!canManage()){notify('请先登录房管');return;}const kind=command.dataset.kind,value=command.dataset.value;const destination=kind==='music'?'/music/unblack':'/chat/unblack';const body=kind==='music'?{id:value,sendTime:Date.now()}:{sessionId:value,sendTime:Date.now()};if(await safeCommand('admin.raw',{destination,body:JSON.stringify(body)},'已从黑名单删除')){if(kind==='music')state.musicBlacklist=state.musicBlacklist.filter((item)=>String(item)!==String(value));else state.userBlacklist=state.userBlacklist.filter((item)=>String(item)!==String(value));refreshCurrentPanelBody();}
    } else if (command.dataset.command === 'admin-announce') {
      const content=$('#admin-announcement').value;
      if(await safeCommand('chat.notice',{text:content},'公告已发布'))setRoomAnnouncement(content,{source:'local'});
    } else if (command.dataset.command === 'admin-password') {
      await safeCommand('auth.adminPassword',{password:$('#admin-password').value},'修改密码请求已发送');
    } else if (command.dataset.command === 'admin-vote') {
      await safeCommand('admin.vote',{value:$('#admin-vote').value},'投票比例已提交');
    } else if(command.dataset.command==='admin-pick-wait'){
      const minutes=Number($('#admin-pick-wait')?.value);
      if(!Number.isInteger(minutes)||minutes<0||minutes>1440){notify('等待时间必须是 0 到 1440 的整数分钟');return;}
      await safeCommand('admin.pickWait',{minutes},minutes===0?'已提交取消点歌等待限制':'点歌等待时间已提交');
    } else if(command.dataset.command==='admin-clear-queue-on-leave'){
      const enabled=$('#admin-clear-queue-on-leave')?.value!=='false';
      await safeCommand('admin.clearQueueOnLeave',{enabled},enabled?'已提交开启离房清理点歌':'已提交关闭离房清理点歌');
    } else if(command.dataset.command==='admin-default-playlist-mode'){
      const random=$('#admin-default-playlist-mode')?.value!=='false';
      await safeCommand('admin.defaultPlaylistMode',{random},random?'已提交默认列表随机不重复播放':'已提交默认列表顺序播放');
    } else if (command.dataset.command === 'admin-playlist') {
      const id=normalizeDefaultPlaylistIds($('#admin-playlist')?.value);if(!id){notify('请先输入歌单 ID 或 *歌曲 ID');return;}
      const source=$('input[name="admin-playlist-source"]:checked')?.value || state.adminPlaylistSource;
      state.adminPlaylistSource=source;state.pendingDefaultAppend={source,id,at:Date.now()};
      setAdminPlaylistStatus(`正在导入${source==='qq'?'QQ':'网易'}内容，请等待后端结果…`);
      try{await backend.command('playlist.append',{id,source});setTimeout(()=>{if(state.pendingDefaultAppend?.id===id && state.pendingDefaultAppend?.source===source && Date.now()-state.pendingDefaultAppend.at>=30000){state.pendingDefaultAppend=null;setAdminPlaylistStatus('请求已发送，但后端尚未返回导入结果；请查看默认列表或稍后重试。');}},30000);}
      catch(error){state.pendingDefaultAppend=null;setAdminPlaylistStatus(error.message || '发送失败');notify(error.message || '发送失败');}
    } else if (command.dataset.command === 'admin-raw') {
      if (!state.isAdmin) { notify('请先登录管理员'); return; }
      await safeCommand('admin.raw',{destination:command.dataset.destination},'管理命令已发送');
    } else if (command.dataset.command === 'admin-song') {
      if (!state.isAdmin) { notify('请先登录管理员'); return; }
      const id=$('#admin-song').value.trim(); const dest={top:'/music/top',black:'/music/black',unblack:'/music/unblack'}[command.dataset.operation];
      if(id) await safeCommand('admin.raw',{destination:dest,body:JSON.stringify({id,sendTime:Date.now()})},'歌曲管理请求已发送');
    } else if (command.dataset.command === 'admin-user') {
      if (!state.isAdmin) { notify('请先登录管理员'); return; }
      const id=$('#admin-user').value.trim(); const op=command.dataset.operation; const dest={black:'/chat/black',picker:`/auth/setPicker/${encodeURIComponent(id)}`,'picker-off':`/auth/setNoPicker/${encodeURIComponent(id)}`,voter:`/auth/setVoter/${encodeURIComponent(id)}`,'voter-off':`/auth/setNoVoter/${encodeURIComponent(id)}`}[op]; const body=op==='black'?JSON.stringify({sessionId:id,sendTime:Date.now()}):'';
      if(id) await safeCommand('admin.raw',{destination:dest,body},'成员权限请求已发送');
    } else {
      notify(name ? `已处理《${name}》` : '操作成功');
    }
  }
});

document.addEventListener('submit', async (event) => {
  event.preventDefault();
  if(event.target.id==='system-admin-login-form'){
    await submitSystemAdminLogin(event.target);
    return;
  }
  if (event.target.id === 'composer') {
    const input = $('#message-input');
    const text = input.value.trim();
    if (!text) return;
    const rootLogin=text.match(/^root\s+(.+)$/i);
    if(rootLogin){input.value='';await safeCommand('auth.root',{password:rootLogin[1]},'超级管理员登录请求已发送');return;}
    const rootNotice=text.match(/^通知\s+(.+)$/);
    if(rootNotice && state.isRoot){input.value='';await safeCommand('chat.globalNotice',{text:rootNotice[1]},'全站通知已发送');return;}
    const rootVolume=text.match(/^调整音量(?:\s+(.+))?$/);
    if(rootVolume){input.value='';if(!state.isRoot){notify('仅超级管理员可调整全房间音量');return;}const value=Number(rootVolume[1]);if(!Number.isFinite(value)||value<0||value>100){notify('请输入“调整音量 0-100”');return;}await safeCommand('player.volume',{value:Math.round(value)},`已请求将全房间音量调整为 ${Math.round(value)}`);return;}
    const quickPick=text.match(/^点歌\s+(.+)$/);
    if(quickPick){input.value='';const song=quickPick[1].trim();if(!song){notify('请输入“点歌 歌曲ID”');return;}const ok=await requestMusicPick({name:song,source:sourceCode(state.source)},'快捷点歌请求已发送');if(ok)state.ownPickedIds.add(String(song));return;}
    const pending={text,time:Date.now()};state.lastSentMessage=pending;
    if (await safeCommand('chat.send', { text })) input.value = '';
    else if(state.lastSentMessage===pending)state.lastSentMessage=null;
    return;
  }

  if (event.target.dataset.form === 'create-room') {
    const data = new FormData(event.target);
    let claimedForCreate=false;
    try {
      state.duplicateConnection=false;
      const needPwd=Boolean(data.get('needPwd')), permanent=Boolean(data.get('enableStatus'));
      const payload = { name:data.get('name'), desc:data.get('desc'), password:needPwd?(data.get('password') || ''):'', needPwd, enableStatus:permanent, retainKey:permanent?(data.get('retain') || ''):'' };
      if(state.connected){
        state.pendingCreatedRoom=payload;
        await backend.command('house.create',payload);
        notify('正在创建并切换房间…');
        return;
      }
      claimedForCreate=claimRoomLease('creating');
      if(!claimedForCreate){showDuplicateWindowHome();return;}
      const id = await backend.createRoom(payload);
      await loadRooms();
      const room = state.rooms.find((item)=>String(item.id)===String(id)) || {id,name:payload.name,password:payload.password};
      state.roomId=id; state.room=payload.name; state.roomDescription=payload.desc;state.roomPassword=payload.password;updateDocumentTitle();
      await backend.connect({id,password:payload.password,connectType:'',name:payload.name});
      renderRooms(); closePanel(); notify(`房间 ${payload.name} 已创建`);
    } catch (error) { if(claimedForCreate&&!state.connected)releaseRoomLease();if(!state.duplicateConnection)notify(error.message || '创建房间失败'); }
  }
  if(event.target.dataset.form==='room-password'){
    const password=String(new FormData(event.target).get('password') || '');if(state.pendingRoom)await enterSelectedRoom(state.pendingRoom,password);
  }
  if (event.target.dataset.form === 'profile') {
    const data = new FormData(event.target); const nickname = persistentNickname(data.get('nickname') || '');
    if (nickname && await safeCommand('setting.name',{name:nickname})) persistNickname(nickname);
    closePanel(); notify('个人设置已保存');
  }
});

$('#message-input').addEventListener('keydown', (event) => {
  if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); $('#composer').requestSubmit(); }
});
document.addEventListener('input',(event)=>{
  if(event.target.id==='galaxy-room-search'){state.galaxyKeyword=event.target.value;state.galaxyPage=0;renderGalaxy();}
  if(event.target.id==='room-search'){state.roomFilter=event.target.value;state.roomPage=1;const box=$('#room-search-results');if(box)box.innerHTML=roomResultsHTML();const total=$('#room-total-count');if(total)total.textContent=visibleRooms().length;}
  if(event.target.id==='system-user-ip-filter'){state.systemAdmin.userIpFilter=event.target.value;refreshCurrentPanelBody();}
  if(event.target.id==='history-search'){state.historyFilter=event.target.value;syncHistoryPanel();}
});
document.addEventListener('change',async(event)=>{
  if(event.target.name==='admin-playlist-source')state.adminPlaylistSource=event.target.value;
  if(event.target.id==='system-room-select'){
    const value=event.target.value.trim();
    const room=state.rooms.find((item)=>String(item.id)===value)||state.rooms.find((item)=>String(item.name)===value);
    if(room){state.systemAdmin.selectedRoomId=String(room.id);if(currentPanel==='system-admin')refreshCurrentPanelBody();}
  }
  if(event.target.id==='system-room-can-destroy'){
    const button=$('[data-command="system-save-room"]');
    if(button){button.dataset.confirmDestroy='';button.textContent='保存房间修改';button.classList.remove('danger-button');button.classList.add('primary-button');}
  }
  if(event.target.id==='hide-empty-rooms'){state.hideEmptyRooms=event.target.checked;refreshCurrentPanelBody();}
  if(event.target.id==='hide-locked-rooms'){state.hideLockedRooms=event.target.checked;state.roomPage=1;refreshCurrentPanelBody();}
  if(event.target.id==='hide-unlocked-rooms'){state.hideUnlockedRooms=event.target.checked;state.roomPage=1;refreshCurrentPanelBody();}
  if(event.target.id==='toggle-room-password'){const input=$('[data-conditional="room-password"]');if(input){input.hidden=!event.target.checked;input.required=event.target.checked;}}
  if(event.target.id==='toggle-room-permanent'){const input=$('[data-conditional="room-permanent"]');if(input){input.hidden=!event.target.checked;input.required=event.target.checked;}}
  if(event.target.id==='favorite-import' && event.target.files?.[0]){
    try{const parsed=JSON.parse(await event.target.files[0].text()); const incoming=Array.isArray(parsed)?parsed:Object.values(parsed); const map=new Map(state.favorites.map((item)=>[String(item.id),item])); incoming.forEach((item)=>{if(item?.id!=null)map.set(String(item.id),item);}); state.favorites=[...map.values()]; localStorage.setItem('collectMusic',JSON.stringify(Object.fromEntries(state.favorites.map((item)=>[item.id,item])))); state.queue.forEach((song)=>song.favorite=isFavorite(song.id)); renderQueue(); openPanel('favorites'); notify(`已导入 ${incoming.length} 首收藏`);}catch(_){notify('收藏文件格式不正确');}
  }
});
document.addEventListener('keydown',(event)=>{
  if(event.key!=='Enter' || event.shiftKey)return;
  const commandByInput={'song-search':'search-song','playlist-search':'search-playlist','playlist-user-search':'search-netease-user','picture-search':'search-picture','countdown-minutes':'set-countdown'};
  const command=commandByInput[event.target.id];if(command){event.preventDefault();$(`[data-command="${command}"]`)?.click();}
});

async function loadMoreCurrentPanel(){
  if(currentPanel==='rooms'){
    const total = visibleRooms().length;
    const currentRendered = (state.roomPage || 1) * ROOMS_BATCH_SIZE;
    if (currentRendered < total) {
      state.roomPage = (state.roomPage || 1) + 1;
      const box = $('#room-search-results');
      if (box) box.innerHTML = roomResultsHTML();
    }
  }
  if(currentPanel==='search' && !state.searchLoading && state.searchResults.length<state.searchTotal){state.searchLoading=true;state.searchPage+=1;await safeCommand('music.search',{keyword:state.searchKeyword,source:sourceCode(state.source),pageIndex:state.searchPage,pageSize:20});}
  if(currentPanel==='hot' && !state.hotLoading && state.hotResults.length<state.hotTotal){state.hotLoading=true;state.hotPage+=1;await safeCommand('music.search',{keyword:'*热歌榜',source:state.hotSource,pageIndex:state.hotPage,pageSize:20});}
  if(currentPanel==='default-list' && !state.defaultPlaylist.loading && !state.defaultPlaylist.error && !state.defaultPlaylist.exhausted && state.defaultPlaylist.songs.length<state.defaultPlaylist.total){await loadDefaultPlaylist(state.defaultPlaylist.page+1);}
  if(currentPanel==='playlist' && !state.playlistLoading && state.playlistResults.length<state.playlistTotal){state.playlistLoading=true;state.playlistPage+=1;await safeCommand('playlist.search',{keyword:state.playlistKeyword,source:state.playlistSource,pageIndex:state.playlistPage,pageSize:20});}
  if(currentPanel==='playlist-users' && !state.userLoading && state.userResults.length<state.userTotal){state.userLoading=true;state.userPage+=1;await safeCommand('user.search',{keyword:state.userSearchKeyword,source:'wy',pageIndex:state.userPage,pageSize:20});}
  if(currentPanel==='picture' && !state.pictureLoading && state.pictureResults.length<state.pictureTotal){state.pictureLoading=true;state.picturePage+=1;await safeCommand('picture.search',{keyword:state.pictureKeyword,pageIndex:state.picturePage,pageSize:12});}
}
$('#modal-body').addEventListener('scroll',(event)=>{if(event.target.scrollHeight-event.target.scrollTop-event.target.clientHeight<140)loadMoreCurrentPanel();});
const chatMessages=$('#messages');
let chatScrollIntentTimer=0;
function recordChatScrollIntent(){
  clearTimeout(chatScrollIntentTimer);
  chatScrollIntentTimer=setTimeout(()=>{
    chatFollowLatest=chatIsNearBottom(chatMessages.scrollTop,chatMessages.scrollHeight,chatMessages.clientHeight);
    if(chatFollowLatest)chatHasUnread=false;
    updateChatJumpButton();
  },0);
}
chatMessages.addEventListener('scroll',updateChatJumpButton,{passive:true});
chatMessages.addEventListener('wheel',recordChatScrollIntent,{passive:true});
chatMessages.addEventListener('touchmove',recordChatScrollIntent,{passive:true});
chatMessages.addEventListener('pointerdown',recordChatScrollIntent,{passive:true});
chatMessages.addEventListener('pointermove',(event)=>{if(event.buttons)recordChatScrollIntent();},{passive:true});
chatMessages.addEventListener('keydown',(event)=>{if(['ArrowUp','ArrowDown','PageUp','PageDown','Home','End',' '].includes(event.key))recordChatScrollIntent();});
chatMessages.addEventListener('load',()=>{if(chatFollowLatest)scrollChatToLatest();},true);
$('#seek').addEventListener('input', (event) => {
  const duration = Number.isFinite(audio.duration) ? audio.duration : 0;
  const seconds = Math.round(duration * Number(event.target.value) / 100);
  if (duration) audio.currentTime = seconds;
  $('#current-time').textContent = `${String(Math.floor(seconds / 60)).padStart(2,'0')}:${String(seconds % 60).padStart(2,'0')}`;
});
function syncMuteButtons(){
  $$('[data-action="toggle-mute"]').forEach((button)=>{const muted=Boolean(audio.muted);button.innerHTML=icon(muted?'volume_off':'volume_up');button.setAttribute('aria-label',muted?'开启声音':'关闭声音');button.title=muted?'开启声音':'关闭声音';button.classList.toggle('muted',muted);});
}
function setAudioMuted(muted){
  audio.muted=Boolean(muted);audio.defaultMuted=Boolean(muted);audio.toggleAttribute('muted',Boolean(muted));syncMuteButtons();
}
function applyVolume(value){
  const safe=Math.max(0,Math.min(100,Number(value)||0));audio.volume=safe/100;setAudioMuted(false);$('#volume').value=safe;$('#focus-volume').value=safe;localStorage.setItem('VOLUME',String(safe));
}
$('#volume').addEventListener('input',(event)=>applyVolume(event.target.value));
$('#focus-volume').addEventListener('input',(event)=>applyVolume(event.target.value));
document.addEventListener('keydown', (event) => { if(event.key!=='Escape')return;if(!$('#image-preview').hidden){closeImagePreview();return;}if(state.focusMode)setFocusMode(false);else if(!$('#modal-layer').hidden)closePanel(); });

audio.volume = Number(localStorage.getItem('VOLUME') || 72) / 100;
$('#volume').value = Math.round(audio.volume * 100);
$('#focus-volume').value = Math.round(audio.volume * 100);
syncMuteButtons();
configureMediaSession();
audio.addEventListener('play',()=>{dismissCenterNotice('playback');syncPlayButtons(true,{restartCover:true});});
audio.addEventListener('playing',()=>syncPlayButtons(true));
audio.addEventListener('pause',()=>syncPlayButtons(false));
audio.addEventListener('ended',()=>syncPlayButtons(false));
audio.addEventListener('timeupdate',()=>{
  const duration = Number.isFinite(audio.duration) ? audio.duration : 0;
  $('#seek').value = duration ? (audio.currentTime / duration) * 100 : 0;
  $('#current-time').textContent = formatTime(audio.currentTime);
  const end = $('.time-row span:last-child'); if (end) end.textContent = formatTime(duration);
  updateCurrentLyric(audio.currentTime);
});
audio.addEventListener('error',()=>notify('歌曲音频加载失败，可尝试重新点歌'));
function restorePlaybackVisuals(){
  if(document.hidden)return;
  const playing=Boolean(audio.src && !audio.ended && (state.playing || !audio.paused));
  syncPlayButtons(playing,{restartCover:playing});
  requestAnimationFrame(()=>{if(!document.hidden&&playing)ensureCoverRotation(true);});
  window.setTimeout(()=>{if(!document.hidden&&playing)ensureCoverRotation(true);},120);
}
let backgroundedAt=0;
let resumeConnectionTask=null;
function markAppBackgrounded(){backgroundedAt=Date.now();}
function resumeAfterBackground(){
  const hiddenFor=backgroundedAt ? Date.now()-backgroundedAt : 0;backgroundedAt=0;
  restorePlaybackVisuals();syncPlaybackRetention();
  if(!state.roomId||state.duplicateConnection)return Promise.resolve(false);
  const socketHealthy = Boolean(state.connected && backend.socket?.readyState === 1 && backend.stomp?.connected);
  if(socketHealthy){
    if(hiddenFor > 10000) backend.command('house.members').catch(()=>{});
    return Promise.resolve(true);
  }
  if(resumeConnectionTask)return resumeConnectionTask;
  resumeConnectionTask=backend.ensureActiveConnection({force: true})
    .finally(()=>{resumeConnectionTask=null;});
  return resumeConnectionTask;
}
document.addEventListener('visibilitychange',()=>{if(document.hidden)markAppBackgrounded();else resumeAfterBackground();syncPlaybackRetention();});
window.addEventListener('focus',()=>{restorePlaybackVisuals();resumeAfterBackground();});
window.addEventListener('online',()=>resumeAfterBackground());
window.addEventListener('pageshow',()=>{resumeAfterBackground();if(state.connected&&!claimRoomLease(state.roomId)){backend.disconnect(true).finally(showDuplicateWindowHome);}});
window.addEventListener('pagehide',releaseRoomLease);
window.addEventListener('storage',(event)=>{
  if(event.key!==ROOM_LEASE_KEY || !state.connected)return;
  const remote=readRoomLease();if(!remote?.owner || remote.owner===roomTabId)return;
  const mine={owner:roomTabId,startedAt:roomLeaseStartedAt};
  if(leasePrecedes(remote,mine)){backend.disconnect(true).finally(()=>{releaseRoomLease();showDuplicateWindowHome();});}
  else writeRoomLease(state.roomId);
});

backend.on('connection',({status,sessionId})=>{
  if(status==='duplicate'){
    releaseRoomLease();showDuplicateWindowHome();return;
  }
  state.connectionStatus=status; state.connected=status==='connected';
  syncRoomPanelGate();
  if(status !== 'connected') state.roomState = null;
  renderRoomState();
  $('#online-count').textContent = status==='connected' ? `${state.online} 人` : ({connecting:'连接中',reconnecting:'重连中',error:'连接失败'}[status] || '离线');
  $('#connection-dot').style.background = state.connected ? 'var(--green)' : (status==='connecting'||status==='reconnecting' ? '#d18a19' : '#9b9287');
  if (status==='connected') { if(sessionId)state.mySessionId=String(sessionId);writeRoomLease(state.roomId);state.messages.push({type:'system',text:`已连接 ${state.room}`}); renderMessages();celebrateRoomEntry(state.roomId); backend.command('house.members').catch(()=>{}); }
  if (status==='reconnecting') notify('连接中断，正在自动重连');
  syncEntryExperience();
});
backend.on('default-playlist', (message) => {
  const data = message.data;
  const pending = state.defaultPlaylist;
  if (!data || data.requestId !== pending.requestId || String(data.houseId) !== String(state.roomId)) return;
  const incoming=Array.isArray(data.songs)?data.songs:[];
  const append=pending.page>1;
  const songs=append?[...pending.songs,...incoming]:incoming;
  state.defaultPlaylist = {...pending,...data,page:pending.page,loading:false,error:String(message.code)==='20000'?'':message.message || '读取失败',songs,exhausted:append && incoming.length===0};
  if (currentPanel === 'default-list') {
    if (!append) refreshCurrentPanelBody();
    else {
      const list=$('#default-results');if(list)list.innerHTML=defaultPlaylistRows(songs,state.favorites);
      const status=$('#default-load-state');if(status)status.textContent=state.defaultPlaylist.error || '';
      const scope=$('#default-scope');if(scope)scope.textContent=state.defaultPlaylist.scope==='room'?'当前房间默认列表':'读取当前房间默认列表';
      updateSummary(songs.length,state.defaultPlaylist.total,'首歌曲');
    }
  }
});
backend.on('default-playlist-changed', ({data}) => {
  if (String(data) === String(state.roomId) && currentPanel === 'default-list') loadDefaultPlaylist(1);
});
backend.on('room-state', (message) => {
  const data = message.data;
  if (String(message.code) !== '20000' || !data || String(data.houseId) !== String(state.roomId)) return;
  state.roomState = data;
  state.goodMode = data.goodModel === true;
  renderRoomState();
  renderQueue();
});
backend.on('online',({data})=>{
  state.online=Number(data?.count ?? data ?? 0); $('#online-count').textContent=`${state.online} 人`; $('#chat-online-count').textContent=state.online; if(currentPanel==='members') refreshCurrentPanelBody();
});
backend.on('chat',({data})=>{
  if (!data) return;
  const userId=chatUserId(data);
  const sessionId=String(data.sessionId || data.userId || data.uid || data.senderId || data.fromSessionId || data.id || '');
  const rawContent=String(data.content || '');let text=rawContent;const images=[];text=text.replace(/picture:(https?:\/\/[^\s]+)/gi,(_,url)=>{images.push(url);return '';}).trim();
  const when=new Date(data.pushTime || data.sendTime || Date.now());
  const echoed=Boolean(state.lastSentMessage && rawContent===state.lastSentMessage.text && Date.now()-state.lastSentMessage.time<10000);
  state.pendingPicks=state.pendingPicks.filter((pick)=>Date.now()-pick.at<15000 && pick.roomId===state.roomId);
  const ownPickIndex=state.pendingPicks.findIndex((pick)=>matchesPendingPickChat({content:text},pick));
  const ownPick=ownPickIndex>=0 && (!state.mySessionId || !sessionId || sessionId===state.mySessionId);
  const ownVote=matchesPendingVoteChat({content:rawContent,sessionId,nickName:data.nickName},state.pendingVote,{roomId:state.roomId,sessionId:state.mySessionId,nickname:state.nickname});
  state.pendingLikes=state.pendingLikes.filter((like)=>Date.now()-like.at<10000 && like.roomId===state.roomId);
  const ownLikeIndex=state.pendingLikes.findIndex((like)=>matchesPendingLikeChat({content:rawContent},like));
  const ownLike=ownLikeIndex>=0 && (!state.mySessionId || !sessionId || sessionId===state.mySessionId);
  if(ownPick){state.pendingPicks.splice(ownPickIndex,1);if(sessionId)state.mySessionId=sessionId;}
  if(ownVote){state.pendingVote=null;if(sessionId)state.mySessionId=sessionId;}
  if(ownLike){state.pendingLikes.splice(ownLikeIndex,1);if(sessionId)state.mySessionId=sessionId;}
  if(echoed){if(sessionId)state.mySessionId=sessionId;if(data.nickName)persistNickname(data.nickName);state.lastSentMessage=null;}
  state.messages.push({type:'chat',name:userDisplayName(data,userId),sessionId,time:`${String(when.getHours()).padStart(2,'0')}:${String(when.getMinutes()).padStart(2,'0')}`,text,images,me:echoed || ownPick || ownVote || ownLike});
  renderMessages();
});
backend.on('notice-message',(message)=>{
  if(state.blacklistRequest){const kind=state.blacklistRequest;const raw=message.data ?? message.message;const source=Array.isArray(raw)?raw:(Array.isArray(raw?.data)?raw.data:[raw?.data ?? raw]);const list=source.flatMap((item)=>typeof item==='string'?item.split(/[,，]/):[item]).map((item)=>typeof item==='string'?item.trim().replace(/^(?:用户|音乐|歌曲)?黑名单[：:]\s*/,''):item).filter((item)=>item!=null && String(item).trim()!=='' && !isEmptyBlacklistValue(item));if(kind==='music')state.musicBlacklist=list;else state.userBlacklist=list;state.blacklistRequest='';if(currentPanel===`${kind}-blacklist`)refreshCurrentPanelBody();}
  const text=message.message || (typeof message.data==='string'?message.data:'系统通知');
  if(state.pendingDefaultAppend){
    const result=String(text).match(/已添加\s*(\d+)\s*首歌至默认播放列表/);
    const rejected=String(message.code)!=='20000' && /默认|歌单|导入|平台|权限|超时/.test(String(text));
    if(result || rejected){const sourceName=state.pendingDefaultAppend.source==='qq'?'QQ':'网易';const count=result?Number(result[1]):null;setAdminPlaylistStatus(result?(count?`${sourceName}导入成功：新增 ${count} 首歌曲。`:`${sourceName}导入了 0 首歌曲，请核对歌单 ID 和平台服务。`):`${sourceName}导入失败：${text}`);state.pendingDefaultAppend=null;}
  }
  state.messages.push({type:'system',text:String(text)}); renderMessages();
  if (message.message) notify(message.message);
});
backend.on('notice',(text)=>{ if(text){state.messages.push({type:'system',text:`全站通知：${text}`});renderMessages();showCenterNotice('global-notice','全站通知',String(text),{duration:30000});} });
backend.on('announcement',({data})=>{
  if(!data || typeof data!=='object' || !('content' in data))return;
  const roomId=String(data.houseId ?? state.pendingRoom?.id ?? state.roomId);
  if(roomId!==String(state.roomId) && roomId!==String(state.pendingRoom?.id))return;
  setRoomAnnouncement(data.content ?? '',{roomId,source:'broadcast'});
  if(data.content){state.messages.push({type:'system',text:`公告：${data.content}`});renderMessages();showRoomAnnouncement(data.content);}
});
backend.on('room-create-start',(message)=>{if(String(message.code)==='20000'){state.queue=[];state.currentMusic=null;audio.pause();audio.removeAttribute('src');audio.load();syncAdminUI();renderQueue();}});
backend.on('room-created',async(message)=>{
  const payload=state.pendingCreatedRoom;
  if(!payload)return;
  if(String(message.code)!=='20000'){state.pendingCreatedRoom=null;notify(message.message || '创建房间失败');return;}
  const id=message.data?.id ?? message.data;
  roomAnnouncementCache.delete(String(id));state.pendingCreatedRoom=null;state.roomState=null;renderRoomState();state.roomId=id;state.roomPassword=payload.password;state.room=payload.name;state.roomDescription=payload.desc || '';setRoomAnnouncement('');state.mySessionId=String(backend.sessionId || state.mySessionId || '');state.ownPickedIds.clear();state.likedQueueIds.clear();state.pendingPicks=[];state.pendingVote=null;state.pendingLikes=[];state.isAdmin=false;state.goodMode=false;state.miniCode='';state.miniCodeError=false;
  backend.adoptRoom({id,password:payload.password,connectType:'',name:payload.name});
  updateDocumentTitle();syncAdminUI();renderQueue();syncEntryExperience();closePanel();await loadRooms();renderRooms();
  if(state.nickname)backend.command('setting.name',{name:state.nickname}).catch(()=>{});
  notify(`房间 ${payload.name} 已创建并切换`);
  celebrateRoomEntry(state.roomId);
  backend.command('house.members').catch(()=>{});
});
function applyQueueSnapshot(message){
  const data=message.data;
  if(message.message==='goodlist')state.goodMode=true;
  state.queue=Array.isArray(data) ? data.map((song)=>({...song,picker:normalizedNickname(song.nickName || song.picker || song.userName) || String(song.sessionId || song.userId || '匿名用户'),favorite:isFavorite(song.id)})) : [];
  const playingRow=state.queue[0];if(state.currentMusic&&playingRow&&String(playingRow.id)===String(state.currentMusic.id)){state.currentMusic={...state.currentMusic,nickName:playingRow.nickName || playingRow.picker,sessionId:playingRow.sessionId || state.currentMusic.sessionId};renderCurrentPicker();}
  const waitingIds=new Set(waitingQueueItems(state.queue).map((song)=>String(song.id)));
  for(const id of state.likedQueueIds)if(!waitingIds.has(id))state.likedQueueIds.delete(id);
  if(!state.mySessionId){const pickedSession=sessionIdFromPickedQueue(state.queue,state.ownPickedIds,state.nickname);if(pickedSession){state.mySessionId=pickedSession;renderMessages();}}
  if(state.nickname){
    const ownEcho=state.queue.find((song)=>state.ownPickedIds.has(String(song.id)) && normalizedNickname(song.nickName || song.picker)===normalizedNickname(state.nickname));
    if(ownEcho?.sessionId)state.mySessionId=String(ownEcho.sessionId);
    if(ownEcho?.nickName)persistNickname(ownEcho.nickName);
  }
  renderQueue();
}
backend.on('room-entered',async(message)=>{
  const room=state.pendingRoom;if(!room)return;
  if(String(message.code)!=='20000'){state.pendingRoom=null;state.awaitingRoomEntry=false;state.pendingQueueSnapshot=null;notify(message.message || '进入房间失败');return;}
  const pendingQueueSnapshot=state.pendingQueueSnapshot;
  state.pendingRoom=null;state.awaitingRoomEntry=false;state.pendingQueueSnapshot=null;state.roomState=null;renderRoomState();state.roomId=room.id;state.roomPassword=room.password || '';state.room=room.name;state.roomDescription=room.desc || '';setRoomAnnouncement(announcementFromRoom(message.data) ?? announcementFromRoom(room.raw || room));state.mySessionId=String(backend.sessionId || state.mySessionId || '');state.ownPickedIds.clear();state.likedQueueIds.clear();state.pendingPicks=[];state.pendingVote=null;state.pendingLikes=[];state.isAdmin=state.isRoot;state.online=0;state.members=[];state.queue=[];backend.adoptRoom({id:room.id,password:room.password || '',connectType:'enter',name:room.name});updateDocumentTitle();syncAdminUI();if(pendingQueueSnapshot)applyQueueSnapshot(pendingQueueSnapshot);else renderQueue();renderRooms();syncEntryExperience();closePanel();if(state.nickname)backend.command('setting.name',{name:state.nickname}).catch(()=>{});notify(`已进入 ${room.name}`);celebrateRoomEntry(state.roomId);backend.command('house.members').catch(()=>{});
});
backend.on('queue',(message)=>{
  if(state.awaitingRoomEntry){state.pendingQueueSnapshot=message;return;}
  applyQueueSnapshot(message);
});
backend.on('music',({data})=>{
  if(!state.mySessionId){const pickedSession=sessionIdFromPickedQueue(data?[data]:[],state.ownPickedIds,state.nickname);if(pickedSession){state.mySessionId=pickedSession;renderMessages();}}
  if(data?.id!=null && state.mySessionId)state.ownPickedIds.delete(String(data.id));
  updatePlayer(data);
  state.lyrics=parseTimedLyrics(data?.lyric || '');state.currentLyricIndex=-1;$('#lyrics-view').innerHTML=state.lyrics.length?state.lyrics.map((line,i)=>`<p data-lyric-index="${i}">${escapeHTML(line.text)}</p>`).join(''):'<p>暂无歌词</p>';updateCurrentLyric(0);
});
backend.on('volume',({data})=>{const value=Number(data);if(Number.isFinite(value)){applyVolume(value);const rootVolume=$('#root-volume');if(rootVolume)rootVolume.value=Math.max(0,Math.min(100,value));notify(`房间音量已同步为 ${Math.round(Math.max(0,Math.min(100,value)))}`);}});
backend.on('search-results',({data})=>{
  const results=Array.isArray(data?.data) ? data.data : [];
  if(currentPanel==='hot') {
    state.hotResults=state.hotPage>1?[...state.hotResults,...results]:results;state.hotTotal=Number(data?.totalSize ?? state.hotResults.length);state.hotLoading=false; const list=$('#hot-results'); if(list) list.innerHTML=hotResultsHTML();updateSummary(state.hotResults.length,state.hotTotal,'首歌曲');resolveMissingCovers(results,state.hotSource);
  } else {
    state.searchResults=state.searchPage>1?[...state.searchResults,...results]:results; state.searchTotal=Number(data?.totalSize || state.searchResults.length); state.searchLoading=false;
    if(currentPanel==='search') { const list=$('#search-results'); if(list) list.innerHTML=searchResultsHTML();updateSummary(state.searchResults.length,state.searchTotal,'首歌曲');resolveMissingCovers(results,sourceCode(state.source)); const button=$('[data-command="search-song"]'); if(button){button.disabled=false;button.innerHTML=`${icon('search')}搜索`;} }
  }
  notify(`找到 ${data?.totalSize ?? results.length} 个结果`);
});
backend.on('playlist-results',({data})=>{
  const results=Array.isArray(data?.data)?data.data:[]; state.playlistResults=state.playlistPage>1?[...state.playlistResults,...results]:results; state.playlistTotal=Number(data?.totalSize || state.playlistResults.length); state.playlistLoading=false;
  const box=$('#playlist-results'); if(box) box.innerHTML=playlistResultsHTML();updateSummary(state.playlistResults.length,state.playlistTotal,state.playlistKind==='radio'?'个电台':'个歌单');
});
backend.on('user-results',({data})=>{
  const results=Array.isArray(data?.data)?data.data:[]; state.userResults=state.userPage>1?[...state.userResults,...results]:results; state.userTotal=Number(data?.totalSize || state.userResults.length); state.userLoading=false;
  if(currentPanel==='playlist-users'){const box=$('#playlist-user-results');if(box)box.innerHTML=userResultsHTML();updateSummary(state.userResults.length,state.userTotal,'位用户');}
});
backend.on('picture-results',({data})=>{
  const results=Array.isArray(data?.data)?data.data:[];state.pictureResults=state.picturePage>1?[...state.pictureResults,...results]:results;state.pictureTotal=Number(data?.totalSize ?? state.pictureResults.length);state.pictureLoading=false;
  if(currentPanel==='picture') refreshCurrentPanelBody();
  notify(`找到 ${data?.totalSize ?? state.pictureResults.length} 张图片`);
});
backend.on('members',({data})=>{ state.members=Array.isArray(data)?data:[];const self=state.members.find(isCurrentMember);if(!state.mySessionId&&self?.sessionId)state.mySessionId=String(self.sessionId);state.online=state.members.length; $('#online-count').textContent=`${state.online} 人`; $('#chat-online-count').textContent=state.online; if(currentPanel==='members') refreshCurrentPanelBody(); });
backend.on('nickname',({data})=>{ const name=data?.name || data?.nickName || (typeof data==='string'?data:'') || '';if(data?.sessionId)state.mySessionId=String(data.sessionId);persistNickname(name); });
backend.on('auth-admin',(message)=>{ state.isAdmin=String(message.code)==='20000'; if(!state.isAdmin && backend.lastAuthCommand?.type==='auth.admin') backend.lastAuthCommand=null; syncAdminUI(); renderQueue(); if(currentPanel==='admin'||currentPanel==='user-actions')refreshCurrentPanelBody(); if(state.isAdmin)refreshRoomAnnouncement(); notify(message.message || (state.isAdmin?'管理员登录成功':'管理员登录失败')); });
backend.on('auth-root',(message)=>{state.isRoot=String(message.code)==='20000';if(!state.isRoot){if(backend.lastAuthCommand?.type==='auth.root') backend.lastAuthCommand=null;systemAdminApi.logout();state.systemAdmin.retainKeys=[];state.systemAdmin.users=null;if(currentPanel==='system-admin')openPanel('admin');}state.isAdmin=state.isRoot || state.isAdmin;syncAdminUI();renderQueue();if(currentPanel==='admin'||currentPanel==='user-actions')refreshCurrentPanelBody();if(state.isRoot)refreshRoomAnnouncement();notify(message.message || (state.isRoot?'超级管理员登录成功':'超级管理员登录失败'));});
backend.on('good-mode',({data})=>{state.goodMode=String(data)==='GOOD';renderQueue();});

let roomsRequest=null;
async function loadRooms() {
  if(roomsRequest)return roomsRequest;
  roomsRequest=(async()=>{
    try {
      const rooms=await backend.getRooms();
      state.rooms=sortRoomsLikeLegacy(rooms || []).map((room)=>({id:room.id,name:room.name,desc:room.desc || '听歌房',count:room.population || 0,locked:Boolean(room.needPwd),raw:room}));
      if (state.roomAnnouncement === null) {
        const current = state.rooms.find((room) => String(room.id) === String(state.roomId));
        if (current) setRoomAnnouncement(announcementFromRoom(current.raw));
      }
      renderRooms(); return state.rooms;
    } catch(error) { state.messages.push({type:'system',text:`房间列表读取失败：${error.message}`}); renderMessages(); notify('无法连接后端服务'); return []; }
  })();
  try{return await roomsRequest;}finally{roomsRequest=null;}
}

async function initialize() {
  const buttonObserver=new MutationObserver((records)=>records.forEach((record)=>record.addedNodes.forEach((node)=>{if(node.nodeType===1)annotateButtons(node);})));buttonObserver.observe(document.body,{childList:true,subtree:true});
  setupGridTwinkles();renderRooms(); renderQueue(); setSidebarCollapsed(state.sidebarCollapsed,{persist:false});setPlayerTheme(state.playerTheme);setMobileView('player'); hydrateIcons(); syncPlayButtons(); syncAdminUI(); setFocusMode(false);syncFocusLyrics();
  const rooms=await loadRooms();
  if (!rooms.length) return;
  const query=new URLSearchParams(location.search); const requested=query.get('houseId');
  state.requestedRoomId=requested || '';
  state.requestedRoomPassword=query.get('housePwd') || '';
  state.sharedAutoplayPending=Boolean(requested);
  if (requested) {
    const targetIndex=rooms.findIndex((item)=>String(item.id)===String(requested));
    if (targetIndex>0) rooms.unshift(...rooms.splice(targetIndex,1));
  }
  state.room='请选择听歌房';
  updateDocumentTitle();
  $('#online-count').textContent='未连接';
  $('#chat-online-count').textContent='0';
  state.messages=[{type:'system',text:'请选择一个听歌房，进入后开始同步播放与聊天。'}];
  $('#track-title').textContent='选择房间开始听歌';$('#track-artist').textContent='一起听歌吧';$('#track-album').textContent='音乐房间';$('#current-lyric').textContent='进入房间后自动同步歌词';$('#next-lyric').textContent='';
  renderRooms(); renderMessages(); syncEntryExperience();
  if(requested){
    const target=rooms.find((item)=>String(item.id)===String(requested));
    if(!target){notify('分享的听歌房不存在或已关闭');return;}
    if(target.locked&&!state.requestedRoomPassword){state.pendingRoom=target;openPanel('room-password');return;}
    await enterSelectedRoom(target,state.requestedRoomPassword);
  }
}

let scrollbarIdleTimer;
function showScrollbarsWhileActive() {
  document.documentElement.classList.add('scrollbar-active');
  clearTimeout(scrollbarIdleTimer);
  scrollbarIdleTimer = setTimeout(() => document.documentElement.classList.remove('scrollbar-active'), 1500);
}
for (const eventName of ['pointermove', 'pointerdown', 'wheel', 'touchstart', 'touchmove', 'keydown']) {
  document.addEventListener(eventName, showScrollbarsWhileActive, { passive: true });
}
window.addEventListener('blur', () => {
  clearTimeout(scrollbarIdleTimer);
  document.documentElement.classList.remove('scrollbar-active');
});

document.addEventListener('plusready', () => {
  try {
    const statusbarHeight = plus.navigator.getStatusbarHeight();
    if (statusbarHeight > 0) {
      document.documentElement.style.setProperty('--status-bar-height', `${statusbarHeight}px`);
      document.body.classList.add('has-plus-statusbar');
    }
    plus.device.setWakelock(true);
    document.addEventListener('pause',markAppBackgrounded);
    document.addEventListener('resume',resumeAfterBackground);
  } catch (_) {}
});

function updateMobileChatViewport() {
  if (innerWidth <= 860 && $('#app')?.dataset?.mobileView === 'chat') {
    const vh = window.visualViewport ? window.visualViewport.height : innerHeight;
    const topbarEl = $('.topbar');
    const topbarH = topbarEl && getComputedStyle(topbarEl).display !== 'none' ? topbarEl.offsetHeight : 0;
    const navH = 64;
    const chatPanel = $('.app[data-mobile-view="chat"] .chat-panel');
    if (chatPanel) {
      chatPanel.style.height = `${Math.max(200, vh - topbarH - navH)}px`;
    }
  } else {
    const chatPanel = $('.chat-panel');
    if (chatPanel) chatPanel.style.height = '';
  }
}
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', updateMobileChatViewport);
  window.visualViewport.addEventListener('scroll', updateMobileChatViewport);
}
window.addEventListener('resize', updateMobileChatViewport);
document.addEventListener('focusin', (e) => {
  if (e.target && e.target.id === 'message-input') {
    setTimeout(() => {
      updateMobileChatViewport();
      const messages = $('#messages');
      if (messages) messages.scrollTop = messages.scrollHeight;
    }, 120);
  }
});

initialize();
setInterval(()=>{if(document.visibilityState==='visible'&&(!state.connected||currentPanel==='rooms')){loadRooms().then(()=>{if(currentPanel==='rooms')refreshCurrentPanelBody();});}},60000);

})();