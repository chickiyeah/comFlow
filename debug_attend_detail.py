import requests, json, urllib3
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
CHK = "https://check.jvision.ac.kr"

sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
r = sess.post(f"{CHK}/attend/iwin_sin",
    data={"ikey": json.dumps({"duser_id": uid, "duser_pw": upw})},
    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With": "XMLHttpRequest",
             "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
    verify=False, timeout=20)
rb = r.json().get("rollbook", [])

# 취업과창업 하나만 디버그
target = next((c for c in rb if "000791c118031" in c.get("sugang_code", "")), rb[0])
sc = target["sugang_code"]
print("course:", target.get("sugang_name"), sc)

r2 = sess.post(f"{CHK}/attend/iwin_st_chulseokbu",
    data={"ikey": json.dumps({"dclass": sc, "duser_id": uid})},
    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With": "XMLHttpRequest",
             "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
    verify=False, timeout=10)
d = r2.json()

# rollbook 내 주차 리스트 출력
rb2 = d.get("rollbook", [{}])
if rb2:
    week_list = rb2[0].get("sugangWeekInfoForJspList", [])
    print(f"주차 수: {len(week_list)}")
    for w in week_list[:3]:
        print("  week:", w.get("sugang_order"), "date:", w.get("sugang_date"),
              "detail:", w.get("week_attend_detail"))
        logs = w.get("sugangAttendLogList", [])
        for log in logs:
            print("    log:", {k: v for k, v in log.items()
                               if "attend" in k.lower() or "result" in k.lower() or k in ("sugang_order",)})

# rollbookuser 출력
rbu = d.get("rollbookuser", {})
print("\nrollbookuser:", rbu)

# rollbooksang 출력
rbs = d.get("rollbooksang", {})
print("rollbooksang:", rbs)

# sugangUserInfoList 출력
suil = d.get("sugangUserInfoList", [])
print("sugangUserInfoList:", suil[:2] if suil else "[]")
