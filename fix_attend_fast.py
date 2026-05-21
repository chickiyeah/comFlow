content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

old_all = '''@app.post("/api/attend/all")
async def fetch_all_attendance(req: LoginRequest, db: DbSession = Depends(get_db)):'''

new_all = '''class AttendAllRequest(BaseModel):
    """전체 출결 조회 — 저장된 포털 쿠키로 attend 로그인 + 병렬 과목 조회"""
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]  # 포털 sync 때 저장한 세션 쿠키


@app.post("/api/attend/all")
async def fetch_all_attendance(req: AttendAllRequest):'''

if old_all in content:
    content = content.replace(old_all, new_all, 1)
    # Also replace the inner logic: replace portal login with stored cookies
    old_login_block = '''        # 포털 SSO 로그인
        sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
                 params={"langKnd":"ko","userId":req.user_id,"password":req.user_pw},
                 verify=False, timeout=10)
        sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
                  data={"langKnd":"ko","userId":req.user_id,"username":req.user_id,"password":req.user_pw},
                  verify=False, timeout=10)'''

    new_login_block = '''        # 저장된 포털 쿠키로 세션 복원 (재로그인 불필요)
        _restore_session_to(sess, req.session_cookies)'''

    if old_login_block in content:
        content = content.replace(old_login_block, new_login_block, 1)
        print("Replaced portal login with stored cookies")

    # Replace sequential loop with parallel version
    old_loop = '''        # 과목별 출결 조회
        courses_attend = []
        attend_headers = {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest",
            "Origin": ATTEND_URL,
        }

        for course in rollbook:
            sugang_code = course.get("sugang_code", "")
            sugang_name = course.get("sugang_name", "")
            sugang_num  = course.get("sugang_semester_num", "")
            if not sugang_code:
                continue

            attend_headers["Referer"] = (
                f"{ATTEND_URL}/jvision/student/01_go_win_classtime.html"
                f"?sugang_code={sugang_code}&sugang_semester_num={sugang_num}"
                f"&title=%EA%B3%BC%EB%AA%A9%EB%B3%84%EC%B6%9C%EC%84%9D%ED%98%84%ED%99%A9"
                f"&userid={req.user_id}"
            )
            chul_ikey = json.dumps({"dclass": sugang_code, "duser_id": req.user_id})
            try:
                chul_resp = sess.post(
                    f"{ATTEND_URL}/attend/iwin_st_chulseokbu",
                    data={"ikey": chul_ikey},
                    headers=attend_headers,
                    verify=False, timeout=10
                )
                chul_data = chul_resp.json()
            except Exception:
                chul_data = {}

            courses_attend.append({
                "sugang_code":  sugang_code,
                "sugang_name":  sugang_name,
                "professor":    course.get("professor_name", ""),
                "week":         course.get("sugang_week", ""),
                "starttime":    course.get("sugang_starttime", ""),
                "endtime":      course.get("sugang_endtime", ""),
                "total_classes":course.get("sugang_total_number", 0),
                "attend_raw":   chul_data,
            })'''

    new_loop = '''        # 과목별 출결 병렬 조회 (ThreadPoolExecutor)
        from concurrent.futures import ThreadPoolExecutor, as_completed

        def fetch_one_course(course):
            sugang_code = course.get("sugang_code", "")
            sugang_num  = course.get("sugang_semester_num", "")
            if not sugang_code:
                return None
            try:
                s = requests.Session()
                for c in sess.cookies:
                    s.cookies.set(c.name, c.value, domain=c.domain, path=c.path)
                chul_ikey = json.dumps({"dclass": sugang_code, "duser_id": req.user_id})
                r = s.post(
                    f"{ATTEND_URL}/attend/iwin_st_chulseokbu",
                    data={"ikey": chul_ikey},
                    headers={
                        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                        "X-Requested-With": "XMLHttpRequest",
                        "Origin": ATTEND_URL,
                        "Referer": (f"{ATTEND_URL}/jvision/student/01_go_win_classtime.html"
                                    f"?sugang_code={sugang_code}&sugang_semester_num={sugang_num}"
                                    f"&userid={req.user_id}"),
                    },
                    verify=False, timeout=8
                )
                chul_data = r.json()
            except Exception:
                chul_data = {}
            return {
                "sugang_code":  sugang_code,
                "sugang_name":  course.get("sugang_name", ""),
                "professor":    course.get("professor_name", ""),
                "week":         course.get("sugang_week", ""),
                "starttime":    course.get("sugang_starttime", ""),
                "endtime":      course.get("sugang_endtime", ""),
                "total_classes":course.get("sugang_total_number", 0),
                "is_interlock": course.get("is_interlock", 0),
                "attend_raw":   chul_data,
            }

        courses_attend = []
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = {executor.submit(fetch_one_course, c): c for c in rollbook if c.get("sugang_code")}
            for fut in as_completed(futures):
                result = fut.result()
                if result:
                    courses_attend.append(result)
        courses_attend.sort(key=lambda x: x.get("week", 0))'''

    if old_loop in content:
        content = content.replace(old_loop, new_loop, 1)
        print("Replaced sequential loop with parallel version")
    else:
        print("Loop NOT FOUND - checking...")

    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("SAVED attend/all updates")
else:
    print("old_all NOT FOUND")
