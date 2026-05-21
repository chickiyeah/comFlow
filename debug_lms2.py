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

# GET form page first (critical!)
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl",
    headers={"Referer":f"{LMS}/ilos/mp/main.acl"}, verify=False, timeout=10)

# Get course list (2026-1)
r = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

print(f"Course list: {len(r.content)} bytes")
print(r.text)

# Extract course IDs (ky values) from HTML
# Look for links to attendance or course pages
ky_matches = re.findall(r'ky=([A-Za-z0-9]+)', r.text)
print(f"\nFound ky values: {set(ky_matches)}")

# Now try attendance with the first ky value
if ky_matches:
    ky = list(set(ky_matches))[0]
    print(f"\n[Attendance for ky={ky}]")
    sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl", verify=False, timeout=10)
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl","Origin":LMS},
        verify=False, timeout=15)
    print(f"Attendance: {len(ra.content)} bytes, content-type={ra.headers.get('Content-Type','')}")
    print(ra.text[:2000])
