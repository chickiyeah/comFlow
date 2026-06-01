# reload trigger
from datetime import datetime

from fastapi import Depends, FastAPI, Request, Form, HTTPException
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel
import qrcode
import base64
import requests
from io import BytesIO
from requests import Session as DbSession
import uvicorn
from db import get_db
import haksik
from intranet import run_vision_final_extractor


app = FastAPI()
templates = Jinja2Templates(directory="templates")


# ── 요청 모델 ────────────────────────────────────────────────
class LoginRequest(BaseModel):
    user_id: str
    user_pw: str

from typing import Union, List, Any

class PortalRequest(BaseModel):
    """저장된 토큰 + 쿠키로 학교 포털 API 호출"""
    user_id: str
    access_token: str
    session_cookies: Union[List[Any], dict]  # 리스트(도메인 포함) 또는 flat dict 모두 허용


# ── 유틸 ─────────────────────────────────────────────────────
def generate_qr_base64(data: str):
    qr = qrcode.QRCode(version=1, box_size=10, border=1)
    qr.add_data(data)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    buffered = BytesIO()
    img.save(buffered, format="PNG")
    return base64.b64encode(buffered.getvalue()).decode()


NMAIN_URL = "https://my.jvision.ac.kr/NMain"

def _nmain_headers(access_token: str) -> dict:
    """NMain 요청에 필요한 공통 헤더"""
    return {
        "Accept": "application/xml, text/xml, */*",
        "Content-Type": "text/xml",
        "X-Requested-With": "XMLHttpRequest",
        "Referer": "https://my.jvision.ac.kr/MyVisionApp/index.jsp",
        "Origin": "https://my.jvision.ac.kr",
        "Host": "my.jvision.ac.kr",
        "Cache-Control": "no-cache",
        "Authorization": f"Bearer {access_token}",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    }



def _restore_session_to(sess, session_cookies) -> None:
    """저장된 쿠키를 requests.Session에 복원"""
    if isinstance(session_cookies, list):
        for c in session_cookies:
            sess.cookies.set(c["name"], c["value"],
                             domain=c.get("domain", ""),
                             path=c.get("path", "/"))
    elif isinstance(session_cookies, dict):
        sess.cookies.update(session_cookies)


def _restore_session(access_token: str, session_cookies) -> requests.Session:
    """저장된 쿠키 + 토큰으로 세션 복원 (도메인/경로 포함 리스트 포맷 지원)"""
    sess = requests.Session()
    if isinstance(session_cookies, list):
        # 새 포맷: [{name, value, domain, path}, ...]
        for c in session_cookies:
            sess.cookies.set(c["name"], c["value"],
                             domain=c.get("domain", ""),
                             path=c.get("path", "/"))
    else:
        # 구 포맷: {name: value} flat dict
        sess.cookies.update(session_cookies)
    sess.headers.update({"Authorization": f"Bearer {access_token}"})
    return sess


# ── 기존 페이지 ───────────────────────────────────────────────
@app.get("/", response_class=HTMLResponse)
def login_page(request: Request):
    return templates.TemplateResponse("login.html", {"request": request})


@app.post("/mobile-id", response_class=HTMLResponse)
def show_id_card(request: Request, user_id: str = Form(...), user_pw: str = Form(...),
                 db: DbSession = Depends(get_db)):
    try:
        student_info, file_info, _, __, ___ = run_vision_final_extractor(db, user_id, user_pw)
        qr = generate_qr_base64(student_info['학번'])
        return templates.TemplateResponse("student_id.html", {
            "request": request, "student": student_info, "file": file_info, "qr": qr
        })
    except Exception as e:
        return HTMLResponse(f"<script>alert('로그인 실패: {e}'); location.href='/';</script>")


@app.get("/haksik", response_class=HTMLResponse)
async def get_haksik_page(request: Request):
    weekly_menus = haksik.get_weekly_menu()
    now = datetime.now()
    today_str = now.strftime("%d")
    today_full = now.strftime("%Y년 %m월 %d일")
    weekday_kr = ["월", "화", "수", "목", "금", "토", "일"][now.weekday()]
    today_menu = next((m for m in weekly_menus if m['day_num'] == today_str), None)
    return templates.TemplateResponse("haksik.html", {
        "request": request,
        "today_date": f"{today_full} ({weekday_kr}요일)",
        "menu": today_menu
    })


@app.get("/api/haksik")
async def get_haksik_api(date: str = None):
    data = haksik.get_weekly_menu(date)
    return JSONResponse(content=data)


# ── 학교 포털 인증 ─────────────────────────────────────────────

@app.post("/api/sync-profile")
async def sync_profile(req: LoginRequest, db: DbSession = Depends(get_db)):
    """
    학교 포털 로그인 → 학생정보 + 학생증사진 + ACCESS_TOKEN + REFRESH_TOKEN + 세션쿠키 반환
    비밀번호는 이 요청에서만 사용, CampusFlow에 저장되지 않음
    """
    try:
        student_info, file_info, access_token, refresh_token, session_cookies = \
            run_vision_final_extractor(db, req.user_id, req.user_pw)

        if isinstance(student_info, dict) and student_info.get("res") == "bad login":
            return JSONResponse(
                content={"success": False, "message": "학번 또는 비밀번호가 올바르지 않습니다."},
                status_code=401
            )

        return JSONResponse(content={
            "success": True,
            "data": {
                "name":           student_info.get("이름"),
                "studentId":      student_info.get("학번"),
                "department":     student_info.get("학과"),
                "phone":          student_info.get("전화번호"),
                "email":          student_info.get("이메일"),
                "address":        student_info.get("주소"),
                "profileImage":   file_info.get("이미지데이터") if file_info else None,
                # ── 토큰 두 개 ──────────────────────────────────
                "accessToken":    access_token,   # Bearer 토큰 (짧음, 곧 만료)
                "refreshToken":   refresh_token,  # 리프레시 토큰 (김, 재발급용)
                # ── 세션 쿠키 (토큰과 함께 보내야 함) ──────────────
                "sessionCookies": session_cookies,
            }
        })
    except Exception as e:
        return JSONResponse(
            content={"success": False, "message": f"동기화 오류: {str(e)}"},
            status_code=500
        )


