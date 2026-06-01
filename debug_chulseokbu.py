import requests, json, urllib3
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
CHK = "https://check.jvision.ac.kr"

sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
ikey = json.dumps({"duser_id": uid, "duser_pw": upw})
r = sess.post(f"{CHK}/attend/iwin_sin",
    data={"ikey": ikey},
    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With": "XMLHttpRequest",
             "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
    verify=False, timeout=20)
data = r.json()
rb = data.get("rollbook", [])

# 중복 제거 (같은 과목 여러 시간대)
seen = set()
unique = []
for c in rb:
    sc = c.get("sugang_code", "")
    nm = c.get("sugang_name", "")
    key = nm  # 과목명 기준 중복 제거
    if key not in seen:
        seen.add(key)
        unique.append(c)

print(f"고유 과목 수: {len(unique)}")
print()

# 취업과창업 테스트
for c in unique:
    sc = c.get("sugang_code", "")
    nm = c.get("sugang_name", "")
    il = c.get("is_interlock", "?")
    print(f"[{nm}] code={sc} interlock={il}")
    r2 = sess.post(f"{CHK}/attend/iwin_st_chulseokbu",
        data={"ikey": json.dumps({"dclass": sc, "duser_id": uid})},
        headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With": "XMLHttpRequest",
                 "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
        verify=False, timeout=10)
    try:
        d = r2.json()
        print(f"  -> HTTP {r2.status_code}, keys={list(d.keys()) if isinstance(d, dict) else type(d)}")
        if isinstance(d, dict):
            for k, v in d.items():
                print(f"     {k}: {v}")
        elif isinstance(d, list) and d:
            print(f"  list[0]: {d[0]}")
    except Exception as e:
        print(f"  -> ERROR {r2.status_code}: {r2.text[:200]}")
    print()
