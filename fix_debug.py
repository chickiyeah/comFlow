content = open('C:/Users/ruddls030/jvision/intranet.py', encoding='utf-8').read()
# Add debug after nmain_res line
old = '''        nmain_res = session.post(
            nmain_url,
            data=nmain_payload.encode('utf-8'),
            headers=nmain_headers,
            verify=False
        )

        print(f"📍 최종 응답 상태 코드: {nmain_res.status_code}")'''
new = '''        nmain_res = session.post(
            nmain_url,
            data=nmain_payload.encode('utf-8'),
            headers=nmain_headers,
            verify=False
        )

        print(f"📍 최종 응답 상태 코드: {nmain_res.status_code}")
        print("🍪 LoginAction 후 my.jvision.ac.kr 쿠키:")
        for c in session.cookies:
            if 'my.jvision' in c.domain:
                print(f"   [{c.domain}] {c.name}={c.value[:60]}... path={c.path}")'''
if old in content:
    content = content.replace(old, new)
    open('C:/Users/ruddls030/jvision/intranet.py', 'w', encoding='utf-8').write(content)
    print("PATCHED OK")
else:
    print("NOT FOUND")
    idx = content.find('nmain_res = session.post')
    if idx >= 0:
        print(repr(content[idx:idx+300]))
