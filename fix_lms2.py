content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_lms2 = '''

class LmsCourseRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"   # 1=1학기, 2=2학기


class LmsAttendRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"
    # 출결 조회 시 course_id 필요 (과목목록에서 추출)
    course_id: str = ""


def _lms_login(req_user_id: str, req_user_pw: str, session_cookies) -> requests.Session:
    """LMS 로그인 후 세션 반환"""
    import urllib3
    urllib3.disable_warnings()
    sess = requests.Session()
    sess.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language": "ko,en-US;q=0.9",
    })
    _restore_session_to(sess, session_cookies)
    # _language_, co_check 쿠키 추가 (LMS 필수)
    sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")

    sess.post(f"{LMS_URL}/ilos/lo/login.acl",
        data={"returnURL":"","challenge":"","response":"",
              "usr_id": req_user_id, "usr_pwd": req_user_pw},
        headers={"Content-Type":"application/x-www-form-urlencoded",
                 "Referer":f"{LMS_URL}/ilos/main/member/login_form.acl",
                 "Origin":LMS_URL},
        verify=False, timeout=15, allow_redirects=True)
    return sess


def _lms_headers(referer: str) -> dict:
    return {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "X-Requested-With": "XMLHttpRequest",
        "Referer": referer,
        "Origin": LMS_URL,
    }


@app.post("/api/lms/courses")
def lms_courses(req: LmsCourseRequest):
    """
    LMS 수강 과목 목록 조회
    course_register_list.acl(현재) + list2.acl(연도/학기별) + list_b.acl(비정규)
    """
    try:
        sess = _lms_login(req.user_id, req.user_pw, req.session_cookies)
        referer = f"{LMS_URL}/ilos/mp/course_register_list_form.acl"
        headers = _lms_headers(referer)

        # 현재 학기 과목
        r1 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
            data={"encoding": "utf-8"}, headers=headers, verify=False, timeout=10)

        # 특정 연도/학기 과목
        r2 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list2.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "2", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        # 비정규 과목
        r3 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list_b.acl",
            data={"YEAR": "", "TERM": "", "NON_TERM": "B", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        return JSONResponse(content={
            "success": True,
            "current": {"status": r1.status_code, "size": len(r1.content), "snippet": r1.text[:1000]},
            "by_term":  {"status": r2.status_code, "size": len(r2.content), "snippet": r2.text[:1000]},
            "non_term": {"status": r3.status_code, "size": len(r3.content), "snippet": r3.text[:500]},
        })
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/lms/attendance")
def lms_attendance(req: LmsAttendRequest):
    """
    LMS 출결 조회 (attendance_list.acl)
    course_id: lms/courses에서 추출한 과목 ID
    """
    try:
        sess = _lms_login(req.user_id, req.user_pw, req.session_cookies)
        referer = f"{LMS_URL}/ilos/st/course/attendance_list_form.acl"
        headers = _lms_headers(referer)

        payload = {"encoding": "utf-8"}
        if req.course_id:
            payload["course_id"] = req.course_id
        if req.year:
            payload["YEAR"] = req.year
        if req.term:
            payload["TERM"] = req.term

        resp = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
            data=payload, headers=headers, verify=False, timeout=15)

        return JSONResponse(content={
            "success": True,
            "status": resp.status_code,
            "size": len(resp.content),
            "content_type": resp.headers.get("Content-Type", ""),
            "snippet": resp.text[:2000],
        })
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
if 'lms_courses' not in content and old_main in content:
    content = content.replace(old_main, new_lms2 + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("ADDED LMS courses + attendance - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("Already exists or main not found")
