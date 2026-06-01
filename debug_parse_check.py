"""iwin_st_chulseokbu 응답 → _parse_check_attend 결과 직접 검증"""
import requests, json, urllib3
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
CHK = "https://check.jvision.ac.kr"

sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0"})
r = sess.post(f"{CHK}/attend/iwin_sin",
    data={"ikey": json.dumps({"duser_id": uid, "duser_pw": upw})},
    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With": "XMLHttpRequest",
             "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
    verify=False, timeout=20)
rb = r.json().get("rollbook", [])

# 취업과창업 코드로 테스트
target = next((c for c in rb if "000791" in c.get("sugang_code", "")), rb[0])
sc = target["sugang_code"]
print("Testing:", target.get("sugang_name"), sc)

# s2 세션 (fetch_check_course 방식으로 복제)
s2 = requests.Session()
s2.headers.update({"User-Agent": "Mozilla/5.0"})
for c in sess.cookies:
    s2.cookies.set(c.name, c.value, domain=c.domain, path=c.path)

r2 = s2.post(f"{CHK}/attend/iwin_st_chulseokbu",
    data={"ikey": json.dumps({"dclass": sc, "duser_id": uid})},
    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With": "XMLHttpRequest",
             "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
    verify=False, timeout=30)

print("HTTP:", r2.status_code, "size:", len(r2.content))
raw = r2.json()
print("top keys:", list(raw.keys()))

rbu = raw.get("rollbookuser", [])
print("rollbookuser type:", type(rbu), "len:", len(rbu) if rbu else 0)
if rbu and isinstance(rbu, list) and rbu[0]:
    week_list = rbu[0].get("attendCountResultList", [])
    print("attendCountResultList len:", len(week_list))
    total_attend = sum(w.get("attend_count", 0) for w in week_list)
    total_absent  = sum(w.get("absent_count", 0)  for w in week_list)
    total_none    = sum(w.get("none_count", 0)    for w in week_list)
    print(f"  attend={total_attend} absent={total_absent} none={total_none}")
else:
    print("rollbookuser empty or wrong type")
    print("raw rollbookuser:", repr(rbu)[:200])
