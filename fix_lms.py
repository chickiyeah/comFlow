content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_lms = '''

# ── LMS 이러닝 (lms.jvision.ac.kr) ──────────────────────────────

LMS_URL = "https://lms.jvision.ac.kr"


def fetch_lms_all(req):
    """
    LMS 이러닝 로그인 + 전체 수강 과목 출결 조회
    is_interlock=1 인 과목의 실제 출결 데이터 소스
    """
    import urllib3, json
    urllib3.disable_warnings()

    sess = requests.Session()
    sess.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language": "ko,en-US;q=0.9",
    })

    # 저장된 포털 쿠키로 세션 복원 (EnviewSessionId가 lms.jvision.ac.kr SSO에 사용됨)
    _restore_session_to(sess, req.session_cookies)

    # LMS 로그인
    login_data = {
        "returnURL": "",
        "challenge": "",
        "response": "",
        "usr_id": req.user_id,
        "usr_pwd": req.user_pw,
    }
    login_resp = sess.post(
        f"{LMS_URL}/ilos/lo/login.acl",
        data=login_data,
        headers={
            "Content-Type": "application/x-www-form-urlencoded",
            "Referer": f"{LMS_URL}/ilos/main/member/login_form.acl",
            "Origin": LMS_URL,
        },
        verify=False, timeout=15, allow_redirects=True
    )

    lms_jid = sess.cookies.get("JSESSIONID", domain="lms.jvision.ac.kr")
    login_success = login_resp.status_code in (200, 302) and lms_jid is not None

    if not login_success:
        return {"success": False, "message": "LMS 로그인 실패", "status": login_resp.status_code}

    # 내 수강 목록 조회
    course_resp = sess.post(
        f"{LMS_URL}/ilos/st/course/eclass_list_form.acl",
        data={"start": "1", "display": "20", "OPEN_DTM": "", "close_YN": "N"},
        headers={
            "Content-Type": "application/x-www-form-urlencoded",
            "X-Requested-With": "XMLHttpRequest",
            "Referer": f"{LMS_URL}/ilos/st/course/eclass_list_form.acl",
            "Origin": LMS_URL,
        },
        verify=False, timeout=10
    )

    # 출결 개요 페이지 (대부분의 LMS 시스템)
    attend_resp = sess.post(
        f"{LMS_URL}/ilos/st/attend/attend_list_form.acl",
        data={},
        headers={
            "Content-Type": "application/x-www-form-urlencoded",
            "X-Requested-With": "XMLHttpRequest",
            "Referer": f"{LMS_URL}/ilos/st/attend/attend_list_form.acl",
            "Origin": LMS_URL,
        },
        verify=False, timeout=10
    )

    return {
        "success": True,
        "lms_jsessionid": lms_jid,
        "login_url": login_resp.url,
        "course_status": course_resp.status_code,
        "course_size": len(course_resp.content),
        "attend_status": attend_resp.status_code,
        "attend_size": len(attend_resp.content),
        "attend_snippet": attend_resp.text[:500] if attend_resp.text else "",
    }


@app.post("/api/lms/login")
def lms_login(req: AttendRequest):
    """LMS 로그인 + 초기 데이터 탐색"""
    try:
        result = fetch_lms_all(req)
        return JSONResponse(content=result)
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
if 'lms_login' not in content and old_main in content:
    content = content.replace(old_main, new_lms + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)

    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("ADDED LMS endpoints - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"SYNTAX ERROR: {e}")
else:
    print("LMS already exists or main not found")
