package com.chukta

import androidx.compose.runtime.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

class Desk(val platform: Platform, val scope: CoroutineScope) {
    val api = Api(platform)
    private val cached = runCatching { platform.read("snapshot")?.let { ChuktaJson.decodeFromString<Snapshot>(it) } }.getOrNull()
    var user by mutableStateOf(cached?.user); private set
    var trips by mutableStateOf(cached?.trips ?: emptyList()); private set
    var queue by mutableStateOf(cached?.queue ?: emptyList()); private set
    var screen by mutableStateOf("splash")
    var selectedId by mutableStateOf("")
    var tab by mutableStateOf("Expenses")
    var expenseId by mutableStateOf("")
    var busy by mutableStateOf(false); private set
    var offline by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    var phone by mutableStateOf("")
    var devCode by mutableStateOf<String?>(null)
    var resend by mutableStateOf(0)
    var paying by mutableStateOf<Transfer?>(null)
    var receipt by mutableStateOf<Settlement?>(null)
    var deleted by mutableStateOf<AddExpense?>(null)
    val trip: Trip? get() = trips.find { it.id == selectedId }
    fun persist() = platform.write("snapshot", ChuktaJson.encodeToString(Snapshot(user, trips, queue)))
    fun update(t: Trip) { trips = trips.filter { it.id != t.id } + t; persist() }
    fun action(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: ApiError) {
                error = e.message
                if (e.status == 401) { api.token = null; platform.write("session", null); screen = "login" }
            }
            catch (_: Exception) { offline = true; error = "Can't reach Chukta. Check your connection and retry." }
            finally { busy = false }
        }
    }
    suspend fun refresh() {
        if (api.token == null) return
        user = api.get("/v1/me")
        // Keep idempotency keys when retrying. A response lost after saving cannot create duplicates.
        for (q in queue.toList()) {
            try {
                api.send<AddExpense, Trip>("/v1/trips/${q.tripId}/expenses", q.body)
                queue = queue.filter { it.body.id != q.body.id }; persist()
            } catch (e: ApiError) {
                error = "A saved expense needs attention: ${e.message}"; throw e
            }
        }
        trips = api.get("/v1/trips"); offline = false; persist()
    }
    fun sendOtp() = action {
        val result = api.send<Map<String,String>, Otp>("/v1/auth/otp", mapOf("phone" to "+91" + phone))
        devCode = result.devCode; resend = result.resendAfter; screen = "otp"
    }
    fun verify(code: String) = action {
        val auth = api.send<Map<String,String>, Auth>("/v1/auth/verify", mapOf("phone" to "+91" + phone, "code" to code))
        if(user!=null&&user!!.id!=auth.user.id) {
            if(queue.isNotEmpty())throw ApiError(409,"Sign in with the original account to sync offline expenses before switching accounts.")
            trips=emptyList();selectedId="";paying=null;receipt=null;deleted=null;platform.write("avatar",null)
        }
        api.token = auth.token; platform.write("session", auth.token); user = auth.user; persist()
        screen = "verified"; delay(700)
        if (auth.user.name.isEmpty()) screen = "profile" else { screen = "home"; refresh() }
    }
    fun profile(name: String, upi: String = user?.upi ?: "") = action {
        user = api.send<Map<String,String>, User>("/v1/me", mapOf("name" to name, "upi" to upi), HttpMethod.Patch)
        persist(); screen = "home"; refresh()
    }
    fun create(body: CreateTrip) = action { val t = api.send<CreateTrip, Trip>("/v1/trips", body); update(t); selectedId = t.id; tab = "Expenses"; screen = "trip" }
    fun addExpense(body: AddExpense) = action {
        val t = trip ?: return@action
        try { update(api.send<AddExpense,Trip>("/v1/trips/${t.id}/expenses", body)); offline = false }
        catch (e: CancellationException) { throw e }
        catch (e: ApiError) { throw e }
        catch (_: Exception) {
            queue = queue + QueuedExpense(t.id, body)
            val e = Expense(body.id, body.title, body.amount, body.paidBy, body.category, body.date, if(body.weights.isEmpty())equalSplit(body.amount, t.people.map { it.id }) else weightedSplit(body.amount,body.weights), user?.id ?: "",body.weights,body.receipt)
            update(t.copy(expenses = t.expenses + e)); offline = true; notice = "Saved on this phone. Tap Sync when you're online."
        }
        screen = "trip"; tab = "Expenses"
    }
    fun editExpense(body: AddExpense) = action {
        val t=trip ?: return@action
        update(api.send<AddExpense,Trip>("/v1/trips/${t.id}/expenses/${body.id}",body,HttpMethod.Patch))
        screen="trip";tab="Expenses"
    }
    fun removeExpense() = action {
        val t = trip ?: return@action
        val e=t.expenses.find{it.id==expenseId} ?: return@action
        if (queue.any { it.body.id == expenseId }) {
            queue = queue.filter { it.body.id != expenseId }; update(t.copy(expenses = t.expenses.filter { it.id != expenseId }))
        } else update(api.send<Map<String,String>,Trip>("/v1/trips/${t.id}/expenses/$expenseId", emptyMap(), HttpMethod.Delete))
        screen = "trip"
        deleted=AddExpense(e.id,e.title,e.amount,e.paidBy,e.category,e.date,e.weights,e.receipt)
    }
    fun settle(amount: Long, method: String, reference: String) = action {
        val t = trip ?: return@action; val p = paying ?: return@action
        val request = AddSettlement(platform.newId(), p.from, p.to, amount, method, reference)
        val updated = api.send<AddSettlement,Trip>("/v1/trips/${t.id}/settlements", request)
        update(updated); receipt = updated.settlements.find { it.id == request.id }; paying = null; screen = "paid"
    }
    fun closeTrip() = action { val t = trip ?: return@action; update(api.send<Map<String,String>,Trip>("/v1/trips/${t.id}/close", emptyMap())); screen = "home" }
    fun join(invite: String, memberId: String) = action {
        val result = api.send<Map<String,String>,JoinResult>("/v1/join", mapOf("invite" to invite, "memberId" to memberId))
        result.trip?.let { update(it); selectedId = it.id; screen = "trip" }
        if(result.pending) notice = "Request sent. Your trip owner can approve your name. Tap Sync after approval."
    }
    fun approve(userId: String) = action { val t=trip ?: return@action; update(api.send<Map<String,String>,Trip>("/v1/trips/${t.id}/approve",mapOf("userId" to userId))) }
    fun setMemberUpi(memberId: String, upi: String) = action { val t = trip ?: return@action; update(api.send<Map<String,String>,Trip>("/v1/trips/${t.id}/members", mapOf("memberId" to memberId, "upi" to upi))) }
    fun logout() = action {
        api.send<Map<String,String>,Map<String,Boolean>>("/v1/logout", emptyMap())
        api.token = null; user = null; trips = emptyList(); queue = emptyList()
        platform.write("session", null); platform.write("snapshot", null); screen = "login"
    }
    fun back() { error = null; paying = null; screen = when(screen) { "otp" -> "login"; "profile" -> "login"; "create", "trip", "settings" -> "home"; "expense", "detail", "paid" -> "trip"; else -> "login" } }
    fun openTrip(t: Trip) { selectedId = t.id; tab = "Expenses"; screen = "trip" }
    fun changeServer(url: String) {
        platform.write("api-url",url.trimEnd('/'));platform.write("session",null);platform.write("snapshot",null)
        api.token=null;user=null;trips=emptyList();queue=emptyList();offline=false;screen="login"
    }
    fun inviteText(t: Trip) = "Join ${t.name} on Chukta: ${platform.apiUrl.trimEnd('/')}/join/${t.invite}\nInvite code: ${t.invite}"
}
