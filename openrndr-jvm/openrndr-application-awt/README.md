# openrndr-application-awt

An application back-end that draws OPENRNDR programs in AWT components, to embed them in Swing, AWT or Compose Desktop
user interfaces.

* `ProgramPanel` / `programPanel { }` is a Swing panel that shows a program. Put it in a Swing layout, or in Compose with
  `SwingPanel(factory = { programPanel { ... } })`.
* With `-Dorg.openrndr.application=AWT`, or as the only back-end on the class path, `application { }` shows its program
  in an AWT frame.

All programs are set up, drawn and receive their events on one render thread, `openrndr-awt-render`, not on AWT's event
thread. Share values between the user interface and a program through thread-safe state, for example `@Volatile`
properties, and change Swing or Compose state from a program with `SwingUtilities.invokeLater`.

## Rendering

* On macOS with OpenGL ES (the default on Apple Silicon) programs are drawn through ANGLE: the canvas gets a Core
  Animation layer through JAWT, ANGLE draws in a Metal layer that it adds to it. All ANGLE canvases share one context.
* Otherwise programs are drawn with OpenGL through [lwjgl3-awt](https://github.com/LWJGLX/lwjgl3-awt)'s `AWTGLCanvas`,
  or with OpenGL ES on Windows and Linux. lwjgl3-awt does not create OpenGL ES contexts on macOS.

## Not supported

Child windows, fullscreen windows, moving the cursor, window hit tests, manual presentation, and resource and draw
threads.

## Demos

* `DemoApplicationAWT01`: `application { }` on the AWT back-end
* `DemoSwing01`: two programs in a Swing split pane with Swing controls
* `DemoCompose01` in `openrndr-application-awt-compose-demo`: a program in a Compose Desktop window next to Compose
  controls
