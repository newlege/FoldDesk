package com.folddesk.browser

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
        binding.bookmarkScroll.doOnLayout { relayoutBookmarkChips() }
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
            binding.bookmarkScroll.post { relayoutBookmarkChips() }
        }
    }

    fun refresh() {
        binding.bookmarkContainer.removeAllViews()
        bookmarkStore.all().forEach { entry ->
            val chip = ItemBookmarkBinding.inflate(inflater, binding.bookmarkContainer, false)
            chip.bookmarkTitle.text = entry.title
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
        binding.bookmarkScroll.post {
            applyBookmarkBarWidth()
            relayoutBookmarkChips()
        }
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

    private fun relayoutBookmarkChips() {
        val container = binding.bookmarkContainer
        val scrollWidth = binding.bookmarkScroll.width
        if (scrollWidth <= 0 || container.childCount == 0) return

        val minChipPx = (48 * activity.resources.displayMetrics.density).toInt()
        val totalMin = minChipPx * container.childCount
        val fill = totalMin <= scrollWidth

        val containerParams = container.layoutParams
        containerParams.width = if (fill) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        container.layoutParams = containerParams

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val lp = child.layoutParams as LinearLayout.LayoutParams
            if (fill) {
                lp.width = 0
                lp.weight = 1f
            } else {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
            }
            child.layoutParams = lp
            val title = child.findViewById<TextView>(R.id.bookmarkTitle)
            title?.layoutParams = (title.layoutParams as LinearLayout.LayoutParams).apply {
                width = if (fill) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
                weight = if (fill) 1f else 0f
            }
        }
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
