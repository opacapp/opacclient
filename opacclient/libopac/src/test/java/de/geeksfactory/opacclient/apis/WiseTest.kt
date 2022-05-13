package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.objects.Account
import de.geeksfactory.opacclient.objects.DetailedItem
import de.geeksfactory.opacclient.objects.Library
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.searchfields.TextSearchField
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito
import java.util.*

@RunWith(Parameterized::class)
class WiseTest(private val file: String) : BaseHtmlTest() {
    val wise =  Mockito.spy(Wise::class.java)

    init {
        wise.init(Library().apply {
            data = JSONObject().apply {
                put("baseurl", "https://catalogus.biblionetdrenthe.nl")
                put("imgurl", "/cgi-bin/momredir.pl?")
                put("api-key-id", "4251d316-b964-44cc-b5fc-2f888d3716a8")
                put("api-key", "218f81af-dcab-4cca-97f3-a4cffe19a0c9")
                put("app-name", "WiseCatPlus")
                put("search-locations", JSONArray("[{\"0\" :  \"in jouw bibliotheek\"}, {\"2\" :  \"in de provincie\"}]"))
                put("languages", listOf("nl","de"))
            }
            libraryId = 9585
        }, HttpClientFactory("test"))
    }

    @Test
    fun testResult() {
        val json = JSONObject(readResource("/wise/result/$file"))

        val objects = json.getJSONArray("objects")

        assertTrue(objects.length() > 0)

    }


    companion object {

        private val FILES = arrayOf("assen.json")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun files(): Collection<Array<String>> {
            val files = ArrayList<Array<String>>()
            for (file in FILES) {
                files.add(arrayOf(file))
            }
            return files
        }
    }}
