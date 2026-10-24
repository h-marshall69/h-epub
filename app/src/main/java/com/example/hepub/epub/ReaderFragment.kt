package com.example.hepub.epub

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.publication.Publication
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.hepub.data.local.AppDatabase
import android.util.Log

class ReaderFragment : Fragment() {
    interface Host {
        fun onNavigatorReady(navigator: EpubNavigatorFragment)
        fun onLocator(locator: org.readium.r2.shared.publication.Locator)
    }
    companion object {
        var publication: Publication? = null
        var bookId: String? = null
        var initialLocator: org.readium.r2.shared.publication.Locator? = null
        fun create(publication: Publication, initial: org.readium.r2.shared.publication.Locator?) =
            ReaderFragment().also { this.publication = publication }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val publication = Companion.publication
        if (publication == null) {
            childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            return
        }
        val factory = EpubNavigatorFactory(publication, EpubNavigatorFactory.Configuration())
        Log.i("HEPUB-Readium", "navigator publication readingOrder=${publication.readingOrder.size}, resources=${publication.resources.size}, toc=${publication.tableOfContents.size}")
        // Readium expects the resource order here. The table of contents is UI metadata,
        // not the navigator's reading sequence.
        childFragmentManager.fragmentFactory = factory.createFragmentFactory(Companion.initialLocator, publication.readingOrder, listener = null)
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, state: Bundle?): View {
        return android.widget.FrameLayout(requireContext()).apply { id = android.view.View.generateViewId() }
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        if (state == null) childFragmentManager.beginTransaction().replace(view.id, EpubNavigatorFragment::class.java, Bundle(), "navigator").commitNow()
        val navigator = childFragmentManager.findFragmentByTag("navigator") as? EpubNavigatorFragment
        navigator?.let { (activity as? Host)?.onNavigatorReady(it) }
        viewLifecycleOwner.lifecycleScope.launch {
            navigator?.currentLocator?.let { flow -> flow.debounce(500).distinctUntilChanged().collectLatest { locator ->
                Log.d("HEPUB-Readium", "currentLocator href=${locator.href} progression=${locator.locations.totalProgression} position=${locator.locations.position}")
                (activity as? Host)?.onLocator(locator)
                bookId?.let { id ->
                    val json = locator.toJSON().toString()
                    withContext(Dispatchers.IO) { AppDatabase.get(requireContext()).bookDao().saveLocator(id, json) }
                }
            } }
        }
    }
}
