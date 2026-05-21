import xml.etree.ElementTree as ET

with open('menu_raw.xml', encoding='utf-8') as f:
    content = f.read()

root = ET.fromstring(content)
ds_out = root.find(".//Dataset[@id='ds_out']")
if ds_out is None:
    print("No ds_out found")
    exit()

# Find grade-related menus
rows = ds_out.findall('Rows/Row')
print(f"Total menu entries: {len(rows)}")

keywords = ['성적', '수강', 'ussc', 'usss', 'ussu', '학사정보', '조회']
found = []
for row in rows:
    cols = {c.get('id'): (c.text or '') for c in row.findall('Col')}
    menu_nm = cols.get('MENU_NM', '')
    prog_path = cols.get('PROG_PATH2', '')
    work_det_nm = cols.get('WORK_BBS_DETL_NM', '')

    if any(kw in menu_nm or kw in prog_path or kw in work_det_nm for kw in keywords):
        found.append(cols)
        print(f"MENU_ID={cols.get('MENU_ID')} NM={menu_nm} PROG={prog_path} DETL={work_det_nm}")

print(f"\nFound {len(found)} grade-related entries")
