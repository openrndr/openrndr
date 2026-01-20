import web.events.AddEventListenerOptions
import web.events.EventType
import web.events.addEventListener
import web.html.Image
import kotlin.js.Promise

internal fun promiseImage(url: String): Promise<Image> {
    return Promise { resolve, _ ->
        val image = Image()
        val options: AddEventListenerOptions = js("{}")
        options.capture = false

        image.addEventListener(EventType("load"), {
            resolve(image)
        }, options)
        image.src = url
    }
}
