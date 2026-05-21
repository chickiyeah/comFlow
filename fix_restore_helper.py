content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

if '_restore_session_to' not in content:
    helper = '''
def _restore_session_to(sess, session_cookies) -> None:
    """저장된 쿠키를 requests.Session에 복원 (리스트/딕트 포맷 모두 지원)"""
    if isinstance(session_cookies, list):
        for c in session_cookies:
            sess.cookies.set(c["name"], c["value"],
                             domain=c.get("domain", ""),
                             path=c.get("path", "/"))
    else:
        sess.cookies.update(session_cookies)


'''
    # Insert right before _restore_session
    idx = content.find('def _restore_session(')
    if idx > 0:
        content = content[:idx] + helper + content[idx:]
        print("Added _restore_session_to before _restore_session")
    else:
        # Insert after NMAIN_URL line
        idx2 = content.find('NMAIN_URL = ')
        nl = content.find('\n', idx2)
        content = content[:nl+1] + '\n' + helper + content[nl+1:]
        print("Added _restore_session_to after NMAIN_URL")

    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("_restore_session_to already exists")
