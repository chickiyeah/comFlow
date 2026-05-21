content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Find _restore_session_to and check indentation
idx = content.find('def _restore_session_to(')
if idx > 0:
    # Check what's before it
    line_start = content.rfind('\n', 0, idx) + 1
    prefix = content[line_start:idx]
    print(f"_restore_session_to found at char {idx}, prefix='{prefix}'")
    if prefix.strip() and len(prefix) > 0:
        print("  -> INDENTED (inside a function!) - need to move to module level")
        # Find the full function definition
        fn_start = line_start
        # Find end: next def/class at same or lower indentation
        fn_end = idx + 1
        while fn_end < len(content):
            nl = content.find('\n', fn_end)
            if nl < 0:
                fn_end = len(content)
                break
            next_line = content[nl+1:]
            if next_line and next_line[0] not in (' ', '\t', '\n', '#'):
                fn_end = nl + 1
                break
            fn_end = nl + 1

        fn_def = content[fn_start:fn_end]
        # Strip indentation
        lines = fn_def.split('\n')
        indent = len(lines[0]) - len(lines[0].lstrip())
        fn_def_clean = '\n'.join(l[indent:] if len(l) > indent else l for l in lines)

        # Remove from current position
        content = content[:fn_start] + content[fn_end:]

        # Add at module level before _restore_session
        target = 'def _restore_session('
        t_idx = content.find(target)
        if t_idx > 0:
            content = content[:t_idx] + fn_def_clean + '\n\n' + content[t_idx:]
            print("  -> Moved to module level before _restore_session")
    else:
        print("  -> Already at module level")
else:
    print("_restore_session_to NOT found - adding it")
    helper = '''
def _restore_session_to(sess, session_cookies) -> None:
    """저장된 쿠키를 requests.Session에 복원"""
    if isinstance(session_cookies, list):
        for c in session_cookies:
            sess.cookies.set(c["name"], c["value"],
                             domain=c.get("domain", ""),
                             path=c.get("path", "/"))
    elif isinstance(session_cookies, dict):
        sess.cookies.update(session_cookies)


'''
    t_idx = content.find('def _restore_session(')
    if t_idx > 0:
        content = content[:t_idx] + helper + content[t_idx:]
        print("Added at module level")

open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)

import py_compile
try:
    py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
    print("SYNTAX OK")
except py_compile.PyCompileError as e:
    print(f"ERROR: {e}")

# Quick import check
import importlib.util, sys
spec = importlib.util.spec_from_file_location("web_check", "C:/Users/ruddls030/jvision/web.py")
try:
    mod = importlib.util.module_from_spec(spec)
    print("Module created OK")
    # Don't run, just check
except Exception as e:
    print(f"Import error: {e}")