# ── 토큰 + 쿠키로 데이터 조회 ─────────────────────────────────

@app.post("/api/grades")
async def fetch_grades(req: PortalRequest):
    """
    저장된 ACCESS_TOKEN + 세션쿠키로 성적 조회
    intranet.py 플로우: 쿠키 복원 → Bearer 토큰 → NMain XML 요청
    """
    try:
        import urllib3
        from bs4 import BeautifulSoup
        urllib3.disable_warnings()

        # 세션 복원 (쿠키 + 토큰)
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        # 성적 조회 XML - fsp_ds_prog 포함 (insertLog가 PROG_ID 필요)
        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCAction</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
    </Parameters>
    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TX_NAME" type="string" size="100"/>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_SQL_ID" type="string" size="200"/>
            <Column id="KEY_INCREMENT" type="INT" size="10"/>
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
                <Col id="SQL_ID">hs/gr/gr01:gr_hakjum_list_s00</Col>
                <Col id="KEY_ZERO_LEN">0</Col>
                <Col id="EXEC_TYPE">B</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="MENU_ID">2001</Col>
                <Col id="PROG_ID">gr90::usgr9001m.xfdl</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="100"/>
            <Column id="USER_NM" type="string" size="200"/>
            <Column id="DEPT_CD" type="string" size="10"/>
            <Column id="USER_IP" type="string" size="32"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="USER_ID">{req.user_id}</Col>
                <Col id="USER_NM"></Col>
                <Col id="DEPT_CD"></Col>
                <Col id="USER_IP"></Col>
            </Row>
        </Rows>
    </Dataset>
</Root>"""

        res = sess.post(
            NMAIN_URL,
            data=payload.encode("utf-8"),
            headers=headers,
            verify=False,
            timeout=20
        )

        # XML 응답에서 ErrorCode 확인
        from bs4 import BeautifulSoup as BS
        raw = res.text
        try:
            soup = BS(raw, 'xml')
            err_code = soup.find("Parameter", id="ErrorCode")
            err_msg  = soup.find("Parameter", id="ErrorMsg")
            if err_code and int(err_code.text) < 0:
                return JSONResponse(content={
                    "success": False,
                    "errorCode": err_code.text,
                    "message": err_msg.text if err_msg else "포털 오류",
                    "raw": raw[:1000]
                })
        except Exception:
            pass

        return JSONResponse(content={
            "success": True,
            "status": res.status_code,
            "raw": raw[:10000]
        })

    except Exception as e:
        return JSONResponse(
            content={"success": False, "message": str(e)},
            status_code=500
        )


@app.post("/api/portal")
async def fetch_portal_data(req: PortalRequest, sql_id: str = "hs/gr/gr01:gr_hakjum_list_s00"):
    """
    범용 포털 데이터 조회 — SQL_ID를 파라미터로 받아서 다양한 데이터 조회 가능
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
    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TX_NAME" type="string" size="100"/>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_ZERO_LEN" type="INT" size="10"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">{sql_id}</Col>
                <Col id="KEY_ZERO_LEN">0</Col>
                <Col id="EXEC_TYPE">B</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="MENU_ID">system</Col>
                <Col id="PROG_ID">system::main.xfdl</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="100"/>
            <Column id="USER_NM" type="string" size="200"/>
            <Column id="DEPT_CD" type="string" size="10"/>
            <Column id="USER_IP" type="string" size="32"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="USER_ID">{req.user_id}</Col>
                <Col id="USER_NM"></Col>
                <Col id="DEPT_CD"></Col>
                <Col id="USER_IP"></Col>
            </Row>
        </Rows>
    </Dataset>
</Root>"""

        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"),
                        headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text[:10000]})

    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)




@app.post("/api/explore")
def explore_menus(req: PortalRequest, sql_id: str = "sys/com:com_layout_s01"):
    """메뉴 구조 탐색 - 사용 가능한 SQL_ID 찾기"""
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCActionNoSession</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
        <Parameter id="UI_CD">0010</Parameter>
    </Parameters>
    <Dataset id="fsp_ds_cmd">
        <ColumnInfo>
            <Column id="TX_NAME" type="string" size="100"/>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_ZERO_LEN" type="INT" size="10"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">{sql_id}</Col>
                <Col id="KEY_ZERO_LEN">0</Col>
                <Col id="EXEC_TYPE">B</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_prog">
        <Rows>
            <Row>
                <Col id="MENU_ID">system</Col>
                <Col id="PROG_ID">system</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
</Root>"""
        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text[:20000]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)



