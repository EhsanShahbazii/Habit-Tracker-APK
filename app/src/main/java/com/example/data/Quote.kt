package com.example.data

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class Quote(
    val id: Int,
    val quote: String,
    val author: String
)

object QuoteLoader {
    fun loadQuotes(context: Context): List<Quote> {
        return try {
            val jsonString = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, Quote::class.java)
            val adapter = moshi.adapter<List<Quote>>(type)
            adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
