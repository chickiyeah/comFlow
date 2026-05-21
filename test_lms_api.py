import requests, json
r = requests.post('http://localhost:8000/api/lms/login',
    json={'user_id':'201918023','user_pw':'D@lstn!0722','session_cookies':[],'year':'2026','term':'1'},
    timeout=25)
print(f"status={r.status_code}, size={len(r.text)}")
d = r.json()
print(f"success={d.get('success')}")
print(f"klass_ids={d.get('klass_ids')}")
courses = d.get('courses', [])
print(f"courses={len(courses)}")
for c in courses:
    print(f"  ky={c.get('ky')} success={c.get('success')}")
    if c.get('html'):
        print(f"  html_snippet={c.get('html')[:200]}")
