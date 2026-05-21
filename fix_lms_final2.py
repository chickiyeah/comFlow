import re

content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Find fetch_lms_all function and replace it
# Find start
start_idx = content.find('def fetch_lms_all(req):')
if start_idx < 0:
    print("fetch_lms_all NOT FOUND")
    exit()

# Find end - the next def/class or @app at module level (0 indentation)
after = content[start_idx + 20:]
end_match = re.search(r'\n(def |class |@app\.)', after)
if end_match:
    end_idx = start_idx + 20 + end_match.start() + 1
else:
    end_idx = len(content)

print(f"Found fetch_lms_all at {start_idx}:{end_idx}")

new_impl = r'''def _lms_full_login(user_id, user_pw):
    """LMS 완전 로그인: 폼 GET → POST → JS redirect 추적"""
    import re, urllib3
    urllib3.disable_warnings()
    sess = requests.Session()
    sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
    sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("EnviewLangKnd", "ko", domain="lms.jvision.ac.kr", path="/")
    sess.get(f"{LMS_URL}/ilos/main/member/login_form.acl", verify=False, timeout=10)
    r_login = sess.post(f"{LMS_URL}/ilos/lo/login.acl",
        data={"returnURL":"","challenge":"","response":"","usr_id":user_id,"usr_pwd":user_pw},
        headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS_URL}/ilos/main/member/login_form.acl","Origin":LMS_URL},
        verify=False, timeout=15, allow_redirects=False)
    js_match = re.search(r"document\.location\.href=['\"]([^'\"]+)['\"]", r_login.text)
    if js_match:
        main_url = js_match.group(1)
        if not main_url.startswith("http"):
            main_url = LMS_URL + main_url
        sess.get(main_url, verify=False, timeout=15)
    return sess


def fetch_lms_all(req):
    """
    LMS 출결 완전 플로:
    1. 완전 로그인 (폼 GET → POST → JS redirect)
    2. 과목 폼 GET (eclassRoom 컨텍스트 준비)
    3. 과목 목록 (eclassRoom ID 추출)
    4. 각 과목: eclass_room2.acl → submain → 출결
    """
    import re, urllib3
    urllib3.disable_warnings()

    sess = _lms_full_login(req.user_id, req.user_pw)
    lms_jid = next((c.value for c in sess.cookies if c.name == 'JSESSIONID' and 'lms.jvision' in c.domain), None)
    if not lms_jid:
        return {"success": False, "message": "LMS JSESSIONID 없음"}

    sess.get(f"{LMS_URL}/ilos/mp/course_register_list_form.acl",
        headers={"Referer": f"{LMS_URL}/ilos/main/main_form.acl"}, verify=False, timeout=10)

    r_courses = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
        data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl","Origin":LMS_URL},
        verify=False, timeout=10)

    klass_ids = list(set(re.findall(r"eclassRoom\('([^']+)'\)", r_courses.text)))
    courses_attend = []

    for kjkey in klass_ids:
        r_ec = sess.post(f"{LMS_URL}/ilos/st/course/eclass_room2.acl",
            data={"KJKEY":kjkey,"FLAG":"mp","returnURI":"/ilos/st/course/submain_form.acl","encoding":"utf-8"},
            headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With":"XMLHttpRequest",
                     "Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl","Origin":LMS_URL},
            verify=False, timeout=10)
        try:
            ec = r_ec.json()
            if ec.get("isError"):
                continue
            return_url = ec.get("returnURL", "/ilos/st/course/submain_form.acl")
        except Exception:
            continue

        r_sub = sess.get(f"{LMS_URL}{return_url}",
            headers={"Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl"},
            verify=False, timeout=10, allow_redirects=True)

        n_ky_vals = list(set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text)))

        for ky in n_ky_vals:
            sess.get(f"{LMS_URL}/ilos/st/course/attendance_list_form.acl",
                params={"ky":ky,"ud":req.user_id}, verify=False, timeout=10)
            r_at = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
                data={"ud":req.user_id,"ky":ky,"encoding":"utf-8"},
                headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With":"XMLHttpRequest",
                         "Referer":f"{LMS_URL}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS_URL},
                verify=False, timeout=15)
            courses_attend.append({
                "kjkey": kjkey,
                "ky": ky,
                "success": len(r_at.content) > 200,
                "html": r_at.text[:5000] if len(r_at.content) > 200 else None,
                "error": r_at.text[:100] if len(r_at.content) <= 200 else None,
            })

    return {"success": True, "year": req.year, "term": req.term,
            "klass_ids": klass_ids, "courses": courses_attend}

'''

content = content[:start_idx] + new_impl + content[end_idx:]
open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)

import py_compile
try:
    py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
    print("FIXED - SYNTAX OK")
except py_compile.PyCompileError as e:
    print(f"ERROR: {e}")
