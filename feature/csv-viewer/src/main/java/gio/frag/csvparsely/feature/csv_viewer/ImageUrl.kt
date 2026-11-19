package gio.frag.csvparsely.feature.csv_viewer

import android.net.Uri

private val IMAGE_SCHEMES = setOf("http", "https")
private val IMAGE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".gif", ".webp")

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * Whether [text] is an `http` or `https` URL whose path ends in an image extension, ignoring case. Only the path is
 * checked, so `https://example.com/photo.png?size=2` is an image and `https://example.com/report.pdf` is not.
 */
internal fun isImageUrl(text: String): Boolean {
    // Uri.parse never throws; text that isn't a URL just has no scheme or path
    val uri = Uri.parse(text)
    val path = uri.path ?: return false
    return uri.scheme?.lowercase() in IMAGE_SCHEMES &&
        IMAGE_EXTENSIONS.any { extension -> path.endsWith(extension, ignoreCase = true) }
}
