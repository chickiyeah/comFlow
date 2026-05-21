import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")

# Full login
sess.get(f"{LMS}/ilos/main/member/login_form.acl", verify=False, timeout=10)
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=False)
js_redir = re.search(r"document\.location\.href=['\"]([^'\"]+)['\"]", r_login.text)
if js_redir:
    main_r = sess.get(LMS + js_redir.group(1) if not js_redir.group(1).startswith('http') else js_redir.group(1), verify=False, timeout=15)
    print(f"Main page: {len(main_r.content)} bytes")

# GET the course list FORM page (not the API)
r_form = sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl",
    headers={"Referer":f"{LMS}/ilos/main/main_form.acl"}, verify=False, timeout=10)
print(f"Course form page: {len(r_form.content)} bytes")

# Save form HTML
with open("C:/Users/ruddls030/jvision/course_form.html", "w", encoding="utf-8-sig", errors="replace") as f:
    f.write(r_form.text)
print("Saved course_form.html")

# Find eclassRoom in form page
if 'eclassRoom' in r_form.text:
    fn = re.search(r'(function\s+eclassRoom[^{]*\{[^}]+\})', r_form.text)
    print(f"eclassRoom in form page: {fn.group() if fn else 'found but no match'}")
else:
    print("eclassRoom NOT in form page")

# Find JS includes in form page
js_includes = re.findall(r'src=["\']([^"\']*\.js[^"\']*)["\']', r_form.text)
print(f"JS files in form page: {js_includes}")

# Check each JS for eclassRoom
for js_url in js_includes:
    if not js_url.startswith('http'):
        js_url = LMS + js_url
    try:
        js_r = sess.get(js_url, verify=False, timeout=5)
        if 'eclassRoom' in js_r.text:
            fn_match = re.search(r'(function\s+eclassRoom\s*\([^)]*\)\s*\{[^}]+\})', js_r.text)
            print(f"\n*** eclassRoom in {js_url}:")
            print(fn_match.group() if fn_match else "found but can't extract")
    except:
        pass