@app.post("/api/login-datasets")
def get_login_datasets(req: LoginRequest, db: DbSession = Depends(get_db)):
    """NMain LoginAction 응답의 전체 데이터셋 반환 - SQL_ID 탐색용"""
    import requests as req_lib
    import urllib3
    urllib3.disable_warnings()

    session = req_lib.Session()
    common_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language": "ko,en-US;q=0.9",
    }

    # SSO login
    auth_url = "https://portal.jvision.ac.kr/user/loginAuth.face"
    session.get(auth_url, params={"langKnd": "ko", "userId": req.user_id, "password": req.user_pw},
                headers=common_headers, verify=False)
    login_url = "https://portal.jvision.ac.kr/user/loginProcess.face"
    session.post(login_url, data={"langKnd": "ko", "userId": req.user_id, "username": req.user_id, "password": req.user_pw},
                 headers=common_headers, verify=False)

    portal_sid = session.cookies.get('JSESSIONID', domain='portal.jvision.ac.kr')
    if portal_sid:
        session.cookies.set('EnviewSessionId', portal_sid, domain='my.jvision.ac.kr', path='/')

    sso_res = session.get(
        "http://sso.jvision.ac.kr/enpass/login?gateway=client&epAppId=jvisionCommon&service=https://my.jvision.ac.kr/MyVisionApp/index.jsp",
        headers=common_headers, verify=False, allow_redirects=True
    )

    session.cookies.set('JSESSIONID', '', domain='my.jvision.ac.kr', path='/')

    nmain_headers = {
        "Accept": "application/xml, text/xml, */*",
        "Content-Type": "text/xml",
        "X-Requested-With": "XMLHttpRequest",
        "Referer": "https://my.jvision.ac.kr/MyVisionApp/index.jsp",
        "Origin": "https://my.jvision.ac.kr",
        "Host": "my.jvision.ac.kr",
        "Cache-Control": "no-cache",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    }

    login_payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">LoginAction</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
        <Parameter id="LOGIN_METHOD">nexacro</Parameter>
        <Parameter id="OVER_LOGIN">Y</Parameter>
    </Parameters>
    <Dataset id="ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="256"/>
            <Column id="USER_PW" type="string" size="256"/>
            <Column id="REMEM_ID" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row><Col id="USER_ID">{req.user_id}</Col><Col id="USER_PW">{req.user_pw}</Col></Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_cmd"><ColumnInfo></ColumnInfo><Rows></Rows></Dataset>
    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows><Row><Col id="MENU_ID">unknown</Col><Col id="PROG_ID">frame::login_frame.xfdl</Col><Col id="LOCALE">ko_KR</Col></Row></Rows>
    </Dataset>
    <Dataset id="fsp_ds_user"><ColumnInfo><Column id="USER_ID" type="STRING" size="32"/></ColumnInfo><Rows></Rows></Dataset>
</Root>"""

    nmain_res = session.post("https://my.jvision.ac.kr/NMain", data=login_payload.encode('utf-8'),
                              headers=nmain_headers, verify=False)

    return JSONResponse(content={"success": True, "status": nmain_res.status_code, "raw": nmain_res.text})



@app.post("/api/layout")
def get_layout(req: PortalRequest, sql_id: str = "sys/com:com_layout_s01"):
    """메뉴/레이아웃 데이터 조회 - 최소 fsp_ds_cmd 사용"""
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        # intranet.py 정리 호출과 동일한 JVCActionNoSession 사용
        payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
    <Parameters>
        <Parameter id="fsp_action">JVCActionNoSession</Parameter>
        <Parameter id="fsp_cmd">execute</Parameter>
        <Parameter id="fsp_logId">all</Parameter>
        <Parameter id="fsp_logging">true</Parameter>
        <Parameter id="UI_CD">0010</Parameter>
    </Parameters>
    <Dataset id="fsp_ds_cmd">
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
    </Dataset>
    <Dataset id="fsp_ds_prog">
        <ColumnInfo>
            <Column id="MENU_ID" type="string" size="256"/>
            <Column id="PROG_ID" type="string" size="256"/>
            <Column id="LOCALE" type="string" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="MENU_ID">system</Col>
                <Col id="PROG_ID">system</Col>
                <Col id="LOCALE">ko_KR</Col>
            </Row>
        </Rows>
    </Dataset>
    <Dataset id="fsp_ds_user">
        <ColumnInfo>
            <Column id="USER_ID" type="string" size="100"/>
        </ColumnInfo>
        <Rows>
            <Row><Col id="USER_ID">{req.user_id}</Col></Row>
        </Rows>
    </Dataset>
</Root>"""
        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "data": _parse_nexacro(res.text, "ds_out"), "raw": res.text[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)



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



def _parse_nexacro(xml_text: str, dataset_id: str = "ds_out") -> list:
    """Nexacro XML ds_out 데이터셋을 [{col_id: value, ...}] 리스트로 변환"""
    try:
        from xml.etree import ElementTree as ET
        ns = {"ns": "http://www.nexacroplatform.com/platform/dataset"}
        root = ET.fromstring(xml_text)
        # Check error
        for param in root.findall("ns:Parameters/ns:Parameter", ns):
            if param.get("id") == "ErrorCode" and param.text and int(param.text) < 0:
                return []
        # Find dataset
        ds = root.find(f"ns:Dataset[@id='{dataset_id}']", ns)
        if ds is None:
            return []
        # Get column IDs
        cols = [c.get("id") for c in ds.findall("ns:ColumnInfo/ns:Column", ns)]
        rows = []
        for row in ds.findall("ns:Rows/ns:Row", ns):
            record = {}
            for col in row.findall("ns:Col", ns):
                cid = col.get("id")
                record[cid] = col.text or ""
            rows.append(record)
        return rows
    except Exception as e:
        return []

