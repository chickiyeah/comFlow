import requests, urllib3, re, json
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

# POST to submain (gets redirected to main_form.acl which has N-format IDs)
r_main = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"Main page: {r_main.status_code}, URL: {r_main.url}")

# Extract all N-format ky values from main page
n_vals = list(set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_main.text)))
print(f"N-format ky values: {n_vals}")

# Try each N-format ky for attendance
for ky in n_vals:
    print(f"\n[Attendance ky={ky}]")
    # Access form page with ky as query param (as browser does)
    form_r = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
        params={"ky": ky, "ud": uid},
        headers={"Referer": f"{LMS}/ilos/main/main_form.acl"},
        verify=False, timeout=10)
    print(f"  Form GET: {form_r.status_code}, size={len(form_r.content)}")

    # POST attendance
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud": uid, "ky": ky, "encoding": "utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}&ud={uid}",
                 "Origin":LMS},
        verify=False, timeout=15)
    print(f"  Attendance: {ra.status_code}, size={len(ra.content)}, type={ra.headers.get('Content-Type','')}")
    if ra.status_code == 200 and len(ra.content) > 200:
        print(f"  SUCCESS! Response: {ra.text[:500]}")
    else:
        print(f"  Response: {ra.text[:200]}")
