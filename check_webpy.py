path = 'C:/Users/ruddls030/jvision/web.py'
c = open(path, encoding='utf-8').read()

idx = c.find('_parse_check_attend')
if idx >= 0:
    print("=== _parse_check_attend ===")
    print(c[idx:idx+600])
else:
    print("NOT FOUND")

print()
idx2 = c.find('fetch_check_course')
if idx2 >= 0:
    print("=== fetch_check_course ===")
    print(c[idx2:idx2+400])

print()
# timeout 값 확인
import re
timeouts = re.findall(r'timeout=\d+', c[c.find('combined_attendance'):c.find('combined_attendance')+3000] if 'combined_attendance' in c else c)
print("timeouts in combined_attendance:", timeouts[:10])
