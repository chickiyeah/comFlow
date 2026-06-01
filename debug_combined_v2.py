"""combined_attendance와 동일한 로직으로 에러 추적"""
import requests, json, urllib3
from concurrent.futures import ThreadPoolExecutor, as_completed
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
CHK = "https://check.jvision.ac.kr"

sess_chk = requests.Session()
sess_chk.headers.update({"User-Agent": "Mozilla/5.0"})

# iwin_sin 로그인
ikey = json.dumps({"duser_id": uid, "duser_pw": upw})
login_r = sess_chk.post(f"{CHK}/attend/iwin_sin",
    data={"ikey": ikey},
    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With": "XMLHttpRequest",
             "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
    verify=False, timeout=20, allow_redirects=True)
login_data = login_r.json()
rollbook = login_data.get("rollbook", [])
print(f"login OK, rollbook={len(rollbook)}, sess_cookies={len(list(sess_chk.cookies))}")

# 과목명 기준 중복 제거
seen_nm, unique_rb = set(), []
for c in rollbook:
    nm = c.get("sugang_name", "")
    if nm not in seen_nm:
        seen_nm.add(nm)
        unique_rb.append(c)
print(f"unique courses: {len(unique_rb)}")

def fetch_check_course(course):
    sc = course.get("sugang_code", "")
    if not sc:
        return None
    s2 = requests.Session()
    s2.headers.update({"User-Agent": "Mozilla/5.0"})
    for c in sess_chk.cookies:
        s2.cookies.set(c.name, c.value, domain=c.domain, path=c.path)
    try:
        chul_r = s2.post(f"{CHK}/attend/iwin_st_chulseokbu",
            data={"ikey": json.dumps({"dclass": sc, "duser_id": uid})},
            headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With": "XMLHttpRequest",
                     "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
            verify=False, timeout=30)
        raw = chul_r.json()
        rbu = raw.get("rollbookuser", [])
        week_list = rbu[0].get("attendCountResultList", []) if (rbu and isinstance(rbu, list)) else []
        attend = sum(w.get("attend_count", 0) for w in week_list)
        absent = sum(w.get("absent_count", 0)  for w in week_list)
        late   = sum(w.get("late_count", 0)    for w in week_list)
        none   = sum(w.get("none_count", 0)    for w in week_list)
        name   = course.get("sugang_name", sc)
        print(f"  OK [{name}] 출={attend} 결={absent} 지={late} 미={none} rbu_len={len(rbu)} week_len={len(week_list)}")
        return {"name": name, "attend": attend, "absent": absent, "none": none}
    except Exception as e:
        print(f"  ERR [{sc}]: {e}")
        return None

with ThreadPoolExecutor(max_workers=4) as ex:
    futs = [ex.submit(fetch_check_course, c) for c in unique_rb]
    results = [f.result() for f in as_completed(futs) if f.result()]
print(f"\n완료: {len(results)}개")
