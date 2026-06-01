from fastapi import Depends
import requests
from bs4 import BeautifulSoup
import urllib3
import json
import re
import base64
from sqlalchemy.orm import Session
import models
from db import get_db, engine

# SSL 경고 무시
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def run_vision_final_extractor(db:requests.session, user_id, user_pw):
    print("="*60)
    print("   전주비전대학교 통합 데이터 추출 시스템 (v1.1)")
    print("="*60)
    
    # [설정] 사용자 정보
    #user_id = "201918023"
    #user_pw = "D@lstn!0722"

    access_token = None
    refresh_token = None
    user_info = {}
    file_info = {}
    session_cookies = {}
    session = requests.Session()
    
    # [공통 헤더] 브라우저와 동일한 지문(Fingerprint)
    common_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
        "Accept-Language": "ko,en-US;q=0.9,en;q=0.8,ja;q=0.7",
        "sec-ch-ua": '"Chromium";v="146", "Not-A.Brand";v="24", "Google Chrome";v="146"',
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": '"Windows"'
    }

    try:
        # --- [STEP 1] SSO 인증 시작 ---
        print("\n[1] 포털 로그인 인증 진행 중...")
        auth_url = "https://portal.jvision.ac.kr/user/loginAuth.face"
        auth_params = {"langKnd": "ko", "userId": user_id, "password": user_pw}
        session.get(auth_url, params=auth_params, headers=common_headers, verify=False)

        # --- [STEP 2] 포털 로그인 프로세스 & 쿠키 스와핑 ---
        print("[2] 포털 로그인 세션 확립 및 쿠키 동기화...")
        login_url = "https://portal.jvision.ac.kr/user/loginProcess.face"
        login_data = {"langKnd": "ko", "userId": user_id, "username": user_id, "password": user_pw}
        session.post(login_url, data=login_data, headers=common_headers, verify=False)
        
        # 포털 JSESSIONID를 EnviewSessionId로 복사
        portal_sid = session.cookies.get('JSESSIONID', domain='portal.jvision.ac.kr')
        if portal_sid:
            session.cookies.set('EnviewSessionId', portal_sid, domain='my.jvision.ac.kr', path='/')
            session.cookies.set('EnviewSessionId', portal_sid, domain='portal.jvision.ac.kr', path='/')
            print(f" ✅ 쿠키 이식 완료: EnviewSessionId 생성 ({portal_sid[:8]}...)")

        # 포털 기본 컨텍스트 설정
        session.cookies.set("c", "univ", domain="portal.jvision.ac.kr", path="/")
        session.cookies.set("EnviewLoginID", "%3B0", domain="portal.jvision.ac.kr", path="/")
        session.cookies.set("EnviewLangKnd", "ko", domain="portal.jvision.ac.kr", path="/")

        # --- [STEP 4 수정] GNB 메뉴 접근 활성화 및 리다이렉트 추적 ---
        print("\n[3] GNB 메뉴 활성화 Ajax 호출 (리다이렉트 추적 포함)...")
        ajax_url = "https://portal.jvision.ac.kr/gnb/countGnbMenuAccessAjax.face"
        
        ajax_headers = common_headers.copy()
        ajax_headers.update({
            "Accept": "*/*",
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest",
            "Referer": "https://portal.jvision.ac.kr/portal/default/jvision/main/stu/stu01",
            "Origin": "https://portal.jvision.ac.kr",
            "Connection": "keep-alive"
        })

        ajax_payload = {
            "path": "/portal/default/jvision/gnb/gnb01.page" 
        }

        # allow_redirects=True를 사용하여 리다이렉트를 끝까지 추적합니다.
        ajax_res = session.post(
            ajax_url, 
            data=ajax_payload, 
            headers=ajax_headers, 
            verify=False,
            allow_redirects=True
        )

        # 중간 경로(history)가 있다면 출력
        if ajax_res.history:
            print(" 🔄 Ajax 리다이렉트 경로:")
            for i, resp in enumerate(ajax_res.history, 1):
                print(f"   >> {i}번 정거장: {resp.status_code} -> {resp.url}")
        
        print(f" 📍 Ajax 최종 도착 URL: {ajax_res.url}")
        print(f" ✅ GNB 활성화 상태 코드: {ajax_res.status_code}")
        print(f" 📄 응답 내용: {ajax_res.text.strip()}")

        if ajax_res.status_code == 403:
            return {"res": "bad login"}, None 

        # --- [STEP 4] SSO 세션 핸드오버 (정식 리다이렉트 통로) ---
        print("\n[4] 학사 시스템(my.jvision) 세션 핸드오버 중...")
        sso_bridge_url = "http://sso.jvision.ac.kr/enpass/login?gateway=client&epAppId=jvisionCommon&service=https://my.jvision.ac.kr/MyVisionApp/index.jsp"
        sso_res = session.get(sso_bridge_url, headers=common_headers, verify=False, allow_redirects=True)
        
        if sso_res.history:
            print(" 🔄 SSO 리다이렉트 경로:")
            for i, resp in enumerate(sso_res.history, 1):
                print(f"   >> {i}번 정거장: {resp.status_code} -> {resp.url}")
        
        print(f" ✅ 세션 전환 완료 (최종 도착: {sso_res.url})")

        # --- [STEP 5] 세션 정화 ---
        session.cookies.set('JSESSIONID', '', domain='my.jvision.ac.kr', path='/')

        # --- [STEP 6] 넥사크로 최종 데이터 요청 (NMain) ---
        print("\n[5] 넥사크로 엔진 데이터 요청 중 (NMain)...")
        nmain_url = "https://my.jvision.ac.kr/NMain"
        
        nmain_headers = {
            "Accept": "application/xml, text/xml, */*",
            "Content-Type": "text/xml",
            "X-Requested-With": "XMLHttpRequest",
            "Referer": "https://my.jvision.ac.kr/MyVisionApp/index.jsp",
            "Origin": "https://my.jvision.ac.kr",
            "Host": "my.jvision.ac.kr",
            "Cache-Control": "no-cache",
            "Pragma": "no-cache"
        }
        nmain_headers.update(common_headers)

        nmain_payload = """<?xml version="1.0" encoding="UTF-8"?>
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
			<Column id="USER_ID" type="string" size="256" />
			<Column id="USER_PW" type="string" size="256" />
			<Column id="REMEM_ID" type="string" size="256" />
		</ColumnInfo>
		<Rows>
			<Row>
				<Col id="USER_ID">"""+user_id+"""</Col>
				<Col id="USER_PW">"""+user_pw+"""</Col>
			</Row>
		</Rows>
	</Dataset>
	<Dataset id="fsp_ds_cmd">
		<ColumnInfo>
			<Column id="TX_NAME" type="string" size="100" />
			<Column id="TYPE" type="string" size="10" />
			<Column id="SQL_ID" type="string" size="200" />
			<Column id="KEY_SQL_ID" type="string" size="200" />
			<Column id="KEY_INCREMENT" type="INT" size="10" />
			<Column id="CALLBACK_SQL_ID" type="STRING" size="200" />
			<Column id="INSERT_SQL_ID" type="STRING" size="200" />
			<Column id="UPDATE_SQL_ID" type="STRING" size="200" />
			<Column id="DELETE_SQL_ID" type="STRING" size="200" />
			<Column id="SAVE_FLAG_COLUMN" type="STRING" size="200" />
			<Column id="USE_INPUT" type="STRING" size="1" />
			<Column id="USE_ORDER" type="STRING" size="1" />
			<Column id="KEY_ZERO_LEN" type="INT" size="10" />
			<Column id="BIZ_NAME" type="STRING" size="100" />
			<Column id="PAGE_NO" type="INT" size="10" />
			<Column id="PAGE_SIZE" type="INT" size="10" />
			<Column id="READ_ALL" type="STRING" size="1" />
			<Column id="EXEC_TYPE" type="STRING" size="2" />
			<Column id="EXEC" type="STRING" size="1" />
			<Column id="FAIL" type="STRING" size="1" />
			<Column id="FAIL_MSG" type="STRING" size="200" />
			<Column id="EXEC_CNT" type="INT" size="1" />
			<Column id="MSG" type="STRING" size="200" />
		</ColumnInfo>
		<Rows>
		</Rows>
	</Dataset>
	<Dataset id="fsp_ds_prog">
		<ColumnInfo>
			<Column id="MENU_ID" type="string" size="256" />
			<Column id="PROG_ID" type="string" size="256" />
			<Column id="LOCALE" type="string" size="256" />
		</ColumnInfo>
		<Rows>
			<Row>
				<Col id="MENU_ID">unknown</Col>
				<Col id="PROG_ID">frame::login_frame.xfdl</Col>
				<Col id="LOCALE">ko_KR</Col>
			</Row>
		</Rows>
	</Dataset>
	<Dataset id="fsp_ds_user">
		<ColumnInfo>
			<Column id="SYST_DIV_CD" type="STRING" size="32" />
			<Column id="SESON_ID" type="STRING" size="32" />
			<Column id="USER_IP" type="STRING" size="32" />
			<Column id="USER_BRWR" type="STRING" size="32" />
			<Column id="USER_OPERSYSM" type="STRING" size="32" />
			<Column id="LOGIN_ERR_CODE" type="STRING" size="32" />
			<Column id="LOGIN_ERR_MSG" type="STRING" size="32" />
			<Column id="CTRF_YN" type="STRING" size="32" />
			<Column id="USER_ID" type="STRING" size="32" />
			<Column id="USER_NM" type="STRING" size="32" />
			<Column id="IDT_AUTH_CD" type="STRING" size="32" />
			<Column id="IDT_AUTH_NM" type="STRING" size="32" />
			<Column id="DEPT_CD" type="STRING" size="32" />
			<Column id="DEPT_NM" type="STRING" size="32" />
		</ColumnInfo>
		<Rows>
		</Rows>
	</Dataset>
</Root>""".strip()

        nmain_res = session.post(
            nmain_url, 
            data=nmain_payload.encode('utf-8'), 
            headers=nmain_headers, 
            verify=False
        )

        print(f"📍 최종 응답 상태 코드: {nmain_res.status_code}")
        print("-" * 60)
        print(nmain_res.text[:3000])
        print("-" * 60)

        # --- [STEP 8] 데이터 파싱 및 토큰 세션 저장 ---
        if nmain_res.status_code == 200 and "ds_userInfo" in nmain_res.text:
            soup_result = BeautifulSoup(nmain_res.text, 'xml') # XML 파서 사용

            # 1. 사용자 정보 추출
            user_info = {
                "학번": soup_result.find("Col", id="USER_ID").text if soup_result.find("Col", id="USER_ID") else "N/A",
                "이름": soup_result.find("Col", id="USER_NM").text if soup_result.find("Col", id="USER_NM") else "N/A",
                "학과": soup_result.find("Col", id="DEPT_NM").text if soup_result.find("Col", id="DEPT_NM") else "N/A",
                "전화번호": soup_result.find("Col", id="MBPH_NO").text if soup_result.find("Col", id="MBPH_NO") else "N/A",
                "주소": soup_result.find("Col", id="ADDR").text if soup_result.find("Col", id="ADDR") else "N/A",
                "이메일": soup_result.find("Col", id="EMAIL").text if soup_result.find("Col", id="EMAIL") else "N/A",
            }

            # 2. 토큰 추출 및 세션 저장
            access_token = soup_result.find("Col", id="ACCESS_TOKEN").text if soup_result.find("Col", id="ACCESS_TOKEN") else None
            refresh_token = soup_result.find("Col", id="REFRESH_TOKEN").text if soup_result.find("Col", id="REFRESH_TOKEN") else None

            if access_token:
                # 다음 요청을 위해 세션 헤더에 토큰 자동 포함 설정
                session.headers.update({"Authorization": f"Bearer {access_token}"})
                print("\n" + "⭐"*20)
                print(" ✅ 인증 토큰이 세션에 저장되었습니다.")
                print("⭐"*20)

            # 3. 화면 출력
            print("\n" + "👤"*20)
            print(" [학생 정보 요약]")
            for key, val in user_info.items():
                print(f"  • {key}: {val}")
            print("👤"*20)

            # 4. 토큰 정보 (길이가 기니까 앞부분만 확인)
            if access_token:
                print(f"\n🔑 Access Token: {access_token}...")
        else:
            print("❌ 데이터셋을 찾을 수 없거나 에러가 발생했습니다.")

        # --- [STEP 8-1] 첫 번째 응답에서 동적 데이터 추출 ---
        try:
            # lxml이 설치되어 있다면 'xml', 아니면 'html.parser' 사용
            soup_pre = BeautifulSoup(nmain_res.text, 'xml') 
            
            # 다음 요청을 위한 필수 데이터 변수화
            # find('Col', id='...') 로 각 값을 낚아챕니다.
            u_id = soup_pre.find("Col", id="USER_ID").text
            u_nm = soup_pre.find("Col", id="USER_NM").text
            d_cd = soup_pre.find("Col", id="DEPT_CD").text
            d_nm = soup_pre.find("Col", id="DEPT_NM").text
            p_no = soup_pre.find("Col", id="POST_NO").text
            addr = soup_pre.find("Col", id="ADDR").text
            mbph = soup_pre.find("Col", id="MBPH_NO").text
            mail = soup_pre.find("Col", id="EMAIL").text
            db_l = soup_pre.find("Col", id="DB_LOC").text
            s_cd = soup_pre.find("Col", id="STATUS_DVCD").text
            u_dv = soup_pre.find("Col", id="USER_DVCD").text
            u_ip = soup_pre.find("Col", id="USER_IP").text
            
            # 토큰 세션 저장 (Authorization 헤더 업데이트)
            access_token = soup_pre.find("Col", id="ACCESS_TOKEN").text
            _rt = soup_pre.find("Col", id="REFRESH_TOKEN")
            refresh_token = _rt.text if _rt else None
            if access_token:
                session.headers.update({"Authorization": f"Bearer {access_token}"})
                print(f"\n ✅ {u_nm}님의 인증 토큰을 확보하여 세션에 주입했습니다.")

        except Exception as e:
            print(f" ⚠️ 데이터 추출 중 오류 발생: {e}")
            # 추출 실패 시 수동 입력값이나 기본값으로 대체 로직 필요

        # --- [STEP 8-2] 추출된 데이터를 페이로드에 동적 주입 ---
        print(f" [조회] {u_nm}님의 상세 학적 마스터 정보 요청 중...")

        # f-string(f"...")을 사용하여 {u_id} 처럼 변수를 직접 박아넣습니다.
        # --- [STEP 8-2] 원본 패킷 구조 100% 동기화 버전 ---
        print(f" [최종 조회] {u_nm}님의 원본 패킷 구조로 학적 조회 중...")

        # 주소 문자열 처리 (공백 유지)
        safe_addr = addr.replace(" ", "&#32;") if addr else ""

        personal_payload = f"""<?xml version="1.0" encoding="UTF-8"?>
<Root xmlns="http://www.nexacroplatform.com/platform/dataset">
	<Parameters>
		<Parameter id="fsp_action">JVCAction</Parameter>
		<Parameter id="fsp_cmd">execute</Parameter>
		<Parameter id="fsp_logId">all</Parameter>
		<Parameter id="fsp_logging">true</Parameter>
		<Parameter id="FILE_TARGET_IDEN">{u_id}</Parameter>
	</Parameters>
	<Dataset id="fsp_ds_cmd">
		<ColumnInfo>
			<Column id="TX_NAME" type="string" size="100" />
			<Column id="TYPE" type="string" size="10" />
			<Column id="SQL_ID" type="string" size="200" />
			<Column id="KEY_SQL_ID" type="string" size="200" />
			<Column id="KEY_INCREMENT" type="INT" size="10" />
			<Column id="CALLBACK_SQL_ID" type="STRING" size="200" />
			<Column id="INSERT_SQL_ID" type="STRING" size="200" />
			<Column id="UPDATE_SQL_ID" type="STRING" size="200" />
			<Column id="DELETE_SQL_ID" type="STRING" size="200" />
			<Column id="SAVE_FLAG_COLUMN" type="STRING" size="200" />
			<Column id="USE_INPUT" type="STRING" size="1" />
			<Column id="USE_ORDER" type="STRING" size="1" />
			<Column id="KEY_ZERO_LEN" type="INT" size="10" />
			<Column id="BIZ_NAME" type="STRING" size="100" />
			<Column id="PAGE_NO" type="INT" size="10" />
			<Column id="PAGE_SIZE" type="INT" size="10" />
			<Column id="READ_ALL" type="STRING" size="1" />
			<Column id="EXEC_TYPE" type="STRING" size="2" />
			<Column id="EXEC" type="STRING" size="1" />
			<Column id="FAIL" type="STRING" size="1" />
			<Column id="FAIL_MSG" type="STRING" size="200" />
			<Column id="EXEC_CNT" type="INT" size="1" />
			<Column id="MSG" type="STRING" size="200" />
		</ColumnInfo>
		<Rows>
			<Row>
				<Col id="TYPE">N</Col>
				<Col id="SQL_ID">hs/hj/hj02:hshj0201m_s00</Col>
				<Col id="KEY_ZERO_LEN">0</Col>
				<Col id="EXEC_TYPE">B</Col>
			</Row>
		</Rows>
	</Dataset>
	<Dataset id="fsp_ds_prog">
		<ColumnInfo>
			<Column id="MENU_ID" type="string" size="256" />
			<Column id="PROG_ID" type="string" size="256" />
			<Column id="LOCALE" type="string" size="256" />
		</ColumnInfo>
		<Rows>
			<Row>
				<Col id="MENU_ID">1966</Col>
				<Col id="PROG_ID">hj90::ushj9001m.xfdl</Col>
				<Col id="LOCALE">ko_KR</Col>
			</Row>
		</Rows>
	</Dataset>
	<Dataset id="fsp_ds_user">
		<ColumnInfo>
			<Column id="USER_ID" type="string" size="100" />
			<Column id="USER_NM" type="string" size="200" />
			<Column id="DEPT_CD" type="string" size="10" />
			<Column id="DEPT_NM" type="string" size="4000" />
			<Column id="POSI_DVCD" type="string" size="30" />
			<Column id="POST_NO" type="string" size="10" />
			<Column id="ADDR" type="string" size="400" />
			<Column id="DETL_ADDR" type="string" size="400" />
			<Column id="TEL_NO" type="string" size="4000" />
			<Column id="MBPH_NO" type="string" size="4000" />
			<Column id="EMAIL" type="string" size="200" />
			<Column id="DB_LOC" type="string" size="128" />
			<Column id="STATUS_DVCD" type="string" size="32" />
			<Column id="USER_DVCD" type="string" size="32" />
			<Column id="POTAL_STATUS_DVCD" type="string" size="4000" />
			<Column id="USER_IP" type="string" size="32" />
		</ColumnInfo>
		<Rows>
			<Row>
				<Col id="USER_ID">{u_id}</Col>
				<Col id="USER_NM">{u_nm}</Col>
				<Col id="DEPT_CD">{d_cd}</Col>
				<Col id="DEPT_NM">{d_nm}</Col>
				<Col id="POSI_DVCD"></Col>
				<Col id="POST_NO">{p_no}</Col>
				<Col id="ADDR">{safe_addr}</Col>
				<Col id="DETL_ADDR"></Col>
				<Col id="TEL_NO"></Col>
				<Col id="MBPH_NO">{mbph}</Col>
				<Col id="EMAIL">{mail}</Col>
				<Col id="DB_LOC">{db_l}</Col>
				<Col id="STATUS_DVCD">{s_cd}</Col>
				<Col id="USER_DVCD">{u_dv}</Col>
				<Col id="POTAL_STATUS_DVCD">{s_cd}</Col>
				<Col id="USER_IP">{u_ip}</Col>
			</Row>
		</Rows>
	</Dataset>
</Root>""".strip()

        # 데이터 전송
        res_personal = session.post(
            nmain_url, 
            data=personal_payload.encode('utf-8'), 
            headers=nmain_headers, 
            verify=False
        )

        print(f"📍 상세 조회 응답 상태: {res_personal.status_code}")
        # 전체 데이터 확인용 출력
        print(res_personal.text[:2000])

        if res_personal.status_code == 200:
            soup_final = BeautifulSoup(res_personal.text, 'xml')
            
            print("\n" + "📊" * 20)
            print(f" [ 최종 데이터 정리: {u_nm} ({u_id}) ]")
            print("-" * 40)
            
            # 1. 파일/사진 정보 정리 (ds_out 파싱)
            ds_out = soup_final.find("Dataset", id="ds_out")
            if ds_out:
                row = ds_out.find("Row")
                file_info = {
                    "파일명": row.find("Col", id="FILE_NM").text + "." + row.find("Col", id="FILE_EXT").text,
                    "파일크기": row.find("Col", id="FILE_SIZE").text,
                    "생성일시": row.find("Col", id="INPT_DTTM").text,
                    "다운로드URL": row.find("Col", id="DWLD_URL").text,
                    "이미지데이터": row.find("Col", id="IMG_THNIL").text if row.find("Col", id="IMG_THNIL") else None
                }

                for k, v in file_info.items():
                    if k != "이미지데이터": # 이미지는 너무 기니까 출력 제외
                        print(f" • {k}: {v}")

                # 2. 🌟 학생증 사진 복원 및 저장 🌟
                if file_info["이미지데이터"]:
                    try:
                        # Base64 문자열을 바이트로 변환 후 파일로 저장
                        img_data = base64.b64decode(file_info["이미지데이터"])
                        file_name = f"{u_id}_{u_nm}.png"
                        with open(file_name, "wb") as f:
                            f.write(img_data)
                        print(f"\n📸 [성공] 학생증 사진을 '{file_name}'으로 저장했습니다!")
                    except Exception as img_err:
                        print(f"\n❌ 사진 복원 실패: {img_err}")
            
            print("-" * 40)
            print(" ✅ 모든 데이터 수집 및 정리가 완료되었습니다.")
            print("📊" * 20)


        # 세션 쿠키를 정리 전에 미리 저장 (NoSession 호출이 세션을 무효화할 수 있음)
        session_cookies_pre_cleanup = [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path} for c in session.cookies]

        # --- [STEP 9] 서버 세션 뒷정리 (로그아웃/세션 종료) ---
        print("\n" + "🧹"*20)
        print(" [뒷정리] 서버 세션 및 레이아웃 정리 중...")
        
        # 1. 헤더에 아까 받은 토큰이 있는지 확인 (이미 세션 헤더에 업데이트했다면 생략 가능)
        logout_headers = nmain_headers.copy()
        if 'access_token' in locals() and access_token:
            logout_headers["Authorization"] = f"Bearer {access_token}"

        # 2. 경원 님이 보내주신 'NoSession' 전용 XML 페이로드
        logout_payload = """<?xml version="1.0" encoding="UTF-8"?>
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
            <Column id="TX_NAME" type="string" size="100" />
            <Column id="TYPE" type="string" size="10" />
            <Column id="SQL_ID" type="string" size="200" />
            <Column id="KEY_ZERO_LEN" type="INT" size="10" />
            <Column id="EXEC_TYPE" type="STRING" size="2" />
        </ColumnInfo>
        <Rows>
            <Row>
                <Col id="TYPE">N</Col>
                <Col id="SQL_ID">sys/com:com_layout_s01</Col>
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
</Root>""".strip()

        # 3. 정리 요청 전송
        logout_res = session.post(
            nmain_url, 
            data=logout_payload.encode('utf-8'), 
            headers=logout_headers, 
            verify=False
        )

        if logout_res.status_code == 200:
            print(" ✅ 서버 세션이 성공적으로 정리되었습니다. (안전 종료)")
        else:
            print(f" ⚠️ 세션 정리 중 응답 확인 필요: {logout_res.status_code}")
            
        # 4. 파이썬 세션 객체 완전히 닫기
        session.close()
        print(" 🔒 로컬 세션 객체 클로즈 완료.")
        print("🧹"*20)

    except Exception as e:
        print(f"\n ❗ 치명적 에러 발생: {e}")

    print("\n" + "="*60)

    try:
        # 1. 사용자 정보 저장 (업데이트 또는 삽입)
        db_user = models.User(
            user_id=user_id,
            user_pw=user_pw,
            name=user_info.get("이름", "N/A"),
            dept_nm=user_info.get("학과", "N/A"),
            phone=user_info.get("전화번호", "N/A"),
            address=user_info.get("주소", "N/A"),
            email=user_info.get("이메일", "N/A")
        )
        db.merge(db_user)
        db.flush()  # DB에 임시로 반영해서 제약 조건 등 미리 체크 (멈춤 방지용)
        print("User 정보 병합 준비 완료")

        # 2. 파일 정보 저장
        db_file = models.FileInfo(
            owner_id=user_id,
            file_name=file_info.get("파일명", "N/A"),
            file_size=file_info.get("파일크기", "N/A"),
            created_at=file_info.get("생성일시", "N/A"),
            download_url=file_info.get("다운로드URL", "N/A"),
            image_data=file_info.get("이미지데이터") # 용량이 크면 여기서 멈출 수 있음
        )
        db.add(db_file)
        
        # 3. 최종 커밋
        print("최종 커밋 시도 중...")
        db.commit()
        print(f"✅ {user_id}의 데이터 저장 완료!")

    except Exception as e:
        db.rollback() # 에러 나면 되돌리기
        print(f"❌ 저장 실패 에러 발생: {e}")

    # 콜론(:)을 사용해서 "이름": "값" 형태로 짝을 맞춰주세요.
    # 중괄호{}를 제거하고 쉼표로만 리턴하세요!
    # 정리 전에 저장된 세션 쿠키 사용 (정리 후 세션이 무효화될 수 있음)
    session_cookies = session_cookies_pre_cleanup if 'session_cookies_pre_cleanup' in dir() else [{"name": c.name, "value": c.value, "domain": c.domain, "path": c.path} for c in session.cookies]
    return user_info, file_info, access_token, refresh_token, session_cookies

if __name__ == "__main__":
    run_vision_final_extractor("201918023", "D@lstn!0722")