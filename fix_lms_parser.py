content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Fix the broken _parse_lms_attend function
old_parse = r'''def _parse_lms_attend(html: str) -> dict:
    """LMS 출결 HTML → {present, absent, late, not_checked, has_data}"""
    import re
    # JS override 값 우선 ("#div_1").html("N") 또는 <span id="div_1">N</span>
    def extract(pattern, text):
        m = re.search(pattern, text)
        return int(m.group(1)) if m else 0
    present   = extract(r'div_1["\)]+\.html\(["'](\d+)', html) or extract(r'id="div_1">(\d+)', html)
    absent    = extract(r'div_2["\)]+\.html\(["'](\d+)', html) or extract(r'id="div_2">(\d+)', html)
    late      = extract(r'div_3["\)]+\.html\(["'](\d+)', html) or extract(r'id="div_3">(\d+)', html)
    not_chk   = extract(r'div_4["\)]+\.html\(["'](\d+)', html) or extract(r'id="div_4">(\d+)', html)
    total = present + absent + late + not_chk
    has_data = total > 0 or '조회된 자료' not in html
    return {"present": present, "absent": absent, "late": late,
            "not_checked": not_chk, "total": total, "has_data": has_data}'''

new_parse = '''def _parse_lms_attend(html: str) -> dict:
    """LMS 출결 HTML: div_1=출석, div_2=결석, div_3=지각, div_4=미체크"""
    import re
    def get_count(div_id, text):
        # JS: $("#div_1").html("5") or HTML: <span id="div_1">5</span>
        m = re.search(r'div_' + div_id + r'[^>]*>(\d+)', text)
        if not m:
            m = re.search(r'"div_' + div_id + r'"[^"]*\.html[^"]*"(\d+)"', text)
        return int(m.group(1)) if m else 0
    present   = get_count("1", html)
    absent    = get_count("2", html)
    late      = get_count("3", html)
    not_chk   = get_count("4", html)
    total = present + absent + late + not_chk
    has_data = total > 0
    return {"present": present, "absent": absent, "late": late,
            "not_checked": not_chk, "total": total, "has_data": has_data}'''

if old_parse in content:
    content = content.replace(old_parse, new_parse)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile('C:/Users/ruddls030/jvision/web.py', doraise=True)
        print("FIXED _parse_lms_attend - SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"ERROR: {e}")
else:
    print("Pattern not found")
    idx = content.find('def _parse_lms_attend')
    print(f"Found at: {idx}")
    if idx > 0:
        print(repr(content[idx:idx+300]))
