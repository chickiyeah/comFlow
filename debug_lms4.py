import requests, urllib3, re, time
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS_ID = "A20261118031052182850c01"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

# Login
sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)

# GET course submain (eclassRoom equivalent)
print(f"[1] Navigate to course submain: ky={KLASS_ID}")
r1 = sess.get(f"{LMS}/ilos/st/course/submain_form.acl?ky={KLASS_ID}",
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"}, verify=False, timeout=10)
print(f"  Status: {r1.status_code}, URL: {r1.url}, Size: {len(r1.content)}")

# Look for ky in the response
ky_matches = re.findall(r'ky=([A-Za-z0-9]+)', r1.text)
print(f"  ky values in submain: {set(ky_matches)}")

# Try attendance with the KLASS_ID as ky
print(f"\n[2] Attendance with ky=KLASS_ID")
r_form = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
    params={"ky": KLASS_ID},
    headers={"Referer":f"{LMS}/ilos/st/course/submain_form.acl?ky={KLASS_ID}"}, verify=False, timeout=10)
print(f"  Form GET: {r_form.status_code}, size={len(r_form.content)}")
print(r_form.url)

# POST attendance
ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data={"ud":uid,"ky":KLASS_ID,"encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={KLASS_ID}","Origin":LMS},
    verify=False, timeout=15)
print(f"  Attendance POST: {ra.status_code}, size={len(ra.content)}, type={ra.headers.get('Content-Type','')}")
print(ra.text[:2000])
