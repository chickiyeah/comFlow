import requests, urllib3, re
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

# GET form page
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)

# Get course list
r = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

html = r.text
print(f"HTML size: {len(html)}")

# Find all N2026... patterns
n_patterns = re.findall(r'N\d{4}[A-Z0-9]+c\d+', html)
print(f"N-format IDs: {n_patterns}")

# Find any course IDs or ky-like patterns
course_ids = re.findall(r'[ANX][0-9]{4}[A-Za-z0-9]{5,}c\d+', html)
print(f"Course IDs (any prefix): {set(course_ids)}")

# Find all onclick handlers
onclicks = re.findall(r"onclick=\"([^\"]+)\"", html)
print(f"Onclick handlers: {onclicks[:10]}")

# Find JavaScript variables that might contain course IDs
js_vars = re.findall(r"var\s+\w+\s*=\s*['\"]([^'\"]+)['\"]", html)
print(f"JS vars with strings: {[v for v in js_vars if len(v) > 5][:20]}")

# Write full HTML to file for inspection
with open("C:/Users/ruddls030/jvision/lms_courses.html", "w", encoding="utf-8-sig", errors="replace") as f:
    f.write(html)
print("HTML saved to lms_courses.html")