@app.post("/api/grades/terms")
def fetch_grade_terms(req: PortalRequest):
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
        return JSONResponse(content={"success": True, "data": _parse_nexacro(res.text, "ds_out"), "raw": res.text[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/grades/detail")
def fetch_grade_detail(req: GradeDetailRequest):
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
        return JSONResponse(content={"success": True, "data": _parse_nexacro(res.text, "ds_out"), "raw": res.text[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)



@app.post("/api/shuttle")
def fetch_shuttle(req: PortalRequest):
    """
    통학버스 정보 조회 (sdsb9001m_s01 + s02 + s03 동시 실행)
    """
    try:
        import urllib3
        urllib3.disable_warnings()
        sess = _restore_session(req.access_token, req.session_cookies)
        headers = _nmain_headers(req.access_token)

        shuttle_rows = """
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s01</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s02</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>
            <Row><Col id="TYPE">N</Col><Col id="SQL_ID">sd/sb/sb90:sdsb9001m_s03</Col><Col id="KEY_ZERO_LEN">0</Col><Col id="EXEC_TYPE">B</Col></Row>"""

        col_info = """            <Column id="TX_NAME" type="string" size="100"/>
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
            <Column id="MSG" type="STRING" size="200"/>"""

        payload = (
            '<?xml version="1.0" encoding="UTF-8"?>'
            '<Root xmlns="http://www.nexacroplatform.com/platform/dataset">'
            '<Parameters>'
            '<Parameter id="fsp_action">JVCAction</Parameter>'
            '<Parameter id="fsp_cmd">execute</Parameter>'
            '<Parameter id="fsp_logId">all</Parameter>'
            '<Parameter id="fsp_logging">true</Parameter>'
            '</Parameters>'
            '<Dataset id="fsp_ds_cmd">'
            '<ColumnInfo>' + col_info + '</ColumnInfo>'
            '<Rows>' + shuttle_rows + '</Rows>'
            '</Dataset>'
            '<Dataset id="fsp_ds_prog">'
            '<ColumnInfo>'
            '<Column id="MENU_ID" type="string" size="256"/>'
            '<Column id="PROG_ID" type="string" size="256"/>'
            '<Column id="LOCALE" type="string" size="256"/>'
            '</ColumnInfo>'
            '<Rows><Row>'
            '<Col id="MENU_ID">1992</Col>'
            '<Col id="PROG_ID">sdsb90::sdsb9001m.xfdl</Col>'
            '<Col id="LOCALE">ko_KR</Col>'
            '</Row></Rows>'
            '</Dataset>'
            '<Dataset id="fsp_ds_user">'
            '<ColumnInfo><Column id="USER_ID" type="string" size="100"/></ColumnInfo>'
            '<Rows><Row><Col id="USER_ID">' + req.user_id + '</Col></Row></Rows>'
            '</Dataset>'
            '</Root>'
        )

        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        raw = res.text

        from xml.etree import ElementTree as ET
        ns = {"ns": "http://www.nexacroplatform.com/platform/dataset"}
        try:
            root_el = ET.fromstring(raw)
            datasets = {}
            for ds in root_el.findall("ns:Dataset", ns):
                ds_id = ds.get("id")
                if ds_id and ds_id.startswith("fsp_ds"):
                    continue
                rows_out = []
                for row in ds.findall("ns:Rows/ns:Row", ns):
                    record = {col.get("id"): (col.text or "") for col in row.findall("ns:Col", ns)}
                    rows_out.append(record)
                datasets[ds_id] = rows_out
        except Exception as parse_err:
            datasets = {"parse_error": str(parse_err)}

        return JSONResponse(content={"success": True, "datasets": datasets, "raw": raw[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


class ScheduleRequest(BaseModel):
    user_id: str
    access_token: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    smr: str = "SU002001"   # SU002001=1학기, SU002002=2학기


@app.post("/api/schedule/portal")
def fetch_portal_schedule(req: ScheduleRequest):
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



# ── 출결 시스템 (check.jvision.ac.kr) ────────────────────────────

ATTEND_URL = "https://check.jvision.ac.kr"

class AttendRequest(BaseModel):
    """출결 시스템 로그인 — 저장된 포털 세션 + 학번/비밀번호"""
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]  # 포털 sync 때 저장한 세션 쿠키


@app.post("/api/attend/login")
def attend_login(req: AttendRequest):
    """
    출결 시스템 로그인 (check.jvision.ac.kr/attend/iwin_sin)
    저장된 포털 EnviewSessionId + 학번/비밀번호로 출결 JSESSIONID 취득
    포털 재로그인 없이 저장된 쿠키 재사용 → 빠름
    """
    try:
        import urllib3, json
        urllib3.disable_warnings()

        sess = requests.Session()
        sess.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept-Language": "ko,en-US;q=0.9,en;q=0.8",
        })

        # 저장된 포털 쿠키를 check.jvision.ac.kr 도메인에도 설정
        portal_enview_sid = None
        cookies_src = req.session_cookies if isinstance(req.session_cookies, list) else []

        for c in cookies_src:
            name, val, domain, path = c.get("name"), c.get("value",""), c.get("domain",""), c.get("path","/")
            # 포털/SSO EnviewSessionId를 출결 도메인에 이식
            if name == "EnviewSessionId":
                sess.cookies.set(name, val, domain="check.jvision.ac.kr", path="/")
                portal_enview_sid = val
            if name in ("EnviewLangKnd", "ENPASSTGC"):
                sess.cookies.set(name, val, domain="check.jvision.ac.kr", path="/")

        # 출결 시스템 로그인 (iwin_sin)
        ikey_json = json.dumps({"duser_id": req.user_id, "duser_pw": req.user_pw})
        attend_headers = {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest",
            "Referer": f"{ATTEND_URL}/jvision/student/",
            "Origin": ATTEND_URL,
        }
        resp = sess.post(
            f"{ATTEND_URL}/attend/iwin_sin",
            data={"ikey": ikey_json},
            headers=attend_headers,
            verify=False,
            timeout=15,
            allow_redirects=True
        )

        attend_jsessionid = sess.cookies.get("JSESSIONID", domain="check.jvision.ac.kr")
        all_attend_cookies = [
            {"name": c.name, "value": c.value, "domain": c.domain, "path": c.path}
            for c in sess.cookies
        ]
        success = resp.status_code in (200, 302)

        return JSONResponse(content={
            "success": success,
            "status": resp.status_code,
            "jsessionid": attend_jsessionid,
            "portal_enview_sid": portal_enview_sid,
            "all_cookies": all_attend_cookies,
            "response_snippet": resp.text[:500] if resp.text else ""
        })
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)



class AttendAllRequest(BaseModel):
    """전체 출결 조회 — 저장된 포털 쿠키로 attend 로그인 + 병렬 과목 조회"""
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]  # 포털 sync 때 저장한 세션 쿠키


@app.post("/api/attend/all")
def fetch_all_attendance(req: AttendAllRequest):
    """
    전체 출결 조회 (로그인 → rollbook 파싱 → 모든 과목 iwin_st_chulseokbu 호출)
    반환: { success, semester, courses: [{sugang_name, attend_list}] }
    """
    try:
        import urllib3, json
        urllib3.disable_warnings()

        sess = requests.Session()
        common_headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept-Language": "ko,en-US;q=0.9",
        }
        sess.headers.update(common_headers)

        # 저장된 포털 쿠키로 세션 복원 (재로그인 불필요)
        _restore_session_to(sess, req.session_cookies)

        # 출결 시스템 로그인
        ikey_json = json.dumps({"duser_id": req.user_id, "duser_pw": req.user_pw})
        login_resp = sess.post(
            f"{ATTEND_URL}/attend/iwin_sin",
            data={"ikey": ikey_json},
            headers={
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest",
                "Referer": f"{ATTEND_URL}/jvision/student/",
                "Origin": ATTEND_URL,
            },
            verify=False, timeout=15, allow_redirects=True
        )

        login_data = login_resp.json()
        if login_data.get("xidedu", {}).get("xmsg") != "Ok":
            return JSONResponse(content={"success": False, "message": "출결 시스템 로그인 실패"})

        attend_jid = sess.cookies.get("JSESSIONID", domain="check.jvision.ac.kr")
        rollbook = login_data.get("rollbook", [])
        rollbooksang = login_data.get("rollbooksang", {})

        # 과목별 출결 병렬 조회 (ThreadPoolExecutor)
        from concurrent.futures import ThreadPoolExecutor, as_completed

        def fetch_one_course(course):
            sugang_code = course.get("sugang_code", "")
            sugang_num  = course.get("sugang_semester_num", "")
            if not sugang_code:
                return None
            try:
                s = requests.Session()
                for c in sess.cookies:
                    s.cookies.set(c.name, c.value, domain=c.domain, path=c.path)
                chul_ikey = json.dumps({"dclass": sugang_code, "duser_id": req.user_id})
                r = s.post(
                    f"{ATTEND_URL}/attend/iwin_st_chulseokbu",
                    data={"ikey": chul_ikey},
                    headers={
                        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                        "X-Requested-With": "XMLHttpRequest",
                        "Origin": ATTEND_URL,
                        "Referer": (f"{ATTEND_URL}/jvision/student/01_go_win_classtime.html"
                                    f"?sugang_code={sugang_code}&sugang_semester_num={sugang_num}"
                                    f"&userid={req.user_id}"),
                    },
                    verify=False, timeout=8
                )
                chul_data = r.json()
            except Exception:
                chul_data = {}
            return {
                "sugang_code":  sugang_code,
                "sugang_name":  course.get("sugang_name", ""),
                "professor":    course.get("professor_name", ""),
                "week":         course.get("sugang_week", ""),
                "starttime":    course.get("sugang_starttime", ""),
                "endtime":      course.get("sugang_endtime", ""),
                "total_classes":course.get("sugang_total_number", 0),
                "is_interlock": course.get("is_interlock", 0),
                "attend_raw":   chul_data,
            }

        courses_attend = []
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = {executor.submit(fetch_one_course, c): c for c in rollbook if c.get("sugang_code")}
            for fut in as_completed(futures):
                result = fut.result()
                if result:
                    courses_attend.append(result)
        courses_attend.sort(key=lambda x: x.get("week", 0))

        return JSONResponse(content={
            "success": True,
            "semester": {
                "year": rollbooksang.get("sugang_year"),
                "semester": rollbooksang.get("sugang_semester"),
                "start_day": rollbooksang.get("sugang_start_day"),
                "end_day": rollbooksang.get("sugang_end_day"),
            },
            "courses": courses_attend,
            "user": login_data.get("xuser", {}),
        })

    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/attend/course")
def fetch_course_attendance(req: AttendRequest, sugang_code: str = ""):
    """
    단일 과목 출결 조회 (iwin_st_chulseokbu)
    저장된 attend JSESSIONID 재사용
    """
    try:
        import urllib3, json
        urllib3.disable_warnings()

        sess = requests.Session()
        _restore_session_to(sess, req.session_cookies)

        ikey_json = json.dumps({"dclass": sugang_code, "duser_id": req.user_id})
        resp = sess.post(
            f"{ATTEND_URL}/attend/iwin_st_chulseokbu",
            data={"ikey": ikey_json},
            headers={
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest",
                "Referer": f"{ATTEND_URL}/jvision/student/",
                "Origin": ATTEND_URL,
            },
            verify=False, timeout=10
        )
        return JSONResponse(content={"success": True, "data": resp.json()})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)



# ── LMS 이러닝 (lms.jvision.ac.kr) ──────────────────────────────

LMS_URL = "https://lms.jvision.ac.kr"


def _lms_full_login(user_id, user_pw):
    """LMS 완전 로그인: 폼 GET → POST → JS redirect 추적"""
    import re, urllib3
    urllib3.disable_warnings()
    sess = requests.Session()
    sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
    sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("EnviewLangKnd", "ko", domain="lms.jvision.ac.kr", path="/")
    sess.get(f"{LMS_URL}/ilos/main/member/login_form.acl", verify=False, timeout=10)
    r_login = sess.post(f"{LMS_URL}/ilos/lo/login.acl",
        data={"returnURL":"","challenge":"","response":"","usr_id":user_id,"usr_pwd":user_pw},
        headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS_URL}/ilos/main/member/login_form.acl","Origin":LMS_URL},
        verify=False, timeout=15, allow_redirects=False)
    js_match = re.search(r"document\.location\.href=['\"]([^'\"]+)['\"]", r_login.text)
    if js_match:
        main_url = js_match.group(1)
        if not main_url.startswith("http"):
            main_url = LMS_URL + main_url
        sess.get(main_url, verify=False, timeout=15)
    return sess


