import requests, re
LMS = "https://lms.jvision.ac.kr"
uid = "201918023"
upw = "D@lstn!0722"
KLASS = "A20261118031052182850c01"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")

sess.get(f"{LMS}/ilos/main/member/login_form.acl", verify=False, timeout=10)
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=False)
js_match = re.search(r"document\.location\.href=['\"]([^'\"]+)['\"]", r_login.text)
if js_match:
    m = js_match.group(1)
    sess.get((LMS + m) if not m.startswith('http') else m, verify=False, timeout=15)

sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

sess.post(f"{LMS}/ilos/st/course/eclass_room2.acl",
    data={"KJKEY":KLASS,"FLAG":"mp","returnURI":"/ilos/st/course/submain_form.acl","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

sess.get(f"{LMS}/ilos/st/course/submain_form.acl",
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"}, verify=False, timeout=10)

r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
    headers={"Referer":f"{LMS}/ilos/st/course/submain_form.acl"}, verify=False, timeout=10)

# Extract all hidden inputs from form
hidden_inputs = {}
for match in re.finditer(r'<input[^>]+name=["\']([^"\']+)["\'][^>]*value=["\']([^"\']*)["\']', r_af.text):
    hidden_inputs[match.group(1)] = match.group(2)
print(f"Hidden form fields: {hidden_inputs}")

# POST with all form fields
payload = {"ud": uid, "encoding": "utf-8"}
payload.update(hidden_inputs)
print(f"Full payload: {payload}")

ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data=payload,
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl","Origin":LMS},
    verify=False, timeout=15)
print(f"\nPOST with form fields: {ra.status_code}, {len(ra.content)} bytes")
if len(ra.content) > 200:
    print(f"SUCCESS!\n{ra.text[:600]}")
else:
    print(f"Response: {ra.text[:100]}")

# Also try with just KJ_KEY (A-format) as ky
ra2 = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data={"ud": uid, "ky": KLASS, "encoding": "utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl","Origin":LMS},
    verify=False, timeout=15)
print(f"\nPOST with A-format ky: {ra2.status_code}, {len(ra2.content)} bytes")
if len(ra2.content) > 200:
    print(f"SUCCESS!\n{ra2.text[:600]}")
