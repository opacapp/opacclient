package de.geeksfactory.opacclient.apis

import de.geeksfactory.opacclient.networking.HttpClientFactory
import de.geeksfactory.opacclient.networking.NotReachableException
import de.geeksfactory.opacclient.objects.*
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField
import de.geeksfactory.opacclient.searchfields.DropdownSearchField
import de.geeksfactory.opacclient.searchfields.SearchField
import de.geeksfactory.opacclient.searchfields.SearchQuery
import de.geeksfactory.opacclient.utils.jsonObject
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.floor

data class Authentication(val libraryId: String, val patronSystemId: String,val token: String, val membershipExpiryDate: org.joda.time.LocalDate )

lateinit var apiKeyId: String
lateinit var apiKey: String
lateinit var applicationName: String
var auth: Authentication? =  null

/**
 * OpacApi implementation for Wise owned by OCLC https://www.oclc.org/en/wise.html
 *
 *
 * @author Henk Klijn Hesselink, April 2021
 */


class WiseKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
                .header("WISE_KEY", genWisekey())
        if ( auth != null) requestBuilder.addHeader("Authorization", auth!!.token)
        val request = requestBuilder.build()
        return chain.proceed(request)
    }

    fun genWisekey(): String {
        val epochDay: String  = floor(Date().time / 8.64e7).toLong().toString()
        val data = epochDay + applicationName

        val mac = Mac.getInstance("HmacSHA256")
        val secret_key = SecretKeySpec(apiKey.toByteArray(charset("UTF-8")), "HmacSHA256")
        mac.init(secret_key)
        val signature= bytesToHex(mac.doFinal(data.toByteArray(charset("UTF-8"))))
        return "$apiKeyId:$signature"
    }
    private val hexArray = "0123456789abcdef".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            var v = (bytes[j] and 0xFF.toByte()).toInt()
            if ( v < 0) v = 256+v

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}

open class Wise : OkHttpBaseApi() {
    protected lateinit var baseurl: String
    protected lateinit var imgurl: String
    protected var searchLocations: List<Pair<String,String>> = listOf(Pair("0","Own branch"))
    protected var homeBranch: Long? = -1
    protected lateinit var libraryData: JSONObject
    protected lateinit var queryString: String
    protected lateinit var scope: String
    protected var mediaTypeQS: String = ""
    protected var vcgrpt: String = ""
    protected lateinit var holdParameters: String
    protected var pickupBranch: Long? = -1

    protected val ENCODING = "UTF-8"

    private val mediatypes = mapOf(
//            null to SearchResult.MediaType.UNKNOWN
            "CD" to SearchResult.MediaType.CD,
            "DVD" to SearchResult.MediaType.DVD,
            "DVR" to SearchResult.MediaType.GAME_CONSOLE,
            "CSP" to SearchResult.MediaType.GAME_CONSOLE,
            "GBK" to SearchResult.MediaType.AUDIOBOOK,
            "BOE" to SearchResult.MediaType.BOOK
    )

    private val mediatypes2 = mapOf(
//            null to SearchResult.MediaType.UNKNOWN
            "CD" to SearchResult.MediaType.CD,
            "DVD" to SearchResult.MediaType.DVD,
            "DVR" to SearchResult.MediaType.GAME_CONSOLE,
            "CSP" to SearchResult.MediaType.GAME_CONSOLE,
            "GBK" to SearchResult.MediaType.AUDIOBOOK,
            "BOE" to SearchResult.MediaType.BOOK
    )

    private val itemstatuses = mapOf(
            "available" to "Aanwezig",
            "hold_allowed" to "Reserveren mogelijk",
            "reference" to "Alleen inkijken, niet lenen",
            "on_hold" to "Gereserveerd",
            "on_loan" to "Uitgeleend",
            "on_order" to "Is besteld",
            "not_available" to "Niet beschikbaar",
            "unknown" to "Niet beschikbaar"
    )

