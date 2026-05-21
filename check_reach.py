import requests, urllib3
urllib3.disable_warnings()
try:
    r = requests.get('https://check.jvision.ac.kr', timeout=5, verify=False)
    print(f"OK: {r.status_code} {len(r.text)}chars")
except Exception as e:
    print(f"FAIL: {e}")