def fetch_lms_all(req):
    """
    LMS 출결 완전 플로:
    1. 완전 로그인 (폼 GET → POST → JS redirect)
    2. 과목 폼 GET (eclassRoom 컨텍스트 준비)
    3. 과목 목록 (eclassRoom ID 추출)
    4. 각 과목: eclass_room2.acl → submain → 출결
    """
    import re, urllib3
    urllib3.disable_warnings()

    sess = _lms_full_login(req.user_id, req.user_pw)
    lms_jid = next((c.value for c in sess.cookies if c.name == 'JSESSIONID' and 'lms.jvision' in c.domain), None)
    if not lms_jid:
        return {"success": False, "message": "LMS JSESSIONID 없음"}

    sess.get(f"{LMS_URL}/ilos/mp/course_register_list_form.acl",
        headers={"Referer": f"{LMS_URL}/ilos/main/main_form.acl"}, verify=False, timeout=10)

    r_courses = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
        data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl","Origin":LMS_URL},
        verify=False, timeout=10)

    klass_ids = list(set(re.findall(r"eclassRoom\('([^']+)'\)", r_courses.text)))
    courses_attend = []

    for kjkey in klass_ids:
        r_ec = sess.post(f"{LMS_URL}/ilos/st/course/eclass_room2.acl",
            data={"KJKEY":kjkey,"FLAG":"mp","returnURI":"/ilos/st/course/submain_form.acl","encoding":"utf-8"},
            headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With":"XMLHttpRequest",
                     "Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl","Origin":LMS_URL},
            verify=False, timeout=10)
        try:
            ec = r_ec.json()
            if ec.get("isError"):
                continue
            return_url = ec.get("returnURL", "/ilos/st/course/submain_form.acl")
        except Exception:
            continue

        # 서브메인 진입 (course context 확립)
        sess.get(f"{LMS_URL}{return_url}",
            headers={"Referer":f"{LMS_URL}/ilos/mp/course_register_list_form.acl"},
            verify=False, timeout=10, allow_redirects=True)

        # 출결 폼 GET (ky 불필요 — 세션 컨텍스트로 자동 처리)
        sess.get(f"{LMS_URL}/ilos/st/course/attendance_list_form.acl",
            headers={"Referer":f"{LMS_URL}/ilos/st/course/submain_form.acl"},
            verify=False, timeout=10)

        # 출결 데이터 POST — ky는 A-포맷 klass_id 그대로 사용 가능
        r_at = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
            data={"ud":req.user_id,"ky":kjkey,"encoding":"utf-8"},
            headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With":"XMLHttpRequest",
                     "Referer":f"{LMS_URL}/ilos/st/course/attendance_list_form.acl","Origin":LMS_URL},
            verify=False, timeout=15)

        courses_attend.append({
            "kjkey": kjkey,
            "success": len(r_at.content) > 200,
            "html": r_at.text[:5000] if len(r_at.content) > 200 else None,
            "error": r_at.text[:100] if len(r_at.content) <= 200 else None,
        })

    return {"success": True, "year": req.year, "term": req.term,
            "klass_ids": klass_ids, "courses": courses_attend}

class LmsCourseRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"   # 1=1학기, 2=2학기


@app.post("/api/lms/login")
def lms_login(req: LmsCourseRequest):
    """LMS 완전 출결 조회 (로그인 + eclass_room2 + attendance)"""
    try:
        result = fetch_lms_all(req)
        return JSONResponse(content=result)
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)



class LmsCourseRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"   # 1=1학기, 2=2학기


class LmsAttendRequest(BaseModel):
    user_id: str
    user_pw: str
    session_cookies: Union[List[Any], dict]
    year: str = "2026"
    term: str = "1"
    course_id: str = ""  # ky 파라미터값 (예: N2026B182940c00)


def _lms_login(req_user_id: str, req_user_pw: str, session_cookies) -> requests.Session:
    """LMS 로그인 후 세션 반환"""
    import urllib3
    urllib3.disable_warnings()
    sess = requests.Session()
    sess.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language": "ko,en-US;q=0.9",
    })
    _restore_session_to(sess, session_cookies)
    # _language_, co_check 쿠키 추가 (LMS 필수)
    sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")

    sess.post(f"{LMS_URL}/ilos/lo/login.acl",
        data={"returnURL":"","challenge":"","response":"",
              "usr_id": req_user_id, "usr_pwd": req_user_pw},
        headers={"Content-Type":"application/x-www-form-urlencoded",
                 "Referer":f"{LMS_URL}/ilos/main/member/login_form.acl",
                 "Origin":LMS_URL},
        verify=False, timeout=15, allow_redirects=True)
    return sess


