import requests, urllib3, json, time
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"

sess = requests.Session()
h = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
     "Accept-Language": "ko,en-US;q=0.9"}
sess.headers.update(h)

# Direct login to LMS
print("[1] LMS direct login...")
t = time.time()
r = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)
print(f"  {time.time()-t:.2f}s, status={r.status_code}, url={r.url}")
jid = sess.cookies.get("JSESSIONID", domain="lms.jvision.ac.kr")
print(f"  JSESSIONID: {jid}")
print(f"  All cookies: {[(c.name,c.domain) for c in sess.cookies]}")

# Access form page first (get CSRF or session setup)
print("[2] GET form page...")
form_r = sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl",
    headers={"Referer": f"{LMS}/ilos/mp/main.acl"}, verify=False, timeout=10)
print(f"  status={form_r.status_code}, size={len(form_r.content)}")

# Try course_register_list.acl with various params
print("[3] POST course_register_list.acl...")
for params in [
    {"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    {"YEAR":"2025","TERM":"1","num":"1","encoding":"utf-8"},
    {"num":"1","encoding":"utf-8"},
    {"encoding":"utf-8"},
]:
    r2 = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
        data=params,
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl",
                 "Origin":LMS},
        verify=False, timeout=10)
    print(f"  params={params}: status={r2.status_code} size={len(r2.content)} snippet={r2.text[:150]}")
