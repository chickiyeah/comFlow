import requests, re
LMS = "https://lms.jvision.ac.kr"
uid = "201918023"
upw = "D@lstn!0722"
KLASS = "A20261118031052182850c01"

# Call the actual API and print the raw response from eclass_room2 and submain
r = requests.post('http://localhost:8000/api/lms/login',
    json={'user_id': uid, 'user_pw': upw, 'session_cookies': [], 'year': '2026', 'term': '1'},
    timeout=25)
print(f"API: {r.status_code}")
d = r.json()
print(f"success={d.get('success')}, klass_ids={d.get('klass_ids')}, courses={len(d.get('courses',[]))}")

# If no courses, run the flow manually and debug
print("\n--- Manual debug ---")
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
    sess.get((LMS + js_match.group(1)) if not js_match.group(1).startswith('http') else js_match.group(1), verify=False, timeout=15)

sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
klass_ids = list(set(re.findall(r"eclassRoom\('([^']+)'\)", r_c.text)))
print(f"klass_ids from course list: {klass_ids}")

for kjkey in klass_ids:
    r_ec = sess.post(f"{LMS}/ilos/st/course/eclass_room2.acl",
        data={"KJKEY":kjkey,"FLAG":"mp","returnURI":"/ilos/st/course/submain_form.acl","encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
        verify=False, timeout=10)
    print(f"eclass_room2: {r_ec.status_code}, {len(r_ec.content)}, {r_ec.text[:150]}")

    try:
        ec = r_ec.json()
        return_url = ec.get("returnURL", "/ilos/st/course/submain_form.acl")
        is_error = ec.get("isError", True)
        print(f"  isError={is_error}, returnURL={return_url}")
    except:
        print("  JSON parse failed")
        continue

    r_sub = sess.get(f"{LMS}{return_url}",
        headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
        verify=False, timeout=10, allow_redirects=True)
    print(f"submain: {r_sub.status_code}, URL={r_sub.url}, size={len(r_sub.content)}")

    n_vals = list(set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text)))
    print(f"N-format IDs: {n_vals}")