def _lms_headers(referer: str) -> dict:
    return {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "X-Requested-With": "XMLHttpRequest",
        "Referer": referer,
        "Origin": LMS_URL,
    }


@app.post("/api/lms/courses")
def lms_courses(req: LmsCourseRequest):
    """
    LMS 수강 과목 목록 조회
    course_register_list.acl(현재) + list2.acl(연도/학기별) + list_b.acl(비정규)
    """
    try:
        sess = _lms_login(req.user_id, req.user_pw, req.session_cookies)
        referer = f"{LMS_URL}/ilos/mp/course_register_list_form.acl"
        headers = _lms_headers(referer)

        # 과목목록1: YEAR+TERM+num=1+encoding (유저 확인 포맷)
        r1 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        # 과목목록2: num=2
        r2 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list2.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "2", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        # 비정규 과목
        r3 = sess.post(f"{LMS_URL}/ilos/mp/course_register_list_b.acl",
            data={"YEAR": "", "TERM": "", "NON_TERM": "B", "encoding": "utf-8"},
            headers=headers, verify=False, timeout=10)

        return JSONResponse(content={
            "success": True,
            "current": {"status": r1.status_code, "size": len(r1.content), "snippet": r1.text[:1000]},
            "by_term":  {"status": r2.status_code, "size": len(r2.content), "snippet": r2.text[:1000]},
            "non_term": {"status": r3.status_code, "size": len(r3.content), "snippet": r3.text[:500]},
        })
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/lms/attendance")
def lms_attendance(req: LmsAttendRequest):
    """
    LMS 출결 조회 (attendance_list.acl)
    course_id: lms/courses에서 추출한 과목 ID
    """
    try:
        sess = _lms_login(req.user_id, req.user_pw, req.session_cookies)
        referer = f"{LMS_URL}/ilos/st/course/attendance_list_form.acl"
        headers = _lms_headers(referer)

        # payload: ud=학번&ky=과목키(klass_id)&encoding=utf-8
        payload = {"ud": req.user_id, "encoding": "utf-8"}
        if req.course_id:
            payload["ky"] = req.course_id  # ky = klass_id (N2026B182940c00 형식)

        resp = sess.post(f"{LMS_URL}/ilos/st/course/attendance_list.acl",
            data=payload, headers=headers, verify=False, timeout=15)

        return JSONResponse(content={
            "success": True,
            "status": resp.status_code,
            "size": len(resp.content),
            "content_type": resp.headers.get("Content-Type", ""),
            "snippet": resp.text[:2000],
        })
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)



# ── 통합 출결 엔드포인트 ──────────────────────────────────────────

def _parse_lms_attend(html: str) -> dict:
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
            "not_checked": not_chk, "total": total, "has_data": has_data}


def _parse_check_attend(attend_raw) -> dict:
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
            "not_checked": none, "total": total}