    private val holdstatuses = mapOf(
            "ACTIVE" to "In behandeling",
            "CLOSED" to "Afgehandeld",
            "IN_TRANSPORT" to "In behandeling",
            "LOANED" to "Uitgeleend",
            "MESSAGE_SENT" to "Staat voor u klaar",
            "RECIEVED" to "Staat klaar",
            "RECEIVED" to "Staat klaar",
            "RETURNED_TO_BRANCH" to "Terug bij vestiging",
            "UNKNOWN" to "Onbekend"
    )

    override fun init(library: Library, factory: HttpClientFactory) {
        http_interceptor = WiseKeyInterceptor()
        super.init(library, factory)
        libraryData = library.data
        homeBranch = library.libraryId
        baseurl = libraryData.getString("baseurl")
        imgurl = libraryData.getString("imgurl")
        val cats = libraryData.optJSONArray("search-locations")
        if ( cats != null)  {
            val catsList =  List(cats.length(),cats::getJSONObject)
            searchLocations = emptyList()
            catsList.forEach {
                json -> val key = json.names()[0];  searchLocations = searchLocations + Pair(key as String, json.get(key) as String)
            }
        }
        if ( libraryData.has("api-key-id") ) {
            apiKeyId = libraryData.getString("api-key-id")
            apiKey = libraryData.getString("api-key")
            applicationName = libraryData.getString("app-name")
        }
    }


    override fun prolongAll(account: Account?, useraction: Int, selection: String?): OpacApi.ProlongAllResult {
        TODO("Not yet implemented")
    }


    protected fun parseSearchResult(fields: JSONObject):
            SearchResult {
        return SearchResult().apply {
            val title = fields.getJSONObject("titel").getJSONObject("content").get("value")
//            val year: Any? = fields.getJSONObject("pubjaar")?.getJSONObject("content")?.get("value")
            val year = getFieldValue(fields.optJSONObject("pubjaar"), "label")

            val author = getFieldValue(fields.optJSONObject("auteur"))
            val mediumSrt = getFieldValue(fields.getJSONObject("medium_srt"))

            innerhtml = "<b>$title</b><br>${author ?: ""}<br>${year ?: ""}"
            id = fields.getJSONObject("id").getJSONObject("content").get("value").toString()
            cover = getImgUrl(getFieldValue(fields.optJSONObject("momkeys")))
            type = mediatypes[mediumSrt]

        }
    }


    private fun getFieldValue(fieldObject: JSONObject?, key: String = "value", flatten: Boolean = false): String? {
        if (fieldObject == null) return null

        val contentObject = fieldObject.optJSONObject("content")
        if (contentObject != null) {
            return contentObject.getString(key)
        }
        val arr = fieldObject.optJSONArray("content")
        if (arr != null && arr.length() > 0) {
            if ( flatten) {
                return  List(arr.length(), arr::getJSONObject).map{ o -> o.getString(key) }.reduce{ u,v -> "$u, $v"}
            }
            return arr.getJSONObject(0).getString(key)
        }
        return null
    }

    private fun getImgUrl(momkeys: String?, size: Int = 80): String? {
        return "$baseurl${imgurl}size=${size}&$momkeys"
    }

    override fun search(query: List<SearchQuery>): SearchRequestResult {
        mediaTypeQS= ""
        vcgrpt = "0"
        query.forEach { q -> when(q.searchField.id){
            "materiaal"   -> mediaTypeQS= "&wf_medium_srt=${q.value}"
            "search"      -> queryString = q.value.split(" ").reduce { u, v -> u + "%20" + v }
            "searchScope" -> scope = q.value
            "location"    -> vcgrpt = q.value
        } }
        return searchGetPage(1)
    }

    override fun searchGetPage(pagina: Int): SearchRequestResult {
        val AMOUNT = 10
        val page = pagina -1
        val response = httpPost(
                "$baseurl/cgi-bin/bx.pl",
                RequestBody.create(
                        MediaType.parse("application/x-www-form-urlencoded"),
                        "prt=INTERNET&var=portal&vestnr=$homeBranch&fmt=json&search_in=$scope&amount=$AMOUNT&catalog=default&event=osearch" +
                                "&preset=all&offset=${page*AMOUNT}$mediaTypeQS&qs=$queryString&vcgrpf=0&backend=wise&vcgrpt=$vcgrpt"),
                ENCODING, false)

        val json = JSONObject(response)
        val objects = List(json.getJSONArray("objects").length(), json.getJSONArray("objects")::getJSONObject)
        val searchResultList = objects.map { o -> o.getJSONObject("fields") }.filter { fields -> getFieldValue(fields.getJSONObject("medium_srt")) != "Website" } .map { fields -> parseSearchResult(fields) }

        with(json.getJSONObject("paging")) {
            return SearchRequestResult(
                    searchResultList,
                    getInt("total"),
                    getInt("total") / getInt("perpage"),
                    getInt("offset") / getInt("perpage")
            )
        }
    }


