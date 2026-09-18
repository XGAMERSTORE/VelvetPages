package cz.bookbond.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int CREAM = Color.rgb(251,246,239);
    private static final int PAPER = Color.rgb(255,253,249);
    private static final int WINE = Color.rgb(109,40,57);
    private static final int WINE_DARK = Color.rgb(74,24,38);
    private static final int ROSE = Color.rgb(201,130,142);
    private static final int GOLD = Color.rgb(185,140,74);
    private static final int INK = Color.rgb(45,37,39);
    private static final int MUTED = Color.rgb(123,109,112);
    private static final int LINE = Color.rgb(234,223,211);

    private ApiClient api;
    private LinearLayout root;
    private FrameLayout content;
    private final Handler handler = new Handler();
    private Runnable chatPoll;
    private String chatPeerId = "";
    private String chatPeerName = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(CREAM);
        w.setNavigationBarColor(CREAM);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        api = new ApiClient(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CREAM);
        setContentView(root);
        if (api.loggedIn()) renderMain("discover"); else renderLogin();
    }

    @Override protected void onDestroy() {
        stopChatPolling();
        super.onDestroy();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable bg(int color, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp((int)radius));
        if (strokeWidth > 0) d.setStroke(dp(strokeWidth), strokeColor);
        return d;
    }

    private TextView tv(String text, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setLineSpacing(0,1.08f);
        return t;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextColor(INK); e.setHintTextColor(Color.rgb(160,145,148)); e.setTextSize(16);
        e.setSingleLine(true); e.setPadding(dp(16),0,dp(16),0);
        e.setBackground(bg(PAPER,16,LINE,1));
        e.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54)));
        return e;
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false); b.setText(text); b.setTextSize(16); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : WINE);
        b.setBackground(bg(primary ? WINE : PAPER,18, primary ? WINE : LINE,1));
        b.setPadding(dp(14),0,dp(14),0);
        b.setMinHeight(dp(52));
        return b;
    }

    private void gap(LinearLayout l, int h) {
        Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); l.addView(s);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18),dp(18),dp(18),dp(18)); c.setBackground(bg(PAPER,24,LINE,1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0,0,0,dp(14)); c.setLayoutParams(p); return c;
    }

    private ScrollView scrollWith(LinearLayout body) {
        ScrollView s = new ScrollView(this); s.setFillViewport(true); s.addView(body); return s;
    }

    private LinearLayout paddedBody() {
        LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setPadding(dp(20),dp(18),dp(20),dp(28)); return b;
    }

    private void clear() { stopChatPolling(); root.removeAllViews(); }

    private void renderLogin() {
        clear();
        LinearLayout body = paddedBody(); body.setGravity(Gravity.CENTER_HORIZONTAL);
        gap(body,32);
        TextView mark = tv("♥  BOOKBOND  📖",18,WINE,true); mark.setLetterSpacing(.08f); body.addView(mark);
        gap(body,18);
        TextView h = tv("Potkej člověka,\nse kterým chceš dočíst kapitolu.",34,WINE_DARK,true); h.setGravity(Gravity.CENTER); body.addView(h);
        gap(body,10);
        TextView sub = tv("Knižní seznamka pro lidi, kteří raději mluví o příbězích než o small talku.",16,MUTED,false); sub.setGravity(Gravity.CENTER); body.addView(sub);
        gap(body,30);
        LinearLayout box = card();
        TextView bh = tv("Přihlášení",22,INK,true); box.addView(bh); gap(box,14);
        EditText email = field("E-mail"); email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); box.addView(email); gap(box,10);
        EditText pass = field("Heslo"); pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); box.addView(pass); gap(box,14);
        Button login = button("Vstoupit do BookBond",true); box.addView(login); gap(box,10);
        Button register = button("Vytvořit účet",false); box.addView(register); gap(box,12);
        TextView server = tv("Server: " + api.server(),12,MUTED,false); server.setGravity(Gravity.CENTER); server.setPadding(0,dp(8),0,dp(8)); box.addView(server);
        body.addView(box);
        TextView privacy = tv("18+ • skutečné účty a zprávy jsou ukládány na tvém serveru",12,MUTED,false); privacy.setGravity(Gravity.CENTER); body.addView(privacy);
        root.addView(scrollWith(body),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));

        server.setOnClickListener(v -> serverDialog(() -> renderLogin()));
        register.setOnClickListener(v -> renderRegister());
        login.setOnClickListener(v -> {
            String e=email.getText().toString(), p=pass.getText().toString();
            if(e.trim().isEmpty()||p.isEmpty()){toast("Vyplň e-mail a heslo.");return;}
            login.setEnabled(false); login.setText("Připojuji…");
            api.login(e,p,new ApiClient.Callback(){ public void ok(JSONObject d){renderMain("discover");} public void fail(String m){login.setEnabled(true);login.setText("Vstoupit do BookBond");error(m);} });
        });
    }

    private void renderRegister() {
        clear();
        LinearLayout b = paddedBody();
        TextView back = tv("‹ Přihlášení",15,WINE,true); back.setPadding(0,dp(8),0,dp(12)); b.addView(back);
        TextView h = tv("Tvůj čtenářský profil",30,WINE_DARK,true); b.addView(h); gap(b,6);
        b.addView(tv("Minimum informací, maximum témat k hovoru.",15,MUTED,false)); gap(b,20);
        EditText name=field("Jméno / přezdívka"); b.addView(name);gap(b,10);
        EditText age=field("Věk (18+)"); age.setInputType(InputType.TYPE_CLASS_NUMBER);b.addView(age);gap(b,10);
        EditText city=field("Město");b.addView(city);gap(b,10);
        EditText genres=field("Oblíbené žánry – fantasy, krimi…");b.addView(genres);gap(b,10);
        EditText books=field("Oblíbené knihy / série");b.addView(books);gap(b,10);
        EditText email=field("E-mail"); email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);b.addView(email);gap(b,10);
        EditText pass=field("Heslo – alespoň 8 znaků"); pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);b.addView(pass);gap(b,16);
        Button create=button("Vytvořit můj profil",true);b.addView(create);gap(b,10);
        TextView server=tv("Používám server: "+api.server(),12,MUTED,false);server.setGravity(Gravity.CENTER);server.setPadding(0,dp(8),0,dp(8));b.addView(server);
        root.addView(scrollWith(b),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        back.setOnClickListener(v->renderLogin()); server.setOnClickListener(v->serverDialog(this::renderRegister));
        create.setOnClickListener(v->{
            int a; try{a=Integer.parseInt(age.getText().toString());}catch(Exception ex){toast("Zadej věk.");return;}
            if(a<18){toast("BookBond je pouze pro 18+.");return;}
            if(name.getText().toString().trim().isEmpty()||email.getText().toString().trim().isEmpty()||pass.getText().length()<8){toast("Doplň jméno, e-mail a heslo alespoň 8 znaků.");return;}
            create.setEnabled(false);create.setText("Zakládám knihovnu…");
            api.register(email.getText().toString(),pass.getText().toString(),name.getText().toString(),a,city.getText().toString(),genres.getText().toString(),books.getText().toString(),new ApiClient.Callback(){
                public void ok(JSONObject d){renderMain("discover");}
                public void fail(String m){create.setEnabled(true);create.setText("Vytvořit můj profil");error(m);}
            });
        });
    }

    private void renderMain(String tab) {
        clear();
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(20),dp(12),dp(14),dp(10));
        TextView logo = tv("BookBond",25,WINE_DARK,true); top.addView(logo,new LinearLayout.LayoutParams(0,dp(50),1));
        TextView server = tv("● online",12,GOLD,true); server.setPadding(dp(12),dp(8),dp(12),dp(8)); server.setBackground(bg(PAPER,16,LINE,1)); top.addView(server);
        root.addView(top);
        content = new FrameLayout(this); root.addView(content,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        LinearLayout nav = new LinearLayout(this); nav.setPadding(dp(8),dp(7),dp(8),dp(8)); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(PAPER);
        addNav(nav,"⌕\nObjevuj","discover",tab); addNav(nav,"♡\nChaty","chats",tab); addNav(nav,"✦\nStopy","feed",tab); addNav(nav,"☻\nProfil","profile",tab);
        root.addView(nav,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(70)));
        server.setOnClickListener(v->serverDialog(()->renderMain(tab)));
        if(tab.equals("discover")) showDiscover(); else if(tab.equals("chats")) showChats(); else if(tab.equals("feed")) showFeed(); else showProfile();
    }

    private void addNav(LinearLayout nav,String label,String id,String active){
        TextView t=tv(label,12,id.equals(active)?WINE:MUTED,id.equals(active)); t.setGravity(Gravity.CENTER); t.setPadding(dp(4),0,dp(4),0);
        if(id.equals(active)) t.setBackground(bg(Color.rgb(249,236,232),16,Color.TRANSPARENT,0));
        t.setOnClickListener(v->renderMain(id)); nav.addView(t,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
    }

    private void showDiscover() {
        LinearLayout b=paddedBody(); content.addView(scrollWith(b));
        b.addView(tv("Lidé mezi stránkami",29,WINE_DARK,true)); gap(b,4); b.addView(tv("Najdi společný žánr. Zbytek už může napsat příběh.",15,MUTED,false)); gap(b,18);
        TextView loading=tv("Hledám čtenáře…",15,MUTED,false); b.addView(loading);
        api.likes(new ApiClient.Callback(){public void ok(JSONObject ld){
            Set<String> liked=new HashSet<>(); JSONArray li=ld.optJSONArray("items"); if(li!=null) for(int i=0;i<li.length();i++){JSONObject x=li.optJSONObject(i);if(x!=null&&api.userId().equals(x.optString("fromUser")))liked.add(x.optString("toUser"));}
            api.users(new ApiClient.Callback(){public void ok(JSONObject ud){
                b.removeView(loading); JSONArray a=ud.optJSONArray("items"); int shown=0;
                if(a!=null) for(int i=0;i<a.length();i++){JSONObject u=a.optJSONObject(i);if(u==null||liked.contains(u.optString("id")))continue; addDiscoverCard(b,u);shown++;}
                if(shown==0){LinearLayout c=card();c.addView(tv("Pro dnešek dočteno ✨",22,WINE,true));gap(c,6);c.addView(tv("Už jsi prošel/prošla všechny dostupné profily. Noví čtenáři se objeví automaticky.",14,MUTED,false));b.addView(c);}
            }public void fail(String m){b.removeView(loading);addErrorCard(b,m);}});
        }public void fail(String m){b.removeView(loading);addErrorCard(b,m);}});
    }

    private void addDiscoverCard(LinearLayout parent, JSONObject u){
        LinearLayout c=card();
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView av=tv(u.optString("avatarEmoji","📚"),32,WINE,true);av.setGravity(Gravity.CENTER);av.setBackground(bg(Color.rgb(249,236,232),28,Color.TRANSPARENT,0)); head.addView(av,new LinearLayout.LayoutParams(dp(62),dp(62)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(14),0,0,0);
        names.addView(tv(u.optString("displayName","Čtenář")+", "+u.optInt("age",18),22,INK,true)); names.addView(tv("📍 "+blank(u.optString("city"),"někde mezi knihami"),14,MUTED,false)); head.addView(names,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); c.addView(head);gap(c,14);
        String genres=blank(u.optString("genres"),"žánry zatím tajné"); TextView chip=tv("  "+genres+"  ",13,WINE,true);chip.setPadding(dp(7),dp(7),dp(7),dp(7));chip.setBackground(bg(Color.rgb(249,236,232),14,Color.TRANSPARENT,0));c.addView(chip);gap(c,12);
        c.addView(tv(blank(u.optString("bio"),"Řeknu ti to u první kapitoly."),15,INK,false));gap(c,12);
        TextView books=tv("Na poličce: "+blank(u.optString("books"),"zatím nic veřejného"),14,MUTED,false);c.addView(books);gap(c,14);
        LinearLayout actions=new LinearLayout(this);Button skip=button("Přeskočit",false);Button like=button("♥ Zaujalo mě",true);actions.addView(skip,new LinearLayout.LayoutParams(0,dp(52),1));Space sp=new Space(this);actions.addView(sp,new LinearLayout.LayoutParams(dp(10),1));actions.addView(like,new LinearLayout.LayoutParams(0,dp(52),1));c.addView(actions);
        skip.setOnClickListener(v->{c.setVisibility(View.GONE);});
        like.setOnClickListener(v->{like.setEnabled(false);like.setText("Ukládám ♥");api.like(u.optString("id"),new ApiClient.Callback(){public void ok(JSONObject d){toast("Uloženo. Pokud tě lajknul/a taky, objeví se v Chatech.");c.setVisibility(View.GONE);}public void fail(String m){like.setEnabled(true);like.setText("♥ Zaujalo mě");error(m);}});});
        parent.addView(c);
    }

    private void showChats(){
        LinearLayout b=paddedBody();content.addView(scrollWith(b));b.addView(tv("Vzájemné záložky",29,WINE_DARK,true));gap(b,4);b.addView(tv("Chat se otevře, když jste si dali like oba.",15,MUTED,false));gap(b,18);TextView load=tv("Kontroluji match…",15,MUTED,false);b.addView(load);
        api.likes(new ApiClient.Callback(){public void ok(JSONObject ld){Set<String> out=new HashSet<>(),in=new HashSet<>();JSONArray li=ld.optJSONArray("items");if(li!=null)for(int i=0;i<li.length();i++){JSONObject x=li.optJSONObject(i);if(x==null)continue;if(api.userId().equals(x.optString("fromUser")))out.add(x.optString("toUser"));if(api.userId().equals(x.optString("toUser")))in.add(x.optString("fromUser"));}
            api.users(new ApiClient.Callback(){public void ok(JSONObject ud){b.removeView(load);JSONArray a=ud.optJSONArray("items");int n=0;if(a!=null)for(int i=0;i<a.length();i++){JSONObject u=a.optJSONObject(i);if(u!=null&&out.contains(u.optString("id"))&&in.contains(u.optString("id"))){addMatchCard(b,u);n++;}}if(n==0){LinearLayout c=card();c.addView(tv("Ještě žádný vzájemný match",20,WINE,true));gap(c,6);c.addView(tv("Jakmile si dáte like oba, tady se objeví soukromý chat.",14,MUTED,false));b.addView(c);}}public void fail(String m){b.removeView(load);addErrorCard(b,m);}});
        }public void fail(String m){b.removeView(load);addErrorCard(b,m);}});
    }

    private void addMatchCard(LinearLayout b,JSONObject u){
        LinearLayout c=card();LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView av=tv(u.optString("avatarEmoji","📚"),27,WINE,true);av.setGravity(Gravity.CENTER);av.setBackground(bg(Color.rgb(249,236,232),24,Color.TRANSPARENT,0));r.addView(av,new LinearLayout.LayoutParams(dp(54),dp(54)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,dp(8),0);info.addView(tv(u.optString("displayName","Čtenář"),18,INK,true));info.addView(tv(blank(u.optString("genres"),"společný příběh čeká"),13,MUTED,false));r.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));Button chat=button("Napsat",true);r.addView(chat,new LinearLayout.LayoutParams(dp(100),dp(48)));c.addView(r);chat.setOnClickListener(v->openChat(u));b.addView(c);
    }

    private void openChat(JSONObject peer){
        stopChatPolling(); chatPeerId=peer.optString("id");chatPeerName=peer.optString("displayName","Čtenář");content.removeAllViews();LinearLayout whole=new LinearLayout(this);whole.setOrientation(LinearLayout.VERTICAL);whole.setPadding(dp(16),dp(10),dp(16),dp(8));content.addView(whole);
        LinearLayout hdr=new LinearLayout(this);hdr.setGravity(Gravity.CENTER_VERTICAL);TextView back=tv("‹",34,WINE,true);back.setGravity(Gravity.CENTER);hdr.addView(back,new LinearLayout.LayoutParams(dp(44),dp(50)));LinearLayout hi=new LinearLayout(this);hi.setOrientation(LinearLayout.VERTICAL);hi.addView(tv(chatPeerName,20,INK,true));hi.addView(tv("Vzájemný knižní match",12,MUTED,false));hdr.addView(hi,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));whole.addView(hdr);
        ScrollView scroll=new ScrollView(this);LinearLayout msgs=new LinearLayout(this);msgs.setOrientation(LinearLayout.VERTICAL);msgs.setPadding(dp(4),dp(12),dp(4),dp(12));scroll.addView(msgs);whole.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        LinearLayout sendRow=new LinearLayout(this);sendRow.setGravity(Gravity.CENTER_VERTICAL);EditText input=field("Napiš něco o knížce…");sendRow.addView(input,new LinearLayout.LayoutParams(0,dp(54),1));Space sp=new Space(this);sendRow.addView(sp,new LinearLayout.LayoutParams(dp(8),1));Button send=button("➤",true);sendRow.addView(send,new LinearLayout.LayoutParams(dp(62),dp(54)));whole.addView(sendRow);
        back.setOnClickListener(v->renderMain("chats"));
        Runnable load=()->api.messages(chatPeerId,new ApiClient.Callback(){public void ok(JSONObject d){msgs.removeAllViews();JSONArray a=d.optJSONArray("items");if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m!=null)addBubble(msgs,m);}scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}public void fail(String m){}});
        load.run();
        chatPoll=new Runnable(){public void run(){load.run();handler.postDelayed(this,3000);}};handler.postDelayed(chatPoll,3000);
        send.setOnClickListener(v->{String text=input.getText().toString().trim();if(text.isEmpty())return;send.setEnabled(false);api.sendMessage(chatPeerId,text,new ApiClient.Callback(){public void ok(JSONObject d){input.setText("");send.setEnabled(true);load.run();}public void fail(String m){send.setEnabled(true);error(m);}});});
    }

    private void addBubble(LinearLayout msgs,JSONObject m){boolean mine=api.userId().equals(m.optString("sender"));TextView t=tv(m.optString("body"),15,mine?Color.WHITE:INK,false);t.setPadding(dp(14),dp(10),dp(14),dp(10));t.setBackground(bg(mine?WINE:PAPER,18,mine?WINE:LINE,1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.73),ViewGroup.LayoutParams.WRAP_CONTENT);p.gravity=mine?Gravity.END:Gravity.START;p.setMargins(0,dp(4),0,dp(4));msgs.addView(t,p);}

    private void showFeed(){
        LinearLayout b=paddedBody();content.addView(scrollWith(b));b.addView(tv("Knižní stopy",29,WINE_DARK,true));gap(b,4);b.addView(tv("Co čteš, co doporučuješ, kam vyrážíš s knihou.",15,MUTED,false));gap(b,14);Button add=button("＋ Přidat stopu",true);b.addView(add,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));gap(b,18);TextView load=tv("Listuji komunitou…",15,MUTED,false);b.addView(load);add.setOnClickListener(v->newPostDialog(()->{content.removeAllViews();showFeed();}));
        api.posts(new ApiClient.Callback(){public void ok(JSONObject d){b.removeView(load);JSONArray a=d.optJSONArray("items");if(a==null||a.length()==0){LinearLayout c=card();c.addView(tv("První stránka je prázdná",20,WINE,true));gap(c,6);c.addView(tv("Přidej první knižní stopu pro ostatní.",14,MUTED,false));b.addView(c);return;}for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null)addPostCard(b,p);}}public void fail(String m){b.removeView(load);addErrorCard(b,m);}});
    }

    private void addPostCard(LinearLayout b,JSONObject p){LinearLayout c=card();JSONObject ex=p.optJSONObject("expand");JSONObject au=ex==null?null:ex.optJSONObject("author");String name=au==null?"Čtenář":au.optString("displayName","Čtenář");String avatar=au==null?"📚":au.optString("avatarEmoji","📚");String kind=p.optString("kind","status");String icon=kind.equals("recommendation")?"★":kind.equals("trail")?"⌁":"✦";TextView head=tv(avatar+"  "+name+"   "+icon,15,WINE,true);c.addView(head);gap(c,10);String book=p.optString("book","");if(!book.isEmpty()){TextView bk=tv("📖 "+book,13,GOLD,true);c.addView(bk);gap(c,8);}c.addView(tv(p.optString("text",""),16,INK,false));b.addView(c);}

    private void showProfile(){
        LinearLayout b=paddedBody();content.addView(scrollWith(b));JSONObject u=api.user();b.addView(tv("Můj profil",29,WINE_DARK,true));gap(b,4);b.addView(tv("Ať druhý člověk hned ví, na jaké stránce tě otevřít.",15,MUTED,false));gap(b,18);
        EditText avatar=field("Emoji avatar");avatar.setText(u.optString("avatarEmoji","📚"));b.addView(avatar);gap(b,10);EditText name=field("Jméno");name.setText(u.optString("displayName"));b.addView(name);gap(b,10);EditText age=field("Věk");age.setInputType(InputType.TYPE_CLASS_NUMBER);age.setText(String.valueOf(u.optInt("age",18)));b.addView(age);gap(b,10);EditText city=field("Město");city.setText(u.optString("city"));b.addView(city);gap(b,10);EditText genres=field("Žánry");genres.setText(u.optString("genres"));b.addView(genres);gap(b,10);EditText books=field("Oblíbené knihy");books.setText(u.optString("books"));b.addView(books);gap(b,10);EditText bio=field("Krátké bio");bio.setSingleLine(false);bio.setMinHeight(dp(90));bio.setGravity(Gravity.TOP);bio.setPadding(dp(16),dp(14),dp(16),dp(14));bio.setText(u.optString("bio"));b.addView(bio);gap(b,14);Button save=button("Uložit profil",true);b.addView(save,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));gap(b,10);Button server=button("Nastavení serveru",false);b.addView(server,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));gap(b,10);Button logout=button("Odhlásit se",false);b.addView(logout,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));
        save.setOnClickListener(v->{int a;try{a=Integer.parseInt(age.getText().toString());}catch(Exception e){toast("Neplatný věk.");return;}if(a<18){toast("BookBond je 18+.");return;}save.setEnabled(false);api.updateProfile(name.getText().toString(),a,city.getText().toString(),genres.getText().toString(),books.getText().toString(),bio.getText().toString(),avatar.getText().toString(),new ApiClient.Callback(){public void ok(JSONObject d){save.setEnabled(true);toast("Profil uložen ✨");}public void fail(String m){save.setEnabled(true);error(m);}});});
        server.setOnClickListener(v->serverDialog(()->renderMain("profile")));logout.setOnClickListener(v->{api.logout();renderLogin();});
    }

    private void newPostDialog(Runnable done){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(4),dp(20),0);EditText book=field("Kniha / série (volitelné)");box.addView(book);gap(box,10);EditText text=field("Co chceš sdílet?");text.setSingleLine(false);text.setMinHeight(dp(110));text.setGravity(Gravity.TOP);text.setPadding(dp(16),dp(14),dp(16),dp(14));box.addView(text);
        String[] kinds={"Právě čtu","Doporučení","Knižní výprava / trail"};new AlertDialog.Builder(this).setTitle("Nová knižní stopa").setSingleChoiceItems(kinds,0,null).setView(box).setNegativeButton("Zrušit",null).setPositiveButton("Publikovat",(d,w)->{
            AlertDialog ad=(AlertDialog)d;int checked=ad.getListView().getCheckedItemPosition();String kind=checked==1?"recommendation":checked==2?"trail":"status";if(text.getText().toString().trim().isEmpty()){toast("Napiš text příspěvku.");return;}api.post(kind,book.getText().toString(),text.getText().toString(),new ApiClient.Callback(){public void ok(JSONObject x){toast("Publikováno ✦");done.run();}public void fail(String m){error(m);}});
        }).show();
    }

    private void serverDialog(Runnable after){
        EditText input=field("https://api.tvoje-domena.cz");input.setText(api.server());LinearLayout w=new LinearLayout(this);w.setPadding(dp(18),dp(4),dp(18),0);w.addView(input,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("BookBond server").setMessage("Zadej veřejnou HTTPS adresu backendu. Pro lokální test může být např. http://192.168.1.20:8090").setView(w).setNegativeButton("Zrušit",null).setPositiveButton("Uložit",null).setNeutralButton("Test",null).create();
        dialog.setOnShowListener(x->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{api.setServer(input.getText().toString());api.logout();dialog.dismiss();after.run();});dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{api.setServer(input.getText().toString());api.ping(new ApiClient.Callback(){public void ok(JSONObject d){toast("Server odpovídá ✓");}public void fail(String m){error(m);}});});});dialog.show();
    }

    private void stopChatPolling(){if(chatPoll!=null){handler.removeCallbacks(chatPoll);chatPoll=null;}chatPeerId="";chatPeerName="";}
    private String blank(String s,String fallback){return s==null||s.trim().isEmpty()?fallback:s;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void error(String m){new AlertDialog.Builder(this).setTitle("BookBond").setMessage(m).setPositiveButton("OK",null).show();}
    private void addErrorCard(LinearLayout b,String m){LinearLayout c=card();c.addView(tv("Nepodařilo se připojit",20,WINE,true));gap(c,6);c.addView(tv(m,13,MUTED,false));gap(c,10);TextView hint=tv("Klepni na „● online“ nahoře a zkontroluj adresu serveru.",13,GOLD,true);c.addView(hint);b.addView(c);}
}
