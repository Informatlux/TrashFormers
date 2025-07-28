package com.informatlux.test

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class to hold the structured information for each suggestion
data class PlaceSuggestion(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double
)

// The NominatimService interface has been REMOVED from this file.

class PlacesAutoCompleteAdapter(context: Context) :
    ArrayAdapter<PlaceSuggestion>(context, 0), Filterable {

    private var suggestions = mutableListOf<PlaceSuggestion>()

    override fun getCount(): Int = suggestions.size
    override fun getItem(position: Int): PlaceSuggestion? = suggestions.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_autocomplete_suggestion, parent, false)

        val titleTextView = view.findViewById<TextView>(R.id.title)
        val subtitleTextView = view.findViewById<TextView>(R.id.subtitle)

        getItem(position)?.let {
            titleTextView.text = it.title
            subtitleTextView.text = it.subtitle
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint != null && constraint.length > 2) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // This line now correctly calls the service in its own separate file.
                            val apiResults = NominatimService.api.searchLocation(constraint.toString())
                            val newSuggestions = apiResults.mapNotNull {
                                val parts = it.display_name.split(", ")
                                if (parts.isNotEmpty()) {
                                    PlaceSuggestion(
                                        title = parts[0],
                                        subtitle = parts.drop(1).joinToString(", "),
                                        lat = it.lat.toDouble(),
                                        lon = it.lon.toDouble()
                                    )
                                } else {
                                    null
                                }
                            }

                            withContext(Dispatchers.Main) {
                                suggestions.clear()
                                suggestions.addAll(newSuggestions)
                                notifyDataSetChanged()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                results.values = suggestions
                results.count = suggestions.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                // Handled in the coroutine
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return (resultValue as? PlaceSuggestion)?.title ?: ""
            }
        }
    }
}