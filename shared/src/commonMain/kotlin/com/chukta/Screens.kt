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
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import io.ktor.http.encodeURLParameter

@Composable fun Home(d: Desk) {
    var invite by remember { mutableStateOf(d.platform.read("pendingInvite")!=null) }
    val due = d.trips.filter{!it.closed}.flatMap { t -> t.transfers().filter{it.from==t.ownId(d.user)}.map{t to it} }
    val total = due.sumOf{it.second.amount}
    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart=36.dp,bottomEnd=36.dp)).background(Haldi).padding(24.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column { Txt("Hey ${d.user?.name?.substringBefore(' ') ?: "friend"}",24,bold=true,display=true); Txt("Hisaab saaf, dosti pakki.",13) }
                Box(Modifier.clickable{d.screen="settings"}) { Avatar(d.user?.name?:"K",true) }
            }
            Spacer(Modifier.height(28.dp))
            if(due.isEmpty()) {
                Txt(if(d.trips.isEmpty()) "Your first trip" else "Hisaab saaf",15)
                Spacer(Modifier.height(4.dp))
                Txt(if(d.trips.isEmpty()) "Going somewhere\nwith friends?" else "All caught up.\nWhere next?",40,bold=true,display=true)
                Spacer(Modifier.height(14.dp)); Txt("Make a trip, add everyone, and Chukta keeps the hisaab for you.",15)
                Spacer(Modifier.height(28.dp)); Primary("Create a trip",!d.busy,true){d.screen="create"}
            } else {
                Txt("You pay",15); Txt(money(total),68,bold=true,display=true)
                Txt(due.joinToString(" · "){(t,p)->"${t.members.first{it.id==p.to}.name} ${money(p.amount)}"},14)
                Spacer(Modifier.height(20.dp))
                val (t,p)=due.first()
                Panel(padding=18) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Txt("${t.icon} ${t.name}",12,Muted); Txt("Payment 1 of ${due.size}",12,Muted) }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Row(verticalAlignment=Alignment.CenterVertically){Txt("You",17,bold=true);Spacer(Modifier.width(6.dp));Asset("ba632.svg",38f,12f);Spacer(Modifier.width(6.dp));Txt(t.members.first{it.id==p.to}.name,17,bold=true)}
                        Txt(money(p.amount),24,bold=true,display=true)
                    }
                    Spacer(Modifier.height(14.dp)); Asset("ec8a2.svg",306f,2f);Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Column { Txt(t.members.first{it.id==p.to}.upi.ifEmpty{"Add friend's UPI ID"},12);Txt("Opens any UPI app",11,Muted) }
                        SmallAction("Pay ${money(p.amount)}",true){d.selectedId=t.id;d.screen="trip";d.tab="Balances";d.paying=p}
                    }
                }
            }
        }
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            if(d.trips.isEmpty()) {
                Panel(Modifier.clickable{invite=true},padding=14) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(Soft,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Asset("37b7e.svg",20f)}
                        Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Txt("Got an invite link?",14,bold=true);Txt("Paste it to join your friends' trip",12,Muted)}
                        Asset("d4529.svg",20f)
                    }
                }
                Spacer(Modifier.height(2.dp));Txt("How Chukta works",20,bold=true,display=true)
                listOf("Make a trip" to "Add friends from contacts or WhatsApp.","Add every kharcha" to "Takes 10 seconds, works offline.","Settle on UPI" to "Chukta shows who pays whom.").forEachIndexed { i, (title, body) ->
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(Color.White,RoundedCornerShape(10.dp)).border(1.dp,Line,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Txt((i+1).toString(),14,bold=true)}
                        Spacer(Modifier.width(14.dp));Column{Txt(title,15,bold=true);Txt(body,12,Muted)}
                    }
                }
            } else {
                val gets=d.trips.sumOf{t->t.transfers().filter{it.to==t.ownId(d.user)}.sumOf{it.amount}}
                if(gets>0) Panel {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Asset("f3d07.svg",20f);Column{Txt("You get",12,Muted);Txt(money(gets),18,Green,true)}
                        SmallAction("Remind"){d.platform.share("A friendly Chukta reminder: our trip hisaab has ${money(gets)} outstanding. Open Chukta when you get a chance.")}
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Txt("Your trips",20,bold=true,display=true);SmallAction("+ New trip"){d.screen="create"}}
                d.trips.chunked(2).forEach { row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        row.forEach { t ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.White).border(1.dp,Line,RoundedCornerShape(20.dp)).clickable{d.openTrip(t)}) {
                                Box(Modifier.fillMaxWidth().height(90.dp).background(if(t.icon=="🏔️")Color(0xFFE2EFF7) else Soft)) {
                                    Box(Modifier.padding(10.dp).background(Color.White,RoundedCornerShape(8.dp)).padding(6.dp)){Txt(if(t.closed)"Closed" else "Ongoing",11)}
                                    Txt(t.icon,44,modifier=Modifier.align(Alignment.Center))
                                }
                                Column(Modifier.padding(14.dp)) {
                                    Txt(t.name,18,bold=true,display=true);Txt("${t.dates.ifEmpty{"Dates optional"}} · ${t.people.size} friends",11,Muted)
                                    val net=t.balances().find{it.member.id==t.ownId(d.user)}?.net ?: 0
                                    Spacer(Modifier.height(8.dp));Txt(if(net<0)"You pay ${money(-net)}" else if(net>0)"You get ${money(net)}" else "All settled",12,if(net<0)Mitti else Green,true)
                                }
                            }
                        }
                        if(row.size==1) Spacer(Modifier.weight(1f))
                    }
                }
                SmallAction("Join a friend's trip"){invite=true}
            }
        }
    }
    if(invite)InviteDialog(d){invite=false}
}
@Composable fun InviteDialog(d: Desk, dismiss: () -> Unit) {
    var value by remember{mutableStateOf(d.platform.read("pendingInvite") ?: "")};var preview by remember{mutableStateOf<InvitePreview?>(null)}
    var selected by remember{mutableStateOf("")}
    val code=value.trim().substringAfterLast('/').substringBefore('?')
    AlertDialog(onDismissRequest=dismiss,title={Txt("Join your friends' trip",22,bold=true,display=true)},text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Txt("Paste an invite link or code. You can preview a trip before signing in.",14,Muted)
            Field(value,{value=it;preview=null},"Invite link or code")
            preview?.let{
                Txt("${it.icon} ${it.name}",20,bold=true);Txt("${it.members} friends · total ${money(it.total)}",14)
                Txt("Choose your name to see your share",13,Muted)
                it.people.forEach { person -> Row(Modifier.fillMaxWidth().clickable{selected=person.id}.padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                    Txt((if(selected==person.id)"✓ " else "")+person.name,14,bold=selected==person.id)
                    Txt(if(person.balance<0)"You pay ${money(-person.balance)}" else "You get ${money(person.balance)}",12,if(person.balance<0)Mitti else Green)
                } }
            }
        }
    },confirmButton={TextButton(enabled=code.isNotBlank()&&!d.busy,onClick={
        if(preview==null)d.action{preview=d.api.get("/v1/invites/$code")}
        else if(d.api.token==null){d.platform.write("pendingInvite",code);dismiss();d.screen="login";d.notice="Sign in to join ${preview!!.name}. Your invite is saved."}
        else if(selected.isNotEmpty()){d.join(code,selected);d.platform.write("pendingInvite",null);dismiss()}
    }){Txt(if(preview==null)"Preview trip" else if(d.api.token==null)"Sign in to join" else "Join trip",bold=true)}},dismissButton={TextButton(onClick=dismiss){Txt("Cancel")}})
}
@Composable fun CreateScreen(d: Desk) {
    var name by remember{mutableStateOf("")};var dates by remember{mutableStateOf("")};var icon by remember{mutableStateOf("🏖️")}
    var friends by remember{mutableStateOf(listOf<String>())};var add by remember{mutableStateOf(false)};var friend by remember{mutableStateOf("")}
    var kitty by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().background(Paper)) {
        Top("New trip",{d.back()})
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Column{Txt("Trip icon",13,Muted);Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                listOf("🏖️","🏔️","🎉","✈️").forEach { emoji ->
                    Box(Modifier.size(56.dp).background(if(emoji==icon)Soft else Color.White,RoundedCornerShape(16.dp)).border(if(emoji==icon)2.dp else 1.dp,if(emoji==icon)Ink else Line,RoundedCornerShape(16.dp)).clickable{icon=emoji},contentAlignment=Alignment.Center){Txt(emoji,26)}
                }
                Box(Modifier.size(56.dp).background(Color.White,RoundedCornerShape(16.dp)).border(1.dp,Line,RoundedCornerShape(16.dp)).clickable{d.notice="Choose a trip icon from the four options."},contentAlignment=Alignment.Center){Asset("b557c.svg",24f)}
            }}
            Column{Txt("Trip name",13,Muted);Spacer(Modifier.height(8.dp));Field(name,{name=it.take(80)},"Goa trip")}
            Column{Txt("Dates · optional",13,Muted);Spacer(Modifier.height(8.dp));Field(dates,{dates=it.take(80)},"e.g. 9–11 Oct"){Asset("66a68.svg",20f)}}
            Column {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Txt("Members · ${friends.size+1}",13,Muted);Txt("From contacts",13,bold=true,modifier=Modifier.clickable{d.platform.pickContact{if(!it.isNullOrBlank()&&friends.size<20)friends=friends+it}}) }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    Column(horizontalAlignment=Alignment.CenterHorizontally){Avatar(d.user?.name?:"You",true);Spacer(Modifier.height(6.dp));Txt("You",12)}
                    friends.forEach { n -> Column(Modifier.clickable{friends=friends-n},horizontalAlignment=Alignment.CenterHorizontally){Avatar(n);Spacer(Modifier.height(6.dp));Txt(n.substringBefore(' '),12)} }
                    Column(Modifier.clickable{add=true},horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(44.dp).border(1.dp,Muted,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Asset("628b1.svg",20f)};Spacer(Modifier.height(6.dp));Txt("Add",12)}
                }
                Spacer(Modifier.height(14.dp))
                Panel(Modifier.clickable(enabled=name.isNotBlank()&&!d.busy){
                    d.action {
                        val t=d.api.send<CreateTrip,Trip>("/v1/trips",CreateTrip(name,icon,dates,friends,kitty))
                        d.update(t);d.openTrip(t);d.platform.share(d.inviteText(t))
                    }
                },padding=14) {
                    Row(verticalAlignment=Alignment.CenterVertically){Asset("37b7e.svg",20f);Spacer(Modifier.width(10.dp));Txt("Invite with a WhatsApp link",14,bold=true,modifier=Modifier.weight(1f));Asset("d4529.svg",20f)}
                }
            }
            Row(Modifier.fillMaxWidth().background(Soft,RoundedCornerShape(20.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Txt("Kitty",16,bold=true);Txt("One common pot, paid before the trip",12,Muted)}
                Switch(kitty,{kitty=it},colors=SwitchDefaults.colors(checkedTrackColor=Ink,checkedThumbColor=Haldi))
            }
        }
        Box(Modifier.padding(24.dp)){Primary(if(d.busy)"Creating…" else "Create trip",name.isNotBlank()&&!d.busy){d.create(CreateTrip(name,icon,dates,friends,kitty))}}
    }
    if(add)AlertDialog(onDismissRequest={add=false},title={Txt("Add a friend",22,bold=true,display=true)},text={Column{Txt("Friends can join through the trip invite. Tap an existing name to remove it.",13,Muted);Spacer(Modifier.height(12.dp));Field(friend,{friend=it.take(60)},"Friend's name")}},confirmButton={TextButton(enabled=friend.isNotBlank()&&friends.size<20,onClick={friends=friends+friend.trim();friend="";add=false}){Txt("Add",bold=true)}},dismissButton={TextButton(onClick={add=false}){Txt("Cancel")}})
}
@Composable fun TripHeader(d: Desk,t: Trip) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart=28.dp,bottomEnd=28.dp)).background(Soft).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Box(Modifier.size(40.dp).background(Color.White,RoundedCornerShape(12.dp)).clickable{d.screen="home"},contentAlignment=Alignment.Center){Asset("51f7b.svg",20f,description="Back")}
            Box(Modifier.size(40.dp).background(Color.White,RoundedCornerShape(12.dp)).clickable{d.platform.share(d.inviteText(t))},contentAlignment=Alignment.Center){Asset("589db.svg",20f,description="Share trip")}
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(56.dp).background(Color.White,RoundedCornerShape(18.dp)),contentAlignment=Alignment.Center){Txt(t.icon,28)}
            Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Txt(t.name,24,bold=true,display=true);Txt("${t.dates.ifEmpty{"Your trip"}} · ${t.people.size} friends",12,Muted)}
            Row { t.people.take(4).forEach { Avatar(it.name,it.id==t.ownId(d.user),28) } }
        }
    }
}
@Composable fun TripScreen(d: Desk) {
    val t=d.trip ?: return
    Column(Modifier.fillMaxSize().background(Paper)) {
        TripHeader(d,t)
        if(t.ownerId==d.user?.id&&t.joinRequests.isNotEmpty())Row(Modifier.fillMaxWidth().background(Soft).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            val request=t.joinRequests.first();Txt("${request.name} wants to join as ${t.members.first{it.id==request.memberId}.name}",12,modifier=Modifier.weight(1f));SmallAction("Approve"){d.approve(request.userId)}
        }
        Row(Modifier.padding(horizontal=24.dp,vertical=16.dp).fillMaxWidth().height(48.dp).background(Color.White,RoundedCornerShape(14.dp)).border(1.dp,Line,RoundedCornerShape(14.dp)).padding(4.dp)) {
            listOf("Expenses","Balances","Summary").forEach { label ->
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(if(d.tab==label)Ink else Color.Transparent).clickable{d.tab=label},contentAlignment=Alignment.Center){Txt(label,14,if(d.tab==label)Color.White else Muted,bold=d.tab==label)}
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            when(d.tab){"Expenses"->Expenses(d,t);"Balances"->Balances(d,t);else->Summary(d,t)}
            Spacer(Modifier.height(4.dp))
        }
        if(d.tab=="Expenses"&&!t.closed)Box(Modifier.padding(24.dp)){Primary("+  Add expense",!d.busy){d.expenseId="";d.screen="expense"}}
        if(d.tab=="Summary")Row(Modifier.padding(24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.weight(0.38f)){SmallAction(if(t.closed)"Closed" else "Close trip"){if(!t.closed)d.closeTrip()}}
            Box(Modifier.weight(0.62f)){Primary("Share summary"){d.platform.share("${t.icon} ${t.name}\nTotal spent: ${money(t.expenses.sumOf{it.amount})}\n${t.people.size} friends · ${t.expenses.size} expenses\n${if(t.transfers().isEmpty())"Hisaab chukta!" else "Some balances are still open"}\nMade with Chukta")}}
        }
    }
    if(d.paying!=null)PaySheet(d,t)
}
@Composable fun Expenses(d: Desk,t: Trip) {
    val own=t.balances().find{it.member.id==t.ownId(d.user)}
    Panel(padding=16) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            listOf("Total" to money(t.expenses.sumOf{it.amount}),"My share" to money(own?.share?:0),"My balance" to money(own?.net?:0)).forEachIndexed { i,(label,value) ->
                Column{Txt(label,11,Muted);Txt(value,18,if(i==2&&(own?.net?:0)<0)Mitti else Ink,true,true)}
                if(i<2)Spacer(Modifier.width(1.dp).height(34.dp).background(Line))
            }
        }
    }
    if(t.kitty)KittyPanel(d,t)
    d.deleted?.let{deleted->Row(Modifier.fillMaxWidth().background(Soft,RoundedCornerShape(12.dp)).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Txt("Expense deleted",13);SmallAction("Undo"){d.addExpense(deleted);d.deleted=null}
    }}
    if(t.expenses.isEmpty())Panel(color=Soft,padding=24){Txt("Every kharcha, in one place.",24,bold=true,display=true);Spacer(Modifier.height(10.dp));Txt("Add your first expense. We'll split it equally and keep everyone's hisaab clear.",14,Muted)}
    t.expenses.groupBy{it.date}.toSortedMap(compareByDescending{it}).forEach { (date,list) ->
        Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Txt(date,14,bold=true);Txt(money(list.sumOf{it.amount}),12,Muted)}
        Panel(padding=14){
            list.reversed().forEachIndexed { i,e ->
                Row(Modifier.fillMaxWidth().clickable{d.expenseId=e.id;d.screen="detail"}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Soft,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Asset(categoryIcon(e.category),20f)}
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)){Txt(e.title,15,bold=true);Txt("${t.members.find{it.id==e.paidBy}?.name ?: "Friend"} paid",12,Muted)}
                    Column(horizontalAlignment=Alignment.End){Txt(money(e.amount),15,bold=true);Txt("your share ${money(e.shares.find{it.memberId==t.ownId(d.user)}?.amount?:0)}",11,Muted)}
                }
                if(i<list.lastIndex)DividerLine()
            }
        }
    }
}
@Composable fun KittyPanel(d: Desk,t: Trip) {
    var show by remember{mutableStateOf(false)};var value by remember{mutableStateOf("")};var payer by remember{mutableStateOf(t.ownId(d.user))}
    val remaining=t.contributions.sumOf{it.amount}-t.expenses.filter{it.paidBy=="kitty-"+t.id}.sumOf{it.amount}
    Panel(color=Soft){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Column{Txt("Kitty balance",14,bold=true);Txt(money(remaining),24,bold=true,display=true)}
        if(!t.closed)SmallAction("Add funds"){show=true}
    };Txt("Contributions are included in everyone's net hisaab.",12,Muted)}
    if(show)AlertDialog(onDismissRequest={show=false},title={Txt("Record kitty contribution",22,bold=true)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Txt("Record money already added to the common pot.",13,Muted)
        Field(value,{value=it},"Amount in rupees",numeric=true)
        t.people.forEach{m->Txt((if(payer==m.id)"✓ " else "")+m.name,14,modifier=Modifier.fillMaxWidth().clickable{payer=m.id}.padding(8.dp))}
    }},confirmButton={TextButton(enabled=(parseAmount(value)?:0)>0&&!d.busy,onClick={
        d.action{d.update(d.api.send<Contribution,Trip>("/v1/trips/${t.id}/contributions",Contribution(d.platform.newId(),payer,parseAmount(value)!!)))};show=false
    }){Txt("Record funds",bold=true)}},dismissButton={TextButton(onClick={show=false}){Txt("Cancel")}})
}
fun categoryIcon(category: String) = when(category){"Stay"->"89de1.svg";"Travel"->"9cef2.svg";"Fun"->"3d815.svg";else->"8e873.svg"}
@Composable fun Balances(d: Desk,t: Trip) {
    val transfers=t.transfers();val count=t.settlements.size+transfers.size
    val original=t.settlements.sumOf{it.amount}+transfers.sumOf{it.amount}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Txt("${t.settlements.size} of $count settled",14,bold=true);Txt("${money(t.settlements.sumOf{it.amount})} of ${money(original)} back",12,Muted)}
    LinearProgressIndicator(progress={if(original==0L)1f else t.settlements.sumOf{it.amount}.toFloat()/original},modifier=Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),color=Haldi,trackColor=Line)
    val own=transfers.filter{it.from==t.ownId(d.user)}
    Txt("Your payments",13,Muted)
    if(own.isEmpty())Panel(color=Soft){Txt("Your hisaab is clear.",22,bold=true,display=true);Txt("Nothing left for you to pay.",14,Muted)}
    own.forEachIndexed { i,p ->
        val to=t.members.first{it.id==p.to}
        if(i==0)Panel(color=Haldi,padding=18) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Row(verticalAlignment=Alignment.CenterVertically){Txt("You",16,bold=true);Spacer(Modifier.width(8.dp));Asset("44723.svg",38f,12f);Spacer(Modifier.width(8.dp));Txt(to.name,16,bold=true)};Txt("1 of ${own.size}",11)}
            Txt(money(p.amount),40,bold=true,display=true);Spacer(Modifier.height(12.dp));DashedLine();Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Txt("${t.name} share",12);SmallAction("Pay ${money(p.amount)}",true){d.paying=p}}
        } else Panel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Row(verticalAlignment=Alignment.CenterVertically){Txt("You",14,bold=true);Asset("6d483.svg",34f,12f);Txt(to.name,14,bold=true)};Txt(money(p.amount),16,bold=true);SmallAction("Pay",true){d.paying=p}}}
    }
    val others=transfers.filter{it.from!=t.ownId(d.user)}
    if(others.isNotEmpty()||t.settlements.isNotEmpty())Txt("Others",13,Muted)
    others.forEach { p -> Panel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Txt("${t.members.first{it.id==p.from}.name} → ${t.members.first{it.id==p.to}.name}",14,bold=true);Txt(money(p.amount),14,bold=true)}} }
    t.settlements.forEach { s -> Panel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Txt("${t.members.first{it.id==s.from}.name} → ${t.members.first{it.id==s.to}.name}",13,Muted);Row(verticalAlignment=Alignment.CenterVertically){Asset("7d286.svg",14f);Txt(" Settled",12,bold=true)}}} }
    Txt("How it adds up",13,Muted)
    Panel{t.balances().forEachIndexed{i,b->Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
        Avatar(b.member.name,b.member.id==t.ownId(d.user),30);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Txt(if(b.member.id==t.ownId(d.user))"You" else b.member.name,14);Txt("paid ${money(b.paid)} · share ${money(b.share)}",11,Muted)}
        Column(horizontalAlignment=Alignment.End){Txt((if(b.net>0)"+" else "")+money(b.net),16,if(b.net<0)Mitti else Green,true);Txt(if(b.net<0)"pays" else if(b.net>0)"gets back" else "settled",11,Muted)}
    };if(i<t.members.lastIndex)DividerLine()}}
}
@Composable fun Summary(d: Desk,t: Trip) {
    val total=t.expenses.sumOf{it.amount};val settled=t.transfers().isEmpty()
    Panel(color=Haldi,padding=20) {
        Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).background(Ink,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Asset("e64da.svg",20f,16.94f)};Spacer(Modifier.width(12.dp));Column{Txt(if(settled)"All settled" else "Trip hisaab",20,bold=true,display=true);Txt("${t.settlements.size} payments recorded · ${t.name}",12)}}
        Spacer(Modifier.height(16.dp));Txt("Total spent",12)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Txt(money(total),40,bold=true,display=true);Column(horizontalAlignment=Alignment.End){Txt(money(if(t.members.isEmpty())0 else total/t.people.size),20,bold=true,display=true);Txt("each, ${t.people.size} friends",11)}}
        Spacer(Modifier.height(12.dp));Asset("26819.svg",302f,2f);Spacer(Modifier.height(12.dp))
        val biggest=t.expenses.groupBy{it.date}.maxByOrNull{it.value.sumOf{e->e.amount}}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Txt("Days",11);Txt(t.expenses.map{it.date}.distinct().size.toString(),14,bold=true)};Column{Txt("Expenses",11);Txt(t.expenses.size.toString(),14,bold=true)};Column{Txt("Biggest day",11);Txt(biggest?.let{money(it.value.sumOf{e->e.amount})}?:"—",14,bold=true)}}
    }
    Panel {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Txt("By category",14,bold=true);Txt("share of ${money(total)}",11,Muted)}
        Spacer(Modifier.height(10.dp))
        listOf("Stay","Food","Travel","Fun").forEach { c ->
            val amount=t.expenses.filter{it.category==c}.sumOf{it.amount};val ratio=if(total==0L)0f else amount.toFloat()/total
            Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){Txt("$c ${(ratio*100).toInt()}%",13,Muted);Txt(money(amount),13,bold=true)}
            LinearProgressIndicator(progress={ratio},modifier=Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),color=Ink,trackColor=Line)
        }
    }
}


