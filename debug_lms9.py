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

# Course list form page
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

# POST to submain to get main_form.acl
r_main = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"Main: {r_main.url}, {len(r_main.content)} bytes")

html = r_main.text
# Find N-format IDs and their surrounding context
n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', html))
print(f"N-format IDs: {n_vals}")

# Find context around each N-format ID (50 chars before and after)
for nv in n_vals:
    idx = html.find(nv)
    while idx > 0:
        ctx = html[max(0,idx-100):idx+100]
        # Only show if it has meaningful content (not just whitespace)
        if any(c.isalpha() for c in ctx.replace(nv,'')):
            print(f"\nContext around {nv}:")
            print(repr(ctx))
        idx = html.find(nv, idx+1)

# Find all links containing N-format
links = re.findall(r'href=["\']([^"\']*N\d{4}[^"\']+)["\']', html)
print(f"\nLinks with N-format: {links[:10]}")

# Save main_form HTML
with open("C:/Users/ruddls030/jvision/main_form.html", "w", encoding="utf-8-sig", errors="replace") as f:
    f.write(html)
print("Saved main_form.html")
