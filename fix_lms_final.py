content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Replace the existing fetch_lms_all function with the complete working implementation
old_lms_all = '''def fetch_lms_all(req):
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
        "usr_pw": req.user_pw,
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
    }'''

new_lms_all = '''def _lms_full_login(user_id: str, user_pw: str) -> requests.Session:
    """
    LMS 완전 로그인 플로:
    1. 로그인 폼 GET (선행 필수)
    2. 로그인 POST (JS redirect → main_form.acl)
    3. main_form.acl GET (세션 완전 초기화)
    """
    import urllib3, re
    urllib3.disable_warnings()

    sess = requests.Session()
    sess.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
        "Accept-Language": "ko,en-US;q=0.9",
    })
    sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("EnviewLangKnd", "ko", domain="lms.jvision.ac.kr", path="/")

    # 1. 로그인 폼 GET (JSESSIONID 준비)
    sess.get(f"{LMS_URL}/ilos/main/member/login_form.acl", verify=False, timeout=10)

    # 2. 로그인 POST (JS redirect, HTTP redirect 비활성화)
    r_login = sess.post(f"{LMS_URL}/ilos/lo/login.acl",
        data={"returnURL": "", "challenge": "", "response": "",
              "usr_id": user_id, "usr_pwd": user_pw},
        headers={"Content-Type": "application/x-www-form-urlencoded",
                 "Referer": f"{LMS_URL}/ilos/main/member/login_form.acl",
                 "Origin": LMS_URL, "Upgrade-Insecure-Requests": "1"},
        verify=False, timeout=15, allow_redirects=False)

    # 3. JS redirect 추출 및 따라가기
    js_match = re.search(r"document\.location\.href=['\"]([^'\"]+)['\"]", r_login.text)
    if js_match:
        main_url = js_match.group(1)
        if not main_url.startswith("http"):
            main_url = LMS_URL + main_url
        sess.get(main_url, verify=False, timeout=15)

    return sess


def fetch_lms_all(req):
    """
    LMS 이러닝 출결 완전 플로:
    1. 완전 로그인 (로그인 폼 GET → POST → JS redirect 추적)
    2. 과목 목록 폼 GET (eclassRoom AJAX context 준비)
    3. 과목 목록 조회 (eclassRoom IDs 추출)
    4. 각 과목: eclass_room2.acl POST → submain GET → 출결 조회
    """
    import urllib3, re, json
    urllib3.disable_warnings()

    sess = _lms_full_login(req.user_id, req.user_pw)

    lms_jid = next((c.value for c in sess.cookies if c.name == 'JSESSIONID' and 'lms.jvision' in c.domain), None)
    if not lms_jid:
        return {"success": False, "message": "LMS 로그인 실패: JSESSIONID 없음"}

    # 과목 목록 폼 GET (eclassRoom 컨텍스트 준비 필수)
    sess.get(f"{LMS_URL}/ilos/mp/course_register_list_form.acl",
        headers={"Referer": f"{LMS_URL}/ilos/main/main_form.acl"}, verify=False, timeout=10)

    # 과목 목록 조회
    r_courses = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
        data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
        headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With": "XMLHttpRequest",
                 "Referer": f"{LMS_URL}/ilos/mp/course_register_list_form.acl", "Origin": LMS_URL},
        verify=False, timeout=10)

    # eclassRoom IDs 추출 (강의실이 열린 과목만)
    klass_ids = list(set(re.findall(r"eclassRoom\('([^']+)'\)", r_courses.text)))

    courses_attend = []
    for kjkey in klass_ids:
        # eclass_room2.acl AJAX 호출 (빠진 단계! 이게 없으면 submain 접근 불가)
        r_ec = sess.post(f"{LMS_URL}/ilos/st/course/eclass_room2.acl",
            data={"KJKEY": kjkey, "FLAG": "mp",
                  "returnURI": "/ilos/st/course/submain_form.acl", "encoding": "utf-8"},
            headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With": "XMLHttpRequest",
                     "Referer": f"{LMS_URL}/ilos/mp/course_register_list_form.acl",
                     "Origin": LMS_URL},
            verify=False, timeout=10)

        try:
            ec_json = r_ec.json()
            if ec_json.get("isError"):
                continue
            return_url = ec_json.get("returnURL", "/ilos/st/course/submain_form.acl")
        except Exception:
            continue

        # Submain 접근 (N-format ky 획득)
        r_sub = sess.get(f"{LMS_URL}{return_url}",
            headers={"Referer": f"{LMS_URL}/ilos/mp/course_register_list_form.acl",
                     "Sec-Fetch-Dest": "document", "Sec-Fetch-Mode": "navigate"},
            verify=False, timeout=10, allow_redirects=True)

        n_ky_vals = list(set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text)))

        # 각 ky에 대해 출결 조회
        for ky in n_ky_vals:
            sess.get(f"{LMS_URL}/ilos/st/course/attendance_list_form.acl",
                params={"ky": ky, "ud": req.user_id}, verify=False, timeout=10)
            r_attend = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
                data={"ud": req.user_id, "ky": ky, "encoding": "utf-8"},
                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With": "XMLHttpRequest",
                         "Referer": f"{LMS_URL}/ilos/st/course/attendance_list_form.acl?ky={ky}",
                         "Origin": LMS_URL},
                verify=False, timeout=15)
            courses_attend.append({
                "kjkey": kjkey,
                "ky": ky,
                "attend_html": r_attend.text[:5000] if len(r_attend.content) > 200 else None,
                "error": r_attend.text[:100] if len(r_attend.content) <= 200 else None,
            })

    return {
        "success": True,
        "lms_jsessionid": lms_jid,
        "year": req.year,
        "term": req.term,
        "klass_ids": klass_ids,
        "courses": courses_attend,
    }'''

if old_lms_all in content:
    content = content.replace(old_lms_all, new_lms_all)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("FIXED LMS all flow - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("Pattern not found")
    idx = content.find('def fetch_lms_all(')
    if idx >= 0:
        print(f"Found fetch_lms_all at {idx}")
        print(repr(content[idx:idx+200]))