    override fun cancel(media: String?, account: Account?, useraction: Int, selection: String?): OpacApi.CancelResult {
        val itemId = media!!.split(":")[2]

        httpDelete("$baseurl/restapi/patron/${auth!!.patronSystemId}/library/${auth!!.libraryId}/hold/$itemId")
        return OpacApi.CancelResult(OpacApi.MultiStepResult.Status.OK)
    }

    override fun getShareUrl(id: String?, title: String?): String {
        val titleId = id!!.split(":")[0]
        return "$baseurl/wise-apps/catalog/$homeBranch/detail/wise/$titleId"
    }

    override fun filterResults(filter: Filter?, option: Filter.Option?): SearchRequestResult {
        TODO("Not yet implemented")
    }

    override fun getSupportedLanguages(): MutableSet<String> {
        val langs: MutableSet<String> = HashSet()
        if (libraryData.has("languages")) {
            try {
                for (i in 0 until libraryData.getJSONArray("languages").length()) {
                    langs.add(libraryData.getJSONArray("languages").getString(i))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return langs
    }

    override fun parseSearchFields(): List<SearchField> {
        if (!initialised) start()

        val freeSearchField = BarcodeSearchField("search","Suchen", false,false,"")
        val ietsTitelOrAuteur = DropdownSearchField().apply {
            id = "searchScope"
            displayName = "Zoeken op"
            dropdownValues = listOf(
                    DropdownSearchField.Option("iets", "Alles"),
                    DropdownSearchField.Option("titel", "Titel"),
                    DropdownSearchField.Option("auteur", "Auteur"))
            isAdvanced = false
        }


        val materiaal = DropdownSearchField().apply {
            id = "materiaal"
            displayName = "Materiaal"
            dropdownValues = listOf(
                    DropdownSearchField.Option("", "Alles"),
                    DropdownSearchField.Option("BOE", "Boek"),
                    DropdownSearchField.Option("GBK", "Luisterboek"),
                    DropdownSearchField.Option("CD", "CD"),
                    DropdownSearchField.Option("DVD", "DVD")
            )
            isAdvanced = false
        }
        if ( searchLocations.size > 1 ) {
            val locations = DropdownSearchField().apply {
                id = "location"
                displayName = "Locatie"
                dropdownValues = searchLocations.map { (first, last) -> DropdownSearchField.Option(first, last)  }
                isAdvanced = false
            }
            return listOf(freeSearchField, ietsTitelOrAuteur, locations, materiaal)
        }
        return listOf(freeSearchField, ietsTitelOrAuteur, materiaal)

    }


    override fun getSupportFlags(): Int {
        return OpacApi.SUPPORT_FLAG_ENDLESS_SCROLLING or OpacApi
                .SUPPORT_FLAG_WARN_RESERVATION_FEES or OpacApi.SUPPORT_FLAG_ACCOUNT_PROLONG_ALL
    }

    override fun getResultById(id: String?, homebranch: String?): DetailedItem {
        val titleId = id!!.split(":")[0]
        val response = httpGet(
                "$baseurl/cgi-bin/bx.pl?event=odetail&fmt=json&oid=$titleId&partials=about&pub=0&vestnr=$homeBranch",
                ENCODING, false)

        val fields = JSONObject(response).getJSONObject("fields")


        val omschr = Detail( "",getFieldValue(fields.optJSONObject("tt_info")))
//        val copys = getCopies(id,homebranch)
        val copys: List<Copy> = searchLocations.map { branchCatGroup -> getCopies(id,homebranch, branchCatGroup.first)}.fold(emptyList<Copy>()){
            u, v -> listOf(u,v).flatten()
        }
        val firstItem = if (copys.isEmpty()) null else copys.filter { copy -> copy.status == itemstatuses["available"] }.firstOrNull()
        val di = DetailedItem().apply {
            title = getFieldValue(fields.getJSONObject("titel"))
            cover = getImgUrl(getFieldValue(fields.optJSONObject("momkeys")),260)
            val mediumSrt = getFieldValue(fields.getJSONObject("medium_srt"))
            mediaType = mediatypes[mediumSrt]
            copies = copys
            isReservable = firstItem != null
            reservation_info = if (isReservable) firstItem!!.barcode else null

        }
        di.addDetail(omschr)
        di.addDetail( Detail("Auteur", getFieldValue(fields.optJSONObject("auteur")), true ))
        val medewerker = fields.optJSONObject("medewerker")
        if ( medewerker != null ) di.addDetail( Detail("Medewerker", getFieldValue( fieldObject = fields.optJSONObject("medewerker"), flatten = true), true ))
        val isbn = fields.optJSONObject("isbn")
        if ( isbn != null ) di.addDetail( Detail("ISBN", getFieldValue(fields.optJSONObject("isbn")), true ))
        val impressum = fields.optJSONObject("impressum")
        if ( impressum != null ) di.addDetail( Detail("Uitgever", getFieldValue(fields.optJSONObject("impressum")), true ))
        val ageCategory = fields.optJSONObject("leeftijd categorie")
        if ( ageCategory != null ) di.addDetail( Detail("Leeftijd", getFieldValue(fields.optJSONObject("leeftijd categorie")), true ))
        di.addDetail( Detail("Aanschafinfo", getFieldValue(fields.optJSONObject("aanschafinfo")), true ))
        return di
    }

    fun getCopies(titleIdAndPossibleItemId: String?, homebranch: String?, branchCatGroup: String): List<Copy> {

        val titleId :String = titleIdAndPossibleItemId!!.split(":")[0]

        val url = "$baseurl/restapi/title/$titleId/iteminformation?branchCatGroups=$branchCatGroup&branchId=$homeBranch&clientType=I"
        val itemArray: String  = try { httpGet(
                url,
                ENCODING, false)
        } catch (e: NotReachableException) {
            "[]"
        }

        val itemsInformation =  JSONArray(itemArray)
        val itemInformationList = List(itemsInformation.length(), itemsInformation::getJSONObject)
        if (itemInformationList.isEmpty()) return emptyList()

        val copyList: List<Copy> = itemInformationList.map{ item -> Copy().apply{
            branch = item.getString("branchName")
            location = item.optString("subLocation")
            barcode = item.optString("id")
        }}
        val items = copyList.map{ item -> item.barcode}.reduce { u,v -> "$u,$v"  }
        val respAvailable = httpGet(
                "$baseurl/restapi/item/$items/availability",
                ENCODING, false)
        val arrayAvail =  JSONArray(respAvailable)
        val copyAvailList = List(arrayAvail.length(), arrayAvail::getJSONObject)

        for( copyAvail in copyAvailList) {
            for(copy in copyList) {
                if ( copy.barcode.equals(copyAvail["itemId"].toString())) {
                    val availStatus = (copyAvail["availabilityStatus"] as String).toLowerCase(Locale.getDefault())
                    copy.status = itemstatuses[availStatus] ?: itemstatuses["unknown"]
                }
            }
        }
        return copyList
    }

    override fun reservation(item: DetailedItem, account: Account, useraction: Int, selection: String?): OpacApi.ReservationResult {
        auth = login(account)

        if (useraction == 0 && selection == null) {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.SELECTION_NEEDED).apply {
                actionIdentifier = OpacApi.ReservationResult.ACTION_USER
                message = "Ophalen bij thuis vestiging?"
                this.selection = listOf(hashMapOf("key" to "Y","value" to "Ja"), hashMapOf("key" to "N","value" to "Andere vestiging"))
            }
        } else if (useraction == OpacApi.ReservationResult.ACTION_USER) {
            val prepHold =  httpGet("$baseurl/restapi/patron/${auth!!.patronSystemId}/library/${auth!!.libraryId}/preparehold/${item.id}/branch/$homeBranch", ENCODING).jsonObject
            if ( selection == "N") {
                // Select alternative branch
                val pickupLocations = prepHold.getJSONArray("pickupLocations")
                val plList = List( pickupLocations.length(), pickupLocations::getJSONObject)

                val plListOfMaps = plList.map {
                    pl -> hashMapOf( "key" to "${item.id}:${pl.getString("branchId")}:${pl.getString("description")}",
                        "value" to  pl.getString("description") )
                }
                return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.SELECTION_NEEDED).apply {
                    actionIdentifier = OpacApi.ReservationResult.ACTION_BRANCH
                    this.selection = plListOfMaps.sortedBy { a -> a.get("value")}
                    message = "Alternatieve ophaallocatie"
                }
            }
            val costs = prepHold.getDouble("reservationCost")
            val queue = prepHold.optString("queuePosition")
            holdParameters = prepHold.optString("holdParameters")
            pickupBranch = homeBranch

            if ( costs < 0.000001 && queue == "") {
                // Do reservation own branch free at costs
                val jsonData = RequestBody.create(MEDIA_TYPE_JSON,"{\"bibliographicRecordId\": \"${item.id}\", \"pickupLocationBranchId\":\"$homeBranch\", \"holdParameters\":\"$holdParameters\" }")
                httpPost("$baseurl/restapi/patron/${auth!!.patronSystemId}/library/${auth!!.libraryId}/hold",jsonData, ENCODING).jsonObject

                return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.OK)
            }
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.CONFIRMATION_NEEDED).apply {
                actionIdentifier = OpacApi.ReservationResult.ACTION_CONFIRMATION
                details = listOf( arrayOf("Kosten", costs.toString()), arrayOf("Wachtrij positie", queue))
                message = "Reserverings kosten en/of wachtrij"
            }

        } else if (useraction == OpacApi.ReservationResult.ACTION_BRANCH) {
            pickupBranch = selection!!.split(":")[1].toLong()
            val prepHold =  httpGet("$baseurl/restapi/patron/${auth!!.patronSystemId}/library/${auth!!.libraryId}/preparehold/${item.id}/branch/$pickupBranch", ENCODING).jsonObject
            val costs = prepHold.getString("reservationCost")
            val queue = prepHold.optString("queuePosition")
            holdParameters = prepHold.optString("holdParameters")
            // Confirm Fee and queue
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.CONFIRMATION_NEEDED).apply {
                actionIdentifier = OpacApi.ReservationResult.ACTION_CONFIRMATION
                details = listOf(arrayOf("Ophaallocatie", selection.split(":")[2]), arrayOf("Kosten", costs.toString()), arrayOf("Wachtrij positie", queue))
                message = "Ophalen op andere locatie"
            }
        } else if (useraction == OpacApi.ReservationResult.ACTION_CONFIRMATION) {
            // Do reservation at pickup  branch with possible fees
            val jsonData = RequestBody.create(MEDIA_TYPE_JSON,"{\"bibliographicRecordId\": \"${item.id}\", \"pickupLocationBranchId\":\"$pickupBranch\", \"holdParameters\":\"$holdParameters\" }")
            httpPost("$baseurl/restapi/patron/${auth!!.patronSystemId}/library/${auth!!.libraryId}/hold",jsonData, ENCODING).jsonObject

            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.OK)
        } else {
            return OpacApi.ReservationResult(OpacApi.MultiStepResult.Status.ERROR)
        }
    }

    override fun setLanguage(language: String?) {

    }

    override fun prolong(media: String?, account: Account?, useraction: Int, selection: String?): OpacApi.ProlongResult {
        val itemId = media!!.split(":")[1]
        httpPost("$baseurl/restapi/patron/${auth!!.patronSystemId}/item/${itemId}/loanrenewal", RequestBody.create(MEDIA_TYPE_JSON, "{}"),ENCODING).jsonObject
        return OpacApi.ProlongResult(OpacApi.MultiStepResult.Status.OK)
    }

    override fun account(account: Account): AccountData {
        auth = login(account)

        val loans =  httpGet("$baseurl/restapi/patron/${auth!!.patronSystemId}/library/${auth!!.libraryId}/loan", ENCODING).jsonObject
        val holds =  httpGet("$baseurl/restapi/patron/${auth!!.patronSystemId}/library/${auth!!.libraryId}/hold", ENCODING).jsonObject
        val feeAndFines = httpGet("$baseurl/restapi/patron/${auth!!.patronSystemId}/fee", ENCODING).jsonObject.getJSONArray("items")
        val itemsList = List( feeAndFines.length(), feeAndFines::getJSONObject)
        val fee = itemsList.map { item -> item.getDouble("amount")/100 }.fold(0.0){ x,y -> x+y}

        return AccountData(account.id).apply {
            pendingFees = fee.toString()
            lent = parseLent(loans.getJSONArray("items"))
            reservations = parseHolds(holds.getJSONArray("items"))
            validUntil = auth!!.membershipExpiryDate.toString()
        }
    }

    private fun parseHolds(holds: JSONArray): List<ReservedItem> {
        val items = List(holds.length(), holds::getJSONObject)
        return items.map { item ->
            ReservedItem().apply {
                id = item.getString("bibliographicRecordId") + ":" + item.getString("holdNumber") + ":" + item.getString("id") // combine title id, copy id an hold id
                title = item.getString("title")
                author = item.getString("author")
                mediaType = mediatypes[item.getString("medium")]
                cover = getImgUrl(item.getString("momkeys").replace(';', '&'))
                branch = item.getString("pickupLocationName")
                status = holdstatuses[item.getString("holdStatus").toUpperCase(Locale.getDefault())] ?: holdstatuses["UNKNOWN"]
                if (item.getBoolean("cancelAllowed")) cancelData = id
                dbId = item.getLong("itemId")
                if (item.getBoolean("awaitingPickup")) readyDate = org.joda.time.LocalDate.parse(item.getString("holdDueDate"))
                if (false && item.getBoolean("updateAllowed"))  bookingData = id

            }
        }
    }

    internal fun parseLent(json: JSONArray): List<LentItem> {
        val items = List(json.length(), json::getJSONObject)
        return items.map { item -> LentItem().apply {
            id = item.getString("bibliographicRecordId") + ":" + item.getString("itemId") // combine title id and copy id
            title = item.getString("title")
            author = item.getString("author")
            mediaType = mediatypes2[item.getString("medium")]
            cover = getImgUrl(item.getString("momkeys").replace(';', '&'))
            lendingBranch = item.getString("branchId")
            // TODO cache branch names
            val response = httpGet("$baseurl/restapi/branch/$lendingBranch", ENCODING).jsonObject

            if (response.optString("name") != "") lendingBranch = response.get("name") as String
            val dueDate = org.joda.time.LocalDate.parse(item.getString("dueDate"))
            deadline = dueDate
            isRenewable = item.getBoolean("itemRenewable")
            val s = item.optString("newDueDate")
            if ( s != "") {
                val newDueDate = org.joda.time.LocalDate.parse(s)
                if ( newDueDate.isAfter(dueDate)) prolongData = id
            }
        } }
    }


    override fun checkAccountData(account: Account) {
        auth = login(account)
    }

    fun login(account: Account) : Authentication {
        val jsonData = RequestBody.create(MEDIA_TYPE_JSON,"{\"username\": \"${account.name}\", \"password\":\"${account.password}\" }")
        val authenicateCall = httpPost("$baseurl/restapi/patron/authentication",jsonData, ENCODING)

        val response = JSONObject(authenicateCall)
        return Authentication(
                token = response.getString("token"),
                libraryId = response.getString("libraryId"),
                patronSystemId = response.getString("patronSystemId"),
                membershipExpiryDate =  org.joda.time.LocalDate.parse(response.getString("membershipExpiryDate"))
        )
    }

    override fun getResult(position: Int): DetailedItem {
        TODO("Not yet implemented")
    }

    override fun shouldUseMeaningDetector(): Boolean {
        return false
    }

}
