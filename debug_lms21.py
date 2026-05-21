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

# Full login flow
sess.get(f"{LMS}/ilos/main/member/login_form.acl", verify=False, timeout=10)
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl",
             "Origin":LMS,"Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate"},
    verify=False, timeout=15, allow_redirects=False)
js_redir = re.search(r"document\.location\.href=['\"]([^'\"]+)['\"]", r_login.text)
if js_redir:
    sess.get(LMS + js_redir.group(1) if not js_redir.group(1).startswith('http') else js_redir.group(1),
        verify=False, timeout=15)

# Course list
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

# Find JS files in the course list HTML
js_files = re.findall(r'src=["\']([^"\']*\.js[^"\']*)["\']', r_c.text)
print(f"JS files in course list HTML: {js_files}")

# Download each JS file and look for eclassRoom
for js_url in js_files:
    if not js_url.startswith('http'):
        js_url = LMS + js_url
    try:
        js_r = sess.get(js_url, verify=False, timeout=10)
        if 'eclassRoom' in js_r.text:
            # Find the function
            fn_match = re.search(r'function\s+eclassRoom[^{]*\{([^}]+)\}', js_r.text)
            if fn_match:
                print(f"\neclassRoom in {js_url}:")
                print(fn_match.group())
    except Exception as e:
        print(f"Error fetching {js_url}: {e}")

# Also check the main HTML page for eclassRoom
r_main = sess.get(f"{LMS}/ilos/main/main_form.acl", verify=False, timeout=10)
js_files_main = re.findall(r'src=["\']([^"\']*\.js[^"\']*)["\']', r_main.text)
print(f"\nJS files in main page: {js_files_main[:5]}")
for js_url in js_files_main[:10]:
    if not js_url.startswith('http'):
        js_url = LMS + js_url
    try:
        js_r = sess.get(js_url, verify=False, timeout=5)
        if 'eclassRoom' in js_r.text:
            fn_match = re.search(r'(function\s+eclassRoom[^{]*\{[^}]+\})', js_r.text)
            if fn_match:
                print(f"\n*** eclassRoom FOUND in {js_url}:")
                print(fn_match.group()[:300])
    except Exception as e:
        pass
