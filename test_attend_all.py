import requests, urllib3, json
urllib3.disable_warnings()

r = requests.post("http://10.8.0.14:8000/api/attend/all",
    json={"user_id":"201918023","user_pw":"D@lstn!0722"},
    timeout=120)
data = r.json()
print(f"success={data['success']}")
if data['success']:
    print(f"학기: {data['semester']}")
    print(f"과목 수: {len(data['courses'])}")
    for c in data['courses']:
        attend_raw = c.get('attend_raw', {})
        keys = list(attend_raw.keys()) if isinstance(attend_raw, dict) else 'error'
        print(f"  [{c['sugang_name']}] is_interlock? / attend keys: {keys}")
else:
    print(f"Error: {data.get('message','')}")
