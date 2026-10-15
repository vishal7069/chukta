@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.chukta

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import io.ktor.http.encodeURLParameter
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import coil3.compose.AsyncImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable fun ExpenseScreen(d: Desk) {
    val t=d.trip ?: return
    val existing=t.expenses.find{it.id==d.expenseId}
    var value by remember{mutableStateOf(existing?.let{(it.amount/100).toString()+"."+(it.amount%100).toString().padStart(2,'0')}?:"")}
    var title by remember{mutableStateOf(existing?.title?:"")}
    var category by remember{mutableStateOf(existing?.category?:"Food")}
    var payer by remember{mutableStateOf(existing?.paidBy?:t.ownId(d.user).ifEmpty{t.members.first().id})}
    var splitDialog by remember{mutableStateOf(false)}
    var weights by remember{mutableStateOf(existing?.weights?.takeIf{it.isNotEmpty()}?:t.people.associate{it.id to 1})}
    var date by remember{mutableStateOf(existing?.date?:d.platform.today())}
    var photo by remember{mutableStateOf(existing?.receipt?:"")}
    var picker by remember{mutableStateOf(false)};var dateDialog by remember{mutableStateOf(false)}
    val amount=parseAmount(value) ?: 0L
    Column(Modifier.fillMaxSize().background(Paper)) {
        Top(if(existing==null)"Add expense" else "Edit expense",{d.back()},t.name)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
            Txt(if(value.isBlank())"₹0" else "₹$value",56,bold=true,display=true,modifier=Modifier.fillMaxWidth(),align=TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Txt("Paid by ${if(payer==t.ownId(d.user))"you" else t.members.first{it.id==payer}.name} · split ${t.people.size} ways · ${money(amount/t.people.size)} each",12,Muted,modifier=Modifier.fillMaxWidth(),align=TextAlign.Center)
            Spacer(Modifier.height(14.dp));Field(title,{title=it.take(100)},"What was the kharcha?")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Food","Stay","Travel","Fun").forEach { c ->
                    Row(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp)).background(if(category==c)Ink else Color.White).border(1.dp,if(category==c)Ink else Line,RoundedCornerShape(12.dp)).clickable{category=c},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                        // Food's selected icon is the white Figma asset; other icons keep their original ink.
                        if(c=="Food" && category==c)Asset("72f4b.svg",14f) else if(category!=c)Asset(categoryIcon(c),14f)
                        Spacer(Modifier.width(4.dp));Txt(c,13,if(category==c)Color.White else Ink,bold=true)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Panel(padding=16) {
                Row(Modifier.fillMaxWidth().height(44.dp).clickable{picker=true},horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Txt("Paid by",14,Muted)
                    Row(verticalAlignment=Alignment.CenterVertically){Avatar(t.members.first{it.id==payer}.name,payer==t.ownId(d.user),24);Txt("  ${if(payer==t.ownId(d.user))"You" else t.members.first{it.id==payer}.name}",14,bold=true);Spacer(Modifier.width(8.dp));Asset("a8ecb.svg",18f)}
                }
                DividerLine()
                Row(Modifier.fillMaxWidth().height(44.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Txt("Split",14,Muted);Row(Modifier.clickable{splitDialog=true},verticalAlignment=Alignment.CenterVertically){t.people.take(4).forEach{Avatar(it.name,it.id==t.ownId(d.user),22)};Txt(if(weights.values.all{it==1})"  Equally" else "  By shares",14,bold=true);Asset("a8ecb.svg",18f)}
                }
                DividerLine()
                Row(Modifier.fillMaxWidth().height(40.dp).clickable{dateDialog=true},horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Txt("Date",14,Muted);Row{Txt(date,14,bold=true);Spacer(Modifier.width(8.dp));Asset("a8ecb.svg",18f)}}
            }
            Spacer(Modifier.height(12.dp))
            Keypad(decimal=true){key ->
                value=when(key){"⌫"->value.dropLast(1);"."->if('.' in value)value else (value.ifEmpty{"0"}+".");else->if(value.length<11 && ('.' !in value || value.substringAfter('.').length<2)) { if(value=="0")key else value+key } else value}
            }
            Spacer(Modifier.height(12.dp))
            SmallAction(if(photo.isEmpty())"Add bill photo" else "Replace bill photo"){
                d.platform.pickPhoto{uri->if(uri!=null){photo=d.platform.photoData(uri)?:"";if(photo.isEmpty())d.notice="Couldn't read this photo. Try another image."}}
            }
        }
        Box(Modifier.padding(24.dp)){Primary(if(d.busy)"Saving…" else "Save expense",amount>0&&title.isNotBlank()&&date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))&&!d.busy){
            val body=AddExpense(existing?.id?:d.platform.newId(),title,amount,payer,category,date,weights,photo)
            if(existing==null)d.addExpense(body) else d.editExpense(body)
        }}
    }
    if(picker)AlertDialog(onDismissRequest={picker=false},title={Txt("Who paid?",22,bold=true)},text={Column{t.members.forEach{m->Row(Modifier.fillMaxWidth().clickable{payer=m.id;picker=false}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Avatar(m.name,m.id==t.ownId(d.user),32);Txt("  ${m.name}",15,bold=m.id==payer)}}}},confirmButton={})
    if(dateDialog)AlertDialog(onDismissRequest={dateDialog=false},title={Txt("Expense date",22,bold=true)},text={Field(date,{date=it.take(10)},"YYYY-MM-DD")},confirmButton={TextButton(onClick={dateDialog=false}){Txt("Done",bold=true)}})
    if(splitDialog)AlertDialog(onDismissRequest={splitDialog=false},title={Txt("Split by shares",22,bold=true)},text={Column{
        Txt("Use 0 to exclude someone. A family of four can have 4 shares.",13,Muted)
        t.people.forEach{m->Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Txt(m.name,14,modifier=Modifier.weight(1f));SmallAction("−"){weights=weights+(m.id to ((weights[m.id]?:1)-1).coerceAtLeast(0))};Txt(" ${weights[m.id]?:1} ",16,bold=true);SmallAction("+"){weights=weights+(m.id to ((weights[m.id]?:1)+1).coerceAtMost(1000))}
        }}
    }},confirmButton={TextButton(enabled=weights.values.sum()>0,onClick={splitDialog=false}){Txt("Done",bold=true)}})
}
@Composable fun DetailScreen(d: Desk) {
    val t=d.trip ?: return;val e=t.expenses.find{it.id==d.expenseId} ?: return
    var deleting by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().background(Paper)) {
        Top("Expense",{d.back()})
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(64.dp).background(Soft,RoundedCornerShape(19.dp)),contentAlignment=Alignment.Center){Asset(if(e.category=="Food")"7dd69.svg" else categoryIcon(e.category),32f)}
            Txt(e.title,26,bold=true,display=true);Txt("${e.date} · ${e.category}",14,Muted)
            Panel(padding=20){Txt(money(e.amount),44,bold=true,display=true);Spacer(Modifier.height(12.dp));Row(verticalAlignment=Alignment.CenterVertically){Avatar(t.members.first{it.id==e.paidBy}.name,e.paidBy==t.ownId(d.user),28);Txt("  Paid by ${t.members.first{it.id==e.paidBy}.name}",15,bold=true)}}
            Panel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Txt("Split equally · ${e.shares.size} people",14,bold=true);Txt("to the paise",11,Muted)}
                e.shares.forEach{share->val m=t.members.first{it.id==share.memberId};Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Row(verticalAlignment=Alignment.CenterVertically){Avatar(m.name,m.id==t.ownId(d.user),28);Txt("  ${if(m.id==t.ownId(d.user))"You" else m.name}",15)};Txt(money(share.amount),15,bold=true)}}
            }
            Row(verticalAlignment=Alignment.CenterVertically){Asset("ba3d3.svg",16f);Txt("  Added by ${t.members.find{it.id==e.addedBy}?.name ?: "Friend"}",12,Muted)}
            if(e.receipt.isNotEmpty())ReceiptImage(e.receipt)
        }
        if(!t.closed&&t.settlements.isEmpty())Row(Modifier.padding(24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.weight(0.38f)){SmallAction("Delete"){deleting=true}}
            Box(Modifier.weight(0.62f)){Primary("Edit",!d.busy){d.screen="expense"}}
        }
    }
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Txt("Delete this kharcha?",22,bold=true)},text={Txt("This removes ${e.title} and recalculates everyone's balance.")},confirmButton={TextButton(onClick={deleting=false;d.removeExpense()}){Txt("Delete",color=Mitti,bold=true)}},dismissButton={TextButton(onClick={deleting=false}){Txt("Keep expense")}})
}
@OptIn(ExperimentalEncodingApi::class)
@Composable fun ReceiptImage(value: String) {
    val bytes=remember(value){runCatching{Base64.decode(value.substringAfter(','))}.getOrNull()}
    if(bytes!=null)AsyncImage(bytes,"Attached bill photo",Modifier.fillMaxWidth().heightIn(max=400.dp).clip(RoundedCornerShape(16.dp)))
}
@Composable fun PaySheet(d: Desk,t: Trip) {
    val p=d.paying ?: return;val recipient=t.members.first{it.id==p.to}
    var value by remember(p){mutableStateOf((p.amount/100).toString()+if(p.amount%100>0)"."+ (p.amount%100).toString().padStart(2,'0') else "")}
    var upi by remember(recipient.upi){mutableStateOf(recipient.upi)}
    var edit by remember{mutableStateOf(false)};var qr by remember{mutableStateOf(false)};var confirm by remember{mutableStateOf<String?>(null)}
    var reference by remember{mutableStateOf("")}
    val amount=parseAmount(value) ?: 0
    val valid=amount in 1..p.amount
    val validUpi=upi.matches(Regex("[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}"))
    val uri="upi://pay?pa=${upi.encodeURLParameter()}&pn=${recipient.name.encodeURLParameter()}&am=${amount/100}.${(amount%100).toString().padStart(2,'0')}&cu=INR&tn=${("Chukta "+t.name).encodeURLParameter()}"
    ModalBottomSheet(onDismissRequest={d.paying=null},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Color.White,dragHandle={Box(Modifier.padding(top=10.dp,bottom=18.dp).size(40.dp,5.dp).background(Line,RoundedCornerShape(3.dp)))}) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Txt("Pay ${recipient.name.substringBefore(' ')}",28,bold=true,display=true)
            Txt("${t.name} · payment ${t.settlements.size+1}",13,Muted)
            Row(Modifier.fillMaxWidth().background(Paper,RoundedCornerShape(18.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                Avatar(recipient.name);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Txt(recipient.name,15,bold=true);Txt(upi.ifEmpty{"UPI ID needed"},12,Muted)}
                Box(Modifier.size(40.dp).clickable(enabled=validUpi){d.platform.copy(upi);d.notice="UPI ID copied"},contentAlignment=Alignment.Center){Asset("7c47e.svg",20f,description="Copy UPI ID")}
            }
            if(!validUpi)Field(upi,{upi=it.take(100)},"Friend's UPI ID, e.g. name@upi")
            if(edit)Field(value,{value=it},"Amount",numeric=true)
            else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Txt(money(amount),48,bold=true,display=true);SmallAction("Edit"){edit=true}}
            Txt("Pay part now and the rest later, if you like.",12,Muted)
            Primary("Pay ${money(amount)}",valid&&validUpi&&!d.busy){
                if(d.platform.openUrl(uri))confirm="upi_manual" else d.notice="No UPI app found. Use the QR, copy the ID, or record a cash payment."
            }
            Txt("Opens your UPI app. You come back here to confirm.",11,Muted,modifier=Modifier.fillMaxWidth(),align=TextAlign.Center)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                listOf("Show QR" to "3cf5b.svg","Copy UPI ID" to "4d5a1.svg","Paid in cash" to "de163.svg").forEachIndexed { i,(label,file) ->
                    Column(Modifier.weight(1f).height(76.dp).background(Paper,RoundedCornerShape(16.dp)).clickable {
                        when(i){0->if(valid&&validUpi)qr=true else d.notice="Enter a valid UPI ID and amount first.";1->if(validUpi){d.platform.copy(upi);d.notice="UPI ID copied"}else d.notice="Enter a UPI ID first.";else->if(valid)confirm="cash"}
                    },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Asset(file,20f);Spacer(Modifier.height(8.dp));Txt(label,11,bold=true)}
                }
            }
            TextButton(onClick={confirm="upi_manual"},enabled=valid){Txt("Already paid? Record UPI reference",12,bold=true)}
        }
    }
    if(qr)AlertDialog(onDismissRequest={qr=false},title={Txt("Pay ${recipient.name}",22,bold=true)},text={Column(horizontalAlignment=Alignment.CenterHorizontally){Image(rememberQrCodePainter(uri),"UPI payment QR",Modifier.size(250.dp).background(Color.White).padding(16.dp));Txt("${money(amount)} · $upi",14)}},confirmButton={TextButton(onClick={qr=false}){Txt("Close")}})
    confirm?.let { method ->
        AlertDialog(onDismissRequest={confirm=null},title={Txt(if(method=="cash")"Record cash payment?" else "Confirm your UPI payment",22,bold=true,display=true)},text={
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                Txt(if(method=="cash")"Confirm you handed ${money(amount)} to ${recipient.name}." else "Check that your UPI app shows a successful payment, then enter its reference. Chukta records your confirmation.")
                if(method=="upi_manual")Field(reference,{reference=it.take(100)},"UPI transaction reference")
            }
        },confirmButton={TextButton(enabled=valid&&!d.busy&&(method=="cash"||reference.isNotBlank()),onClick={confirm=null;d.settle(amount,method,reference)}){Txt("Confirm payment",bold=true)}},dismissButton={TextButton(onClick={confirm=null}){Txt("Cancel")}})
    }
}
@Composable fun PaidScreen(d: Desk) {
    val t=d.trip ?: return;val receipt=d.receipt ?: return
    val recipient=t.members.first{it.id==receipt.to};val next=t.transfers().firstOrNull{it.from==t.ownId(d.user)}
    Column(Modifier.fillMaxSize().background(Haldi)) {
        Top("",{d.screen="trip";d.tab="Balances"})
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.padding(top=16.dp,bottom=24.dp).size(116.dp).rotate(6f).shadow(16.dp,RoundedCornerShape(36.dp)).background(Ink,RoundedCornerShape(36.dp)),contentAlignment=Alignment.Center){Asset("fd27b.svg",52f,44f)}
            Txt("Hisaab chukta!",40,bold=true,display=true)
            Spacer(Modifier.height(12.dp));Txt("You paid ${recipient.name.substringBefore(' ')} ${money(receipt.amount)} for ${t.name}.",15,modifier=Modifier.fillMaxWidth(),align=TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Panel{LineRow("To",recipient.name);DividerLine();LineRow("Amount",money(receipt.amount));DividerLine();LineRow(if(receipt.method=="cash")"Method" else "UPI ref",if(receipt.method=="cash")"Cash" else receipt.reference);DividerLine();LineRow("When",d.platform.formatDate(receipt.date))}
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Txt("${t.settlements.size} payments recorded",13,bold=true);Txt(if(next==null)"All clear" else "Left: ${money(next.amount)}",12)}
            Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={t.settlements.size.toFloat()/(t.settlements.size+t.transfers().size).coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),color=Ink,trackColor=Soft)
        }
        Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Primary(if(next==null)"See trip summary" else "Pay ${t.members.first{it.id==next.to}.name} ${money(next.amount)}"){
                d.screen="trip";d.tab=if(next==null)"Summary" else "Balances";d.paying=next
            }
            Spacer(Modifier.height(12.dp));TextButton(onClick={d.screen="trip";d.tab="Balances"}){Txt("Back to trip",15,bold=true)}
        }
    }
}
@Composable fun SettingsScreen(d: Desk) {
    var name by remember{mutableStateOf(d.user?.name?:"")}
    var upi by remember{mutableStateOf(d.user?.upi?:"")}
    var logout by remember{mutableStateOf(false)}
    var connection by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().background(Paper)){
        Top("Your profile",{d.back()})
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Avatar(name,true,64);Txt("Your name",13,Muted);Field(name,{name=it},"Your name")
            Txt("Your UPI ID",13,Muted);Field(upi,{upi=it},"name@upi")
            Txt("Friends use this ID to settle with you. Payments happen in their UPI app.",13,Muted)
            Primary("Save profile",name.isNotBlank()&&!d.busy){d.profile(name,upi)}
            Panel{Txt("Your saved hisaab",18,bold=true,display=true);Txt("${d.trips.size} trips · ${d.queue.size} expenses waiting to sync",14,Muted);Spacer(Modifier.height(12.dp));SmallAction("Sync now"){d.action{d.refresh()}}}
            SmallAction("Share my backup"){d.platform.share(ChuktaJson.encodeToString(Snapshot.serializer(),Snapshot(d.user,d.trips,d.queue)))}
            SmallAction("Sign out"){logout=true}
            if(d.platform.development)SmallAction("API connection"){connection=true}
        }
    }
    if(logout)AlertDialog(onDismissRequest={logout=false},title={Txt("Sign out?",22,bold=true)},text={Txt(if(d.queue.isNotEmpty())"Sync ${d.queue.size} offline expenses before signing out." else "Your online trip history stays saved. This phone's session and cache will be cleared.")},confirmButton={TextButton(enabled=d.queue.isEmpty(),onClick={logout=false;d.logout()}){Txt("Sign out",bold=true)}},dismissButton={TextButton(onClick={logout=false}){Txt("Stay signed in")}})
    if(connection)ConnectionDialog(d){connection=false}
}


