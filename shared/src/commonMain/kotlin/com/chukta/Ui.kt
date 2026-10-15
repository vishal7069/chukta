@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.chukta

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import coil3.compose.LocalPlatformContext
import com.chukta.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlinx.coroutines.delay

val Haldi = Color(0xFFF7B500)
val Ink = Color(0xFF1C1A17)
val Paper = Color(0xFFFBF7F0)
val Soft = Color(0xFFFFF1C7)
val Muted = Color(0xFF6B6459)
val Line = Color(0xFFE4DCCF)
val Green = Color(0xFF008A53)
val Mitti = Color(0xFFA84B26)
data class Fonts(val body: FontFamily, val display: FontFamily)
val LocalFonts = staticCompositionLocalOf { Fonts(FontFamily.Default, FontFamily.Default) }

@Composable fun Txt(text: String, size: Int = 15, color: Color = Ink, bold: Boolean = false, display: Boolean = false, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(text, modifier, color, size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = if (display) LocalFonts.current.display else LocalFonts.current.body,
        lineHeight = (size * if (display) 1.1f else 1.45f).sp, textAlign = align,
        letterSpacing = if (display) (-size * 0.01f).sp else 0.sp)
}
@Composable fun Asset(file: String, w: Float = 24f, h: Float = w, description: String? = null, modifier: Modifier = Modifier) {
    val bytes by produceState<ByteArray?>(null, file) { value = Res.readBytes("files/$file") }
    val context = LocalPlatformContext.current
    if (bytes != null) AsyncImage(
        model = ImageRequest.Builder(context).data(bytes).decoderFactory(SvgDecoder.Factory()).build(),
        contentDescription = description, contentScale = ContentScale.Fit, modifier = modifier.size(w.dp, h.dp)
    ) else Spacer(modifier.size(w.dp, h.dp))
}
@Composable fun Primary(label: String, enabled: Boolean = true, arrow: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(150))
    Row(modifier.fillMaxWidth().height(56.dp).scale(scale).clip(RoundedCornerShape(18.dp)).background(if (enabled) Ink else Ink.copy(alpha = 0.08f))
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Txt(label, 17, if (enabled) Color.White else Muted.copy(alpha = 0.55f), bold = true)
        if (arrow) { Spacer(Modifier.width(8.dp)); Asset(if(enabled) "0e067.svg" else "42ad6.svg", 20f) }
    }
}
@Composable fun SmallAction(label: String, dark: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp)).background(if(dark) Ink else Color.White).border(if(dark) 0.dp else 1.dp, Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Txt(label, 14, if(dark) Color.White else Ink, true)
    }
}
@Composable fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, padding: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color).border(1.dp, if(color == Color.White) Line else Color.Transparent, RoundedCornerShape(20.dp)).padding(padding.dp), content = content)
}
@Composable fun Avatar(name: String, own: Boolean = false, size: Int = 44) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.3f).dp)).background(if(own) Ink else Soft), contentAlignment = Alignment.Center) {
        Txt(name.take(1).uppercase(), (size * 0.42f).toInt(), if(own) Haldi else Ink, true, true)
    }
}
@Composable fun LineRow(label: String, value: String, color: Color = Ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Txt(label, 14, Muted); Txt(value, 14, color, true)
    }
}
@Composable fun DividerLine() { Spacer(Modifier.fillMaxWidth().height(1.dp).background(Line)) }
@Composable fun Top(title: String, onBack: () -> Unit, subtitle: String? = null, end: @Composable () -> Unit = { Spacer(Modifier.size(44.dp)) }) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) { Asset("80169.svg",24f,description="Back") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Txt(title,16,bold=true); subtitle?.let { Txt(it,12,Muted) } }
        end()
    }
}
@Composable fun Field(value: String, onValue: (String) -> Unit, hint: String = "", numeric: Boolean = false, modifier: Modifier = Modifier, suffix: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp,Line,RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(value, onValue, Modifier.weight(1f), singleLine = true, textStyle = TextStyle(color=Ink,fontSize=17.sp,fontFamily=LocalFonts.current.body,fontWeight=FontWeight.Medium),
            keyboardOptions = KeyboardOptions(keyboardType = if(numeric) KeyboardType.Number else KeyboardType.Text),
            decorationBox = { inner -> if(value.isEmpty()) Txt(hint,17,Muted.copy(alpha=0.6f)); inner() })
        suffix()
    }
}
@Composable fun Keypad(decimal: Boolean = false, phone: Boolean = false, onKey: (String) -> Unit) {
    val rows = listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf(if(decimal) "." else "", "0","⌫"))
    Column(Modifier.fillMaxWidth().background(if(phone) Color(0xFFECE5D9) else Color.Transparent).padding(if(phone) 6.dp else 0.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { key ->
                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(if(phone) 10.dp else 14.dp)).background(if(key.isEmpty() || (phone && key=="⌫")) Color.Transparent else Color.White)
                    .clickable(enabled=key.isNotEmpty()) { onKey(key) }, contentAlignment=Alignment.Center) {
                    if(key=="⌫") Asset(if(decimal) "b5d4f.svg" else "0ce26.svg",22f,description="Delete digit") else Txt(key,24,bold=true)
                }
            } }
        }
    }
}
@Composable fun Brand(large: Boolean = false) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(if(large) 11.dp else 5.5.dp)) {
        Asset(if(large) "ada2c.svg" else "9b065.svg", if(large) 58f else 26.316f, if(large) 49.13f else 22.291f)
        Asset(if(large) "25c0d.svg" else "9e014.svg", if(large) 164f else 73.222f, if(large) 40f else 18f,description="Chukta")
    }
}
@Composable fun DashedLine() {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        drawLine(Ink.copy(alpha=0.25f), androidx.compose.ui.geometry.Offset(0f,1f), androidx.compose.ui.geometry.Offset(size.width,1f), strokeWidth=1.5.dp.toPx(), pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),6.dp.toPx())))
    }
}
@Composable fun LoginArt() {
    Box(Modifier.fillMaxWidth().height(320.dp)) {
        Panel(Modifier.offset(x=118.dp,y=38.dp).width(222.dp).rotate(-7f).shadow(18.dp,RoundedCornerShape(24.dp)),padding=17) {
            Txt("🏔️  Manali trip",11,Muted)
            Row(verticalAlignment=Alignment.CenterVertically) { Txt("Riya",16,bold=true); Spacer(Modifier.width(6.dp)); Asset("9be34.svg",38f,12f); Spacer(Modifier.width(6.dp)); Txt("You",16,bold=true) }
            Txt("₹1,500",38,bold=true,display=true)
            Spacer(Modifier.height(12.dp)); DashedLine(); Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Txt("Settled",12,bold=true); Txt("Last week",11,Muted) }
        }
        Panel(Modifier.offset(x=48.dp,y=157.dp).width(241.dp).rotate(3f).shadow(18.dp,RoundedCornerShape(24.dp)),padding=17) {
            Txt("🏖️  Goa trip",11,Muted)
            Row(verticalAlignment=Alignment.CenterVertically) { Txt("You",16,bold=true); Spacer(Modifier.width(6.dp)); Asset("9c796.svg",38f,12f); Spacer(Modifier.width(6.dp)); Txt("Aman",16,bold=true) }
            Txt("₹2,000",38,bold=true,display=true)
            Spacer(Modifier.height(12.dp)); DashedLine(); Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Txt("Paid on UPI",12,bold=true); Txt("Just now",11,Muted) }
        }
        Box(Modifier.offset(x=252.dp,y=126.dp).size(77.dp).rotate(-12f).shadow(16.dp,RoundedCornerShape(21.dp)).background(Ink,RoundedCornerShape(21.dp)),contentAlignment=Alignment.Center) { Asset("dca1e.svg",33.292f,28.2f) }
    }
}

