import requests, urllib3, json, re, time
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

# Login
sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)

# GET form page first
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl",
    headers={"Referer":f"{LMS}/ilos/mp/main.acl"}, verify=False, timeout=10)

# Get course list (2026-1)
r = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

# Parse eclassRoom IDs
eclass_ids = re.findall(r"eclassRoom\('([^']+)'\)", r.text)
print(f"eclassRoom IDs found: {eclass_ids}")

# Extract all course links
course_links = re.findall(r'onclick="eclassRoom\(\'([^\']+)\'\)"[^>]*title="([^"]*)"', r.text)
print(f"Course links: {course_links}")

# Try to get attendance directly with the known ky
print(f"\n[Test attendance with ky=N2026B182940c00]")
# GET form page first
sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"}, verify=False, timeout=10)

ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data={"ud":uid,"ky":"N2026B182940c00","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl","Origin":LMS},
    verify=False, timeout=15)
print(f"Attendance: {len(ra.content)} bytes, type={ra.headers.get('Content-Type','')}")
print(ra.text[:2000])
