content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_endpoint = '''

@app.post("/api/attend/all")
async def fetch_all_attendance(req: LoginRequest, db: DbSession = Depends(get_db)):
    """
    전체 출결 조회 (로그인 → rollbook 파싱 → 모든 과목 iwin_st_chulseokbu 호출)
    반환: { success, semester, courses: [{sugang_name, attend_list}] }
    """
    try:
        import urllib3, json
        urllib3.disable_warnings()

        sess = requests.Session()
        common_headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept-Language": "ko,en-US;q=0.9",
        }
        sess.headers.update(common_headers)

        # 포털 SSO 로그인
        sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
                 params={"langKnd":"ko","userId":req.user_id,"password":req.user_pw},
                 verify=False, timeout=10)
        sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
                  data={"langKnd":"ko","userId":req.user_id,"username":req.user_id,"password":req.user_pw},
                  verify=False, timeout=10)

        # 출결 시스템 로그인
        ikey_json = json.dumps({"duser_id": req.user_id, "duser_pw": req.user_pw})
        login_resp = sess.post(
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

        login_data = login_resp.json()
        if login_data.get("xidedu", {}).get("xmsg") != "Ok":
            return JSONResponse(content={"success": False, "message": "출결 시스템 로그인 실패"})

        attend_jid = sess.cookies.get("JSESSIONID", domain="check.jvision.ac.kr")
        rollbook = login_data.get("rollbook", [])
        rollbooksang = login_data.get("rollbooksang", {})

        # 과목별 출결 조회
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
            })

        return JSONResponse(content={
            "success": True,
            "semester": {
                "year": rollbooksang.get("sugang_year"),
                "semester": rollbooksang.get("sugang_semester"),
                "start_day": rollbooksang.get("sugang_start_day"),
                "end_day": rollbooksang.get("sugang_end_day"),
            },
            "courses": courses_attend,
            "user": login_data.get("xuser", {}),
        })

    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/attend/course")
async def fetch_course_attendance(req: AttendRequest, sugang_code: str = ""):
    """
    단일 과목 출결 조회 (iwin_st_chulseokbu)
    저장된 attend JSESSIONID 재사용
    """
    try:
        import urllib3, json
        urllib3.disable_warnings()

        sess = requests.Session()
        _restore_session_to(sess, req.session_cookies)

        ikey_json = json.dumps({"dclass": sugang_code, "duser_id": req.user_id})
        resp = sess.post(
            f"{ATTEND_URL}/attend/iwin_st_chulseokbu",
            data={"ikey": ikey_json},
            headers={
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest",
                "Referer": f"{ATTEND_URL}/jvision/student/",
                "Origin": ATTEND_URL,
            },
            verify=False, timeout=10
        )
        return JSONResponse(content={"success": True, "data": resp.json()})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
if 'fetch_all_attendance' not in content and old_main in content:
    content = content.replace(old_main, new_endpoint + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("ADDED attend/all and attend/course endpoints")
else:
    print("Already exists or main not found")