@Composable fun ChuktaApp(platform: Platform, nativeBack: @Composable ((Boolean, () -> Unit) -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val desk = remember { Desk(platform,scope) }
    val fonts = Fonts(FontFamily(Font(Res.font.jakarta,FontWeight.Normal),Font(Res.font.jakarta,FontWeight.Bold)),FontFamily(Font(Res.font.bricolage,FontWeight.Normal),Font(Res.font.bricolage,FontWeight.Bold)))
    CompositionLocalProvider(LocalFonts provides fonts) {
        MaterialTheme(colorScheme=lightColorScheme(primary=Ink,background=Paper,surface=Color.White,onSurface=Ink), typography=Typography(bodyLarge=TextStyle(fontFamily=fonts.body))) {
            nativeBack?.invoke(desk.screen !in listOf("home","login","splash")) { desk.back() }
            val yellow = desk.screen in listOf("splash","login","otp","verified","profile","paid","home")
            Box(Modifier.fillMaxSize().background(if(yellow) Haldi else Paper)) {
                if(desk.screen !in listOf("splash","paid"))Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(Paper))
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),contentAlignment=Alignment.TopCenter) {
                    Box(Modifier.widthIn(max=if(desk.screen=="trip") 900.dp else 600.dp).fillMaxSize()) {
                        AnimatedContent(desk.screen,transitionSpec={ (fadeIn(tween(200))+slideInHorizontally(tween(300)){it/12}) togetherWith (fadeOut(tween(150))) }, label="Chukta navigation") { screen ->
                            when(screen) {
                                "splash" -> Splash(desk)
                                "login","otp","verified","profile" -> Onboarding(desk,screen)
                                "home" -> Home(desk)
                                "create" -> CreateScreen(desk)
                                "trip" -> TripScreen(desk)
                                "expense" -> ExpenseScreen(desk)
                                "detail" -> DetailScreen(desk)
                                "paid" -> PaidScreen(desk)
                                "settings" -> SettingsScreen(desk)
                            }
                        }
                        if(desk.offline && desk.screen !in listOf("splash","login","otp","verified","profile")) {
                            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Soft).clickable { desk.action { desk.refresh() } }.padding(8.dp),horizontalArrangement=Arrangement.Center) {
                                Txt("Offline · ${desk.queue.size} saved · Tap to sync",12,bold=true)
                            }
                        }
                        if(desk.busy) LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth(),color=Ink)
                    }
                }
            }
            desk.error?.let { message -> AlertDialog(onDismissRequest={desk.error=null},title={Txt("Let's try again",22,bold=true,display=true)},text={Txt(message)},confirmButton={TextButton(onClick={desk.error=null}){Txt("OK",bold=true)}}) }
            desk.notice?.let { message -> AlertDialog(onDismissRequest={desk.notice=null},text={Txt(message)},confirmButton={TextButton(onClick={desk.notice=null}){Txt("OK",bold=true)}}) }
        }
    }
    LaunchedEffect(Unit) { while(true) { delay(1000); if(desk.resend>0) desk.resend-- } }
}
@Composable fun Splash(d: Desk) {
    val scale=remember { Animatable(1.12f) }; val lift=remember { Animatable(-14f) }; val reveal=remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f,spring(dampingRatio=0.45f,stiffness=450f))
        lift.animateTo(0f,tween(450)); reveal.animateTo(1f,tween(500)); delay(300)
        d.screen=if(d.api.token==null) "login" else if(d.user?.name.isNullOrEmpty()) "profile" else "home"
        if(d.api.token!=null) d.action { d.refresh() }
    }
    Box(Modifier.fillMaxSize().background(Haldi).clickable { if(d.api.token==null) d.screen="login" },contentAlignment=Alignment.Center) {
        Box(Modifier.size(176.dp).alpha((1-reveal.value)*0.22f).border(2.dp,Ink,RoundedCornerShape(50.dp)))
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Row(Modifier.scale(scale.value).offset(y=lift.value.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                val markWidth=128f-70f*reveal.value
                Asset("64195.svg",markWidth,markWidth*0.84706f)
                Box(Modifier.width((164f*reveal.value).dp).clipToBounds().alpha(reveal.value)) { Asset("25c0d.svg",164f,40f,description="Chukta") }
            }
            Spacer(Modifier.height(22.dp)); Txt("Hisaab saaf, dosti pakki.",16,bold=true,modifier=Modifier.alpha(reveal.value))
        }
    }
}
@Composable fun Step(number: Int, back: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom=22.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color.White,RoundedCornerShape(12.dp)).border(1.dp,Line,RoundedCornerShape(12.dp)).clickable(onClick=back),contentAlignment=Alignment.Center){Asset("4598b.svg",20f,description="Back")}
        Spacer(Modifier.width(16.dp))
        repeat(3) { index -> Spacer(Modifier.width(36.dp).height(4.dp).background(if(index<number) Ink else Ink.copy(alpha=0.14f),RoundedCornerShape(2.dp))); Spacer(Modifier.width(6.dp)) }
        Txt("Step $number of 3",12,Muted,true,modifier=Modifier.padding(start=6.dp))
    }
}
@Composable fun Onboarding(d: Desk, screen: String) {
    var focused by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(d.user?.name ?: "") }
    var photo by remember { mutableStateOf(d.platform.read("avatar")) }
    var inviteDialog by remember { mutableStateOf(d.platform.read("pendingInvite")!=null) }
    var policy by remember { mutableStateOf<String?>(null) }
    var connection by remember{mutableStateOf(false)}
    val sheetOffset=if(screen=="login" && !focused) 376.dp else 156.dp
    BoxWithConstraints(Modifier.fillMaxSize().background(Haldi)) {
        val available=maxHeight
        Column {
            Row(Modifier.fillMaxWidth().padding(start=24.dp,end=24.dp,top=14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                Brand()
                if(d.platform.development)Txt("API setup",11,bold=true,modifier=Modifier.clickable{connection=true}.padding(8.dp))
            }
            LoginArt()
        }
        Column(Modifier.fillMaxWidth().padding(top=sheetOffset).fillMaxHeight().clip(RoundedCornerShape(topStart=32.dp,topEnd=32.dp)).background(Paper)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
                if(screen!="login" || focused) Step(when(screen){"profile"->3;"otp","verified"->2;else->1}) { if(screen=="login") focused=false else d.back() }
                when(screen) {
                    "login" -> {
                        Txt("Hisaab saaf,\ndosti pakki.",32,bold=true,display=true)
                        Spacer(Modifier.height(8.dp)); Txt("Log in with your number. No passwords.",15,Muted)
                        Spacer(Modifier.height(20.dp)); Txt("Mobile number",13,bold=true)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().height(56.dp).background(Color.White,RoundedCornerShape(16.dp)).border(1.dp,if(focused) Ink else Line,RoundedCornerShape(16.dp)).clickable{focused=true}.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                            Txt("+91",17,bold=true); Spacer(Modifier.width(10.dp)); Asset("589ea.svg",16f); Spacer(Modifier.width(10.dp)); Spacer(Modifier.width(1.dp).height(24.dp).background(Line)); Spacer(Modifier.width(10.dp))
                            Txt(if(d.phone.isEmpty()) "10-digit number" else d.phone.take(5)+" "+d.phone.drop(5),17,if(d.phone.isEmpty()) Muted.copy(alpha=0.6f) else Ink,modifier=Modifier.weight(1f))
                            if(d.phone.length==10) Asset("1a280.svg",20f)
                        }
                        Spacer(Modifier.height(12.dp)); Primary(if(d.busy) "Sending…" else "Get OTP",d.phone.matches(Regex("[6-9][0-9]{9}")) && !d.busy,arrow=true){d.sendOtp()}
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth().clickable{inviteDialog=true},horizontalArrangement=Arrangement.Center) { Txt("Got an invite link? ",14,Muted); Text("See your share first",fontSize=14.sp,fontFamily=LocalFonts.current.body,textDecoration=TextDecoration.Underline,fontWeight=FontWeight.SemiBold) }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
                            Txt("By continuing you agree to ",11,Muted)
                            Txt("Terms",11,bold=true,modifier=Modifier.clickable{policy="Terms"})
                            Txt(" and ",11,Muted)
                            Txt("Privacy Policy.",11,bold=true,modifier=Modifier.clickable{policy="Privacy Policy"})
                        }
                    }
                    "otp","verified" -> {
                        Txt(if(screen=="verified") "Number verified" else "Enter the code",28,bold=true,display=true)
                        Spacer(Modifier.height(8.dp)); Txt("Sent to +91 ${d.phone.take(5)} ${d.phone.drop(5)}. Change",14,Muted,modifier=Modifier.clickable{d.screen="login"})
                        Spacer(Modifier.height(26.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            repeat(6) { i -> Box(Modifier.weight(1f).height(56.dp).background(if(screen=="verified") Color(0xFFE5F3E9) else Color.White,RoundedCornerShape(14.dp)).border(1.dp,if(i==code.length) Ink else Line,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) { Txt(code.getOrNull(i)?.toString() ?: "",24,bold=true) } }
                        }
                        Spacer(Modifier.height(18.dp))
                        if(screen=="verified") Row(verticalAlignment=Alignment.CenterVertically){Asset("1a280.svg",20f); Txt("  You're in!",14,Green,true)}
                        else Txt(if(d.resend>0) "Didn't get it? Resend in 0:${d.resend.toString().padStart(2,'0')}" else "Didn't get it? Resend code",13,Muted,modifier=Modifier.clickable(enabled=d.resend==0&&!d.busy){d.sendOtp();code=""})
                    }
                    "profile" -> {
                        Txt("What do friends\ncall you?",28,bold=true,display=true)
                        Spacer(Modifier.height(8.dp)); Txt("Shown on bills, tickets and settle-ups.",14,Muted)
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            if(photo==null) Avatar(name.ifEmpty{"K"},size=64) else AsyncImage(photo,"Your photo",Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)),contentScale=ContentScale.Crop)
                            Spacer(Modifier.width(14.dp)); Column(Modifier.clickable{d.platform.pickPhoto{if(it!=null){photo=it;d.platform.write("avatar",it)}}}) {
                                Row(verticalAlignment=Alignment.CenterVertically){Asset("12b4b.svg",18f);Txt("  Add a photo",15,bold=true)}
                                Txt("Optional · helps friends spot you",12,Muted)
                            }
                        }
                        Spacer(Modifier.height(26.dp)); Txt("Your name",13,bold=true); Spacer(Modifier.height(8.dp))
                        Field(name,{name=it.take(60)},"Your name"){if(name.isNotBlank())Asset("1a280.svg",20f)}
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth().background(Soft,RoundedCornerShape(16.dp)).padding(16.dp),verticalAlignment=Alignment.Top) {
                            Asset("b1750.svg",20f); Spacer(Modifier.width(12.dp)); Txt("We'll ask for your UPI ID only when a friend needs to pay you.",13,modifier=Modifier.weight(1f))
                        }
                    }
                }
            }
            if(screen=="profile") Box(Modifier.padding(horizontal=24.dp,vertical=16.dp)){Primary(if(d.busy)"Saving…" else "Continue",name.isNotBlank()&&!d.busy,true){d.profile(name)}}
            if((focused && screen=="login") || screen=="otp") {
                if(screen=="otp" && d.devCode!=null) Column(Modifier.fillMaxWidth().background(Color(0xFFECE5D9)).padding(top=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    SmallAction("Dev OTP · ${d.devCode}"){code=d.devCode!!;d.verify(code)}
                }
                Keypad(phone=true){key ->
                    if(screen=="login") d.phone=if(key=="⌫")d.phone.dropLast(1) else (d.phone+key).take(10)
                    else if(!d.busy) { code=if(key=="⌫")code.dropLast(1) else (code+key).take(6); if(code.length==6)d.verify(code) }
                }
            }
        }
    }
    if(inviteDialog) InviteDialog(d){inviteDialog=false}
    if(connection)ConnectionDialog(d){connection=false}
    policy?.let { label -> AlertDialog(onDismissRequest={policy=null},title={Txt(label,22,bold=true)},text={Txt("Chukta stores your name, mobile number, trip expenses and settlement records to run your shared ledger. Photos remain on your device. UPI payments happen in your UPI app. This development build uses a local API; published legal terms and a support contact must be configured before public release.")},confirmButton={TextButton(onClick={policy=null}){Txt("Close")}}) }
}
@Composable fun ConnectionDialog(d: Desk,dismiss:()->Unit) {
    var url by remember{mutableStateOf(d.platform.apiUrl)}
    AlertDialog(onDismissRequest=dismiss,title={Txt("Development API",22,bold=true)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Txt("On a real phone, enter the HTTPS backend URL or this computer's LAN URL. Changing servers clears this phone's session.",13,Muted)
        Field(url,{url=it},"https://api.your-domain.com")
    }},confirmButton={TextButton(enabled=url.startsWith("https://")||(d.platform.development&&url.startsWith("http://")),onClick={d.changeServer(url);dismiss()}){Txt("Connect",bold=true)}},dismissButton={TextButton(onClick=dismiss){Txt("Cancel")}})
}
