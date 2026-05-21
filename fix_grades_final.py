from datetime import datetime

content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

new_endpoints = '''

# ── 학교 포털 성적 조회 ───────────────────────────────────────────

class GradeDetailRequest(BaseModel):
    """성적 상세 조회 요청 — ussc9001m_s01 결과에서 넘어온 학기 정보"""
    user_id: str
    access_token: str
    session_cookies: Union[List[Any], dict]
    year: str                  # 예: "2019"
    smr: str                   # 학기 코드 예: "SU002002"
    smr_nm: str = "1학기"      # 학기 이름
    # s01 결과 집계값 (없어도 서버가 채워줌, 있으면 그대로 전달)
    bach_warn_cnt: str = "0"
    sum_fac_point: str = "0"
    sum_acq_point: str = "0"
    cnt_atlec_sbjt: str = "0"
    cnt_evl_sbjt: str = "0"
    perg_avg: str = "0"
    gpa_avg: str = "0"
    perc_pnt: str = "0"


def _full_ds_user(user_id: str) -> str:
    """fsp_ds_user XML 스니펫 (USER_ID만 필수, 나머지 서버 세션에서 채워짐)"""
    return f"""    <Dataset id="fsp_ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="100"/>
            <Column id="USER_NM" type="string" size="200"/>
            <Column id="DEPT_CD" type="string" size="10"/>
            <Column id="DEPT_NM" type="string" size="4000"/>
            <Column id="POSI_DVCD" type="string" size="30"/>
            <Column id="POST_NO" type="string" size="10"/>
            <Column id="ADDR" type="string" size="400"/>
            <Column id="DETL_ADDR" type="string" size="400"/>
            <Column id="TEL_NO" type="string" size="4000"/>
            <Column id="MBPH_NO" type="string" size="4000"/>
            <Column id="EMAIL" type="string" size="200"/>
            <Column id="DB_LOC" type="string" size="128"/>
            <Column id="STATUS_DVCD" type="string" size="32"/>
            <Column id="USER_DVCD" type="string" size="32"/>
            <Column id="POTAL_STATUS_DVCD" type="string" size="4000"/>
            <Column id="USER_IP" type="string" size="32"/>
        </ColumnInfo>
        <Rows>
            <Row><Col id="USER_ID">{user_id}</Col></Row>
        </Rows>
    </Dataset>"""


def _full_ds_cmd(sql_id: str) -> str:
    """fsp_ds_cmd XML 스니펫 — EXEC/EXEC_CNT/FAIL 포함 (JVC 필수)"""
    return f"""    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TX_NAME" type="string" size="100"/>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_SQL_ID" type="string" size="200"/>
            <Column id="KEY_INCREMENT" type="INT" size="10"/>
            <Column id="CALLBACK_SQL_ID" type="STRING" size="200"/>
            <Column id="INSERT_SQL_ID" type="STRING" size="200"/>
            <Column id="UPDATE_SQL_ID" type="STRING" size="200"/>
            <Column id="DELETE_SQL_ID" type="STRING" size="200"/>
            <Column id="SAVE_FLAG_COLUMN" type="STRING" size="200"/>
            <Column id="USE_INPUT" type="STRING" size="1"/>
            <Column id="USE_ORDER" type="STRING" size="1"/>
            <Column id="KEY_ZERO_LEN" type="INT" size="10"/>
            <Column id="BIZ_NAME" type="STRING" size="100"/>
            <Column id="PAGE_NO" type="INT" size="10"/>
            <Column id="PAGE_SIZE" type="INT" size="10"/>
            <Column id="READ_ALL" type="STRING" size="1"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
            <Column id="EXEC" type="STRING" size="1"/>
            <Column id="FAIL" type="STRING" size="1"/>
            <Column id="FAIL_MSG" type="STRING" size="200"/>
            <Column id="EXEC_CNT" type="INT" size="1"/>
            <Column id="MSG" type="STRING" size="200"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">{sql_id}</Col>
                <Col id="KEY_ZERO_LEN">0</Col>
                <Col id="EXEC_TYPE">B</Col>
            </Row>
        </Rows>
    </Dataset>"""


def _grade_ds_prog() -> str:
    return """    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="MENU_ID">1979</Col>
                <Col id="PROG_ID">sc90::ussc9001m.xfdl</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>"""


@app.post("/api/grades/terms")
async def fetch_grade_terms(req: PortalRequest):
    """
    학기 목록 조회 (ussc9001m_s01)
    SNO + 학사일정 날짜 범위로 수강한 학기 목록 반환
    """
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        # 오늘 기준 ±5년 범위로 전 학기 조회
        today = datetime.now()
        stt = f"{today.year - 5}0101000000000"
        end = f"{today.year + 1}1231000000000"

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
            <Column id="SNO" type="STRING" size="256"/>
            <Column id="STT_DTTM" type="STRING" size="256"/>
            <Column id="END_DTTM" type="STRING" size="256"/>
            <Column id="SCHD_DVCD" type="STRING" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="SNO">{req.user_id}</Col>
                <Col id="STT_DTTM">{stt}</Col>
                <Col id="END_DTTM">{end}</Col>
                <Col id="SCHD_DVCD">SUX01013</Col>
            </Row>
        </Rows>
    </Dataset>
{_full_ds_cmd("hs/sc/sc90:ussc9001m_s01")}
{_grade_ds_prog()}
{_full_ds_user(req.user_id)}
</Root>"""

        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/grades/detail")
async def fetch_grade_detail(req: GradeDetailRequest):
    """
    학기별 성적 상세 조회 (ussc9001m_s02)
    ussc9001m_s01 결과로 받은 year/smr 파라미터 전달
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
        <Parameter id="YEAR">{req.year}</Parameter>
        <Parameter id="SMR">{req.smr}</Parameter>
        <Parameter id="SMR_NM">{req.smr_nm}</Parameter>
        <Parameter id="SNO">{req.user_id}</Parameter>
        <Parameter id="BACH_WARN_CNT">{req.bach_warn_cnt}</Parameter>
        <Parameter id="SUM_FAC_POINT">{req.sum_fac_point}</Parameter>
        <Parameter id="SUM_ACQ_POINT">{req.sum_acq_point}</Parameter>
        <Parameter id="CNT_ATLEC_SBJT">{req.cnt_atlec_sbjt}</Parameter>
        <Parameter id="CNT_EVL_SBJT">{req.cnt_evl_sbjt}</Parameter>
        <Parameter id="PERG_AVG">{req.perg_avg}</Parameter>
        <Parameter id="GPA_AVG">{req.gpa_avg}</Parameter>
        <Parameter id="PERC_PNT">{req.perc_pnt}</Parameter>
    </Parameters>
{_full_ds_cmd("hs/sc/sc90:ussc9001m_s02")}
{_grade_ds_prog()}
{_full_ds_user(req.user_id)}
</Root>"""

        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

old_main = "if __name__ == \"__main__\":"
# Only insert once (check if already added)
if 'fetch_grade_terms' not in content:
    if old_main in content:
        content = content.replace(old_main, new_endpoints + old_main, 1)
        open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
        print("ADDED grade endpoints (terms + detail)")
    else:
        print("Could not find main block")
else:
    print("Grade endpoints already exist")