@app.post("/api/attendance/combined")
def combined_attendance(req: LmsCourseRequest):
    """
    통합 출결 조회: check.jvision(a) + LMS(b) = 전체 출결
    - is_interlock=0: check.jvision iwin_st_chulseokbu
    - is_interlock=1: LMS eclass_room2 → attendance_list
    반환: [{course_name, source, present, absent, late, not_checked, total}]
    """
    import re, json, urllib3
    from concurrent.futures import ThreadPoolExecutor, as_completed
    urllib3.disable_warnings()
    results = []

    # ── A. check.jvision 출결 ─────────────────────────────────────────
    try:
        # 포털 로그인 → 출결 시스템 로그인
        sess_chk = requests.Session()
        _restore_session_to(sess_chk, req.session_cookies)
        sess_chk.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

        # 포털 쿠키 재사용으로 빠른 SSO 로그인 시도
        chk_url = "https://check.jvision.ac.kr"
        ikey_json = json.dumps({"duser_id": req.user_id, "duser_pw": req.user_pw})
        login_r = sess_chk.post(f"{chk_url}/attend/iwin_sin",
            data={"ikey": ikey_json},
            headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With": "XMLHttpRequest",
                     "Referer": f"{chk_url}/jvision/student/", "Origin": chk_url},
            verify=False, timeout=20, allow_redirects=True)

        login_data = login_r.json()
        rollbook = login_data.get("rollbook", [])

        def fetch_check_course(course):
            sc = course.get("sugang_code", "")
            if not sc:
                return None
            s2 = requests.Session()
            s2.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
            for c in sess_chk.cookies:
                s2.cookies.set(c.name, c.value, domain=c.domain, path=c.path)
            chul_r = s2.post(f"{chk_url}/attend/iwin_st_chulseokbu",
                data={"ikey": json.dumps({"dclass": sc, "duser_id": req.user_id})},
                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With": "XMLHttpRequest",
                         "Referer": f"{chk_url}/jvision/student/", "Origin": chk_url},
                verify=False, timeout=30)
            try:
                raw = chul_r.json()
            except Exception as je:
                print(f"[check] JSON parse error for {sc}: {je}")
                raw = {}
            rbu = raw.get("rollbookuser", []) if isinstance(raw, dict) else []
            wl  = rbu[0].get("attendCountResultList", []) if rbu else []
            print(f"[check] {sc[:30]} rbu={len(rbu)} wl={len(wl)} attend={sum(w.get('attend_count',0) for w in wl)}")
            parsed = _parse_check_attend(raw)
            return {
                "course_name": course.get("sugang_name", sc),
                "source": "check",
                "sugang_code": sc,
                "total_classes": course.get("sugang_total_number", 0),
                **parsed,
            }

        # 과목명 기준 중복 제거 (같은 과목 여러 시간대)
        seen_nm, unique_rb = set(), []
        for c in rollbook:
            nm = c.get("sugang_name", "")
            if nm not in seen_nm:
                seen_nm.add(nm)
                unique_rb.append(c)
        with ThreadPoolExecutor(max_workers=6) as ex:
            futs = [ex.submit(fetch_check_course, c) for c in unique_rb]
            for f in as_completed(futs):
                r = f.result()
                if r:
                    results.append(r)

    except Exception as e:
        results.append({"error": f"check.jvision 오류: {str(e)}", "source": "check"})

    # ── B. LMS 출결 ────────────────────────────────────────────────────
    try:
        sess_lms = _lms_full_login(req.user_id, req.user_pw)
        lms_url = LMS_URL

        sess_lms.get(f"{lms_url}/ilos/mp/course_register_list_form.acl",
            headers={"Referer": f"{lms_url}/ilos/main/main_form.acl"}, verify=False, timeout=10)
        r_c = sess_lms.post(f"{lms_url}/ilos/mp/course_register_list.acl",
            data={"YEAR": req.year, "TERM": req.term, "num": "1", "encoding": "utf-8"},
            headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                     "X-Requested-With": "XMLHttpRequest",
                     "Referer": f"{lms_url}/ilos/mp/course_register_list_form.acl", "Origin": lms_url},
            verify=False, timeout=10)

        klass_ids = list(set(re.findall(r"eclassRoom\('([^']+)'\)", r_c.text)))

        for kjkey in klass_ids:
            r_ec = sess_lms.post(f"{lms_url}/ilos/st/course/eclass_room2.acl",
                data={"KJKEY": kjkey, "FLAG": "mp",
                      "returnURI": "/ilos/st/course/submain_form.acl", "encoding": "utf-8"},
                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With": "XMLHttpRequest",
                         "Referer": f"{lms_url}/ilos/mp/course_register_list_form.acl",
                         "Origin": lms_url},
                verify=False, timeout=10)
            try:
                ec = r_ec.json()
                if ec.get("isError"): continue
                return_url = ec.get("returnURL", "/ilos/st/course/submain_form.acl")
            except Exception:
                continue

            sess_lms.get(f"{lms_url}{return_url}",
                headers={"Referer": f"{lms_url}/ilos/mp/course_register_list_form.acl"},
                verify=False, timeout=10)
            sess_lms.get(f"{lms_url}/ilos/st/course/attendance_list_form.acl",
                headers={"Referer": f"{lms_url}/ilos/st/course/submain_form.acl"},
                verify=False, timeout=10)
            r_at = sess_lms.post(f"{lms_url}/ilos/st/course/attendance_list.acl",
                data={"ud": req.user_id, "ky": kjkey, "encoding": "utf-8"},
                headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With": "XMLHttpRequest",
                         "Referer": f"{lms_url}/ilos/st/course/attendance_list_form.acl",
                         "Origin": lms_url},
                verify=False, timeout=15)

            parsed = _parse_lms_attend(r_at.text) if len(r_at.content) > 200 else {}
            results.append({
                "course_name": kjkey,
                "source": "lms",
                "kjkey": kjkey,
                **parsed,
            })

    except Exception as e:
        results.append({"error": f"LMS 오류: {str(e)}", "source": "lms"})

    # 출석률 계산
    total_present = sum(r.get("present", 0) for r in results if "error" not in r)
    total_absent  = sum(r.get("absent", 0)  for r in results if "error" not in r)
    total_late    = sum(r.get("late", 0)    for r in results if "error" not in r)
    total_classes = sum(r.get("total", 0)   for r in results if "error" not in r)
    attend_rate   = round(total_present / total_classes * 100, 1) if total_classes > 0 else None

    return JSONResponse(content={
        "success": True,
        "year": req.year,
        "term": req.term,
        "courses": results,
        "summary": {
            "total_present": total_present,
            "total_absent": total_absent,
            "total_late": total_late,
            "total_classes": total_classes,
            "attendance_rate": attend_rate,
        }
    })

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
