# openrndr-js

This directory contains modules that are only relevant to the Kotlin/JS and Kotlin/Wasm targets.

| module                                                                       | description
|------------------------------------------------------------------------------|---------------------------------------
| [openrndr-webgl](openrndr-webgl)                                             | WebGL graphics back-end
| [openrndr-image-driver-browser](openrndr-image-driver-browser)               | Image driver, images decoded and encoded by the browser
| [openrndr-fontdriver-harfbuzzjs](openrndr-fontdriver-harfbuzzjs)             | Font driver backed by [harfbuzzjs](https://github.com/harfbuzz/harfbuzzjs)
| [openrndr-textshapingdriver-harfbuzzjs](openrndr-textshapingdriver-harfbuzzjs) | Text shaping driver backed by harfbuzzjs

## Fonts

The WebGL application sets up the harfbuzzjs font and text shaping drivers before the program starts. Fonts can be
loaded without blocking with `loadFaceSuspend(url)` in a program, `loadFace(url)` loads them with a synchronous request.

`drawer.text` works like on the JVM. The default font, IBM Plex Mono (SIL Open Font License, see
[openrndr-fontdriver-harfbuzzjs/IBMPlexMono-LICENSE.txt](openrndr-fontdriver-harfbuzzjs/IBMPlexMono-LICENSE.txt)), is
embedded, `loadFont(url, size)` loads other fonts.

harfbuzzjs imports Node's `module` built-in in code that only runs in Node.js. Applications built with webpack need a
fallback for it, in `webpack.config.d/harfbuzzjs.js`:

```js
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, { module: false });
```

## Images

Browsers decode images asynchronously: PNG, JPEG, GIF, WebP and BMP images are loaded with `loadImageSuspend(url)` in a
program, `loadImage(url)` only loads DDS images. `ColorBuffer`s can be encoded as PNG or JPEG data urls.
