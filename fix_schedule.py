content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_schedule = '''

class ScheduleRequest(BaseModel):
    user_id: str
    access_token: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    smr: str = "SU002001"   # SU002001=1학기, SU002002=2학기


@app.post("/api/schedule/portal")
async def fetch_portal_schedule(req: ScheduleRequest):
    """
    학교 포털 수강시간표 조회 (ussu9001m_s01)
    ds_in: YEAR, SMR, SNO
    """
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCAction</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
    </Parameters>
    <Dataset id="ds_in">
        <ColumnInfo>
            <Column id="YEAR" type="STRING" size="256"/>
            <Column id="SMR" type="STRING" size="256"/>
            <Column id="SNO" type="STRING" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="YEAR">{req.year}</Col>
                <Col id="SMR">{req.smr}</Col>
                <Col id="SNO">{req.user_id}</Col>
            </Row>
        </Rows>
    </Dataset>
{_full_ds_cmd("hs/su/su90:ussu9001m_s01")}
    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="MENU_ID">1976</Col>
                <Col id="PROG_ID">su90::ussu9001m.xfdl</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
{_full_ds_user(req.user_id)}
</Root>"""

        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "data": _parse_nexacro(res.text, "ds_out"), "raw": res.text[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
if 'fetch_portal_schedule' not in content and old_main in content:
    content = content.replace(old_main, new_schedule + old_main, 1)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("ADDED schedule endpoint")
else:
    print("Schedule already exists or main not found")
