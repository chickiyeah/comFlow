import requests, urllib3, json
urllib3.disable_warnings()

sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0"})
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
         params={"langKnd":"ko","userId":"201918023","password":"D@lstn!0722"}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
          data={"langKnd":"ko","userId":"201918023","username":"201918023","password":"D@lstn!0722"},
          verify=False, timeout=10)

resp = sess.post("https://check.jvision.ac.kr/attend/iwin_sin",
    data={"ikey": json.dumps({"duser_id":"201918023","duser_pw":"D@lstn!0722"})},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":"https://check.jvision.ac.kr/jvision/student/",
             "Origin":"https://check.jvision.ac.kr"},
    verify=False, timeout=10, allow_redirects=True)

data = resp.json()

# rollbook first item - course schedule
print("=== rollbook[0] (수강 과목) ===")
if data['rollbook']:
    rb = data['rollbook'][0]
    for k, v in rb.items():
        print(f"  {k}: {v}")

print()

# rollbookindi first few items - attendance records
print("=== rollbookindi[0] (출결 정보?) ===")
if data['rollbookindi']:
    ri = data['rollbookindi'][0]
    for k, v in ri.items():
        print(f"  {k}: {v}")
    print(f"  ... (총 {len(data['rollbookindi'])}개)")
