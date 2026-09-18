import json, urllib.request, urllib.parse, time, sys

BASE="http://127.0.0.1:8090"

def req(method,path,data=None,token=None):
    body=None if data is None else json.dumps(data).encode()
    headers={"Content-Type":"application/json"}
    if token: headers["Authorization"]=token
    r=urllib.request.Request(BASE+path,data=body,headers=headers,method=method)
    try:
        with urllib.request.urlopen(r,timeout=10) as resp:
            raw=resp.read().decode()
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw=e.read().decode()
        print(method,path,e.code,raw,file=sys.stderr)
        raise

for _ in range(40):
    try:
        s,d=req("GET","/api/health")
        if s==200: break
    except Exception: time.sleep(1)
else: raise SystemExit("PocketBase health endpoint did not start")

stamp=str(int(time.time()))
pw="BookBondTest!"+stamp
users=[]
for who in ("Alice","Bob"):
    email=f"{who.lower()}.{stamp}@example.test"
    _,rec=req("POST","/api/collections/users/records",{
        "email":email,"password":pw,"passwordConfirm":pw,
        "displayName":who,"age":25,"city":"Ostrava",
        "genres":"fantasy, krimi","books":"Narnie, Zaklinac",
        "bio":"Testovaci ctenar","avatarEmoji":"📚"
    })
    _,auth=req("POST","/api/collections/users/auth-with-password",{"identity":email,"password":pw})
    users.append((rec["id"],auth["token"]))

a,at=users[0]; b,bt=users[1]
req("POST","/api/collections/likes/records",{"fromUser":a,"toUser":b},at)
req("POST","/api/collections/likes/records",{"fromUser":b,"toUser":a},bt)
req("POST","/api/collections/messages/records",{"sender":a,"receiver":b,"body":"Ahoj, co prave ctes?"},at)
flt=urllib.parse.quote(f"(sender='{a}' && receiver='{b}') || (sender='{b}' && receiver='{a}')")
_,msgs=req("GET",f"/api/collections/messages/records?filter={flt}",token=bt)
assert any(x.get("body")=="Ahoj, co prave ctes?" for x in msgs["items"])
req("POST","/api/collections/posts/records",{"author":b,"kind":"recommendation","book":"Narnie","text":"Dnes doporucuji Narnii."},bt)
_,posts=req("GET","/api/collections/posts/records?expand=author",token=at)
assert any(x.get("book")=="Narnie" for x in posts["items"])
print("BOOKBOND_BACKEND_SMOKE_OK")
