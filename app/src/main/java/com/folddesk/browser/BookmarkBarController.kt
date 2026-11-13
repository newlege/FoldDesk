package com.folddesk.browser

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import com.folddesk.browser.databinding.ActivityMainBinding
import com.folddesk.browser.databinding.ItemBookmarkBinding
import java.net.HttpURLConnection
import java.net.URL

class BookmarkBarController(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val bookmarkStore: BookmarkStore,
    private val onOpenBookmark: (String) -> Unit,
    private val onBookmarksChanged: () -> Unit,
) {

    private val inflater = LayoutInflater.from(activity)

    init {
        binding.tabScroll.doOnLayout { applyBookmarkBarWidth() }
    }

    fun setStarClickListener(listener: () -> Unit) {
        binding.buttonBookmarkStar.setOnClickListener { listener() }
    }

    fun applyBookmarkBarWidth() {
        val tabWidth = binding.tabScroll.width
        if (tabWidth <= 0) return
        val target = (tabWidth * BOOKMARK_BAR_WIDTH_FRACTION).toInt()
        val params = binding.bookmarkScroll.layoutParams
        if (params.width != target) {
            params.width = target
            binding.bookmarkScroll.layoutParams = params
        }
    }

    fun refresh() {
        binding.bookmarkContainer.removeAllViews()
        val containerParams = binding.bookmarkContainer.layoutParams
        containerParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        binding.bookmarkContainer.layoutParams = containerParams

        bookmarkStore.all().forEach { entry ->
            val chip = ItemBookmarkBinding.inflate(inflater, binding.bookmarkContainer, false)
            chip.bookmarkTitle.text = entry.title
            ChromeTextWidths.applyBookmarkTitleWidth(chip.bookmarkTitle)
            val chipLp = chip.root.layoutParams as? LinearLayout.LayoutParams
                ?: LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            chipLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            chipLp.weight = 0f
            chip.root.layoutParams = chipLp
            loadFavicon(chip.bookmarkFavicon, entry.url)
            chip.root.setOnClickListener { onOpenBookmark(entry.url) }
            chip.root.setOnLongClickListener {
                bookmarkStore.remove(entry.url)
                Toast.makeText(activity, activity.getString(R.string.bookmark_removed), Toast.LENGTH_SHORT).show()
                refresh()
                onBookmarksChanged()
                true
            }
            binding.bookmarkContainer.addView(chip.root)
        }
        binding.bookmarkScroll.post { applyBookmarkBarWidth() }
    }

    fun updateStarState(isBookmarked: Boolean) {
        binding.buttonBookmarkStar.setImageResource(
            if (isBookmarked) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star_big_off
            },
        )
    }

    private fun loadFavicon(imageView: ImageView, pageUrl: String) {
        imageView.setImageResource(android.R.drawable.ic_menu_compass)
        val faviconUrl = BookmarkStore.faviconUrlFor(pageUrl)
        Thread {
            val bitmap = try {
                val conn = URL(faviconUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
            if (bitmap != null) {
                activity.runOnUiThread { imageView.setImageBitmap(bitmap) }
            }
        }.start()
    }

    companion object {
        const val BOOKMARK_BAR_WIDTH_FRACTION = 0.6f
    }
}
