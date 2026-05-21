import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"
N_KY = "N2026B182940c00"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"})

# Login form first, then login
sess.get(f"{LMS}/ilos/main/member/login_form.acl", verify=False, timeout=10)
sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)

# Course list
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

# Save the full HTML for inspection
with open("C:/Users/ruddls030/jvision/course_list.html", "w", encoding="utf-8-sig", errors="replace") as f:
    f.write(r_c.text)
print(f"Saved course_list.html ({len(r_c.content)} bytes)")

# Find form elements
forms = re.findall(r'<form[^>]*>.*?</form>', r_c.text, re.DOTALL)
print(f"Forms found: {len(forms)}")
for i, form in enumerate(forms):
    action = re.search(r'action=["\']([^"\']+)["\']', form)
    print(f"  Form {i}: action={action.group(1) if action else 'none'}, excerpt={form[:100]}")

# Find JavaScript eclassRoom function definition
# Look in the response HTML
js_eclassroom = re.search(r'function\s+eclassRoom\s*\([^)]*\)[^{]*\{[^}]+\}', r_c.text, re.DOTALL)
if js_eclassroom:
    print(f"\neclassRoom function: {js_eclassroom.group()[:300]}")
else:
    print("\neclassRoom function not in HTML - likely in external JS")

# Try different approach: GET the course by navigating directly
print("\n\nTrying GET to submain (not POST):")
r_sub_get = sess.get(f"{LMS}/ilos/st/course/submain_form.acl",
    params={"ky": KLASS, "ud": uid},
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"  Status: {r_sub_get.status_code}, URL: {r_sub_get.url}, size: {len(r_sub_get.content)}")
n_get = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub_get.text))
print(f"  N-format IDs: {n_get}")

# Try N_KY direct attendance access (browser-captured ky)
print(f"\nDirect attendance with browser ky={N_KY}:")
r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
    params={"ky": N_KY, "ud": uid}, verify=False, timeout=10)
print(f"  Form GET: {r_af.status_code}, size={len(r_af.content)}")
ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data={"ud":uid,"ky":N_KY,"encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={N_KY}&ud={uid}","Origin":LMS},
    verify=False, timeout=15)
print(f"  Attend POST: {ra.status_code}, size={len(ra.content)}")
if len(ra.content) > 200:
    print(f"  SUCCESS! {ra.text[:600]}")
else:
    print(f"  Response: {ra.text[:100]}")
