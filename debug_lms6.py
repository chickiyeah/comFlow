import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

# Login
sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)

# Simulate eclassRoom: POST to submain_form.acl with ky
print("[1] POST to submain (eclassRoom simulation)...")
r1 = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"  Status: {r1.status_code}, URL: {r1.url}, Size: {len(r1.content)}")

# Check what ky values appear in submain
ky_vals = re.findall(r'ky=([A-Za-z0-9]+)', r1.text)
print(f"  ky in submain HTML: {set(ky_vals)}")

# Look for N2026 pattern in submain
n_vals = re.findall(r'N\d{4}[A-Z0-9]+c\d+', r1.text)
print(f"  N-format IDs in submain: {n_vals}")

# Try to access attendance from within submain context
print("\n[2] Attendance from submain context...")
ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data={"ud":uid,"ky":KLASS,"encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/submain_form.acl?ky={KLASS}","Origin":LMS},
    verify=False, timeout=15)
print(f"  Status: {ra.status_code}, Size: {len(ra.content)}, Type: {ra.headers.get('Content-Type','')}")
print(f"  Response: {ra.text[:500]}")
