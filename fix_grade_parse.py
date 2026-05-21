content = open('C:/Users/ruddls030/jvision/web.py', encoding='utf-8').read()

# Replace raw XML return in terms endpoint with parsed JSON
old_terms_return = '''        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/grades/detail")'''

new_terms_return = '''        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "data": _parse_nexacro(res.text, "ds_out"), "raw": res.text[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)


@app.post("/api/grades/detail")'''

# Replace raw XML return in detail endpoint
old_detail_return = '''        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "raw": res.text})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

new_detail_return = '''        res = sess.post(NMAIN_URL, data=payload.encode("utf-8"), headers=headers, verify=False, timeout=20)
        return JSONResponse(content={"success": True, "data": _parse_nexacro(res.text, "ds_out"), "raw": res.text[:500]})
    except Exception as e:
        return JSONResponse(content={"success": False, "message": str(e)}, status_code=500)

'''

# Add Nexacro XML parser helper before the grade endpoints
parser_func = '''
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

'''

# Insert parser before fetch_grade_terms
old_marker = '@app.post("/api/grades/terms")'
if old_marker in content and '_parse_nexacro' not in content:
    content = content.replace(old_marker, parser_func + old_marker)
    patched_parser = True
else:
    patched_parser = '_parse_nexacro' in content
    print(f"Parser already exists: {patched_parser}")

patched_terms = False
patched_detail = False

if old_terms_return in content:
    content = content.replace(old_terms_return, new_terms_return)
    patched_terms = True
    print("Patched terms return")
else:
    print("Terms return NOT FOUND")

if old_detail_return in content:
    content = content.replace(old_detail_return, new_detail_return)
    patched_detail = True
    print("Patched detail return")
else:
    print("Detail return NOT FOUND")

if patched_parser or patched_terms or patched_detail:
    open('C:/Users/ruddls030/jvision/web.py', 'w', encoding='utf-8').write(content)
    print("Saved web.py")
