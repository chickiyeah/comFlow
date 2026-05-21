import requests, re
LMS = "https://lms.jvision.ac.kr"
uid = "201918023"
upw = "D@lstn!0722"
KLASS = "A20261118031052182850c01"

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
    m = js_match.group(1)
    sess.get((LMS + m) if not m.startswith('http') else m, verify=False, timeout=15)

sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

r_ec = sess.post(f"{LMS}/ilos/st/course/eclass_room2.acl",
    data={"KJKEY":KLASS,"FLAG":"mp","returnURI":"/ilos/st/course/submain_form.acl","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

ec = r_ec.json()
return_url = ec.get("returnURL", "/ilos/st/course/submain_form.acl")

r_sub = sess.get(f"{LMS}{return_url}",
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"Submain: {len(r_sub.content)} bytes")

# Save submain HTML
with open("C:/Users/ruddls030/jvision/submain_page.html", "w", encoding="utf-8-sig", errors="replace") as f:
    f.write(r_sub.text)
print("Saved submain_page.html")

# Look for ky in attendance links
ky_in_links = re.findall(r'ky=([A-Za-z0-9]+)', r_sub.text)
print(f"ky values in attendance links: {set(ky_in_links)}")

# Look for attendance-related URLs
attend_urls = re.findall(r'attendance[^"\']*', r_sub.text)
print(f"attendance URLs: {set(attend_urls[:20])}")

# Look for any ID-like patterns
all_ids = re.findall(r'[A-Z]\d{4}[A-Za-z0-9]{5,}c\d+', r_sub.text)
print(f"All ID patterns: {set(all_ids)}")

# Look in JS variables
js_vars = re.findall(r'["\']([NnAaBb]\d{4}[A-Za-z0-9]+c\d+)["\']', r_sub.text)
print(f"ID in JS strings: {set(js_vars)}")
