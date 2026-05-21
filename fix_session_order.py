import re

content = open('C:/Users/ruddls030/jvision/intranet.py', encoding='utf-8').read()

# Find and fix: move session_cookies capture to BEFORE the logout/cleanup step
# Current: session_cookies is at the very end AFTER logout
# Fix: capture BEFORE the logout JVCActionNoSession call

# The logout section starts with the cleanup payload
old_cleanup_section = '''        # --- [STEP 9] 서버 세션 뒷정리 (로그아웃/세션 종료) ---'''
if old_cleanup_section not in content:
    # Try with spaces
    old_cleanup_section = content[content.find('STEP 9'):content.find('STEP 9')+20]
    print("Section header not found, searching...")
    idx = content.find('JVCActionNoSession')
    print(f"JVCActionNoSession at index {idx}")
    if idx > 0:
        # Find the start of the STEP 9 comment
        step9_idx = content.rfind('\n', 0, idx)
        step9_idx = content.rfind('\n', 0, step9_idx)
        print(f"Around: {repr(content[step9_idx:step9_idx+100])}")

# Replace the final session_cookies line to also add one BEFORE the cleanup
old_final = '    session_cookies = [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path} for c in session.cookies]'
if old_final in content:
    print("Found session_cookies line - already patched to list format")

    # Find where the STEP 9 cleanup starts
    step9_comment = '        # --- [STEP 9]'
    idx = content.find(step9_comment)
    if idx > 0:
        # Insert cookie capture before STEP 9
        pre_capture = '\n        # 세션 쿠키를 정리 전에 미리 저장 (NoSession 호출이 세션을 무효화할 수 있음)\n        session_cookies_pre_cleanup = [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path} for c in session.cookies]\n\n'
        content = content[:idx] + pre_capture + content[idx:]

        # Replace the final session_cookies line to use the pre-cleanup version
        content = content.replace(
            '    session_cookies = [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path} for c in session.cookies]',
            '    # 정리 전에 저장된 세션 쿠키 사용 (정리 후 세션이 무효화될 수 있음)\n    session_cookies = session_cookies_pre_cleanup if \'session_cookies_pre_cleanup\' in dir() else [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path} for c in session.cookies]'
        )
        open('C:/Users/ruddls030/jvision/intranet.py', 'w', encoding='utf-8').write(content)
        print("PATCHED - cookie capture moved before cleanup")
    else:
        print("Could not find STEP 9 comment: " + repr(content[content.find('JVCActionNoSession')-200:content.find('JVCActionNoSession')]))
else:
    print("Final session_cookies line not found")
    idx = content.rfind('session_cookies')
    print(repr(content[idx-5:idx+100]))
