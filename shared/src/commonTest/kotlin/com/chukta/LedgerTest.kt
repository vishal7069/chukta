package com.chukta
import kotlin.test.*
class LedgerTest {
    @Test fun exactSplit() {
        assertEquals(listOf(33334L,33333L,33333L),equalSplit(100000,listOf("a","b","c")).map{it.amount})
        assertEquals(124005,parseAmount("1240.05"))
        assertNull(parseAmount("-5"));assertNull(parseAmount("4.555"))
    }
    @Test fun figmaBalances() {
        val members=listOf("aman","riya","you","neha").map{Member(it,it,it)}
        val expenses=listOf("aman" to 1200000L,"riya" to 600000L,"you" to 200000L).map{(p,a)->Expense(p,p,a,p,"Stay","2026-10-10",equalSplit(a,members.map{it.id}))}
        val trip=Trip("goa","you","Goa trip",members=members,expenses=expenses)
        assertEquals(listOf(700000L,100000L,-300000L,-500000L),trip.balances().map{it.net})
        assertEquals(listOf(Transfer("neha","aman",500000),Transfer("you","aman",200000),Transfer("you","riya",100000)),trip.transfers())
        assertEquals(0L,trip.balances().sumOf{it.net})
    }
    @Test fun kittyAndWeights() {
        assertEquals(listOf(57143L,42857L),weightedSplit(100000,mapOf("a" to 4,"b" to 3)).map{it.amount})
        val t=Trip("trip","a","Spiti",kitty=true,members=listOf(Member("a","a","A"),Member("b","b","B"),Member("kitty-trip","kitty","Kitty")),contributions=listOf(Contribution("c","a",100000)),expenses=listOf(Expense("e","Lunch",60000,"kitty-trip","Food","2026-10-14",equalSplit(60000,listOf("a","b")))))
        assertEquals(listOf(70000L,-30000L,-40000L),t.balances().map{it.net})
        assertEquals(0L,t.balances().sumOf{it.net})
    }
}
