import sys
import io
import json

def init_env(import_path: str):
    try:
        sys.path.index(import_path)
    except ValueError:
        sys.path.insert(0,import_path)
    import you_get

def parse_origin_from_referer(referer: str) -> str | None:
    from urllib.parse import urlparse
    parsed = urlparse(referer)
    if parsed.scheme and parsed.netloc:
        return f"{parsed.scheme}://{parsed.netloc}"
    return None

def build_headers_from_extra(extra: dict) -> dict:
    headers = {}
    if "ua" in extra:
        headers["User-Agent"] = extra["ua"]
    if "referer" in extra:
        headers["Referer"] = extra["referer"]
        origin = parse_origin_from_referer(extra["referer"])
        if origin:
            headers["Origin"] = origin
    return headers

def wrap_result(result: str) -> str:
    try:
        o = json.loads(result)
        if o["extra"] is None:
            return result
        o["request_headers"] = build_headers_from_extra(o["extra"])
        return json.dumps(o)
    except:
        return result


def extract(import_path: str, url: str):
    init_env(import_path)
    import you_get
    byte_buf = io.BytesIO()
    text_wrapper = io.TextIOWrapper(byte_buf, encoding='utf-8')

    old_stdout = sys.stdout
    sys.stdout = text_wrapper
    
    sys.argv = ['you-get', '--json',url]
    
    try:
        you_get.main()
        text_wrapper.flush()
        output = byte_buf.getvalue().decode('utf-8')
        return wrap_result(output)
    except SystemExit:
        text_wrapper.flush()
        return wrap_result(byte_buf.getvalue().decode('utf-8'))
    finally:
        sys.stdout = old_stdout