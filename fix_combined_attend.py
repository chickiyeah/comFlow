content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_endpoint = '''

# ── 통합 출결 엔드포인트 ──────────────────────────────────────────

def _parse_lms_attend(html: str) -> dict:
    """LMS 출결 HTML → {present, absent, late, not_checked, has_data}"""
    import re
    # JS override 값 우선 ("#div_1").html("N") 또는 <span id="div_1">N</span>
    def extract(pattern, text):
        m = re.search(pattern, text)
        return int(m.group(1)) if m else 0
    present   = extract(r'div_1["\)]+\.html\(["\'](\d+)', html) or extract(r'id="div_1">(\d+)', html)
    absent    = extract(r'div_2["\)]+\.html\(["\'](\d+)', html) or extract(r'id="div_2">(\d+)', html)
    late      = extract(r'div_3["\)]+\.html\(["\'](\d+)', html) or extract(r'id="div_3">(\d+)', html)
    not_chk   = extract(r'div_4["\)]+\.html\(["\'](\d+)', html) or extract(r'id="div_4">(\d+)', html)
    total = present + absent + late + not_chk
    has_data = total > 0 or '조회된 자료' not in html
    return {"present": present, "absent": absent, "late": late,
            "not_checked": not_chk, "total": total, "has_data": has_data}


def _parse_check_attend(attend_raw) -> dict:
    """check.jvision 출결 raw (JSON/dict) → {present, absent, late, ...}"""
    if not attend_raw or not isinstance(attend_raw, dict):
        return {"present": 0, "absent": 0, "late": 0, "not_checked": 0, "total": 0}
    # iwin_st_chulseokbu response structure varies — try common keys
    def get_int(d, *keys):
        for k in keys:
            v = d.get(k, d.get(k.upper(), d.get(k.lower(), None)))
            if v is not None:
                try: return int(v)
                except: pass
        return 0
    return {
        "present":     get_int(attend_raw, "cnt_attend", "ATTEND", "출석"),
        "absent":      get_int(attend_raw, "cnt_absence", "ABSENCE", "결석"),
        "late":        get_int(attend_raw, "cnt_late", "LATE", "지각"),
        "not_checked": get_int(attend_raw, "cnt_nocheck", "NOCHECK", "미체크"),
        "total":       get_int(attend_raw, "total", "TOTAL"),
    }


@app.post("/api/attendance/combined")
def combined_attendance(req: LmsCourseRequest):
    """
    통합 출결 조회: check.jvision(a) + LMS(b) = 전체 출결
    - is_interlock=0: check.jvision iwin_st_chulseokbu
    - is_interlock=1: LMS eclass_room2 → attendance_list
    반환: [{course_name, source, present, absent, late, not_checked, total}]
    """
    import re, json, urllib3
    from concurrent.futures import ThreadPoolExecutor, as_completed
    urllib3.disable_warnings()
    results = []

    # ── A. check.jvision 출결 ─────────────────────────────────────────
    try:
        # 포털 로그인 → 출결 시스템 로그인
        sess_chk = requests.Session()
        _restore_session_to(sess_chk, req.session_cookies)
        sess_chk.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

        # 포털 쿠키 재사용으로 빠른 SSO 로그인 시도
        chk_url = "https://check.jvision.ac.kr"
        ikey_json = json.dumps({"duser_id": req.user_id, "duser_pw": req.user_pw})
        login_r = sess_chk.post(f"{chk_url}/attend/iwin_sin",
            data={"ikey": ikey_json},
            headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With": "XMLHttpRequest",
                     "Referer": f"{chk_url}/jvision/student/", "Origin": chk_url},
            verify=False, timeout=20, allow_redirects=True)

        login_data = login_r.json()
        rollbook = login_data.get("rollbook", [])

        def fetch_check_course(course):
            sc = course.get("sugang_code", "")
            if not sc or course.get("is_interlock", 1) == 1:
                return None  # LMS 전담
            s2 = requests.Session()
            for c in sess_chk.cookies:
                s2.cookies.set(c.name, c.value, domain=c.domain, path=c.path)
            chul_r = s2.post(f"{chk_url}/attend/iwin_st_chulseokbu",
                data={"ikey": json.dumps({"dclass": sc, "duser_id": req.user_id})},
                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With": "XMLHttpRequest",
                         "Referer": f"{chk_url}/jvision/student/", "Origin": chk_url},
                verify=False, timeout=8)
            try:
                raw = chul_r.json()
            except Exception:
                raw = {}
            parsed = _parse_check_attend(raw)
            return {
                "course_name": course.get("sugang_name", sc),
                "source": "check",
                "sugang_code": sc,
                "total_classes": course.get("sugang_total_number", 0),
                **parsed,
            }

        with ThreadPoolExecutor(max_workers=6) as ex:
            futs = [ex.submit(fetch_check_course, c) for c in rollbook]
            for f in as_completed(futs):
                r = f.result()
                if r:
                    results.append(r)

    except Exception as e:
        results.append({"error": f"check.jvision 오류: {str(e)}", "source": "check"})

    # ── B. LMS 출결 ────────────────────────────────────────────────────
    try:
        sess_lms = _lms_full_login(req.user_id, req.user_pw)
        lms_url = LMS_URL

        sess_lms.get(f"{lms_url}/ilos/mp/course_register_list_form.acl",
            headers={"Referer": f"{lms_url}/ilos/main/main_form.acl"}, verify=False, timeout=10)
        r_c = sess_lms.post(f"{lms_url}/ilos/mp/course_register_list.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
            headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With": "XMLHttpRequest",
                     "Referer": f"{lms_url}/ilos/mp/course_register_list_form.acl", "Origin": lms_url},
            verify=False, timeout=10)

        klass_ids = list(set(re.findall(r"eclassRoom\('([^']+)'\)", r_c.text)))

        for kjkey in klass_ids:
            r_ec = sess_lms.post(f"{lms_url}/ilos/st/course/eclass_room2.acl",
                data={"KJKEY": kjkey, "FLAG": "mp",
                      "returnURI": "/ilos/st/course/submain_form.acl", "encoding": "utf-8"},
                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With": "XMLHttpRequest",
                         "Referer": f"{lms_url}/ilos/mp/course_register_list_form.acl",
                         "Origin": lms_url},
                verify=False, timeout=10)
            try:
                ec = r_ec.json()
                if ec.get("isError"): continue
                return_url = ec.get("returnURL", "/ilos/st/course/submain_form.acl")
            except Exception:
                continue

            sess_lms.get(f"{lms_url}{return_url}",
                headers={"Referer": f"{lms_url}/ilos/mp/course_register_list_form.acl"},
                verify=False, timeout=10)
            sess_lms.get(f"{lms_url}/ilos/st/course/attendance_list_form.acl",
                headers={"Referer": f"{lms_url}/ilos/st/course/submain_form.acl"},
                verify=False, timeout=10)
            r_at = sess_lms.post(f"{lms_url}/ilos/st/course/attendance_list.acl",
                data={"ud": req.user_id, "ky": kjkey, "encoding": "utf-8"},
                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With": "XMLHttpRequest",
                         "Referer": f"{lms_url}/ilos/st/course/attendance_list_form.acl",
                         "Origin": lms_url},
                verify=False, timeout=15)

            parsed = _parse_lms_attend(r_at.text) if len(r_at.content) > 200 else {}
            results.append({
                "course_name": kjkey,
                "source": "lms",
                "kjkey": kjkey,
                **parsed,
            })

    except Exception as e:
        results.append({"error": f"LMS 오류: {str(e)}", "source": "lms"})

    # 출석률 계산
    total_present = sum(r.get("present", 0) for r in results if "error" not in r)
    total_absent  = sum(r.get("absent", 0)  for r in results if "error" not in r)
    total_late    = sum(r.get("late", 0)    for r in results if "error" not in r)
    total_classes = sum(r.get("total", 0)   for r in results if "error" not in r)
    attend_rate   = round(total_present / total_classes * 100, 1) if total_classes > 0 else None

    return JSONResponse(content={
        "success": True,
        "year": req.year,
        "term": req.term,
        "courses": results,
        "summary": {
            "total_present": total_present,
            "total_absent": total_absent,
            "total_late": total_late,
            "total_classes": total_classes,
            "attendance_rate": attend_rate,
        }
    })

'''

old_main = "if __name__ == \"__main__\":"
if 'combined_attendance' not in content and old_main in content:
    content = content.replace(old_main, new_endpoint + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("ADDED combined_attendance - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("Already exists or main not found")
