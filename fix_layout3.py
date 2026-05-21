content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

old = '''            <Column id="TX_NAME" type="string" size="100"/>
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
    </Dataset>'''

new = '''            <Column id="TX_NAME" type="string" size="100"/>
            <Column id="TYPE" type="string" size="10"/>
            <Column id="SQL_ID" type="string" size="200"/>
            <Column id="KEY_ZERO_LEN" type="INT" size="10"/>
            <Column id="EXEC_TYPE" type="STRING" size="2"/>
            <Column id="EXEC" type="STRING" size="1"/>
            <Column id="FAIL" type="STRING" size="1"/>
            <Column id="FAIL_MSG" type="STRING" size="200"/>
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
    </Dataset>'''

if old in content:
    content = content.replace(old, new)
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("PATCHED - added EXEC/FAIL columns")
else:
    print("NOT FOUND")
    idx = content.find('KEY_ZERO_LEN" type="INT"')
    while idx > 0:
        print(f"At {idx}: " + repr(content[idx-20:idx+100]))
        idx = content.find('KEY_ZERO_LEN" type="INT"', idx+1)
