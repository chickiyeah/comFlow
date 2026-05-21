content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Fix: remove the old attend endpoint and add a proper one that uses stored session
# The key insight: .jvision.ac.kr wildcard cookies work, no need to manually seed

old_attend = '''class AttendRequest(BaseModel):
    """출결 시스템 로그인 — 저장된 포털 세션 + 학번/비밀번호"""
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]  # 포털 sync 때 저장한 세션 쿠키


@app.post("/api/attend/login")
async def attend_login(req: AttendRequest):'''

new_attend = '''class AttendRequest(BaseModel):
    """출결 시스템 로그인 — 저장된 포털 세션 + 학번/비밀번호"""
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]  # 포털 sync 때 저장한 세션 쿠키


@app.post("/api/attend/login")
async def attend_login(req: AttendRequest):
    """
    출결 시스템 로그인 (check.jvision.ac.kr/attend/iwin_sin)
    저장된 포털 쿠키(.jvision.ac.kr 와일드카드)를 그대로 재사용
    """
    try:
        import urllib3, json
        urllib3.disable_warnings()

        sess = requests.Session()
        sess.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept-Language": "ko,en-US;q=0.9",
        })

        # 저장된 모든 포털 쿠키 복원 (.jvision.ac.kr 와일드카드가 check.jvision.ac.kr에 적용됨)
        _restore_session_to(sess, req.session_cookies)

        # 출결 로그인
        ikey_json = json.dumps({"duser_id": req.user_id, "duser_pw": req.user_pw})
        resp = sess.post(
            f"{ATTEND_URL}/attend/iwin_sin",
            data={"ikey": ikey_json},
            headers={
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest",
                "Referer": f"{ATTEND_URL}/jvision/student/",
                "Origin": ATTEND_URL,
            },
            verify=False, timeout=15, allow_redirects=True
        )

        # check.jvision.ac.kr JSESSIONID 취득
        attend_jid = sess.cookies.get("JSESSIONID", domain="check.jvision.ac.kr")
        attend_cookies = [
            {"name": c.name, "value": c.value, "domain": c.domain, "path": c.path}
            for c in sess.cookies
        ]

        try:
            result = resp.json()
            success = result.get("xidedu", {}).get("xmsg") == "Ok"
        except Exception:
            success = resp.status_code == 200 and attend_jid is not None
            result = {}

        return JSONResponse(content={
            "success": success,
            "jsessionid": attend_jid,
            "user_info": result.get("xuser", {}),
            "all_cookies": attend_cookies,
        })
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

# placeholder - will be replaced
def _bogus_placeholder_():
    pass'''

# Find the boundary of the old attend function
start_idx = content.find(old_attend)
if start_idx < 0:
    print("Could not find old attend")
    exit()

# Find the next @app.post or end of old_attend function
# The function ends before the next @app decorator
search_from = start_idx + len(old_attend)
next_app = content.find('\n@app.post("/api/', search_from)
if next_app < 0:
    next_app = content.find('\nif __name__', search_from)

old_func_body = content[start_idx:next_app]
print(f"Replacing {len(old_func_body)} chars of old attend function")

content = content[:start_idx] + new_attend + '\n\n' + content[next_app:]
# Remove the bogus placeholder
content = content.replace('''
# placeholder - will be replaced
def _bogus_placeholder_():
    pass''', '')

# Also add _restore_session_to helper if not exists
if '_restore_session_to' not in content:
    helper = '''
def _restore_session_to(sess: requests.Session, session_cookies) -> None:
    """저장된 쿠키를 requests.Session에 복원 (리스트/딕트 포맷 모두 지원)"""
    if isinstance(session_cookies, list):
        for c in session_cookies:
            sess.cookies.set(c["name"], c["value"],
                             domain=c.get("domain", ""),
                             path=c.get("path", "/"))
    else:
        sess.cookies.update(session_cookies)


'''
    # Insert before _restore_session
    idx = content.find('def _restore_session(')
    if idx > 0:
        content = content[:idx] + helper + content[idx:]
        print("Added _restore_session_to helper")

open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
print("FIXED attend_login")
