content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_endpoint = '''

# ── 출결 시스템 (check.jvision.ac.kr) ────────────────────────────

ATTEND_URL = "https://check.jvision.ac.kr"

@app.post("/api/attend/login")
async def attend_login(req: LoginRequest, db: DbSession = Depends(get_db)):
    """
    출결 시스템 로그인 (check.jvision.ac.kr/attend/iwin_sin)
    포털 SSO 세션 + 학번/비밀번호로 출결 시스템 JSESSIONID 취득
    반환: { success, jsessionid, cookies }
    """
    try:
        import urllib3, json, urllib.parse
        urllib3.disable_warnings()

        # 포털 세션 재사용 (iwin_sin은 EnviewSessionId 필요)
        sess = requests.Session()
        sess.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept-Language": "ko,en-US;q=0.9,en;q=0.8",
        })

        # 포털 로그인해서 EnviewSessionId 취득
        auth_url = "https://portal.jvision.ac.kr/user/loginAuth.face"
        sess.get(auth_url, params={"langKnd": "ko", "userId": req.user_id, "password": req.user_pw}, verify=False)
        sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
                  data={"langKnd": "ko", "userId": req.user_id, "username": req.user_id, "password": req.user_pw},
                  verify=False)

        portal_sid = sess.cookies.get('JSESSIONID', domain='portal.jvision.ac.kr')
        if portal_sid:
            sess.cookies.set('EnviewSessionId', portal_sid, domain='check.jvision.ac.kr', path='/')
            sess.cookies.set('EnviewLangKnd', 'ko', domain='check.jvision.ac.kr', path='/')

        # 출결 시스템 로그인
        ikey_json = json.dumps({"duser_id": req.user_id, "duser_pw": req.user_pw})
        attend_headers = {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest",
            "Referer": f"{ATTEND_URL}/jvision/student/",
            "Origin": ATTEND_URL,
        }
        resp = sess.post(
            f"{ATTEND_URL}/attend/iwin_sin",
            data={"ikey": ikey_json},
            headers=attend_headers,
            verify=False,
            allow_redirects=True
        )

        attend_jsessionid = sess.cookies.get('JSESSIONID', domain='check.jvision.ac.kr')
        attend_cookies = [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path}
                          for c in sess.cookies if 'check.jvision' in c.domain or 'jvision' in c.domain]

        success = resp.status_code in (200, 302) and attend_jsessionid is not None

        return JSONResponse(content={
            "success": success,
            "status": resp.status_code,
            "jsessionid": attend_jsessionid,
            "cookies": attend_cookies,
            "response_snippet": resp.text[:300] if resp.text else ""
        })
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
if 'attend_login' not in content and old_main in content:
    content = content.replace(old_main, new_endpoint + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("ADDED attend_login endpoint")
else:
    print("Already exists or main not found")
