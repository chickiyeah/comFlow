"""
check.jvision 출결 파싱 수정:
1. is_interlock=1 과목도 check.jvision에서 직접 조회
2. _parse_check_attend: rollbookuser attendCountResultList 집계
3. combined_attendance: 과목명 기준 중복 제거 후 조회
"""
import re

path = r'C:/Users/ruddls030/jvision/web.py'
content = open(path, encoding='utf-8').read()

# ── 1. _parse_check_attend 교체 ──────────────────────────────────────
old_parse = '''def _parse_check_attend(attend_raw) -> dict:
    """check.jvision 출결 raw (JSON/dict) → {present, absent, late, ...}"""
    if not attend_raw or not isinstance(attend_raw, dict):
        return {"present": 0, "absent": 0, "late": 0, "not_checked": 0, "total": 0}
    # iwin_st_chulseokbu response structure varies — try common keys
    def get_int(d, *keys):
        for k in keys:
            v = d.get(k, d.get(k.upper(), d.get(k.lower(), None)))
            if v is not None:
                try: return int(v)
                except: pass
        return 0
    return {
        "present":     get_int(attend_raw, "cnt_attend", "ATTEND", "출석"),
        "absent":      get_int(attend_raw, "cnt_absence", "ABSENCE", "결석"),
        "late":        get_int(attend_raw, "cnt_late", "LATE", "지각"),
        "not_checked": get_int(attend_raw, "cnt_nocheck", "NOCHECK", "미체크"),
        "total":       get_int(attend_raw, "total", "TOTAL"),
    }'''

new_parse = '''def _parse_check_attend(attend_raw) -> dict:
    """iwin_st_chulseokbu 응답 → {present, absent, late, not_checked, total}
    rollbookuser[0].attendCountResultList 주차별 집계 방식"""
    if not attend_raw or not isinstance(attend_raw, dict):
        return {"present": 0, "absent": 0, "late": 0, "not_checked": 0, "total": 0}
    rbu = attend_raw.get("rollbookuser", [])
    if not rbu or not isinstance(rbu, list):
        return {"present": 0, "absent": 0, "late": 0, "not_checked": 0, "total": 0}
    week_list = rbu[0].get("attendCountResultList", []) if rbu else []
    present = sum(w.get("attend_count", 0) for w in week_list)
    absent  = sum(w.get("absent_count",  0) for w in week_list)
    late    = sum(w.get("late_count",    0) for w in week_list)
    none    = sum(w.get("none_count",    0) for w in week_list)
    total   = present + absent + late + none
    return {"present": present, "absent": absent, "late": late,
            "not_checked": none, "total": total}'''

# ── 2. fetch_check_course 내 is_interlock 필터 제거 ──────────────────
old_filter = '''    def fetch_check_course(course):
            sc = course.get("sugang_code", "")
            if not sc or course.get("is_interlock", 1) == 1:
                return None  # LMS 전담'''

new_filter = '''    def fetch_check_course(course):
            sc = course.get("sugang_code", "")
            if not sc:
                return None'''

# ── 3. rollbook 중복 제거 적용 ────────────────────────────────────────
old_executor = '''        with ThreadPoolExecutor(max_workers=6) as ex:
            futs = [ex.submit(fetch_check_course, c) for c in rollbook]'''

new_executor = '''        # 과목명 기준 중복 제거 (같은 과목 여러 시간대)
        seen_nm, unique_rb = set(), []
        for c in rollbook:
            nm = c.get("sugang_name", "")
            if nm not in seen_nm:
                seen_nm.add(nm)
                unique_rb.append(c)
        with ThreadPoolExecutor(max_workers=6) as ex:
            futs = [ex.submit(fetch_check_course, c) for c in unique_rb]'''

changed = False

if old_parse in content:
    content = content.replace(old_parse, new_parse, 1)
    print("FIXED _parse_check_attend")
    changed = True
else:
    print("WARNING: _parse_check_attend pattern not found")

if old_filter in content:
    content = content.replace(old_filter, new_filter, 1)
    print("FIXED fetch_check_course is_interlock filter")
    changed = True
else:
    print("WARNING: fetch_check_course filter pattern not found")

if old_executor in content:
    content = content.replace(old_executor, new_executor, 1)
    print("FIXED rollbook deduplication")
    changed = True
else:
    print("WARNING: executor pattern not found")

if changed:
    open(path, 'w', encoding='utf-8').write(content)
    import py_compile
    try:
        py_compile.compile(path, doraise=True)
        print("SYNTAX OK")
    except py_compile.PyCompileError as e:
        print(f"SYNTAX ERROR: {e}")
else:
    print("No changes made")
