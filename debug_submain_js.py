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

r_ec = sess.post(f"{LMS}/ilos/st/course/eclass_room2.acl",
    data={"KJKEY":KLASS,"FLAG":"mp","returnURI":"/ilos/st/course/submain_form.acl","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

r_sub = sess.get(f"{LMS}/ilos/st/course/submain_form.acl",
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)

# Look for attendance-related JS and onclick in submain
html = r_sub.text

# Find onclick or href with attendance
attend_patterns = re.findall(r'[^\n]{0,50}attendance[^\n]{0,100}', html)
print("Attendance patterns in HTML:")
for p in set(attend_patterns[:20]):
    if any(c.isalpha() for c in p):
        print(f"  {p.strip()[:150]}")

# Look for JS function calls with ky-like params
fn_calls = re.findall(r'\w+\([^\)]{0,100}[Kk][Yy][^\)]{0,50}\)', html)
print(f"\nFunction calls with ky: {fn_calls[:10]}")

# Look for any JavaScript variable assignments
js_vars = re.findall(r'var\s+\w+\s*=\s*["\']([A-Za-z0-9]+)["\']', html)
print(f"\nJS vars: {[v for v in js_vars if len(v) > 5][:20]}")

# Look at the attendance tab/menu area specifically
attend_section = re.search(r'(?i)attendance.{0,2000}', html, re.DOTALL)
if attend_section:
    print(f"\nAttendance section (first 300 chars):")
    print(attend_section.group()[:300])

# Try clicking attendance_list_form.acl directly (no ky)
print("\n\nTrying attendance_list_form.acl without ky...")
r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
    headers={"Referer":f"{LMS}/ilos/st/course/submain_form.acl"}, verify=False, timeout=10)
print(f"Status: {r_af.status_code}, size: {len(r_af.content)}, URL: {r_af.url}")
print(f"Response snippet: {r_af.text[:300]}")

# Try attendance_list.acl without ky
ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data={"ud":uid,"encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/submain_form.acl","Origin":LMS},
    verify=False, timeout=15)
print(f"\nattendance_list.acl (no ky): {ra.status_code}, size: {len(ra.content)}")
if len(ra.content) > 200:
    print(f"POSSIBLE SUCCESS: {ra.text[:400]}")
