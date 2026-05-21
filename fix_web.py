content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()
old = '''def _restore_session(access_token: str, session_cookies: dict) -> requests.Session:
    """저장된 쿠키 + 토큰으로 세션 복원"""
    sess = requests.Session()
    sess.cookies.update(session_cookies)
    sess.headers.update({"Authorization": f"Bearer {access_token}"})
    return sess'''
new = '''def _restore_session(access_token: str, session_cookies) -> requests.Session:
    """저장된 쿠키 + 토큰으로 세션 복원 (도메인/경로 포함 리스트 포맷 지원)"""
    sess = requests.Session()
    if isinstance(session_cookies, list):
        # 새 포맷: [{name, value, domain, path}, ...]
        for c in session_cookies:
            sess.cookies.set(c["name"], c["value"],
                             domain=c.get("domain", ""),
                             path=c.get("path", "/"))
    else:
        # 구 포맷: {name: value} flat dict
        sess.cookies.update(session_cookies)
    sess.headers.update({"Authorization": f"Bearer {access_token}"})
    return sess'''
if old in content:
    content = content.replace(old, new)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("PATCHED OK")
else:
    print("NOT FOUND")
