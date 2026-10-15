package com.chukta

import kotlinx.serialization.Serializable

@Serializable data class User(val id: String, val phone: String, val name: String = "", val upi: String = "")
@Serializable data class Auth(val token: String, val user: User)
@Serializable data class Otp(val resendAfter: Int = 60, val devCode: String? = null)
@Serializable data class Member(val id: String, val userId: String = "", val name: String, val upi: String = "")
@Serializable data class Share(val memberId: String, val amount: Long)
@Serializable data class Expense(val id: String, val title: String, val amount: Long, val paidBy: String, val category: String, val date: String, val shares: List<Share> = emptyList(), val addedBy: String = "", val weights: Map<String,Int> = emptyMap(), val receipt: String = "")
@Serializable data class Settlement(val id: String, val from: String, val to: String, val amount: Long, val method: String, val reference: String = "", val date: String = "", val verification: String = "user_recorded")
@Serializable data class JoinRequest(val userId: String, val name: String, val memberId: String)
@Serializable data class JoinResult(val trip: Trip? = null, val pending: Boolean = false)
@Serializable data class Contribution(val id: String, val memberId: String, val amount: Long, val date: String = "")
@Serializable data class Trip(val id: String, val ownerId: String, val name: String, val icon: String = "🏖️", val dates: String = "", val kitty: Boolean = false, val closed: Boolean = false, val invite: String = "", val members: List<Member> = emptyList(), val expenses: List<Expense> = emptyList(), val settlements: List<Settlement> = emptyList(), val joinRequests: List<JoinRequest> = emptyList(), val contributions: List<Contribution> = emptyList())
@Serializable data class CreateTrip(val name: String, val icon: String, val dates: String, val members: List<String>, val kitty: Boolean)
@Serializable data class AddExpense(val id: String, val title: String, val amount: Long, val paidBy: String, val category: String, val date: String, val weights: Map<String,Int> = emptyMap(), val receipt: String = "")
@Serializable data class AddSettlement(val id: String, val from: String, val to: String, val amount: Long, val method: String, val reference: String)
@Serializable data class QueuedExpense(val tripId: String, val body: AddExpense)
@Serializable data class Snapshot(val user: User? = null, val trips: List<Trip> = emptyList(), val queue: List<QueuedExpense> = emptyList())
@Serializable data class PreviewPerson(val id: String, val name: String, val balance: Long)
@Serializable data class InvitePreview(val id: String, val name: String, val icon: String, val members: Int, val total: Long, val people: List<PreviewPerson> = emptyList())
data class Transfer(val from: String, val to: String, val amount: Long)
data class Balance(val member: Member, val paid: Long, val share: Long, val net: Long)
fun Trip.ownId(user: User?): String = members.find { it.userId == user?.id }?.id ?: ""
val Trip.people: List<Member> get() = members.filter{it.userId!="kitty"}

fun equalSplit(amount: Long, members: List<String>): List<Share> {
    require(amount > 0 && members.isNotEmpty() && members.distinct().size == members.size)
    return members.mapIndexed { index, member -> Share(member, amount / members.size + if (index < amount % members.size) 1 else 0) }
}
fun Trip.balances(): List<Balance> = members.map { m ->
    val contributionsPaid = if(m.userId=="kitty") -contributions.sumOf{it.amount} else contributions.filter{it.memberId==m.id}.sumOf{it.amount}
    val paid = expenses.filter { it.paidBy == m.id }.sumOf { it.amount } + contributionsPaid
    val share = expenses.flatMap { it.shares }.filter { it.memberId == m.id }.sumOf { it.amount }
    val sent = settlements.filter { it.from == m.id }.sumOf { it.amount }
    val received = settlements.filter { it.to == m.id }.sumOf { it.amount }
    Balance(m, paid, share, paid - share + sent - received)
}
fun weightedSplit(amount: Long, weights: Map<String,Int>): List<Share> {
    require(amount>0 && weights.isNotEmpty() && weights.values.all{it in 0..1000})
    val total=weights.values.sum();require(total>0)
    val result=weights.map{(id,w)->Share(id,amount*w/total)}.toMutableList()
    var left=amount-result.sumOf{it.amount}
    weights.entries.withIndex().sortedWith(compareByDescending<IndexedValue<Map.Entry<String,Int>>>{amount*it.value.value%total}.thenBy{it.index}).forEach {
        if(left-->0)result[it.index]=result[it.index].copy(amount=result[it.index].amount+1)
    }
    return result.filter{it.amount>0}
}
fun Trip.transfers(): List<Transfer> {
    val rows = balances()
    val debts = rows.filter { it.net < 0 }.sortedBy { it.net }.map { it.member.id to -it.net }.toMutableList()
    val credits = rows.filter { it.net > 0 }.sortedByDescending { it.net }.map { it.member.id to it.net }.toMutableList()
    val result = mutableListOf<Transfer>()
    for (d in debts.indices) for (c in credits.indices) {
        val amount = minOf(debts[d].second, credits[c].second)
        if (amount > 0) {
            result += Transfer(debts[d].first, credits[c].first, amount)
            debts[d] = debts[d].first to debts[d].second - amount
            credits[c] = credits[c].first to credits[c].second - amount
        }
    }
    return result
}
fun money(paise: Long): String {
    val absolute = if (paise < 0) -paise else paise
    val digits = (absolute / 100).toString()
    val formatted = digits.reversed().chunked(3).joinToString(",").reversed()
    val fraction = if (absolute % 100 == 0L) "" else "." + (absolute % 100).toString().padStart(2, '0')
    return (if (paise < 0) "−" else "") + "₹" + formatted + fraction
}
fun parseAmount(value: String): Long? {
    if (!Regex("^\\d{1,8}(\\.\\d{0,2})?$").matches(value)) return null
    val parts = value.split(".")
    return parts[0].toLong() * 100 + (parts.getOrElse(1) { "" }.padEnd(2, '0').toLongOrNull() ?: 0)
}
